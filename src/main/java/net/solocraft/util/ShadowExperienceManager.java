package net.solocraft.util;

import net.solocraft.dungeon.runtime.DungeonLevelHelper;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.projectile.Projectile;

import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Records actual post-mitigation shadow damage on a target, then distributes
 * that target's XP pool when it dies. A player or another shadow may land the
 * final blow without erasing earlier contributions.
 */
public final class ShadowExperienceManager {
	private static final String LEDGER = "sl_shadow_xp_ledger";
	private static final String TOTAL_DAMAGE = "total_damage";
	private static final String CONTRIBUTORS = "contributors";
	private static final String OWNER = "owner";
	private static final String SHADOW_ID = "shadow_id";
	private static final String SHADOW_ENTITY = "shadow_entity";
	private static final String DAMAGE = "damage";
	private static final String AWARDED = "awarded";
	private static final double MAX_TRACKED_DAMAGE = 1_000_000_000.0D;

	private static final TagKey<EntityType<?>> SOLO_BOSS_TAG = entityTypeTag(
			"minecraft", "soloboss");
	private static final TagKey<EntityType<?>> HIGH_TIER_TAG = entityTypeTag(
			"minecraft", "hightier");
	private static final TagKey<EntityType<?>> MID_TIER_TAG = entityTypeTag(
			"minecraft", "midtier");

	private ShadowExperienceManager() {
	}

	public static void recordDamage(LivingDamageEvent.Post event) {
		if (event == null
				|| event.getEntity().level().isClientSide()
				|| JobChangeQuestManager.isAttemptEntity(
						event.getEntity())
				|| !isEligibleTarget(event.getEntity()))
			return;
		LivingEntity victim = event.getEntity();
		double damage = Math.max(0.0D, event.getNewDamage());
		if (!Double.isFinite(damage) || damage <= 0.0D)
			return;

		CompoundTag ledger = victim.getPersistentData().contains(LEDGER,
				Tag.TAG_COMPOUND)
						? victim.getPersistentData().getCompound(LEDGER)
						: new CompoundTag();
		ledger.putDouble(TOTAL_DAMAGE, boundedAdd(
				ledger.getDouble(TOTAL_DAMAGE), damage));

		Entity shadow = resolveShadow(event.getSource().getEntity());
		if (shadow == null)
			shadow = resolveShadow(event.getSource().getDirectEntity());
		if (shadow != null)
			recordShadowContribution(ledger, shadow, damage);
		victim.getPersistentData().put(LEDGER, ledger);
	}

	/**
	 * Awards every contributing shadow and returns how many roster entries
	 * received XP.
	 */
	public static int awardContributions(LivingEntity victim) {
		if (JobChangeQuestManager.isAttemptEntity(victim)
				|| !isEligibleTarget(victim)
				|| !(victim.level() instanceof ServerLevel level))
			return 0;
		CompoundTag persistent = victim.getPersistentData();
		if (!persistent.contains(LEDGER, Tag.TAG_COMPOUND))
			return 0;
		CompoundTag ledger = persistent.getCompound(LEDGER);
		if (ledger.getBoolean(AWARDED))
			return 0;
		ledger.putBoolean(AWARDED, true);
		persistent.put(LEDGER, ledger);

		ListTag contributors = ledger.getList(CONTRIBUTORS, Tag.TAG_COMPOUND);
		if (contributors.isEmpty())
			return 0;
		int targetPool = targetXpPool(victim);
		double countedTargetDamage = Math.max(
				Math.max(1.0D, victim.getMaxHealth()),
				ledger.getDouble(TOTAL_DAMAGE));
		int awardedCount = 0;
		MinecraftServer server = level.getServer();
		for (int index = 0; index < contributors.size(); index++) {
			CompoundTag contribution = contributors.getCompound(index);
			if (!contribution.hasUUID(OWNER))
				continue;
			String shadowId = contribution.getString(SHADOW_ID);
			double shadowDamage = contribution.getDouble(DAMAGE);
			if (shadowId.isEmpty() || !Double.isFinite(shadowDamage)
					|| shadowDamage <= 0.0D)
				continue;
			ServerPlayer owner = server.getPlayerList().getPlayer(
					contribution.getUUID(OWNER));
			if (owner == null)
				continue;
			Entity shadowEntity = contribution.hasUUID(SHADOW_ENTITY)
					? findEntity(server, contribution.getUUID(SHADOW_ENTITY))
					: null;
			int earned = ShadowMonarchManager.grantCombatXp(owner, shadowId,
					shadowEntity, targetPool, shadowDamage,
					countedTargetDamage);
			if (earned > 0)
				awardedCount++;
		}
		return awardedCount;
	}

	public static int targetXpPool(LivingEntity target) {
		if (target == null)
			return 0;
		CompoundTag data = target.getPersistentData();
		int configuredBaseXp = data.contains(
				DungeonMobLevelAdapter.XP_REWARD_TAG, Tag.TAG_ANY_NUMERIC)
						? Math.max(0, data.getInt(
								DungeonMobLevelAdapter.XP_REWARD_TAG))
						: -1;
		String role = data.getString(DungeonMobLevelAdapter.ROLE_TAG);
		boolean boss = "boss".equals(role)
				|| target.getType().is(SOLO_BOSS_TAG)
				|| target.getType().is(HIGH_TIER_TAG)
				|| target.getType() == EntityType.WITHER
				|| target.getType() == EntityType.ENDER_DRAGON;
		boolean elite = boss || "elite".equals(role)
				|| target.getType().is(MID_TIER_TAG);
		return ShadowExperienceRules.targetXpPool(target.getMaxHealth(),
				attributeValue(target, Attributes.ATTACK_DAMAGE),
				attributeValue(target, Attributes.ARMOR),
				attributeValue(target, Attributes.ARMOR_TOUGHNESS),
				DungeonLevelHelper.levelOf(target), configuredBaseXp,
				elite, boss, target instanceof Animal);
	}

	@Nullable
	public static Entity resolveShadow(@Nullable Entity source) {
		return resolveShadow(source, new HashSet<>());
	}

	private static Entity resolveShadow(@Nullable Entity source,
			Set<UUID> visited) {
		if (source == null || !visited.add(source.getUUID()))
			return null;
		if (ShadowMonarchManager.isTrackedShadowEntity(source)
				&& !ShadowMonarchManager.getShadowRosterId(source).isEmpty())
			return source;
		if (source instanceof Projectile projectile)
			return resolveShadow(projectile.getOwner(), visited);
		return null;
	}

	private static boolean isEligibleTarget(@Nullable LivingEntity target) {
		return target instanceof Mob
				&& !ShadowMonarchManager.isShadowEntity(target)
				&& !ShadowMonarchManager.isTrackedShadowEntity(target);
	}

	private static void recordShadowContribution(CompoundTag ledger,
			Entity shadow, double damage) {
		UUID ownerId = ShadowMonarchManager.getShadowOwnerUUID(shadow);
		String shadowId = ShadowMonarchManager.getShadowRosterId(shadow);
		if (ownerId == null || shadowId.isEmpty())
			return;
		ListTag contributors = ledger.getList(CONTRIBUTORS, Tag.TAG_COMPOUND);
		CompoundTag entry = null;
		for (int index = 0; index < contributors.size(); index++) {
			CompoundTag candidate = contributors.getCompound(index);
			if (candidate.hasUUID(OWNER)
					&& ownerId.equals(candidate.getUUID(OWNER))
					&& shadowId.equals(candidate.getString(SHADOW_ID))) {
				entry = candidate;
				break;
			}
		}
		if (entry == null) {
			entry = new CompoundTag();
			entry.putUUID(OWNER, ownerId);
			entry.putString(SHADOW_ID, shadowId);
			contributors.add(entry);
		}
		entry.putUUID(SHADOW_ENTITY, shadow.getUUID());
		entry.putDouble(DAMAGE, boundedAdd(entry.getDouble(DAMAGE), damage));
		ledger.put(CONTRIBUTORS, contributors);
	}

	private static double boundedAdd(double current, double addition) {
		double safeCurrent = Double.isFinite(current)
				? Math.max(0.0D, current) : 0.0D;
		double safeAddition = Double.isFinite(addition)
				? Math.max(0.0D, addition) : 0.0D;
		return Math.min(MAX_TRACKED_DAMAGE, safeCurrent + safeAddition);
	}

	private static double attributeValue(LivingEntity entity,
			Holder<Attribute> attribute) {
		AttributeInstance instance = entity.getAttribute(attribute);
		return instance == null ? 0.0D
				: Math.max(0.0D, instance.getValue());
	}

	@Nullable
	private static Entity findEntity(MinecraftServer server, UUID entityId) {
		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(entityId);
			if (entity != null)
				return entity;
		}
		return null;
	}

	private static TagKey<EntityType<?>> entityTypeTag(String namespace,
			String path) {
		return TagKey.create(Registries.ENTITY_TYPE,
				ResourceLocation.fromNamespaceAndPath(namespace, path));
	}
}
