package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.JobChangeQuestManager;
import net.solocraft.util.InstanceDungeonKeyAccess;
import net.solocraft.util.StoryModeIntroSavedData;
import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.runtime.DungeonEncounterRuntime;
import net.solocraft.dungeon.runtime.DungeonInstanceSavedData;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.dungeon.runtime.SnowRedGateArenaManager;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class DungeonDimensionPlayerLeavesDimensionProcedure {
	private static final double RETURN_X_OFFSET = 3.0D;
	private static final String STORY_INTRO_DUNGEON_TAG =
			"story_intro_ancient_golem";

	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (!((sourceentity.level().dimension()) == Level.OVERWORLD)) {
			if ((sourceentity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sololeveling:dungeon_dimension_kasaka")))
					|| (sourceentity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sololeveling:dungeon_dimension_igris")))) {
				boolean isIgrisDungeon = (sourceentity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sololeveling:dungeon_dimension_igris")));
				boolean canLeave = isIgrisDungeon ? JobChangeQuestManager.isFinished(sourceentity) : (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).instancecomplete == true;
				if (canLeave) {
					if (!isIgrisDungeon && sourceentity instanceof Player player)
						InstanceDungeonKeyAccess.markCompleted(player);
					if (!entity.level().isClientSide())
						entity.discard();
					sourceentity.getPersistentData().putString("dungeon_tag", (entity.getStringUUID()));
					if (sourceentity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
						ResourceKey<Level> destinationType = Level.OVERWORLD;
						if (_player.level().dimension() == destinationType)
							return;
						ServerLevel nextLevel = _player.server.getLevel(destinationType);
						if (nextLevel != null) {
							_player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
							_player.teleportTo(nextLevel, _player.getX(), _player.getY(), _player.getZ(), _player.getYRot(), _player.getXRot());
							_player.connection.send(new ClientboundPlayerAbilitiesPacket(_player.getAbilities()));
							for (MobEffectInstance _effectinstance : _player.getActiveEffects())
								_player.connection.send(new ClientboundUpdateMobEffectPacket(_player.getId(), _effectinstance, false));
							_player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
						}
					}
					SololevelingMod.queueServerWork(2, () -> {
						{
							Entity _ent = sourceentity;
							_ent.teleportTo(((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunX + RETURN_X_OFFSET),
									((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunY),
									((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunZ));
							if (_ent instanceof ServerPlayer _serverPlayer)
								_serverPlayer.connection.teleport(((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunX + RETURN_X_OFFSET),
										((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunY),
										((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunZ), _ent.getYRot(), _ent.getXRot());
						}
						{
							boolean _setval = false;
							sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.instancecomplete = _setval;
								capability.syncPlayerVariables(sourceentity);
							});
						}
					});
				} else {
					if (sourceentity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal(isIgrisDungeon ? "You cant leave the Job Change Quest before it is complete" : "You cant leave before you complete the dungeon"), true);
				}
			} else if ((sourceentity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sololeveling:dungeon_dimension_snow")))) {
				boolean hasScopedInstance = !sourceentity.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG).isBlank()
						|| !entity.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG).isBlank();
				Optional<DungeonInstanceSavedData.Instance> scopedInstance = runtimeInstance(sourceentity, entity);
				if (hasScopedInstance) {
					boolean scopedCompletion = scopedInstance.map(instance -> instance.completed()
							&& instance.participants().contains(sourceentity.getUUID())).orElse(false);
					if (!scopedCompletion) {
						if (sourceentity instanceof Player _player && !_player.level().isClientSide())
							_player.displayClientMessage(Component.literal("You cant leave a red gate before you defeat the boss"), true);
						return;
					}
					String dungeonTag = currentDungeonTag(sourceentity, entity);
					markGateCleared(world, dungeonTag);
					if (!returnToSavedOverworld(sourceentity))
						return;
					if (sourceentity instanceof ServerPlayer player)
						recordStoryDungeonClear(player, dungeonTag, scopedInstance);
					sourceentity.getPersistentData().putBoolean("slr_procedural_dungeon", false);
					sourceentity.getPersistentData().putBoolean("slr_procedural_red_gate", false);
					resetDungeonReturnState(sourceentity);
					if (scopedInstance.map(instance -> instance.completed() && instance.participants().isEmpty()).orElse(false)
							&& !entity.level().isClientSide())
						entity.discard();
					return;
				}

				// Compatibility path for snow-dungeon saves created before scoped runtime
				// instances existed. New red gates must never use this global branch.
				if (!(sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).BossKilled) {
					if (sourceentity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("You cant leave a red gate before you defeat the boss"), true);
					return;
				}
				for (Entity entityiterator : new ArrayList<>(world.players())) {
					if ((entityiterator.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sololeveling:dungeon_dimension_snow")))) {
						if (!entityiterator.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG).isBlank())
							continue;
						String dungeonTag = currentDungeonTag(entityiterator, entity);
						if (!returnToSavedOverworld(entityiterator))
							continue;
						if (entityiterator instanceof ServerPlayer player)
							recordStoryDungeonClear(player, dungeonTag,
									Optional.empty());
						entityiterator.getPersistentData().putString("dungeon_tag", "");
						entityiterator.getPersistentData().putBoolean("slr_procedural_dungeon", false);
						entityiterator.getPersistentData().putBoolean("slr_procedural_red_gate", false);
						resetDungeonReturnState(entityiterator);
					}
				}
			} else if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dungeoning
					|| !sourceentity.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG).isBlank()
					|| (entity != null && !entity.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG).isBlank())) {
				boolean proceduralRedGate = sourceentity.getPersistentData().getBoolean("slr_procedural_red_gate");
				boolean hasScopedInstance = !sourceentity.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG).isBlank()
						|| (entity != null && !entity.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG).isBlank());
				Optional<DungeonInstanceSavedData.Instance> scopedInstance = runtimeInstance(sourceentity, entity);
				boolean scopedCompletion = hasScopedInstance
						? scopedInstance.map(instance -> instance.completed()
								&& instance.participants().contains(sourceentity.getUUID())).orElse(false)
						: (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new SololevelingModVariables.PlayerVariables())).BossKilled;
				if (proceduralRedGate && !scopedCompletion) {
					if (sourceentity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("You cant leave a red gate before you defeat the boss"), true);
					return;
				}
				String dungeonTag = currentDungeonTag(sourceentity, entity);
				if (scopedCompletion)
					markGateCleared(world, dungeonTag);
				if (!returnToSavedOverworld(sourceentity))
					return;
				if (scopedCompletion && sourceentity instanceof ServerPlayer player)
					recordStoryDungeonClear(player, dungeonTag, scopedInstance);
				if (!dungeonTag.isEmpty())
					sourceentity.getPersistentData().putString("dungeon_tag", dungeonTag);
				sourceentity.getPersistentData().putBoolean("slr_procedural_dungeon", false);
				sourceentity.getPersistentData().putBoolean("slr_procedural_red_gate", false);
				resetDungeonReturnState(sourceentity);
				if (scopedInstance.map(instance -> instance.completed() && instance.participants().isEmpty()).orElse(false)
						&& !entity.level().isClientSide())
					entity.discard();
			}
		}
	}

	private static boolean returnToSavedOverworld(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || player.level().isClientSide())
			return false;
		ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
		if (overworld == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		double returnX = vars.DunX + RETURN_X_OFFSET;
		double returnY = vars.DunY;
		double returnZ = vars.DunZ;
		BlockPos safeReturn = findSafeOverworldReturn(overworld,
				returnX, returnY, returnZ);
		player.setNoGravity(false);
		player.setDeltaMovement(0.0D, 0.0D, 0.0D);
		player.fallDistance = 0.0F;
		player.teleportTo(overworld, safeReturn.getX() + 0.5D,
				safeReturn.getY(), safeReturn.getZ() + 0.5D,
				player.getYRot(), player.getXRot());
		return true;
	}

	/**
	 * Self-service recovery path used by the permission-free /slr escape command.
	 * It never marks a gate complete or awards progression; it only detaches the
	 * caller from their current runtime instance and returns them safely.
	 */
	public static boolean emergencyExit(ServerPlayer player) {
		if (player == null || player.server == null)
			return false;
		boolean hasRuntimeBinding = !player.getPersistentData()
				.getString(DungeonMobLevelAdapter.INSTANCE_TAG).isBlank();
		boolean dungeoning = player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(capability -> capability.dungeoning)
				.orElse(false);
		if (player.level().dimension().equals(Level.OVERWORLD)
				&& !hasRuntimeBinding && !dungeoning)
			return false;

		ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
		if (overworld == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		double returnX = vars.DunX + RETURN_X_OFFSET;
		double returnY = vars.DunY;
		double returnZ = vars.DunZ;
		if (returnX == RETURN_X_OFFSET && returnY == 0.0D && returnZ == 0.0D) {
			BlockPos spawn = overworld.getSharedSpawnPos();
			returnX = spawn.getX() + 0.5D;
			returnY = spawn.getY() + 1.0D;
			returnZ = spawn.getZ() + 0.5D;
		}
		BlockPos safeReturn = findSafeOverworldReturn(overworld,
				returnX, returnY, returnZ);

		resetDungeonReturnState(player);
		player.getPersistentData().remove("dungeon_tag");
		player.getPersistentData().putBoolean("slr_procedural_dungeon", false);
		player.getPersistentData().putBoolean("slr_procedural_red_gate", false);
		player.setNoGravity(false);
		player.setDeltaMovement(0.0D, 0.0D, 0.0D);
		player.fallDistance = 0.0F;
		overworld.getChunk(safeReturn);
		player.teleportTo(overworld, safeReturn.getX() + 0.5D,
				safeReturn.getY(), safeReturn.getZ() + 0.5D,
				player.getYRot(), player.getXRot());
		return true;
	}

	private static BlockPos findSafeOverworldReturn(ServerLevel overworld,
			double requestedX, double requestedY, double requestedZ) {
		BlockPos requested = BlockPos.containing(requestedX, requestedY,
				requestedZ);
		overworld.getChunk(requested);
		int[] verticalOffsets = {0, 1, -1, 2, -2, 3, -3, 4, -4};
		for (int radius = 0; radius <= 8; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius)
						continue;
					for (int dy : verticalOffsets) {
						BlockPos candidate = requested.offset(dx, dy, dz);
						if (isSafeStandingPosition(overworld, candidate))
							return candidate;
					}
					int surfaceY = overworld.getHeight(
							Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
							requested.getX() + dx, requested.getZ() + dz);
					BlockPos surface = new BlockPos(requested.getX() + dx,
							surfaceY, requested.getZ() + dz);
					if (isSafeStandingPosition(overworld, surface))
						return surface;
				}
			}
		}
		// A gate over water has no dry footing anywhere in range, and teleporting
		// the player to world spawn from the far side of the map is far worse than
		// putting them back where they went in. Surfacing them at the gate column
		// leaves them treading water next to their own gate, which is recoverable.
		BlockPos surfaced = surfaceAboveFluid(overworld, requested);
		if (surfaced != null)
			return surfaced;

		BlockPos spawn = overworld.getSharedSpawnPos().above();
		overworld.getChunk(spawn);
		if (isSafeStandingPosition(overworld, spawn))
			return spawn;
		int surfaceY = overworld.getHeight(
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				spawn.getX(), spawn.getZ());
		return new BlockPos(spawn.getX(), surfaceY, spawn.getZ());
	}

	/**
	 * Last resort before world spawn: the first open, non-solid block at or above
	 * the requested column's surface. Standing in water is acceptable here; being
	 * teleported across the world is not.
	 */
	private static BlockPos surfaceAboveFluid(ServerLevel overworld,
			BlockPos requested) {
		int surfaceY = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				requested.getX(), requested.getZ());
		if (surfaceY <= overworld.getMinBuildHeight())
			return null;
		for (int y = surfaceY; y < Math.min(surfaceY + 6,
				overworld.getMaxBuildHeight() - 1); y++) {
			BlockPos candidate = new BlockPos(requested.getX(), y, requested.getZ());
			BlockState feet = overworld.getBlockState(candidate);
			BlockState head = overworld.getBlockState(candidate.above());
			if (feet.getCollisionShape(overworld, candidate).isEmpty()
					&& head.getCollisionShape(overworld, candidate.above()).isEmpty())
				return candidate;
		}
		return null;
	}

	private static boolean isSafeStandingPosition(ServerLevel level,
			BlockPos position) {
		if (position.getY() <= level.getMinBuildHeight()
				|| position.getY() >= level.getMaxBuildHeight() - 1)
			return false;
		BlockPos belowPos = position.below();
		BlockState below = level.getBlockState(belowPos);
		BlockState feet = level.getBlockState(position);
		BlockState head = level.getBlockState(position.above());
		return below.isFaceSturdy(level, belowPos, Direction.UP)
				&& below.getFluidState().isEmpty()
				&& feet.getFluidState().isEmpty()
				&& head.getFluidState().isEmpty()
				&& feet.getCollisionShape(level, position).isEmpty()
				&& head.getCollisionShape(level, position.above()).isEmpty();
	}

	/**
	 * Detaches a player from a completed dungeon when an alternate exit (currently
	 * Cartenon) teleports them somewhere other than the normal return portal.
	 */
	public static void completeAlternateExit(ServerPlayer player) {
		if (player == null)
			return;
		String dungeonTag = player.getPersistentData().getString("dungeon_tag");
		Optional<DungeonInstanceSavedData.Instance> scopedInstance =
				runtimeInstance(player, null);
		markGateCleared(player.serverLevel(), dungeonTag);
		recordStoryDungeonClear(player, dungeonTag, scopedInstance);
		resetDungeonReturnState(player);
		player.getPersistentData().remove("dungeon_tag");
		player.getPersistentData().putBoolean("slr_procedural_dungeon", false);
		player.getPersistentData().putBoolean("slr_procedural_red_gate", false);
		player.setNoGravity(false);
		player.fallDistance = 0.0F;
	}

	private static String currentDungeonTag(Entity player, Entity returnPortal) {
		String tag = player.getPersistentData().getString("dungeon_tag");
		if (tag == null || tag.isEmpty())
			tag = returnPortal.getPersistentData().getString("dungeon_tag");
		return tag == null ? "" : tag;
	}

	private static void markGateCleared(LevelAccessor world, String dungeonTag) {
		if (dungeonTag == null || dungeonTag.isEmpty())
			return;
		String token = dungeonTag + ",";
		if (!SololevelingModVariables.MapVariables.get(world).GatesCleared.contains(token)) {
			SololevelingModVariables.MapVariables.get(world).GatesCleared = SololevelingModVariables.MapVariables.get(world).GatesCleared + token;
			SololevelingModVariables.MapVariables.get(world).syncData(world);
		}
	}

	private static void recordStoryDungeonClear(ServerPlayer player,
			String dungeonTag,
			Optional<DungeonInstanceSavedData.Instance> scopedInstance) {
		if (player == null || STORY_INTRO_DUNGEON_TAG.equals(dungeonTag))
			return;
		String receipt = scopedInstance
				.map(instance -> "instance:" + instance.id())
				.orElseGet(() -> dungeonTag == null || dungeonTag.isBlank()
						? "" : "gate:" + dungeonTag);
		StoryModeIntroSavedData.get(player.server)
				.recordPostIntroDungeonClear(player.getUUID(), receipt);
	}

	private static void resetDungeonReturnState(Entity entity) {
		String instanceText = entity.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG);
		if (entity instanceof ServerPlayer player && !instanceText.isBlank()) {
			try {
				DungeonInstanceSavedData registry = DungeonInstanceSavedData.get(player.serverLevel());
				registry.getInstance(UUID.fromString(instanceText)).ifPresent(instance -> {
					DungeonEncounterRuntime.clearHighlightsFor(player, instance);
					instance.removeParticipant(player.getUUID());
					SnowRedGateArenaManager.onParticipantExited(player.getServer(), instance);
					registry.pruneCompletedEmptyInstances();
				});
			} catch (IllegalArgumentException ignored) {
			}
		}
		entity.getPersistentData().remove(DungeonMobLevelAdapter.INSTANCE_TAG);
		entity.getPersistentData().remove(SnowRedGateArenaManager.TERRITORY_TAG);
		entity.getPersistentData().remove("slr_red_gate_wave_notice");
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.BossKilled = false;
			capability.dungeoning = false;
			capability.syncPlayerVariables(entity);
		});
	}

	private static Optional<DungeonInstanceSavedData.Instance> runtimeInstance(Entity player, Entity portal) {
		if (!(player instanceof ServerPlayer serverPlayer))
			return Optional.empty();
		String value = player.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG);
		if (value.isBlank() && portal != null)
			value = portal.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG);
		if (value.isBlank())
			return Optional.empty();
		try {
			return DungeonInstanceSavedData.get(serverPlayer.serverLevel()).getInstance(UUID.fromString(value));
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
		}
	}
}
