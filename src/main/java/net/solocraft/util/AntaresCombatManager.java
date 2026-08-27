package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.AntaresVfxEventMessage;
import net.solocraft.network.SololevelingModVariables;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative combat runtime for Antares, Monarch of Destruction. */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class AntaresCombatManager {
	public static final int JOB_ID = 10;
	public static final String IDENTITY = "antares";
	public static final String DESTRUCTION_CLAW = "Destruction Claw";
	public static final String BREATH_OF_DESTRUCTION = "Breath of Destruction";
	public static final String MONARCHS_DESCENT = "Monarch's Descent";
	public static final String SOVEREIGN_ROAR = "Sovereign Roar";
	public static final String EXTINCTION = "Extinction";
	public static final String MONARCH_MANIFESTATION = "Monarch Manifestation";

	public static final String CLAW_COOLDOWN = "antares_destruction_claw";
	public static final String BREATH_COOLDOWN = "antares_breath_of_destruction";
	public static final String DESCENT_COOLDOWN = "antares_monarchs_descent";
	public static final String ROAR_COOLDOWN = "antares_sovereign_roar";
	public static final String EXTINCTION_COOLDOWN = "antares_extinction";
	public static final String MANIFESTATION_COOLDOWN = "antares_manifestation";
	public static final String MANIFESTATION_AURA = "antares_manifestation";

	private static final int RUIN_DECAY_DELAY = 240;
	private static final int RUIN_DECAY_INTERVAL = 100;
	private static final int BREATH_WINDUP = 8;
	private static final int EXTINCTION_WINDUP = 20;
	/** Long-range beams deliberately ignore terrain for combat targeting. */
	public static final double BREATH_RANGE = 144.0D;
	public static final double MANIFESTED_BREATH_RANGE = 176.0D;
	public static final double EXTINCTION_RANGE = 288.0D;
	public static final double MANIFESTED_EXTINCTION_RANGE = 352.0D;
	private static final int BREATH_TARGET_CAP = 32;
	private static final int EXTINCTION_TARGET_CAP = 48;
	private static final String FALL_SAFE_UNTIL = "antares_fall_safe_until";
	private static final ResourceLocation MANIFEST_SPEED =
			ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
					"attribute/antares_manifestation_speed");
	private static final ResourceLocation MANIFEST_KNOCKBACK =
			ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
					"attribute/antares_manifestation_stability");

	private static final Map<UUID, RuinState> RUIN = new HashMap<>();
	private static final Map<UUID, BreathState> BREATHS = new HashMap<>();
	private static final Map<UUID, DescentState> DESCENTS = new HashMap<>();
	private static final Map<UUID, RoarState> ROARS = new HashMap<>();
	private static final Map<UUID, ExtinctionState> EXTINCTIONS = new HashMap<>();
	private static final Map<UUID, ManifestationState> MANIFESTATIONS = new HashMap<>();

	private AntaresCombatManager() {
	}

	public static boolean isAntaresVessel(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables data = variables(entity);
		return (int) data.JOB == JOB_ID
				&& (IDENTITY.equals(data.vesselIdentity) || data.vesselIdentity.isBlank());
	}

	public static boolean isManifested(Entity entity) {
		return entity != null && MANIFESTATIONS.containsKey(entity.getUUID());
	}

	public static int ruin(Entity entity) {
		RuinState state = entity == null ? null : RUIN.get(entity.getUUID());
		return state == null ? 0 : state.charges;
	}

	public static void castDestructionClaw(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !canCast(player)
				|| !ready(player, CLAW_COOLDOWN, DESTRUCTION_CLAW))
			return;
		RuinState ruin = ruinState(player);
		boolean finisher = player.isShiftKeyDown()
				&& AntaresCombatRules.canSpendFullRuin(ruin.charges);
		boolean manifested = isManifested(player);
		int baseMana = finisher ? 260 : 140;
		int mana = VesselManaScaling.strengthScaledCost(player, baseMana,
				finisher ? 0.32D : 0.22D);
		if (!consumeMana(player, mana))
			return;

		Vec3 origin = player.getEyePosition().add(player.getLookAngle().normalize().scale(0.35D));
		Vec3 forward = player.getLookAngle().normalize();
		double reach = finisher ? (manifested ? 8.0D : 7.0D)
				: (manifested ? 6.2D : 5.2D);
		double halfAngle = finisher ? 0.18D : 0.34D;
		double strength = TemporaryStatBonusManager.effectiveStrength(player);
		double intelligence = TemporaryStatBonusManager.effectiveIntelligence(player);
		float damage = (float) (9.0D + strength / 9.0D + intelligence / 28.0D);
		damage *= finisher ? 2.15F : manifested ? 1.18F : 1.0F;

		List<LivingEntity> targets = targetsInCone(player, origin, forward, reach,
				halfAngle, finisher ? 3.0D : 2.1D);
		int hits = 0;
		for (LivingEntity target : targets) {
			float applied = target instanceof Player
					? AntaresCombatRules.playerDamage(damage) : damage;
			if (!dealDestruction(player, target, applied))
				continue;
			target.igniteForSeconds(finisher ? 4 : 2);
			if (finisher) {
				Vec3 away = horizontal(target.position().subtract(player.position()));
				if (away.lengthSqr() > 0.001D) {
					double control = AntaresCombatRules.bossControlScale(isBoss(target));
					target.setDeltaMovement(target.getDeltaMovement()
							.add(away.normalize().scale(0.65D * control)).add(0.0D, 0.16D * control, 0.0D));
					target.hurtMarked = true;
				}
			}
			hits++;
		}

		if (finisher)
			setRuin(player, 0, true);
		else if (hits > 0)
			gainRuin(player, 1);
		CooldownManager.set(player, CLAW_COOLDOWN,
				finisher ? 120 : manifested ? 58 : 72);
		player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
		Vec3 focus = origin.add(forward.scale(reach));
		AbilityDestructionManager.line(player,
				AbilityDestructionManager.Profile.ANTARES_CLAW, origin, focus,
				strength + intelligence * 0.32D, manifested || finisher);
		if (finisher)
			AbilityDestructionManager.impact(player,
					AbilityDestructionManager.Profile.ANTARES_CLAW_FINISH, focus,
					strength + intelligence * 0.32D, true);
		AntaresVfxEventMessage.sendClaw(player, origin, focus,
				finisher, hits > 0, visualSeed(player, 17));
		player.level().playSound(null, player.blockPosition(),
				finisher ? SoundEvents.TRIDENT_THUNDER.value() : SoundEvents.PLAYER_ATTACK_SWEEP,
				SoundSource.PLAYERS, finisher ? 1.05F : 0.82F,
				finisher ? 0.58F : 0.72F);
	}

	public static void castBreathOfDestruction(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !canCast(player)
				|| !ready(player, BREATH_COOLDOWN, BREATH_OF_DESTRUCTION))
			return;
		boolean manifested = isManifested(player);
		int mana = VesselManaScaling.strengthScaledCost(player,
				manifested ? 350 : 320, 0.28D);
		if (!consumeMana(player, mana))
			return;
		long now = player.level().getGameTime();
		int seed = visualSeed(player, 31);
		BREATHS.put(player.getUUID(), new BreathState(now,
				now + (manifested ? 42L : 34L), now + BREATH_WINDUP,
				seed));
		CooldownManager.set(player, BREATH_COOLDOWN, manifested ? 135 : 160);
		Vec3[] beam = piercingBeam(player,
				manifested ? MANIFESTED_BREATH_RANGE : BREATH_RANGE);
		AntaresVfxEventMessage.sendBreathCharge(player, beam[0], beam[1],
				BREATH_WINDUP, manifested, seed);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
				SoundSource.PLAYERS, 0.72F, manifested ? 0.66F : 0.74F);
	}

	public static void castMonarchsDescent(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !canCast(player)
				|| !ready(player, DESCENT_COOLDOWN, MONARCHS_DESCENT))
			return;
		boolean manifested = isManifested(player);
		int mana = VesselManaScaling.strengthScaledCost(player,
				manifested ? 310 : 280, 0.25D);
		if (!consumeMana(player, mana))
			return;
		long now = player.level().getGameTime();
		Vec3 look = player.getLookAngle().normalize();
		Vec3 horizontal = horizontal(look);
		if (horizontal.lengthSqr() < 0.001D)
			horizontal = horizontalLook(player);
		horizontal = horizontal.normalize();
		boolean diving = !player.onGround() && look.y < -0.18D;
		Vec3 launch = diving
				? new Vec3(horizontal.x * 1.35D, Math.min(-0.48D, look.y * 1.1D), horizontal.z * 1.35D)
				: new Vec3(horizontal.x * (manifested ? 1.4D : 1.2D),
						manifested ? 0.96D : 0.82D,
						horizontal.z * (manifested ? 1.4D : 1.2D));
		int seed = visualSeed(player, 43);
		DESCENTS.put(player.getUUID(), new DescentState(now, now + 42L,
				horizontal, seed, manifested));
		CooldownManager.set(player, DESCENT_COOLDOWN, manifested ? 145 : 180);
		player.getPersistentData().putLong(FALL_SAFE_UNTIL, now + 85L);
		player.fallDistance = 0.0F;
		player.setDeltaMovement(launch);
		player.hurtMarked = true;
		AntaresVfxEventMessage.sendDescentLaunch(player, manifested, seed);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ELYTRA_FLYING,
				SoundSource.PLAYERS, 0.92F, manifested ? 0.62F : 0.72F);
	}

	public static void castSovereignRoar(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !canCast(player)
				|| !ready(player, ROAR_COOLDOWN, SOVEREIGN_ROAR))
			return;
		boolean manifested = isManifested(player);
		int mana = VesselManaScaling.strengthScaledCost(player,
				manifested ? 455 : 420, 0.3D);
		if (!consumeMana(player, mana))
			return;
		long now = player.level().getGameTime();
		int seed = visualSeed(player, 59);
		ROARS.put(player.getUUID(), new RoarState(now + 6L, seed, manifested));
		CooldownManager.set(player, ROAR_COOLDOWN, manifested ? 225 : 280);
		AntaresVfxEventMessage.sendRoarCharge(player, manifested, seed);
		player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_ANGRY,
				SoundSource.PLAYERS, 0.66F, 0.7F);
	}

	public static void castExtinction(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !canCast(player)
				|| !ready(player, EXTINCTION_COOLDOWN, EXTINCTION))
			return;
		RuinState ruin = ruinState(player);
		if (!AntaresCombatRules.canSpendFullRuin(ruin.charges)) {
			fail(player, "Extinction requires full Ruin.");
			return;
		}
		boolean manifested = isManifested(player);
		int mana = VesselManaScaling.strengthScaledCost(player,
				manifested ? 980 : 900, 0.36D);
		if (!consumeMana(player, mana))
			return;
		double range = manifested ? MANIFESTED_EXTINCTION_RANGE : EXTINCTION_RANGE;
		Vec3 direction = player.getLookAngle().normalize();
		Vec3[] beam = piercingBeam(player, range);
		long now = player.level().getGameTime();
		int seed = visualSeed(player, 73);
		EXTINCTIONS.put(player.getUUID(), new ExtinctionState(beam[0], beam[1],
				direction, range, now, now + EXTINCTION_WINDUP, seed, manifested));
		setRuin(player, 0, true);
		CooldownManager.setFullDuration(player, EXTINCTION_COOLDOWN,
				manifested ? 520 : 600);
		AntaresVfxEventMessage.sendExtinctionCharge(player, beam[0], beam[1],
				manifested, seed);
		player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
				SoundSource.PLAYERS, 1.15F, 0.52F);
		player.displayClientMessage(Component.literal("EXTINCTION - AIM LOCKED")
				.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
	}

	public static void toggleManifestation(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !player.isAlive()
				|| !isAntaresVessel(player))
			return;
		if (isManifested(player)) {
			endManifestation(player, false, true);
			return;
		}
		if (isActionLocked(player)
				|| !ready(player, MANIFESTATION_COOLDOWN, MONARCH_MANIFESTATION))
			return;
		int mana = VesselManaScaling.strengthScaledCost(player, 800, 0.12D);
		if (!consumeMana(player, mana))
			return;
		long now = player.level().getGameTime();
		int seed = visualSeed(player, 89);
		MANIFESTATIONS.put(player.getUUID(), new ManifestationState(now + 20L, seed));
		applyManifestationAttributes(player);
		PlayerAuraSystem.setContinuous(player, MANIFESTATION_AURA, 1.45F);
		PlayerAuraSystem.burst(player, MANIFESTATION_AURA, 28, 1.9F);
		AntaresVfxEventMessage.sendManifestation(player, true, seed);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
				SoundSource.PLAYERS, 0.9F, 0.48F);
		player.displayClientMessage(Component.literal("MONARCH MANIFESTATION - ACTIVE")
				.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (false || event.getEntity().level().isClientSide()
				|| !(event.getEntity() instanceof ServerPlayer player))
			return;
		if (!isAntaresVessel(player)) {
			if (hasRuntime(player))
				clearPlayer(player, true);
			return;
		}
		long now = player.level().getGameTime();
		updateRuin(player, now);
		updateBreath(player, now);
		updateDescent(player, now);
		updateRoar(player, now);
		updateExtinction(player, now);
		updateManifestation(player, now);
		if (player.tickCount % 40 == Math.floorMod(player.getId(), 40))
			syncRuin(player, false);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onAttackEntity(AttackEntityEvent event) {
		if (isActionLocked(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		if (isActionLocked(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		if (isActionLocked(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (isActionLocked(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (isActionLocked(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamage(LivingDamageEvent.Pre event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || !isManifested(player))
			return;
		if (event.getSource().is(DamageTypeTags.IS_FIRE))
			event.setNewDamage(event.getNewDamage() * 0.55F);
		else if (event.getSource().is(DamageTypeTags.IS_EXPLOSION))
			event.setNewDamage(event.getNewDamage() * 0.78F);
	}

	@SubscribeEvent
	public static void onFall(LivingFallEvent event) {
		if (event.getEntity() instanceof Player player
				&& player.getPersistentData().getLong(FALL_SAFE_UNTIL)
						>= player.level().getGameTime()) {
			event.setDamageMultiplier(0.0F);
			player.fallDistance = 0.0F;
		}
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			clearPlayer(player, true);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			clearPlayer(player, true);
	}

	@SubscribeEvent
	public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			clearPlayer(player, true);
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && isAntaresVessel(player))
			syncRuin(player, true);
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		RUIN.clear();
		BREATHS.clear();
		DESCENTS.clear();
		ROARS.clear();
		EXTINCTIONS.clear();
		MANIFESTATIONS.clear();
	}

	public static void resetPlayerState(ServerPlayer player) {
		if (player != null)
			clearPlayer(player, true);
	}

	private static void updateRuin(ServerPlayer player, long now) {
		RuinState state = ruinState(player);
		if (state.charges <= 0 || isManifested(player) || now - state.lastCombat < RUIN_DECAY_DELAY)
			return;
		if (now >= state.nextDecay) {
			state.charges = AntaresCombatRules.clampRuin(state.charges - 1);
			state.nextDecay = now + RUIN_DECAY_INTERVAL;
			syncRuin(player, true);
		}
	}

	private static void updateBreath(ServerPlayer player, long now) {
		BreathState state = BREATHS.get(player.getUUID());
		if (state == null)
			return;
		if (!player.isAlive() || now >= state.endTick) {
			finishBreath(player, state);
			return;
		}
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x * 0.58D, motion.y, motion.z * 0.58D);
		if (now < state.nextPulse)
			return;
		state.nextPulse += 4L;
		boolean manifested = isManifested(player);
		double beamRange = manifested ? MANIFESTED_BREATH_RANGE : BREATH_RANGE;
		Vec3[] beam = piercingBeam(player, beamRange);
		double radius = manifested ? 1.8D : 1.38D;
		double strength = TemporaryStatBonusManager.effectiveStrength(player);
		double intelligence = TemporaryStatBonusManager.effectiveIntelligence(player);
		float damage = (float) ((3.8D + strength / 38.0D + intelligence / 21.0D)
				* (manifested ? 1.16D : 1.0D));
		int hits = 0;
		for (LivingEntity target : targetsAlongSegment(player, beam[0], beam[1], radius,
				BREATH_TARGET_CAP)) {
			float applied = target instanceof Player
					? AntaresCombatRules.playerDamage(damage) : damage;
			if (dealDestruction(player, target, applied)) {
				target.igniteForSeconds(manifested ? 4 : 3);
				hits++;
			}
		}
		if (hits > 0 && !state.ruinAwarded) {
			state.ruinAwarded = true;
			gainRuin(player, 1);
		}
		BlockHitResult terrainHit = terrainHit(player, beam[0], beam[1]);
		if (terrainHit.getType() == HitResult.Type.BLOCK) {
			Vec3 direction = beam[1].subtract(beam[0]).normalize();
			Vec3 terrain = terrainHit.getLocation();
			Vec3 drillStart = terrain.subtract(direction.scale(manifested ? 2.6D : 2.0D));
			AbilityDestructionManager.line(player,
					AbilityDestructionManager.Profile.ANTARES_BREATH,
					drillStart, terrain.add(direction.scale(manifested ? 64.0D : 48.0D)),
					intelligence + strength * 0.55D, manifested);
		}
		AntaresVfxEventMessage.sendBreathStream(player, beam[0], beam[1],
				manifested, hits > 0, state.seed + state.pulse++ * 31);
		player.level().playSound(null, BlockPos.containing(beam[1]), SoundEvents.FIRECHARGE_USE,
				SoundSource.PLAYERS, 0.42F, 0.46F + state.pulse * 0.025F);
	}

	private static void finishBreath(ServerPlayer player, BreathState state) {
		BREATHS.remove(player.getUUID());
		Vec3[] beam = piercingBeam(player, isManifested(player)
				? MANIFESTED_BREATH_RANGE : BREATH_RANGE);
		AntaresVfxEventMessage.sendBreathEnd(player, beam[0], beam[1], state.seed + 101);
	}

	private static void updateDescent(ServerPlayer player, long now) {
		DescentState state = DESCENTS.get(player.getUUID());
		if (state == null)
			return;
		long elapsed = now - state.startTick;
		player.fallDistance = 0.0F;
		player.getPersistentData().putLong(FALL_SAFE_UNTIL, now + 45L);
		if (elapsed <= 16L && !player.horizontalCollision) {
			Vec3 desired = horizontalLook(player);
			Vec3 blended = state.direction.scale(0.76D).add(desired.scale(0.24D));
			if (blended.lengthSqr() > 0.001D)
				state.direction = blended.normalize();
			Vec3 movement = player.getDeltaMovement();
			double targetSpeed = state.manifested ? 1.32D : 1.12D;
			Vec3 horizontalMotion = state.direction.scale(targetSpeed);
			player.setDeltaMovement(horizontalMotion.x,
					Math.max(-1.35D, movement.y - (elapsed > 8L ? 0.06D : 0.0D)),
					horizontalMotion.z);
			player.hurtMarked = true;
		}
		boolean contactedTerrain = elapsed >= 5L
				&& (player.onGround() || player.horizontalCollision);
		if (contactedTerrain || now >= state.endTick)
			finishDescent(player, state, contactedTerrain);
	}

	private static void finishDescent(ServerPlayer player, DescentState state,
			boolean contactedTerrain) {
		DESCENTS.remove(player.getUUID());
		Vec3 center = player.position().add(0.0D, 0.12D, 0.0D);
		double radius = state.manifested ? 5.8D : 4.6D;
		double strength = TemporaryStatBonusManager.effectiveStrength(player);
		double vitality = TemporaryStatBonusManager.effectiveVitality(player);
		float damage = (float) ((13.0D + strength / 7.5D + vitality / 24.0D)
				* (state.manifested ? 1.2D : 1.0D));
		int hits = 0;
		for (LivingEntity target : targetsInRadius(player, center, radius, 3.6D)) {
			float applied = target instanceof Player
					? AntaresCombatRules.playerDamage(damage) : damage;
			if (!dealDestruction(player, target, applied))
				continue;
			Vec3 away = horizontal(target.position().subtract(center));
			if (away.lengthSqr() > 0.001D) {
				double control = AntaresCombatRules.bossControlScale(isBoss(target));
				target.setDeltaMovement(target.getDeltaMovement().add(
						away.normalize().scale(0.7D * control)).add(0.0D, 0.38D * control, 0.0D));
				target.hurtMarked = true;
			}
			hits++;
		}
		if (hits > 0)
			gainRuin(player, 1);
		if (contactedTerrain)
			AbilityDestructionManager.impact(player,
					AbilityDestructionManager.Profile.ANTARES_DESCENT, center,
					strength + vitality * 0.32D, state.manifested);
		AntaresVfxEventMessage.sendDescentImpact(player, center, (float) radius,
				state.manifested, hits > 0, state.seed + 211);
		player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
				SoundSource.PLAYERS, 1.2F, state.manifested ? 0.48F : 0.58F);
	}

	private static void updateRoar(ServerPlayer player, long now) {
		RoarState state = ROARS.get(player.getUUID());
		if (state == null || now < state.releaseTick)
			return;
		ROARS.remove(player.getUUID());
		Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.48D, 0.0D);
		double radius = state.manifested ? 10.0D : 8.0D;
		double strength = TemporaryStatBonusManager.effectiveStrength(player);
		double intelligence = TemporaryStatBonusManager.effectiveIntelligence(player);
		float damage = (float) ((8.0D + strength / 14.0D + intelligence / 30.0D)
				* (state.manifested ? 1.18D : 1.0D));
		int hits = 0;
		for (LivingEntity target : targetsInRadius(player, center, radius, radius * 0.72D)) {
			float applied = target instanceof Player
					? AntaresCombatRules.playerDamage(damage) : damage;
			if (!dealDestruction(player, target, applied))
				continue;
			boolean boss = isBoss(target);
			int duration = boss ? 35 : target instanceof Player ? 50 : 90;
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
					duration, boss ? 0 : 2, false, true));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
					duration, boss ? 0 : 1, false, true));
			Vec3 away = horizontal(target.position().subtract(center));
			if (away.lengthSqr() > 0.001D) {
				double control = AntaresCombatRules.bossControlScale(boss);
				target.setDeltaMovement(target.getDeltaMovement().add(
						away.normalize().scale(1.0D * control)).add(0.0D, 0.24D * control, 0.0D));
				target.hurtMarked = true;
			}
			AntaresVfxEventMessage.sendOverawedMark(player, target, duration,
					state.seed + target.getId() * 17);
			hits++;
		}
		int projectiles = repelProjectiles(player, center, radius);
		if (hits > 0)
			gainRuin(player, 1);
		AbilityDestructionManager.ring(player,
				AbilityDestructionManager.Profile.ANTARES_ROAR, center, radius,
				strength + intelligence * 0.48D, state.manifested);
		AntaresVfxEventMessage.sendRoarRelease(player, center, (float) radius,
				state.manifested, hits > 0, state.seed + 307);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
				SoundSource.PLAYERS, 1.15F, projectiles > 0 ? 0.52F : 0.6F);
	}

	private static void updateExtinction(ServerPlayer player, long now) {
		ExtinctionState state = EXTINCTIONS.get(player.getUUID());
		if (state == null)
			return;
		Vec3 movement = player.getDeltaMovement();
		player.setDeltaMovement(movement.x * 0.24D, movement.y, movement.z * 0.24D);
		if (now >= state.nextPulse && state.pulse < 3) {
			fireExtinctionPulse(player, state);
			state.pulse++;
			state.nextPulse += 5L;
		}
		if (state.pulse >= 3 && now >= state.nextPulse + 1L) {
			EXTINCTIONS.remove(player.getUUID());
			AntaresVfxEventMessage.sendExtinctionAftermath(player, state.origin,
					state.focus, state.manifested, state.seed + 509);
		}
	}

	private static void fireExtinctionPulse(ServerPlayer player, ExtinctionState state) {
		Vec3 intendedEnd = state.origin.add(state.direction.scale(state.range));
		state.focus = intendedEnd;
		double radius = state.manifested ? 2.9D : 2.35D;
		double strength = TemporaryStatBonusManager.effectiveStrength(player);
		double intelligence = TemporaryStatBonusManager.effectiveIntelligence(player);
		int hits = 0;
		for (LivingEntity target : targetsAlongSegment(player, state.origin, state.focus, radius,
				EXTINCTION_TARGET_CAP)) {
			double health = Math.min(isBoss(target) ? 18.0D : 10.0D,
					target.getMaxHealth() * 0.015D);
			float damage = (float) ((13.0D + strength / 11.0D
					+ intelligence / 15.0D + health) * (state.manifested ? 1.18D : 1.0D));
			if (target instanceof Player)
				damage = AntaresCombatRules.playerDamage(damage) * 0.76F;
			if (dealDestruction(player, target, damage)) {
				target.igniteForSeconds(state.manifested ? 7 : 5);
				hits++;
			}
		}
		BlockHitResult terrainHit = terrainHit(player, state.origin, state.focus);
		if (terrainHit.getType() == HitResult.Type.BLOCK) {
			double depth = state.manifested
					? switch (state.pulse) {
						case 0 -> 64.0D;
						case 1 -> 112.0D;
						default -> 160.0D;
					}
					: switch (state.pulse) {
						case 0 -> 48.0D;
						case 1 -> 88.0D;
						default -> 128.0D;
					};
			Vec3 terrain = terrainHit.getLocation();
			Vec3 drillStart = terrain.subtract(state.direction.scale(0.55D));
			Vec3 drillEnd = terrain.add(state.direction.scale(depth));
			AbilityDestructionManager.line(player,
					AbilityDestructionManager.Profile.ANTARES_EXTINCTION,
					drillStart, drillEnd, strength + intelligence * 0.75D,
					state.manifested || state.pulse == 2);
			if (state.pulse == 2)
				AbilityDestructionManager.impact(player,
						AbilityDestructionManager.Profile.ANTARES_EXTINCTION_FINISH,
						drillEnd, strength + intelligence * 0.75D, true);
		}
		AntaresVfxEventMessage.sendExtinctionPulse(player, state.origin, state.focus,
				state.pulse, state.manifested, hits > 0, state.seed + state.pulse * 71);
		player.level().playSound(null, BlockPos.containing(state.focus), SoundEvents.GENERIC_EXPLODE.value(),
				SoundSource.PLAYERS, 1.1F, 0.48F + state.pulse * 0.08F);
	}

	private static void updateManifestation(ServerPlayer player, long now) {
		ManifestationState state = MANIFESTATIONS.get(player.getUUID());
		if (state == null)
			return;
		if (!player.isAlive()) {
			endManifestation(player, true, true);
			return;
		}
		if (now < state.nextDrain)
			return;
		state.nextDrain += 20L;
		if (!drainMana(player, 16)) {
			endManifestation(player, true, true);
			player.displayClientMessage(Component.literal("Monarch Manifestation ended: MP depleted.")
					.withStyle(ChatFormatting.RED), true);
		}
	}

	private static void gainRuin(ServerPlayer player, int amount) {
		RuinState state = ruinState(player);
		int updated = AntaresCombatRules.gainRuin(state.charges, amount);
		if (updated == state.charges) {
			state.lastCombat = player.level().getGameTime();
			state.nextDecay = state.lastCombat + RUIN_DECAY_DELAY;
			return;
		}
		setRuin(player, updated, true);
		if (updated == AntaresCombatRules.MAX_RUIN) {
			player.displayClientMessage(Component.literal("RUIN COMPLETE")
					.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
			player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
					SoundSource.PLAYERS, 0.52F, 0.66F);
		}
	}

	private static void setRuin(ServerPlayer player, int charges, boolean forceSync) {
		RuinState state = ruinState(player);
		state.charges = AntaresCombatRules.clampRuin(charges);
		state.lastCombat = player.level().getGameTime();
		state.nextDecay = state.lastCombat + RUIN_DECAY_DELAY;
		syncRuin(player, forceSync);
	}

	private static void syncRuin(ServerPlayer player, boolean force) {
		RuinState state = ruinState(player);
		int signature = state.charges | (isManifested(player) ? 1 << 4 : 0);
		if (!force && signature == state.lastSyncSignature)
			return;
		state.lastSyncSignature = signature;
		AntaresVfxEventMessage.sendRuin(player, state.charges,
				AntaresCombatRules.MAX_RUIN, isManifested(player));
	}

	private static RuinState ruinState(ServerPlayer player) {
		return RUIN.computeIfAbsent(player.getUUID(), ignored -> {
			RuinState state = new RuinState();
			state.lastCombat = player.level().getGameTime();
			state.nextDecay = state.lastCombat + RUIN_DECAY_DELAY;
			return state;
		});
	}

	private static void endManifestation(ServerPlayer player, boolean silent,
			boolean sendVisual) {
		ManifestationState removed = MANIFESTATIONS.remove(player.getUUID());
		if (removed == null) {
			removeManifestationAttributes(player);
			return;
		}
		removeManifestationAttributes(player);
		PlayerAuraSystem.clearContinuous(player);
		PlayerAuraSystem.burst(player, MANIFESTATION_AURA, 14, 0.8F);
		CooldownManager.setFullDuration(player, MANIFESTATION_COOLDOWN, 80);
		if (sendVisual)
			AntaresVfxEventMessage.sendManifestation(player, false, removed.seed + 701);
		if (!silent)
			player.displayClientMessage(Component.literal("MONARCH MANIFESTATION - RELEASED")
					.withStyle(ChatFormatting.GRAY), true);
		syncRuin(player, true);
	}

	private static void applyManifestationAttributes(ServerPlayer player) {
		removeManifestationAttributes(player);
		if (player.getAttribute(Attributes.MOVEMENT_SPEED) != null)
			player.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(
					new AttributeModifier(MANIFEST_SPEED, 0.12D,
							AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		if (player.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null)
			player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).addTransientModifier(
					new AttributeModifier(MANIFEST_KNOCKBACK, 0.15D,
							AttributeModifier.Operation.ADD_VALUE));
	}

	private static void removeManifestationAttributes(ServerPlayer player) {
		if (player.getAttribute(Attributes.MOVEMENT_SPEED) != null)
			player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(MANIFEST_SPEED);
		if (player.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null)
			player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(MANIFEST_KNOCKBACK);
	}

	private static int repelProjectiles(ServerPlayer player, Vec3 center, double radius) {
		int removed = 0;
		for (Projectile projectile : player.serverLevel().getEntitiesOfClass(Projectile.class,
				new AABB(center, center).inflate(radius), candidate -> candidate.isAlive())) {
			Entity owner = projectile.getOwner();
			if (owner == player || owner != null && (player.isAlliedTo(owner) || owner.isAlliedTo(player)))
				continue;
			projectile.discard();
			removed++;
		}
		return removed;
	}

	private static List<LivingEntity> targetsInCone(ServerPlayer player, Vec3 origin,
			Vec3 direction, double reach, double minimumDot, double verticalInflation) {
		AABB search = player.getBoundingBox().expandTowards(direction.scale(reach))
				.inflate(reach * 0.55D, verticalInflation, reach * 0.55D);
		List<LivingEntity> result = new ArrayList<>();
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
				search, candidate -> validTarget(player, candidate))) {
			Vec3 point = target.getBoundingBox().getCenter();
			Vec3 delta = point.subtract(origin);
			double distance = delta.length();
			if (distance <= reach + target.getBbWidth() * 0.5D && distance > 0.001D
					&& delta.normalize().dot(direction) >= minimumDot
					&& player.hasLineOfSight(target))
				result.add(target);
		}
		return result;
	}

	private static List<LivingEntity> targetsAlongSegment(ServerPlayer player,
			Vec3 start, Vec3 end, double radius, int targetCap) {
		AABB search = new AABB(start, end).inflate(radius + 1.0D);
		List<LivingEntity> result = new ArrayList<>();
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
				search, candidate -> validTarget(player, candidate))) {
			double allowance = radius + target.getBbWidth() * 0.45D;
			if (distanceToSegment(target.getBoundingBox().getCenter(), start, end) <= allowance)
				result.add(target);
		}
		result.sort(Comparator.comparingDouble((LivingEntity target) ->
				target.getBoundingBox().getCenter().distanceToSqr(start))
				.thenComparing(target -> target.getUUID().toString()));
		if (result.size() > targetCap)
			return new ArrayList<>(result.subList(0, targetCap));
		return result;
	}

	private static List<LivingEntity> targetsInRadius(ServerPlayer player,
			Vec3 center, double radius, double vertical) {
		AABB search = new AABB(center, center).inflate(radius, vertical, radius);
		List<LivingEntity> result = new ArrayList<>();
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
				search, candidate -> validTarget(player, candidate))) {
			if (target.getBoundingBox().getCenter().distanceTo(center)
					<= radius + target.getBbWidth() * 0.5D && player.hasLineOfSight(target))
				result.add(target);
		}
		return result;
	}

	private static Vec3[] piercingBeam(ServerPlayer player, double range) {
		Vec3 direction = player.getLookAngle().normalize();
		Vec3 start = player.getEyePosition().add(direction.scale(0.65D));
		return new Vec3[] { start, start.add(direction.scale(range)) };
	}

	/** Terrain is sampled only for optional block destruction; it never stops beam damage. */
	private static BlockHitResult terrainHit(ServerPlayer player, Vec3 start, Vec3 end) {
		return player.serverLevel().clip(new ClipContext(start, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
	}

	private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
		Vec3 segment = end.subtract(start);
		double lengthSqr = segment.lengthSqr();
		if (lengthSqr < 1.0E-8D)
			return point.distanceTo(start);
		double t = Mth.clamp(point.subtract(start).dot(segment) / lengthSqr, 0.0D, 1.0D);
		return point.distanceTo(start.add(segment.scale(t)));
	}

	private static boolean validTarget(Player player, LivingEntity target) {
		if (target == null || target == player || !target.isAlive() || !target.isAttackable()
				|| target.isInvulnerable() || target instanceof ArmorStand)
			return false;
		if (player.isAlliedTo(target) || target.isAlliedTo(player))
			return false;
		if (ShadowMonarchManager.isOwnedShadow(target, player))
			return false;
		if (target instanceof TamableAnimal tame && player.getUUID().equals(tame.getOwnerUUID()))
			return false;
		if (target instanceof Player other)
			return !other.isCreative() && !other.isSpectator() && player.canHarmPlayer(other);
		return true;
	}

	private static boolean dealDestruction(ServerPlayer player, LivingEntity target,
			float damage) {
		if (!validTarget(player, target))
			return false;
		target.invulnerableTime = 0;
		boolean hurt = target.hurt(player.damageSources().playerAttack(player),
				Math.max(0.5F, damage));
		if (hurt)
			target.setLastHurtByPlayer(player);
		return hurt;
	}

	private static boolean isBoss(LivingEntity target) {
		return !(target instanceof Player)
				&& (target.getType().is(Tags.EntityTypes.BOSSES)
						|| target.getMaxHealth() >= 250.0F);
	}

	private static boolean consumeMana(ServerPlayer player, int amount) {
		if (player.isCreative())
			return true;
		SololevelingModVariables.PlayerVariables data = variables(player);
		if (data.MP < amount) {
			fail(player, "Not enough MP (" + amount + " required)");
			return false;
		}
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.MP = Math.max(0.0D, capability.MP - amount);
					capability.syncPlayerVariables(player);
				});
		CooldownManager.set(player, "mana_refresh", 35);
		return true;
	}

	private static boolean drainMana(ServerPlayer player, int amount) {
		if (player.isCreative())
			return true;
		SololevelingModVariables.PlayerVariables data = variables(player);
		if (data.MP < amount)
			return false;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.MP = Math.max(0.0D, capability.MP - amount);
					capability.syncPlayerVariables(player);
				});
		CooldownManager.set(player, "mana_refresh", 30);
		return true;
	}

	private static boolean ready(ServerPlayer player, String key, String display) {
		if (!CooldownManager.isOnCooldown(player, key))
			return true;
		fail(player, display + ": " + CooldownManager.getRemainingSeconds(player, key) + "s");
		return false;
	}

	private static boolean canCast(ServerPlayer player) {
		if (!player.isAlive() || !isAntaresVessel(player))
			return false;
		if (isActionLocked(player)) {
			fail(player, "Another destruction art is still active.");
			return false;
		}
		return true;
	}

	private static boolean isActionLocked(Entity entity) {
		if (entity == null)
			return false;
		UUID id = entity.getUUID();
		return BREATHS.containsKey(id) || DESCENTS.containsKey(id)
				|| ROARS.containsKey(id) || EXTINCTIONS.containsKey(id);
	}

	private static boolean hasRuntime(ServerPlayer player) {
		UUID id = player.getUUID();
		return RUIN.containsKey(id) || BREATHS.containsKey(id) || DESCENTS.containsKey(id)
				|| ROARS.containsKey(id) || EXTINCTIONS.containsKey(id)
				|| MANIFESTATIONS.containsKey(id);
	}

	private static void clearPlayer(ServerPlayer player, boolean sendVisual) {
		UUID id = player.getUUID();
		BreathState breath = BREATHS.remove(id);
		DESCENTS.remove(id);
		ROARS.remove(id);
		EXTINCTIONS.remove(id);
		if (breath != null && sendVisual)
			AntaresVfxEventMessage.sendBreathEnd(player, player.position(), player.position(),
					breath.seed + 809);
		if (MANIFESTATIONS.containsKey(id))
			endManifestation(player, true, sendVisual);
		else
			removeManifestationAttributes(player);
		RUIN.remove(id);
		player.getPersistentData().remove(FALL_SAFE_UNTIL);
		if (sendVisual)
			AntaresVfxEventMessage.sendRuin(player, 0, AntaresCombatRules.MAX_RUIN, false);
	}

	private static Vec3 horizontalLook(Entity entity) {
		Vec3 value = horizontal(entity.getLookAngle());
		return value.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : value.normalize();
	}

	private static Vec3 horizontal(Vec3 value) {
		return new Vec3(value.x, 0.0D, value.z);
	}

	private static int visualSeed(ServerPlayer player, int salt) {
		long time = player.level().getGameTime();
		return player.getUUID().hashCode() * 31 + (int) (time ^ time >>> 32) + salt;
	}

	private static void fail(ServerPlayer player, String message) {
		player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), true);
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static final class RuinState {
		private int charges;
		private long lastCombat;
		private long nextDecay;
		private int lastSyncSignature = Integer.MIN_VALUE;
	}

	private static final class BreathState {
		private final long startTick;
		private final long endTick;
		private long nextPulse;
		private final int seed;
		private int pulse;
		private boolean ruinAwarded;
		private BreathState(long startTick, long endTick, long nextPulse, int seed) {
			this.startTick = startTick;
			this.endTick = endTick;
			this.nextPulse = nextPulse;
			this.seed = seed;
		}
	}

	private static final class DescentState {
		private final long startTick;
		private final long endTick;
		private Vec3 direction;
		private final int seed;
		private final boolean manifested;

		private DescentState(long startTick, long endTick, Vec3 direction,
				int seed, boolean manifested) {
			this.startTick = startTick;
			this.endTick = endTick;
			this.direction = direction;
			this.seed = seed;
			this.manifested = manifested;
		}
	}

	private record RoarState(long releaseTick, int seed, boolean manifested) {
	}

	private static final class ExtinctionState {
		private final Vec3 origin;
		private Vec3 focus;
		private final Vec3 direction;
		private final double range;
		private final long startTick;
		private long nextPulse;
		private final int seed;
		private final boolean manifested;
		private int pulse;

		private ExtinctionState(Vec3 origin, Vec3 focus, Vec3 direction,
				double range, long startTick,
				long nextPulse, int seed, boolean manifested) {
			this.origin = origin;
			this.focus = focus;
			this.direction = direction;
			this.range = range;
			this.startTick = startTick;
			this.nextPulse = nextPulse;
			this.seed = seed;
			this.manifested = manifested;
		}
	}

	private static final class ManifestationState {
		private long nextDrain;
		private final int seed;

		private ManifestationState(long nextDrain, int seed) {
			this.nextDrain = nextDrain;
			this.seed = seed;
		}
	}
}
