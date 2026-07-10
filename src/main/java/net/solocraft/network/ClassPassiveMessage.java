package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.ClassPassiveClientState;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/**
 * Server → Client packet that updates one passive-display slot.
 *
 * passiveType values:
 *   0 = Assassin shadow-combo tier  (int 0-10)
 *   1 = Fighter  battle-power       (double 0-100)
 *   2 = Tanker   iron-wall stacks   (int 0-10)
 *   3 = Healer   resonance stacks   (int 0-5)
 *   4 = Ranger   focus charge       (double 0-100)
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClassPassiveMessage {

    public final int    passiveType;
    public final double value;

    public ClassPassiveMessage(int passiveType, double value) {
        this.passiveType = passiveType;
        this.value       = value;
    }

    public ClassPassiveMessage(FriendlyByteBuf buf) {
        this.passiveType = buf.readInt();
        this.value       = buf.readDouble();
    }

    public static void buffer(ClassPassiveMessage msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.passiveType);
        buf.writeDouble(msg.value);
    }

    /** Handler runs on the client's main thread (packet is S→C). */
    public static void handler(ClassPassiveMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClassPassiveClientState.update(msg.passiveType, msg.value));
        ctx.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        SololevelingMod.addNetworkMessage(
                ClassPassiveMessage.class,
                ClassPassiveMessage::buffer,
                ClassPassiveMessage::new,
                ClassPassiveMessage::handler);
    }
}
