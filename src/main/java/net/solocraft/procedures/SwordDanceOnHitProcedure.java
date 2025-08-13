package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class SwordDanceOnHitProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingAttackEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity, event.getSource().getEntity());
		}
	}

	public static void execute(Entity entity, Entity sourceentity) {
		execute(null, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		boolean CanTeleport = false;
		double rand = 0;
		double ZTeleport = 0;
		double YTeleport = 0;
		double XTeleport = 0;
		if (sourceentity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.SWORD_DANCE.get())) {
			if (!(sourceentity instanceof LivingEntity _livEnt1 && _livEnt1.hasEffect(SololevelingModMobEffects.DANCE_COOLDOWN.get()))) {
				rand = Mth.nextInt(RandomSource.create(), 1, 2);
				if (rand == 1) {
					{
						Entity _ent = sourceentity;
						_ent.teleportTo((entity.getX() + (-2) * entity.getLookAngle().x), (entity.getY() + 0.8 + (-1.3) * entity.getLookAngle().y), (entity.getZ() + (-2) * entity.getLookAngle().z));
						if (_ent instanceof ServerPlayer _serverPlayer)
							_serverPlayer.connection.teleport((entity.getX() + (-2) * entity.getLookAngle().x), (entity.getY() + 0.8 + (-1.3) * entity.getLookAngle().y), (entity.getZ() + (-2) * entity.getLookAngle().z), _ent.getYRot(),
									_ent.getXRot());
					}
					sourceentity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3((entity.getX()), (entity.getY() + 1.6), (entity.getZ())));
					if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.DANCE_COOLDOWN.get(), 15, 1, false, false));
				} else if (rand == 2) {
					{
						Entity _ent = sourceentity;
						_ent.teleportTo((entity.getX()), (entity.getY() + 2), (entity.getZ()));
						if (_ent instanceof ServerPlayer _serverPlayer)
							_serverPlayer.connection.teleport((entity.getX()), (entity.getY() + 2), (entity.getZ()), _ent.getYRot(), _ent.getXRot());
					}
					sourceentity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3((entity.getX()), (entity.getY() + 1.6), (entity.getZ())));
					if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.NO_FALL_DAMAGE.get(), 30, 1, false, false));
					if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 1, false, false));
					if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.DANCE_COOLDOWN.get(), 15, 1, false, false));
				}
			}
		}
	}
}
