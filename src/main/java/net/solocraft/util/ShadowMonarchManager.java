package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dkc.DkcSpatialLayout;
import net.solocraft.dungeon.runtime.DungeonInstanceSavedData;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.api.skill.HunterAbilityRegistry;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.BeruShadowEntity;
import net.solocraft.entity.GoblinArcherShadowEntity;
import net.solocraft.entity.GoblinClubShadowEntity;
import net.solocraft.entity.GoblinMageShadowEntity;
import net.solocraft.entity.IgrisShadowEntity;
import net.solocraft.entity.KamishShadowEntity;
import net.solocraft.entity.OrcShadowEntity;
import net.solocraft.entity.ShadowKaiselinEntity;
import net.solocraft.entity.ShadowGreenOrcEntity;
import net.solocraft.entity.ShadowHighOrcEntity;
import net.solocraft.entity.ShadowIronEntity;
import net.solocraft.entity.ShadowPolarBearEntity;
import net.solocraft.entity.ShadowSold1Entity;
import net.solocraft.entity.SteelFangWolfShadowEntity;
import net.solocraft.entity.TuskShadowEntity;
import net.solocraft.procedures.SkillSlotHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

public class ShadowMonarchManager {
	public static final String FORMATION_PREFIX = "Formation:";
	public static final int FORMATION_COLOR = 0xB965FF;
	public static final String COMMAND_DEFAULT = "default";
	public static final String COMMAND_PROTECT = "protect";
	public static final String COMMAND_BERSERK = "berserk";
	public static final String COMMAND_FOLLOW = "follow";
	public static final String COMMAND_CLEAR_DUNGEON = "clear_dungeon";
	public static final int RANK_NORMAL = 0;
	public static final int RANK_ELITE = 1;
	public static final int RANK_KNIGHT = 2;
	public static final int RANK_ELITE_KNIGHT = 3;
	public static final int RANK_GENERAL = 4;
	public static final int RANK_MARSHAL = 5;
	public static final int RANK_GRAND_MARSHAL = 6;
	public static final double SHADOW_HEALTH_PER_HEALING_MANA =
			ShadowHealingRules.HEALTH_PER_MANA;
	public static final int BASE_SHADOW_LEVEL_CAP = 10;
	public static final int MAX_ADMIN_SHADOW_LEVEL = 1_000_000;
	private static final int PLAYER_LEVEL_CAP_START = 40;
	private static final int PLAYER_LEVELS_PER_CAP_INCREASE = 20;
	private static final int SHADOW_LEVELS_PER_CAP_INCREASE = 10;
	private static final int MAX_SAFE_SHADOW_LEVEL = (Integer.MAX_VALUE - 35) / 15;
	private static final String ROOT = "sololeveling_shadow_monarch";
	private static final String SHADOWS = "shadows";
	private static final String FORMATIONS = "formations";
	private static final String GLOW_COLORS = "glow_colors";
	private static final String IRON_MAX = "iron_max";
	private static final String IRON_SUMMONED = "iron_summoned";
	/** Sentinel for "this type has no assigned outline colour". */
	public static final int NO_GLOW = -1;
	private static final String RANK = "rank";
	private static final String STARTING_RANK = "starting_rank";
	private static final String RANK_SCHEMA = "rank_schema";
	private static final int RANK_SCHEMA_VERSION = 3;
	private static final String GRAND_MARSHAL_ID = "grand_marshal_id";
	private static final String ADMIN_LEVEL_FLOOR = "admin_level_floor";
	private static final String SHADOW_ID = "sl_shadow_id";
	private static final String SHADOW_TYPE = "sl_shadow_type";
	private static final String SHADOW_OWNER = "sl_shadow_owner";
	private static final String SHADOW_COMMAND = "sl_shadow_command";
	private static final String PLAYER_COMMAND = "sl_shadow_command_mode";
	private static final String PLAYER_RESET_GENERATION = "sl_shadow_reset_generation";
	private static final String SHADOW_GENERATION = "sl_shadow_generation";
	private static final String SHADOW_INVENTORY = "sl_shadow_inventory";
	private static final String EQUIPMENT = "equipment";
	private static final String SHADOW_EQUIPMENT = "sl_shadow_equipment";
	private static final String CACHED_LEVEL_CAP = "shadow_level_cap";
	private static final String BASE_HEALTH = "sl_shadow_base_health";
	private static final String BASE_ATTACK = "sl_shadow_base_attack";
	private static final String APPLIED_LEVEL = "sl_shadow_applied_level";
	private static final String APPLIED_RANK = "sl_shadow_applied_rank";
	private static final String INTRINSIC_MARSHAL_DOMAIN = "sl_shadow_rank_domain";
	private static final String TEMPORARY_DOMAIN_UNTIL = "sl_shadow_temporary_domain_until";
	private static final int MARSHAL_DOMAIN_AMPLIFIER = 1;
	private static final int MARSHAL_DOMAIN_DURATION_TICKS = 240;
	private static final int MARSHAL_DOMAIN_REFRESH_THRESHOLD_TICKS = 80;
	private static final String INSUFFICIENT_MANA_NOTICE = "sl_shadow_mana_notice";
	private static final String SAVED_HEALTH = "health";
	private static final String SAVED_HEALTH_AT = "health_saved_at";
	private static final String PROCEDURAL_DUNGEON_TAG = "slr_procedural_dungeon";
	private static final TagKey<EntityType<?>> SHADOW_ENTITY_TAG = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("shadows"));
	private static final int CLEAR_SCAN_INTERVAL_TICKS = 20;
	private static final int CLEAR_REPATH_INTERVAL_TICKS = 30;
	private static final int CLEAR_STUCK_TICKS = 80;
	private static final int CLEAR_FAILED_TARGET_COOLDOWN_TICKS = 120;
	private static final int CLEAR_MAX_CANDIDATES = 128;
	private static final int CLEAR_MAX_PATH_ATTEMPTS_PER_TICK = 8;
	private static final int CLEAR_MAX_TARGET_CHOICES_PER_SHADOW = 5;
	/**
	 * Sweep radius. Anything hostile inside this is fought before the group
	 * pushes on, which is what makes the command read as "clear", not "rush".
	 */
	private static final double CLEAR_ENGAGE_RADIUS_SQR = 26.0D * 26.0D;
	/** Travelling to a distant objective repaths faster than corridor combat. */
	private static final int CLEAR_ADVANCE_REPATH_TICKS = 20;
	/** Advancing is slightly quicker than fighting so the group keeps formation. */
	private static final double CLEAR_ADVANCE_SPEED = 1.15D;
	private static final float SHADOW_TRAVERSAL_STEP_HEIGHT = 1.1F;
	private static final TagKey<EntityType<?>> SOLO_BOSS_TAG = TagKey.create(
			Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("minecraft", "soloboss"));
	private static final Map<UUID, ClearDungeonState> CLEAR_DUNGEON_STATES = new HashMap<>();

	private ShadowMonarchManager() {
	}

	public record ShadowDisplayProgress(int rank, int level, int rankXp,
			int rankXpNeeded, int nextRank, boolean levelCapped,
			boolean maxRank, boolean grandMarshalEligible,
			boolean grandMarshalActive) {
	}

	public record ShadowLevelCommandResult(boolean knownTarget, int changed,
			int lowestLevel, int highestLevel) {
	}

	public record GrandMarshalAssignmentResult(boolean success, String message) {
	}

	public record GrandMarshalCommander(String shadowId, String type,
			String name, int level, LivingEntity entity) {
	}

	public record ShadowHealingQuote(int bossManaCost, int allManaCost,
			int bossTargets, int allTargets) {
	}

	public record ShadowHealingResult(boolean success, int healedShadows,
			int manaConsumed, int healthRestored, String message) {
	}

	private record ShadowHealingTarget(LivingEntity entity, String type,
			double missingHealth) {
	}

	public static boolean isFormationSkill(String skill) {
		return skill != null && skill.startsWith(FORMATION_PREFIX);
	}

	public static String displaySkillName(Entity entity, String skill) {
		// Contributed abilities carry their addon marker everywhere a name is shown.
		if (HunterAbilityRegistry.isContributed(skill))
			return HunterAbilityRegistry.displayName(skill);
		if (isFormationSkill(skill)) {
			String embeddedName = formationNameFromSkill(skill);
			if (!embeddedName.isEmpty())
				return embeddedName;
			CompoundTag formation = getFormation(entity, formationIdFromSkill(skill));
			return formation == null ? "Formation" : formation.getString("name");
		}
		return "Critical Strike".equals(skill) ? "Cross Strike" : skill;
	}

	public static int skillColor(Entity entity, String skill) {
		int contributed = HunterAbilityRegistry.color(skill);
		if (contributed >= 0)
			return contributed;
		if (isFormationSkill(skill))
			return FORMATION_COLOR;
		if (JobSkillManager.isJobSkill(skill))
			return JobSkillManager.skillColor(skill);
		return 0xFFFFFF;
	}

	/** Iron is an unreleased shadow and remains invisible and unusable unless
	 * the owning player's persisted developer preview is enabled. */
	public static boolean isShadowAvailableFor(Player player,
			String requestedType) {
		String type = normalizeShadowType(
				requestedType == null ? "" : requestedType);
		return !type.isEmpty() && (!"iron".equals(type)
				|| DeveloperModeManager.isEnabled(player));
	}

	public static boolean summonType(LevelAccessor world, double x, double y, double z, Entity caster, String type) {
		if (!(caster instanceof ServerPlayer player) || !(world instanceof ServerLevel level))
			return false;
		type = normalizeShadowType(type);
		if (type.isEmpty() || !isShadowAvailableFor(player, type)
				|| CartenonSuppression.blockVesselSkill(player))
			return false;
		ensureRoster(player);
		absorbVisibleOwnedShadows(player);
		enforceSummonedLimit(player, type);
		CompoundTag shadow = firstSummonableShadow(player, type);
		if (shadow == null)
			return false;
		return summonShadow(level, player, shadow, new Vec3(x, y, z), true);
	}

	public static int summonAllOfType(LevelAccessor world, double x, double y, double z, Entity caster, String type) {
		if (!(caster instanceof ServerPlayer player) || !(world instanceof ServerLevel level))
			return 0;
		type = normalizeShadowType(type);
		if (type.isEmpty() || !isShadowAvailableFor(player, type)
				|| CartenonSuppression.blockVesselSkill(player))
			return 0;
		ensureRoster(player);
		absorbVisibleOwnedShadows(player);
		enforceSummonedLimit(player, type);
		List<CompoundTag> matching = ownedRosterWithinLimit(player, type);
		int summoned = 0;
		Vec3 origin = new Vec3(x, y, z);
		for (CompoundTag shadow : matching) {
			Vec3 pos = spreadSummonPosition(player, origin, summoned);
			if (summonShadow(level, player, shadow, pos, true))
				summoned++;
		}
		return summoned;
	}

	public static boolean castFormation(LevelAccessor world, Entity caster, String skill) {
		if (!(caster instanceof ServerPlayer player) || !(world instanceof ServerLevel level) || !isFormationSkill(skill))
			return false;
		ensureRoster(player);
		CompoundTag formation = getFormation(player, formationIdFromSkill(skill));
		if (formation == null)
			return false;
		ListTag members = formation.getList("members", Tag.TAG_COMPOUND);
		Vec3 look = player.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0, look.z);
		if (forward.lengthSqr() < 0.001)
			forward = new Vec3(0, 0, 1);
		forward = forward.normalize();
		Vec3 right = new Vec3(-forward.z, 0, forward.x);
		Vec3 origin = player.position().add(forward.scale(2.5));
		boolean summonedAny = false;
		for (int i = 0; i < members.size(); i++) {
			CompoundTag member = members.getCompound(i);
			CompoundTag shadow = getShadow(player, member.getString("id"));
			if (shadow == null)
				continue;
			Vec3 pos = origin.add(right.scale(member.getDouble("rx"))).add(0, member.getDouble("ry"), 0).add(forward.scale(member.getDouble("rz")));
			summonedAny |= summonShadow(level, player, shadow, pos, true);
		}
		return summonedAny;
	}

	public static String saveFormationFromSummoned(Player player, String requestedName) {
		if (!(player.level() instanceof ServerLevel))
			return "";
		ensureRoster(player);
		absorbVisibleOwnedShadows(player);
		List<CompoundTag> shadows = summonedOwnedShadows(player);
		if (shadows.isEmpty())
			return "";
		String id = UUID.randomUUID().toString();
		String name = cleanFormationName(requestedName, formationCount(player) + 1);
		Vec3 look = player.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0, look.z);
		if (forward.lengthSqr() < 0.001)
			forward = new Vec3(0, 0, 1);
		forward = forward.normalize();
		Vec3 right = new Vec3(-forward.z, 0, forward.x);
		ListTag members = new ListTag();
		for (CompoundTag shadow : shadows) {
			UUID summonedId = shadow.getUUID("summoned");
			Entity summoned = ((ServerLevel) player.level()).getEntity(summonedId);
			if (summoned == null)
				continue;
			Vec3 delta = summoned.position().subtract(player.position());
			CompoundTag member = new CompoundTag();
			member.putString("id", shadow.getString("id"));
			member.putDouble("rx", delta.dot(right));
			member.putDouble("ry", Math.max(-1.0, Math.min(3.0, delta.y)));
			member.putDouble("rz", delta.dot(forward));
			members.add(member);
		}
		if (members.isEmpty())
			return "";
		CompoundTag formation = new CompoundTag();
		formation.putString("id", id);
		formation.putString("name", name);
		formation.put("members", members);
		formations(player).add(formation);
		appendFormationSkill(player, id, name);
		player.getPersistentData().put(ROOT, root(player));
		return name;
	}

	public static boolean removeFormationSkill(Player player, int skillIndex) {
		if (player == null || skillIndex < 1)
			return false;
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		List<String> skills = parseSkillList(vars.Plist);
		if (skillIndex > skills.size())
			return false;
		String removed = skills.get(skillIndex - 1);
		if (!isFormationSkill(removed))
			return false;
		String formationId = formationIdFromSkill(removed);
		skills.remove(skillIndex - 1);
		removeFormationData(player, formationId);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.Plist = writeSkillList(skills);
			for (int slot = 1; slot <= 16; slot++) {
				if (removed.equals(SkillSlotHelper.getSlot(capability, slot)))
					SkillSlotHelper.setSlot(capability, slot, "");
			}
			if (removed.equals(capability.PselectedPower))
				capability.PselectedPower = "";
			capability.syncPlayerVariables(player);
		});
		return true;
	}

	public static List<String> formationSkills(Player player) {
		ArrayList<String> result = new ArrayList<>();
		if (player == null)
			return result;
		ListTag list = formations(player);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag formation = list.getCompound(i);
			String id = formation.getString("id");
			if (!id.isEmpty())
				result.add(FORMATION_PREFIX + id + "|" + cleanFormationName(formation.getString("name"), i + 1));
		}
		return result;
	}

	public static int grantCombatXp(Player owner, String shadowId,
			Entity shadowEntity, int targetXpPool, double shadowDamage,
			double countedTargetDamage) {
		if (owner == null || shadowId == null || shadowId.isEmpty())
			return 0;
		ensureRoster(owner);
		CompoundTag shadow = getShadow(owner, shadowId);
		if (shadow == null)
			return 0;
		int level = Math.max(1, shadow.getInt("level"));
		int levelCap = shadowLevelCap(owner);
		int xp = Math.max(0, shadow.getInt("xp"));
		Entity activeShadow = shadowId.equals(getShadowRosterId(shadowEntity))
				? shadowEntity : null;
		if (activeShadow != null
				&& (activeShadow.getPersistentData().getInt(APPLIED_LEVEL) != level
						|| activeShadow.getPersistentData().getInt(APPLIED_RANK) != rankOf(shadow)))
			applyLevelStatsPreservingHealth(activeShadow, shadow);
		if (level >= levelCap) {
			return 0;
		}
		int earnedXp = ShadowExperienceRules.contributionXp(targetXpPool,
				shadowDamage, countedTargetDamage, level);
		if (earnedXp <= 0)
			return 0;
		xp = (int) Math.min(Integer.MAX_VALUE, (long) xp + earnedXp);
		int needed = xpNeeded(level, shadow.getString("type"));
		boolean leveled = false;
		while (level < levelCap && xp >= needed) {
			xp -= needed;
			level++;
			if (level % 10 == 0)
				promoteShadow(owner, shadow, true);
			needed = xpNeeded(level, shadow.getString("type"));
			leveled = true;
		}
		if (level >= levelCap)
			xp = Math.min(xp, needed - 1);
		shadow.putInt("level", level);
		shadow.putInt("xp", xp);
		owner.getPersistentData().put(ROOT, root(owner));
		if (leveled && activeShadow != null)
			applyLevelStats(activeShadow, shadow, false);
		if (leveled && owner instanceof ServerPlayer player)
			player.displayClientMessage(Component.literal(shadow.getString("name") + " reached Lv." + level), true);
		return earnedXp;
	}

	public static void collectManaStoneDropsFromKill(Entity shadowEntity, Entity killed) {
		if (shadowEntity == null || killed == null || !(killed.level() instanceof ServerLevel level))
			return;
		Vec3 dropPos = killed.position();
		SololevelingMod.queueServerWork(1, () -> {
			if (shadowEntity.isRemoved() || !isTrackedShadowEntity(shadowEntity))
				return;
			AABB area = new AABB(dropPos, dropPos).inflate(3.0D);
			for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area, itemEntity -> isCollectibleManaStone(itemEntity.getItem()))) {
				ItemStack stack = itemEntity.getItem().copy();
				if (stack.isEmpty())
					continue;
				addStackToShadowInventory(shadowEntity, stack);
				itemEntity.discard();
			}
		});
	}

	public static void dropStoredShadowInventory(Entity shadowEntity) {
		if (shadowEntity == null || shadowEntity.level().isClientSide())
			return;
		saveBossHealthBeforeDespawn(null, shadowEntity);
		CompoundTag data = shadowEntity.getPersistentData();
		if (!data.contains(SHADOW_INVENTORY, Tag.TAG_LIST))
			return;
		ListTag inventory = data.getList(SHADOW_INVENTORY, Tag.TAG_COMPOUND);
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = ItemStackData.load(inventory.getCompound(i), shadowEntity.registryAccess());
			if (!stack.isEmpty())
				shadowEntity.spawnAtLocation(stack);
		}
		data.remove(SHADOW_INVENTORY);
	}

	/**
	 * Copies the authoritative shadow roster to the replacement player entity.
	 * Forge does not carry arbitrary {@link Entity#getPersistentData()} across a
	 * death clone, while the legacy capability counters do. Without this copy,
	 * {@link #ensureRoster(Player)} sees those counters after respawn and rebuilds
	 * the same number of shadows as brand-new level-one entries.
	 */
	public static void preserveProgressAfterPlayerClone(Player original, Player replacement) {
		if (original == null || replacement == null || replacement.level().isClientSide())
			return;
		CompoundTag originalData = original.getPersistentData();
		CompoundTag replacementData = replacement.getPersistentData();
		if (originalData.contains(ROOT, Tag.TAG_COMPOUND))
			replacementData.put(ROOT, originalData.getCompound(ROOT).copy());
		else
			replacementData.remove(ROOT);
		if (originalData.contains(PLAYER_COMMAND, Tag.TAG_STRING))
			replacementData.putString(PLAYER_COMMAND, originalData.getString(PLAYER_COMMAND));
		else
			replacementData.remove(PLAYER_COMMAND);
		if (originalData.contains(PLAYER_RESET_GENERATION, Tag.TAG_LONG))
			replacementData.putLong(PLAYER_RESET_GENERATION, originalData.getLong(PLAYER_RESET_GENERATION));
		else
			replacementData.remove(PLAYER_RESET_GENERATION);
	}

	/**
	 * Releases only the exact roster slot occupied by a shadow that has died.
	 * Its level, XP, rank, name and saved boss health remain on that slot, making
	 * a later summon a revival instead of a fresh extraction. Matching the entity
	 * UUID prevents a late death from unlinking a newer replacement summon.
	 */
	public static void handleTrackedShadowDeath(Entity shadowEntity) {
		if (shadowEntity == null || shadowEntity.level().isClientSide()
				|| !(shadowEntity.level() instanceof ServerLevel level))
			return;
		CompoundTag entityData = shadowEntity.getPersistentData();
		if (!entityData.hasUUID(SHADOW_OWNER))
			return;
		String shadowId = entityData.getString(SHADOW_ID);
		if (shadowId.isEmpty())
			return;
		Player owner = findOnlineOwner(level, entityData.getUUID(SHADOW_OWNER));
		if (owner == null)
			return;
		CompoundTag shadow = getShadow(owner, shadowId);
		if (shadow == null || !shadow.hasUUID("summoned")
				|| !shadowEntity.getUUID().equals(shadow.getUUID("summoned")))
			return;
		shadow.remove("summoned");
		owner.getPersistentData().put(ROOT, root(owner));
		String type = shadow.getString("type");
		if (!type.isEmpty())
			updateLegacySpawnCounter(owner, type, -1);
	}

	public static void saveBossHealthBeforeDespawn(Entity ownerEntity, Entity shadowEntity) {
		Player owner = ownerEntity instanceof Player player ? player : null;
		saveBossHealthBeforeDespawn(owner, shadowEntity);
	}

	private static void saveBossHealthBeforeDespawn(Player owner, Entity shadowEntity) {
		if (!(shadowEntity instanceof LivingEntity living) || shadowEntity.level().isClientSide())
			return;
		if (!living.isAlive() || living.getHealth() <= 0.0F)
			return;
		CompoundTag data = shadowEntity.getPersistentData();
		String type = data.getString(SHADOW_TYPE);
		if (type.isEmpty())
			type = typeFromEntity(shadowEntity);
		if (!isBoss(type))
			return;
		if (owner == null && shadowEntity.level() instanceof ServerLevel level
				&& data.hasUUID(SHADOW_OWNER))
			owner = findOnlineOwner(level, data.getUUID(SHADOW_OWNER));
		if (owner == null)
			return;
		String id = data.getString(SHADOW_ID);
		if (id.isEmpty())
			return;
		CompoundTag shadow = getShadow(owner, id);
		if (shadow == null)
			return;
		shadow.putDouble(SAVED_HEALTH, Math.max(1.0D, Math.min(living.getHealth(), living.getMaxHealth())));
		shadow.putLong(SAVED_HEALTH_AT, shadowEntity.level().getGameTime());
		shadow.remove("summoned");
		owner.getPersistentData().put(ROOT, root(owner));
	}

	public static void tagExistingSummon(Player owner, Entity summoned, String type) {
		if (owner == null || summoned == null || type == null || owner.level().isClientSide())
			return;
		if (!isShadowAvailableFor(owner, type)) {
			summoned.discard();
			return;
		}
		ensureRoster(owner);
		CompoundTag shadow = firstAvailableShadow(owner, type);
		if (shadow == null)
			shadow = createShadow(owner, type, countOwned(owner, type) + 1);
		tagSummonedEntity(owner, shadow, summoned);
	}

	public static boolean modifyShadowAmount(Player player, String requestedType, int amount) {
		if (player == null || requestedType == null || amount == 0)
			return false;
		String type = normalizeShadowType(requestedType);
		if (type.isEmpty())
			return false;
		if (amount > 0 && !isShadowAvailableFor(player, type))
			return false;
		// Reductions can trim a live roster entry immediately. Check the whole
		// operation before touching roster counters so an ice-prisoned shadow can
		// neither be dismissed nor leave the player's saved roster out of sync.
		if (amount < 0 && SilladIcePrisonManager.guardManualDismiss(player))
			return false;
		ensureRoster(player);
		int current = legacyMax(player, type);
		int updated = Math.max(0, current + amount);
		if ("iron".equals(type)) {
			updated = Math.min(1, updated);
			if (updated == current)
				return false;
		}
		setLegacyMax(player, type, updated);
		if (updated < current)
			trimOwnedShadows(player, type, updated);
		else
			ensureRoster(player);
		repairGrandMarshalClaim(player, root(player));
		JobSkillManager.syncJobSkills(player);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> capability.syncPlayerVariables(player));
		return true;
	}

	public static List<String> shadowCommandTargets() {
		ArrayList<String> targets = new ArrayList<>();
		targets.add("all");
		targets.addAll(List.of(shadowTypes()));
		return List.copyOf(targets);
	}

	public static String currentShadowCommand(Entity shadow) {
		if (shadow == null)
			return COMMAND_DEFAULT;
		return commandOrDefault(shadow.getPersistentData().getString(SHADOW_COMMAND));
	}

	/**
	 * Administrative shadow levelling. The command may move owned roster entries
	 * beyond the normal player-derived cap, but never beyond the technical bound.
	 * An override floor preserves that commanded level without allowing ordinary
	 * XP to continue climbing past it.
	 */
	public static ShadowLevelCommandResult modifyShadowLevels(Player player,
			String requestedType, int value, boolean additive) {
		if (player == null || requestedType == null)
			return new ShadowLevelCommandResult(false, 0, 0, 0);
		String requested = requestedType.trim();
		boolean all = "all".equalsIgnoreCase(requested);
		String type = all ? "" : normalizeShadowType(requested);
		if (!all && type.isEmpty())
			return new ShadowLevelCommandResult(false, 0, 0, 0);

		ensureRoster(player);
		ArrayList<CompoundTag> targets = new ArrayList<>();
		for (String candidateType : shadowTypes()) {
			if (all || candidateType.equals(type))
				targets.addAll(ownedRosterWithinLimit(player, candidateType));
		}
		if (targets.isEmpty())
			return new ShadowLevelCommandResult(true, 0, 0, 0);

		CompoundTag ownerRoot = root(player);
		int normalCap = shadowLevelCap(player);
		int lowest = MAX_ADMIN_SHADOW_LEVEL;
		int highest = 1;
		for (CompoundTag shadow : targets) {
			int oldLevel = Math.max(1, shadow.getInt("level"));
			long requestedLevel = additive ? (long) oldLevel + Math.max(0, value)
					: value;
			int newLevel = (int) Math.max(1L,
					Math.min(MAX_ADMIN_SHADOW_LEVEL, requestedLevel));
			shadow.putInt("level", newLevel);
			if (!additive)
				shadow.putInt("xp", 0);
			if (newLevel > normalCap)
				shadow.putInt(ADMIN_LEVEL_FLOOR, newLevel);
			else
				shadow.remove(ADMIN_LEVEL_FLOOR);
			recalculateRankAfterAdminLevel(ownerRoot, shadow);
			refreshSummonedShadowRank(player, shadow);
			lowest = Math.min(lowest, newLevel);
			highest = Math.max(highest, newLevel);
		}
		player.getPersistentData().put(ROOT, ownerRoot);
		JobSkillManager.syncJobSkills(player);
		return new ShadowLevelCommandResult(true, targets.size(), lowest, highest);
	}

	public static boolean dismissShadowType(Player player, String requestedType) {
		if (player == null || requestedType == null || player.level().isClientSide())
			return false;
		String type = normalizeShadowType(requestedType);
		if (!isDismissibleShadowType(type))
			return false;
		ensureRoster(player);
		if (legacyMax(player, type) <= 0)
			return false;
		if (!modifyShadowAmount(player, type, -1))
			return false;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.shadowstorageusage = Math.max(0, capability.shadowstorageusage - 1);
			capability.syncPlayerVariables(player);
		});
		player.getPersistentData().put(ROOT, root(player));
		return true;
	}

	public static boolean isInDungeon(Player player) {
		if (player == null)
			return false;
		if (DkcFloorRegistry.isSharedDkc(player.level())) {
			// The server authorizes dungeon-only commands only for a player who is
			// inside their allocated floor and still has an active DKC run. The
			// client has no synchronized slot/run state, so coordinates are enough
			// there to keep the command UI responsive; the server remains decisive.
			if (player instanceof ServerPlayer serverPlayer)
				return player.getPersistentData().getBoolean(DkcSpatialLayout.ACTIVE_RUN_TAG)
						&& DkcSpatialLayout.floor(serverPlayer) > 0;
			return DkcSpatialLayout.floorAt(player.blockPosition()) > 0;
		}
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		String dimension = player.level().dimension().location().getPath();
		return vars.dungeoning || dimension.contains("dungeon") || dimension.contains("castle");
	}

	public static boolean isTrackedShadowEntity(Entity entity) {
		return entity != null && entity.getPersistentData().hasUUID(SHADOW_OWNER);
	}

	public static boolean isShadowEntity(Entity entity) {
		return entity != null && entity.getType().is(SHADOW_ENTITY_TAG);
	}

	public static UUID getShadowOwnerUUID(Entity entity) {
		if (entity == null)
			return null;
		if (entity instanceof TamableAnimal tame && tame.getOwnerUUID() != null)
			return tame.getOwnerUUID();
		CompoundTag data = entity.getPersistentData();
		return data.hasUUID(SHADOW_OWNER) ? data.getUUID(SHADOW_OWNER) : null;
	}

	public static Player getShadowOwnerPlayer(Entity shadow) {
		if (shadow == null)
			return null;
		if (shadow instanceof net.minecraft.world.entity.OwnableEntity ownable
				&& ownable.getOwner() instanceof Player player
				&& player.level() == shadow.level())
			return player;
		UUID ownerId = getShadowOwnerUUID(shadow);
		return ownerId == null ? null : shadow.level().getPlayerByUUID(ownerId);
	}

	/**
	 * Dismisses every loaded summon that a player left in their previous
	 * dimension. A tame animal resolves its owner only inside its current level,
	 * so allowing it to tick after the player changes dimensions turns
	 * {@link TamableAnimal#getOwner()} into {@code null} in generated procedures.
	 */
	public static int dismissLoadedOwnedShadows(ServerPlayer owner,
			ResourceKey<Level> previousDimension) {
		if (owner == null || owner.server == null || previousDimension == null)
			return 0;
		ServerLevel previousLevel = owner.server.getLevel(previousDimension);
		if (previousLevel == null)
			return 0;
		ArrayList<Entity> ownedShadows = new ArrayList<>();
		for (Entity candidate : previousLevel.getAllEntities()) {
			UUID ownerId = getShadowOwnerUUID(candidate);
			if (owner.getUUID().equals(ownerId)
					&& (isShadowEntity(candidate)
							|| isTrackedShadowEntity(candidate)))
				ownedShadows.add(candidate);
		}
		for (Entity shadow : ownedShadows)
			dismissLoadedShadow(owner, shadow);
		return ownedShadows.size();
	}

	/** Immediately withdraws already-summoned Iron instances when their owner
	 * turns developer preview off, while retaining the saved roster entry. */
	public static int dismissLockedPreviewShadows(ServerPlayer owner) {
		if (owner == null || owner.server == null
				|| DeveloperModeManager.isEnabled(owner))
			return 0;
		ArrayList<Entity> locked = new ArrayList<>();
		for (ServerLevel level : owner.server.getAllLevels()) {
			for (Entity candidate : level.getAllEntities()) {
				if (!owner.getUUID().equals(getShadowOwnerUUID(candidate)))
					continue;
				String type = candidate.getPersistentData().getString(
						SHADOW_TYPE);
				if (type.isEmpty())
					type = typeFromEntity(candidate);
				if ("iron".equals(type))
					locked.add(candidate);
			}
		}
		for (Entity shadow : locked)
			dismissLoadedShadow(owner, shadow);
		return locked.size();
	}

	/**
	 * Entity-tick safety net for old saves and dimension changes performed by
	 * other mods. Returns {@code true} when the caller must stop processing this
	 * shadow because its owner is unavailable in the shadow's current level.
	 */
	public static boolean handleUnavailableShadowOwner(Entity shadowEntity) {
		if (shadowEntity == null)
			return false;
		UUID ownerId = getShadowOwnerUUID(shadowEntity);
		if (ownerId == null)
			return false;
		if (shadowEntity.level().isClientSide()) {
			return shadowEntity instanceof TamableAnimal tame
					&& tame.getOwner() == null;
		}
		if (!(shadowEntity.level() instanceof ServerLevel level))
			return false;
		Player owner = findOnlineOwner(level, ownerId);
		if (owner == null) {
			if (shadowEntity instanceof Mob mob) {
				mob.setTarget(null);
				mob.getNavigation().stop();
			}
			return true;
		}
		if (owner.isAlive() && owner.level() == shadowEntity.level())
			return false;
		dismissLoadedShadow(owner, shadowEntity);
		return true;
	}

	private static void dismissLoadedShadow(Player owner,
			Entity shadowEntity) {
		if (shadowEntity == null || shadowEntity.level().isClientSide()
				|| shadowEntity.isRemoved())
			return;
		CompoundTag entityData = shadowEntity.getPersistentData();
		if (owner == null && shadowEntity.level() instanceof ServerLevel level) {
			UUID ownerId = getShadowOwnerUUID(shadowEntity);
			if (ownerId != null)
				owner = findOnlineOwner(level, ownerId);
		}

		String type = entityData.getString(SHADOW_TYPE);
		if (type.isEmpty())
			type = typeFromEntity(shadowEntity);
		if (owner != null) {
			String shadowId = entityData.getString(SHADOW_ID);
			CompoundTag rosterShadow = shadowId.isEmpty()
					? null : getShadow(owner, shadowId);
			boolean releasedRosterSlot = rosterShadow != null
					&& rosterShadow.hasUUID("summoned")
					&& shadowEntity.getUUID().equals(
							rosterShadow.getUUID("summoned"));
			saveBossHealthBeforeDespawn(owner, shadowEntity);
			if (releasedRosterSlot) {
				rosterShadow.remove("summoned");
				owner.getPersistentData().put(ROOT, root(owner));
			}
			if ((releasedRosterSlot || shadowId.isEmpty()) && !type.isEmpty())
				updateLegacySpawnCounter(owner, type, -1);
		}
		dropStoredShadowInventory(shadowEntity);
		shadowEntity.discard();
	}

	public static String getShadowRosterId(Entity entity) {
		if (entity == null)
			return "";
		return entity.getPersistentData().getString(SHADOW_ID);
	}

	public static boolean isOwnedShadow(Entity shadow, LivingEntity owner) {
		UUID ownerId = getShadowOwnerUUID(shadow);
		return isShadowEntity(shadow) && owner != null && ownerId != null && ownerId.equals(owner.getUUID());
	}

	/**
	 * Shared, fail-closed predicate for scripted shadow attacks. In addition to
	 * the normal hostile-target rules this applies party, team, tame-owner and
	 * shadow-owner checks from {@link MageCombatHelper}, so area attacks cannot
	 * splash the monarch or any allied unit.
	 */
	public static boolean canShadowDamage(Entity shadow, Entity candidate) {
		if (!(shadow instanceof Mob mob)
				|| !(candidate instanceof LivingEntity target))
			return false;
		Player owner = getShadowOwnerPlayer(shadow);
		if (owner == null)
			return false;
		if (!target.isAlive() || !target.isAttackable() || target.isInvulnerable())
			return false;
		Player targetPlayer = target instanceof Player player ? player
				: target instanceof TamableAnimal targetTame
						&& targetTame.getOwner() instanceof Player player ? player : null;
		if (targetPlayer != null && haveSameGuild(owner, targetPlayer))
			return false;
		return MageCombatHelper.isValidTarget(shadow, target)
				&& (target == findOwnerCombatPriorityTarget(mob, owner)
						? isValidOwnerDirectedTarget(target, mob, owner)
						: isValidShadowTarget(target, mob, owner));
	}

	private static boolean haveSameGuild(Player first, Player second) {
		double firstGuild = first.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(data -> data.GuildCode).orElse(0.0D);
		double secondGuild = second.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(data -> data.GuildCode).orElse(0.0D);
		return Double.isFinite(firstGuild) && firstGuild != 0.0D
				&& Double.compare(firstGuild, secondGuild) == 0;
	}

	/**
	 * Removes one player's shadow roster and every currently loaded summon.
	 * Stored mana stones are dropped before entities are discarded. A generation
	 * marker also invalidates summons in unloaded chunks when they load later.
	 */
	public static void resetPlayerProgress(ServerPlayer player) {
		if (player == null || player.server == null)
			return;
		CompoundTag playerData = player.getPersistentData();
		if (playerData.contains(ROOT, Tag.TAG_COMPOUND)) {
			ListTag roster = playerData.getCompound(ROOT).getList(SHADOWS, Tag.TAG_COMPOUND);
			for (int index = 0; index < roster.size(); index++)
				returnEquipmentToPlayer(player, roster.getCompound(index));
		}
		long currentGeneration = playerData.getLong(PLAYER_RESET_GENERATION);
		long nextGeneration = currentGeneration == Long.MAX_VALUE
				? 1L : Math.max(1L, currentGeneration + 1L);
		playerData.putLong(PLAYER_RESET_GENERATION, nextGeneration);
		CLEAR_DUNGEON_STATES.remove(player.getUUID());

		ArrayList<Entity> loadedOwnedShadows = new ArrayList<>();
		for (ServerLevel level : player.server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				UUID ownerId = getShadowOwnerUUID(entity);
				if (player.getUUID().equals(ownerId)
						&& (isShadowEntity(entity) || isTrackedShadowEntity(entity)))
					loadedOwnedShadows.add(entity);
			}
		}
		for (Entity shadow : loadedOwnedShadows) {
			dropStoredShadowInventory(shadow);
			shadow.discard();
		}
		playerData.remove(ROOT);
		playerData.remove(PLAYER_COMMAND);
	}

	public static boolean haveSameShadowOwner(Entity first, Entity second) {
		UUID firstOwner = getShadowOwnerUUID(first);
		UUID secondOwner = getShadowOwnerUUID(second);
		return isShadowEntity(first) && isShadowEntity(second) && firstOwner != null && firstOwner.equals(secondOwner);
	}

	public static boolean commandSummonedShadows(Player player, String requestedCommand) {
		if (!(player instanceof ServerPlayer serverPlayer))
			return false;
		String command = normalizeCommand(requestedCommand);
		if (command.isEmpty())
			return false;
		if (COMMAND_CLEAR_DUNGEON.equals(command) && !isInDungeon(player)) {
			serverPlayer.displayClientMessage(Component.literal("Clear Dungeon can only be used inside a dungeon."), true);
			return false;
		}
		ensureRoster(player);
		absorbVisibleOwnedShadows(player);
		CLEAR_DUNGEON_STATES.remove(player.getUUID());
		player.getPersistentData().putString(PLAYER_COMMAND, command);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.berserk = COMMAND_BERSERK.equals(command);
			capability.syncPlayerVariables(player);
		});
		List<CompoundTag> summoned = summonedOwnedShadows(player);
		for (CompoundTag shadow : summoned) {
			Entity entity = serverPlayer.serverLevel().getEntity(shadow.getUUID("summoned"));
			if (entity == null || !entity.isAlive())
				continue;
			if (entity instanceof Mob mob) {
				resetShadowCommandState(mob);
				entity.getPersistentData().putString(SHADOW_COMMAND, command);
				applyCommandTarget(mob, player, command);
			} else {
				entity.getPersistentData().putString(SHADOW_COMMAND, command);
			}
		}
		serverPlayer.displayClientMessage(Component.literal("Shadow Command: " + commandDisplayName(command) + " (" + summoned.size() + " shadows)"), true);
		return true;
	}

	public static boolean hasShadowForDisplay(Player player, String type) {
		return ownedCountForDisplay(player, type) > 0;
	}

	public static String shadowCountText(Player player, String type) {
		if (player == null)
			return "0/0";
		int owned = ownedCountForDisplay(player, type);
		int summoned = Math.min(owned, summonedCountForDisplay(player, type));
		return summoned + "/" + owned;
	}

	public static void prepareRosterForDisplay(Player player) {
		if (player != null && !player.level().isClientSide())
			ensureRoster(player);
	}

	public static int highestRankForDisplay(Player player, String requestedType) {
		return progressForDisplay(player, requestedType).rank();
	}

	public static int highestLevelForDisplay(Player player, String requestedType) {
		return progressForDisplay(player, requestedType).level();
	}

	public static ShadowDisplayProgress progressForDisplay(Player player, String requestedType) {
		String type = normalizeShadowType(requestedType == null ? "" : requestedType);
		int initialRank = startingRank(type);
		if (player == null || type.isEmpty()
				|| !isShadowAvailableFor(player, type))
			return new ShadowDisplayProgress(initialRank, 1, 0, 1,
					initialRank, true, false, false, false);
		CompoundTag shadow = strongestOwnedShadow(player, type);
		if (shadow == null)
			return new ShadowDisplayProgress(initialRank, 1, 0, 1,
					initialRank, true, false, false, false);

		int level = Math.max(1, shadow.getInt("level"));
		int rank = rankOf(shadow);
		boolean grandMarshalActive = isClaimedGrandMarshal(player, shadow);
		boolean grandMarshalEligible = isGrandMarshalEligible(shadow);
		int maximumRank = maximumRank(type);
		boolean maximum = rank >= maximumRank;
		int nextRank = maximum ? rank : Math.min(maximumRank, rank + 1);

		int bandStart = level < 10 ? 1 : level - Math.floorMod(level, 10);
		long nextMilestoneLong = ((long) level / 10L + 1L) * 10L;
		int nextMilestone = (int) Math.min(MAX_SAFE_SHADOW_LEVEL, nextMilestoneLong);
		long progress = Math.max(0, shadow.getInt("xp"));
		for (int current = bandStart; current < level; current++)
			progress = saturatingDisplayXpAdd(progress, xpNeeded(current, type));
		long requirement = 0L;
		for (int current = bandStart; current < nextMilestone; current++)
			requirement = saturatingDisplayXpAdd(requirement, xpNeeded(current, type));
		int needed = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, requirement));
		int earned = grandMarshalEligible && !grandMarshalActive ? needed
				: (int) Math.max(0L, Math.min(needed, progress));
		return new ShadowDisplayProgress(rank, level, earned, needed, nextRank,
				level >= effectiveShadowLevelCap(player, shadow), maximum,
				grandMarshalEligible, grandMarshalActive);
	}

	public static boolean isGrandMarshalEligibleForDisplay(Player player,
			String requestedType) {
		if (player == null)
			return false;
		String type = normalizeShadowType(requestedType == null ? ""
				: requestedType);
		return isGrandMarshalEligible(strongestOwnedShadow(player, type));
	}

	public static boolean isGrandMarshalForDisplay(Player player,
			String requestedType) {
		if (player == null)
			return false;
		String type = normalizeShadowType(requestedType == null ? ""
				: requestedType);
		return isClaimedGrandMarshal(player, strongestOwnedShadow(player, type));
	}

	public static int grandMarshalRequiredLevel(String requestedType) {
		String type = normalizeShadowType(requestedType == null ? ""
				: requestedType);
		if (!isBoss(type))
			return MAX_ADMIN_SHADOW_LEVEL;
		return Math.max(10, (RANK_GRAND_MARSHAL - startingRank(type)) * 10);
	}

	public static String grandMarshalSignatureName(String requestedType) {
		String type = normalizeShadowType(requestedType == null ? ""
				: requestedType);
		return switch (type) {
			case "igris" -> "Crimson Cross";
			case "beru" -> "King's Restoration";
			case "kamish" -> "Dragon's Dread";
			case "tusk" -> "Gravitational Ruin";
			case "kaisel" -> "Sky Rend";
			default -> "Grand Marshal Authority";
		};
	}

	public static boolean hasAssignedGrandMarshal(Entity entity) {
		if (!(entity instanceof Player player))
			return false;
		if (!player.level().isClientSide())
			ensureRoster(player);
		String claimedId = root(player).getString(GRAND_MARSHAL_ID);
		CompoundTag claimed = claimedId.isEmpty() ? null
				: getShadow(player, claimedId);
		return claimed != null && rankOf(claimed) == RANK_GRAND_MARSHAL
				&& isGrandMarshalEligibleByLevel(claimed);
	}

	/**
	 * Promotes the strongest owned shadow represented by the selected boss card.
	 * A player has exactly one command seat: assigning a new Grand Marshal
	 * returns the previous commander to Marshal without deleting its progress.
	 */
	public static GrandMarshalAssignmentResult assignGrandMarshal(Player player,
			String requestedType) {
		if (!(player instanceof ServerPlayer serverPlayer))
			return new GrandMarshalAssignmentResult(false,
					"Grand Marshal assignment is server-authoritative.");
		if (!VesselProgressionManager.isShadowMonarch(serverPlayer))
			return new GrandMarshalAssignmentResult(false,
					"Only the Shadow Monarch can appoint a Grand Marshal.");
		String type = normalizeShadowType(requestedType == null ? ""
				: requestedType);
		if (!isBoss(type))
			return new GrandMarshalAssignmentResult(false,
					"Only boss shadows can become Grand Marshal.");

		ensureRoster(serverPlayer);
		CompoundTag target = strongestOwnedShadow(serverPlayer, type);
		if (target == null)
			return new GrandMarshalAssignmentResult(false,
					"You do not own that boss shadow.");
		if (isClaimedGrandMarshal(serverPlayer, target))
			return new GrandMarshalAssignmentResult(false,
					target.getString("name") + " is already your Grand Marshal.");
		if (rankOf(target) != RANK_MARSHAL)
			return new GrandMarshalAssignmentResult(false,
					target.getString("name") + " must first reach Marshal rank.");
		int requiredLevel = grandMarshalRequiredLevel(type);
		if (Math.max(1, target.getInt("level")) < requiredLevel)
			return new GrandMarshalAssignmentResult(false,
					"Grand Marshal promotion unlocks at shadow level "
							+ requiredLevel + ".");

		CompoundTag ownerRoot = root(serverPlayer);
		String oldId = ownerRoot.getString(GRAND_MARSHAL_ID);
		CompoundTag previous = oldId.isEmpty() ? null
				: getShadow(serverPlayer, oldId);
		if (previous != null && !previous.getString("id")
				.equals(target.getString("id"))) {
			previous.putInt(RANK, RANK_MARSHAL);
			refreshSummonedShadowRank(serverPlayer, previous);
		}
		target.putInt(STARTING_RANK, startingRank(type));
		target.putInt(RANK, RANK_GRAND_MARSHAL);
		ownerRoot.putString(GRAND_MARSHAL_ID, target.getString("id"));
		serverPlayer.getPersistentData().put(ROOT, ownerRoot);
		refreshSummonedShadowRank(serverPlayer, target);
		JobSkillManager.syncJobSkills(serverPlayer);

		Component title = Component.literal("GRAND MARSHAL APPOINTED")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		Component under = Component.literal(target.getString("name") + "\n")
				.withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(Component.literal(grandMarshalSignatureName(type))
						.withStyle(ChatFormatting.YELLOW,
								ChatFormatting.BOLD));
		SystemNotifications.showTitleUnder(serverPlayer,
				rankColor(RANK_GRAND_MARSHAL), 120, title, under);
		return new GrandMarshalAssignmentResult(true,
				target.getString("name") + " is now your Grand Marshal. "
						+ grandMarshalSignatureName(type)
						+ " is available in the skill list.");
	}

	public static GrandMarshalCommander activeGrandMarshal(
			ServerPlayer player) {
		if (player == null)
			return null;
		ensureRoster(player);
		String claimedId = root(player).getString(GRAND_MARSHAL_ID);
		CompoundTag shadow = claimedId.isEmpty() ? null
				: getShadow(player, claimedId);
		if (shadow == null || rankOf(shadow) != RANK_GRAND_MARSHAL
				|| !shadow.hasUUID("summoned"))
			return null;
		Entity summoned = findSummonedEntity(player,
				shadow.getUUID("summoned"));
		if (!(summoned instanceof LivingEntity living) || !living.isAlive()
				|| summoned.level() != player.level()
				|| !isCurrentSummonedInstance(player, summoned))
			return null;
		return new GrandMarshalCommander(claimedId,
				shadow.getString("type"), shadow.getString("name"),
				Math.max(1, shadow.getInt("level")), living);
	}

	private static long saturatingDisplayXpAdd(long current, int amount) {
		return Math.min(Integer.MAX_VALUE, Math.max(0L, current) + Math.max(0, amount));
	}

	public static boolean isCustomizableBoss(String requestedType) {
		String type = normalizeShadowType(requestedType == null ? "" : requestedType);
		return "igris".equals(type) || "tusk".equals(type);
	}

	public static boolean isValidBossEquipment(String requestedType, ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return true;
		String type = normalizeShadowType(requestedType == null ? "" : requestedType);
		return stack.getCount() == 1 && ("igris".equals(type)
				&& stack.is(SololevelingModItems.DEMON_KINGS_LONG_SWORD.get())
				|| "tusk".equals(type) && stack.is(SololevelingModItems.ORB_OF_AVARICE.get()));
	}

	public static ItemStack equipmentForDisplay(Player player, String requestedType) {
		if (player == null)
			return ItemStack.EMPTY;
		String type = normalizeShadowType(requestedType == null ? "" : requestedType);
		CompoundTag shadow = strongestOwnedShadow(player, type);
		return equipmentOf(shadow, player.registryAccess()).copy();
	}

	public static boolean hasEquipmentForDisplay(Player player, String requestedType) {
		return !equipmentForDisplay(player, requestedType).isEmpty();
	}

	/**
	 * Replaces the equipment on the strongest owned entry represented by a boss
	 * card. The full stack tag is retained, while both the public API and menu
	 * slot independently enforce the per-boss item whitelist.
	 */
	public static boolean setEquipmentForDisplay(Player player, String requestedType, ItemStack requestedStack) {
		if (player == null || player.level().isClientSide())
			return false;
		String type = normalizeShadowType(requestedType == null ? "" : requestedType);
		if (!isCustomizableBoss(type))
			return false;
		ensureRoster(player);
		CompoundTag shadow = strongestOwnedShadow(player, type);
		ItemStack stack = requestedStack == null ? ItemStack.EMPTY : requestedStack.copy();
		if (shadow == null || !isValidBossEquipment(type, stack))
			return false;
		if (stack.isEmpty()) {
			shadow.remove(EQUIPMENT);
		} else {
			stack.setCount(1);
			shadow.put(EQUIPMENT, ItemStackData.save(stack, player.registryAccess()));
		}
		if (shadow.hasUUID("summoned")) {
			Entity summoned = findSummonedEntity(player, shadow.getUUID("summoned"));
			if (summoned != null)
				syncEquipmentTag(summoned, shadow);
		}
		player.getPersistentData().put(ROOT, root(player));
		return true;
	}

	public static boolean isEquipmentEquipped(Entity shadowEntity, Item item) {
		if (shadowEntity == null || item == null)
			return false;
		CompoundTag data = shadowEntity.getPersistentData();
		if (!data.contains(SHADOW_EQUIPMENT, Tag.TAG_STRING))
			return false;
		return BuiltInRegistries.ITEM.getKey(item).toString()
				.equals(data.getString(SHADOW_EQUIPMENT));
	}

	public static String typeForSummonButton(int buttonId) {
		return switch (buttonId) {
			case 0 -> "goblin_club";
			case 1 -> "goblin_archer";
			case 2 -> "goblin_mage";
			case 3 -> "wolf";
			case 4 -> "knight";
			case 5 -> "polar_bear";
			case 6 -> "orc";
			case 7 -> "igris";
			case 8 -> "beru";
			case 9 -> "kamish";
			case 10 -> "high_orc";
			case 11 -> "tusk";
			case 12 -> "kaisel";
			case 13 -> "iron";
			default -> "";
		};
	}

	public static boolean isGrandMarshalType(String requestedType) {
		return requestedType != null && isBoss(normalizeShadowType(requestedType));
	}

	public static int startingRankForType(String type) {
		return startingRank(normalizeShadowType(type));
	}

	/** Returns the authoritative rank already applied to a summoned shadow. */
	public static int appliedShadowRank(Entity entity) {
		if (entity == null)
			return RANK_NORMAL;
		CompoundTag data = entity.getPersistentData();
		if (data.contains(APPLIED_RANK, Tag.TAG_INT))
			return Math.max(RANK_NORMAL, Math.min(RANK_GRAND_MARSHAL,
					data.getInt(APPLIED_RANK)));
		return Math.max(RANK_NORMAL, Math.min(RANK_GRAND_MARSHAL,
				startingRank(typeFromEntity(entity))));
	}

	public static int maximumRankForType(String type) {
		return maximumRank(normalizeShadowType(type));
	}

	public static String rankDisplayName(int rank) {
		return switch (Math.max(RANK_NORMAL, Math.min(RANK_GRAND_MARSHAL, rank))) {
			case RANK_ELITE -> "Elite";
			case RANK_KNIGHT -> "Knight";
			case RANK_ELITE_KNIGHT -> "Elite Knight";
			case RANK_GENERAL -> "General";
			case RANK_MARSHAL -> "Marshal";
			case RANK_GRAND_MARSHAL -> "Grand Marshal";
			default -> "Normal";
		};
	}

	public static int rankColor(int rank) {
		return switch (rank) {
			case RANK_ELITE -> 0xFF62D6FF;
			case RANK_KNIGHT -> 0xFF79A7FF;
			case RANK_ELITE_KNIGHT -> 0xFFB47CFF;
			case RANK_GENERAL -> 0xFFE36CFF;
			case RANK_MARSHAL -> 0xFFFF5CA8;
			case RANK_GRAND_MARSHAL -> 0xFFFFC84A;
			default -> 0xFFB8C1D9;
		};
	}

	public static int ownedCountForDisplay(Player player, String type) {
		if (player == null)
			return 0;
		String normalized = normalizeShadowType(type);
		if (normalized.isEmpty()
				|| !isShadowAvailableFor(player, normalized))
			return 0;
		return Math.max(legacyMax(player, normalized), countOwned(player, normalized));
	}

	public static int summonedCountForDisplay(Player player, String type) {
		if (player == null)
			return 0;
		String normalized = normalizeShadowType(type);
		if (normalized.isEmpty()
				|| !isShadowAvailableFor(player, normalized))
			return 0;
		int rosterCount = 0;
		ListTag shadows = shadows(player);
		for (int i = 0; i < shadows.size(); i++) {
			CompoundTag shadow = shadows.getCompound(i);
			if (normalized.equals(shadow.getString("type")) && shadow.hasUUID("summoned"))
				rosterCount++;
		}
		return Math.max(legacySpawned(player, normalized), rosterCount);
	}

	/** Converts actual missing health into the exact whole-mana GUI quote. */
	public static int healingManaCost(double missingHealth) {
		return ShadowHealingRules.manaCost(missingHealth);
	}

	/** Live server quote used by synchronized summon-menu fields. */
	public static ShadowHealingQuote healingQuote(Player player) {
		if (!(player instanceof ServerPlayer owner))
			return new ShadowHealingQuote(0, 0, 0, 0);
		List<ShadowHealingTarget> targets = healingTargets(owner);
		double bossMissing = 0.0D;
		double allMissing = 0.0D;
		int bossTargets = 0;
		for (ShadowHealingTarget target : targets) {
			allMissing += target.missingHealth();
			if (isHealingBossType(target.type())) {
				bossMissing += target.missingHealth();
				bossTargets++;
			}
		}
		return new ShadowHealingQuote(healingManaCost(bossMissing),
				healingManaCost(allMissing), bossTargets, targets.size());
	}

	/**
	 * Fully restores either the named boss roster or every live owned summon.
	 * Cost and targets are recomputed on the server when the packet arrives, so a
	 * stale tooltip or forged client packet can never underpay or heal strangers.
	 */
	public static ShadowHealingResult healSummonedShadows(ServerPlayer owner,
			boolean bossesOnly) {
		if (owner == null || !VesselProgressionManager.isShadowMonarch(owner))
			return new ShadowHealingResult(false, 0, 0, 0,
					"Only the Shadow Monarch can restore shadow soldiers.");
		List<ShadowHealingTarget> selected = new ArrayList<>();
		double missingHealth = 0.0D;
		for (ShadowHealingTarget target : healingTargets(owner)) {
			if (bossesOnly && !isHealingBossType(target.type()))
				continue;
			selected.add(target);
			missingHealth += target.missingHealth();
		}
		int quotedCost = healingManaCost(missingHealth);
		String group = bossesOnly ? "summoned boss shadows"
				: "summoned shadows";
		if (selected.isEmpty() || quotedCost <= 0)
			return new ShadowHealingResult(false, 0, 0, 0,
					"No " + group + " currently need healing.");
		if (!hasSummonMana(owner, quotedCost)) {
			int available = owner.getCapability(
					SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
					.map(data -> (int) Math.max(0.0D, Math.floor(data.MP)))
					.orElse(0);
			return new ShadowHealingResult(false, 0, 0, 0,
					"Not enough mana: " + quotedCost + " MP required ("
							+ available + " available).");
		}

		consumeSummonMana(owner, quotedCost);
		for (ShadowHealingTarget target : selected) {
			LivingEntity living = target.entity();
			living.setHealth(living.getMaxHealth());
			if (living.level() instanceof ServerLevel level) {
				level.sendParticles((SimpleParticleType)
						SololevelingModParticleTypes.SHADOW_REVIVE.get(),
						living.getX(), living.getY() + living.getBbHeight() * 0.55D,
						living.getZ(), 10, living.getBbWidth() * 0.3D,
						living.getBbHeight() * 0.25D,
						living.getBbWidth() * 0.3D, 0.03D);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
						living.getX(), living.getY() + living.getBbHeight() * 0.5D,
						living.getZ(), 8, living.getBbWidth() * 0.25D,
						living.getBbHeight() * 0.2D,
						living.getBbWidth() * 0.25D, 0.02D);
			}
		}
		owner.serverLevel().playSound(null, owner.blockPosition(),
				SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
				0.9F, bossesOnly ? 1.15F : 0.95F);
		int restored = (int) Math.min(Integer.MAX_VALUE,
				Math.ceil(missingHealth));
		int consumed = owner.getAbilities().instabuild ? 0 : quotedCost;
		return new ShadowHealingResult(true, selected.size(), consumed, restored,
				"Restored " + restored + " health across " + selected.size()
						+ (bossesOnly ? " boss shadow" : " shadow")
						+ (selected.size() == 1 ? "" : "s") + " for "
						+ consumed + " MP.");
	}

	private static List<ShadowHealingTarget> healingTargets(
			ServerPlayer owner) {
		ArrayList<ShadowHealingTarget> result = new ArrayList<>();
		if (owner == null || owner.server == null)
			return result;
		ensureRoster(owner);
		Set<UUID> seenEntities = new HashSet<>();
		ListTag roster = shadows(owner);
		for (int index = 0; index < roster.size(); index++) {
			CompoundTag shadow = roster.getCompound(index);
			if (!shadow.hasUUID("summoned"))
				continue;
			UUID entityId = shadow.getUUID("summoned");
			if (!seenEntities.add(entityId))
				continue;
			Entity entity = findSummonedEntity(owner, entityId);
			if (!(entity instanceof LivingEntity living) || !living.isAlive()
					|| living.isRemoved() || !isOwnedShadow(living, owner)
					|| !isCurrentSummonedInstance(owner, living))
				continue;
			double missing = Math.max(0.0D,
					living.getMaxHealth() - living.getHealth());
			if (missing <= 0.01D)
				continue;
			result.add(new ShadowHealingTarget(living,
					normalizeShadowType(shadow.getString("type")), missing));
		}
		return result;
	}

	/**
	 * Ticks every summoned shadow from the owner's roster. This intentionally
	 * avoids a world/AABB lookup per player; roster UUIDs are the authoritative
	 * summoned instances.
	 */
	public static void tickCommandedShadows(ServerPlayer owner) {
		if (owner == null || !owner.isAlive() || !(owner.level() instanceof ServerLevel level))
			return;
		List<Mob> clearDungeonShadows = new ArrayList<>();
		for (Mob mob : summonedOwnedMobs(owner)) {
			prepareShadowTraversal(mob);
			if ((level.getGameTime() + mob.getId()) % 20L < 10L)
				synchronizeShadowLevel(owner, mob);
			if (!isCurrentSummonedInstance(owner, mob)) {
				dropStoredShadowInventory(mob);
				mob.discard();
				continue;
			}
			String command = commandOrDefault(mob.getPersistentData().getString(SHADOW_COMMAND));
			if (applyOwnerCombatPriority(mob, owner))
				continue;
			if (COMMAND_CLEAR_DUNGEON.equals(command)) {
				if (isInDungeon(owner))
					clearDungeonShadows.add(mob);
				else {
					mob.setTarget(null);
					mob.getNavigation().stop();
				}
				continue;
			}
			applyCommandTarget(mob, owner, command);
		}
		if (clearDungeonShadows.isEmpty())
			CLEAR_DUNGEON_STATES.remove(owner.getUUID());
		else
			tickClearDungeonCoordinator(owner, clearDungeonShadows);
		cleanupClearDungeonStates(level.getGameTime());
	}

	/**
	 * Compatibility entry point for callers that only have one shadow. Clear
	 * Dungeon assignment is owner-scoped and therefore handled by
	 * {@link #tickCommandedShadows(ServerPlayer)}.
	 */
	public static void tickCommandedShadow(Entity entity) {
		if (!(entity instanceof Mob mob) || !(entity.level() instanceof ServerLevel level))
			return;
		CompoundTag data = entity.getPersistentData();
		if (!data.hasUUID(SHADOW_OWNER))
			return;
		prepareShadowTraversal(mob);
		Player owner = findOnlineOwner(level, data.getUUID(SHADOW_OWNER));
		if (owner != null && (level.getGameTime() + entity.getId()) % 20L == 0L)
			synchronizeShadowLevel(owner, entity);
		if (owner != null && !isCurrentSummonedInstance(owner, entity)) {
			dropStoredShadowInventory(entity);
			entity.discard();
			return;
		}
		String command = commandOrDefault(data.getString(SHADOW_COMMAND));
		if (owner == null || !owner.isAlive()) {
			mob.setTarget(null);
			return;
		}
		if (COMMAND_CLEAR_DUNGEON.equals(command)) {
			if (!isInDungeon(owner)) {
				mob.setTarget(null);
				mob.getNavigation().stop();
			}
			return;
		}
		applyCommandTarget(mob, owner, command);
	}

	/**
	 * Called by the shared target-selector goal. Owner combat intent is checked
	 * every tick; the more expensive command scans remain staggered.
	 */
	public static void tickShadowTargeting(Mob shadow) {
		if (shadow == null || shadow.level().isClientSide())
			return;
		Player owner = getShadowOwnerPlayer(shadow);
		if (owner == null || !owner.isAlive()) {
			shadow.setTarget(null);
			return;
		}
		if (applyOwnerCombatPriority(shadow, owner))
			return;
		if (Math.floorMod(shadow.tickCount + shadow.getId(), 5) != 0)
			return;
		String command = commandOrDefault(shadow.getPersistentData()
				.getString(SHADOW_COMMAND));
		if (COMMAND_CLEAR_DUNGEON.equals(command)) {
			LivingEntity current = shadow.getTarget();
			if (!isInDungeon(owner)
					|| current != null
							&& !isValidClearDungeonTarget(current, shadow, owner)) {
				shadow.setTarget(null);
				shadow.getNavigation().stop();
			}
			return;
		}
		applyCommandTarget(shadow, owner, command);
	}

	private static boolean summonShadow(ServerLevel level, ServerPlayer owner, CompoundTag shadow, Vec3 pos, boolean allowRecall) {
		if (allowRecall && shadow.hasUUID("summoned")) {
			Entity existing = findSummonedEntity(owner, shadow.getUUID("summoned"));
			if (existing != null && existing.isAlive()) {
				// Reusing a summon button normally recalls the existing entity. An ice
				// prison must remain a real encounter commitment, so do not let either
				// same-level teleporting or cross-dimension recreation bypass it.
				if (SilladIcePrisonManager.isImprisoned(existing)) {
					SilladIcePrisonManager.guardManualDismiss(owner);
					return false;
				}
				if (existing.level() == level) {
					existing.teleportTo(pos.x, pos.y, pos.z);
					existing.setYRot(owner.getYRot());
					existing.setXRot(0);
					applyLevelStats(existing, shadow, false);
					playSummonEffects(level, pos);
					return true;
				}
				return recallShadowFromOtherDimension(level, owner, shadow, existing, pos);
			}
			shadow.remove("summoned");
			owner.getPersistentData().put(ROOT, root(owner));
		}
		EntityType<?> type = entityType(shadow.getString("type"));
		if (type == null)
			return false;
		int manaCost = summonManaCost(shadow);
		if (!hasSummonMana(owner, manaCost)) {
			notifyInsufficientSummonMana(owner, shadow, manaCost);
			return false;
		}
		Entity spawned = type.spawn(level, BlockPos.containing(pos), MobSpawnType.MOB_SUMMONED);
		if (spawned == null)
			return false;
		spawned.moveTo(pos.x, pos.y, pos.z, owner.getYRot(), 0);
		tagSummonedEntity(owner, shadow, spawned);
		consumeSummonMana(owner, manaCost);
		updateLegacySpawnCounter(owner, shadow.getString("type"), 1);
		playSummonEffects(level, pos);
		return true;
	}

	private static boolean recallShadowFromOtherDimension(ServerLevel level, ServerPlayer owner, CompoundTag shadow, Entity existing, Vec3 pos) {
		EntityType<?> type = entityType(shadow.getString("type"));
		if (type == null)
			return false;
		int manaCost = summonManaCost(shadow);
		if (!hasSummonMana(owner, manaCost)) {
			notifyInsufficientSummonMana(owner, shadow, manaCost);
			return false;
		}
		ListTag carriedInventory = copyShadowInventory(existing);
		saveBossHealthBeforeDespawn(owner, existing);
		Entity spawned = type.spawn(level, BlockPos.containing(pos), MobSpawnType.MOB_SUMMONED);
		if (spawned == null)
			return false;
		existing.discard();
		spawned.moveTo(pos.x, pos.y, pos.z, owner.getYRot(), 0);
		tagSummonedEntity(owner, shadow, spawned);
		consumeSummonMana(owner, manaCost);
		if (carriedInventory != null)
			spawned.getPersistentData().put(SHADOW_INVENTORY, carriedInventory);
		playSummonEffects(level, pos);
		return true;
	}

	private static ListTag copyShadowInventory(Entity entity) {
		if (entity == null || !entity.getPersistentData().contains(SHADOW_INVENTORY, Tag.TAG_LIST))
			return null;
		return entity.getPersistentData().getList(SHADOW_INVENTORY, Tag.TAG_COMPOUND).copy();
	}

	private static void playSummonEffects(ServerLevel level, Vec3 pos) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt != null) {
			bolt.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(pos.x, pos.y - 1.0D, pos.z)));
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
		level.sendParticles((SimpleParticleType) SololevelingModParticleTypes.SHADOW_REVIVE.get(), pos.x, pos.y + 1.6D, pos.z, 20, 0.35D, 0.65D, 0.35D, 0.04D);
		level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 1.0D, pos.z, 80, 1.3D, 1.2D, 1.3D, 0.18D);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.8D, pos.z, 24, 0.8D, 0.8D, 0.8D, 0.04D);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 1.2D, pos.z, 35, 0.6D, 0.9D, 0.6D, 0.15D);
	}

	private static int summonManaCost(CompoundTag shadow) {
		String type = shadow.getString("type");
		int baseCost = switch (type) {
			case "goblin_club" -> 8;
			case "goblin_archer" -> 10;
			case "goblin_mage", "wolf" -> 14;
			case "knight" -> 18;
			case "polar_bear", "orc" -> 22;
			case "high_orc" -> 32;
			case "iron" -> 55;
			case "igris" -> 90;
			case "tusk" -> 120;
			case "kaisel" -> 140;
			case "beru" -> 180;
			case "kamish" -> 260;
			default -> 12;
		};
		double rankMultiplier = switch (rankOf(shadow)) {
			case RANK_ELITE -> 1.35D;
			case RANK_KNIGHT -> 1.8D;
			case RANK_ELITE_KNIGHT -> 2.35D;
			case RANK_GENERAL -> 3.0D;
			case RANK_MARSHAL -> 3.8D;
			case RANK_GRAND_MARSHAL -> 5.0D;
			default -> 1.0D;
		};
		double levelMultiplier = 1.0D + Math.max(0, shadow.getInt("level") - 1) * 0.0125D;
		return Math.max(1, (int) Math.ceil(baseCost * rankMultiplier * levelMultiplier));
	}

	private static boolean hasSummonMana(Player player, int cost) {
		if (player != null && player.getAbilities().instabuild)
			return true;
		return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(variables -> variables.MP >= cost).orElse(false);
	}

	private static void consumeSummonMana(Player player, int cost) {
		if (player != null && player.getAbilities().instabuild)
			return;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.MP = Math.max(0.0D, capability.MP - cost);
			capability.syncPlayerVariables(player);
		});
	}

	private static void notifyInsufficientSummonMana(ServerPlayer player, CompoundTag shadow, int cost) {
		long now = player.level().getGameTime();
		if (player.getPersistentData().getLong(INSUFFICIENT_MANA_NOTICE) > now)
			return;
		player.getPersistentData().putLong(INSUFFICIENT_MANA_NOTICE, now + 20L);
		SystemNotifications.showNegativeTitleUnder(player, 0xFFFF3D6E, 70,
				Component.literal("NOT ENOUGH MANA").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
				Component.literal(shadow.getString("name") + " requires " + cost + " MP").withStyle(ChatFormatting.GRAY));
	}

	private static void tagSummonedEntity(Player owner, CompoundTag shadow, Entity spawned) {
		spawned.getPersistentData().putString(SHADOW_ID, shadow.getString("id"));
		spawned.getPersistentData().putString(SHADOW_TYPE, shadow.getString("type"));
		spawned.getPersistentData().putUUID(SHADOW_OWNER, owner.getUUID());
		spawned.getPersistentData().putLong(SHADOW_GENERATION,
				owner.getPersistentData().getLong(PLAYER_RESET_GENERATION));
		String command = commandOrDefault(owner.getPersistentData().getString(PLAYER_COMMAND));
		spawned.getPersistentData().putString(SHADOW_COMMAND, command);
		shadow.putUUID("summoned", spawned.getUUID());
		owner.getPersistentData().put(ROOT, root(owner));
		if (spawned instanceof TamableAnimal tame)
			tame.tame(owner);
		syncEquipmentTag(spawned, shadow);
		applyLevelStats(spawned, shadow, true);
		if (spawned instanceof Mob mob)
			applyCommandTarget(mob, owner, command);
	}

	private static Vec3 spreadSummonPosition(Player player, Vec3 origin, int index) {
		if (index <= 0)
			return origin;
		Vec3 look = player.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0.0D, look.z);
		if (forward.lengthSqr() < 0.001D)
			forward = new Vec3(0.0D, 0.0D, 1.0D);
		forward = forward.normalize();
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		int ringIndex = index - 1;
		int slot = ringIndex % 8;
		int ring = ringIndex / 8;
		double radius = 1.6D + ring * 1.15D;
		double angle = slot * (Math.PI * 2.0D / 8.0D) + ring * 0.35D;
		return origin.add(right.scale(Math.cos(angle) * radius)).add(forward.scale(Math.sin(angle) * radius));
	}

	private static void applyCommandTarget(Mob shadow, Player owner, String command) {
		if (applyOwnerCombatPriority(shadow, owner))
			return;
		if (COMMAND_DEFAULT.equals(command)) {
			shadow.setTarget(findDefaultCommandTarget(shadow, owner));
			return;
		}
		if (COMMAND_FOLLOW.equals(command)) {
			shadow.setTarget(null);
			shadow.getNavigation().stop();
			return;
		}
		if (COMMAND_CLEAR_DUNGEON.equals(command) && !isInDungeon(owner)) {
			shadow.setTarget(null);
			shadow.getNavigation().stop();
			return;
		}
		if (COMMAND_CLEAR_DUNGEON.equals(command)) {
			// Clear Dungeon target acquisition is deliberately owner-scoped. The
			// level tick coordinator shares one bounded candidate refresh and path
			// budget across the full summoned group.
			return;
		}
		if (COMMAND_PROTECT.equals(command)) {
			LivingEntity threat = findOwnerThreat(shadow, owner);
			shadow.setTarget(threat);
			return;
		}
		LivingEntity current = shadow.getTarget();
		if (isValidShadowTarget(current, shadow, owner))
			return;
		LivingEntity target = findNearestHostile(shadow, owner, 48.0D);
		shadow.setTarget(target);
	}

	private static void resetShadowCommandState(Mob shadow) {
		shadow.setTarget(null);
		shadow.getNavigation().stop();
		CompoundTag data = shadow.getPersistentData();
		data.putDouble("MF", 0.0D);
		data.putBoolean("sprint", false);
		if (shadow instanceof IgrisShadowEntity igris) {
			data.putString("state", "idle");
			igris.animationprocedure = "empty";
			igris.setAnimation("empty");
		}
	}

	public static LivingEntity findDefaultCommandTarget(Mob shadow, Player owner) {
		if (shadow == null || owner == null)
			return null;
		LivingEntity priority = findOwnerCombatPriorityTarget(shadow, owner);
		if (priority != null)
			return priority;
		LivingEntity current = shadow.getTarget();
		if (isDefaultLinkedTarget(current, shadow, owner))
			return current;
		LivingEntity ownerTarget = owner.getLastHurtMob();
		if (isValidOwnerDirectedTarget(ownerTarget, shadow, owner))
			return ownerTarget;
		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		if (isValidShadowTarget(ownerAttacker, shadow, owner))
			return ownerAttacker;
		return shadow.level().getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(32.0D), mob -> mob.getTarget() == owner && isValidShadowTarget(mob, shadow, owner)).stream()
				.min((a, b) -> Double.compare(a.distanceToSqr(shadow), b.distanceToSqr(shadow))).orElse(null);
	}

	private static boolean isDefaultLinkedTarget(LivingEntity target, Mob shadow, Player owner) {
		if (target == owner.getLastHurtMob())
			return isValidOwnerDirectedTarget(target, shadow, owner);
		if (!isValidShadowTarget(target, shadow, owner))
			return false;
		return target == owner.getLastHurtByMob() || target instanceof Mob mob && isProtectTarget(mob.getTarget(), owner);
	}

	private static boolean isValidOwnerDirectedTarget(LivingEntity target, Mob shadow, Player owner) {
		if (target == null || !target.isAlive() || target.level() != shadow.level()
				|| target == shadow || target == owner || !target.isAttackable()
				|| target.isInvulnerable())
			return false;
		if (target.getType().is(SHADOW_ENTITY_TAG)
				|| owner.isAlliedTo(target) || shadow.isAlliedTo(target))
			return false;
		if (target instanceof TamableAnimal tame && owner.getUUID().equals(tame.getOwnerUUID()))
			return false;
		Player targetPlayer = target instanceof Player player ? player
				: target instanceof TamableAnimal tame
						&& tame.getOwner() instanceof Player player ? player : null;
		if (targetPlayer != null && haveSameGuild(owner, targetPlayer))
			return false;
		return MageCombatHelper.isValidTarget(shadow, target);
	}

	/**
	 * The monarch's most recent combat event outranks every command mode. If the
	 * player attacks after being hit, shadows switch to that victim; if the
	 * player is then attacked, they immediately peel to the new attacker.
	 */
	public static LivingEntity findOwnerCombatPriorityTarget(Mob shadow) {
		return findOwnerCombatPriorityTarget(shadow,
				getShadowOwnerPlayer(shadow));
	}

	public static LivingEntity findOwnerCombatPriorityTarget(Mob shadow,
			Player owner) {
		if (shadow == null || owner == null || !owner.isAlive()
				|| owner.level() != shadow.level())
			return null;
		LivingEntity attacked = owner.getLastHurtMob();
		LivingEntity attacker = owner.getLastHurtByMob();
		boolean attackedValid = isValidOwnerDirectedTarget(attacked, shadow,
				owner);
		boolean attackerValid = isValidOwnerDirectedTarget(attacker, shadow,
				owner);
		if (attackedValid && attackerValid)
			return owner.getLastHurtMobTimestamp()
					>= owner.getLastHurtByMobTimestamp() ? attacked : attacker;
		if (attackedValid)
			return attacked;
		return attackerValid ? attacker : null;
	}

	private static boolean applyOwnerCombatPriority(Mob shadow, Player owner) {
		LivingEntity priority = findOwnerCombatPriorityTarget(shadow, owner);
		if (priority == null)
			return false;
		if (shadow.getTarget() != priority)
			shadow.setTarget(priority);
		return true;
	}

	private static LivingEntity findOwnerThreat(Mob shadow, Player owner) {
		LivingEntity current = shadow.getTarget();
		if (isProtectThreat(current, shadow, owner))
			return current;
		LivingEntity attacker = owner.getLastHurtByMob();
		if (isValidShadowTarget(attacker, shadow, owner))
			return attacker;
		AABB searchArea = owner.getBoundingBox().minmax(shadow.getBoundingBox()).inflate(48.0D);
		return shadow.level().getEntitiesOfClass(Mob.class, searchArea, mob -> isProtectThreat(mob, shadow, owner)).stream()
				.min((a, b) -> Double.compare(a.distanceToSqr(shadow), b.distanceToSqr(shadow))).orElse(null);
	}

	private static boolean isProtectThreat(LivingEntity candidate, Mob shadow, Player owner) {
		if (!isValidShadowTarget(candidate, shadow, owner))
			return false;
		if (candidate == owner.getLastHurtByMob())
			return true;
		return candidate instanceof Mob mob && isProtectTarget(mob.getTarget(), owner);
	}

	private static boolean isProtectTarget(LivingEntity target, Player owner) {
		return target == owner || isOwnedShadow(target, owner);
	}

	private static LivingEntity findNearestHostile(Mob shadow, Player owner, double range) {
		return shadow.level().getEntitiesOfClass(LivingEntity.class, shadow.getBoundingBox().inflate(range), target -> isValidShadowTarget(target, shadow, owner)).stream()
				.min((a, b) -> Double.compare(a.distanceToSqr(shadow), b.distanceToSqr(shadow))).orElse(null);
	}

	public static boolean shouldFollowOwner(Entity shadow) {
		if (shadow == null)
			return false;
		String command = commandOrDefault(shadow.getPersistentData()
				.getString(SHADOW_COMMAND));
		return COMMAND_DEFAULT.equals(command)
				|| COMMAND_PROTECT.equals(command)
				|| COMMAND_FOLLOW.equals(command);
	}

	public static boolean isValidClearDungeonTarget(LivingEntity target, Mob shadow, Player owner) {
		if (!(owner instanceof ServerPlayer serverOwner) || target == null || target.level() != owner.level()
				|| !isValidShadowTarget(target, shadow, owner))
			return false;
		return matchesDungeonContext(target, serverOwner, dungeonTargetContext(serverOwner));
	}

	private static boolean isValidShadowTarget(LivingEntity target, Mob shadow, Player owner) {
		if (target == null || !target.isAlive()
				|| target.level() != shadow.level()
				|| target == shadow || target == owner
				|| !target.isAttackable() || target.isInvulnerable())
			return false;
		if (target instanceof Player)
			return false;
		if (target.getType().is(SHADOW_ENTITY_TAG))
			return false;
		if (target instanceof TamableAnimal tame && owner.getUUID().equals(tame.getOwnerUUID()))
			return false;
		Player targetPlayer = target instanceof TamableAnimal tame
				&& tame.getOwner() instanceof Player player ? player : null;
		if (targetPlayer != null && haveSameGuild(owner, targetPlayer))
			return false;
		if (!MageCombatHelper.isValidTarget(shadow, target))
			return false;
		return target instanceof Monster
				|| target instanceof Mob mob
						&& isProtectTarget(mob.getTarget(), owner);
	}

	public static boolean canReachShadowTarget(Mob shadow, LivingEntity target) {
		if (shadow == null || target == null || !target.isAlive())
			return false;
		if (CombatRangeHelper.withinSurfaceRange(shadow, target, 6.0D) && shadow.hasLineOfSight(target))
			return true;
		Path path = shadow.getNavigation().createPath(target.blockPosition(), 0);
		return path != null && path.canReach();
	}

	private static void tickClearDungeonCoordinator(ServerPlayer owner, List<Mob> shadows) {
		ServerLevel level = owner.serverLevel();
		long now = level.getGameTime();
		DungeonTargetContext context = dungeonTargetContext(owner);
		ClearDungeonState state = CLEAR_DUNGEON_STATES.computeIfAbsent(owner.getUUID(), ignored -> new ClearDungeonState());
		if (state.lastSeenTick > now || !context.key().equals(state.contextKey)) {
			state.reset(context.key(), now);
			for (Mob shadow : shadows) {
				shadow.setTarget(null);
				shadow.getNavigation().stop();
			}
		}
		state.lastSeenTick = now;
		state.failedUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
		state.progress.keySet().removeIf(shadowId -> shadows.stream().noneMatch(shadow -> shadow.getUUID().equals(shadowId)));
		if (now >= state.nextCandidateScanTick) {
			state.candidateIds = collectClearDungeonCandidates(owner, shadows, context).stream().map(Entity::getUUID).toList();
			state.nextCandidateScanTick = now + CLEAR_SCAN_INTERVAL_TICKS;
		}

		Map<UUID, Integer> assignments = new HashMap<>();
		for (Mob shadow : shadows) {
			LivingEntity current = shadow.getTarget();
			if (isValidClearDungeonTarget(current, shadow, owner)
					&& !isFailedTarget(state, shadow, current, now))
				assignments.merge(current.getUUID(), 1, Integer::sum);
		}

		PathAttemptBudget pathBudget = new PathAttemptBudget();
		for (Mob shadow : shadows) {
			LivingEntity current = shadow.getTarget();
			if (current == null && tickClearTraversal(owner, shadow, state,
					now, pathBudget))
				continue;
			if (isValidClearDungeonTarget(current, shadow, owner)
					&& !isFailedTarget(state, shadow, current, now)) {
				if (tickClearTargetProgress(state, shadow, current, now, pathBudget))
					continue;
				assignments.computeIfPresent(current.getUUID(), (id, count) -> count > 1 ? count - 1 : null);
			} else {
				shadow.setTarget(null);
				state.progress.remove(shadow.getUUID());
			}
			assignClearDungeonTarget(owner, shadow, state, assignments, now, pathBudget);
		}
	}

	private static boolean tickClearTargetProgress(ClearDungeonState state, Mob shadow, LivingEntity target,
			long now, PathAttemptBudget pathBudget) {
		ClearShadowProgress progress = state.progress.computeIfAbsent(shadow.getUUID(), ignored -> new ClearShadowProgress());
		double distance = shadow.distanceToSqr(target);
		if (!target.getUUID().equals(progress.targetId)) {
			progress.reset(target, shadow, distance, now);
			return true;
		}
		progress.traversing = false;
		progress.lastShadowPosition = shadow.position();
		boolean hasUsefulSight = shadow.hasLineOfSight(target);
		boolean closeEnoughToFight = hasUsefulSight
				&& CombatRangeHelper.withinSurfaceRange(shadow, target, 7.0D);
		boolean damagedTarget = target.getHealth() + 0.01F < progress.lastTargetHealth;
		if (closeEnoughToFight || damagedTarget || distance + 1.0D < progress.lastDistance)
			progress.lastProgressTick = now;
		progress.lastDistance = distance;
		progress.lastTargetHealth = target.getHealth();

		if (!hasUsefulSight && shadow.getNavigation().isDone() && now >= progress.nextRepathTick
				&& pathBudget.tryUse()) {
			if (!startClearDungeonPath(shadow, target)) {
				// A partial route is travel, not combat. Clear the Mob target before
				// the melee goal can replace that route with direct wall pressure.
				advanceTowardObjective(shadow, target);
				shadow.setTarget(null);
				progress.beginTraversal(target, shadow, distance, now);
				return true;
			}
			progress.nextRepathTick = now + clearRepathDelay(shadow);
		}
		if (now - progress.lastProgressTick < CLEAR_STUCK_TICKS)
			return true;
		// The boss is never blacklisted; abandoning it would strand the group
		// with nothing to do for the rest of the run.
		if (isClearDungeonBoss(target)) {
			progress.lastProgressTick = now;
			return true;
		}
		failClearDungeonTarget(state, shadow, target, now);
		return false;
	}

	private static void assignClearDungeonTarget(ServerPlayer owner, Mob shadow, ClearDungeonState state,
			Map<UUID, Integer> assignments, long now, PathAttemptBudget pathBudget) {
		if (!(shadow.level() instanceof ServerLevel level))
			return;
		// Sweep first: anything hostile within engagement range gets fought.
		Set<UUID> examined = new HashSet<>();
		for (int choice = 0; choice < CLEAR_MAX_TARGET_CHOICES_PER_SHADOW; choice++) {
			LivingEntity candidate = selectClearDungeonCandidate(owner, shadow, state, assignments,
					examined, now, level, CLEAR_ENGAGE_RADIUS_SQR);
			if (candidate == null)
				break;
			examined.add(candidate.getUUID());
			boolean hasSight = shadow.hasLineOfSight(candidate);
			if (shadow instanceof ShadowKaiselinEntity && !hasSight) {
				failClearDungeonTarget(state, shadow, candidate, now);
				continue;
			}
			if (!hasSight && !pathBudget.tryUse())
				break;
			ClearShadowProgress progress = state.progress.computeIfAbsent(shadow.getUUID(), ignored -> new ClearShadowProgress());
			if (hasSight || startClearDungeonPath(shadow, candidate)) {
				shadow.setTarget(candidate);
				progress.reset(candidate, shadow, shadow.distanceToSqr(candidate), now);
			} else if (advanceTowardObjective(shadow, candidate)) {
				shadow.setTarget(null);
				progress.beginTraversal(candidate, shadow,
						shadow.distanceToSqr(candidate), now);
			} else {
				failClearDungeonTarget(state, shadow, candidate, now);
				continue;
			}
			assignments.merge(candidate.getUUID(), 1, Integer::sum);
			return;
		}

		// Nothing left in sweep range: push toward the boss rather than idling.
		LivingEntity objective = selectClearDungeonObjective(owner, shadow, state, level);
		if (objective != null) {
			ClearShadowProgress progress = state.progress.computeIfAbsent(shadow.getUUID(),
					ignored -> new ClearShadowProgress());
			if (shadow.hasLineOfSight(objective)
					|| startClearDungeonPath(shadow, objective)) {
				shadow.setTarget(objective);
				progress.reset(objective, shadow,
						shadow.distanceToSqr(objective), now);
			} else {
				shadow.setTarget(null);
				if (!objective.getUUID().equals(progress.targetId)
						|| !progress.traversing)
					progress.beginTraversal(objective, shadow,
							shadow.distanceToSqr(objective), now);
				if (now >= progress.nextRepathTick) {
					advanceTowardObjective(shadow, objective);
					progress.nextRepathTick = now
							+ CLEAR_ADVANCE_REPATH_TICKS;
				}
			}
			return;
		}

		// Truly nothing hostile left: regroup on the owner instead of freezing.
		shadow.setTarget(null);
		state.progress.remove(shadow.getUUID());
		if (shadow.distanceToSqr(owner) > 36.0D)
			advanceTowardObjective(shadow, owner);
		else
			shadow.getNavigation().stop();
	}

	private static LivingEntity selectClearDungeonCandidate(ServerPlayer owner, Mob shadow,
			ClearDungeonState state, Map<UUID, Integer> assignments, Set<UUID> examined,
			long now, ServerLevel level, double maxDistanceSqr) {
		LivingEntity best = null;
		int bestAssignments = Integer.MAX_VALUE;
		double bestDistance = Double.MAX_VALUE;
		for (UUID candidateId : state.candidateIds) {
			if (examined.contains(candidateId))
				continue;
			Entity entity = level.getEntity(candidateId);
			if (!(entity instanceof LivingEntity candidate)
					|| !isValidClearDungeonTarget(candidate, shadow, owner)
					|| isFailedTarget(state, shadow, candidate, now))
				continue;
			int assigned = assignments.getOrDefault(candidateId, 0);
			double distance = shadow.distanceToSqr(candidate);
			if (distance > maxDistanceSqr)
				continue;
			if (assigned < bestAssignments || assigned == bestAssignments && distance < bestDistance) {
				best = candidate;
				bestAssignments = assigned;
				bestDistance = distance;
			}
		}
		return best;
	}

	/** Dungeon bosses are the objective, so they are never given up on. */
	private static boolean isClearDungeonBoss(LivingEntity target) {
		if (target == null)
			return false;
		if (target.getType().is(SOLO_BOSS_TAG))
			return true;
		return DungeonMobLevelAdapter.MobRole.fromString(target.getPersistentData()
				.getString(DungeonMobLevelAdapter.ROLE_TAG))
				== DungeonMobLevelAdapter.MobRole.BOSS;
	}

	/**
	 * Drives a shadow toward an objective it cannot currently fight.
	 *
	 * <p>A full path is preferred and a partial path is still useful for advancing
	 * through a long corridor. Ground shadows are never direct-steered toward the
	 * objective: doing that bypasses the path and makes them run into the wall
	 * between rooms.
	 */
	private static boolean advanceTowardObjective(Mob shadow, LivingEntity objective) {
		if (objective == null)
			return false;
		double x = objective.getX();
		double y = objective.getY();
		double z = objective.getZ();
		shadow.getLookControl().setLookAt(objective, 30.0F, 30.0F);
		if (shadow instanceof ShadowKaiselinEntity) {
			// Kaisel flies; the ground navigator would refuse most of these routes.
			shadow.getMoveControl().setWantedPosition(x, y, z, CLEAR_ADVANCE_SPEED);
			return true;
		}
		Path path = shadow.getNavigation().createPath(objective.blockPosition(), 0);
		if (path != null && shadow.getNavigation().moveTo(path, CLEAR_ADVANCE_SPEED))
			return true;
		if (shadow.getNavigation().moveTo(x, y, z, CLEAR_ADVANCE_SPEED))
			return true;
		return false;
	}

	/** Nearest objective for a shadow with nothing in sweep range: boss first. */
	private static LivingEntity selectClearDungeonObjective(ServerPlayer owner, Mob shadow,
			ClearDungeonState state, ServerLevel level) {
		LivingEntity boss = null;
		double bossDistance = Double.MAX_VALUE;
		LivingEntity nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (UUID candidateId : state.candidateIds) {
			Entity entity = level.getEntity(candidateId);
			if (!(entity instanceof LivingEntity candidate)
					|| !isValidClearDungeonTarget(candidate, shadow, owner))
				continue;
			double distance = shadow.distanceToSqr(candidate);
			if (isClearDungeonBoss(candidate)) {
				if (distance < bossDistance) {
					boss = candidate;
					bossDistance = distance;
				}
			} else if (distance < nearestDistance) {
				nearest = candidate;
				nearestDistance = distance;
			}
		}
		return boss != null ? boss : nearest;
	}

	private static boolean startClearDungeonPath(Mob shadow, LivingEntity target) {
		if (shadow instanceof ShadowKaiselinEntity)
			return shadow.hasLineOfSight(target);
		Path path = shadow.getNavigation().createPath(target.blockPosition(), 0);
		if (path == null || !path.canReach())
			return false;
		// Combat targets require a complete path. Partial routes are handled by the
		// traversal state without setting Mob.target, so melee goals cannot replace
		// the route with direct movement into a wall.
		return shadow.getNavigation().moveTo(path, 1.0D);
	}

	private static boolean tickClearTraversal(ServerPlayer owner, Mob shadow,
			ClearDungeonState state, long now, PathAttemptBudget pathBudget) {
		ClearShadowProgress progress = state.progress.get(shadow.getUUID());
		if (progress == null || !progress.traversing || progress.targetId == null)
			return false;
		Entity entity = owner.serverLevel().getEntity(progress.targetId);
		if (!(entity instanceof LivingEntity objective)
				|| !isValidClearDungeonTarget(objective, shadow, owner)
				|| isFailedTarget(state, shadow, objective, now)) {
			state.progress.remove(shadow.getUUID());
			shadow.getNavigation().stop();
			return false;
		}
		if (shadow instanceof BeruShadowEntity beru
				&& (shadow.isInWaterOrBubble()
						|| !shadow.onGround() && shadow.fallDistance > 1.5F))
			beru.beginTraversalRecoveryFlight(objective);

		double distance = shadow.distanceToSqr(objective);
		double movedSqr = shadow.position().distanceToSqr(
				progress.lastShadowPosition);
		if (movedSqr > 0.025D || distance + 1.0D < progress.lastDistance)
			progress.lastProgressTick = now;
		progress.lastDistance = distance;
		progress.lastShadowPosition = shadow.position();

		if (shadow.hasLineOfSight(objective)
				|| now >= progress.nextRepathTick && pathBudget.tryUse()
						&& startClearDungeonPath(shadow, objective)) {
			shadow.setTarget(objective);
			progress.reset(objective, shadow, distance, now);
			return true;
		}
		if (shadow.getNavigation().isDone()
				&& now >= progress.nextRepathTick) {
			if (pathBudget.tryUse())
				advanceTowardObjective(shadow, objective);
			progress.nextRepathTick = now + clearRepathDelay(shadow);
		}

		if (now - progress.lastProgressTick < CLEAR_STUCK_TICKS)
			return true;
		if (tryRecallShadowNearOwner(shadow, owner, objective)) {
			progress.lastProgressTick = now;
			progress.lastDistance = shadow.distanceToSqr(objective);
			progress.lastShadowPosition = shadow.position();
			progress.nextRepathTick = now + 5L;
			return true;
		}
		if (shadow instanceof BeruShadowEntity beru) {
			beru.beginTraversalRecoveryFlight(objective);
			progress.lastProgressTick = now;
			return true;
		}
		if (isClearDungeonBoss(objective)) {
			// The boss remains authoritative. Keep retrying without turning it into
			// a combat target until a genuine route or owner-side recovery exists.
			progress.lastProgressTick = now;
			progress.nextRepathTick = now + clearRepathDelay(shadow);
			return true;
		}
		failClearDungeonTarget(state, shadow, objective, now);
		return false;
	}

	private static void prepareShadowTraversal(Mob shadow) {
		if (shadow == null)
			return;
		shadow.getNavigation().setCanFloat(true);
		if (shadow.maxUpStep() < SHADOW_TRAVERSAL_STEP_HEIGHT)
			shadow.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(SHADOW_TRAVERSAL_STEP_HEIGHT);
		if ((shadow.isInWaterOrBubble() || shadow.horizontalCollision)
				&& !shadow.getNavigation().isDone())
			shadow.getJumpControl().jump();
	}

	/**
	 * Recovers a stalled shadow only after its owner has legitimately reached a
	 * meaningfully better side of the dungeon. This avoids wall-clipping while
	 * still catching up shadows trapped by carpets, vines, water, or small ledges.
	 */
	private static boolean tryRecallShadowNearOwner(Mob shadow,
			ServerPlayer owner, LivingEntity objective) {
		if (shadow.distanceToSqr(owner) < 100.0D
				|| owner.distanceToSqr(objective) + 64.0D
						>= shadow.distanceToSqr(objective))
			return false;
		ServerLevel level = owner.serverLevel();
		BlockPos origin = owner.blockPosition();
		for (int radius = 2; radius <= 4; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius)
						continue;
					for (int dy = -1; dy <= 2; dy++) {
						BlockPos position = origin.offset(dx, dy, dz);
						if (!isSafeShadowRecoveryPosition(level, shadow,
								position))
							continue;
						shadow.getNavigation().stop();
						shadow.teleportTo(position.getX() + 0.5D,
								position.getY(), position.getZ() + 0.5D);
						shadow.fallDistance = 0.0F;
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Shared last-resort recovery for a combat goal that has exhausted local
	 * repathing. The existing owner-progress checks remain authoritative, so this
	 * cannot teleport a shadow through a wall the owner has not passed.
	 */
	public static boolean tryRecoverStuckShadowNearOwner(Mob shadow,
			LivingEntity objective) {
		Player owner = getShadowOwnerPlayer(shadow);
		return owner instanceof ServerPlayer serverOwner
				&& objective != null
				&& tryRecallShadowNearOwner(shadow, serverOwner, objective);
	}

	private static boolean isSafeShadowRecoveryPosition(ServerLevel level,
			Mob shadow, BlockPos position) {
		if (!level.hasChunkAt(position)
				|| !level.getWorldBorder().isWithinBounds(position))
			return false;
		BlockPos floorPos = position.below();
		BlockState floor = level.getBlockState(floorPos);
		if (!floor.isFaceSturdy(level, floorPos, Direction.UP)
				|| !floor.getFluidState().isEmpty())
			return false;
		Vec3 destination = Vec3.atBottomCenterOf(position);
		AABB moved = shadow.getBoundingBox().move(
				destination.subtract(shadow.position()));
		return level.noCollision(shadow, moved);
	}

	private static long clearRepathDelay(Mob shadow) {
		return CLEAR_REPATH_INTERVAL_TICKS + Math.floorMod(shadow.getId(), 10);
	}

	private static void failClearDungeonTarget(ClearDungeonState state, Mob shadow, LivingEntity target, long now) {
		state.failedUntil.put(new ClearTargetKey(shadow.getUUID(), target.getUUID()),
				now + CLEAR_FAILED_TARGET_COOLDOWN_TICKS);
		state.progress.remove(shadow.getUUID());
		if (shadow.getTarget() == target)
			shadow.setTarget(null);
		shadow.getNavigation().stop();
	}

	private static boolean isFailedTarget(ClearDungeonState state, Mob shadow, LivingEntity target, long now) {
		return state.failedUntil.getOrDefault(new ClearTargetKey(shadow.getUUID(), target.getUUID()), 0L) > now;
	}

	private static List<LivingEntity> collectClearDungeonCandidates(ServerPlayer owner, List<Mob> shadows,
			DungeonTargetContext context) {
		ServerLevel level = owner.serverLevel();
		Comparator<LivingEntity> nearestFirst = Comparator.comparingDouble(candidate -> distanceToShadowGroup(candidate, shadows));
		PriorityQueue<LivingEntity> nearest = new PriorityQueue<>(CLEAR_MAX_CANDIDATES, nearestFirst.reversed());
		Set<UUID> seen = new HashSet<>();
		if (context.instanceId() != null) {
			DungeonInstanceSavedData.get(level).getInstance(context.instanceId()).ifPresent(instance -> {
				if (!instance.dimension().equals(level.dimension())
						|| !instance.participants().isEmpty() && !instance.participants().contains(owner.getUUID()))
					return;
				for (DungeonInstanceSavedData.EncounterState encounter : instance.encounters()) {
					if (!encounter.activated() || encounter.completed())
						continue;
					for (UUID mobId : encounter.trackedMobs()) {
						Entity entity = level.getEntity(mobId);
						if (entity instanceof LivingEntity candidate && seen.add(candidate.getUUID())
								&& isValidClearDungeonTarget(candidate, shadows.get(0), owner))
							offerBoundedCandidate(nearest, candidate, nearestFirst);
					}
				}
			});
		} else {
			// Legacy/fixed dungeons do not expose encounter UUIDs. Scan the loaded
			// entity set once for the whole group and retain only the closest
			// bounded set from the matching gate tag, DKC floor, or private level.
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof LivingEntity candidate && seen.add(candidate.getUUID())
						&& isValidClearDungeonTarget(candidate, shadows.get(0), owner))
					offerBoundedCandidate(nearest, candidate, nearestFirst);
			}
		}
		ArrayList<LivingEntity> result = new ArrayList<>(nearest);
		result.sort(nearestFirst);
		return result;
	}

	private static void offerBoundedCandidate(PriorityQueue<LivingEntity> candidates,
			LivingEntity candidate, Comparator<LivingEntity> nearestFirst) {
		if (candidates.size() < CLEAR_MAX_CANDIDATES) {
			candidates.offer(candidate);
			return;
		}
		LivingEntity farthest = candidates.peek();
		if (farthest != null && nearestFirst.compare(candidate, farthest) < 0) {
			candidates.poll();
			candidates.offer(candidate);
		}
	}

	private static double distanceToShadowGroup(LivingEntity candidate, List<Mob> shadows) {
		double closest = Double.MAX_VALUE;
		for (Mob shadow : shadows)
			closest = Math.min(closest, candidate.distanceToSqr(shadow));
		return closest;
	}

	private static DungeonTargetContext dungeonTargetContext(ServerPlayer owner) {
		CompoundTag data = owner.getPersistentData();
		String instanceText = data.getString(DungeonMobLevelAdapter.INSTANCE_TAG).trim();
		UUID instanceId = parseUuid(instanceText);
		String legacyTag = data.getString(DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG);
		boolean strictLegacyTag = data.getBoolean(PROCEDURAL_DUNGEON_TAG);
		int dkcFloor = DkcFloorRegistry.isSharedDkc(owner.level()) ? DkcSpatialLayout.floor(owner) : 0;
		String key = owner.level().dimension().location() + "|" + (instanceId == null ? "" : instanceId)
				+ "|" + legacyTag + "|" + strictLegacyTag + "|" + dkcFloor;
		return new DungeonTargetContext(key, instanceId, legacyTag, strictLegacyTag, dkcFloor);
	}

	private static boolean matchesDungeonContext(LivingEntity target, ServerPlayer owner,
			DungeonTargetContext context) {
		if (target.level() != owner.level())
			return false;
		if (context.instanceId() != null)
			return context.instanceId().toString().equals(
					target.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG));
		if (context.dkcFloor() > 0)
			return DkcSpatialLayout.isEntityInOwnedFloor(target, owner.getUUID(), context.dkcFloor());
		if (!context.legacyTag().isEmpty()) {
			String targetTag = target.getPersistentData().getString(DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG);
			// Generated procedural runs tag every encounter and must match
			// exactly. Older fixed dungeon dimensions predate mob tagging, so
			// untagged mobs remain eligible while another run's explicit tag does
			// not.
			return context.legacyTag().equals(targetTag)
					|| !context.strictLegacyTag() && targetTag.isEmpty();
		}
		return target.level().dimension().equals(owner.level().dimension());
	}

	private static UUID parseUuid(String value) {
		if (value == null || value.isBlank())
			return null;
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private static List<Mob> summonedOwnedMobs(ServerPlayer owner) {
		ArrayList<Mob> result = new ArrayList<>();
		ListTag roster = shadows(owner);
		ServerLevel level = owner.serverLevel();
		for (int index = 0; index < roster.size(); index++) {
			CompoundTag shadow = roster.getCompound(index);
			if (!shadow.hasUUID("summoned"))
				continue;
			Entity entity = level.getEntity(shadow.getUUID("summoned"));
			if (entity instanceof Mob mob && mob.isAlive())
				result.add(mob);
		}
		return result;
	}

	private static void cleanupClearDungeonStates(long now) {
		if (now % 1200L != 0L)
			return;
		CLEAR_DUNGEON_STATES.entrySet().removeIf(entry ->
				entry.getValue().lastSeenTick > now || now - entry.getValue().lastSeenTick > 1200L);
	}

	private record DungeonTargetContext(String key, UUID instanceId, String legacyTag,
			boolean strictLegacyTag, int dkcFloor) {
	}

	private record ClearTargetKey(UUID shadowId, UUID targetId) {
	}

	private static final class PathAttemptBudget {
		private int used;

		private boolean tryUse() {
			if (used >= CLEAR_MAX_PATH_ATTEMPTS_PER_TICK)
				return false;
			used++;
			return true;
		}
	}

	private static final class ClearShadowProgress {
		private UUID targetId;
		private double lastDistance;
		private float lastTargetHealth;
		private long lastProgressTick;
		private long nextRepathTick;
		private boolean traversing;
		private Vec3 lastShadowPosition = Vec3.ZERO;

		private void reset(LivingEntity target, Mob shadow, double distance, long now) {
			targetId = target.getUUID();
			lastDistance = distance;
			lastTargetHealth = target.getHealth();
			lastProgressTick = now;
			nextRepathTick = now + clearRepathDelay(shadow);
			traversing = false;
			lastShadowPosition = shadow.position();
		}

		private void beginTraversal(LivingEntity target, Mob shadow,
				double distance, long now) {
			boolean newObjective = !target.getUUID().equals(targetId)
					|| !traversing;
			targetId = target.getUUID();
			lastDistance = distance;
			lastTargetHealth = target.getHealth();
			traversing = true;
			if (newObjective) {
				lastProgressTick = now;
				lastShadowPosition = shadow.position();
			}
			nextRepathTick = now + CLEAR_ADVANCE_REPATH_TICKS;
		}
	}

	private static final class ClearDungeonState {
		private String contextKey = "";
		private long nextCandidateScanTick;
		private long lastSeenTick;
		private List<UUID> candidateIds = List.of();
		private final Map<ClearTargetKey, Long> failedUntil = new HashMap<>();
		private final Map<UUID, ClearShadowProgress> progress = new HashMap<>();

		private void reset(String contextKey, long now) {
			this.contextKey = contextKey;
			nextCandidateScanTick = now;
			candidateIds = List.of();
			failedUntil.clear();
			progress.clear();
		}
	}

	private static String normalizeCommand(String command) {
		if (command == null)
			return "";
		String value = command.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
		return switch (value) {
			case COMMAND_DEFAULT, COMMAND_PROTECT, COMMAND_BERSERK, COMMAND_FOLLOW, COMMAND_CLEAR_DUNGEON -> value;
			default -> "";
		};
	}

	private static String commandOrDefault(String command) {
		String normalized = normalizeCommand(command);
		return normalized.isEmpty() ? COMMAND_DEFAULT : normalized;
	}

	private static String commandDisplayName(String command) {
		return switch (command) {
			case COMMAND_DEFAULT -> "Default";
			case COMMAND_PROTECT -> "Protect";
			case COMMAND_BERSERK -> "Berserk";
			case COMMAND_FOLLOW -> "Follow";
			case COMMAND_CLEAR_DUNGEON -> "Clear Dungeon";
			default -> "Unknown";
		};
	}

	private static void applyLevelStats(Entity entity, CompoundTag shadow, boolean restoreSavedBossHealth) {
		if (!(entity instanceof LivingEntity living))
			return;
		int level = Math.max(1, shadow.getInt("level"));
		int rank = rankOf(shadow);
		String type = shadow.getString("type");
		if (living.getAttribute(Attributes.MAX_HEALTH) != null) {
			CompoundTag data = living.getPersistentData();
			if (!data.contains(BASE_HEALTH))
				data.putDouble(BASE_HEALTH, living.getAttribute(Attributes.MAX_HEALTH).getBaseValue());
			double base = data.getDouble(BASE_HEALTH);
			living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(base + (level - 1) * healthGain(type));
			if (isBoss(type))
				applyBossHealth(living, shadow, restoreSavedBossHealth);
			else
				living.setHealth(living.getMaxHealth());
		}
		if (living.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
			CompoundTag data = living.getPersistentData();
			if (!data.contains(BASE_ATTACK))
				data.putDouble(BASE_ATTACK, living.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
			double base = data.getDouble(BASE_ATTACK);
			living.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(base + (level - 1) * attackGain(type));
		}
		entity.getPersistentData().putInt(APPLIED_LEVEL, level);
		entity.getPersistentData().putInt(APPLIED_RANK, rank);
		entity.setCustomName(Component.literal(shadow.getString("name") + " [" + rankDisplayName(rank) + "] Lv." + level));
		maintainMarshalDomainBoost(living, rank);
	}

	/**
	 * Marshal is the first rank whose own power permanently manifests the
	 * Monarch's Domain. Renewing the normal domain package here keeps the bonus
	 * authoritative on the summoned entity without relying on the player's
	 * temporary Domain cast or its legacy capability flag.
	 */
	private static void maintainMarshalDomainBoost(LivingEntity shadow,
			int rank) {
		if (shadow.level().isClientSide())
			return;
		CompoundTag data = shadow.getPersistentData();
		if (rank < RANK_MARSHAL) {
			if (data.getBoolean(INTRINSIC_MARSHAL_DOMAIN))
				clearIntrinsicMarshalDomainBoost(shadow);
			return;
		}
		maintainMarshalDomainEffect(shadow, MobEffects.DAMAGE_BOOST);
		maintainMarshalDomainEffect(shadow, MobEffects.MOVEMENT_SPEED);
		maintainMarshalDomainEffect(shadow, MobEffects.DAMAGE_RESISTANCE);
		maintainMarshalDomainEffect(shadow,
				SololevelingModMobEffects.DOMAIN_BOOST);
		data.putBoolean(INTRINSIC_MARSHAL_DOMAIN, true);
	}

	private static void maintainMarshalDomainEffect(LivingEntity shadow,
			Holder<MobEffect> effect) {
		MobEffectInstance active = shadow.getEffect(effect);
		boolean temporaryDomainActive = shadow.getPersistentData()
				.getLong(TEMPORARY_DOMAIN_UNTIL) > shadow.level().getGameTime();
		if (active != null && temporaryDomainActive)
			return;
		if (active != null
				&& active.getAmplifier() >= MARSHAL_DOMAIN_AMPLIFIER
				&& active.getDuration() > MARSHAL_DOMAIN_REFRESH_THRESHOLD_TICKS)
			return;
		shadow.addEffect(new MobEffectInstance(effect,
				MARSHAL_DOMAIN_DURATION_TICKS, MARSHAL_DOMAIN_AMPLIFIER,
				false, false));
	}

	private static void clearIntrinsicMarshalDomainBoost(
			LivingEntity shadow) {
		CompoundTag data = shadow.getPersistentData();
		boolean temporaryDomainActive = data.getLong(TEMPORARY_DOMAIN_UNTIL)
				> shadow.level().getGameTime();
		if (!temporaryDomainActive) {
			removeIntrinsicMarshalDomainEffect(shadow,
					MobEffects.DAMAGE_BOOST);
			removeIntrinsicMarshalDomainEffect(shadow,
					MobEffects.MOVEMENT_SPEED);
			removeIntrinsicMarshalDomainEffect(shadow,
					MobEffects.DAMAGE_RESISTANCE);
			removeIntrinsicMarshalDomainEffect(shadow,
					SololevelingModMobEffects.DOMAIN_BOOST);
		}
		data.remove(INTRINSIC_MARSHAL_DOMAIN);
	}

	private static void removeIntrinsicMarshalDomainEffect(
			LivingEntity shadow, Holder<MobEffect> effect) {
		MobEffectInstance active = shadow.getEffect(effect);
		if (active != null
				&& active.getAmplifier() == MARSHAL_DOMAIN_AMPLIFIER
				&& active.getDuration() <= MARSHAL_DOMAIN_DURATION_TICKS)
			shadow.removeEffect(effect);
	}

	/**
	 * Records the explicit player-cast Domain window separately from permanent
	 * rank upkeep. This allows a later admin rank correction to remove only the
	 * intrinsic Marshal effects without deleting the active cast.
	 */
	public static void markTemporaryDomainBoost(Entity shadow,
			int durationTicks) {
		if (shadow == null || shadow.level().isClientSide()
				|| durationTicks <= 0)
			return;
		CompoundTag data = shadow.getPersistentData();
		long until = shadow.level().getGameTime() + durationTicks;
		data.putLong(TEMPORARY_DOMAIN_UNTIL,
				Math.max(data.getLong(TEMPORARY_DOMAIN_UNTIL), until));
	}

	private static void applyLevelStatsPreservingHealth(Entity entity, CompoundTag shadow) {
		if (!(entity instanceof LivingEntity living))
			return;
		float currentHealth = living.getHealth();
		applyLevelStats(entity, shadow, false);
		living.setHealth(Math.max(1.0F, Math.min(currentHealth, living.getMaxHealth())));
	}

	private static void applyBossHealth(LivingEntity living, CompoundTag shadow, boolean restoreSavedHealth) {
		float maxHealth = living.getMaxHealth();
		if (!restoreSavedHealth) {
			living.setHealth(Math.max(1.0F, Math.min(living.getHealth(), maxHealth)));
			return;
		}
		if (!shadow.contains(SAVED_HEALTH)) {
			living.setHealth(maxHealth);
			return;
		}
		double savedHealth = shadow.getDouble(SAVED_HEALTH);
		long savedAt = shadow.getLong(SAVED_HEALTH_AT);
		long now = living.level().getGameTime();
		double regenerated = savedHealth + Math.max(0L, now - savedAt) / 20.0D;
		living.setHealth((float) Math.max(1.0D, Math.min(maxHealth, regenerated)));
		shadow.putDouble(SAVED_HEALTH, living.getHealth());
		shadow.putLong(SAVED_HEALTH_AT, now);
	}

	private static void addStackToShadowInventory(Entity shadowEntity, ItemStack stack) {
		if (!isCollectibleManaStone(stack))
			return;
		CompoundTag data = shadowEntity.getPersistentData();
		ListTag inventory = data.getList(SHADOW_INVENTORY, Tag.TAG_COMPOUND);
		int remaining = stack.getCount();
		for (int i = 0; i < inventory.size() && remaining > 0; i++) {
			ItemStack stored = ItemStackData.load(inventory.getCompound(i), shadowEntity.registryAccess());
			if (!ItemStack.isSameItemSameComponents(stored, stack) || stored.getCount() >= stored.getMaxStackSize())
				continue;
			int move = Math.min(remaining, stored.getMaxStackSize() - stored.getCount());
			stored.grow(move);
			remaining -= move;
			inventory.set(i, ItemStackData.save(stored, shadowEntity.registryAccess()));
		}
		while (remaining > 0) {
			ItemStack stored = stack.copy();
			stored.setCount(Math.min(remaining, stored.getMaxStackSize()));
			remaining -= stored.getCount();
			inventory.add(ItemStackData.save(stored, shadowEntity.registryAccess()));
		}
		data.put(SHADOW_INVENTORY, inventory);
	}

	private static boolean isCollectibleManaStone(ItemStack stack) {
		return stack != null && !stack.isEmpty() && isCollectibleManaStone(stack.getItem());
	}

	private static boolean isCollectibleManaStone(Item item) {
		return item == SololevelingModItems.MANA_CRYSTAL_E.get()
				|| item == SololevelingModItems.MANA_CRYSTAL_D.get()
				|| item == SololevelingModItems.MANA_CRYSTAL_C.get()
				|| item == SololevelingModItems.MANA_CRYSTAL_B.get()
				|| item == SololevelingModItems.MANA_CRYSTAL_A.get();
	}

	private static CompoundTag firstSummonableShadow(Player player, String type) {
		CompoundTag available = firstAvailableShadow(player, type);
		if (available != null)
			return available;
		CompoundTag bestActive = null;
		for (CompoundTag shadow : ownedRosterWithinLimit(player, type)) {
			if (!shadow.hasUUID("summoned"))
				continue;
			Entity existing = findSummonedEntity(player, shadow.getUUID("summoned"));
			if (existing != null && existing.isAlive() && isBetterShadow(shadow, bestActive))
				bestActive = shadow;
		}
		return bestActive;
	}

	private static CompoundTag firstAvailableShadow(Player player, String type) {
		CompoundTag best = null;
		for (CompoundTag shadow : ownedRosterWithinLimit(player, type)) {
			boolean available = !shadow.hasUUID("summoned");
			if (!available) {
				Entity existing = findSummonedEntity(player, shadow.getUUID("summoned"));
				available = existing == null || !existing.isAlive();
				if (available)
					shadow.remove("summoned");
			}
			if (available && isBetterShadow(shadow, best))
				best = shadow;
		}
		if (best != null)
			return best;
		int max = legacyMax(player, type);
		int count = countOwned(player, type);
		if (count < max)
			return createShadow(player, type, count + 1);
		return null;
	}

	private static List<CompoundTag> ownedRosterWithinLimit(Player player, String type) {
		int limit = Math.max(0, legacyMax(player, type));
		if (limit == 0)
			return List.of();
		ArrayList<CompoundTag> matching = new ArrayList<>();
		ListTag roster = shadows(player);
		for (int i = 0; i < roster.size(); i++) {
			CompoundTag shadow = roster.getCompound(i);
			if (type.equals(shadow.getString("type")))
				matching.add(shadow);
		}
		matching.sort(ShadowMonarchManager::compareStrongestFirst);
		if (matching.size() > limit)
			return new ArrayList<>(matching.subList(0, limit));
		return matching;
	}

	private static int compareStrongestFirst(CompoundTag first, CompoundTag second) {
		int byRank = Integer.compare(rankOf(second), rankOf(first));
		if (byRank != 0)
			return byRank;
		int byLevel = Integer.compare(Math.max(1, second.getInt("level")), Math.max(1, first.getInt("level")));
		if (byLevel != 0)
			return byLevel;
		int byXp = Integer.compare(second.getInt("xp"), first.getInt("xp"));
		if (byXp != 0)
			return byXp;
		return first.getString("id").compareTo(second.getString("id"));
	}

	private static void enforceSummonedLimit(ServerPlayer player, String type) {
		List<CompoundTag> allowed = ownedRosterWithinLimit(player, type);
		ArrayList<String> allowedIds = new ArrayList<>(allowed.size());
		for (CompoundTag shadow : allowed)
			allowedIds.add(shadow.getString("id"));
		int removed = 0;
		ListTag roster = shadows(player);
		for (int i = 0; i < roster.size(); i++) {
			CompoundTag shadow = roster.getCompound(i);
			if (!type.equals(shadow.getString("type")) || allowedIds.contains(shadow.getString("id")) || !shadow.hasUUID("summoned"))
				continue;
			Entity existing = findSummonedEntity(player, shadow.getUUID("summoned"));
			if (existing != null) {
				// Defer over-limit cleanup until the prison is broken. Removing it here
				// would silently provide the same escape as a manual dismissal.
				if (SilladIcePrisonManager.isImprisoned(existing))
					continue;
				dropStoredShadowInventory(existing);
				existing.discard();
				removed++;
			}
			shadow.remove("summoned");
		}
		if (removed > 0)
			updateLegacySpawnCounter(player, type, -removed);
		player.getPersistentData().put(ROOT, root(player));
	}

	private static boolean isBetterShadow(CompoundTag candidate, CompoundTag current) {
		if (current == null)
			return true;
		int candidateRank = rankOf(candidate);
		int currentRank = rankOf(current);
		if (candidateRank != currentRank)
			return candidateRank > currentRank;
		int candidateLevel = Math.max(1, candidate.getInt("level"));
		int currentLevel = Math.max(1, current.getInt("level"));
		if (candidateLevel != currentLevel)
			return candidateLevel > currentLevel;
		int candidateXp = candidate.getInt("xp");
		int currentXp = current.getInt("xp");
		if (candidateXp != currentXp)
			return candidateXp > currentXp;
		return candidate.getString("id").compareTo(current.getString("id")) < 0;
	}

	private static CompoundTag createShadow(Player player, String type, int number) {
		CompoundTag shadow = new CompoundTag();
		shadow.putString("id", UUID.randomUUID().toString());
		shadow.putString("type", type);
		shadow.putString("name", defaultName(type, number));
		shadow.putInt("level", 1);
		shadow.putInt("xp", 0);
		shadow.putInt(STARTING_RANK, startingRank(type));
		shadow.putInt(RANK, startingRank(type));
		shadow.putBoolean("boss", isBoss(type));
		shadows(player).add(shadow);
		return shadow;
	}

	private static void ensureRoster(Player player) {
		if (!player.getPersistentData().contains(ROOT, Tag.TAG_COMPOUND))
			player.getPersistentData().put(ROOT, new CompoundTag());
		for (String type : shadowTypes()) {
			int max = legacyMax(player, type);
			while (countOwned(player, type) < max)
				createShadow(player, type, countOwned(player, type) + 1);
		}
		CompoundTag root = root(player);
		if (root.getInt(RANK_SCHEMA) != RANK_SCHEMA_VERSION)
			migrateShadowRanks(player, root);
		else
			repairGrandMarshalClaim(player, root);
		int levelCap = shadowLevelCap(player);
		if (root.getInt(CACHED_LEVEL_CAP) != levelCap) {
			ListTag roster = shadows(player);
			for (int i = 0; i < roster.size(); i++)
				normalizeShadowProgress(player, roster.getCompound(i));
			root.putInt(CACHED_LEVEL_CAP, levelCap);
			player.getPersistentData().put(ROOT, root);
		}
	}

	private static void migrateShadowRanks(Player player, CompoundTag root) {
		int previousSchema = root.getInt(RANK_SCHEMA);
		ListTag roster = shadows(player);
		for (int i = 0; i < roster.size(); i++) {
			CompoundTag shadow = roster.getCompound(i);
			String type = shadow.getString("type");
			int starting = startingRank(type);
			shadow.putInt(STARTING_RANK, starting);
			if (previousSchema < 2) {
				shadow.putInt(RANK, automaticRankForLevel(type,
						Math.max(1, shadow.getInt("level"))));
			} else if (previousSchema < 3 && "iron".equals(type)) {
				int current = shadow.contains(RANK, Tag.TAG_INT)
						? shadow.getInt(RANK) : starting;
				shadow.putInt(RANK, Math.min(RANK_MARSHAL,
						Math.max(current, automaticRankForLevel(type,
								Math.max(1, shadow.getInt("level"))))));
			}
		}
		if (previousSchema < 2) {
			// Schema 1 appointed a Grand Marshal automatically. Schema 2 makes the
			// promotion an explicit player choice, so legacy automatic claims return
			// to Marshal until the player appoints one in the summon screen.
			root.remove(GRAND_MARSHAL_ID);
		}
		root.putInt(RANK_SCHEMA, RANK_SCHEMA_VERSION);
		player.getPersistentData().put(ROOT, root);
		repairGrandMarshalClaim(player, root);
	}

	private static void repairGrandMarshalClaim(Player player, CompoundTag root) {
		ListTag roster = shadows(player);
		String claimedId = root.getString(GRAND_MARSHAL_ID);
		CompoundTag claimed = claimedId.isEmpty() ? null : getShadow(player, claimedId);
		if (claimed == null || !isBoss(claimed.getString("type"))
				|| rankOf(claimed) != RANK_GRAND_MARSHAL
				|| !isGrandMarshalEligibleByLevel(claimed)) {
			claimed = null;
			root.remove(GRAND_MARSHAL_ID);
		}
		for (int i = 0; i < roster.size(); i++) {
			CompoundTag shadow = roster.getCompound(i);
			if (rankOf(shadow) != RANK_GRAND_MARSHAL)
				continue;
			if (claimed == null
					|| !claimed.getString("id").equals(shadow.getString("id"))) {
				shadow.putInt(RANK, automaticRankForLevel(
						shadow.getString("type"),
						Math.max(1, shadow.getInt("level"))));
			}
		}
	}

	private static boolean isBetterGrandMarshalCandidate(CompoundTag candidate, CompoundTag current) {
		if (current == null)
			return true;
		int byPower = Integer.compare(bossPower(candidate.getString("type")), bossPower(current.getString("type")));
		if (byPower != 0)
			return byPower > 0;
		int byLevel = Integer.compare(candidate.getInt("level"), current.getInt("level"));
		if (byLevel != 0)
			return byLevel > 0;
		return candidate.getInt("xp") > current.getInt("xp");
	}

	private static int bossPower(String type) {
		return switch (type) {
			case "kamish" -> 5;
			case "beru" -> 4;
			case "tusk" -> 3;
			case "kaisel" -> 2;
			case "igris" -> 1;
			default -> 0;
		};
	}

	private static int startingRank(String type) {
		return switch (type) {
			case "iron" -> RANK_ELITE;
			case "igris", "kaisel" -> RANK_KNIGHT;
			case "tusk" -> RANK_ELITE_KNIGHT;
			case "beru", "kamish" -> RANK_GENERAL;
			default -> RANK_NORMAL;
		};
	}

	private static int automaticRankForLevel(String type, int level) {
		int desired = startingRank(type) + Math.max(0, level) / 10;
		return Math.min(desired, automaticRankCap(type));
	}

	private static boolean isGrandMarshalEligibleByLevel(CompoundTag shadow) {
		if (shadow == null || !isBoss(shadow.getString("type")))
			return false;
		return Math.max(1, shadow.getInt("level"))
				>= grandMarshalRequiredLevel(shadow.getString("type"));
	}

	private static boolean isGrandMarshalEligible(CompoundTag shadow) {
		return shadow != null && rankOf(shadow) == RANK_MARSHAL
				&& isGrandMarshalEligibleByLevel(shadow);
	}

	private static boolean isClaimedGrandMarshal(Player player,
			CompoundTag shadow) {
		if (player == null || shadow == null
				|| rankOf(shadow) != RANK_GRAND_MARSHAL)
			return false;
		return shadow.getString("id")
				.equals(root(player).getString(GRAND_MARSHAL_ID));
	}

	private static void recalculateRankAfterAdminLevel(CompoundTag ownerRoot,
			CompoundTag shadow) {
		String id = shadow.getString("id");
		boolean assigned = id.equals(ownerRoot.getString(GRAND_MARSHAL_ID));
		int level = Math.max(1, shadow.getInt("level"));
		String type = shadow.getString("type");
		shadow.putInt(STARTING_RANK, startingRank(type));
		if (assigned && isBoss(type)
				&& level >= grandMarshalRequiredLevel(type)) {
			shadow.putInt(RANK, RANK_GRAND_MARSHAL);
			return;
		}
		shadow.putInt(RANK, automaticRankForLevel(type, level));
		if (assigned)
			ownerRoot.remove(GRAND_MARSHAL_ID);
	}

	private static void refreshSummonedShadowRank(Player owner,
			CompoundTag shadow) {
		if (owner == null || shadow == null || !shadow.hasUUID("summoned"))
			return;
		Entity summoned = findSummonedEntity(owner,
				shadow.getUUID("summoned"));
		if (summoned != null && summoned.isAlive())
			applyLevelStatsPreservingHealth(summoned, shadow);
	}

	private static int rankOf(CompoundTag shadow) {
		if (shadow == null)
			return RANK_NORMAL;
		String type = shadow.getString("type");
		int starting = shadow.contains(STARTING_RANK, Tag.TAG_INT) ? shadow.getInt(STARTING_RANK) : startingRank(type);
		int rank = shadow.contains(RANK, Tag.TAG_INT) ? shadow.getInt(RANK) : starting;
		int maximum = maximumRank(type);
		return Math.max(starting, Math.min(maximum, rank));
	}

	private static boolean promoteShadow(Player owner, CompoundTag shadow, boolean showPopup) {
		if (owner == null || shadow == null)
			return false;
		String type = shadow.getString("type");
		int oldRank = rankOf(shadow);
		int newRank;
		CompoundTag ownerRoot = root(owner);
		if (!isMarshalProgressionType(type)) {
			if (oldRank >= RANK_ELITE_KNIGHT)
				return false;
			newRank = oldRank + 1;
		} else if (oldRank < RANK_MARSHAL) {
			newRank = oldRank + 1;
		} else {
			return false;
		}
		shadow.putInt(STARTING_RANK, startingRank(type));
		shadow.putInt(RANK, newRank);
		owner.getPersistentData().put(ROOT, ownerRoot);
		if (showPopup && owner instanceof ServerPlayer serverPlayer) {
			Component title = Component.literal("SHADOW RANK UP").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
			Component under = Component.literal(shadow.getString("name") + "\n")
					.withStyle(ChatFormatting.LIGHT_PURPLE)
					.append(Component.literal(rankDisplayName(oldRank) + " -> " + rankDisplayName(newRank)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			SystemNotifications.showTitleUnder(serverPlayer, rankColor(newRank), 100, title, under);
		}
		return true;
	}

	/**
	 * Shadows begin with a level cap of 10. Starting after player level 40, every
	 * additional 20 player levels unlock another 10 shadow levels.
	 */
	public static int shadowLevelCap(Player player) {
		if (player == null)
			return BASE_SHADOW_LEVEL_CAP;
		double playerLevel = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(variables -> variables.Level).orElse(0.0D);
		if (!Double.isFinite(playerLevel) || playerLevel <= PLAYER_LEVEL_CAP_START)
			return BASE_SHADOW_LEVEL_CAP;
		double rawTiers = Math.floor((playerLevel - PLAYER_LEVEL_CAP_START) / PLAYER_LEVELS_PER_CAP_INCREASE);
		long maximumTiers = (MAX_SAFE_SHADOW_LEVEL - BASE_SHADOW_LEVEL_CAP) / SHADOW_LEVELS_PER_CAP_INCREASE;
		long tiers = (long) Math.min(maximumTiers, Math.max(0.0D, rawTiers));
		return BASE_SHADOW_LEVEL_CAP + (int) tiers * SHADOW_LEVELS_PER_CAP_INCREASE;
	}

	private static int effectiveShadowLevelCap(Player player,
			CompoundTag shadow) {
		int normalCap = shadowLevelCap(player);
		int adminFloor = shadow == null ? 0
				: Math.max(0, shadow.getInt(ADMIN_LEVEL_FLOOR));
		return Math.max(normalCap,
				Math.min(MAX_ADMIN_SHADOW_LEVEL, adminFloor));
	}

	private static boolean normalizeShadowProgress(Player player, CompoundTag shadow) {
		boolean overrideChanged = shadow.contains(ADMIN_LEVEL_FLOOR,
				Tag.TAG_INT)
				&& shadow.getInt(ADMIN_LEVEL_FLOOR) <= shadowLevelCap(player);
		if (overrideChanged)
			shadow.remove(ADMIN_LEVEL_FLOOR);
		int cap = effectiveShadowLevelCap(player, shadow);
		int originalLevel = shadow.getInt("level");
		int originalXp = shadow.getInt("xp");
		int level = Math.max(1, Math.min(cap, originalLevel));
		int xp = Math.max(0, originalXp);
		String type = shadow.getString("type");
		while (level < cap && xp >= xpNeeded(level, type)) {
			xp -= xpNeeded(level, type);
			level++;
			if (level % 10 == 0)
				promoteShadow(player, shadow, false);
		}
		if (level >= cap)
			xp = Math.min(xp, xpNeeded(level, type) - 1);
		if (level == originalLevel && xp == originalXp)
			return overrideChanged;
		shadow.putInt("level", level);
		shadow.putInt("xp", xp);
		return true;
	}

	private static void synchronizeShadowLevel(Player owner, Entity shadowEntity) {
		String id = shadowEntity.getPersistentData().getString(SHADOW_ID);
		if (id.isEmpty())
			return;
		CompoundTag shadow = getShadow(owner, id);
		if (shadow == null)
			return;
		boolean changed = normalizeShadowProgress(owner, shadow);
		syncEquipmentTag(shadowEntity, shadow);
		int level = Math.max(1, shadow.getInt("level"));
		int rank = rankOf(shadow);
		if (shadowEntity.getPersistentData().getInt(APPLIED_LEVEL) != level
				|| shadowEntity.getPersistentData().getInt(APPLIED_RANK) != rank)
			applyLevelStatsPreservingHealth(shadowEntity, shadow);
		else if (shadowEntity instanceof LivingEntity living)
			maintainMarshalDomainBoost(living, rank);
		if (changed)
			owner.getPersistentData().put(ROOT, root(owner));
	}

	private static List<CompoundTag> summonedOwnedShadows(Player player) {
		ArrayList<CompoundTag> result = new ArrayList<>();
		if (!(player.level() instanceof ServerLevel level))
			return result;
		ListTag shadows = shadows(player);
		for (int i = 0; i < shadows.size(); i++) {
			CompoundTag shadow = shadows.getCompound(i);
			if (!shadow.hasUUID("summoned"))
				continue;
			Entity entity = level.getEntity(shadow.getUUID("summoned"));
			if (entity != null && entity.isAlive())
				result.add(shadow);
		}
		return result;
	}

	/**
	 * Recalls every summoned shadow. Used when a place forbids them outright --
	 * the Cartenon return is fought alone, without the army.
	 */
	public static void recallAllShadows(Player player) {
		if (player == null)
			return;
		absorbVisibleOwnedShadows(player);
	}

	private static void absorbVisibleOwnedShadows(Player player) {
		if (!(player.level() instanceof ServerLevel level))
			return;
		AABB area = player.getBoundingBox().inflate(96);
		for (TamableAnimal tame : level.getEntitiesOfClass(TamableAnimal.class, area, e -> e.getOwnerUUID() != null && e.getOwnerUUID().equals(player.getUUID()))) {
			if (!tame.getPersistentData().getString(SHADOW_ID).isEmpty())
				continue;
			String type = typeFromEntity(tame);
			if (type.isEmpty())
				continue;
			CompoundTag shadow = firstAvailableShadow(player, type);
			if (shadow == null)
				shadow = createShadow(player, type, countOwned(player, type) + 1);
			tagSummonedEntity(player, shadow, tame);
		}
		for (ShadowKaiselinEntity kaisel : level.getEntitiesOfClass(ShadowKaiselinEntity.class, area, e -> e.getOwnerUUID() != null && e.getOwnerUUID().equals(player.getUUID()))) {
			if (!kaisel.getPersistentData().getString(SHADOW_ID).isEmpty())
				continue;
			CompoundTag shadow = firstAvailableShadow(player, "kaisel");
			if (shadow == null)
				shadow = createShadow(player, "kaisel", countOwned(player, "kaisel") + 1);
			tagSummonedEntity(player, shadow, kaisel);
		}
	}

	private static Entity findSummonedEntity(Player player, UUID entityId) {
		if (player == null || entityId == null || !(player.level() instanceof ServerLevel level))
			return null;
		for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
			Entity entity = serverLevel.getEntity(entityId);
			if (entity != null)
				return entity;
		}
		return null;
	}

	private static Player findOnlineOwner(ServerLevel level, UUID ownerId) {
		if (level == null || ownerId == null)
			return null;
		return level.getServer().getPlayerList().getPlayer(ownerId);
	}

	private static boolean isCurrentSummonedInstance(Player owner, Entity entity) {
		if (owner == null || entity == null)
			return true;
		long ownerGeneration = owner.getPersistentData()
				.getLong(PLAYER_RESET_GENERATION);
		if (ownerGeneration != entity.getPersistentData()
				.getLong(SHADOW_GENERATION))
			return false;
		String id = entity.getPersistentData().getString(SHADOW_ID);
		if (id.isEmpty())
			return true;
		CompoundTag shadow = getShadow(owner, id);
		if (shadow == null)
			return true;
		return shadow.hasUUID("summoned") && shadow.getUUID("summoned").equals(entity.getUUID());
	}

	private static CompoundTag getShadow(Entity entity, String id) {
		if (entity == null || id == null)
			return null;
		ListTag shadows = shadows(entity);
		for (int i = 0; i < shadows.size(); i++) {
			CompoundTag shadow = shadows.getCompound(i);
			if (id.equals(shadow.getString("id")))
				return shadow;
		}
		return null;
	}

	private static CompoundTag strongestOwnedShadow(Player player, String type) {
		if (player == null || type == null || type.isEmpty())
			return null;
		CompoundTag best = null;
		for (CompoundTag shadow : ownedRosterWithinLimit(player, type)) {
			if (isBetterShadow(shadow, best))
				best = shadow;
		}
		return best;
	}

	private static ItemStack equipmentOf(CompoundTag shadow, HolderLookup.Provider registries) {
		if (shadow == null || !shadow.contains(EQUIPMENT, Tag.TAG_COMPOUND))
			return ItemStack.EMPTY;
		return ItemStackData.load(shadow.getCompound(EQUIPMENT), registries);
	}

	private static void syncEquipmentTag(Entity entity, CompoundTag shadow) {
		if (entity == null)
			return;
		ItemStack equipment = equipmentOf(shadow, entity.registryAccess());
		if (equipment.isEmpty())
			entity.getPersistentData().remove(SHADOW_EQUIPMENT);
		else
			entity.getPersistentData().putString(SHADOW_EQUIPMENT,
					BuiltInRegistries.ITEM.getKey(equipment.getItem()).toString());
	}

	private static void returnEquipmentToPlayer(Player player, CompoundTag shadow) {
		if (player == null)
			return;
		ItemStack equipment = equipmentOf(shadow, player.registryAccess());
		if (equipment.isEmpty())
			return;
		shadow.remove(EQUIPMENT);
		if (!player.getInventory().add(equipment))
			player.drop(equipment, false);
	}

	private static CompoundTag getFormation(Entity entity, String id) {
		if (entity == null || id == null)
			return null;
		ListTag formations = formations(entity);
		for (int i = 0; i < formations.size(); i++) {
			CompoundTag formation = formations.getCompound(i);
			if (id.equals(formation.getString("id")))
				return formation;
		}
		return null;
	}

	private static void removeFormationData(Entity entity, String id) {
		ListTag formations = formations(entity);
		for (int i = formations.size() - 1; i >= 0; i--) {
			if (id.equals(formations.getCompound(i).getString("id")))
				formations.remove(i);
		}
	}

	private static CompoundTag root(Entity entity) {
		CompoundTag data = entity.getPersistentData();
		if (!data.contains(ROOT, Tag.TAG_COMPOUND))
			data.put(ROOT, new CompoundTag());
		CompoundTag root = data.getCompound(ROOT);
		if (!root.contains(SHADOWS, Tag.TAG_LIST))
			root.put(SHADOWS, new ListTag());
		if (!root.contains(FORMATIONS, Tag.TAG_LIST))
			root.put(FORMATIONS, new ListTag());
		return root;
	}

	private static ListTag shadows(Entity entity) {
		return root(entity).getList(SHADOWS, Tag.TAG_COMPOUND);
	}

	// ── per-type outline colours ──────────────────────────────────────────────

	/** Every shadow type that can be given an outline colour, in roster order. */
	public static List<String> customizableTypes() {
		return List.of(shadowTypes());
	}

	/**
	 * Outline colour for a shadow type, or {@link #NO_GLOW} when the player has
	 * not assigned one. Stored per type rather than per roster entry so a whole
	 * species reads the same colour on the field.
	 */
	public static int glowColor(Player player, String requestedType) {
		if (player == null)
			return NO_GLOW;
		String type = normalizeShadowType(requestedType == null ? "" : requestedType);
		if (type.isEmpty())
			return NO_GLOW;
		CompoundTag colors = root(player).getCompound(GLOW_COLORS);
		return colors.contains(type, Tag.TAG_INT)
				? colors.getInt(type) & 0xFFFFFF : NO_GLOW;
	}

	/** Assigns or clears a type's outline colour. Pass {@link #NO_GLOW} to clear. */
	public static void setGlowColor(Player player, String requestedType, int rgb) {
		if (player == null)
			return;
		String type = normalizeShadowType(requestedType == null ? "" : requestedType);
		if (type.isEmpty())
			return;
		CompoundTag playerRoot = root(player);
		CompoundTag colors = playerRoot.getCompound(GLOW_COLORS);
		if (rgb == NO_GLOW)
			colors.remove(type);
		else
			colors.putInt(type, rgb & 0xFFFFFF);
		playerRoot.put(GLOW_COLORS, colors);
		player.getPersistentData().put(ROOT, playerRoot);
	}

	/** Live summoned shadows paired with their roster type, for outline syncing. */
	public static Map<UUID, String> summonedShadowTypes(ServerPlayer owner) {
		Map<UUID, String> result = new LinkedHashMap<>();
		if (owner == null)
			return result;
		ListTag roster = shadows(owner);
		ServerLevel level = owner.serverLevel();
		for (int index = 0; index < roster.size(); index++) {
			CompoundTag shadow = roster.getCompound(index);
			if (!shadow.hasUUID("summoned"))
				continue;
			UUID id = shadow.getUUID("summoned");
			Entity entity = level.getEntity(id);
			if (entity != null && entity.isAlive() && !entity.isRemoved())
				result.put(id, normalizeShadowType(shadow.getString("type")));
		}
		return result;
	}

	private static ListTag formations(Entity entity) {
		return root(entity).getList(FORMATIONS, Tag.TAG_COMPOUND);
	}

	private static int formationCount(Entity entity) {
		return formations(entity).size();
	}

	private static int countOwned(Player player, String type) {
		int count = 0;
		ListTag shadows = shadows(player);
		for (int i = 0; i < shadows.size(); i++) {
			if (type.equals(shadows.getCompound(i).getString("type")))
				count++;
		}
		return count;
	}

	private static void appendFormationSkill(Player player, String id, String name) {
		String skill = FORMATION_PREFIX + id + "|" + cleanFormationName(name, 1);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			String list = capability.Plist == null || capability.Plist.isEmpty() ? "." : capability.Plist;
			if (!list.contains(FORMATION_PREFIX + id)) {
				capability.Plist = list + skill + ",";
				capability.syncPlayerVariables(player);
			}
		});
	}

	private static String cleanFormationName(String value, int number) {
		String name = value == null ? "" : value.trim();
		if (name.isEmpty())
			return "Formation " + number;
		name = name.replace(',', ' ').replace('|', ' ');
		return name.length() > 24 ? name.substring(0, 24) : name;
	}

	private static List<String> parseSkillList(String plistOriginal) {
		ArrayList<String> result = new ArrayList<>();
		if (plistOriginal == null || plistOriginal.isEmpty())
			return result;
		for (String item : plistOriginal.split(",")) {
			String skill = item == null ? "" : item.trim();
			if (skill.startsWith("."))
				skill = skill.substring(1);
			if (!skill.isEmpty())
				result.add(skill);
		}
		return result;
	}

	private static String writeSkillList(List<String> skills) {
		if (skills == null || skills.isEmpty())
			return ".";
		StringBuilder builder = new StringBuilder(".");
		for (String skill : skills) {
			if (skill != null && !skill.isEmpty())
				builder.append(skill).append(",");
		}
		return builder.toString();
	}

	private static String formationIdFromSkill(String skill) {
		if (!isFormationSkill(skill))
			return "";
		String value = skill.substring(FORMATION_PREFIX.length());
		int split = value.indexOf('|');
		return split >= 0 ? value.substring(0, split) : value;
	}

	private static String formationNameFromSkill(String skill) {
		if (!isFormationSkill(skill))
			return "";
		int split = skill.indexOf('|');
		return split >= 0 && split + 1 < skill.length() ? skill.substring(split + 1) : "";
	}

	private static int xpNeeded(int level, String type) {
		long base = 35L + Math.max(1L, level) * 15L;
		double multiplier = switch (type) {
			case "iron" -> 1.4D;
			case "igris" -> 1.75D;
			case "kaisel" -> 2.0D;
			case "tusk" -> 2.25D;
			case "beru" -> 3.0D;
			case "kamish" -> 4.0D;
			case "high_orc" -> 1.2D;
			default -> 1.0D;
		};
		return Math.max(1, (int) Math.min(Integer.MAX_VALUE, Math.ceil(base * multiplier)));
	}

	private static double healthGain(String type) {
		return switch (type) {
			case "iron" -> 6.5;
			case "igris", "beru", "kamish", "tusk", "kaisel" -> 8.0;
			case "high_orc", "polar_bear" -> 5.0;
			case "knight", "orc", "wolf" -> 3.0;
			default -> 2.0;
		};
	}

	private static double attackGain(String type) {
		return switch (type) {
			case "iron" -> 0.85;
			case "igris", "beru", "kamish", "tusk", "kaisel" -> 1.25;
			case "high_orc", "polar_bear" -> 0.85;
			case "knight", "orc", "wolf" -> 0.55;
			default -> 0.4;
		};
	}

	private static String[] shadowTypes() {
		return new String[]{"goblin_club", "goblin_archer", "goblin_mage", "wolf", "knight", "polar_bear", "orc", "igris", "beru", "kamish", "high_orc", "tusk", "kaisel", "iron"};
	}

	private static EntityType<?> entityType(String type) {
		return switch (type) {
			case "goblin_club" -> SololevelingModEntities.GOBLIN_CLUB_SHADOW.get();
			case "goblin_archer" -> SololevelingModEntities.GOBLIN_ARCHER_SHADOW.get();
			case "goblin_mage" -> SololevelingModEntities.GOBLIN_MAGE_SHADOW.get();
			case "wolf" -> SololevelingModEntities.STEEL_FANG_WOLF_SHADOW.get();
			case "knight" -> SololevelingModEntities.SHADOW_SOLD_1.get();
			case "polar_bear" -> SololevelingModEntities.SHADOW_POLAR_BEAR.get();
			case "orc" -> SololevelingModEntities.SHADOW_GREEN_ORC.get();
			case "igris" -> SololevelingModEntities.IGRIS_SHADOW.get();
			case "beru" -> SololevelingModEntities.BERU_SHADOW.get();
			case "kamish" -> SololevelingModEntities.KAMISH_SHADOW.get();
			case "high_orc" -> SololevelingModEntities.SHADOW_HIGH_ORC.get();
			case "tusk" -> SololevelingModEntities.TUSK_SHADOW.get();
			case "kaisel" -> SololevelingModEntities.SHADOW_KAISELIN.get();
			case "iron" -> SololevelingModEntities.SHADOW_IRON.get();
			default -> null;
		};
	}

	private static String typeFromEntity(Entity entity) {
		if (entity instanceof GoblinClubShadowEntity)
			return "goblin_club";
		if (entity instanceof GoblinArcherShadowEntity)
			return "goblin_archer";
		if (entity instanceof GoblinMageShadowEntity)
			return "goblin_mage";
		if (entity instanceof SteelFangWolfShadowEntity)
			return "wolf";
		if (entity instanceof ShadowSold1Entity)
			return "knight";
		if (entity instanceof ShadowPolarBearEntity)
			return "polar_bear";
		if (entity instanceof ShadowGreenOrcEntity)
			return "orc";
		if (entity instanceof OrcShadowEntity)
			return "orc";
		if (entity instanceof IgrisShadowEntity)
			return "igris";
		if (entity instanceof BeruShadowEntity)
			return "beru";
		if (entity instanceof KamishShadowEntity)
			return "kamish";
		if (entity instanceof ShadowHighOrcEntity)
			return "high_orc";
		if (entity instanceof TuskShadowEntity)
			return "tusk";
		if (entity instanceof ShadowKaiselinEntity)
			return "kaisel";
		if (entity instanceof ShadowIronEntity)
			return "iron";
		return "";
	}

	private static String normalizeShadowType(String type) {
		String value = type.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
		return switch (value) {
			case "goblin", "goblin_fighter", "goblin_club" -> "goblin_club";
			case "archer", "goblin_archer" -> "goblin_archer";
			case "mage", "goblin_mage" -> "goblin_mage";
			case "lycan", "wolf" -> "wolf";
			case "soldier", "knight" -> "knight";
			case "bear", "polar", "polar_bear" -> "polar_bear";
			case "orc", "green_orc" -> "orc";
			case "highorc", "high_orc" -> "high_orc";
			case "igris" -> "igris";
			case "beru" -> "beru";
			case "kamish" -> "kamish";
			case "tusk" -> "tusk";
			case "kaisel", "kaiselin", "shadow_kaisel", "shadow_kaiselin" -> "kaisel";
			case "iron", "shadow_iron" -> "iron";
			default -> "";
		};
	}

	private static String defaultName(String type, int number) {
		return switch (type) {
			case "goblin_club" -> "Goblin Fighter " + number;
			case "goblin_archer" -> "Goblin Archer " + number;
			case "goblin_mage" -> "Goblin Mage " + number;
			case "wolf" -> "Lycan " + number;
			case "knight" -> "Knight " + number;
			case "polar_bear" -> "Polar Bear " + number;
			case "orc" -> "Orc " + number;
			case "igris" -> "Igris";
			case "beru" -> "Beru";
			case "kamish" -> "Kamish";
			case "high_orc" -> "High Orc " + number;
			case "tusk" -> "Tusk";
			case "kaisel" -> "Kaisel";
			case "iron" -> "Iron";
			default -> type.replace('_', ' ').toLowerCase(Locale.ROOT);
		};
	}

	private static boolean isBoss(String type) {
		return "igris".equals(type) || "beru".equals(type) || "kamish".equals(type) || "tusk".equals(type) || "kaisel".equals(type);
	}

	private static boolean isMarshalProgressionType(String type) {
		return isBoss(type) || "iron".equals(type);
	}

	private static boolean isHealingBossType(String type) {
		return isBoss(type) || "iron".equals(type);
	}

	private static int automaticRankCap(String type) {
		return isMarshalProgressionType(type) ? RANK_MARSHAL
				: RANK_ELITE_KNIGHT;
	}

	private static int maximumRank(String type) {
		if (isBoss(type))
			return RANK_GRAND_MARSHAL;
		return "iron".equals(type) ? RANK_MARSHAL
				: RANK_ELITE_KNIGHT;
	}

	private static boolean isDismissibleShadowType(String type) {
		return "goblin_club".equals(type) || "goblin_archer".equals(type) || "goblin_mage".equals(type) || "wolf".equals(type) || "knight".equals(type) || "polar_bear".equals(type) || "orc".equals(type) || "high_orc".equals(type);
	}

	private static int legacyMax(Player player, String type) {
		if ("iron".equals(type))
			return Math.max(0, Math.min(1, root(player).getInt(IRON_MAX)));
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		return switch (type) {
			case "goblin_club" -> (int) vars.GobShadowMax;
			case "goblin_archer" -> (int) vars.ShadowGoblinArcherMax;
			case "goblin_mage" -> (int) vars.ShadowGoblinMageMax;
			case "wolf" -> (int) vars.WolfShadowMax;
			case "knight" -> (int) vars.ordshadowmax;
			case "polar_bear" -> (int) vars.polarbearmax;
			case "orc" -> (int) vars.orcmax;
			case "igris" -> (int) vars.igris;
			case "beru" -> (int) vars.berumax;
			case "kamish" -> (int) vars.shadowdragonmax;
			case "high_orc" -> (int) vars.highorcmax;
			case "tusk" -> (int) vars.tuskmax;
			case "kaisel" -> (int) vars.Kaisel;
			default -> 0;
		};
	}

	private static int legacySpawned(Player player, String type) {
		if ("iron".equals(type))
			return Math.max(0, Math.min(1, root(player).getInt(IRON_SUMMONED)));
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		return switch (type) {
			case "goblin_club" -> (int) vars.GobShadow;
			case "goblin_archer" -> (int) vars.ShadowGoblinArcherAmount;
			case "goblin_mage" -> (int) vars.ShadowGoblinMageAmount;
			case "wolf" -> (int) vars.WolfShadow;
			case "knight" -> (int) vars.OrdShadow;
			case "polar_bear" -> (int) vars.polarbear;
			case "orc" -> (int) vars.orcspawned;
			case "igris" -> (int) vars.IgrisSpawned;
			case "beru" -> (int) vars.beru;
			case "kamish" -> (int) vars.shadowdragonnum;
			case "high_orc" -> (int) vars.highorcspawned;
			case "tusk" -> (int) vars.tuskspawned;
			case "kaisel" -> (int) vars.KaiselSpawned;
			default -> 0;
		};
	}

	private static void setLegacyMax(Player player, String type, int amount) {
		if ("iron".equals(type)) {
			CompoundTag playerRoot = root(player);
			playerRoot.putInt(IRON_MAX, Math.max(0, Math.min(1, amount)));
			player.getPersistentData().put(ROOT, playerRoot);
			return;
		}
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			switch (type) {
				case "goblin_club" -> capability.GobShadowMax = amount;
				case "goblin_archer" -> capability.ShadowGoblinArcherMax = amount;
				case "goblin_mage" -> capability.ShadowGoblinMageMax = amount;
				case "wolf" -> capability.WolfShadowMax = amount;
				case "knight" -> capability.ordshadowmax = amount;
				case "polar_bear" -> capability.polarbearmax = amount;
				case "orc" -> capability.orcmax = amount;
				case "igris" -> capability.igris = amount;
				case "beru" -> capability.berumax = amount;
				case "kamish" -> capability.shadowdragonmax = amount;
				case "high_orc" -> capability.highorcmax = amount;
				case "tusk" -> capability.tuskmax = amount;
				case "kaisel" -> capability.Kaisel = amount;
				default -> {
				}
			}
		});
	}

	private static void trimOwnedShadows(Player player, String type, int amount) {
		ListTag shadows = shadows(player);
		while (countOwned(player, type) > amount) {
			int removeIndex = weakestShadowIndex(shadows, type);
			if (removeIndex < 0)
				return;
			CompoundTag shadow = shadows.getCompound(removeIndex);
			if (shadow.hasUUID("summoned") && player.level() instanceof ServerLevel level) {
				Entity summoned = findSummonedEntity(player, shadow.getUUID("summoned"));
				if (summoned != null) {
					dropStoredShadowInventory(summoned);
					summoned.discard();
					updateLegacySpawnCounter(player, type, -1);
				}
			}
			returnEquipmentToPlayer(player, shadow);
			shadows.remove(removeIndex);
		}
	}

	private static int weakestShadowIndex(ListTag shadows, String type) {
		int weakestIndex = -1;
		CompoundTag weakest = null;
		for (int i = 0; i < shadows.size(); i++) {
			CompoundTag shadow = shadows.getCompound(i);
			if (!type.equals(shadow.getString("type")))
				continue;
			if (isWeakerShadow(shadow, weakest)) {
				weakest = shadow;
				weakestIndex = i;
			}
		}
		return weakestIndex;
	}

	private static boolean isWeakerShadow(CompoundTag candidate, CompoundTag current) {
		if (current == null)
			return true;
		int candidateRank = rankOf(candidate);
		int currentRank = rankOf(current);
		if (candidateRank != currentRank)
			return candidateRank < currentRank;
		int candidateLevel = Math.max(1, candidate.getInt("level"));
		int currentLevel = Math.max(1, current.getInt("level"));
		if (candidateLevel != currentLevel)
			return candidateLevel < currentLevel;
		int candidateXp = candidate.getInt("xp");
		int currentXp = current.getInt("xp");
		if (candidateXp != currentXp)
			return candidateXp < currentXp;
		return candidate.getString("id").compareTo(current.getString("id")) > 0;
	}

	private static void updateLegacySpawnCounter(Player player, String type, int amount) {
		if ("iron".equals(type)) {
			CompoundTag playerRoot = root(player);
			int current = playerRoot.getInt(IRON_SUMMONED);
			playerRoot.putInt(IRON_SUMMONED, Math.max(0, Math.min(1, current + amount)));
			player.getPersistentData().put(ROOT, playerRoot);
			return;
		}
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			switch (type) {
				case "goblin_club" -> capability.GobShadow = Math.max(0, capability.GobShadow + amount);
				case "goblin_archer" -> capability.ShadowGoblinArcherAmount = Math.max(0, capability.ShadowGoblinArcherAmount + amount);
				case "goblin_mage" -> capability.ShadowGoblinMageAmount = Math.max(0, capability.ShadowGoblinMageAmount + amount);
				case "wolf" -> capability.WolfShadow = Math.max(0, capability.WolfShadow + amount);
				case "knight" -> capability.OrdShadow = Math.max(0, capability.OrdShadow + amount);
				case "polar_bear" -> capability.polarbear = Math.max(0, capability.polarbear + amount);
				case "orc" -> capability.orcspawned = Math.max(0, capability.orcspawned + amount);
				case "igris" -> capability.IgrisSpawned = Math.max(0, capability.IgrisSpawned + amount);
				case "beru" -> capability.beru = Math.max(0, capability.beru + amount);
				case "kamish" -> capability.shadowdragonnum = Math.max(0, capability.shadowdragonnum + amount);
				case "high_orc" -> capability.highorcspawned = Math.max(0, capability.highorcspawned + amount);
				case "tusk" -> capability.tuskspawned = Math.max(0, capability.tuskspawned + amount);
				case "kaisel" -> capability.KaiselSpawned = Math.max(0, capability.KaiselSpawned + amount);
				default -> {
				}
			}
			capability.syncPlayerVariables(player);
		});
	}
}
