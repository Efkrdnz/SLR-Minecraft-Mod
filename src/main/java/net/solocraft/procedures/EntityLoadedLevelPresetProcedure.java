package net.solocraft.procedures;

import net.solocraft.entity.ThomasAndreEntity;
import net.solocraft.entity.StoneGolemEntity;
import net.solocraft.entity.SteelFangWolfEntity;
import net.solocraft.entity.SteelFangedLycanEntity;
import net.solocraft.entity.SpiderBossEntity;
import net.solocraft.entity.SkeletonWarriorEntity;
import net.solocraft.entity.SkeletonSummonerEntity;
import net.solocraft.entity.SkeletonBruteEntity;
import net.solocraft.entity.RedAntsEntity;
import net.solocraft.entity.PolarBearEntity;
import net.solocraft.entity.MutatedEntity;
import net.solocraft.entity.MiniGemGolemEntity;
import net.solocraft.entity.KargalganEntity;
import net.solocraft.entity.KangTaeshikEntity;
import net.solocraft.entity.IceElfEntity;
import net.solocraft.entity.HunterEntity;
import net.solocraft.entity.HighOrcEntity;
import net.solocraft.entity.GreenOrcEntity;
import net.solocraft.entity.GoblinMageEntity;
import net.solocraft.entity.GoblinKingEntity;
import net.solocraft.entity.GoblinClubEntity;
import net.solocraft.entity.GoblinArcherEntity;
import net.solocraft.entity.GemGolemEntity;
import net.solocraft.entity.FuturisticGolemEntity;
import net.solocraft.entity.FangedKasakaEntity;
import net.solocraft.entity.DKnight3Entity;
import net.solocraft.entity.DKnight2Entity;
import net.solocraft.entity.DKnight1Entity;
import net.solocraft.entity.CentipedeEntity;
import net.solocraft.entity.BloodRedComIgrisEntity;
import net.solocraft.entity.BeruBossEntity;
import net.solocraft.entity.BarukaEntity;
import net.solocraft.entity.AncientSamuraiEntity;
import net.solocraft.entity.AncientGolemEntity;
import net.solocraft.util.NamedHunterCombatManager;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class EntityLoadedLevelPresetProcedure {
	public static final String LEVEL_STAT_MULTIPLIER_TAG = "SLRLevelStatMultiplier";
	private static final double LUSH_CAVE_LEVEL_SCALING = 0.8D;

	@SubscribeEvent
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		execute(event, event.getLevel(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (NamedHunterCombatManager.isNamedHunter(entity))
			return;
		double rand = 0;
		double rank_addition = 0;
		double baseMaxHealth = getBaseAttributeValue(entity, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
		double baseAttackDamage = getBaseAttributeValue(entity, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
		if (entity.getPersistentData().getDouble("Level") == 0) {
			if (!(entity instanceof HunterEntity)) {
				if (world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, new ResourceLocation("dund")))) {
					rank_addition = 0;
				} else if (world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, new ResourceLocation("dunc")))) {
					rank_addition = 10;
				} else if (world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, new ResourceLocation("dunb")))) {
					rank_addition = 20;
				} else if (world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, new ResourceLocation("duna")))) {
					rank_addition = 30;
				} else if (world.getBiome(BlockPos.containing(x, y, z)).is(TagKey.create(Registries.BIOME, new ResourceLocation("duns")))) {
					rank_addition = 40;
				} else {
					rank_addition = 0;
				}
			} else {
				if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("D")) {
					rank_addition = 0;
				} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("C")) {
					rank_addition = 15;
				} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("B")) {
					rank_addition = 30;
				} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("A")) {
					rank_addition = 45;
				} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("S")) {
					rank_addition = 65;
				} else {
					rank_addition = 0;
				}
			}
			if (entity instanceof GoblinClubEntity || entity instanceof GoblinArcherEntity || entity instanceof GoblinMageEntity) {
				rand = Mth.nextInt(RandomSource.create(), (int) (rank_addition + 1), (int) (rank_addition + 10));
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + rand * 0.4));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).getBaseValue() + rand * 0.1));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof SteelFangWolfEntity || entity instanceof SteelFangedLycanEntity) {
				rand = Mth.nextInt(RandomSource.create(), (int) (rank_addition + 3), (int) (rank_addition + 10));
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + rand * 0.4));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).getBaseValue() + rand * 0.1));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + rand * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof DKnight1Entity || entity instanceof DKnight2Entity || entity instanceof DKnight3Entity) {
				rand = Mth.nextInt(RandomSource.create(), (int) (rank_addition + 1), (int) (rank_addition + 26));
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + rand * 0.4));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 1) * 0.1));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof MutatedEntity) {
				rand = Mth.nextInt(RandomSource.create(), (int) (rank_addition + 1), (int) (rank_addition + 21));
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 11) * 0.4));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 11) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof IceElfEntity) {
				rand = Mth.nextInt(RandomSource.create(), (int) (rank_addition + 11), (int) (rank_addition + 21));
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 11) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof CentipedeEntity) {
				rand = Mth.nextInt(RandomSource.create(), 40, 50);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 55) * 0.4));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 45) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof PolarBearEntity) {
				rand = Mth.nextInt(RandomSource.create(), 40, 64);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 57) * 0.4));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 57) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof RedAntsEntity) {
				rand = Mth.nextInt(RandomSource.create(), (int) (rank_addition + 11), (int) (rank_addition + 31));
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 20) * 0.4));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 20) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof BarukaEntity) {
				rand = Mth.nextInt(RandomSource.create(), 60, 75);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 70) * 1));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 70) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof BeruBossEntity) {
				if (entity instanceof BeruBossEntity animatable)
					animatable.setTexture("beru_base");
				rand = Mth.nextInt(RandomSource.create(), 90, 100);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 95) * 0.5));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 95) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof AncientSamuraiEntity) {
				rand = Mth.nextInt(RandomSource.create(), 25, 35);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 25) * 1));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 25) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof FangedKasakaEntity) {
				rand = Mth.nextInt(RandomSource.create(), 10, 20);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 15) * 1));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 15) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof BloodRedComIgrisEntity) {
				rand = Mth.nextInt(RandomSource.create(), 58, 70);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue(BloodRedComIgrisEntity.DUNGEON_MAX_HEALTH);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue(BloodRedComIgrisEntity.DUNGEON_ATTACK_DAMAGE);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
						.setBaseValue(BloodRedComIgrisEntity.DUNGEON_ARMOR);
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof GoblinKingEntity) {
				rand = Mth.nextInt(RandomSource.create(), 10, 25);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 10) * 1));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 10) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof SpiderBossEntity) {
				rand = Mth.nextInt(RandomSource.create(), 20, 25);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 20) * 1));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 20) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof GemGolemEntity) {
				rand = Mth.nextInt(RandomSource.create(), 40, 45);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 45) * 1));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 45) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof StoneGolemEntity || entity instanceof MiniGemGolemEntity) {
				rand = Mth.nextInt(RandomSource.create(), (int) (rank_addition + 1), (int) (rank_addition + 15));
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - (rank_addition + 5)) * 0.4));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - (rank_addition + 5)) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof FuturisticGolemEntity) {
				rand = Mth.nextInt(RandomSource.create(), 50, 65);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 55) * 1));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 55) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof AncientGolemEntity) {
				rand = Mth.nextInt(RandomSource.create(), 55, 70);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + rand * 0.2));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 62) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof KargalganEntity) {
				rand = Mth.nextInt(RandomSource.create(), 70, 80);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 75) * 0.2));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 75) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof GreenOrcEntity) {
				rand = Mth.nextInt(RandomSource.create(), (int) (rank_addition + 1), (int) (rank_addition + 15));
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 11) * 0.4));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 11) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof HighOrcEntity) {
				rand = Mth.nextInt(RandomSource.create(), (int) (rank_addition + 11), (int) (rank_addition + 31));
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 18) * 0.4));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 18) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof KangTaeshikEntity) {
				rand = Mth.nextInt(RandomSource.create(), 30, 40);
				entity.getPersistentData().putDouble("Level", rand);
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof ThomasAndreEntity) {
				rand = Mth.nextInt(RandomSource.create(), 120, 150);
				entity.getPersistentData().putDouble("Level", rand);
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof HunterEntity) {
				rand = Mth.nextInt(RandomSource.create(), (int) (rank_addition + 1), (int) (rank_addition + 15));
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
			}
			if (entity instanceof SkeletonBruteEntity) {
				rand = Mth.nextInt(RandomSource.create(), (int) (rank_addition + 13), (int) (rank_addition + 25));
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + rand * 0.4));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + rand * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof SkeletonWarriorEntity) {
				rand = Mth.nextInt(RandomSource.create(), (int) (rank_addition + 13), (int) (rank_addition + 25));
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + rand * 0.4));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + rand * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			if (entity instanceof SkeletonSummonerEntity) {
				rand = Mth.nextInt(RandomSource.create(), 50, 75);
				entity.setCustomName(Component.literal((entity.getDisplayName().getString() + " (Level: " + new java.text.DecimalFormat("##").format(rand) + ")")));
				entity.getPersistentData().putDouble("Level", rand);
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + (rand - 75) * 0.2));
				((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
						.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + (rand - 75) * 0.2));
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
			}
			applyLevelDifficultyBoost(entity);
			recordLevelStatMultiplier(entity, baseMaxHealth, baseAttackDamage);
		}
	}

	private static double getBaseAttributeValue(Entity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
		if (!(entity instanceof LivingEntity living) || living.getAttribute(attribute) == null)
			return 0;
		return living.getAttribute(attribute).getBaseValue();
	}

	private static void recordLevelStatMultiplier(Entity entity, double baseMaxHealth, double baseAttackDamage) {
		if (!(entity instanceof LivingEntity living) || entity.getPersistentData().getDouble("Level") <= 0)
			return;
		double multiplier = 1;
		double scaledMaxHealth = getBaseAttributeValue(living, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
		double scaledAttackDamage = getBaseAttributeValue(living, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
		if (baseMaxHealth > 0)
			multiplier = Math.max(multiplier, scaledMaxHealth / baseMaxHealth);
		if (baseAttackDamage > 0)
			multiplier = Math.max(multiplier, scaledAttackDamage / baseAttackDamage);
		entity.getPersistentData().putDouble(LEVEL_STAT_MULTIPLIER_TAG, Math.max(1, multiplier));
	}

	private static void applyLevelDifficultyBoost(Entity entity) {
		if (!(entity instanceof LivingEntity living))
			return;
		// The Job Change encounter is hand-balanced; its displayed level must not
		// silently add a second layer of health and damage scaling.
		if (entity instanceof BloodRedComIgrisEntity)
			return;
		double level = Math.max(0, entity.getPersistentData().getDouble("Level"));
		if (level <= 0)
			return;
		boolean boss = entity instanceof BloodRedComIgrisEntity || entity instanceof FangedKasakaEntity || entity instanceof GoblinKingEntity
				|| entity instanceof SpiderBossEntity || entity instanceof GemGolemEntity || entity instanceof FuturisticGolemEntity
				|| entity instanceof AncientGolemEntity || entity instanceof KargalganEntity || entity instanceof BarukaEntity || entity instanceof BeruBossEntity
				|| entity instanceof ThomasAndreEntity;
		double dungeonScaling = DunPlaceLushProcedure.isPlacingLushDungeon() ? LUSH_CAVE_LEVEL_SCALING : 1.0D;
		if (dungeonScaling < 1.0D)
			entity.getPersistentData().putBoolean("SLRLushDungeonMob", true);
		double healthPerLevel = (boss ? 1.9D : 1.15D) * dungeonScaling;
		double damagePerLevel = (boss ? 0.18D : 0.11D) * dungeonScaling;
		double armorPerLevel = (boss ? 0.035D : 0.025D) * dungeonScaling;
		if (living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null)
			living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
					.setBaseValue(living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() + level * healthPerLevel);
		if (living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null)
			living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
					.setBaseValue(living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + level * damagePerLevel);
		if (living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) != null)
			living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
					.setBaseValue(living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).getBaseValue() + level * armorPerLevel);
		if (living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) != null)
			living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)
					.setBaseValue(Math.min(0.95D, living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).getBaseValue() + level * 0.0025D * dungeonScaling));
		living.setHealth(living.getMaxHealth());
	}

}
