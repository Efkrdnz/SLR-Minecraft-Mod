package net.solocraft.world.inventory;

import net.solocraft.block.entity.GuildComputerBlockEntity;
import net.solocraft.guild.GuildData;
import net.solocraft.guild.GuildHunter;
import net.solocraft.guild.GuildMemberPermissions;
import net.solocraft.guild.GuildSavedData;
import net.solocraft.init.SololevelingModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Container menu for the Guild Computer.
 *
 * Slot layout (same ordering on both client and server):
 *   0–26   : guild storage (3×9)
 *   27–62  : player main inventory (3×9)
 *   63–71  : player hotbar
 *
 * Storage/player slots are controlled by {@link #storageTabActive} — they are
 * only rendered/interactive on the Storage tab.
 */
public class GuildComputerMenu extends AbstractContainerMenu {

    // ── Slot layout constants (relative to guiLeft / guiTop) ─────────────────
    public static final int STORAGE_X = 89;
    public static final int STORAGE_Y = 46;
    public static final int INV_X     = 89;
    public static final int INV_Y     = 108;
    public static final int HOTBAR_Y  = 166;

    // ── Guild data decoded from server ────────────────────────────────────────
    public final BlockPos computerPos;

    public boolean hasGuild      = false;
    public UUID    guildId;
    public String  guildName;
    public UUID    ownerUUID;
    public String  ownerName;
    public int     guildLevel;
    public long    guildXp;
    public long    xpToNext;
    public int     totalClears;
    public int     activeBuffSlot1;
    public int     activeBuffSlot2;

    public UUID    viewerUUID;
    public boolean viewerIsOwner;

    public final List<GuildMemberPermissions> members     = new ArrayList<>();
    public final List<GuildHunter>            hunters     = new ArrayList<>();
    public final List<GuildHunter>            recruitPool = new ArrayList<>();

    public record LeaderboardEntry(String name, int level, int clears, boolean isOwnGuild) {}
    public final List<LeaderboardEntry> leaderboard = new ArrayList<>();

    // ── Teams ─────────────────────────────────────────────────────────────────
    /** Lightweight team record for the client. */
    public record TeamInfo(UUID id, String name, List<UUID> memberIds,
                           boolean autoRaidEnabled, int autoRaidMaxRank) {}
    public final List<TeamInfo> teams = new ArrayList<>();

    // ── Nearby gate info (from server world scan) ─────────────────────────────
    public record NearbyGate(UUID entityId, BlockPos pos, String label, int rank) {}
    public final List<NearbyGate> nearbyGates = new ArrayList<>();

    // ── Active deployments ────────────────────────────────────────────────────
    public record DeploymentInfo(UUID id, UUID teamId, String teamName,
                                 String gateLabel, int gateRank,
                                 long completesAt, long xpReward) {}
    public final List<DeploymentInfo> deployments = new ArrayList<>();

    /** Server game-time when this menu was opened (for client-side countdown). */
    public long serverGameTime = 0L;

    // Server-side reference (null on client)
    private GuildComputerBlockEntity blockEntity;

    // ── Client constructor (from FriendlyByteBuf) ─────────────────────────────

    public GuildComputerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(SololevelingModMenus.GUILD_COMPUTER.get(), containerId);

        computerPos = buf.readBlockPos();
        hasGuild    = buf.readBoolean();

        if (hasGuild) {
            guildId       = buf.readUUID();
            guildName     = buf.readUtf();
            ownerUUID     = buf.readUUID();
            ownerName     = buf.readUtf();
            guildLevel    = buf.readInt();
            guildXp       = buf.readLong();
            xpToNext      = buf.readLong();
            totalClears   = buf.readInt();
            activeBuffSlot1 = buf.readInt();
            activeBuffSlot2 = buf.readInt();
            viewerUUID    = buf.readUUID();
            viewerIsOwner = buf.readBoolean();

            int memberCount = buf.readInt();
            for (int i = 0; i < memberCount; i++) {
                GuildMemberPermissions p = new GuildMemberPermissions(buf.readUUID(), buf.readUtf());
                p.canOpen        = buf.readBoolean();
                p.tabOverview    = buf.readBoolean();
                p.tabRoster      = buf.readBoolean();
                p.tabTeams       = buf.readBoolean();
                p.tabDungeons    = buf.readBoolean();
                p.tabStorage     = buf.readBoolean();
                p.tabBuffs       = buf.readBoolean();
                p.tabLeaderboard = buf.readBoolean();
                members.add(p);
            }

            int hunterCount = buf.readInt();
            for (int i = 0; i < hunterCount; i++) {
                GuildHunter h = new GuildHunter(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readUtf());
                h.status = buf.readUtf();
                hunters.add(h);
            }

            int poolCount = buf.readInt();
            for (int i = 0; i < poolCount; i++) {
                recruitPool.add(new GuildHunter(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readUtf()));
            }

            int lbSize = buf.readInt();
            for (int i = 0; i < lbSize; i++) {
                leaderboard.add(new LeaderboardEntry(
                        buf.readUtf(), buf.readInt(), buf.readInt(), buf.readBoolean()));
            }

            // Teams
            int teamCount = buf.readInt();
            for (int i = 0; i < teamCount; i++) {
                UUID teamId   = buf.readUUID();
                String tname  = buf.readUtf();
                boolean autoRaidEnabled = buf.readBoolean();
                int autoRaidMaxRank = buf.readInt();
                int mCount    = buf.readInt();
                List<UUID> mids = new ArrayList<>();
                for (int j = 0; j < mCount; j++) mids.add(buf.readUUID());
                teams.add(new TeamInfo(teamId, tname, mids, autoRaidEnabled, autoRaidMaxRank));
            }

            // Nearby gates
            int gateCount = buf.readInt();
            for (int i = 0; i < gateCount; i++) {
                nearbyGates.add(new NearbyGate(buf.readUUID(), buf.readBlockPos(), buf.readUtf(), buf.readInt()));
            }

            // Active deployments
            int depCount = buf.readInt();
            for (int i = 0; i < depCount; i++) {
                deployments.add(new DeploymentInfo(
                        buf.readUUID(),  // id
                        buf.readUUID(),  // teamId
                        buf.readUtf(),   // teamName
                        buf.readUtf(),   // gateLabel
                        buf.readInt(),   // gateRank
                        buf.readLong(),  // completesAt
                        buf.readLong()   // xpReward
                ));
            }

            serverGameTime = buf.readLong();
        } else {
            viewerUUID    = playerInventory.player.getUUID();
            viewerIsOwner = false;
        }

        // Storage slots backed by a SimpleContainer — server syncs actual contents
        SimpleContainer clientStorage = new SimpleContainer(27);
        addStorageSlots(clientStorage, playerInventory);
    }

    // ── Server constructor (from BlockEntity) ─────────────────────────────────

    public GuildComputerMenu(int containerId, Inventory playerInventory, GuildComputerBlockEntity be, Player player) {
        super(SololevelingModMenus.GUILD_COMPUTER.get(), containerId);
        this.blockEntity = be;
        this.computerPos = be.getBlockPos();

        GuildData guild = null;
        GuildSavedData savedData = null;
        if (!player.level().isClientSide()) {
            savedData = GuildSavedData.get((ServerLevel) player.level());
            guild = savedData.getGuildForPlayer(player.getUUID());
        }

        Container storage = guild != null
                ? new GuildStorageContainer(guild, savedData)
                : new SimpleContainer(27);
        addStorageSlots(storage, playerInventory);
    }

    // ── Storage-tab visibility flag (set by the client screen) ───────────────

    /**
     * When true all 63 storage + player inventory slots are active (rendered
     * and interactive). Toggled by GuildComputerScreen when the Storage tab
     * is selected / deselected.
     */
    public boolean storageTabActive = false;

    // ── Shared slot registration ──────────────────────────────────────────────

    private void addStorageSlots(net.minecraft.world.Container storage, Inventory player) {
        // Slots 0–26: guild storage (3×9)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new StorageSlot(storage, col + row * 9,
                        STORAGE_X + col * 18, STORAGE_Y + row * 18));

        // Slots 27–62: player main inventory (3×9)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new StorageSlot(player, col + row * 9 + 9,
                        INV_X + col * 18, INV_Y + row * 18));

        // Slots 63–71: hotbar
        for (int col = 0; col < 9; col++)
            addSlot(new StorageSlot(player, col, INV_X + col * 18, HOTBAR_Y));
    }

    /** A slot that is only active (rendered + interactive) on the Storage tab. */
    private class StorageSlot extends Slot {
        StorageSlot(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }
        @Override
        public boolean isActive() {
            return storageTabActive;
        }
    }

    // ── Shift-click ───────────────────────────────────────────────────────────

    private static class GuildStorageContainer implements Container {
        private final GuildData guild;
        private final GuildSavedData savedData;

        private GuildStorageContainer(GuildData guild, GuildSavedData savedData) {
            this.guild = guild;
            this.savedData = savedData;
        }

        @Override
        public int getContainerSize() {
            return guild.storageItems.size();
        }

        @Override
        public boolean isEmpty() {
            return guild.storageItems.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return guild.storageItems.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack result = ContainerHelper.removeItem(guild.storageItems, slot, count);
            if (!result.isEmpty()) setChanged();
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(guild.storageItems, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            guild.storageItems.set(slot, stack);
            if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
            setChanged();
        }

        @Override
        public void setChanged() {
            savedData.markDirty();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            guild.storageItems.clear();
            setChanged();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < 27) {
            if (!moveItemStackTo(stack, 27, 63, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 27, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) { return true; }
}
