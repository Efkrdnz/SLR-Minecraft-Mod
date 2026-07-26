package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.Portal12Entity;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class DunIgrisDetectionProcedure {
	private static final ResourceKey<Level> IGRIS_DIMENSION = ResourceKey.create(Registries.DIMENSION,
			new ResourceLocation("sololeveling", "dungeon_dimension_igris"));

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player.level(), event.player);
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (!entity.level().dimension().equals(IGRIS_DIMENSION))
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (capability.tpd)
				return;
			AABB nearby = AABB.ofSize(entity.position(), 6.0D, 6.0D, 6.0D);
			boolean reachedPortal = !world.getEntitiesOfClass(Portal12Entity.class, nearby,
					portal -> portal.distanceToSqr(entity) <= 9.0D).isEmpty();
			if (!reachedPortal)
				return;
			capability.tpd = true;
			capability.syncPlayerVariables(entity);
			entity.setNoGravity(false);
		});
	}
}
