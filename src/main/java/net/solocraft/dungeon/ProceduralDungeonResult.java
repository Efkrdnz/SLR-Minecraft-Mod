package net.solocraft.dungeon;

import net.minecraft.core.BlockPos;

public class ProceduralDungeonResult {
	public final BlockPos startPos;
	public final BlockPos returnPortalPos;
	public final BlockPos bossRoomCenter;
	public final int rooms;
	public final int monsters;

	public ProceduralDungeonResult(BlockPos startPos, BlockPos returnPortalPos, BlockPos bossRoomCenter, int rooms, int monsters) {
		this.startPos = startPos;
		this.returnPortalPos = returnPortalPos;
		this.bossRoomCenter = bossRoomCenter;
		this.rooms = rooms;
		this.monsters = monsters;
	}
}
