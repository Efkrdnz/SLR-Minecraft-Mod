package net.solocraft.dungeon;

public class ProceduralDungeonSettings {
	public final ProceduralDungeonRank rank;
	public final DungeonTheme theme;
	public final int complexity;
	public final int targetRooms;

	public ProceduralDungeonSettings(ProceduralDungeonRank rank, DungeonTheme theme, int complexity) {
		this.rank = rank;
		this.theme = theme;
		this.complexity = Math.max(1, Math.min(10, complexity));
		this.targetRooms = Math.max(5, Math.min(24, rank.defaultRooms + this.complexity));
	}
}
