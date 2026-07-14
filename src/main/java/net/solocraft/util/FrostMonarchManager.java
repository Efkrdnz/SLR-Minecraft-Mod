package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.block.FrostCausewayBlock;
import net.solocraft.entity.GlacialPursuitEntity;
import net.solocraft.init.SololevelingModBlocks;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
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
	public static final int SPIRITUALIZATION_MANA_PER_SECOND = 14;

	private static final String LEGACY_SPIRITUALIZATION_COOLDOWN = "frost_spiritualization_exhaustion";
	private static final String SPIRITUALIZED = "frost_spiritualized";
	private static final String LEGACY_SPIRITUALIZED_UNTIL = "frost_spiritualized_until";
	private static final List<String> LEGACY_PROGRESS_KEYS = List.of(
			"frost_winter_prongs", "frost_last_prong_source", "frost_last_prong_tick",
			"frost_last_payoff_tick", "frost_next_decay_tick", "frost_royal_seals",
			"frost_used_skills", "frost_last_combat_tick");
	private static final String TEMPORARY_SPEAR = "frost_temporary_spear";
	private static final String WHITEOUT_UNTIL = "frost_whiteout_until";
	private static final String WHITEOUT_PVE = "frost_whiteout_pve";
	private static final String WHITEOUT_OWNER = "frost_whiteout_owner";
	private static final String STILLNESS_IMMUNE_UNTIL = "frost_stillness_immune_until";
	private static final String PATH_FALL_PROTECTION_UNTIL = "frost_path_fall_protection_until";
	private static final int MAX_FROSTBITE = 5;
	private static final int FROSTBITE_LIFETIME = 120;
	private static final int PURSUIT_BLOCK_GRACE_TICKS = 12;
	private static final int PURSUIT_REMOUNT_GRACE_TICKS = 10;
	private static final TagKey<EntityType<?>> BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE,
			new ResourceLocation("soloboss"));
	private static final TagKey<EntityType<?>> SHADOWS = TagKey.create(Registries.ENTITY_TYPE,
			new ResourceLocation("shadows"));

	private static final int SPEAR_BIT = 1;
	private static final int STILLNESS_BIT = 1 << 1;
	private static final int WINTER_BIT = 1 << 3;
	private static final int WHITEOUT_BIT = 1 << 4;

	private static final Map<UUID, SpearState> SPEARS = new HashMap<>();
	private static final Map<UUID, StillnessState> STILLNESS = new HashMap<>();
	private static final Map<UUID, GlacialPursuitState> GLACIAL_PURSUITS = new HashMap<>();
	private static final Map<UUID, FrostbiteState> FROSTBITE = new HashMap<>();
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

	/** X/C/V/B use Frost job actions only while the universal combat controls are off. */
	public static boolean isDirectAbilityMode(Entity entity) {
		return isFrostMonarch(entity) && !variables(entity).combatmode;
	}

	public static boolean hasActiveFrozenPath(Entity entity) {
		return entity != null && GLACIAL_PURSUITS.containsKey(entity.getUUID());
	}

	public static boolean isSpiritualized(Entity entity) {
		return entity != null && entity.getPersistentData().getBoolean(SPIRITUALIZED);
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
		int mana = manifested ? 320 : 280;
		if (!canStartCast(player, FLASH_FREEZE, mana, 0))
			return;
		commitCast(player, FLASH_FREEZE, mana, 140, 0);
		double range = manifested ? 12.0D : 9.0D;
		double minimumDot = manifested ? 0.67D : 0.78D;
		List<LivingEntity> targets = findConeTargets(player, range, 8, minimumDot);
		for (LivingEntity target : targets)
			dealFrostDamage(player, target, flashFreezeDamage(player, target, manifested), 3, true);
		showFlashFreeze(player, range, manifested);
		player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
				SoundSource.PLAYERS, 1.0F, manifested ? 0.72F : 0.88F);
	}

	public static void castFrostCounter(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isFrostMonarch(player))
			return;
		boolean manifested = isSpiritualized(player);
		int mana = manifested ? 280 : 240;
		if (!canStartCast(player, FROST_COUNTER, mana, 0))
			return;
		commitCast(player, FROST_COUNTER, mana, 180, 0);
		FROST_COUNTERS.put(player.getUUID(), new FrostCounterState(
				player.level().getGameTime() + (manifested ? 40 : 32), manifested));
		player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getEyeY() - 0.4D,
				player.getZ(), 28, 0.55D, 0.75D, 0.55D, 0.03D);
		player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK,
				SoundSource.PLAYERS, 0.85F, 1.45F);
	}

	public static void castAbsoluteZero(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isFrostMonarch(player))
			return;
		AbsoluteZeroState existing = ABSOLUTE_ZEROS.get(player.getUUID());
		if (existing != null) {
			detonateAbsoluteZero(player, existing, 1.0F);
			return;
		}
		boolean manifested = isSpiritualized(player);
		int mana = manifested ? 700 : 600;
		if (!canStartCast(player, ABSOLUTE_ZERO, mana, 0))
			return;
		commitCast(player, ABSOLUTE_ZERO, mana, 400, 0);
		long now = player.level().getGameTime();
		ABSOLUTE_ZEROS.put(player.getUUID(), new AbsoluteZeroState(
				now + (manifested ? 200 : 160), now + 6, manifested));
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
		if (GLACIAL_PURSUITS.containsKey(player.getUUID()))
			return;
		boolean manifested = isSpiritualized(player);
		boolean riderMode = player.isShiftKeyDown();
		int mana = (manifested ? 210 : 180) + (riderMode ? 30 : 0);
		if (!canStartCast(player, FROZEN_PATH, mana, 0))
			return;
		ServerLevel level = player.serverLevel();
		Vec3 direction = pursuitLookDirection(player, riderMode);
		Vec3 riderForward = horizontal(direction);
		if (riderForward.lengthSqr() < 0.01D)
			riderForward = Vec3.directionFromRotation(0.0F, player.getYRot());
		Vec3 start = riderMode
				? player.position().add(riderForward.normalize().scale(0.72D)).add(0.0D, 0.06D, 0.0D)
				: player.getEyePosition().add(direction.scale(0.82D));
		GlacialPursuitEntity pursuit = GlacialPursuitEntity.spawn(level, player, start, direction,
				riderMode, manifested);
		if (!pursuit.isAddedToWorld()) {
			pursuit.discard();
			message(player, "The frozen current failed to form.", ChatFormatting.RED);
			return;
		}
		consumeMana(player, mana);
		long now = level.getGameTime();
		GlacialPursuitState state = new GlacialPursuitState(level.dimension(), pursuit.getUUID(),
				direction, start, now, now + (manifested ? 120 : 100),
				riderMode, manifested);
		GLACIAL_PURSUITS.put(player.getUUID(), state);
		if (riderMode)
			player.startRiding(pursuit, true);
		level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2,
				SoundSource.PLAYERS, 0.9F, manifested ? 0.72F : 0.86F);
	}

	public static void releaseFrozenPath(Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return;
		GlacialPursuitState state = GLACIAL_PURSUITS.get(player.getUUID());
		if (state != null)
			endGlacialPursuit(player, state, true, true);
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
		clearObsoleteSpiritualizationCooldowns(player);
		if (isSpiritualized(player)) {
			endSpiritualization(player, false);
			return;
		}
		if (!player.isCreative() && variables(player).MP < SPIRITUALIZATION_MANA_PER_SECOND) {
			message(player, "Not enough MP to sustain Frost Spiritualization.", ChatFormatting.RED);
			return;
		}
		clearLegacyProgress(player);
		player.getPersistentData().putBoolean(SPIRITUALIZED, true);
		player.getPersistentData().remove(LEGACY_SPIRITUALIZED_UNTIL);
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
		clearObsoleteSpiritualizationCooldowns(player);
		if (player.getPersistentData().getBoolean(SPIRITUALIZED)) {
			player.getPersistentData().remove(LEGACY_SPIRITUALIZED_UNTIL);
			if (player.tickCount % 20 == 0 && !drainSpiritualizationMana(player))
				endSpiritualization(player, true);
		}
		clearLegacyProgress(player);
		updateSpear(player, now);
		updateFrostbite(player, now);
		updateFrozenTargets(player, now);
		updateFrostCounter(player, now);
		updateAbsoluteZero(player, now);
		updateStillness(player, now);
		updateGlacialPursuit(player, now);
		updateWinter(player, now);
		updateWhiteout(player, now);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onFrostCounterHurt(LivingHurtEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide())
			return;
		if (event.getSource().is(DamageTypes.FALL)
				&& player.getPersistentData().getLong(PATH_FALL_PROTECTION_UNTIL) >= player.level().getGameTime()) {
			event.setCanceled(true);
			player.fallDistance = 0.0F;
			return;
		}
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
		dealFrostDamage(player, attacker, frostCounterDamage(player, attacker, state.manifested), 4, true);
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
		event.setAmount(event.getAmount() * (state.manifested ? 1.55F : 1.40F)
				+ shatterFlatBonus(attacker, event.getEntity()));
		emitShatter(attacker, event.getEntity(), state.manifested);
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
	public static void onGlacialPursuitDismount(EntityMountEvent event) {
		if (event.isMounting() || event.getEntityMounting().level().isClientSide()
				|| !(event.getEntityMounting() instanceof ServerPlayer player)
				|| !(event.getEntityBeingMounted() instanceof GlacialPursuitEntity pursuit))
			return;
		GlacialPursuitState state = GLACIAL_PURSUITS.get(player.getUUID());
		if (state != null && state.entityId.equals(pursuit.getUUID())
				&& player.isAlive() && !player.isChangingDimension())
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			releaseFreeze(player);
			clearAll(player, false);
		}
	}

	@SubscribeEvent
	public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			releaseFreeze(player);
			clearAll(player, false);
		}
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		FROSTBITE.remove(event.getEntity().getUUID());
		releaseFreeze(event.getEntity());
		if (event.getEntity() instanceof ServerPlayer player)
			clearAll(player, true);
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		GLACIAL_PURSUITS.clear();
		FROSTBITE.clear();
		FROZEN_TARGETS.clear();
		FROST_COUNTERS.clear();
		ABSOLUTE_ZEROS.clear();
	}

	private static boolean dealFrostDamage(ServerPlayer owner, LivingEntity target, float amount,
			int frostbite, boolean canShatter) {
		if (!validTarget(owner, target))
			return false;
		FrozenTargetState frozen = FROZEN_TARGETS.get(target.getUUID());
		if (canShatter && frozen != null && owner.getUUID().equals(frozen.ownerId)) {
			releaseFreeze(target);
			float shatterDamage = amount * (frozen.manifested ? 1.55F : 1.40F)
					+ shatterFlatBonus(owner, target);
			target.invulnerableTime = 0;
			boolean hurt = hurtFrostRaw(owner, target, shatterDamage);
			emitShatter(owner, target, frozen.manifested);
			return hurt;
		}
		boolean hurt = hurtFrostRaw(owner, target, amount);
		if (hurt && frostbite > 0)
			applyFrostbite(owner, target, frostbite, isSpiritualized(owner));
		return hurt;
	}

	private static boolean hurtFrostRaw(ServerPlayer owner, LivingEntity target, float amount) {
		if (!validTarget(owner, target))
			return false;
		boolean hurt = target.hurt(owner.damageSources().indirectMagic(owner, owner), Math.max(0.5F, amount));
		if (hurt)
			target.setLastHurtByPlayer(owner);
		return hurt;
	}

	private static void applyFrostbite(ServerPlayer owner, LivingEntity target, int amount, boolean manifested) {
		if (!validTarget(owner, target) || FROZEN_TARGETS.containsKey(target.getUUID()))
			return;
		long now = owner.level().getGameTime();
		FrostbiteState state = FROSTBITE.get(target.getUUID());
		if (state == null || !owner.getUUID().equals(state.ownerId)
				|| state.dimension != owner.level().dimension() || now > state.expiresAt) {
			state = new FrostbiteState(owner.getUUID(), owner.level().dimension(), 0, now + FROSTBITE_LIFETIME);
			FROSTBITE.put(target.getUUID(), state);
		}
		state.stacks = Math.min(MAX_FROSTBITE, state.stacks + Math.max(1, amount));
		state.expiresAt = now + FROSTBITE_LIFETIME;
		target.setTicksFrozen(Math.max(target.getTicksFrozen(), state.stacks * 24));
		if (state.stacks >= MAX_FROSTBITE) {
			FROSTBITE.remove(target.getUUID());
			applyFreeze(owner, target, manifested ? 44 : 34, manifested);
			owner.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getEyeY(), target.getZ(),
					24, 0.34D, 0.48D, 0.34D, 0.05D);
			owner.level().playSound(null, target.blockPosition(), SoundEvents.GLASS_PLACE,
					SoundSource.PLAYERS, 0.72F, 1.42F);
		}
	}

	private static int frostbiteStacks(ServerPlayer owner, LivingEntity target) {
		FrostbiteState state = FROSTBITE.get(target.getUUID());
		return state != null && owner.getUUID().equals(state.ownerId) ? state.stacks : 0;
	}

	private static void updateFrostbite(ServerPlayer owner, long now) {
		List<UUID> remove = new ArrayList<>();
		for (Map.Entry<UUID, FrostbiteState> entry : FROSTBITE.entrySet()) {
			FrostbiteState state = entry.getValue();
			if (!owner.getUUID().equals(state.ownerId))
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
			if ((now & 7L) == 0L)
				owner.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getEyeY(), target.getZ(),
						Math.min(4, state.stacks), 0.22D, 0.3D, 0.22D, 0.012D);
		}
		for (UUID id : remove)
			FROSTBITE.remove(id);
	}

	private static float shatterFlatBonus(ServerPlayer owner, LivingEntity target) {
		double intelligence = variables(owner).Intelligence;
		double healthContribution = Math.min(isBoss(target) ? 10.0D : 16.0D, target.getMaxHealth() * 0.025D);
		double damage = 5.0D + intelligence / 40.0D + healthContribution;
		if (target instanceof Player)
			damage *= 0.60D;
		return (float) damage;
	}

	private static void emitShatter(ServerPlayer owner, LivingEntity shattered, boolean manifested) {
		ServerLevel level = owner.serverLevel();
		Vec3 center = shattered.getBoundingBox().getCenter();
		double radius = manifested ? 5.0D : 3.6D;
		int maximum = manifested ? 9 : 6;
		float shardDamage = (float) ((manifested ? 7.0D : 5.0D) + variables(owner).Intelligence / 52.0D);
		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
				shattered.getBoundingBox().inflate(radius), candidate -> candidate != shattered && validTarget(owner, candidate));
		nearby.sort(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(center)));
		for (int i = 0; i < Math.min(maximum, nearby.size()); i++) {
			LivingEntity target = nearby.get(i);
			if (hurtFrostRaw(owner, target, shardDamage))
				applyFrostbite(owner, target, 1, manifested);
			spawnParticleLine(level, center, target.getBoundingBox().getCenter(), ParticleTypes.SNOWFLAKE, 5);
		}
		level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z, manifested ? 52 : 36,
				0.52D, 0.62D, 0.52D, 0.12D);
		level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, manifested ? 12 : 8,
				0.38D, 0.44D, 0.38D, 0.08D);
		level.playSound(null, shattered.blockPosition(), SoundEvents.GLASS_BREAK,
				SoundSource.PLAYERS, 1.0F, manifested ? 1.35F : 1.55F);
	}

	private static void applyFreeze(ServerPlayer owner, LivingEntity target, int duration, boolean manifested) {
		if (!validTarget(owner, target))
			return;
		FROSTBITE.remove(target.getUUID());
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
			detonateAbsoluteZero(player, state, 0.78F);
			return;
		}
		double radius = state.manifested ? 10.0D : 8.0D;
		boolean pulse = now >= state.nextPulseAt;
		if (pulse) {
			state.nextPulseAt = now + 10;
			state.pulseCount++;
		}
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(radius, 4.0D, radius), candidate -> validTarget(player, candidate))) {
			if (target.distanceToSqr(player) > radius * radius)
				continue;
			Vec3 movement = target.getDeltaMovement();
			double slow = target instanceof Player ? 0.90D : isBoss(target) ? 0.74D : 0.64D;
			target.setDeltaMovement(movement.x * slow, movement.y, movement.z * slow);
			target.hurtMarked = true;
			if (pulse)
				dealFrostDamage(player, target, absoluteZeroPulseDamage(player, target, state.manifested),
						state.pulseCount % 2 == 0 ? 1 : 0, false);
		}
		if ((now & 3L) == 0L)
			showAbsoluteZero(player, radius);
	}

	private static void detonateAbsoluteZero(ServerPlayer player, AbsoluteZeroState state, float power) {
		if (ABSOLUTE_ZEROS.get(player.getUUID()) != state)
			return;
		ABSOLUTE_ZEROS.remove(player.getUUID());
		double radius = state.manifested ? 11.0D : 9.0D;
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(radius, 5.0D, radius), candidate -> validTarget(player, candidate))) {
			if (target.distanceToSqr(player) > radius * radius)
				continue;
			int stacks = frostbiteStacks(player, target);
			float damage = absoluteZeroDetonationDamage(player, target, state.manifested)
					* power * (1.0F + stacks * 0.08F);
			dealFrostDamage(player, target, damage, state.manifested ? 2 : 1, true);
			Vec3 away = target.position().subtract(player.position());
			if (away.lengthSqr() > 0.01D && !isBoss(target)) {
				Vec3 force = away.normalize().scale(target instanceof Player ? 0.32D : 0.58D);
				target.setDeltaMovement(force.x, Math.max(0.16D, target.getDeltaMovement().y), force.z);
				target.hurtMarked = true;
			}
		}
		player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.4D,
				player.getZ(), state.manifested ? 110 : 78, radius * 0.55D, 0.55D, radius * 0.55D, 0.13D);
		player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.25D,
				player.getZ(), state.manifested ? 24 : 16, radius * 0.42D, 0.25D, radius * 0.42D, 0.08D);
		player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE,
				SoundSource.PLAYERS, 1.0F, 1.55F);
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
			if (dealFrostDamage(player, target, damage, 2, true))
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
			dealFrostDamage(player, target, primary * multiplier, same ? 1 : 2, true);
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
		if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos),
				entity -> entity.isAlive() && entity != player).isEmpty())
			return FrozenCellResult.SKIPPED;
		BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
		int returnLevel = water && oldState.hasProperty(BlockStateProperties.LEVEL)
				? oldState.getValue(BlockStateProperties.LEVEL)
				: FrostCausewayBlock.RETURN_AIR;
		BlockState frost = SololevelingModBlocks.FROST_CAUSEWAY.get().defaultBlockState()
				.setValue(FrostCausewayBlock.RETURN_LEVEL, returnLevel);
		if (!level.setBlock(pos, frost, 2))
			return FrozenCellResult.SKIPPED;
		if (ForgeEventFactory.onBlockPlace(player, snapshot, Direction.UP)) {
			snapshot.restore(true, false);
			return FrozenCellResult.SKIPPED;
		}
		FrostCausewayBlock.refresh(level, pos, level.getBlockState(pos));
		return FrozenCellResult.TRACKED;
	}

	private static void updateGlacialPursuit(ServerPlayer player, long now) {
		GlacialPursuitState state = GLACIAL_PURSUITS.get(player.getUUID());
		if (state == null)
			return;
		if (state.dimension != player.level().dimension()) {
			endGlacialPursuit(player, state, false, false);
			return;
		}
		if (now > state.expiresAt) {
			endGlacialPursuit(player, state, true, true);
			return;
		}
		Entity raw = player.serverLevel().getEntity(state.entityId);
		if (!(raw instanceof GlacialPursuitEntity pursuit) || !pursuit.isAlive()) {
			endGlacialPursuit(player, state, false, true);
			return;
		}
		if (state.riderMode && !pursuit.hasPassenger(player)) {
			if (!player.isAlive()) {
				endGlacialPursuit(player, state, false, false);
				return;
			}
			if (player.distanceToSqr(pursuit) > 9.0D)
				player.teleportTo(pursuit.getX(), pursuit.getY() + pursuit.getPassengersRidingOffset(), pursuit.getZ());
			if (!player.startRiding(pursuit, true)) {
				state.remountFailures++;
				pursuit.setFlight(pursuit.position(), Vec3.ZERO);
				if (state.remountFailures > PURSUIT_REMOUNT_GRACE_TICKS)
					endGlacialPursuit(player, state, true, true);
				return;
			}
			state.remountFailures = 0;
		}
		if (now >= state.nextManaAt) {
			state.nextManaAt = now + 5;
			int upkeep = (state.riderMode ? 14 : 10) + (state.manifested ? 2 : 0);
			if (variables(player).MP < upkeep) {
				message(player, "Glacial Pursuit ended: not enough MP.", ChatFormatting.RED);
				endGlacialPursuit(player, state, true, true);
				return;
			}
			consumeMana(player, upkeep);
		}

		Vec3 current = pursuit.position();
		Vec3 look = pursuitLookDirection(player, state.riderMode);
		Vec3 desired = state.riderMode
				? look
				: player.getEyePosition().add(look.scale(42.0D)).subtract(current);
		if (desired.lengthSqr() < 0.01D)
			desired = look;
		state.direction = steerDirection(state.direction, desired.normalize(),
				Math.toRadians(state.riderMode ? 6.0D : 8.5D));
		double speed = state.riderMode
				? (state.manifested ? 1.05D : 0.90D)
				: (state.manifested ? 1.30D : 1.15D);
		Vec3 movement = state.direction.scale(speed);
		Vec3 next = current.add(movement);
		BlockPos nextPos = BlockPos.containing(next);
		ServerLevel level = player.serverLevel();
		if (!level.hasChunkAt(nextPos) || !level.getWorldBorder().isWithinBounds(nextPos)
				|| next.y <= level.getMinBuildHeight() + 1 || next.y >= level.getMaxBuildHeight() - 2) {
			endGlacialPursuit(player, state, false, true);
			return;
		}
		BlockHitResult blockHit = level.clip(new ClipContext(current, next, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, pursuit));
		if (blockHit.getType() != HitResult.Type.MISS
				&& !isPursuitPassableIce(state, level.getBlockState(blockHit.getBlockPos()))) {
			boolean steppingOffSupport = state.riderMode && movement.y < -0.02D
					&& blockHit.getDirection() == Direction.UP
					&& current.distanceTo(blockHit.getLocation()) <= 0.38D;
			if (!steppingOffSupport && state.riderMode
					&& blockHit.getDirection() == Direction.UP) {
				Vec3 flat = horizontal(state.direction);
				if (flat.lengthSqr() < 0.01D) {
					pauseBlockedPursuit(player, state, pursuit);
					return;
				}
				movement = flat.normalize().scale(speed);
				next = current.add(movement);
				BlockHitResult wallCheck = level.clip(new ClipContext(current, next, ClipContext.Block.COLLIDER,
						ClipContext.Fluid.NONE, pursuit));
				if (wallCheck.getType() != HitResult.Type.MISS
						&& !isPursuitPassableIce(state, level.getBlockState(wallCheck.getBlockPos()))) {
					pauseBlockedPursuit(player, state, pursuit);
					return;
				}
			} else if (!steppingOffSupport) {
				if (state.riderMode) {
					pauseBlockedPursuit(player, state, pursuit);
					return;
				}
				pursuit.setFlight(blockHit.getLocation().subtract(state.direction.scale(0.12D)), Vec3.ZERO);
				endGlacialPursuit(player, state, true, true);
				return;
			}
		}
		if (state.riderMode && pursuit.hasPassenger(player)
				&& !hasRiderClearance(level, player, movement)) {
			Vec3 flat = horizontal(state.direction);
			Vec3 fallback = flat.lengthSqr() < 0.01D ? Vec3.ZERO : flat.normalize().scale(speed);
			if (fallback.lengthSqr() < 0.01D || !hasRiderClearance(level, player, fallback)) {
				pauseBlockedPursuit(player, state, pursuit);
				return;
			}
			movement = fallback;
			next = current.add(movement);
		}

		state.blockedTicks = 0;
		pursuit.setFlight(next, movement);
		state.distanceTravelled += speed;
		layPursuitTrail(player, state, state.lastTrailPoint, next);
		state.lastTrailPoint = next;
		hitPursuitTargets(player, state, current, next, now);
		if ((now & 1L) == 0L)
			level.sendParticles(ParticleTypes.SNOWFLAKE, next.x, next.y, next.z,
					state.manifested ? 5 : 3, 0.22D, 0.22D, 0.22D, 0.025D);
	}

	private static boolean hasRiderClearance(ServerLevel level, ServerPlayer player, Vec3 movement) {
		AABB moved = player.getBoundingBox().move(movement);
		double horizontalInset = Math.min(0.08D, moved.getXsize() * 0.16D);
		AABB body = new AABB(moved.minX + horizontalInset, moved.minY + 0.48D,
				moved.minZ + horizontalInset, moved.maxX - horizontalInset,
				moved.maxY - 0.04D, moved.maxZ - horizontalInset);
		if (body.maxY <= body.minY)
			return true;
		for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(body.minX), Mth.floor(body.minY), Mth.floor(body.minZ),
				Mth.floor(body.maxX), Mth.floor(body.maxY), Mth.floor(body.maxZ))) {
			BlockState block = level.getBlockState(pos);
			if (block.is(SololevelingModBlocks.FROST_CAUSEWAY.get()))
				continue;
			for (AABB collision : block.getCollisionShape(level, pos).toAabbs()) {
				if (body.intersects(collision.move(pos.getX(), pos.getY(), pos.getZ())))
					return false;
			}
		}
		return true;
	}

	private static boolean isPursuitPassableIce(GlacialPursuitState pursuit, BlockState block) {
		if (block.is(SololevelingModBlocks.FROST_CAUSEWAY.get()))
			return true;
		if (pursuit.riderMode)
			return false;
		return block.is(net.minecraft.world.level.block.Blocks.ICE)
				|| block.is(net.minecraft.world.level.block.Blocks.PACKED_ICE)
				|| block.is(net.minecraft.world.level.block.Blocks.BLUE_ICE)
				|| block.is(net.minecraft.world.level.block.Blocks.FROSTED_ICE);
	}

	private static void pauseBlockedPursuit(ServerPlayer player, GlacialPursuitState state,
			GlacialPursuitEntity pursuit) {
		pursuit.setFlight(pursuit.position(), Vec3.ZERO);
		state.blockedTicks++;
		if (state.blockedTicks > PURSUIT_BLOCK_GRACE_TICKS)
			endGlacialPursuit(player, state, true, true);
	}

	private static void layPursuitTrail(ServerPlayer player, GlacialPursuitState state, Vec3 start, Vec3 end) {
		double length = start.distanceTo(end);
		int samples = Math.max(1, Mth.ceil(length / 0.42D));
		for (int i = 0; i <= samples; i++) {
			Vec3 point = start.lerp(end, i / (double) samples);
			double floorOffset = state.riderMode ? 0.86D : 1.62D;
			BlockPos center = BlockPos.containing(point.x, point.y - floorOffset, point.z);
			Direction forward = Direction.fromYRot(yawFor(state.direction));
			Direction right = forward.getClockWise();
			Vec3 centerPoint = Vec3.atCenterOf(center);
			Vec3 rightVector = Vec3.atLowerCornerOf(right.getNormal());
			double lateral = point.subtract(centerPoint).dot(rightVector);
			Direction secondDirection = lateral >= 0.0D ? right : right.getOpposite();
			placePursuitCell(player, state, center);
			placePursuitCell(player, state, center.relative(secondDirection));
		}
	}

	private static void placePursuitCell(ServerPlayer player, GlacialPursuitState state, BlockPos pos) {
		BlockPos immutable = pos.immutable();
		if (state.trailCells.contains(immutable))
			return;
		if (freezePathCell(player, immutable).tracked())
			state.trailCells.add(immutable);
	}

	private static void hitPursuitTargets(ServerPlayer player, GlacialPursuitState state,
			Vec3 start, Vec3 end, long now) {
		double radius = state.manifested ? 1.22D : 0.96D;
		AABB sweep = new AABB(start, end).inflate(radius);
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, sweep,
				candidate -> validTarget(player, candidate))) {
			if (target.getBoundingBox().inflate(radius).clip(start, end).isEmpty()
					|| now < state.nextHitAt.getOrDefault(target.getUUID(), 0L))
				continue;
			state.nextHitAt.put(target.getUUID(), now + 18);
			if (!dealFrostDamage(player, target, glacialPursuitDamage(player, target, state.manifested), 2, true))
				continue;
			double force = target instanceof Player ? 0.52D : isBoss(target) ? 0.24D : 1.05D;
			Vec3 launch = state.direction.scale(force);
			target.setDeltaMovement(launch.x, Math.max(target.getDeltaMovement().y, 0.16D * force), launch.z);
			target.hurtMarked = true;
			player.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
					net.minecraft.world.level.block.Blocks.PACKED_ICE.defaultBlockState()),
					target.getX(), target.getEyeY(), target.getZ(), state.manifested ? 18 : 12,
					0.32D, 0.38D, 0.32D, 0.08D);
		}
	}

	private static void endGlacialPursuit(ServerPlayer player, GlacialPursuitState state,
			boolean burst, boolean startCooldown) {
		if (GLACIAL_PURSUITS.get(player.getUUID()) != state)
			return;
		GLACIAL_PURSUITS.remove(player.getUUID());
		ServerLevel pursuitLevel = player.getServer().getLevel(state.dimension);
		Entity raw = pursuitLevel == null ? null : pursuitLevel.getEntity(state.entityId);
		Vec3 end = raw == null ? state.lastTrailPoint : raw.position();
		if (burst && state.dimension.equals(player.level().dimension()))
			pursuitReleaseBurst(player, state, end);
		if (raw != null) {
			raw.ejectPassengers();
			raw.discard();
		}
		if (state.riderMode) {
			if (player.isPassenger())
				player.stopRiding();
			player.fallDistance = 0.0F;
			if (startCooldown) {
				Vec3 exit = state.direction.scale(state.manifested ? 0.76D : 0.62D);
				player.setDeltaMovement(exit.x, Math.max(0.12D, exit.y), exit.z);
				player.hurtMarked = true;
				player.getPersistentData().putLong(PATH_FALL_PROTECTION_UNTIL,
						player.level().getGameTime() + 50);
			}
		}
		if (startCooldown) {
			CooldownManager.set(player, FROZEN_PATH, state.manifested ? 140 : 160);
			CooldownManager.set(player, "mana_refresh", 40);
		}
	}

	private static void pursuitReleaseBurst(ServerPlayer player, GlacialPursuitState state, Vec3 center) {
		double radius = state.manifested ? 4.2D : 3.2D;
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
				new AABB(center, center).inflate(radius), candidate -> validTarget(player, candidate))) {
			if (target.distanceToSqr(center) > radius * radius)
				continue;
			dealFrostDamage(player, target,
					glacialPursuitDamage(player, target, state.manifested) * 0.48F, 1, true);
		}
		player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z,
				state.manifested ? 48 : 32, 0.75D, 0.75D, 0.75D, 0.14D);
		player.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
				net.minecraft.world.level.block.Blocks.PACKED_ICE.defaultBlockState()),
				center.x, center.y, center.z, state.manifested ? 28 : 18,
				0.62D, 0.62D, 0.62D, 0.11D);
		player.level().playSound(null, BlockPos.containing(center), SoundEvents.GLASS_BREAK,
				SoundSource.PLAYERS, 0.9F, 1.18F);
	}

	private static Vec3 pursuitLookDirection(ServerPlayer player, boolean riderMode) {
		float pitch = riderMode
				? Mth.clamp(player.getXRot(), -28.0F, 22.0F)
				: Mth.clamp(player.getXRot(), -55.0F, 55.0F);
		return Vec3.directionFromRotation(pitch, player.getYRot()).normalize();
	}

	private static Vec3 steerDirection(Vec3 current, Vec3 desired, double maximumAngle) {
		if (current.lengthSqr() < 1.0E-6D)
			return desired;
		Vec3 from = current.normalize();
		double dot = Mth.clamp(from.dot(desired), -1.0D, 1.0D);
		double angle = Math.acos(dot);
		if (angle <= maximumAngle)
			return desired;
		double blend = maximumAngle / Math.max(1.0E-5D, angle);
		Vec3 steered = from.scale(1.0D - blend).add(desired.scale(blend));
		return steered.lengthSqr() < 1.0E-6D ? desired : steered.normalize();
	}

	private static float yawFor(Vec3 direction) {
		return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
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

	private static boolean drainSpiritualizationMana(ServerPlayer player) {
		if (player.isCreative())
			return true;
		boolean enoughMana = variables(player).MP >= SPIRITUALIZATION_MANA_PER_SECOND;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(data -> {
			data.MP = enoughMana
					? Math.max(0.0D, data.MP - SPIRITUALIZATION_MANA_PER_SECOND)
					: 0.0D;
			data.syncPlayerVariables(player);
		});
		if (enoughMana)
			CooldownManager.set(player, "mana_refresh", 35);
		return enoughMana;
	}

	private static void clearObsoleteSpiritualizationCooldowns(ServerPlayer player) {
		if (CooldownManager.getRemainingTicks(player, LEGACY_SPIRITUALIZATION_COOLDOWN) > 0)
			CooldownManager.clear(player, LEGACY_SPIRITUALIZATION_COOLDOWN);
		if (CooldownManager.getRemainingTicks(player, SPIRITUALIZATION) > 0)
			CooldownManager.clear(player, SPIRITUALIZATION);
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

	private static void endSpiritualization(ServerPlayer player, boolean insufficientMana) {
		if (!player.getPersistentData().getBoolean(SPIRITUALIZED))
			return;
		player.getPersistentData().remove(SPIRITUALIZED);
		player.getPersistentData().remove(LEGACY_SPIRITUALIZED_UNTIL);
		PlayerAuraSystem.clearContinuous(player);
		clearLegacyProgress(player);
		player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
				SoundSource.PLAYERS, 0.8F, 1.25F);
		if (insufficientMana)
			message(player, "Frost Spiritualization ended: insufficient MP.", ChatFormatting.RED);
	}

	private static void clearAll(ServerPlayer player, boolean endForm) {
		removeSpear(player.getUUID(), player.getServer());
		GlacialPursuitState pursuit = GLACIAL_PURSUITS.get(player.getUUID());
		if (pursuit != null)
			endGlacialPursuit(player, pursuit, false, false);
		STILLNESS.remove(player.getUUID());
		WINTERS.remove(player.getUUID());
		WHITEOUTS.remove(player.getUUID());
		FROST_COUNTERS.remove(player.getUUID());
		ABSOLUTE_ZEROS.remove(player.getUUID());
		FROSTBITE.entrySet().removeIf(entry -> entry.getValue().ownerId.equals(player.getUUID()));
		releaseFreezesOwnedBy(player);
		if (endForm && player.getPersistentData().getBoolean(SPIRITUALIZED))
			endSpiritualization(player, false);
		else
			clearLegacyProgress(player);
	}

	private static boolean hasAnyState(ServerPlayer player) {
		UUID id = player.getUUID();
		return player.getPersistentData().getBoolean(SPIRITUALIZED)
				|| SPEARS.containsKey(id) || STILLNESS.containsKey(id) || GLACIAL_PURSUITS.containsKey(id)
				|| WINTERS.containsKey(id) || WHITEOUTS.containsKey(id) || FROST_COUNTERS.containsKey(id)
				|| ABSOLUTE_ZEROS.containsKey(id)
				|| FROSTBITE.values().stream().anyMatch(state -> state.ownerId.equals(id))
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
		double damage = 22.0D + data.Strength / 8.0D + data.Intelligence / 14.0D;
		if (target instanceof Player)
			damage *= 0.75D;
		if (manifested)
			damage *= 1.15D;
		return (float) damage;
	}

	private static float flashFreezeDamage(ServerPlayer player, LivingEntity target, boolean manifested) {
		SololevelingModVariables.PlayerVariables data = variables(player);
		return scaleFrostDamage(12.0D + data.Strength / 18.0D + data.Intelligence / 10.0D,
				target, manifested);
	}

	private static float frostCounterDamage(ServerPlayer player, LivingEntity target, boolean manifested) {
		SololevelingModVariables.PlayerVariables data = variables(player);
		return scaleFrostDamage(16.0D + data.Strength / 12.0D + data.Intelligence / 12.0D,
				target, manifested);
	}

	private static float glacialPursuitDamage(ServerPlayer player, LivingEntity target, boolean manifested) {
		SololevelingModVariables.PlayerVariables data = variables(player);
		return scaleFrostDamage(14.0D + data.Strength / 14.0D + data.Intelligence / 10.0D,
				target, manifested);
	}

	private static float absoluteZeroPulseDamage(ServerPlayer player, LivingEntity target, boolean manifested) {
		SololevelingModVariables.PlayerVariables data = variables(player);
		return scaleFrostDamage(4.0D + data.Strength / 50.0D + data.Intelligence / 30.0D,
				target, manifested);
	}

	private static float absoluteZeroDetonationDamage(ServerPlayer player, LivingEntity target,
			boolean manifested) {
		SololevelingModVariables.PlayerVariables data = variables(player);
		return scaleFrostDamage(18.0D + data.Strength / 14.0D + data.Intelligence / 9.0D,
				target, manifested);
	}

	private static float scaleFrostDamage(double damage, LivingEntity target, boolean manifested) {
		if (target instanceof Player)
			damage *= 0.65D;
		else if (isBoss(target))
			damage *= 0.80D;
		if (manifested)
			damage *= 1.20D;
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

	private static final class GlacialPursuitState {
		private final ResourceKey<Level> dimension;
		private final UUID entityId;
		private final long startedAt;
		private final long expiresAt;
		private final boolean riderMode;
		private final boolean manifested;
		private final Set<BlockPos> trailCells = new HashSet<>();
		private final Map<UUID, Long> nextHitAt = new HashMap<>();
		private Vec3 direction;
		private Vec3 lastTrailPoint;
		private long nextManaAt;
		private double distanceTravelled;
		private int blockedTicks;
		private int remountFailures;

		private GlacialPursuitState(ResourceKey<Level> dimension, UUID entityId, Vec3 direction,
				Vec3 lastTrailPoint, long startedAt, long expiresAt,
				boolean riderMode, boolean manifested) {
			this.dimension = dimension;
			this.entityId = entityId;
			this.direction = direction;
			this.lastTrailPoint = lastTrailPoint;
			this.startedAt = startedAt;
			this.expiresAt = expiresAt;
			this.riderMode = riderMode;
			this.manifested = manifested;
			this.nextManaAt = startedAt + 5;
		}
	}

	private static final class FrostbiteState {
		private final UUID ownerId;
		private final ResourceKey<Level> dimension;
		private int stacks;
		private long expiresAt;

		private FrostbiteState(UUID ownerId, ResourceKey<Level> dimension, int stacks, long expiresAt) {
			this.ownerId = ownerId;
			this.dimension = dimension;
			this.stacks = stacks;
			this.expiresAt = expiresAt;
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
		private long nextPulseAt;
		private int pulseCount;

		private AbsoluteZeroState(long expiresAt, long nextPulseAt, boolean manifested) {
			this.expiresAt = expiresAt;
			this.nextPulseAt = nextPulseAt;
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

	private record FrozenCellResult(boolean tracked) {
		private static final FrozenCellResult SKIPPED = new FrozenCellResult(false);
		private static final FrozenCellResult TRACKED = new FrozenCellResult(true);
	}

	private record WinterSample(Vec3 position, float yaw, float pitch, Vec3 velocity) {
	}
}
