package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.BeastVfxEntity;
import net.solocraft.network.BeastHuntStatusMessage;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Server-authoritative combat foundation for Rakan's vessel moveset. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class BeastMonarchManager {
	public static final int JOB_ID = 9;
	public static final String IDENTITY = "rakan";
	public static final String INTERCEPT_COOLDOWN = "beast_predators_intercept";

	private static final String NEXT_STRIKE = "beast_next_strike";
	private static final String LAST_STRIKE = "beast_last_strike";
	private static final String COMBO = "beast_cadence_combo";
	private static final String FALL_SAFE_UNTIL = "beast_fall_safe_until";
	private static final String AMBUSH_UNTIL = "beast_ambush_until";
	private static final String AMBUSH_TARGET = "beast_ambush_target";
	private static final String STATUS_ACTIVE = "beast_status_active";

	private static final int MAX_HUNT = 100;
	private static final int FEAT_COOLDOWN = 120;
	private static final int OPENING_WINDOW = 160;
	private static final int HUNT_DECAY_DELAY = 100;
	private static final int CULL_DECAY_GRACE = 240;

	private static final Map<UUID, HuntState> HUNTS = new HashMap<>();
	private static final Map<UUID, InterceptCharge> CHARGES = new HashMap<>();
	private static final Map<UUID, InterceptState> INTERCEPTS = new HashMap<>();

	private BeastMonarchManager() {
	}

	public static boolean isBeastVessel(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables data = variables(entity);
		return (int) data.JOB == JOB_ID && (IDENTITY.equals(data.vesselIdentity) || data.vesselIdentity.isBlank());
	}

	public static boolean isFangStance(Entity entity) {
		return isBeastVessel(entity) && variables(entity).combatmode;
	}

	public static void enhancedClawStrike(Player entity) {
		if (!(entity instanceof ServerPlayer player) || !isFangStance(player) || !player.isAlive())
			return;
		long now = player.level().getGameTime();
		if (now < player.getPersistentData().getLong(NEXT_STRIKE))
			return;

		HuntState state = HUNTS.computeIfAbsent(player.getUUID(), ignored -> new HuntState());
		long lastStrike = player.getPersistentData().getLong(LAST_STRIKE);
		int combo = now - lastStrike <= 18 ? player.getPersistentData().getInt(COMBO) % 3 + 1 : 1;
		int baseMana = combo == 1 ? 40 : combo == 2 ? 50 : 70;
		int mana = VesselManaScaling.strengthScaledCost(player, baseMana, 0.28D);
		if (!consumeMana(player, mana))
			return;

		int recovery = combo == 3 ? 7 : 5;
		player.getPersistentData().putLong(NEXT_STRIKE, now + recovery);
		player.getPersistentData().putLong(LAST_STRIKE, now);
		player.getPersistentData().putInt(COMBO, combo);
		state.combo = combo;
		player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

		Vec3 forward = horizontalLook(player);
		double reach = combo == 1 ? 4.35D : combo == 2 ? 4.65D : 5.0D;
		LivingEntity target = crosshairTarget(player, reach);
		boolean hit = false;
		boolean ambush = false;
		boolean savageRend = false;
		if (target != null) {
			ambush = isAmbushReady(player, target, now);
			boolean quarryHit = Objects.equals(state.quarryId, target.getUUID());
			savageRend = combo == 3 && quarryHit && Integer.bitCount(state.openingMask) >= 2;
			double strength = variables(player).Strength;
			double baseDamage = 8.0D + strength / 12.0D;
			double ratio = combo == 1 ? 0.65D : combo == 2 ? 0.75D : 1.0D;
			double huntBonus = quarryHit ? 1.0D + state.hunt * 0.002D : 1.0D;
			float damage = (float) (baseDamage * ratio * huntBonus
					* (ambush ? 1.18D : 1.0D) * (savageRend ? 1.35D : 1.0D));
			if (target instanceof Player)
				damage *= 0.78F;
			hit = dealPhysical(player, target, damage);
			if (hit) {
				if (combo == 1)
					prepareCadenceAndQuarry(player, target, state, now);
				state.lastInteraction = now;
				applyClawReaction(player, target, combo, forward);
				spawnScar(player, target, combo, ambush);
				if (ambush) {
					consumeAmbush(player);
					awardFeat(player, state, Feat.PURSUIT, now);
				}
				if (savageRend) {
					state.openingMask = 0;
					BeastVfxEntity.spawnAttached(player.serverLevel(), target, BeastVfxEntity.OPENING,
							0xFF5548, 0x4A0000, 1.45F, 1.62F, 0.0F, 18, 2);
					player.level().playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE,
							SoundSource.PLAYERS, 0.68F, 0.72F);
				}
				if (combo == 3)
					checkAngleBreak(player, target, state, now);
			}
		}

		applyCadenceMovement(player, state, combo, forward);
		spawnClawArc(player, combo, hit, ambush);
		player.level().playSound(null, player.blockPosition(),
				combo == 3 ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_SWEEP,
				SoundSource.PLAYERS, combo == 3 ? 0.95F : 0.72F, combo == 3 ? 0.72F : 0.9F + combo * 0.08F);
		syncStatus(player, state, true);
	}

	public static void beginPredatorsIntercept(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isFangStance(player) || !player.isAlive()
				|| CHARGES.containsKey(player.getUUID()) || INTERCEPTS.containsKey(player.getUUID()))
			return;
		if (!ready(player, INTERCEPT_COOLDOWN))
			return;

		HuntState state = HUNTS.computeIfAbsent(player.getUUID(), ignored -> new HuntState());
		LivingEntity quarry = quarry(player, state);
		LivingEntity target = quarry != null && canReadForIntercept(player, quarry) ? quarry : null;
		CHARGES.put(player.getUUID(), new InterceptCharge(target == null ? null : target.getUUID(),
				player.level().getGameTime(), player.position(), target == null ? 0.0D : CombatRangeHelper.surfaceDistance(player, target)));
		player.displayClientMessage(Component.literal(target == null ? "PREDATOR'S BOUND" : "QUARRY INTERCEPT")
				.withStyle(target == null ? ChatFormatting.GRAY : ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
		if (target != null)
			BeastVfxEntity.spawnAttached(player.serverLevel(), target, BeastVfxEntity.QUARRY,
					0xD01818, 0x2A0000, 0.82F, 1.15F, 0.0F, 15, 0);
	}

	public static void releasePredatorsIntercept(Entity entity, int pressedMs) {
		if (!(entity instanceof ServerPlayer player) || !isFangStance(player))
			return;
		InterceptCharge charge = CHARGES.remove(player.getUUID());
		if (charge == null || !ready(player, INTERCEPT_COOLDOWN))
			return;

		HuntState hunt = HUNTS.computeIfAbsent(player.getUUID(), ignored -> new HuntState());
		LivingEntity target = charge.targetId == null ? null : livingEntity(player.serverLevel(), charge.targetId);
		if (target == null || !validTarget(player, target) || !Objects.equals(hunt.quarryId, target.getUUID())
				|| CombatRangeHelper.surfaceDistance(player, target) > 55.0D)
			target = null;

		int baseMana = target == null ? 180 : 260;
		int mana = VesselManaScaling.strengthScaledCost(player, baseMana, 0.34D);
		if (!consumeMana(player, mana))
			return;
		double power = Mth.clamp(0.55D + Math.max(0, pressedMs) / 1000.0D, 0.55D, 1.25D);
		Vec3 destination;
		Vec3 direction;
		double maximumDistance;
		if (target != null) {
			Vec3 preyForward = horizontal(target.getDeltaMovement());
			if (preyForward.lengthSqr() < 0.01D)
				preyForward = horizontal(target.getLookAngle());
			if (preyForward.lengthSqr() < 0.01D)
				preyForward = horizontalLook(player);
			preyForward = preyForward.normalize();
			double side = interceptSide(player, target, preyForward);
			Vec3 right = new Vec3(-preyForward.z, 0.0D, preyForward.x).scale(side);
			double leadTicks = 4.0D + power * 5.0D;
			Vec3 predicted = target.position().add(horizontal(target.getDeltaMovement()).scale(leadTicks));
			destination = predicted.add(right.scale(2.15D + target.getBbWidth() * 0.45D));
			maximumDistance = Math.min(18.0D, player.position().distanceTo(destination) + 1.5D);
			direction = interceptDirection(player.position(), destination, 0.28D);
		} else {
			direction = clampVertical(player.getLookAngle(), 0.34D).normalize();
			maximumDistance = 8.0D + power * 6.0D;
			destination = player.position().add(direction.scale(maximumDistance));
		}

		double speed = (target == null ? 1.3D : 1.55D) + power * (target == null ? 0.48D : 0.58D);
		int lifetime = Mth.clamp((int) Math.ceil(maximumDistance / speed) + 4, 7, 18);
		long now = player.level().getGameTime();
		InterceptState state = new InterceptState(target == null ? null : target.getUUID(), direction,
				destination, player.position(), speed, maximumDistance, now + lifetime);
		INTERCEPTS.put(player.getUUID(), state);
		CooldownManager.set(player, INTERCEPT_COOLDOWN, target == null ? 45 : 65);
		player.getPersistentData().putLong(FALL_SAFE_UNTIL, now + lifetime + 45L);
		player.fallDistance = 0.0F;
		player.setDeltaMovement(direction.scale(speed));
		player.hurtMarked = true;
		BeastVfxEntity.spawnAttached(player.serverLevel(), player, BeastVfxEntity.INTERCEPT,
				0xE31824, 0x420005, 1.6F + (float) power * 0.35F, 3.4F + (float) power,
				0.0F, lifetime, target == null ? 0 : 1);
		player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1,
				SoundSource.PLAYERS, 0.9F, target == null ? 0.7F : 0.58F);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
				|| !(event.player instanceof ServerPlayer player))
			return;
		if (!isBeastVessel(player)) {
			if (player.getPersistentData().getBoolean(STATUS_ACTIVE) || HUNTS.containsKey(player.getUUID())
					|| CHARGES.containsKey(player.getUUID()) || INTERCEPTS.containsKey(player.getUUID()))
				clearPlayer(player);
			return;
		}

		HuntState state = HUNTS.computeIfAbsent(player.getUUID(), ignored -> new HuntState());
		long now = player.level().getGameTime();
		updateQuarry(player, state, now);
		updateHuntDecay(player, state, now);
		updateOpenings(state, now);
		updateCharge(player, now);
		updateIntercept(player, now);
		if (state.combo != 0 && now - player.getPersistentData().getLong(LAST_STRIKE) > 18)
			state.combo = 0;
		if (!isFangStance(player)) {
			CHARGES.remove(player.getUUID());
			if (INTERCEPTS.containsKey(player.getUUID()))
				finishIntercept(player, INTERCEPTS.get(player.getUUID()), false);
			state.combo = 0;
		}
		if (player.getPersistentData().getLong(AMBUSH_UNTIL) < now)
			consumeAmbush(player);
		syncStatus(player, state, false);
	}

	@SubscribeEvent
	public static void onAttackEntity(AttackEntityEvent event) {
		if (isFangStance(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		if (isFangStance(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onFall(LivingFallEvent event) {
		if (event.getEntity() instanceof Player player
				&& player.getPersistentData().getLong(FALL_SAFE_UNTIL) >= player.level().getGameTime()) {
			event.setDamageMultiplier(0.15F);
			player.fallDistance *= 0.25F;
		}
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		Entity source = event.getSource().getEntity();
		if (source instanceof ServerPlayer player && isBeastVessel(player)) {
			HuntState state = HUNTS.get(player.getUUID());
			if (state != null && Objects.equals(state.quarryId, event.getEntity().getUUID())) {
				long now = player.level().getGameTime();
				awardFeat(player, state, Feat.CULL, now);
				state.quarryId = null;
				state.lastInteraction = now;
				state.decayGraceUntil = now + CULL_DECAY_GRACE;
				syncStatus(player, state, true);
			}
		}
		if (event.getEntity() instanceof ServerPlayer dead)
			clearPlayer(dead);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			clearPlayer(player);
	}

	@SubscribeEvent
	public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			clearPlayer(player);
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		HUNTS.clear();
		CHARGES.clear();
		INTERCEPTS.clear();
	}

	private static void prepareCadenceAndQuarry(ServerPlayer player, LivingEntity target, HuntState state, long now) {
		LivingEntity current = quarry(player, state);
		if (current == null) {
			claimQuarry(player, target, state, now, false);
		} else if (current != target && player.isShiftKeyDown()) {
			claimQuarry(player, target, state, now, true);
		}
		if (Objects.equals(state.quarryId, target.getUUID())) {
			state.cadenceTarget = target.getUUID();
			state.cadenceStartVector = relativeAttackVector(player, target);
		}
	}

	private static void claimQuarry(ServerPlayer player, LivingEntity target, HuntState state, long now, boolean switching) {
		if (!validTarget(player, target))
			return;
		LivingEntity current = quarry(player, state);
		if (switching && current != null && current != target) {
			if (now < state.swapLockUntil) {
				player.displayClientMessage(Component.literal("Quarry swap is locked.").withStyle(ChatFormatting.RED), true);
				return;
			}
			if (state.hunt < 15) {
				player.displayClientMessage(Component.literal("15 Hunt is required to abandon living Quarry.")
						.withStyle(ChatFormatting.RED), true);
				return;
			}
			state.hunt -= 15;
			state.swapLockUntil = now + 80;
		}
		state.quarryId = target.getUUID();
		state.lastInteraction = now;
		state.lastLineOfSight = now;
		state.cadenceTarget = null;
		state.cadenceStartVector = null;
		BeastVfxEntity.spawnAttached(player.serverLevel(), target, BeastVfxEntity.QUARRY,
				0xE21C24, 0x300000, 0.94F, 1.25F, 0.0F, 22, 0);
		player.level().playSound(null, target.blockPosition(), SoundEvents.WOLF_GROWL,
				SoundSource.PLAYERS, 0.9F, 0.62F);
		player.displayClientMessage(Component.literal("QUARRY: ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
				.append(target.getDisplayName().copy().withStyle(ChatFormatting.WHITE)), true);
		if (target instanceof ServerPlayer quarryPlayer)
			quarryPlayer.displayClientMessage(Component.literal("You have been marked as Quarry.")
					.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
	}

	private static void checkAngleBreak(ServerPlayer player, LivingEntity target, HuntState state, long now) {
		if (!Objects.equals(state.quarryId, target.getUUID()) || !Objects.equals(state.cadenceTarget, target.getUUID())
				|| state.cadenceStartVector == null)
			return;
		Vec3 current = relativeAttackVector(player, target);
		double dot = Mth.clamp(state.cadenceStartVector.dot(current), -1.0D, 1.0D);
		double angle = Math.toDegrees(Math.acos(dot));
		state.cadenceTarget = null;
		state.cadenceStartVector = null;
		if (angle >= 60.0D)
			awardFeat(player, state, Feat.ANGLE_BREAK, now);
	}

	private static void awardFeat(ServerPlayer player, HuntState state, Feat feat, long now) {
		if (state.quarryId == null || state.lastFeat == feat || now < state.featReadyAt.getOrDefault(feat, 0L))
			return;
		if (now - state.gainWindowStart >= 20) {
			state.gainWindowStart = now;
			state.gainedThisSecond = 0;
		}
		int gained = Math.min(feat.hunt, Math.max(0, 18 - state.gainedThisSecond));
		if (gained <= 0)
			return;
		state.hunt = Math.min(MAX_HUNT, state.hunt + gained);
		state.gainedThisSecond += gained;
		state.lastFeat = feat;
		state.featReadyAt.put(feat, now + FEAT_COOLDOWN);
		state.lastInteraction = now;
		state.decayGraceUntil = Math.max(state.decayGraceUntil, now + HUNT_DECAY_DELAY);
		if (now > state.openingExpiresAt) {
			state.openingMask = 0;
			state.openingExpiresAt = now + OPENING_WINDOW;
		}
		if (state.openingMask == 0)
			state.openingExpiresAt = now + OPENING_WINDOW;
		state.openingMask |= feat.bit;
		player.displayClientMessage(Component.literal(feat.display + "  +" + gained + " HUNT")
				.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
		if (Integer.bitCount(state.openingMask) >= 2) {
			LivingEntity target = quarry(player, state);
			if (target != null)
				BeastVfxEntity.spawnAttached(player.serverLevel(), target, BeastVfxEntity.OPENING,
						0xFF3A32, 0x5A0000, 1.15F, 1.38F, 0.0F, 16, 0);
		}
	}

	private static void updateQuarry(ServerPlayer player, HuntState state, long now) {
		LivingEntity quarry = quarry(player, state);
		if (quarry == null) {
			state.quarryId = null;
			return;
		}
		if (player.hasLineOfSight(quarry))
			state.lastLineOfSight = now;
		double distance = CombatRangeHelper.surfaceDistance(player, quarry);
		if ((distance > 24.0D && now - state.lastLineOfSight > 80) || now - state.lastInteraction > 200) {
			state.quarryId = null;
			state.cadenceTarget = null;
			state.cadenceStartVector = null;
		}
	}

	private static void updateHuntDecay(ServerPlayer player, HuntState state, long now) {
		if (state.hunt <= 0 || now <= state.decayGraceUntil || now - state.lastInteraction <= HUNT_DECAY_DELAY)
			return;
		if (now % 5L == 0L) {
			state.hunt = Math.max(0, state.hunt - 1);
			if (state.hunt == 0) {
				state.openingMask = 0;
				state.lastFeat = null;
			}
		}
	}

	private static void updateOpenings(HuntState state, long now) {
		if (state.openingMask != 0 && now > state.openingExpiresAt)
			state.openingMask = 0;
	}

	private static void updateCharge(ServerPlayer player, long now) {
		InterceptCharge charge = CHARGES.get(player.getUUID());
		if (charge != null && now - charge.startedAt > 100)
			CHARGES.remove(player.getUUID());
	}

	private static void updateIntercept(ServerPlayer player, long now) {
		InterceptState state = INTERCEPTS.get(player.getUUID());
		if (state == null)
			return;
		if (!player.isAlive() || !isFangStance(player) || now >= state.expiresAt || player.horizontalCollision) {
			finishIntercept(player, state, true);
			return;
		}
		Vec3 movement = state.direction.scale(state.speed);
		player.setDeltaMovement(movement);
		player.fallDistance = 0.0F;
		player.hurtMarked = true;
		double traveled = player.position().distanceTo(state.start);
		boolean reachedDestination = player.position().distanceToSqr(state.destination) <= 1.2D;
		if (state.targetId != null) {
			LivingEntity target = livingEntity(player.serverLevel(), state.targetId);
			if (target == null || !validTarget(player, target)) {
				finishIntercept(player, state, false);
				return;
			}
			if (CombatRangeHelper.surfaceDistance(player, target) <= 1.25D) {
				finishIntercept(player, state, true);
				return;
			}
		}
		if (traveled >= state.maximumDistance || reachedDestination)
			finishIntercept(player, state, true);
	}

	private static void finishIntercept(ServerPlayer player, InterceptState state, boolean armAmbush) {
		if (state == null || !INTERCEPTS.remove(player.getUUID(), state))
			return;
		Vec3 current = player.getDeltaMovement();
		player.setDeltaMovement(current.x * 0.28D, Math.min(0.16D, current.y * 0.35D), current.z * 0.28D);
		player.hurtMarked = true;
		if (!armAmbush || state.targetId == null)
			return;
		LivingEntity target = livingEntity(player.serverLevel(), state.targetId);
		if (target == null || !validTarget(player, target) || player.position().distanceTo(state.start) < 4.5D
				|| CombatRangeHelper.surfaceDistance(player, target) > 6.0D)
			return;
		long now = player.level().getGameTime();
		player.getPersistentData().putLong(AMBUSH_UNTIL, now + 14);
		player.getPersistentData().putUUID(AMBUSH_TARGET, target.getUUID());
		BeastVfxEntity.spawnAttached(player.serverLevel(), player, BeastVfxEntity.OPENING,
				0xFF2B2B, 0x4A0000, 0.85F, 1.0F, 0.0F, 10, 1);
	}

	private static boolean isAmbushReady(ServerPlayer player, LivingEntity target, long now) {
		return now <= player.getPersistentData().getLong(AMBUSH_UNTIL)
				&& player.getPersistentData().hasUUID(AMBUSH_TARGET)
				&& player.getPersistentData().getUUID(AMBUSH_TARGET).equals(target.getUUID());
	}

	private static void consumeAmbush(ServerPlayer player) {
		player.getPersistentData().remove(AMBUSH_UNTIL);
		player.getPersistentData().remove(AMBUSH_TARGET);
	}

	private static void applyCadenceMovement(ServerPlayer player, HuntState state, int combo, Vec3 forward) {
		Vec3 step;
		if (combo == 2) {
			double side = Math.abs(player.xxa) > 0.05F ? Math.signum(player.xxa) : state.orbitSide;
			state.orbitSide = -side;
			Vec3 lateral = new Vec3(-forward.z, 0.0D, forward.x).scale(side * 0.24D);
			step = lateral.add(forward.scale(0.08D));
		} else {
			step = forward.scale(combo == 1 ? 0.13D : 0.1D);
		}
		if (player.level().noCollision(player, player.getBoundingBox().move(step))) {
			player.setDeltaMovement(player.getDeltaMovement().add(step));
			player.hurtMarked = true;
		}
	}

	private static void applyClawReaction(ServerPlayer player, LivingEntity target, int combo, Vec3 forward) {
		double resistance = Math.max(0.15D, 1.0D - target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
		double force = (combo == 3 ? 0.38D : combo == 2 ? 0.17D : 0.11D) * resistance;
		Vec3 push = forward.scale(force);
		target.setDeltaMovement(target.getDeltaMovement().add(push.x, combo == 3 ? 0.08D * resistance : 0.0D, push.z));
		target.hurtMarked = true;
	}

	private static void spawnClawArc(ServerPlayer player, int combo, boolean hit, boolean ambush) {
		Vec3 direction = player.getLookAngle().normalize();
		Vec3 origin = player.getEyePosition().add(direction.scale(combo == 3 ? 2.0D : 1.65D)).add(0.0D, -0.48D, 0.0D);
		float scale = combo == 1 ? 1.75F : combo == 2 ? 1.95F : 2.25F;
		float length = combo == 3 ? 2.15F : 1.75F;
		float roll = combo == 1 ? -24.0F : combo == 2 ? 24.0F : 0.0F;
		BeastVfxEntity.spawn(player.serverLevel(), origin, direction, BeastVfxEntity.CLAW,
				ambush ? 0xFF4036 : hit ? 0xED1F2B : 0xB3121B, 0x3A0005,
				scale, length, roll, combo == 3 ? 11 : 9, combo);
	}

	private static void spawnScar(ServerPlayer player, LivingEntity target, int combo, boolean ambush) {
		float width = Mth.clamp(target.getBbWidth() * 1.1F, 0.78F, 2.35F);
		float height = Mth.clamp(target.getBbHeight() * 0.52F, 0.9F, 2.7F);
		BeastVfxEntity.spawnAttached(player.serverLevel(), target, BeastVfxEntity.SCAR,
				ambush ? 0xFF5148 : 0xE51D29, 0x260000, width, height,
				combo == 2 ? 12.0F : combo == 3 ? -5.0F : -14.0F, combo == 3 ? 17 : 13, combo);
	}

	private static LivingEntity crosshairTarget(ServerPlayer player, double reach) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		AABB search = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(2.0D);
		LivingEntity best = null;
		double bestAlong = Double.MAX_VALUE;
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
				candidate -> validTarget(player, candidate))) {
			if (CombatRangeHelper.surfaceDistance(player, target) > reach)
				continue;
			Vec3 closest = closestPoint(eye, target.getBoundingBox());
			Vec3 to = closest.subtract(eye);
			double along = to.dot(look);
			if (along < -0.2D || along > reach)
				continue;
			double perpendicular = to.subtract(look.scale(Math.max(0.0D, along))).length();
			double allowance = 0.72D + target.getBbWidth() * 0.16D;
			if (perpendicular <= allowance && along < bestAlong && player.hasLineOfSight(target)) {
				best = target;
				bestAlong = along;
			}
		}
		return best;
	}

	private static boolean canReadForIntercept(ServerPlayer player, LivingEntity target) {
		if (!validTarget(player, target) || CombatRangeHelper.surfaceDistance(player, target) > 42.0D
				|| !player.hasLineOfSight(target))
			return false;
		Vec3 toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePosition()).normalize();
		return player.getLookAngle().normalize().dot(toTarget) >= 0.25D;
	}

	private static boolean validTarget(Player player, LivingEntity target) {
		if (target == null || target == player || !target.isAlive() || !target.isAttackable()
				|| target.isInvulnerable() || target instanceof ArmorStand)
			return false;
		if (player.isAlliedTo(target) || target.isAlliedTo(player) || ShadowMonarchManager.isOwnedShadow(target, player))
			return false;
		if (target instanceof TamableAnimal tame && player.getUUID().equals(tame.getOwnerUUID()))
			return false;
		if (target instanceof Player other)
			return !other.isCreative() && !other.isSpectator() && player.canHarmPlayer(other);
		return true;
	}

	private static boolean dealPhysical(ServerPlayer player, LivingEntity target, float damage) {
		if (!validTarget(player, target))
			return false;
		target.invulnerableTime = 0;
		boolean hurt = target.hurt(player.damageSources().playerAttack(player), Math.max(0.5F, damage));
		if (hurt)
			target.setLastHurtByPlayer(player);
		return hurt;
	}

	private static boolean consumeMana(ServerPlayer player, int amount) {
		if (player.isCreative())
			return true;
		SololevelingModVariables.PlayerVariables data = variables(player);
		if (data.MP < amount) {
			player.displayClientMessage(Component.literal("Not enough MP (" + amount + " required)")
					.withStyle(ChatFormatting.RED), true);
			return false;
		}
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.MP = Math.max(0.0D, capability.MP - amount);
			capability.syncPlayerVariables(player);
		});
		CooldownManager.set(player, "mana_refresh", 35);
		return true;
	}

	private static boolean ready(ServerPlayer player, String key) {
		if (!CooldownManager.isOnCooldown(player, key))
			return true;
		player.displayClientMessage(Component.literal("Predator's Intercept: "
				+ CooldownManager.getRemainingSeconds(player, key) + "s").withStyle(ChatFormatting.RED), true);
		return false;
	}

	private static LivingEntity quarry(ServerPlayer player, HuntState state) {
		if (state == null || state.quarryId == null)
			return null;
		LivingEntity target = livingEntity(player.serverLevel(), state.quarryId);
		return validTarget(player, target) ? target : null;
	}

	private static LivingEntity livingEntity(ServerLevel level, UUID id) {
		Entity entity = id == null ? null : level.getEntity(id);
		return entity instanceof LivingEntity living ? living : null;
	}

	private static Vec3 relativeAttackVector(ServerPlayer player, LivingEntity target) {
		Vec3 relative = horizontal(player.position().subtract(target.position()));
		return relative.lengthSqr() < 0.001D ? horizontalLook(player).scale(-1.0D) : relative.normalize();
	}

	private static double interceptSide(ServerPlayer player, LivingEntity target, Vec3 preyForward) {
		if (Math.abs(player.xxa) > 0.05F)
			return Math.signum(player.xxa);
		Vec3 toPlayer = horizontal(player.position().subtract(target.position()));
		double cross = preyForward.x * toPlayer.z - preyForward.z * toPlayer.x;
		return cross >= 0.0D ? 1.0D : -1.0D;
	}

	private static Vec3 interceptDirection(Vec3 origin, Vec3 destination, double verticalLimit) {
		Vec3 delta = destination.subtract(origin);
		Vec3 flat = horizontal(delta);
		if (flat.lengthSqr() < 0.001D)
			return new Vec3(0.0D, Mth.clamp(delta.y, -verticalLimit, verticalLimit), 1.0D).normalize();
		double horizontalLength = flat.length();
		double y = Mth.clamp(delta.y / Math.max(1.0D, horizontalLength), -verticalLimit, verticalLimit);
		Vec3 normalized = flat.normalize();
		return new Vec3(normalized.x, y, normalized.z).normalize();
	}

	private static Vec3 clampVertical(Vec3 direction, double limit) {
		return new Vec3(direction.x, Mth.clamp(direction.y, -limit, limit), direction.z);
	}

	private static Vec3 closestPoint(Vec3 point, AABB box) {
		return new Vec3(Mth.clamp(point.x, box.minX, box.maxX), Mth.clamp(point.y, box.minY, box.maxY),
				Mth.clamp(point.z, box.minZ, box.maxZ));
	}

	private static Vec3 horizontalLook(Entity entity) {
		Vec3 look = horizontal(entity.getLookAngle());
		return look.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : look.normalize();
	}

	private static Vec3 horizontal(Vec3 vector) {
		return new Vec3(vector.x, 0.0D, vector.z);
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static void syncStatus(ServerPlayer player, HuntState state, boolean force) {
		LivingEntity quarry = quarry(player, state);
		String quarryName = quarry == null ? "NO QUARRY" : quarry.getDisplayName().getString();
		int openings = Integer.bitCount(state.openingMask);
		int signature = Objects.hash(state.hunt, state.quarryId, openings, state.combo, isFangStance(player));
		if (!force && signature == state.lastSyncSignature)
			return;
		state.lastSyncSignature = signature;
		player.getPersistentData().putBoolean(STATUS_ACTIVE, true);
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new BeastHuntStatusMessage(true, quarryName, state.hunt, Math.min(2, openings), state.combo,
						isFangStance(player)));
	}

	private static void clearPlayer(ServerPlayer player) {
		HUNTS.remove(player.getUUID());
		CHARGES.remove(player.getUUID());
		INTERCEPTS.remove(player.getUUID());
		consumeAmbush(player);
		player.getPersistentData().remove(NEXT_STRIKE);
		player.getPersistentData().remove(LAST_STRIKE);
		player.getPersistentData().remove(COMBO);
		player.getPersistentData().remove(FALL_SAFE_UNTIL);
		if (player.getPersistentData().getBoolean(STATUS_ACTIVE)) {
			player.getPersistentData().remove(STATUS_ACTIVE);
			SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
					new BeastHuntStatusMessage(false, "", 0, 0, 0, false));
		}
	}

	private enum Feat {
		PURSUIT(1, 10, "PURSUIT"),
		ANGLE_BREAK(1 << 1, 12, "ANGLE BREAK"),
		CULL(1 << 4, 20, "CULL");

		private final int bit;
		private final int hunt;
		private final String display;

		Feat(int bit, int hunt, String display) {
			this.bit = bit;
			this.hunt = hunt;
			this.display = display;
		}
	}

	private static final class HuntState {
		private UUID quarryId;
		private int hunt;
		private long lastInteraction;
		private long lastLineOfSight;
		private long swapLockUntil;
		private long decayGraceUntil;
		private Feat lastFeat;
		private final Map<Feat, Long> featReadyAt = new EnumMap<>(Feat.class);
		private long gainWindowStart;
		private int gainedThisSecond;
		private int openingMask;
		private long openingExpiresAt;
		private UUID cadenceTarget;
		private Vec3 cadenceStartVector;
		private double orbitSide = 1.0D;
		private int combo;
		private int lastSyncSignature = Integer.MIN_VALUE;
	}

	private record InterceptCharge(UUID targetId, long startedAt, Vec3 start, double startingDistance) {
	}

	private record InterceptState(UUID targetId, Vec3 direction, Vec3 destination, Vec3 start,
			double speed, double maximumDistance, long expiresAt) {
	}
}
