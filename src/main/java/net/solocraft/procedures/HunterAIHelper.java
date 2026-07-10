package net.solocraft.procedures;

import net.solocraft.entity.HunterEntity;
import net.solocraft.entity.RangerProjectileEntity;
import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.eventbus.api.Event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class HunterAIHelper {
	private static final String DEFENSE_COOLDOWN = "slr_hunter_defense_cd";
	private static final String SKILL_COOLDOWN = "slr_hunter_skill_cd";
	private static final String BACKLINE_DODGE_COOLDOWN = "slr_hunter_backline_dodge_cd";

	private HunterAIHelper() {
	}

	public static void tickCooldowns(Entity entity) {
		decrement(entity, DEFENSE_COOLDOWN);
		decrement(entity, SKILL_COOLDOWN);
		decrement(entity, BACKLINE_DODGE_COOLDOWN);
	}

	public static void tryDefensiveReaction(Event event, Entity entity, Entity attacker) {
		if (!(entity instanceof HunterEntity hunter) || !(entity instanceof LivingEntity living) || attacker == null || entity.level().isClientSide())
			return;
		if (entity.getPersistentData().getInt(DEFENSE_COOLDOWN) > 0)
			return;
		String hunterClass = hunter.getEntityData().get(HunterEntity.DATA_HunterClass);
		int rank = rankValue(hunter);
		double roll = entity.level().random.nextDouble();
		boolean handled = switch (hunterClass) {
			case "Tanker" -> roll < 0.72D ? block(event, living, attacker, rank, true) : roll < 0.76D && dodge(event, living, attacker, rank, 0.55D);
			case "Fighter" -> roll < 0.36D ? block(event, living, attacker, rank, false) : roll < 0.62D && dodge(event, living, attacker, rank, 0.72D);
			case "Assassin" -> roll < 0.70D ? dodge(event, living, attacker, rank, 1.05D) : roll < 0.78D && block(event, living, attacker, rank, false);
			case "Ranger" -> roll < 0.28D && dodge(event, living, attacker, rank, 0.72D);
			case "Mage" -> roll < 0.22D && ward(event, living, rank);
			case "Healer" -> roll < 0.26D && ward(event, living, rank);
			default -> false;
		};
		if (handled)
			entity.getPersistentData().putInt(DEFENSE_COOLDOWN, Math.max(12, 26 - rank * 2));
	}

	public static void tankerCombatTick(Entity entity) {
		if (!(entity instanceof HunterEntity hunter) || !(entity instanceof LivingEntity living))
			return;
		Entity target = target(entity);
		if (target == null)
			return;
		int rank = rankValue(hunter);
		if (skillReady(entity, 70 - rank * 5)) {
			living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 45, Math.min(2, 1 + rank / 3), false, false));
			living.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 70, Math.min(2, rank / 2), false, false));
			tauntNearby(entity, rank);
			if (entity.distanceTo(target) <= 4.25D)
				strike(target, entity, 2.5F + rank * 1.25F, 0.55D);
			blockParticles(entity);
		}
	}

	public static void fighterCombatTick(Entity entity) {
		if (!(entity instanceof HunterEntity hunter) || !(entity instanceof LivingEntity living))
			return;
		Entity target = target(entity);
		if (target == null)
			return;
		int rank = rankValue(hunter);
		if (skillReady(entity, 55 - rank * 4)) {
			living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 45, Math.min(1, rank / 3), false, false));
			living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, Math.min(1, rank / 3), false, false));
			if (entity.distanceTo(target) > 3.5D)
				dashToward(entity, target, 1.05D);
			else
				strike(target, entity, 3.5F + rank * 1.55F, 0.65D);
			hitParticles(entity);
		}
	}

	public static void assassinCombatTick(Entity entity) {
		if (!(entity instanceof HunterEntity hunter) || !(entity instanceof LivingEntity living))
			return;
		Entity target = target(entity);
		if (target == null)
			return;
		int rank = rankValue(hunter);
		if (skillReady(entity, 42 - rank * 3)) {
			living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 35, Math.min(2, rank / 3), false, false));
			if (rank >= 4)
				living.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 35, 0, false, false));
			dashBehind(entity, target, 1.05D + rank * 0.05D);
			if (entity.distanceTo(target) <= 4.0D)
				strike(target, entity, 3.0F + rank * 1.65F, 0.25D);
			dodgeParticles(entity);
		}
	}

	public static void rangerCombatTick(Entity entity) {
		if (!(entity instanceof HunterEntity hunter))
			return;
		Entity target = target(entity);
		if (target == null)
			return;
		int rank = rankValue(hunter);
		if (entity.distanceTo(target) < 3.75D && backlineDodgeReady(entity, 45))
			dodgeMove(entity, target, 0.42D);
		if (rank >= 3 && skillReady(entity, 82 - rank * 3)) {
			shoot(entity, target, 0.8F + rank * 0.45F, 0.0D);
			if (rank >= 4)
				shoot(entity, target, 0.8F + rank * 0.38F, 0.09D);
			if (rank >= 5)
				shoot(entity, target, 0.8F + rank * 0.35F, -0.09D);
		}
	}

	public static void casterBacklineTick(Entity entity) {
		if (!(entity instanceof HunterEntity hunter) || !(entity instanceof LivingEntity living))
			return;
		Entity target = target(entity);
		if (target == null)
			return;
		int rank = rankValue(hunter);
		if (entity.distanceTo(target) < 3.5D && backlineDodgeReady(entity, 60))
			dodgeMove(entity, target, 0.45D);
		if (rank >= 4 && skillReady(entity, 120 - rank * 4)) {
			living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 45, 0, false, false));
			living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, false));
			wardParticles(entity);
		}
	}

	public static void healerSupportTick(Entity entity) {
		if (!(entity instanceof HunterEntity hunter) || !(entity instanceof LivingEntity living))
			return;
		int rank = rankValue(hunter);
		if (living.getHealth() / living.getMaxHealth() < 0.45F && skillReady(entity, 105 - rank * 4)) {
			living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, Math.min(1, rank / 3), false, false));
			living.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 65, Math.min(1, rank / 3), false, false));
			wardParticles(entity);
		}
	}

	private static boolean block(Event event, LivingEntity hunter, Entity attacker, int rank, boolean tanker) {
		cancel(event);
		hunter.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, tanker ? 38 : 22, tanker ? Math.min(2, 1 + rank / 3) : Math.min(1, rank / 3), false, false));
		if (tanker)
			hunter.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 55, Math.min(2, rank / 3), false, false));
		pushAttacker(hunter, attacker, tanker ? 0.45D : 0.25D);
		blockParticles(hunter);
		return true;
	}

	private static boolean dodge(Event event, LivingEntity hunter, Entity attacker, int rank, double strength) {
		cancel(event);
		dodgeMove(hunter, attacker, strength + rank * 0.025D);
		hunter.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 18, Math.min(2, rank / 3), false, false));
		dodgeParticles(hunter);
		return true;
	}

	private static boolean ward(Event event, LivingEntity hunter, int rank) {
		cancel(event);
		hunter.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 24, Math.min(1, rank / 3), false, false));
		hunter.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 38, Math.min(1, rank / 3), false, false));
		wardParticles(hunter);
		return true;
	}

	private static void cancel(Event event) {
		if (event != null && event.isCancelable())
			event.setCanceled(true);
	}

	private static boolean skillReady(Entity entity, int cooldown) {
		if (entity.getPersistentData().getInt(SKILL_COOLDOWN) > 0)
			return false;
		entity.getPersistentData().putInt(SKILL_COOLDOWN, Math.max(20, cooldown));
		return true;
	}

	private static boolean backlineDodgeReady(Entity entity, int cooldown) {
		if (entity.getPersistentData().getInt(BACKLINE_DODGE_COOLDOWN) > 0)
			return false;
		entity.getPersistentData().putInt(BACKLINE_DODGE_COOLDOWN, cooldown);
		return true;
	}

	private static void decrement(Entity entity, String key) {
		int value = entity.getPersistentData().getInt(key);
		if (value > 0)
			entity.getPersistentData().putInt(key, value - 1);
	}

	private static int rankValue(HunterEntity hunter) {
		return switch (hunter.getEntityData().get(HunterEntity.DATA_Rank)) {
			case "S" -> 4;
			case "A" -> 4;
			case "B" -> 3;
			case "C" -> 2;
			default -> 1;
		};
	}

	private static Entity target(Entity entity) {
		return entity instanceof Mob mob ? mob.getTarget() : null;
	}

	private static void strike(Entity target, Entity hunter, float damage, double knockback) {
		if (!(target instanceof LivingEntity living) || hunter.level().isClientSide())
			return;
		living.hurt(new DamageSource(hunter.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK), hunter), damage);
		pushAttacker((LivingEntity) hunter, target, knockback);
	}

	private static void tauntNearby(Entity hunter, int rank) {
		if (!(hunter instanceof LivingEntity living))
			return;
		for (Mob mob : hunter.level().getEntitiesOfClass(Mob.class, hunter.getBoundingBox().inflate(7 + rank), e -> true)) {
			if (mob == hunter || mob instanceof HunterEntity || mob.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("hunters"))))
				continue;
			mob.setTarget(living);
		}
	}

	private static void dashToward(Entity hunter, Entity target, double strength) {
		Vec3 dir = target.position().subtract(hunter.position()).normalize();
		hunter.setDeltaMovement(dir.x * strength, 0.18D, dir.z * strength);
	}

	private static void dashBehind(Entity hunter, Entity target, double strength) {
		Vec3 behind = target.getLookAngle().scale(-1.0D).normalize();
		hunter.setDeltaMovement(behind.x * strength, 0.12D, behind.z * strength);
	}

	private static void dodgeMove(Entity hunter, Entity attacker, double strength) {
		Vec3 away = hunter.position().subtract(attacker.position()).normalize();
		Vec3 side = new Vec3(-away.z, 0, away.x);
		if (hunter.level().random.nextBoolean())
			side = side.scale(-1);
		hunter.setDeltaMovement((away.x * 0.35D + side.x * 0.75D) * strength, 0.08D, (away.z * 0.35D + side.z * 0.75D) * strength);
		hunter.hasImpulse = true;
	}

	private static void pushAttacker(LivingEntity hunter, Entity attacker, double strength) {
		Vec3 dir = attacker.position().subtract(hunter.position()).normalize();
		attacker.setDeltaMovement(dir.x * strength, Math.max(attacker.getDeltaMovement().y, 0.12D), dir.z * strength);
	}

	private static void shoot(Entity hunter, Entity target, float damage, double spread) {
		Level level = hunter.level();
		if (level.isClientSide())
			return;
		Vec3 direction = target.position().add(0, target.getBbHeight() * 0.55D, 0).subtract(hunter.position().add(0, hunter.getBbHeight() * 0.7D, 0)).normalize();
		direction = direction.add(level.random.nextGaussian() * spread, level.random.nextGaussian() * spread * 0.4D, level.random.nextGaussian() * spread).normalize();
		Projectile projectile = new RangerProjectileEntity(SololevelingModEntities.RANGER_PROJECTILE.get(), level);
		if (projectile instanceof AbstractArrow arrow) {
			arrow.setOwner(hunter);
			arrow.setBaseDamage(damage);
			arrow.setKnockback(1);
			arrow.setSilent(true);
		}
		projectile.setPos(hunter.getX(), hunter.getEyeY() - 0.1D, hunter.getZ());
		projectile.shoot(direction.x, direction.y, direction.z, 2.7F, 0);
		level.addFreshEntity(projectile);
	}

	private static void blockParticles(Entity entity) {
		particles(entity, ParticleTypes.CRIT, 10);
		entity.level().playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.65F, 0.9F + entity.level().random.nextFloat() * 0.25F);
	}

	private static void dodgeParticles(Entity entity) {
		particles(entity, ParticleTypes.POOF, 8);
	}

	private static void hitParticles(Entity entity) {
		particles(entity, ParticleTypes.SWEEP_ATTACK, 4);
	}

	private static void wardParticles(Entity entity) {
		particles(entity, ParticleTypes.ENCHANT, 14);
	}

	private static void particles(Entity entity, net.minecraft.core.particles.ParticleOptions particle, int count) {
		if (entity.level() instanceof ServerLevel level)
			level.sendParticles(particle, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(), count, 0.45D, 0.65D, 0.45D, 0.04D);
	}
}
