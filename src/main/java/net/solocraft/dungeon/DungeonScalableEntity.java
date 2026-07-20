package net.solocraft.dungeon;

/** Implemented by dungeon mobs whose model and hitbox can scale together. */
public interface DungeonScalableEntity {
	float getDungeonScale();

	void setDungeonScale(float scale);
}
