
package net.solocraft.network;

import net.solocraft.procedures.TripleJumpOnKeyPressedProcedure;
import net.solocraft.SololevelingMod;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TripleJumpMessage {
	int type, pressedms;
	double motionX, motionZ;

	public TripleJumpMessage(int type, int pressedms) {
		this(type, pressedms, 0, 0);
	}

	public TripleJumpMessage(int type, int pressedms, double motionX, double motionZ) {
		this.type = type;
		this.pressedms = pressedms;
		this.motionX = motionX;
		this.motionZ = motionZ;
	}

	public TripleJumpMessage(FriendlyByteBuf buffer) {
		this.type = buffer.readInt();
		this.pressedms = buffer.readInt();
		this.motionX = buffer.readDouble();
		this.motionZ = buffer.readDouble();
	}

	public static void buffer(TripleJumpMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.type);
		buffer.writeInt(message.pressedms);
		buffer.writeDouble(message.motionX);
		buffer.writeDouble(message.motionZ);
	}

	public static void handler(TripleJumpMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			pressAction(context.getSender(), message.type, message.pressedms, message.motionX, message.motionZ);
		});
		context.setPacketHandled(true);
	}

	public static void pressAction(Player entity, int type, int pressedms) {
		pressAction(entity, type, pressedms, 0, 0);
	}

	public static void pressAction(Player entity, int type, int pressedms, double motionX, double motionZ) {
		if (entity == null)
			return;
		Level world = entity.level();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(entity.blockPosition()))
			return;
		if (type == 0) {

			TripleJumpOnKeyPressedProcedure.execute(world, x, y, z, entity, motionX, motionZ);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(TripleJumpMessage.class, TripleJumpMessage::buffer, TripleJumpMessage::new, TripleJumpMessage::handler);
	}
}
