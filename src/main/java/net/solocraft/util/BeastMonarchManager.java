package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.BeastVfxEntity;
import net.solocraft.init.SololevelingModBlocks;
import net.solocraft.network.BeastHuntStatusMessage;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative combat foundation for Rakan's vessel moveset. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class BeastMonarchManager {
	public static final int JOB_ID = 9;
	public static final String IDENTITY = "rakan";
	public static final String INTERCEPT_COOLDOWN = "beast_predators_intercept";
	public static final String CLAW_RIFT = "Claw-Rift Passage";
	public static final String RUBBLE_JAW = "Rubble Jaw";
	public static final String KINGS_MAUL = "King's Maul";
	public static final String FERAL_RECONSTITUTION = "Feral Reconstitution";
	public static final String WHITE_FANG_SOVEREIGN = "White Fang Sovereign";
	public static final String CLAW_RIFT_COOLDOWN = "beast_claw_rift";
	public static final String RUBBLE_JAW_COOLDOWN = "beast_rubble_jaw";
	public static final String KINGS_MAUL_COOLDOWN = "beast_kings_maul";
	public static final String RECONSTITUTION_COOLDOWN = "beast_feral_reconstitution";
	public static final String SOVEREIGN_COOLDOWN = "beast_white_fang_sovereign";
	public static final String SOVEREIGN_AURA = "beast_white_fang";

	private static final String NEXT_STRIKE = "beast_next_strike";
	private static final String LAST_STRIKE = "beast_last_strike";
	private static final String COMBO = "beast_cadence_combo";
	private static final String FALL_SAFE_UNTIL = "beast_fall_safe_until";
	private static final String AMBUSH_UNTIL = "beast_ambush_until";
	private static final String AMBUSH_TARGET = "beast_ambush_target";
	private static final String AMBUSH_KIND = "beast_ambush_kind";
	private static final String HERD_UNTIL = "beast_herd_until";
	private static final String HERD_TARGET = "beast_herd_target";
	private static final String STATUS_ACTIVE = "beast_status_active";
	private static final String PROVOKED_BY_PREFIX = "slr_beast_provoked_by_";

	private static final int MAX_HUNT = 100;
	private static final int FEAT_COOLDOWN = 120;
	private static final int OPENING_WINDOW = 160;
	private static final int HUNT_DECAY_DELAY = 100;
	private static final int CULL_DECAY_GRACE = 240;
	private static final int PROVOKED_DURATION = 1200;
	private static final UUID WHITE_FANG_SPEED_MODIFIER = UUID.fromString("f4b6d431-11f9-47c9-8d9d-1cc8829d1c56");

	private static final Map<UUID, HuntState> HUNTS = new HashMap<>();
	private static final Map<UUID, InterceptCharge> CHARGES = new HashMap<>();
	private static final Map<UUID, InterceptState> INTERCEPTS = new HashMap<>();
	private static final Map<UUID, RiftState> RIFTS = new HashMap<>();
	private static final Map<UUID, JawState> JAWS = new HashMap<>();
	private static final Map<UUID, UUID> PENDING_JAWS = new HashMap<>();
	private static final Map<UUID, Deque<Wound>> WOUNDS = new HashMap<>();
	private static final Map<UUID, ReconstitutionState> RECONSTITUTIONS = new HashMap<>();
	private static final Map<UUID, SovereignChannel> SOVEREIGN_CHANNELS = new HashMap<>();
	private static final Map<UUID, SovereignState> SOVEREIGNS = new HashMap<>();

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

	public static boolean isWhiteFangSovereign(Entity entity) {
		return entity != null && SOVEREIGNS.containsKey(entity.getUUID());
	}

	public static void enhancedClawStrike(Player entity) {
		if (!(entity instanceof ServerPlayer player) || !isFangStance(player) || !player.isAlive()
				|| isActionLocked(player))
			return;
		long now = player.level().getGameTime();
		if (now < player.getPersistentData().getLong(NEXT_STRIKE))
			return;

		HuntState state = HUNTS.computeIfAbsent(player.getUUID(), ignored -> new HuntState());
		boolean sovereign = isWhiteFangSovereign(player);
		int beats = sovereign ? 4 : 3;
		long lastStrike = player.getPersistentData().getLong(LAST_STRIKE);
		int comboWindow = sovereign ? 22 : 18;
		int combo = now - lastStrike <= comboWindow ? player.getPersistentData().getInt(COMBO) % beats + 1 : 1;
		int baseMana = sovereign ? (combo == 4 ? 55 : combo == 3 ? 45 : 40)
				: combo == 1 ? 40 : combo == 2 ? 50 : 70;
		int mana = VesselManaScaling.strengthScaledCost(player, baseMana, 0.28D);
		if (!consumeMana(player, mana))
			return;

		boolean finisher = combo == beats;
		int recovery = finisher ? 7 : 5;
		player.getPersistentData().putLong(NEXT_STRIKE, now + recovery);
		player.getPersistentData().putLong(LAST_STRIKE, now);
		player.getPersistentData().putInt(COMBO, combo);
		state.combo = combo;
		player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

		Vec3 forward = horizontalLook(player);
		double reach = (combo == 1 ? 4.35D : combo == 2 ? 4.65D : 5.0D) + (sovereign ? 1.25D : 0.0D);
		LivingEntity target = crosshairTarget(player, reach);
		boolean hit = false;
		boolean ambush = false;
		boolean savageRend = false;
		if (target != null) {
			ambush = isAmbushReady(player, target, now);
			boolean quarryHit = Objects.equals(state.quarryId, target.getUUID());
			savageRend = finisher && quarryHit && Integer.bitCount(state.openingMask) >= 2;
			double strength = variables(player).Strength;
			double baseDamage = 8.0D + strength / 12.0D;
			double ratio = sovereign
					? combo == 1 ? 0.62D : combo == 2 ? 0.68D : combo == 3 ? 0.75D : 1.05D
					: combo == 1 ? 0.65D : combo == 2 ? 0.75D : 1.0D;
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
					int ambushKind = player.getPersistentData().getInt(AMBUSH_KIND);
					consumeAmbush(player);
					awardFeat(player, state, ambushKind == 2 ? Feat.RIFT_AMBUSH : Feat.PURSUIT, now);
				}
				if (isHerdReady(player, target, now)) {
					consumeHerd(player);
					awardFeat(player, state, Feat.HERD, now);
				}
				if (savageRend) {
					state.openingMask = 0;
					BeastVfxEntity.spawnAttached(player.serverLevel(), target, BeastVfxEntity.OPENING,
							0xFF5548, 0x4A0000, 1.45F, 1.62F, 0.0F, 18, 2);
					player.level().playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE,
							SoundSource.PLAYERS, 0.68F, 0.72F);
				}
				if (finisher)
					checkAngleBreak(player, target, state, now);
			}
		}

		applyCadenceMovement(player, state, combo, forward);
		spawnClawArc(player, combo, hit, ambush);
		player.level().playSound(null, player.blockPosition(),
				finisher ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_SWEEP,
				SoundSource.PLAYERS, finisher ? 0.95F : 0.72F, finisher ? 0.72F : 0.9F + combo * 0.08F);
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

	public static void castClawRift(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !canCastBeastSkill(player)
				|| !ready(player, CLAW_RIFT_COOLDOWN, CLAW_RIFT))
			return;
		boolean sovereign = isWhiteFangSovereign(player);
		int mana = VesselManaScaling.strengthScaledCost(player, sovereign ? 250 : 220, 0.26D);
		if (!consumeMana(player, mana))
			return;
		Vec3 direction = horizontalLook(player);
		Vec3 start = player.position();
		Vec3 destination = sweptDestination(player, direction, sovereign ? 12.0D : 9.0D);
		double strength = variables(player).Strength;
		float damage = (float) ((10.0D + strength / 8.0D) * (sovereign ? 1.28D : 1.0D));
		int hits = damageAlongPath(player, start, destination, sovereign ? 1.65D : 1.35D, damage, 10);
		player.teleportTo(destination.x, destination.y, destination.z);
		player.setDeltaMovement(direction.scale(0.48D));
		player.hurtMarked = true;
		player.fallDistance = 0.0F;
		player.getPersistentData().putLong(FALL_SAFE_UNTIL, player.level().getGameTime() + 35L);
		CooldownManager.set(player, CLAW_RIFT_COOLDOWN, sovereign ? 90 : 120);
		Vec3 entry = start.add(0.0D, player.getBbHeight() * 0.52D, 0.0D);
		Vec3 exit = destination.add(0.0D, player.getBbHeight() * 0.52D, 0.0D);
		Vec3 middle = entry.add(exit).scale(0.5D);
		BeastVfxEntity.spawn(player.serverLevel(), entry, direction, BeastVfxEntity.RIFT,
				0xFF812F, 0x3A0800, 1.05F, 2.45F, 0.0F, 15, 0);
		BeastVfxEntity.spawn(player.serverLevel(), exit, direction.scale(-1.0D), BeastVfxEntity.RIFT,
				0xFFB34A, 0x2C0500, 1.1F, 2.55F, 0.0F, 15, 1);
		BeastVfxEntity.spawn(player.serverLevel(), middle, direction, BeastVfxEntity.CLAW,
				0xFF8A24, 0x3D0900, sovereign ? 3.8F : 3.2F, sovereign ? 3.3F : 2.8F,
				0.0F, 14, 3);
		player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
		player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2,
				SoundSource.PLAYERS, 1.0F, hits > 0 ? 0.72F : 0.9F);
	}

	public static void castRubbleJaw(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !canCastBeastSkill(player)
				|| !ready(player, RUBBLE_JAW_COOLDOWN, RUBBLE_JAW))
			return;
		boolean sovereign = isWhiteFangSovereign(player);
		int mana = VesselManaScaling.strengthScaledCost(player, sovereign ? 340 : 300, 0.28D);
		if (!consumeMana(player, mana))
			return;
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		BlockHitResult hit = player.serverLevel().clip(new ClipContext(eye, eye.add(look.scale(12.0D)),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 center = hit.getType() == HitResult.Type.BLOCK
				? hit.getLocation().add(0.0D, 0.12D, 0.0D)
				: player.position().add(horizontalLook(player).scale(7.0D)).add(0.0D, 0.2D, 0.0D);
		double radius = sovereign ? 5.75D : 4.5D;
		double strength = variables(player).Strength;
		float damage = (float) ((12.0D + strength / 7.0D) * (sovereign ? 1.25D : 1.0D));
		int hits = 0;
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
				new AABB(center.x - radius, center.y - 2.0D, center.z - radius,
						center.x + radius, center.y + 3.5D, center.z + radius),
				candidate -> validTarget(player, candidate))) {
			if (target.getBoundingBox().getCenter().distanceTo(center) > radius + target.getBbWidth() * 0.5D
					|| !player.hasLineOfSight(target))
				continue;
			if (dealPhysical(player, target, target instanceof Player ? damage * 0.72F : damage)) {
				Vec3 away = horizontal(target.position().subtract(center));
				if (away.lengthSqr() < 0.01D)
					away = horizontalLook(player);
				double resistance = Math.max(0.2D, 1.0D - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
				target.setDeltaMovement(target.getDeltaMovement().add(away.normalize().scale(0.42D * resistance))
						.add(0.0D, (sovereign ? 0.72D : 0.55D) * resistance, 0.0D));
				target.hurtMarked = true;
				hits++;
			}
		}
		CooldownManager.set(player, RUBBLE_JAW_COOLDOWN, sovereign ? 130 : 170);
		BeastVfxEntity.spawn(player.serverLevel(), center, horizontalLook(player), BeastVfxEntity.RUBBLE_JAW,
				0xFF9A2E, 0x3E0C00, sovereign ? 3.1F : 2.55F, sovereign ? 6.2F : 5.1F,
				0.0F, 20, hits > 0 ? 1 : 0);
		player.level().playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE,
				SoundSource.PLAYERS, 1.15F, sovereign ? 0.58F : 0.68F);
	}

	public static void castKingsMaul(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !canCastBeastSkill(player)
				|| !ready(player, KINGS_MAUL_COOLDOWN, KINGS_MAUL))
			return;
		boolean sovereign = isWhiteFangSovereign(player);
		int mana = VesselManaScaling.strengthScaledCost(player, sovereign ? 430 : 380, 0.34D);
		if (!consumeMana(player, mana))
			return;
		LivingEntity target = crosshairTarget(player, sovereign ? 11.0D : 9.0D);
		if (target == null)
			target = forwardTarget(player, sovereign ? 10.0D : 8.0D, 0.58D);
		Vec3 direction = target == null ? horizontalLook(player)
				: horizontal(target.getBoundingBox().getCenter().subtract(player.position())).normalize();
		double lunge = target == null ? 4.5D
				: Mth.clamp(CombatRangeHelper.surfaceDistance(player, target) - 1.0D, 0.0D, sovereign ? 6.5D : 5.0D);
		Vec3 destination = sweptDestination(player, direction, lunge);
		player.teleportTo(destination.x, destination.y, destination.z);
		player.setDeltaMovement(direction.scale(0.38D));
		player.hurtMarked = true;
		if (target == null || !validTarget(player, target) || CombatRangeHelper.surfaceDistance(player, target) > 4.0D)
			target = forwardTarget(player, 4.5D, 0.35D);
		double strength = variables(player).Strength;
		double perception = variables(player).perception;
		float damage = (float) ((18.0D + strength / 5.0D + perception / 25.0D)
				* (sovereign ? 1.3D : 1.0D));
		if (target != null && dealPhysical(player, target, target instanceof Player ? damage * 0.68F : damage)) {
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
					sovereign ? 35 : 24, sovereign ? 2 : 1, false, true));
			Vec3 movement = target.getDeltaMovement();
			target.setDeltaMovement(movement.x * 0.35D, Math.max(0.16D, movement.y), movement.z * 0.35D);
			target.hurtMarked = true;
			BeastVfxEntity.spawnAttached(player.serverLevel(), target, BeastVfxEntity.KINGS_MAUL,
					0xFF9A2E, 0x420B00, Mth.clamp(target.getBbWidth() * 1.5F, 1.65F, 3.6F),
					Mth.clamp(target.getBbHeight() * 0.72F, 1.8F, 4.0F), 0.0F, 18, 2);
		}
		CooldownManager.set(player, KINGS_MAUL_COOLDOWN, sovereign ? 150 : 200);
		player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
		player.level().playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ATTACK,
				SoundSource.PLAYERS, 1.1F, sovereign ? 0.56F : 0.66F);
	}

	public static void castFeralReconstitution(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !canCastBeastSkill(player)
				|| !ready(player, RECONSTITUTION_COOLDOWN, FERAL_RECONSTITUTION))
			return;
		boolean sovereign = isWhiteFangSovereign(player);
		int mana = VesselManaScaling.strengthScaledCost(player, sovereign ? 300 : 260, 0.2D);
		if (!consumeMana(player, mana))
			return;
		double vitality = variables(player).Vitality;
		float missing = player.getMaxHealth() - player.getHealth();
		float heal = (float) Math.min(missing, (6.0D + vitality / 10.0D) * (sovereign ? 1.35D : 1.0D));
		if (heal > 0.0F)
			player.heal(heal);
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, sovereign ? 180 : 120,
				sovereign ? 1 : 0, false, true));
		CooldownManager.set(player, RECONSTITUTION_COOLDOWN, sovereign ? 200 : 240);
		BeastVfxEntity.spawnAttached(player.serverLevel(), player, BeastVfxEntity.RECONSTITUTION,
				0xFFF2D1, 0xD55A18, sovereign ? 1.8F : 1.45F, sovereign ? 2.0F : 1.65F,
				0.0F, 22, sovereign ? 2 : 0);
		player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE,
				SoundSource.PLAYERS, 0.75F, sovereign ? 0.7F : 0.82F);
	}

	public static void castWhiteFangSovereign(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !player.isAlive() || !isBeastVessel(player))
			return;
		if (isWhiteFangSovereign(player)) {
			endSovereign(player, false);
			return;
		}
		if (!canCastBeastSkill(player) || !ready(player, SOVEREIGN_COOLDOWN, WHITE_FANG_SOVEREIGN))
			return;
		int mana = VesselManaScaling.strengthScaledCost(player, 600, 0.18D);
		if (!consumeMana(player, mana))
			return;
		long now = player.level().getGameTime();
		SOVEREIGNS.put(player.getUUID(), new SovereignState(now + 400L, now + 400L));
		CooldownManager.set(player, SOVEREIGN_COOLDOWN, 600);
		applyWhiteFangSpeed(player);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.combatmode = true;
			capability.syncPlayerVariables(player);
		});
		PlayerAuraSystem.setContinuous(player, SOVEREIGN_AURA, 1.35F);
		PlayerAuraSystem.burst(player, SOVEREIGN_AURA, 30, 1.8F);
		BeastVfxEntity.spawnAttached(player.serverLevel(), player, BeastVfxEntity.WHITE_FANG,
				0xFFF4D6, 0xE05C18, 3.0F, 3.4F, 0.0F, 28, 1);
		player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_ROAR,
				SoundSource.PLAYERS, 0.78F, 1.35F);
		player.displayClientMessage(Component.literal("WHITE FANG SOVEREIGN - ACTIVE")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
				|| !(event.player instanceof ServerPlayer player))
			return;
		if (!isBeastVessel(player)) {
			if (player.getPersistentData().getBoolean(STATUS_ACTIVE) || HUNTS.containsKey(player.getUUID())
					|| CHARGES.containsKey(player.getUUID()) || INTERCEPTS.containsKey(player.getUUID())
					|| RIFTS.containsKey(player.getUUID()) || JAWS.containsKey(player.getUUID())
					|| RECONSTITUTIONS.containsKey(player.getUUID()) || SOVEREIGNS.containsKey(player.getUUID())
					|| SOVEREIGN_CHANNELS.containsKey(player.getUUID()))
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
		updateRift(player, state, now);
		updateJaw(player, state, now);
		updateReconstitution(player, state, now);
		updateSovereignChannel(player, state, now);
		updateSovereign(player, now);
		phaseOwnRubble(player, now);
		int comboWindow = isWhiteFangSovereign(player) ? 22 : 18;
		if (state.combo != 0 && now - player.getPersistentData().getLong(LAST_STRIKE) > comboWindow)
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
		if (isFangStance(event.getEntity()) || isActionLocked(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		if (isFangStance(event.getEntity()) || isActionLocked(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		Player player = event.getEntity();
		if (isActionLocked(player) || isWhiteFangSovereign(player))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Player player = event.getEntity();
		if (isActionLocked(player)
				|| (isWhiteFangSovereign(player) && event.getItemStack().getItem() instanceof BlockItem))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (isActionLocked(event.getEntity()) || isWhiteFangSovereign(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamage(LivingDamageEvent event) {
		Entity damageOwner = event.getSource().getEntity();
		if (damageOwner instanceof ServerPlayer attacker && isBeastVessel(attacker)
				&& event.getSource().getDirectEntity() == attacker && event.getEntity() != attacker)
			markDirectlyProvoked(attacker, event.getEntity());
		if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F)
			return;
		if (RECONSTITUTIONS.containsKey(player.getUUID()))
			interruptReconstitution(player);
		if (SOVEREIGN_CHANNELS.containsKey(player.getUUID()))
			interruptSovereignChannel(player);
		if (!isBeastVessel(player))
			return;
		HuntState hunt = HUNTS.get(player.getUUID());
		Entity attacker = event.getSource().getEntity();
		if (hunt == null || hunt.quarryId == null || attacker == null
				|| !hunt.quarryId.equals(attacker.getUUID()))
			return;
		double recoverable = event.getAmount();
		if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE))
			recoverable *= 0.5D;
		Deque<Wound> wounds = WOUNDS.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
		wounds.addLast(new Wound(attacker.getUUID(), recoverable, player.level().getGameTime()));
		while (!wounds.isEmpty() && player.level().getGameTime() - wounds.peekFirst().tick > 80L)
			wounds.removeFirst();
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
		RIFTS.clear();
		JAWS.clear();
		PENDING_JAWS.clear();
		WOUNDS.clear();
		RECONSTITUTIONS.clear();
		SOVEREIGN_CHANNELS.clear();
		SOVEREIGNS.clear();
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
		if (now < state.satedUntil)
			return;
		SovereignState sovereign = SOVEREIGNS.get(player.getUUID());
		if (sovereign != null) {
			state.lastFeat = feat;
			state.featReadyAt.put(feat, now + FEAT_COOLDOWN);
			state.lastInteraction = now;
			if (now > state.openingExpiresAt) {
				state.openingMask = 0;
				state.openingExpiresAt = now + OPENING_WINDOW;
			}
			if (state.openingMask == 0)
				state.openingExpiresAt = now + OPENING_WINDOW;
			state.openingMask |= feat.bit;
			if (sovereign.extendedFeats.add(feat) && sovereign.extendedFeats.size() <= 4) {
				sovereign.expiresAt = Math.min(sovereign.hardExpiresAt, sovereign.expiresAt + 20L);
				player.displayClientMessage(Component.literal(feat.display + "  SOVEREIGN +1s")
						.withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), true);
			}
			return;
		}
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

	private static void updateRift(ServerPlayer player, HuntState hunt, long now) {
		RiftState rift = RIFTS.get(player.getUUID());
		if (rift == null)
			return;
		if (!player.isAlive() || now >= rift.expiresAt) {
			RIFTS.remove(player.getUUID(), rift);
			return;
		}
		if (now == rift.armedAt)
			player.level().playSound(null, BlockPos.containing(rift.entry), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
					SoundSource.PLAYERS, 0.55F, 1.45F);
		if (now < rift.armedAt)
			return;
		processRiftDirection(player, hunt, rift, rift.entry, rift.entryNormal, rift.exit,
				rift.exitNormal, rift.forwardSides, rift.usedForward, now, false);
		if (rift.twoWay)
			processRiftDirection(player, hunt, rift, rift.exit, rift.exitNormal.scale(-1.0D), rift.entry,
					rift.entryNormal.scale(-1.0D), rift.reverseSides, rift.usedReverse, now, true);
	}

	private static void processRiftDirection(ServerPlayer owner, HuntState hunt, RiftState rift,
			Vec3 source, Vec3 sourceNormal, Vec3 destination, Vec3 destinationNormal,
			Map<UUID, Double> previousSides, Set<UUID> used, long now, boolean reverse) {
		AABB scan = new AABB(source.x - 1.45D, source.y - 1.7D, source.z - 1.45D,
				source.x + 1.45D, source.y + 1.7D, source.z + 1.45D);
		int checked = 0;
		for (LivingEntity traveller : owner.serverLevel().getEntitiesOfClass(LivingEntity.class, scan,
				candidate -> canUseRift(candidate) && !used.contains(candidate.getUUID()))) {
			if (++checked > 20)
				break;
			long immunity = rift.immunityUntil.getOrDefault(traveller.getUUID(), 0L);
			if (now < immunity)
				continue;
			Vec3 relative = traveller.getBoundingBox().getCenter().subtract(source);
			double side = relative.dot(sourceNormal);
			double previous = previousSides.getOrDefault(traveller.getUUID(),
					side - traveller.getDeltaMovement().dot(sourceNormal));
			previousSides.put(traveller.getUUID(), side);
			Vec3 lateral = relative.subtract(sourceNormal.scale(side));
			if (previous > -0.04D || side < -0.04D || traveller.getDeltaMovement().dot(sourceNormal) <= 0.025D
					|| Math.abs(relative.y) > 1.55D || horizontal(lateral).length() > 1.05D)
				continue;
			Vec3 safe = safeRiftExit(owner.serverLevel(), traveller, destination, destinationNormal);
			if (safe == null)
				continue;
			Vec3 velocity = rotateMomentum(traveller.getDeltaMovement(), sourceNormal, destinationNormal);
			float yawDelta = (float) Math.toDegrees(horizontalAngle(destinationNormal) - horizontalAngle(sourceNormal));
			traveller.teleportTo(safe.x, safe.y, safe.z);
			traveller.setYRot(traveller.getYRot() + yawDelta);
			traveller.setYHeadRot(traveller.getYHeadRot() + yawDelta);
			traveller.setDeltaMovement(velocity);
			traveller.hurtMarked = true;
			used.add(traveller.getUUID());
			rift.immunityUntil.put(traveller.getUUID(), now + 10L);
			owner.level().playSound(null, BlockPos.containing(destination), SoundEvents.ENDERMAN_TELEPORT,
					SoundSource.PLAYERS, 0.7F, reverse ? 0.72F : 0.88F);
			if (traveller == owner && !reverse && source.distanceTo(destination) >= 5.0D
					&& angleDegrees(sourceNormal, destinationNormal) >= 60.0D) {
				LivingEntity quarry = quarry(owner, hunt);
				if (quarry != null) {
					owner.getPersistentData().putLong(AMBUSH_UNTIL, now + 24L);
					owner.getPersistentData().putUUID(AMBUSH_TARGET, quarry.getUUID());
					owner.getPersistentData().putInt(AMBUSH_KIND, 2);
				}
			}
		}
	}

	private static void placeRubbleJaw(ServerPlayer player, ServerLevel castLevel, UUID castId,
			BlockPos center, Vec3 facing,
			List<BlockPos> planned) {
		if (!player.isAlive() || !isBeastVessel(player) || player.serverLevel() != castLevel
				|| !castId.equals(PENDING_JAWS.get(player.getUUID())))
			return;
		PENDING_JAWS.remove(player.getUUID());
		List<BlockPos> placed = new ArrayList<>();
		for (BlockPos pos : planned) {
			if (placed.size() >= 8 || !canPlaceRubble(castLevel, pos))
				continue;
			castLevel.setBlock(pos, SololevelingModBlocks.BEAST_RUBBLE.get().defaultBlockState(), 3);
			placed.add(pos.immutable());
		}
		if (placed.isEmpty()) {
			fail(player, "Rubble Jaw was obstructed before it could rise.");
			return;
		}
		long now = player.level().getGameTime();
		JAWS.put(player.getUUID(), new JawState(castId, castLevel, placed, Vec3.atCenterOf(center), facing.normalize(),
				now + 100L));
		player.level().playSound(null, center, SoundEvents.DEEPSLATE_PLACE,
				SoundSource.PLAYERS, 1.25F, 0.56F);
	}

	private static void updateJaw(ServerPlayer player, HuntState hunt, long now) {
		JawState jaw = JAWS.get(player.getUUID());
		if (jaw == null)
			return;
		if (now >= jaw.expiresAt) {
			clearJaw(player);
			return;
		}
		if ((now & 1L) != 0L)
			return;
		LivingEntity quarry = quarry(player, hunt);
		if (quarry == null)
			return;
		Vec3 heading = horizontal(quarry.getDeltaMovement());
		if (heading.lengthSqr() < 0.01D)
			heading = horizontal(quarry.getLookAngle());
		if (heading.lengthSqr() > 0.001D)
			heading = heading.normalize();
		double nearest = jaw.positions.stream().mapToDouble(pos -> Vec3.atCenterOf(pos).distanceTo(quarry.position()))
				.min().orElse(Double.MAX_VALUE);
		if (nearest <= 3.25D && jaw.lastHeading != null && heading.lengthSqr() > 0.001D
				&& angleDegrees(jaw.lastHeading, heading) >= 75.0D)
			armHerd(player, quarry, now);
		if (heading.lengthSqr() > 0.001D)
			jaw.lastHeading = heading;
		Vec3 relative = quarry.getBoundingBox().getCenter().subtract(jaw.mouthCenter);
		double side = relative.dot(jaw.facing);
		Vec3 right = new Vec3(-jaw.facing.z, 0.0D, jaw.facing.x);
		if (!Double.isNaN(jaw.lastMouthSide) && side * jaw.lastMouthSide <= 0.0D
				&& Math.abs(relative.dot(right)) <= 1.35D && Math.abs(relative.y) <= 2.1D)
			armHerd(player, quarry, now);
		jaw.lastMouthSide = side;
	}

	private static void phaseOwnRubble(ServerPlayer player, long now) {
		if (!isWhiteFangSovereign(player))
			return;
		JawState jaw = JAWS.get(player.getUUID());
		if (jaw == null || jaw.level != player.serverLevel())
			return;
		AABB body = player.getBoundingBox().inflate(0.18D);
		for (BlockPos pos : jaw.positions) {
			if (!body.intersects(new AABB(pos)) || jaw.phased.contains(pos)
					|| !player.serverLevel().getBlockState(pos).is(SololevelingModBlocks.BEAST_RUBBLE.get()))
				continue;
			jaw.phased.add(pos);
			player.serverLevel().removeBlock(pos, false);
			SololevelingMod.queueServerWork(5, () -> {
				JawState current = JAWS.get(player.getUUID());
				if (current == jaw && player.level().getGameTime() < current.expiresAt
						&& player.serverLevel().getBlockState(pos).isAir())
					player.serverLevel().setBlock(pos, SololevelingModBlocks.BEAST_RUBBLE.get().defaultBlockState(), 3);
				jaw.phased.remove(pos);
			});
		}
	}

	private static void updateReconstitution(ServerPlayer player, HuntState hunt, long now) {
		ReconstitutionState channel = RECONSTITUTIONS.get(player.getUUID());
		if (channel == null)
			return;
		Vec3 velocity = player.getDeltaMovement();
		player.setDeltaMovement(velocity.x * 0.72D, velocity.y, velocity.z * 0.72D);
		player.hurtMarked = true;
		hunt.lastInteraction = now;
		if (now < channel.finishesAt)
			return;
		if (hunt.hunt < 30 || !canAffordMana(player, 180) || !consumeMana(player, 180)) {
			interruptReconstitution(player);
			return;
		}
		if (!RECONSTITUTIONS.remove(player.getUUID(), channel))
			return;
		hunt.hunt -= 30;
		double maximumHealth = player.getMaxHealth();
		double vitality = variables(player).Vitality;
		double heal = channel.pvp
				? Math.min(channel.woundAmount * 0.30D, Math.min(6.0D, maximumHealth * 0.12D))
				: Math.min(channel.woundAmount * 0.45D, Math.min(6.0D + vitality / 10.0D, maximumHealth * 0.25D));
		player.heal((float) Math.max(0.0D, heal));
		WOUNDS.remove(player.getUUID());
		CooldownManager.set(player, RECONSTITUTION_COOLDOWN, 360);
		BeastVfxEntity.spawnAttached(player.serverLevel(), player, BeastVfxEntity.RECONSTITUTION,
				0xFFFFFF, 0xA71920, 1.45F, 1.8F, 0.0F, 12, 2);
		player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE,
				SoundSource.PLAYERS, 0.72F, 0.76F);
		player.displayClientMessage(Component.literal("WOUNDS RECONSTITUTED  +"
				+ String.format(java.util.Locale.ROOT, "%.1f", heal)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), true);
		syncStatus(player, hunt, true);
	}

	private static void interruptReconstitution(ServerPlayer player) {
		if (RECONSTITUTIONS.remove(player.getUUID()) == null)
			return;
		CooldownManager.set(player, RECONSTITUTION_COOLDOWN, 160);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK,
				SoundSource.PLAYERS, 0.75F, 0.58F);
		player.displayClientMessage(Component.literal("RECONSTITUTION INTERRUPTED")
				.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
	}

	private static void updateSovereignChannel(ServerPlayer player, HuntState hunt, long now) {
		SovereignChannel channel = SOVEREIGN_CHANNELS.get(player.getUUID());
		if (channel == null)
			return;
		Vec3 velocity = player.getDeltaMovement();
		player.setDeltaMovement(velocity.x * 0.78D, velocity.y, velocity.z * 0.78D);
		player.hurtMarked = true;
		if (now < channel.finishesAt)
			return;
		if (hunt.hunt < 100 || !canAffordMana(player, 720) || !consumeMana(player, 720)) {
			interruptSovereignChannel(player);
			return;
		}
		if (!SOVEREIGN_CHANNELS.remove(player.getUUID(), channel))
			return;
		hunt.hunt = 0;
		hunt.openingMask = 0;
		hunt.lastFeat = null;
		SOVEREIGNS.put(player.getUUID(), new SovereignState(now + 280L, now + 360L));
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.combatmode = true;
			capability.syncPlayerVariables(player);
		});
		CooldownManager.set(player, "mana_refresh", 360);
		PlayerAuraSystem.setContinuous(player, SOVEREIGN_AURA, 1.25F);
		PlayerAuraSystem.burst(player, SOVEREIGN_AURA, 28, 1.7F);
		BeastVfxEntity.spawnAttached(player.serverLevel(), player, BeastVfxEntity.WHITE_FANG,
				0xFFFFFF, 0xA5111B, 2.8F, 3.2F, 0.0F, 24, 1);
		player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_ROAR,
				SoundSource.PLAYERS, 0.72F, 1.28F);
		player.displayClientMessage(Component.literal("THE WHITE FANG WALKS")
				.withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), true);
		syncStatus(player, hunt, true);
	}

	private static void updateSovereign(ServerPlayer player, long now) {
		SovereignState state = SOVEREIGNS.get(player.getUUID());
		if (state == null)
			return;
		player.fallDistance = Math.min(player.fallDistance, 5.0F);
		if (!player.isAlive() || !isBeastVessel(player) || !isFangStance(player)
				|| now >= state.expiresAt || now >= state.hardExpiresAt)
			endSovereign(player, false);
	}

	private static void interruptSovereignChannel(ServerPlayer player) {
		if (SOVEREIGN_CHANNELS.remove(player.getUUID()) == null)
			return;
		CooldownManager.set(player, SOVEREIGN_COOLDOWN, 120);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK,
				SoundSource.PLAYERS, 0.82F, 0.48F);
		player.displayClientMessage(Component.literal("MANIFESTATION INTERRUPTED")
				.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
	}

	private static void endSovereign(ServerPlayer player, boolean silent) {
		SovereignState removed = SOVEREIGNS.remove(player.getUUID());
		if (removed == null)
			return;
		removeWhiteFangSpeed(player);
		PlayerAuraSystem.clearContinuous(player);
		HuntState hunt = HUNTS.get(player.getUUID());
		if (hunt != null) {
			hunt.satedUntil = player.level().getGameTime() + 120L;
			hunt.openingMask = 0;
			hunt.combo = 0;
		}
		if (!silent) {
			PlayerAuraSystem.burst(player, SOVEREIGN_AURA, 18, 0.9F);
			player.displayClientMessage(Component.literal("THE SOVEREIGN WITHDRAWS")
					.withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD), true);
		}
	}

	private static void resolveKingsMaul(ServerPlayer player, UUID targetId, Vec3 committedTarget,
			Vec3 line, boolean sovereign) {
		if (!player.isAlive() || !isBeastVessel(player))
			return;
		LivingEntity target = livingEntity(player.serverLevel(), targetId);
		HuntState hunt = HUNTS.get(player.getUUID());
		if (target == null || hunt == null || !Objects.equals(hunt.quarryId, targetId)) {
			maulWhiff(player);
			return;
		}
		Vec3 start = player.position();
		Vec3 landing = start;
		if (!sovereign) {
			for (double distance = 0.25D; distance <= 2.5D; distance += 0.25D) {
				Vec3 candidate = start.add(line.scale(distance));
				AABB moved = player.getBoundingBox().move(candidate.subtract(player.position()));
				if (!player.level().noCollision(player, moved))
					break;
				landing = candidate;
			}
			player.teleportTo(landing.x, landing.y, landing.z);
		}
		Vec3 currentTarget = target.getBoundingBox().getCenter();
		double offLine = distanceFromLine(currentTarget, start.add(0.0D, target.getBbHeight() * 0.45D, 0.0D), line);
		boolean crossedCommit = currentTarget.distanceTo(committedTarget) <= 2.35D && offLine <= 1.45D;
		if (!crossedCommit || CombatRangeHelper.surfaceDistance(player, target) > (sovereign ? 2.75D : 3.2D)
				|| !player.hasLineOfSight(target)) {
			maulWhiff(player);
			return;
		}
		double strength = variables(player).Strength;
		double perception = variables(player).perception;
		float damage = (float) (18.0D + strength / 6.0D + perception / 20.0D);
		if (target instanceof Player)
			damage *= 0.65F;
		if (!dealPhysical(player, target, damage)) {
			maulWhiff(player);
			return;
		}
		Vec3 away = horizontal(target.position().subtract(player.position()));
		boolean fleeing = away.lengthSqr() > 0.001D
				&& horizontal(target.getDeltaMovement()).dot(away.normalize()) > 0.08D;
		if (fleeing) {
			double factor = target instanceof Player ? 0.85D : 0.70D;
			Vec3 movement = target.getDeltaMovement();
			target.setDeltaMovement(movement.x * factor, movement.y, movement.z * factor);
			if (target instanceof Player fleeingPlayer)
				fleeingPlayer.setSprinting(false);
		} else if (target.isBlocking() && !target.getUseItem().isEmpty()) {
			target.getUseItem().hurtAndBreak(2, target, broken -> {
			});
		}
		BeastVfxEntity.spawnAttached(player.serverLevel(), target, BeastVfxEntity.KINGS_MAUL,
				0xFF3540, 0x340005, Mth.clamp(target.getBbWidth() * 1.3F, 1.3F, 3.2F),
				Mth.clamp(target.getBbHeight() * 0.65F, 1.5F, 3.5F), 0.0F, 18, 2);
		player.level().playSound(null, target.blockPosition(), SoundEvents.RAVAGER_ATTACK,
				SoundSource.PLAYERS, 1.15F, 0.62F);
	}

	private static void maulWhiff(ServerPlayer player) {
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE,
				SoundSource.PLAYERS, 0.85F, 0.58F);
		player.displayClientMessage(Component.literal("KING'S MAUL - WHIFF")
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD), true);
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
		player.getPersistentData().putInt(AMBUSH_KIND, 1);
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
		player.getPersistentData().remove(AMBUSH_KIND);
	}

	private static void armHerd(ServerPlayer player, LivingEntity target, long now) {
		player.getPersistentData().putLong(HERD_UNTIL, now + 40L);
		player.getPersistentData().putUUID(HERD_TARGET, target.getUUID());
	}

	private static boolean isHerdReady(ServerPlayer player, LivingEntity target, long now) {
		return now <= player.getPersistentData().getLong(HERD_UNTIL)
				&& player.getPersistentData().hasUUID(HERD_TARGET)
				&& player.getPersistentData().getUUID(HERD_TARGET).equals(target.getUUID());
	}

	private static void consumeHerd(ServerPlayer player) {
		player.getPersistentData().remove(HERD_UNTIL);
		player.getPersistentData().remove(HERD_TARGET);
	}

	private static void applyCadenceMovement(ServerPlayer player, HuntState state, int combo, Vec3 forward) {
		int finisher = isWhiteFangSovereign(player) ? 4 : 3;
		Vec3 step;
		if (combo == 2) {
			double side = Math.abs(player.xxa) > 0.05F ? Math.signum(player.xxa) : state.orbitSide;
			state.orbitSide = -side;
			Vec3 lateral = new Vec3(-forward.z, 0.0D, forward.x).scale(side * 0.24D);
			step = lateral.add(forward.scale(0.08D));
		} else {
			step = forward.scale(combo == 1 ? 0.13D : combo == finisher ? 0.1D : 0.075D);
		}
		if (player.level().noCollision(player, player.getBoundingBox().move(step))) {
			player.setDeltaMovement(player.getDeltaMovement().add(step));
			player.hurtMarked = true;
		}
	}

	private static void applyClawReaction(ServerPlayer player, LivingEntity target, int combo, Vec3 forward) {
		int finisher = isWhiteFangSovereign(player) ? 4 : 3;
		double resistance = Math.max(0.15D, 1.0D - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		double force = (combo == finisher ? 0.38D : combo == 2 ? 0.17D : 0.11D) * resistance;
		Vec3 push = forward.scale(force);
		target.setDeltaMovement(target.getDeltaMovement().add(push.x,
				combo == finisher ? 0.08D * resistance : 0.0D, push.z));
		target.hurtMarked = true;
	}

	private static void spawnClawArc(ServerPlayer player, int combo, boolean hit, boolean ambush) {
		int finisher = isWhiteFangSovereign(player) ? 4 : 3;
		Vec3 direction = player.getLookAngle().normalize();
		Vec3 origin = player.getEyePosition().add(direction.scale(combo == finisher ? 2.25D : 1.9D))
				.add(0.0D, -0.48D, 0.0D);
		float scale = combo == 1 ? 1.85F : combo == 2 ? 2.0F : combo == finisher ? 2.55F : 2.15F;
		float length = combo == finisher ? 2.35F : 1.9F;
		float roll = combo == 1 ? -9.0F : combo == 2 ? 9.0F : 0.0F;
		BeastVfxEntity.spawn(player.serverLevel(), origin, direction, BeastVfxEntity.CLAW,
				ambush ? 0xFF4036 : hit ? 0xED1F2B : 0xB3121B, 0x3A0005,
				scale, length, roll, combo == finisher ? 12 : 10, combo);
	}

	private static void spawnScar(ServerPlayer player, LivingEntity target, int combo, boolean ambush) {
		int finisher = isWhiteFangSovereign(player) ? 4 : 3;
		float width = Mth.clamp(target.getBbWidth() * 1.1F, 0.78F, 2.35F);
		float height = Mth.clamp(target.getBbHeight() * 0.52F, 0.9F, 2.7F);
		BeastVfxEntity.spawnAttached(player.serverLevel(), target, BeastVfxEntity.SCAR,
				ambush ? 0xFF5148 : 0xE51D29, 0x260000, width, height,
				combo == 2 ? 12.0F : combo == finisher ? -5.0F : -14.0F,
				combo == finisher ? 17 : 13, combo);
	}

	private static Vec3 sweptDestination(ServerPlayer player, Vec3 direction, double maximumDistance) {
		Vec3 start = player.position();
		Vec3 normalized = horizontal(direction);
		if (normalized.lengthSqr() < 0.001D)
			normalized = horizontalLook(player);
		normalized = normalized.normalize();
		Vec3 result = start;
		for (double distance = 0.25D; distance <= maximumDistance + 0.001D; distance += 0.25D) {
			Vec3 candidate = start.add(normalized.scale(distance));
			if (!player.serverLevel().hasChunkAt(BlockPos.containing(candidate)))
				break;
			AABB moved = player.getBoundingBox().move(candidate.subtract(start));
			if (!player.level().noCollision(player, moved))
				break;
			result = candidate;
		}
		return result;
	}

	private static int damageAlongPath(ServerPlayer player, Vec3 start, Vec3 end, double radius,
			float damage, int maximumTargets) {
		Vec3 strikeStart = start.add(0.0D, player.getBbHeight() * 0.48D, 0.0D);
		Vec3 strikeEnd = end.add(0.0D, player.getBbHeight() * 0.48D, 0.0D);
		AABB search = player.getBoundingBox().move(start.subtract(player.position()))
				.expandTowards(end.subtract(start)).inflate(radius + 0.8D);
		List<LivingEntity> targets = new ArrayList<>(player.serverLevel().getEntitiesOfClass(
				LivingEntity.class, search, candidate -> validTarget(player, candidate)));
		targets.sort((first, second) -> Double.compare(first.distanceToSqr(player), second.distanceToSqr(player)));
		int hits = 0;
		for (LivingEntity target : targets) {
			if (hits >= maximumTargets || target.getBoundingBox().inflate(radius).clip(strikeStart, strikeEnd).isEmpty())
				continue;
			float applied = target instanceof Player ? damage * 0.72F : damage;
			if (!dealPhysical(player, target, applied))
				continue;
			Vec3 direction = horizontal(end.subtract(start));
			if (direction.lengthSqr() > 0.001D) {
				target.setDeltaMovement(target.getDeltaMovement().add(direction.normalize().scale(0.3D)));
				target.hurtMarked = true;
			}
			if (hits < 4)
				BeastVfxEntity.spawnAttached(player.serverLevel(), target, BeastVfxEntity.SCAR,
						0xFF9A2E, 0x3A0900, Mth.clamp(target.getBbWidth() * 1.2F, 0.95F, 2.8F),
						Mth.clamp(target.getBbHeight() * 0.58F, 1.1F, 3.1F), 0.0F, 15, 3);
			hits++;
		}
		return hits;
	}

	private static LivingEntity forwardTarget(ServerPlayer player, double range, double minimumDot) {
		Vec3 look = player.getLookAngle().normalize();
		AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(range * 0.42D);
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
				candidate -> validTarget(player, candidate))) {
			if (CombatRangeHelper.surfaceDistance(player, target) > range || !player.hasLineOfSight(target))
				continue;
			Vec3 toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
			if (toTarget.lengthSqr() < 0.001D || look.dot(toTarget.normalize()) < minimumDot)
				continue;
			double distance = target.distanceToSqr(player);
			if (distance < bestDistance) {
				best = target;
				bestDistance = distance;
			}
		}
		return best;
	}

	private static void applyWhiteFangSpeed(ServerPlayer player) {
		var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null)
			return;
		speed.removeModifier(WHITE_FANG_SPEED_MODIFIER);
		speed.addTransientModifier(new AttributeModifier(WHITE_FANG_SPEED_MODIFIER,
				"White Fang Sovereign speed", 0.18D, AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	private static void removeWhiteFangSpeed(ServerPlayer player) {
		var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null)
			speed.removeModifier(WHITE_FANG_SPEED_MODIFIER);
	}

	private static boolean validRiftEntry(ServerPlayer player, Vec3 entry) {
		ServerLevel level = player.serverLevel();
		BlockPos pos = BlockPos.containing(entry);
		if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
				|| !level.getFluidState(pos).isEmpty()
				|| !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty())
			return false;
		AABB volume = new AABB(entry.x - 0.72D, entry.y - 1.18D, entry.z - 0.72D,
				entry.x + 0.72D, entry.y + 1.18D, entry.z + 0.72D);
		return level.getEntitiesOfClass(LivingEntity.class, volume,
				living -> living != player && living.isAlive()).isEmpty();
	}

	private static Vec3 safeRiftExit(ServerLevel level, LivingEntity entity, Vec3 portalCenter, Vec3 normal) {
		Vec3 outward = normal.normalize();
		Vec3 base = portalCenter.add(outward.scale(0.72D + entity.getBbWidth() * 0.52D))
				.add(0.0D, -entity.getBbHeight() * 0.5D, 0.0D);
		double[] yOffsets = { 0.0D, 0.5D, 1.0D, 1.5D, -0.5D };
		for (double yOffset : yOffsets) {
			Vec3 candidate = base.add(0.0D, yOffset, 0.0D);
			BlockPos feet = BlockPos.containing(candidate);
			BlockPos head = BlockPos.containing(candidate.add(0.0D, entity.getBbHeight() * 0.82D, 0.0D));
			if (!level.hasChunkAt(feet) || !level.hasChunkAt(head)
					|| !level.getWorldBorder().isWithinBounds(feet)
					|| !level.getFluidState(feet).isEmpty() || !level.getFluidState(head).isEmpty())
				continue;
			AABB moved = entity.getBoundingBox().move(candidate.subtract(entity.position()));
			if (!level.noCollision(entity, moved)
					|| !level.getEntitiesOfClass(LivingEntity.class, moved,
							other -> other != entity && other.isAlive()).isEmpty())
				continue;
			return candidate;
		}
		return null;
	}

	private static boolean canUseRift(LivingEntity entity) {
		return entity.isAlive() && !(entity instanceof ArmorStand) && !entity.isPassenger()
				&& !entity.isVehicle() && entity.getBbWidth() <= 2.25F && entity.getBbHeight() <= 3.2F;
	}

	private static Vec3 rotateMomentum(Vec3 velocity, Vec3 sourceNormal, Vec3 destinationNormal) {
		double angle = horizontalAngle(destinationNormal) - horizontalAngle(sourceNormal);
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double x = velocity.x * cos - velocity.z * sin;
		double z = velocity.x * sin + velocity.z * cos;
		double horizontalSpeed = Math.sqrt(x * x + z * z);
		if (horizontalSpeed > 1.6D) {
			x *= 1.6D / horizontalSpeed;
			z *= 1.6D / horizontalSpeed;
		}
		return new Vec3(x, Mth.clamp(velocity.y, -1.6D, 1.6D), z);
	}

	private static double horizontalAngle(Vec3 direction) {
		return Math.atan2(direction.z, direction.x);
	}

	private static double angleDegrees(Vec3 first, Vec3 second) {
		if (first.lengthSqr() < 0.001D || second.lengthSqr() < 0.001D)
			return 0.0D;
		double dot = Mth.clamp(first.normalize().dot(second.normalize()), -1.0D, 1.0D);
		return Math.toDegrees(Math.acos(dot));
	}

	private static BlockPos findGround(ServerLevel level, BlockPos seed, int range) {
		for (int offset = range; offset >= -range; offset--) {
			BlockPos candidate = seed.offset(0, offset, 0);
			BlockState state = level.getBlockState(candidate);
			BlockPos floor = candidate.below();
			if ((state.isAir() || state.canBeReplaced()) && level.getFluidState(candidate).isEmpty()
					&& level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP))
				return candidate.immutable();
		}
		return null;
	}

	private static List<BlockPos> rubbleJawPositions(ServerLevel level, BlockPos center, Vec3 facing) {
		Vec3 forward = horizontal(facing).normalize();
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		Vec3 origin = Vec3.atCenterOf(center);
		Set<BlockPos> unique = new HashSet<>();
		for (int depth = 0; depth < 4; depth++) {
			double sideDistance = 1.15D + depth * 0.62D;
			Vec3 row = origin.add(forward.scale(-depth * 0.9D));
			for (int side = -1; side <= 1; side += 2) {
				BlockPos projected = BlockPos.containing(row.add(right.scale(side * sideDistance)));
				BlockPos grounded = findGround(level, projected, 2);
				if (grounded != null)
					unique.add(grounded);
			}
		}
		return new ArrayList<>(unique);
	}

	private static boolean canPlaceRubble(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		BlockPos floor = pos.below();
		return level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
				&& (state.isAir() || state.canBeReplaced()) && level.getFluidState(pos).isEmpty()
				&& level.getBlockEntity(pos) == null
				&& level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
				&& level.getEntitiesOfClass(LivingEntity.class, new AABB(pos), LivingEntity::isAlive).isEmpty();
	}

	private static void clearJaw(ServerPlayer player) {
		PENDING_JAWS.remove(player.getUUID());
		JawState jaw = JAWS.remove(player.getUUID());
		if (jaw == null)
			return;
		for (BlockPos pos : jaw.positions) {
			if (jaw.level.getBlockState(pos).is(SololevelingModBlocks.BEAST_RUBBLE.get()))
				jaw.level.removeBlock(pos, false);
		}
	}

	private static boolean insideOpenFlank(ServerPlayer player, LivingEntity target) {
		Vec3 heading = horizontal(target.getDeltaMovement());
		if (heading.lengthSqr() < 0.015D)
			heading = horizontal(target.getLookAngle());
		if (heading.lengthSqr() < 0.001D)
			return true;
		Vec3 open = heading.normalize().scale(-1.0D);
		Vec3 attacker = horizontal(player.position().subtract(target.position()));
		return attacker.lengthSqr() > 0.001D && attacker.normalize().dot(open) >= Math.cos(Math.toRadians(50.0D));
	}

	private static double distanceFromLine(Vec3 point, Vec3 origin, Vec3 direction) {
		Vec3 normalized = direction.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
		Vec3 delta = point.subtract(origin);
		double along = Math.max(0.0D, delta.dot(normalized));
		return delta.subtract(normalized.scale(along)).length();
	}

	private static double eligibleWounds(ServerPlayer player, LivingEntity quarry, long now) {
		if (quarry == null)
			return 0.0D;
		Deque<Wound> wounds = WOUNDS.get(player.getUUID());
		if (wounds == null)
			return 0.0D;
		while (!wounds.isEmpty() && now - wounds.peekFirst().tick > 80L)
			wounds.removeFirst();
		return wounds.stream().filter(wound -> wound.source.equals(quarry.getUUID()))
				.mapToDouble(wound -> wound.amount).sum();
	}

	private static boolean canCastBeastSkill(ServerPlayer player) {
		return player.isAlive() && isBeastVessel(player) && !isActionLocked(player);
	}

	private static boolean isActionLocked(Entity entity) {
		return entity != null && (RECONSTITUTIONS.containsKey(entity.getUUID())
				|| SOVEREIGN_CHANNELS.containsKey(entity.getUUID())
				|| INTERCEPTS.containsKey(entity.getUUID()) || CHARGES.containsKey(entity.getUUID()));
	}

	private static boolean canAffordMana(ServerPlayer player, int amount) {
		return player.isCreative() || variables(player).MP >= amount;
	}

	private static void fail(ServerPlayer player, String message) {
		player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), true);
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
		boolean directlyProvoked = wasDirectlyProvoked(player, target);
		if ((player.isAlliedTo(target) || target.isAlliedTo(player)) && !directlyProvoked)
			return false;
		if (ShadowMonarchManager.isOwnedShadow(target, player))
			return false;
		if (target instanceof TamableAnimal tame && player.getUUID().equals(tame.getOwnerUUID()))
			return false;
		if (target instanceof Player other)
			return !other.isCreative() && !other.isSpectator() && player.canHarmPlayer(other);
		return true;
	}

	private static void markDirectlyProvoked(ServerPlayer player, LivingEntity target) {
		target.getPersistentData().putLong(PROVOKED_BY_PREFIX + player.getUUID(),
				target.level().getGameTime() + PROVOKED_DURATION);
	}

	private static boolean wasDirectlyProvoked(Player player, LivingEntity target) {
		return player.getLastHurtMob() == target || target.getLastHurtByMob() == player
				|| target.getPersistentData().getLong(PROVOKED_BY_PREFIX + player.getUUID()) >= target.level().getGameTime();
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
		return ready(player, key, "Predator's Intercept");
	}

	private static boolean ready(ServerPlayer player, String key, String displayName) {
		if (!CooldownManager.isOnCooldown(player, key))
			return true;
		player.displayClientMessage(Component.literal(displayName + ": "
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
		clearJaw(player);
		HUNTS.remove(player.getUUID());
		CHARGES.remove(player.getUUID());
		INTERCEPTS.remove(player.getUUID());
		RIFTS.remove(player.getUUID());
		PENDING_JAWS.remove(player.getUUID());
		WOUNDS.remove(player.getUUID());
		RECONSTITUTIONS.remove(player.getUUID());
		SOVEREIGN_CHANNELS.remove(player.getUUID());
		if (SOVEREIGNS.containsKey(player.getUUID()))
			endSovereign(player, true);
		consumeAmbush(player);
		consumeHerd(player);
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
		RIFT_AMBUSH(1 << 2, 16, "RIFT AMBUSH"),
		HERD(1 << 3, 16, "HERD"),
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
		private long satedUntil;
		private int lastSyncSignature = Integer.MIN_VALUE;
	}

	private static final class RiftState {
		private final UUID castId;
		private final Vec3 entry;
		private final Vec3 entryNormal;
		private final Vec3 exit;
		private final Vec3 exitNormal;
		private final long armedAt;
		private final long expiresAt;
		private final boolean twoWay;
		private final Map<UUID, Double> forwardSides = new HashMap<>();
		private final Map<UUID, Double> reverseSides = new HashMap<>();
		private final Set<UUID> usedForward = new HashSet<>();
		private final Set<UUID> usedReverse = new HashSet<>();
		private final Map<UUID, Long> immunityUntil = new HashMap<>();

		private RiftState(UUID castId, Vec3 entry, Vec3 entryNormal, Vec3 exit, Vec3 exitNormal,
				long armedAt, long expiresAt, boolean twoWay) {
			this.castId = castId;
			this.entry = entry;
			this.entryNormal = entryNormal;
			this.exit = exit;
			this.exitNormal = exitNormal;
			this.armedAt = armedAt;
			this.expiresAt = expiresAt;
			this.twoWay = twoWay;
		}
	}

	private static final class JawState {
		private final UUID castId;
		private final ServerLevel level;
		private final List<BlockPos> positions;
		private final Vec3 mouthCenter;
		private final Vec3 facing;
		private final long expiresAt;
		private final Set<BlockPos> phased = new HashSet<>();
		private Vec3 lastHeading;
		private double lastMouthSide = Double.NaN;

		private JawState(UUID castId, ServerLevel level, List<BlockPos> positions, Vec3 mouthCenter,
				Vec3 facing, long expiresAt) {
			this.castId = castId;
			this.level = level;
			this.positions = positions;
			this.mouthCenter = mouthCenter;
			this.facing = facing;
			this.expiresAt = expiresAt;
		}
	}

	private record Wound(UUID source, double amount, long tick) {
	}

	private record ReconstitutionState(UUID quarryId, double woundAmount, boolean pvp,
			long finishesAt, boolean shedding) {
	}

	private record SovereignChannel(long finishesAt) {
	}

	private static final class SovereignState {
		private long expiresAt;
		private final long hardExpiresAt;
		private final Set<Feat> extendedFeats = new HashSet<>();

		private SovereignState(long expiresAt, long hardExpiresAt) {
			this.expiresAt = expiresAt;
			this.hardExpiresAt = hardExpiresAt;
		}
	}

	private record InterceptCharge(UUID targetId, long startedAt, Vec3 start, double startingDistance) {
	}

	private record InterceptState(UUID targetId, Vec3 direction, Vec3 destination, Vec3 start,
			double speed, double maximumDistance, long expiresAt) {
	}
}
