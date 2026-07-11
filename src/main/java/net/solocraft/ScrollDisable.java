package net.solocraft;

import net.solocraft.init.SololevelingModKeyMappings;
import net.solocraft.network.Ability2Message;
import net.solocraft.network.SololevelingModVariables;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.Minecraft;

@Mod.EventBusSubscriber(modid = "sololeveling", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
		int direction = event.getScrollDelta() > 0 ? 1 : -1;
		SololevelingMod.PACKET_HANDLER.sendToServer(new Ability2Message(2, direction));
		event.setCanceled(true);
	}
}
