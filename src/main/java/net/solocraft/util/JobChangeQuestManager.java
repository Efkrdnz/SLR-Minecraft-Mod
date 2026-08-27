package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.api.vessel.Vessel;
import net.solocraft.api.vessel.VesselRegistry;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.network.VesselSelectionStateMessage;
import net.solocraft.procedures.DungeonDimensionPlayerLeavesDimensionProcedure;
import net.solocraft.procedures.JobChangeCleanupProcedure;
import net.solocraft.util.VesselManager.AssignmentResult;
import net.solocraft.util.VesselManager.VesselDefinition;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.solocraft.network.compat.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber
public final class JobChangeQuestManager {
	public static final String QUEST_ID = "job_change";
	public static final int STATE_IDLE = 0;
	public static final int STATE_DUNGEON_ACTIVE = -4;
	public static final int STATE_SHADOW_PRESENTATION = -3;
	public static final int STATE_ADVANCEMENT = -2;
	public static final int STATE_SELECTION = -1;

	private static final String TOKEN = QUEST_ID + ",";
	private static final String SELECTION_AUTHORIZED = "slr_job_change_selection_authorized";
	private static final String COMMAND_SELECTION_AUTHORIZED = "slr_command_vessel_selection_authorized";
	private static final String SELECTION_OPEN_AFTER_TAG =
			"slr_job_change_selection_open_after";
	public static final String ATTEMPT_ID_TAG =
			"slr_job_change_attempt_id";
	public static final String ATTEMPT_OWNER_TAG =
			"slr_job_change_attempt_owner";
	private static final String ATTEMPT_BOSS_PROCESSED_TAG =
			"slr_job_change_attempt_boss_processed";
	private static final String ADVANCEMENT_POINT_CREDITED_TAG =
			"slr_job_change_advancement_point_credited";
	private static final String RETRY_AFTER_TAG =
			"slr_job_change_retry_after";
	private static final String FAILURE_NOTICE_TAG =
			"slr_job_change_failure_notice";
	private static final long RETRY_DELAY_TICKS = 20L * 10L;
	private static final long SELECTION_OPEN_DELAY_TICKS = 30L;
	private static final int ACCENT = 0xFF7A5CFF;
	private static final double PARTY_RANGE_SQR = 256.0D * 256.0D;
	private static final ResourceKey<Level> IGRIS_DIMENSION =
			ResourceKey.create(Registries.DIMENSION,
					ResourceLocation.fromNamespaceAndPath("sololeveling",
							"dungeon_dimension_igris"));

	private JobChangeQuestManager() {
	}

	public static boolean isUnlocked(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		return vars.JOB > 0 || contains(vars.unlocked_quests, QUEST_ID);
	}

	public static boolean isFinished(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		return contains(vars.finished_quests, QUEST_ID) || (vars.JOB > 0 && state(vars) == STATE_IDLE);
	}

	public static boolean isVisible(Entity entity) {
		return isUnlocked(entity) && !isFinished(entity);
	}

	public static boolean isDungeonActive(Entity entity) {
		return entity != null && state(vars(entity)) == STATE_DUNGEON_ACTIVE;
	}

	public static boolean isAdvancementActive(Entity entity) {
		return entity != null && state(vars(entity)) == STATE_ADVANCEMENT;
	}

	public static boolean isSelectionPending(Entity entity) {
		return entity != null && state(vars(entity)) == STATE_SELECTION;
	}

	public static boolean isShadowPresentation(Entity entity) {
		return entity != null && state(vars(entity)) == STATE_SHADOW_PRESENTATION;
	}

	public static boolean canResumeDungeon(Entity entity) {
		if (!(entity instanceof ServerPlayer player)
				|| (!isDungeonActive(player)
						&& !isAdvancementActive(player)))
			return false;
		UUID attemptId = activeAttemptId(player);
		return attemptId != null
				&& JobChangeAttemptSavedData.get(player.server)
						.isActive(attemptId);
	}

	public static int advancementPoints(Entity entity) {
		return entity == null ? 0 : Math.max(0, (int) Math.round(vars(entity).jobadvpoint));
	}

	public static int requiredPoints(Entity entity) {
		if (entity == null)
			return 50;
		return Math.max(1, entity.level().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_JOB_CHANGE_POINTS));
	}

	public static boolean isOverworld(Entity entity) {
		return entity != null && Level.OVERWORLD.equals(entity.level().dimension());
	}

	public static boolean unlock(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || !isOverworld(player) || isUnlocked(player) || isFinished(player))
			return false;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (!contains(capability.unlocked_quests, QUEST_ID))
				capability.unlocked_quests = append(capability.unlocked_quests);
			capability.jobkey = true;
			capability.syncPlayerVariables(player);
		});
		SystemNotifications.showTitleUnder(player, ACCENT, 100,
				Component.literal("QUEST UNLOCKED").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				Component.literal("Job Change Quest").withStyle(ChatFormatting.LIGHT_PURPLE));
		return true;
	}

	/**
	 * Operator-only recovery path that makes Job Change available again, even
	 * after an earlier completion. It deliberately resets only the Job Change
	 * quest receipt/status; vessel ownership and unrelated player progression
	 * are left intact.
	 */
	public static boolean forceTriggerQuest(ServerPlayer player) {
		if (player == null || !isOverworld(player))
			return false;
		UUID attemptId = activeAttemptId(player);
		if (attemptId != null) {
			JobChangeCleanupProcedure.executeAttempt(player.server, attemptId);
			JobChangeAttemptSavedData.get(player.server)
					.removeParticipant(attemptId, player.getUUID());
		}
		clearActiveAttempt(player);
		clearRetryState(player);
		player.getPersistentData().remove(SELECTION_AUTHORIZED);
		player.getPersistentData().remove(COMMAND_SELECTION_AUTHORIZED);
		persistentPlayerData(player).remove(SELECTION_OPEN_AFTER_TAG);
		player.getPersistentData().remove("slr_job_change_dungeon");
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.unlocked_quests = append(removeToken(capability.unlocked_quests, QUEST_ID));
			capability.finished_quests = removeToken(capability.finished_quests, QUEST_ID);
			capability.jobtimer = STATE_IDLE;
			capability.jobadvpoint = 0;
			capability.JobChange_timer = 0;
			capability.instancecomplete = false;
			capability.giftstatus = false;
			capability.jobkey = true;
			capability.JOB = 0;
			capability.BossKilled = false;
			capability.tpd = false;
			capability.syncPlayerVariables(player);
		});
		player.setNoGravity(false);
		player.fallDistance = 0.0F;
		closeSelection(player);
		SystemNotifications.showTitleUnder(player, ACCENT, 100,
				Component.literal("QUEST RESET").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				Component.literal("Job Change Quest is available again.").withStyle(ChatFormatting.LIGHT_PURPLE));
		return true;
	}

	public static boolean startDungeonRun(ServerPlayer player) {
		if (player == null || !isOverworld(player)
				|| retryDelayTicks(player) > 0)
			return false;
		UUID previousAttempt = activeAttemptId(player);
		if (previousAttempt != null)
			JobChangeAttemptSavedData.get(player.server).removeParticipant(
					previousAttempt, player.getUUID());
		UUID attemptId = UUID.randomUUID();
		setActiveAttemptId(player, attemptId);
		clearRetryState(player);
		JobChangeAttemptSavedData.get(player.server).start(attemptId,
				player.getUUID(), currentGameTime(player.server));
		player.getPersistentData().remove(SELECTION_AUTHORIZED);
		player.getPersistentData().remove(COMMAND_SELECTION_AUTHORIZED);
		persistentPlayerData(player).remove(SELECTION_OPEN_AFTER_TAG);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.jobtimer = STATE_DUNGEON_ACTIVE;
			capability.jobadvpoint = 0;
			capability.JobChange_timer = 0;
			capability.instancecomplete = false;
			capability.syncPlayerVariables(player);
		});
		return true;
	}

	/** Starts the post-Igris trial for the killer and nearby members of their party. */
	public static List<ServerPlayer> beginAdvancementPhase(ServerPlayer killer,
			Entity defeatedBoss) {
		UUID attemptId = attemptId(defeatedBoss);
		if (attemptId == null
				|| !matchesActiveAttempt(killer, defeatedBoss)
				|| !markBossProcessed(defeatedBoss))
			return List.of();
		List<ServerPlayer> participants = questParticipants(killer, false);
		participants.removeIf(player -> !isDungeonActive(player)
				|| player.level() != defeatedBoss.level());
		// Reported as "killed Igris and nothing happened". Every exit above this
		// point is silent, so a player who gets filtered out here is left standing
		// in a cleared room with no quest, no message and nothing to act on. This
		// cannot repair the state -- the cause is still unknown -- but it turns an
		// unreportable dead end into something a player can tell us about.
		if (participants.isEmpty()) {
			SystemNotifications.showNegativeTitleUnder(killer, 0xFFFF3D3D, 120,
					Component.literal("TRIAL NOT STARTED")
							.withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
					Component.literal("The System did not register this kill. Use /slr stuck to recover.")
							.withStyle(ChatFormatting.GRAY));
			SololevelingMod.LOGGER.warn(
					"Job change trial did not start for {}: attempt={} dungeonActive={} bossLevel={} playerLevel={}",
					killer.getGameProfile().getName(), attemptId,
					isDungeonActive(killer), defeatedBoss.level().dimension().location(),
					killer.level().dimension().location());
		}
		for (ServerPlayer player : participants) {
			if (isAdvancementActive(player) || isSelectionPending(player) || isShadowPresentation(player))
				continue;
			UUID previousAttempt = activeAttemptId(player);
			if (previousAttempt != null
					&& !previousAttempt.equals(attemptId)) {
				JobChangeCleanupProcedure.executeAttempt(player.server,
						previousAttempt);
				JobChangeAttemptSavedData.get(player.server)
						.removeParticipant(previousAttempt,
								player.getUUID());
			}
			setActiveAttemptId(player, attemptId);
			JobChangeAttemptSavedData.get(player.server).addParticipant(
					attemptId, player.getUUID(),
					currentGameTime(player.server));
			player.getPersistentData().remove(SELECTION_AUTHORIZED);
			persistentPlayerData(player).remove(SELECTION_OPEN_AFTER_TAG);
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.giftstatus = true;
				capability.jobtimer = STATE_ADVANCEMENT;
				capability.jobadvpoint = 0;
				capability.JobChange_timer = 0;
				capability.instancecomplete = false;
				capability.syncPlayerVariables(player);
			});
			SystemNotifications.showTitleUnder(player, 0xFFDF9607, 100,
					Component.literal("BOSS SLAIN").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					Component.literal("Defeat the summoned knights [0/" + requiredPoints(player) + "]")
							.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		return participants;
	}

	/** Grants one shared party point for a qualifying knight kill. */
	public static void grantAdvancementPoint(ServerPlayer killer, Entity defeated) {
		if (killer == null || defeated == null
				|| !isAdvancementActive(killer)
				|| !defeated.getPersistentData().getBoolean(
						JobChangeKnightBalance.QUEST_KNIGHT_TAG)
				|| !matchesActiveAttempt(killer, defeated)
				|| defeated.getPersistentData().getBoolean(
						ADVANCEMENT_POINT_CREDITED_TAG))
			return;
		UUID attemptId = attemptId(defeated);
		List<ServerPlayer> recipients = questParticipants(killer, true);
		recipients.removeIf(player -> !hasAttemptId(player, attemptId));
		if (recipients.isEmpty())
			return;
		// LivingDeathEvent is the canonical path, but generated entity callbacks
		// may also invoke the compatibility entry point. Keep both the entity marker
		// and a durable per-attempt UUID ledger so one death can never increment
		// progress twice, regardless of callback order or save reloads.
		if (!JobChangeAttemptSavedData.get(defeated.level().getServer())
				.creditAdvancementKill(attemptId, defeated.getUUID(),
						currentGameTime(defeated.level().getServer())))
			return;
		defeated.getPersistentData().putBoolean(
				ADVANCEMENT_POINT_CREDITED_TAG, true);

		for (ServerPlayer player : recipients) {
			int required = requiredPoints(player);
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.jobadvpoint = Math.min(required, Math.max(0, capability.jobadvpoint) + 1);
				capability.syncPlayerVariables(player);
			});
			int progress = advancementPoints(player);
			player.displayClientMessage(Component.literal("Advancement Points [" + progress + "/" + required + "]")
					.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
		}

		// Shared progress may contain a recovering or previously de-synchronised
		// party member. Never invalidate the attempt while anyone still displays
		// 49/50; the next valid kill can safely bring every recipient to 50.
		boolean allComplete = recipients.stream().allMatch(player ->
				advancementPoints(player) >= requiredPoints(player));
		if (!allComplete)
			return;

		// Transition players first, while the attempt is still authoritative.
		// Cleanup/removal afterward prevents the one-second recovery audit from
		// observing an inactive attempt paired with STATE_ADVANCEMENT and forcing
		// an emergency return-portal exit.
		for (ServerPlayer player : recipients)
			enterSelection(player);
		JobChangeCleanupProcedure.completeAttempt(
				defeated.level().getServer(), attemptId);
		JobChangeAttemptSavedData.get(defeated.level().getServer())
				.complete(attemptId);
	}

	public static void selectVessel(ServerPlayer player, String type, String identity) {
		if (player == null)
			return;
		if (!isSelectionPending(player) || !isSelectionAuthorized(player)) {
			if (isFinished(player) || vars(player).JOB > 0)
				closeSelection(player);
			else
				selectionError(player, "The Job Change trial is not complete.");
			return;
		}

		VesselDefinition definition = VesselManager.definition(type, identity);
		if (definition == null) {
			// Not a built-in. Contributed vessels answer to the same gates above, and
			// this branch only runs where selection previously failed outright, so
			// nothing about built-in selection changes.
			if (!selectContributedVessel(player, identity))
				selectionError(player, "That vessel does not exist.");
			return;
		}
		if (!VesselManager.isSelectableFor(player, definition)) {
			selectionError(player, "WIP (Work in progress)");
			return;
		}
		if (!selectionOpenReady(player)) {
			closeSelection(player);
			return;
		}

		AssignmentResult result = VesselManager.isAntares(definition)
				? VesselManager.assignAntaresVessel(player, true)
				: VesselManager.assignPlayer(player, definition, true);
		if (result == AssignmentResult.LOCKED) {
			selectionError(player, definition.name() + " has already reached the server limit.");
			return;
		}
		if (result != AssignmentResult.SUCCESS) {
			selectionError(player, "The System could not assign that vessel.");
			return;
		}

		player.getPersistentData().remove(COMMAND_SELECTION_AUTHORIZED);
		closeSelection(player);
		if ("ashborn".equals(definition.identity())) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.jobtimer = STATE_SHADOW_PRESENTATION;
				capability.JobChange_timer = 1;
				capability.instancecomplete = false;
				capability.syncPlayerVariables(player);
			});
			return;
		}

		finish(player);
		int color = VesselManager.RULER.equals(definition.type()) ? 0xFF3FC6FF : 0xFFB965FF;
		SystemNotifications.showTitleUnder(player, color, 120,
				Component.literal("VESSEL SELECTED").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
				Component.literal(definition.commandDisplay()).withStyle(
						VesselManager.RULER.equals(definition.type()) ? ChatFormatting.AQUA : ChatFormatting.LIGHT_PURPLE,
						ChatFormatting.BOLD));
	}

	public static void requestSelectionScreen(ServerPlayer player) {
		if (player == null)
			return;
		if (isSelectionPending(player) && isSelectionAuthorized(player)
				&& selectionOpenReady(player))
			sendSelectionState(player);
	}

	/** Opens the same server-authorized choice used after the advancement trial. */
	public static void openSelectionFromCommand(ServerPlayer player) {
		if (player == null)
			return;
		player.getPersistentData().putBoolean(SELECTION_AUTHORIZED, true);
		player.getPersistentData().putBoolean(COMMAND_SELECTION_AUTHORIZED, true);
		persistentPlayerData(player).remove(SELECTION_OPEN_AFTER_TAG);
		int required = requiredPoints(player);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.jobtimer = STATE_SELECTION;
			capability.jobadvpoint = required;
			capability.JobChange_timer = 0;
			capability.instancecomplete = false;
			capability.syncPlayerVariables(player);
		});
		sendSelectionState(player);
	}

	public static void finish(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof ServerPlayer player) {
			UUID attemptId = activeAttemptId(player);
			if (attemptId != null) {
				JobChangeCleanupProcedure.completeAttempt(player.server,
						attemptId);
				JobChangeAttemptSavedData.get(player.server)
						.complete(attemptId);
			}
			clearActiveAttempt(player);
			clearRetryState(player);
		}
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (!contains(capability.unlocked_quests, QUEST_ID))
				capability.unlocked_quests = append(capability.unlocked_quests);
			if (!contains(capability.finished_quests, QUEST_ID))
				capability.finished_quests = append(capability.finished_quests);
			capability.jobtimer = STATE_IDLE;
			capability.JobChange_timer = 0;
			capability.instancecomplete = true;
			capability.jobkey = false;
			capability.syncPlayerVariables(entity);
		});
		entity.setNoGravity(false);
		entity.getPersistentData().putBoolean("slr_job_change_dungeon", false);
		entity.getPersistentData().remove(SELECTION_AUTHORIZED);
		entity.getPersistentData().remove(COMMAND_SELECTION_AUTHORIZED);
		if (entity instanceof Player)
			persistentPlayerData(entity).remove(SELECTION_OPEN_AFTER_TAG);
	}

	/**
	 * Clears every durable and transient Job Change receipt for a character
	 * reset. In particular, both quest-token lists must be cleared together:
	 * leaving either token behind permanently blocks {@link #unlockIfEligible}.
	 */
	public static void resetForPlayerReset(ServerPlayer player) {
		if (player == null)
			return;
		UUID attemptId = activeAttemptId(player);
		if (attemptId != null) {
			JobChangeCleanupProcedure.executeAttempt(player.server,
					attemptId);
			JobChangeAttemptSavedData.get(player.server)
					.removeParticipant(attemptId, player.getUUID());
		}
		clearActiveAttempt(player);
		clearRetryState(player);
		player.getPersistentData().remove(SELECTION_AUTHORIZED);
		player.getPersistentData().remove(COMMAND_SELECTION_AUTHORIZED);
		persistentPlayerData(player).remove(SELECTION_OPEN_AFTER_TAG);
		player.getPersistentData().remove("slr_job_change_dungeon");
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.unlocked_quests = removeToken(capability.unlocked_quests, QUEST_ID);
			capability.finished_quests = removeToken(capability.finished_quests, QUEST_ID);
			capability.jobtimer = STATE_IDLE;
			capability.jobadvpoint = 0;
			capability.JobChange_timer = 0;
			capability.instancecomplete = false;
			capability.giftstatus = false;
			capability.jobkey = false;
			capability.JOB = 0;
			capability.syncPlayerVariables(player);
		});
		player.setNoGravity(false);
		closeSelection(player);
	}

	@Nullable
	public static UUID attemptId(Entity entity) {
		if (entity == null)
			return null;
		CompoundTag data;
		if (entity instanceof Player) {
			CompoundTag root = entity.getPersistentData();
			if (!root.contains(Player.PERSISTED_NBT_TAG,
					Tag.TAG_COMPOUND))
				return null;
			data = root.getCompound(Player.PERSISTED_NBT_TAG);
		} else {
			data = entity.getPersistentData();
		}
		return data.hasUUID(ATTEMPT_ID_TAG)
				? data.getUUID(ATTEMPT_ID_TAG) : null;
	}

	@Nullable
	public static UUID activeAttemptId(Entity entity) {
		return attemptId(entity);
	}

	public static boolean hasAttemptId(Entity entity,
			@Nullable UUID attemptId) {
		UUID actual = attemptId(entity);
		return attemptId != null && attemptId.equals(actual);
	}

	public static boolean isAttemptEntity(Entity entity) {
		return entity != null && !(entity instanceof Player)
				&& entity.getPersistentData().hasUUID(ATTEMPT_ID_TAG);
	}

	@Nullable
	public static UUID attemptOwner(Entity entity) {
		if (entity == null || entity instanceof Player)
			return null;
		CompoundTag data = entity.getPersistentData();
		return data.hasUUID(ATTEMPT_OWNER_TAG)
				? data.getUUID(ATTEMPT_OWNER_TAG) : null;
	}

	public static void tagAttemptEntity(Entity entity, UUID attemptId,
			UUID ownerId) {
		if (entity == null || entity instanceof Player || attemptId == null)
			return;
		entity.getPersistentData().putUUID(ATTEMPT_ID_TAG, attemptId);
		if (ownerId != null)
			entity.getPersistentData().putUUID(ATTEMPT_OWNER_TAG,
					ownerId);
	}

	public static void copyAttempt(Entity source, Entity target) {
		if (source == null || target == null)
			return;
		UUID attemptId = attemptId(source);
		if (attemptId == null)
			return;
		UUID ownerId = source.getPersistentData().hasUUID(
				ATTEMPT_OWNER_TAG)
						? source.getPersistentData().getUUID(
								ATTEMPT_OWNER_TAG)
						: null;
		tagAttemptEntity(target, attemptId, ownerId);
	}

	public static boolean matchesActiveAttempt(ServerPlayer player,
			Entity encounterEntity) {
		if (player == null || encounterEntity == null
				|| player.level() != encounterEntity.level())
			return false;
		UUID playerAttempt = activeAttemptId(player);
		if (playerAttempt == null
				|| !hasAttemptId(encounterEntity, playerAttempt))
			return false;
		return JobChangeAttemptSavedData.get(player.server)
				.isActive(playerAttempt);
	}

	public static boolean isAttemptActive(MinecraftServer server,
			UUID attemptId) {
		if (server == null || attemptId == null
				|| !JobChangeAttemptSavedData.get(server)
						.isActive(attemptId))
			return false;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (hasAttemptId(player, attemptId)
					&& (isDungeonActive(player)
							|| isAdvancementActive(player)))
				return true;
		}
		return false;
	}

	public static int retryDelayTicks(ServerPlayer player) {
		if (player == null || player.server == null)
			return 0;
		long remaining = persistentPlayerData(player).getLong(
				RETRY_AFTER_TAG) - currentGameTime(player.server);
		return (int) Math.min(Integer.MAX_VALUE,
				Math.max(0L, remaining));
	}

	public static boolean failActiveAttempt(ServerPlayer failedPlayer) {
		if (failedPlayer == null
				|| (!isDungeonActive(failedPlayer)
						&& !isAdvancementActive(failedPlayer)))
			return false;
		UUID attemptId = activeAttemptId(failedPlayer);
		long now = currentGameTime(failedPlayer.server);
		long retryAfter = now + RETRY_DELAY_TICKS;
		Set<UUID> participantIds = new LinkedHashSet<>();
		if (attemptId != null) {
			participantIds.addAll(JobChangeAttemptSavedData
					.get(failedPlayer.server).invalidate(attemptId,
							retryAfter, now));
			JobChangeCleanupProcedure.executeAttempt(failedPlayer.server,
					attemptId);
		} else {
			// Compatibility for a player who was mid-attempt when updating from
			// a build that did not yet persist attempt IDs.
			participantIds.add(failedPlayer.getUUID());
			if (failedPlayer.level() instanceof ServerLevel)
				JobChangeCleanupProcedure.execute(failedPlayer.level(),
						failedPlayer.getX(), failedPlayer.getY(),
						failedPlayer.getZ());
		}

		List<ServerPlayer> affected = new ArrayList<>();
		for (ServerPlayer player :
				failedPlayer.server.getPlayerList().getPlayers()) {
			if (participantIds.contains(player.getUUID())
					|| (attemptId != null
							&& hasAttemptId(player, attemptId)))
				affected.add(player);
		}
		if (affected.stream().noneMatch(player ->
				player.getUUID().equals(failedPlayer.getUUID())))
			affected.add(failedPlayer);

		for (ServerPlayer player : affected) {
			resetFailedAttemptState(player, attemptId, retryAfter);
			if (player != failedPlayer && player.isAlive()) {
				showFailureNotice(player);
				persistentPlayerData(player).remove(FAILURE_NOTICE_TAG);
				if (player.level().dimension().equals(IGRIS_DIMENSION))
					DungeonDimensionPlayerLeavesDimensionProcedure
							.emergencyExit(player);
			}
		}
		return true;
	}

	public static void unlockIfEligible(LevelAccessor world, Entity entity, int requiredLevel) {
		if (!(entity instanceof ServerPlayer player) || world == null)
			return;
		SololevelingModVariables.PlayerVariables vars = vars(player);
		if (vars.JOB > 0 && state(vars) == STATE_IDLE) {
			// Admin vessel assignment and legacy reconciliation are not normal
			// quest initiation and remain valid in every dimension.
			finish(player);
			return;
		}
		if (!isOverworld(player))
			return;
		if (vars.Player && vars.Level >= requiredLevel && vars.JOB == 0 && !contains(vars.finished_quests, QUEST_ID) && !contains(vars.unlocked_quests, QUEST_ID))
			unlock(player);
	}

	public static boolean hasAdvancementPlayerNear(Entity portal, double range) {
		if (portal == null || portal.level().isClientSide())
			return false;
		UUID attemptId = attemptId(portal);
		if (attemptId == null)
			return false;
		double rangeSqr = range * range;
		for (ServerPlayer player : portal.level().getServer().getPlayerList().getPlayers()) {
			if (player.level() == portal.level()
					&& isAdvancementActive(player)
					&& hasAttemptId(player, attemptId)
					&& player.distanceToSqr(portal) <= rangeSqr)
				return true;
		}
		return false;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player
				&& player.level().dimension().equals(IGRIS_DIMENSION))
			failActiveAttempt(player);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onAttemptMobDrops(LivingDropsEvent event) {
		if (isAttemptEntity(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onAttemptMobExperience(
			LivingExperienceDropEvent event) {
		if (isAttemptEntity(event.getEntity()))
			event.setDroppedExperience(0);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (!(event.getEntity() instanceof ServerPlayer clone))
			return;
		CompoundTag originalRoot = event.getOriginal().getPersistentData();
		if (!originalRoot.contains(Player.PERSISTED_NBT_TAG,
				Tag.TAG_COMPOUND))
			return;
		CompoundTag original = originalRoot.getCompound(
				Player.PERSISTED_NBT_TAG);
		CompoundTag copied = persistentPlayerData(clone);
		if (original.hasUUID(ATTEMPT_ID_TAG))
			copied.putUUID(ATTEMPT_ID_TAG,
					original.getUUID(ATTEMPT_ID_TAG));
		if (original.contains(RETRY_AFTER_TAG, Tag.TAG_ANY_NUMERIC))
			copied.putLong(RETRY_AFTER_TAG,
					original.getLong(RETRY_AFTER_TAG));
		if (original.getBoolean(FAILURE_NOTICE_TAG))
			copied.putBoolean(FAILURE_NOTICE_TAG, true);
		if (original.contains(SELECTION_OPEN_AFTER_TAG,
				Tag.TAG_ANY_NUMERIC))
			copied.putLong(SELECTION_OPEN_AFTER_TAG,
					original.getLong(SELECTION_OPEN_AFTER_TAG));
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			recover(player, true);
			showPendingFailureNotice(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			recover(player, true);
			showPendingFailureNotice(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (false || !(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0)
			return;
		recover(player, player.tickCount % 40 == 0);
	}

	private static void enterSelection(ServerPlayer player) {
		if (player == null || !isAdvancementActive(player)
				|| advancementPoints(player) < requiredPoints(player))
			return;
		// The caller keeps the shared attempt active until every participant has
		// left STATE_ADVANCEMENT, then completes it exactly once.
		clearActiveAttempt(player);
		clearRetryState(player);
		player.getPersistentData().putBoolean(SELECTION_AUTHORIZED, true);
		persistentPlayerData(player).putLong(SELECTION_OPEN_AFTER_TAG,
				currentGameTime(player.server) + SELECTION_OPEN_DELAY_TICKS);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.jobtimer = STATE_SELECTION;
			capability.JobChange_timer = 0;
			capability.instancecomplete = false;
			capability.syncPlayerVariables(player);
		});
		SystemNotifications.showTitleUnder(player, ACCENT, 90,
				Component.literal("ADVANCEMENT COMPLETE").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				Component.literal("Choose the power that will become your vessel.").withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	private static void recover(ServerPlayer player, boolean refreshScreen) {
		SololevelingModVariables.PlayerVariables data = vars(player);
		int currentState = state(data);
		if (currentState == STATE_DUNGEON_ACTIVE
				|| currentState == STATE_ADVANCEMENT) {
			UUID attemptId = activeAttemptId(player);
			JobChangeAttemptSavedData.AttemptView attempt =
					attemptId == null ? null
							: JobChangeAttemptSavedData.get(player.server)
									.view(attemptId);
			if (attempt == null || !attempt.active()) {
				long retryAfter = attempt == null
						? currentGameTime(player.server)
						: attempt.retryAfterGameTime();
				resetFailedAttemptState(player, attemptId, retryAfter);
				persistentPlayerData(player).remove(FAILURE_NOTICE_TAG);
				showFailureNotice(player);
				if (player.level().dimension().equals(IGRIS_DIMENSION))
					DungeonDimensionPlayerLeavesDimensionProcedure
							.emergencyExit(player);
				return;
			}
		}

		// Migrate worlds that were saved during the old 100-second survival timer.
		if (data.JOB == 0 && data.jobtimer > 0) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.jobtimer = STATE_ADVANCEMENT;
				capability.jobadvpoint = Math.max(0, capability.jobadvpoint - 1);
				capability.syncPlayerVariables(player);
			});
			currentState = STATE_ADVANCEMENT;
		}

		// The old flow could already be inside the automatic Shadow poem.
		if (data.JOB == 0 && data.JobChange_timer > 0) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.jobtimer = STATE_SELECTION;
				capability.JobChange_timer = 0;
				capability.instancecomplete = false;
				capability.syncPlayerVariables(player);
			});
			currentState = STATE_SELECTION;
		}

		if (data.JOB == 0 && currentState == STATE_SELECTION && !isSelectionAuthorized(player)) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.jobtimer = STATE_IDLE;
				capability.jobadvpoint = 0;
				capability.JobChange_timer = 0;
				capability.syncPlayerVariables(player);
			});
			player.getPersistentData().remove(SELECTION_AUTHORIZED);
			closeSelection(player);
			return;
		}

		if (currentState == STATE_SELECTION && data.JOB > 0 && !isCommandSelectionAuthorized(player)) {
			VesselDefinition definition = VesselManager.currentDefinition(player);
			if (definition != null && "ashborn".equals(definition.identity())) {
				player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.jobtimer = STATE_SHADOW_PRESENTATION;
					capability.JobChange_timer = Math.max(1, capability.JobChange_timer);
					capability.syncPlayerVariables(player);
				});
			} else {
				finish(player);
			}
			closeSelection(player);
			return;
		}

		if (currentState == STATE_SELECTION && isSelectionAuthorized(player)) {
			CompoundTag persisted = persistentPlayerData(player);
			boolean delayed = persisted.contains(SELECTION_OPEN_AFTER_TAG,
					Tag.TAG_ANY_NUMERIC);
			if (selectionOpenReady(player) && (delayed || refreshScreen))
				sendSelectionState(player);
		} else if (data.JOB > 0 && currentState == STATE_IDLE && !contains(data.finished_quests, QUEST_ID)) {
			finish(player);
		}
	}

	private static List<ServerPlayer> questParticipants(ServerPlayer anchor, boolean requireAdvancement) {
		List<ServerPlayer> result = new ArrayList<>();
		if (anchor == null)
			return result;
		String party = vars(anchor).party;
		for (ServerPlayer candidate : anchor.serverLevel().players()) {
			boolean samePlayer = candidate.getUUID().equals(anchor.getUUID());
			boolean sameParty = party != null && !party.isBlank() && party.equals(vars(candidate).party);
			if ((!samePlayer && !sameParty) || candidate.distanceToSqr(anchor) > PARTY_RANGE_SQR)
				continue;
			if (requireAdvancement && !isAdvancementActive(candidate))
				continue;
			if (!requireAdvancement && (vars(candidate).JOB > 0 || isFinished(candidate)))
				continue;
			result.add(candidate);
		}
		if (result.isEmpty() && (!requireAdvancement || isAdvancementActive(anchor)))
			result.add(anchor);
		return result;
	}

	/**
	 * Selects a vessel contributed by an addon, under the same rules a built-in
	 * answers to: the trial gates above have already run, and the server claim
	 * limit is enforced here against the same saved data and the same game rule.
	 *
	 * @return false when no contributed vessel matches, so the caller can report
	 *         the original "does not exist" error
	 */
	private static boolean selectContributedVessel(ServerPlayer player, String identity) {
		Vessel vessel = VesselRegistry.byStoredIdentity(identity)
				.filter(candidate -> !candidate.hasLegacyJobId())
				.orElse(null);
		if (vessel == null)
			return false;
		if (!selectionOpenReady(player)) {
			closeSelection(player);
			return true;
		}

		// Prefixed so a mod namespaced "monarch" or "ruler" cannot collide with a
		// built-in claim key, which is stored as type + ":" + identity.
		String claimKey = "vessel:" + vessel.id();
		if (!VesselClaimSavedData.get(player.serverLevel())
				.tryClaim(claimKey, player.getUUID(), VesselManager.vesselLimit(player))) {
			selectionError(player, vessel.fallbackName() + " has already reached the server limit.");
			return true;
		}
		if (!VesselRegistry.assign(player, vessel)) {
			selectionError(player, "The System could not assign that vessel.");
			return true;
		}

		player.getPersistentData().remove(COMMAND_SELECTION_AUTHORIZED);
		closeSelection(player);
		return true;
	}

	private static void selectionError(ServerPlayer player, String message) {
		SystemNotifications.showNegativeTitleUnder(player, 0xFFFF3D5A, 80,
				Component.literal("VESSEL UNAVAILABLE").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				Component.literal(message).withStyle(ChatFormatting.GRAY));
		if (isSelectionPending(player) && isSelectionAuthorized(player)
				&& selectionOpenReady(player))
			sendSelectionState(player);
		else
			closeSelection(player);
	}

	private static void sendSelectionState(ServerPlayer player) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new VesselSelectionStateMessage(true, advancementPoints(player), requiredPoints(player),
						VesselManager.vesselLimit(player), VesselManager.claimCounts(player),
						DeveloperModeManager.isEnabled(player)));
	}

	private static void closeSelection(ServerPlayer player) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new VesselSelectionStateMessage(false, 0, requiredPoints(player),
						VesselManager.vesselLimit(player), new int[0],
						DeveloperModeManager.isEnabled(player)));
	}

	private static boolean markBossProcessed(Entity boss) {
		if (boss == null || boss.getPersistentData().getBoolean(
				ATTEMPT_BOSS_PROCESSED_TAG))
			return false;
		boss.getPersistentData().putBoolean(
				ATTEMPT_BOSS_PROCESSED_TAG, true);
		return true;
	}

	private static void setActiveAttemptId(ServerPlayer player,
			UUID attemptId) {
		if (player == null || attemptId == null)
			return;
		persistentPlayerData(player).putUUID(ATTEMPT_ID_TAG, attemptId);
	}

	private static void clearActiveAttempt(ServerPlayer player) {
		if (player != null)
			persistentPlayerData(player).remove(ATTEMPT_ID_TAG);
	}

	private static void clearRetryState(ServerPlayer player) {
		if (player == null)
			return;
		CompoundTag data = persistentPlayerData(player);
		data.remove(RETRY_AFTER_TAG);
		data.remove(FAILURE_NOTICE_TAG);
	}

	private static void resetFailedAttemptState(ServerPlayer player,
			@Nullable UUID attemptId, long retryAfter) {
		if (player == null)
			return;
		player.getPersistentData().remove(SELECTION_AUTHORIZED);
		player.getPersistentData().remove(COMMAND_SELECTION_AUTHORIZED);
		persistentPlayerData(player).remove(SELECTION_OPEN_AFTER_TAG);
		player.getPersistentData().putBoolean("slr_job_change_dungeon",
				false);
		player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY,
				null).ifPresent(capability -> {
					if (!contains(capability.unlocked_quests, QUEST_ID))
						capability.unlocked_quests = append(
								capability.unlocked_quests);
					capability.jobtimer = STATE_IDLE;
					capability.jobadvpoint = 0;
					capability.JobChange_timer = 0;
					capability.instancecomplete = false;
					capability.giftstatus = false;
					capability.jobkey = true;
					capability.BossKilled = false;
					capability.tpd = false;
					capability.syncPlayerVariables(player);
				});
		clearActiveAttempt(player);
		CompoundTag persisted = persistentPlayerData(player);
		if (retryAfter > currentGameTime(player.server))
			persisted.putLong(RETRY_AFTER_TAG, retryAfter);
		else
			persisted.remove(RETRY_AFTER_TAG);
		persisted.putBoolean(FAILURE_NOTICE_TAG, true);
		if (attemptId != null)
			JobChangeAttemptSavedData.get(player.server)
					.acknowledgeFailure(attemptId, player.getUUID());
		player.setNoGravity(false);
		player.fallDistance = 0.0F;
		closeSelection(player);
	}

	private static void showFailureNotice(ServerPlayer player) {
		int seconds = (retryDelayTicks(player) + 19) / 20;
		var detail = seconds > 0
				? Component.literal("The attempt was reset. Retry in "
						+ seconds + " seconds.")
				: Component.literal(
						"The attempt was reset. You can try again.");
		SystemNotifications.showNegativeTitleUnder(player,
				0xFFFF3D5A, 100,
				Component.literal("JOB CHANGE FAILED")
						.withStyle(ChatFormatting.RED,
								ChatFormatting.BOLD),
				detail.copy().withStyle(ChatFormatting.GRAY));
	}

	private static void showPendingFailureNotice(ServerPlayer player) {
		CompoundTag persisted = persistentPlayerData(player);
		if (!persisted.getBoolean(FAILURE_NOTICE_TAG))
			return;
		// The death handler resets the quest before Minecraft creates the
		// replacement player. At respawn/login time that replacement is already
		// idle, so recover() cannot infer that it still needs to leave the Igris
		// dimension. Keep this durable marker until the emergency exit succeeds.
		if (player.level().dimension().equals(IGRIS_DIMENSION)
				&& !DungeonDimensionPlayerLeavesDimensionProcedure
						.emergencyExit(player)) {
			showFailureNotice(player);
			return;
		}
		persisted.remove(FAILURE_NOTICE_TAG);
		showFailureNotice(player);
	}

	private static CompoundTag persistentPlayerData(Entity entity) {
		CompoundTag root = entity.getPersistentData();
		if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
			root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
		return root.getCompound(Player.PERSISTED_NBT_TAG);
	}

	private static long currentGameTime(MinecraftServer server) {
		return server == null ? 0L : server.overworld().getGameTime();
	}

	private static boolean isSelectionAuthorized(Entity entity) {
		return entity != null && (entity.getPersistentData().getBoolean(SELECTION_AUTHORIZED)
				|| advancementPoints(entity) >= requiredPoints(entity));
	}

	private static boolean isCommandSelectionAuthorized(Entity entity) {
		return entity != null && entity.getPersistentData().getBoolean(COMMAND_SELECTION_AUTHORIZED);
	}

	private static int state(SololevelingModVariables.PlayerVariables vars) {
		return (int) Math.round(vars.jobtimer);
	}

	private static SololevelingModVariables.PlayerVariables vars(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static boolean contains(String list, String id) {
		return list != null && list.contains(id + ",");
	}

	private static String append(String list) {
		if (list == null || list.equals("\"\"") || list.isBlank())
			return TOKEN;
		return list + TOKEN;
	}

	private static boolean selectionOpenReady(ServerPlayer player) {
		CompoundTag persisted = persistentPlayerData(player);
		if (!persisted.contains(SELECTION_OPEN_AFTER_TAG,
				Tag.TAG_ANY_NUMERIC))
			return true;
		if (currentGameTime(player.server)
				< persisted.getLong(SELECTION_OPEN_AFTER_TAG))
			return false;
		persisted.remove(SELECTION_OPEN_AFTER_TAG);
		return true;
	}

	private static String removeToken(String list, String id) {
		if (list == null || list.isBlank())
			return "";
		StringBuilder result = new StringBuilder();
		for (String entry : list.split(",")) {
			if (!entry.isBlank() && !entry.equals(id))
				result.append(entry).append(',');
		}
		return result.toString();
	}
}
