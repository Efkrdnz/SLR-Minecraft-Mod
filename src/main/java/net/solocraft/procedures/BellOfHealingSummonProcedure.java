package net.solocraft.procedures;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.BellOfHealingEntity;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Comparator;
import net.solocraft.util.CooldownManager;

public class BellOfHealingSummonProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world instanceof ServerLevel _level) {
			Entity entityToSpawn = SololevelingModEntities.BELL_OF_HEALING.get().spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
			if (entityToSpawn != null) {
				entityToSpawn.setYRot(world.getRandom().nextFloat() * 360F);
			}
		}
		if (world instanceof Level _level) {
			if (!_level.isClientSide()) {
				_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:bellirng")), SoundSource.NEUTRAL, (float) 0.2, 1);
			} else {
				_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:bellirng")), SoundSource.NEUTRAL, (float) 0.2, 1, false);
			}
		}
		CooldownManager.set(entity, "Blessing Mark", 6000);
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
			for (Entity entityiterator : _entfound) {
				if (entityiterator instanceof BellOfHealingEntity) {
					if (!(entityiterator instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false)) {
						entityiterator.setCustomName(Component.literal((entity.getDisplayName().getString() + "'s" + " " + "Bell Of Healing")));
						if (entityiterator instanceof TamableAnimal _toTame && entity instanceof Player _owner)
							_toTame.tame(_owner);
					}
				}
			}
		}
	}
}
