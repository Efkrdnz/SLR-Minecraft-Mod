package net.solocraft.procedures;

import net.solocraft.entity.KargalganEntity;
import net.solocraft.entity.DragonheadEntity;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import java.util.Comparator;

public class KargalganHymOfDragonProcedure {
	public static void execute(LevelAccessor world, double y, Entity entity) {
		if (entity == null)
			return;
		double rand = 0;
		double x = 0;
		double z = 0;
		double yaw = 0;
		rand = Mth.nextInt(RandomSource.create(), 1, 3);
		x = entity.getX();
		z = entity.getZ();
		yaw = entity.getYRot();
		if (entity instanceof KargalganEntity) {
			((KargalganEntity) entity).setAnimation("cast1");
		}
		{
			Entity _ent = entity;
			if (!_ent.level().isClientSide() && _ent.getServer() != null) {
				_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
						_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), ("summon sololeveling:dragonhead " + x + " ~4 " + z + " {Rotation:[" + yaw + "f,0f]}"));
			}
		}
		if (!world.getEntitiesOfClass(DragonheadEntity.class, AABB.ofSize(new Vec3((entity.getX()), y, (entity.getZ())), 5, 5, 5), e -> true).isEmpty()) {
			if (((Entity) world.getEntitiesOfClass(DragonheadEntity.class, AABB.ofSize(new Vec3((entity.getX()), y, (entity.getZ())), 5, 5, 5), e -> true).stream().sorted(new Object() {
				Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
					return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
				}
			}.compareDistOf((entity.getX()), y, (entity.getZ()))).findFirst().orElse(null)) instanceof TamableAnimal _toTame && entity instanceof Player _owner)
				_toTame.tame(_owner);
			((Entity) world.getEntitiesOfClass(DragonheadEntity.class, AABB.ofSize(new Vec3((entity.getX()), y, (entity.getZ())), 5, 5, 5), e -> true).stream().sorted(new Object() {
				Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
					return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
				}
			}.compareDistOf((entity.getX()), y, (entity.getZ()))).findFirst().orElse(null)).getPersistentData().putDouble("Level", (entity.getPersistentData().getDouble("Level")));
		}
		if (world instanceof Level _level) {
			if (!_level.isClientSide()) {
				_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.ender_dragon.growl")), SoundSource.NEUTRAL, 1, 1);
			} else {
				_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.ender_dragon.growl")), SoundSource.NEUTRAL, 1, 1, false);
			}
		}
	}
}
