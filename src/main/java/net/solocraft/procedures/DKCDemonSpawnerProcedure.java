package net.solocraft.procedures;

import net.solocraft.SololevelingMod;
import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dkc.DkcSpatialLayout;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.entity.DemonEntity;
import net.solocraft.entity.DemonKnightEntity;
import net.solocraft.init.SololevelingModEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

/** Bounded, staged DKC waves with at most 7-12 active enemies per player. */
public class DKCDemonSpawnerProcedure {
	public static final String ROLE_TAG = "dkc_encounter_role";
	public static final String FLOOR_WAVE_ROLE = "floor_wave";
	public static final String MINIBOSS_TAG = "dkc_miniboss";
	public static final String ATTEMPT_TAG = "dkc_wave_attempt";
	private static final String ATTEMPT_SUFFIX = "_attempt";
	private static final String MINIBOSS_SPAWNED_SUFFIX = "_miniboss_spawned";
	private static final String SPAWN_RETRY_AFTER_SUFFIX = "_spawn_retry_after";
	private static final int MAX_BATCH_SPAWNS = 6;
	private static final long FAILED_SPAWN_RETRY_TICKS = 200L;
	private static final ResourceLocation MINIBOSS_HEALTH_MODIFIER = modifierId("health");
	private static final ResourceLocation MINIBOSS_DAMAGE_MODIFIER = modifierId("damage");
	private static final ResourceLocation MINIBOSS_ARMOR_MODIFIER = modifierId("armor");
	private static final ResourceLocation MINIBOSS_TOUGHNESS_MODIFIER = modifierId("armor_toughness");
	private static final ResourceLocation MINIBOSS_KNOCKBACK_MODIFIER = modifierId("knockback_resistance");
	private static final ResourceLocation MINIBOSS_SPEED_MODIFIER = modifierId("movement_speed");

	public static void execute(LevelAccessor world, Entity entity) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof ServerPlayer player))
			return;
		int floor = DkcSpatialLayout.floor(player);
		if (floor < 2 || floor > 19)
			return;
		GameType mode = player.gameMode.getGameModeForPlayer();
		if (mode != GameType.SURVIVAL && mode != GameType.ADVENTURE)
			return;

		CompoundTag data = player.getPersistentData();
		String prefix = prefix(floor);
		if (data.getBoolean(prefix + "_spawned"))
			return;
		startNewAttempt(data, prefix);
		data.putDouble(prefix + "_required", DkcFloorRegistry.requiredKills(floor));
		data.putDouble(prefix + "_killed", 0.0D);
		data.putBoolean(prefix + "_spawned", true);
		data.putBoolean(prefix + "_spawning", true);
		data.putBoolean(prefix + "_initial_spawned", false);
		data.putBoolean(prefix + "_complete", false);
		data.putBoolean(prefix + MINIBOSS_SPAWNED_SUFFIX, false);
		data.putLong(prefix + "_enter_time", level.getGameTime());
	}

	public static void checkDelayedSpawn(LevelAccessor world, Entity entity) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof ServerPlayer player))
			return;
		int floor = DkcSpatialLayout.floor(player);
		if (floor < 2 || floor > 19)
			return;
		CompoundTag data = player.getPersistentData();
		String prefix = prefix(floor);
		if (!data.getBoolean(prefix + "_spawned"))
			return;
		ensureAttempt(data, prefix);
		if (data.getBoolean(prefix + "_complete")) {
			if (floor == 10 && level.getGameTime() % 40L == 0L)
				DkcFloorBuilder.ensureBosses(player, floor);
			return;
		}

		long elapsed = level.getGameTime() - data.getLong(prefix + "_enter_time");
		if (!data.getBoolean(prefix + "_initial_spawned")) {
			if (elapsed < 60L)
				return;
			data.putBoolean(prefix + "_initial_spawned", true);
			data.putBoolean(prefix + "_spawning", false);
			spawnToCap(level, player, floor);
			return;
		}
		if (level.getGameTime() % 100L == Math.floorMod(player.getId(), 100))
			spawnToCap(level, player, floor);
	}

	private static void spawnToCap(ServerLevel level, ServerPlayer player, int floor) {
		CompoundTag data = player.getPersistentData();
		String floorPrefix = prefix(floor);
		if (level.getGameTime() < data.getLong(floorPrefix + SPAWN_RETRY_AFTER_SUFFIX))
			return;
		int killed = (int) data.getDouble(floorPrefix + "_killed");
		int required = DkcFloorRegistry.requiredKills(floor);
		int remaining = Math.max(0, required - killed);
		if (remaining == 0)
			return;
		int alive = aliveCount(level, player, floor);
		int desired = Math.min(DkcFloorRegistry.activeEnemyCap(floor), remaining);
		int requested = Math.max(0, desired - alive);
		if (requested <= 0)
			return;
		int spawned = spawnBatch(level, player, floor, requested);
		if (spawned == 0)
			data.putLong(floorPrefix + SPAWN_RETRY_AFTER_SUFFIX,
					level.getGameTime() + FAILED_SPAWN_RETRY_TICKS);
		else
			data.remove(floorPrefix + SPAWN_RETRY_AFTER_SUFFIX);
	}

	private static int spawnBatch(ServerLevel level, ServerPlayer player, int floor, int requested) {
		int count = Math.min(MAX_BATCH_SPAWNS,
				Math.min(requested, DkcFloorRegistry.activeEnemyCap(floor)));
		int spawned = 0;
		CompoundTag playerData = player.getPersistentData();
		int waveAttempt = ensureAttempt(playerData, prefix(floor));
		String minibossKey = prefix(floor) + MINIBOSS_SPAWNED_SUFFIX;
		boolean minibossSpawned = playerData.getBoolean(minibossKey);
		if (!minibossSpawned && hasOwnedMiniboss(level, player, floor, waveAttempt)) {
			minibossSpawned = true;
			playerData.putBoolean(minibossKey, true);
		}
		for (int index = 0; index < count; index++) {
			boolean knight = level.random.nextFloat() < DkcFloorRegistry.knightShare(floor);
			Mob enemy = knight ? SololevelingModEntities.DEMON_KNIGHT.get().create(level)
					: SololevelingModEntities.DEMON.get().create(level);
			if (enemy == null)
				continue;
			enemy.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
			try {
				enemy.finalizeSpawn(level, level.getCurrentDifficultyAt(player.blockPosition()),
						MobSpawnType.SPAWNER, null);
			} catch (RuntimeException exception) {
				enemy.discard();
				SololevelingMod.LOGGER.warn("Failed to finalize a DKC floor-wave mob", exception);
				continue;
			}
			if (floor == net.solocraft.dkc.DkcRadiruManager.FLOOR) {
				enemy.setCustomName(Component.literal(knight ? "Radiru Royal Guard" : "Radiru Defender")
						.withStyle(ChatFormatting.DARK_PURPLE));
				enemy.setCustomNameVisible(false);
			}
			boolean eliteCandidate = enemy instanceof DemonEntity
					|| floor > net.solocraft.dkc.DkcRadiruManager.FLOOR
					&& enemy instanceof DemonKnightEntity;
			boolean promoted = !minibossSpawned && eliteCandidate
					&& level.random.nextFloat() < minibossChance(floor);
			if (promoted) {
				if (enemy instanceof DemonKnightEntity knightEnemy)
					promoteKnightMiniboss(knightEnemy, floor);
				else if (enemy instanceof DemonEntity demonEnemy)
					promoteDemonMiniboss(demonEnemy, floor);
			}
			BlockPos pos = DkcFloorBuilder.findCombatSpawn(level, player, floor,
					index + spawned * 13, enemy);
			if (pos == null) {
				enemy.discard();
				continue;
			}
			enemy.getPersistentData().putDouble("dkc_floor_number", floor);
			enemy.getPersistentData().putString("dkc_spawned_by", player.getStringUUID());
			enemy.getPersistentData().putString(ROLE_TAG, FLOOR_WAVE_ROLE);
			enemy.getPersistentData().putInt(ATTEMPT_TAG, waveAttempt);
			enemy.setTarget(player);
			if (!level.addFreshEntity(enemy)) {
				enemy.discard();
				continue;
			}
			if (promoted) {
				minibossSpawned = true;
				playerData.putBoolean(minibossKey, true);
			}
			spawned++;
		}
		if (spawned > 0)
			SololevelingMod.LOGGER.debug("Spawned {} DKC enemies for {} on floor {}", spawned,
					player.getGameProfile().getName(), floor);
		return spawned;
	}

	private static boolean hasOwnedMiniboss(ServerLevel level, ServerPlayer player, int floor, int attempt) {
		String owner = player.getStringUUID();
		AABB area = DkcFloorBuilder.combatBounds(player, floor);
		boolean demon = !level.getEntitiesOfClass(DemonEntity.class, area,
				entity -> entity.getPersistentData().getBoolean(MINIBOSS_TAG)
						&& ownsWave(entity, owner, floor, attempt)).isEmpty();
		return demon || !level.getEntitiesOfClass(DemonKnightEntity.class, area,
				entity -> entity.getPersistentData().getBoolean(MINIBOSS_TAG)
						&& ownsWave(entity, owner, floor, attempt)).isEmpty();
	}

	private static float minibossChance(int floor) {
		return Math.min(0.12F, 0.08F + Math.max(0, floor - 2) * 0.002F);
	}

	private static void promoteDemonMiniboss(DemonEntity demon, int floor) {
		CompoundTag data = demon.getPersistentData();
		if (data.getBoolean(MINIBOSS_TAG))
			return;
		data.putBoolean(MINIBOSS_TAG, true);
		data.putString(DungeonMobLevelAdapter.ROLE_TAG, DungeonMobLevelAdapter.MobRole.ELITE.id());
		demon.setVisualScale(Math.min(1.65F, 1.48F + floor * 0.008F));
		demon.refreshDimensions();
		demon.setCustomName(Component.literal("Elite Demon")
				.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
		demon.setCustomNameVisible(true);

		double healthMultiplier = Math.min(3.0D, 2.35D + floor * 0.0325D);
		double damageMultiplier = Math.min(1.70D, 1.40D + floor * 0.015D);
		addPermanent(demon.getAttribute(Attributes.MAX_HEALTH), MINIBOSS_HEALTH_MODIFIER,
				"DKC miniboss health", healthMultiplier - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addPermanent(demon.getAttribute(Attributes.ATTACK_DAMAGE), MINIBOSS_DAMAGE_MODIFIER,
				"DKC miniboss damage", damageMultiplier - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addPermanent(demon.getAttribute(Attributes.ARMOR), MINIBOSS_ARMOR_MODIFIER,
				"DKC miniboss armor", Math.min(14.0D, 7.0D + floor * 0.35D), AttributeModifier.Operation.ADD_VALUE);
		addPermanent(demon.getAttribute(Attributes.ARMOR_TOUGHNESS), MINIBOSS_TOUGHNESS_MODIFIER,
				"DKC miniboss armor toughness", Math.min(4.0D, 2.0D + floor * 0.10D), AttributeModifier.Operation.ADD_VALUE);
		AttributeInstance knockback = demon.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (knockback != null)
			addPermanent(knockback, MINIBOSS_KNOCKBACK_MODIFIER, "DKC miniboss knockback resistance",
					Math.max(0.0D, 0.85D - knockback.getValue()), AttributeModifier.Operation.ADD_VALUE);
		demon.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE,
				0, false, false, true));
		demon.setHealth(demon.getMaxHealth());
	}

	private static void promoteKnightMiniboss(DemonKnightEntity knight, int floor) {
		CompoundTag data = knight.getPersistentData();
		if (data.getBoolean(MINIBOSS_TAG))
			return;
		data.putBoolean(MINIBOSS_TAG, true);
		data.putString(DungeonMobLevelAdapter.ROLE_TAG, DungeonMobLevelAdapter.MobRole.ELITE.id());

		int variant = knight.getVariant();
		double upperFloor = Math.max(0, floor - 16);
		double healthMultiplier;
		double damageMultiplier;
		double armorBonus;
		double toughnessBonus;
		double targetKnockbackResistance;
		float scale;
		Component name;
		switch (variant) {
			case 1 -> {
				healthMultiplier = 2.45D + upperFloor * 0.06D;
				damageMultiplier = 1.20D + upperFloor * 0.02D;
				armorBonus = 9.0D;
				toughnessBonus = 3.5D;
				targetKnockbackResistance = 1.0D;
				scale = 1.38F + (float) upperFloor * 0.012F;
				name = Component.translatable("entity.sololeveling.dkc_demon_knight_bulwark");
			}
			case 2 -> {
				healthMultiplier = 2.05D + upperFloor * 0.05D;
				damageMultiplier = 1.45D + upperFloor * 0.025D;
				armorBonus = 4.0D;
				toughnessBonus = 1.5D;
				targetKnockbackResistance = 0.92D;
				scale = 1.30F + (float) upperFloor * 0.012F;
				name = Component.translatable("entity.sololeveling.dkc_demon_knight_executioner");
			}
			default -> {
				healthMultiplier = 2.15D + upperFloor * 0.06D;
				damageMultiplier = 1.30D + upperFloor * 0.025D;
				armorBonus = 6.0D;
				toughnessBonus = 2.5D;
				targetKnockbackResistance = 0.95D;
				scale = 1.34F + (float) upperFloor * 0.012F;
				name = Component.translatable("entity.sololeveling.dkc_elite_demon_knight");
			}
		}

		knight.setVisualScale(scale);
		knight.refreshDimensions();
		knight.setCustomName(name.copy().withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
		knight.setCustomNameVisible(true);
		addPermanent(knight.getAttribute(Attributes.MAX_HEALTH), MINIBOSS_HEALTH_MODIFIER,
				"DKC knight miniboss health", healthMultiplier - 1.0D,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addPermanent(knight.getAttribute(Attributes.ATTACK_DAMAGE), MINIBOSS_DAMAGE_MODIFIER,
				"DKC knight miniboss damage", damageMultiplier - 1.0D,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addPermanent(knight.getAttribute(Attributes.ARMOR), MINIBOSS_ARMOR_MODIFIER,
				"DKC knight miniboss armor", armorBonus, AttributeModifier.Operation.ADD_VALUE);
		addPermanent(knight.getAttribute(Attributes.ARMOR_TOUGHNESS), MINIBOSS_TOUGHNESS_MODIFIER,
				"DKC knight miniboss armor toughness", toughnessBonus,
				AttributeModifier.Operation.ADD_VALUE);
		AttributeInstance knockback = knight.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (knockback != null)
			addPermanent(knockback, MINIBOSS_KNOCKBACK_MODIFIER,
					"DKC knight miniboss knockback resistance",
					Math.max(0.0D, targetKnockbackResistance - knockback.getValue()),
					AttributeModifier.Operation.ADD_VALUE);
		if (variant == 2)
			addPermanent(knight.getAttribute(Attributes.MOVEMENT_SPEED), MINIBOSS_SPEED_MODIFIER,
					"DKC knight executioner speed", 0.10D,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		knight.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE,
				0, false, false, true));
		knight.setHealth(knight.getMaxHealth());
	}

	private static void addPermanent(AttributeInstance attribute, ResourceLocation id, String name,
			double amount, AttributeModifier.Operation operation) {
		if (attribute == null || !Double.isFinite(amount) || amount <= 0.0D)
			return;
		if (attribute.getModifier(id) != null)
			attribute.removeModifier(id);
		attribute.addPermanentModifier(new AttributeModifier(id, amount, operation));
	}

	private static ResourceLocation modifierId(String statistic) {
		return ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
				"attribute/dkc_miniboss_" + statistic);
	}

	public static int aliveCount(ServerLevel level, Player player, int floor) {
		String owner = player.getStringUUID();
		int attempt = currentAttempt(player, floor);
		AABB area = DkcFloorBuilder.combatBounds((ServerPlayer) player, floor);
		int demons = level.getEntitiesOfClass(DemonEntity.class, area,
				entity -> ownsWave(entity, owner, floor, attempt)).size();
		int knights = level.getEntitiesOfClass(DemonKnightEntity.class, area,
				entity -> ownsWave(entity, owner, floor, attempt)).size();
		return demons + knights;
	}

	public static void discardOwnedWave(ServerLevel level, ServerPlayer player, int floor) {
		String owner = player.getStringUUID();
		AABB area = DkcFloorBuilder.combatBounds(player, floor);
		level.getEntitiesOfClass(DemonEntity.class, area, entity -> ownsFloorWave(entity, owner, floor))
				.forEach(Entity::discard);
		level.getEntitiesOfClass(DemonKnightEntity.class, area, entity -> ownsFloorWave(entity, owner, floor))
				.forEach(Entity::discard);
	}

	private static boolean ownsWave(Entity entity, String owner, int floor, int attempt) {
		CompoundTag tag = entity.getPersistentData();
		return ownsFloorWave(entity, owner, floor) && attempt > 0 && tag.getInt(ATTEMPT_TAG) == attempt;
	}

	private static boolean ownsFloorWave(Entity entity, String owner, int floor) {
		CompoundTag tag = entity.getPersistentData();
		return floor == (int) tag.getDouble("dkc_floor_number")
				&& owner.equals(tag.getString("dkc_spawned_by"))
				&& FLOOR_WAVE_ROLE.equals(tag.getString(ROLE_TAG));
	}

	public static boolean isCurrentWaveMob(Entity entity, Player player, int floor) {
		return entity != null && player != null
				&& ownsWave(entity, player.getStringUUID(), floor, currentAttempt(player, floor));
	}

	public static int currentAttempt(Player player, int floor) {
		return player == null ? 0 : player.getPersistentData().getInt(prefix(floor) + ATTEMPT_SUFFIX);
	}

	public static void invalidateAttempt(Player player, int floor) {
		if (player == null)
			return;
		startNewAttempt(player.getPersistentData(), prefix(floor));
	}

	public static void checkWaveSpawn(LevelAccessor world, Entity entity) {
		// Kept for generated call-site compatibility; wave replenishment is timed.
	}

	public static void respawnDemons(LevelAccessor world, Player player, int floor, int count) {
		if (world instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer)
			spawnBatch(level, serverPlayer, floor, count);
	}

	private static String prefix(int floor) {
		return "dkc_floor_" + floor;
	}

	private static int ensureAttempt(CompoundTag data, String prefix) {
		int attempt = data.getInt(prefix + ATTEMPT_SUFFIX);
		if (attempt <= 0) {
			attempt = 1;
			data.putInt(prefix + ATTEMPT_SUFFIX, attempt);
		}
		return attempt;
	}

	private static int startNewAttempt(CompoundTag data, String prefix) {
		int current = data.getInt(prefix + ATTEMPT_SUFFIX);
		int next = current >= Integer.MAX_VALUE || current < 0 ? 1 : current + 1;
		data.putInt(prefix + ATTEMPT_SUFFIX, next);
		return next;
	}
}
