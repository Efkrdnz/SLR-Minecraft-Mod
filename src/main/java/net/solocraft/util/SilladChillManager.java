package net.solocraft.util;

import net.solocraft.entity.SilladBossEntity;
import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The ambient cold of Sillad's arena.
 *
 * <p>Before this, the Monarch of Frost's domain froze the ground and nothing
 * else: a player who never got hit never got cold, and the fight read as "boss
 * with ice-themed attacks" rather than a place that is actively trying to kill
 * you by being cold. This manager makes the arena itself the second opponent.
 *
 * <p>Chill lives in persistent data so it survives a relog mid-fight, while the
 * <em>stage</em> is mirrored onto {@link net.solocraft.potion.ChillMobEffect} so
 * vanilla handles syncing it to the client for the screen frost. The numbers all
 * come from {@link FrostChillRules}, which is dependency-free and tested.
 */
public final class SilladChillManager {
	/** Chill is re-evaluated on this cadence, not every tick. */
	public static final int INTERVAL = 10;

	private static final String CHILL_TAG = "slr_sillad_chill";
	private static final String GRACE_TAG = "slr_sillad_chill_grace";
	private static final int WARMTH_SCAN_RADIUS = 3;
	private static final int WARMTH_SCAN_HEIGHT = 1;
	/** Warmth is capped so a ring of torches cannot trivialise the domain. */
	private static final int MAX_WARMTH_SOURCES = 3;
	private static final double SCAN_MARGIN = 12.0D;

	private SilladChillManager() {
	}

	/** Drives every living thing in and around the domain. Called from the boss tick. */
	public static void tick(SilladBossEntity sillad) {
		if (sillad == null || !sillad.isAlive()
				|| !(sillad.level() instanceof ServerLevel level))
			return;
		if (Math.floorMod(sillad.tickCount + sillad.getId(), INTERVAL) != 0)
			return;

		BlockPos center = SilladFrozenDomainManager.center(sillad);
		double radius = SilladFrozenDomainManager.radius(sillad);
		Vec3 heart = Vec3.atCenterOf(center);
		double scan = radius + SCAN_MARGIN;
		AABB box = new AABB(heart.x - scan, heart.y - scan, heart.z - scan,
				heart.x + scan, heart.y + scan, heart.z + scan);

		List<LivingEntity> occupants = level.getEntitiesOfClass(LivingEntity.class, box,
				candidate -> candidate.isAlive() && isChillable(candidate));
		for (LivingEntity occupant : occupants)
			tickOccupant(level, sillad, occupant, heart, radius);
	}

	/**
	 * Sillad and his own creations are not troubled by their own winter, and
	 * neither is anything that already ignores freezing.
	 */
	private static boolean isChillable(LivingEntity entity) {
		return !(entity instanceof SilladBossEntity)
				&& !entity.getType().is(net.minecraft.tags.EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES)
				&& !(entity instanceof Player player && (player.isCreative() || player.isSpectator()));
	}

	private static void tickOccupant(ServerLevel level, SilladBossEntity sillad,
			LivingEntity occupant, Vec3 heart, double radius) {
		double horizontal = Math.sqrt(
				Math.pow(occupant.getX() - heart.x, 2) + Math.pow(occupant.getZ() - heart.z, 2));
		boolean inside = horizontal <= radius;
		double chill = readChill(occupant);
		if (chill <= 0.0D && !inside)
			return;

		double delta = FrostChillRules.deltaPerSecond(sillad.getCombatPhase(),
				occupant.distanceTo(sillad), inside, standingOnFrost(level, occupant),
				warmthSources(level, occupant), occupant.isOnFire());
		// Immediately after freezing solid the cold cannot take hold again. It
		// still thaws during the grace, so the window is a chance to get out of
		// the domain rather than a free pass to keep standing in it.
		if (level.getGameTime() < occupant.getPersistentData().getLong(GRACE_TAG))
			delta = Math.min(delta, -FrostChillRules.THAW_PER_SECOND);
		double updated = FrostChillRules.advance(chill, delta, INTERVAL);

		FrostChillRules.Stage previous = FrostChillRules.stageFor(chill);
		FrostChillRules.Stage stage = FrostChillRules.stageFor(updated);

		if (stage == FrostChillRules.Stage.GLACIAL) {
			// Freezing solid hands off to the frostbite system that already owns
			// rooting, brittleness and the execution window, then drops back so
			// the player has something to fight out of rather than a hard lock.
			SilladBossCombatManager.forceGlacialBreak(sillad, occupant);
			updated = FrostChillRules.BREAK_RESET_TO;
			stage = FrostChillRules.stageFor(updated);
			occupant.getPersistentData().putLong(GRACE_TAG,
					level.getGameTime() + FrostChillRules.GLACIAL_GRACE_TICKS);
		}

		writeChill(occupant, updated);
		applyStage(occupant, stage);
		if (stage != previous)
			announce(occupant, stage, previous);
		emitAtmosphere(level, occupant, stage, updated);
	}

	private static void applyStage(LivingEntity occupant, FrostChillRules.Stage stage) {
		int amplifier = FrostChillRules.amplifierFor(stage);
		if (amplifier < 0) {
			occupant.removeEffect(SololevelingModMobEffects.CHILL);
			return;
		}
		// Refreshed rather than extended: the manager is the clock, and a long
		// duration would leave the marker behind after the player walked out.
		occupant.addEffect(new MobEffectInstance(SololevelingModMobEffects.CHILL,
				INTERVAL * 3, amplifier, false, false, true));
		switch (stage) {
			case NUMB -> {
				occupant.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
						INTERVAL * 3, 0, false, false, false));
				occupant.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,
						INTERVAL * 3, 0, false, false, false));
			}
			case FROSTBOUND -> {
				occupant.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
						INTERVAL * 3, 1, false, false, false));
				occupant.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,
						INTERVAL * 3, 1, false, false, false));
				occupant.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
						INTERVAL * 3, 0, false, false, false));
			}
			default -> {
			}
		}
	}

	/** Crossing a threshold is told to the player, in both directions. */
	private static void announce(LivingEntity occupant, FrostChillRules.Stage stage,
			FrostChillRules.Stage previous) {
		if (!(occupant instanceof ServerPlayer player))
			return;
		boolean worsening = stage.ordinal() > previous.ordinal();
		if (stage == FrostChillRules.Stage.CLEAR) {
			player.displayClientMessage(net.minecraft.network.chat.Component.literal(
					"§bThe cold loosens its grip."), true);
			return;
		}
		player.displayClientMessage(net.minecraft.network.chat.Component.literal(
				(worsening ? "§b" : "§3") + stage.displayName()), true);
		if (worsening)
			player.playNotifySound(SoundEvents.PLAYER_HURT_FREEZE,
					SoundSource.PLAYERS, 0.6F, 1.0F);
	}

	private static void emitAtmosphere(ServerLevel level, LivingEntity occupant,
			FrostChillRules.Stage stage, double chill) {
		if (stage == FrostChillRules.Stage.CLEAR)
			return;
		double intensity = FrostChillRules.intensity(chill);
		// Breath fog: the cheapest, most readable signal that a body is cold.
		level.sendParticles(ParticleTypes.SNOWFLAKE,
				occupant.getX(), occupant.getEyeY() - 0.15D, occupant.getZ(),
				1 + (int) (intensity * 3.0D), 0.12D, 0.05D, 0.12D, 0.006D);
		if (stage.ordinal() < FrostChillRules.Stage.FROSTBOUND.ordinal())
			return;
		// Ice forming on the body itself once the cold has actually taken hold.
		level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
				occupant.getX(), occupant.getY() + occupant.getBbHeight() * 0.5D,
				occupant.getZ(), 3, 0.3D, 0.4D, 0.3D, 0.01D);
		if (occupant instanceof ServerPlayer player
				&& Math.floorMod(player.tickCount, INTERVAL * 4) == 0)
			player.playNotifySound(SoundEvents.POWDER_SNOW_STEP,
					SoundSource.AMBIENT, 0.7F, 0.55F);
	}

	/** Sillad's own ice underfoot bites harder than plain ground. */
	private static boolean standingOnFrost(ServerLevel level, LivingEntity occupant) {
		BlockState below = level.getBlockState(occupant.blockPosition().below());
		return below.is(Blocks.ICE) || below.is(Blocks.PACKED_ICE)
				|| below.is(Blocks.BLUE_ICE) || below.is(Blocks.FROSTED_ICE)
				|| below.is(Blocks.SNOW_BLOCK) || below.is(Blocks.POWDER_SNOW);
	}

	/**
	 * Counts nearby heat. This is the counterplay: the domain is survivable if
	 * the party brings warmth into it, which turns the arena into something to
	 * manage rather than a timer to outlast.
	 */
	private static int warmthSources(ServerLevel level, LivingEntity occupant) {
		BlockPos origin = occupant.blockPosition();
		int found = 0;
		for (int dx = -WARMTH_SCAN_RADIUS; dx <= WARMTH_SCAN_RADIUS; dx++) {
			for (int dz = -WARMTH_SCAN_RADIUS; dz <= WARMTH_SCAN_RADIUS; dz++) {
				for (int dy = -WARMTH_SCAN_HEIGHT; dy <= WARMTH_SCAN_HEIGHT; dy++) {
					if (isWarm(level.getBlockState(origin.offset(dx, dy, dz)))
							&& ++found >= MAX_WARMTH_SOURCES)
						return MAX_WARMTH_SOURCES;
				}
			}
		}
		return found;
	}

	private static boolean isWarm(BlockState state) {
		if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
				|| state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.TORCH)
				|| state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_TORCH)
				|| state.is(Blocks.SOUL_WALL_TORCH) || state.is(Blocks.LANTERN)
				|| state.is(Blocks.SOUL_LANTERN) || state.is(Blocks.GLOWSTONE)
				|| state.is(Blocks.SHROOMLIGHT))
			return true;
		// Campfires and furnaces only count while actually burning.
		if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
				|| state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE)
				|| state.is(Blocks.SMOKER))
			return state.hasProperty(BlockStateProperties.LIT)
					&& state.getValue(BlockStateProperties.LIT);
		return false;
	}

	public static double readChill(LivingEntity entity) {
		return entity == null ? 0.0D
				: FrostChillRules.clamp(entity.getPersistentData().getDouble(CHILL_TAG));
	}

	private static void writeChill(LivingEntity entity, double chill) {
		if (chill <= 0.0D)
			entity.getPersistentData().remove(CHILL_TAG);
		else
			entity.getPersistentData().putDouble(CHILL_TAG, FrostChillRules.clamp(chill));
	}

	/** Clears the cold from everything when the encounter ends. */
	public static void clear(LivingEntity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().remove(CHILL_TAG);
		entity.getPersistentData().remove(GRACE_TAG);
		entity.removeEffect(SololevelingModMobEffects.CHILL);
	}
}
