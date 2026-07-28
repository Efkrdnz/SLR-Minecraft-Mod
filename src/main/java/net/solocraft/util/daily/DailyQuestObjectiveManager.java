package net.solocraft.util.daily;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.entity.TrainingBotEntity;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.DailyQuestHelper;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.util.SystemPlayerAccess;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative tracking for the Minecraft-native Daily Quest.
 *
 * <p>This class only owns objective progress. It deliberately does not grant
 * rewards or deactivate the quest. Reward code can subscribe to
 * {@link DailyQuestObjectivesCompletedEvent}, perform the existing reward
 * transaction, and then clear {@code ActiveDaily}.</p>
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class DailyQuestObjectiveManager {
	public static final double NORMAL_MINING_TARGET = 32.0D;
	public static final double SECRET_MINING_TARGET = 64.0D;
	public static final double NORMAL_THREAT_TARGET = 8.0D;
	public static final double SECRET_THREAT_TARGET = 16.0D;
	public static final double NORMAL_DISTANCE_TARGET = 500.0D;
	public static final double SECRET_DISTANCE_TARGET = 1000.0D;

	/**
	 * Data packs can extend this tag with natural or modded blocks that should
	 * count toward the mining objective.
	 */
	public static final TagKey<Block> DAILY_MINEABLE = TagKey.create(Registries.BLOCK,
			new ResourceLocation(SololevelingMod.MODID, "daily_mineable"));

	/** Data-pack overrides for threat eligibility and point weighting. */
	public static final TagKey<EntityType<?>> DAILY_THREAT_EXCLUDED = entityTag("daily_threat_excluded");
	public static final TagKey<EntityType<?>> DAILY_THREAT_ELITES = entityTag("daily_threat_elites");
	public static final TagKey<EntityType<?>> DAILY_THREAT_MINIBOSSES = entityTag("daily_threat_minibosses");
	public static final TagKey<EntityType<?>> DAILY_THREAT_BOSSES = entityTag("daily_threat_bosses");

	private static final String MINED_POSITIONS_TAG = "slr_daily_mined_positions";
	private static final String MINED_DIMENSION_TAG = "Dimension";
	private static final String MINED_POSITION_TAG = "Position";
	private static final String COMPLETION_FIRED_TAG = "slr_daily_objectives_completion_fired";

	private static final String RADIRU_RESIDENT_TAG = "radiru_resident";
	private static final String RADIRU_TRAINING_DUMMY_TAG = "radiru_training_dummy";
	public static final String SYSTEM_TRAINING_OWNER_TAG = "slr_training_owner";
	private static final int SYSTEM_TRAINING_BOT_THREAT_POINTS = 8;
	private static final double MAX_ON_FOOT_DISTANCE_PER_TICK = 2.5D;
	private static final long CLIENT_SYNC_INTERVAL = 20L;

	private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();

	private DailyQuestObjectiveManager() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onBlockBroken(BlockEvent.BreakEvent event) {
		if (!(event.getPlayer() instanceof ServerPlayer player) || player instanceof FakePlayer
				|| !isSurvival(player) || !isQuestActive(player))
			return;
		BlockState state = event.getState();
		if (!isDailyMineable(state) || !hasCorrectMiningTool(player, state))
			return;
		recordMinedBlock(player, event.getPos());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDeath(LivingDeathEvent event) {
		if (!(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel))
			return;
		ServerPlayer player = creditedPlayer(event.getSource());
		if (player == null || player instanceof FakePlayer || !isSurvival(player)
				|| !isQuestActive(player))
			return;
		LivingEntity target = event.getEntity();
		boolean systemTrainingBot = isOwnedSystemTrainingBot(target, player);
		if (!systemTrainingBot && !isEligibleThreat(target, player))
			return;
		int points = systemTrainingBot ? SYSTEM_TRAINING_BOT_THREAT_POINTS : threatWeight(target);
		if (points > 0)
			recordThreatPoints(player, points);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player))
			return;
		SololevelingModVariables.PlayerVariables variables = variables(player);
		if (variables == null || !variables.ActiveDaily
				|| !SystemPlayerAccess.hasSystem(player)) {
			clearInactiveQuestRuntime(player);
			return;
		}

		RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
		if (variables.dailyMinedBlocks <= 0.0D && variables.dailyThreatPoints <= 0.0D
				&& variables.RUN <= 0.0D)
			resetFreshQuestMarkers(player);

		trackOnFootDistance(player, variables, runtime);
		boolean complete = evaluateCompletion(player, variables);
		if (runtime.dirty && (complete || runtime.lastSyncAt == Long.MIN_VALUE
				|| player.level().getGameTime() - runtime.lastSyncAt >= CLIENT_SYNC_INTERVAL))
			syncNow(player, variables, runtime);
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			resetMovementAnchor(player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			RUNTIME.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			resetMovementAnchor(player);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			resetMovementAnchor(player);
	}

	@SubscribeEvent
	public static void onPlayerTeleported(EntityTeleportEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			resetMovementAnchor(player);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		CompoundTag oldData = event.getOriginal().getPersistentData();
		CompoundTag newData = player.getPersistentData();
		if (oldData.contains(MINED_POSITIONS_TAG, Tag.TAG_LIST))
			newData.put(MINED_POSITIONS_TAG, oldData.getList(MINED_POSITIONS_TAG, Tag.TAG_COMPOUND).copy());
		if (oldData.getBoolean(COMPLETION_FIRED_TAG))
			newData.putBoolean(COMPLETION_FIRED_TAG, true);
		resetMovementAnchor(player);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		RUNTIME.clear();
	}

	public static boolean isQuestActive(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables variables = variables(player);
		return variables != null && variables.ActiveDaily
				&& SystemPlayerAccess.hasSystem(player);
	}

	public static boolean isSecretQuest(ServerPlayer player) {
		return DailyQuestHelper.isSecretQuest(player);
	}

	public static double miningTarget(ServerPlayer player) {
		return isSecretQuest(player) ? SECRET_MINING_TARGET : NORMAL_MINING_TARGET;
	}

	public static double threatTarget(ServerPlayer player) {
		return isSecretQuest(player) ? SECRET_THREAT_TARGET : NORMAL_THREAT_TARGET;
	}

	public static double distanceTarget(ServerPlayer player) {
		return isSecretQuest(player) ? SECRET_DISTANCE_TARGET : NORMAL_DISTANCE_TARGET;
	}

	/**
	 * The combat objective is waived on Peaceful so a Daily Quest never becomes
	 * impossible after world difficulty changes.
	 */
	public static boolean isThreatObjectiveRequired(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables variables = variables(player);
		return player != null && variables != null && !variables.dailyCombatWaived;
	}

	public static boolean isDailyMineable(BlockState state) {
		return state != null && state.is(DAILY_MINEABLE);
	}

	public static boolean hasCorrectMiningTool(ServerPlayer player, BlockState state) {
		if (player == null || state == null)
			return false;
		ItemStack tool = player.getMainHandItem();
		return !tool.isEmpty() && tool.isCorrectToolForDrops(state);
	}

	/**
	 * Records one unique block position for this quest. Persisting the small
	 * position set prevents place/break loops and survives reconnects.
	 */
	public static boolean recordMinedBlock(ServerPlayer player, BlockPos position) {
		if (player == null || position == null || !isSurvival(player)
				|| !isQuestActive(player))
			return false;
		SololevelingModVariables.PlayerVariables variables = variables(player);
		if (variables == null || variables.dailyMinedBlocks >= miningTarget(player)
				|| !rememberMinedPosition(player, position, variables.dailyMinedBlocks))
			return false;

		double previous = variables.dailyMinedBlocks;
		variables.dailyMinedBlocks = Math.min(miningTarget(player), previous + 1.0D);
		markDirty(player);
		DailyQuestHelper.checkSecretTransition(player, previous, variables.dailyMinedBlocks,
				NORMAL_MINING_TARGET);
		evaluateCompletion(player, variables);
		return true;
	}

	public static boolean recordThreatPoints(ServerPlayer player, int points) {
		if (player == null || points <= 0 || !isSurvival(player)
				|| !isQuestActive(player)
				|| !isThreatObjectiveRequired(player))
			return false;
		SololevelingModVariables.PlayerVariables variables = variables(player);
		if (variables == null || variables.dailyThreatPoints >= threatTarget(player))
			return false;

		double previous = variables.dailyThreatPoints;
		variables.dailyThreatPoints = Math.min(threatTarget(player), previous + points);
		markDirty(player);
		DailyQuestHelper.checkSecretTransition(player, previous, variables.dailyThreatPoints,
				NORMAL_THREAT_TARGET);
		evaluateCompletion(player, variables);
		return true;
	}

	public static boolean recordDistance(ServerPlayer player, double blocks) {
		if (player == null || !Double.isFinite(blocks) || blocks <= 0.0D
				|| !isSurvival(player) || !isQuestActive(player))
			return false;
		SololevelingModVariables.PlayerVariables variables = variables(player);
		if (variables == null || variables.RUN >= distanceTarget(player))
			return false;

		double previous = variables.RUN;
		variables.RUN = Math.min(distanceTarget(player), previous + blocks);
		markDirty(player);
		DailyQuestHelper.checkSecretTransition(player, previous, variables.RUN, NORMAL_DISTANCE_TARGET);
		evaluateCompletion(player, variables);
		return true;
	}

	public static boolean isComplete(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables variables = variables(player);
		return variables != null && variables.ActiveDaily && objectivesComplete(player, variables);
	}

	public static ProgressSnapshot snapshot(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables variables = variables(player);
		boolean secret = isSecretQuest(player);
		double mined = variables == null ? 0.0D : Math.max(0.0D, variables.dailyMinedBlocks);
		double threat = variables == null ? 0.0D : Math.max(0.0D, variables.dailyThreatPoints);
		double distance = variables == null ? 0.0D : Math.max(0.0D, variables.RUN);
		return new ProgressSnapshot(secret, mined,
				secret ? SECRET_MINING_TARGET : NORMAL_MINING_TARGET,
				threat, secret ? SECRET_THREAT_TARGET : NORMAL_THREAT_TARGET,
				distance, secret ? SECRET_DISTANCE_TARGET : NORMAL_DISTANCE_TARGET,
				isThreatObjectiveRequired(player));
	}

	/**
	 * Re-evaluates and emits the one-shot completion event. Integration code can
	 * call this after administrative progress changes.
	 */
	public static boolean evaluateCompletion(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables variables = variables(player);
		return variables != null && evaluateCompletion(player, variables);
	}

	/**
	 * Clears anti-exploit and one-shot markers. Quest assignment/reset code
	 * should call this when replacing an active quest without first setting
	 * {@code ActiveDaily} to false.
	 */
	public static void resetQuestRuntime(ServerPlayer player) {
		if (player == null)
			return;
		RUNTIME.remove(player.getUUID());
		CompoundTag data = player.getPersistentData();
		data.remove(MINED_POSITIONS_TAG);
		data.remove(COMPLETION_FIRED_TAG);
	}

	public static boolean isEligibleThreat(LivingEntity target, ServerPlayer player) {
		if (target == null || player == null || !(target instanceof Enemy)
				|| target.getType().is(DAILY_THREAT_EXCLUDED))
			return false;
		CompoundTag data = target.getPersistentData();
		if (data.getBoolean(RADIRU_TRAINING_DUMMY_TAG) || data.getBoolean(RADIRU_RESIDENT_TAG)
				|| ShadowMonarchManager.isShadowEntity(target))
			return false;
		if (target instanceof TamableAnimal tame && tame.isTame())
			return false;
		if (target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null)
			return false;
		return !player.isAlliedTo(target) && !target.isAlliedTo(player);
	}

	/**
	 * A System-spawned Training Bot is the guaranteed Combat Readiness fallback.
	 * It deliberately bypasses the general exclusion tag only when its persisted
	 * owner is the credited player and that player still has an active training
	 * session. Spawn eggs, commands, another player's bot, and Radiru dummies
	 * therefore remain ineligible.
	 */
	public static boolean isOwnedSystemTrainingBot(LivingEntity target, ServerPlayer player) {
		if (!(target instanceof TrainingBotEntity) || player == null)
			return false;
		CompoundTag targetData = target.getPersistentData();
		if (targetData.getBoolean(RADIRU_TRAINING_DUMMY_TAG)
				|| targetData.getBoolean(RADIRU_RESIDENT_TAG)
				|| !targetData.hasUUID(SYSTEM_TRAINING_OWNER_TAG)
				|| !player.getUUID().equals(targetData.getUUID(SYSTEM_TRAINING_OWNER_TAG)))
			return false;
		SololevelingModVariables.PlayerVariables variables = variables(player);
		return variables != null && variables.istraining;
	}

	/**
	 * Point order is explicit data-pack tags, dungeon role metadata, vanilla
	 * bosses, then conservative health/name fallbacks for legacy mod mobs.
	 */
	public static int threatWeight(LivingEntity target) {
		if (target == null)
			return 0;
		EntityType<?> type = target.getType();
		if (type.is(DAILY_THREAT_BOSSES))
			return 8;
		if (type.is(DAILY_THREAT_MINIBOSSES))
			return 4;
		if (type.is(DAILY_THREAT_ELITES))
			return 3;

		CompoundTag data = target.getPersistentData();
		String role = data.getString(DungeonMobLevelAdapter.ROLE_TAG).trim().toLowerCase(Locale.ROOT);
		if (!role.isEmpty()) {
			if (role.equals("boss"))
				return 8;
			if (role.equals("miniboss") || role.equals("mini_boss"))
				return 4;
			if (role.equals("elite"))
				return 3;
			if (role.equals("normal"))
				return 1;
		}

		if (target instanceof EnderDragon || target instanceof WitherBoss
				|| booleanTag(data, "Boss", "boss", "slr_boss"))
			return 8;
		if (booleanTag(data, "Miniboss", "miniboss", "mini_boss", "slr_miniboss"))
			return 4;
		if (booleanTag(data, "Elite", "elite", "slr_elite"))
			return 3;

		ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
		String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
		if (path.contains("boss"))
			return 8;
		if (path.contains("miniboss") || path.contains("mini_boss"))
			return 4;

		double maximumHealth = target.getMaxHealth();
		if (maximumHealth >= 300.0D)
			return 8;
		if (maximumHealth >= 160.0D)
			return 4;
		if (maximumHealth >= 60.0D)
			return 3;
		return 1;
	}

	@Nullable
	public static ServerPlayer creditedPlayer(DamageSource source) {
		if (source == null)
			return null;
		Set<UUID> visited = new HashSet<>();
		ServerPlayer credited = creditedPlayer(source.getEntity(), visited);
		return credited != null ? credited : creditedPlayer(source.getDirectEntity(), visited);
	}

	private static boolean evaluateCompletion(ServerPlayer player,
			SololevelingModVariables.PlayerVariables variables) {
		if (!variables.ActiveDaily || !objectivesComplete(player, variables))
			return false;
		CompoundTag data = player.getPersistentData();
		if (data.getBoolean(COMPLETION_FIRED_TAG))
			return true;

		data.putBoolean(COMPLETION_FIRED_TAG, true);
		RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
		if (runtime.dirty)
			syncNow(player, variables, runtime);
		MinecraftForge.EVENT_BUS.post(new DailyQuestObjectivesCompletedEvent(player, snapshot(player)));
		return true;
	}

	private static boolean objectivesComplete(ServerPlayer player,
			SololevelingModVariables.PlayerVariables variables) {
		return variables.dailyMinedBlocks >= miningTarget(player)
				&& (!isThreatObjectiveRequired(player)
						|| variables.dailyThreatPoints >= threatTarget(player))
				&& variables.RUN >= distanceTarget(player);
	}

	private static void trackOnFootDistance(ServerPlayer player,
			SololevelingModVariables.PlayerVariables variables, RuntimeState runtime) {
		long gameTime = player.level().getGameTime();
		ResourceKey<Level> dimension = player.level().dimension();
		Vec3 position = player.position();
		boolean eligible = isOnFoot(player);
		MovementAnchor previous = runtime.anchor;
		runtime.anchor = new MovementAnchor(dimension, position, gameTime, eligible);

		if (previous == null || !previous.dimension.equals(dimension) || previous.gameTime + 1L != gameTime
				|| !previous.eligible || !eligible || variables.RUN >= distanceTarget(player))
			return;
		double dx = position.x - previous.position.x;
		double dz = position.z - previous.position.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		if (horizontal <= 0.0D || horizontal > MAX_ON_FOOT_DISTANCE_PER_TICK)
			return;
		recordDistance(player, horizontal);
	}

	private static boolean isOnFoot(ServerPlayer player) {
		return isSurvival(player) && player.isAlive() && !player.isSleeping() && !player.isPassenger()
				&& !player.isFallFlying() && !player.getAbilities().flying
				&& !player.isSwimming() && !player.isInWaterOrBubble() && !player.isInLava();
	}

	private static boolean isSurvival(ServerPlayer player) {
		return player != null && !player.isCreative() && !player.isSpectator();
	}

	private static void resetMovementAnchor(ServerPlayer player) {
		if (player == null)
			return;
		RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
		runtime.anchor = null;
	}

	private static void markDirty(ServerPlayer player) {
		RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState()).dirty = true;
	}

	private static void syncNow(ServerPlayer player, SololevelingModVariables.PlayerVariables variables,
			RuntimeState runtime) {
		variables.syncPlayerVariables(player);
		runtime.dirty = false;
		runtime.lastSyncAt = player.level().getGameTime();
	}

	private static void clearInactiveQuestRuntime(ServerPlayer player) {
		RUNTIME.remove(player.getUUID());
		CompoundTag data = player.getPersistentData();
		data.remove(MINED_POSITIONS_TAG);
		data.remove(COMPLETION_FIRED_TAG);
	}

	private static void resetFreshQuestMarkers(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		data.remove(MINED_POSITIONS_TAG);
		data.remove(COMPLETION_FIRED_TAG);
	}

	private static boolean rememberMinedPosition(ServerPlayer player, BlockPos position,
			double currentProgress) {
		CompoundTag data = player.getPersistentData();
		if (currentProgress <= 0.0D)
			data.remove(MINED_POSITIONS_TAG);
		ListTag positions = data.getList(MINED_POSITIONS_TAG, Tag.TAG_COMPOUND);
		String dimension = player.level().dimension().location().toString();
		long packedPosition = position.asLong();
		for (Tag entry : positions) {
			if (entry instanceof CompoundTag mined
					&& packedPosition == mined.getLong(MINED_POSITION_TAG)
					&& dimension.equals(mined.getString(MINED_DIMENSION_TAG)))
				return false;
		}
		CompoundTag mined = new CompoundTag();
		mined.putString(MINED_DIMENSION_TAG, dimension);
		mined.putLong(MINED_POSITION_TAG, packedPosition);
		positions.add(mined);
		data.put(MINED_POSITIONS_TAG, positions);
		return true;
	}

	@Nullable
	private static ServerPlayer creditedPlayer(@Nullable Entity source, Set<UUID> visited) {
		if (source == null || !visited.add(source.getUUID()))
			return null;
		if (source instanceof ServerPlayer player)
			return player instanceof FakePlayer ? null : player;
		if (source instanceof Projectile projectile) {
			ServerPlayer owner = creditedPlayer(projectile.getOwner(), visited);
			if (owner != null)
				return owner;
		}
		if (source instanceof TamableAnimal tame) {
			ServerPlayer owner = creditedPlayer(tame.getOwner(), visited);
			if (owner != null)
				return owner;
		}
		if (source instanceof OwnableEntity ownable) {
			ServerPlayer owner = creditedPlayer(ownable.getOwner(), visited);
			if (owner != null)
				return owner;
		}
		if (source.getServer() != null) {
			UUID ownerId = ShadowMonarchManager.getShadowOwnerUUID(source);
			if (ownerId != null)
				return source.getServer().getPlayerList().getPlayer(ownerId);
		}
		return null;
	}

	private static boolean booleanTag(CompoundTag data, String... keys) {
		for (String key : keys)
			if (data.getBoolean(key))
				return true;
		return false;
	}

	private static TagKey<EntityType<?>> entityTag(String path) {
		return TagKey.create(Registries.ENTITY_TYPE,
				new ResourceLocation(SololevelingMod.MODID, path));
	}

	@Nullable
	private static SololevelingModVariables.PlayerVariables variables(@Nullable ServerPlayer player) {
		if (player == null)
			return null;
		return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(null);
	}

	public record ProgressSnapshot(boolean secretQuest, double minedBlocks, double miningTarget,
			double threatPoints, double threatTarget, double distance, double distanceTarget,
			boolean threatRequired) {
		public boolean complete() {
			return minedBlocks >= miningTarget
					&& (!threatRequired || threatPoints >= threatTarget)
					&& distance >= distanceTarget;
		}
	}

	private static final class RuntimeState {
		private MovementAnchor anchor;
		private long lastSyncAt = Long.MIN_VALUE;
		private boolean dirty;
	}

	private record MovementAnchor(ResourceKey<Level> dimension, Vec3 position, long gameTime,
			boolean eligible) {
	}
}
