package net.solocraft.command;

import net.solocraft.SololevelingMod;
import net.solocraft.world.dimension.rift.RiftGeometry;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Read-only coordinate diagnostics for the Rift implementation. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class DimensionalRiftCommand {
	private DimensionalRiftCommand() {
	}

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		var rift = Commands.literal("rift")
				.then(Commands.literal("info").executes(context -> info(context.getSource())));
		// Keep the same root requirement as the mod's existing /slr command. Brigadier
		// merges duplicate literal roots, so a weaker requirement here could otherwise
		// expose older administrative branches depending on registration order.
		event.getDispatcher().register(Commands.literal("slr")
				.requires(source -> source.hasPermission(3))
				.then(rift));
	}

	private static int info(CommandSourceStack source) {
		double x = source.getPosition().x;
		double z = source.getPosition().z;
		RiftGeometry.Region region = RiftGeometry.resolveDefault(x, z);
		String regionName = region.type().name().toLowerCase(Locale.ROOT).replace('_', ' ');
		String territory = region.territory() == null ? "none" : region.territory().displayName();
		int expectedLevel = RiftGeometry.levelForDistance(region.distance());
		double remaining = Math.max(0.0D, RiftGeometry.DEFAULT_PLAYABLE_RADIUS - region.distance());
		double starEdge = RiftGeometry.starRadius(RiftGeometry.angle(x, z),
				RiftGeometry.DEFAULT_STAR_CORE_RADIUS, RiftGeometry.DEFAULT_STAR_TIP_RADIUS,
				RiftGeometry.DEFAULT_STAR_EXPONENT);

		source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
				"Rift: %s | territory: %s | radius: %.1f | expected mob level: %d | void in: %.1f | local star edge: %.1f",
				regionName, territory, region.distance(), expectedLevel, remaining, starEdge))
				.withStyle(ChatFormatting.AQUA), false);
		return 1;
	}

}
