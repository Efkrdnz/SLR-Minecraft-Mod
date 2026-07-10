package net.solocraft.procedures;

import net.solocraft.init.SololevelingModItems;
import net.solocraft.entity.KangTaeshikEntity;
import net.solocraft.SololevelingMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.solocraft.util.CooldownManager;

public class KangTaeshikOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double Chain = 0;
		double ChainWait = 0;
		double motionZ = 0;
		double deltaZ = 0;
		double deltaX = 0;
		double motionY = 0;
		double Yspeed = 0;
		double deltaY = 0;
		double motionX = 0;
		double speed = 0;
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			if (entity instanceof Mob _entity)
				_entity.getNavigation().moveTo(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()),
						((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ()), 1);
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + 1.6),
					((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
			if (entity instanceof KangTaeshikEntity _datEntSetI)
				_datEntSetI.getEntityData().set(KangTaeshikEntity.DATA_IA, (int) ((entity instanceof KangTaeshikEntity _datEntI ? _datEntI.getEntityData().get(KangTaeshikEntity.DATA_IA) : 0) + 1));
		} else {
			if (entity instanceof KangTaeshikEntity _datEntSetI)
				_datEntSetI.getEntityData().set(KangTaeshikEntity.DATA_IA, 0);
		}
		if ((entity instanceof KangTaeshikEntity _datEntI ? _datEntI.getEntityData().get(KangTaeshikEntity.DATA_IA) : 0) == 40) {
			if (entity instanceof LivingEntity _entity) {
				ItemStack _setstack = new ItemStack(Blocks.AIR);
				_setstack.setCount(1);
				_entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
				if (_entity instanceof Player _player)
					_player.getInventory().setChanged();
			}
			if (entity instanceof LivingEntity _entity) {
				ItemStack _setstack = new ItemStack(Blocks.AIR);
				_setstack.setCount(1);
				_entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
				if (_entity instanceof Player _player)
					_player.getInventory().setChanged();
			}
			{
				Entity _entity = entity;
				if (_entity instanceof Player _player) {
					_player.getInventory().armor.set(3, new ItemStack(Blocks.AIR));
					_player.getInventory().setChanged();
				} else if (_entity instanceof LivingEntity _living) {
					_living.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Blocks.AIR));
				}
			}
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 1, false, false));
			CooldownManager.set(entity, "Stealth", 80);
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, false));
			entity.setSprinting(true);
			SololevelingMod.queueServerWork(80, () -> {
				entity.setSprinting(false);
				if (entity instanceof LivingEntity _entity) {
					ItemStack _setstack = new ItemStack(SololevelingModItems.DAGGER_DUOLITY_A.get());
					_setstack.setCount(1);
					_entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
					if (_entity instanceof Player _player)
						_player.getInventory().setChanged();
				}
				if (entity instanceof LivingEntity _entity) {
					ItemStack _setstack = new ItemStack(SololevelingModItems.DAGGER_HEAT_A.get());
					_setstack.setCount(1);
					_entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
					if (_entity instanceof Player _player)
						_player.getInventory().setChanged();
				}
				{
					Entity _entity = entity;
					if (_entity instanceof Player _player) {
						_player.getInventory().armor.set(3, new ItemStack(SololevelingModItems.KANG_HAIR_HELMET.get()));
						_player.getInventory().setChanged();
					} else if (_entity instanceof LivingEntity _living) {
						_living.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SololevelingModItems.KANG_HAIR_HELMET.get()));
					}
				}
			});
		}
		if (entity.getPersistentData().getDouble("IA") % 40 == 0 && entity.getPersistentData().getDouble("IA") != 0) {
			deltaX = -Math.sin((entity.getYRot() / 180) * (float) Math.PI);
			deltaY = -Math.sin((entity.getXRot() / 180) * (float) Math.PI);
			deltaZ = Math.cos((entity.getYRot() / 180) * (float) Math.PI);
			speed = 3;
			motionX = deltaX * speed;
			motionY = deltaY * speed;
			motionZ = deltaZ * speed;
			entity.setDeltaMovement(entity.getDeltaMovement().add(motionX, motionY, motionZ));
		}
		if ((entity instanceof KangTaeshikEntity _datEntI ? _datEntI.getEntityData().get(KangTaeshikEntity.DATA_IA) : 0) >= 330) {
			if (entity instanceof KangTaeshikEntity _datEntSetI)
				_datEntSetI.getEntityData().set(KangTaeshikEntity.DATA_IA, 0);
		}
	}
}
