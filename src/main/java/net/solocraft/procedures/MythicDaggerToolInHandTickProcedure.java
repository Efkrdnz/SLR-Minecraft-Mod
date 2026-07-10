package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.init.SololevelingModItems;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;

import javax.annotation.Nullable;
import net.solocraft.util.CooldownManager;

@Mod.EventBusSubscriber
public class MythicDaggerToolInHandTickProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingAttackEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity, event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		execute(null, world, x, y, z, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.SWORD_ENHANCE.get()) && !CooldownManager.isOnCooldown(entity, "counter")
				&& (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.MYTHIC_DAGGER.get()) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.DRAGON_BREATH, x, y, z, 5, 2, 2, 2, 0.2);
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.DRAGON_BREATH, (sourceentity.getX() + (-3) * sourceentity.getLookAngle().x), (sourceentity.getY() + 1.6 + (-1.3) * sourceentity.getLookAngle().y),
						(sourceentity.getZ() + (-3) * sourceentity.getLookAngle().z), 5, 2, 2, 2, 0.2);
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 1));
			CooldownManager.set(entity, "counter", 260);
			if (event != null && event.isCancelable()) {
				event.setCanceled(true);
			}
			{
				Entity _ent = entity;
				_ent.teleportTo((sourceentity.getX() + (-3) * sourceentity.getLookAngle().x), (sourceentity.getY() + 1.6 + (-1.3) * sourceentity.getLookAngle().y), (sourceentity.getZ() + (-3) * sourceentity.getLookAngle().z));
				if (_ent instanceof ServerPlayer _serverPlayer)
					_serverPlayer.connection.teleport((sourceentity.getX() + (-3) * sourceentity.getLookAngle().x), (sourceentity.getY() + 1.6 + (-1.3) * sourceentity.getLookAngle().y), (sourceentity.getZ() + (-3) * sourceentity.getLookAngle().z),
							_ent.getYRot(), _ent.getXRot());
			}
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3((sourceentity.getX()), (sourceentity.getY() + 1.6), (sourceentity.getZ())));
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.enderman.teleport")), SoundSource.NEUTRAL, 2, 1);
				} else {
					_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.enderman.teleport")), SoundSource.NEUTRAL, 2, 1, false);
				}
			}
		}
	}
}
