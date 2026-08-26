package net.solocraft.procedures;

import net.solocraft.dungeon.runtime.SnowRedGateArenaManager;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.GateCompletionTokens;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;

@EventBusSubscriber
public class IsGateClearedProcedure {
	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null || entity.level().isClientSide() || entity.tickCount % 20 != 0)
			return;

		if (!entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("portals"))))
			return;

		SololevelingModVariables.MapVariables variables = SololevelingModVariables.MapVariables.get(world);
		String gateId = entity.getStringUUID();
		if (!GateCompletionTokens.contains(variables.GatesCleared, gateId))
			return;

		if (entity.getPersistentData().getBoolean("slr_is_red_gate")) {
			variables.RedGate = world.getServer() != null
					&& SnowRedGateArenaManager.hasActiveArena(world.getServer());
		}
		entity.discard();
		variables.GatesCleared = GateCompletionTokens.remove(variables.GatesCleared, gateId);
		variables.syncData(world);
	}
}
