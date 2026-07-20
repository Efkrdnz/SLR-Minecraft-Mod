package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.guild.GuildBuffRegistry;
import net.solocraft.guild.GuildData;
import net.solocraft.guild.GuildDeployment;
import net.solocraft.guild.GuildGateHelper;
import net.solocraft.guild.GuildHunter;
import net.solocraft.guild.GuildInvitationManager;
import net.solocraft.guild.GuildTeam;
import net.solocraft.guild.HunterRecruitManager;
import net.solocraft.guild.GuildMemberPermissions;
import net.solocraft.guild.GuildSavedData;
import net.solocraft.guild.GuildTickHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import net.solocraft.block.entity.GuildComputerBlockEntity;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client → Server packet for all guild management actions.
 *
 * Actions:
 *   "create"            – create a new guild with given name
 *   "invite_member"     – invite an online player by name
 *   "remove_member"     – remove a member by UUID string
 *   "toggle_perm"       – toggle one permission flag for a member
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class GuildActionMessage {

    public final String   action;
    public final BlockPos computerPos;
    public final String   param1; // varies per action
    public final String   param2; // varies per action

    public GuildActionMessage(String action, BlockPos computerPos, String param1, String param2) {
        this.action      = action;
        this.computerPos = computerPos;
        this.param1      = param1;
        this.param2      = param2;
    }

    // ── Self-registration ─────────────────────────────────────────────────────

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        SololevelingMod.addNetworkMessage(
                GuildActionMessage.class,
                GuildActionMessage::encode,
                GuildActionMessage::decode,
                GuildActionMessage::handle);
    }

    // ── Codec ─────────────────────────────────────────────────────────────────

    public static void encode(GuildActionMessage msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.action);
        buf.writeBlockPos(msg.computerPos);
        buf.writeUtf(msg.param1);
        buf.writeUtf(msg.param2);
    }

    public static GuildActionMessage decode(FriendlyByteBuf buf) {
        return new GuildActionMessage(
                buf.readUtf(),
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readUtf());
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(GuildActionMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            GuildSavedData data = GuildSavedData.get(level);
            GuildTickHandler.resolveDueDeployments(level.getServer(), level.getServer().overworld(), data);
            UUID playerUUID = player.getUUID();

            BlockEntity computerBlock = level.getBlockEntity(msg.computerPos);
            if (!(computerBlock instanceof GuildComputerBlockEntity)) return;

            switch (msg.action) {

                case "create" -> {
                    // param1 = guild name
                    String guildName = msg.param1.trim();
                    if (guildName.isEmpty() || guildName.length() > 24) return;

                    // Player must not already be in a guild
                    if (data.getGuildForPlayer(playerUUID) != null) return;

                    GuildData guild = data.createGuild(guildName, playerUUID, player.getName().getString());
                    if (guild == null) {
                        // Name taken — notify player
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cGuild name \"" + guildName + "\" is already taken."));
                        return;
                    }

                    // Bind this computer to the new guild
                    BlockEntity be = level.getBlockEntity(msg.computerPos);
                    if (be instanceof GuildComputerBlockEntity computer) {
                        computer.setBoundGuildId(guild.id);
                    }

                    data.markDirty();
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aGuild §e\"" + guildName + "\" §acreated successfully!"));

                    // Re-open the screen with fresh data
                    reopenScreen(player, level, msg.computerPos);
                }

                case "invite_member" -> {
                    // param1 = target player name
                    GuildData guild = data.getGuildForPlayer(playerUUID);
                    if (guild == null || !guild.canOperate(playerUUID)) return;

                    String targetName = msg.param1.trim();
                    ServerPlayer target = level.getServer().getPlayerList().getPlayerByName(targetName);
                    if (target == null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cPlayer \"" + targetName + "\" is not online."));
                        return;
                    }
                    GuildInvitationManager.sendInvitation(player, target);
                }

                case "delete_guild" -> {
                    GuildData guild = data.getGuildForPlayer(playerUUID);
                    if (guild == null || !guild.canOperate(playerUUID)) return;

                    String guildName = guild.name;
                    if (data.deleteGuild(guild.id)) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "Â§cGuild Â§e\"" + guildName + "\" Â§chas been disbanded."));
                        reopenScreen(player, level, msg.computerPos);
                    }
                }

                case "remove_member" -> {
                    // param1 = target UUID string
                    GuildData guild = data.getGuildForPlayer(playerUUID);
                    if (guild == null || !guild.canOperate(playerUUID)) return;

                    UUID targetUUID = UUID.fromString(msg.param1);
                    guild.memberPermissions.removeIf(p -> p.playerUUID.equals(targetUUID));
                    data.markDirty();
                    reopenScreen(player, level, msg.computerPos);
                }

                case "toggle_perm" -> {
                    // param1 = target UUID string, param2 = permission key
                    GuildData guild = data.getGuildForPlayer(playerUUID);
                    if (guild == null || !guild.canOperate(playerUUID)) return;

                    UUID targetUUID = UUID.fromString(msg.param1);
                    GuildMemberPermissions perms = guild.getPermissions(targetUUID);
                    if (perms == null) return;

                    switch (msg.param2) {
                        case "canOpen"        -> perms.setAll(!perms.canOpen);
                        case "tabOverview"    -> perms.tabOverview    = !perms.tabOverview;
                        case "tabRoster"      -> perms.tabRoster      = !perms.tabRoster;
                        case "tabTeams"       -> perms.tabTeams       = !perms.tabTeams;
                        case "tabDungeons"    -> perms.tabDungeons    = !perms.tabDungeons;
                        case "tabStorage"     -> perms.tabStorage     = !perms.tabStorage;
                        case "tabBuffs"       -> perms.tabBuffs       = !perms.tabBuffs;
                        case "tabLeaderboard" -> perms.tabLeaderboard = !perms.tabLeaderboard;
                    }
                    data.markDirty();
                    reopenScreen(player, level, msg.computerPos);
                }

                case "set_buff" -> {
                    GuildData guild = data.getGuildForPlayer(playerUUID);
                    if (guild == null || !guild.canOperate(playerUUID)) return;
                    int slot;
                    int buffId;
                    try {
                        slot = Integer.parseInt(msg.param1);
                        buffId = Integer.parseInt(msg.param2);
                    } catch (NumberFormatException e) {
                        return;
                    }
                    if (slot < 1 || slot > 2) return;
                    if (!GuildBuffRegistry.isSlotUnlocked(guild, slot)) {
                        player.sendSystemMessage(Component.literal("§cThat guild buff slot is still locked."));
                        return;
                    }
                    if (!GuildBuffRegistry.isUnlocked(guild, buffId)) {
                        player.sendSystemMessage(Component.literal("§cYour guild has not unlocked that buff yet."));
                        return;
                    }
                    if (slot == 1) guild.activeBuffSlot1 = buffId;
                    else guild.activeBuffSlot2 = buffId;
                    data.markDirty();
                    reopenScreen(player, level, msg.computerPos);
                }

                case "hire" -> {
                    // param1 = recruit UUID string
                    GuildData guild = data.getGuildForPlayer(playerUUID);
                    if (guild == null) return;
                    if (!guild.canOperate(playerUUID)) return;
                    guild.pruneTeamMembers();

                    if (guild.hunters.size() >= GuildData.MAX_HUNTERS) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cGuild is full! (max " + GuildData.MAX_HUNTERS + " hunters)"));
                        return;
                    }

                    UUID recruitId = UUID.fromString(msg.param1);
                    GuildHunter recruit = guild.getRecruit(recruitId);
                    if (recruit == null) return;

                    // Check and deduct hire cost
                    int cost = GuildHunter.hireCost(recruit.rank);
                    ItemStack costItem = getCostItem(recruit.rank);
                    if (!hasEnoughItems(player, costItem, cost)) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cNot enough " + GuildHunter.hireMaterialName(recruit.rank)
                                + "! Need §e" + cost + "§c."));
                        return;
                    }
                    removeItems(player, costItem, cost);

                    guild.recruitPool.remove(recruit);
                    guild.removeHunterFromAllTeams(recruit.id);
                    recruit.status = "idle";
                    guild.hunters.add(recruit);
                    data.markDirty();
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aHired §e" + recruit.name + " §7[" + recruit.rank + " " + recruit.hunterClass + "]§a!"));
                    reopenScreen(player, level, msg.computerPos);
                }

                case "dismiss" -> {
                    // param1 = hunter UUID string
                    GuildData guild = data.getGuildForPlayer(playerUUID);
                    if (guild == null || !guild.canOperate(playerUUID)) return;
                    guild.pruneTeamMembers();

                    UUID hunterId = UUID.fromString(msg.param1);
                    GuildHunter hunter = guild.getHunter(hunterId);
                    if (hunter == null) return;
                    if ("deployed".equals(hunter.status)) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cCannot dismiss a deployed hunter!"));
                        return;
                    }
                    guild.removeHunterFromAllTeams(hunterId);
                    guild.hunters.remove(hunter);
                    data.markDirty();
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§7Dismissed §f" + hunter.name + "§7."));
                    reopenScreen(player, level, msg.computerPos);
                }

                case "refresh_pool" -> {
                    // Costs 8 gold ingots, regenerates the recruit pool
                    GuildData guild = data.getGuildForPlayer(playerUUID);
                    if (guild == null || !guild.canOperate(playerUUID)) return;

                    int cost = 8;
                    ItemStack gold = new ItemStack(Items.GOLD_INGOT);
                    if (!hasEnoughItems(player, gold, cost)) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cNeed §e8 Gold Ingots §cto refresh the recruit pool."));
                        return;
                    }
                    removeItems(player, gold, cost);
                    HunterRecruitManager.fillPool(guild);
                    data.markDirty();
                    reopenScreen(player, level, msg.computerPos);
                }

                case "assign_hunter" -> {
                    // param1 = teamId, param2 = hunterId
                    GuildData guild = data.getGuildForPlayer(playerUUID);
                    if (guild == null || !guild.canOperate(playerUUID)) return;
                    guild.pruneTeamMembers();

                    UUID teamId   = UUID.fromString(msg.param1);
                    UUID hunterId = UUID.fromString(msg.param2);
                    GuildTeam team = guild.getTeam(teamId);
                    if (team == null) return;
                    if (guild.getDeploymentForTeam(teamId) != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "Â§cCannot modify a deployed team!"));
                        return;
                    }
                    if (team.memberIds.size() >= GuildTeam.MAX_SIZE) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cTeam is full! (max " + GuildTeam.MAX_SIZE + " hunters)"));
                        return;
                    }
                    // Must be a hired hunter
                    if (guild.getHunter(hunterId) == null) return;

                    guild.removeHunterFromAllTeams(hunterId);
                    team.memberIds.add(hunterId);
                    data.markDirty();
                    reopenScreen(player, level, msg.computerPos);
                }

                case "remove_from_team" -> {
                    // param1 = teamId, param2 = hunterId
                    GuildData guild = data.getGuildForPlayer(playerUUID);
                    if (guild == null || !guild.canOperate(playerUUID)) return;
                    guild.pruneTeamMembers();

                    UUID teamId   = UUID.fromString(msg.param1);
                    UUID hunterId = UUID.fromString(msg.param2);
                    GuildTeam team = guild.getTeam(teamId);
                    if (team == null) return;

                    // Cannot remove from a deployed team
                    if (guild.getDeploymentForTeam(teamId) != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cCannot modify a deployed team!"));
                        return;
                    }

                    guild.removeHunterFromAllTeams(hunterId);
                    data.markDirty();
                    reopenScreen(player, level, msg.computerPos);
                }

                case "set_team_auto_raid" -> {
                    // param1 = teamId, param2 = "enabled:maxRank"
                    GuildData guild = data.getGuildForPlayer(playerUUID);
                    if (guild == null || !guild.canOperate(playerUUID)) return;
                    guild.pruneTeamMembers();

                    UUID teamId = UUID.fromString(msg.param1);
                    GuildTeam team = guild.getTeam(teamId);
                    if (team == null) return;

                    String[] parts = msg.param2.split(":");
                    if (parts.length != 2) return;
                    int maxRank;
                    try {
                        maxRank = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        return;
                    }
                    team.autoRaidEnabled = Boolean.parseBoolean(parts[0]);
                    team.autoRaidMaxRank = Math.max(1, Math.min(6, maxRank));
                    data.markDirty();
                    reopenScreen(player, level, msg.computerPos);
                }

                case "deploy_team" -> {
                    // param1 = teamId, param2 = gateKey ("sim:1"–"sim:6" or real gate UUID)
                    GuildData guild = data.getGuildForPlayer(playerUUID);
                    if (guild == null || !guild.canOperate(playerUUID)) return;
                    guild.pruneTeamMembers();

                    UUID teamId = UUID.fromString(msg.param1);
                    GuildTeam team = guild.getTeam(teamId);
                    if (team == null || team.memberIds.isEmpty()) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cTeam has no members!"));
                        return;
                    }
                    // Team already deployed?
                    if (guild.getDeploymentForTeam(teamId) != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§cTeam is already on a mission!"));
                        return;
                    }
                    // All team members must be idle hunters
                    for (UUID hid : team.memberIds) {
                        GuildHunter h = guild.getHunter(hid);
                        if (h == null || "deployed".equals(h.status)) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§cAll hunters must be idle before deploying!"));
                            return;
                        }
                    }

                    // Resolve gate info from param2
                    String gateLabel;
                    int    gateRank;
                    String gateEntityUUID = "";

                    if (msg.param2.startsWith("sim:")) {
                        // Simulated mission
                        try { gateRank = Integer.parseInt(msg.param2.substring(4)); }
                        catch (NumberFormatException e) { return; }
                        gateRank  = Math.max(1, Math.min(6, gateRank));
                        gateLabel = GuildDeployment.rankLabel(gateRank) + "-Rank Mission";
                    } else {
                        // Real gate entity
                        Entity gate;
                        try {
                            gateEntityUUID = msg.param2;
                            gate = GuildGateHelper.findGate(level, UUID.fromString(gateEntityUUID));
                        } catch (IllegalArgumentException e) { return; }
                        if (gate != null) {
                            GuildData raidingGuild = GuildGateHelper.findGuildRaidingGate(data, gateEntityUUID);
                            if (raidingGuild != null) {
                                player.sendSystemMessage(Component.literal("§cThis gate has already been bought by §e"
                                        + raidingGuild.name + " §cguild and is currently being raided."));
                                return;
                            }
                            if (GuildGateHelper.isGateReserved(gate)) {
                                player.sendSystemMessage(Component.literal("§cThis gate has already been bought by §e"
                                        + GuildGateHelper.reservedGuildName(gate) + " §cguild and is currently being raided."));
                                return;
                            }
                            if (GuildGateHelper.isGateInteracted(gate)) {
                                player.sendSystemMessage(Component.literal("§cThat gate has already been interacted with."));
                                return;
                            }
                        }
                        if (gate == null) {
                            player.sendSystemMessage(Component.literal("§cThat gate is no longer available."));
                            return;
                        }
                        for (GuildDeployment d : guild.deployments) {
                            if (gateEntityUUID.equals(d.gateEntityUUID)) {
                                player.sendSystemMessage(Component.literal("§cAnother team is already assigned to that gate."));
                                return;
                            }
                        }
                        gateRank = GuildGateHelper.gateRank(gate);
                        gateLabel = GuildGateHelper.gateLabel(gate);
                    }

                    // Create the deployment
                    long now      = level.getGameTime();
                    long duration = GuildDeployment.durationTicks(gateRank);
                    long xp       = GuildDeployment.xpForRank(gateRank);
                    GuildDeployment dep = new GuildDeployment(
                            UUID.randomUUID(), teamId, team.name,
                            gateLabel, gateRank, gateEntityUUID,
                            now + duration, xp);
                    guild.deployments.add(dep);

                    // Mark team hunters as deployed
                    for (UUID hid : team.memberIds) {
                        GuildHunter h = guild.getHunter(hid);
                        if (h != null) h.status = "deployed";
                    }
                    if (!gateEntityUUID.isEmpty()) {
                        try {
                            Entity gate = GuildGateHelper.findGate(level, UUID.fromString(gateEntityUUID));
                            GuildGateHelper.reserveGate(gate, guild);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    data.markDirty();
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§aTeam §e" + team.name + " §adeployed to §f" + gateLabel + "§a!"));
                    reopenScreen(player, level, msg.computerPos);
                }

                case "recall_team" -> {
                    // param1 = deploymentId
                    GuildData guild = data.getGuildForPlayer(playerUUID);
                    if (guild == null || !guild.canOperate(playerUUID)) return;

                    UUID depId = UUID.fromString(msg.param1);
                    GuildDeployment dep = null;
                    for (GuildDeployment d : guild.deployments) {
                        if (d.id.equals(depId)) { dep = d; break; }
                    }
                    if (dep == null) return;

                    // Set team hunters back to idle
                    GuildTeam team = guild.getTeam(dep.teamId);
                    if (team != null) {
                        for (UUID hid : team.memberIds) {
                            GuildHunter h = guild.getHunter(hid);
                            if (h != null) h.status = "idle";
                        }
                    }
                    if (!dep.gateEntityUUID.isEmpty()) {
                        try {
                            Entity gate = GuildGateHelper.findGate(level, UUID.fromString(dep.gateEntityUUID));
                            GuildGateHelper.clearReservation(gate);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    guild.deployments.remove(dep);
                    data.markDirty();
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§7Team recalled (no reward)."));
                    reopenScreen(player, level, msg.computerPos);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void reopenScreen(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof GuildComputerBlockEntity computer) {
            NetworkHooks.openScreen(player, computer,
                    buf -> computer.writeScreenOpeningData(player, buf));
        }
    }

    // ── Item helpers ──────────────────────────────────────────────────────────

    private static ItemStack getCostItem(String rank) {
        return switch (rank) {
            case "E" -> new ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
            case "D" -> new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT);
            default  -> new ItemStack(net.minecraft.world.item.Items.DIAMOND);
        };
    }

    private static boolean hasEnoughItems(ServerPlayer player, ItemStack template, int count) {
        return player.getInventory().countItem(template.getItem()) >= count;
    }

    private static void removeItems(ServerPlayer player, ItemStack template, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.getItem() == template.getItem()) {
                int take = Math.min(slot.getCount(), remaining);
                slot.shrink(take);
                remaining -= take;
            }
        }
    }
}
