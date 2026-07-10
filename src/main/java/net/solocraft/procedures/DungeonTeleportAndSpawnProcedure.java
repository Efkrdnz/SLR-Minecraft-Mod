package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.Portal12Entity;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

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

@Mod.EventBusSubscriber
public class DungeonTeleportAndSpawnProcedure {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player.level(), event.player.getX(), event.player.getY(), event.player.getZ(), event.player);
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, new ResourceLocation("duns"))) || world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, new ResourceLocation("duna")))
				|| world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, new ResourceLocation("dunb"))) || world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, new ResourceLocation("dunc")))
				|| world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, new ResourceLocation("dund")))) {
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
