package net.solocraft.util;

import net.solocraft.entity.TuskShadowEntity;
import net.solocraft.entity.ai.TuskShadowCombatPolicy;
import net.solocraft.init.SololevelingModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Server-authoritative spell scheduler for Shadow Tusk.
 *
 * <p>One cast timeline controls animation, impact, and cooldown state. This
 * replaces the old collection of independent per-tick counters which could
 * overlap animations and apply damage without a usable firing position.</p>
 */
public final class TuskShadowCombatManager {
	private static final String INITIALIZED = "sl_tusk_combat_initialized";
	private static final String NEXT_SOUL_FLAME_AT = "sl_tusk_soul_flame_at";
	private static final String NEXT_CURSE_FIELD_AT = "sl_tusk_curse_field_at";
	private static final String NEXT_GROUND_SMASH_AT = "sl_tusk_ground_smash_at";
	private static final String NEXT_HYMN_AT = "sl_tusk_hymn_at";
	private static final String NEXT_GLOBAL_CAST_AT = "sl_tusk_global_cast_at";
	private static final String CAST_KIND = "sl_tusk_cast_kind";
	private static final String CAST_TARGET = "sl_tusk_cast_target";
	private static final String CAST_RELEASE_AT = "sl_tusk_cast_release_at";
	private static final String CAST_END_AT = "sl_tusk_cast_end_at";
	private static final String CAST_RELEASED = "sl_tusk_cast_released";

	private static final String SOUL_FLAME = "soul_flame";
	private static final String CURSE_FIELD = "curse_field";
	private static final String GROUND_SMASH = "ground_smash";
	private static final String HYMN_DEFENSE = "hymn_defense";
	private static final String HYMN_OFFENSE = "hymn_offense";

	private static final int SOUL_FLAME_CAST_TICKS = 15;
	private static final int SOUL_FLAME_RELEASE_TICK = 10;
	private static final int CURSE_FIELD_CAST_TICKS = 15;
	private static final int CURSE_FIELD_RELEASE_TICK = 11;
	private static final int HYMN_CAST_TICKS = 15;
	private static final int HYMN_RELEASE_TICK = 10;
	private static final int GROUND_SMASH_CAST_TICKS = 25;
	private static final int GROUND_SMASH_RELEASE_TICK = 17;
	private static final int GLOBAL_RECOVERY_TICKS = 4;

	private static final double SOUL_FLAME_RANGE = 24.0D;
	private static final double CURSE_FIELD_RANGE = 22.0D;
	private static final double CURSE_FIELD_RADIUS = 4.0D;
	private static final double GROUND_SMASH_TRIGGER_RANGE = 5.5D;
	private static final double GROUND_SMASH_RADIUS = 6.5D;
	private static final double OWNER_BUFF_RANGE = 32.0D;
	private static final int CURSE_FIELD_MAX_TARGETS = 6;
	private static final int GROUND_SMASH_MAX_TARGETS = 8;

	private TuskShadowCombatManager() {
	}

	public static void tick(TuskShadowEntity tusk) {
		if (tusk == null || !(tusk.level() instanceof ServerLevel level)
				|| !tusk.isAlive())
			return;
		initializeSchedule(tusk, level.getGameTime());
		if (tickActiveCast(tusk, level))
			return;

		Player owner = ShadowMonarchManager.getShadowOwnerPlayer(tusk);
		LivingEntity target = tusk.getTarget();
		if (owner == null || !owner.isAlive() || !isSafeTarget(tusk, target))
			return;
		if (tusk.distanceToSqr(owner)
				> square(TuskShadowCombatPolicy.OWNER_LEASH_RANGE))
			return;
		long now = level.getGameTime();
		if (now < tusk.getPersistentData().getLong(NEXT_GLOBAL_CAST_AT))
			return;

		double distance = CombatRangeHelper.surfaceDistance(tusk, target);
		boolean hasLineOfSight = tusk.getSensing().hasLineOfSight(target);
		CompoundTag data = tusk.getPersistentData();
		if (now >= data.getLong(NEXT_GROUND_SMASH_AT)) {
			List<LivingEntity> closeThreats = validTargets(tusk,
					tusk.position(), GROUND_SMASH_RADIUS, 2);
			if (!closeThreats.isEmpty()
					&& (CombatRangeHelper.surfaceDistance(tusk,
							closeThreats.get(0)) <= GROUND_SMASH_TRIGGER_RANGE
							|| closeThreats.size() >= 2)) {
				beginCast(tusk, closeThreats.get(0), GROUND_SMASH,
						"groundsmash", GROUND_SMASH_RELEASE_TICK,
						GROUND_SMASH_CAST_TICKS);
				data.putLong(NEXT_GROUND_SMASH_AT,
						now + 280L + tusk.getRandom().nextInt(61));
				return;
			}
		}
		if (!TuskShadowCombatPolicy.isUsefulCastingPosition(distance,
				hasLineOfSight))
			return;

		if (now >= data.getLong(NEXT_HYMN_AT)
				&& tusk.distanceToSqr(owner) <= square(OWNER_BUFF_RANGE)
				&& owner.getHealth() <= owner.getMaxHealth() * 0.45F) {
			beginCast(tusk, owner, HYMN_DEFENSE, "cast",
					HYMN_RELEASE_TICK, HYMN_CAST_TICKS);
			data.putLong(NEXT_HYMN_AT,
					now + 400L + tusk.getRandom().nextInt(81));
			return;
		}

		if (now >= data.getLong(NEXT_CURSE_FIELD_AT)
				&& distance <= CURSE_FIELD_RANGE
				&& countValidTargets(tusk, target.position(),
						CURSE_FIELD_RADIUS, 2) >= 2) {
			beginCast(tusk, target, CURSE_FIELD, "cast",
					CURSE_FIELD_RELEASE_TICK, CURSE_FIELD_CAST_TICKS);
			data.putLong(NEXT_CURSE_FIELD_AT,
					now + 210L + tusk.getRandom().nextInt(51));
			return;
		}

		if (now >= data.getLong(NEXT_HYMN_AT)
				&& tusk.distanceToSqr(owner) <= square(OWNER_BUFF_RANGE)) {
			beginCast(tusk, owner, HYMN_OFFENSE, "cast",
					HYMN_RELEASE_TICK, HYMN_CAST_TICKS);
			data.putLong(NEXT_HYMN_AT,
					now + 400L + tusk.getRandom().nextInt(81));
			return;
		}

		if (now >= data.getLong(NEXT_SOUL_FLAME_AT)
				&& distance <= SOUL_FLAME_RANGE) {
			beginCast(tusk, target, SOUL_FLAME, "cast",
					SOUL_FLAME_RELEASE_TICK, SOUL_FLAME_CAST_TICKS);
			data.putLong(NEXT_SOUL_FLAME_AT,
					now + 55L + tusk.getRandom().nextInt(21));
		}
	}

	public static boolean isCasting(TuskShadowEntity tusk) {
		if (tusk == null)
			return false;
		CompoundTag data = tusk.getPersistentData();
		return !data.getString(CAST_KIND).isEmpty()
				&& tusk.level().getGameTime() <= data.getLong(CAST_END_AT);
	}

	private static void initializeSchedule(TuskShadowEntity tusk, long now) {
		CompoundTag data = tusk.getPersistentData();
		if (data.getBoolean(INITIALIZED))
			return;
		data.putBoolean(INITIALIZED, true);
		data.putLong(NEXT_SOUL_FLAME_AT, now);
		data.putLong(NEXT_CURSE_FIELD_AT,
				now + 60L + Math.floorMod(tusk.getId(), 41));
		data.putLong(NEXT_GROUND_SMASH_AT,
				now + 100L + Math.floorMod(tusk.getId(), 61));
		data.putLong(NEXT_HYMN_AT,
				now + 80L + Math.floorMod(tusk.getId(), 41));
		data.putLong(NEXT_GLOBAL_CAST_AT, now);
		clearCast(data);
	}

	private static void beginCast(TuskShadowEntity tusk, LivingEntity target,
			String kind, String animation, int releaseDelay, int duration) {
		long now = tusk.level().getGameTime();
		CompoundTag data = tusk.getPersistentData();
		data.putString(CAST_KIND, kind);
		data.putUUID(CAST_TARGET, target.getUUID());
		data.putLong(CAST_RELEASE_AT, now + releaseDelay);
		data.putLong(CAST_END_AT, now + duration);
		data.putBoolean(CAST_RELEASED, false);
		data.putLong(NEXT_GLOBAL_CAST_AT,
				now + duration + GLOBAL_RECOVERY_TICKS);
		tusk.getNavigation().stop();
		tusk.setCombatState("casting_" + kind);
		tusk.setAnimation(animation);
		if (tusk.level() instanceof ServerLevel level)
			level.playSound(null, tusk.blockPosition(),
					SoundEvents.EVOKER_CAST_SPELL, SoundSource.NEUTRAL,
					0.85F, kind.equals(GROUND_SMASH) ? 0.72F : 1.08F);
	}

	private static boolean tickActiveCast(TuskShadowEntity tusk,
			ServerLevel level) {
		CompoundTag data = tusk.getPersistentData();
		String kind = data.getString(CAST_KIND);
		if (kind.isEmpty())
			return false;
		long now = level.getGameTime();
		long endAt = data.getLong(CAST_END_AT);
		if (now > endAt + 1L) {
			clearCast(data);
			tusk.setCombatState("idle");
			return false;
		}

		LivingEntity castTarget = resolveCastTarget(level, data);
		if (castTarget != null)
			tusk.getLookControl().setLookAt(castTarget, 60.0F, 60.0F);
		tusk.getNavigation().stop();
		tusk.setCombatState("casting_" + kind);
		if (!data.getBoolean(CAST_RELEASED)
				&& now >= data.getLong(CAST_RELEASE_AT)) {
			data.putBoolean(CAST_RELEASED, true);
			releaseCast(tusk, level, castTarget, kind);
		}
		if (now >= endAt) {
			clearCast(data);
			tusk.setCombatState("holding");
			return false;
		}
		return true;
	}

	private static LivingEntity resolveCastTarget(ServerLevel level,
			CompoundTag data) {
		if (!data.hasUUID(CAST_TARGET))
			return null;
		Entity entity = level.getEntity(data.getUUID(CAST_TARGET));
		return entity instanceof LivingEntity living ? living : null;
	}

	private static void releaseCast(TuskShadowEntity tusk, ServerLevel level,
			LivingEntity target, String kind) {
		switch (kind) {
			case SOUL_FLAME -> releaseSoulFlame(tusk, level, target);
			case CURSE_FIELD -> releaseCurseField(tusk, level, target);
			case GROUND_SMASH -> releaseGroundSmash(tusk, level);
			case HYMN_DEFENSE -> releaseHymn(tusk, level, target, true);
			case HYMN_OFFENSE -> releaseHymn(tusk, level, target, false);
			default -> {
			}
		}
	}

	private static void releaseSoulFlame(TuskShadowEntity tusk,
			ServerLevel level, LivingEntity target) {
		if (!canReleaseAtTarget(tusk, target, SOUL_FLAME_RANGE))
			return;
		Vec3 start = tusk.getEyePosition().add(0.0D, -0.25D, 0.0D);
		Vec3 end = target.position().add(0.0D,
				target.getBbHeight() * 0.55D, 0.0D);
		Vec3 delta = end.subtract(start);
		for (int point = 0; point <= 8; point++) {
			Vec3 position = start.add(delta.scale(point / 8.0D));
			level.sendParticles((SimpleParticleType)
					SololevelingModParticleTypes.MANA_PURPLE.get(),
					position.x, position.y, position.z, 1,
					0.035D, 0.035D, 0.035D, 0.0D);
		}
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				end.x, end.y, end.z, 12, 0.35D, 0.45D, 0.35D, 0.035D);
		level.sendParticles(ParticleTypes.FLAME,
				end.x, end.y, end.z, 7, 0.28D, 0.35D, 0.28D, 0.025D);
		float damage = TuskShadowCombatPolicy.soulFlameDamage(
				tusk.getAttributeValue(Attributes.ATTACK_DAMAGE));
		if (target.hurt(magicDamage(level, tusk), damage))
			target.igniteForSeconds(3);
		level.playSound(null, BlockPos.containing(end), SoundEvents.BLAZE_SHOOT,
				SoundSource.NEUTRAL, 0.9F, 0.8F);
	}

	private static void releaseCurseField(TuskShadowEntity tusk,
			ServerLevel level, LivingEntity target) {
		if (!canReleaseAtTarget(tusk, target, CURSE_FIELD_RANGE))
			return;
		Vec3 center = target.position();
		level.sendParticles((SimpleParticleType)
				SololevelingModParticleTypes.MANA_PURPLE.get(),
				center.x, center.y + 0.35D, center.z, 28,
				2.4D, 0.35D, 2.4D, 0.055D);
		level.sendParticles(ParticleTypes.WITCH,
				center.x, center.y + 0.55D, center.z, 18,
				2.0D, 0.3D, 2.0D, 0.025D);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,
				center.x, center.y + 0.15D, center.z, 12,
				2.2D, 0.2D, 2.2D, 0.025D);
		float damage = TuskShadowCombatPolicy.curseFieldDamage(
				tusk.getAttributeValue(Attributes.ATTACK_DAMAGE));
		for (LivingEntity affected : validTargets(tusk, center,
				CURSE_FIELD_RADIUS, CURSE_FIELD_MAX_TARGETS)) {
			affected.addEffect(new MobEffectInstance(
					MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true));
			affected.addEffect(new MobEffectInstance(
					MobEffects.WEAKNESS, 80, 0, false, true));
			affected.hurt(magicDamage(level, tusk), damage);
		}
	}

	private static void releaseGroundSmash(TuskShadowEntity tusk,
			ServerLevel level) {
		Vec3 center = tusk.position();
		level.sendParticles((SimpleParticleType)
				SololevelingModParticleTypes.IMPACT_22.get(),
				center.x, center.y + 0.2D, center.z, 22,
				2.8D, 0.18D, 2.8D, 0.08D);
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
				center.x, center.y + 0.15D, center.z, 24,
				3.0D, 0.25D, 3.0D, 0.055D);
		level.sendParticles(ParticleTypes.EXPLOSION,
				center.x, center.y + 0.2D, center.z, 5,
				1.8D, 0.2D, 1.8D, 0.0D);
		float damage = TuskShadowCombatPolicy.groundSmashDamage(
				tusk.getAttributeValue(Attributes.ATTACK_DAMAGE));
		for (LivingEntity affected : validTargets(tusk, center,
				GROUND_SMASH_RADIUS, GROUND_SMASH_MAX_TARGETS)) {
			if (!affected.hurt(level.damageSources().mobAttack(tusk), damage))
				continue;
			Vec3 away = affected.position().subtract(center)
					.multiply(1.0D, 0.0D, 1.0D);
			if (away.lengthSqr() > 1.0E-4D) {
				away = away.normalize().scale(0.72D);
				affected.push(away.x, 0.38D, away.z);
			}
		}
		level.playSound(null, tusk.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
				SoundSource.NEUTRAL, 1.35F, 0.72F);
	}

	private static void releaseHymn(TuskShadowEntity tusk,
			ServerLevel level, LivingEntity target, boolean defensive) {
		Player owner = ShadowMonarchManager.getShadowOwnerPlayer(tusk);
		if (owner == null || target != owner || !owner.isAlive()
				|| tusk.distanceToSqr(owner) > square(OWNER_BUFF_RANGE + 4.0D))
			return;
		if (defensive) {
			applyRespectingStronger(owner, MobEffects.DAMAGE_RESISTANCE,
					140, 0);
			applyRespectingStronger(owner, MobEffects.ABSORPTION, 140, 0);
			applyRespectingStronger(owner, MobEffects.REGENERATION, 100, 0);
		} else {
			applyRespectingStronger(owner, MobEffects.DAMAGE_BOOST, 120, 0);
			applyRespectingStronger(owner, MobEffects.MOVEMENT_SPEED, 120, 0);
		}
		level.sendParticles((SimpleParticleType)
				SololevelingModParticleTypes.MANA_PURPLE.get(),
				owner.getX(), owner.getY() + owner.getBbHeight() * 0.55D,
				owner.getZ(), 22, owner.getBbWidth() * 0.7D,
				owner.getBbHeight() * 0.35D,
				owner.getBbWidth() * 0.7D, 0.04D);
		level.sendParticles(ParticleTypes.ENCHANT,
				owner.getX(), owner.getY() + 1.0D, owner.getZ(), 14,
				0.55D, 0.75D, 0.55D, 0.055D);
	}

	private static void applyRespectingStronger(LivingEntity target,
			Holder<MobEffect> effect, int duration, int amplifier) {
		MobEffectInstance active = target.getEffect(effect);
		if (active != null && (active.getAmplifier() > amplifier
				|| active.getAmplifier() == amplifier
						&& active.getDuration() >= duration))
			return;
		target.addEffect(new MobEffectInstance(effect, duration, amplifier,
				false, true));
	}

	private static boolean canReleaseAtTarget(TuskShadowEntity tusk,
			LivingEntity target, double range) {
		return isSafeTarget(tusk, target)
				&& CombatRangeHelper.withinSurfaceRange(tusk, target, range)
				&& tusk.getSensing().hasLineOfSight(target);
	}

	private static boolean isSafeTarget(TuskShadowEntity tusk,
			LivingEntity target) {
		return target != null && target.level() == tusk.level()
				&& ShadowMonarchManager.canShadowDamage(tusk, target);
	}

	private static int countValidTargets(TuskShadowEntity tusk, Vec3 center,
			double radius, int stopAt) {
		int count = 0;
		AABB area = new AABB(center, center).inflate(radius, 2.5D, radius);
		for (LivingEntity candidate : tusk.level().getEntitiesOfClass(
				LivingEntity.class, area,
				target -> tusk.getSensing().hasLineOfSight(target)
						&& ShadowMonarchManager.canShadowDamage(tusk, target))) {
			count++;
			if (count >= stopAt)
				return count;
		}
		return count;
	}

	private static List<LivingEntity> validTargets(TuskShadowEntity tusk,
			Vec3 center, double radius, int maximum) {
		AABB area = new AABB(center, center).inflate(radius, 2.5D, radius);
		return tusk.level().getEntitiesOfClass(LivingEntity.class, area,
				candidate -> candidate.distanceToSqr(center)
						<= radius * radius + 6.25D
						&& tusk.getSensing().hasLineOfSight(candidate)
						&& ShadowMonarchManager.canShadowDamage(tusk, candidate))
				.stream()
				.sorted(Comparator.comparingDouble(
						candidate -> candidate.distanceToSqr(center)))
				.limit(maximum)
				.toList();
	}

	private static DamageSource magicDamage(ServerLevel level,
			TuskShadowEntity tusk) {
		return new DamageSource(level.registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(DamageTypes.MAGIC), tusk);
	}

	private static void clearCast(CompoundTag data) {
		data.remove(CAST_KIND);
		data.remove(CAST_TARGET);
		data.remove(CAST_RELEASE_AT);
		data.remove(CAST_END_AT);
		data.remove(CAST_RELEASED);
	}

	private static double square(double value) {
		return value * value;
	}
}
