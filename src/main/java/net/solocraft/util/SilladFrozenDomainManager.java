package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.SilladBossEntity;

import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bounded, server-authoritative terrain control for the Monarch of Frost.
 *
 * <p>The frozen terrain is intentionally permanent. Work is spread across an
 * outward annulus and a small moving wake so the arena visibly succumbs to
 * Sillad without loading chunks or creating a single-tick block-update spike.
 * Protection mods can cancel the normal Forge placement event or extend the
 * {@code sololeveling:sillad_domain_immune} block tag.</p>
 */
public final class SilladFrozenDomainManager {
	private static final int MAX_RADIUS = SilladBossRules.frozenDomainRadius(
			SilladBossRules.PHASE_THREE);
	private static final int WAKE_RADIUS = 5;
	private static final int GLOBAL_INSPECTION_BUDGET = 384;
	private static final int MAIN_COLUMN_BUDGET = 40;
	private static final int WAKE_COLUMN_BUDGET = 8;
	private static final int MAX_PARTICLE_MUTATIONS = 10;
	private static final TagKey<Block> DOMAIN_IMMUNE = TagKey.create(
			Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
					"sillad_domain_immune"));
	private static final List<ColumnOffset> OFFSETS = createOffsets(MAX_RADIUS);
	private static final List<ColumnOffset> WAKE_OFFSETS = createOffsets(
			WAKE_RADIUS);
	private static final Map<UUID, DomainState> STATES = new HashMap<>();

	private static long budgetTick = Long.MIN_VALUE;
	private static int globalInspections;
	private static int globalMutations;

	private SilladFrozenDomainManager() {
	}

	public static void tick(SilladBossEntity sillad) {
		if (sillad == null || !sillad.isAlive()
				|| !(sillad.level() instanceof ServerLevel level)
				|| !EventHooks.canEntityGrief(level, sillad))
			return;
		refreshGlobalBudget(level.getGameTime());
		if (globalInspections <= 0 || globalMutations <= 0)
			return;

		DomainState state = STATES.computeIfAbsent(sillad.getUUID(), ignored ->
				new DomainState(sillad.getEncounterHome(), sillad.blockPosition()));
		state.requestedRadius = Math.max(state.requestedRadius,
				SilladBossRules.frozenDomainRadius(sillad.getCombatPhase()));
		BlockPos encounterHome = sillad.getEncounterHome();
		if (!sameHorizontal(state.mainCenter, encounterHome)
				|| Math.abs(state.mainCenter.getY() - encounterHome.getY()) > 2) {
			state.mainCenter = encounterHome.immutable();
			state.mainCursor = 0;
		}

		BlockPos current = sillad.blockPosition();
		if (horizontalDistanceSqr(state.wakeCenter, current) >= 9
				|| Math.abs(state.wakeCenter.getY() - current.getY()) > 2) {
			state.wakeCenter = current.immutable();
			state.wakeCursor = 0;
		}

		WorkBudget work = new WorkBudget(
				Math.min(GLOBAL_INSPECTION_BUDGET, globalInspections),
				Math.min(SilladBossRules.FROZEN_DOMAIN_COLUMN_BUDGET,
						globalMutations));
		int phase = Math.max(sillad.getCombatPhase(),
				phaseForRequestedRadius(state.requestedRadius));
		processSweep(level, sillad, state, false, phase,
				MAIN_COLUMN_BUDGET, work);
		processSweep(level, sillad, state, true, phase,
				WAKE_COLUMN_BUDGET, work);
		globalInspections -= work.usedInspections;
		globalMutations -= work.usedMutations;

		if (work.usedMutations > 0
				&& Math.floorMod(sillad.tickCount + sillad.getId(), 8) == 0)
			level.playSound(null, sillad.blockPosition(),
					SoundEvents.GLASS_PLACE, SoundSource.HOSTILE,
					0.55F, 0.62F);
		if (sillad.tickCount % 10 == 0 && state.mainCursor > 0) {
			ColumnOffset edge = OFFSETS.get(Math.min(state.mainCursor - 1,
					OFFSETS.size() - 1));
			double radius = Math.min(state.requestedRadius,
					Math.sqrt(edge.distanceSquared));
			spawnRing(level, Vec3.atBottomCenterOf(state.mainCenter)
					.add(0.0D, 0.16D, 0.0D), Math.max(1.0D, radius));
		}
	}

	/**
	 * Where the domain is centred, or the Monarch himself before any sweep has
	 * run. The ambient chill measures containment against this rather than
	 * against Sillad's live position, so backing away from him is not the same
	 * as leaving his domain.
	 */
	public static BlockPos center(SilladBossEntity sillad) {
		if (sillad == null)
			return BlockPos.ZERO;
		DomainState state = STATES.get(sillad.getUUID());
		return state == null ? sillad.getEncounterHome() : state.mainCenter;
	}

	/** Radius the domain has claimed, in blocks. */
	public static int radius(SilladBossEntity sillad) {
		if (sillad == null)
			return 0;
		DomainState state = STATES.get(sillad.getUUID());
		return state == null
				? SilladBossRules.frozenDomainRadius(SilladBossRules.PHASE_ONE)
				: state.requestedRadius;
	}

	/** Begins the next physical annulus while the phase-transition VFX expands. */
	public static void requestExpansion(SilladBossEntity sillad, int phase) {
		if (sillad == null || sillad.level().isClientSide())
			return;
		DomainState state = STATES.computeIfAbsent(sillad.getUUID(), ignored ->
				new DomainState(sillad.getEncounterHome(), sillad.blockPosition()));
		state.requestedRadius = Math.max(state.requestedRadius,
				SilladBossRules.frozenDomainRadius(phase));
	}

	/** Clears scheduling state only; the conquered terrain deliberately remains. */
	public static void cleanup(SilladBossEntity sillad) {
		if (sillad != null)
			STATES.remove(sillad.getUUID());
	}

	private static void processSweep(ServerLevel level,
			SilladBossEntity sillad, DomainState state, boolean wake, int phase,
			int columnBudget, WorkBudget work) {
		List<ColumnOffset> offsets = wake ? WAKE_OFFSETS : OFFSETS;
		BlockPos center = wake ? state.wakeCenter : state.mainCenter;
		int cursor = wake ? state.wakeCursor : state.mainCursor;
		int radius = wake ? WAKE_RADIUS : state.requestedRadius;
		int attempted = 0;
		while (cursor < offsets.size() && attempted < columnBudget
				&& work.canInspect() && work.canMutate()) {
			ColumnOffset offset = offsets.get(cursor);
			if (offset.distanceSquared > radius * radius)
				break;
			cursor++;
			attempted++;
			freezeColumn(level, sillad,
					center.offset(offset.x, 0, offset.z),
					wake ? 6 : 10, wake ? 6 : 8, phase, work);
		}
		if (wake)
			state.wakeCursor = cursor >= offsets.size() ? 0 : cursor;
		else
			state.mainCursor = cursor;
	}

	private static void freezeColumn(ServerLevel level,
			SilladBossEntity sillad, BlockPos column, int above, int below,
			int phase, WorkBudget work) {
		BlockPos decoration = null;
		for (int y = column.getY() + above;
				y >= column.getY() - below && work.canInspect(); y--) {
			BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
			work.inspect();
			if (!validPosition(level, pos))
				continue;
			BlockState state = level.getBlockState(pos);
			if (state.isAir())
				continue;
			boolean rawWater = state.is(Blocks.WATER)
					|| state.getFluidState().is(FluidTags.WATER)
							&& state.getBlock() == Blocks.WATER;
			boolean rawLava = state.is(Blocks.LAVA)
					|| state.getFluidState().is(FluidTags.LAVA)
							&& state.getBlock() == Blocks.LAVA;
			if (rawWater || rawLava || state.is(Blocks.POWDER_SNOW)) {
				replace(level, sillad, pos, state,
						frozenState(phase, pos, rawLava), work);
				return;
			}
			if (state.hasProperty(BlockStateProperties.WATERLOGGED)
					&& state.getValue(BlockStateProperties.WATERLOGGED))
				return;
			if (state.getCollisionShape(level, pos).isEmpty()) {
				if (state.canBeReplaced() && decoration == null)
					decoration = pos.immutable();
				continue;
			}
			if (!state.isCollisionShapeFullBlock(level, pos)
					|| !canConvert(level, sillad, pos, state))
				return;
			BlockState replacement = frozenState(phase, pos, false);
			if (replace(level, sillad, pos, state, replacement, work)
					&& decoration != null && work.canMutate()
					&& snowCell(pos, phase)
					&& level.getEntitiesOfClass(LivingEntity.class,
							new AABB(decoration), LivingEntity::isAlive).isEmpty()) {
				BlockState snow = Blocks.SNOW.defaultBlockState();
				if (snow.canSurvive(level, decoration))
					replace(level, sillad, decoration,
							level.getBlockState(decoration), snow, work);
			}
			return;
		}
	}

	private static boolean replace(ServerLevel level,
			SilladBossEntity sillad, BlockPos pos, BlockState previous,
			BlockState replacement, WorkBudget work) {
		if (!work.canMutate() || previous == replacement
				|| previous.equals(replacement)
				|| level.getBlockEntity(pos) != null
				|| previous.is(DOMAIN_IMMUNE)
				|| previous.is(BlockTags.WITHER_IMMUNE)
				|| previous.getDestroySpeed(level, pos) < 0.0F
				|| !CommonHooks.canEntityDestroy(level, pos, sillad))
			return false;
		BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level,
				pos);
		if (!level.setBlock(pos, replacement, 3))
			return false;
		if (EventHooks.onBlockPlace(sillad, snapshot, Direction.UP)) {
			snapshot.restore(2);
			return false;
		}
		work.mutate();
		if (work.usedMutations <= MAX_PARTICLE_MUTATIONS)
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
					replacement), pos.getX() + 0.5D, pos.getY() + 0.65D,
					pos.getZ() + 0.5D, 4, 0.30D, 0.24D, 0.30D, 0.045D);
		return true;
	}

	private static boolean canConvert(ServerLevel level,
			SilladBossEntity sillad, BlockPos pos, BlockState state) {
		if (state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
				|| state.is(DOMAIN_IMMUNE) || state.is(BlockTags.WITHER_IMMUNE)
				|| level.getBlockEntity(pos) != null
				|| state.getDestroySpeed(level, pos) < 0.0F)
			return false;
		return CommonHooks.canEntityDestroy(level, pos, sillad);
	}

	private static boolean validPosition(ServerLevel level, BlockPos pos) {
		return pos.getY() > level.getMinBuildHeight()
				&& pos.getY() < level.getMaxBuildHeight() - 1
				&& level.getWorldBorder().isWithinBounds(pos)
				&& level.hasChunkAt(pos);
	}

	private static BlockState frozenState(int phase, BlockPos pos,
			boolean lava) {
		long mixed = pos.asLong() * 0x9E3779B97F4A7C15L;
		int divisor = phase >= SilladBossRules.PHASE_THREE ? 2
				: phase >= SilladBossRules.PHASE_TWO ? 4 : 9;
		boolean blue = lava || Math.floorMod(Long.hashCode(mixed), divisor) == 0;
		return (blue ? Blocks.BLUE_ICE : Blocks.PACKED_ICE)
				.defaultBlockState();
	}

	private static boolean snowCell(BlockPos pos, int phase) {
		int divisor = phase >= SilladBossRules.PHASE_THREE ? 5 : 8;
		return Math.floorMod(Long.hashCode(pos.asLong()
				^ 0xC6BC279692B5CC83L), divisor) == 0;
	}

	private static List<ColumnOffset> createOffsets(int radius) {
		List<ColumnOffset> result = new ArrayList<>();
		for (int x = -radius; x <= radius; x++)
			for (int z = -radius; z <= radius; z++) {
				int distance = x * x + z * z;
				if (distance <= radius * radius)
					result.add(new ColumnOffset(x, z, distance));
			}
		result.sort(Comparator.comparingInt(ColumnOffset::distanceSquared)
				.thenComparingInt(ColumnOffset::x)
				.thenComparingInt(ColumnOffset::z));
		return List.copyOf(result);
	}

	private static void spawnRing(ServerLevel level, Vec3 center,
			double radius) {
		int points = Math.max(16, Math.min(48,
				(int) Math.ceil(radius * 2.4D)));
		for (int index = 0; index < points; index++) {
			double angle = Math.PI * 2.0D * index / points;
			level.sendParticles(ParticleTypes.SNOWFLAKE,
					center.x + Math.cos(angle) * radius, center.y,
					center.z + Math.sin(angle) * radius,
					1, 0.04D, 0.03D, 0.04D, 0.005D);
		}
	}

	private static int phaseForRequestedRadius(int radius) {
		if (radius >= SilladBossRules.frozenDomainRadius(
				SilladBossRules.PHASE_THREE))
			return SilladBossRules.PHASE_THREE;
		if (radius >= SilladBossRules.frozenDomainRadius(
				SilladBossRules.PHASE_TWO))
			return SilladBossRules.PHASE_TWO;
		return SilladBossRules.PHASE_ONE;
	}

	private static void refreshGlobalBudget(long gameTime) {
		if (budgetTick == gameTime)
			return;
		budgetTick = gameTime;
		globalInspections = GLOBAL_INSPECTION_BUDGET;
		globalMutations = SilladBossRules.FROZEN_DOMAIN_COLUMN_BUDGET;
	}

	private static boolean sameHorizontal(BlockPos first, BlockPos second) {
		return first.getX() == second.getX() && first.getZ() == second.getZ();
	}

	private static int horizontalDistanceSqr(BlockPos first, BlockPos second) {
		int x = first.getX() - second.getX();
		int z = first.getZ() - second.getZ();
		return x * x + z * z;
	}

	private record ColumnOffset(int x, int z, int distanceSquared) {
	}

	private static final class DomainState {
		private BlockPos mainCenter;
		private BlockPos wakeCenter;
		private int requestedRadius = SilladBossRules.frozenDomainRadius(
				SilladBossRules.PHASE_ONE);
		private int mainCursor;
		private int wakeCursor;

		private DomainState(BlockPos mainCenter, BlockPos wakeCenter) {
			this.mainCenter = mainCenter.immutable();
			this.wakeCenter = wakeCenter.immutable();
		}
	}

	private static final class WorkBudget {
		private final int inspectionLimit;
		private final int mutationLimit;
		private int usedInspections;
		private int usedMutations;

		private WorkBudget(int inspectionLimit, int mutationLimit) {
			this.inspectionLimit = Math.max(0, inspectionLimit);
			this.mutationLimit = Math.max(0, mutationLimit);
		}

		private boolean canInspect() {
			return usedInspections < inspectionLimit;
		}

		private boolean canMutate() {
			return usedMutations < mutationLimit;
		}

		private void inspect() {
			usedInspections++;
		}

		private void mutate() {
			usedMutations++;
		}
	}
}
