package net.solocraft.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.ServerChatEvent;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
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
