package net.solocraft;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.solocraft.init.SololevelingModTabs;
import net.solocraft.init.SololevelingModSounds;
import net.solocraft.init.SololevelingModPotions;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModPaintings;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.init.SololevelingModMenus;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModBlocks;
import net.solocraft.init.SololevelingModBlockEntities;
import net.solocraft.world.dimension.rift.RiftWorldgenRegistries;

import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;

import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.AbstractMap;
import java.util.Optional;

@Mod("sololeveling")
public class SololevelingMod {
	public static final Logger LOGGER = LogManager.getLogger(SololevelingMod.class);
	public static final String MODID = "sololeveling";

	public SololevelingMod() {
		MinecraftForge.EVENT_BUS.register(this);
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		RiftWorldgenRegistries.register(bus);
		SololevelingModSounds.REGISTRY.register(bus);
		SololevelingModBlocks.REGISTRY.register(bus);
		SololevelingModBlockEntities.REGISTRY.register(bus);
		SololevelingModItems.REGISTRY.register(bus);
		SololevelingModEntities.REGISTRY.register(bus);

		SololevelingModTabs.REGISTRY.register(bus);

		SololevelingModMobEffects.REGISTRY.register(bus);
		SololevelingModPotions.REGISTRY.register(bus);
		SololevelingModPaintings.REGISTRY.register(bus);
		SololevelingModParticleTypes.REGISTRY.register(bus);

		SololevelingModMenus.REGISTRY.register(bus);
	}

	private static final String PROTOCOL_VERSION = "3";
	public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
	private static int messageID = 0;

	public static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
		PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer);
		messageID++;
	}

	public static <T> void addNetworkMessage(Class<T> messageType,
			BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder,
			BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer, NetworkDirection direction) {
		PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer,
				Optional.of(direction));
		messageID++;
	}

	private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();
	private static final Collection<ServerBoundWork> serverBoundWorkQueue = new ConcurrentLinkedQueue<>();

	public static void queueServerWork(int tick, Runnable action) {
		if (action == null)
			return;
		workQueue.add(new AbstractMap.SimpleEntry(action, Math.max(1, tick)));
	}

	/**
	 * Queues work that is valid only for one concrete server instance. Unlike the
	 * legacy global queue, these entries are discarded when that server stops (or
	 * when a different integrated server starts in the same client JVM).
	 */
	public static void queueServerWork(MinecraftServer server, int tick, Runnable action) {
		if (server == null || action == null)
			return;
		serverBoundWorkQueue.add(new ServerBoundWork(server, Math.max(1, tick), action));
	}

	@SubscribeEvent
	public void tick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END
				|| workQueue.isEmpty() && serverBoundWorkQueue.isEmpty())
			return;

		List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
		workQueue.forEach(work -> {
			work.setValue(work.getValue() - 1);
			if (work.getValue() == 0)
				actions.add(work);
		});
		actions.forEach(e -> e.getKey().run());
		workQueue.removeAll(actions);

		List<ServerBoundWork> finished = new ArrayList<>();
		List<Runnable> boundActions = new ArrayList<>();
		serverBoundWorkQueue.forEach(work -> {
			if (work.server != event.getServer()) {
				finished.add(work);
				return;
			}
			work.remainingTicks--;
			if (work.remainingTicks == 0) {
				finished.add(work);
				boundActions.add(work.action);
			}
		});
		serverBoundWorkQueue.removeAll(finished);
		boundActions.forEach(Runnable::run);
	}

	@SubscribeEvent
	public void onServerStopping(ServerStoppingEvent event) {
		workQueue.clear();
		serverBoundWorkQueue.removeIf(work -> work.server == event.getServer());
	}

	private static final class ServerBoundWork {
		private final MinecraftServer server;
		private final Runnable action;
		private int remainingTicks;

		private ServerBoundWork(MinecraftServer server, int remainingTicks, Runnable action) {
			this.server = server;
			this.remainingTicks = remainingTicks;
			this.action = action;
		}
	}
}
