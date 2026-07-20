package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.StatueOfGodEntity;
import net.solocraft.entity.StatueaxeEntity;
import net.solocraft.entity.StatuehammerEntity;
import net.solocraft.entity.StatueswordEntity;
import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the Cartenon Temple over multiple server ticks. The generator deliberately
 * limits block inspection and mutation independently so empty air is scanned quickly
 * while dense terrain cannot create a single large tick spike.
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class CartenonTempleGenerator {
	private static final int HALF_WIDTH = 77;
	private static final int MAX_Z = 154;
	private static final int ROOM_CENTER_Z = 77;
	private static final double ROOM_RADIUS = 77.0D;
	private static final double INNER_WALL_RADIUS = 75.0D;
	private static final int MIN_Y = -3;
	private static final int MAX_Y = 52;
	private static final int BLOCK_CHECKS_PER_TICK = 4096;
	private static final int BLOCK_CHANGES_PER_TICK = 512;
	private static final int[] STATUE_X = {31, 49, 58, 59, 51, 32};
	private static final int[] STATUE_Z = {25, 43, 63, 86, 108, 128};
	private static final int[] COLUMN_X = {19, 33, 38, 33, 19, -19, -33, -38, -33, -19};
	private static final int[] COLUMN_Z = {44, 58, 77, 96, 110, 110, 96, 77, 58, 44};
	private static final int[] BRAZIER_Z = {30, 54, 78, 102, 122};
	private static final int[][] GOD_LIGHTS = {
			{0, 22, 140, 9}, {-4, 21, 141, 9}, {4, 21, 141, 9},
			{0, 19, 140, 9}, {-6, 18, 141, 8}, {6, 18, 141, 8},
			{0, 16, 139, 9}, {-5, 15, 140, 8}, {5, 15, 140, 8},
			{0, 13, 139, 8}, {-7, 11, 140, 8}, {7, 11, 140, 8},
			{0, 8, 138, 8}
	};
	private static final Map<BuildKey, BuildJob> ACTIVE_BUILDS = new LinkedHashMap<>();

	private CartenonTempleGenerator() {
	}

	public static boolean start(ServerPlayer target) {
		ServerLevel level = target.serverLevel();
		if (ACTIVE_BUILDS.keySet().stream().anyMatch(key -> key.dimension().equals(level.dimension()))) {
			target.sendSystemMessage(Component.literal("A Cartenon Temple is already being built in this dimension.")
					.withStyle(ChatFormatting.RED));
			return false;
		}

		Direction forward = target.getDirection();
		if (forward.getAxis().isVertical())
			forward = Direction.NORTH;
		BlockPos entranceCenter = target.blockPosition().relative(forward, 14).below();
		if (entranceCenter.getY() + MIN_Y < level.getMinBuildHeight()
				|| entranceCenter.getY() + MAX_Y + 2 >= level.getMaxBuildHeight()) {
			target.sendSystemMessage(Component.literal("There is not enough vertical build space for the Cartenon Temple here.")
					.withStyle(ChatFormatting.RED));
			return false;
		}

		if (!startAt(level, entranceCenter, forward, target.getUUID(), target.getGameProfile().getName(), false, null))
			return false;
		target.sendSystemMessage(Component.literal("Cartenon Temple construction started: 155-block circular chamber, facing "
				+ forward.getName() + ".").withStyle(ChatFormatting.AQUA));
		target.sendSystemMessage(Component.literal("The temple is being placed gradually to protect server TPS.")
				.withStyle(ChatFormatting.DARK_GRAY));
		return true;
	}

	/**
	 * Starts a staged temple build at an exact floor origin. Separate origins in
	 * the same dimension may build concurrently, which is used by multiplayer
	 * Cartenon instances.
	 */
	public static boolean startAt(ServerLevel level, BlockPos origin, Direction forward, UUID ownerId,
			String ownerName, boolean sealedEntrance, Runnable onComplete) {
		if (level == null || origin == null || forward == null || forward.getAxis().isVertical())
			return false;
		if (origin.getY() + MIN_Y < level.getMinBuildHeight()
				|| origin.getY() + MAX_Y + 2 >= level.getMaxBuildHeight())
			return false;
		BuildKey key = new BuildKey(level.dimension(), origin.immutable());
		if (ACTIVE_BUILDS.containsKey(key))
			return false;
		BuildJob job = new BuildJob(level, origin.immutable(), forward, ownerId,
				ownerName == null ? "unknown" : ownerName, sealedEntrance, onComplete);
		ACTIVE_BUILDS.put(key, job);
		return true;
	}

	public static boolean isBuildingAt(ServerLevel level, BlockPos origin) {
		return level != null && origin != null
				&& ACTIVE_BUILDS.containsKey(new BuildKey(level.dimension(), origin.immutable()));
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || ACTIVE_BUILDS.isEmpty())
			return;

		int activeCount = Math.max(1, ACTIVE_BUILDS.size());
		int checksPerJob = Math.max(1024, BLOCK_CHECKS_PER_TICK / activeCount);
		int changesPerJob = Math.max(128, BLOCK_CHANGES_PER_TICK / activeCount);
		List<BuildKey> completed = new ArrayList<>();
		for (Map.Entry<BuildKey, BuildJob> entry : new ArrayList<>(ACTIVE_BUILDS.entrySet())) {
			BuildJob job = entry.getValue();
			if (job.tick(checksPerJob, changesPerJob))
				completed.add(entry.getKey());
		}
		completed.forEach(ACTIVE_BUILDS::remove);
	}

	private record BuildKey(ResourceKey<Level> dimension, BlockPos origin) {
	}

	private static final class BuildJob {
		private final ServerLevel level;
		private final BlockPos origin;
		private final Direction forward;
		private final Direction right;
		private final UUID ownerId;
		private final String ownerName;
		private final boolean sealedEntrance;
		private final Runnable onComplete;
		private final long totalChecks;
		private int localX = -HALF_WIDTH;
		private int localZ;
		private int localY = MIN_Y;
		private long checked;
		private long changed;
		private int lastProgress = -1;
		private boolean blockPassFinished;

		private BuildJob(ServerLevel level, BlockPos origin, Direction forward, UUID ownerId, String ownerName,
				boolean sealedEntrance, Runnable onComplete) {
			this.level = level;
			this.origin = origin;
			this.forward = forward;
			this.right = forward.getClockWise();
			this.ownerId = ownerId;
			this.ownerName = ownerName;
			this.sealedEntrance = sealedEntrance;
			this.onComplete = onComplete;
			this.totalChecks = (long) (HALF_WIDTH * 2 + 1) * (MAX_Z + 1) * (MAX_Y - MIN_Y + 1);
		}

		private boolean tick(int checkBudget, int changeBudget) {
			if (blockPassFinished)
				return finish();

			int checksThisTick = 0;
			int changesThisTick = 0;
			while (!blockPassFinished && checksThisTick < checkBudget && changesThisTick < changeBudget) {
				BlockState desired = desiredState(localX, localY, localZ);
				if (desired != null) {
					BlockPos worldPos = toWorld(localX, localY, localZ);
					BlockState existing = level.getBlockState(worldPos);
					if (!existing.equals(desired)) {
						level.setBlock(worldPos, desired, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
						changesThisTick++;
						changed++;
					}
				}
				checksThisTick++;
				checked++;
				advanceCursor();
			}

			reportProgress();
			return false;
		}

		private void advanceCursor() {
			localX++;
			if (localX <= HALF_WIDTH)
				return;
			localX = -HALF_WIDTH;
			localZ++;
			if (localZ <= MAX_Z)
				return;
			localZ = 0;
			localY++;
			if (localY > MAX_Y)
				blockPassFinished = true;
		}

		private void reportProgress() {
			int progress = (int) Math.min(100L, checked * 100L / totalChecks);
			int step = progress / 10;
			if (step <= lastProgress)
				return;
			lastProgress = step;
			ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
			if (owner != null) {
				owner.displayClientMessage(Component.literal("Cartenon Temple  " + Math.min(100, step * 10) + "%")
						.withStyle(ChatFormatting.DARK_AQUA), true);
			}
		}

		private boolean finish() {
			removePreviousTempleStatues();
			spawnTempleStatues();
			// Versioned hidden completion marker. A partial or legacy build never receives this block.
			level.setBlock(toWorld(0, -2, 0), Blocks.LODESTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			BlockPos center = toWorld(0, 1, 78);
			level.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.6F, 0.62F);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.getX() + 0.5D, center.getY() + 1.5D,
					center.getZ() + 0.5D, 80, 5.0D, 1.2D, 5.0D, 0.025D);

			ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
			Component message = Component.literal("Cartenon Temple complete for " + ownerName + ": " + changed
					+ " blocks changed and 13 statues placed.").withStyle(ChatFormatting.GREEN);
			if (owner != null)
				owner.sendSystemMessage(message);
			else
				SololevelingMod.LOGGER.info(message.getString());
			if (onComplete != null) {
				try {
					onComplete.run();
				} catch (RuntimeException exception) {
					SololevelingMod.LOGGER.error("Cartenon Temple completion callback failed", exception);
				}
			}
			return true;
		}

		private BlockPos toWorld(int x, int y, int z) {
			return origin.relative(right, x).relative(forward, z).above(y);
		}

		private BlockState desiredState(int x, int y, int z) {
			if (!isInsideFootprint(x, z)) {
				// Dedicated instances may contain the old rectangular chamber. Clear only
				// those legacy corners; command builds never erase surrounding terrain.
				return sealedEntrance && Math.abs(x) <= 50 ? Blocks.AIR.defaultBlockState() : null;
			}
			int roof = roofHeight(x, z);
			if (y < MIN_Y || y > roof + 1)
				return null;

			if (y == -3)
				return Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
			if (y == -2)
				return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
			if (y == -1)
				return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			if (y == 0)
				return floorState(x, z);

			boolean frontOpening = !sealedEntrance && z <= 4 && isFrontOpening(x, y);
			if (isOuterShell(x, z)) {
				if (frontOpening)
					return Blocks.AIR.defaultBlockState();
				return wallState(x, y, z);
			}

			if (y == roof + 1)
				return Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
			if (y == roof)
				return ceilingState(x, z);

			BlockState godLight = statueGodLightState(x, y, z);
			if (godLight != null)
				return godLight;

			BlockState dais = daisState(x, y, z);
			if (dais != null)
				return dais;

			BlockState throne = throneState(x, y, z);
			if (throne != null)
				return throne;

			BlockState pedestal = pedestalState(x, y, z);
			if (pedestal != null)
				return pedestal;

			BlockState niche = nicheState(x, y, z);
			if (niche != null)
				return niche;

			BlockState column = columnState(x, y, z);
			if (column != null)
				return column;

			BlockState arch = archState(x, y, z, 14, 15, 18);
			if (arch == null)
				arch = archState(x, y, z, 124, 19, 21);
			if (arch != null)
				return arch;

			BlockState brazier = brazierState(x, y, z);
			if (brazier != null)
				return brazier;

			BlockState lamp = hangingLampState(x, y, z, roof);
			if (lamp != null)
				return lamp;

			return Blocks.AIR.defaultBlockState();
		}

		private int roofHeight(int x, int z) {
			double normalized = Math.min(1.0D, roomRadius(x, z) / INNER_WALL_RADIUS);
			double dome = Math.sqrt(Math.max(0.0D, 1.0D - normalized * normalized));
			return 34 + (int) Math.round(dome * 16.0D);
		}

		private boolean isInsideFootprint(int x, int z) {
			return roomRadius(x, z) <= ROOM_RADIUS + 0.35D;
		}

		private boolean isOuterShell(int x, int z) {
			return roomRadius(x, z) >= INNER_WALL_RADIUS;
		}

		private double roomRadius(int x, int z) {
			double dz = z - ROOM_CENTER_Z;
			return Math.sqrt((double) x * x + dz * dz);
		}

		private boolean isFrontOpening(int x, int y) {
			int absX = Math.abs(x);
			if (y <= 11)
				return absX <= 7;
			if (y > 19)
				return false;
			int dy = y - 11;
			return x * x + dy * dy <= 64;
		}

		private BlockState wallState(int x, int y, int z) {
			double radius = roomRadius(x, z);
			if (radius >= ROOM_RADIUS - 0.55D)
				return Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
			if (y <= 2)
				return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
			if (y % 9 == 0)
				return Blocks.POLISHED_ANDESITE.defaultBlockState();
			if ((z + y * 3 + Math.abs(x)) % 31 == 0)
				return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
			if (y % 9 == 1 && ((int) Math.round(radius * 2.0D) / 6) % 2 == 0)
				return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
			return radius >= ROOM_RADIUS - 1.35D ? Blocks.DEEPSLATE_BRICKS.defaultBlockState()
					: Blocks.STONE_BRICKS.defaultBlockState();
		}

		private BlockState ceilingState(int x, int z) {
			double radius = roomRadius(x, z);
			if (Math.abs(radius - 58.0D) < 0.8D || Math.abs(radius - 38.0D) < 0.72D
					|| Math.abs(x) <= 1 || Math.abs(z - ROOM_CENTER_Z) <= 1)
				return Blocks.POLISHED_BASALT.defaultBlockState();
			if ((Math.abs(x) + z) % 17 == 0)
				return Blocks.CHISELED_DEEPSLATE.defaultBlockState();
			return Blocks.DEEPSLATE_TILES.defaultBlockState();
		}

		private BlockState floorState(int x, int z) {
			int absX = Math.abs(x);
			double chamberRadius = roomRadius(x, z);
			if (chamberRadius >= INNER_WALL_RADIUS - 2.0D)
				return Blocks.POLISHED_DEEPSLATE.defaultBlockState();

			int circleZ = z - ROOM_CENTER_Z;
			double radius = Math.sqrt((double) x * x + (double) circleZ * circleZ);
			if (Math.abs(radius - 60.0D) < 0.72D || Math.abs(radius - 44.0D) < 0.7D
					|| Math.abs(radius - 27.0D) < 0.72D || Math.abs(radius - 18.0D) < 0.65D
					|| Math.abs(radius - 9.0D) < 0.58D)
				return Blocks.GILDED_BLACKSTONE.defaultBlockState();
			if (radius < 4.0D)
				return (x + circleZ) % 2 == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
						: Blocks.CHISELED_DEEPSLATE.defaultBlockState();
			if (radius < 66.0D && (Math.abs(x) <= 1 || Math.abs(circleZ) <= 1
					|| Math.abs(Math.abs(x) - Math.abs(circleZ)) <= 1))
				return Blocks.DARK_PRISMARINE.defaultBlockState();

			if (absX <= 5) {
				if (z % 12 == 0 && absX <= 1)
					return Blocks.SEA_LANTERN.defaultBlockState();
				return z % 8 <= 1 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
						: Blocks.POLISHED_ANDESITE.defaultBlockState();
			}
			if (radius >= 36.0D && radius <= 39.0D)
				return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
			if ((Math.floorDiv(x, 7) + Math.floorDiv(z, 9)) % 2 == 0)
				return Blocks.DEEPSLATE_TILES.defaultBlockState();
			return Blocks.STONE_BRICKS.defaultBlockState();
		}

		private BlockState pedestalState(int x, int y, int z) {
			for (int i = 0; i < STATUE_Z.length; i++) {
				int centerZ = STATUE_Z[i];
				for (int centerX : new int[]{-STATUE_X[i], STATUE_X[i]}) {
					int dx = Math.abs(x - centerX);
					int dz = Math.abs(z - centerZ);
					if (y == 1 && dx <= 4 && dz <= 4)
						return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
					if (y == 2 && dx <= 3 && dz <= 3)
						return (dx == 3 || dz == 3) ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
								: Blocks.DEEPSLATE_BRICKS.defaultBlockState();
					if (y == 3 && dx <= 2 && dz <= 2)
						return dx == 0 && dz == 0 ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
								: Blocks.POLISHED_DEEPSLATE.defaultBlockState();
				}
			}
			return null;
		}

		private BlockState nicheState(int x, int y, int z) {
			if (y < 3 || y > 20)
				return null;
			for (int centerZ : STATUE_Z) {
				int dz = Math.abs(z - centerZ);
				if (dz > 5)
					continue;
				double radialZ = z - ROOM_CENTER_Z;
				double wallXSquared = 73.0D * 73.0D - radialZ * radialZ;
				if (wallXSquared <= 0.0D)
					continue;
				int wallX = (int) Math.round(Math.sqrt(wallXSquared));
				if (Math.abs(Math.abs(x) - wallX) > 1)
					continue;
				if (dz == 5 || y == 3 || y == 20)
					return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
				if ((y == 10 || y == 16) && dz <= 4)
					return Blocks.POLISHED_ANDESITE.defaultBlockState();
				if (dz == 0 && (y == 8 || y == 15))
					return Blocks.SEA_LANTERN.defaultBlockState();
				return Blocks.DARK_PRISMARINE.defaultBlockState();
			}
			return null;
		}

		private BlockState columnState(int x, int y, int z) {
			for (int i = 0; i < COLUMN_Z.length; i++) {
				int dx = Math.abs(x - COLUMN_X[i]);
				int dz = Math.abs(z - COLUMN_Z[i]);
				if (y <= 2 && dx <= 3 && dz <= 3)
					return y == 2 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
							: Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
				if (y >= 3 && y <= 31 && dx <= 1 && dz <= 1) {
					if (y % 8 == 0)
						return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
					return Blocks.POLISHED_BASALT.defaultBlockState();
				}
				if (y >= 32 && y <= 34 && dx <= 3 && dz <= 3)
					return y == 32 ? Blocks.POLISHED_ANDESITE.defaultBlockState()
							: Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			}
			return null;
		}

		private BlockState archState(int x, int y, int z, int centerZ, int halfWidth, int springY) {
			if (Math.abs(z - centerZ) > 1 || y < 1)
				return null;
			int absX = Math.abs(x);
			if (y <= springY && absX >= halfWidth - 2 && absX <= halfWidth)
				return archMaterial(y, z);
			int dy = y - springY;
			if (dy < 0 || dy > halfWidth)
				return null;
			int edge = (int) Math.round(Math.sqrt((double) halfWidth * halfWidth - (double) dy * dy));
			if (Math.abs(absX - edge) <= 1)
				return archMaterial(y, z);
			return null;
		}

		private BlockState archMaterial(int y, int z) {
			if ((y + z) % 5 == 0)
				return Blocks.GILDED_BLACKSTONE.defaultBlockState();
			return y % 4 == 0 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
					: Blocks.POLISHED_DEEPSLATE.defaultBlockState();
		}

		private BlockState brazierState(int x, int y, int z) {
			if (Math.abs(x) != 19)
				return null;
			for (int centerZ : BRAZIER_Z) {
				if (z != centerZ)
					continue;
				if (y == 1)
					return Blocks.CHISELED_DEEPSLATE.defaultBlockState();
				if (y == 2)
					return Blocks.SOUL_SOIL.defaultBlockState();
				if (y == 3)
					return Blocks.SOUL_FIRE.defaultBlockState();
			}
			return null;
		}

		private BlockState statueGodLightState(int x, int y, int z) {
			if (z < 138 || z > 141 || y < 8 || y > 22)
				return null;
			for (int[] light : GOD_LIGHTS) {
				if (x == light[0] && y == light[1] && z == light[2])
					return Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, light[3]);
			}
			return null;
		}

		private BlockState hangingLampState(int x, int y, int z, int roof) {
			if (x != 0 && x != -16 && x != 16)
				return null;
			if (z < 24 || z > 132 || z % 18 != 6)
				return null;
			int lanternY = roof - 12;
			if (y == lanternY)
				return Blocks.SOUL_LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true);
			if (y > lanternY && y < roof)
				return Blocks.CHAIN.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
			return null;
		}

		private BlockState daisState(int x, int y, int z) {
			int height = daisHeight(x, z);
			if (height <= 0 || y < 1 || y > height)
				return null;
			if (y == height)
				return (Math.abs(x) + z) % 7 == 0 ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
						: Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
			return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
		}

		private int daisHeight(int x, int z) {
			if (z < 126)
				return 0;
			int tier = Mth.clamp(1 + (z - 126) / 4, 1, 6);
			int width = 31 - tier * 2;
			return Math.abs(x) <= width ? tier : 0;
		}

		private BlockState throneState(int x, int y, int z) {
			int absX = Math.abs(x);
			if (z >= 149 && z <= 151 && y >= 6 && y <= 29) {
				int width = Math.max(3, 10 - (y - 6) / 4);
				if (absX <= width) {
					if (absX == width || y % 6 == 0)
						return Blocks.GILDED_BLACKSTONE.defaultBlockState();
					return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
				}
			}
			if (absX >= 9 && absX <= 11 && z >= 140 && z <= 149 && y >= 6 && y <= 12)
				return y == 12 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
						: Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
			if (z == 148 && y >= 27 && y <= 31 && absX <= 12 - Math.abs(y - 29) * 3)
				return Blocks.GILDED_BLACKSTONE.defaultBlockState();
			return null;
		}

		private void removePreviousTempleStatues() {
			BlockPos first = toWorld(-HALF_WIDTH, 0, 0);
			BlockPos second = toWorld(HALF_WIDTH, MAX_Y, MAX_Z);
			AABB bounds = new AABB(first, second).inflate(4.0D);
			for (Mob mob : level.getEntitiesOfClass(Mob.class, bounds,
					entity -> entity.getPersistentData().getBoolean("CartenonTempleStatue"))) {
				mob.discard();
			}
		}

		private void spawnTempleStatues() {
			EntityType<?>[] types = {
					SololevelingModEntities.STATUEAXE.get(),
					SololevelingModEntities.STATUEHAMMER.get(),
					SololevelingModEntities.STATUESWORD.get()
			};
			for (int i = 0; i < STATUE_Z.length; i++) {
				spawnStatue(types[i % types.length], -STATUE_X[i], 4, STATUE_Z[i], 0, ROOM_CENTER_Z);
				spawnStatue(types[(i + 1) % types.length], STATUE_X[i], 4, STATUE_Z[i], 0, ROOM_CENTER_Z);
			}
			spawnStatue(SololevelingModEntities.STATUE_OF_GOD.get(), 0, daisHeight(0, 145) + 1, 145, 0, ROOM_CENTER_Z);
		}

		private void spawnStatue(EntityType<?> type, int x, int y, int z, int lookX, int lookZ) {
			if (!(type.create(level) instanceof Mob statue))
				return;
			BlockPos spawnPos = toWorld(x, y, z);
			BlockPos lookPos = toWorld(lookX, y, lookZ);
			double dx = lookPos.getX() - spawnPos.getX();
			double dz = lookPos.getZ() - spawnPos.getZ();
			float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
			statue.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, yaw, 0.0F);
			statue.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null, null);
			statue.setYRot(yaw);
			statue.setYBodyRot(yaw);
			statue.setYHeadRot(yaw);
			statue.getPersistentData().putFloat("CartenonHomeYaw", yaw);
			statue.setNoAi(true);
			statue.setPersistenceRequired();
			statue.getPersistentData().putBoolean("CartenonTempleStatue", true);
			if (statue instanceof StatueaxeEntity axe)
				axe.setTempleScale(2.0F);
			else if (statue instanceof StatuehammerEntity hammer)
				hammer.setTempleScale(2.0F);
			else if (statue instanceof StatueswordEntity sword)
				sword.setTempleScale(2.0F);
			else if (statue instanceof StatueOfGodEntity god) {
				god.getEntityData().set(StatueOfGodEntity.DATA_default_x, spawnPos.getX());
				god.getEntityData().set(StatueOfGodEntity.DATA_default_y, spawnPos.getY());
				god.getEntityData().set(StatueOfGodEntity.DATA_default_z, spawnPos.getZ());
			}
			statue.refreshDimensions();
			level.addFreshEntity(statue);
		}
	}
}
