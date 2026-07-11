package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModSounds;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.StealthBossDetectionHelper;
import net.solocraft.entity.RulersAuthorityAuraEntity;
import net.solocraft.entity.HunterEntity;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative controller for Ruler's Hand and Ruler's Authority.
 * One key supports taps, holds, camera control, scroll distance, throws and slams.
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RulersAuthorityManager {
    private static final String COOLDOWN = "telekinesis";
    private static final String LAUNCH_PROTECTION = "sl_telekinesis_launch_protection_until";
    private static final int HOLD_TICKS = 5;
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> BOSS_TAG =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("soloboss"));

    private static final Map<UUID, ControlSession> SESSIONS = new HashMap<>();
    private static final Map<UUID, ThrownState> THROWN = new HashMap<>();

    private RulersAuthorityManager() {
    }

    public static boolean hasAbility(Entity entity) {
        if (entity == null)
            return false;
        String abilities = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .map(data -> data.abilities)
                .orElse("");
        return containsAbility(abilities, "telekinesis")
                || containsAbility(abilities, "rulers_hand")
                || containsAbility(abilities, "ruler_s_hand")
                || containsAbility(abilities, "rulers_authority")
                || containsAbility(abilities, "ruler_s_authority");
    }

    public static boolean hasAuthority(Entity entity) {
        if (entity == null)
            return false;
        String abilities = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .map(data -> data.abilities)
                .orElse("");
        // Existing saves use "telekinesis" for the runestone that grants Ruler's Authority.
        return containsAbility(abilities, "telekinesis")
                || containsAbility(abilities, "rulers_authority")
                || containsAbility(abilities, "ruler_s_authority");
    }

    public static void begin(ServerPlayer player) {
        if (!canUse(player) || SESSIONS.containsKey(player.getUUID()))
            return;
        if (CooldownManager.isOnCooldown(player, COOLDOWN)) {
            player.displayClientMessage(Component.literal("Ruler's Authority is on cooldown").withStyle(ChatFormatting.RED), true);
            return;
        }

        boolean authority = hasAuthority(player);
        ControlSession session = new ControlSession(player.level().getGameTime(), player.isShiftKeyDown(), authority,
                authority ? 9.0D : 7.0D);
        SESSIONS.put(player.getUUID(), session);
    }

    public static void release(ServerPlayer player, int pressedMs) {
        ControlSession session = SESSIONS.remove(player.getUUID());
        if (session == null)
            return;

        Entity controlled = getControlledEntity(player, session);
        if (controlled != null) {
            if (session.bossResistance) {
                finishBossPressure(player, controlled, session);
            } else {
                throwControlled(player, controlled, session, player.isShiftKeyDown());
            }
            return;
        }

        discardAura(session);
        boolean charged = pressedMs >= 450 || player.level().getGameTime() - session.startedAt >= 9;
        performTap(player, session.sneakingAtStart || player.isShiftKeyDown(), charged, session.authority);
    }

    public static void adjustDistance(ServerPlayer player, int direction) {
        ControlSession session = SESSIONS.get(player.getUUID());
        if (session == null || session.controlledId == null || session.bossResistance || direction == 0)
            return;
        double maximum = session.authority ? 22.0D : 14.0D;
        session.distance = Mth.clamp(session.distance + Math.signum(direction) * 1.5D, 3.0D, maximum);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player))
            return;

        ControlSession session = SESSIONS.get(player.getUUID());
        if (session == null)
            return;
        if (!canUse(player) || player.isSpectator() || !player.isAlive()) {
            cancelSession(player, session);
            return;
        }

        long heldTicks = player.level().getGameTime() - session.startedAt;
        if (session.controlledId == null && heldTicks >= HOLD_TICKS) {
            tryAcquire(player, session);
        }

        Entity controlled = getControlledEntity(player, session);
        if (controlled == null)
            return;
        if (!controlled.isAlive() || controlled.level() != player.level()
                || controlled.distanceToSqr(player) > Math.pow(session.authority ? 45.0D : 30.0D, 2)) {
            restoreGravity(controlled, session);
            discardAura(session);
            session.controlledId = null;
            session.controlledEntity = null;
            return;
        }

        if (advanceBreakout(player, controlled, session))
            return;

        session.drainTicker++;
        if (session.drainTicker >= 20) {
            session.drainTicker = 0;
            double drain = (session.authority ? 18.0D : 12.0D) + session.weight * 3.0D;
            if (!consumeMana(player, drain)) {
                player.displayClientMessage(Component.literal("Ruler's power released: not enough mana").withStyle(ChatFormatting.RED), true);
                cancelSession(player, session);
                return;
            }
            CooldownManager.set(player, "mana_refresh", 35);
        }

        if (session.bossResistance) {
            applyBossPressure(player, controlled, session);
        } else {
            updateControlledPosition(player, controlled, session);
        }
        if (session.authority)
            deflectNearbyProjectiles(player);
        renderControlEffect(player.serverLevel(), player, controlled, session);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || THROWN.isEmpty())
            return;

        Iterator<Map.Entry<UUID, ThrownState>> iterator = THROWN.entrySet().iterator();
        while (iterator.hasNext()) {
            ThrownState state = iterator.next().getValue();
            state.ticks++;
            Entity target = state.target;
            ServerPlayer owner = state.owner;
            if (target == null || owner == null || !target.isAlive() || target.level() != owner.level()) {
                iterator.remove();
                continue;
            }

            Entity collision = findThrowCollision(owner, target);
            boolean hitSurface = target.horizontalCollision || (state.ticks > 4 && target.onGround());
            if (collision != null || hitSurface) {
                resolveImpact(owner, target, collision, state);
                iterator.remove();
            } else if (state.ticks > 45) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ControlSession session = SESSIONS.remove(player.getUUID());
            if (session != null) {
                discardAura(session);
                restoreGravity(getControlledEntity(player, session), session);
            }
            THROWN.values().removeIf(state -> state.owner.getUUID().equals(player.getUUID()));
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ControlSession session = SESSIONS.remove(player.getUUID());
            if (session != null) {
                discardAura(session);
                restoreGravity(getControlledEntity(player, session), session);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        long protectedUntil = entity.getPersistentData().getLong(LAUNCH_PROTECTION);
        if (protectedUntil < entity.level().getGameTime()) {
            entity.getPersistentData().remove(LAUNCH_PROTECTION);
            return;
        }
        event.setDamageMultiplier(event.getDamageMultiplier() * 0.3F);
        entity.getPersistentData().remove(LAUNCH_PROTECTION);
    }

    private static void tryAcquire(ServerPlayer player, ControlSession session) {
        double range = session.authority ? 30.0D : 18.0D;
        Entity target = findLookTarget(player, range);
        if (target == null)
            return;

        ControlProfile control = createControlProfile(player, target, session.authority);
        if (control.negated) {
            consumeMana(player, session.authority ? 42.0D : 30.0D);
            renderNegation(player.serverLevel(), target);
            playTelekinesisSound(player, 0.48F);
            player.displayClientMessage(Component.literal(target.getDisplayName().getString()
                    + " negated " + (session.authority ? "Ruler's Authority" : "Ruler's Hand"))
                    .withStyle(ChatFormatting.RED), true);
            if (target instanceof ServerPlayer targetPlayer) {
                targetPlayer.displayClientMessage(Component.literal("You negated " + player.getName().getString() + "'s control")
                        .withStyle(ChatFormatting.AQUA), true);
            }
            CooldownManager.set(player, "mana_refresh", 20);
            CooldownManager.set(player, COOLDOWN, session.authority ? 28 : 38);
            SESSIONS.remove(player.getUUID());
            return;
        }

        session.casterPower = control.casterPower;
        session.targetPower = control.targetPower;
        session.breakoutLimitTicks = control.breakoutTicks;
        session.unstableControl = control.unstable;

        boolean boss = isBoss(target);
        if (boss && !session.authority) {
            if (!consumeMana(player, 55.0D)) {
                player.displayClientMessage(Component.literal("Not enough mana to pressure " + target.getDisplayName().getString()).withStyle(ChatFormatting.RED), true);
                SESSIONS.remove(player.getUUID());
                return;
            }
            player.displayClientMessage(Component.literal(target.getDisplayName().getString() + " resisted Ruler's Hand").withStyle(ChatFormatting.RED), true);
            session.controlledId = target.getUUID();
            session.controlledEntity = target;
            session.bossResistance = true;
            session.weight = entityWeight(target);
            session.auraEntity = RulersAuthorityAuraEntity.spawn(player.serverLevel(), target, session.authority, true);
            notifyControlledTarget(player, target);
            return;
        }

        double weight = entityWeight(target);
        if (!session.authority && weight > 5.5D) {
            if (!consumeMana(player, 55.0D)) {
                player.displayClientMessage(Component.literal("Not enough mana to pressure " + target.getDisplayName().getString()).withStyle(ChatFormatting.RED), true);
                SESSIONS.remove(player.getUUID());
                return;
            }
            player.displayClientMessage(Component.literal("This target is too powerful for Ruler's Hand").withStyle(ChatFormatting.RED), true);
            session.controlledId = target.getUUID();
            session.controlledEntity = target;
            session.bossResistance = true;
            session.weight = weight;
            session.auraEntity = RulersAuthorityAuraEntity.spawn(player.serverLevel(), target, session.authority, true);
            notifyControlledTarget(player, target);
            return;
        }

        double initialCost = (session.authority ? 120.0D : 80.0D) + weight * 8.0D;
        if (!consumeMana(player, initialCost)) {
            player.displayClientMessage(Component.literal("Not enough mana to seize " + target.getDisplayName().getString()).withStyle(ChatFormatting.RED), true);
            SESSIONS.remove(player.getUUID());
            return;
        }

        session.controlledId = target.getUUID();
        session.controlledEntity = target;
        session.bossResistance = boss;
        session.originalNoGravity = target.isNoGravity();
        session.weight = weight;
        session.auraEntity = RulersAuthorityAuraEntity.spawn(player.serverLevel(), target, session.authority, boss);
        if (!boss) {
            target.setNoGravity(true);
            target.setDeltaMovement(Vec3.ZERO);
            if (target instanceof Mob mob)
                mob.getNavigation().stop();
        }
        CooldownManager.set(player, "mana_refresh", 35);
        playTelekinesisSound(player, boss ? 0.7F : 1.15F);
        player.displayClientMessage(Component.literal(boss
                ? "Authority pressure: " + target.getDisplayName().getString()
                : (session.unstableControl ? "Unstable control: " : "Seized: ") + target.getDisplayName().getString())
                .withStyle(session.unstableControl ? ChatFormatting.YELLOW : ChatFormatting.AQUA), true);
        notifyControlledTarget(player, target);
    }

    private static void performTap(ServerPlayer player, boolean pull, boolean charged, boolean authority) {
        if (CooldownManager.isOnCooldown(player, COOLDOWN))
            return;

        double range = authority ? 28.0D : 17.0D;
        Entity directTarget = findLookTarget(player, range);
        double cost = (authority ? 100.0D : 65.0D) + (charged ? 40.0D : 0.0D);
        if (!consumeMana(player, cost)) {
            player.displayClientMessage(Component.literal("Not enough mana").withStyle(ChatFormatting.RED), true);
            CooldownManager.set(player, COOLDOWN, 10);
            return;
        }

        List<Entity> targets;
        if (directTarget != null) {
            targets = List.of(directTarget);
        } else {
            targets = findConeTargets(player, range, authority ? 8 : 4, pull ? 0.88D : 0.82D);
        }

        double strength = (authority ? 1.45D : 1.0D) * (charged ? 1.35D : 1.0D);
        for (Entity target : targets) {
            ControlProfile control = createControlProfile(player, target, authority);
            if (control.negated) {
                renderNegation(player.serverLevel(), target);
                if (target == directTarget)
                    player.displayClientMessage(Component.literal(target.getDisplayName().getString() + " negated the force").withStyle(ChatFormatting.RED), true);
                continue;
            }
            double targetStrength = strength * control.effectiveness;
            if (pull)
                pullEntity(player, target, targetStrength);
            else
                pushEntity(player, target, targetStrength);
        }

        if (authority && targets.isEmpty() && pull)
            pullNearbyItems(player, range * 0.7D, strength);

        renderPressureWave(player.serverLevel(), player, pull, charged, range);
        playTelekinesisSound(player, pull ? 1.35F : 0.95F);
        CooldownManager.set(player, "mana_refresh", 30);
        CooldownManager.set(player, COOLDOWN, charged ? (authority ? 32 : 42) : (authority ? 20 : 28));
    }

    private static void pushEntity(ServerPlayer player, Entity target, double strength) {
        if (isBoss(target)) {
            staggerBoss(target, 16);
            strength *= 0.22D;
        }
        Vec3 direction = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (direction.lengthSqr() < 0.01D)
            direction = player.getLookAngle();
        direction = direction.normalize();
        target.setDeltaMovement(direction.x * strength, Math.max(0.22D, direction.y * strength + 0.28D), direction.z * strength);
        markTelekineticLaunch(target, 120);
        markVelocityChanged(target);
        dealPressureDamage(player, target, isBoss(target) ? 0.35F : 1.0F);
    }

    private static void pullEntity(ServerPlayer player, Entity target, double strength) {
        if (isBoss(target)) {
            staggerBoss(target, 20);
            strength *= 0.18D;
        }
        Vec3 destination = player.getEyePosition().add(player.getLookAngle().scale(2.2D));
        Vec3 direction = destination.subtract(target.getBoundingBox().getCenter());
        double distanceScale = Mth.clamp(direction.length() / 7.0D, 0.75D, 1.8D);
        Vec3 motion = direction.normalize().scale(strength * distanceScale);
        target.setDeltaMovement(motion);
        markTelekineticLaunch(target, 120);
        markVelocityChanged(target);
        dealPressureDamage(player, target, isBoss(target) ? 0.25F : 0.65F);
    }

    private static void updateControlledPosition(ServerPlayer player, Entity target, ControlSession session) {
        Vec3 desiredCenter = player.getEyePosition().add(player.getLookAngle().scale(session.distance));
        Vec3 currentCenter = target.getBoundingBox().getCenter();
        Vec3 correction = desiredCenter.subtract(currentCenter);
        double maximumSpeed = session.authority ? 2.2D : 1.55D;
        Vec3 motion = correction.scale(session.authority ? 0.42D : 0.34D);
        if (motion.length() > maximumSpeed)
            motion = motion.normalize().scale(maximumSpeed);
        target.setDeltaMovement(motion);
        target.fallDistance = 0;
        if (target instanceof LivingEntity living)
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 5, false, false, false));
        if (target instanceof Mob mob)
            mob.getNavigation().stop();
        markVelocityChanged(target);
    }

    private static void throwControlled(ServerPlayer player, Entity target, ControlSession session, boolean slam) {
        discardAura(session);
        restoreGravity(target, session);
        Vec3 look = player.getLookAngle().normalize();
        double speed = session.authority ? 2.45D : 1.85D;
        Vec3 velocity = slam
                ? new Vec3(look.x * speed * 0.6D, -Math.max(1.7D, speed), look.z * speed * 0.6D)
                : look.scale(speed).add(0, 0.12D, 0);
        target.setDeltaMovement(velocity);
        markTelekineticLaunch(target, 200);
        if (target instanceof Projectile projectile)
            projectile.setOwner(player);
        markVelocityChanged(target);

        if (target instanceof LivingEntity) {
            float impactDamage = telekineticDamage(player, session.authority) * (slam ? 1.35F : 1.0F);
            THROWN.put(target.getUUID(), new ThrownState(target, player, impactDamage, slam, session.authority));
        }
        renderReleaseEffect(player.serverLevel(), target, slam);
        playTelekinesisSound(player, slam ? 0.72F : 1.05F);
        CooldownManager.set(player, "mana_refresh", 35);
        CooldownManager.set(player, COOLDOWN, session.authority ? 30 : 42);
    }

    private static void applyBossPressure(ServerPlayer player, Entity target, ControlSession session) {
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4, session.authority ? 2 : 0, false, false, false));
            if (target instanceof Mob mob)
                mob.getNavigation().stop();
        }
        target.setDeltaMovement(target.getDeltaMovement().scale(session.authority ? 0.35D : 0.7D));
        markVelocityChanged(target);
    }

    private static void finishBossPressure(ServerPlayer player, Entity target, ControlSession session) {
        discardAura(session);
        if (target instanceof LivingEntity living) {
            float damage = telekineticDamage(player, session.authority) * (session.authority ? 0.65F : 0.2F);
            living.hurt(player.damageSources().playerAttack(player), damage);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, session.authority ? 45 : 15,
                    session.authority ? 2 : 0, false, false, false));
        }
        renderReleaseEffect(player.serverLevel(), target, true);
        playTelekinesisSound(player, 0.65F);
        CooldownManager.set(player, COOLDOWN, session.authority ? 45 : 60);
    }

    private static void deflectNearbyProjectiles(ServerPlayer player) {
        List<Projectile> projectiles = player.level().getEntitiesOfClass(Projectile.class,
                player.getBoundingBox().inflate(3.0D), projectile -> projectile.isAlive() && projectile.getOwner() != player);
        for (Projectile projectile : projectiles) {
            Vec3 away = projectile.position().subtract(player.getEyePosition());
            if (away.lengthSqr() < 0.01D)
                away = player.getLookAngle();
            double speed = Math.max(1.1D, projectile.getDeltaMovement().length());
            projectile.setOwner(player);
            projectile.setDeltaMovement(away.normalize().scale(speed * 1.2D));
            markVelocityChanged(projectile);
            player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, projectile.getX(), projectile.getY(), projectile.getZ(),
                    4, 0.08D, 0.08D, 0.08D, 0.08D);
        }
    }

    private static boolean advanceBreakout(ServerPlayer controller, Entity target, ControlSession session) {
        if (!(target instanceof LivingEntity living) || session.breakoutLimitTicks <= 0)
            return false;

        double struggle = 1.0D;
        if (target instanceof ServerPlayer targetPlayer) {
            if (targetPlayer.isShiftKeyDown())
                struggle *= 2.6D;
            if (targetPlayer.isSprinting())
                struggle *= 1.25D;
        } else if (target instanceof Mob mob && mob.isAggressive()) {
            struggle *= 1.12D;
        }
        if (living.hurtTime > 0)
            struggle *= 1.35D;
        if (session.unstableControl)
            struggle *= 1.2D;

        session.breakoutProgress += struggle / session.breakoutLimitTicks;
        if (session.breakoutProgress < 1.0D)
            return false;

        discardAura(session);
        restoreGravity(target, session);
        if (!session.bossResistance) {
            Vec3 away = target.position().subtract(controller.position());
            if (away.lengthSqr() < 0.01D)
                away = controller.getLookAngle().scale(-1.0D);
            away = away.normalize().scale(0.62D);
            target.setDeltaMovement(away.x, 0.28D, away.z);
            markTelekineticLaunch(target, 50);
            markVelocityChanged(target);
        }
        renderNegation(controller.serverLevel(), target);
        playTelekinesisSound(controller, 0.56F);
        controller.displayClientMessage(Component.literal(target.getDisplayName().getString() + " broke free")
                .withStyle(ChatFormatting.RED), true);
        if (target instanceof ServerPlayer targetPlayer) {
            targetPlayer.displayClientMessage(Component.literal("You broke free from " + controller.getName().getString())
                    .withStyle(ChatFormatting.GREEN), true);
        }
        CooldownManager.set(controller, COOLDOWN, session.authority ? 42 : 55);
        SESSIONS.remove(controller.getUUID());
        return true;
    }

    private static ControlProfile createControlProfile(ServerPlayer controller, Entity target, boolean authority) {
        if (!(target instanceof LivingEntity))
            return new ControlProfile(1.0D, 1.0D, false, false, 0, 1.0D);

        double casterPower = playerPower(controller) + (authority ? 12.0D : 0.0D);
        double targetPower = entityPower(target) * healthControlFactor(target);
        double gap = targetPower - casterPower;
        int negateGap = target instanceof Player ? 26 : isBoss(target) ? 24 : 34;
        boolean negated = gap >= negateGap;
        boolean unstable = gap >= 10.0D;

        double breakoutTicks;
        if (target instanceof Player) {
            breakoutTicks = Mth.clamp(84.0D - gap * 2.0D, 18.0D, 150.0D);
        } else {
            breakoutTicks = Mth.clamp(130.0D - gap * 2.2D, 28.0D, 240.0D);
            if (isBoss(target))
                breakoutTicks = Math.min(breakoutTicks, 65.0D);
        }
        double effectiveness = Mth.clamp(1.0D - gap * 0.018D, 0.18D, 1.35D);
        return new ControlProfile(casterPower, targetPower, negated, unstable,
                (int) Math.round(breakoutTicks), effectiveness);
    }

    private static double playerPower(Player player) {
        SololevelingModVariables.PlayerVariables variables = player
                .getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .orElse(new SololevelingModVariables.PlayerVariables());
        return Math.max(1.0D, Math.max(variables.Level, rankLevel((int) Math.round(variables.HunterRank))));
    }

    private static double entityPower(Entity target) {
        if (target instanceof Player player)
            return playerPower(player);

        if (target instanceof HunterEntity hunter) {
            return switch (hunter.getEntityData().get(HunterEntity.DATA_Rank).toUpperCase()) {
                case "S" -> 110.0D;
                case "A" -> 82.0D;
                case "B" -> 58.0D;
                case "C" -> 38.0D;
                case "D" -> 22.0D;
                case "E" -> 10.0D;
                default -> estimateLivingPower(hunter);
            };
        }

        String[] levelKeys = {"sl_level", "entity_level", "shadow_level", "level", "Level"};
        for (String key : levelKeys) {
            if (target.getPersistentData().contains(key, Tag.TAG_ANY_NUMERIC)) {
                double storedLevel = target.getPersistentData().getDouble(key);
                if (storedLevel > 0.0D)
                    return storedLevel;
            }
        }

        if (target.getCustomName() != null) {
            String name = target.getCustomName().getString();
            int marker = name.lastIndexOf("Lv.");
            if (marker >= 0) {
                int start = marker + 3;
                int end = start;
                while (end < name.length() && Character.isDigit(name.charAt(end)))
                    end++;
                if (end > start) {
                    try {
                        return Double.parseDouble(name.substring(start, end));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        return target instanceof LivingEntity living ? estimateLivingPower(living) : 1.0D;
    }

    private static double estimateLivingPower(LivingEntity living) {
        AttributeInstance attack = living.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance armor = living.getAttribute(Attributes.ARMOR);
        double attackValue = attack == null ? 0.0D : attack.getValue();
        double armorValue = armor == null ? 0.0D : armor.getValue();
        double estimate = living.getMaxHealth() * 0.24D + attackValue * 1.4D + armorValue * 0.55D;
        if (isBoss(living))
            estimate += 18.0D;
        return Mth.clamp(estimate, 1.0D, 200.0D);
    }

    private static double healthControlFactor(Entity target) {
        if (!(target instanceof LivingEntity living) || living.getMaxHealth() <= 0.0F)
            return 1.0D;
        double healthRatio = Mth.clamp(living.getHealth() / living.getMaxHealth(), 0.0D, 1.0D);
        // Full-health targets keep all resistance; near-death targets retain only about half.
        return 0.52D + Math.sqrt(healthRatio) * 0.48D;
    }

    private static double rankLevel(int rank) {
        return switch (rank) {
            case 6 -> 110.0D;
            case 5 -> 82.0D;
            case 4 -> 58.0D;
            case 3 -> 38.0D;
            case 2 -> 22.0D;
            case 1 -> 10.0D;
            default -> 1.0D;
        };
    }

    private static void notifyControlledTarget(ServerPlayer controller, Entity target) {
        if (target instanceof ServerPlayer targetPlayer) {
            targetPlayer.displayClientMessage(Component.literal("Held by " + controller.getName().getString()
                    + " - hold Sneak to break free").withStyle(ChatFormatting.AQUA), true);
        }
    }

    private static void renderNegation(ServerLevel level, Entity target) {
        Vec3 center = target.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 30,
                target.getBbWidth() * 0.48D, target.getBbHeight() * 0.35D,
                target.getBbWidth() * 0.48D, 0.18D);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z, 18,
                target.getBbWidth() * 0.35D, target.getBbHeight() * 0.28D,
                target.getBbWidth() * 0.35D, 0.08D);
    }

    private static Entity findLookTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        HitResult blockHit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS)
            end = blockHit.getLocation();

        AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.2D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, search,
                candidate -> isValidTarget(player, candidate), start.distanceToSqr(end));
        return hit == null ? null : hit.getEntity();
    }

    private static List<Entity> findConeTargets(ServerPlayer player, double range, int maximum, double minimumDot) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        List<Entity> targets = new ArrayList<>();
        for (Entity candidate : player.level().getEntities(player, player.getBoundingBox().inflate(range),
                entity -> isValidTarget(player, entity))) {
            Vec3 direction = candidate.getBoundingBox().getCenter().subtract(eye);
            if (direction.lengthSqr() > range * range || direction.lengthSqr() < 0.01D)
                continue;
            if (look.dot(direction.normalize()) < minimumDot || !player.hasLineOfSight(candidate))
                continue;
            targets.add(candidate);
        }
        targets.sort(Comparator.comparingDouble(player::distanceToSqr));
        return targets.size() > maximum ? new ArrayList<>(targets.subList(0, maximum)) : targets;
    }

    private static boolean isValidTarget(ServerPlayer player, Entity candidate) {
        if (candidate == null || candidate == player || !candidate.isAlive() || candidate.isSpectator()
                || candidate.isPassengerOfSameVehicle(player) || candidate == player.getVehicle())
            return false;
        if (!(candidate instanceof LivingEntity || candidate instanceof ItemEntity || candidate instanceof Projectile))
            return false;
        if (candidate instanceof TamableAnimal tame && tame.isOwnedBy(player))
            return false;
        if (candidate instanceof Projectile projectile && projectile.getOwner() == player)
            return false;
        if (candidate instanceof Player other && sameParty(player, other))
            return false;
        return true;
    }

    private static boolean sameParty(ServerPlayer player, Player other) {
        String playerParty = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .map(data -> data.party).orElse("");
        String otherParty = other.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .map(data -> data.party).orElse("");
        return !playerParty.isBlank() && playerParty.equals(otherParty);
    }

    private static boolean isBoss(Entity entity) {
        return entity instanceof LivingEntity
                && (entity.getType().is(BOSS_TAG) || StealthBossDetectionHelper.seesThroughStealth(entity));
    }

    private static double entityWeight(Entity entity) {
        double volume = Math.max(0.25D, entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight());
        double health = entity instanceof LivingEntity living ? living.getMaxHealth() / 45.0D : 0.5D;
        return Mth.clamp(0.5D + volume * 0.35D + health, 0.75D, 10.0D);
    }

    private static void dealPressureDamage(ServerPlayer player, Entity target, float multiplier) {
        if (target instanceof LivingEntity living) {
            living.hurt(player.damageSources().playerAttack(player), telekineticDamage(player, hasAuthority(player)) * 0.35F * multiplier);
        }
    }

    private static float telekineticDamage(ServerPlayer player, boolean authority) {
        double intelligence = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .map(data -> data.Intelligence).orElse(0.0D);
        return (float) ((authority ? 7.0D : 4.0D) + Math.min(authority ? 24.0D : 12.0D,
                intelligence * (authority ? 0.065D : 0.04D)));
    }

    private static Entity findThrowCollision(ServerPlayer owner, Entity thrown) {
        AABB area = thrown.getBoundingBox().inflate(0.45D);
        List<LivingEntity> hits = thrown.level().getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate != thrown && candidate != owner && isValidTarget(owner, candidate));
        return hits.isEmpty() ? null : hits.get(0);
    }

    private static void resolveImpact(ServerPlayer owner, Entity thrown, Entity collision, ThrownState state) {
        Vec3 impact = thrown.getBoundingBox().getCenter();
        if (thrown instanceof LivingEntity living)
            living.hurt(owner.damageSources().playerAttack(owner), state.damage);
        if (collision instanceof LivingEntity living)
            living.hurt(owner.damageSources().playerAttack(owner), state.damage);

        double radius = state.authority ? 3.0D : 2.0D;
        for (LivingEntity nearby : thrown.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(impact, impact).inflate(radius), entity -> entity != thrown && entity != collision
                        && entity != owner && isValidTarget(owner, entity))) {
            nearby.hurt(owner.damageSources().playerAttack(owner), state.damage * 0.55F);
            Vec3 knockback = nearby.position().subtract(impact);
            if (knockback.lengthSqr() > 0.01D) {
                knockback = knockback.normalize().scale(state.authority ? 0.8D : 0.5D);
                nearby.setDeltaMovement(nearby.getDeltaMovement().add(knockback.x, 0.25D, knockback.z));
                markVelocityChanged(nearby);
            }
        }

        if (thrown.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, state.slammed ? 3 : 1,
                    0.25D, 0.2D, 0.25D, 0.02D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y, impact.z, state.authority ? 24 : 14,
                    radius * 0.45D, 0.35D, radius * 0.45D, 0.18D);
        }
        playTelekinesisSound(owner, state.slammed ? 0.58F : 0.78F);
    }

    private static void pullNearbyItems(ServerPlayer player, double range, double strength) {
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(range),
                Entity::isAlive)) {
            Vec3 direction = player.getEyePosition().subtract(item.position());
            if (direction.lengthSqr() > 0.01D) {
                item.setDeltaMovement(direction.normalize().scale(strength * 1.2D));
                item.hasImpulse = true;
            }
        }
    }

    private static void staggerBoss(Entity target, int duration) {
        if (target instanceof LivingEntity living)
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1, false, false, false));
    }

    private static void renderControlEffect(ServerLevel level, ServerPlayer player, Entity target, ControlSession session) {
        if (player.tickCount % 2 != 0)
            return;
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.45D));
        Vec3 end = target.getBoundingBox().getCenter();
        Vec3 span = end.subtract(start);
        int points = Math.max(3, Math.min(9, (int) (span.length() / 2.0D)));
        for (int i = 1; i <= points; i++) {
            Vec3 point = start.add(span.scale(i / (double) (points + 1)));
            level.sendParticles(i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK,
                    point.x, point.y, point.z, 1, 0.015D, 0.015D, 0.015D, 0.0D);
        }
        level.sendParticles(session.bossResistance ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.ELECTRIC_SPARK,
                end.x, end.y, end.z, session.authority ? 3 : 1,
                target.getBbWidth() * 0.25D, target.getBbHeight() * 0.2D, target.getBbWidth() * 0.25D, 0.03D);
    }

    private static void renderPressureWave(ServerLevel level, ServerPlayer player, boolean pull, boolean charged, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 0.01D)
            side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 up = side.cross(look).normalize();
        int rings = charged ? 5 : 3;
        for (int ring = 1; ring <= rings; ring++) {
            double distance = Math.min(range, ring * (charged ? 3.0D : 2.4D));
            double radius = distance * 0.18D;
            for (int point = 0; point < 10; point++) {
                double angle = Math.PI * 2.0D * point / 10.0D;
                Vec3 offset = side.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
                Vec3 position = eye.add(look.scale(distance)).add(offset);
                level.sendParticles(pull ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.ELECTRIC_SPARK,
                        position.x, position.y, position.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void renderReleaseEffect(ServerLevel level, Entity target, boolean slam) {
        Vec3 center = target.getBoundingBox().getCenter();
        level.sendParticles(slam ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z, 16, target.getBbWidth() * 0.3D,
                target.getBbHeight() * 0.25D, target.getBbWidth() * 0.3D, 0.12D);
    }

    private static void playTelekinesisSound(ServerPlayer player, float pitch) {
        player.level().playSound(null, player.blockPosition(), SololevelingModSounds.TELEPUSH.get(),
                SoundSource.PLAYERS, 0.72F, pitch);
    }

    private static boolean consumeMana(ServerPlayer player, double amount) {
        if (player.isCreative())
            return true;
        SololevelingModVariables.PlayerVariables variables = player
                .getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
        if (variables == null || variables.MP < amount)
            return false;
        variables.MP = Math.max(0, variables.MP - amount);
        variables.syncPlayerVariables(player);
        return true;
    }

    private static boolean canUse(ServerPlayer player) {
        if (player == null || !hasAbility(player))
            return false;
        return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .map(data -> data.combatmode).orElse(false);
    }

    private static Entity getControlledEntity(ServerPlayer player, ControlSession session) {
        if (session == null || session.controlledId == null)
            return null;
        if (session.controlledEntity != null && !session.controlledEntity.isRemoved())
            return session.controlledEntity;
        if (player.level() instanceof ServerLevel level)
            session.controlledEntity = level.getEntity(session.controlledId);
        return session.controlledEntity;
    }

    private static void cancelSession(ServerPlayer player, ControlSession session) {
        discardAura(session);
        restoreGravity(getControlledEntity(player, session), session);
        SESSIONS.remove(player.getUUID());
        if (session.controlledId != null)
            CooldownManager.set(player, COOLDOWN, 20);
    }

    private static void restoreGravity(Entity target, ControlSession session) {
        if (target != null && !session.bossResistance)
            target.setNoGravity(session.originalNoGravity);
    }

    private static void discardAura(ControlSession session) {
        if (session != null && session.auraEntity != null && !session.auraEntity.isRemoved())
            session.auraEntity.discard();
        if (session != null)
            session.auraEntity = null;
    }

    private static void markVelocityChanged(Entity target) {
        target.hasImpulse = true;
        if (target instanceof ServerPlayer serverPlayer)
            serverPlayer.hurtMarked = true;
    }

    private static void markTelekineticLaunch(Entity target, int durationTicks) {
        if (target instanceof LivingEntity) {
            long expiry = target.level().getGameTime() + Math.max(1, durationTicks);
            target.getPersistentData().putLong(LAUNCH_PROTECTION, expiry);
        }
    }

    private static boolean containsAbility(String abilities, String id) {
        if (abilities == null || abilities.isBlank())
            return false;
        for (String token : abilities.replace('"', ' ').trim().split("\\s+")) {
            if (id.equalsIgnoreCase(token))
                return true;
        }
        return false;
    }

    private static final class ControlSession {
        private final long startedAt;
        private final boolean sneakingAtStart;
        private final boolean authority;
        private double distance;
        private UUID controlledId;
        private Entity controlledEntity;
        private RulersAuthorityAuraEntity auraEntity;
        private boolean bossResistance;
        private boolean originalNoGravity;
        private boolean unstableControl;
        private double weight = 1.0D;
        private double casterPower;
        private double targetPower;
        private double breakoutProgress;
        private int breakoutLimitTicks;
        private int drainTicker;

        private ControlSession(long startedAt, boolean sneakingAtStart, boolean authority, double distance) {
            this.startedAt = startedAt;
            this.sneakingAtStart = sneakingAtStart;
            this.authority = authority;
            this.distance = distance;
        }
    }

    private static final class ControlProfile {
        private final double casterPower;
        private final double targetPower;
        private final boolean negated;
        private final boolean unstable;
        private final int breakoutTicks;
        private final double effectiveness;

        private ControlProfile(double casterPower, double targetPower, boolean negated, boolean unstable,
                int breakoutTicks, double effectiveness) {
            this.casterPower = casterPower;
            this.targetPower = targetPower;
            this.negated = negated;
            this.unstable = unstable;
            this.breakoutTicks = breakoutTicks;
            this.effectiveness = effectiveness;
        }
    }

    private static final class ThrownState {
        private final Entity target;
        private final ServerPlayer owner;
        private final float damage;
        private final boolean slammed;
        private final boolean authority;
        private int ticks;

        private ThrownState(Entity target, ServerPlayer owner, float damage, boolean slammed, boolean authority) {
            this.target = target;
            this.owner = owner;
            this.damage = damage;
            this.slammed = slammed;
            this.authority = authority;
        }
    }
}
