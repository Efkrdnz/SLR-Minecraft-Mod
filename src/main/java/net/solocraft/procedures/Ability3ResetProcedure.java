package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.entity.ShadowKaiselinEntity;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Comparator;

@Mod.EventBusSubscriber
public class Ability3ResetProcedure {
	private static final String SHADOW_OWNER = "sl_shadow_owner";

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB == 1) {
			boolean keepRiddenKaisel = event == null && isRidingOwnedKaisel(entity);
			resetShadowCounters(entity, keepRiddenKaisel);
			{
				final Vec3 _center = new Vec3(x, y, z);
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(400 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					boolean ownedTameShadow = entityiterator instanceof TamableAnimal _tamIsTamedBy && entity instanceof LivingEntity _livEnt && _tamIsTamedBy.isOwnedBy(_livEnt);
					boolean ownedTaggedShadow = entityiterator.getPersistentData().hasUUID(SHADOW_OWNER) && entityiterator.getPersistentData().getUUID(SHADOW_OWNER).equals(entity.getUUID());
					if (keepRiddenKaisel && entityiterator == entity.getVehicle())
						continue;
					if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows"))) && (ownedTameShadow || ownedTaggedShadow)) {
						if (!entityiterator.level().isClientSide()) {
							ShadowMonarchManager.saveBossHealthBeforeDespawn(entity, entityiterator);
							ShadowMonarchManager.dropStoredShadowInventory(entityiterator);
							entityiterator.discard();
						}
					}
				}
			}
		}
	}

	private static boolean isRidingOwnedKaisel(Entity entity) {
		Entity vehicle = entity.getVehicle();
		return vehicle instanceof ShadowKaiselinEntity && vehicle.getPersistentData().hasUUID(SHADOW_OWNER) && vehicle.getPersistentData().getUUID(SHADOW_OWNER).equals(entity.getUUID());
	}

	private static void resetShadowCounters(Entity entity, boolean keepRiddenKaisel) {
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.OrdShadow = 0;
			capability.GobShadow = 0;
			capability.WolfShadow = 0;
			capability.IgrisSpawned = 0;
			capability.orcspawned = 0;
			capability.ShadowGoblinArcherAmount = 0;
			capability.ShadowGoblinMageAmount = 0;
			capability.beru = 0;
			capability.summonlimitusage = 0;
			capability.polarbear = 0;
			capability.shadowdragonnum = 0;
			capability.highorcspawned = 0;
			capability.tuskspawned = 0;
			if (keepRiddenKaisel)
				capability.KaiselSpawned = Math.max(1, capability.KaiselSpawned);
			else
				capability.KaiselSpawned = 0;
			capability.syncPlayerVariables(entity);
		});
	}
}
