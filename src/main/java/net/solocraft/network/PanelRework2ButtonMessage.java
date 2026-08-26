
package net.solocraft.network;

import net.solocraft.world.inventory.PanelRework2Menu;
import net.solocraft.procedures.VitalityIncreaseProcedure;
import net.solocraft.procedures.StrengthIncreaseProcedure;
import net.solocraft.procedures.SpeedIncreaseProcedure;
import net.solocraft.procedures.SenseIncreaseProcedure;
import net.solocraft.procedures.RewardScreenOpenProcedure;
import net.solocraft.procedures.OpenTrainingGUIProcedure;
import net.solocraft.procedures.OpenStoreGUIProcedure;
import net.solocraft.procedures.OpenMainQuestsGUIProcedure;
import net.solocraft.procedures.IntelligenceIncreaseProcedure;
import net.solocraft.procedures.CraftingGUIopenProcedure;
import net.solocraft.procedures.AbilitiesGUIopenProcedure;
import net.solocraft.util.StatInvestmentHelper;
import net.solocraft.SololevelingMod;

import net.solocraft.network.compat.NetworkEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import java.util.function.Supplier;
import java.util.HashMap;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class PanelRework2ButtonMessage {
	private final int buttonID, x, y, z, investmentAmount;

	public PanelRework2ButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
		this.investmentAmount = buffer.readInt();
	}

	public PanelRework2ButtonMessage(int buttonID, int x, int y, int z) {
		this(buttonID, x, y, z, 0);
	}

	public PanelRework2ButtonMessage(int buttonID, int x, int y, int z, int investmentAmount) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.investmentAmount = investmentAmount;
	}

	public static void buffer(PanelRework2ButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
		buffer.writeInt(message.investmentAmount);
	}

	public static void handler(PanelRework2ButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			int buttonID = message.buttonID;
			int x = message.x;
			int y = message.y;
			int z = message.z;
			int investmentAmount = message.investmentAmount;
			handleButtonAction(entity, buttonID, x, y, z, investmentAmount);
		});
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		handleButtonAction(entity, buttonID, x, y, z, 0);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z, int investmentAmount) {
		Level world = entity.level();
		HashMap guistate = PanelRework2Menu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {
			invest(entity, StatInvestmentHelper.Stat.STRENGTH, investmentAmount);
		}
		if (buttonID == 1) {
			invest(entity, StatInvestmentHelper.Stat.AGILITY, investmentAmount);
		}
		if (buttonID == 2) {
			invest(entity, StatInvestmentHelper.Stat.PERCEPTION, investmentAmount);
		}
		if (buttonID == 3) {
			invest(entity, StatInvestmentHelper.Stat.VITALITY, investmentAmount);
		}
		if (buttonID == 4) {
			invest(entity, StatInvestmentHelper.Stat.INTELLIGENCE, investmentAmount);
		}
		if (buttonID == 5) {

			OpenStoreGUIProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 6) {

			OpenMainQuestsGUIProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 7) {

			RewardScreenOpenProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 8) {

			CraftingGUIopenProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 9) {

			OpenTrainingGUIProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 10) {

			AbilitiesGUIopenProcedure.execute(world, x, y, z, entity);
		}
	}

	private static void invest(Player entity, StatInvestmentHelper.Stat stat, int requested) {
		if (requested == 1 || requested == 5 || requested == 10) {
			StatInvestmentHelper.invest(entity, stat, requested);
			return;
		}
		// Legacy panel messages use zero and retain their configured investvalue.
		switch (stat) {
			case STRENGTH -> StrengthIncreaseProcedure.execute(entity);
			case AGILITY -> SpeedIncreaseProcedure.execute(entity);
			case PERCEPTION -> SenseIncreaseProcedure.execute(entity);
			case VITALITY -> VitalityIncreaseProcedure.execute(entity);
			case INTELLIGENCE -> IntelligenceIncreaseProcedure.execute(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(PanelRework2ButtonMessage.class, PanelRework2ButtonMessage::buffer, PanelRework2ButtonMessage::new, PanelRework2ButtonMessage::handler);
	}
}
