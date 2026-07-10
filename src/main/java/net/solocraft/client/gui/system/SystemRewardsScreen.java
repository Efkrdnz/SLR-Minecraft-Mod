package net.solocraft.client.gui.system;

import net.solocraft.SololevelingMod;
import net.solocraft.network.RewardPanelButtonMessage;
import net.solocraft.util.RewardManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** System-styled Rewards panel. Shows all pending reward slots and grows downward for extra rewards. */
public class SystemRewardsScreen extends SystemScreen {
	private final List<SystemButton> claimButtons = new ArrayList<>();

	public SystemRewardsScreen() {
		super(Component.literal("REWARDS"));
		this.panelW = 230;
		this.panelH = 160;
	}

	@Override
	protected void init() {
		Player player = Minecraft.getInstance().player;
		int rows = Math.max(3, player == null ? 0 : RewardManager.allRewards(player).size());
		this.panelH = Math.max(160, 72 + rows * 34);
		super.init();
		addRenderableWidget(new SystemButton(panelX + 3, panelY + 3, 40, 12, Component.literal("< Back"), b -> openChild(new SystemPanelScreen())));

		claimButtons.clear();
		for (int slot = 1; slot <= rows; slot++) {
			final int rewardSlot = slot;
			SystemButton button = new SystemButton(panelX + panelW - 62, rowY(slot - 1), 50, 20, Component.literal("Claim"), b -> claim(rewardSlot));
			claimButtons.add(button);
			addRenderableWidget(button);
		}
	}

	private int rowY(int index) {
		return panelY + 40 + index * 34;
	}

	private void claim(int slot) {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return;
		BlockPos bp = player.blockPosition();
		SololevelingMod.PACKET_HANDLER.sendToServer(new RewardPanelButtonMessage(99 + slot, bp.getX(), bp.getY(), bp.getZ()));
	}

	@Override
	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		Player entity = Minecraft.getInstance().player;
		if (entity == null)
			return;
		List<String> rewards = RewardManager.allRewards(entity);
		boolean any = false;
		for (int index = 0; index < claimButtons.size(); index++) {
			int y = rowY(index);
			boolean empty = index >= rewards.size();
			String shown = empty ? "§8- empty -" : RewardManager.displayName(rewards.get(index));
			SystemButton button = claimButtons.get(index);
			button.active = !empty;
			button.visible = isFullyOpen() && !empty;
			if (!empty)
				any = true;
			g.drawString(this.font, "§7Slot " + (index + 1), panelX + 12, y - 8, TEXT_SUB, false);
			g.drawString(this.font, shown, panelX + 12, y + 4, TEXT_MAIN, false);
			g.fill(panelX + 10, y + 24, panelX + panelW - 10, y + 25, ACCENT_SOFT);
		}
		if (!any) {
			String none = "No rewards to collect";
			g.drawString(this.font, none, panelX + (panelW - this.font.width(none)) / 2, panelY + 24, TEXT_SUB, false);
		}
	}
}
