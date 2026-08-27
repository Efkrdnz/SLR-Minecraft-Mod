package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.entity.HunterEntity;
import net.solocraft.entity.SteelFangWolfEntity;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.network.SololevelingModVariables;

import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

import java.util.Set;

/** Runtime behavior for the difficulty and dungeon-death world creation rules. */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class SoloLevelingWorldRulesManager {
	private static final int STANDARD_PERCENT = 100;
	private static final int FORGIVING_DEATH_RULE = 1;
	private static final int HARSH_DEATH_RULE = 2;

	private static final String FORGIVING_SNAPSHOT = "slr_forgiving_death_snapshot";
	private static final String SNAPSHOT_INVENTORY = "Inventory";
	private static final String SNAPSHOT_SELECTED_SLOT = "SelectedSlot";
	private static final String SNAPSHOT_XP_LEVEL = "ExperienceLevel";
	private static final String SNAPSHOT_XP_TOTAL = "ExperienceTotal";
	private static final String SNAPSHOT_XP_PROGRESS = "ExperienceProgress";

	private static final ResourceLocation ENEMY_HEALTH_ID = modifierId("enemy_health");
	private static final ResourceLocation ENEMY_DAMAGE_ID = modifierId("enemy_damage");
	private static final ResourceLocation BOSS_HEALTH_ID = modifierId("boss_health");
	private static final ResourceLocation BOSS_DAMAGE_ID = modifierId("boss_damage");
	private static final ResourceLocation BOSS_ARMOR_ID = modifierId("boss_armor");
	private static final ResourceLocation BOSS_TOUGHNESS_ID = modifierId("boss_armor_toughness");
	private static final ResourceLocation BOSS_KNOCKBACK_ID = modifierId("boss_knockback_resistance");

	private static final TagKey<net.minecraft.world.entity.EntityType<?>> SOLO_BOSS_TAG =
			TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("minecraft", "soloboss"));
	private static final TagKey<net.minecraft.world.entity.EntityType<?>> SOLO_ENEMY_TAG =
			TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("minecraft", "dm"));

	private static final Set<String> NON_ENEMY_MONSTER_IDS = Set.of(
			"after_image",
			"after_image_1",
			"after_image_2",
			"arrow_splash",
			"attackshard",
			"detect_eye_inv",
			"fx_puddle",
			"fxspik",
			"ice_chunk",
			"icecle",
			"magic_eye",
			"secretary",
			"slasheffectsword",
			"sung_jin_woo",
			"thomas_andre",
			"training_bot");

	private SoloLevelingWorldRulesManager() {
	}

	/**
	 * Runs after legacy and runtime level scalers. Stable resource IDs make repeated joins
	 * and chunk reloads replace these modifiers instead of compounding them.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob))
			return;
		ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
		boolean runtimeDungeonMob = mob.getPersistentData().getBoolean(
				DungeonMobLevelAdapter.RUNTIME_SPAWN_TAG)
				|| !mob.getPersistentData().getString(
						DungeonMobLevelAdapter.INSTANCE_TAG).isBlank();
		boolean taggedEnemy = mob.getType().is(SOLO_ENEMY_TAG);
		boolean taggedBoss = mob.getType().is(SOLO_BOSS_TAG);
		if (id == null || !SololevelingMod.MODID.equals(id.getNamespace())
				&& !runtimeDungeonMob && !taggedEnemy && !taggedBoss)
			return;

		boolean excludedAlly = isHunterOrSummon(mob, id.getPath());
		boolean boss = !excludedAlly && isBoss(mob);
		if (!boss && !isEnemy(mob, id.getPath(), excludedAlly))
			return;

		GameRules rules = event.getLevel().getLevelData().getGameRules();
		double enemyAmount = boss ? 0.0D : percentAmount(rules.getInt(
				SololevelingModGameRules.SOLO_LEVELING_ENEMY_SCALE));
		double bossAmount = boss ? percentAmount(rules.getInt(
				SololevelingModGameRules.SOLO_LEVELING_BOSS_POWER)) : 0.0D;
		float oldMaxHealth = mob.getMaxHealth();
		float healthRatio = oldMaxHealth > 0.0F
				? Mth.clamp(mob.getHealth() / oldMaxHealth, 0.0F, 1.0F) : 1.0F;

		replaceModifier(mob.getAttribute(Attributes.MAX_HEALTH), ENEMY_HEALTH_ID,
				"SLR enemy health setting", enemyAmount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		replaceModifier(mob.getAttribute(Attributes.ATTACK_DAMAGE), ENEMY_DAMAGE_ID,
				"SLR enemy damage setting", enemyAmount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		replaceModifier(mob.getAttribute(Attributes.MAX_HEALTH), BOSS_HEALTH_ID,
				"SLR boss health setting", bossAmount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		replaceModifier(mob.getAttribute(Attributes.ATTACK_DAMAGE), BOSS_DAMAGE_ID,
				"SLR boss damage setting", bossAmount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		replaceModifier(mob.getAttribute(Attributes.ARMOR), BOSS_ARMOR_ID,
				"SLR boss armor setting", bossAmount * 8.0D, AttributeModifier.Operation.ADD_VALUE);
		replaceModifier(mob.getAttribute(Attributes.ARMOR_TOUGHNESS), BOSS_TOUGHNESS_ID,
				"SLR boss armor toughness setting", bossAmount * 4.0D,
				AttributeModifier.Operation.ADD_VALUE);
		replaceModifier(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE), BOSS_KNOCKBACK_ID,
				"SLR boss knockback resistance setting", bossAmount * 0.2D,
				AttributeModifier.Operation.ADD_VALUE);

		if (mob.isAlive())
			mob.setHealth(Math.max(1.0F, mob.getMaxHealth() * healthRatio));
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || !isSoloDungeon(player.level()))
			return;
		int rule = player.level().getGameRules().getInt(
				SololevelingModGameRules.SOLO_LEVELING_DEATH_RULES);
		player.getPersistentData().remove(FORGIVING_SNAPSHOT);
		if (rule == FORGIVING_DEATH_RULE) {
			captureForgivingSnapshot(player);
		} else if (rule == HARSH_DEATH_RULE) {
			applyHarshLoss(player);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onPlayerDrops(LivingDropsEvent event) {
		if (event.getEntity() instanceof ServerPlayer player
				&& player.getPersistentData().contains(FORGIVING_SNAPSHOT, Tag.TAG_COMPOUND))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onPlayerExperienceDrop(LivingExperienceDropEvent event) {
		if (event.getEntity() instanceof ServerPlayer player
				&& player.getPersistentData().contains(FORGIVING_SNAPSHOT, Tag.TAG_COMPOUND))
			event.setDroppedExperience(0);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (!event.isWasDeath()
				|| !event.getOriginal().getPersistentData().contains(
						FORGIVING_SNAPSHOT, Tag.TAG_COMPOUND))
			return;
		CompoundTag snapshot = event.getOriginal().getPersistentData()
				.getCompound(FORGIVING_SNAPSHOT);
		if (!(event.getEntity() instanceof ServerPlayer replacement))
			return;

		replacement.getInventory().clearContent();
		replacement.getInventory().load(snapshot.getList(SNAPSHOT_INVENTORY, Tag.TAG_COMPOUND));
		replacement.getInventory().selected = Mth.clamp(
				snapshot.getInt(SNAPSHOT_SELECTED_SLOT), 0, 8);
		replacement.experienceLevel = Math.max(0, snapshot.getInt(SNAPSHOT_XP_LEVEL));
		replacement.totalExperience = Math.max(0, snapshot.getInt(SNAPSHOT_XP_TOTAL));
		replacement.experienceProgress = Mth.clamp(
				snapshot.getFloat(SNAPSHOT_XP_PROGRESS), 0.0F, 1.0F);
		replacement.getInventory().setChanged();
		event.getOriginal().getPersistentData().remove(FORGIVING_SNAPSHOT);
		replacement.getPersistentData().remove(FORGIVING_SNAPSHOT);
	}

	private static void captureForgivingSnapshot(ServerPlayer player) {
		CompoundTag snapshot = new CompoundTag();
		snapshot.put(SNAPSHOT_INVENTORY, player.getInventory().save(new ListTag()));
		snapshot.putInt(SNAPSHOT_SELECTED_SLOT, player.getInventory().selected);
		snapshot.putInt(SNAPSHOT_XP_LEVEL, player.experienceLevel);
		snapshot.putInt(SNAPSHOT_XP_TOTAL, player.totalExperience);
		snapshot.putFloat(SNAPSHOT_XP_PROGRESS, player.experienceProgress);
		player.getPersistentData().put(FORGIVING_SNAPSHOT, snapshot);
	}

	private static void applyHarshLoss(ServerPlayer player) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(variables -> {
					variables.Xp = Math.max(0.0D, variables.Xp * 0.75D);
					variables.golds = Math.max(0.0D, variables.golds * 0.90D);
				});
	}

	private static boolean isSoloDungeon(Level level) {
		ResourceLocation dimension = level.dimension().location();
		if (!SololevelingMod.MODID.equals(dimension.getNamespace()))
			return false;
		String path = dimension.getPath();
		return path.startsWith("dungeon_dimension") || path.equals("cartenon_temple");
	}

	private static boolean isEnemy(Mob mob, String path, boolean excludedAlly) {
		if (excludedAlly || NON_ENEMY_MONSTER_IDS.contains(path))
			return false;
		return mob.getType().is(SOLO_ENEMY_TAG)
				|| mob instanceof SteelFangWolfEntity
				|| mob instanceof Monster;
	}

	private static boolean isBoss(Mob mob) {
		return mob.getType().is(SOLO_BOSS_TAG)
				|| "boss".equalsIgnoreCase(mob.getPersistentData().getString(
						DungeonMobLevelAdapter.ROLE_TAG));
	}

	private static boolean isHunterOrSummon(Mob mob, String path) {
		if (mob instanceof HunterEntity)
			return true;
		if (path.startsWith("shadow_") || path.endsWith("_shadow")
				|| path.contains("_shadow_"))
			return true;
		if (mob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null)
			return true;
		if (mob instanceof TamableAnimal tame) {
			if (mob instanceof SteelFangWolfEntity)
				return tame.isTame() || tame.getOwnerUUID() != null;
			return true;
		}
		return false;
	}

	private static double percentAmount(int percent) {
		int safePercent = Mth.clamp(percent, 25, 400);
		return safePercent == STANDARD_PERCENT
				? 0.0D : safePercent / 100.0D - 1.0D;
	}

	private static void replaceModifier(@Nullable AttributeInstance instance, ResourceLocation id,
			String name, double amount, AttributeModifier.Operation operation) {
		if (instance == null)
			return;
		if (instance.getModifier(id) != null)
			instance.removeModifier(id);
		if (Double.isFinite(amount) && amount != 0.0D)
			instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
	}

	private static ResourceLocation modifierId(String statistic) {
		return ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
				"attribute/world_rules_" + statistic);
	}
}
