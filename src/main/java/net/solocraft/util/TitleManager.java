package net.solocraft.util;

import net.solocraft.entity.SteelFangWolfEntity;
import net.solocraft.entity.SteelFangedLycanEntity;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber
public final class TitleManager {
	public static final int NONE = 0;
	public static final int WOLF_ASSASSIN = 1;
	public static final String WOLF_ASSASSIN_KEY = "wolf_assassin";
	public static final int WOLF_ASSASSIN_REQUIRED_KILLS = 20;

	private static final UUID WOLF_ASSASSIN_SPEED_ID = UUID.fromString("2ba3c4d2-2cf0-42f9-bf4d-89a296e1c304");
	private static final String WOLF_ASSASSIN_SPEED_NAME = "Wolf Assassin title speed";

	private TitleManager() {
	}

	public static String displayName(int titleId) {
		return titleId == WOLF_ASSASSIN ? "Wolf Assassin" : "None";
	}

	public static boolean isUnlocked(SololevelingModVariables.PlayerVariables vars, int titleId) {
		if (titleId == NONE)
			return true;
		if (vars.title == WOLF_ASSASSIN && titleId == WOLF_ASSASSIN)
			return true;
		return titleId == WOLF_ASSASSIN && hasToken(vars.unlockedTitles, WOLF_ASSASSIN_KEY);
	}

	public static List<Component> tooltip(SololevelingModVariables.PlayerVariables vars, int titleId) {
		List<Component> lines = new ArrayList<>();
		if (titleId == NONE) {
			lines.add(Component.literal("None").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
			lines.add(Component.literal("No title effect equipped.").withStyle(ChatFormatting.DARK_GRAY));
			return lines;
		}
		if (titleId == WOLF_ASSASSIN) {
			boolean unlocked = isUnlocked(vars, WOLF_ASSASSIN);
			lines.add(Component.literal("Wolf Assassin").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
			if (unlocked) {
				lines.add(Component.literal("+10% damage vs wolves and lycans").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("+3% movement speed").withStyle(ChatFormatting.GRAY));
			} else {
				lines.add(Component.literal("Locked").withStyle(ChatFormatting.RED));
				lines.add(Component.literal((int) Math.min(vars.wolfAssassinKills, WOLF_ASSASSIN_REQUIRED_KILLS) + "/" + WOLF_ASSASSIN_REQUIRED_KILLS + " wolf-family kills").withStyle(ChatFormatting.GRAY));
			}
		}
		return lines;
	}

	public static boolean selectTitle(ServerPlayer player, int titleId) {
		if (player == null || titleId < NONE || titleId > WOLF_ASSASSIN)
			return false;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			ensureLegacyUnlock(vars);
			if (isUnlocked(vars, titleId)) {
				vars.title = titleId;
				vars.syncPlayerVariables(player);
			}
		});
		applySpeed(player);
		return true;
	}

	public static boolean isWolfFamily(Entity entity) {
		return entity instanceof SteelFangWolfEntity || entity instanceof SteelFangedLycanEntity;
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onLivingHurt(LivingHurtEvent event) {
		if (!isWolfFamily(event.getEntity()))
			return;
		ServerPlayer owner = owningPlayer(event.getSource().getEntity());
		if (owner == null)
			return;
		SololevelingModVariables.PlayerVariables vars = owner.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		ensureLegacyUnlock(vars);
		if ((int) vars.title == WOLF_ASSASSIN && isUnlocked(vars, WOLF_ASSASSIN)) {
			event.setAmount(event.getAmount() * 1.10F);
		}
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (!isWolfFamily(event.getEntity()))
			return;
		ServerPlayer owner = owningPlayer(event.getSource().getEntity());
		if (owner == null)
			return;
		owner.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			ensureLegacyUnlock(vars);
			if (isUnlocked(vars, WOLF_ASSASSIN))
				return;
			vars.wolfAssassinKills = Math.min(WOLF_ASSASSIN_REQUIRED_KILLS, vars.wolfAssassinKills + 1);
			if (vars.wolfAssassinKills >= WOLF_ASSASSIN_REQUIRED_KILLS) {
				vars.unlockedTitles = addToken(vars.unlockedTitles, WOLF_ASSASSIN_KEY);
				SystemNotifications.showTitleUnder(owner, 0xFFFFB83D, 110,
						Component.literal("TITLE UNLOCKED").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
						Component.literal("Wolf Assassin").withStyle(ChatFormatting.WHITE));
			}
			vars.syncPlayerVariables(owner);
		});
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player))
			return;
		if (player.tickCount % 20 == 0) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(TitleManager::ensureLegacyUnlock);
			applySpeed(player);
		}
	}

	private static void applySpeed(ServerPlayer player) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null)
			return;
		AttributeModifier old = speed.getModifier(WOLF_ASSASSIN_SPEED_ID);
		if (old != null)
			speed.removeModifier(WOLF_ASSASSIN_SPEED_ID);
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		ensureLegacyUnlock(vars);
		if ((int) vars.title == WOLF_ASSASSIN && isUnlocked(vars, WOLF_ASSASSIN)) {
			speed.addTransientModifier(new AttributeModifier(WOLF_ASSASSIN_SPEED_ID, WOLF_ASSASSIN_SPEED_NAME, 0.03D, AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}

	private static ServerPlayer owningPlayer(Entity source) {
		if (source instanceof ServerPlayer player)
			return player;
		if (source instanceof TamableAnimal tame && tame.getOwner() instanceof ServerPlayer owner)
			return owner;
		if (source instanceof Projectile projectile && projectile.getOwner() != null)
			return owningPlayer(projectile.getOwner());
		if (source != null) {
			UUID ownerId = ShadowMonarchManager.getShadowOwnerUUID(source);
			if (ownerId != null && source.getServer() != null)
				return source.getServer().getPlayerList().getPlayer(ownerId);
		}
		return null;
	}

	private static void ensureLegacyUnlock(SololevelingModVariables.PlayerVariables vars) {
		if ((int) vars.title == WOLF_ASSASSIN && !hasToken(vars.unlockedTitles, WOLF_ASSASSIN_KEY)) {
			vars.unlockedTitles = addToken(vars.unlockedTitles, WOLF_ASSASSIN_KEY);
		}
	}

	private static boolean hasToken(String csv, String token) {
		if (csv == null || csv.isBlank())
			return false;
		for (String part : csv.split(",")) {
			if (part.trim().equals(token))
				return true;
		}
		return false;
	}

	private static String addToken(String csv, String token) {
		if (hasToken(csv, token))
			return csv;
		if (csv == null || csv.isBlank())
			return token;
		return csv + "," + token;
	}
}
