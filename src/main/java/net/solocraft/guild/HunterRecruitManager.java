package net.solocraft.guild;

import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

import java.util.UUID;

/**
 * Generates randomised NPC hunter candidates for a guild's recruitment pool.
 * Higher guild level improves odds of seeing rarer ranks.
 */
public class HunterRecruitManager {

    private static final String[] CLASSES = {
        "Assassin", "Fighter", "Tanker", "Mage", "Ranger", "Healer"
    };

    private static final String[] NAMES_FIRST = {
        "Jin", "Kai", "Ryu", "Hana", "Sora", "Yuki", "Toma", "Nara",
        "Eren", "Mika", "Leon", "Zara", "Kira", "Dash", "Nova", "Axel",
        "Rio", "Vex", "Colt", "Sera", "Orion", "Lyra", "Blaze", "Storm",
        "Kael", "Vera", "Rex", "Mira", "Zion", "Aria", "Dusk", "Flint"
    };

    private static final String[] NAMES_LAST = {
        "Park", "Yun", "Cho", "Han", "Kim", "Lee", "Kang",
        "Cross", "Vale", "Stone", "Frost", "Ember", "Ash",
        "Ward", "Swift", "Iron", "Black", "Grey", "Silver",
        "Steele", "Quinn", "Drake", "Voss", "Chen", "Moon"
    };

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Clears and refills the guild's recruit pool.
     * Pool size = min(3 + guild.level, 10).
     */
    public static void fillPool(GuildData guild) {
        guild.recruitPool.clear();
        int size = Math.min(3 + guild.level, 10);
        for (int i = 0; i < size; i++) {
            guild.recruitPool.add(generate(guild.level));
        }
    }

    /** Generates a single hunter candidate scaled to the guild's level. */
    public static GuildHunter generate(int guildLevel) {
        RandomSource rng = RandomSource.create();
        String rank = rollRank(guildLevel, rng);
        String cls  = CLASSES[Mth.nextInt(rng, 0, CLASSES.length - 1)];
        String firstName = NAMES_FIRST[Mth.nextInt(rng, 0, NAMES_FIRST.length - 1)];
        String lastName  = NAMES_LAST [Mth.nextInt(rng, 0, NAMES_LAST.length  - 1)];
        return new GuildHunter(UUID.randomUUID(), firstName + " " + lastName, rank, cls);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Rolls a rank using cumulative probabilities.
     * Base at guild level 1: E≈40%, D≈30%, C≈18%, B≈8%, A≈3%, S≈1%.
     * Each level shifts 2% from E/D toward higher ranks.
     */
    private static String rollRank(int guildLevel, RandomSource rng) {
        double roll = rng.nextDouble();
        int lv = Math.max(0, Math.min(guildLevel - 1, 9)); // clamp 0-9

        double sChance = 0.010 + lv * 0.003;
        double aChance = sChance + 0.030 + lv * 0.005;
        double bChance = aChance + 0.080 + lv * 0.008;
        double cChance = bChance + 0.180 + lv * 0.010;
        double dChance = cChance + 0.300 - lv * 0.010;

        if (roll < sChance) return "S";
        if (roll < aChance) return "A";
        if (roll < bChance) return "B";
        if (roll < cChance) return "C";
        if (roll < dChance) return "D";
        return "E";
    }
}
