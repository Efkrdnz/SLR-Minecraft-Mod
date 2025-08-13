package net.solocraft.procedures;

import net.solocraft.init.SololevelingModEntities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

public class FlameVortexPProjectileHitsBlockProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world instanceof Level _level && !_level.isClientSide())
			_level.explode(null, x, y, z, 1, Level.ExplosionInteraction.NONE);
		if (world instanceof ServerLevel _level) {
			Entity _entityToSpawn = SololevelingModEntities.FLAME_VORTEX.get().create(_level);
			_entityToSpawn.moveTo(x, (y + 1.6), z, world.getRandom().nextFloat() * 360.0F, 0.0F);
			if (_entityToSpawn instanceof Mob _mobToSpawn) {
				_mobToSpawn.finalizeSpawn(_level, _level.getCurrentDifficultyAt(_entityToSpawn.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
			}
			if ((_entityToSpawn) instanceof TamableAnimal _toTame && entity instanceof Player _owner)
				_toTame.tame(_owner);
			_level.addFreshEntity(_entityToSpawn);
		}
	}
}
