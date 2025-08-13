package net.solocraft.client.gui;

import net.solocraft.world.inventory.UnlockedSkillsTab2Menu;
import net.solocraft.procedures.PlistReturn9Procedure;
import net.solocraft.procedures.PlistReturn16Procedure;
import net.solocraft.procedures.PlistReturn15Procedure;
import net.solocraft.procedures.PlistReturn14Procedure;
import net.solocraft.procedures.PlistReturn13Procedure;
import net.solocraft.procedures.PlistReturn12Procedure;
import net.solocraft.procedures.PlistReturn11Procedure;
import net.solocraft.procedures.PlistReturn10Procedure;
import net.solocraft.procedures.PlistButtonCon9Procedure;
import net.solocraft.procedures.PlistButtonCon16Procedure;
import net.solocraft.procedures.PlistButtonCon15Procedure;
import net.solocraft.procedures.PlistButtonCon14Procedure;
import net.solocraft.procedures.PlistButtonCon13Procedure;
import net.solocraft.procedures.PlistButtonCon12Procedure;
import net.solocraft.procedures.PlistButtonCon11Procedure;
import net.solocraft.procedures.PlistButtonCon10Procedure;
import net.solocraft.network.UnlockedSkillsTab2ButtonMessage;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class UnlockedSkillsTab2Screen extends AbstractContainerScreen<UnlockedSkillsTab2Menu> {
	private final static HashMap<String, Object> guistate = UnlockedSkillsTab2Menu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	Button button_equip;
	Button button_equip1;
	Button button_equip2;
	Button button_equip3;
	Button button_equip4;
	Button button_equip5;
	Button button_equip6;
	Button button_equip7;
	ImageButton imagebutton_button1;
	ImageButton imagebutton_button11;

	public UnlockedSkillsTab2Screen(UnlockedSkillsTab2Menu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/unlocked_skills_tab_2.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/big_frame2.png"), this.leftPos + -75, this.topPos + -108, 0, 0, 150, 200, 150, 200);

		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	public void containerTick() {
		super.containerTick();
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font,

				PlistReturn9Procedure.execute(entity), -61, -88, -1, false);
		guiGraphics.drawString(this.font,

				PlistReturn10Procedure.execute(entity), -61, -69, -1, false);
		guiGraphics.drawString(this.font,

				PlistReturn11Procedure.execute(entity), -61, -50, -1, false);
		guiGraphics.drawString(this.font,

				PlistReturn12Procedure.execute(entity), -61, -31, -1, false);
		guiGraphics.drawString(this.font,

				PlistReturn13Procedure.execute(entity), -61, -12, -1, false);
		guiGraphics.drawString(this.font,

				PlistReturn14Procedure.execute(entity), -61, 7, -1, false);
		guiGraphics.drawString(this.font,

				PlistReturn15Procedure.execute(entity), -61, 26, -1, false);
		guiGraphics.drawString(this.font,

				PlistReturn16Procedure.execute(entity), -61, 44, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.unlocked_skills_tab_2.label_empty"), -120, -22, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.unlocked_skills_tab_2.label_empty1"), 89, -22, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		button_equip = Button.builder(Component.translatable("gui.sololeveling.unlocked_skills_tab_2.button_equip"), e -> {
			if (PlistButtonCon9Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new UnlockedSkillsTab2ButtonMessage(0, x, y, z));
				UnlockedSkillsTab2ButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}).bounds(this.leftPos + 18, this.topPos + -95, 51, 20).build(builder -> new Button(builder) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (PlistButtonCon9Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		});
		guistate.put("button:button_equip", button_equip);
		this.addRenderableWidget(button_equip);
		button_equip1 = Button.builder(Component.translatable("gui.sololeveling.unlocked_skills_tab_2.button_equip1"), e -> {
			if (PlistButtonCon10Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new UnlockedSkillsTab2ButtonMessage(1, x, y, z));
				UnlockedSkillsTab2ButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}).bounds(this.leftPos + 18, this.topPos + -76, 51, 20).build(builder -> new Button(builder) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (PlistButtonCon10Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		});
		guistate.put("button:button_equip1", button_equip1);
		this.addRenderableWidget(button_equip1);
		button_equip2 = Button.builder(Component.translatable("gui.sololeveling.unlocked_skills_tab_2.button_equip2"), e -> {
			if (PlistButtonCon11Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new UnlockedSkillsTab2ButtonMessage(2, x, y, z));
				UnlockedSkillsTab2ButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		}).bounds(this.leftPos + 18, this.topPos + -57, 51, 20).build(builder -> new Button(builder) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (PlistButtonCon11Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		});
		guistate.put("button:button_equip2", button_equip2);
		this.addRenderableWidget(button_equip2);
		button_equip3 = Button.builder(Component.translatable("gui.sololeveling.unlocked_skills_tab_2.button_equip3"), e -> {
			if (PlistButtonCon12Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new UnlockedSkillsTab2ButtonMessage(3, x, y, z));
				UnlockedSkillsTab2ButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		}).bounds(this.leftPos + 18, this.topPos + -38, 51, 20).build(builder -> new Button(builder) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (PlistButtonCon12Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		});
		guistate.put("button:button_equip3", button_equip3);
		this.addRenderableWidget(button_equip3);
		button_equip4 = Button.builder(Component.translatable("gui.sololeveling.unlocked_skills_tab_2.button_equip4"), e -> {
			if (PlistButtonCon13Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new UnlockedSkillsTab2ButtonMessage(4, x, y, z));
				UnlockedSkillsTab2ButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		}).bounds(this.leftPos + 18, this.topPos + -19, 51, 20).build(builder -> new Button(builder) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (PlistButtonCon13Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		});
		guistate.put("button:button_equip4", button_equip4);
		this.addRenderableWidget(button_equip4);
		button_equip5 = Button.builder(Component.translatable("gui.sololeveling.unlocked_skills_tab_2.button_equip5"), e -> {
			if (PlistButtonCon14Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new UnlockedSkillsTab2ButtonMessage(5, x, y, z));
				UnlockedSkillsTab2ButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		}).bounds(this.leftPos + 18, this.topPos + 0, 51, 20).build(builder -> new Button(builder) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (PlistButtonCon14Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		});
		guistate.put("button:button_equip5", button_equip5);
		this.addRenderableWidget(button_equip5);
		button_equip6 = Button.builder(Component.translatable("gui.sololeveling.unlocked_skills_tab_2.button_equip6"), e -> {
			if (PlistButtonCon15Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new UnlockedSkillsTab2ButtonMessage(6, x, y, z));
				UnlockedSkillsTab2ButtonMessage.handleButtonAction(entity, 6, x, y, z);
			}
		}).bounds(this.leftPos + 18, this.topPos + 19, 51, 20).build(builder -> new Button(builder) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (PlistButtonCon15Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		});
		guistate.put("button:button_equip6", button_equip6);
		this.addRenderableWidget(button_equip6);
		button_equip7 = Button.builder(Component.translatable("gui.sololeveling.unlocked_skills_tab_2.button_equip7"), e -> {
			if (PlistButtonCon16Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new UnlockedSkillsTab2ButtonMessage(7, x, y, z));
				UnlockedSkillsTab2ButtonMessage.handleButtonAction(entity, 7, x, y, z);
			}
		}).bounds(this.leftPos + 18, this.topPos + 38, 51, 20).build(builder -> new Button(builder) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (PlistButtonCon16Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		});
		guistate.put("button:button_equip7", button_equip7);
		this.addRenderableWidget(button_equip7);
		imagebutton_button1 = new ImageButton(this.leftPos + -127, this.topPos + -27, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_button1.png"), 48, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new UnlockedSkillsTab2ButtonMessage(8, x, y, z));
				UnlockedSkillsTab2ButtonMessage.handleButtonAction(entity, 8, x, y, z);
			}
		});
		guistate.put("button:imagebutton_button1", imagebutton_button1);
		this.addRenderableWidget(imagebutton_button1);
		imagebutton_button11 = new ImageButton(this.leftPos + 80, this.topPos + -27, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_button11.png"), 48, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new UnlockedSkillsTab2ButtonMessage(9, x, y, z));
				UnlockedSkillsTab2ButtonMessage.handleButtonAction(entity, 9, x, y, z);
			}
		});
		guistate.put("button:imagebutton_button11", imagebutton_button11);
		this.addRenderableWidget(imagebutton_button11);
	}
}
