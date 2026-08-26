package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Evaluation reminder system.
 *
 * Class assignment is owned by the Hunter Evaluator. This manager retains its
 * old class tables for save and test compatibility, but normal leveling now
 * only reminds unclassified players to visit an evaluator:
 *
 *   Speed + Perception  →  Assassin  (1)
 *   Strength + Intel    →  Mage      (2)
 *   Speed + Strength    →  Fighter   (3)
 *   Strength + Vitality →  Tanker    (4)
 *   Vitality + Intel    →  Healer    (5)
 *   Perception + Vit    →  Ranger    (6)
 *
 * Each class is scored as the sum of its two key stats; the highest scorer wins.
 *
 * Story Mode remains free to assign its scripted class directly.
 */
@EventBusSubscriber
public final class StatAwakeningManager {

    private StatAwakeningManager() {}

    // ── Thresholds ────────────────────────────────────────────────────────────

    /** Level at which the early-warning message fires. */
    private static final int  WARN_LEVEL   = 18;
    /** Level at which the stronger evaluation reminder fires. */
    private static final int  AWAKEN_LEVEL = 20;
    /** Minimum combined stat score required; prevents awakening on fresh characters. */
    private static final double MIN_SCORE  = 5.0;

    // ── PersistentData keys ───────────────────────────────────────────────────

    private static final String KEY_WARNED   = "sl_awaken_warned";
    private static final String KEY_AWAKENED = "sl_awakened";
    private static final String KEY_EVALUATION_REMINDER =
            "sl_evaluation_reminder";

    // ── Tick handler ──────────────────────────────────────────────────────────

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        // Only check every 100 ticks (~5 s) to keep overhead minimal
        if (sp.tickCount % 100 != 0) return;

        CompoundTag data = sp.getPersistentData();

        // Nothing left to do once the full awakening has fired
        if (data.getBoolean(KEY_AWAKENED)) return;

        var cap = sp.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new SololevelingModVariables.PlayerVariables());

        // If a class was already assigned (through evaluation or earlier self-awaken),
        // just mark it done so we stop checking.
        if (cap.Classes != 0) {
            data.putBoolean(KEY_AWAKENED, true);
            return;
        }

        int level = (int) cap.Level;

        // ── Early reminder at level 18 ────────────────────────────────────────
        if (level >= WARN_LEVEL && !data.getBoolean(KEY_WARNED)) {
            data.putBoolean(KEY_WARNED, true);
            sp.displayClientMessage(Component.literal(
                "§6§l⚠ Hunter Evaluation Available ⚠\n" +
                "§eVisit a Hunter Evaluator to discover your rank and class.\n" +
                "§7Rank is measured automatically; class results may be rerolled."),
                false);
            SystemNotifications.showTitleUnder(sp, 0xFFFFB83D, 110,
                Component.literal("EVALUATION AVAILABLE")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("Place your hand on an Evaluator crystal.")
                        .withStyle(ChatFormatting.GRAY));
        }

        // Class assignment no longer happens automatically at level 20.
        if (level >= AWAKEN_LEVEL
                && !data.getBoolean(KEY_EVALUATION_REMINDER)) {
            data.putBoolean(KEY_EVALUATION_REMINDER, true);
            sp.displayClientMessage(Component.literal(
                    "§bThe Hunter Association is waiting to evaluate you. " +
                    "Right-click an Evaluator crystal when you are ready."),
                    false);
        }
    }

    // ── Class computation ─────────────────────────────────────────────────────

    /**
     * Returns the class ID (1-6) determined by the player's top stat pair, or 0
     * if no combination clears the minimum score threshold.
     */
    private static int computeClass(SololevelingModVariables.PlayerVariables cap) {
        double spd  = cap.Speed;
        double str  = cap.Strength;
        double vit  = cap.Vitality;
        double intel = cap.Intelligence;
        double per  = cap.perception;

        // Score = sum of the two key stats for each class
        double[] scores = {
            spd  + per,   // 1 = Assassin
            str  + intel, // 2 = Mage
            spd  + str,   // 3 = Fighter
            str  + vit,   // 4 = Tanker
            vit  + intel, // 5 = Healer
            per  + vit,   // 6 = Ranger
        };

        int    best      = 0;
        double bestScore = MIN_SCORE;   // must beat this threshold
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > bestScore) {
                bestScore = scores[i];
                best      = i + 1;
            }
        }
        return best;
    }

    // ── Awakening execution ───────────────────────────────────────────────────

    private static void triggerAwakening(ServerPlayer sp, int classNum) {
        String name  = CLASS_NAMES[classNum - 1];
        String color = CLASS_COLORS[classNum - 1];
        int accent = CLASS_ACCENTS[classNum - 1];

        // 1. Assign class
        sp.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
          .ifPresent(cap -> {
              cap.Classes = classNum;
              cap.syncPlayerVariables(sp);
          });

        // 2. Grant starter skills for the class
        grantStarterSkills(sp, classNum);
		if (classNum == 2) {
			name = MageSpellProgression.displayName(sp);
			boolean barrier = MageSpellProgression.isBarrierMage(sp);
			boolean arcane = MageSpellProgression.isArcaneMage(sp);
			boolean storm = MageSpellProgression.isStormMage(sp);
			boolean curse = MageSpellProgression.isCurseMage(sp);
			color = barrier ? "aqua" : arcane ? "light_purple" : storm ? "yellow"
					: curse ? "dark_purple" : "red";
			accent = barrier ? 0xFF5CE8FF : arcane ? 0xFF8A5CFF
					: storm ? 0xFFFFD45A : curse ? 0xFFA05CFF : 0xFFFF5A2A;
		}

        SystemNotifications.showTitleUnder(sp, accent, 140,
            Component.literal("STAT AWAKENING").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
            Component.literal("Your dominant stats awakened " + name + ".\nStarter skills have been unlocked.").withStyle(ChatFormatting.GRAY));

        // 3. Title — "✦ AWAKENING ✦" then class name subtitle
        if (sp.getServer() != null && sp.level() instanceof ServerLevel sl) {
            CommandSourceStack css = new CommandSourceStack(
                    CommandSource.NULL,
                    sp.position(),
                    sp.getRotationVector(),
                    sl, 4,
                    sp.getName().getString(),
                    sp.getDisplayName(),
                    sp.getServer(), sp);

            sp.getServer().getCommands().performPrefixedCommand(css,
                "/title " + sp.getName().getString() +
                " title {\"text\":\"\\u2726 AWAKENING \\u2726\"," +
                "\"color\":\"gold\",\"bold\":true}");

            sp.getServer().getCommands().performPrefixedCommand(css,
                "/title " + sp.getName().getString() +
                " subtitle {\"text\":\"" + name + "\",\"color\":\"" + color + "\"," +
                "\"bold\":true,\"italic\":true}");
        }

        // 4. Chat feedback
        sp.displayClientMessage(Component.literal(
            "§6§l⚡ Stat Awakening ⚡\n" +
            "§fYour dominant stats have awakened you as a §e§l" + name + "§f!\n" +
            "§7Starter skills have been added to your Plist.\n" +
            "§7Seek formal §bEvaluation§7 to unlock higher-tier skills."), false);
    }

    // ── Starter skill grants ──────────────────────────────────────────────────

    /**
     * Adds the base skills for the awakened class. These are the same skills
     * a low-rank Evaluation would grant; higher-tier skills remain locked behind
     * HunterRank progression, so formal evaluation retains its value.
     *
     * Skills not yet in Plist are appended; duplicates are skipped.
     */
    private static void grantStarterSkills(ServerPlayer sp, int classNum) {
		if (classNum == 2) {
			MageSpellProgression.assignRandomSpecialization(sp);
			MageSpellProgression.grantStarterSpells(sp);
			return;
		}
        String[] skills = STARTER_SKILLS[classNum - 1];
        for (String skill : skills) {
            addSkillIfMissing(sp, skill);
        }
    }

    private static void addSkillIfMissing(ServerPlayer sp, String skill) {
        var cap = sp.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(null);
        if (cap == null) return;
        if (cap.Plist.contains(skill)) return; // already unlocked

        sp.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
          .ifPresent(c -> {
              c.Plist += skill + ",";
              c.syncPlayerVariables(sp);
          });
    }

    // ── Data tables ───────────────────────────────────────────────────────────

    /** Class names indexed by (classId - 1). */
    private static final String[] CLASS_NAMES = {
        "Assassin", "Mage", "Fighter", "Tanker", "Healer", "Ranger"
    };

    /**
     * Title subtitle colours per class (Minecraft JSON colour names or hex).
     * Matches the colours used by Hunter Evaluation.
     */
    private static final String[] CLASS_COLORS = {
        "#009DFF",  // Assassin — blue
        "#BF5FFF",  // Mage     — purple
        "#D8D8D8",  // Fighter  — silver
        "#3060FF",  // Tanker   — deep blue
        "#06FF00",  // Healer   — green
        "#FFBC00",  // Ranger   — amber
    };

    /**
     * Starter skills per class, indexed by (classId - 1).
     * These are the fundamental skills that do not require a runestone.
     */
    private static final int[] CLASS_ACCENTS = {
        0xFF009DFF,  // Assassin
        0xFFBF5FFF,  // Mage
        0xFFD8D8D8,  // Fighter
        0xFF3060FF,  // Tanker
        0xFF06FF00,  // Healer
        0xFFFFBC00,  // Ranger
    };

    private static final String[][] STARTER_SKILLS = {
        // 1 Assassin
        { AssassinSkillManager.GHOST_STEP, AssassinSkillManager.NIGHT_REND },
        // 2 Mage
        { FireMageSpellManager.FLAME_WEAVING, FireMageSpellManager.IGNITION_ORB },
        // 3 Fighter
        { "Slash Dash", "Ground Slam" },
        // 4 Tanker
        { "Tank Leap", "Shield Bash" },
        // 5 Healer
        { "Heal Beam", "Purification" },
        // 6 Ranger
        { RangerCombatManager.MANA_QUIVER, RangerCombatManager.BACK_STEP },
    };
}
