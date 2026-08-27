package net.solocraft;

import net.solocraft.init.SololevelingModKeyMappings;
import net.solocraft.network.Ability2Message;
import net.solocraft.network.SololevelingModVariables;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.client.Minecraft;

@EventBusSubscriber(modid = "sololeveling", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ScrollDisable {
	@SubscribeEvent
	public static void Scroll(InputEvent.MouseScrollingEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.screen != null || !SololevelingModKeyMappings.ABILITY_2.isDown())
			return;
		var variables = minecraft.player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		if (!variables.combatmode || !net.solocraft.util.RulersAuthorityManager.hasAbility(minecraft.player))
			return;
		double scrollDelta = event.getScrollDeltaY();
		if (scrollDelta == 0.0D)
			return;
		int direction = scrollDelta > 0.0D ? 1 : -1;
		SololevelingMod.PACKET_HANDLER.sendToServer(new Ability2Message(2, direction));
		event.setCanceled(true);
	}
}
