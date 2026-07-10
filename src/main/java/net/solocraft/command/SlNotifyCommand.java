package net.solocraft.command;

import net.solocraft.util.SystemNotifications;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.brigadier.arguments.StringArgumentType;

/**
 * Test command for the System notification overlay. Demonstrates all three
 * layouts; message text supports {@code &}/{@code §} colour codes.
 *
 * <ul>
 *   <li>{@code /slnotify title <text…>}   — big centred title</li>
 *   <li>{@code /slnotify under <text…>}   — small centred undertext</li>
 *   <li>{@code /slnotify both <title> | <under>} — title + undertext</li>
 * </ul>
 */
@Mod.EventBusSubscriber
public class SlNotifyCommand {
	private static final int DURATION = 80; // ticks

	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("slnotify").requires(s -> s.hasPermission(2))
				.then(Commands.literal("title").then(Commands.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					SystemNotifications.showTitle(player, SystemNotifications.ACCENT, DURATION, comp(StringArgumentType.getString(ctx, "text")));
					return 1;
				})))
				.then(Commands.literal("under").then(Commands.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					SystemNotifications.showUnder(player, SystemNotifications.ACCENT, DURATION, comp(StringArgumentType.getString(ctx, "text")));
					return 1;
				})))
				.then(Commands.literal("both").then(Commands.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					String[] parts = StringArgumentType.getString(ctx, "text").split("\\|", 2);
					Component title = comp(parts[0].trim());
					Component under = parts.length > 1 && !parts[1].trim().isEmpty() ? comp(parts[1].trim()) : null;
					SystemNotifications.showTitleUnder(player, SystemNotifications.ACCENT, DURATION, title, under);
					return 1;
				}))));
	}

	private static Component comp(String s) {
		return Component.literal(s.replace("\\n", "\n").replace('&', '§'));
	}
}
