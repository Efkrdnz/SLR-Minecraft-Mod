package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

@EventBusSubscriber
public class FortifiedTargetIsHurtProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingIncomingDamageEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity, event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		execute(null, world, x, y, z, entity, sourceentity);
	}

	private static void execute(@Nullable ICancellableEvent event, LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double scale_opp = 0;
		double scale_self = 0;
		if (entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.FORTIFY)) {
			if (sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("hunters"))) || sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("dm")))) {
				scale_opp = sourceentity.getPersistentData().getDouble("Level");
			} else if (sourceentity instanceof Player) {
				scale_opp = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level;
			}
			if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("dm"))) || entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("hunters")))) {
				scale_self = entity.getPersistentData().getDouble("Level");
			} else if (entity instanceof Player) {
				scale_self = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level;
			}
			if (Math.random() < (scale_self) / ((float) scale_opp + 20)) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 5, 1, false, false));
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.destroy")), SoundSource.PLAYERS, (float) 0.7, (float) 0.5);
					} else {
						_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.destroy")), SoundSource.PLAYERS, (float) 0.7, (float) 0.5, false);
					}
				}
				if (event != null) {
					event.setCanceled(true);
				}
			}
		}
	}
}
