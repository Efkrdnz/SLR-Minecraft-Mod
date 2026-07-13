package net.solocraft.procedures;

import net.solocraft.init.SololevelingModItems;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.RangerProjectileEntity;
import net.solocraft.entity.HunterEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class RandomHunterRangerTickProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		double Rank = 0;
		double rand = 0;
		double dmg_modifier = 0;
		Entity ent = null;
		if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("S")) {
			dmg_modifier = 18;
		} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("A")) {
			dmg_modifier = 14;
		} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("B")) {
			dmg_modifier = 10;
		} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("C")) {
			dmg_modifier = 6;
		} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("D")) {
			dmg_modifier = 4;
		}
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			HunterAIHelper.rangerCombatTick(entity);
			entity.lookAt(EntityAnchorArgument.Anchor.EYES,
					new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getBbHeight()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
			if ((entity instanceof HunterEntity _datEntI ? _datEntI.getEntityData().get(HunterEntity.DATA_IA) : 0) <= 25) {
				if (entity instanceof HunterEntity _datEntSetI)
					_datEntSetI.getEntityData().set(HunterEntity.DATA_IA, (int) ((entity instanceof HunterEntity _datEntI ? _datEntI.getEntityData().get(HunterEntity.DATA_IA) : 0) + 1));
				if ((entity instanceof HunterEntity _datEntI ? _datEntI.getEntityData().get(HunterEntity.DATA_IA) : 0) == 20) {
					if (((entity instanceof LivingEntity _entity) ? _entity.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof BowItem
							|| (entity instanceof LivingEntity _entity ? _entity.getOffhandItem() : ItemStack.EMPTY).getItem() instanceof BowItem
							|| ((entity instanceof LivingEntity _entity) ? _entity.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof CrossbowItem
							|| (entity instanceof LivingEntity _entity ? _entity.getOffhandItem() : ItemStack.EMPTY).getItem() instanceof CrossbowItem) {
						entity.setDeltaMovement(new Vec3((Mth.nextDouble(RandomSource.create(), -0.25, 0.25) * 0.8), 0.12, (Mth.nextDouble(RandomSource.create(), -0.25, 0.25) * 0.8)));
						entity.hasImpulse = true;
						{
							Entity _shootFrom = entity;
							Level projectileLevel = _shootFrom.level();
							if (!projectileLevel.isClientSide()) {
								Projectile _entityToSpawn = new Object() {
									public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
										AbstractArrow entityToSpawn = new RangerProjectileEntity(SololevelingModEntities.RANGER_PROJECTILE.get(), level);
										entityToSpawn.setOwner(shooter);
										entityToSpawn.setBaseDamage(damage);
										entityToSpawn.setKnockback(knockback);
										entityToSpawn.setSilent(true);
										return entityToSpawn;
									}
								}.getArrow(projectileLevel, entity, (float) (dmg_modifier / 3), 1);
								_entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
								_entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 3, 0);
								projectileLevel.addFreshEntity(_entityToSpawn);
							}
						}
					} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.SPIRIT_BOW.get()) {
						entity.setDeltaMovement(new Vec3((Mth.nextDouble(RandomSource.create(), -0.25, 0.25) * 0.8), 0.12, (Mth.nextDouble(RandomSource.create(), -0.25, 0.25) * 0.8)));
						entity.hasImpulse = true;
						{
							Entity _shootFrom = entity;
							Level projectileLevel = _shootFrom.level();
							if (!projectileLevel.isClientSide()) {
								Projectile _entityToSpawn = new Object() {
									public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
										AbstractArrow entityToSpawn = new RangerProjectileEntity(SololevelingModEntities.RANGER_PROJECTILE.get(), level);
										entityToSpawn.setOwner(shooter);
										entityToSpawn.setBaseDamage(damage);
										entityToSpawn.setKnockback(knockback);
										entityToSpawn.setSilent(true);
										return entityToSpawn;
									}
								}.getArrow(projectileLevel, entity, (float) (dmg_modifier / 3), 1);
								_entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
								_entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 3, 0);
								projectileLevel.addFreshEntity(_entityToSpawn);
							}
						}
					}
				}
			} else {
				if (entity instanceof HunterEntity _datEntSetI)
					_datEntSetI.getEntityData().set(HunterEntity.DATA_IA, 0);
			}
		}
	}
}
