package net.solocraft.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

public class CrossAttackProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double particleNum = 0;
		double vX = 0;
		double vY = 0;
		double vZ = 0;
		double ypos4 = 0;
		double ypos3 = 0;
		double i = 0;
		double x_pos = 0;
		double ypos2 = 0;
		double z_pos = 0;
		double speed = 0;
		double arcAngle = 0;
		double radAngle = 0;
		double zpos4 = 0;
		double zpos3 = 0;
		double zpos2 = 0;
		double radYaw = 0;
		double radPitch = 0;
		double xpos4 = 0;
		double xpos3 = 0;
		double angle = 0;
		double xpos2 = 0;
		double y_pos = 0;
		double radius = 0;
		double height = 0;
		TestParticlesRightclickedProcedure.execute(world, x, y, z, entity);
	}
}
