package net.solocraft.procedures;

import net.solocraft.dungeon.ProceduralDungeonCompletionHandler;
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

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

import java.util.Comparator;

@EventBusSubscriber
public class DungeonTeleportAndSpawnProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true) {
			execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		// Procedural entrants are placed at their generated safe start directly.
		// Their entrance-side portal is revealed only after boss completion, so the
		// legacy proximity alignment below would otherwise drag them away from loot.
		if (world.isClientSide()
				|| entity.getPersistentData().getBoolean(
						ProceduralDungeonCompletionHandler.PROCEDURAL_DUNGEON_TAG))
			return;
		if (world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, ResourceLocation.parse("duns"))) || world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, ResourceLocation.parse("duna")))
				|| world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, ResourceLocation.parse("dunb"))) || world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, ResourceLocation.parse("dunc")))
				|| world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, ResourceLocation.parse("dund")))) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).tpd == false) {
				if (!world.getEntitiesOfClass(Portal12Entity.class, AABB.ofSize(new Vec3((entity.getX()), (entity.getY()), (entity.getZ())), 220, 220, 220), e -> true).isEmpty()) {
					{
						Entity _ent = entity;
						_ent.teleportTo((((Entity) world.getEntitiesOfClass(Portal12Entity.class, AABB.ofSize(new Vec3((entity.getX()), (entity.getY()), (entity.getZ())), 220, 220, 220), e -> true).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
							}
						}.compareDistOf((entity.getX()), (entity.getY()), (entity.getZ()))).findFirst().orElse(null)).getX() + 3),
								(((Entity) world.getEntitiesOfClass(Portal12Entity.class, AABB.ofSize(new Vec3((entity.getX()), (entity.getY()), (entity.getZ())), 220, 220, 220), e -> true).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
									}
								}.compareDistOf((entity.getX()), (entity.getY()), (entity.getZ()))).findFirst().orElse(null)).getY()),
								(((Entity) world.getEntitiesOfClass(Portal12Entity.class, AABB.ofSize(new Vec3((entity.getX()), (entity.getY()), (entity.getZ())), 220, 220, 220), e -> true).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
									}
								}.compareDistOf((entity.getX()), (entity.getY()), (entity.getZ()))).findFirst().orElse(null)).getZ()));
						if (_ent instanceof ServerPlayer _serverPlayer)
							_serverPlayer.connection.teleport((((Entity) world.getEntitiesOfClass(Portal12Entity.class, AABB.ofSize(new Vec3((entity.getX()), (entity.getY()), (entity.getZ())), 220, 220, 220), e -> true).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
								}
							}.compareDistOf((entity.getX()), (entity.getY()), (entity.getZ()))).findFirst().orElse(null)).getX() + 3),
									(((Entity) world.getEntitiesOfClass(Portal12Entity.class, AABB.ofSize(new Vec3((entity.getX()), (entity.getY()), (entity.getZ())), 220, 220, 220), e -> true).stream().sorted(new Object() {
										Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
											return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
										}
									}.compareDistOf((entity.getX()), (entity.getY()), (entity.getZ()))).findFirst().orElse(null)).getY()),
									(((Entity) world.getEntitiesOfClass(Portal12Entity.class, AABB.ofSize(new Vec3((entity.getX()), (entity.getY()), (entity.getZ())), 220, 220, 220), e -> true).stream().sorted(new Object() {
										Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
											return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
										}
									}.compareDistOf((entity.getX()), (entity.getY()), (entity.getZ()))).findFirst().orElse(null)).getZ()), _ent.getYRot(), _ent.getXRot());
					}
				}
			}
		}
	}
}
