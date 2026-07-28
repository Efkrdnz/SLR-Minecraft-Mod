package net.solocraft.dungeon;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

public class ProceduralDungeonResult {
	public final BlockPos startPos;
	@Nullable
	public final BlockPos returnPortalPos;
	public final BlockPos bossRoomCenter;
	public final int rooms;
	public final int monsters;

	public ProceduralDungeonResult(BlockPos startPos, @Nullable BlockPos returnPortalPos,
			BlockPos bossRoomCenter, int rooms, int monsters) {
		this.startPos = startPos;
		this.returnPortalPos = returnPortalPos;
		this.bossRoomCenter = bossRoomCenter;
		this.rooms = rooms;
		this.monsters = monsters;
	}
}
