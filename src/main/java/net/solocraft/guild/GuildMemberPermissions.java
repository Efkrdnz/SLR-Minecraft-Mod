package net.solocraft.guild;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Per-player permission record stored inside GuildData.
 * All flags default to false — the owner manually grants access.
 */
public class GuildMemberPermissions {

    public UUID playerUUID;
    public String playerName; // cached display name

    // Can this player open the computer at all?
    public boolean canOpen      = false;

    // Which tabs can they see?
    public boolean tabOverview    = false;
    public boolean tabRoster      = false;
    public boolean tabTeams       = false;
    public boolean tabDungeons    = false;
    public boolean tabStorage     = false;
    public boolean tabBuffs       = false;
    public boolean tabLeaderboard = false;
    // tabManagement is always owner-only — not stored here

    public GuildMemberPermissions(UUID playerUUID, String playerName) {
        this.playerUUID  = playerUUID;
        this.playerName  = playerName;
    }

    public void setAll(boolean value) {
        canOpen = value;
        tabOverview = value;
        tabRoster = value;
        tabTeams = value;
        tabDungeons = value;
        tabStorage = value;
        tabBuffs = value;
        tabLeaderboard = value;
    }

    // ── Serialisation ─────────────────────────────────────────────────────────

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", playerUUID);
        tag.putString("name", playerName);
        tag.putBoolean("canOpen",        canOpen);
        tag.putBoolean("tabOverview",    tabOverview);
        tag.putBoolean("tabRoster",      tabRoster);
        tag.putBoolean("tabTeams",       tabTeams);
        tag.putBoolean("tabDungeons",    tabDungeons);
        tag.putBoolean("tabStorage",     tabStorage);
        tag.putBoolean("tabBuffs",       tabBuffs);
        tag.putBoolean("tabLeaderboard", tabLeaderboard);
        return tag;
    }

    public static GuildMemberPermissions load(CompoundTag tag) {
        GuildMemberPermissions p = new GuildMemberPermissions(
                tag.getUUID("uuid"),
                tag.getString("name"));
        p.canOpen        = tag.getBoolean("canOpen");
        p.tabOverview    = tag.getBoolean("tabOverview");
        p.tabRoster      = tag.getBoolean("tabRoster");
        p.tabTeams       = tag.getBoolean("tabTeams");
        p.tabDungeons    = tag.getBoolean("tabDungeons");
        p.tabStorage     = tag.getBoolean("tabStorage");
        p.tabBuffs       = tag.getBoolean("tabBuffs");
        p.tabLeaderboard = tag.getBoolean("tabLeaderboard");
        return p;
    }

    // ── Write into a flat buffer (used in menu FriendlyByteBuf) ──────────────

    public void writeToTag(CompoundTag out, int index) {
        out.put("perm_" + index, save());
    }

    public static GuildMemberPermissions readFromTag(CompoundTag in, int index) {
        return load(in.getCompound("perm_" + index));
    }
}
