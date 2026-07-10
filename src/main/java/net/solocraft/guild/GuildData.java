package net.solocraft.guild;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * All data for a single guild.
 * Stored inside GuildSavedData (WorldSavedData) — persists across restarts.
 */
public class GuildData {

    // ── Identity ──────────────────────────────────────────────────────────────
    public UUID   id;
    public String name;
    public UUID   ownerUUID;
    public String ownerName;

    // ── Progression ───────────────────────────────────────────────────────────
    public int  level = 1;
    public long xp    = 0;

    // ── Stats ─────────────────────────────────────────────────────────────────
    public int totalClears = 0;

    // ── Members & permissions ─────────────────────────────────────────────────
    public List<GuildMemberPermissions> memberPermissions = new ArrayList<>();

    // ── Hunters ───────────────────────────────────────────────────────────────
    /** Hunters the guild has hired (max 25 = 5 teams × 5). */
    public List<GuildHunter>   hunters     = new ArrayList<>();
    /** Candidates currently available to hire from the recruitment pool. */
    public List<GuildHunter>   recruitPool = new ArrayList<>();

    /** Max hunters the guild can hold regardless of level. */
    public static final int MAX_HUNTERS = 25;

    /** Number of candidates shown in the recruitment pool. */
    public int recruitPoolSize() { return Math.min(3 + level, 10); }

    // ── Teams ─────────────────────────────────────────────────────────────────
    /** Exactly 5 teams, auto-created on guild creation. */
    public List<GuildTeam>       teams       = new ArrayList<>();
    /** Active hunter deployments. Completed entries are removed by GuildTickHandler. */
    public List<GuildDeployment> deployments = new ArrayList<>();

    /** Shared 27-slot storage for every Guild Computer opened by this guild. */
    public NonNullList<ItemStack> storageItems = NonNullList.withSize(27, ItemStack.EMPTY);

    public int activeBuffSlot1 = GuildBuffRegistry.NONE;
    public int activeBuffSlot2 = GuildBuffRegistry.NONE;

    // ── Constructor ───────────────────────────────────────────────────────────

    public GuildData(UUID id, String name, UUID ownerUUID, String ownerName) {
        this.id        = id;
        this.name      = name;
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        initDefaultTeams();
    }

    private void initDefaultTeams() {
        String[] defaultNames = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon"};
        for (String tname : defaultNames) {
            teams.add(new GuildTeam(UUID.randomUUID(), tname));
        }
    }

    // ── Progression helpers ───────────────────────────────────────────────────

    /** Awards XP and handles automatic level-ups. */
    public void awardXp(long amount) {
        xp += amount;
        while (xp >= xpForLevel(level)) {
            xp -= xpForLevel(level);
            level++;
        }
    }

    /** XP needed to reach the NEXT level from the given level. */
    public static long xpForLevel(int level) {
        return (long) level * level * 500L;
    }

    // ── Lookup helpers ────────────────────────────────────────────────────────

    public GuildMemberPermissions getPermissions(UUID playerUUID) {
        for (GuildMemberPermissions p : memberPermissions) {
            if (p.playerUUID.equals(playerUUID)) return p;
        }
        return null;
    }

    public boolean canAccess(UUID playerUUID) {
        if (playerUUID.equals(ownerUUID)) return true;
        GuildMemberPermissions p = getPermissions(playerUUID);
        return p != null && p.canOpen;
    }

    public boolean canOperate(UUID playerUUID) {
        return playerUUID.equals(ownerUUID);
    }

    public boolean canAccessTab(UUID playerUUID, String tab) {
        if (playerUUID.equals(ownerUUID)) return true;
        GuildMemberPermissions p = getPermissions(playerUUID);
        if (p == null) return false;
        return switch (tab) {
            case "overview"    -> p.tabOverview;
            case "roster"      -> p.tabRoster;
            case "teams"       -> p.tabTeams;
            case "dungeons"    -> p.tabDungeons;
            case "storage"     -> p.tabStorage;
            case "buffs"       -> p.tabBuffs;
            case "leaderboard" -> p.tabLeaderboard;
            default            -> false;
        };
    }

    public GuildHunter getHunter(UUID hunterId) {
        for (GuildHunter h : hunters) {
            if (h.id.equals(hunterId)) return h;
        }
        return null;
    }

    public GuildHunter getRecruit(UUID recruitId) {
        for (GuildHunter h : recruitPool) {
            if (h.id.equals(recruitId)) return h;
        }
        return null;
    }

    public GuildTeam getTeam(UUID teamId) {
        for (GuildTeam t : teams) {
            if (t.id.equals(teamId)) return t;
        }
        return null;
    }

    /** Returns the team the given hunter belongs to, or null. */
    public GuildTeam getTeamForHunter(UUID hunterId) {
        for (GuildTeam t : teams) {
            if (t.memberIds.contains(hunterId)) return t;
        }
        return null;
    }

    public void removeHunterFromAllTeams(UUID hunterId) {
        for (GuildTeam t : teams) {
            t.memberIds.removeIf(id -> id.equals(hunterId));
        }
    }

    public void pruneTeamMembers() {
        for (GuildTeam t : teams) {
            java.util.Set<UUID> seen = new java.util.HashSet<>();
            t.memberIds.removeIf(id -> getHunter(id) == null || !seen.add(id));
        }
    }

    public void reconcileHunterDeploymentStatus() {
        java.util.Set<UUID> deployedHunters = new java.util.HashSet<>();
        deployments.removeIf(deployment -> getTeam(deployment.teamId) == null);
        for (GuildDeployment deployment : deployments) {
            GuildTeam team = getTeam(deployment.teamId);
            if (team == null) continue;
            for (UUID hunterId : team.memberIds) {
                if (getHunter(hunterId) != null) deployedHunters.add(hunterId);
            }
        }
        for (GuildHunter hunter : hunters) {
            if (deployedHunters.contains(hunter.id)) {
                hunter.status = "deployed";
            } else if ("deployed".equals(hunter.status)) {
                hunter.status = "idle";
            }
        }
    }

    /** Returns the active deployment for the given team, or null. */
    public GuildDeployment getDeploymentForTeam(UUID teamId) {
        for (GuildDeployment d : deployments) {
            if (d.teamId.equals(teamId)) return d;
        }
        return null;
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    public String getRankBadge() {
        return switch (level) {
            case 1  -> "§7[E]";
            case 2  -> "§7[D]";
            case 3  -> "§a[C]";
            case 4  -> "§b[B]";
            case 5  -> "§e[A]";
            default -> "§6[S]";
        };
    }

    // ── NBT serialisation ─────────────────────────────────────────────────────

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id",          id);
        tag.putString("name",      name);
        tag.putUUID("ownerUUID",   ownerUUID);
        tag.putString("ownerName", ownerName);
        tag.putInt("level",        level);
        tag.putLong("xp",          xp);
        tag.putInt("totalClears",  totalClears);
        tag.putInt("activeBuffSlot1", activeBuffSlot1);
        tag.putInt("activeBuffSlot2", activeBuffSlot2);

        // Member permissions
        ListTag permList = new ListTag();
        for (GuildMemberPermissions p : memberPermissions) permList.add(p.save());
        tag.put("members", permList);

        // Hired hunters
        ListTag hunterList = new ListTag();
        for (GuildHunter h : hunters) hunterList.add(h.save());
        tag.put("hunters", hunterList);

        // Recruit pool
        ListTag poolList = new ListTag();
        for (GuildHunter h : recruitPool) poolList.add(h.save());
        tag.put("recruitPool", poolList);

        // Teams
        ListTag teamList = new ListTag();
        for (GuildTeam t : teams) teamList.add(t.save());
        tag.put("teams", teamList);

        // Deployments
        ListTag depList = new ListTag();
        for (GuildDeployment d : deployments) depList.add(d.save());
        tag.put("deployments", depList);

        CompoundTag storageTag = new CompoundTag();
        ContainerHelper.saveAllItems(storageTag, storageItems);
        tag.put("storage", storageTag);

        return tag;
    }

    public static GuildData load(CompoundTag tag) {
        GuildData g = new GuildData(
                tag.getUUID("id"),
                tag.getString("name"),
                tag.getUUID("ownerUUID"),
                tag.getString("ownerName"));
        g.level       = tag.getInt("level");
        g.xp          = tag.getLong("xp");
        g.totalClears = tag.getInt("totalClears");
        g.activeBuffSlot1 = tag.getInt("activeBuffSlot1");
        g.activeBuffSlot2 = tag.getInt("activeBuffSlot2");

        // Clear auto-created default teams before loading
        g.teams.clear();

        ListTag permList = tag.getList("members", Tag.TAG_COMPOUND);
        for (int i = 0; i < permList.size(); i++)
            g.memberPermissions.add(GuildMemberPermissions.load(permList.getCompound(i)));

        ListTag hunterList = tag.getList("hunters", Tag.TAG_COMPOUND);
        for (int i = 0; i < hunterList.size(); i++)
            g.hunters.add(GuildHunter.load(hunterList.getCompound(i)));

        ListTag poolList = tag.getList("recruitPool", Tag.TAG_COMPOUND);
        for (int i = 0; i < poolList.size(); i++)
            g.recruitPool.add(GuildHunter.load(poolList.getCompound(i)));

        // Teams — if none saved (old guild), create defaults
        ListTag teamList = tag.getList("teams", Tag.TAG_COMPOUND);
        if (teamList.isEmpty()) {
            g.initDefaultTeams();
        } else {
            for (int i = 0; i < teamList.size(); i++)
                g.teams.add(GuildTeam.load(teamList.getCompound(i)));
        }

        // Deployments
        ListTag depList = tag.getList("deployments", Tag.TAG_COMPOUND);
        for (int i = 0; i < depList.size(); i++)
            g.deployments.add(GuildDeployment.load(depList.getCompound(i)));
        g.reconcileHunterDeploymentStatus();

        if (tag.contains("storage", Tag.TAG_COMPOUND)) {
            ContainerHelper.loadAllItems(tag.getCompound("storage"), g.storageItems);
        }

        return g;
    }
}
