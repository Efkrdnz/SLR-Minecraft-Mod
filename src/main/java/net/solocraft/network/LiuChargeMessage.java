package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.LiuZhigangCombatManager;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LiuChargeMessage {
	public static final int BEGIN = 0;
	public static final int RELEASE = 1;
	public static final int CANCEL = 2;

	private final int action;

	public LiuChargeMessage(int action) {
		this.action = action;
	}

	public LiuChargeMessage(FriendlyByteBuf buffer) {
		this.action = buffer.readByte();
	}

	public static void buffer(LiuChargeMessage message, FriendlyByteBuf buffer) {
		buffer.writeByte(message.action);
	}

	public static void handler(LiuChargeMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null || !player.level().hasChunkAt(player.blockPosition()))
				return;
			switch (message.action) {
				case BEGIN -> LiuZhigangCombatManager.beginBeamCharge(player);
				case RELEASE -> LiuZhigangCombatManager.releaseBeamCharge(player);
				case CANCEL -> LiuZhigangCombatManager.cancelBeamCharge(player);
				default -> {
				}
			}
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(LiuChargeMessage.class, LiuChargeMessage::buffer,
				LiuChargeMessage::new, LiuChargeMessage::handler);
	}
}
