package net.solocraft.guild;

import net.solocraft.init.SololevelingModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Server-tick listener that resolves completed hunter deployments.
 * Runs every 5 seconds (100 ticks) to avoid unnecessary overhead.
 */
@Mod.EventBusSubscriber
public class GuildTickHandler {

    private static int counter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++counter < 100) return;
        counter = 0;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerLevel overworld = server.overworld();
        GuildSavedData data = GuildSavedData.get(overworld);
        resolveDueDeployments(server, overworld, data);
        assignAutoRaids(server, overworld, data);
    }

    public static void resolveDueDeployments(MinecraftServer server, ServerLevel overworld, GuildSavedData data) {
        if (server == null || overworld == null || data == null) return;
        long gameTime = overworld.getGameTime();
        boolean changed = false;

        for (GuildData guild : data.allGuilds()) {
            guild.pruneTeamMembers();
            guild.reconcileHunterDeploymentStatus();
            Iterator<GuildDeployment> iterator = guild.deployments.iterator();
            while (iterator.hasNext()) {
                GuildDeployment dep = iterator.next();
                if (gameTime < dep.completesAt) continue;

                guild.awardXp(dep.xpReward);
                guild.totalClears++;
                List<ItemStack> crystalRewards = createManaCrystalRewards(guild, dep);
                int storedCrystalCount = insertRewards(guild, crystalRewards);
                GuildGateHelper.markGateCleared(overworld, dep.gateEntityUUID);

                GuildTeam team = guild.getTeam(dep.teamId);
                if (team != null) {
                    for (UUID hid : team.memberIds) {
                        GuildHunter h = guild.getHunter(hid);
                        if (h != null) h.status = "idle";
                    }
                }

                ServerPlayer owner = server.getPlayerList().getPlayer(guild.ownerUUID);
                if (owner != null) {
                    owner.sendSystemMessage(Component.literal(
                            "§a[Guild] Mission complete! Team §e" + dep.teamName
                                    + " §areturned from §f" + dep.gateLabel
                                    + "§a. Guild gained §e" + dep.xpReward + " XP§a"
                                    + (storedCrystalCount > 0 ? " §7and stored §b" + storedCrystalCount + " mana crystals§a!" : "!")));
                }

                iterator.remove();
                changed = true;
            }
            guild.reconcileHunterDeploymentStatus();
        }
        if (changed) data.markDirty();
    }

    private static void assignAutoRaids(MinecraftServer server, ServerLevel overworld, GuildSavedData data) {
        if (server == null || overworld == null || data == null) return;

        List<Entity> availableGates = new ArrayList<>();
        for (Entity gate : overworld.getAllEntities()) {
            if (!GuildGateHelper.isDeployableGate(gate)) continue;
            String gateId = gate.getUUID().toString();
            if (!GuildGateHelper.isGateInteracted(gate)
                    && !GuildGateHelper.isGateReserved(gate)
                    && GuildGateHelper.findGuildRaidingGate(data, gateId) == null) {
                availableGates.add(gate);
            }
        }
        if (availableGates.isEmpty()) return;

        boolean changed = false;
        long now = overworld.getGameTime();
        for (GuildData guild : data.allGuilds()) {
            guild.pruneTeamMembers();
            guild.reconcileHunterDeploymentStatus();
            for (GuildTeam team : guild.teams) {
                if (!team.autoRaidEnabled || team.memberIds.isEmpty() || guild.getDeploymentForTeam(team.id) != null) continue;
                if (!teamMembersIdle(guild, team)) continue;

                Entity chosenGate = null;
                int chosenRank = 0;
                for (Entity gate : availableGates) {
                    int rank = GuildGateHelper.gateRank(gate);
                    if (rank <= team.autoRaidMaxRank) {
                        chosenGate = gate;
                        chosenRank = rank;
                        break;
                    }
                }
                if (chosenGate == null) continue;

                String gateUuid = chosenGate.getUUID().toString();
                GuildDeployment dep = new GuildDeployment(
                        UUID.randomUUID(), team.id, team.name,
                        GuildGateHelper.gateLabel(chosenGate), chosenRank, gateUuid,
                        now + adjustedDurationTicks(guild, team, chosenRank),
                        adjustedXpReward(guild, team, chosenRank));
                guild.deployments.add(dep);
                for (UUID hid : team.memberIds) {
                    GuildHunter h = guild.getHunter(hid);
                    if (h != null) h.status = "deployed";
                }
                GuildGateHelper.reserveGate(chosenGate, guild);
                availableGates.remove(chosenGate);
                changed = true;

                ServerPlayer owner = server.getPlayerList().getPlayer(guild.ownerUUID);
                if (owner != null) {
                    owner.sendSystemMessage(Component.literal("§b[Guild] Auto raid: Team §e" + team.name
                            + " §bdeparted for §f" + dep.gateLabel + "§b."));
                }
                if (availableGates.isEmpty()) break;
            }
            guild.reconcileHunterDeploymentStatus();
            if (availableGates.isEmpty()) break;
        }
        if (changed) data.markDirty();
    }

    private static boolean teamMembersIdle(GuildData guild, GuildTeam team) {
        for (UUID hid : team.memberIds) {
            GuildHunter h = guild.getHunter(hid);
            if (h == null || "deployed".equals(h.status)) return false;
        }
        return true;
    }

    private static long adjustedDurationTicks(GuildData guild, GuildTeam team, int gateRank) {
        double powerRatio = Math.max(0.65D, Math.min(1.35D, teamPower(guild, team) / Math.max(1.0D, gateRank * 5.0D)));
        return Math.max(1200L, Math.round(GuildDeployment.durationTicks(gateRank) / powerRatio));
    }

    private static long adjustedXpReward(GuildData guild, GuildTeam team, int gateRank) {
        double powerRatio = Math.max(0.75D, Math.min(1.25D, teamPower(guild, team) / Math.max(1.0D, gateRank * 5.0D)));
        return Math.round(GuildDeployment.xpForRank(gateRank) * powerRatio);
    }

    private static double teamPower(GuildData guild, GuildTeam team) {
        if (guild == null || team == null) return 1.0D;
        double power = 0.0D;
        for (UUID hid : team.memberIds) {
            GuildHunter hunter = guild.getHunter(hid);
            if (hunter == null) continue;
            power += hunter.rankScore() + classBonus(hunter.hunterClass);
        }
        return Math.max(1.0D, power);
    }

    private static double classBonus(String hunterClass) {
        return switch (hunterClass) {
            case "Tanker", "Healer" -> 0.35D;
            case "Mage", "Ranger" -> 0.25D;
            case "Assassin", "Fighter" -> 0.2D;
            default -> 0.0D;
        };
    }

    private static List<ItemStack> createManaCrystalRewards(GuildData guild, GuildDeployment dep) {
        double powerRatio = Math.max(0.75D, Math.min(1.35D,
                teamPower(guild, guild.getTeam(dep.teamId)) / Math.max(1.0D, dep.gateRank * 5.0D)));
        int main = Math.max(1, (int) Math.round((2 + dep.gateRank) * powerRatio));
        int lower = dep.gateRank > 1 ? Math.max(1, (int) Math.round((1 + dep.gateRank / 2.0D) * powerRatio)) : 0;

        List<ItemStack> rewards = new ArrayList<>();
        rewards.add(new ItemStack(crystalForRank(dep.gateRank), main));
        if (lower > 0) rewards.add(new ItemStack(crystalForRank(dep.gateRank - 1), lower));
        if (dep.gateRank >= 5) rewards.add(new ItemStack(crystalForRank(dep.gateRank), 1));
        return rewards;
    }

    private static int insertRewards(GuildData guild, List<ItemStack> rewards) {
        int inserted = 0;
        for (ItemStack reward : rewards) {
            int before = reward.getCount();
            mergeIntoGuildStorage(guild, reward);
            inserted += before - reward.getCount();
        }
        return inserted;
    }

    private static void mergeIntoGuildStorage(GuildData guild, ItemStack stack) {
        if (guild == null || stack.isEmpty()) return;
        for (int i = 0; i < guild.storageItems.size() && !stack.isEmpty(); i++) {
            ItemStack slot = guild.storageItems.get(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(move);
                stack.shrink(move);
            }
        }
        for (int i = 0; i < guild.storageItems.size() && !stack.isEmpty(); i++) {
            if (guild.storageItems.get(i).isEmpty()) {
                int move = Math.min(stack.getCount(), stack.getMaxStackSize());
                ItemStack copy = stack.copy();
                copy.setCount(move);
                guild.storageItems.set(i, copy);
                stack.shrink(move);
            }
        }
    }

    private static Item crystalForRank(int rank) {
        return switch (rank) {
            case 2 -> SololevelingModItems.MANA_CRYSTAL_D.get();
            case 3 -> SololevelingModItems.MANA_CRYSTAL_C.get();
            case 4 -> SololevelingModItems.MANA_CRYSTAL_B.get();
            case 5 -> SololevelingModItems.MANA_CRYSTAL_A.get();
            case 6 -> SololevelingModItems.MANA_CRYSTAL_S.get();
            default -> SololevelingModItems.MANA_CRYSTAL_E.get();
        };
    }
}
