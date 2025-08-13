package net.solocraft.procedures;

import net.solocraft.entity.KargalganEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Comparator;

public class KargalganOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double dirX = 0;
		double dirZ = 0;
		if ((entity instanceof KargalganEntity _datEntI ? _datEntI.getEntityData().get(KargalganEntity.DATA_CooldownCurseMagic) : 0) > 0) {
			if (entity instanceof KargalganEntity _datEntSetI)
				_datEntSetI.getEntityData().set(KargalganEntity.DATA_CooldownCurseMagic, (int) ((entity instanceof KargalganEntity _datEntI ? _datEntI.getEntityData().get(KargalganEntity.DATA_CooldownCurseMagic) : 0) - 1));
		}
		if (!world.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(x, y, z), 7, 7, 7), e -> true).isEmpty()
				&& !(((Entity) world.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(x, y, z), 7, 7, 7), e -> true).stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
					}
				}.compareDistOf(x, y, z)).findFirst().orElse(null)) instanceof TamableAnimal _tamIsTamedBy && entity instanceof LivingEntity _livEnt ? _tamIsTamedBy.isOwnedBy(_livEnt) : false)
				&& !(((Entity) world.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(x, y, z), 7, 7, 7), e -> true).stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
					}
				}.compareDistOf(x, y, z)).findFirst().orElse(null)) == entity)) {
			if (entity instanceof KargalganEntity _datEntSetI)
				_datEntSetI.getEntityData().set(KargalganEntity.DATA_PushTimer, (int) ((entity instanceof KargalganEntity _datEntI ? _datEntI.getEntityData().get(KargalganEntity.DATA_PushTimer) : 0) + 1));
		} else {
			if (entity instanceof KargalganEntity _datEntSetI)
				_datEntSetI.getEntityData().set(KargalganEntity.DATA_PushTimer, 0);
		}
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			if (entity instanceof KargalganEntity _datEntSetI)
				_datEntSetI.getEntityData().set(KargalganEntity.DATA_AI, (int) ((entity instanceof KargalganEntity _datEntI ? _datEntI.getEntityData().get(KargalganEntity.DATA_AI) : 0) + 1));
			if ((entity instanceof KargalganEntity _datEntI ? _datEntI.getEntityData().get(KargalganEntity.DATA_AI) : 0) == 20) {
				KargalganHymOfDragonProcedure.execute(world, y, entity);
			}
			if ((entity instanceof KargalganEntity _datEntI ? _datEntI.getEntityData().get(KargalganEntity.DATA_AI) : 0) == 90) {
				if (entity instanceof KargalganEntity _datEntSetI)
					_datEntSetI.getEntityData().set(KargalganEntity.DATA_AI, 0);
			}
			if ((entity instanceof KargalganEntity _datEntI ? _datEntI.getEntityData().get(KargalganEntity.DATA_AI) : 0) == 130) {
				if (entity instanceof KargalganEntity _datEntSetI)
					_datEntSetI.getEntityData().set(KargalganEntity.DATA_AI, 0);
			}
			if ((entity instanceof KargalganEntity _datEntI ? _datEntI.getEntityData().get(KargalganEntity.DATA_CooldownCurseMagic) : 0) <= 0) {
				KargalganHymnOfAgonyProcedure.execute(world, entity);
				if (entity instanceof KargalganEntity _datEntSetI)
					_datEntSetI.getEntityData().set(KargalganEntity.DATA_CooldownCurseMagic, 200);
			}
			if ((entity instanceof KargalganEntity _datEntI ? _datEntI.getEntityData().get(KargalganEntity.DATA_Push) : 0) >= 3) {
				if (!world.isClientSide()) {
					if (entity instanceof KargalganEntity _datEntSetI)
						_datEntSetI.getEntityData().set(KargalganEntity.DATA_Push, 0);
					{
						final Vec3 _center = new Vec3(x, y, z);
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(25 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
						for (Entity entityiterator : _entfound) {
							if (!(entity == entityiterator)) {
								dirX = (entityiterator.getX() - entity.getX())
										* (5 / Math.sqrt(Math.pow(entityiterator.getX() - entity.getX(), 2) + Math.pow(entityiterator.getY() - entity.getY(), 2) + Math.pow(entityiterator.getZ() - entity.getZ(), 2)));
								dirZ = (entityiterator.getZ() - entity.getZ())
										* (5 / Math.sqrt(Math.pow(entityiterator.getX() - entity.getX(), 2) + Math.pow(entityiterator.getY() - entity.getY(), 2) + Math.pow(entityiterator.getZ() - entity.getZ(), 2)));
								entityiterator.setDeltaMovement(new Vec3(dirX, 1, dirZ));
							}
						}
					}
				}
			}
			if ((entity instanceof KargalganEntity _datEntI ? _datEntI.getEntityData().get(KargalganEntity.DATA_PushTimer) : 0) >= 60) {
				if (!world.isClientSide()) {
					if (entity instanceof KargalganEntity _datEntSetI)
						_datEntSetI.getEntityData().set(KargalganEntity.DATA_PushTimer, 0);
					{
						final Vec3 _center = new Vec3(x, y, z);
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(25 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
						for (Entity entityiterator : _entfound) {
							if (!(entity == entityiterator)) {
								dirX = (entityiterator.getX() - entity.getX())
										* (5 / Math.sqrt(Math.pow(entityiterator.getX() - entity.getX(), 2) + Math.pow(entityiterator.getY() - entity.getY(), 2) + Math.pow(entityiterator.getZ() - entity.getZ(), 2)));
								dirZ = (entityiterator.getZ() - entity.getZ())
										* (5 / Math.sqrt(Math.pow(entityiterator.getX() - entity.getX(), 2) + Math.pow(entityiterator.getY() - entity.getY(), 2) + Math.pow(entityiterator.getZ() - entity.getZ(), 2)));
								entityiterator.setDeltaMovement(new Vec3(dirX, 1, dirZ));
							}
						}
					}
				}
			}
		}
	}
}
