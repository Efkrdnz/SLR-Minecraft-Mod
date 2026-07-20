package net.solocraft.util;

import net.solocraft.entity.BaekYoonhoEntity;
import net.solocraft.entity.ChaHaeInEntity;
import net.solocraft.entity.ChoijongEntity;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Fixed profiles, retaliation, and defensive reactions for named S-rank hunters. */
@Mod.EventBusSubscriber
public final class NamedHunterCombatManager {
	private static final String PROFILE_VERSION = "slr_named_hunter_profile";
	private static final int CURRENT_PROFILE_VERSION = 1;
	private static final String RETALIATION_TARGET = "slr_named_hunter_retaliation_target";
	private static final String RETALIATION_UNTIL = "slr_named_hunter_retaliation_until";
	private static final String DEFENSE_READY_AT = "slr_named_hunter_defense_ready";
	private static final long RETALIATION_TICKS = 20L * 30L;

	private NamedHunterCombatManager() {
	}

	public static boolean isNamedHunter(Entity entity) {
		return entity instanceof ChoijongEntity || entity instanceof ChaHaeInEntity || entity instanceof BaekYoonhoEntity;
	}

	public static void tick(PathfinderMob hunter) {
		if (hunter.level().isClientSide())
			return;
		ensureFixedProfile(hunter);
		CompoundTag data = hunter.getPersistentData();
		long now = hunter.level().getGameTime();
		if (!data.hasUUID(RETALIATION_TARGET) || data.getLong(RETALIATION_UNTIL) < now) {
			clearRetaliation(data);
			return;
		}
		if (!(hunter.level() instanceof ServerLevel serverLevel))
			return;
		Entity storedTarget = serverLevel.getEntity(data.getUUID(RETALIATION_TARGET));
		if (!(storedTarget instanceof LivingEntity target) || !canRetaliateAgainst(hunter, target)
				|| hunter.distanceToSqr(target) > 128.0D * 128.0D) {
			clearRetaliation(data);
			return;
		}
		if (hunter.getTarget() != target)
			hunter.setTarget(target);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onNamedHunterAttacked(LivingAttackEvent event) {
		if (event == null || event.isCanceled() || !isNamedHunter(event.getEntity()) || event.getEntity().level().isClientSide())
			return;
		LivingEntity attacker = resolveAttacker(event.getEntity(), event.getSource().getEntity(), event.getSource().getDirectEntity());
		if (attacker == null || !canRetaliateAgainst(event.getEntity(), attacker))
			return;
		PathfinderMob hunter = (PathfinderMob) event.getEntity();
		hunter.setLastHurtByMob(attacker);
		hunter.setTarget(attacker);
		CompoundTag data = hunter.getPersistentData();
		data.putUUID(RETALIATION_TARGET, attacker.getUUID());
		data.putLong(RETALIATION_UNTIL, hunter.level().getGameTime() + RETALIATION_TICKS);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onNamedHunterHurt(LivingHurtEvent event) {
		if (event == null || event.isCanceled() || event.getAmount() <= 0 || !isNamedHunter(event.getEntity())
				|| event.getEntity().level().isClientSide() || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return;
		LivingEntity hunter = event.getEntity();
		LivingEntity attacker = resolveAttacker(hunter, event.getSource().getEntity(), event.getSource().getDirectEntity());
		if (attacker == null || !canRetaliateAgainst(hunter, attacker))
			return;
		long now = hunter.level().getGameTime();
		CompoundTag data = hunter.getPersistentData();
		if (data.getLong(DEFENSE_READY_AT) > now)
			return;

		double attributeThreat = combatPower(attacker) / Math.max(1.0D, combatPower(hunter));
		double strikeThreat = event.getAmount() / Math.max(4.0D, hunter.getMaxHealth() * 0.08D);
		double threat = Mth.clamp(Math.max(attributeThreat, strikeThreat), 0.35D, 2.5D);
		boolean reacted = react(event, hunter, attacker, threat);
		data.putLong(DEFENSE_READY_AT, now + (reacted ? 14L : 4L));
	}

	private static boolean react(LivingHurtEvent event, LivingEntity hunter, LivingEntity attacker, double threat) {
		double roll = hunter.getRandom().nextDouble();
		if (hunter instanceof ChoijongEntity) {
			double dodgeChance = Mth.clamp(0.16D + threat * 0.18D, 0.22D, 0.58D);
			return roll < dodgeChance && dodge(event, hunter, attacker, 1.0D);
		}
		if (hunter instanceof ChaHaeInEntity) {
			double dodgeChance = threat >= 1.0D ? Mth.clamp(0.22D + (threat - 1.0D) * 0.14D, 0.22D, 0.43D) : 0.11D;
			double blockChance = threat >= 1.0D ? 0.16D : 0.31D;
			if (threat >= 1.0D) {
				if (roll < dodgeChance)
					return dodge(event, hunter, attacker, 0.82D);
				return roll < dodgeChance + blockChance && block(event, hunter, 0.55F);
			}
			if (roll < blockChance)
				return block(event, hunter, 0.55F);
			return roll < blockChance + dodgeChance && dodge(event, hunter, attacker, 0.76D);
		}

		double dodgeChance = threat >= 1.35D ? Mth.clamp(0.14D + (threat - 1.35D) * 0.10D, 0.14D, 0.27D) : 0.06D;
		double blockChance = threat >= 1.35D ? 0.34D : 0.43D;
		if (threat >= 1.35D) {
			if (roll < dodgeChance)
				return dodge(event, hunter, attacker, 0.68D);
			return roll < dodgeChance + blockChance && block(event, hunter, 0.42F);
		}
		if (roll < blockChance)
			return block(event, hunter, 0.42F);
		return roll < blockChance + dodgeChance && dodge(event, hunter, attacker, 0.62D);
	}

	private static boolean dodge(LivingHurtEvent event, LivingEntity hunter, LivingEntity attacker, double strength) {
		event.setCanceled(true);
		Vec3 away = hunter.position().subtract(attacker.position());
		away = new Vec3(away.x, 0.0D, away.z);
		if (away.lengthSqr() < 1.0E-5D)
			away = new Vec3(hunter.getLookAngle().x, 0.0D, hunter.getLookAngle().z).scale(-1.0D);
		away = away.normalize();
		Vec3 side = new Vec3(-away.z, 0.0D, away.x).scale(hunter.getRandom().nextBoolean() ? 1.0D : -1.0D);
		Vec3 movement = away.scale(0.62D).add(side.scale(0.78D)).normalize().scale(strength);
		hunter.setDeltaMovement(movement.x, Math.max(0.10D, hunter.getDeltaMovement().y), movement.z);
		hunter.hasImpulse = true;
		if (hunter instanceof PathfinderMob pathfinder)
			pathfinder.getNavigation().stop();
		particles(hunter, ParticleTypes.POOF, 10, 0.04D);
		hunter.level().playSound(null, hunter.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.55F, 1.25F);
		return true;
	}

	private static boolean block(LivingHurtEvent event, LivingEntity hunter, float damageMultiplier) {
		event.setAmount(Math.max(0.5F, event.getAmount() * damageMultiplier));
		hunter.setDeltaMovement(hunter.getDeltaMovement().multiply(0.35D, 1.0D, 0.35D));
		particles(hunter, ParticleTypes.CRIT, 9, 0.02D);
		hunter.level().playSound(null, hunter.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.7F, 0.9F);
		return true;
	}

	private static void ensureFixedProfile(PathfinderMob hunter) {
		CompoundTag data = hunter.getPersistentData();
		if (data.getInt(PROFILE_VERSION) == CURRENT_PROFILE_VERSION)
			return;
		Profile profile = profile(hunter);
		if (profile == null)
			return;
		float healthRatio = hunter.getMaxHealth() > 0 ? Mth.clamp(hunter.getHealth() / hunter.getMaxHealth(), 0.0F, 1.0F) : 1.0F;
		setBase(hunter, Attributes.MAX_HEALTH, profile.health());
		setBase(hunter, Attributes.ARMOR, profile.armor());
		setBase(hunter, Attributes.ATTACK_DAMAGE, profile.damage());
		setBase(hunter, Attributes.MOVEMENT_SPEED, profile.speed());
		setBase(hunter, Attributes.FOLLOW_RANGE, 64.0D);
		setBase(hunter, Attributes.KNOCKBACK_RESISTANCE, profile.knockbackResistance());
		setBase(hunter, Attributes.ATTACK_KNOCKBACK, profile.attackKnockback());
		hunter.setHealth(Math.max(1.0F, (float) (profile.health() * healthRatio)));
		hunter.setCustomName(profile.name());
		hunter.setCustomNameVisible(true);
		data.remove("Level");
		data.remove("SLRLevelStatMultiplier");
		if (hunter instanceof ChoijongEntity)
			data.putDouble("int", 95.0D);
		data.putInt(PROFILE_VERSION, CURRENT_PROFILE_VERSION);
	}

	private static Profile profile(PathfinderMob hunter) {
		if (hunter instanceof ChoijongEntity)
			return new Profile(240.0D, 8.0D, 12.0D, 0.36D, 0.20D, 0.0D, net.minecraft.network.chat.Component.literal("Choi Jong-In"));
		if (hunter instanceof ChaHaeInEntity)
			return new Profile(300.0D, 22.0D, 30.0D, 0.42D, 0.35D, 0.85D, net.minecraft.network.chat.Component.literal("Cha Hae-In"));
		if (hunter instanceof BaekYoonhoEntity)
			return new Profile(340.0D, 34.0D, 27.0D, 0.41D, 0.55D, 1.0D, net.minecraft.network.chat.Component.literal("Baek Yoonho"));
		return null;
	}

	private static void setBase(LivingEntity entity, Attribute attribute, double value) {
		if (entity.getAttribute(attribute) != null)
			entity.getAttribute(attribute).setBaseValue(value);
	}

	private static double combatPower(LivingEntity entity) {
		return entity.getMaxHealth() * 0.08D + entity.getArmorValue() * 1.2D
				+ entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 4.0D;
	}

	private static LivingEntity resolveAttacker(LivingEntity victim, Entity source, Entity directSource) {
		Entity resolved = source != null ? source : directSource;
		if (resolved instanceof Projectile projectile && projectile.getOwner() != null)
			resolved = projectile.getOwner();
		if (resolved instanceof TamableAnimal tame && tame.isTame() && tame.getOwner() != null)
			resolved = tame.getOwner();
		UUID shadowOwner = resolved == null ? null : ShadowMonarchManager.getShadowOwnerUUID(resolved);
		if (shadowOwner != null && victim.level() instanceof ServerLevel serverLevel) {
			Player owner = serverLevel.getPlayerByUUID(shadowOwner);
			if (owner != null)
				resolved = owner;
		}
		return resolved instanceof LivingEntity living ? living : null;
	}

	private static boolean canRetaliateAgainst(LivingEntity hunter, LivingEntity target) {
		if (target == hunter || !target.isAlive() || isNamedHunter(target))
			return false;
		return !(target instanceof Player player) || (!player.isCreative() && !player.isSpectator());
	}

	private static void clearRetaliation(CompoundTag data) {
		data.remove(RETALIATION_TARGET);
		data.remove(RETALIATION_UNTIL);
	}

	private static void particles(LivingEntity entity, net.minecraft.core.particles.ParticleOptions type, int count, double speed) {
		if (entity.level() instanceof ServerLevel level)
			level.sendParticles(type, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(), count, 0.4D, 0.55D, 0.4D, speed);
	}

	private record Profile(double health, double armor, double damage, double speed, double knockbackResistance,
			double attackKnockback, net.minecraft.network.chat.Component name) {
	}
}
