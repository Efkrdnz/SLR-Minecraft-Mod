package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class IsGateClearedProcedure {
	@SubscribeEvent
	public static void onEntityTick(LivingEvent.LivingTickEvent event) {
		execute(event, event.getEntity().level(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		String uuid = "";
		if (world.dayTime() % 20 == 0) {
			if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("portals")))) {
				if (SololevelingModVariables.MapVariables.get(world).GatesCleared.contains(entity.getStringUUID())) {
					if (entity.getPersistentData().getBoolean("slr_is_red_gate")) {
						SololevelingModVariables.MapVariables.get(world).RedGate = false;
					}
					if (!entity.level().isClientSide())
						entity.discard();
					SololevelingModVariables.MapVariables.get(world).GatesCleared = SololevelingModVariables.MapVariables.get(world).GatesCleared.replace(entity.getStringUUID(), "");
					SololevelingModVariables.MapVariables.get(world).syncData(world);
				}
			}
			if (!(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dungeoning) {
				entity.getPersistentData().putString("dungeon_tag", "");
			}
		}
	}
}
