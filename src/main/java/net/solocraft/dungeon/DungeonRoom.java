package net.solocraft.dungeon;

/** A room footprint in procedural-dungeon grid coordinates. */
public final class DungeonRoom {
	public enum Type {
		ENTRY,
		NORMAL,
		TREASURE,
		BOSS
	}

	public enum Shape {
		RECTANGLE,
		CHAMFERED,
		ROUND,
		CROSS
	}

	public final int gx;
	public final int gz;
	public final int width;
	public final int length;
	public final Type type;
	public final Shape shape;

	public DungeonRoom(int gx, int gz, int width, int length, Type type) {
		this(gx, gz, width, length, type, Shape.RECTANGLE);
	}

	public DungeonRoom(int gx, int gz, int width, int length, Type type, Shape shape) {
		this.gx = gx;
		this.gz = gz;
		this.width = width;
		this.length = length;
		this.type = type;
		this.shape = shape == null ? Shape.RECTANGLE : shape;
	}

	public DungeonRoom withType(Type newType) {
		return new DungeonRoom(gx, gz, width, length, newType, shape);
	}

	public int centerX() {
		return gx + width / 2;
	}

	public int centerZ() {
		return gz + length / 2;
	}

	public boolean containsGrid(int x, int z) {
		return containsLocal(x - gx, z - gz);
	}

	public boolean containsLocal(int x, int z) {
		if (x < 0 || z < 0 || x >= width || z >= length)
			return false;
		return switch (shape) {
			case RECTANGLE -> true;
			case CHAMFERED -> {
				int cut = Math.max(2, Math.min(4, Math.min(width, length) / 5));
				int edgeX = Math.min(x, width - 1 - x);
				int edgeZ = Math.min(z, length - 1 - z);
				yield edgeX + edgeZ >= cut - 1;
			}
			case ROUND -> {
				double radiusX = Math.max(1.0D, (width - 1) * 0.5D);
				double radiusZ = Math.max(1.0D, (length - 1) * 0.5D);
				double dx = (x - radiusX) / radiusX;
				double dz = (z - radiusZ) / radiusZ;
				yield dx * dx + dz * dz <= 1.04D;
			}
			case CROSS -> {
				int halfX = Math.max(2, width / 6);
				int halfZ = Math.max(2, length / 6);
				yield Math.abs(x - width / 2) <= halfX || Math.abs(z - length / 2) <= halfZ;
			}
		};
	}

	public boolean isEdgeLocal(int x, int z) {
		if (!containsLocal(x, z))
			return false;
		return !containsLocal(x + 1, z) || !containsLocal(x - 1, z)
				|| !containsLocal(x, z + 1) || !containsLocal(x, z - 1);
	}

	/** Returns true when the room bounds, including a wall border, overlap. */
	public boolean overlaps(DungeonRoom other, int margin) {
		int ax1 = gx - 1 - margin;
		int ax2 = gx + width + margin;
		int az1 = gz - 1 - margin;
		int az2 = gz + length + margin;
		int bx1 = other.gx - 1;
		int bx2 = other.gx + other.width;
		int bz1 = other.gz - 1;
		int bz2 = other.gz + other.length;
		return ax1 < bx2 && ax2 > bx1 && az1 < bz2 && az2 > bz1;
	}
}
