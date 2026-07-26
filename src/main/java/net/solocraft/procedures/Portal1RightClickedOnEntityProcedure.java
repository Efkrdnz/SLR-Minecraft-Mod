package net.solocraft.procedures;

import net.solocraft.dungeon.ProceduralDungeonGateHandler;
import net.solocraft.dungeon.ProceduralDungeonRank;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.MagicReadingHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import java.util.List;
import java.util.Comparator;

public class Portal1RightClickedOnEntityProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (sourceentity == null)
			return;
		if (ProceduralDungeonGateHandler.isProceduralGate(entity)) {
			ProceduralDungeonGateHandler.enter(world, x, y, z, entity, sourceentity);
			return;
		}
		if (!MagicReadingHelper.isHoldingMagicReader(sourceentity)) {
			if (net.solocraft.guild.GuildGateHelper.prepareGateEntry(world, entity, sourceentity))
				return;
			{
				final Vec3 _center = new Vec3(x, y, z);
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(500 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))
							&& (entityiterator instanceof TamableAnimal _tamIsTamedBy && sourceentity instanceof LivingEntity _livEnt ? _tamIsTamedBy.isOwnedBy(_livEnt) : false)) {
						if (!entityiterator.level().isClientSide())
							entityiterator.discard();
					}
				}
			}
			{
				double _setval = sourceentity.getX();
				sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.DunX = _setval;
					capability.syncPlayerVariables(sourceentity);
				});
			}
			{
				double _setval = sourceentity.getY();
				sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.DunY = _setval;
					capability.syncPlayerVariables(sourceentity);
				});
			}
			{
				double _setval = sourceentity.getZ();
				sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.DunZ = _setval;
					capability.syncPlayerVariables(sourceentity);
				});
			}
			sourceentity.setNoGravity(true);
		} else {
			MagicReadingHelper.showRankReading(sourceentity, ProceduralDungeonRank.D);
		}
	}
}
