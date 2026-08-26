package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.List;

/**
 * Curated signature arts borrowed from the currently appointed Grand Marshal.
 * Every variant uses the same cooldown key, so changing the command seat cannot
 * reset its recharge.
 */
public final class GrandMarshalAbilityManager {
	public static final String COOLDOWN_KEY = "grand_marshal_authority";

	private static final DustParticleOptions SHADOW_PURPLE =
			new DustParticleOptions(new Vector3f(0.48F, 0.10F, 0.92F), 1.2F);
	private static final DustParticleOptions CRIMSON =
			new DustParticleOptions(new Vector3f(0.95F, 0.05F, 0.12F), 1.25F);
	private static final DustParticleOptions SKY_BLUE =
			new DustParticleOptions(new Vector3f(0.25F, 0.78F, 1.0F), 1.0F);

	private GrandMarshalAbilityManager() {
	}

	public static boolean cast(Entity caster) {
		if (!(caster instanceof ServerPlayer player))
			return false;
		if (!DeveloperModeManager.isEnabled(player)) {
			player.displayClientMessage(Component.literal(
					"WIP (Work in progress)")
					.withStyle(net.minecraft.ChatFormatting.RED,
							net.minecraft.ChatFormatting.BOLD), true);
			return false;
		}
		ShadowMonarchManager.GrandMarshalCommander commander =
				ShadowMonarchManager.activeGrandMarshal(player);
		if (commander == null) {
			player.displayClientMessage(Component.literal(
					"\u00A7cYour Grand Marshal must be summoned and alive."),
					true);
			return false;
		}
		AbilitySpec spec = specification(commander.type());
		if (spec == null)
			return false;
		if (CooldownManager.isOnCooldown(player, COOLDOWN_KEY)) {
			player.displayClientMessage(Component.literal(
					"\u00A7cGrand Marshal Authority is recharging. \u00A77("
							+ CooldownManager.getRemainingSeconds(player,
									COOLDOWN_KEY)
							+ "s)"), true);
			return false;
		}
		if (!consumeMana(player, spec.manaCost()))
			return false;

		linkCommander(player, commander.entity());
		switch (commander.type()) {
			case "igris" -> castCrimsonCross(player, commander.level());
			case "beru" -> castKingsRestoration(player, commander);
			case "tusk" -> castGravitationalRuin(player, commander.level());
			case "kamish" -> castDragonsDread(player, commander.level());
			case "kaisel" -> castSkyRend(player, commander.level());
			default -> {
				return false;
			}
		}
		CooldownManager.set(player, COOLDOWN_KEY, spec.cooldownTicks());
		CooldownManager.set(player, "mana_refresh", 40);
		player.displayClientMessage(Component.literal("\u00A75"
				+ commander.name() + "\u00A77 \u2014 \u00A7d"
				+ ShadowMonarchManager.grandMarshalSignatureName(
						commander.type())), true);
		return true;
	}

	private static void castCrimsonCross(ServerPlayer player, int shadowLevel) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position().add(0.0D,
				player.getBbHeight() * 0.55D, 0.0D);
		Vec3 forward = horizontalLook(player);
		float damage = (float) (14.0D
				+ TemporaryStatBonusManager.effectiveStrength(player) / 10.0D
				+ shadowPower(shadowLevel));
		AABB search = player.getBoundingBox().inflate(11.0D, 4.0D, 11.0D);
		for (LivingEntity target : targets(player, search)) {
			Vec3 offset = target.getBoundingBox().getCenter().subtract(origin);
			double along = offset.dot(forward);
			double perpendicular = offset.subtract(forward.scale(along))
					.length();
			if (along > 0.0D && along <= 10.5D
					&& perpendicular <= 2.25D + target.getBbWidth() * 0.5D) {
				dealDamage(player, target, damage);
				target.knockback(0.45D, -forward.x, -forward.z);
			}
		}
		AbilityDestructionManager.fissure(player,
				AbilityDestructionManager.Profile.LIU_SWORD_CUT,
				player.position(), forward, 10.5D,
				TemporaryStatBonusManager.effectiveStrength(player)
						+ shadowPower(shadowLevel) * 10.0D, shadowLevel >= 4);
		for (int i = 1; i <= 26; i++) {
			double distance = i * 0.4D;
			Vec3 center = origin.add(forward.scale(distance));
			Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
			double cross = (i - 13) * 0.075D;
			level.sendParticles(i % 2 == 0 ? CRIMSON : SHADOW_PURPLE,
					center.x + right.x * cross, center.y + cross,
					center.z + right.z * cross, 1, 0.02D, 0.02D, 0.02D,
					0.0D);
			level.sendParticles(i % 2 == 0 ? SHADOW_PURPLE : CRIMSON,
					center.x - right.x * cross, center.y + cross,
					center.z - right.z * cross, 1, 0.02D, 0.02D, 0.02D,
					0.0D);
		}
		level.playSound(null, BlockPos.containing(player.position()),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F,
				0.65F);
	}

	private static void castKingsRestoration(ServerPlayer player,
			ShadowMonarchManager.GrandMarshalCommander commander) {
		ServerLevel level = player.serverLevel();
		float playerHealing = player.getMaxHealth() * 0.28F
				+ (float) Math.min(18.0D, shadowPower(commander.level()));
		player.heal(playerHealing);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100,
				1, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 180, 1,
				false, true));
		commander.entity().heal(commander.entity().getMaxHealth() * 0.4F);
		for (LivingEntity living : level.getEntitiesOfClass(
				LivingEntity.class, player.getBoundingBox().inflate(16.0D),
				target -> target.isAlive()
						&& ShadowMonarchManager.isOwnedShadow(target,
								player))) {
			living.heal(living.getMaxHealth() * 0.22F);
		}
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(),
				player.getY() + 1.0D, player.getZ(), 55, 1.3D, 1.2D, 1.3D,
				0.05D);
		level.sendParticles(ParticleTypes.HEART, player.getX(),
				player.getY() + 1.25D, player.getZ(), 12, 0.8D, 0.7D, 0.8D,
				0.03D);
		level.playSound(null, BlockPos.containing(player.position()),
				SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F,
				0.8F);
	}

	private static void castGravitationalRuin(ServerPlayer player,
			int shadowLevel) {
		ServerLevel level = player.serverLevel();
		Vec3 center = aimedCenter(player, 9.0D);
		double radius = 5.5D;
		float damage = (float) (18.0D
				+ TemporaryStatBonusManager.effectiveIntelligence(player)
						/ 9.0D
				+ shadowPower(shadowLevel) * 1.15D);
		for (LivingEntity target : targets(player,
				new AABB(center, center).inflate(radius))) {
			if (target.position().distanceToSqr(center) > radius * radius)
				continue;
			dealDamage(player, target, damage);
			Vec3 pull = center.subtract(target.position());
			Vec3 horizontal = new Vec3(pull.x, 0.0D, pull.z);
			if (horizontal.lengthSqr() > 0.01D)
				horizontal = horizontal.normalize().scale(0.45D);
			target.setDeltaMovement(target.getDeltaMovement()
					.add(horizontal.x, 0.55D, horizontal.z));
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
					80, 1, false, true));
			 target.hurtMarked = true;
		}
		AbilityDestructionManager.impact(player,
				AbilityDestructionManager.Profile.GRAND_MARSHAL_GRAVITY, center,
				TemporaryStatBonusManager.effectiveIntelligence(player)
						+ shadowPower(shadowLevel) * 10.35D, shadowLevel >= 4);
		drawRing(level, center.add(0.0D, 0.15D, 0.0D), radius,
				SHADOW_PURPLE, 48);
		drawRing(level, center.add(0.0D, 0.45D, 0.0D), radius * 0.62D,
				SHADOW_PURPLE, 32);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x,
				center.y + 0.7D, center.z, 70, 2.2D, 0.9D, 2.2D, 0.12D);
		level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.5D,
				center.z, 4, 1.0D, 0.5D, 1.0D, 0.0D);
		level.playSound(null, BlockPos.containing(center),
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.15F,
				0.7F);
	}

	private static void castDragonsDread(ServerPlayer player,
			int shadowLevel) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.getEyePosition().add(0.0D, -0.25D, 0.0D);
		Vec3 forward = player.getLookAngle().normalize();
		float damage = (float) (24.0D
				+ TemporaryStatBonusManager.effectiveStrength(player) / 16.0D
				+ TemporaryStatBonusManager.effectiveIntelligence(player)
						/ 11.0D
				+ shadowPower(shadowLevel) * 1.35D);
		for (LivingEntity target : targets(player,
				player.getBoundingBox().inflate(14.0D, 8.0D, 14.0D))) {
			Vec3 offset = target.getBoundingBox().getCenter().subtract(origin);
			double distance = offset.length();
			if (distance <= 0.1D || distance > 13.0D)
				continue;
			double alignment = offset.normalize().dot(forward);
			if (alignment < 0.68D)
				continue;
			dealDamage(player, target, damage);
			target.igniteForSeconds(5);
			 target.knockback(0.65D, -forward.x, -forward.z);
		}
		AbilityDestructionManager.line(player,
				AbilityDestructionManager.Profile.GRAND_MARSHAL_DREAD,
				origin, origin.add(forward.scale(13.0D)),
				TemporaryStatBonusManager.effectiveIntelligence(player)
						+ TemporaryStatBonusManager.effectiveStrength(player) * 0.6875D
						+ shadowPower(shadowLevel) * 14.85D,
				shadowLevel >= 4);
		for (int step = 1; step <= 18; step++) {
			double distance = step * 0.72D;
			Vec3 point = origin.add(forward.scale(distance));
			double spread = 0.08D + distance * 0.055D;
			level.sendParticles(step % 3 == 0
							? ParticleTypes.SOUL_FIRE_FLAME
							: ParticleTypes.FLAME,
					point.x, point.y, point.z, 5, spread, spread, spread,
					0.04D);
		}
		level.playSound(null, BlockPos.containing(player.position()),
				SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.1F,
				0.82F);
	}

	private static void castSkyRend(ServerPlayer player, int shadowLevel) {
		ServerLevel level = player.serverLevel();
		Vec3 center = aimedCenter(player, 10.0D);
		double radius = 4.5D;
		float damage = (float) (16.0D
				+ TemporaryStatBonusManager.effectiveAgility(player) / 10.0D
				+ TemporaryStatBonusManager.effectiveIntelligence(player)
						/ 18.0D
				+ shadowPower(shadowLevel));
		for (LivingEntity target : targets(player,
				new AABB(center, center).inflate(radius, 5.0D, radius))) {
			if (target.position().distanceToSqr(center) > radius * radius * 1.3D)
				continue;
			dealDamage(player, target, damage);
			target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.75D,
					0.0D));
			target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0,
					false, true));
			 target.hurtMarked = true;
		}
		AbilityDestructionManager.impact(player,
				AbilityDestructionManager.Profile.GRAND_MARSHAL_SKY_REND, center,
				TemporaryStatBonusManager.effectiveAgility(player)
						+ TemporaryStatBonusManager.effectiveIntelligence(player) * 0.556D
						+ shadowPower(shadowLevel) * 10.0D,
				shadowLevel >= 4);
		for (int i = 0; i < 34; i++) {
			double y = center.y + 7.0D - i * 0.23D;
			double angle = i * 1.7D;
			double radiusAtPoint = 0.25D + (i % 5) * 0.08D;
			level.sendParticles(i % 3 == 0 ? SKY_BLUE
							: ParticleTypes.ELECTRIC_SPARK,
					center.x + Math.cos(angle) * radiusAtPoint, y,
					center.z + Math.sin(angle) * radiusAtPoint, 1, 0.03D,
					0.03D, 0.03D, 0.02D);
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x,
				center.y + 0.5D, center.z, 55, 1.8D, 1.0D, 1.8D, 0.18D);
		level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.2D,
				center.z, 28, 1.4D, 0.35D, 1.4D, 0.08D);
		level.playSound(null, BlockPos.containing(center),
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.85F,
				1.35F);
	}

	private static List<LivingEntity> targets(ServerPlayer player, AABB area) {
		return player.serverLevel().getEntitiesOfClass(LivingEntity.class,
				area, target -> validTarget(player, target));
	}

	private static boolean validTarget(ServerPlayer player,
			LivingEntity target) {
		if (target == null || target == player || !target.isAlive()
				|| !target.isAttackable() || target.isInvulnerable()
				|| target instanceof ArmorStand)
			return false;
		if (player.isAlliedTo(target) || target.isAlliedTo(player)
				|| ShadowMonarchManager.isOwnedShadow(target, player))
			return false;
		if (target instanceof TamableAnimal tame
				&& player.getUUID().equals(tame.getOwnerUUID()))
			return false;
		if (target instanceof Player other)
			return !other.isCreative() && !other.isSpectator()
					&& player.canHarmPlayer(other);
		return true;
	}

	private static boolean dealDamage(ServerPlayer player,
			LivingEntity target, float damage) {
		if (!validTarget(player, target))
			return false;
		target.invulnerableTime = 0;
		return target.hurt(player.damageSources().playerAttack(player),
				Math.max(0.5F, damage));
	}

	private static boolean consumeMana(ServerPlayer player, int amount) {
		if (player.isCreative())
			return true;
		SololevelingModVariables.PlayerVariables variables =
				player.getCapability(
						SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY,
						null)
						.orElse(new SololevelingModVariables.PlayerVariables());
		if (variables.MP < amount) {
			player.displayClientMessage(Component.literal(
					"\u00A7cNot enough MP! \u00A77(" + amount
							+ " required)"), true);
			return false;
		}
		player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.MP = Math.max(0.0D, capability.MP - amount);
					capability.syncPlayerVariables(player);
				});
		return true;
	}

	private static void linkCommander(ServerPlayer player,
			LivingEntity commander) {
		ServerLevel level = player.serverLevel();
		Vec3 start = commander.getEyePosition();
		Vec3 end = player.getEyePosition();
		Vec3 route = end.subtract(start);
		int steps = Math.max(5, (int) Math.ceil(route.length() * 2.0D));
		for (int i = 0; i <= steps; i++) {
			Vec3 point = start.add(route.scale(i / (double) steps));
			level.sendParticles(i % 3 == 0 ? ParticleTypes.SOUL_FIRE_FLAME
							: SHADOW_PURPLE,
					point.x, point.y, point.z, 1, 0.03D, 0.03D, 0.03D,
					0.0D);
		}
	}

	private static Vec3 horizontalLook(ServerPlayer player) {
		Vec3 look = player.getLookAngle();
		Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
		return horizontal.lengthSqr() < 0.001D
				? new Vec3(0.0D, 0.0D, 1.0D)
				: horizontal.normalize();
	}

	private static Vec3 aimedCenter(ServerPlayer player, double distance) {
		Vec3 look = player.getLookAngle();
		return player.getEyePosition().add(look.scale(distance))
				.add(0.0D, -1.0D, 0.0D);
	}

	private static double shadowPower(int shadowLevel) {
		return Math.min(200, Math.max(1, shadowLevel)) * 0.12D;
	}

	private static void drawRing(ServerLevel level, Vec3 center,
			double radius, DustParticleOptions particle, int points) {
		for (int i = 0; i < points; i++) {
			double angle = Math.PI * 2.0D * i / points;
			level.sendParticles(particle,
					center.x + Math.cos(angle) * radius, center.y,
					center.z + Math.sin(angle) * radius, 1, 0.0D, 0.0D,
					0.0D, 0.0D);
		}
	}

	private static AbilitySpec specification(String type) {
		return switch (type) {
			case "igris" -> new AbilitySpec(280, 160);
			case "beru" -> new AbilitySpec(320, 280);
			case "tusk" -> new AbilitySpec(460, 240);
			case "kamish" -> new AbilitySpec(600, 360);
			case "kaisel" -> new AbilitySpec(340, 200);
			default -> null;
		};
	}

	private record AbilitySpec(int manaCost, int cooldownTicks) {
	}
}
