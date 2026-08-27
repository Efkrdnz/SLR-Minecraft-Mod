package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.FangedKasakaEntity;
import net.solocraft.entity.BloodRedComIgrisEntity;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;

@EventBusSubscriber
public class KasakaDungeonDeathResetProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(Entity entity, Entity sourceentity) {
		execute(null, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (entity instanceof BloodRedComIgrisEntity || entity instanceof FangedKasakaEntity) {
			if ((entity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sololeveling:dungeon_dimension_kasaka")))
					|| (entity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sololeveling:dungeon_dimension_igris")))) {
				{
					boolean _setval = true;
					sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.instancecomplete = _setval;
						capability.syncPlayerVariables(sourceentity);
					});
				}
			}
		}
	}
}
