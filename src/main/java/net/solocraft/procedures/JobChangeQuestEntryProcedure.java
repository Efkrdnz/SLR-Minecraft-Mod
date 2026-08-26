package net.solocraft.procedures;

import net.solocraft.SololevelingMod;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.JobChangeQuestManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class JobChangeQuestEntryProcedure {
	private static final double PLAYER_PORTAL_ENTRY_X_OFFSET = 3.0D;
	/** How long to keep waiting for the arena before giving up on gravity. */
	private static final int GRAVITY_RESTORE_ATTEMPTS = 40;
	private static final int GRAVITY_RESTORE_INTERVAL_TICKS = 10;
	private static final int GROUND_SEARCH_DEPTH = 40;

	private static final ResourceKey<Level> IGRIS_DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sololeveling:dungeon_dimension_igris"));

	public static boolean execute(LevelAccessor world, Entity entity) {
		if (!(entity instanceof ServerPlayer player) || world == null)
			return false;
		if (!JobChangeQuestManager.isVisible(player)) {
			player.displayClientMessage(Component.literal("\u00A75No active Job Change Quest."), true);
			return false;
		}
		if (JobChangeQuestManager.isSelectionPending(player)) {
			JobChangeQuestManager.requestSelectionScreen(player);
			return false;
		}
		if (JobChangeQuestManager.isShadowPresentation(player)) {
			player.displayClientMessage(Component.literal("\u00A75Your Job assignment is still in progress."), true);
			return false;
		}
		if (player.level().dimension() == IGRIS_DIMENSION) {
			player.displayClientMessage(Component.literal("\u00A75Job Change Quest is already active."), true);
			return false;
		}
		if (!JobChangeQuestManager.isOverworld(player)) {
			player.displayClientMessage(Component.literal(
					"\u00A7cThe Job Change Quest can only be entered from the Minecraft Overworld."), true);
			return false;
		}
		boolean resume = JobChangeQuestManager.canResumeDungeon(player);
		if (!resume) {
			int retryTicks = JobChangeQuestManager.retryDelayTicks(player);
			if (retryTicks > 0) {
				player.displayClientMessage(Component.literal(
						"\u00A7cYou can restart the Job Change Quest in "
								+ ((retryTicks + 19) / 20)
								+ " seconds."), true);
				return false;
			}
		}
		ResourceKey<Level> destinationType = IGRIS_DIMENSION;
		ServerLevel nextLevel = player.server.getLevel(destinationType);
		if (nextLevel == null) {
			player.displayClientMessage(Component.literal("\u00A7cThe Job Change dungeon is unavailable."), true);
			return false;
		}
		if (!resume) {
			if (!JobChangeQuestManager.startDungeonRun(player))
				return false;
			saveEntryState(player);
		}
		player.getPersistentData().putBoolean("slr_job_change_dungeon", true);
		player.setNoGravity(true);
		player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
		player.teleportTo(nextLevel, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
		for (MobEffectInstance effect : player.getActiveEffects())
			player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effect, false));
		player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
		SololevelingMod.queueServerWork(70, () -> {
			if (!player.isAlive() || player.level().dimension() != IGRIS_DIMENSION) {
				player.setNoGravity(false);
				return;
			}
			SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
			double entryX = vars.randplayerx + PLAYER_PORTAL_ENTRY_X_OFFSET;
			// The arena sits at a random coordinate up to thirty million blocks
			// out, in a dimension where nothing is loaded. Generate it before the
			// player arrives: teleporting first only requests the chunks, and a
			// server that has not finished generating them by the time gravity
			// comes back drops the player through empty space into the void.
			ensureArenaChunksLoaded(player.serverLevel(), entryX, vars.randplayerz);
			player.connection.teleport(entryX, vars.randplayery, vars.randplayerz, player.getYRot(), player.getXRot());
			protectDuringDungeonLoad(player);
			if (!resume && player.isAlive() && player.level().dimension() == IGRIS_DIMENSION)
				spawnIgrisDungeon(player);
			SololevelingMod.queueServerWork(resume ? 10 : 35, () -> {
				if (player.isAlive() && player.level().dimension() == IGRIS_DIMENSION) {
					protectDuringDungeonLoad(player);
					// Only fall once there is something to fall onto.
					restoreGravityWhenGrounded(player, GRAVITY_RESTORE_ATTEMPTS);
				} else {
					player.setNoGravity(false);
				}
				SololevelingMod.queueServerWork(10, () -> {
					if (player.isAlive())
						player.fallDistance = 0;
				});
			});
			SololevelingMod.queueServerWork(55, () -> {
				if (!resume && player.isAlive() && player.level().dimension() == IGRIS_DIMENSION)
					spawnIgrisDungeon(player);
			});
		});
		return true;
	}

	/**
	 * Generates the chunks the arena will occupy, synchronously.
	 *
	 * <p>{@code getChunk} with a full status blocks until the chunk exists rather
	 * than merely queueing it, which is the point: the player is about to be put
	 * there and everything after this assumes there is a world under them.
	 *
	 * <p>Three by three because the arena is wider than one chunk, and a player
	 * standing on a generated chunk beside an ungenerated one still falls.
	 */
	private static void ensureArenaChunksLoaded(ServerLevel level, double x, double z) {
		int centerX = SectionPos.blockToSectionCoord(Mth.floor(x));
		int centerZ = SectionPos.blockToSectionCoord(Mth.floor(z));
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++)
				level.getChunk(centerX + dx, centerZ + dz, ChunkStatus.FULL, true);
	}

	/**
	 * Hands gravity back once the player has ground beneath them.
	 *
	 * <p>Restoring it on a fixed timer assumed the arena had finished generating,
	 * which on a slower server it had not: the player resumed falling through
	 * empty space and into the void. The friend beside them was fine because
	 * their own coordinates had generated in time, and teleporting to that friend
	 * worked because those chunks were already loaded.
	 *
	 * <p>The attempt budget is a floor, not a guarantee -- if the ground never
	 * appears the player is left weightless rather than dropped, because floating
	 * is recoverable and the void is not.
	 */
	private static void restoreGravityWhenGrounded(ServerPlayer player, int attemptsLeft) {
		if (!player.isAlive() || player.level().dimension() != IGRIS_DIMENSION) {
			player.setNoGravity(false);
			return;
		}
		if (hasGroundBeneath(player)) {
			player.setNoGravity(false);
			player.fallDistance = 0;
			return;
		}
		if (attemptsLeft <= 0) {
			// Still nothing after the full budget. Keep them up rather than
			// dropping them, and say so, so a stuck player reports something
			// actionable instead of dying to a black screen.
			player.displayClientMessage(Component.literal(
					"§cThe Job Change arena is still generating. Hold on."), true);
			return;
		}
		protectDuringDungeonLoad(player);
		SololevelingMod.queueServerWork(GRAVITY_RESTORE_INTERVAL_TICKS,
				() -> restoreGravityWhenGrounded(player, attemptsLeft - 1));
	}

	/** True when there is a solid block within the drop the player could survive. */
	private static boolean hasGroundBeneath(ServerPlayer player) {
		BlockPos at = player.blockPosition();
		int lowest = Math.max(player.level().getMinBuildHeight(),
				at.getY() - GROUND_SEARCH_DEPTH);
		for (int y = at.getY(); y >= lowest; y--) {
			BlockPos probe = new BlockPos(at.getX(), y, at.getZ());
			if (!player.level().getBlockState(probe).getCollisionShape(
					player.level(), probe).isEmpty())
				return true;
		}
		return false;
	}

	private static void protectDuringDungeonLoad(ServerPlayer player) {
		player.fallDistance = 0;
		player.setDeltaMovement(0.0D, 0.0D, 0.0D);
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 1, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4, false, false));
	}

	private static void saveEntryState(ServerPlayer player) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.DunX = player.getX();
			capability.DunY = player.getY();
			capability.DunZ = player.getZ();
			capability.randplayerx = Mth.nextInt(RandomSource.create(), -29999999, 29999999);
			capability.randplayery = Mth.nextInt(RandomSource.create(), 60, 120);
			capability.randplayerz = Mth.nextInt(RandomSource.create(), -29999999, 29999999);
			capability.instancecomplete = false;
			capability.BossKilled = false;
			capability.tpd = false;
			capability.syncPlayerVariables(player);
		});
	}

	private static void spawnIgrisDungeon(ServerPlayer player) {
		DunPlaceIgrisProcedure.executeForAttempt(player);
	}
}
