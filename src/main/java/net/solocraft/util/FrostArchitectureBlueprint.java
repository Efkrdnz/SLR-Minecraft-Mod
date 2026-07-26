package net.solocraft.util;

import java.util.Arrays;

public enum FrostArchitectureBlueprint {
	CRYSTAL_DOME(0, "gui.sololeveling.frost_architecture.crystal_dome"),
	FROZEN_BASTION(1, "gui.sololeveling.frost_architecture.frozen_bastion"),
	GLACIER_WALL(2, "gui.sololeveling.frost_architecture.glacier_wall"),
	PALE_BRIDGE(3, "gui.sololeveling.frost_architecture.pale_bridge"),
	SPIRE_CAGE(4, "gui.sololeveling.frost_architecture.spire_cage"),
	RAMPART_RING(5, "gui.sololeveling.frost_architecture.rampart_ring");

	private final int id;
	private final String translationKey;

	FrostArchitectureBlueprint(int id, String translationKey) {
		this.id = id;
		this.translationKey = translationKey;
	}

	public int id() {
		return id;
	}

	public String translationKey() {
		return translationKey;
	}

	public static FrostArchitectureBlueprint byId(int id) {
		return Arrays.stream(values()).filter(value -> value.id == id).findFirst().orElse(CRYSTAL_DOME);
	}
}
