package net.solocraft.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Durable authority for Job Change attempts.
 *
 * <p>The player and encounter entities also carry the attempt UUID, but this
 * registry decides whether that UUID is still valid. Failed attempts remain
 * here until every participant has acknowledged the failure, so an offline
 * party member cannot reconnect and resume an arena that was already reset.</p>
 */
public final class JobChangeAttemptSavedData extends SavedData {
	private static final String DATA_NAME = "sololeveling_job_change_attempts";
	private static final SavedData.Factory<JobChangeAttemptSavedData> FACTORY =
			new SavedData.Factory<>(JobChangeAttemptSavedData::new, JobChangeAttemptSavedData::load);
	private static final int MAX_ATTEMPTS = 1_024;
	private static final int MAX_PARTICIPANTS = 64;
	private static final int MAX_CREDITED_KILLS = 4_096;

	private final Map<UUID, Attempt> attempts = new LinkedHashMap<>();

	public record AttemptView(UUID owner, Set<UUID> participants,
			boolean active, long retryAfterGameTime) {
		public AttemptView {
			participants = Set.copyOf(participants);
		}
	}

	public static JobChangeAttemptSavedData get(MinecraftServer server) {
		if (server == null)
			throw new IllegalArgumentException("A server is required.");
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	public void start(UUID attemptId, UUID owner, long gameTime) {
		if (attemptId == null || owner == null)
			return;
		if (attempts.size() >= MAX_ATTEMPTS)
			pruneOldest();
		Attempt attempt = new Attempt(owner);
		attempt.participants.add(owner);
		attempt.updatedGameTime = Math.max(0L, gameTime);
		attempts.put(attemptId, attempt);
		setDirty();
	}

	public boolean isActive(UUID attemptId) {
		Attempt attempt = attempts.get(attemptId);
		return attempt != null && attempt.active;
	}

	public AttemptView view(UUID attemptId) {
		Attempt attempt = attempts.get(attemptId);
		return attempt == null ? null : attempt.view();
	}

	public void addParticipant(UUID attemptId, UUID playerId, long gameTime) {
		Attempt attempt = attempts.get(attemptId);
		if (attempt == null || !attempt.active || playerId == null
				|| attempt.participants.size() >= MAX_PARTICIPANTS)
			return;
		boolean changed = attempt.participants.add(playerId);
		attempt.updatedGameTime = Math.max(attempt.updatedGameTime,
				Math.max(0L, gameTime));
		if (changed)
			setDirty();
	}

	/**
	 * Atomically credits one defeated quest enemy to an active attempt.
	 *
	 * <p>The entity also carries a lightweight compatibility marker, but the
	 * attempt ledger is the authority that survives callback ordering and save
	 * reloads. A generated death callback and Forge's LivingDeathEvent therefore
	 * cannot count the same knight twice.</p>
	 */
	public boolean creditAdvancementKill(UUID attemptId, UUID defeatedId,
			long gameTime) {
		Attempt attempt = attempts.get(attemptId);
		if (attempt == null || !attempt.active || defeatedId == null
				|| attempt.creditedKills.size() >= MAX_CREDITED_KILLS
				|| !attempt.creditedKills.add(defeatedId))
			return false;
		attempt.updatedGameTime = Math.max(attempt.updatedGameTime,
				Math.max(0L, gameTime));
		setDirty();
		return true;
	}

	public Set<UUID> invalidate(UUID attemptId, long retryAfterGameTime,
			long gameTime) {
		Attempt attempt = attempts.get(attemptId);
		if (attempt == null)
			return Set.of();
		attempt.active = false;
		attempt.retryAfterGameTime = Math.max(0L, retryAfterGameTime);
		attempt.updatedGameTime = Math.max(attempt.updatedGameTime,
				Math.max(0L, gameTime));
		setDirty();
		return Set.copyOf(attempt.participants);
	}

	public void complete(UUID attemptId) {
		if (attemptId != null && attempts.remove(attemptId) != null)
			setDirty();
	}

	/**
	 * Removes one player from a failed attempt. Active attempts are retained
	 * until explicit completion or invalidation.
	 */
	public void acknowledgeFailure(UUID attemptId, UUID playerId) {
		Attempt attempt = attempts.get(attemptId);
		if (attempt == null || attempt.active || playerId == null)
			return;
		if (!attempt.participants.remove(playerId))
			return;
		if (attempt.participants.isEmpty())
			attempts.remove(attemptId);
		setDirty();
	}

	public void removeParticipant(UUID attemptId, UUID playerId) {
		Attempt attempt = attempts.get(attemptId);
		if (attempt == null || playerId == null
				|| !attempt.participants.remove(playerId))
			return;
		if (attempt.participants.isEmpty())
			attempts.remove(attemptId);
		setDirty();
	}

	@Override
	@Nonnull
	public CompoundTag save(@Nonnull CompoundTag root, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (Map.Entry<UUID, Attempt> entry : attempts.entrySet()) {
			CompoundTag tag = new CompoundTag();
			tag.putUUID("AttemptId", entry.getKey());
			tag.putUUID("Owner", entry.getValue().owner);
			tag.putBoolean("Active", entry.getValue().active);
			tag.putLong("RetryAfter", entry.getValue().retryAfterGameTime);
			tag.putLong("Updated", entry.getValue().updatedGameTime);
			ListTag participants = new ListTag();
			for (UUID playerId : entry.getValue().participants) {
				CompoundTag participant = new CompoundTag();
				participant.putUUID("Id", playerId);
				participants.add(participant);
			}
			tag.put("Participants", participants);
			ListTag creditedKills = new ListTag();
			for (UUID defeatedId : entry.getValue().creditedKills) {
				CompoundTag credited = new CompoundTag();
				credited.putUUID("Id", defeatedId);
				creditedKills.add(credited);
			}
			tag.put("CreditedKills", creditedKills);
			list.add(tag);
		}
		root.put("Attempts", list);
		return root;
	}

	private static JobChangeAttemptSavedData load(CompoundTag root, HolderLookup.Provider registries) {
		JobChangeAttemptSavedData data = new JobChangeAttemptSavedData();
		ListTag list = root.getList("Attempts", Tag.TAG_COMPOUND);
		int limit = Math.min(MAX_ATTEMPTS, list.size());
		for (int index = 0; index < limit; index++) {
			CompoundTag tag = list.getCompound(index);
			if (!tag.hasUUID("AttemptId") || !tag.hasUUID("Owner"))
				continue;
			Attempt attempt = new Attempt(tag.getUUID("Owner"));
			attempt.active = tag.getBoolean("Active");
			attempt.retryAfterGameTime = Math.max(0L,
					tag.getLong("RetryAfter"));
			attempt.updatedGameTime = Math.max(0L, tag.getLong("Updated"));
			ListTag participants = tag.getList("Participants",
					Tag.TAG_COMPOUND);
			int participantLimit = Math.min(MAX_PARTICIPANTS,
					participants.size());
			for (int participantIndex = 0;
					participantIndex < participantLimit; participantIndex++) {
				CompoundTag participant = participants.getCompound(
						participantIndex);
				if (participant.hasUUID("Id"))
					attempt.participants.add(participant.getUUID("Id"));
			}
			if (attempt.participants.isEmpty())
				attempt.participants.add(attempt.owner);
			ListTag creditedKills = tag.getList("CreditedKills",
					Tag.TAG_COMPOUND);
			int creditedLimit = Math.min(MAX_CREDITED_KILLS,
					creditedKills.size());
			for (int creditedIndex = 0; creditedIndex < creditedLimit;
					creditedIndex++) {
				CompoundTag credited = creditedKills.getCompound(creditedIndex);
				if (credited.hasUUID("Id"))
					attempt.creditedKills.add(credited.getUUID("Id"));
			}
			data.attempts.put(tag.getUUID("AttemptId"), attempt);
		}
		return data;
	}

	private void pruneOldest() {
		UUID oldest = null;
		long oldestTime = Long.MAX_VALUE;
		for (Map.Entry<UUID, Attempt> entry : attempts.entrySet()) {
			if (entry.getValue().updatedGameTime < oldestTime) {
				oldest = entry.getKey();
				oldestTime = entry.getValue().updatedGameTime;
			}
		}
		if (oldest != null)
			attempts.remove(oldest);
	}

	private static final class Attempt {
		private final UUID owner;
		private final LinkedHashSet<UUID> participants =
				new LinkedHashSet<>();
		private final LinkedHashSet<UUID> creditedKills =
				new LinkedHashSet<>();
		private boolean active = true;
		private long retryAfterGameTime;
		private long updatedGameTime;

		private Attempt(UUID owner) {
			this.owner = owner;
		}

		private AttemptView view() {
			return new AttemptView(owner, participants, active,
					retryAfterGameTime);
		}
	}
}
