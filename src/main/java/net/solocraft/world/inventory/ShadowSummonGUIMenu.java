
package net.solocraft.world.inventory;

import net.solocraft.init.SololevelingModMenus;
import net.solocraft.util.ShadowMonarchManager;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

public class ShadowSummonGUIMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
	private static final int SHADOW_TYPE_COUNT = 14;
	private static final int FIELD_RANK = 0;
	private static final int FIELD_LEVEL_LOW = 1;
	private static final int FIELD_LEVEL_HIGH = 2;
	private static final int FIELD_RANK_XP_LOW = 3;
	private static final int FIELD_RANK_XP_HIGH = 4;
	private static final int FIELD_RANK_XP_NEEDED_LOW = 5;
	private static final int FIELD_RANK_XP_NEEDED_HIGH = 6;
	private static final int FIELD_NEXT_RANK = 7;
	private static final int FIELD_FLAGS = 8;
	private static final int FIELD_OWNED = 9;
	private static final int FIELD_SUMMONED = 10;
	private static final int FIELD_COUNT = 11;
	private static final int SHADOW_DATA_COUNT = SHADOW_TYPE_COUNT * FIELD_COUNT;
	private static final int HEAL_BOSS_COST_LOW = SHADOW_DATA_COUNT;
	private static final int HEAL_BOSS_COST_HIGH = SHADOW_DATA_COUNT + 1;
	private static final int HEAL_ALL_COST_LOW = SHADOW_DATA_COUNT + 2;
	private static final int HEAL_ALL_COST_HIGH = SHADOW_DATA_COUNT + 3;
	private static final int MENU_DATA_COUNT = SHADOW_DATA_COUNT + 4;
	private static final int FLAG_LEVEL_CAPPED = 1;
	private static final int FLAG_MAX_RANK = 1 << 1;
	private static final int FLAG_CUSTOMIZABLE = 1 << 2;
	private static final int FLAG_EQUIPPED = 1 << 3;
	private static final int FLAG_GRAND_MARSHAL_ELIGIBLE = 1 << 4;
	private static final int FLAG_GRAND_MARSHAL_ACTIVE = 1 << 5;
	public final static HashMap<String, Object> guistate = new HashMap<>();
	public final Level world;
	public final Player entity;
	public int x, y, z;
	private ContainerLevelAccess access = ContainerLevelAccess.NULL;
	private IItemHandler internal;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private boolean bound = false;
	private Supplier<Boolean> boundItemMatcher = null;
	private Entity boundEntity = null;
	private BlockEntity boundBlockEntity = null;
	private final ContainerData shadowData;
	private long healingQuoteTick = Long.MIN_VALUE;
	private ShadowMonarchManager.ShadowHealingQuote cachedHealingQuote =
			new ShadowMonarchManager.ShadowHealingQuote(0, 0, 0, 0);

	public ShadowSummonGUIMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		super(SololevelingModMenus.SHADOW_SUMMON_GUI.get(), id);
		this.entity = inv.player;
		this.world = inv.player.level();
		this.internal = new ItemStackHandler(0);
		if (this.world.isClientSide()) {
			this.shadowData = new SimpleContainerData(MENU_DATA_COUNT);
		} else {
			ShadowMonarchManager.prepareRosterForDisplay(this.entity);
			this.shadowData = new ContainerData() {
				@Override
				public int get(int index) {
					if (index < 0 || index >= MENU_DATA_COUNT)
						return 0;
					if (index >= SHADOW_DATA_COUNT) {
						ShadowMonarchManager.ShadowHealingQuote quote =
								healingQuoteForData();
						return switch (index) {
							case HEAL_BOSS_COST_LOW -> lowWord(quote.bossManaCost());
							case HEAL_BOSS_COST_HIGH -> highWord(quote.bossManaCost());
							case HEAL_ALL_COST_LOW -> lowWord(quote.allManaCost());
							case HEAL_ALL_COST_HIGH -> highWord(quote.allManaCost());
							default -> 0;
						};
					}
					int buttonId = index % SHADOW_TYPE_COUNT;
					int field = index / SHADOW_TYPE_COUNT;
					String type = ShadowMonarchManager.typeForSummonButton(buttonId);
					ShadowMonarchManager.ShadowDisplayProgress progress =
							ShadowMonarchManager.progressForDisplay(ShadowSummonGUIMenu.this.entity, type);
					return switch (field) {
						case FIELD_RANK -> progress.rank();
						case FIELD_LEVEL_LOW -> lowWord(progress.level());
						case FIELD_LEVEL_HIGH -> highWord(progress.level());
						case FIELD_RANK_XP_LOW -> lowWord(progress.rankXp());
						case FIELD_RANK_XP_HIGH -> highWord(progress.rankXp());
						case FIELD_RANK_XP_NEEDED_LOW -> lowWord(progress.rankXpNeeded());
						case FIELD_RANK_XP_NEEDED_HIGH -> highWord(progress.rankXpNeeded());
						case FIELD_NEXT_RANK -> progress.nextRank();
						case FIELD_FLAGS -> (progress.levelCapped() ? FLAG_LEVEL_CAPPED : 0)
								| (progress.maxRank() ? FLAG_MAX_RANK : 0)
								// Every owned shadow can be customized now that outline
							// colours are per type; artifacts remain boss-only.
							| (ShadowMonarchManager.hasShadowForDisplay(
									ShadowSummonGUIMenu.this.entity, type)
											? FLAG_CUSTOMIZABLE : 0)
								| (ShadowMonarchManager.hasEquipmentForDisplay(
										ShadowSummonGUIMenu.this.entity, type) ? FLAG_EQUIPPED : 0)
								| (progress.grandMarshalEligible()
										? FLAG_GRAND_MARSHAL_ELIGIBLE : 0)
								| (progress.grandMarshalActive()
										? FLAG_GRAND_MARSHAL_ACTIVE : 0);
						case FIELD_OWNED -> ShadowMonarchManager.ownedCountForDisplay(
								ShadowSummonGUIMenu.this.entity, type);
						case FIELD_SUMMONED -> ShadowMonarchManager.summonedCountForDisplay(
								ShadowSummonGUIMenu.this.entity, type);
						default -> 0;
					};
				}

				@Override
				public void set(int index, int value) {
				}

				@Override
				public int getCount() {
					return MENU_DATA_COUNT;
				}
			};
		}
		this.addDataSlots(this.shadowData);
		BlockPos pos = null;
		if (extraData != null) {
			pos = extraData.readBlockPos();
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			access = ContainerLevelAccess.create(world, pos);
		}
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.bound) {
			if (this.boundItemMatcher != null)
				return this.boundItemMatcher.get();
			else if (this.boundBlockEntity != null)
				return AbstractContainerMenu.stillValid(this.access, player, this.boundBlockEntity.getBlockState().getBlock());
			else if (this.boundEntity != null)
				return this.boundEntity.isAlive();
		}
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		return ItemStack.EMPTY;
	}

	public Map<Integer, Slot> get() {
		return customSlots;
	}

	public int shadowRank(int buttonId) {
		if (buttonId < 0 || buttonId >= SHADOW_TYPE_COUNT)
			return ShadowMonarchManager.RANK_NORMAL;
		return data(FIELD_RANK, buttonId);
	}

	public int shadowLevel(int buttonId) {
		if (buttonId < 0 || buttonId >= SHADOW_TYPE_COUNT)
			return 1;
		return Math.max(1, joinedWords(data(FIELD_LEVEL_LOW, buttonId),
				data(FIELD_LEVEL_HIGH, buttonId)));
	}

	public int rankXp(int buttonId) {
		if (buttonId < 0 || buttonId >= SHADOW_TYPE_COUNT)
			return 0;
		return Math.max(0, joinedWords(data(FIELD_RANK_XP_LOW, buttonId),
				data(FIELD_RANK_XP_HIGH, buttonId)));
	}

	public int rankXpNeeded(int buttonId) {
		if (buttonId < 0 || buttonId >= SHADOW_TYPE_COUNT)
			return 1;
		return Math.max(1, joinedWords(data(FIELD_RANK_XP_NEEDED_LOW, buttonId),
				data(FIELD_RANK_XP_NEEDED_HIGH, buttonId)));
	}

	public int nextRank(int buttonId) {
		if (buttonId < 0 || buttonId >= SHADOW_TYPE_COUNT)
			return ShadowMonarchManager.RANK_NORMAL;
		return data(FIELD_NEXT_RANK, buttonId);
	}

	public boolean isAtLevelCap(int buttonId) {
		return hasFlag(buttonId, FLAG_LEVEL_CAPPED);
	}

	public boolean isMaxRank(int buttonId) {
		return hasFlag(buttonId, FLAG_MAX_RANK);
	}

	public boolean isCustomizable(int buttonId) {
		return hasFlag(buttonId, FLAG_CUSTOMIZABLE);
	}

	public boolean isEquipped(int buttonId) {
		return hasFlag(buttonId, FLAG_EQUIPPED);
	}

	public boolean isGrandMarshalEligible(int buttonId) {
		return hasFlag(buttonId, FLAG_GRAND_MARSHAL_ELIGIBLE);
	}

	public boolean isGrandMarshalActive(int buttonId) {
		return hasFlag(buttonId, FLAG_GRAND_MARSHAL_ACTIVE);
	}

	public boolean hasShadow(int buttonId) {
		return ownedCount(buttonId) > 0;
	}

	public int ownedCount(int buttonId) {
		if (buttonId < 0 || buttonId >= SHADOW_TYPE_COUNT)
			return 0;
		return Math.max(0, data(FIELD_OWNED, buttonId));
	}

	public int summonedCount(int buttonId) {
		if (buttonId < 0 || buttonId >= SHADOW_TYPE_COUNT)
			return 0;
		return Math.min(ownedCount(buttonId),
				Math.max(0, data(FIELD_SUMMONED, buttonId)));
	}

	public String shadowCountText(int buttonId) {
		return summonedCount(buttonId) + "/" + ownedCount(buttonId);
	}

	public int bossHealingManaCost() {
		return Math.max(0, joinedWords(this.shadowData.get(HEAL_BOSS_COST_LOW),
				this.shadowData.get(HEAL_BOSS_COST_HIGH)));
	}

	public int allHealingManaCost() {
		return Math.max(0, joinedWords(this.shadowData.get(HEAL_ALL_COST_LOW),
				this.shadowData.get(HEAL_ALL_COST_HIGH)));
	}

	private ShadowMonarchManager.ShadowHealingQuote healingQuoteForData() {
		long now = this.world.getGameTime();
		if (this.healingQuoteTick != now) {
			this.healingQuoteTick = now;
			this.cachedHealingQuote = ShadowMonarchManager.healingQuote(this.entity);
		}
		return this.cachedHealingQuote;
	}

	private boolean hasFlag(int buttonId, int flag) {
		return buttonId >= 0 && buttonId < SHADOW_TYPE_COUNT
				&& (data(FIELD_FLAGS, buttonId) & flag) != 0;
	}

	private int data(int field, int buttonId) {
		return this.shadowData.get(field * SHADOW_TYPE_COUNT + buttonId);
	}

	private static int lowWord(int value) {
		return value & 0xFFFF;
	}

	private static int highWord(int value) {
		return value >>> 16 & 0xFFFF;
	}

	private static int joinedWords(int low, int high) {
		return low & 0xFFFF | (high & 0xFFFF) << 16;
	}
}
