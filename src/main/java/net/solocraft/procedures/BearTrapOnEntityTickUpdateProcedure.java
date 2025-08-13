package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.BearTrapEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import java.util.List;
import java.util.Comparator;

public class BearTrapOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double counter = 0;
		if (entity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) {
			if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == (null))) {
				if (entity instanceof BearTrapEntity _datEntSetI)
					_datEntSetI.getEntityData().set(BearTrapEntity.DATA_trigger_timer, (int) ((entity instanceof BearTrapEntity _datEntI ? _datEntI.getEntityData().get(BearTrapEntity.DATA_trigger_timer) : 0) + 1));
				if ((entity instanceof BearTrapEntity _datEntI ? _datEntI.getEntityData().get(BearTrapEntity.DATA_trigger_timer) : 0) > 19) {
					if (entity instanceof BearTrapEntity _datEntSetL)
						_datEntSetL.getEntityData().set(BearTrapEntity.DATA_trigger, true);
				}
				if (entity instanceof BearTrapEntity _datEntL7 && _datEntL7.getEntityData().get(BearTrapEntity.DATA_trigger)) {
					{
						final Vec3 _center = new Vec3(x, y, z);
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
						for (Entity entityiterator : _entfound) {
							if ((((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)
									.equals("")) {
								if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == entityiterator || entity == entityiterator) && entityiterator instanceof LivingEntity) {
									counter = 1;
								}
							} else {
								if ((((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
										.orElse(new SololevelingModVariables.PlayerVariables())).party)
										.equals((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)) {
									if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == entityiterator || entity == entityiterator) && entityiterator instanceof LivingEntity) {
										counter = 1;
									}
								}
							}
						}
					}
					if (counter > 0) {
						if (world instanceof Level _level && !_level.isClientSide()) {
							_level.explode((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null),
									new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:ranger"))),
											(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null)),
									null, x, y, z, 3, false, Level.ExplosionInteraction.NONE);
						}
						if (!entity.level().isClientSide())
							entity.discard();
					}
				}
			}
		}
	}
}
