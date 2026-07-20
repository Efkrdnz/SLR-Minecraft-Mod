package net.solocraft.dungeon.runtime.layout;

/** High-level graph shape used by the canonical dungeon layout planner. */
public enum DungeonLayoutTopology {
	/** One critical start-to-boss chain. Unused optional sockets remain sealed. */
	LINEAR,
	/** A reserved start-to-boss chain followed by optional side branches. */
	BRANCHING,
	/** Exact editor-authored placements and socket-to-socket connections. */
	FIXED
}
