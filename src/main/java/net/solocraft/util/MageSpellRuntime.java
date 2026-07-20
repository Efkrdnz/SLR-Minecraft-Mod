package net.solocraft.util;

import net.solocraft.init.SololevelingModItems;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.MagicMissileEntity;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public final class MageSpellRuntime {
	private static final List<PathCast> ACTIVE_PATHS = new ArrayList<>();
	private static final List<MagicMissileCast> ACTIVE_MAGIC_MISSILES = new ArrayList<>();
	private static final TagKey<net.minecraft.world.entity.EntityType<?>> HUNTERS = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("hunters"));

	private MageSpellRuntime() {
	}

	public static void startWaterSlash(LevelAccessor world, Entity caster) {
		startPath(world, caster, PathType.WATER_SLASH, 30.0D);
	}

	public static void startCurseSphere(LevelAccessor world, Entity caster) {
		startPath(world, caster, PathType.CURSE_SPHERE, 30.0D);
	}

	public static void startMagicMissiles(LevelAccessor world, Entity caster) {
		if (world instanceof ServerLevel level && caster != null)
			ACTIVE_MAGIC_MISSILES.add(new MagicMissileCast(level, caster.getUUID()));
	}

	private static void startPath(LevelAccessor world, Entity caster, PathType type, double range) {
		if (!(world instanceof ServerLevel level) || caster == null)
			return;
		Vec3 start = caster.getEyePosition().subtract(0, type == PathType.WATER_SLASH ? 0.4D : 0.6D, 0);
		Vec3 intendedEnd = start.add(caster.getLookAngle().normalize().scale(range));
		HitResult hit = level.clip(new ClipContext(start, intendedEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		Vec3 end = hit.getType() == HitResult.Type.MISS ? intendedEnd : hit.getLocation();
		double distance = start.distanceTo(end);
		if (distance < 0.1D)
			return;
		ACTIVE_PATHS.add(new PathCast(level, caster.getUUID(), type, start, end.subtract(start).normalize(), distance));
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		Iterator<PathCast> pathIterator = ACTIVE_PATHS.iterator();
		while (pathIterator.hasNext()) {
			PathCast cast = pathIterator.next();
			Entity caster = cast.level.getEntity(cast.casterId);
			if (caster == null || !caster.isAlive() || caster.level() != cast.level || cast.tick(caster))
				pathIterator.remove();
		}
		Iterator<MagicMissileCast> missileIterator = ACTIVE_MAGIC_MISSILES.iterator();
		while (missileIterator.hasNext()) {
			MagicMissileCast cast = missileIterator.next();
			Entity caster = cast.level.getEntity(cast.casterId);
			if (caster == null || !caster.isAlive() || caster.level() != cast.level || cast.tick(caster))
				missileIterator.remove();
		}
	}

	private enum PathType {
		WATER_SLASH,
		CURSE_SPHERE
	}

	private static final class PathCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final PathType type;
		private final Vec3 direction;
		private final Set<UUID> hitTargets = new HashSet<>();
		private Vec3 position;
		private double remaining;
		private int age;

		private PathCast(ServerLevel level, UUID casterId, PathType type, Vec3 position, Vec3 direction, double remaining) {
			this.level = level;
			this.casterId = casterId;
			this.type = type;
			this.position = position;
			this.direction = direction;
			this.remaining = remaining;
		}

		private boolean tick(Entity caster) {
			age++;
			if (type == PathType.CURSE_SPHERE && age <= 5)
				return false;
			double speed = type == PathType.WATER_SLASH ? 3.5D : 6.0D;
			double travel = Math.min(speed, remaining);
			Vec3 previous = position;
			position = position.add(direction.scale(travel));
			remaining -= travel;

			if (type == PathType.WATER_SLASH) {
				level.sendParticles(ParticleTypes.SPLASH, position.x, position.y, position.z, 18, 1.2D, 0.35D, 1.2D, 0.08D);
				damageWaterSlash(caster, previous);
			} else {
				level.sendParticles(ParticleTypes.SMOKE, position.x, position.y, position.z, 10, 0.35D, 0.35D, 0.35D, 0.02D);
				damageCurseSphere(caster, previous);
			}
			return remaining <= 0.001D || age > 40;
		}

		private void damageWaterSlash(Entity caster, Vec3 previous) {
			AABB area = new AABB(previous, position).inflate(6.0D, 2.0D, 6.0D);
			float damage = (float) (3 + MageCombatHelper.intelligence(caster) / 10.0D);
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, target -> MageCombatHelper.isValidTarget(caster, target))) {
				if (!hitTargets.add(target.getUUID()))
					continue;
				target.clearFire();
				MageCombatHelper.hurt(level, caster, target, damage);
			}
		}

		private void damageCurseSphere(Entity caster, Vec3 previous) {
			AABB area = new AABB(previous, position).inflate(1.5D);
			double damage = MageCombatHelper.intelligence(caster) / 20.0D + 4.0D;
			if (caster instanceof LivingEntity living && living.isHolding(SololevelingModItems.STORM_GRIAMORE.get()))
				damage *= 1.5D;
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, target -> MageCombatHelper.isValidTarget(caster, target))) {
				if (hitTargets.add(target.getUUID()))
					MageCombatHelper.hurt(level, caster, target, (float) damage);
			}
		}
	}

	private static final class MagicMissileCast {
		private final ServerLevel level;
		private final UUID casterId;
		private int fired;

		private MagicMissileCast(ServerLevel level, UUID casterId) {
			this.level = level;
			this.casterId = casterId;
		}

		private boolean tick(Entity caster) {
			float damage;
			if (caster.getType().is(HUNTERS))
				damage = (float) (2 + caster.getPersistentData().getDouble("int") / 10.0D);
			else if (caster instanceof Player)
				damage = (float) (2 + MageCombatHelper.intelligence(caster) / 100.0D);
			else
				return true;

			MagicMissileEntity missile = new MagicMissileEntity(SololevelingModEntities.MAGIC_MISSILE.get(), level);
			missile.setOwner(caster);
			missile.setBaseDamage(damage);
			missile.setKnockback(1);
			missile.setSilent(true);
			missile.setPos(caster.getX(), caster.getEyeY() - 0.1D, caster.getZ());
			Vec3 look = caster.getLookAngle();
			missile.shoot(look.x, look.y, look.z, 0.4F, 90.0F);
			level.addFreshEntity(missile);
			return ++fired >= 15;
		}
	}
}
