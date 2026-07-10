package net.solocraft.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.solocraft.dungeon.DungeonTheme;
import net.solocraft.dungeon.ProceduralDungeonGenerator;
import net.solocraft.dungeon.ProceduralDungeonRank;
import net.solocraft.dungeon.ProceduralDungeonResult;
import net.solocraft.dungeon.ProceduralDungeonSettings;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.stream.IntStream;

@Mod.EventBusSubscriber
public class SlrProceduralDungeonCommand {
	private static final SuggestionProvider<CommandSourceStack> RANK_SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(ProceduralDungeonRank.values()).map(Enum::name), builder);
	private static final SuggestionProvider<CommandSourceStack> THEME_SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(DungeonTheme.values()).map(theme -> theme.name().toLowerCase()), builder);
	private static final SuggestionProvider<CommandSourceStack> COMPLEXITY_SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(IntStream.rangeClosed(1, 10).mapToObj(Integer::toString), builder);

	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("slr").requires(source -> source.hasPermission(3))
				.then(Commands.argument("name", EntityArgument.player())
						.then(Commands.literal("proceduraldungeon")
								.then(Commands.argument("rank", StringArgumentType.word()).suggests(RANK_SUGGESTIONS)
										.then(Commands.argument("theme", StringArgumentType.word()).suggests(THEME_SUGGESTIONS)
												.then(Commands.argument("complexity", IntegerArgumentType.integer(1, 10)).suggests(COMPLEXITY_SUGGESTIONS).executes(arguments -> {
													ServerPlayer player = EntityArgument.getPlayer(arguments, "name");
													if (!(player.level() instanceof ServerLevel level))
														return 0;
													ProceduralDungeonRank rank = ProceduralDungeonRank.fromString(StringArgumentType.getString(arguments, "rank"));
													DungeonTheme theme = DungeonTheme.fromString(StringArgumentType.getString(arguments, "theme"));
													int complexity = IntegerArgumentType.getInteger(arguments, "complexity");
													ProceduralDungeonResult result = ProceduralDungeonGenerator.generate(level, player.blockPosition(), new ProceduralDungeonSettings(rank, theme, complexity), player);
													player.teleportTo(level, result.startPos.getX() + 0.5, result.startPos.getY(), result.startPos.getZ() + 0.5, player.getYRot(), player.getXRot());
													arguments.getSource().sendSuccess(() -> Component.literal("Generated " + rank.name() + " " + theme.name().toLowerCase()
															+ " procedural dungeon: " + result.rooms + " rooms, " + result.monsters + " mobs. Start "
															+ result.startPos.getX() + " " + result.startPos.getY() + " " + result.startPos.getZ()), true);
													return 1;
												})))))));
	}
}
