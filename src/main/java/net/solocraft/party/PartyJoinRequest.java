package net.solocraft.party;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/** A short-lived request to join one party. */
final class PartyJoinRequest {
	private final UUID playerId;
	private final String playerName;
	private final long expiresAtMillis;

	PartyJoinRequest(UUID playerId, String playerName, long expiresAtMillis) {
		this.playerId = playerId;
		String cleanName = playerName == null ? "" : playerName.trim();
		this.playerName = cleanName.isEmpty() ? "Unknown"
				: cleanName.substring(0, Math.min(32, cleanName.length()));
		this.expiresAtMillis = expiresAtMillis;
	}

	UUID playerId() {
		return playerId;
	}

	String playerName() {
		return playerName;
	}

	long expiresAtMillis() {
		return expiresAtMillis;
	}

	boolean expired(long now) {
		return expiresAtMillis <= now;
	}

	CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putUUID("Id", playerId);
		tag.putString("Name", playerName);
		tag.putLong("ExpiresAt", expiresAtMillis);
		return tag;
	}

	static PartyJoinRequest load(CompoundTag tag) {
		if (tag == null || !tag.hasUUID("Id"))
			return null;
		return new PartyJoinRequest(tag.getUUID("Id"), tag.getString("Name"),
				tag.getLong("ExpiresAt"));
	}
}
