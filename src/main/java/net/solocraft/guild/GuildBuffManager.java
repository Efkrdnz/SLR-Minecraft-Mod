package net.solocraft.guild;

import net.solocraft.entity.ManaArrowEntity;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.util.TemporaryStatBonusManager;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.UUID;

@EventBusSubscriber
public final class GuildBuffManager {
    private static final ResourceLocation FOOTWORK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath("sololeveling",
                    "attribute/guild_assassin_footwork");
    private static final String COMBAT_UNTIL = "sl_guild_combat_until";
    private static final String DEFENSE_UNTIL = "sl_guild_defense_until";
    private static final String BATTLE_UNTIL = "sl_guild_battle_until";
    private static final String LAST_MP = "sl_guild_last_mp";

    private GuildBuffManager() {
    }

    public static boolean hasActive(Entity entity, int buffId) {
        GuildData guild = guildFor(entity);
        return guild != null && (guild.activeBuffSlot1 == buffId || guild.activeBuffSlot2 == buffId);
    }

    public static double xpMultiplier(Entity entity) {
        return hasActive(entity, GuildBuffRegistry.SYSTEM_SPONSORSHIP) ? 1.15D : 1.0D;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0 || event.getEntity().level().isClientSide()) return;

        ServerPlayer attacker = owningPlayer(event.getSource().getEntity());
        if (attacker != null) {
            markCombat(attacker, 160);
            boolean magic = isMagicDamage(event.getSource());
            boolean skill = isSkillDamage(event.getSource());
            float multiplier = 1.0F;
            if (!magic && hasActive(attacker, GuildBuffRegistry.PHYSICAL_CONDITIONING)) multiplier *= 1.15F;
            if (magic && hasActive(attacker, GuildBuffRegistry.ARCANE_CIRCULATION)) multiplier *= 1.10F;
            if (hasActive(attacker, GuildBuffRegistry.HUNTERS_PRECISION) && isPrecisionHit(attacker)) multiplier *= 1.15F;
            if (hasActive(attacker, GuildBuffRegistry.BATTLE_RHYTHM)) {
                long now = attacker.level().getGameTime();
                if (skill || attacker.getPersistentData().getLong(BATTLE_UNTIL) > now) {
                    multiplier *= 1.12F;
                    attacker.getPersistentData().putLong(BATTLE_UNTIL, now + 80);
                }
            }
            event.setAmount(event.getAmount() * multiplier);
        }

        if (event.getEntity() instanceof ServerPlayer victim) {
            if (hasActive(victim, GuildBuffRegistry.ASSASSIN_FOOTWORK) && victim.getRandom().nextFloat() < 0.10F) {
                event.setCanceled(true);
                markCombat(victim, 160);
                return;
            }
            float multiplier = 1.0F;
            if (hasActive(victim, GuildBuffRegistry.DEFENSIVE_FORMATION)) {
                if (victim.isBlocking()) multiplier *= 0.85F;
                if (victim.getPersistentData().getLong(DEFENSE_UNTIL) > victim.level().getGameTime()) multiplier *= 0.92F;
                victim.getPersistentData().putLong(DEFENSE_UNTIL, victim.level().getGameTime() + 40);
            }
            markCombat(victim, 160);
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && hasActive(player, GuildBuffRegistry.COMBAT_RECOVERY)) {
            event.setAmount(event.getAmount() * 1.20F);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        ServerPlayer player = owningPlayer(event.getSource().getEntity());
        if (player == null || !hasActive(player, GuildBuffRegistry.COMBAT_RECOVERY)) return;
        player.heal(Math.max(1.0F, player.getMaxHealth() * 0.03F));
        player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
            vars.MP = Math.min(vars.Mana, vars.MP + Math.max(5.0D, vars.Mana * 0.05D));
            vars.syncPlayerVariables(player);
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (false || !(event.getEntity() instanceof ServerPlayer player)) return;
        applyFootworkSpeed(player);
        tickManaBuffs(player);
    }

    private static void tickManaBuffs(ServerPlayer player) {
        player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
            double beforeMp = vars.MP;
            double lastMp = player.getPersistentData().contains(LAST_MP) ? player.getPersistentData().getDouble(LAST_MP) : vars.MP;
            if (hasActive(player, GuildBuffRegistry.MANA_EFFICIENCY) && vars.MP < lastMp) {
                double spent = lastMp - vars.MP;
                vars.MP = Math.min(vars.Mana, vars.MP + spent * 0.25D);
            }
            if (hasActive(player, GuildBuffRegistry.ARCANE_CIRCULATION) && vars.MP < vars.Mana && player.tickCount % 20 == 0) {
                double base = Math.max(1.0D,
                        ((TemporaryStatBonusManager.effectiveIntelligence(player) / 20.0D) * 2.0D)
                                + vars.manaregen);
                vars.MP = Math.min(vars.Mana, vars.MP + base * 0.10D);
            }
            if (Double.compare(vars.MP, beforeMp) != 0)
                vars.syncPlayerVariables(player);
            player.getPersistentData().putDouble(LAST_MP, vars.MP);
        });
    }

    private static void applyFootworkSpeed(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        AttributeModifier old = speed.getModifier(FOOTWORK_SPEED_ID);
        boolean desired = hasActive(player, GuildBuffRegistry.ASSASSIN_FOOTWORK)
                && player.getPersistentData().getLong(COMBAT_UNTIL) > player.level().getGameTime();
        boolean matches = old != null
                && Double.compare(old.amount(), 0.12D) == 0
                && old.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        if (!desired) {
            if (old != null)
                speed.removeModifier(FOOTWORK_SPEED_ID);
            return;
        }
        if (matches)
            return;
        if (old != null)
            speed.removeModifier(FOOTWORK_SPEED_ID);
        speed.addTransientModifier(new AttributeModifier(FOOTWORK_SPEED_ID,
                0.12D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void markCombat(ServerPlayer player, int ticks) {
        player.getPersistentData().putLong(COMBAT_UNTIL, player.level().getGameTime() + ticks);
    }

    private static boolean isPrecisionHit(ServerPlayer player) {
        boolean vanillaCrit = player.fallDistance > 0.0F && !player.onGround() && !player.isInWater();
        return vanillaCrit || player.getRandom().nextFloat() < 0.05F;
    }

    private static boolean isMagicDamage(DamageSource source) {
        return source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.LIGHTNING_BOLT)
                || source.is(DamageTypes.IN_FIRE)
                || source.is(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("sololeveling:mage")));
    }

    private static boolean isSkillDamage(DamageSource source) {
        if (isMagicDamage(source)) return true;
        return source.is(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("sololeveling:assassin")))
                || source.is(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("sololeveling:fighter")))
                || source.is(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("sololeveling:ranger")))
                || source.getDirectEntity() instanceof ManaArrowEntity;
    }

    private static GuildData guildFor(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return null;
        return GuildSavedData.get(player.serverLevel()).getGuildForPlayer(player.getUUID());
    }

    private static ServerPlayer owningPlayer(Entity source) {
        if (source instanceof ServerPlayer player) return player;
        if (source instanceof TamableAnimal tame && tame.getOwner() instanceof ServerPlayer owner) return owner;
        if (source instanceof Projectile projectile && projectile.getOwner() != null) return owningPlayer(projectile.getOwner());
        if (source != null) {
            UUID ownerId = ShadowMonarchManager.getShadowOwnerUUID(source);
            if (ownerId != null && source.getServer() != null)
                return source.getServer().getPlayerList().getPlayer(ownerId);
        }
        return null;
    }
}
