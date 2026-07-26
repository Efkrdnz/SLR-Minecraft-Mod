package net.solocraft.util;

import net.solocraft.dungeon.ProceduralDungeonRank;
import net.solocraft.init.SololevelingModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class MagicReadingHelper {
	private static final String MESSAGE_PREFIX = "Magic Reading: ";
	private static final String[] UNREADABLE_RESULTS = {"9999", "ERROR", "N/A", "Cannot Read!"};

	private MagicReadingHelper() {
	}

	public static boolean isHoldingMagicReader(Entity entity) {
		return (entity instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.MAGIC_READER.get();
	}

	public static void showRankReading(Entity reader, ProceduralDungeonRank rank) {
		ReadingRange range = rangeFor(rank);
		sendReading(reader, String.valueOf(Mth.nextInt(RandomSource.create(), range.minInclusive(), range.maxInclusive())));
	}

	public static void showUnreadableReading(Entity reader) {
		sendReading(reader, UNREADABLE_RESULTS[Mth.nextInt(RandomSource.create(), 0, UNREADABLE_RESULTS.length - 1)]);
	}

	public static ReadingRange rangeFor(ProceduralDungeonRank rank) {
		return switch (rank == null ? ProceduralDungeonRank.E : rank) {
			case E -> new ReadingRange(101, 199);
			case D -> new ReadingRange(201, 399);
			case C -> new ReadingRange(401, 599);
			case B -> new ReadingRange(601, 799);
			case A -> new ReadingRange(801, 999);
			case S -> new ReadingRange(1001, 1499);
		};
	}

	private static void sendReading(Entity reader, String value) {
		if (reader instanceof Player player && !player.level().isClientSide())
			player.displayClientMessage(Component.literal(MESSAGE_PREFIX + value), false);
	}

	public record ReadingRange(int minInclusive, int maxInclusive) {
	}
}
