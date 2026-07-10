package net.solocraft.block.entity;

import net.solocraft.guild.GuildData;
import net.solocraft.guild.GuildDeployment;
import net.solocraft.guild.GuildGateHelper;
import net.solocraft.guild.GuildHunter;
import net.solocraft.guild.GuildMemberPermissions;
import net.solocraft.guild.GuildSavedData;
import net.solocraft.guild.GuildTeam;
import net.solocraft.guild.GuildTickHandler;
import net.solocraft.init.SololevelingModBlockEntities;
import net.solocraft.world.inventory.GuildComputerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Block entity for the Guild Computer.
 *
 * Implements {@link Container} so its 27-slot inventory integrates cleanly
 * with the Minecraft slot system (slot sync, shift-click, etc.).
 */
public class GuildComputerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {

    // ── Storage ───────────────────────────────────────────────────────────────

    private final NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

    @Nullable
    private UUID boundGuildId = null;

    // ── Constructor ───────────────────────────────────────────────────────────

    public GuildComputerBlockEntity(BlockPos pos, BlockState state) {
        super(SololevelingModBlockEntities.GUILD_COMPUTER.get(), pos, state);
    }

    // ── Guild binding ─────────────────────────────────────────────────────────

    @Nullable
    public UUID getBoundGuildId() { return boundGuildId; }

    public void setBoundGuildId(UUID id) {
        this.boundGuildId = id;
        setChanged();
    }

    @Nullable
    public GuildData getGuild() {
        if (level == null || level.isClientSide() || boundGuildId == null) return null;
        return GuildSavedData.get((ServerLevel) level).getGuild(boundGuildId);
    }

    @Nullable
    private GuildData getGuildFor(Player player) {
        if (level == null || level.isClientSide()) return null;
        return GuildSavedData.get((ServerLevel) level).getGuildForPlayer(player.getUUID());
    }

    // ── Container ─────────────────────────────────────────────────────────────

    public NonNullList<ItemStack> getItems() { return items; }

    @Override public int getContainerSize()      { return 27; }
    @Override public boolean isEmpty()           { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, slot, count);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override public void setChanged()                { super.setChanged(); }
    @Override public boolean stillValid(Player player){ return true; }
    @Override public void clearContent()              { items.clear(); setChanged(); }

    // ── WorldlyContainer — block hoppers from touching this inventory ──────────
    private static final int[] NO_SLOTS = new int[0];
    @Override public int[] getSlotsForFace(Direction side)                              { return NO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int i, ItemStack s, Direction d)   { return false; }
    @Override public boolean canTakeItemThroughFace(int i, ItemStack s, Direction d)    { return false; }

    // ── MenuProvider ──────────────────────────────────────────────────────────

    @Override
    public Component getDisplayName() { return Component.literal("Guild Computer"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GuildComputerMenu(containerId, playerInventory, this, player);
    }

    /**
     * Serialises all guild state + hunter data into the buf sent to the client
     * when the screen first opens.
     */
    public void writeScreenOpeningData(Player player, FriendlyByteBuf buf) {
        buf.writeBlockPos(getBlockPos());

        GuildSavedData guildData = level != null && !level.isClientSide()
                ? GuildSavedData.get((ServerLevel) level) : null;
        GuildData guild = getGuildFor(player);

        if (guild == null) {
            buf.writeBoolean(false);
            return;
        }

        if (guildData != null) {
            GuildTickHandler.resolveDueDeployments(((ServerLevel) level).getServer(), ((ServerLevel) level).getServer().overworld(), guildData);
            migrateLegacyStorageToGuild(guild, guildData);
            guild.pruneTeamMembers();
            guild.reconcileHunterDeploymentStatus();
            guildData.markDirty();
        }

        buf.writeBoolean(true);

        // ── Basic guild info ──────────────────────────────────────────────────
        buf.writeUUID(guild.id);
        buf.writeUtf(guild.name);
        buf.writeUUID(guild.ownerUUID);
        buf.writeUtf(guild.ownerName);
        buf.writeInt(guild.level);
        buf.writeLong(guild.xp);
        buf.writeLong(GuildData.xpForLevel(guild.level));
        buf.writeInt(guild.totalClears);
        buf.writeInt(guild.activeBuffSlot1);
        buf.writeInt(guild.activeBuffSlot2);

        buf.writeUUID(player.getUUID());
        buf.writeBoolean(guild.canOperate(player.getUUID()));

        // ── Member permissions ────────────────────────────────────────────────
        buf.writeInt(guild.memberPermissions.size());
        for (GuildMemberPermissions p : guild.memberPermissions) {
            buf.writeUUID(p.playerUUID);
            buf.writeUtf(p.playerName);
            buf.writeBoolean(p.canOpen);
            buf.writeBoolean(p.tabOverview);
            buf.writeBoolean(p.tabRoster);
            buf.writeBoolean(p.tabTeams);
            buf.writeBoolean(p.tabDungeons);
            buf.writeBoolean(p.tabStorage);
            buf.writeBoolean(p.tabBuffs);
            buf.writeBoolean(p.tabLeaderboard);
        }

        // ── Hired hunters ─────────────────────────────────────────────────────
        buf.writeInt(guild.hunters.size());
        for (GuildHunter h : guild.hunters) {
            buf.writeUUID(h.id);
            buf.writeUtf(h.name);
            buf.writeUtf(h.rank);
            buf.writeUtf(h.hunterClass);
            buf.writeUtf(h.status);
        }

        // ── Recruit pool ──────────────────────────────────────────────────────
        buf.writeInt(guild.recruitPool.size());
        for (GuildHunter h : guild.recruitPool) {
            buf.writeUUID(h.id);
            buf.writeUtf(h.name);
            buf.writeUtf(h.rank);
            buf.writeUtf(h.hunterClass);
        }

        // ── Leaderboard (top 10) ──────────────────────────────────────────────
        if (level != null && !level.isClientSide()) {
            var lb = GuildSavedData.get((ServerLevel) level).getLeaderboard();
            int size = Math.min(lb.size(), 10);
            buf.writeInt(size);
            for (int i = 0; i < size; i++) {
                GuildData g = lb.get(i);
                buf.writeUtf(g.name);
                buf.writeInt(g.level);
                buf.writeInt(g.totalClears);
                buf.writeBoolean(g.id.equals(guild.id));
            }
        } else {
            buf.writeInt(0);
        }

        // ── Teams (5 per guild) ───────────────────────────────────────────────
        buf.writeInt(guild.teams.size());
        for (GuildTeam t : guild.teams) {
            buf.writeUUID(t.id);
            buf.writeUtf(t.name);
            buf.writeBoolean(t.autoRaidEnabled);
            buf.writeInt(t.autoRaidMaxRank);
            buf.writeInt(t.memberIds.size());
            for (java.util.UUID mid : t.memberIds) buf.writeUUID(mid);
        }

        // ── Nearby RedGate entities (within 256 blocks) ───────────────────────
        if (level != null && !level.isClientSide()) {
            ServerLevel sl = (ServerLevel) level;
            java.util.List<Entity> gates = sl.getEntitiesOfClass(
                    Entity.class,
                    new AABB(getBlockPos()).inflate(256),
                    GuildGateHelper::isDeployableGate);
            java.util.List<Entity> available = new java.util.ArrayList<>();
            for (Entity g : gates) {
                String gateId = g.getUUID().toString();
                if (!GuildGateHelper.isGateInteracted(g)
                        && !GuildGateHelper.isGateReserved(g)
                        && GuildGateHelper.findGuildRaidingGate(guildData, gateId) == null) {
                    available.add(g);
                }
            }
            buf.writeInt(available.size());
            for (Entity g : available) {
                buf.writeUUID(g.getUUID());
                buf.writeBlockPos(g.blockPosition());
                buf.writeUtf(GuildGateHelper.gateLabel(g));
                buf.writeInt(GuildGateHelper.gateRank(g));
            }
        } else {
            buf.writeInt(0);
        }

        // ── Active deployments ────────────────────────────────────────────────
        buf.writeInt(guild.deployments.size());
        for (GuildDeployment d : guild.deployments) {
            buf.writeUUID(d.id);
            buf.writeUUID(d.teamId);
            buf.writeUtf(d.teamName);
            buf.writeUtf(d.gateLabel);
            buf.writeInt(d.gateRank);
            buf.writeLong(d.completesAt);
            buf.writeLong(d.xpReward);
        }

        // ── Current server game-time (for client-side countdown display) ──────
        buf.writeLong(level != null && !level.isClientSide()
                ? ((ServerLevel) level).getGameTime() : 0L);
    }

    private void migrateLegacyStorageToGuild(GuildData guild, GuildSavedData guildData) {
        if (boundGuildId == null || !boundGuildId.equals(guild.id) || isEmpty()) return;

        boolean movedAny = false;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            ItemStack remaining = stack.copy();
            if (i < guild.storageItems.size() && guild.storageItems.get(i).isEmpty()) {
                guild.storageItems.set(i, remaining);
                items.set(i, ItemStack.EMPTY);
                movedAny = true;
                continue;
            }

            remaining = moveIntoGuildStorage(remaining, guild);
            if (remaining.isEmpty()) {
                items.set(i, ItemStack.EMPTY);
                movedAny = true;
            } else if (remaining.getCount() != stack.getCount()) {
                items.set(i, remaining);
                movedAny = true;
            }
        }

        if (movedAny) {
            guildData.markDirty();
            setChanged();
        }
    }

    private ItemStack moveIntoGuildStorage(ItemStack stack, GuildData guild) {
        for (int i = 0; i < guild.storageItems.size(); i++) {
            ItemStack target = guild.storageItems.get(i);
            if (target.isEmpty() || !ItemStack.isSameItemSameTags(target, stack)) continue;
            int move = Math.min(stack.getCount(), target.getMaxStackSize() - target.getCount());
            if (move <= 0) continue;
            target.grow(move);
            stack.shrink(move);
            if (stack.isEmpty()) return ItemStack.EMPTY;
        }

        for (int i = 0; i < guild.storageItems.size(); i++) {
            if (!guild.storageItems.get(i).isEmpty()) continue;
            guild.storageItems.set(i, stack.copy());
            return ItemStack.EMPTY;
        }

        return stack;
    }

    // ── NBT persistence ───────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        if (boundGuildId != null) tag.putUUID("boundGuildId", boundGuildId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        if (tag.hasUUID("boundGuildId")) boundGuildId = tag.getUUID("boundGuildId");
    }
}
