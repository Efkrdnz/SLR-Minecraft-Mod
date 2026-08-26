package net.solocraft.procedures;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.ServerChatEvent;

import javax.annotation.Nullable;

@EventBusSubscriber
public class JoiningPartySecondProcedure {
	@SubscribeEvent
	public static void onChat(ServerChatEvent event) {
		execute(event);
	}

	public static void execute() {
		execute(null);
	}

	private static void execute(@Nullable Event event) {
	}
}
