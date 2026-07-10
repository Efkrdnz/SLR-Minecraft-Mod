package net.solocraft.guild;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * A virtual NPC hunter that belongs to a guild.
 * Not a real entity — lives entirely as saved data in GuildData.
 */
public class GuildHunter {

    public UUID   id;
    public String name;
    public String rank;        // "E" "D" "C" "B" "A" "S"
    public String hunterClass; // "Assassin" "Fighter" "Tanker" "Mage" "Ranger" "Healer"
    public String status;      // "idle" | "deployed" | "recovering"

    public GuildHunter(UUID id, String name, String rank, String hunterClass) {
        this.id          = id;
        this.name        = name;
        this.rank        = rank;
        this.hunterClass = hunterClass;
        this.status      = "idle";
    }

    // ── Rank helpers ──────────────────────────────────────────────────────────

    /** Numeric score used in success-rate maths (E=1 … S=6). */
    public int rankScore() {
        return switch (rank) {
            case "E" -> 1; case "D" -> 2; case "C" -> 3;
            case "B" -> 4; case "A" -> 5; case "S" -> 6;
            default  -> 0;
        };
    }

    public static String rankColor(String rank) {
        return switch (rank) {
            case "E" -> "§7";
            case "D" -> "§f";
            case "C" -> "§a";
            case "B" -> "§b";
            case "A" -> "§e";
            case "S" -> "§6";
            default  -> "§7";
        };
    }

    public static String classColor(String cls) {
        return switch (cls) {
            case "Assassin" -> "§5";
            case "Fighter"  -> "§c";
            case "Tanker"   -> "§6";
            case "Mage"     -> "§9";
            case "Ranger"   -> "§a";
            case "Healer"   -> "§d";
            default         -> "§7";
        };
    }

    // ── Hire cost ─────────────────────────────────────────────────────────────

    /** Minecraft item ID of the material used to hire this rank. */
    public static String hireMaterialId(String rank) {
        return switch (rank) {
            case "E" -> "minecraft:iron_ingot";
            case "D" -> "minecraft:gold_ingot";
            default  -> "minecraft:diamond"; // C B A S
        };
    }

    public static String hireMaterialName(String rank) {
        return switch (rank) {
            case "E" -> "Iron";
            case "D" -> "Gold";
            default  -> "Diamond";
        };
    }

    public static int hireCost(String rank) {
        return switch (rank) {
            case "E" -> 8;
            case "D" -> 8;
            case "C" -> 4;
            case "B" -> 8;
            case "A" -> 16;
            case "S" -> 32;
            default  -> 0;
        };
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id",     id);
        tag.putString("name", name);
        tag.putString("rank", rank);
        tag.putString("class",  hunterClass);
        tag.putString("status", status);
        return tag;
    }

    public static GuildHunter load(CompoundTag tag) {
        GuildHunter h = new GuildHunter(
                tag.getUUID("id"),
                tag.getString("name"),
                tag.getString("rank"),
                tag.getString("class"));
        h.status = tag.contains("status") ? tag.getString("status") : "idle";
        return h;
    }
}
