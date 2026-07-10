package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Automatic class awakening system.
 *
 * If a player reaches level {@value #AWAKEN_LEVEL} without selecting a class
 * through the formal evaluation, their two dominant stats determine their class:
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
 * At level {@value #WARN_LEVEL} the player receives an early warning so they can
 * consciously invest stats in the class direction they want.
 *
 * On awakening the player receives two starter skills for their class.
 * Higher-tier skills are still gated behind the formal evaluation (HunterRank),
 * so there is a meaningful incentive to evaluate rather than just self-awaken.
 */
@Mod.EventBusSubscriber
public final class StatAwakeningManager {

    private StatAwakeningManager() {}

    // ── Thresholds ────────────────────────────────────────────────────────────

    /** Level at which the early-warning message fires. */
    private static final int  WARN_LEVEL   = 18;
    /** Level at which the actual awakening fires. */
    private static final int  AWAKEN_LEVEL = 20;
    /** Minimum combined stat score required; prevents awakening on fresh characters. */
    private static final double MIN_SCORE  = 5.0;

    // ── PersistentData keys ───────────────────────────────────────────────────

    private static final String KEY_WARNED   = "sl_awaken_warned";
    private static final String KEY_AWAKENED = "sl_awakened";

    // ── Tick handler ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

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

        // ── Early warning at level 18 ─────────────────────────────────────────
        if (level >= WARN_LEVEL && !data.getBoolean(KEY_WARNED)) {
            data.putBoolean(KEY_WARNED, true);
            int hint = computeClass(cap); // current leading class
            String hintName = hint > 0 ? CLASS_NAMES[hint - 1] : "Unknown";
            sp.displayClientMessage(Component.literal(
                "§6§l⚠ Awakening Approaches ⚠\n" +
                "§eYour strength suggests a §b" + hintName + " §eawakening at level " +
                AWAKEN_LEVEL + ".\n§7Tip: Invest stats to steer your destiny — " +
                "or seek formal Evaluation for greater power."), false);
            SystemNotifications.showTitleUnder(sp, 0xFFFFB83D, 110,
                Component.literal("AWAKENING APPROACHES").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("Your stats suggest " + hintName + ".\nReach Lv " + AWAKEN_LEVEL + " to awaken.").withStyle(ChatFormatting.GRAY));
        }

        // ── Awakening at level 20 ─────────────────────────────────────────────
        if (level >= AWAKEN_LEVEL) {
            int cls = computeClass(cap);
            if (cls == 0) return; // stats too low / tied — wait

            triggerAwakening(sp, cls);
            data.putBoolean(KEY_AWAKENED, true);
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

        // 1. Assign class
        sp.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
          .ifPresent(cap -> {
              cap.Classes = classNum;
              cap.syncPlayerVariables(sp);
          });

        // 2. Grant starter skills for the class
        grantStarterSkills(sp, classNum);

        SystemNotifications.showTitleUnder(sp, CLASS_ACCENTS[classNum - 1], 140,
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
     * Adds two base skills for the awakened class.  These are the same skills
     * a low-rank Evaluation would grant; higher-tier skills remain locked behind
     * HunterRank progression, so formal evaluation retains its value.
     *
     * Skills not yet in Plist are appended; duplicates are skipped.
     */
    private static void grantStarterSkills(ServerPlayer sp, int classNum) {
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
     * Matches the colours used in EvaluationTimerProcedure.
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
     * Two starter skills per class, indexed by (classId - 1).
     * These are the most fundamental skills — always available regardless of rank.
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
        { "Shadowstep", "Backstab" },
        // 2 Mage
        { "Fireball", "Water Slash" },
        // 3 Fighter
        { "Slash Dash", "Ground Slam" },
        // 4 Tanker
        { "Tank Leap", "Shield Bash" },
        // 5 Healer
        { "Heal Beam", "Purification" },
        // 6 Ranger
        { "Back Step", "Sharpshooter" },
    };
}
