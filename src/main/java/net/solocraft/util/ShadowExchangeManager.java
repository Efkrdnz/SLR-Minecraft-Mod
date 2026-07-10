package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModSounds;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ShadowExchangeManager {
	public static final int MAX_ANCHORS = 7;
	private static final String ROOT = "sololeveling_shadow_exchange";
	private static final String ANCHORS = "anchors";
	private static final String MIGRATED = "legacy_migrated";
	private static final int CAST_DELAY_TICKS = 24;
	private static final int COOLDOWN_TICKS = 80;
	private static final int ACCENT_NEGATIVE = 0xFFFF3D3D;
	private static final int ACCENT_SUCCESS = 0xFFB75CFF;

	private ShadowExchangeManager() {
	}

	public static boolean saveAnchor(LevelAccessor world, Entity entity, String shadowType, String displayName) {
		if (!(entity instanceof ServerPlayer player))
			return false;
		if (isRestrictedDimension(player)) {
			negative(player, "EXCHANGE LOCKED", "You cannot set an anchor here.");
			return false;
		}
		ensureMigrated(player);
		String normalizedType = normalizeType(shadowType);
		if (!hasShadowType(player, normalizedType)) {
			negative(player, "ANCHOR FAILED", "You do not have that shadow.");
			return false;
		}
		ListTag anchors = anchors(player);
		if (anchors.size() >= MAX_ANCHORS) {
			negative(player, "ANCHORS FULL", "Remove an exchange anchor first.");
			return false;
		}
		BlockPos safe = findSafeStandPos(player.serverLevel(), player.blockPosition());
		if (safe == null) {
			negative(player, "ANCHOR FAILED", "No safe ground found nearby.");
			return false;
		}
		CompoundTag anchor = new CompoundTag();
		anchor.putString("id", UUID.randomUUID().toString());
		anchor.putString("name", cleanName(displayName, anchors.size() + 1));
		anchor.putString("shadowType", normalizedType);
		anchor.putString("dimension", player.level().dimension().location().toString());
		anchor.putInt("x", safe.getX());
		anchor.putInt("y", safe.getY());
		anchor.putInt("z", safe.getZ());
		anchor.putLong("created", player.level().getGameTime());
		anchors.add(anchor);
		saveAndSync(player);
		player.closeContainer();
		success(player, "EXCHANGE ANCHOR SET", anchor.getString("name"));
		playQuiet(player.serverLevel(), player.position(), SololevelingModSounds.PANELOPEN.get(), 0.34F, 0.72F);
		player.serverLevel().sendParticles(ParticleTypes.SMOKE, safe.getX() + 0.5D, safe.getY() + 0.15D, safe.getZ() + 0.5D, 24, 0.45D, 0.12D, 0.45D, 0.02D);
		return true;
	}

	public static boolean startExchange(LevelAccessor world, Entity entity, int slot) {
		if (!(entity instanceof ServerPlayer player))
			return false;
		if (CooldownManager.isOnCooldown(player, "shadow_exchange")) {
			negative(player, "EXCHANGE UNAVAILABLE", "Skill is on cooldown.");
			return false;
		}
		ensureMigrated(player);
		CompoundTag anchor = anchor(player, slot);
		if (anchor == null) {
			negative(player, "EXCHANGE FAILED", "No anchor in that slot.");
			return false;
		}
		ServerLevel targetLevel = targetLevel(player, anchor);
		if (targetLevel == null) {
			removeAnchor(player, slot);
			negative(player, "EXCHANGE FAILED", "Target dimension no longer exists.");
			return false;
		}
		BlockPos requested = new BlockPos(anchor.getInt("x"), anchor.getInt("y"), anchor.getInt("z"));
		BlockPos safe = findSafeStandPos(targetLevel, requested);
		if (safe == null) {
			negative(player, "EXCHANGE FAILED", "No safe landing point found.");
			return false;
		}
		Vec3 start = player.position();
		player.closeContainer();
		CooldownManager.set(player, "shadow_exchange", COOLDOWN_TICKS);
		playQuiet(player.serverLevel(), start, SololevelingModSounds.PANELOPEN.get(), 0.32F, 0.68F);
		player.serverLevel().sendParticles(ParticleTypes.SMOKE, start.x, start.y + 0.2D, start.z, 48, 0.6D, 0.9D, 0.6D, 0.02D);
		player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, CAST_DELAY_TICKS + 12, 0, false, false));
		SololevelingMod.queueServerWork(CAST_DELAY_TICKS, () -> completeExchange(player, slot, anchor.getString("id")));
		return true;
	}

	public static boolean removeAnchor(Entity entity, int slot) {
		if (!(entity instanceof ServerPlayer player))
			return false;
		ensureMigrated(player);
		ListTag anchors = anchors(player);
		int index = slot - 1;
		if (index < 0 || index >= anchors.size())
			return false;
		anchors.remove(index);
		saveAndSync(player);
		success(player, "ANCHOR REMOVED", "Shadow Exchange slot " + slot);
		return true;
	}

	public static boolean hasAnchor(Entity entity, int slot) {
		return !anchorDisplay(entity, slot).isEmpty();
	}

	public static String anchorDisplay(Entity entity, int slot) {
		if (entity == null || slot < 1 || slot > MAX_ANCHORS)
			return "";
		if (!entity.level().isClientSide() && entity instanceof Player player)
			ensureMigrated(player);
		List<String> coords = legacyList(vars(entity).ExchangeCords);
		if (slot - 1 >= coords.size())
			return "";
		return coords.get(slot - 1);
	}

	private static void completeExchange(ServerPlayer player, int slot, String anchorId) {
		if (player == null || !player.isAlive())
			return;
		ensureMigrated(player);
		CompoundTag anchor = anchor(player, slot);
		if (anchor == null || !anchorId.equals(anchor.getString("id"))) {
			negative(player, "EXCHANGE FAILED", "The anchor changed during casting.");
			return;
		}
		ServerLevel targetLevel = targetLevel(player, anchor);
		if (targetLevel == null) {
			removeAnchor(player, slot);
			negative(player, "EXCHANGE FAILED", "Target dimension no longer exists.");
			return;
		}
		BlockPos safe = findSafeStandPos(targetLevel, new BlockPos(anchor.getInt("x"), anchor.getInt("y"), anchor.getInt("z")));
		if (safe == null) {
			negative(player, "EXCHANGE FAILED", "No safe landing point found.");
			return;
		}
		ServerLevel startLevel = player.serverLevel();
		Vec3 start = player.position();
		startLevel.sendParticles(ParticleTypes.SMOKE, start.x, start.y + 0.2D, start.z, 64, 0.75D, 1.1D, 0.75D, 0.02D);
		player.teleportTo(targetLevel, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D, player.getYRot(), player.getXRot());
		targetLevel.sendParticles(ParticleTypes.SMOKE, safe.getX() + 0.5D, safe.getY() + 0.2D, safe.getZ() + 0.5D, 64, 0.75D, 1.1D, 0.75D, 0.02D);
		playQuiet(targetLevel, Vec3.atBottomCenterOf(safe), SololevelingModSounds.PANELCLOSE.get(), 0.38F, 0.78F);
		player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 8, 0, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 14, 0, false, false));
		removeAnchorSilently(player, slot);
		success(player, "SHADOW EXCHANGE", "Arrived at " + shortDimension(anchor.getString("dimension")) + ".");
	}

	private static void removeAnchorSilently(ServerPlayer player, int slot) {
		ListTag anchors = anchors(player);
		int index = slot - 1;
		if (index >= 0 && index < anchors.size()) {
			anchors.remove(index);
			saveAndSync(player);
		}
	}

	private static CompoundTag anchor(Player player, int slot) {
		if (player == null || slot < 1 || slot > MAX_ANCHORS)
			return null;
		ListTag anchors = anchors(player);
		int index = slot - 1;
		return index >= 0 && index < anchors.size() ? anchors.getCompound(index) : null;
	}

	private static ServerLevel targetLevel(ServerPlayer player, CompoundTag anchor) {
		ResourceLocation id = ResourceLocation.tryParse(anchor.getString("dimension"));
		if (id == null)
			return null;
		ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
		return player.server.getLevel(key);
	}

	private static BlockPos findSafeStandPos(ServerLevel level, BlockPos origin) {
		level.getChunkAt(origin);
		if (isSafeStandPos(level, origin))
			return origin;
		for (int radius = 1; radius <= 4; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.abs(dx) != radius && Math.abs(dz) != radius)
						continue;
					for (int dy = 2; dy >= -4; dy--) {
						BlockPos pos = origin.offset(dx, dy, dz);
						if (isSafeStandPos(level, pos))
							return pos;
					}
				}
			}
		}
		return null;
	}

	private static boolean isSafeStandPos(ServerLevel level, BlockPos pos) {
		if (pos.getY() <= level.getMinBuildHeight() + 1 || pos.getY() >= level.getMaxBuildHeight() - 2)
			return false;
		BlockState floor = level.getBlockState(pos.below());
		return floor.isFaceSturdy(level, pos.below(), Direction.UP)
				&& level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
				&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
	}

	private static boolean isRestrictedDimension(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables vars = vars(player);
		String dimension = player.level().dimension().location().getPath();
		return vars.dungeoning || dimension.contains("dungeon") || dimension.contains("castle");
	}

	private static void ensureMigrated(Player player) {
		CompoundTag root = root(player);
		if (root.getBoolean(MIGRATED))
			return;
		ListTag anchors = anchors(player);
		if (anchors.isEmpty()) {
			List<String> coords = legacyList(vars(player).ExchangeCords);
			List<String> dims = legacyList(vars(player).ExchangeDimensions);
			int count = Math.min(MAX_ANCHORS, coords.size());
			for (int i = 0; i < count; i++) {
				LegacyCoord coord = parseCoord(coords.get(i));
				if (coord == null)
					continue;
				String dim = i < dims.size() ? parseDimension(dims.get(i)) : player.level().dimension().location().toString();
				CompoundTag anchor = new CompoundTag();
				anchor.putString("id", UUID.randomUUID().toString());
				anchor.putString("name", "Legacy Anchor " + (anchors.size() + 1));
				anchor.putString("shadowType", "legacy");
				anchor.putString("dimension", dim);
				anchor.putInt("x", coord.x);
				anchor.putInt("y", coord.y);
				anchor.putInt("z", coord.z);
				anchor.putLong("created", player.level().getGameTime());
				anchors.add(anchor);
			}
		}
		root.putBoolean(MIGRATED, true);
		saveAndSync(player);
	}

	private static void saveAndSync(Player player) {
		player.getPersistentData().put(ROOT, root(player));
		syncLegacyMirror(player);
	}

	private static void syncLegacyMirror(Player player) {
		ListTag anchors = anchors(player);
		StringBuilder cords = new StringBuilder(".");
		StringBuilder dims = new StringBuilder(".");
		for (int i = 0; i < anchors.size(); i++) {
			CompoundTag anchor = anchors.getCompound(i);
			String coord = anchor.getInt("x") + " " + anchor.getInt("y") + " " + anchor.getInt("z");
			cords.append(displayName(anchor, i + 1)).append(" | ").append(coord).append(",");
			dims.append("execute in ").append(anchor.getString("dimension")).append(" run tp @p ").append(coord).append(",");
		}
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.ExchangeCords = cords.toString();
			capability.ExchangeDimensions = dims.toString();
			capability.syncPlayerVariables(player);
		});
	}

	private static String displayName(CompoundTag anchor, int fallbackSlot) {
		String name = anchor.getString("name");
		return name == null || name.isBlank() ? "Anchor " + fallbackSlot : name;
	}

	private static CompoundTag root(Entity entity) {
		CompoundTag data = entity.getPersistentData();
		if (!data.contains(ROOT, Tag.TAG_COMPOUND))
			data.put(ROOT, new CompoundTag());
		CompoundTag root = data.getCompound(ROOT);
		if (!root.contains(ANCHORS, Tag.TAG_LIST))
			root.put(ANCHORS, new ListTag());
		return root;
	}

	private static ListTag anchors(Entity entity) {
		return root(entity).getList(ANCHORS, Tag.TAG_COMPOUND);
	}

	private static List<String> legacyList(String value) {
		ArrayList<String> result = new ArrayList<>();
		if (value == null || value.isBlank())
			return result;
		String cleaned = value.startsWith(".") ? value.substring(1) : value;
		for (String part : cleaned.split(",")) {
			String item = part == null ? "" : part.trim();
			if (!item.isEmpty() && !".".equals(item))
				result.add(item);
		}
		return result;
	}

	private static LegacyCoord parseCoord(String value) {
		if (value == null || value.isBlank())
			return null;
		String coord = value.contains("|") ? value.substring(value.indexOf('|') + 1).trim() : value.trim();
		String[] parts = coord.split("\\s+");
		if (parts.length < 3)
			return null;
		try {
			return new LegacyCoord((int) Math.floor(Double.parseDouble(parts[0])), (int) Math.floor(Double.parseDouble(parts[1])), (int) Math.floor(Double.parseDouble(parts[2])));
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private static String parseDimension(String command) {
		if (command == null)
			return "minecraft:overworld";
		int start = command.indexOf("execute in ");
		int end = command.indexOf(" run tp");
		if (start >= 0 && end > start)
			return command.substring(start + "execute in ".length(), end).trim();
		return "minecraft:overworld";
	}

	private static String cleanName(String displayName, int slot) {
		String name = displayName == null ? "" : displayName.trim();
		if (name.isEmpty())
			name = "Anchor " + slot;
		name = name.replace(',', ' ').replace('|', ' ');
		return name.length() > 24 ? name.substring(0, 24) : name;
	}

	private static String normalizeType(String type) {
		return type == null ? "" : type.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
	}

	private static boolean hasShadowType(Player player, String type) {
		SololevelingModVariables.PlayerVariables vars = vars(player);
		return switch (type) {
			case "knight" -> vars.ordshadowmax > 0;
			case "goblin_club" -> vars.GobShadowMax > 0;
			case "goblin_archer" -> vars.ShadowGoblinArcherMax > 0;
			case "goblin_mage" -> vars.ShadowGoblinMageMax > 0;
			case "wolf" -> vars.WolfShadowMax > 0;
			case "polar_bear" -> vars.polarbearmax > 0;
			case "orc" -> vars.orcmax > 0;
			case "high_orc" -> vars.highorcmax > 0;
			default -> false;
		};
	}

	private static String shortDimension(String dimension) {
		if (dimension == null || dimension.isBlank())
			return "target";
		int colon = dimension.indexOf(':');
		return colon >= 0 ? dimension.substring(colon + 1).replace('_', ' ') : dimension.replace('_', ' ');
	}

	private static void playQuiet(ServerLevel level, Vec3 pos, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
		level.playSound(null, BlockPos.containing(pos), sound, SoundSource.PLAYERS, volume, pitch);
	}

	private static void success(ServerPlayer player, String title, String under) {
		SystemNotifications.showTitleUnder(player, ACCENT_SUCCESS, 70,
				Component.literal(title).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
				Component.literal(under).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	private static void negative(ServerPlayer player, String title, String under) {
		SystemNotifications.showNegativeTitleUnder(player, ACCENT_NEGATIVE, 80,
				Component.literal(title).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				Component.literal(under).withStyle(ChatFormatting.RED));
	}

	private static SololevelingModVariables.PlayerVariables vars(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
	}

	private record LegacyCoord(int x, int y, int z) {
	}
}
