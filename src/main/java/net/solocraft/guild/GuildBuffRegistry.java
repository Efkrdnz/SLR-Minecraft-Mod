package net.solocraft.guild;

import java.util.List;

public final class GuildBuffRegistry {
    public static final int NONE = 0;
    public static final int PHYSICAL_CONDITIONING = 1;
    public static final int ARCANE_CIRCULATION = 2;
    public static final int DEFENSIVE_FORMATION = 3;
    public static final int ASSASSIN_FOOTWORK = 4;
    public static final int HUNTERS_PRECISION = 5;
    public static final int COMBAT_RECOVERY = 6;
    public static final int MANA_EFFICIENCY = 7;
    public static final int BATTLE_RHYTHM = 8;
    public static final int SYSTEM_SPONSORSHIP = 9;

    private static final List<GuildBuff> BUFFS = List.of(
            new GuildBuff(PHYSICAL_CONDITIONING, "Physical Conditioning", 2, "+15% physical damage."),
            new GuildBuff(ARCANE_CIRCULATION, "Arcane Circulation", 3, "+10% magic damage and +10% mana recovery speed."),
            new GuildBuff(DEFENSIVE_FORMATION, "Defensive Formation", 4, "+15% damage reduction while blocking. +8% damage reduction briefly after being hit."),
            new GuildBuff(ASSASSIN_FOOTWORK, "Assassin Footwork", 5, "+12% movement speed in combat and 10% chance to dodge incoming damage."),
            new GuildBuff(HUNTERS_PRECISION, "Hunter's Precision", 6, "+15% critical damage and 5% chance for a precision critical hit."),
            new GuildBuff(COMBAT_RECOVERY, "Combat Recovery", 7, "+20% healing received and restores mana on kills."),
            new GuildBuff(MANA_EFFICIENCY, "Mana Efficiency", 8, "Refunds 25% of mana spent."),
            new GuildBuff(BATTLE_RHYTHM, "Battle Rhythm", 9, "+12% damage while chaining class-skill hits."),
            new GuildBuff(SYSTEM_SPONSORSHIP, "System Sponsorship", 10, "+15% XP gained from the System.")
    );

    private GuildBuffRegistry() {
    }

    public static List<GuildBuff> all() {
        return BUFFS;
    }

    public static GuildBuff byId(int id) {
        for (GuildBuff buff : BUFFS) {
            if (buff.id() == id) return buff;
        }
        return null;
    }

    public static String displayName(int id) {
        GuildBuff buff = byId(id);
        return buff == null ? "None" : buff.name();
    }

    public static int unlockLevel(int id) {
        GuildBuff buff = byId(id);
        return buff == null ? 1 : buff.unlockLevel();
    }

    public static boolean isUnlocked(GuildData guild, int id) {
        return id == NONE || (byId(id) != null && guild.level >= unlockLevel(id));
    }

    public static boolean isSlotUnlocked(GuildData guild, int slot) {
        return slot == 1 || (slot == 2 && guild.level >= 10);
    }

    public record GuildBuff(int id, String name, int unlockLevel, String description) {
    }
}
