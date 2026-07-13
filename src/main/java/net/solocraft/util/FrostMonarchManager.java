package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.block.FrostCausewayBlock;
import net.solocraft.init.SololevelingModBlocks;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.server.ServerStoppingEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative Frost Monarch combat kit and temporary-world lifecycle. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class FrostMonarchManager {
	public static final String FLASH_FREEZE = "Flash Freeze";
	public static final String FROZEN_PATH = "Frozen Path";
	public static final String FROST_COUNTER = "Frost Counter";
	public static final String ABSOLUTE_ZERO = "Absolute Zero";
	public static final String STILLNESS_DECREE = "Stillness Decree";
	public static final String PALE_CAUSEWAY = "Pale Causeway";
	public static final String WINTER_REMEMBERS = "Winter Remembers";
	public static final String WHITEOUT_PROCESSION = "Whiteout Procession";
	public static final String SPIRITUALIZATION = "Frost Monarch Spiritualization";
	public static final String AURA_ID = "frost_spiritualization";

	public static final String SPIRITUALIZATION_COOLDOWN = "frost_spiritualization_exhaustion";
	private static final String SPIRITUALIZED = "frost_spiritualized";
	private static final String SPIRITUALIZED_UNTIL = "frost_spiritualized_until";
	private static final List<String> LEGACY_PROGRESS_KEYS = List.of(
			"frost_winter_prongs", "frost_last_prong_source", "frost_last_prong_tick",
			"frost_last_payoff_tick", "frost_next_decay_tick", "frost_royal_seals",
			"frost_used_skills", "frost_last_combat_tick");
	private static final String TEMPORARY_SPEAR = "frost_temporary_spear";
	private static final String WHITEOUT_UNTIL = "frost_whiteout_until";
	private static final String WHITEOUT_PVE = "frost_whiteout_pve";
	private static final String WHITEOUT_OWNER = "frost_whiteout_owner";
	private static final String STILLNESS_IMMUNE_UNTIL = "frost_stillness_immune_until";
	private static final TagKey<EntityType<?>> BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE,
			new ResourceLocation("soloboss"));
	private static final TagKey<EntityType<?>> SHADOWS = TagKey.create(Registries.ENTITY_TYPE,
			new ResourceLocation("shadows"));

	private static final int SPEAR_BIT = 1;
	private static final int STILLNESS_BIT = 1 << 1;
	private static final int CAUSEWAY_BIT = 1 << 2;
	private static final int WINTER_BIT = 1 << 3;
	private static final int WHITEOUT_BIT = 1 << 4;

	private static final Map<UUID, SpearState> SPEARS = new HashMap<>();
	private static final Map<UUID, StillnessState> STILLNESS = new HashMap<>();
	private static final Map<UUID, CausewayState> CAUSEWAYS = new HashMap<>();
	private static final Map<UUID, WinterState> WINTERS = new HashMap<>();
	private static final Map<UUID, WhiteoutState> WHITEOUTS = new HashMap<>();
	private static final Map<UUID, FrozenTargetState> FROZEN_TARGETS = new HashMap<>();
	private static final Map<UUID, FrostCounterState> FROST_COUNTERS = new HashMap<>();
	private static final Map<UUID, AbsoluteZeroState> ABSOLUTE_ZEROS = new HashMap<>();

	private FrostMonarchManager() {
	}

	public static boolean isFrostMonarch(Entity entity) {
		return entity != null && entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(data -> (int) data.JOB == 3).orElse(false);
	}

	public static boolean isSpiritualized(Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return entity != null && entity.getPersistentData().getBoolean(SPIRITUALIZED);
		return player.getPersistentData().getBoolean(SPIRITUALIZED)
				&& player.getPersistentData().getLong(SPIRITUALIZED_UNTIL) >= player.level().getGameTime();
	}

	public static void castIceSpear(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isFrostMonarch(player))
			return;
		if (player.isShiftKeyDown()) {
			conjureIceSpear(player);
			return;
		}
		SpearState existing = SPEARS.get(player.getUUID());
		if (existing != null) {
			if (existing.phase != SpearPhase.RECALLING) {
				existing.phase = SpearPhase.RECALLING;
				existing.expiresAt = player.level().getGameTime() + 30;
				player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN,
						SoundSource.PLAYERS, 0.9F, 1.35F);
			}
			return;
		}
		boolean manifested = isSpiritualized(player);
		int mana = manifested ? 300 : 260;
		if (!canStartCast(player, JobSkillManager.ICE_SPEAR, mana, SPEAR_BIT))
			return;
		commitCast(player, JobSkillManager.ICE_SPEAR, mana, 110, SPEAR_BIT);
		ServerLevel level = player.serverLevel();
		Vec3 look = player.getLookAngle().normalize();
		Vec3 start = player.getEyePosition().add(look.scale(0.45D));
		ItemEntity visual = new ItemEntity(level, start.x, start.y, start.z,
				new ItemStack(SololevelingModItems.ICE_SPEAR.get()));
		visual.setNoGravity(true);
		visual.setNeverPickUp();
		visual.setInvulnerable(true);
		visual.getPersistentData().putBoolean(TEMPORARY_SPEAR, true);
		visual.setDeltaMovement(Vec3.ZERO);
		SpearState state = new SpearState(level.dimension(), visual.getUUID(), start, look,
				manifested, level.getGameTime() + 40);
		SPEARS.put(player.getUUID(), state);
		if (!level.addFreshEntity(visual)) {
			SPEARS.remove(player.getUUID(), state);
			return;
		}
		level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW,
				SoundSource.PLAYERS, 1.0F, 1.25F);
	}

	private static void conjureIceSpear(ServerPlayer player) {
		ItemStack spear = new ItemStack(SololevelingModItems.ICE_SPEAR.get());
		boolean alreadyCarried = false;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot).is(SololevelingModItems.ICE_SPEAR.get())) {
				alreadyCarried = true;
				break;
			}
		}
		if (alreadyCarried) {
			message(player, "You already carry an Ice Spear.", ChatFormatting.GRAY);
			return;
		}
		if (!canStartCast(player, JobSkillManager.ICE_SPEAR, 100, SPEAR_BIT))
			return;
		commitCast(player, JobSkillManager.ICE_SPEAR, 100, 60, SPEAR_BIT);
		if (!player.getInventory().add(spear))
			player.drop(spear, false);
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_PLACE,
				SoundSource.PLAYERS, 0.9F, 1.45F);
		player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getEyeY() - 0.35D,
				player.getZ(), 18, 0.28D, 0.32D, 0.28D, 0.04D);
		message(player, "Ice Spear formed in your inventory.", ChatFormatting.AQUA);
	}

	public static void castFlashFreeze(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isFrostMonarch(player))
			return;
		boolean manifested = isSpiritualized(player);
		int mana = manifested ? 340 : 300;
		if (!canStartCast(player, FLASH_FREEZE, mana, 0))
			return;
		commitCast(player, FLASH_FREEZE, mana, 200, 0);
		double range = manifested ? 12.0D : 9.0D;
		double minimumDot = manifested ? 0.67D : 0.78D;
		List<LivingEntity> targets = findConeTargets(player, range, 8, minimumDot);
		for (LivingEntity target : targets)
			applyFreeze(player, target, manifested ? 40 : 30, manifested);
		showFlashFreeze(player, range, manifested);
		player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
				SoundSource.PLAYERS, 1.0F, manifested ? 0.72F : 0.88F);
	}

	public static void castFrostCounter(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isFrostMonarch(player))
			return;
		boolean manifested = isSpiritualized(player);
		int mana = manifested ? 300 : 260;
		if (!canStartCast(player, FROST_COUNTER, mana, 0))
			return;
		commitCast(player, FROST_COUNTER, mana, 240, 0);
		FROST_COUNTERS.put(player.getUUID(), new FrostCounterState(
				player.level().getGameTime() + (manifested ? 100 : 80), manifested));
		player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getEyeY() - 0.4D,
				player.getZ(), 28, 0.55D, 0.75D, 0.55D, 0.03D);
		player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK,
				SoundSource.PLAYERS, 0.85F, 1.45F);
	}

	public static void castAbsoluteZero(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isFrostMonarch(player))
			return;
		boolean manifested = isSpiritualized(player);
		int mana = manifested ? 700 : 600;
		if (!canStartCast(player, ABSOLUTE_ZERO, mana, 0))
			return;
		commitCast(player, ABSOLUTE_ZERO, mana, 440, 0);
		long now = player.level().getGameTime();
		ABSOLUTE_ZEROS.put(player.getUUID(), new AbsoluteZeroState(
				now + (manifested ? 200 : 160), manifested));
		player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.2D,
				player.getZ(), manifested ? 90 : 65, manifested ? 5.0D : 4.0D,
				0.35D, manifested ? 5.0D : 4.0D, 0.05D);
		player.level().playSound(null, player.blockPosition(), SoundEvents.POWDER_SNOW_PLACE,
				SoundSource.PLAYERS, 1.2F, 0.55F);
	}

	public static void castStillnessDecree(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isFrostMonarch(player))
			return;
		StillnessState existing = STILLNESS.get(player.getUUID());
		if (existing != null) {
			releaseStillness(player, existing);
			return;
		}
		boolean manifested = isSpiritualized(player);
		int mana = manifested ? 460 : 400;
		if (!canStartCast(player, STILLNESS_DECREE, mana, STILLNESS_BIT))
			return;
		commitCast(player, STILLNESS_DECREE, mana, 240, STILLNESS_BIT);
		long now = player.level().getGameTime();
		STILLNESS.put(player.getUUID(), new StillnessState(now + (manifested ? 20 : 12),
				now + (manifested ? 70 : 62), manifested));
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
				SoundSource.PLAYERS, 0.9F, 0.65F);
	}

	public static void castPaleCauseway(Entity entity) {
		castFrozenPath(entity);
	}

	public static void castFrozenPath(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isFrostMonarch(player))
			return;
		boolean manifested = isSpiritualized(player);
		int mana = manifested ? 280 : 240;
		if (!canStartCast(player, FROZEN_PATH, mana, 0))
			return;
		removeCauseway(player.getUUID());
		PathCandidate candidate = solveFrozenPath(player, manifested ? 32 : 24, manifested ? 5 : 3);
		ServerLevel level = player.serverLevel();
		Map<BlockPos, Integer> pathCells = new LinkedHashMap<>();
		for (Map.Entry<BlockPos, Integer> entry : candidate.stepsByPos.entrySet()) {
			BlockPos pos = entry.getKey();
			FrozenCellResult result = freezePathCell(player, pos);
			if (result.tracked())
				pathCells.put(pos.immutable(), entry.getValue());
		}
		if (pathCells.isEmpty()) {
			message(player, "Frozen Path could not cross this protected area.", ChatFormatting.RED);
			return;
		}
		commitCast(player, FROZEN_PATH, mana, 180, 0);
		CAUSEWAYS.put(player.getUUID(), new CausewayState(level.dimension(), pathCells,
				candidate.direction, candidate.baseY, level.getGameTime() + 240, manifested));
		level.playSound(null, player.blockPosition(), SoundEvents.GLASS_PLACE,
				SoundSource.PLAYERS, 1.0F, 0.72F);
		spawnCausewayWave(level, pathCells);
	}

	public static void castWinterRemembers(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isFrostMonarch(player))
			return;
		WinterState existing = WINTERS.get(player.getUUID());
		if (existing != null) {
			resolveWinter(player, existing);
			return;
		}
		boolean manifested = isSpiritualized(player);
		LivingEntity target = findLookTarget(player, player.level().isClientSide() ? 12.0D : 16.0D);
		if (target == null) {
			message(player, "Winter found no valid path to remember.", ChatFormatting.RED);
			return;
		}
		int mana = manifested ? 600 : 520;
		if (!canStartCast(player, WINTER_REMEMBERS, mana, WINTER_BIT))
			return;
		commitCast(player, WINTER_REMEMBERS, mana, 300, WINTER_BIT);
		long now = player.level().getGameTime();
		WinterState state = new WinterState(player.serverLevel().dimension(), target.getUUID(),
				now + 60, now + 4, manifested);
		state.samples.add(sample(target));
		WINTERS.put(player.getUUID(), state);
		target.getPersistentData().putLong("frost_recorded_until", now + 60);
		player.level().playSound(null, target.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
				SoundSource.PLAYERS, 0.75F, 1.65F);
	}

	public static void castWhiteoutProcession(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isFrostMonarch(player))
			return;
		boolean manifested = isSpiritualized(player);
		int mana = manifested ? 750 : 650;
		if (!canStartCast(player, WHITEOUT_PROCESSION, mana, WHITEOUT_BIT))
			return;
		commitCast(player, WHITEOUT_PROCESSION, mana, 440, WHITEOUT_BIT);
		Vec3 direction = horizontal(player.getLookAngle());
		if (direction.lengthSqr() < 0.01D)
			direction = Vec3.directionFromRotation(0, player.getYRot());
		long now = player.level().getGameTime();
		WHITEOUTS.put(player.getUUID(), new WhiteoutState(player.serverLevel().dimension(),
				player.position(), direction.normalize(), now, now + 140, manifested, UUID.randomUUID()));
		player.level().playSound(null, player.blockPosition(), SoundEvents.POWDER_SNOW_PLACE,
				SoundSource.PLAYERS, 1.2F, 0.55F);
	}

	public static void toggleSpiritualization(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isFrostMonarch(player))
			return;
		if (isSpiritualized(player)) {
			endSpiritualization(player, true);
			return;
		}
		if (CooldownManager.isOnCooldown(player, SPIRITUALIZATION_COOLDOWN)) {
			message(player, "Your spiritual body is exhausted for "
					+ CooldownManager.getRemainingSeconds(player, SPIRITUALIZATION_COOLDOWN) + "s.", ChatFormatting.RED);
			return;
		}
		clearLegacyProgress(player);
		long now = player.level().getGameTime();
		player.getPersistentData().putBoolean(SPIRITUALIZED, true);
		player.getPersistentData().putLong(SPIRITUALIZED_UNTIL, now + 480);
		PlayerAuraSystem.setContinuous(player, AURA_ID, 1.2F);
		player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getEyeY(), player.getZ(),
				55, 0.55D, 0.95D, 0.55D, 0.08D);
		player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
				SoundSource.PLAYERS, 1.0F, 1.55F);
		message(player, "Frost spiritualization awakened.", ChatFormatting.AQUA);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
				|| !(event.player instanceof ServerPlayer player))
			return;
		if (!isFrostMonarch(player)) {
			if (hasAnyState(player) || Math.abs(variables(player).frostcharge) > 0.01D)
				clearAll(player, true);
			return;
		}
		long now = player.level().getGameTime();
		if (player.getPersistentData().getBoolean(SPIRITUALIZED)
				&& player.getPersistentData().getLong(SPIRITUALIZED_UNTIL) < now)
			endSpiritualization(player, true);
		clearLegacyProgress(player);
		updateSpear(player, now);
		updateFrozenTargets(player, now);
		updateFrostCounter(player, now);
		updateAbsoluteZero(player, now);
		updateStillness(player, now);
		updateCauseway(player, now);
		updateWinter(player, now);
		updateWhiteout(player, now);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onFrostCounterHurt(LivingHurtEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide())
			return;
		if (event.isCanceled() || event.getAmount() <= 0.0F)
			return;
		FrostCounterState state = FROST_COUNTERS.get(player.getUUID());
		long now = player.level().getGameTime();
		if (state == null || now > state.expiresAt) {
			FROST_COUNTERS.remove(player.getUUID());
			return;
		}
		Entity source = event.getSource().getEntity();
		if (!(source instanceof LivingEntity attacker) || !validTarget(player, attacker))
			return;
		FROST_COUNTERS.remove(player.getUUID());
		event.setAmount(event.getAmount() * (state.manifested ? 0.25F : 0.40F));
		applyFreeze(player, attacker, state.manifested ? 35 : 25, state.manifested);
		player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getEyeY(), player.getZ(),
				36, 0.65D, 0.85D, 0.65D, 0.08D);
		player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
				SoundSource.PLAYERS, 1.0F, 1.25F);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onFrozenTargetHurt(LivingHurtEvent event) {
		if (event.getEntity().level().isClientSide() || !event.getSource().is(DamageTypes.PLAYER_ATTACK))
			return;
		FrozenTargetState state = FROZEN_TARGETS.get(event.getEntity().getUUID());
		if (state == null || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
				|| !attacker.getUUID().equals(state.ownerId))
			return;
		releaseFreeze(event.getEntity());
		event.setAmount(event.getAmount() * (state.manifested ? 1.35F : 1.25F));
		attacker.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, event.getEntity().getX(),
				event.getEntity().getEyeY(), event.getEntity().getZ(), 28,
				0.35D, 0.45D, 0.35D, 0.09D);
		attacker.level().playSound(null, event.getEntity().blockPosition(), SoundEvents.GLASS_BREAK,
				SoundSource.PLAYERS, 0.9F, 1.65F);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onWhiteoutProjectileDamage(LivingHurtEvent event) {
		if (!(event.getSource().getDirectEntity() instanceof Projectile projectile))
			return;
		long now = event.getEntity().level().getGameTime();
		if (projectile.getPersistentData().getLong(WHITEOUT_UNTIL) < now
				|| !projectile.getPersistentData().hasUUID(WHITEOUT_OWNER)
				|| !(event.getEntity().level() instanceof ServerLevel level))
			return;
		Entity rawOwner = level.getEntity(projectile.getPersistentData().getUUID(WHITEOUT_OWNER));
		if (!(rawOwner instanceof ServerPlayer frostOwner)
				|| !isProtectedByWhiteout(frostOwner, event.getEntity()))
			return;
		float multiplier = projectile.getPersistentData().getBoolean(WHITEOUT_PVE) ? 0.70F : 0.85F;
		event.setAmount(event.getAmount() * multiplier);
	}

	@SubscribeEvent
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if (!event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity item
				&& item.getPersistentData().getBoolean(TEMPORARY_SPEAR)
				&& SPEARS.values().stream().noneMatch(state -> state.visualId.equals(item.getUUID())))
			item.discard();
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			releaseFreeze(player);
			clearAll(player, true);
		}
	}

	@SubscribeEvent
	public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			releaseFreeze(player);
			clearAll(player, true);
		}
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		releaseFreeze(event.getEntity());
		if (event.getEntity() instanceof ServerPlayer player)
			clearAll(player, true);
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		CAUSEWAYS.clear();
		FROZEN_TARGETS.clear();
		FROST_COUNTERS.clear();
		ABSOLUTE_ZEROS.clear();
	}

	private static void applyFreeze(ServerPlayer owner, LivingEntity target, int duration, boolean manifested) {
		if (!validTarget(owner, target))
			return;
		boolean hardRoot = !(target instanceof Player) && !isBoss(target);
		if (target instanceof Player)
			duration = Math.min(duration, manifested ? 14 : 10);
		else if (isBoss(target))
			duration = Math.min(duration, manifested ? 24 : 18);
		FrozenTargetState previous = FROZEN_TARGETS.get(target.getUUID());
		int previousFrozenTicks = previous == null ? target.getTicksFrozen() : previous.previousFrozenTicks;
		FROZEN_TARGETS.put(target.getUUID(), new FrozenTargetState(owner.getUUID(),
				target.level().dimension(), owner.level().getGameTime() + duration,
				hardRoot, manifested, previousFrozenTicks));
		target.setTicksFrozen(Math.max(target.getTicksFrozen(), 100));
		if (target instanceof Mob mob)
			mob.getNavigation().stop();
	}

	private static void updateFrozenTargets(ServerPlayer owner, long now) {
		List<UUID> remove = new ArrayList<>();
		for (Map.Entry<UUID, FrozenTargetState> entry : FROZEN_TARGETS.entrySet()) {
			FrozenTargetState state = entry.getValue();
			if (!state.ownerId.equals(owner.getUUID()))
				continue;
			if (state.dimension != owner.level().dimension() || now > state.expiresAt) {
				remove.add(entry.getKey());
				continue;
			}
			Entity raw = owner.serverLevel().getEntity(entry.getKey());
			if (!(raw instanceof LivingEntity target) || !target.isAlive()) {
				remove.add(entry.getKey());
				continue;
			}
			Vec3 movement = target.getDeltaMovement();
			double factor = state.hardRoot ? 0.0D : 0.18D;
			target.setDeltaMovement(movement.x * factor, movement.y, movement.z * factor);
			target.hurtMarked = true;
			target.setTicksFrozen(Math.max(target.getTicksFrozen(), 100));
			if (target instanceof Mob mob)
				mob.getNavigation().stop();
			if ((now & 3L) == 0L)
				owner.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getEyeY(),
						target.getZ(), state.hardRoot ? 5 : 3, 0.28D, 0.38D, 0.28D, 0.02D);
		}
		for (UUID id : remove)
			releaseFreeze(id, owner.getServer());
	}

	private static void releaseFreeze(UUID targetId, net.minecraft.server.MinecraftServer server) {
		FrozenTargetState state = FROZEN_TARGETS.remove(targetId);
		if (state == null || server == null)
			return;
		ServerLevel level = server.getLevel(state.dimension);
		Entity raw = level == null ? null : level.getEntity(targetId);
		if (raw instanceof LivingEntity target && target.getTicksFrozen() <= 100)
			target.setTicksFrozen(state.previousFrozenTicks);
	}

	private static void releaseFreeze(LivingEntity target) {
		FrozenTargetState state = FROZEN_TARGETS.remove(target.getUUID());
		if (state != null && target.getTicksFrozen() <= 100)
			target.setTicksFrozen(state.previousFrozenTicks);
	}

	private static void releaseFreezesOwnedBy(ServerPlayer owner) {
		List<UUID> targets = FROZEN_TARGETS.entrySet().stream()
				.filter(entry -> entry.getValue().ownerId.equals(owner.getUUID()))
				.map(Map.Entry::getKey).toList();
		for (UUID target : targets)
			releaseFreeze(target, owner.getServer());
	}

	private static void updateFrostCounter(ServerPlayer player, long now) {
		FrostCounterState state = FROST_COUNTERS.get(player.getUUID());
		if (state == null)
			return;
		if (now > state.expiresAt) {
			FROST_COUNTERS.remove(player.getUUID());
			return;
		}
		if ((now & 3L) == 0L)
			player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getEyeY() - 0.35D,
					player.getZ(), 4, 0.42D, 0.55D, 0.42D, 0.0D);
	}

	private static void updateAbsoluteZero(ServerPlayer player, long now) {
		AbsoluteZeroState state = ABSOLUTE_ZEROS.get(player.getUUID());
		if (state == null)
			return;
		if (now > state.expiresAt) {
			ABSOLUTE_ZEROS.remove(player.getUUID());
			return;
		}
		double radius = state.manifested ? 10.0D : 8.0D;
		int threshold = state.manifested ? 30 : 40;
		Set<UUID> inside = new HashSet<>();
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(radius, 4.0D, radius), candidate -> validTarget(player, candidate))) {
			if (target.distanceToSqr(player) > radius * radius)
				continue;
			inside.add(target.getUUID());
			Vec3 movement = target.getDeltaMovement();
			double slow = target instanceof Player ? 0.90D : isBoss(target) ? 0.72D : 0.62D;
			target.setDeltaMovement(movement.x * slow, movement.y, movement.z * slow);
			target.hurtMarked = true;
			int exposure = state.exposure.merge(target.getUUID(), 1, Integer::sum);
			if (exposure >= threshold && state.frozenOnce.add(target.getUUID()))
				applyFreeze(player, target, state.manifested ? 36 : 25, state.manifested);
		}
		state.exposure.keySet().removeIf(id -> !inside.contains(id));
		if ((now & 3L) == 0L)
			showAbsoluteZero(player, radius);
	}

	private static void showFlashFreeze(ServerPlayer player, double range, boolean manifested) {
		Vec3 forward = player.getLookAngle().normalize();
		Vec3 right = horizontal(new Vec3(-forward.z, 0.0D, forward.x)).normalize();
		for (int step = 1; step <= (int) range; step++) {
			double width = step * (manifested ? 0.44D : 0.34D);
			for (int lane = -2; lane <= 2; lane++) {
				Vec3 point = player.getEyePosition().add(forward.scale(step))
						.add(right.scale(width * lane / 2.0D));
				player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z,
						1, 0.08D, 0.08D, 0.08D, 0.02D);
			}
		}
	}

	private static void showAbsoluteZero(ServerPlayer player, double radius) {
		player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.18D,
				player.getZ(), 28, radius * 0.72D, 0.12D, radius * 0.72D, 0.01D);
		player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.12D,
				player.getZ(), 7, radius * 0.45D, 0.04D, radius * 0.45D, 0.0D);
	}

	private static void updateSpear(ServerPlayer player, long now) {
		SpearState state = SPEARS.get(player.getUUID());
		if (state == null)
			return;
		if (state.dimension != player.level().dimension()) {
			removeSpear(player.getUUID(), player.getServer());
			return;
		}
		Entity raw = player.serverLevel().getEntity(state.visualId);
		if (!(raw instanceof ItemEntity visual) || !visual.isAlive() || now > state.expiresAt) {
			removeSpear(player.getUUID(), player.getServer());
			return;
		}
		switch (state.phase) {
			case FLYING -> advanceFlyingSpear(player, visual, state, now);
			case ANCHORED -> updateAnchoredSpear(player, visual, state, now);
			case RECALLING -> advanceReturningSpear(player, visual, state);
		}
	}

	private static void advanceFlyingSpear(ServerPlayer player, ItemEntity visual, SpearState state, long now) {
		ServerLevel level = player.serverLevel();
		Vec3 start = visual.position();
		Vec3 next = start.add(state.direction.scale(1.8D));
		BlockHitResult blockHit = level.clip(new ClipContext(start, next, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, player));
		Vec3 clippedEnd = blockHit.getType() == HitResult.Type.MISS ? next : blockHit.getLocation();
		AABB search = visual.getBoundingBox().expandTowards(clippedEnd.subtract(start)).inflate(0.65D);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(visual, start, clippedEnd, search,
				candidate -> candidate instanceof LivingEntity living && validTarget(player, living),
				start.distanceToSqr(clippedEnd));
		if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
			float damage = spearPrimaryDamage(player, target, state.manifested);
			if (hurtPhysical(player, target, damage))
				state.outwardTarget = target.getUUID();
			state.anchorTarget = target.getUUID();
			state.anchorPoint = target.getBoundingBox().getCenter();
			state.phase = SpearPhase.ANCHORED;
			state.expiresAt = now + (state.manifested ? 120 : 80);
			visual.setPos(state.anchorPoint.x, state.anchorPoint.y, state.anchorPoint.z);
			impactParticles(level, state.anchorPoint, 18);
			level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK,
					SoundSource.PLAYERS, 0.75F, 1.65F);
			return;
		}
		if (blockHit.getType() != HitResult.Type.MISS) {
			state.anchorBlock = blockHit.getBlockPos().immutable();
			state.anchorPoint = blockHit.getLocation();
			state.phase = SpearPhase.ANCHORED;
			state.expiresAt = now + (state.manifested ? 120 : 80);
			visual.setPos(state.anchorPoint.x, state.anchorPoint.y, state.anchorPoint.z);
			impactParticles(level, state.anchorPoint, 12);
			level.playSound(null, state.anchorBlock, SoundEvents.GLASS_PLACE,
					SoundSource.PLAYERS, 0.8F, 1.35F);
			return;
		}
		state.distanceTravelled += 1.8D;
		if (state.distanceTravelled >= 24.0D || !level.hasChunkAt(BlockPos.containing(next))) {
			removeSpear(player.getUUID(), player.getServer());
			return;
		}
		visual.setPos(next.x, next.y, next.z);
		level.sendParticles(ParticleTypes.SNOWFLAKE, next.x, next.y, next.z, 2,
				0.06D, 0.06D, 0.06D, 0.01D);
	}

	private static void updateAnchoredSpear(ServerPlayer player, ItemEntity visual, SpearState state, long now) {
		ServerLevel level = player.serverLevel();
		if (state.anchorTarget != null) {
			Entity entity = level.getEntity(state.anchorTarget);
			if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
				removeSpear(player.getUUID(), player.getServer());
				return;
			}
			state.anchorPoint = target.getBoundingBox().getCenter();
			visual.setPos(state.anchorPoint.x, state.anchorPoint.y, state.anchorPoint.z);
		} else if (state.anchorBlock == null || level.getBlockState(state.anchorBlock).isAir()) {
			removeSpear(player.getUUID(), player.getServer());
			return;
		}
		if (now % 5 == 0)
			level.sendParticles(ParticleTypes.END_ROD, state.anchorPoint.x, state.anchorPoint.y,
					state.anchorPoint.z, state.manifested ? 3 : 1, 0.12D, 0.18D, 0.12D, 0.0D);
	}

	private static void advanceReturningSpear(ServerPlayer player, ItemEntity visual, SpearState state) {
		ServerLevel level = player.serverLevel();
		Vec3 start = visual.position();
		Vec3 destination = player.getEyePosition();
		Vec3 offset = destination.subtract(start);
		if (offset.lengthSqr() < 1.0D) {
			removeSpear(player.getUUID(), player.getServer());
			return;
		}
		Vec3 next = start.add(offset.normalize().scale(Math.min(2.25D, offset.length())));
		HitResult blockHit = level.clip(new ClipContext(start, next, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, player));
		if (blockHit.getType() != HitResult.Type.MISS) {
			removeSpear(player.getUUID(), player.getServer());
			return;
		}
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
				new AABB(start, next).inflate(0.75D), target -> validTarget(player, target));
		targets.sort(Comparator.comparingDouble(target -> target.distanceToSqr(start)));
		for (LivingEntity target : targets) {
			if (state.returnHits.contains(target.getUUID())
					|| target.getBoundingBox().inflate(0.45D).clip(start, next).isEmpty())
				continue;
			state.returnHits.add(target.getUUID());
			boolean same = target.getUUID().equals(state.outwardTarget);
			float primary = spearPrimaryDamage(player, target, state.manifested);
			float multiplier = same ? (target instanceof Player ? 0.05F : 0.40F) : 0.60F;
			if (same)
				target.invulnerableTime = 0;
			hurtPhysical(player, target, primary * multiplier);
		}
		visual.setPos(next.x, next.y, next.z);
		level.sendParticles(ParticleTypes.END_ROD, next.x, next.y, next.z, 2,
				0.08D, 0.08D, 0.08D, 0.0D);
	}

	private static void tetherToSpear(ServerPlayer player, SpearState state) {
		Vec3 anchor = state.anchorPoint;
		Vec3 difference = anchor.subtract(player.position());
		double maximum = state.manifested ? 14.0D : 10.0D;
		if (difference.lengthSqr() > maximum * maximum || difference.lengthSqr() < 1.0D) {
			message(player, "The spear anchor is out of tether range.", ChatFormatting.RED);
			return;
		}
		Vec3 velocity = difference.normalize().scale(Math.min(1.4D, 0.35D + difference.length() * 0.18D));
		if (!player.level().noCollision(player, player.getBoundingBox().move(velocity.scale(0.55D)))) {
			message(player, "The tether path is blocked.", ChatFormatting.RED);
			return;
		}
		player.setDeltaMovement(velocity);
		player.hurtMarked = true;
		player.fallDistance = 0;
		player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getEyeY(), player.getZ(),
				16, 0.25D, 0.35D, 0.25D, 0.05D);
		removeSpear(player.getUUID(), player.getServer());
	}

	private static void removeSpear(UUID owner, net.minecraft.server.MinecraftServer server) {
		SpearState state = SPEARS.remove(owner);
		if (state == null || server == null)
			return;
		ServerLevel level = server.getLevel(state.dimension);
		if (level != null) {
			Entity visual = level.getEntity(state.visualId);
			if (visual != null)
				visual.discard();
		}
	}

	private static void updateStillness(ServerPlayer player, long now) {
		StillnessState state = STILLNESS.get(player.getUUID());
		if (state == null)
			return;
		if (now <= state.catchUntil) {
			captureProjectiles(player, state, now);
			cancelIncomingMomentum(player, state, now);
			showStillnessPlane(player, state);
		}
		if (now > state.releaseUntil)
			STILLNESS.remove(player.getUUID());
	}

	private static void captureProjectiles(ServerPlayer player, StillnessState state, long now) {
		if (state.charges >= 3)
			return;
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		List<Projectile> projectiles = player.serverLevel().getEntitiesOfClass(Projectile.class,
				player.getBoundingBox().inflate(14.0D), projectile -> projectile.isAlive()
						&& projectile.getOwner() != player
						&& projectile.getPersistentData().getLong(WHITEOUT_UNTIL) < now);
		projectiles.sort(Comparator.comparingDouble(player::distanceToSqr));
		for (Projectile projectile : projectiles) {
			if (state.charges >= 3 || !isIncomingHostileProjectile(player, projectile, eye, look))
				continue;
			Entity owner = projectile.getOwner();
			Vec3 point = projectile.position();
			UUID contribution = owner instanceof LivingEntity ? owner.getUUID() : null;
			if (projectile instanceof ThrownTrident) {
				projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.20D));
				projectile.hurtMarked = true;
			} else {
				projectile.discard();
			}
			state.charges++;
			player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z,
					12, 0.18D, 0.18D, 0.18D, 0.02D);
			if (!state.masteryAwarded && contribution != null)
				state.masteryAwarded = true;
		}
	}

	private static void cancelIncomingMomentum(ServerPlayer player, StillnessState state, long now) {
		if (state.momentumUsed)
			return;
		for (LivingEntity target : findConeTargets(player, 14.0D, 3, 0.94D)) {
			if (target instanceof Player && player.distanceToSqr(target) > 81.0D)
				continue;
			Vec3 horizontalVelocity = horizontal(target.getDeltaMovement());
			if (horizontalVelocity.lengthSqr() < 0.0625D
					|| target.getPersistentData().getLong(STILLNESS_IMMUNE_UNTIL) >= now)
				continue;
			Vec3 towardCaster = horizontal(player.position().subtract(target.position())).normalize();
			if (horizontalVelocity.normalize().dot(towardCaster) < 0.45D)
				continue;
			double factor = target instanceof Player ? 0.50D : 0.15D;
			target.setDeltaMovement(horizontalVelocity.x * factor, target.getDeltaMovement().y,
					horizontalVelocity.z * factor);
			target.hurtMarked = true;
			target.getPersistentData().putLong(STILLNESS_IMMUNE_UNTIL, now + 160);
			state.momentumUsed = true;
			if (!state.masteryAwarded)
				state.masteryAwarded = true;
			player.serverLevel().sendParticles(ParticleTypes.END_ROD, target.getX(), target.getEyeY(),
					target.getZ(), 16, 0.25D, 0.35D, 0.25D, 0.0D);
			break;
		}
	}

	private static void releaseStillness(ServerPlayer player, StillnessState state) {
		STILLNESS.remove(player.getUUID());
		Vec3 end = player.getEyePosition().add(player.getLookAngle().scale(16.0D));
		LivingEntity lookedAt = findLookTarget(player, 18.0D);
		List<LivingEntity> targets = state.manifested && player.isShiftKeyDown()
				? findConeTargets(player, 16.0D, 3, 0.86D)
				: lookedAt == null ? List.of() : List.of(lookedAt);
		if (state.charges > 0) {
			for (LivingEntity target : targets) {
				if (target == null)
					continue;
				double intelligence = variables(player).Intelligence;
				float cap = (float) (target instanceof Player ? 12.0D + intelligence / 30.0D
						: 18.0D + intelligence / 20.0D);
				float damage = cap * (state.charges / 3.0F);
				hurtPhysical(player, target, damage);
				spawnParticleLine(player.serverLevel(), player.getEyePosition(), target.getBoundingBox().getCenter(),
						ParticleTypes.SNOWFLAKE, 14);
				end = target.getBoundingBox().getCenter();
			}
		} else {
			spawnParticleLine(player.serverLevel(), player.getEyePosition(), end, ParticleTypes.END_ROD, 10);
		}
		player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
				SoundSource.PLAYERS, 0.85F, 1.8F);
	}

	private static void showStillnessPlane(ServerPlayer player, StillnessState state) {
		if (player.tickCount % 2 != 0)
			return;
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		Vec3 right = horizontal(new Vec3(-look.z, 0.0D, look.x)).normalize();
		Vec3 center = eye.add(look.scale(1.8D));
		for (int i = -2; i <= 2; i++) {
			Vec3 point = center.add(right.scale(i * 0.38D));
			player.serverLevel().sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z,
					1, 0.0D, 0.35D, 0.0D, 0.0D);
		}
		if (state.charges > 0)
			player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getEyeY() + 0.25D,
					player.getZ(), state.charges, 0.35D, 0.08D, 0.35D, 0.0D);
	}

	private static PathCandidate solveFrozenPath(ServerPlayer player, int maximumLength, int width) {
		Direction forward = Direction.fromYRot(player.getYRot());
		Direction right = forward.getClockWise();
		Vec3 direction = Vec3.atLowerCornerOf(forward.getNormal()).normalize();
		int baseY = BlockPos.containing(player.getX(), player.getY() - 0.2D, player.getZ()).getY();
		int currentY = baseY;
		int halfWidth = width / 2;
		Map<BlockPos, Integer> positions = new LinkedHashMap<>();
		BlockPos origin = player.blockPosition();
		for (int step = 1; step <= maximumLength; step++) {
			BlockPos center = new BlockPos(origin.relative(forward, step).getX(), currentY,
					origin.relative(forward, step).getZ());
			if (!rowHasHeadroom(player.serverLevel(), center, right, halfWidth)) {
				BlockPos raised = center.above();
				if (!rowHasHeadroom(player.serverLevel(), raised, right, halfWidth))
					continue;
				center = raised;
				currentY++;
			}
			for (int lane = -halfWidth; lane <= halfWidth; lane++)
				positions.put(center.relative(right, lane).immutable(), step);
		}
		return new PathCandidate(positions, baseY, direction);
	}

	private static boolean rowHasHeadroom(ServerLevel level, BlockPos center, Direction right, int halfWidth) {
		for (int lane = -halfWidth; lane <= halfWidth; lane++) {
			BlockPos floor = center.relative(right, lane);
			if (!level.hasChunkAt(floor) || !level.getWorldBorder().isWithinBounds(floor)
					|| floor.getY() <= level.getMinBuildHeight() || floor.getY() >= level.getMaxBuildHeight() - 2)
				return false;
			for (int height = 1; height <= 2; height++) {
				BlockPos head = floor.above(height);
				if (!level.getBlockState(head).getCollisionShape(level, head).isEmpty())
					return false;
			}
		}
		return true;
	}

	private static FrozenCellResult freezePathCell(ServerPlayer player, BlockPos pos) {
		ServerLevel level = player.serverLevel();
		if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
				|| pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight() - 1)
			return FrozenCellResult.SKIPPED;
		BlockState oldState = level.getBlockState(pos);
		if (oldState.is(SololevelingModBlocks.FROST_CAUSEWAY.get())) {
			FrostCausewayBlock.refresh(level, pos, oldState);
			return FrozenCellResult.TRACKED;
		}
		boolean air = oldState.isAir();
		boolean water = oldState.is(net.minecraft.world.level.block.Blocks.WATER);
		boolean walkable = !oldState.getCollisionShape(level, pos).isEmpty();
		// Solid terrain is frozen mechanically and visually without replacing it. This keeps
		// plants, redstone, block entities, protected land, and unbreakable blocks intact.
		if (!air && !water)
			return walkable ? FrozenCellResult.TRACKED : FrozenCellResult.SKIPPED;
		if (!level.getFluidState(pos).isEmpty() && !water)
			return FrozenCellResult.SKIPPED;
		if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, ItemStack.EMPTY))
			return FrozenCellResult.SKIPPED;
		if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos), Entity::isAlive).isEmpty())
			return FrozenCellResult.SKIPPED;
		BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
		int returnLevel = water && oldState.hasProperty(BlockStateProperties.LEVEL)
				? oldState.getValue(BlockStateProperties.LEVEL)
				: FrostCausewayBlock.RETURN_AIR;
		BlockState frost = SololevelingModBlocks.FROST_CAUSEWAY.get().defaultBlockState()
				.setValue(FrostCausewayBlock.RETURN_LEVEL, returnLevel);
		if (!level.setBlock(pos, frost, 3))
			return FrozenCellResult.SKIPPED;
		if (ForgeEventFactory.onBlockPlace(player, snapshot, Direction.UP)) {
			snapshot.restore(true, false);
			return FrozenCellResult.SKIPPED;
		}
		FrostCausewayBlock.refresh(level, pos, level.getBlockState(pos));
		return FrozenCellResult.TRACKED;
	}

	private static void updateCauseway(ServerPlayer player, long now) {
		CausewayState state = CAUSEWAYS.get(player.getUUID());
		if (state == null)
			return;
		if (state.dimension != player.level().dimension() || now > state.expiresAt) {
			removeCauseway(player.getUUID());
			return;
		}
		BlockPos feet = BlockPos.containing(player.getX(), player.getY() - 0.1D, player.getZ());
		Integer step = state.pathCells.get(feet);
		if (step != null) {
			applyFrozenPathMomentum(player, state.direction, state.manifested);
			player.fallDistance = 0.0F;
		}
		if ((now & 1L) == 0L) {
			for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
					state.bounds.inflate(0.0D, 2.0D, 0.0D), candidate -> validTarget(player, candidate))) {
				BlockPos targetFloor = BlockPos.containing(target.getX(), target.getY() - 0.1D, target.getZ());
				if (!state.pathCells.containsKey(targetFloor))
					continue;
				Vec3 movement = target.getDeltaMovement();
				target.setDeltaMovement(movement.x * 0.55D, movement.y, movement.z * 0.55D);
				target.hurtMarked = true;
			}
		}
		if (now % 5 == 0) {
			Vec3 center = state.bounds.getCenter();
			double spreadX = Math.max(0.3D, state.bounds.getXsize() * 0.48D);
			double spreadZ = Math.max(0.3D, state.bounds.getZsize() * 0.48D);
			player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, center.x,
					state.bounds.minY + 1.02D, center.z, state.manifested ? 36 : 24,
					spreadX, 0.04D, spreadZ, 0.0D);
			player.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
					net.minecraft.world.level.block.Blocks.PACKED_ICE.defaultBlockState()), center.x,
					state.bounds.minY + 1.01D, center.z, state.manifested ? 18 : 12,
					spreadX, 0.02D, spreadZ, 0.0D);
		}
	}

	private static void applyFrozenPathMomentum(ServerPlayer player, Vec3 direction, boolean manifested) {
		Vec3 movement = horizontal(player.getDeltaMovement());
		double along = movement.dot(direction);
		if (along <= 0.02D)
			return;
		double boosted = Math.min(manifested ? 0.54D : 0.40D,
				Math.max(along, manifested ? 0.34D : 0.25D));
		Vec3 lateral = movement.subtract(direction.scale(along)).scale(0.35D);
		Vec3 result = direction.scale(boosted).add(lateral);
		player.setDeltaMovement(result.x, player.getDeltaMovement().y, result.z);
		player.hurtMarked = true;
	}

	private static void removeCauseway(UUID owner) {
		CAUSEWAYS.remove(owner);
	}

	private static void spawnCausewayWave(ServerLevel level, Map<BlockPos, Integer> placed) {
		AABB bounds = pathBounds(placed.keySet());
		Vec3 center = bounds.getCenter();
		level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, bounds.minY + 1.0D, center.z,
				Math.min(80, Math.max(18, placed.size())),
				Math.max(0.3D, bounds.getXsize() * 0.48D), 0.08D,
				Math.max(0.3D, bounds.getZsize() * 0.48D), 0.03D);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
				net.minecraft.world.level.block.Blocks.PACKED_ICE.defaultBlockState()), center.x,
				bounds.minY + 1.0D, center.z, Math.min(48, Math.max(12, placed.size() / 2)),
				Math.max(0.3D, bounds.getXsize() * 0.48D), 0.05D,
				Math.max(0.3D, bounds.getZsize() * 0.48D), 0.02D);
	}

	private static AABB pathBounds(Set<BlockPos> positions) {
		int minX = positions.stream().mapToInt(BlockPos::getX).min().orElse(0);
		int minY = positions.stream().mapToInt(BlockPos::getY).min().orElse(0);
		int minZ = positions.stream().mapToInt(BlockPos::getZ).min().orElse(0);
		int maxX = positions.stream().mapToInt(BlockPos::getX).max().orElse(0);
		int maxY = positions.stream().mapToInt(BlockPos::getY).max().orElse(0);
		int maxZ = positions.stream().mapToInt(BlockPos::getZ).max().orElse(0);
		return new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
	}

	private static void updateWinter(ServerPlayer player, long now) {
		WinterState state = WINTERS.get(player.getUUID());
		if (state == null)
			return;
		if (state.dimension != player.level().dimension()) {
			WINTERS.remove(player.getUUID());
			return;
		}
		Entity raw = player.serverLevel().getEntity(state.targetId);
		if (!(raw instanceof LivingEntity target) || !target.isAlive() || target.isPassenger()
				|| target.isVehicle()) {
			WINTERS.remove(player.getUUID());
			return;
		}
		if (!player.hasLineOfSight(target))
			state.lostSightTicks++;
		else
			state.lostSightTicks = 0;
		if (state.lostSightTicks >= 10) {
			WINTERS.remove(player.getUUID());
			message(player, "The remembered path dissolved out of sight.", ChatFormatting.GRAY);
			return;
		}
		if (now >= state.nextSampleAt && state.samples.size() < 16) {
			WinterSample previous = state.samples.get(state.samples.size() - 1);
			WinterSample current = sample(target);
			state.samples.add(current);
			state.nextSampleAt = now + 4;
			spawnParticleLine(player.serverLevel(), previous.position, current.position,
					ParticleTypes.SNOWFLAKE, 3);
		}
		if (now >= state.resolveAt)
			resolveWinter(player, state);
	}

	private static void resolveWinter(ServerPlayer player, WinterState state) {
		if (WINTERS.get(player.getUUID()) != state)
			return;
		WINTERS.remove(player.getUUID());
		if (state.dimension != player.level().dimension())
			return;
		Entity raw = player.serverLevel().getEntity(state.targetId);
		if (!(raw instanceof LivingEntity target) || !validTarget(player, target)
				|| target.isPassenger() || target.isVehicle() || !player.hasLineOfSight(target))
			return;
		if (isBoss(target)) {
			target.setDeltaMovement(0.0D, target.getDeltaMovement().y, 0.0D);
			target.hurtMarked = true;
			if (target instanceof Mob mob)
				mob.getNavigation().stop();
			float damage = (float) (16.0D + variables(player).Intelligence / 20.0D);
			hurtPhysical(player, target, damage);
			impactParticles(player.serverLevel(), target.getBoundingBox().getCenter(), 28);
			return;
		}
		double cap = target instanceof Player ? 4.0D : target.getMaxHealth() >= 100.0F ? 6.0D : 10.0D;
		List<WinterSample> ordered = new ArrayList<>();
		int start = state.manifested && player.isShiftKeyDown() ? state.samples.size() / 2 : 0;
		for (int i = start; i < state.samples.size(); i++)
			ordered.add(state.samples.get(i));
		if (start > 0)
			for (int i = 0; i < start; i++)
				ordered.add(state.samples.get(i));
		WinterSample chosen = null;
		for (WinterSample sample : ordered) {
			if (target.position().distanceTo(sample.position) <= cap && isSafeRewindPosition(player.serverLevel(), target, sample.position)) {
				chosen = sample;
				break;
			}
		}
		if (chosen == null) {
			message(player, "No safe remembered position remained.", ChatFormatting.GRAY);
			return;
		}
		Vec3 before = target.position();
		if (target instanceof ServerPlayer targetPlayer)
			targetPlayer.connection.teleport(chosen.position.x, chosen.position.y, chosen.position.z,
					chosen.yaw, chosen.pitch);
		else
			target.teleportTo(chosen.position.x, chosen.position.y, chosen.position.z);
		target.setYRot(chosen.yaw);
		target.setXRot(chosen.pitch);
		target.setDeltaMovement(chosen.velocity);
		target.fallDistance = 0;
		target.hurtMarked = true;
		double moved = before.distanceTo(chosen.position);
		double intelligence = variables(player).Intelligence;
		float damage = (float) Math.min(24.0D + intelligence / 20.0D,
				12.0D + intelligence / 15.0D + moved * 1.5D);
		if (target instanceof Player)
			damage *= 0.65F;
		target.invulnerableTime = 0;
		hurtPhysical(player, target, damage);
		if (target instanceof Player)
			target.getPersistentData().putLong("frost_rewind_immune_until", player.level().getGameTime() + 160);
		spawnParticleLine(player.serverLevel(), before, chosen.position, ParticleTypes.END_ROD, 20);
		player.level().playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS, 0.8F, 1.75F);
	}

	private static boolean isSafeRewindPosition(ServerLevel level, LivingEntity target, Vec3 position) {
		BlockPos feet = BlockPos.containing(position);
		if (!level.hasChunkAt(feet) || !level.getWorldBorder().isWithinBounds(feet)
				|| position.y <= level.getMinBuildHeight() + 1 || position.y >= level.getMaxBuildHeight() - 2
				|| !level.getFluidState(feet).isEmpty())
			return false;
		AABB moved = target.getBoundingBox().move(position.subtract(target.position()));
		BlockPos below = BlockPos.containing(position.x, position.y - 0.01D, position.z);
		return level.noCollision(target, moved)
				&& !level.getBlockState(below).getCollisionShape(level, below).isEmpty()
				&& level.getFluidState(below).isEmpty();
	}

	private static WinterSample sample(LivingEntity target) {
		return new WinterSample(target.position(), target.getYRot(), target.getXRot(), target.getDeltaMovement());
	}

	private static void updateWhiteout(ServerPlayer player, long now) {
		WhiteoutState state = WHITEOUTS.get(player.getUUID());
		if (state == null)
			return;
		if (state.dimension != player.level().dimension() || now > state.expiresAt) {
			WHITEOUTS.remove(player.getUUID());
			return;
		}
		Vec3 front = whiteoutFront(state, now, 0.0D);
		attenuateWhiteoutProjectiles(player, state, front, now);
		disorientWhiteoutMobs(player, state, front);
		if (state.manifested) {
			Vec3 rear = whiteoutFront(state, now, -4.0D);
			attenuateWhiteoutProjectiles(player, state, rear, now);
			boostWhiteoutCorridor(player, state, front, rear);
		}
		if (now % 3 == 0) {
			showWhiteout(player.serverLevel(), front, state.origin.y);
			if (state.manifested)
				showWhiteout(player.serverLevel(), whiteoutFront(state, now, -4.0D), state.origin.y);
		}
	}

	private static Vec3 whiteoutFront(WhiteoutState state, long now, double offset) {
		double progress = Mth.clamp((now - state.startedAt) / 140.0D, 0.0D, 1.0D);
		return state.origin.add(state.direction.scale(progress * 16.0D + offset));
	}

	private static void attenuateWhiteoutProjectiles(ServerPlayer player, WhiteoutState state,
			Vec3 front, long now) {
		ServerLevel level = player.serverLevel();
		for (Projectile projectile : level.getEntitiesOfClass(Projectile.class,
				new AABB(front, front).inflate(4.2D, 4.5D, 4.2D), Entity::isAlive)) {
			if (state.tagged.contains(projectile.getUUID()) || !crossesWhiteout(state, front, projectile)
					|| !isHostileProjectileOwner(player, projectile.getOwner()))
				continue;
			state.tagged.add(projectile.getUUID());
			projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.70D));
			projectile.hurtMarked = true;
			projectile.getPersistentData().putLong(WHITEOUT_UNTIL, now + 200);
			projectile.getPersistentData().putBoolean(WHITEOUT_PVE, !(projectile.getOwner() instanceof Player));
			projectile.getPersistentData().putUUID(WHITEOUT_OWNER, player.getUUID());
			level.sendParticles(ParticleTypes.SNOWFLAKE, projectile.getX(), projectile.getY(), projectile.getZ(),
					10, 0.18D, 0.18D, 0.18D, 0.02D);
			if (!state.masteryAwarded && projectile.getOwner() != null) {
				state.masteryAwarded = true;
			}
		}
	}

	private static boolean crossesWhiteout(WhiteoutState state, Vec3 front, Projectile projectile) {
		Vec3 current = projectile.position();
		if (intersectsWhiteout(state, front, current))
			return true;
		Vec3 previous = current.subtract(projectile.getDeltaMovement());
		double previousDepth = previous.subtract(front).dot(state.direction);
		double currentDepth = current.subtract(front).dot(state.direction);
		double denominator = currentDepth - previousDepth;
		if (Math.abs(denominator) < 1.0E-5D)
			return false;
		double t = -previousDepth / denominator;
		if (t < 0.0D || t > 1.0D)
			return false;
		Vec3 crossing = previous.lerp(current, t);
		Vec3 local = crossing.subtract(front);
		Vec3 right = new Vec3(-state.direction.z, 0.0D, state.direction.x);
		return Math.abs(local.dot(right)) <= 3.5D
				&& crossing.y >= state.origin.y - 0.5D && crossing.y <= state.origin.y + 4.0D;
	}

	private static void disorientWhiteoutMobs(ServerPlayer owner, WhiteoutState state, Vec3 front) {
		for (Mob mob : owner.serverLevel().getEntitiesOfClass(Mob.class,
				new AABB(front, front).inflate(4.2D, 4.5D, 4.2D), candidate -> validTarget(owner, candidate))) {
			if (!intersectsWhiteout(state, front, mob.getBoundingBox().getCenter()))
				continue;
			LivingEntity target = mob.getTarget();
			if (target != owner && !(target instanceof ServerPlayer player && isFriendly(owner, player)))
				continue;
			mob.setTarget(null);
			mob.getNavigation().stop();
			if (!state.masteryAwarded)
				state.masteryAwarded = true;
		}
	}

	private static boolean intersectsWhiteout(WhiteoutState state, Vec3 front, Vec3 point) {
		Vec3 local = point.subtract(front);
		Vec3 right = new Vec3(-state.direction.z, 0.0D, state.direction.x);
		double depth = Math.abs(local.dot(state.direction));
		double lateral = Math.abs(local.dot(right));
		return depth <= 0.9D && lateral <= 3.5D
				&& point.y >= state.origin.y - 0.5D && point.y <= state.origin.y + 4.0D;
	}

	private static void boostWhiteoutCorridor(ServerPlayer owner, WhiteoutState state, Vec3 front, Vec3 rear) {
		Vec3 middle = front.add(rear).scale(0.5D);
		for (ServerPlayer player : owner.serverLevel().getEntitiesOfClass(ServerPlayer.class,
				new AABB(middle, middle).inflate(4.0D, 4.0D, 4.0D), candidate -> isFriendly(owner, candidate))) {
			Vec3 local = player.position().subtract(rear);
			double forward = local.dot(state.direction);
			Vec3 right = new Vec3(-state.direction.z, 0.0D, state.direction.x);
			if (forward < 0.0D || forward > 4.5D || Math.abs(local.dot(right)) > 3.5D)
				continue;
			Vec3 movement = horizontal(player.getDeltaMovement());
			double along = movement.dot(state.direction);
			if (along <= 0.02D)
				continue;
			double boosted = Math.min(0.60D, along * 1.15D + 0.015D);
			if (boosted <= along)
				continue;
			Vec3 result = movement.add(state.direction.scale(boosted - along));
			player.setDeltaMovement(result.x, player.getDeltaMovement().y, result.z);
			player.hurtMarked = true;
		}
	}

	private static void showWhiteout(ServerLevel level, Vec3 center, double baseY) {
		level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, baseY + 1.8D, center.z,
				20, 3.4D, 1.8D, 0.35D, 0.025D);
		level.sendParticles(ParticleTypes.CLOUD, center.x, baseY + 1.5D, center.z,
				4, 2.8D, 1.25D, 0.25D, 0.01D);
	}

	private static boolean canStartCast(ServerPlayer player, String skill, int mana, int bit) {
		if (CooldownManager.isOnCooldown(player, skill)) {
			message(player, skill + " is ready in " + CooldownManager.getRemainingSeconds(player, skill) + "s.",
					ChatFormatting.GRAY);
			return false;
		}
		if (!hasMana(player, mana))
			return false;
		return true;
	}

	private static void commitCast(ServerPlayer player, String skill, int mana, int cooldown, int bit) {
		consumeMana(player, mana);
		CooldownManager.set(player, skill, cooldown);
		CooldownManager.set(player, "mana_refresh", 40);
	}

	private static boolean hasMana(ServerPlayer player, int amount) {
		if (variables(player).MP >= amount)
			return true;
		message(player, "Not enough MP — " + amount + " required.", ChatFormatting.RED);
		return false;
	}

	private static void consumeMana(ServerPlayer player, int amount) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(data -> {
			data.MP = Math.max(0.0D, data.MP - amount);
			data.syncPlayerVariables(player);
		});
		CooldownManager.set(player, "mana_refresh", 40);
	}

	private static void clearLegacyProgress(ServerPlayer player) {
		for (String key : LEGACY_PROGRESS_KEYS)
			player.getPersistentData().remove(key);
		if (Math.abs(variables(player).frostcharge) > 0.01D) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(data -> {
				data.frostcharge = 0.0D;
				data.syncPlayerVariables(player);
			});
		}
	}

	private static void endSpiritualization(ServerPlayer player, boolean exhausted) {
		if (!player.getPersistentData().getBoolean(SPIRITUALIZED))
			return;
		player.getPersistentData().remove(SPIRITUALIZED);
		player.getPersistentData().remove(SPIRITUALIZED_UNTIL);
		PlayerAuraSystem.clearContinuous(player);
		clearLegacyProgress(player);
		if (exhausted)
			CooldownManager.set(player, SPIRITUALIZATION_COOLDOWN, 900);
		player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
				SoundSource.PLAYERS, 0.8F, 1.25F);
		if (exhausted)
			message(player, "Frost spiritualization ended — 45s exhaustion.", ChatFormatting.GRAY);
	}

	private static void clearAll(ServerPlayer player, boolean exhaustForm) {
		removeSpear(player.getUUID(), player.getServer());
		removeCauseway(player.getUUID());
		STILLNESS.remove(player.getUUID());
		WINTERS.remove(player.getUUID());
		WHITEOUTS.remove(player.getUUID());
		FROST_COUNTERS.remove(player.getUUID());
		ABSOLUTE_ZEROS.remove(player.getUUID());
		releaseFreezesOwnedBy(player);
		if (player.getPersistentData().getBoolean(SPIRITUALIZED))
			endSpiritualization(player, exhaustForm);
		else
			clearLegacyProgress(player);
	}

	private static boolean hasAnyState(ServerPlayer player) {
		UUID id = player.getUUID();
		return player.getPersistentData().getBoolean(SPIRITUALIZED)
				|| SPEARS.containsKey(id) || STILLNESS.containsKey(id) || CAUSEWAYS.containsKey(id)
				|| WINTERS.containsKey(id) || WHITEOUTS.containsKey(id) || FROST_COUNTERS.containsKey(id)
				|| ABSOLUTE_ZEROS.containsKey(id)
				|| FROZEN_TARGETS.values().stream().anyMatch(state -> state.ownerId.equals(id));
	}

	private static boolean isIncomingHostileProjectile(ServerPlayer player, Projectile projectile,
			Vec3 eye, Vec3 look) {
		Entity owner = projectile.getOwner();
		if (!isHostileProjectileOwner(player, owner))
			return false;
		double maximum = owner instanceof Player ? 9.0D : 14.0D;
		Vec3 toProjectile = projectile.getBoundingBox().getCenter().subtract(eye);
		if (toProjectile.lengthSqr() > maximum * maximum || toProjectile.lengthSqr() < 0.04D
				|| look.dot(toProjectile.normalize()) < 0.92D)
			return false;
		Vec3 towardPlayer = eye.subtract(projectile.position());
		return towardPlayer.lengthSqr() > 0.01D
				&& projectile.getDeltaMovement().dot(towardPlayer.normalize()) > 0.03D;
	}

	private static boolean isHostileProjectileOwner(ServerPlayer player, Entity owner) {
		if (owner == player)
			return false;
		if (owner instanceof LivingEntity living)
			return validTarget(player, living);
		return owner == null || !owner.isAlliedTo(player);
	}

	private static boolean validTarget(ServerPlayer owner, LivingEntity target) {
		if (target == null || !target.isAlive() || target == owner
				|| target.isAlliedTo(owner) || owner.isAlliedTo(target) || target.getType().is(SHADOWS))
			return false;
		if (target instanceof TamableAnimal tame && owner.getUUID().equals(tame.getOwnerUUID()))
			return false;
		if (target.getPersistentData().hasUUID("mowf_summon_owner")
				&& owner.getUUID().equals(target.getPersistentData().getUUID("mowf_summon_owner")))
			return false;
		if (target instanceof Player other
				&& (other.isCreative() || other.isSpectator() || !owner.canHarmPlayer(other)))
			return false;
		return !(target instanceof Player partyMember && sameParty(owner, partyMember));
	}

	private static boolean sameParty(ServerPlayer first, Player second) {
		String firstParty = first.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(data -> data.party).orElse("");
		String secondParty = second.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(data -> data.party).orElse("");
		return !firstParty.isBlank() && firstParty.equals(secondParty);
	}

	private static boolean isFriendly(ServerPlayer owner, ServerPlayer candidate) {
		return owner == candidate || sameParty(owner, candidate) || owner.isAlliedTo(candidate)
				|| candidate.isAlliedTo(owner);
	}

	private static boolean isProtectedByWhiteout(ServerPlayer owner, LivingEntity candidate) {
		return candidate == owner || owner.isAlliedTo(candidate) || candidate.isAlliedTo(owner)
				|| candidate instanceof ServerPlayer player && sameParty(owner, player);
	}

	private static boolean isBoss(LivingEntity entity) {
		return !(entity instanceof Player)
				&& (entity.getType().is(BOSS_TAG) || entity.getMaxHealth() >= 250.0F);
	}

	private static LivingEntity findLookTarget(ServerPlayer player, double range) {
		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(player.getLookAngle().scale(range));
		HitResult blockHit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, player));
		if (blockHit.getType() != HitResult.Type.MISS)
			end = blockHit.getLocation();
		AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.1D);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, search,
				candidate -> candidate instanceof LivingEntity living && validTarget(player, living)
						&& (!(candidate instanceof Player) || player.distanceToSqr(candidate) <= 144.0D)
						&& candidate.getPersistentData().getLong("frost_rewind_immune_until") < player.level().getGameTime(),
				start.distanceToSqr(end));
		return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
	}

	private static List<LivingEntity> findConeTargets(ServerPlayer player, double range, int maximum,
			double minimumDot) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		List<LivingEntity> targets = new ArrayList<>();
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(range), candidate -> validTarget(player, candidate))) {
			Vec3 direction = target.getBoundingBox().getCenter().subtract(eye);
			if (direction.lengthSqr() < 0.01D || direction.lengthSqr() > range * range
					|| look.dot(direction.normalize()) < minimumDot || !player.hasLineOfSight(target))
				continue;
			targets.add(target);
		}
		targets.sort(Comparator.comparingDouble(player::distanceToSqr));
		return targets.size() > maximum ? new ArrayList<>(targets.subList(0, maximum)) : targets;
	}

	private static boolean hurtPhysical(ServerPlayer owner, LivingEntity target, float amount) {
		if (!validTarget(owner, target))
			return false;
		boolean hurt = target.hurt(owner.damageSources().playerAttack(owner), Math.max(0.5F, amount));
		if (hurt)
			target.setLastHurtByPlayer(owner);
		return hurt;
	}

	private static float spearPrimaryDamage(ServerPlayer player, LivingEntity target, boolean manifested) {
		SololevelingModVariables.PlayerVariables data = variables(player);
		double damage = 20.0D + data.Strength / 8.0D + data.Intelligence / 16.0D;
		if (target instanceof Player)
			damage *= 0.75D;
		if (manifested)
			damage *= 1.15D;
		return (float) damage;
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static Vec3 horizontal(Vec3 vector) {
		return new Vec3(vector.x, 0.0D, vector.z);
	}

	private static void impactParticles(ServerLevel level, Vec3 point, int count) {
		level.sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z, count,
				0.38D, 0.38D, 0.38D, 0.07D);
		level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, Math.max(2, count / 5),
				0.22D, 0.22D, 0.22D, 0.02D);
	}

	private static void spawnParticleLine(ServerLevel level, Vec3 start, Vec3 end,
			net.minecraft.core.particles.ParticleOptions particle, int samples) {
		for (int i = 0; i <= samples; i++) {
			Vec3 point = start.lerp(end, i / (double) Math.max(1, samples));
			level.sendParticles(particle, point.x, point.y, point.z, 1,
					0.02D, 0.02D, 0.02D, 0.0D);
		}
	}

	private static void message(ServerPlayer player, String text, ChatFormatting color) {
		player.displayClientMessage(Component.literal(text).withStyle(color), true);
	}

	private enum SpearPhase {
		FLYING,
		ANCHORED,
		RECALLING
	}

	private static final class SpearState {
		private final ResourceKey<Level> dimension;
		private final UUID visualId;
		private final Vec3 direction;
		private final boolean manifested;
		private final Set<UUID> returnHits = new HashSet<>();
		private SpearPhase phase = SpearPhase.FLYING;
		private Vec3 anchorPoint;
		private BlockPos anchorBlock;
		private UUID anchorTarget;
		private UUID outwardTarget;
		private long expiresAt;
		private double distanceTravelled;
		private boolean masteryAwarded;

		private SpearState(ResourceKey<Level> dimension, UUID visualId, Vec3 anchorPoint,
				Vec3 direction, boolean manifested, long expiresAt) {
			this.dimension = dimension;
			this.visualId = visualId;
			this.anchorPoint = anchorPoint;
			this.direction = direction;
			this.manifested = manifested;
			this.expiresAt = expiresAt;
		}
	}

	private static final class StillnessState {
		private final long catchUntil;
		private final long releaseUntil;
		private final boolean manifested;
		private int charges;
		private boolean momentumUsed;
		private boolean masteryAwarded;

		private StillnessState(long catchUntil, long releaseUntil, boolean manifested) {
			this.catchUntil = catchUntil;
			this.releaseUntil = releaseUntil;
			this.manifested = manifested;
		}
	}

	private static final class CausewayState {
		private final ResourceKey<Level> dimension;
		private final Map<BlockPos, Integer> pathCells;
		private final Vec3 direction;
		private final int baseY;
		private final long expiresAt;
		private final boolean manifested;
		private final AABB bounds;

		private CausewayState(ResourceKey<Level> dimension, Map<BlockPos, Integer> pathCells,
				Vec3 direction, int baseY,
				long expiresAt, boolean manifested) {
			this.dimension = dimension;
			this.pathCells = pathCells;
			this.direction = direction;
			this.baseY = baseY;
			this.expiresAt = expiresAt;
			this.manifested = manifested;
			this.bounds = pathBounds(pathCells.keySet());
		}
	}

	private static final class FrozenTargetState {
		private final UUID ownerId;
		private final ResourceKey<Level> dimension;
		private final long expiresAt;
		private final boolean hardRoot;
		private final boolean manifested;
		private final int previousFrozenTicks;

		private FrozenTargetState(UUID ownerId, ResourceKey<Level> dimension, long expiresAt,
				boolean hardRoot, boolean manifested, int previousFrozenTicks) {
			this.ownerId = ownerId;
			this.dimension = dimension;
			this.expiresAt = expiresAt;
			this.hardRoot = hardRoot;
			this.manifested = manifested;
			this.previousFrozenTicks = previousFrozenTicks;
		}
	}

	private static final class FrostCounterState {
		private final long expiresAt;
		private final boolean manifested;

		private FrostCounterState(long expiresAt, boolean manifested) {
			this.expiresAt = expiresAt;
			this.manifested = manifested;
		}
	}

	private static final class AbsoluteZeroState {
		private final long expiresAt;
		private final boolean manifested;
		private final Map<UUID, Integer> exposure = new HashMap<>();
		private final Set<UUID> frozenOnce = new HashSet<>();

		private AbsoluteZeroState(long expiresAt, boolean manifested) {
			this.expiresAt = expiresAt;
			this.manifested = manifested;
		}
	}

	private static final class WinterState {
		private final ResourceKey<Level> dimension;
		private final UUID targetId;
		private final long resolveAt;
		private final boolean manifested;
		private final List<WinterSample> samples = new ArrayList<>();
		private long nextSampleAt;
		private int lostSightTicks;

		private WinterState(ResourceKey<Level> dimension, UUID targetId, long resolveAt,
				long nextSampleAt, boolean manifested) {
			this.dimension = dimension;
			this.targetId = targetId;
			this.resolveAt = resolveAt;
			this.nextSampleAt = nextSampleAt;
			this.manifested = manifested;
		}
	}

	private static final class WhiteoutState {
		private final ResourceKey<Level> dimension;
		private final Vec3 origin;
		private final Vec3 direction;
		private final long startedAt;
		private final long expiresAt;
		private final boolean manifested;
		@SuppressWarnings("unused")
		private final UUID frontId;
		private final Set<UUID> tagged = new HashSet<>();
		private boolean masteryAwarded;

		private WhiteoutState(ResourceKey<Level> dimension, Vec3 origin, Vec3 direction,
				long startedAt, long expiresAt, boolean manifested, UUID frontId) {
			this.dimension = dimension;
			this.origin = origin;
			this.direction = direction;
			this.startedAt = startedAt;
			this.expiresAt = expiresAt;
			this.manifested = manifested;
			this.frontId = frontId;
		}
	}

	private record PathCandidate(Map<BlockPos, Integer> stepsByPos, int baseY, Vec3 direction) {
	}

	private record FrozenCellResult(boolean tracked) {
		private static final FrozenCellResult SKIPPED = new FrozenCellResult(false);
		private static final FrozenCellResult TRACKED = new FrozenCellResult(true);
	}

	private record WinterSample(Vec3 position, float yaw, float pitch, Vec3 velocity) {
	}
}
