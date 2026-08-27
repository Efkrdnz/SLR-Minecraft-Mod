package net.solocraft.dungeon;

import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/** Keeps the shared E/D dungeon dimension at an introductory difficulty. */
@EventBusSubscriber
public final class LowRankDungeonBalance {
	public static final String PROCEDURAL_RANK_TAG = "slr_procedural_mob_rank";

	private static final ResourceKey<Level> LOW_RANK_DIMENSION = ResourceKey.create(
			Registries.DIMENSION,
			ResourceLocation.fromNamespaceAndPath("sololeveling", "dungeon_dimension_d"));
	private static final TagKey<EntityType<?>> DUNGEON_MOBS = TagKey.create(
			Registries.ENTITY_TYPE, ResourceLocation.parse("dm"));
	private static final ResourceLocation HEALTH_MODIFIER_ID =
			ResourceLocation.fromNamespaceAndPath("sololeveling",
					"attribute/low_rank_dungeon_health");

	private static final double E_HEALTH_MULTIPLIER = 0.65D;
	private static final double D_HEALTH_MULTIPLIER = 0.80D;
	private static final float E_DAMAGE_MULTIPLIER = 0.55F;
	private static final float D_DAMAGE_MULTIPLIER = 0.70F;

	private LowRankDungeonBalance() {
	}

	/** Applies a durable, non-stacking health cap to mobs loaded in E/D content. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()
				|| event.getLevel().dimension() != LOW_RANK_DIMENSION
				|| !isDungeonEnemy(event.getEntity()))
			return;
		applyMobBalance(event.getEntity(), rankFromTag(event.getEntity()));
	}

	/**
	 * Reduces the final outgoing hit, including scripted and projectile damage.
	 * HIGH runs before Tanker's LOWEST mitigation, so class defenses still get
	 * the correctly attributed, already rank-balanced hit.
	 */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingHurt(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof Player victim)
				|| victim.level().isClientSide()
				|| victim.level().dimension() != LOW_RANK_DIMENSION
				|| event.getAmount() <= 0.0F)
			return;
		Entity attacker = resolveAttacker(event.getSource());
		if (!isDungeonEnemy(attacker)
				|| attacker.level().dimension() != LOW_RANK_DIMENSION)
			return;
		ProceduralDungeonRank rank = rankFromTag(attacker);
		float multiplier = rank == ProceduralDungeonRank.E
				? E_DAMAGE_MULTIPLIER : D_DAMAGE_MULTIPLIER;
		event.setAmount(event.getAmount() * multiplier);
	}

	public static void applyMobBalance(Entity entity, ProceduralDungeonRank rank) {
		if (!(entity instanceof LivingEntity living) || !isDungeonEnemy(entity)
				|| (rank != ProceduralDungeonRank.E
						&& rank != ProceduralDungeonRank.D))
			return;
		double multiplier = rank == ProceduralDungeonRank.E
				? E_HEALTH_MULTIPLIER : D_HEALTH_MULTIPLIER;
		AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null)
			return;
		if (maxHealth.getModifier(HEALTH_MODIFIER_ID) != null)
			maxHealth.removeModifier(HEALTH_MODIFIER_ID);
		maxHealth.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER_ID,
				multiplier - 1.0D,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		living.setHealth(Math.min(living.getHealth(), living.getMaxHealth()));
	}

	private static ProceduralDungeonRank rankFromTag(Entity entity) {
		if (entity == null)
			return ProceduralDungeonRank.D;
		return ProceduralDungeonRank.tryParse(
				entity.getPersistentData().getString(PROCEDURAL_RANK_TAG))
				.filter(rank -> rank == ProceduralDungeonRank.E
						|| rank == ProceduralDungeonRank.D)
				.orElse(ProceduralDungeonRank.D);
	}

	private static boolean isDungeonEnemy(Entity entity) {
		return entity != null && !(entity instanceof Player)
				&& (entity instanceof Monster || entity.getType().is(DUNGEON_MOBS));
	}

	private static Entity resolveAttacker(DamageSource source) {
		if (source == null)
			return null;
		Entity attacker = source.getEntity();
		if (attacker == null)
			attacker = source.getDirectEntity();
		for (int depth = 0; depth < 4 && attacker != null; depth++) {
			Entity owner = null;
			if (attacker instanceof Projectile projectile)
				owner = projectile.getOwner();
			if (owner == null && attacker instanceof OwnableEntity ownable)
				owner = ownable.getOwner();
			if (owner == null || owner == attacker)
				break;
			attacker = owner;
		}
		return attacker;
	}
}
