package net.solocraft.party;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/** One ordered party roster entry, including the last known profile details. */
final class PartyMember {
	private final UUID id;
	private String name;
	private int level;
	private String rank;

	PartyMember(UUID id, String name, int level, String rank) {
		this.id = id;
		this.name = cleanText(name, 32, "Unknown");
		this.level = Math.max(0, level);
		this.rank = cleanText(rank, 24, "Unranked");
	}

	UUID id() {
		return id;
	}

	String name() {
		return name;
	}

	int level() {
		return level;
	}

	String rank() {
		return rank;
	}

	boolean update(String nextName, int nextLevel, String nextRank) {
		String cleanName = cleanText(nextName, 32, "Unknown");
		String cleanRank = cleanText(nextRank, 24, "Unranked");
		int cleanLevel = Math.max(0, nextLevel);
		if (name.equals(cleanName) && level == cleanLevel && rank.equals(cleanRank))
			return false;
		name = cleanName;
		level = cleanLevel;
		rank = cleanRank;
		return true;
	}

	CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putUUID("Id", id);
		tag.putString("Name", name);
		tag.putInt("Level", level);
		tag.putString("Rank", rank);
		return tag;
	}

	static PartyMember load(CompoundTag tag) {
		if (tag == null || !tag.hasUUID("Id"))
			return null;
		return new PartyMember(tag.getUUID("Id"), tag.getString("Name"),
				tag.getInt("Level"), tag.getString("Rank"));
	}

	private static String cleanText(String value, int maxLength, String fallback) {
		if (value == null)
			return fallback;
		String clean = value.trim();
		if (clean.isEmpty())
			return fallback;
		return clean.substring(0, Math.min(maxLength, clean.length()));
	}
}
