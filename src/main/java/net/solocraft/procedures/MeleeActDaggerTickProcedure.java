package net.solocraft.procedures;

import net.solocraft.entity.BasicAttackSlashEntity;
import net.solocraft.network.SololevelingModVariables;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@EventBusSubscriber
public class MeleeActDaggerTickProcedure {
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
		if (world instanceof Level level && level.isClientSide())
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).daggermelee) {
			entity.setDeltaMovement(new Vec3(0, 0, 0));
			{
				double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).daggermeleetimer + 1;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.daggermeleetimer = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).daggermeleetimer == 3) {
				BasicAttackSlashProcedure.execute(world, x, y, z, entity, daggerStyle(entity), 0);
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).daggermeleetimer == 6) {
				BasicAttackSlashProcedure.execute(world, x, y, z, entity, daggerStyle(entity), 1);
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).daggermeleetimer == 9) {
				{
					boolean _setval = false;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.daggermelee = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				BasicAttackSlashProcedure.execute(world, x, y, z, entity, daggerStyle(entity), 2);
				{
					double _setval = 0;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.daggermeleetimer = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
		}
	}

	private static int daggerStyle(Entity entity) {
		ItemStack offhand = entity instanceof LivingEntity livingEntity ? livingEntity.getOffhandItem() : ItemStack.EMPTY;
		return offhand.is(ItemTags.create(ResourceLocation.parse("dagger"))) ? BasicAttackSlashEntity.STYLE_DUAL_DAGGER : BasicAttackSlashEntity.STYLE_DAGGER;
	}
}
