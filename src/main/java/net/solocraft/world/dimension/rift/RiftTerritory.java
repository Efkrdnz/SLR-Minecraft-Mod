package net.solocraft.world.dimension.rift;

/** The eight outward progression routes in the Dimensional Rift. */
public enum RiftTerritory {
	DESTRUCTION("destruction", "Destruction", 0.0D),
	FROST("frost", "Frost", 45.0D),
	FANGS("fangs", "Fangs", 90.0D),
	PLAGUES("plagues", "Plagues", 135.0D),
	IRON_BODY("iron_body", "Iron Body", 180.0D),
	WHITE_FLAMES("white_flames", "White Flames", 225.0D),
	TRANSFIGURATION("transfiguration", "Transfiguration", 270.0D),
	BEGINNING("beginning", "Beginning", 315.0D);

	private final String id;
	private final String displayName;
	private final double centerAngleDegrees;

	RiftTerritory(String id, String displayName, double centerAngleDegrees) {
		this.id = id;
		this.displayName = displayName;
		this.centerAngleDegrees = centerAngleDegrees;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public double centerAngleDegrees() {
		return centerAngleDegrees;
	}

	public double centerAngleRadians() {
		return Math.toRadians(centerAngleDegrees);
	}

	public static RiftTerritory byIndex(int index) {
		return values()[Math.floorMod(index, values().length)];
	}

	public static RiftTerritory fromName(String name) {
		if (name == null)
			return null;
		String normalized = name.strip().toLowerCase().replace('-', '_').replace(' ', '_');
		for (RiftTerritory territory : values()) {
			if (territory.id.equals(normalized) || territory.name().equalsIgnoreCase(normalized))
				return territory;
		}
		return null;
	}
}
