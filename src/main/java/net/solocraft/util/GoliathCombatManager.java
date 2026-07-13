package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public final class GoliathCombatManager {
	public static final String CAPTURE = "Capture";
	public static final String POWER_SMASH = "Power Smash";
	public static final String COLLAPSE = "Collapse";

	private static final String IDENTITY = "thomas_andre";
	private static final String NEXT_STRIKE = "goliath_next_strike";
	private static final String LAST_STRIKE = "goliath_last_strike";
	private static final String COMBO = "goliath_combo";
	private static final String FALL_SAFE_UNTIL = "goliath_fall_safe_until";
	private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.66F, 0.08F), 1.35F);
	private static final DustParticleOptions PALE_GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.48F), 0.9F);
	private static final Map<UUID, ChargeState> CHARGES = new HashMap<>();
	private static final Map<UUID, PursuitState> PURSUITS = new HashMap<>();
	private static final Map<UUID, CaptureState> CAPTURES = new HashMap<>();

	private GoliathCombatManager() {
	}

	public static boolean isGoliathVessel(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = variables(entity);
		return vars.JOB == 5 && (IDENTITY.equals(vars.vesselIdentity) || vars.vesselIdentity.isBlank());
	}

	public static boolean isCombatStance(Entity entity) {
		return isGoliathVessel(entity) && variables(entity).combatmode;
	}

	public static boolean isManifested(Entity entity) {
		if (!(entity instanceof LivingEntity living))
			return false;
		return living.getItemBySlot(EquipmentSlot.HEAD).is(SololevelingModItems.GOLIATH_ARMOR_HELMET.get())
				&& living.getItemBySlot(EquipmentSlot.CHEST).is(SololevelingModItems.GOLIATH_ARMOR_CHESTPLATE.get())
				&& living.getItemBySlot(EquipmentSlot.LEGS).is(SololevelingModItems.GOLIATH_ARMOR_LEGGINGS.get())
				&& living.getItemBySlot(EquipmentSlot.FEET).is(SololevelingModItems.GOLIATH_ARMOR_BOOTS.get());
	}

	public static void enhancedStrike(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer) || !isCombatStance(player) || !player.isAlive())
			return;
		long now = player.level().getGameTime();
		if (now < player.getPersistentData().getLong(NEXT_STRIKE))
			return;
		boolean manifested = isManifested(player);
		long last = player.getPersistentData().getLong(LAST_STRIKE);
		int combo = now - last <= 18 ? player.getPersistentData().getInt(COMBO) % 3 + 1 : 1;
		int mana = switch (combo) {
			case 2 -> manifested ? 80 : 55;
			case 3 -> manifested ? 120 : 85;
			default -> manifested ? 65 : 45;
		};
		if (!consumeMana(serverPlayer, mana))
			return;
		player.getPersistentData().putLong(NEXT_STRIKE, now + (manifested ? 4 : 5));
		player.getPersistentData().putLong(LAST_STRIKE, now);
		player.getPersistentData().putInt(COMBO, combo);
		player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

		double strength = variables(player).Strength;
		float damage = (float) ((12.0D + strength / 8.0D) * (combo == 1 ? 1.0D : combo == 2 ? 1.22D : 1.68D) * (manifested ? 1.55D : 1.0D));
		double radius = combo == 1 ? 3.3D : combo == 2 ? 4.2D : 5.6D;
		Vec3 forward = horizontalLook(player);
		Vec3 center = combo == 3 ? player.position() : player.position().add(forward.scale(combo == 1 ? 2.3D : 1.8D));
		List<LivingEntity> targets = targets(serverPlayer, new AABB(center, center).inflate(radius, 2.4D, radius));
		for (LivingEntity target : targets) {
			Vec3 toward = target.position().subtract(player.position());
			if (combo != 3 && toward.lengthSqr() > 0.001D && horizontal(toward).normalize().dot(forward) < (combo == 1 ? 0.18D : -0.2D))
				continue;
			if (dealPhysical(serverPlayer, target, damage)) {
				double force = combo == 3 ? 1.25D : combo == 2 ? 0.85D : 1.05D;
				pushAway(target, player.position(), force * targetResistance(target), combo == 3 ? 0.5D : 0.2D);
			}
		}
		strikeVfx(serverPlayer.serverLevel(), center, forward, combo, radius, manifested);
	}

	public static void castCapture(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isGoliathVessel(player))
			return;
		CaptureState active = CAPTURES.get(player.getUUID());
		if (active != null && active.manifested) {
			finishCapture(player, active, true);
			return;
		}
		if (!ready(player, CAPTURE))
			return;
		boolean manifested = isManifested(player);
		int mana = manifested ? 520 : 330;
		if (!consumeMana(player, mana))
			return;
		double radius = manifested ? 15.0D : 10.0D;
		Vec3 center = player.position().add(horizontalLook(player).scale(manifested ? 7.0D : 5.0D));
		List<LivingEntity> found = targets(player, new AABB(center, center).inflate(radius, 7.0D, radius));
		found.removeIf(target -> !player.hasLineOfSight(target));
		found.sort(Comparator.comparingDouble(player::distanceToSqr));
		if (found.size() > (manifested ? 24 : 14))
			found = new ArrayList<>(found.subList(0, manifested ? 24 : 14));
		List<UUID> ids = found.stream().map(Entity::getUUID).toList();
		CAPTURES.put(player.getUUID(), new CaptureState(ids, player.level().getGameTime() + (manifested ? 28 : 16), manifested));
		CooldownManager.set(player, CAPTURE, manifested ? 150 : 100);
		player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.1F, manifested ? 0.55F : 0.72F);
		waveRing(player.serverLevel(), center, manifested ? 8.0D : 5.0D, 24, 0.3D);
	}

	public static void castPowerSmash(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isGoliathVessel(player) || !ready(player, POWER_SMASH))
			return;
		boolean manifested = isManifested(player);
		if (!consumeMana(player, manifested ? 620 : 390))
			return;
		CooldownManager.set(player, POWER_SMASH, manifested ? 125 : 85);
		Vec3 forward = horizontalLook(player);
		double reach = manifested ? 10.0D : 6.5D;
		double width = manifested ? 5.2D : 3.5D;
		Vec3 center = player.position().add(forward.scale(reach * 0.52D));
		List<LivingEntity> found = targets(player, new AABB(center, center).inflate(width, 2.7D, width));
		found.removeIf(target -> horizontal(target.position().subtract(player.position())).normalize().dot(forward) < 0.42D);
		found.sort(Comparator.comparingDouble(player::distanceToSqr));
		double strength = variables(player).Strength;
		float primary = (float) ((31.0D + strength / 3.8D) * (manifested ? 1.55D : 1.0D));
		for (int i = 0; i < found.size(); i++) {
			LivingEntity target = found.get(i);
			float damage = i == 0 ? primary : primary * (manifested ? 0.68F : 0.42F);
			if (dealPhysical(player, target, damage)) {
				pushAway(target, player.position(), (manifested ? 2.4D : 1.7D) * targetResistance(target), 0.35D);
				target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, manifested ? 100 : 55, manifested ? 2 : 1, false, false));
			}
		}
		player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
		player.setDeltaMovement(player.getDeltaMovement().add(forward.scale(manifested ? 0.75D : 0.45D)));
		player.hurtMarked = true;
		powerSmashVfx(player.serverLevel(), player.position().add(0, 1.1D, 0), forward, reach, manifested);
	}

	public static void castCollapse(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isGoliathVessel(player) || !ready(player, COLLAPSE))
			return;
		boolean manifested = isManifested(player);
		if (!consumeMana(player, manifested ? 850 : 540))
			return;
		CooldownManager.set(player, COLLAPSE, manifested ? 220 : 150);
		player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
		collapseImpact(player, manifested, false);
		if (manifested) {
			for (LivingEntity target : targets(player, player.getBoundingBox().inflate(11.0D, 5.0D, 11.0D))) {
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 18, 9, false, false));
				target.setDeltaMovement(target.getDeltaMovement().scale(0.12D));
				target.hurtMarked = true;
			}
			SololevelingMod.queueServerWork(8, () -> {
				if (player.isAlive() && isManifested(player))
					collapseImpact(player, true, true);
			});
		}
	}

	public static void beginPursuit(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isCombatStance(player) || PURSUITS.containsKey(player.getUUID()))
			return;
		if (!ready(player, "goliath_pursuit"))
			return;
		LivingEntity target = crosshairTarget(player, 46.0D);
		CHARGES.put(player.getUUID(), new ChargeState(target == null ? null : target.getUUID(), player.level().getGameTime()));
		if (target != null) {
			player.displayClientMessage(Component.literal("\u00A76GOLIATH PURSUIT \u00A77- \u00A7fTARGET LOCKED"), true);
			targetMarker(player.serverLevel(), target);
		} else {
			player.displayClientMessage(Component.literal("\u00A76GOLIATH LAUNCH \u00A77- \u00A7fNO TARGET"), true);
		}
	}

	public static void releasePursuit(Entity entity, int pressedMs) {
		if (!(entity instanceof ServerPlayer player) || !isCombatStance(player))
			return;
		ChargeState charge = CHARGES.remove(player.getUUID());
		if (charge == null || !ready(player, "goliath_pursuit"))
			return;
		LivingEntity target = charge.targetId == null ? null : livingEntity(player.serverLevel(), charge.targetId);
		if (target != null && (!validTarget(player, target) || player.distanceToSqr(target) > 55.0D * 55.0D))
			target = null;
		boolean manifested = isManifested(player);
		int mana = target == null ? (manifested ? 260 : 160) : (manifested ? 430 : 280);
		if (!consumeMana(player, mana))
			return;
		double chargePower = Mth.clamp(0.45D + pressedMs / 900.0D, 0.55D, 1.35D);
		Vec3 direction = player.getLookAngle().normalize();
		PursuitState state = new PursuitState(target == null ? null : target.getUUID(), direction, manifested, chargePower,
				player.level().getGameTime(), target == null ? 55 : 22);
		PURSUITS.put(player.getUUID(), state);
		CooldownManager.set(player, "goliath_pursuit", manifested ? 70 : 55);
		player.getPersistentData().putLong(FALL_SAFE_UNTIL, player.level().getGameTime() + 90);
		Vec3 initial = target == null ? direction.scale((manifested ? 2.35D : 1.95D) * chargePower).add(0, Math.max(0.12D, direction.y * 0.35D), 0)
				: directionTo(player, target).scale((manifested ? 2.75D : 2.25D) * chargePower);
		breakDashBlocks(player, initial, manifested);
		player.setDeltaMovement(initial);
		player.hurtMarked = true;
		player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, manifested ? 1.2F : 0.9F, manifested ? 0.62F : 0.82F);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.START || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player))
			return;
		if (!player.isAlive() || !isGoliathVessel(player)) {
			clearState(player);
			return;
		}
		tickCharge(player);
		tickCapture(player);
		tickPursuit(player);
	}

	@SubscribeEvent
	public static void onAttackEntity(AttackEntityEvent event) {
		if (isCombatStance(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		if (isCombatStance(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onFall(LivingFallEvent event) {
		if (event.getEntity() instanceof Player player && player.getPersistentData().getLong(FALL_SAFE_UNTIL) >= player.level().getGameTime()) {
			event.setCanceled(true);
			player.fallDistance = 0.0F;
		}
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		clearState(event.getEntity());
	}

	private static void tickCharge(ServerPlayer player) {
		ChargeState charge = CHARGES.get(player.getUUID());
		if (charge == null)
			return;
		if (player.level().getGameTime() - charge.startedAt > 80) {
			CHARGES.remove(player.getUUID());
			return;
		}
		if (charge.targetId != null && player.tickCount % 3 == 0) {
			LivingEntity target = livingEntity(player.serverLevel(), charge.targetId);
			if (target != null && validTarget(player, target))
				targetMarker(player.serverLevel(), target);
		}
	}

	private static void tickCapture(ServerPlayer player) {
		CaptureState state = CAPTURES.get(player.getUUID());
		if (state == null)
			return;
		if (player.level().getGameTime() >= state.endTick) {
			finishCapture(player, state, false);
			return;
		}
		Vec3 anchor = player.position().add(horizontalLook(player).scale(state.manifested ? 3.2D : 2.5D)).add(0, state.manifested ? 1.8D : 1.0D, 0);
		for (UUID id : state.targetIds) {
			LivingEntity target = livingEntity(player.serverLevel(), id);
			if (target == null || !validTarget(player, target))
				continue;
			Vec3 pull = anchor.subtract(target.getBoundingBox().getCenter());
			double resistance = targetResistance(target);
			double speed = Math.min(state.manifested ? 1.55D : 1.05D, 0.18D + pull.length() * 0.115D) * resistance;
			if (pull.lengthSqr() > 0.04D)
				target.setDeltaMovement(pull.normalize().scale(speed));
			if (state.manifested && pull.lengthSqr() < 9.0D)
				target.setDeltaMovement(target.getDeltaMovement().scale(0.2D).add(0, 0.035D, 0));
			target.fallDistance = 0.0F;
			target.hurtMarked = true;
		}
		if (player.tickCount % 2 == 0)
			captureVfx(player.serverLevel(), anchor, state.manifested);
	}

	private static void finishCapture(ServerPlayer player, CaptureState state, boolean thrown) {
		CAPTURES.remove(player.getUUID());
		Vec3 look = player.getLookAngle().normalize();
		float damage = physicalDamage(player, state.manifested ? (thrown ? 28.0D : 22.0D) : 12.0D,
				state.manifested ? 5.5D : 10.0D);
		for (UUID id : state.targetIds) {
			LivingEntity target = livingEntity(player.serverLevel(), id);
			if (target == null || !validTarget(player, target))
				continue;
			dealPhysical(player, target, damage);
			Vec3 launch = thrown ? look.scale(2.2D * targetResistance(target)).add(0, 0.35D, 0)
					: target.position().subtract(player.position()).normalize().scale(0.7D * targetResistance(target)).add(0, 0.45D, 0);
			target.setDeltaMovement(launch);
			target.hurtMarked = true;
		}
		Vec3 center = player.position().add(horizontalLook(player).scale(2.7D)).add(0, 1.0D, 0);
		waveRing(player.serverLevel(), center, state.manifested ? 7.0D : 4.0D, state.manifested ? 32 : 18, 0.1D);
		player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, state.manifested ? 1.4F : 0.8F, thrown ? 0.65F : 0.9F);
	}

	private static void tickPursuit(ServerPlayer player) {
		PursuitState state = PURSUITS.get(player.getUUID());
		if (state == null)
			return;
		long elapsed = player.level().getGameTime() - state.startedAt;
		LivingEntity target = state.targetId == null ? null : livingEntity(player.serverLevel(), state.targetId);
		if (state.targetId != null && (target == null || !validTarget(player, target))) {
			finishPursuit(player, state, null, false);
			return;
		}
		if (target != null) {
			suspendTarget(target);
			if (CombatRangeHelper.withinSurfaceRange(player, target, 1.45D)) {
				finishPursuit(player, state, target, true);
				return;
			}
			Vec3 motion = directionTo(player, target).scale((state.manifested ? 2.75D : 2.25D) * state.power);
			if (!player.serverLevel().hasChunkAt(BlockPos.containing(player.position().add(motion)))) {
				finishPursuit(player, state, target, false);
				return;
			}
			breakDashBlocks(player, motion, state.manifested);
			if (player.horizontalCollision && !player.serverLevel().noCollision(player, player.getBoundingBox().move(motion.normalize().scale(0.45D)))) {
				finishPursuit(player, state, null, true);
				return;
			}
			player.setDeltaMovement(motion);
		} else {
			Vec3 motion = player.getDeltaMovement();
			if (motion.lengthSqr() < 0.08D)
				motion = state.direction.scale((state.manifested ? 2.25D : 1.85D) * state.power).add(0, -0.08D, 0);
			breakDashBlocks(player, motion, state.manifested);
			boolean blocked = player.horizontalCollision && !player.serverLevel().noCollision(player, player.getBoundingBox().move(motion.normalize().scale(0.45D)));
			if ((elapsed > 2 && (player.onGround() || blocked)) || elapsed >= state.maxTicks) {
				finishPursuit(player, state, null, true);
				return;
			}
		}
		player.fallDistance = 0.0F;
		player.hurtMarked = true;
		damageDashPath(player, state);
		dashTrail(player.serverLevel(), player, state.manifested);
		if (elapsed >= state.maxTicks)
			finishPursuit(player, state, target, target != null);
	}

	private static void finishPursuit(ServerPlayer player, PursuitState state, LivingEntity target, boolean impact) {
		PURSUITS.remove(player.getUUID());
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		player.fallDistance = 0.0F;
		if (!impact)
			return;
		Vec3 center = target == null ? player.position() : target.position();
		double radius = state.manifested ? 8.5D : 6.0D;
		float damage = physicalDamage(player, state.manifested ? 34.0D : 21.0D, state.manifested ? 3.7D : 5.5D);
		for (LivingEntity nearby : targets(player, new AABB(center, center).inflate(radius, 4.0D, radius))) {
			float dealt = nearby == target ? damage * 1.45F : damage;
			if (dealPhysical(player, nearby, dealt))
				pushAway(nearby, center, (state.manifested ? 2.1D : 1.35D) * targetResistance(nearby), 0.7D);
		}
		impactVfx(player.serverLevel(), center, radius, state.manifested);
	}

	private static void damageDashPath(ServerPlayer player, PursuitState state) {
		AABB area = player.getBoundingBox().inflate(state.manifested ? 1.45D : 0.9D);
		float damage = physicalDamage(player, state.manifested ? 12.0D : 7.0D, 12.0D);
		for (LivingEntity target : targets(player, area)) {
			if (!state.hitIds.add(target.getUUID()))
				continue;
			if (dealPhysical(player, target, damage))
				pushAway(target, player.position(), 1.15D * targetResistance(target), 0.25D);
		}
	}

	private static void suspendTarget(LivingEntity target) {
		double resistance = targetResistance(target);
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, resistance < 0.55D ? 4 : 15, false, false));
		Vec3 movement = target.getDeltaMovement();
		target.setDeltaMovement(movement.x * (1.0D - 0.9D * resistance), Math.max(0.025D, movement.y * 0.1D), movement.z * (1.0D - 0.9D * resistance));
		target.fallDistance = 0.0F;
		target.hurtMarked = true;
	}

	private static void collapseImpact(ServerPlayer player, boolean manifested, boolean secondWave) {
		double radius = manifested ? (secondWave ? 12.0D : 10.0D) : 7.0D;
		float damage = physicalDamage(player, manifested ? (secondWave ? 30.0D : 24.0D) : 18.0D,
				manifested ? (secondWave ? 4.6D : 5.2D) : 7.0D);
		Vec3 center = player.position();
		for (LivingEntity target : targets(player, player.getBoundingBox().inflate(radius, 4.5D, radius))) {
			if (dealPhysical(player, target, damage)) {
				double force = (manifested ? 1.65D : 1.05D) * targetResistance(target);
				pushAway(target, center, force, secondWave ? 1.0D : 0.65D);
				if (manifested)
					target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, secondWave ? 100 : 60, secondWave ? 2 : 1, false, false));
			}
		}
		impactVfx(player.serverLevel(), center, radius, manifested);
		player.level().playSound(null, player.blockPosition(), secondWave ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.GENERIC_EXPLODE,
				SoundSource.PLAYERS, manifested ? 1.5F : 1.0F, secondWave ? 0.55F : 0.72F);
	}

	private static void breakDashBlocks(ServerPlayer player, Vec3 motion, boolean manifested) {
		ServerLevel level = player.serverLevel();
		double radius = manifested ? 1.05D : 0.65D;
		AABB swept = player.getBoundingBox().expandTowards(motion).inflate(radius, 0.05D, radius);
		swept = new AABB(swept.minX, swept.minY + 0.16D, swept.minZ, swept.maxX, swept.maxY - 0.08D, swept.maxZ);
		BlockPos min = BlockPos.containing(swept.minX, swept.minY, swept.minZ);
		BlockPos max = BlockPos.containing(swept.maxX, swept.maxY, swept.maxZ);
		int budget = manifested ? 36 : 22;
		int broken = 0;
		for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
			if (broken >= budget)
				break;
			BlockPos pos = cursor.immutable();
			if (!canBreakDashBlock(level, player, pos))
				continue;
			BlockState state = level.getBlockState(pos);
			if (level.destroyBlock(pos, false, player)) {
				broken++;
				if (broken <= 6)
					level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 5, 0.3D, 0.3D, 0.3D, 0.08D);
			}
		}
	}

	private static boolean canBreakDashBlock(ServerLevel level, ServerPlayer player, BlockPos pos) {
		if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, ItemStack.EMPTY) || level.getBlockEntity(pos) != null)
			return false;
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || !state.getFluidState().isEmpty() || state.is(BlockTags.WITHER_IMMUNE) || state.getDestroySpeed(level, pos) < 0.0F)
			return false;
		if (state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY) || state.is(Blocks.BARRIER)
				|| state.is(Blocks.STRUCTURE_BLOCK) || state.is(Blocks.JIGSAW) || state.is(Blocks.COMMAND_BLOCK)
				|| state.is(Blocks.CHAIN_COMMAND_BLOCK) || state.is(Blocks.REPEATING_COMMAND_BLOCK))
			return false;
		return ForgeHooks.canEntityDestroy(level, pos, player);
	}

	private static LivingEntity crosshairTarget(ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.5D);
		LivingEntity best = null;
		double bestAlong = Double.MAX_VALUE;
		for (LivingEntity target : targets(player, search)) {
			Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
			double along = to.dot(look);
			if (along <= 0.0D || along > range)
				continue;
			double perpendicular = to.subtract(look.scale(along)).length();
			double allowance = 1.15D + target.getBbWidth() * 0.5D;
			if (perpendicular <= allowance && along < bestAlong && player.hasLineOfSight(target)) {
				best = target;
				bestAlong = along;
			}
		}
		return best;
	}

	private static List<LivingEntity> targets(ServerPlayer player, AABB area) {
		return player.serverLevel().getEntitiesOfClass(LivingEntity.class, area, target -> validTarget(player, target));
	}

	private static boolean validTarget(Player player, LivingEntity target) {
		if (target == null || target == player || !target.isAlive() || !target.isAttackable() || target.isInvulnerable() || target instanceof ArmorStand)
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
		return target.hurt(player.damageSources().playerAttack(player), Math.max(0.5F, damage));
	}

	private static float physicalDamage(ServerPlayer player, double base, double strengthDivisor) {
		return (float) (base + variables(player).Strength / strengthDivisor);
	}

	private static boolean consumeMana(ServerPlayer player, int amount) {
		if (player.isCreative())
			return true;
		SololevelingModVariables.PlayerVariables vars = variables(player);
		if (vars.MP < amount) {
			player.displayClientMessage(Component.literal("\u00A7cNot enough MP! \u00A77(" + amount + " required)"), true);
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
		player.displayClientMessage(Component.literal("\u00A7c" + key + " is on cooldown."), true);
		return false;
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static LivingEntity livingEntity(ServerLevel level, UUID id) {
		Entity entity = level.getEntity(id);
		return entity instanceof LivingEntity living ? living : null;
	}

	private static Vec3 horizontalLook(Entity entity) {
		Vec3 look = horizontal(entity.getLookAngle());
		return look.lengthSqr() < 0.001D ? new Vec3(0, 0, 1) : look.normalize();
	}

	private static Vec3 horizontal(Vec3 vector) {
		return new Vec3(vector.x, 0, vector.z);
	}

	private static Vec3 directionTo(Entity source, LivingEntity target) {
		Vec3 direction = target.getBoundingBox().getCenter().subtract(source.getBoundingBox().getCenter());
		return direction.lengthSqr() < 0.001D ? source.getLookAngle().normalize() : direction.normalize();
	}

	private static double targetResistance(LivingEntity target) {
		if (target.getMaxHealth() >= 500.0F || target.getBbWidth() >= 4.0F)
			return 0.28D;
		if (target.getMaxHealth() >= 220.0F || target.getBbWidth() >= 2.4F)
			return 0.5D;
		return 1.0D;
	}

	private static void pushAway(LivingEntity target, Vec3 origin, double horizontal, double vertical) {
		Vec3 direction = horizontal(target.position().subtract(origin));
		if (direction.lengthSqr() < 0.001D)
			direction = new Vec3(0, 0, 1);
		direction = direction.normalize();
		target.setDeltaMovement(direction.x * horizontal, Math.max(target.getDeltaMovement().y, vertical), direction.z * horizontal);
		target.hurtMarked = true;
	}

	private static void strikeVfx(ServerLevel level, Vec3 center, Vec3 forward, int combo, double radius, boolean manifested) {
		int points = combo == 3 ? 24 : 14;
		if (combo == 3)
			waveRing(level, center.add(0, 0.18D, 0), radius, points, 0.08D);
		else {
			for (int i = 1; i <= points; i++) {
				double distance = radius * i / points;
				Vec3 pos = center.add(forward.scale(distance - radius * 0.5D));
				level.sendParticles(i % 3 == 0 ? PALE_GOLD : GOLD, pos.x, pos.y + 1.0D, pos.z, 1, 0.12D, 0.12D, 0.12D, 0.02D);
			}
		}
		level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.8D, center.z, manifested ? 18 : 10, radius * 0.18D, 0.4D, radius * 0.18D, 0.08D);
		level.playSound(null, BlockPos.containing(center), combo == 3 ? SoundEvents.ANVIL_LAND : SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS,
				manifested ? 1.25F : 0.85F, combo == 3 ? 0.58F : 0.82F + combo * 0.06F);
	}

	private static void powerSmashVfx(ServerLevel level, Vec3 origin, Vec3 forward, double reach, boolean manifested) {
		Vec3 right = new Vec3(-forward.z, 0, forward.x);
		for (int i = 0; i < (manifested ? 34 : 22); i++) {
			double progress = i / (double) (manifested ? 33 : 21);
			double spread = Math.sin(progress * Math.PI) * (manifested ? 2.2D : 1.25D);
			Vec3 pos = origin.add(forward.scale(progress * reach)).add(right.scale((i % 2 == 0 ? 1 : -1) * spread));
			level.sendParticles(i % 4 == 0 ? ParticleTypes.END_ROD : GOLD, pos.x, pos.y + Math.sin(progress * Math.PI) * 0.6D, pos.z, 1, 0.04D, 0.04D, 0.04D, 0.01D);
		}
		level.sendParticles(ParticleTypes.EXPLOSION, origin.x + forward.x * reach, origin.y, origin.z + forward.z * reach, manifested ? 4 : 2, 0.5D, 0.5D, 0.5D, 0.0D);
		level.playSound(null, BlockPos.containing(origin), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, manifested ? 1.45F : 1.0F, manifested ? 0.55F : 0.78F);
	}

	private static void captureVfx(ServerLevel level, Vec3 anchor, boolean manifested) {
		int points = manifested ? 10 : 6;
		for (int i = 0; i < points; i++) {
			double angle = (level.getGameTime() * 0.34D + i * Math.PI * 2.0D / points);
			double radius = manifested ? 2.7D : 1.7D;
			Vec3 pos = anchor.add(Math.cos(angle) * radius, Math.sin(angle * 1.7D) * 1.2D, Math.sin(angle) * radius);
			level.sendParticles(i % 3 == 0 ? PALE_GOLD : GOLD, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
		}
	}

	private static void targetMarker(ServerLevel level, LivingEntity target) {
		Vec3 center = target.getBoundingBox().getCenter();
		double radius = Math.max(0.8D, target.getBbWidth() * 0.75D);
		for (int i = 0; i < 10; i++) {
			double angle = i * Math.PI * 2.0D / 10.0D + level.getGameTime() * 0.18D;
			level.sendParticles(i % 2 == 0 ? GOLD : PALE_GOLD, center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
		}
	}

	private static void dashTrail(ServerLevel level, Player player, boolean manifested) {
		Vec3 pos = player.position().add(0, player.getBbHeight() * 0.55D, 0);
		level.sendParticles(GOLD, pos.x, pos.y, pos.z, manifested ? 12 : 7, player.getBbWidth() * 0.45D, player.getBbHeight() * 0.3D, player.getBbWidth() * 0.45D, 0.02D);
		level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y - 0.3D, pos.z, manifested ? 7 : 4, 0.2D, 0.2D, 0.2D, 0.04D);
	}

	private static void impactVfx(ServerLevel level, Vec3 center, double radius, boolean manifested) {
		waveRing(level, center.add(0, 0.15D, 0), radius, manifested ? 36 : 24, 0.12D);
		waveRing(level, center.add(0, 0.45D, 0), radius * 0.68D, manifested ? 28 : 18, 0.18D);
		level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.8D, center.z, manifested ? 8 : 4, radius * 0.22D, 0.7D, radius * 0.22D, 0.0D);
		level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.4D, center.z, manifested ? 55 : 30, radius * 0.35D, 0.7D, radius * 0.35D, 0.12D);
		level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, manifested ? 1.7F : 1.15F, manifested ? 0.52F : 0.7F);
	}

	private static void waveRing(ServerLevel level, Vec3 center, double radius, int points, double yWave) {
		for (int i = 0; i < points; i++) {
			double angle = i * Math.PI * 2.0D / points;
			Vec3 pos = center.add(Math.cos(angle) * radius, Math.sin(angle * 3.0D) * yWave, Math.sin(angle) * radius);
			level.sendParticles(i % 4 == 0 ? PALE_GOLD : GOLD, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
		}
	}

	private static void clearState(Player player) {
		UUID id = player.getUUID();
		CHARGES.remove(id);
		PURSUITS.remove(id);
		CAPTURES.remove(id);
		player.getPersistentData().remove(FALL_SAFE_UNTIL);
	}

	private record ChargeState(UUID targetId, long startedAt) {
	}

	private record CaptureState(List<UUID> targetIds, long endTick, boolean manifested) {
	}

	private static final class PursuitState {
		private final UUID targetId;
		private final Vec3 direction;
		private final boolean manifested;
		private final double power;
		private final long startedAt;
		private final int maxTicks;
		private final Set<UUID> hitIds = new HashSet<>();

		private PursuitState(UUID targetId, Vec3 direction, boolean manifested, double power, long startedAt, int maxTicks) {
			this.targetId = targetId;
			this.direction = direction;
			this.manifested = manifested;
			this.power = power;
			this.startedAt = startedAt;
			this.maxTicks = maxTicks;
		}
	}
}
