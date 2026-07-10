package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber
public class DungeonVoidSafetyProcedure {
	private static final String LAST_RESCUE_TICK = "slr_dungeon_void_rescue_tick";
	private static final int RESCUE_SCAN_RADIUS = 24;
	private static final int FALL_Y_THRESHOLD = 0;
	private static final int RESCUE_COOLDOWN_TICKS = 20;

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player))
			return;
		if (!(player.level() instanceof ServerLevel level) || !isSoloDungeonDimension(level))
			return;
		if (player.getY() >= Math.max(level.getMinBuildHeight() + 12, FALL_Y_THRESHOLD))
			return;
		long gameTime = level.getGameTime();
		if (gameTime - player.getPersistentData().getLong(LAST_RESCUE_TICK) < RESCUE_COOLDOWN_TICKS)
			return;
		player.getPersistentData().putLong(LAST_RESCUE_TICK, gameTime);
		rescue(player, level);
	}

	private static boolean isSoloDungeonDimension(ServerLevel level) {
		ResourceLocation id = level.dimension().location();
		return "sololeveling".equals(id.getNamespace()) && id.getPath().startsWith("dungeon_dimension_");
	}

	private static void rescue(ServerPlayer player, ServerLevel level) {
		BlockPos current = BlockPos.containing(player.getX(), player.getY(), player.getZ());
		Optional<BlockPos> safePos = findSafePosition(level, current, RESCUE_SCAN_RADIUS);
		if (safePos.isEmpty()) {
			BlockPos anchor = fallbackAnchor(player);
			safePos = findSafePosition(level, anchor, RESCUE_SCAN_RADIUS + 16);
		}
		BlockPos target = safePos.orElse(level.getSharedSpawnPos().above());
		player.setNoGravity(false);
		player.fallDistance = 0;
		player.setDeltaMovement(Vec3.ZERO);
		player.connection.teleport(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, player.getYRot(), player.getXRot());
	}

	private static BlockPos fallbackAnchor(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		ResourceLocation id = player.level().dimension().location();
		if ("dungeon_dimension_dkc".equals(id.getPath()) && hasStoredPosition(vars.dkc_x, vars.dkc_y, vars.dkc_z))
			return BlockPos.containing(vars.dkc_x + 100, vars.dkc_y + 4, vars.dkc_z + 4);
		if (hasStoredPosition(vars.DunX, vars.DunY, vars.DunZ))
			return BlockPos.containing(player.getX(), vars.DunY, player.getZ());
		return BlockPos.containing(player.getX(), 64, player.getZ());
	}

	private static boolean hasStoredPosition(double x, double y, double z) {
		return x != 0 || y != 0 || z != 0;
	}

	private static Optional<BlockPos> findSafePosition(ServerLevel level, BlockPos anchor, int radius) {
		int minY = level.getMinBuildHeight() + 1;
		int maxY = level.getMaxBuildHeight() - 2;
		for (int r = 0; r <= radius; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r)
						continue;
					int x = anchor.getX() + dx;
					int z = anchor.getZ() + dz;
					for (int y = minY; y <= maxY; y++) {
						BlockPos pos = new BlockPos(x, y, z);
						if (isSafeStandingPosition(level, pos))
							return Optional.of(pos);
					}
				}
			}
		}
		return Optional.empty();
	}

	private static boolean isSafeStandingPosition(ServerLevel level, BlockPos pos) {
		BlockPos belowPos = pos.below();
		BlockState below = level.getBlockState(belowPos);
		BlockState here = level.getBlockState(pos);
		BlockState above = level.getBlockState(pos.above());
		return below.isFaceSturdy(level, belowPos, Direction.UP) && below.getFluidState().isEmpty() && here.getFluidState().isEmpty() && above.getFluidState().isEmpty() && here.getCollisionShape(level, pos).isEmpty()
				&& above.getCollisionShape(level, pos.above()).isEmpty();
	}
}
