package net.solocraft.procedures;

import net.solocraft.entity.StatueOfGodEntity;
import net.solocraft.util.CombatRangeHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;

import java.util.Comparator;

public class StatueOfGodOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (!world.isClientSide()) {
			if (!(entity.getPersistentData().getString("state")).equals("aggresive")) {
				if (!world.getEntitiesOfClass(Player.class, AABB.ofSize(new Vec3(x, y, z), 32, 32, 32), e -> true).isEmpty()) {
					if (new Object() {
						public boolean checkGamemode(Entity _ent) {
							if (_ent instanceof ServerPlayer _serverPlayer) {
								return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
							}
							return false;
						}
					}.checkGamemode(((Entity) world.getEntitiesOfClass(Player.class, AABB.ofSize(new Vec3(x, y, z), 32, 32, 32), e -> true).stream().sorted(new Object() {
						Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
							return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
						}
					}.compareDistOf(x, y, z)).findFirst().orElse(null)))) {
						entity.getPersistentData().putString("state", "aggresive");
						if (entity instanceof StatueOfGodEntity _datEntSetS)
							_datEntSetS.getEntityData().set(StatueOfGodEntity.DATA_state, "aggresive");
					}
				}
			}
			if ((entity.getPersistentData().getString("state")).equals("aggresive")) {
				entity.getPersistentData().putDouble("IA", (entity.getPersistentData().getDouble("IA") + 1));
				if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
					if (entity instanceof Mob _mob && _mob.getTarget() != null) {
						LivingEntity target = _mob.getTarget();
						double deltaX = target.getX() - entity.getX();
						double deltaZ = target.getZ() - entity.getZ();
						float targetYaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX))) - 90.0F;
						entity.setYRot(targetYaw);
						entity.yRotO = targetYaw;
						if (entity instanceof LivingEntity _livingEntity) {
							_livingEntity.yBodyRot = targetYaw;
							_livingEntity.yHeadRot = targetYaw;
						}
					}
					if (entity instanceof Mob _entity)
						_entity.getNavigation().moveTo(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()),
								((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ()), 1);
					if (entity.getPersistentData().getDouble("IA") > 101) {
						entity.setDeltaMovement(new Vec3((((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX() - entity.getX()) * 0.01), (entity.getDeltaMovement().y()),
								(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ() - entity.getZ()) * 0.01)));
						if (CombatRangeHelper.withinSurfaceRange(entity,
								(entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null), 3.0D)) {
							(entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK), entity), 65);
						}
					}
				}
				if (!(!world.getEntitiesOfClass(Player.class, AABB.ofSize(new Vec3(x, y, z), 85, 85, 85), e -> true).isEmpty())) {
					entity.getPersistentData().putString("state", "throne");
					if (entity instanceof StatueOfGodEntity _datEntSetS)
						_datEntSetS.getEntityData().set(StatueOfGodEntity.DATA_state, "throne");
					entity.getPersistentData().putDouble("IA", 0);
					{
						Entity _ent = entity;
						_ent.teleportTo((entity instanceof StatueOfGodEntity _datEntI ? _datEntI.getEntityData().get(StatueOfGodEntity.DATA_default_x) : 0),
								(entity instanceof StatueOfGodEntity _datEntI ? _datEntI.getEntityData().get(StatueOfGodEntity.DATA_default_y) : 0),
								(entity instanceof StatueOfGodEntity _datEntI ? _datEntI.getEntityData().get(StatueOfGodEntity.DATA_default_z) : 0));
						if (_ent instanceof ServerPlayer _serverPlayer)
							_serverPlayer.connection.teleport((entity instanceof StatueOfGodEntity _datEntI ? _datEntI.getEntityData().get(StatueOfGodEntity.DATA_default_x) : 0),
									(entity instanceof StatueOfGodEntity _datEntI ? _datEntI.getEntityData().get(StatueOfGodEntity.DATA_default_y) : 0),
									(entity instanceof StatueOfGodEntity _datEntI ? _datEntI.getEntityData().get(StatueOfGodEntity.DATA_default_z) : 0), _ent.getYRot(), _ent.getXRot());
					}
					{
						Entity _ent = entity;
						_ent.setYRot(180);
						_ent.setXRot(0);
						_ent.setYBodyRot(_ent.getYRot());
						_ent.setYHeadRot(_ent.getYRot());
						_ent.yRotO = _ent.getYRot();
						_ent.xRotO = _ent.getXRot();
						if (_ent instanceof LivingEntity _entity) {
							_entity.yBodyRotO = _entity.getYRot();
							_entity.yHeadRotO = _entity.getYRot();
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 40) {
				if (entity instanceof StatueOfGodEntity) {
					((StatueOfGodEntity) entity).setAnimation("standing and smiling");
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 101) {
				((Mob) entity).setNoAi(false);
			}
			if ((entity.getPersistentData().getString("state")).equals("throne")) {
				((Mob) entity).setNoAi(true);
			}
		}
	}
}
