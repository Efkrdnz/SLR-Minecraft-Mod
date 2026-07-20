package net.solocraft.guild;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative, short-lived invitations for player-created guilds. */
@Mod.EventBusSubscriber
public final class GuildInvitationManager {
    private static final long INVITE_LIFETIME_MS = 120_000L;
    private static final Map<UUID, PendingInvite> PENDING = new HashMap<>();

    private GuildInvitationManager() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("slguildinvite")
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> sendInvitation(
                                        context.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("accept")
                        .then(Commands.argument("token", StringArgumentType.word())
                                .executes(context -> respond(
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "token"), true))))
                .then(Commands.literal("reject")
                        .then(Commands.argument("token", StringArgumentType.word())
                                .executes(context -> respond(
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "token"), false)))));
    }

    public static int sendInvitation(ServerPlayer inviter, ServerPlayer target) {
        pruneExpired();
        GuildSavedData data = GuildSavedData.get(inviter.serverLevel());
        GuildData guild = data.getGuildForPlayer(inviter.getUUID());
        if (guild == null || !guild.canOperate(inviter.getUUID())) {
            inviter.sendSystemMessage(Component.literal("Only the guild owner can invite players.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        if (target.getUUID().equals(inviter.getUUID())) {
            inviter.sendSystemMessage(Component.literal("You cannot invite yourself.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        GuildData targetGuild = data.getGuildForPlayer(target.getUUID());
        if (targetGuild != null) {
            inviter.sendSystemMessage(Component.literal(target.getName().getString() + " is already in a guild.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        PENDING.values().removeIf(invite -> invite.targetId.equals(target.getUUID())
                && invite.guildId.equals(guild.id));
        UUID token = UUID.randomUUID();
        PENDING.put(token, new PendingInvite(guild.id, inviter.getUUID(), target.getUUID(),
                System.currentTimeMillis() + INVITE_LIFETIME_MS));

        MutableComponent accept = Component.literal("[Accept]").withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withBold(true)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/slguildinvite accept " + token))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Join " + guild.name))));
        MutableComponent reject = Component.literal("[Reject]").withStyle(style -> style
                .withColor(ChatFormatting.RED)
                .withBold(true)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/slguildinvite reject " + token))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Reject this invitation"))));
        MutableComponent message = Component.literal("You got invited to ")
                .append(Component.literal(guild.name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal(" guild by "))
                .append(Component.literal(inviter.getName().getString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(". "))
                .append(accept)
                .append(Component.literal(" "))
                .append(reject);
        target.sendSystemMessage(message);
        inviter.sendSystemMessage(Component.literal("Invitation sent to " + target.getName().getString() + ".")
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int respond(ServerPlayer player, String tokenText, boolean accept) {
        pruneExpired();
        UUID token;
        try {
            token = UUID.fromString(tokenText);
        } catch (IllegalArgumentException exception) {
            player.sendSystemMessage(Component.literal("That guild invitation is invalid.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        PendingInvite invite = PENDING.get(token);
        if (invite == null || !invite.targetId.equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("That guild invitation is no longer available.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        PENDING.remove(token);
        if (!accept) {
            player.sendSystemMessage(Component.literal("Guild invitation rejected.")
                    .withStyle(ChatFormatting.GRAY));
            notifyInviter(player, invite,
                    player.getName().getString() + " rejected the guild invitation.", ChatFormatting.RED);
            return 1;
        }

        GuildSavedData data = GuildSavedData.get(player.serverLevel());
        GuildData guild = data.getGuild(invite.guildId);
        if (guild == null || !guild.ownerUUID.equals(invite.inviterId)) {
            player.sendSystemMessage(Component.literal("That guild invitation is no longer valid.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        if (data.getGuildForPlayer(player.getUUID()) != null) {
            player.sendSystemMessage(Component.literal("You are already in a guild.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        guild.memberPermissions.add(new GuildMemberPermissions(
                player.getUUID(), player.getName().getString()));
        data.markDirty();
        PENDING.values().removeIf(other -> other.targetId.equals(player.getUUID()));
        player.sendSystemMessage(Component.literal("You joined " + guild.name + " guild.")
                .withStyle(ChatFormatting.GREEN));
        notifyInviter(player, invite,
                player.getName().getString() + " accepted the invitation and joined the guild.",
                ChatFormatting.GREEN);
        return 1;
    }

    private static void notifyInviter(ServerPlayer responder, PendingInvite invite,
            String text, ChatFormatting color) {
        ServerPlayer inviter = responder.getServer().getPlayerList().getPlayer(invite.inviterId);
        if (inviter != null)
            inviter.sendSystemMessage(Component.literal(text).withStyle(color));
    }

    private static void pruneExpired() {
        long now = System.currentTimeMillis();
        PENDING.values().removeIf(invite -> invite.expiresAtMillis < now);
    }

    private record PendingInvite(UUID guildId, UUID inviterId, UUID targetId, long expiresAtMillis) {
    }
}
