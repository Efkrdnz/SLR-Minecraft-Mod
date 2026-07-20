package net.solocraft.dungeon.runtime;

import net.solocraft.dungeon.DungeonMobVariantScaler;
import net.solocraft.procedures.EntityLoadedLevelPresetProcedure;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import javax.annotation.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Creates deterministic dungeon mobs without using {@link EntityType#spawn}.
 * Metadata and the requested Solo Leveling level are installed before the
 * entity joins the level, preventing the legacy join handler from assigning a
 * different random level.
 */
public final class DungeonMobLevelAdapter {
	public static final String INSTANCE_TAG = "slr_dungeon_instance";
	public static final String ENCOUNTER_TAG = "slr_dungeon_encounter";
	public static final String MARKER_TAG = "slr_dungeon_marker";
	public static final String ROLE_TAG = "slr_dungeon_role";
	public static final String XP_REWARD_TAG = "slr_dungeon_base_xp";
	public static final String LEGACY_DUNGEON_TAG = "dungeon_tag";
	public static final String RUNTIME_SPAWN_TAG = "slr_dungeon_spawned";
	public static final String PENDING_TRACK_TAG = "slr_dungeon_pending_track";
	public static final String SCALING_VERSION_TAG = "slr_dungeon_scaling_version";
	public static final String LEVEL_NAME_APPLIED_TAG = "slr_dungeon_level_name_applied";

	private static final int SCALING_VERSION = 1;
	private static final int MINIMUM_LEVEL = 1;
	private static final int MAXIMUM_LEVEL = 1000;

	private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("06ba80bb-933e-495a-96fd-2867b97c74e1");
	private static final UUID DAMAGE_MODIFIER_ID = UUID.fromString("5248394a-1300-4402-9949-cbb004cf0d7f");
	private static final UUID ARMOR_MODIFIER_ID = UUID.fromString("f37c3550-cf1d-4883-88f5-889e07f9f135");
	private static final UUID KNOCKBACK_MODIFIER_ID = UUID.fromString("9500ef61-c8f4-449f-a512-9b6b29094a47");

	private DungeonMobLevelAdapter() {
	}

	public static SpawnResult spawnExact(ServerLevel level, ResourceLocation entityTypeId, BlockPos position,
			float yaw, SpawnSpec spec) {
		if (level == null || entityTypeId == null || position == null || spec == null)
			return SpawnResult.failure(SpawnFailure.INVALID_ARGUMENT, "Level, entity type, position, and spawn specification are required.");
		EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityTypeId);
		if (entityType == null)
			return SpawnResult.failure(SpawnFailure.UNKNOWN_ENTITY_TYPE, "Unknown entity type: " + entityTypeId);
		return spawnExact(level, entityType, entityTypeId, position, yaw, spec);
	}

	public static SpawnResult spawnExact(ServerLevel level, EntityType<?> entityType, BlockPos position,
			float yaw, SpawnSpec spec) {
		ResourceLocation entityTypeId = entityType == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(entityType);
		if (level == null || entityType == null || position == null || spec == null)
			return SpawnResult.failure(SpawnFailure.INVALID_ARGUMENT, "Level, entity type, position, and spawn specification are required.");
		return spawnExact(level, entityType, entityTypeId, position, yaw, spec);
	}

	private static SpawnResult spawnExact(ServerLevel level, EntityType<?> entityType,
			@Nullable ResourceLocation entityTypeId, BlockPos position, float yaw, SpawnSpec spec) {
		if (spec.instanceId().isBlank())
			return SpawnResult.failure(SpawnFailure.INVALID_ARGUMENT, "A dungeon instance ID is required.");

		final Entity created;
		try {
			created = entityType.create(level);
		} catch (RuntimeException exception) {
			return SpawnResult.failure(SpawnFailure.CREATION_FAILED,
					"Entity factory failed for " + displayId(entityTypeId) + ": " + safeMessage(exception));
		}
		if (created == null)
			return SpawnResult.failure(SpawnFailure.CREATION_FAILED, "Entity factory returned no entity for " + displayId(entityTypeId) + ".");
		if (!(created instanceof Mob mob)) {
			created.discard();
			return SpawnResult.failure(SpawnFailure.NOT_A_MOB,
					"Dungeon encounter entries must create a Mob: " + displayId(entityTypeId));
		}

		float safeYaw = Mth.wrapDegrees(yaw);
		mob.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, safeYaw, 0.0F);
		mob.setYHeadRot(safeYaw);
		mob.setYBodyRot(safeYaw);
		writeRuntimeMetadata(mob, spec);

		if (!level.noCollision(mob, mob.getBoundingBox())) {
			mob.discard();
			return SpawnResult.failure(SpawnFailure.PLACEMENT_BLOCKED,
					"The spawn marker is obstructed for " + displayId(entityTypeId) + " at " + position.toShortString() + ".");
		}

		try {
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(position), MobSpawnType.MOB_SUMMONED, null, null);
		} catch (RuntimeException exception) {
			mob.discard();
			return SpawnResult.failure(SpawnFailure.FINALIZE_FAILED,
					"Spawn finalization failed for " + displayId(entityTypeId) + ": " + safeMessage(exception));
		}
		if (mob.isRemoved())
			return SpawnResult.failure(SpawnFailure.FINALIZE_FAILED,
					"Spawn finalization removed " + displayId(entityTypeId) + ".");
		DungeonMobVariantScaler.applyForLevel(mob, spec.level(), mob.getRandom());
		if (!level.noCollision(mob, mob.getBoundingBox())) {
			mob.discard();
			return SpawnResult.failure(SpawnFailure.PLACEMENT_BLOCKED,
					"The finalized mob does not fit at " + position.toShortString() + ".");
		}

		ScalingResult scaling = applyGenericScaling(mob, spec.level(), spec.role());
		if (!scaling.succeeded()) {
			mob.discard();
			return SpawnResult.failure(SpawnFailure.SCALING_FAILED, scaling.message());
		}

		mob.setPersistenceRequired();
		final boolean added;
		try {
			added = level.addFreshEntity(mob);
		} catch (RuntimeException exception) {
			mob.discard();
			return SpawnResult.failure(SpawnFailure.ADD_FAILED,
					"Could not add " + displayId(entityTypeId) + " to the level: " + safeMessage(exception));
		}
		if (!added) {
			mob.discard();
			return SpawnResult.failure(SpawnFailure.ADD_FAILED,
					"The level rejected " + displayId(entityTypeId) + " at " + position.toShortString() + ".");
		}
		return SpawnResult.success(mob, scaling.level());
	}

	/**
	 * Applies the mod's generic per-level additions with stable permanent
	 * modifiers. Calling this method repeatedly replaces, rather than stacks,
	 * this adapter's modifiers.
	 */
	public static ScalingResult applyGenericScaling(Mob mob, int requestedLevel, MobRole role) {
		if (mob == null)
			return ScalingResult.failure("Cannot scale a null mob.");
		int level = DungeonLevelHelper.clampLevel(requestedLevel, MINIMUM_LEVEL, MAXIMUM_LEVEL);
		MobRole safeRole = role == null ? MobRole.NORMAL : role;

		DungeonLevelHelper.setEntityLevel(mob, level);
		double healthBefore = attributeValueWithout(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER_ID);
		double damageBefore = attributeValueWithout(mob.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_MODIFIER_ID);
		double armorBefore = attributeValueWithout(mob.getAttribute(Attributes.ARMOR), ARMOR_MODIFIER_ID);
		double knockbackBefore = attributeValueWithout(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_MODIFIER_ID);

		addPermanent(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER_ID, "SLR dungeon level health",
				level * safeRole.healthPerLevel);
		addPermanent(mob.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_MODIFIER_ID, "SLR dungeon level damage",
				level * safeRole.damagePerLevel);
		addPermanent(mob.getAttribute(Attributes.ARMOR), ARMOR_MODIFIER_ID, "SLR dungeon level armor",
				level * safeRole.armorPerLevel);

		double knockbackAddition = Math.max(0.0D,
				Math.min(0.95D - knockbackBefore, level * safeRole.knockbackPerLevel));
		addPermanent(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_MODIFIER_ID,
				"SLR dungeon level knockback resistance", knockbackAddition);

		double statMultiplier = 1.0D;
		AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
		AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
		if (health != null && healthBefore > 0.0D)
			statMultiplier = Math.max(statMultiplier, health.getValue() / healthBefore);
		if (damage != null && damageBefore > 0.0D)
			statMultiplier = Math.max(statMultiplier, damage.getValue() / damageBefore);

		CompoundTag data = mob.getPersistentData();
		data.putInt(SCALING_VERSION_TAG, SCALING_VERSION);
		data.putDouble(EntityLoadedLevelPresetProcedure.LEVEL_STAT_MULTIPLIER_TAG,
				Double.isFinite(statMultiplier) ? Math.max(1.0D, statMultiplier) : 1.0D);
		if (!data.getBoolean(LEVEL_NAME_APPLIED_TAG)) {
			Component baseName = mob.getCustomName() == null ? mob.getType().getDescription() : mob.getCustomName().copy();
			mob.setCustomName(Component.empty().append(baseName)
					.append(Component.literal(" [Lv. " + level + "]").withStyle(ChatFormatting.GOLD)));
			mob.setCustomNameVisible(true);
			data.putBoolean(LEVEL_NAME_APPLIED_TAG, true);
		}
		mob.setHealth(mob.getMaxHealth());
		return ScalingResult.success(level, safeRole, healthBefore, damageBefore, armorBefore);
	}

	private static void writeRuntimeMetadata(Mob mob, SpawnSpec spec) {
		CompoundTag data = mob.getPersistentData();
		int safeLevel = DungeonLevelHelper.clampLevel(spec.level(), MINIMUM_LEVEL, MAXIMUM_LEVEL);
		DungeonLevelHelper.setEntityLevel(mob, safeLevel);
		data.putBoolean(RUNTIME_SPAWN_TAG, true);
		data.putBoolean(PENDING_TRACK_TAG, true);
		data.putString(INSTANCE_TAG, spec.instanceId());
		data.putString(ENCOUNTER_TAG, spec.encounterId());
		data.putString(MARKER_TAG, spec.markerId());
		data.putString(ROLE_TAG, spec.role().id);
		if (spec.baseXp() >= 0)
			data.putInt(XP_REWARD_TAG, spec.baseXp());
		String dungeonTag = spec.dungeonTag().isBlank() ? spec.instanceId() : spec.dungeonTag();
		data.putString(LEGACY_DUNGEON_TAG, dungeonTag);
	}

	private static double attributeValueWithout(@Nullable AttributeInstance attribute, UUID modifierId) {
		if (attribute == null)
			return 0.0D;
		if (attribute.getModifier(modifierId) != null)
			attribute.removeModifier(modifierId);
		return attribute.getValue();
	}

	private static void addPermanent(@Nullable AttributeInstance attribute, UUID modifierId, String name, double amount) {
		if (attribute == null || !Double.isFinite(amount) || amount <= 0.0D)
			return;
		if (attribute.getModifier(modifierId) != null)
			attribute.removeModifier(modifierId);
		attribute.addPermanentModifier(new AttributeModifier(modifierId, name, amount, AttributeModifier.Operation.ADDITION));
	}

	private static String displayId(@Nullable ResourceLocation entityTypeId) {
		return entityTypeId == null ? "unregistered entity type" : entityTypeId.toString();
	}

	private static String safeMessage(RuntimeException exception) {
		String message = exception.getMessage();
		return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
	}

	public enum MobRole {
		NORMAL("normal", 1.15D, 0.11D, 0.025D, 0.0025D),
		ELITE("elite", 1.50D, 0.145D, 0.030D, 0.0025D),
		BOSS("boss", 1.90D, 0.18D, 0.035D, 0.0025D);

		private final String id;
		private final double healthPerLevel;
		private final double damagePerLevel;
		private final double armorPerLevel;
		private final double knockbackPerLevel;

		MobRole(String id, double healthPerLevel, double damagePerLevel, double armorPerLevel, double knockbackPerLevel) {
			this.id = id;
			this.healthPerLevel = healthPerLevel;
			this.damagePerLevel = damagePerLevel;
			this.armorPerLevel = armorPerLevel;
			this.knockbackPerLevel = knockbackPerLevel;
		}

		public String id() {
			return id;
		}

		public static MobRole fromString(@Nullable String value) {
			if (value != null) {
				for (MobRole role : values()) {
					if (role.id.equalsIgnoreCase(value) || role.name().equalsIgnoreCase(value))
						return role;
				}
			}
			return NORMAL;
		}
	}

	public record SpawnSpec(String instanceId, String encounterId, String markerId, MobRole role,
			int level, String dungeonTag, int baseXp) {
		public SpawnSpec {
			instanceId = clean(instanceId);
			encounterId = clean(encounterId);
			markerId = clean(markerId);
			role = role == null ? MobRole.NORMAL : role;
			level = DungeonLevelHelper.clampLevel(level, MINIMUM_LEVEL, MAXIMUM_LEVEL);
			dungeonTag = clean(dungeonTag);
			baseXp = Math.max(-1, Math.min(1_000_000, baseXp));
		}

		public SpawnSpec(String instanceId, String encounterId, String markerId, MobRole role, int level) {
			this(instanceId, encounterId, markerId, role, level, instanceId, -1);
		}

		public SpawnSpec(String instanceId, String encounterId, String markerId, MobRole role,
				int level, String dungeonTag) {
			this(instanceId, encounterId, markerId, role, level, dungeonTag, -1);
		}

		private static String clean(@Nullable String value) {
			return value == null ? "" : value.trim();
		}
	}

	public enum SpawnFailure {
		NONE,
		INVALID_ARGUMENT,
		UNKNOWN_ENTITY_TYPE,
		CREATION_FAILED,
		NOT_A_MOB,
		PLACEMENT_BLOCKED,
		FINALIZE_FAILED,
		SCALING_FAILED,
		ADD_FAILED
	}

	public record SpawnResult(@Nullable Mob mob, SpawnFailure failure, String message, int appliedLevel) {
		public SpawnResult {
			failure = Objects.requireNonNull(failure, "failure");
			message = message == null ? "" : message;
		}

		public boolean succeeded() {
			return mob != null && failure == SpawnFailure.NONE;
		}

		private static SpawnResult success(Mob mob, int level) {
			return new SpawnResult(mob, SpawnFailure.NONE, "", level);
		}

		private static SpawnResult failure(SpawnFailure failure, String message) {
			return new SpawnResult(null, failure, message, 0);
		}
	}

	public record ScalingResult(boolean succeeded, int level, MobRole role, String message,
			double baseHealth, double baseDamage, double baseArmor) {
		private static ScalingResult success(int level, MobRole role, double baseHealth, double baseDamage, double baseArmor) {
			return new ScalingResult(true, level, role, "", baseHealth, baseDamage, baseArmor);
		}

		private static ScalingResult failure(String message) {
			return new ScalingResult(false, 0, MobRole.NORMAL, message, 0.0D, 0.0D, 0.0D);
		}
	}
}
