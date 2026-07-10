package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.ShowDamageNumberMessage;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public final class DamageNumberManager {
	private static final int OUTGOING = 0xFFFFF0A8;
	private static final int OUTGOING_HEAVY = 0xFFFF8A24;
	private static final int INCOMING = 0xFFFF4040;

	private DamageNumberManager() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingHurt(LivingHurtEvent event) {
		if (event.getAmount() <= 0 || event.getEntity().level().isClientSide())
			return;
		Set<ServerPlayer> recipients = new HashSet<>();
		ServerPlayer owner = owningPlayer(event.getSource().getEntity());
		if (owner != null) {
			recipients.add(owner);
			send(owner, event.getEntity(), event.getAmount(), event.getAmount() >= 20.0F ? OUTGOING_HEAVY : OUTGOING);
		}
		if (event.getEntity() instanceof ServerPlayer victim && recipients.add(victim)) {
			send(victim, event.getEntity(), event.getAmount(), INCOMING);
		}
	}

	private static void send(ServerPlayer player, Entity target, float amount, int color) {
		double x = target.getX();
		double y = target.getY() + target.getBbHeight() + 0.35D;
		double z = target.getZ();
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ShowDamageNumberMessage(x, y, z, amount, color));
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
