package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.IgrisShadowEntity;
import net.solocraft.entity.TuskShadowEntity;
import net.solocraft.init.SololevelingModItems;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Server-authoritative combat effects supplied by boss-shadow equipment.
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class ShadowEquipmentCombatHandler {
	private static final float TUSK_ORB_DAMAGE_MULTIPLIER = 2.0F;
	private static final String IGRIS_STORM_COOLDOWN = "sl_igris_equipment_storm_at";
	private static final int IGRIS_STORM_COOLDOWN_TICKS = 24;
	private static final double IGRIS_STORM_RADIUS = 7.5D;
	private static final double IGRIS_STORM_RADIUS_SQR = IGRIS_STORM_RADIUS * IGRIS_STORM_RADIUS;
	private static final int IGRIS_STORM_MAX_TARGETS = 4;
	private static final float IGRIS_STORM_DAMAGE = 8.0F;

	private ShadowEquipmentCombatHandler() {
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void amplifyEquippedTuskDamage(LivingHurtEvent event) {
		if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F
				|| !isOrbAmplifiedTuskDamage(event.getSource()))
			return;
		event.setAmount(event.getAmount() * TUSK_ORB_DAMAGE_MULTIPLIER);
	}

	/**
	 * Identifies damage already covered by the Tusk equipment multiplier. The Orb
	 * manager uses this to avoid applying its owner-held magic multiplier to the
	 * same hit a second time.
	 */
	public static boolean isOrbAmplifiedTuskDamage(DamageSource source) {
		Entity attacker = resolveAttackActor(source);
		return attacker instanceof TuskShadowEntity
				&& ShadowMonarchManager.isEquipmentEquipped(
						attacker, SololevelingModItems.ORB_OF_AVARICE.get());
	}

	/**
	 * Attempts one bounded lightning storm at an Igris animation impact. Multiple
	 * impact frames in the same move converge on the persistent cooldown.
	 */
	public static void tryIgrisImpactStorm(LevelAccessor world, Entity source, Vec3 center) {
		if (!(world instanceof ServerLevel level) || !(source instanceof IgrisShadowEntity igris)
				|| center == null || !igris.isAlive() || igris.level() != level
				|| !ShadowMonarchManager.isEquipmentEquipped(
						igris, SololevelingModItems.DEMON_KINGS_LONG_SWORD.get()))
			return;

		CompoundTag data = igris.getPersistentData();
		long now = level.getGameTime();
		if (now < data.getLong(IGRIS_STORM_COOLDOWN))
			return;

		List<LivingEntity> candidates = level.getEntitiesOfClass(
				LivingEntity.class,
				new AABB(center, center).inflate(IGRIS_STORM_RADIUS),
				target -> target.distanceToSqr(center) <= IGRIS_STORM_RADIUS_SQR
						&& ShadowMonarchManager.canShadowDamage(igris, target))
				.stream()
				.sorted(Comparator.comparingDouble(target -> target.distanceToSqr(center)))
				.toList();

		int struck = 0;
		for (LivingEntity target : candidates) {
			if (struck >= IGRIS_STORM_MAX_TARGETS)
				break;
			// Revalidate at the point of effect application in case another event
			// changed ownership, team, party or target state during this impact.
			if (!ShadowMonarchManager.canShadowDamage(igris, target))
				continue;
			if (struck == 0)
				data.putLong(IGRIS_STORM_COOLDOWN, now + IGRIS_STORM_COOLDOWN_TICKS);
			spawnVisualLightning(level, target.position());
			target.hurt(new DamageSource(
					level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
							.getHolderOrThrow(DamageTypes.LIGHTNING_BOLT),
					igris), IGRIS_STORM_DAMAGE);
			struck++;
		}
	}

	private static void spawnVisualLightning(ServerLevel level, Vec3 position) {
		LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
		if (lightning == null)
			return;
		lightning.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(position)));
		lightning.setVisualOnly(true);
		level.addFreshEntity(lightning);
	}

	private static Entity resolveAttackActor(DamageSource source) {
		if (source == null)
			return null;
		Entity attacker = source.getEntity();
		if (attacker instanceof Projectile projectile && projectile.getOwner() != null)
			return projectile.getOwner();
		if (attacker != null)
			return attacker;
		if (source.getDirectEntity() instanceof Projectile projectile
				&& projectile.getOwner() != null)
			return projectile.getOwner();
		return source.getDirectEntity();
	}
}
