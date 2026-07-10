package net.solocraft.guild;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Persists all guild data to the world's saved-data store.
 * Survives server restarts. Access via {@link #get(ServerLevel)}.
 */
public class GuildSavedData extends SavedData {

    private static final String DATA_NAME = "solocraft_guilds";

    /** All guilds keyed by their UUID. */
    private final Map<UUID, GuildData> guilds = new LinkedHashMap<>();

    // ── Static accessor ───────────────────────────────────────────────────────

    public static GuildSavedData get(ServerLevel level) {
        return level.getServer()
                    .overworld()
                    .getDataStorage()
                    .computeIfAbsent(GuildSavedData::load, GuildSavedData::new, DATA_NAME);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /** Creates a new guild and returns it. Returns null if the name is already taken. */
    public GuildData createGuild(String name, UUID ownerUUID, String ownerName) {
        for (GuildData g : guilds.values()) {
            if (g.name.equalsIgnoreCase(name)) return null; // name taken
        }
        UUID id = UUID.randomUUID();
        GuildData g = new GuildData(id, name, ownerUUID, ownerName);
        HunterRecruitManager.fillPool(g); // populate initial recruit pool
        guilds.put(id, g);
        setDirty();
        return g;
    }

    public GuildData getGuild(UUID id) {
        return guilds.get(id);
    }

    /** Returns the guild the given player owns, or null. */
    public GuildData getGuildByOwner(UUID playerUUID) {
        for (GuildData g : guilds.values()) {
            if (g.ownerUUID.equals(playerUUID)) return g;
        }
        return null;
    }

    /** Returns any guild the player is a member/owner of, or null. */
    public GuildData getGuildForPlayer(UUID playerUUID) {
        for (GuildData g : guilds.values()) {
            if (g.ownerUUID.equals(playerUUID)) return g;
            for (GuildMemberPermissions p : g.memberPermissions) {
                if (p.playerUUID.equals(playerUUID)) return g;
            }
        }
        return null;
    }

    /** Sorted by totalClears desc, then level desc, then xp desc. */
    public List<GuildData> getLeaderboard() {
        List<GuildData> list = new ArrayList<>(guilds.values());
        list.sort(Comparator
                .comparingInt((GuildData g) -> -g.totalClears)
                .thenComparingInt(g -> -g.level)
                .thenComparingLong(g -> -g.xp));
        return list;
    }

    public Collection<GuildData> allGuilds() {
        return guilds.values();
    }

    /** Permanently deletes a guild by id. Returns true if it existed. */
    public boolean deleteGuild(UUID guildId) {
        boolean removed = guilds.remove(guildId) != null;
        if (removed) setDirty();
        return removed;
    }

    /** Call after modifying any GuildData directly. */
    public void markDirty() {
        setDirty();
    }

    // ── SavedData overrides ───────────────────────────────────────────────────

    @Nonnull
    @Override
    public CompoundTag save(@Nonnull CompoundTag tag) {
        ListTag list = new ListTag();
        for (GuildData g : guilds.values()) {
            list.add(g.save());
        }
        tag.put("guilds", list);
        return tag;
    }

    private static GuildSavedData load(CompoundTag tag) {
        GuildSavedData data = new GuildSavedData();
        ListTag list = tag.getList("guilds", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            GuildData g = GuildData.load(list.getCompound(i));
            data.guilds.put(g.id, g);
        }
        return data;
    }
}
