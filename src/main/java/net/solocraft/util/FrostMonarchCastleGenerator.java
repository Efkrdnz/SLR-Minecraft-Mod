package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.world.dimension.rift.DimensionalRiftDimension;
import net.solocraft.world.dimension.rift.RiftGeometry;
import net.solocraft.world.dimension.rift.RiftTerritory;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the Boreal Crown, a player-facing Frost Monarch fortress. The wide
 * octagonal curtain and eightfold ice details deliberately echo the shape of
 * the Dimensional Rift's central Monarch star.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class FrostMonarchCastleGenerator {
	private static final int HALF_WIDTH = 64;
	private static final int MIN_Z = -18;
	private static final int MAX_Z = 158;
	private static final int MIN_Y = -18;
	private static final int MAX_Y = 84;
	private static final int OUTER_CENTER_Z = 74;
	private static final int OUTER_HALF_X = 58;
	private static final int OUTER_HALF_Z = 74;
	private static final int OUTER_CUT = 18;
	private static final int SURVEY_CELLS_PER_TICK = 64;
	private static final int CHECKS_PER_TICK = 10_000;
	private static final int CHANGES_PER_TICK = 768;
	private static final Map<ResourceKey<Level>, BuildJob> ACTIVE_BUILDS = new LinkedHashMap<>();

	private FrostMonarchCastleGenerator() {
	}

	public static List<String> suggestions() {
		return List.of("frost_castle");
	}

	public static boolean handles(String normalizedName) {
		return "frostcastle".equals(normalizedName)
				|| "frostmonarchcastle".equals(normalizedName)
				|| "borealcrown".equals(normalizedName)
				|| "silladcastle".equals(normalizedName);
	}

	public static boolean start(ServerPlayer player) {
		if (player == null)
			return false;
		ServerLevel level = player.serverLevel();
		if (ACTIVE_BUILDS.containsKey(level.dimension())) {
			player.sendSystemMessage(Component.literal("A Boreal Crown castle is already being built in this dimension.")
					.withStyle(ChatFormatting.RED));
			return false;
		}

		Direction forward = player.getDirection();
		if (forward.getAxis().isVertical())
			forward = Direction.NORTH;
		Direction right = forward.getClockWise();
		BlockPos anchor = player.blockPosition().relative(forward, 20);
		int floorY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				anchor.getX(), anchor.getZ()) - 1;
		BlockPos origin = new BlockPos(anchor.getX(), floorY, anchor.getZ());
		if (origin.getY() + MIN_Y < level.getMinBuildHeight()
				|| origin.getY() + MAX_Y + 2 >= level.getMaxBuildHeight()) {
			player.sendSystemMessage(Component.literal("The Boreal Crown needs a floor between Y "
					+ (level.getMinBuildHeight() - MIN_Y) + " and Y "
					+ (level.getMaxBuildHeight() - MAX_Y - 3) + " in this dimension.")
					.withStyle(ChatFormatting.RED));
			return false;
		}
		if (DimensionalRiftDimension.LEVEL_KEY.equals(level.dimension())
				&& !isValidFrostSite(origin, forward, right)) {
			player.sendSystemMessage(Component.literal("The complete 129 x 177 castle footprint must remain inside the "
					+ "Frost territory and before the Rift's terrain fade.").withStyle(ChatFormatting.RED));
			return false;
		}
		BuildJob job = new BuildJob(level, origin.immutable(), forward, player.getUUID(),
				player.getGameProfile().getName());
		ACTIVE_BUILDS.put(level.dimension(), job);
		player.sendSystemMessage(Component.literal("Boreal Crown site survey started at " + origin.toShortString()
				+ ", facing " + forward.getName() + ".").withStyle(ChatFormatting.AQUA));
		player.sendSystemMessage(Component.literal("Footprint: 129 x 177; 84 above floor; 18-block foundation. "
				+ "The full-column survey and construction are staged to protect TPS.")
				.withStyle(ChatFormatting.DARK_GRAY));
		return true;
	}

	private static boolean isValidFrostSite(BlockPos origin, Direction forward, Direction right) {
		for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
			for (int z = MIN_Z; z <= MAX_Z; z++) {
				boolean bridge = Math.abs(x) <= 8 && z >= MIN_Z && z <= 7;
				if (!bridge && !insideExpanded(x, z))
					continue;
				BlockPos pos = localToWorld(origin, forward, right, x, 0, z);
				RiftGeometry.Region region = RiftGeometry.resolveDefault(pos.getX(), pos.getZ());
				if (region.type() != RiftGeometry.RegionType.TERRITORY
						|| region.territory() != RiftTerritory.FROST
						|| region.distance() >= RiftGeometry.DEFAULT_TERRAIN_FADE_START)
					return false;
			}
		}
		return true;
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (false || ACTIVE_BUILDS.isEmpty())
			return;
		int count = Math.max(1, ACTIVE_BUILDS.size());
		int checks = Math.max(2_500, CHECKS_PER_TICK / count);
		int changes = Math.max(192, CHANGES_PER_TICK / count);
		List<ResourceKey<Level>> completed = new ArrayList<>();
		for (Map.Entry<ResourceKey<Level>, BuildJob> entry : new ArrayList<>(ACTIVE_BUILDS.entrySet())) {
			try {
				if (entry.getValue().tick(checks, changes))
					completed.add(entry.getKey());
			} catch (RuntimeException exception) {
				SololevelingMod.LOGGER.error("Boreal Crown build failed for {} in {}",
						entry.getValue().ownerName, entry.getKey().location(), exception);
				entry.getValue().abort("Boreal Crown construction stopped after an unexpected server error.");
				completed.add(entry.getKey());
			}
		}
		completed.forEach(ACTIVE_BUILDS::remove);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ACTIVE_BUILDS.clear();
	}

	private static BlockPos localToWorld(BlockPos origin, Direction forward, Direction right, int x, int y, int z) {
		return origin.relative(right, x).relative(forward, z).above(y);
	}

	private static boolean insideOctagon(int x, int z, int halfX, int halfZ, int cornerCut) {
		int ax = Math.abs(x);
		int az = Math.abs(z);
		return ax <= halfX && az <= halfZ && ax + az <= halfX + halfZ - cornerCut;
	}

	private static boolean insideOuter(int x, int z) {
		return insideOctagon(x, z - OUTER_CENTER_Z, OUTER_HALF_X, OUTER_HALF_Z, OUTER_CUT);
	}

	private static boolean insideExpanded(int x, int z) {
		return insideOctagon(x, z - OUTER_CENTER_Z, 64, 84, 14);
	}

	private static final class BuildJob {
		private static final int[][] TOWERS = {{-43, 15}, {43, 15}, {-43, 133}, {43, 133}};
		private static final int[] HALL_COLUMN_Z = {86, 98, 110, 122};

		private final ServerLevel level;
		private final BlockPos origin;
		private final Direction forward;
		private final Direction right;
		private final UUID ownerId;
		private final String ownerName;
		private final long totalChecks;
		private final long totalSurveyCells;
		private int surveyX = -HALF_WIDTH;
		private int surveyZ = MIN_Z;
		private long surveyed;
		private int lastSurveyStep = -1;
		private boolean surveyComplete;
		private int x = -HALF_WIDTH;
		private int z = MIN_Z;
		private int y = MIN_Y;
		private long checked;
		private long changed;
		private int lastProgress = -1;
		private boolean blockPassFinished;

		private BuildJob(ServerLevel level, BlockPos origin, Direction forward, UUID ownerId, String ownerName) {
			this.level = level;
			this.origin = origin;
			this.forward = forward;
			this.right = forward.getClockWise();
			this.ownerId = ownerId;
			this.ownerName = ownerName;
			this.totalSurveyCells = (long) (HALF_WIDTH * 2 + 1) * (MAX_Z - MIN_Z + 1);
			this.totalChecks = (long) (HALF_WIDTH * 2 + 1) * (MAX_Z - MIN_Z + 1) * (MAX_Y - MIN_Y + 1);
		}

		private boolean tick(int checkBudget, int changeBudget) {
			if (!surveyComplete)
				return surveyTerrain();
			if (blockPassFinished)
				return finish();
			int checksThisTick = 0;
			int changesThisTick = 0;
			while (!blockPassFinished && checksThisTick < checkBudget && changesThisTick < changeBudget) {
				BlockState desired = desiredState(x, y, z);
				if (desired != null) {
					BlockPos worldPos = toWorld(x, y, z);
					BlockState existing = level.getBlockState(worldPos);
					if (!existing.equals(desired)) {
						if (level.setBlock(worldPos, desired, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
							changesThisTick++;
							changed++;
						}
					}
				}
				checksThisTick++;
				checked++;
				advance();
			}
			reportProgress();
			return false;
		}

		private boolean surveyTerrain() {
			int minimum = origin.getY() + MIN_Y;
			int maximum = origin.getY() + MAX_Y;
			int cellsThisTick = 0;
			while (!surveyComplete && cellsThisTick < SURVEY_CELLS_PER_TICK) {
				boolean bridge = Math.abs(surveyX) <= 8 && surveyZ >= MIN_Z && surveyZ <= 7;
				if (bridge || insideExpanded(surveyX, surveyZ)) {
					BlockPos pos = toWorld(surveyX, 0, surveyZ);
					int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
							pos.getX(), pos.getZ()) - 1;
					if (surface < minimum || surface > maximum) {
						abort("Boreal Crown survey stopped at " + pos.getX() + ", " + surface + ", "
								+ pos.getZ() + ": terrain must stay between Y " + minimum + " and " + maximum + ".");
						return true;
					}
				}
				cellsThisTick++;
				surveyed++;
				advanceSurvey();
			}
			reportSurveyProgress();
			if (surveyComplete) {
				ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
				if (owner != null)
					owner.sendSystemMessage(Component.literal("Boreal Crown survey complete; staged construction has begun.")
							.withStyle(ChatFormatting.AQUA));
				else
					SololevelingMod.LOGGER.info("Boreal Crown survey complete for {}", ownerName);
			}
			return false;
		}

		private void advanceSurvey() {
			surveyX++;
			if (surveyX <= HALF_WIDTH)
				return;
			surveyX = -HALF_WIDTH;
			surveyZ++;
			if (surveyZ > MAX_Z)
				surveyComplete = true;
		}

		private void reportSurveyProgress() {
			int progress = (int) Math.min(100L, surveyed * 100L / totalSurveyCells);
			int step = progress / 25;
			if (step <= lastSurveyStep)
				return;
			lastSurveyStep = step;
			ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
			if (owner != null)
				owner.displayClientMessage(Component.literal("Boreal Crown site survey  " + Math.min(100, step * 25) + "%")
						.withStyle(ChatFormatting.AQUA), true);
		}

		private void advance() {
			x++;
			if (x <= HALF_WIDTH)
				return;
			x = -HALF_WIDTH;
			z++;
			if (z <= MAX_Z)
				return;
			z = MIN_Z;
			y++;
			if (y > MAX_Y)
				blockPassFinished = true;
		}

		private void reportProgress() {
			int progress = (int) Math.min(100L, checked * 100L / totalChecks);
			int step = progress / 10;
			if (step <= lastProgress)
				return;
			lastProgress = step;
			ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
			if (owner != null)
				owner.displayClientMessage(Component.literal("Boreal Crown  " + Math.min(100, step * 10) + "%")
						.withStyle(ChatFormatting.AQUA), true);
		}

		private boolean finish() {
			BlockPos crown = toWorld(0, 84, 128);
			level.playSound(null, crown, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.8F, 1.45F);
			level.playSound(null, crown, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F, 0.72F);
			level.sendParticles(ParticleTypes.SNOWFLAKE, crown.getX() + 0.5D, crown.getY() + 0.5D,
					crown.getZ() + 0.5D, 220, 16.0D, 12.0D, 16.0D, 0.035D);
			level.sendParticles(ParticleTypes.END_ROD, crown.getX() + 0.5D, crown.getY() + 0.5D,
					crown.getZ() + 0.5D, 90, 8.0D, 16.0D, 8.0D, 0.02D);
			ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
			Component message = Component.literal("The Boreal Crown is complete for " + ownerName + ": "
					+ changed + " blocks changed.").withStyle(ChatFormatting.GREEN);
			if (owner != null)
				owner.sendSystemMessage(message);
			else
				SololevelingMod.LOGGER.info(message.getString());
			return true;
		}

		private void abort(String reason) {
			ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
			if (owner != null)
				owner.sendSystemMessage(Component.literal(reason).withStyle(ChatFormatting.RED));
			else
				SololevelingMod.LOGGER.warn("{}", reason);
		}

		private BlockPos toWorld(int localX, int localY, int localZ) {
			return localToWorld(origin, forward, right, localX, localY, localZ);
		}

		private Direction worldFacing(Direction local) {
			return switch (local) {
				case NORTH -> forward.getOpposite();
				case SOUTH -> forward;
				case EAST -> right;
				case WEST -> right.getOpposite();
				default -> local;
			};
		}

		private BlockState facing(Block block, Direction local) {
			return block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, worldFacing(local));
		}

		private BlockState stair(Block block, Direction local) {
			return facing(block, local).setValue(BlockStateProperties.HALF, Half.BOTTOM);
		}

		private BlockState hangingLantern() {
			return Blocks.SOUL_LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true);
		}

		private BlockState directional(Block block, Direction local) {
			return block.defaultBlockState().setValue(BlockStateProperties.FACING, worldFacing(local));
		}

		private BlockState floorGrindstone(Direction local) {
			return facing(Blocks.GRINDSTONE, local)
					.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
		}

		private BlockState desiredState(int localX, int localY, int localZ) {
			boolean outer = insideOuter(localX, localZ);
			boolean expanded = insideExpanded(localX, localZ);
			boolean bridge = Math.abs(localX) <= 8 && localZ >= MIN_Z && localZ <= 7;

			if (localY <= 0)
				return foundationState(localX, localY, localZ, outer, expanded, bridge);

			BlockState state = bridgeState(localX, localY, localZ);
			if (state != null)
				return state;
			state = towerState(localX, localY, localZ);
			if (state != null)
				return state;
			state = gatehouseState(localX, localY, localZ);
			if (state != null)
				return state;
			state = rampartConnectorState(localX, localY, localZ);
			if (state != null)
				return state;
			state = keepState(localX, localY, localZ);
			if (state != null)
				return state;
			state = curtainWallState(localX, localY, localZ);
			if (state != null)
				return state;
			state = courtyardState(localX, localY, localZ);
			if (state != null)
				return state;

			if (outer)
				return Blocks.AIR.defaultBlockState();
			if (expanded || bridge)
				return Blocks.AIR.defaultBlockState();
			return null;
		}

		private BlockState foundationState(int localX, int localY, int localZ,
				boolean outer, boolean expanded, boolean bridge) {
			boolean structural = outer || bridge || isTowerFootprint(localX, localZ)
					|| Math.abs(localX) <= 22 && localZ >= -2 && localZ <= 25
					|| isInsideKeep(localX, localZ);
			if (structural) {
				if (localY == MIN_Y)
					return Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
				if (localY < -5)
					return ((localX * 13 + localZ * 7 + localY) & 7) == 0
							? Blocks.BLUE_ICE.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
				if (localY < 0)
					return ((localX + localZ + localY) & 5) == 0
							? Blocks.PACKED_ICE.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
				return floorState(localX, localZ);
			}
			if (expanded) {
				if (localY < -2)
					return Blocks.PACKED_ICE.defaultBlockState();
				if (localY == -2)
					return ((Math.abs(localX) + localZ) & 7) == 0
							? Blocks.PACKED_ICE.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
				return Blocks.AIR.defaultBlockState();
			}
			return null;
		}

		private boolean isTowerFootprint(int localX, int localZ) {
			for (int[] tower : TOWERS) {
				if (insideOctagon(localX - tower[0], localZ - tower[1], 13, 13, 5))
					return true;
			}
			return false;
		}

		private BlockState floorState(int localX, int localZ) {
			if (isTowerFloorLight(localX, localZ))
				return Blocks.SEA_LANTERN.defaultBlockState();
			if (Math.abs(localX) <= 7 && localZ <= 80)
				return Math.floorMod(localZ, 9) == 0 && Math.abs(localX) <= 1
						? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.POLISHED_DIORITE.defaultBlockState();
			if (isInsideKeep(localX, localZ)) {
				if (Math.floorMod(localX, 12) == 0 && Math.floorMod(localZ, 12) == 0)
					return Blocks.SEA_LANTERN.defaultBlockState();
				if (Math.abs(localX) <= 15)
					return (Math.abs(localX) + localZ) % 9 == 0
							? Blocks.BLUE_ICE.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
				return ((Math.floorDiv(localX, 6) + Math.floorDiv(localZ, 6)) & 1) == 0
						? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
			}
			if (isCurtainZone(localX, localZ))
				return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
			if (Math.floorMod(localX, 14) == 0 && Math.floorMod(localZ - 22, 14) == 0)
				return Blocks.SEA_LANTERN.defaultBlockState();
			if (Math.abs(localZ - 52) <= 3)
				return Blocks.CALCITE.defaultBlockState();
			return ((Math.floorDiv(localX, 7) + Math.floorDiv(localZ, 7)) & 1) == 0
					? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
		}

		private boolean isTowerFloorLight(int localX, int localZ) {
			for (int[] tower : TOWERS) {
				int dx = localX - tower[0];
				int dz = localZ - tower[1];
				if (Math.abs(dx) == 5 && dz == 0 || dx == 0 && Math.abs(dz) == 5)
					return true;
			}
			return false;
		}

		private BlockState bridgeState(int localX, int localY, int localZ) {
			if (Math.abs(localX) > 8 || localZ < MIN_Z || localZ > 7)
				return null;
			int ax = Math.abs(localX);
			if (localY == 1 && ax == 8)
				return Blocks.CALCITE.defaultBlockState();
			if (localY == 2 && ax == 8 && Math.floorMod(localZ + 18, 5) <= 1)
				return Blocks.PACKED_ICE.defaultBlockState();
			if (localY == 3 && ax == 8 && Math.floorMod(localZ + 18, 5) == 0)
				return Blocks.SEA_LANTERN.defaultBlockState();
			return null;
		}

		private BlockState towerState(int localX, int localY, int localZ) {
			for (int[] tower : TOWERS) {
				int dx = localX - tower[0];
				int dz = localZ - tower[1];
				if (!insideOctagon(dx, dz, 13, 13, 5))
					continue;
				boolean rear = tower[1] > OUTER_CENTER_Z;
				int top = rear ? 46 : 40;
				if (localY <= top) {
					if (dx == 0 && dz == -2 && localY >= 2 && localY < top - 1)
						return facing(Blocks.LADDER, Direction.NORTH);
					if ((localY == 15 || localY == 23 || localY == 29 || rear && localY == 43)) {
						if (Math.abs(dx) == 5 && dz == 0 || dx == 0 && Math.abs(dz) == 5)
							return Blocks.SEA_LANTERN.defaultBlockState();
						return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
					}
					boolean shell = !insideOctagon(dx, dz, 10, 10, 5);
					if (shell) {
						boolean groundDoor = (tower[0] < 0 && dx >= 10 || tower[0] > 0 && dx <= -10)
								&& Math.abs(dz) <= 2 && localY >= 1 && localY <= 5;
						boolean sideCurtainDoor = (tower[0] < 0 && dx <= -10 || tower[0] > 0 && dx >= 10)
								&& Math.abs(dz) <= 2;
						boolean endCurtainDoor = (!rear && dz <= -10 || rear && dz >= 10)
								&& Math.abs(dx) <= 2;
						boolean gateConnectorDoor = !rear
								&& (tower[0] < 0 && dx >= 10 || tower[0] > 0 && dx <= -10)
								&& Math.abs(dz) <= 2;
						boolean upperDoor = localY >= 24 && localY <= 28
								&& (sideCurtainDoor || endCurtainDoor || gateConnectorDoor);
						if (groundDoor || upperDoor)
							return Blocks.AIR.defaultBlockState();
						boolean slit = (Math.abs(dx) <= 1 || Math.abs(dz) <= 1)
								&& (localY >= 7 && localY <= 11 || localY >= 21 && localY <= 25
								|| rear && localY >= 35 && localY <= 39);
						if (slit)
							return (localY & 1) == 0 ? Blocks.CYAN_STAINED_GLASS.defaultBlockState()
									: Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
						if (localY == 5 || localY == 17 || localY == 31 || rear && localY == 43)
							return Blocks.CALCITE.defaultBlockState();
						if ((Math.abs(dx) == 12 || Math.abs(dz) == 12) && localY % 7 <= 1)
							return Blocks.BLUE_ICE.defaultBlockState();
						return (localY + Math.abs(dx) + Math.abs(dz)) % 23 == 0
								? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
								: Blocks.DEEPSLATE_BRICKS.defaultBlockState();
					}
					if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1)
						return localY % 8 == 0 ? Blocks.BLUE_ICE.defaultBlockState()
								: Blocks.POLISHED_BASALT.defaultBlockState();
					if ((localY == 13 || localY == 27 || rear && localY == 41) && dx == 5 && dz == 0)
						return hangingLantern();
					if ((localY == 14 || localY == 28 || rear && localY == 42) && dx == 5 && dz == 0)
						return Blocks.CHAIN.defaultBlockState();
					return Blocks.AIR.defaultBlockState();
				}
				int roofLayer = localY - top - 1;
				int roofRadius = 13 - roofLayer;
				if (roofRadius >= 1 && insideOctagon(dx, dz, roofRadius, roofRadius, Math.min(4, roofRadius / 2))) {
					if (Math.abs(dx) <= 1 || Math.abs(dz) <= 1 || Math.abs(dx) == Math.abs(dz))
						return Blocks.BLUE_ICE.defaultBlockState();
					return roofLayer % 3 == 0 ? Blocks.CALCITE.defaultBlockState()
							: Blocks.PACKED_ICE.defaultBlockState();
				}
				if (dx == 0 && dz == 0 && localY <= top + 18)
					return localY == top + 18 ? Blocks.SEA_LANTERN.defaultBlockState()
							: Blocks.BLUE_ICE.defaultBlockState();
				return Blocks.AIR.defaultBlockState();
			}
			return null;
		}

		private BlockState gatehouseState(int localX, int localY, int localZ) {
			int ax = Math.abs(localX);
			if (ax > 22 || localZ < -2 || localZ > 25)
				return null;
			if (localY > 34) {
				for (int centerX : new int[]{-15, 15}) {
					int dx = localX - centerX;
					int dz = localZ - 11;
					int layer = localY - 34;
					int radius = 7 - layer;
					if (radius >= 1 && insideOctagon(dx, dz, radius, radius, 2))
						return Math.abs(dx) <= 1 || Math.abs(dz) <= 1
								? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
					if (dx == 0 && dz == 0 && localY <= 46)
						return localY == 46 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
				}
				if (localY <= 38 && (ax >= 18 || Math.floorMod(localX + localZ, 5) <= 1))
					return localY == 35 ? Blocks.DEEPSLATE_TILES.defaultBlockState()
							: Blocks.CALCITE.defaultBlockState();
				return Blocks.AIR.defaultBlockState();
			}

			boolean frontBack = localZ <= 0 || localZ >= 23;
			boolean shell = ax >= 19 || frontBack;
			boolean gateOpening = ax <= 6 && (localY <= 11
					|| localY <= 17 && localX * localX + (localY - 11) * (localY - 11) <= 42);
			if (shell) {
				if (frontBack && gateOpening)
					return Blocks.AIR.defaultBlockState();
				if (ax >= 19 && (localZ >= 0 && localZ <= 4 || localZ >= 10 && localZ <= 14)
						&& localY >= 24 && localY <= 28)
					return Blocks.AIR.defaultBlockState();
				if (localZ == -2 && frostCrest(localX, localY))
					return (localX + localY) % 3 == 0 ? Blocks.CALCITE.defaultBlockState()
							: Blocks.BLUE_ICE.defaultBlockState();
				if ((localY >= 7 && localY <= 12 || localY >= 21 && localY <= 27)
						&& (ax == 20 && Math.floorMod(localZ, 8) <= 1
						|| frontBack && Math.floorMod(localX, 9) <= 1))
					return Blocks.CYAN_STAINED_GLASS.defaultBlockState();
				if (localY == 5 || localY == 18 || localY == 31)
					return Blocks.CALCITE.defaultBlockState();
				return (localY + ax + localZ) % 19 == 0 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
						: Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			}
			if (localX == 11 && localZ == 12 && localY >= 1 && localY <= 34)
				return facing(Blocks.LADDER, Direction.WEST);
			if (localX == 12 && localZ == 12 && localY >= 1 && localY <= 34)
				return localY % 8 == 0 ? Blocks.BLUE_ICE.defaultBlockState()
						: Blocks.POLISHED_BASALT.defaultBlockState();
			if ((localY == 17 || localY == 23) && (ax > 7 || localZ > 17)) {
				boolean inlay = (ax == 9 || ax == 15) && (localZ == 6 || localZ == 12 || localZ == 18)
						|| ax == 0 && localZ == 20;
				return inlay ? Blocks.SEA_LANTERN.defaultBlockState()
						: Blocks.POLISHED_DEEPSLATE.defaultBlockState();
			}
			if ((localX == -9 || localX == 9) && localY <= 15 && localZ >= 4 && localZ <= 20
					&& !(localZ >= 10 && localZ <= 13 && localY <= 4))
				return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			if (localZ == 3 && ax <= 6 && localY >= 12 && localY <= 16 && (localX & 1) == 0)
				return Blocks.IRON_BARS.defaultBlockState();
			if ((localX == -14 || localX == 14) && (localZ == 8 || localZ == 18) && localY == 1)
				return directional(Blocks.BARREL, localX < 0 ? Direction.EAST : Direction.WEST);
			if ((localX == -14 || localX == 14) && localZ == 13 && localY == 15)
				return hangingLantern();
			if ((localX == -14 || localX == 14) && localZ == 13 && localY == 16)
				return Blocks.CHAIN.defaultBlockState();
			if (localY == 33)
				return Blocks.DEEPSLATE_TILES.defaultBlockState();
			return Blocks.AIR.defaultBlockState();
		}

		private BlockState rampartConnectorState(int localX, int localY, int localZ) {
			int ax = Math.abs(localX);
			if (ax < 23 || ax > 29 || localZ < 10 || localZ > 14)
				return null;
			if (localY == 23)
				return ax == 26 && localZ == 12 ? Blocks.SEA_LANTERN.defaultBlockState()
						: Blocks.POLISHED_DEEPSLATE.defaultBlockState();
			if (localY == 24 && (localZ == 10 || localZ == 14))
				return Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
			if (localY == 25 && (localZ == 10 || localZ == 14) && (ax == 23 || ax == 26 || ax == 29))
				return Blocks.BLUE_ICE.defaultBlockState();
			return null;
		}

		private boolean frostCrest(int localX, int localY) {
			int dy = localY - 24;
			return localY >= 19 && localY <= 30
					&& (localX == 0 || dy == 0 && Math.abs(localX) <= 9
					|| Math.abs(localX) == Math.abs(dy) && Math.abs(localX) <= 6);
		}

		private BlockState curtainWallState(int localX, int localY, int localZ) {
			if (!isCurtainZone(localX, localZ))
				return null;
			if (localY <= 22) {
				if (localY == 6 || localY == 15)
					return Blocks.BLUE_ICE.defaultBlockState();
				if (localY == 21)
					return Blocks.CALCITE.defaultBlockState();
				if ((Math.abs(localX) * 3 + localZ + localY) % 29 == 0)
					return Blocks.CHISELED_DEEPSLATE.defaultBlockState();
				return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			}
			if (localY == 23)
				return Blocks.DEEPSLATE_TILES.defaultBlockState();
			boolean lightPost = Math.floorMod(Math.abs(localX) * 3 + localZ, 24) == 0;
			if (lightPost && localY == 24)
				return Blocks.SEA_LANTERN.defaultBlockState();
			if (lightPost && localY == 25)
				return Blocks.BLUE_ICE.defaultBlockState();
			if (localY <= 27 && Math.floorMod(Math.abs(localX) + localZ, 6) <= 1)
				return localY == 27 ? Blocks.SNOW_BLOCK.defaultBlockState()
						: Blocks.CALCITE.defaultBlockState();
			return Blocks.AIR.defaultBlockState();
		}

		private boolean isCurtainZone(int localX, int localZ) {
			return insideOuter(localX, localZ)
					&& !insideOctagon(localX, localZ - OUTER_CENTER_Z, 53, 69, OUTER_CUT);
		}

		private boolean isInsideKeep(int localX, int localZ) {
			return insideOctagon(localX, localZ - 112, 40, 37, 10);
		}

		private BlockState keepState(int localX, int localY, int localZ) {
			if (!isInsideKeep(localX, localZ))
				return null;

			BlockState crown = crownTowerState(localX, localY, localZ);
			if (crown != null)
				return crown;
			BlockState roof = keepRoofState(localX, localY, localZ);
			if (roof != null)
				return roof;

			boolean shell = !insideOctagon(localX, localZ - 112, 37, 34, 10);
			boolean entrance = localZ <= 77 && Math.abs(localX) <= 7
					&& (localY <= 11 || localY <= 17
					&& localX * localX + (localY - 11) * (localY - 11) <= 50);
			if (localY <= 32 && shell) {
				if (entrance)
					return Blocks.AIR.defaultBlockState();
				if (isKeepWindow(localX, localY, localZ))
					return (localY & 1) == 0 ? Blocks.CYAN_STAINED_GLASS.defaultBlockState()
							: Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
				if (localZ >= 146 && auroraWindow(localX, localY))
					return Math.floorMod(localX + localY, 4) == 0 ? Blocks.BLUE_ICE.defaultBlockState()
							: Blocks.CYAN_STAINED_GLASS.defaultBlockState();
				if (localY == 6 || localY == 18 || localY == 30)
					return Blocks.CALCITE.defaultBlockState();
				return (Math.abs(localX) + localZ + localY) % 31 == 0
						? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			}

			BlockState hall = throneHallState(localX, localY, localZ);
			if (hall != null)
				return hall;
			BlockState stairs = grandStairState(localX, localY, localZ);
			if (stairs != null)
				return stairs;
			BlockState rooms = sideRoomState(localX, localY, localZ);
			if (rooms != null)
				return rooms;

			if (localY == 16 && (Math.abs(localX) >= 14 || localZ >= 124))
				return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
			if ((Math.abs(localX) == 17 && localZ >= 81 && localZ <= 138)
					&& !(nearDoor(localZ, localY, 90) || nearDoor(localZ, localY, 110)
					|| nearDoor(localZ, localY, 130))) {
				if (localY <= 29)
					return localY == 8 || localY == 20 ? Blocks.CALCITE.defaultBlockState()
							: Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			}
			if ((localZ == 106 || localZ == 124) && Math.abs(localX) >= 18 && Math.abs(localX) <= 36
					&& !(Math.abs(localX) >= 25 && Math.abs(localX) <= 28 && localY <= 4)
					&& localY <= 15)
				return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			return Blocks.AIR.defaultBlockState();
		}

		private boolean nearDoor(int value, int localY, int center) {
			return Math.abs(value - center) <= 2 && localY <= 5;
		}

		private boolean isKeepWindow(int localX, int localY, int localZ) {
			if (!(localY >= 7 && localY <= 13 || localY >= 21 && localY <= 27))
				return false;
			return Math.abs(localX) >= 38 && Math.floorMod(localZ - 82, 12) <= 2
					|| localZ >= 147 && Math.floorMod(localX + 32, 12) <= 2;
		}

		private boolean auroraWindow(int localX, int localY) {
			int ax = Math.abs(localX);
			return localY >= 8 && localY <= 29 && ax <= 14
					&& (ax <= 2 || Math.abs(ax - Math.abs(localY - 19)) <= 1 || localY == 19);
		}

		private BlockState keepRoofState(int localX, int localY, int localZ) {
			if (localY < 33)
				return null;
			if (localY == 33 && Math.abs(localX) >= 19 && Math.abs(localX) <= 21
					&& localZ >= 113 && localZ <= 115)
				return null;
			if (localY == 34 && Math.abs(localX) <= 19 && localZ >= 110 && localZ <= 113)
				return Math.abs(localX) == 19 || localZ == 110 || localZ == 113
						? Blocks.CALCITE.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
			for (int centerX : new int[]{-30, 30}) {
				for (int centerZ : new int[]{88, 134}) {
					int dx = localX - centerX;
					int dz = localZ - centerZ;
					int layer = localY - 33;
					int radius = 7 - layer;
					if (radius >= 1 && insideOctagon(dx, dz, radius, radius, 2))
						return Math.abs(dx) <= 1 || Math.abs(dz) <= 1
								? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
				}
			}
			if (Math.abs(localX) <= 16 && localZ >= 75 && localZ <= 124) {
				double normalized = Math.min(1.0D, Math.abs(localX) / 17.0D);
				int roofY = 35 + (int) Math.round(Math.sqrt(1.0D - normalized * normalized) * 7.0D);
				boolean frontGable = localZ <= 77;
				boolean springCourse = Math.abs(localX) == 16;
				if (localY >= 33 && localY <= roofY && (frontGable || springCourse)) {
					if (localY == 33 || localY == roofY || Math.abs(localX) <= 1)
						return Blocks.BLUE_ICE.defaultBlockState();
					return frontGable ? Blocks.CYAN_STAINED_GLASS.defaultBlockState()
							: Blocks.DEEPSLATE_BRICKS.defaultBlockState();
				}
				if (localY == roofY)
					return Math.abs(localX) <= 1 || Math.floorMod(localZ, 8) == 0
							? Blocks.BLUE_ICE.defaultBlockState() : Blocks.CALCITE.defaultBlockState();
				if (localY < roofY)
					return null;
			}
			if (localY == 33 && (Math.abs(localX) >= 16 || localZ >= 124))
				return Blocks.DEEPSLATE_TILES.defaultBlockState();
			if (localY <= 37 && (Math.abs(localX) >= 36 || localZ >= 145)
					&& Math.floorMod(Math.abs(localX) + localZ, 6) <= 1)
				return Blocks.CALCITE.defaultBlockState();
			return null;
		}

		private BlockState crownTowerState(int localX, int localY, int localZ) {
			if (localY < 34)
				return null;
			int dx = localX;
			int dz = localZ - 128;
			if (!insideOctagon(dx, dz, 18, 18, 7))
				return null;
			if (localY <= 67) {
				if (dx == 0 && dz == -2 && localY >= 35 && localY <= 66)
					return facing(Blocks.LADDER, Direction.NORTH);
				if (localY == 34 || localY == 50 || localY == 64)
					return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
				boolean shell = !insideOctagon(dx, dz, 15, 15, 7);
				if (shell) {
					if (dz <= -15 && Math.abs(dx) <= 2 && localY >= 35 && localY <= 39)
						return Blocks.AIR.defaultBlockState();
					if ((Math.abs(dx) <= 2 || Math.abs(dz) <= 2)
							&& (localY >= 40 && localY <= 47 || localY >= 55 && localY <= 61))
						return Blocks.CYAN_STAINED_GLASS.defaultBlockState();
					if (localY == 38 || localY == 52 || localY == 65)
						return Blocks.CALCITE.defaultBlockState();
					return Math.abs(dx) == Math.abs(dz) ? Blocks.BLUE_ICE.defaultBlockState()
							: Blocks.DEEPSLATE_BRICKS.defaultBlockState();
				}
				if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && localY >= 35 && localY <= 66)
					return localY % 6 == 0 ? Blocks.SEA_LANTERN.defaultBlockState()
							: Blocks.POLISHED_BASALT.defaultBlockState();
				return Blocks.AIR.defaultBlockState();
			}
			int radius = 84 - localY;
			if (radius >= 1 && insideOctagon(dx, dz, radius, radius, Math.min(6, radius / 2))) {
				if (Math.abs(dx) <= 1 || Math.abs(dz) <= 1 || Math.abs(dx) == Math.abs(dz))
					return Blocks.BLUE_ICE.defaultBlockState();
				return localY % 3 == 0 ? Blocks.CALCITE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
			}
			if (dx == 0 && dz == 0)
				return localY == 84 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
			return Blocks.AIR.defaultBlockState();
		}

		private BlockState throneHallState(int localX, int localY, int localZ) {
			int ax = Math.abs(localX);
			int dais = daisHeight(localX, localZ);
			if (dais > 0 && localY <= dais)
				return localY == dais ? Blocks.POLISHED_DIORITE.defaultBlockState()
						: Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			if (ax <= 4 && localZ >= 79 && localZ <= 132 && localY == 1)
				return localZ % 7 == 0 ? Blocks.CYAN_CARPET.defaultBlockState()
						: Blocks.LIGHT_BLUE_CARPET.defaultBlockState();
			for (int columnZ : HALL_COLUMN_Z) {
				if (Math.abs(ax - 12) <= 1 && Math.abs(localZ - columnZ) <= 1) {
					if (localY <= 3 || localY >= 25 && localY <= 28)
						return Blocks.CALCITE.defaultBlockState();
					if (localY <= 24)
						return localY % 7 == 0 ? Blocks.BLUE_ICE.defaultBlockState()
								: Blocks.POLISHED_BASALT.defaultBlockState();
				}
			}
			if (ax >= 14 && ax <= 17 && localZ >= 82 && localZ <= 130 && localY == 16)
				return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
			if (localZ == 82 && ax <= 16 && localY == 16)
				return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
			if (localX == 0 && localZ == 102 && localY >= 27 && localY <= 41)
				return Blocks.CHAIN.defaultBlockState();
			if (localY == 26 && localZ >= 99 && localZ <= 105 && ax <= 4) {
				if (ax == 4 || localZ == 99 || localZ == 105)
					return Blocks.BLUE_ICE.defaultBlockState();
				if ((ax + localZ) % 3 == 0)
					return Blocks.SEA_LANTERN.defaultBlockState();
			}
			if (localZ >= 140 && localZ <= 143 && ax <= 7 && localY >= 5 && localY <= 15) {
				if (ax >= 5 || localY == 5 || localY == 10 || localY == 15)
					return Blocks.BLUE_ICE.defaultBlockState();
				return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
			}
			if (localZ >= 140 && localZ <= 143 && ax <= 7 && localY >= 1 && localY <= 4)
				return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
			if (localZ == 137 && ax <= 3 && localY == 6)
				return stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.SOUTH);
			if (localZ == 138 && ax <= 3 && localY >= 7 && localY <= 11)
				return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
			return null;
		}

		private int daisHeight(int localX, int localZ) {
			if (localZ < 128 || localZ > 139)
				return 0;
			int tier = 1 + (localZ - 128) / 2;
			int width = 14 - tier;
			return Math.abs(localX) <= width ? Math.min(6, tier) : 0;
		}

		private BlockState grandStairState(int localX, int localY, int localZ) {
			int ax = Math.abs(localX);
			if (ax < 19 || ax > 21)
				return null;
			if (localY == 16 && localZ >= 96 && localZ <= 97)
				return Blocks.AIR.defaultBlockState();
			if (localZ >= 83 && localZ <= 98) {
				int height = localZ - 82;
				if (localY < height)
					return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
				if (localY == height)
					return stair(Blocks.POLISHED_DEEPSLATE_STAIRS, Direction.SOUTH);
			}
			if (localZ >= 99 && localZ <= 115) {
				int height = 16 + localZ - 98;
				if (localY >= 16 && localY < height)
					return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
				if (localY == height)
					return stair(Blocks.POLISHED_DEEPSLATE_STAIRS, Direction.SOUTH);
			}
			return null;
		}

		private BlockState sideRoomState(int localX, int localY, int localZ) {
			int ax = Math.abs(localX);
			if (ax < 19 || ax > 35 || localZ < 79 || localZ > 139)
				return null;
			boolean west = localX < 0;
			boolean frontRoom = localZ < 106;
			if (localY == 1 && Math.floorMod(ax + localZ, 17) == 0)
				return Blocks.LIGHT_BLUE_CARPET.defaultBlockState();

			if (west && frontRoom) {
				if (ax >= 25 && ax <= 28 && localZ >= 84 && localZ <= 100 && localY == 1)
					return (localZ == 84 || localZ == 100) ? Blocks.DARK_OAK_FENCE.defaultBlockState()
							: Blocks.DARK_OAK_SLAB.defaultBlockState();
				if ((ax == 23 || ax == 30) && localZ >= 85 && localZ <= 99 && localZ % 3 == 0 && localY == 1)
					return stair(Blocks.DARK_OAK_STAIRS, ax == 23 ? Direction.WEST : Direction.EAST);
				if (localX == -33 && (localZ == 82 || localZ == 102) && localY == 1)
					return directional(Blocks.BARREL, Direction.EAST);
			}
			if (!west && frontRoom) {
				if ((localX == 22 || localX == 32) && localZ >= 82 && localZ <= 101 && localZ % 5 == 0 && localY == 1)
					return localZ % 10 == 0 ? facing(Blocks.ANVIL, Direction.NORTH)
							: Blocks.SMITHING_TABLE.defaultBlockState();
				if (localX >= 25 && localX <= 29 && (localZ == 84 || localZ == 100) && localY >= 1 && localY <= 3)
					return (localX & 1) == 0 ? Blocks.IRON_BARS.defaultBlockState() : Blocks.CHAIN.defaultBlockState();
				if (localX == 34 && localZ == 92 && localY == 1)
					return floorGrindstone(Direction.WEST);
			}
			if (west && !frontRoom) {
				if ((localX == -34 || localX == -20 || localZ == 108 || localZ == 137)
						&& localY >= 1 && localY <= 5 && Math.floorMod(ax + localZ, 5) != 0)
					return Blocks.BOOKSHELF.defaultBlockState();
				if (localX == -27 && localZ == 119 && localY == 1)
					return facing(Blocks.LECTERN, Direction.SOUTH);
				if (localX == -27 && localZ == 128 && localY == 1)
					return Blocks.ENCHANTING_TABLE.defaultBlockState();
			}
			if (!west && !frontRoom) {
				if (localX >= 24 && localX <= 30 && localZ >= 113 && localZ <= 126 && localY == 1)
					return (localX == 24 || localX == 30 || localZ == 113 || localZ == 126)
							? Blocks.DARK_OAK_SLAB.defaultBlockState() : Blocks.CYAN_CARPET.defaultBlockState();
				if ((localX == 22 || localX == 32) && (localZ == 116 || localZ == 123) && localY == 1)
					return stair(Blocks.DARK_OAK_STAIRS, localX == 22 ? Direction.WEST : Direction.EAST);
				if (localX == 34 && localZ >= 130 && localZ <= 136 && localY == 1)
					return Blocks.CARTOGRAPHY_TABLE.defaultBlockState();
			}

			if (localY == 17 && (localZ == 84 || localZ == 96 || localZ == 112 || localZ == 133)
					&& ax >= 22 && ax <= 33)
				return Blocks.BLUE_CARPET.defaultBlockState();
			if (localY == 18 && ax == 33 && (localZ == 86 || localZ == 94 || localZ == 114 || localZ == 132))
				return directional(Blocks.BARREL, localX < 0 ? Direction.EAST : Direction.WEST);
			if ((localY == 14 || localY == 29) && ax == 27
					&& (localZ == 92 || localZ == 118 || localZ == 134))
				return hangingLantern();
			if ((localY == 15 || localY >= 30 && localY <= 32) && ax == 27
					&& (localZ == 92 || localZ == 118 || localZ == 134))
				return Blocks.CHAIN.defaultBlockState();
			return null;
		}

		private BlockState courtyardState(int localX, int localY, int localZ) {
			if (localZ < 22 || localZ > 72 || Math.abs(localX) > 39)
				return null;
			int ax = Math.abs(localX);
			boolean pool = ax >= 18 && ax <= 36 && localZ >= 34 && localZ <= 60;
			if (pool && localY == 1) {
				if (ax == 18 || ax == 36 || localZ == 34 || localZ == 60)
					return Blocks.CALCITE.defaultBlockState();
				return (ax + localZ) % 7 == 0 ? Blocks.SEA_LANTERN.defaultBlockState()
						: Blocks.BLUE_ICE.defaultBlockState();
			}
			for (int centerX : new int[]{-27, 27}) {
				int dx = localX - centerX;
				int dz = localZ - 47;
				if (Math.abs(dx) <= 3 && Math.abs(dz) <= 3 && localY <= 2)
					return localY == 2 ? Blocks.CALCITE.defaultBlockState()
							: Blocks.POLISHED_DEEPSLATE.defaultBlockState();
				int shardHeight = 14 - Math.abs(dx) * 3 - Math.abs(dz) * 2;
				if (localY >= 3 && localY <= shardHeight)
					return (localY + dx + dz) % 4 == 0 ? Blocks.BLUE_ICE.defaultBlockState()
							: Blocks.PACKED_ICE.defaultBlockState();
			}
			for (int brazierX : new int[]{-12, 12}) {
				for (int brazierZ : new int[]{29, 67}) {
					if (localX == brazierX && localZ == brazierZ) {
						if (localY == 1)
							return Blocks.CHISELED_DEEPSLATE.defaultBlockState();
						if (localY == 2)
							return Blocks.SOUL_SOIL.defaultBlockState();
						if (localY == 3)
							return Blocks.SOUL_FIRE.defaultBlockState();
					}
				}
			}
			for (int lampX : new int[]{-9, 9}) {
				for (int lampZ = 24; lampZ <= 70; lampZ += 12) {
					if (localX == lampX && localZ == lampZ) {
						if (localY >= 1 && localY <= 4)
							return Blocks.POLISHED_BASALT.defaultBlockState();
						if (localY == 5)
							return Blocks.SEA_LANTERN.defaultBlockState();
						if (localY == 6)
							return Blocks.BLUE_ICE.defaultBlockState();
					}
				}
			}
			if ((ax == 13 || ax == 39) && (localZ == 28 || localZ == 66) && localY == 1)
				return stair(Blocks.POLISHED_DEEPSLATE_STAIRS,
						localX < 0 ? Direction.EAST : Direction.WEST);
			return null;
		}
	}
}
