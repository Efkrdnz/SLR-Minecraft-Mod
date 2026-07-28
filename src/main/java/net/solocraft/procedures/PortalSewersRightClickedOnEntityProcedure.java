package net.solocraft.procedures;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.ProceduralDungeonRank;
import net.solocraft.entity.Portal12Entity;
import net.solocraft.entity.PortalSewersEntity;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.MagicReadingHelper;
import net.solocraft.util.PlayerEntryGenerationGuard;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class PortalSewersRightClickedOnEntityProcedure {
	private static final String ENTRY_GATE_KEY = "slr_pending_gate_entry";
	private static final String ENTRY_UNTIL_KEY = "slr_pending_gate_entry_until";
	private static final int ENTRY_TIMEOUT_TICKS = 60;
	private static final ResourceKey<Level> DUNGEON_DIMENSION = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling", "dungeon_dimension_d"));

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null || world.isClientSide())
			return;

		if (MagicReadingHelper.isHoldingMagicReader(sourceentity)) {
			MagicReadingHelper.showRankReading(sourceentity, ProceduralDungeonRank.D);
			return;
		}

		if (!(entity instanceof PortalSewersEntity gate) || !(sourceentity instanceof ServerPlayer player))
			return;
		if (net.solocraft.guild.GuildGateHelper.prepareGateEntry(world, gate, player))
			return;
		if (!beginEntry(player, gate))
			return;

		String gateId = gate.getStringUUID();
		long entryGeneration = PlayerEntryGenerationGuard.begin(player);
		boolean generateDungeon = !gate.getEntityData().get(PortalSewersEntity.DATA_usedbefore);
		if (generateDungeon)
			gate.getEntityData().set(PortalSewersEntity.DATA_usedbefore, true);

		removeOwnedShadows(world, x, y, z, player);
		storeReturnPosition(player);

		double targetX = gate.getPersistentData().getDouble("tpx");
		double targetY = gate.getPersistentData().getDouble("tpy");
		double targetZ = gate.getPersistentData().getDouble("tpz");
		UUID playerId = player.getUUID();
		player.getPersistentData().putDouble("tpx", targetX);
		player.getPersistentData().putDouble("tpy", targetY);
		player.getPersistentData().putDouble("tpz", targetZ);
		player.setNoGravity(true);

		SololevelingMod.queueServerWork(10, () -> {
			ServerPlayer currentPlayer = currentPlayer(player.getServer(), playerId, player);
			if (!isCurrentEntry(currentPlayer, entryGeneration, gateId)) {
				abortEntry(player, gate, gateId, generateDungeon,
						entryGeneration);
				return;
			}

			ServerLevel destination = currentPlayer.getServer().getLevel(DUNGEON_DIMENSION);
			if (destination == null) {
				abortEntry(currentPlayer, gate, gateId, generateDungeon,
						entryGeneration);
				return;
			}

			if (currentPlayer.level().dimension() != DUNGEON_DIMENSION)
				changeDimension(currentPlayer, destination);
			currentPlayer.getPersistentData().putString("dungeon_tag", gateId);
			net.solocraft.util.UrgentQuestManager.markDungeonId(currentPlayer, "goblin_sewers");

			SololevelingMod.queueServerWork(5, () -> {
				ServerPlayer teleportedPlayer = currentPlayer(currentPlayer.getServer(), playerId, currentPlayer);
				if (!isCurrentEntry(teleportedPlayer, entryGeneration, gateId) || teleportedPlayer.level().dimension() != DUNGEON_DIMENSION) {
					abortEntry(currentPlayer, gate, gateId, generateDungeon,
							entryGeneration);
					return;
				}

				teleportedPlayer.connection.teleport(targetX, targetY, targetZ, teleportedPlayer.getYRot(), teleportedPlayer.getXRot());
				teleportedPlayer.getPersistentData().putString("dungeon_tag", gateId);

				SololevelingMod.queueServerWork(10, () -> {
					ServerPlayer readyPlayer = currentPlayer(teleportedPlayer.getServer(), playerId, teleportedPlayer);
					if (!isCurrentEntry(readyPlayer, entryGeneration, gateId) || readyPlayer.level().dimension() != DUNGEON_DIMENSION) {
						abortEntry(teleportedPlayer, gate, gateId,
								generateDungeon, entryGeneration);
						return;
					}

					try {
						if (generateDungeon && !hasReturnPortal(destination, targetX, targetY, targetZ))
							generateDungeon(readyPlayer);
						readyPlayer.getPersistentData().putString("dungeon_tag", gateId);
					} finally {
						readyPlayer.setNoGravity(false);
						finishEntry(readyPlayer, gateId);
					}
				});
			});
		});
	}

	private static boolean isCurrentEntry(ServerPlayer player, long entryGeneration, String gateId) {
		return PlayerEntryGenerationGuard.isCurrent(player, entryGeneration)
				&& gateId.equals(player.getPersistentData().getString(ENTRY_GATE_KEY));
	}

	private static boolean beginEntry(ServerPlayer player, PortalSewersEntity gate) {
		long now = player.getServer().overworld().getGameTime();
		if (player.getPersistentData().getLong(ENTRY_UNTIL_KEY) > now)
			return false;
		player.getPersistentData().putString(ENTRY_GATE_KEY, gate.getStringUUID());
		player.getPersistentData().putLong(ENTRY_UNTIL_KEY, now + ENTRY_TIMEOUT_TICKS);
		return true;
	}

	private static void finishEntry(ServerPlayer player, String gateId) {
		if (gateId.equals(player.getPersistentData().getString(ENTRY_GATE_KEY))) {
			player.getPersistentData().remove(ENTRY_GATE_KEY);
			player.getPersistentData().remove(ENTRY_UNTIL_KEY);
		}
	}

	private static void abortEntry(ServerPlayer player,
			PortalSewersEntity gate, String gateId,
			boolean generationWasClaimed, long entryGeneration) {
		ServerPlayer livePlayer = player.getServer() == null ? null
				: player.getServer().getPlayerList().getPlayer(
						player.getUUID());
		ServerPlayer stateOwner = livePlayer == null ? player : livePlayer;
		boolean newerEntry = PlayerEntryGenerationGuard.capture(stateOwner)
				!= entryGeneration;
		boolean newerEntryOwnsGate = newerEntry && gateId.equals(
				stateOwner.getPersistentData().getString(ENTRY_GATE_KEY));
		if (!newerEntry) {
			stateOwner.setNoGravity(false);
			finishEntry(stateOwner, gateId);
		}
		if (generationWasClaimed && !newerEntryOwnsGate
				&& !gate.isRemoved())
			gate.getEntityData().set(PortalSewersEntity.DATA_usedbefore, false);
	}

	private static ServerPlayer currentPlayer(MinecraftServer server, UUID playerId, ServerPlayer expected) {
		if (server == null)
			return null;
		ServerPlayer current = server.getPlayerList().getPlayer(playerId);
		return current == expected && current.isAlive() ? current : null;
	}

	private static void removeOwnedShadows(LevelAccessor world, double x, double y, double z, ServerPlayer player) {
		TagKey<net.minecraft.world.entity.EntityType<?>> shadowTag = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows"));
		for (Entity nearby : world.getEntitiesOfClass(Entity.class, AABB.ofSize(new Vec3(x, y, z), 500, 500, 500), candidate -> true)) {
			if (nearby.getType().is(shadowTag) && nearby instanceof TamableAnimal shadow && shadow.isOwnedBy(player))
				nearby.discard();
		}
	}

	private static void storeReturnPosition(ServerPlayer player) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.DunX = player.getX();
			capability.DunY = player.getY();
			capability.DunZ = player.getZ();
			capability.syncPlayerVariables(player);
		});
	}

	private static void changeDimension(ServerPlayer player, ServerLevel destination) {
		player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
		player.teleportTo(destination, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
		for (MobEffectInstance effect : player.getActiveEffects())
			player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effect));
		player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
	}

	private static boolean hasReturnPortal(ServerLevel level, double x, double y, double z) {
		return !level.getEntitiesOfClass(Portal12Entity.class, AABB.ofSize(new Vec3(x, y, z), 256, 256, 256), portal -> true).isEmpty();
	}

	private static void generateDungeon(ServerPlayer player) {
		player.getServer().getCommands().performPrefixedCommand(
				new CommandSourceStack(CommandSource.NULL, player.position(), player.getRotationVector(), player.serverLevel(), 4, player.getName().getString(), player.getDisplayName(), player.getServer(), player),
				"spawnrandom");
	}
}
