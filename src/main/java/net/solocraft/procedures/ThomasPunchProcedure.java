package net.solocraft.procedures;

import net.solocraft.entity.ThomasAndreEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class ThomasPunchProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double rand = 0;
		if (entity.getPersistentData().getDouble("IA") == 20) {
			if ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 16, 99, false, false));
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 16, 99, false, false));
		}
		if (entity.getPersistentData().getDouble("IA") == 30) {
			if (entity instanceof ThomasAndreEntity) {
				((ThomasAndreEntity) entity).setAnimation("punches");
			}
			{
				Entity _ent = entity;
				_ent.teleportTo(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX() + (-1) * (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getLookAngle().x),
						((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + 0.2),
						((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ() + (-1) * (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getLookAngle().z));
				if (_ent instanceof ServerPlayer _serverPlayer)
					_serverPlayer.connection.teleport(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX() + (-1) * (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getLookAngle().x),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + 0.2),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ() + (-1) * (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getLookAngle().z), _ent.getYRot(), _ent.getXRot());
			}
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + 1.6),
					((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
		}
		if (entity.getPersistentData().getDouble("IA") == 22) {
			if (Math.sqrt(Math.pow(entity.getX() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX(), 2) + Math.pow(entity.getY() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY(), 2)
					+ Math.pow(entity.getZ() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ(), 2)) <= 2) {
				(entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK), entity), 20);
			}
		}
		if (entity.getPersistentData().getDouble("IA") == 34) {
			if (Math.sqrt(Math.pow(entity.getX() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX(), 2) + Math.pow(entity.getY() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY(), 2)
					+ Math.pow(entity.getZ() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ(), 2)) <= 2) {
				(entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK), entity), 20);
				(entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).setDeltaMovement(new Vec3((entity.getLookAngle().x * 2), (entity.getLookAngle().y * 1.5), (entity.getLookAngle().z * 2)));
			}
		}
		if (entity.getPersistentData().getDouble("IA") == 50) {
			AndreStateChangerProcedure.execute(entity);
		}
	}
}
