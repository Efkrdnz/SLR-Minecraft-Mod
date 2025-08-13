package net.solocraft.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;

public class ThomasAndreOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			entity.getPersistentData().putDouble("IA", (entity.getPersistentData().getDouble("IA") + 1));
			entity.setSprinting(true);
		} else {
			entity.getPersistentData().putString("State", "Idle");
			entity.getPersistentData().putDouble("IA", 0);
			entity.setSprinting(false);
		}
		if ((entity.getPersistentData().getString("State")).equals("Idle")) {
			if (entity.getPersistentData().getDouble("IA") == 20) {
				AndreStateChangerProcedure.execute(entity);
			}
		}
		if ((entity.getPersistentData().getString("State")).equals("Punch")) {
			ThomasPunchProcedure.execute(world, entity);
		}
	}
}
