package net.solocraft.procedures;

import net.solocraft.entity.HunterEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class RandomHunterFighterTickProcedure {
	public static void execute(double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double Rank = 0;
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			HunterAIHelper.fighterCombatTick(entity);
			entity.lookAt(EntityAnchorArgument.Anchor.EYES,
					new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getBbHeight()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
			if ((entity instanceof HunterEntity _datEntI ? _datEntI.getEntityData().get(HunterEntity.DATA_IA) : 0) <= 200) {
				if (entity instanceof HunterEntity _datEntSetI)
					_datEntSetI.getEntityData().set(HunterEntity.DATA_IA, (int) ((entity instanceof HunterEntity _datEntI ? _datEntI.getEntityData().get(HunterEntity.DATA_IA) : 0) + 1));
				if ((entity instanceof HunterEntity _datEntI ? _datEntI.getEntityData().get(HunterEntity.DATA_IA) : 0) == 150) {
					{
						Entity _ent = entity;
						_ent.teleportTo(x, (y + 1), z);
						if (_ent instanceof ServerPlayer _serverPlayer)
							_serverPlayer.connection.teleport(x, (y + 1), z, _ent.getYRot(), _ent.getXRot());
					}
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 1, false, false));
				}
				if ((entity instanceof HunterEntity _datEntI ? _datEntI.getEntityData().get(HunterEntity.DATA_IA) : 0) == 157) {
					entity.setDeltaMovement(new Vec3((entity.getLookAngle().x), (entity.getLookAngle().y), (entity.getLookAngle().z)));
				}
			} else {
				if (entity instanceof HunterEntity _datEntSetI)
					_datEntSetI.getEntityData().set(HunterEntity.DATA_IA, 0);
			}
		}
	}
}
