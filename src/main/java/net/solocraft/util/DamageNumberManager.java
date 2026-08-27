package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.dkc.DkcRadiruManager;
import net.solocraft.network.ShowDamageNumberMessage;

import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.solocraft.network.compat.PacketDistributor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Sends actual post-mitigation damage to the attacking/victim client only. */
@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DamageNumberManager {
	private static final int OUTGOING = 0xFFFFF0A8;
	private static final int OUTGOING_HEAVY = 0xFFFF8A24;
	private static final int INCOMING = 0xFFFF4040;

	private DamageNumberManager() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamage(LivingDamageEvent.Post event) {
		float damage = event.getNewDamage();
		if (event.getEntity().level().isClientSide())
			return;
		Set<ServerPlayer> recipients = new HashSet<>();
		ServerPlayer owner = owningPlayer(event.getSource().getEntity());
		// The damage-number path crosses the game bus, the network, a client config
		// gate, and a shader-aware render stage; without a marker at each step a
		// failure anywhere looks identical in game.
		if (owner != null && trace(owner))
			SololevelingMod.LOGGER.info(
					"[damage-numbers] post-damage {} on {} (dummy={})",
					damage, event.getEntity().getType().toShortString(),
					event.getEntity().getPersistentData()
							.getBoolean(DkcRadiruManager.TRAINING_DUMMY_TAG));
		if (damage <= 0.0F)
			return;
		// Radiru targets report the final post-armor value and rolling DPS from
		// LivingDamageEvent; the normal pre-armor number would be misleading.
		if (event.getEntity().getPersistentData().getBoolean(DkcRadiruManager.TRAINING_DUMMY_TAG))
			return;
		if (owner != null) {
			recipients.add(owner);
			send(owner, event.getEntity(), damage,
					damage >= 20.0F ? OUTGOING_HEAVY : OUTGOING);
		}
		if (event.getEntity() instanceof ServerPlayer victim && recipients.add(victim)) {
			send(victim, event.getEntity(), damage, INCOMING);
		}
	}

	private static void send(ServerPlayer player, Entity target, float amount, int color) {
		double x = target.getX();
		double y = target.getY() + target.getBbHeight() + 0.35D;
		double z = target.getZ();
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ShowDamageNumberMessage(x, y, z, amount, color));
		if (trace(player))
			SololevelingMod.LOGGER.info(
					"[damage-numbers] server sent {} to {} at {} {} {}",
					amount, player.getGameProfile().getName(), x, y, z);
	}

	/** Developer mode gates the tracing so ordinary play logs nothing. */
	private static boolean trace(ServerPlayer player) {
		return DeveloperModeManager.isEnabled(player);
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
}
