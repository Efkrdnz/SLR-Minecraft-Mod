package net.solocraft.dungeon;

import java.util.Optional;

public enum ProceduralDungeonRank {
	E(1, 8, 11, 13, 17, 5, 4, 1),
	D(2, 9, 12, 15, 19, 5, 5, 2),
	C(3, 10, 13, 17, 21, 5, 6, 2),
	B(4, 12, 15, 19, 23, 5, 7, 3),
	A(5, 14, 17, 23, 27, 7, 9, 3),
	S(6, 16, 19, 27, 31, 7, 11, 4);

	public final int numericRank;
	public final int defaultRooms;
	public final int minRoomSize;
	public final int maxRoomSize;
	public final int bossRoomSize;
	public final int corridorWidth;
	public final int interiorHeight;
	public final int packSize;

	ProceduralDungeonRank(int numericRank, int defaultRooms, int minRoomSize, int maxRoomSize, int bossRoomSize, int corridorWidth, int interiorHeight, int packSize) {
		this.numericRank = numericRank;
		this.defaultRooms = defaultRooms;
		this.minRoomSize = minRoomSize;
		this.maxRoomSize = maxRoomSize;
		this.bossRoomSize = bossRoomSize;
		this.corridorWidth = corridorWidth;
		this.interiorHeight = interiorHeight;
		this.packSize = packSize;
	}

	public static ProceduralDungeonRank fromString(String value) {
		return tryParse(value).orElse(E);
	}

	/** Strict parser for authoring/datapack validation. Runtime legacy data still uses {@link #fromString}. */
	public static Optional<ProceduralDungeonRank> tryParse(String value) {
		if (value != null) {
			for (ProceduralDungeonRank rank : values()) {
				if (rank.name().equalsIgnoreCase(value))
					return Optional.of(rank);
			}
		}
		return Optional.empty();
	}
}
