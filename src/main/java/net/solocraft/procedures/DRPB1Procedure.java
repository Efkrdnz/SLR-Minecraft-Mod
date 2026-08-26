package net.solocraft.procedures;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import javax.annotation.Nullable;

public class DRPB1Procedure {
	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Post event) {
		execute(event);
	}

	public static void execute() {
		execute(null);
	}

	private static void execute(@Nullable Event event) {
	}
}
