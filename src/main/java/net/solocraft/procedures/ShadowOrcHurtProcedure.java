package net.solocraft.procedures;

import net.solocraft.entity.OrcEntity;
import net.solocraft.SololevelingMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.Registries;

import java.util.List;
import java.util.Comparator;

public class ShadowOrcHurtProcedure {
	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double rand = 0;
		rand = Math.random();
		if (!(sourceentity instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
			if (rand >= 0.81) {
				if (entity instanceof OrcEntity) {
					((OrcEntity) entity).setAnimation("smash");
				}
				SololevelingMod.queueServerWork(25, () -> {
					{
						final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(20 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
						for (Entity entityiterator : _entfound) {
							if (!(entity == entityiterator || (entityiterator instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == (entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null)
									|| (entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == entityiterator)) {
								if (entity.isAlive()) {
									entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK_NO_AGGRO), entity), 10);
									entityiterator.setDeltaMovement(new Vec3(0, 1, 0));
								}
							}
						}
					}
				});
			}
		}
	}
}
