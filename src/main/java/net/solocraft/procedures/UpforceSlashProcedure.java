package net.solocraft.procedures;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.core.particles.ParticleTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.solocraft.util.AbilityDestructionManager;
import net.solocraft.util.CooldownManager;
import net.solocraft.util.MageCombatHelper;
import net.solocraft.util.ManaRules;
import net.solocraft.util.TemporaryStatBonusManager;

/**
 * Ground Slam — bounded radial impact.
 *
 * <p>The previous implementation scaled damage by {@code 5 / distance} with no
 * clamp on the divisor, so a target standing at the epicentre took roughly a
 * fiftyfold multiplier. It also queried {@code Entity.class} with a
 * {@code e -> true} filter across a 32-block-diameter box, hitting party
 * members and neutral players alike. Both are fixed here.</p>
 */
public class UpforceSlashProcedure {
	/** Down from an effective 16-block radius. */
	private static final double RADIUS = 5.0D;
	private static final int TARGET_CAP = 8;
	/** Falloff floor at the rim; the curve never exceeds 1.0 at the centre. */
	private static final double RIM_FALLOFF = 0.35D;

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		int cost = ManaRules.cost(entity, ManaRules.Band.MEDIUM);
		if (!ManaRules.canAfford(entity, cost)) {
			if (entity instanceof Player player && !player.level().isClientSide())
				player.displayClientMessage(Component.literal("Not enough MP!"), true);
			return;
		}
		if (CooldownManager.isOnCooldown(entity, "Ground Slam"))
			return;
		if (!ManaRules.spend(entity, cost))
			return;

		CooldownManager.set(entity, "Ground Slam", 400);
		CooldownManager.set(entity, "mana_refresh", 50);
		playImpact(world, x, y, z);
		if (world instanceof ServerLevel level)
			level.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.2D, z, 1,
					0.0D, 0.0D, 0.0D, 0.0D);
		if (entity instanceof ServerPlayer player)
			AbilityDestructionManager.impact(player,
					AbilityDestructionManager.Profile.FIGHTER_SLAM,
					new Vec3(x, y, z),
					TemporaryStatBonusManager.effectiveStrength(player), false);

		applyImpact(world, new Vec3(x, y, z), entity);
	}

	private static void applyImpact(LevelAccessor world, Vec3 center, Entity entity) {
		double strength = TemporaryStatBonusManager.effectiveStrength(entity);
		double budget = 6.0D + strength * 0.35D;
		DamageSource source = new DamageSource(world.registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,
						ResourceLocation.parse("sololeveling:fighter"))), entity);

		List<LivingEntity> targets = new ArrayList<>(world.getEntitiesOfClass(
				LivingEntity.class, new AABB(center, center).inflate(RADIUS),
				candidate -> MageCombatHelper.isValidTarget(entity, candidate)));
		targets.sort(Comparator.comparingDouble(target -> target.distanceToSqr(center)));

		int hits = 0;
		for (LivingEntity target : targets) {
			if (hits >= TARGET_CAP)
				break;
			double distance = Math.sqrt(target.distanceToSqr(center));
			// Bounded falloff: full damage at the epicentre easing to the rim
			// floor. Never inverted, never unbounded.
			double falloff = Mth.clamp(
					1.0D - (1.0D - RIM_FALLOFF) * (distance / RADIUS),
					RIM_FALLOFF, 1.0D);
			target.hurt(source, (float) (budget * falloff));
			hits++;
		}
	}

	private static void playImpact(LevelAccessor world, double x, double y, double z) {
		if (!(world instanceof Level level) || level.isClientSide())
			return;
		level.playSound(null, BlockPos.containing(x, y, z),
				BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
				SoundSource.NEUTRAL, 0.5F, 1.0F);
		level.playSound(null, BlockPos.containing(x, y, z),
				BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.explode")),
				SoundSource.NEUTRAL, 0.5F, 1.0F);
	}
}
