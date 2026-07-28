package net.solocraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Durable, overworld-backed state for the one-time Story Mode prologue.
 *
 * <p>Only identifiers and state-machine progress live here. The Hunter entities
 * remain normal persistent entities, so their equipment, attributes, health,
 * tame owner, appearance, and other mod data continue to use vanilla entity NBT.</p>
 */
public final class StoryModeIntroSavedData extends SavedData {
	private static final String DATA_NAME = "sololeveling_story_mode_intro";
	private static final int SCHEMA_VERSION = 3;
	public static final int DUNGEON_BREAK_GRACE_CLEARS = 10;

	public enum Stage {
		NOT_STARTED,
		PREPARING,
		ANCIENT_GOLEM,
		GATE_WAIT,
		TEMPLE,
		LASER_EXECUTION,
		WAITING_FOR_SNEAK,
		STATUE_WAKING,
		STATUE_HUNT,
		PLAYER_HUNT,
		COMPLETE;

		private static Stage load(String name) {
			if (name == null || name.isBlank())
				return NOT_STARTED;
			try {
				return valueOf(name);
			} catch (IllegalArgumentException ignored) {
				return NOT_STARTED;
			}
		}

		private static boolean recognizes(String name) {
			if (name == null || name.isBlank())
				return false;
			try {
				valueOf(name);
				return true;
			} catch (IllegalArgumentException ignored) {
				return false;
			}
		}
	}

	private Stage stage = Stage.NOT_STARTED;
	@Nullable
	private UUID ownerId;
	private int playerClassId;
	private boolean dungeonPlaced;
	@Nullable
	private UUID bossId;
	private final ArrayList<UUID> hunterIds = new ArrayList<>();
	@Nullable
	private UUID gateId;
	private int cartenonInstanceId;
	@Nullable
	private UUID godStatueId;
	@Nullable
	private BlockPos godStatuePosition;
	private long stageStartedTick;
	private final ArrayList<UUID> laserTargetIds = new ArrayList<>();
	private int laserFiredCount;
	private int laserKilledCount;
	private long laserLastFiredTick;
	private int postIntroDungeonClears;
	private final LinkedHashSet<String> dungeonClearReceipts = new LinkedHashSet<>();
	/**
	 * Runtime-only recovery observation. It deliberately is not serialized so
	 * every server load gives chunk entities time to attach before replacement.
	 */
	private long bossMissingSinceTick = -1L;

	public static StoryModeIntroSavedData get(ServerLevel level) {
		if (level == null)
			throw new IllegalArgumentException("A server level is required.");
		return get(level.getServer());
	}

	public static StoryModeIntroSavedData get(MinecraftServer server) {
		if (server == null)
			throw new IllegalArgumentException("A Minecraft server is required.");
		return server.overworld().getDataStorage().computeIfAbsent(
				StoryModeIntroSavedData::load, StoryModeIntroSavedData::new, DATA_NAME);
	}

	public Stage stage() {
		return stage;
	}

	public boolean isActive() {
		return ownerId != null && stage != Stage.NOT_STARTED && stage != Stage.COMPLETE;
	}

	@Nullable
	public UUID ownerId() {
		return ownerId;
	}

	public boolean isOwner(UUID playerId) {
		return ownerId != null && ownerId.equals(playerId);
	}

	public int playerClassId() {
		return playerClassId;
	}

	public boolean dungeonPlaced() {
		return dungeonPlaced;
	}

	@Nullable
	public UUID bossId() {
		return bossId;
	}

	public List<UUID> hunterIds() {
		return List.copyOf(hunterIds);
	}

	@Nullable
	public UUID gateId() {
		return gateId;
	}

	public int cartenonInstanceId() {
		return cartenonInstanceId;
	}

	@Nullable
	public UUID godStatueId() {
		return godStatueId;
	}

	@Nullable
	public BlockPos godStatuePosition() {
		return godStatuePosition;
	}

	public long stageStartedTick() {
		return stageStartedTick;
	}

	public List<UUID> laserTargetIds() {
		return List.copyOf(laserTargetIds);
	}

	public int laserFiredCount() {
		return laserFiredCount;
	}

	public int laserKilledCount() {
		return laserKilledCount;
	}

	public long laserLastFiredTick() {
		return laserLastFiredTick;
	}

	public int postIntroDungeonClears() {
		return postIntroDungeonClears;
	}

	public int dungeonBreakGraceRemaining() {
		return Math.max(0, DUNGEON_BREAK_GRACE_CLEARS - postIntroDungeonClears);
	}

	/**
	 * The grace applies world-wide because natural overworld gates are not bound to
	 * one player. The Story owner remains persisted after the prologue completes.
	 */
	public boolean hasDungeonBreakGrace() {
		return ownerId != null && stage != Stage.NOT_STARTED
				&& postIntroDungeonClears < DUNGEON_BREAK_GRACE_CLEARS;
	}

	/**
	 * Credits one successfully exited normal gate dungeon to the Story owner.
	 * Receipts make the boss, normal-exit, party, and alternate-exit paths
	 * idempotent. The prologue itself cannot consume a clear because it records
	 * only after the Story state reaches COMPLETE.
	 */
	public boolean recordPostIntroDungeonClear(UUID playerId, String receipt) {
		String normalizedReceipt = normalizeDungeonClearReceipt(receipt);
		if (!isOwner(playerId) || stage != Stage.COMPLETE
				|| postIntroDungeonClears >= DUNGEON_BREAK_GRACE_CLEARS
				|| normalizedReceipt.isEmpty()
				|| !dungeonClearReceipts.add(normalizedReceipt))
			return false;
		postIntroDungeonClears = Math.min(DUNGEON_BREAK_GRACE_CLEARS,
				postIntroDungeonClears + 1);
		setDirty();
		return true;
	}

	/**
	 * Atomically claims an unstarted world for its first Story Mode player.
	 */
	public boolean claimOwner(UUID playerId, int classId, long gameTick) {
		if (playerId == null || stage != Stage.NOT_STARTED || ownerId != null)
			return false;
		ownerId = playerId;
		playerClassId = classId;
		stage = Stage.PREPARING;
		stageStartedTick = Math.max(0L, gameTick);
		setDirty();
		return true;
	}

	public void setStage(Stage next, long gameTick) {
		if (next == null || stage == next)
			return;
		stage = next;
		stageStartedTick = Math.max(0L, gameTick);
		setDirty();
	}

	public void markDungeonPlaced() {
		if (dungeonPlaced)
			return;
		dungeonPlaced = true;
		setDirty();
	}

	public void setBossId(@Nullable UUID id) {
		if (id != null)
			noteBossObserved();
		if (equalsUuid(bossId, id))
			return;
		bossId = id;
		setDirty();
	}

	public void noteBossObserved() {
		bossMissingSinceTick = -1L;
	}

	public boolean bossRecoveryGraceElapsed(long gameTick, long graceTicks) {
		long now = Math.max(0L, gameTick);
		if (bossMissingSinceTick < 0L || now < bossMissingSinceTick) {
			bossMissingSinceTick = now;
			return false;
		}
		return now - bossMissingSinceTick >= Math.max(0L, graceTicks);
	}

	public void replaceHunterIds(List<UUID> ids) {
		LinkedHashSet<UUID> sanitized = new LinkedHashSet<>();
		if (ids != null) {
			for (UUID id : ids) {
				if (id != null && sanitized.size() < StoryModeIntroManager.TEAM_SIZE)
					sanitized.add(id);
			}
		}
		if (hunterIds.equals(new ArrayList<>(sanitized)))
			return;
		hunterIds.clear();
		hunterIds.addAll(sanitized);
		setDirty();
	}

	public void setGate(@Nullable UUID id, int instanceId, long gameTick) {
		if (stage != Stage.ANCIENT_GOLEM && stage != Stage.GATE_WAIT)
			return;
		boolean changed = !equalsUuid(gateId, id)
				|| cartenonInstanceId != Math.max(1, instanceId)
				|| stage != Stage.GATE_WAIT;
		gateId = id;
		cartenonInstanceId = Math.max(1, instanceId);
		stage = Stage.GATE_WAIT;
		stageStartedTick = Math.max(0L, gameTick);
		if (changed)
			setDirty();
	}

	public void setTemple(int instanceId, @Nullable UUID statueId, long gameTick) {
		if (stage != Stage.GATE_WAIT)
			return;
		boolean changed = cartenonInstanceId != Math.max(1, instanceId)
				|| !equalsUuid(godStatueId, statueId)
				|| stage != Stage.TEMPLE;
		cartenonInstanceId = Math.max(1, instanceId);
		if (!equalsUuid(godStatueId, statueId))
			godStatuePosition = null;
		godStatueId = statueId;
		stage = Stage.TEMPLE;
		stageStartedTick = Math.max(0L, gameTick);
		if (changed)
			setDirty();
	}

	public void setGodStatueId(@Nullable UUID id) {
		if (equalsUuid(godStatueId, id))
			return;
		godStatueId = id;
		godStatuePosition = null;
		setDirty();
	}

	public void trackGodStatue(UUID id, BlockPos position) {
		if (id == null || position == null)
			return;
		boolean changed = !equalsUuid(godStatueId, id)
				|| !position.equals(godStatuePosition);
		godStatueId = id;
		godStatuePosition = position.immutable();
		if (changed)
			setDirty();
	}

	public void beginLasers(List<UUID> targets, long gameTick) {
		LinkedHashSet<UUID> sanitized = new LinkedHashSet<>();
		if (targets != null) {
			for (UUID id : targets) {
				if (id != null && sanitized.size() < 2)
					sanitized.add(id);
			}
		}
		laserTargetIds.clear();
		laserTargetIds.addAll(sanitized);
		laserFiredCount = 0;
		laserKilledCount = 0;
		laserLastFiredTick = 0L;
		stage = Stage.LASER_EXECUTION;
		stageStartedTick = Math.max(0L, gameTick);
		setDirty();
	}

	public void markLaserFired(int count, long gameTick) {
		int next = Math.max(0, Math.min(laserTargetIds.size(), count));
		long nextTick = Math.max(0L, gameTick);
		if (laserFiredCount == next && laserLastFiredTick == nextTick)
			return;
		laserFiredCount = next;
		laserLastFiredTick = nextTick;
		setDirty();
	}

	public boolean replacePendingLaserTarget(int index, UUID replacement) {
		if (stage != Stage.LASER_EXECUTION || replacement == null
				|| index != laserKilledCount || index < 0
				|| index >= laserTargetIds.size())
			return false;
		for (int targetIndex = 0; targetIndex < laserTargetIds.size();
				targetIndex++) {
			if (targetIndex != index
					&& replacement.equals(laserTargetIds.get(targetIndex)))
				return false;
		}
		if (replacement.equals(laserTargetIds.get(index)))
			return true;
		laserTargetIds.set(index, replacement);
		laserFiredCount = Math.min(laserFiredCount, index);
		laserKilledCount = Math.min(laserKilledCount, index);
		laserLastFiredTick = 0L;
		setDirty();
		return true;
	}

	public void setLaserKilledCount(int count) {
		int next = Math.max(0, Math.min(laserFiredCount, count));
		if (laserKilledCount == next)
			return;
		laserKilledCount = next;
		setDirty();
	}

	@Nonnull
	@Override
	public CompoundTag save(@Nonnull CompoundTag root) {
		root.putInt("SchemaVersion", SCHEMA_VERSION);
		root.putString("Stage", stage.name());
		if (ownerId != null)
			root.putUUID("Owner", ownerId);
		root.putInt("PlayerClass", playerClassId);
		root.putBoolean("DungeonPlaced", dungeonPlaced);
		if (bossId != null)
			root.putUUID("Boss", bossId);
		root.put("Hunters", saveUuidList(hunterIds));
		if (gateId != null)
			root.putUUID("Gate", gateId);
		root.putInt("CartenonInstance", cartenonInstanceId);
		if (godStatueId != null)
			root.putUUID("GodStatue", godStatueId);
		if (godStatuePosition != null)
			root.putLong("GodStatuePos", godStatuePosition.asLong());
		root.putLong("StageStartedTick", stageStartedTick);
		root.put("LaserTargets", saveUuidList(laserTargetIds));
		root.putInt("LaserFired", laserFiredCount);
		root.putInt("LaserKilled", laserKilledCount);
		root.putLong("LaserLastFiredTick", laserLastFiredTick);
		root.putInt("PostIntroDungeonClears", postIntroDungeonClears);
		ListTag clearReceipts = new ListTag();
		for (String receipt : dungeonClearReceipts)
			clearReceipts.add(StringTag.valueOf(receipt));
		root.put("DungeonClearReceipts", clearReceipts);
		return root;
	}

	private static StoryModeIntroSavedData load(CompoundTag root) {
		StoryModeIntroSavedData data = new StoryModeIntroSavedData();
		String storedStage = root.getString("Stage");
		boolean unknownStage = !Stage.recognizes(storedStage);
		int storedSchema = root.contains("SchemaVersion", Tag.TAG_INT)
				? root.getInt("SchemaVersion") : 0;
		boolean unsupportedSchema = storedSchema < 0
				|| storedSchema > SCHEMA_VERSION;
		boolean olderSchema = storedSchema >= 0
				&& storedSchema < SCHEMA_VERSION;
		data.stage = Stage.load(storedStage);
		data.ownerId = root.hasUUID("Owner") ? root.getUUID("Owner") : null;
		data.playerClassId = sanitizeClass(root.getInt("PlayerClass"));
		data.dungeonPlaced = root.getBoolean("DungeonPlaced");
		data.bossId = root.hasUUID("Boss") ? root.getUUID("Boss") : null;
		data.hunterIds.addAll(loadUuidList(root, "Hunters", StoryModeIntroManager.TEAM_SIZE));
		data.gateId = root.hasUUID("Gate") ? root.getUUID("Gate") : null;
		data.cartenonInstanceId = Math.max(0, root.getInt("CartenonInstance"));
		data.godStatueId = root.hasUUID("GodStatue") ? root.getUUID("GodStatue") : null;
		data.godStatuePosition = root.contains("GodStatuePos", Tag.TAG_LONG)
				? BlockPos.of(root.getLong("GodStatuePos")) : null;
		data.stageStartedTick = Math.max(0L, root.getLong("StageStartedTick"));
		data.laserTargetIds.addAll(loadUuidList(root, "LaserTargets", 2));
		data.laserFiredCount = Math.max(0, Math.min(data.laserTargetIds.size(), root.getInt("LaserFired")));
		data.laserKilledCount = Math.max(0, Math.min(
				data.laserFiredCount, root.getInt("LaserKilled")));
		data.laserLastFiredTick = Math.max(0L,
				root.getLong("LaserLastFiredTick"));
		data.postIntroDungeonClears = Math.max(0, Math.min(
				DUNGEON_BREAK_GRACE_CLEARS,
				root.getInt("PostIntroDungeonClears")));
		ListTag clearReceipts = root.getList("DungeonClearReceipts",
				Tag.TAG_STRING);
		for (int index = 0; index < clearReceipts.size()
				&& data.dungeonClearReceipts.size()
						< DUNGEON_BREAK_GRACE_CLEARS; index++) {
			String receipt = normalizeDungeonClearReceipt(
					clearReceipts.getString(index));
			if (!receipt.isEmpty())
				data.dungeonClearReceipts.add(receipt);
		}
		data.postIntroDungeonClears = Math.max(data.postIntroDungeonClears,
				data.dungeonClearReceipts.size());
		if (olderSchema && data.laserFiredCount > data.laserKilledCount
				&& data.laserLastFiredTick == 0L)
			data.laserLastFiredTick = data.stageStartedTick;

		boolean recoverOwnedState = data.ownerId != null
				&& (unknownStage
						|| (unsupportedSchema
								&& data.stage != Stage.COMPLETE)
						|| data.stage == Stage.NOT_STARTED);
		if (recoverOwnedState) {
			data.stage = Stage.PREPARING;
			data.bossId = null;
			data.hunterIds.clear();
			data.gateId = null;
			data.cartenonInstanceId = 0;
			data.godStatueId = null;
			data.godStatuePosition = null;
			data.laserTargetIds.clear();
			data.laserFiredCount = 0;
			data.laserKilledCount = 0;
			data.laserLastFiredTick = 0L;
			data.setDirty();
		}

		boolean missingRequiredInstance = data.ownerId != null
				&& data.stage.ordinal() >= Stage.GATE_WAIT.ordinal()
				&& data.stage != Stage.COMPLETE
				&& data.cartenonInstanceId <= 0;
		if (missingRequiredInstance) {
			data.stage = Stage.ANCIENT_GOLEM;
			data.gateId = null;
			data.cartenonInstanceId = 0;
			data.godStatueId = null;
			data.godStatuePosition = null;
			data.laserTargetIds.clear();
			data.laserFiredCount = 0;
			data.laserKilledCount = 0;
			data.laserLastFiredTick = 0L;
			data.setDirty();
		}

		boolean missingLaserPlan = data.ownerId != null
				&& data.stage.ordinal()
						>= Stage.LASER_EXECUTION.ordinal()
				&& data.stage.ordinal() <= Stage.PLAYER_HUNT.ordinal()
				&& data.laserTargetIds.isEmpty();
		if (missingLaserPlan) {
			data.stage = Stage.TEMPLE;
			data.laserFiredCount = 0;
			data.laserKilledCount = 0;
			data.laserLastFiredTick = 0L;
			data.setDirty();
		}

		boolean invalidOwnerState = data.ownerId == null
				&& data.stage != Stage.NOT_STARTED;
		if (invalidOwnerState) {
			data.stage = Stage.NOT_STARTED;
			data.playerClassId = 0;
			data.dungeonPlaced = false;
			data.bossId = null;
			data.hunterIds.clear();
			data.gateId = null;
			data.cartenonInstanceId = 0;
			data.godStatueId = null;
			data.godStatuePosition = null;
			data.laserTargetIds.clear();
			data.laserFiredCount = 0;
			data.laserKilledCount = 0;
			data.laserLastFiredTick = 0L;
			data.postIntroDungeonClears = 0;
			data.dungeonClearReceipts.clear();
			data.setDirty();
		} else if (data.ownerId != null && data.playerClassId == 0) {
			data.playerClassId = StoryModeIntroManager.ASSASSIN_CLASS_ID;
			data.setDirty();
		}
		if (olderSchema)
			data.setDirty();
		return data;
	}

	private static ListTag saveUuidList(List<UUID> values) {
		ListTag list = new ListTag();
		for (UUID value : values) {
			if (value == null)
				continue;
			CompoundTag entry = new CompoundTag();
			entry.putUUID("Id", value);
			list.add(entry);
		}
		return list;
	}

	private static List<UUID> loadUuidList(CompoundTag root, String key, int maximum) {
		Set<UUID> result = new LinkedHashSet<>();
		ListTag list = root.getList(key, Tag.TAG_COMPOUND);
		for (int index = 0; index < list.size() && result.size() < maximum; index++) {
			CompoundTag entry = list.getCompound(index);
			if (entry.hasUUID("Id"))
				result.add(entry.getUUID("Id"));
		}
		return List.copyOf(result);
	}

	private static int sanitizeClass(int classId) {
		return classId == StoryModeIntroManager.ASSASSIN_CLASS_ID
				|| classId == StoryModeIntroManager.FIGHTER_CLASS_ID ? classId : 0;
	}

	private static String normalizeDungeonClearReceipt(String receipt) {
		if (receipt == null)
			return "";
		String normalized = receipt.trim();
		return normalized.length() <= 256 ? normalized
				: normalized.substring(0, 256);
	}

	private static boolean equalsUuid(@Nullable UUID first, @Nullable UUID second) {
		return first == null ? second == null : first.equals(second);
	}
}
