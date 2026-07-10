package net.solocraft.guild;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A team of up to {@value #MAX_SIZE} hired hunters that can be deployed to gates.
 * Teams are created automatically (5 per guild) and persist in GuildData.
 */
public class GuildTeam {

    public UUID        id;
    public String      name;
    /** UUIDs of hired hunters assigned to this team (order preserved). */
    public List<UUID>  memberIds = new ArrayList<>();
    public boolean     autoRaidEnabled = false;
    /** Highest gate rank this team will automatically raid. 1=E ... 6=S. */
    public int         autoRaidMaxRank = 1;

    public static final int MAX_SIZE = 5;

    public GuildTeam(UUID id, String name) {
        this.id   = id;
        this.name = name;
    }

    public boolean hasHunter(UUID hunterId) {
        return memberIds.contains(hunterId);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id",   id);
        tag.putString("name", name);
        tag.putBoolean("autoRaidEnabled", autoRaidEnabled);
        tag.putInt("autoRaidMaxRank", autoRaidMaxRank);
        ListTag list = new ListTag();
        for (UUID uid : memberIds) list.add(StringTag.valueOf(uid.toString()));
        tag.put("members", list);
        return tag;
    }

    public static GuildTeam load(CompoundTag tag) {
        GuildTeam t = new GuildTeam(tag.getUUID("id"), tag.getString("name"));
        t.autoRaidEnabled = tag.getBoolean("autoRaidEnabled");
        t.autoRaidMaxRank = tag.contains("autoRaidMaxRank") ? Math.max(1, Math.min(6, tag.getInt("autoRaidMaxRank"))) : 1;
        ListTag list = tag.getList("members", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            t.memberIds.add(UUID.fromString(list.getString(i)));
        }
        return t;
    }
}
