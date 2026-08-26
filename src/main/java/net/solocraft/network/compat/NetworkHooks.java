package net.solocraft.network.compat;

import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

/** Compatibility facade for Forge's removed NetworkHooks.openScreen helpers. */
public final class NetworkHooks {
	private NetworkHooks() {
	}

	public static void openScreen(ServerPlayer player, MenuProvider provider) {
		player.openMenu(provider);
	}

	public static void openScreen(ServerPlayer player, MenuProvider provider,
			Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
		player.openMenu(provider, extraDataWriter);
	}

	/** Preserves Forge's legacy BlockPos convenience overload. */
	public static void openScreen(ServerPlayer player, MenuProvider provider, BlockPos position) {
		player.openMenu(provider, buffer -> buffer.writeBlockPos(position));
	}
}
