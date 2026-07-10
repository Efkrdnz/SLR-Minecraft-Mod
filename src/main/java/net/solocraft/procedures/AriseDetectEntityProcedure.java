package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.SteelFangWolfEntity;
import net.solocraft.entity.SteelFangedLycanEntity;
import net.solocraft.entity.PolarBearEntity;
import net.solocraft.entity.KargalganEntity;
import net.solocraft.entity.HighOrcEntity;
import net.solocraft.entity.GreenOrcEntity;
import net.solocraft.entity.GoblinMageEntity;
import net.solocraft.entity.GoblinClubEntity;
import net.solocraft.entity.GoblinArcherEntity;
import net.solocraft.entity.DKnight3Entity;
import net.solocraft.entity.DKnight2Entity;
import net.solocraft.entity.DKnight1Entity;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class AriseDetectEntityProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		execute(null, world, x, y, z, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		Entity creditedKiller = ShadowKillCreditHelper.creditedPlayer(world, sourceentity);
		Entity armorEntity = creditedKiller != null ? creditedKiller : sourceentity;
		boolean sourceIsShadow = ShadowMonarchManager.isShadowEntity(sourceentity);
		boolean canCreateSoul = sourceIsShadow || (creditedKiller == sourceentity && isShadowMonarch(sourceentity));
		if (hasShadowArmor(armorEntity) || !canCreateSoul)
			return;
		String soulType = soulTypeFor(entity);
		if (soulType.isEmpty() || !(world instanceof ServerLevel level))
			return;
		spawnSoul(level, world, x, y, z, soulType);
	}

	private static String soulTypeFor(Entity entity) {
		if (entity instanceof Zombie || entity instanceof Husk || entity instanceof Villager || entity instanceof ZombieVillager || entity instanceof Skeleton || entity instanceof Pillager || entity instanceof WitherSkeleton
				|| entity instanceof DKnight3Entity || entity instanceof DKnight2Entity || entity instanceof DKnight1Entity || entity instanceof Drowned)
			return "soldier";
		if (entity instanceof GoblinClubEntity)
			return "goblin";
		if (entity instanceof GoblinArcherEntity)
			return "goblinarc";
		if (entity instanceof GoblinMageEntity)
			return "goblinmage";
		if (entity instanceof SteelFangWolfEntity || entity instanceof SteelFangedLycanEntity)
			return "wolf";
		if (entity instanceof GreenOrcEntity)
			return "orc";
		if (entity instanceof PolarBearEntity)
			return "bear";
		if (entity instanceof HighOrcEntity)
			return "highorc";
		if (entity instanceof KargalganEntity)
			return "tusk";
		return "";
	}

	private static void spawnSoul(ServerLevel level, LevelAccessor world, double x, double y, double z, String soulType) {
		Entity entityToSpawn = SololevelingModEntities.SHADOW_SOUL.get().create(level);
		if (entityToSpawn == null)
			return;
		entityToSpawn.moveTo(x, y, z, world.getRandom().nextFloat() * 360.0F, 0.0F);
		if (entityToSpawn instanceof Mob mobToSpawn)
			mobToSpawn.finalizeSpawn(level, level.getCurrentDifficultyAt(entityToSpawn.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
		entityToSpawn.getPersistentData().putString("soultype", soulType);
		level.addFreshEntity(entityToSpawn);
	}

	private static boolean isShadowMonarch(Entity entity) {
		return entity != null && (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB == 1;
	}

	private static boolean hasShadowArmor(Entity entity) {
		return entity instanceof LivingEntity living && (living.getItemBySlot(EquipmentSlot.FEET).getItem() == SololevelingModItems.SHADOW_ARMOR_BOOTS.get()
				|| living.getItemBySlot(EquipmentSlot.LEGS).getItem() == SololevelingModItems.SHADOW_ARMOR_LEGGINGS.get()
				|| living.getItemBySlot(EquipmentSlot.CHEST).getItem() == SololevelingModItems.SHADOW_ARMOR_CHESTPLATE.get()
				|| living.getItemBySlot(EquipmentSlot.HEAD).getItem() == SololevelingModItems.SHADOW_ARMOR_HELMET.get());
	}
}
