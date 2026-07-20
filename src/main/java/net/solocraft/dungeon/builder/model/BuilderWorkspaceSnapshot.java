package net.solocraft.dungeon.builder.model;

import java.util.List;

/** Small immutable catalog snapshot safe to hand to GUI/network adapters. */
public record BuilderWorkspaceSnapshot(long revision, String activeProjectId,
		List<String> projectIds, List<BuilderMobPool> mobPools, List<DungeonDraft> dungeonDrafts) {
	public BuilderWorkspaceSnapshot {
		activeProjectId = activeProjectId == null ? "" : activeProjectId;
		projectIds = projectIds == null ? List.of() : List.copyOf(projectIds);
		mobPools = mobPools == null ? List.of() : List.copyOf(mobPools);
		dungeonDrafts = dungeonDrafts == null ? List.of() : List.copyOf(dungeonDrafts);
	}
}
