package net.solocraft.guild;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * A single hunter-team deployment to a gate.
 * "Virtual" — no entity movement; the result is calculated when the timer expires.
 */
public class GuildDeployment {

    public UUID   id;
    public UUID   teamId;
    public String teamName;       // cached for notifications
    public String gateLabel;      // display label e.g. "E-Rank Gate @(120, 64)"
    public int    gateRank;       // 1=E … 6=S
    /** UUID of the real gate entity (empty string for simulated missions). */
    public String gateEntityUUID = "";
    /** Server game-time tick when this deployment finishes. */
    public long   completesAt;
    /** XP awarded to the guild on success. */
    public long   xpReward;

    public GuildDeployment(UUID id, UUID teamId, String teamName,
                           String gateLabel, int gateRank,
                           String gateEntityUUID,
                           long completesAt, long xpReward) {
        this.id             = id;
        this.teamId         = teamId;
        this.teamName       = teamName;
        this.gateLabel      = gateLabel;
        this.gateRank       = gateRank;
        this.gateEntityUUID = gateEntityUUID;
        this.completesAt    = completesAt;
        this.xpReward       = xpReward;
    }

    public long ticksRemaining(long currentGameTime) {
        return completesAt - currentGameTime;
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    public static String rankLabel(int rank) {
        return switch (rank) {
            case 1 -> "E"; case 2 -> "D"; case 3 -> "C";
            case 4 -> "B"; case 5 -> "A"; case 6 -> "S";
            default -> "E";
        };
    }

    public static String rankColor(int rank) {
        return switch (rank) {
            case 1 -> "§7"; case 2 -> "§f"; case 3 -> "§a";
            case 4 -> "§b"; case 5 -> "§e"; case 6 -> "§6";
            default -> "§7";
        };
    }

    /** Mission duration in ticks (scales with gate rank). */
    public static long durationTicks(int rank) {
        return switch (rank) {
            case 1 ->  3600L;   // E = 3 min
            case 2 ->  6000L;   // D = 5 min
            case 3 ->  9600L;   // C = 8 min
            case 4 -> 14400L;   // B = 12 min
            case 5 -> 24000L;   // A = 20 min
            case 6 -> 36000L;   // S = 30 min
            default -> 3600L;
        };
    }

    /** Base XP reward for clearing a gate of the given rank. */
    public static long xpForRank(int rank) {
        return switch (rank) {
            case 1 ->    200L;  // E
            case 2 ->    500L;  // D
            case 3 ->   1200L;  // C
            case 4 ->   3000L;  // B
            case 5 ->   8000L;  // A
            case 6 ->  20000L;  // S
            default ->   200L;
        };
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id",             id);
        tag.putUUID("teamId",         teamId);
        tag.putString("teamName",     teamName);
        tag.putString("gateLabel",    gateLabel);
        tag.putInt("gateRank",        gateRank);
        tag.putString("gateEntityUUID", gateEntityUUID);
        tag.putLong("completesAt",    completesAt);
        tag.putLong("xpReward",       xpReward);
        return tag;
    }

    public static GuildDeployment load(CompoundTag tag) {
        return new GuildDeployment(
                tag.getUUID("id"),
                tag.getUUID("teamId"),
                tag.contains("teamName") ? tag.getString("teamName") : "?",
                tag.getString("gateLabel"),
                tag.getInt("gateRank"),
                tag.contains("gateEntityUUID") ? tag.getString("gateEntityUUID") : "",
                tag.getLong("completesAt"),
                tag.getLong("xpReward"));
    }
}
