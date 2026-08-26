package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.Portal12Entity;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;

import java.util.Comparator;

@EventBusSubscriber
public class DunKasakaDetectionProcedure {
	private static final ResourceKey<Level> KASAKA_DIMENSION = ResourceKey.create(Registries.DIMENSION,
			ResourceLocation.fromNamespaceAndPath("sololeveling", "dungeon_dimension_kasaka"));

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true) {
			execute(event, event.getEntity().level(), event.getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (world.isClientSide())
			return;
		if (entity.level().dimension().equals(KASAKA_DIMENSION)) {
			SololevelingModVariables.PlayerVariables variables = entity.getCapability(
					SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
			if (variables != null && !variables.tpd) {
				Entity nearestPortal = world.getEntitiesOfClass(Portal12Entity.class,
						AABB.ofSize(entity.position(), 6.0D, 6.0D, 6.0D),
						portal -> portal.distanceToSqr(entity) <= 9.0D).stream()
						.min(Comparator.comparingDouble(portal -> portal.distanceToSqr(entity)))
						.orElse(null);
				if (nearestPortal != null) {
					if (entity.distanceToSqr(nearestPortal) <= 9) {
						variables.tpd = true;
						variables.syncPlayerVariables(entity);
						entity.setNoGravity(false);
						{
							Entity _ent = entity;
							_ent.teleportTo((nearestPortal.getX() + 3), (nearestPortal.getY()), (nearestPortal.getZ()));
							if (_ent instanceof ServerPlayer _serverPlayer)
								_serverPlayer.connection.teleport((nearestPortal.getX() + 3), (nearestPortal.getY()), (nearestPortal.getZ()), _ent.getYRot(), _ent.getXRot());
						}
					}
				}
			}
		}
	}
}
