package net.solocraft.init;

import net.solocraft.entity.RadiruBloodSpearEntity;
import net.solocraft.entity.EsilRadiruEntity;
import net.solocraft.entity.ArcaneVfxEntity;
import net.solocraft.entity.BarrierVfxEntity;
import net.solocraft.entity.HealerVfxEntity;
import net.solocraft.entity.FireMageVfxEntity;
import net.solocraft.entity.GlacialPursuitEntity;
import net.solocraft.entity.BeastVfxEntity;
import net.solocraft.entity.LiuSwordBeamEntity;
import net.solocraft.entity.LiuSwordVfxEntity;
import net.solocraft.entity.WhiteFlameVfxEntity;
import net.solocraft.entity.WhiteFlameEntity;
import net.solocraft.entity.VulcanEntity;
import net.solocraft.entity.TuskShadowEntity;
import net.solocraft.entity.TrainingBotEntity;
import net.solocraft.entity.ThomasAndreEntity;
import net.solocraft.entity.SungJinWooEntity;
import net.solocraft.entity.SilladBossEntity;
import net.solocraft.entity.StoneGolemEntity;
import net.solocraft.entity.SteelFangedLycanEntity;
import net.solocraft.entity.SteelFangWolfShadowEntity;
import net.solocraft.entity.SteelFangWolfEntity;
import net.solocraft.entity.StatueswordEntity;
import net.solocraft.entity.StatuehammerEntity;
import net.solocraft.entity.StatueaxeEntity;
import net.solocraft.entity.StatueOfGodEntity;
import net.solocraft.entity.SwordBeamProjectileEntity;
import net.solocraft.entity.SpiderWebEntity;
import net.solocraft.entity.SpiderBossEntity;
import net.solocraft.entity.SpawnerPortalEntity;
import net.solocraft.entity.SlasheffectswordEntity;
import net.solocraft.entity.SlashEffectEntity;
import net.solocraft.entity.SlashEntity;
import net.solocraft.entity.Slash6Entity;
import net.solocraft.entity.Slash5Entity;
import net.solocraft.entity.Slash4Entity;
import net.solocraft.entity.Slash3Entity;
import net.solocraft.entity.Slash2Entity;
import net.solocraft.entity.SkeletonWarriorEntity;
import net.solocraft.entity.SkeletonSummonerEntity;
import net.solocraft.entity.SkeletonBruteEntity;
import net.solocraft.entity.ShamanMagicEntity;
import net.solocraft.entity.ShadowStepEntity;
import net.solocraft.entity.ShadowSoulEntity;
import net.solocraft.entity.ShadowSold1Entity;
import net.solocraft.entity.ShadowPolarBearEntity;
import net.solocraft.entity.ShadowIgrisEntity;
import net.solocraft.entity.ShadowHighOrcEntity;
import net.solocraft.entity.ShadowIronEntity;
import net.solocraft.entity.ShadowGreenOrcEntity;
import net.solocraft.entity.SecretaryEntity;
import net.solocraft.entity.RulersHandEntity;
import net.solocraft.entity.RulersAuthorityAuraEntity;
import net.solocraft.entity.DKCTowerAuraEntity;
import net.solocraft.entity.RedGateEntity;
import net.solocraft.entity.RedAntsEntity;
import net.solocraft.entity.RangerProjectileEntity;
import net.solocraft.entity.RandomCaveLargeEntity;
import net.solocraft.entity.QuickSlashesEntity;
import net.solocraft.entity.PortalSewersEntity;
import net.solocraft.entity.PortalSEntity;
import net.solocraft.entity.PortalLushEntity;
import net.solocraft.entity.PortalLabEntity;
import net.solocraft.entity.PortalKargalgansThroneRoomEntity;
import net.solocraft.entity.PortalJobChangeEntity;
import net.solocraft.entity.CartenonGateEntity;
import net.solocraft.entity.PortalEntity;
import net.solocraft.entity.PortalCemeteryEntity;
import net.solocraft.entity.PortalBeruEntity;
import net.solocraft.entity.PortalAncientGolemEntity;
import net.solocraft.entity.Portal1Entity;
import net.solocraft.entity.DatapackGateEntity;
import net.solocraft.entity.Portal12Entity;
import net.solocraft.entity.PolarBearEntity;
import net.solocraft.entity.OrcShadowEntity;
import net.solocraft.entity.OrcEntity;
import net.solocraft.entity.NecroBlastEntity;
import net.solocraft.entity.MutatedEntity;
import net.solocraft.entity.MiniGemGolemEntity;
import net.solocraft.entity.ManaBulletEntity;
import net.solocraft.entity.ManaArrowEntity;
import net.solocraft.entity.MagicalSkullEntity;
import net.solocraft.entity.MagicMissileEntity;
import net.solocraft.entity.MagicEyeEntity;
import net.solocraft.entity.LightBallEntity;
import net.solocraft.entity.KasakaEntity;
import net.solocraft.entity.KargalganEntity;
import net.solocraft.entity.KaiselinEntity;
import net.solocraft.entity.ShadowKaiselinEntity;
import net.solocraft.entity.KangTaeshikEntity;
import net.solocraft.entity.KamishShadowEntity;
import net.solocraft.entity.KamishEntity;
import net.solocraft.entity.IgrisShadowEntity;
import net.solocraft.entity.IgrisEntity;
import net.solocraft.entity.IgrisDeadBodyEntity;
import net.solocraft.entity.IcecleEntity;
import net.solocraft.entity.IceElfEntity;
import net.solocraft.entity.IceChunkEntity;
import net.solocraft.entity.IceBallEntity;
import net.solocraft.entity.HunterEntity;
import net.solocraft.entity.HomingFlameArrowEntity;
import net.solocraft.entity.HighOrcEntity;
import net.solocraft.entity.GreenOrcEntity;
import net.solocraft.entity.GoblinMageShadowEntity;
import net.solocraft.entity.GoblinMageEntity;
import net.solocraft.entity.GoblinKingEntity;
import net.solocraft.entity.GoblinClubShadowEntity;
import net.solocraft.entity.GoblinClubEntity;
import net.solocraft.entity.GoblinArcherShadowEntity;
import net.solocraft.entity.GoblinArcherEntity;
import net.solocraft.entity.GemGolemEntity;
import net.solocraft.entity.FxspikEntity;
import net.solocraft.entity.FxPuddleEntity;
import net.solocraft.entity.FuturisticGolemEntity;
import net.solocraft.entity.FlagOfProtectionEntity;
import net.solocraft.entity.FireFlyEntity;
import net.solocraft.entity.FangedKasakaEntity;
import net.solocraft.entity.ElderBeastEntity;
import net.solocraft.entity.DummyPortalRedEntity;
import net.solocraft.entity.DummyPortalPurpleEntity;
import net.solocraft.entity.DummyPortalNormalEntity;
import net.solocraft.entity.DragonheadEntity;
import net.solocraft.entity.DragonFireballEntity;
import net.solocraft.entity.DragonBreatheEntity;
import net.solocraft.entity.DivineArrowEntity;
import net.solocraft.entity.DetectEyeInvEntity;
import net.solocraft.entity.DemonKnightEntity;
import net.solocraft.entity.DemonEntity;
import net.solocraft.entity.DaggerSlashEntity;
import net.solocraft.entity.ThrownDaggerEntity;
import net.solocraft.entity.DualWieldFlurryEntity;
import net.solocraft.entity.DKnight3Entity;
import net.solocraft.entity.DKnight2Entity;
import net.solocraft.entity.DKnight1Entity;
import net.solocraft.entity.CrossStrikeEntity;
import net.solocraft.entity.CursedChainsEntity;
import net.solocraft.entity.CurseMagicEntity;
import net.solocraft.entity.ChoijongEntity;
import net.solocraft.entity.ChaHaeInEntity;
import net.solocraft.entity.CerberusEntity;
import net.solocraft.entity.CentipedeEntity;
import net.solocraft.entity.BloodRedComIgrisEntity;
import net.solocraft.entity.BeruShadowEntity;
import net.solocraft.entity.BeruDeadBodyEntity;
import net.solocraft.entity.BeruBossEntity;
import net.solocraft.entity.BellOfHealingEntity;
import net.solocraft.entity.BearTrapEntity;
import net.solocraft.entity.BarukaEntity;
import net.solocraft.entity.BaranEntity;
import net.solocraft.entity.BaekYoonhoEntity;
import net.solocraft.entity.AttackshardEntity;
import net.solocraft.entity.ArrowSplashEntity;
import net.solocraft.entity.AncientSamuraiEntity;
import net.solocraft.entity.AncientGolemEntity;
import net.solocraft.entity.AfterImageEntity;
import net.solocraft.entity.AfterImage2Entity;
import net.solocraft.entity.AfterImage1Entity;
import net.solocraft.entity.BasicAttackSlashEntity;
import net.solocraft.SololevelingMod;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class SololevelingModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, SololevelingMod.MODID);
	public static final DeferredHolder<EntityType<?>, EntityType<IgrisEntity>> IGRIS = register("igris",
			EntityType.Builder.<IgrisEntity>of(IgrisEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

					.sized(1.2f, 3f));
	public static final DeferredHolder<EntityType<?>, EntityType<ShadowIgrisEntity>> SHADOW_IGRIS = register("shadow_igris", EntityType.Builder.<ShadowIgrisEntity>of(ShadowIgrisEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(128).setUpdateInterval(3).fireImmune().sized(1.2f, 2f));
	public static final DeferredHolder<EntityType<?>, EntityType<ShadowSold1Entity>> SHADOW_SOLD_1 = register("shadow_sold_1",
			EntityType.Builder.<ShadowSold1Entity>of(ShadowSold1Entity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<SungJinWooEntity>> SUNG_JIN_WOO = register("sung_jin_woo", EntityType.Builder.<SungJinWooEntity>of(SungJinWooEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<OrcEntity>> ORC = register("orc",
			EntityType.Builder.<OrcEntity>of(OrcEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3).fireImmune().sized(0.8f, 3f));
	public static final DeferredHolder<EntityType<?>, EntityType<OrcShadowEntity>> ORC_SHADOW = register("orc_shadow",
			EntityType.Builder.<OrcShadowEntity>of(OrcShadowEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3).fireImmune().sized(0.8f, 3f));
	public static final DeferredHolder<EntityType<?>, EntityType<GemGolemEntity>> GEM_GOLEM = register("gem_golem",
			EntityType.Builder.<GemGolemEntity>of(GemGolemEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3).fireImmune().sized(1f, 3f));
	public static final DeferredHolder<EntityType<?>, EntityType<AttackshardEntity>> ATTACKSHARD = register("attackshard", EntityType.Builder.<AttackshardEntity>of(AttackshardEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(1).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<BeruBossEntity>> BERU_BOSS = register("beru_boss",
			EntityType.Builder.<BeruBossEntity>of(BeruBossEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

					.sized(1f, 4f));
	public static final DeferredHolder<EntityType<?>, EntityType<BeruShadowEntity>> BERU_SHADOW = register("beru_shadow",
			EntityType.Builder.<BeruShadowEntity>of(BeruShadowEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

					.sized(1f, 4f));
	public static final DeferredHolder<EntityType<?>, EntityType<CentipedeEntity>> CENTIPEDE = register("centipede",
			EntityType.Builder.<CentipedeEntity>of(CentipedeEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(256).setUpdateInterval(3)

					.sized(3f, 3.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<DKnight1Entity>> D_KNIGHT_1 = register("d_knight_1",
			EntityType.Builder.<DKnight1Entity>of(DKnight1Entity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<DKnight2Entity>> D_KNIGHT_2 = register("d_knight_2",
			EntityType.Builder.<DKnight2Entity>of(DKnight2Entity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(32).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<DKnight3Entity>> D_KNIGHT_3 = register("d_knight_3",
			EntityType.Builder.<DKnight3Entity>of(DKnight3Entity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(32).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<KasakaEntity>> KASAKA = register("kasaka",
			EntityType.Builder.<KasakaEntity>of(KasakaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

					.sized(3f, 2f));
	public static final DeferredHolder<EntityType<?>, EntityType<MiniGemGolemEntity>> MINI_GEM_GOLEM = register("mini_gem_golem",
			EntityType.Builder.<MiniGemGolemEntity>of(MiniGemGolemEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(1f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<SteelFangWolfEntity>> STEEL_FANG_WOLF = register("steel_fang_wolf",
			EntityType.Builder.<SteelFangWolfEntity>of(SteelFangWolfEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 0.7f));
	public static final DeferredHolder<EntityType<?>, EntityType<SteelFangWolfShadowEntity>> STEEL_FANG_WOLF_SHADOW = register("steel_fang_wolf_shadow",
			EntityType.Builder.<SteelFangWolfShadowEntity>of(SteelFangWolfShadowEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 0.7f));
	public static final DeferredHolder<EntityType<?>, EntityType<AncientSamuraiEntity>> ANCIENT_SAMURAI = register("ancient_samurai",
			EntityType.Builder.<AncientSamuraiEntity>of(AncientSamuraiEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<StoneGolemEntity>> STONE_GOLEM = register("stone_golem",
			EntityType.Builder.<StoneGolemEntity>of(StoneGolemEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(20).setUpdateInterval(3)

					.sized(0.6f, 0.75f));
	public static final DeferredHolder<EntityType<?>, EntityType<SpiderBossEntity>> SPIDER_BOSS = register("spider_boss",
			EntityType.Builder.<SpiderBossEntity>of(SpiderBossEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(1.5f, 2.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<FireFlyEntity>> FIRE_FLY = register("fire_fly",
			EntityType.Builder.<FireFlyEntity>of(FireFlyEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.2f, 0.2f));
	public static final DeferredHolder<EntityType<?>, EntityType<PolarBearEntity>> POLAR_BEAR = register("polar_bear",
			EntityType.Builder.<PolarBearEntity>of(PolarBearEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(1f, 1.4f));
	public static final DeferredHolder<EntityType<?>, EntityType<ShadowPolarBearEntity>> SHADOW_POLAR_BEAR = register("shadow_polar_bear",
			EntityType.Builder.<ShadowPolarBearEntity>of(ShadowPolarBearEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(1f, 1.4f));
	public static final DeferredHolder<EntityType<?>, EntityType<IceElfEntity>> ICE_ELF = register("ice_elf",
			EntityType.Builder.<IceElfEntity>of(IceElfEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<BarukaEntity>> BARUKA = register("baruka",
			EntityType.Builder.<BarukaEntity>of(BarukaEntity::new, MobCategory.AMBIENT).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<ChoijongEntity>> CHOIJONG = register("choijong",
			EntityType.Builder.<ChoijongEntity>of(ChoijongEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<BaekYoonhoEntity>> BAEK_YOONHO = register("baek_yoonho",
			EntityType.Builder.<BaekYoonhoEntity>of(BaekYoonhoEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<MagicEyeEntity>> MAGIC_EYE = register("magic_eye",
			EntityType.Builder.<MagicEyeEntity>of(MagicEyeEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<GoblinKingEntity>> GOBLIN_KING = register("goblin_king",
			EntityType.Builder.<GoblinKingEntity>of(GoblinKingEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(2.5f, 4f));
	public static final DeferredHolder<EntityType<?>, EntityType<StatueOfGodEntity>> STATUE_OF_GOD = register("statue_of_god",
			EntityType.Builder.<StatueOfGodEntity>of(StatueOfGodEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(512).setUpdateInterval(3)

					.sized(5.25f, 23.25f));
	public static final DeferredHolder<EntityType<?>, EntityType<KangTaeshikEntity>> KANG_TAESHIK = register("kang_taeshik",
			EntityType.Builder.<KangTaeshikEntity>of(KangTaeshikEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<RedAntsEntity>> RED_ANTS = register("red_ants",
			EntityType.Builder.<RedAntsEntity>of(RedAntsEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.8f, 1.3f));
	public static final DeferredHolder<EntityType<?>, EntityType<ThomasAndreEntity>> THOMAS_ANDRE = register("thomas_andre",
			EntityType.Builder.<ThomasAndreEntity>of(ThomasAndreEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.7f, 2.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<FangedKasakaEntity>> FANGED_KASAKA = register("fanged_kasaka",
			EntityType.Builder.<FangedKasakaEntity>of(FangedKasakaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

					.sized(5f, 7f));
	public static final DeferredHolder<EntityType<?>, EntityType<FxPuddleEntity>> FX_PUDDLE = register("fx_puddle",
			EntityType.Builder.<FxPuddleEntity>of(FxPuddleEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(1.7f, 0.2f));
	public static final DeferredHolder<EntityType<?>, EntityType<FxspikEntity>> FXSPIK = register("fxspik",
			EntityType.Builder.<FxspikEntity>of(FxspikEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.8f, 5f));
	public static final DeferredHolder<EntityType<?>, EntityType<StatueaxeEntity>> STATUEAXE = register("statueaxe",
			EntityType.Builder.<StatueaxeEntity>of(StatueaxeEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(1f, 3f));
	public static final DeferredHolder<EntityType<?>, EntityType<StatuehammerEntity>> STATUEHAMMER = register("statuehammer", EntityType.Builder.<StatuehammerEntity>of(StatuehammerEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(1f, 3f));
	public static final DeferredHolder<EntityType<?>, EntityType<StatueswordEntity>> STATUESWORD = register("statuesword", EntityType.Builder.<StatueswordEntity>of(StatueswordEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(1f, 3f));
	public static final DeferredHolder<EntityType<?>, EntityType<FuturisticGolemEntity>> FUTURISTIC_GOLEM = register("futuristic_golem",
			EntityType.Builder.<FuturisticGolemEntity>of(FuturisticGolemEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(48).setUpdateInterval(3)

					.sized(1.1f, 3.1f));
	public static final DeferredHolder<EntityType<?>, EntityType<MutatedEntity>> MUTATED = register("mutated",
			EntityType.Builder.<MutatedEntity>of(MutatedEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<KamishShadowEntity>> KAMISH_SHADOW = register("kamish_shadow", EntityType.Builder.<KamishShadowEntity>of(KamishShadowEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(1f, 3f));
	public static final DeferredHolder<EntityType<?>, EntityType<BloodRedComIgrisEntity>> BLOOD_RED_COM_IGRIS = register("blood_red_com_igris",
			EntityType.Builder.<BloodRedComIgrisEntity>of(BloodRedComIgrisEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

					.sized(0.9f, 3.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<IgrisShadowEntity>> IGRIS_SHADOW = register("igris_shadow",
			EntityType.Builder.<IgrisShadowEntity>of(IgrisShadowEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

					.sized(0.9f, 3.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<AncientGolemEntity>> ANCIENT_GOLEM = register("ancient_golem", EntityType.Builder.<AncientGolemEntity>of(AncientGolemEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(1.2f, 4.51f));
	public static final DeferredHolder<EntityType<?>, EntityType<HunterEntity>> HUNTER = register("hunter",
			EntityType.Builder.<HunterEntity>of(HunterEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<ChaHaeInEntity>> CHA_HAE_IN = register("cha_hae_in",
			EntityType.Builder.<ChaHaeInEntity>of(ChaHaeInEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<KargalganEntity>> KARGALGAN = register("kargalgan",
			EntityType.Builder.<KargalganEntity>of(KargalganEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<SkeletonWarriorEntity>> SKELETON_WARRIOR = register("skeleton_warrior",
			EntityType.Builder.<SkeletonWarriorEntity>of(SkeletonWarriorEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<SkeletonBruteEntity>> SKELETON_BRUTE = register("skeleton_brute",
			EntityType.Builder.<SkeletonBruteEntity>of(SkeletonBruteEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.8f, 2.6f));
	public static final DeferredHolder<EntityType<?>, EntityType<KamishEntity>> KAMISH = register("kamish",
			EntityType.Builder.<KamishEntity>of(KamishEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(1f, 3f));
	public static final DeferredHolder<EntityType<?>, EntityType<SteelFangedLycanEntity>> STEEL_FANGED_LYCAN = register("steel_fanged_lycan",
			EntityType.Builder.<SteelFangedLycanEntity>of(SteelFangedLycanEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.9f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<DummyPortalNormalEntity>> DUMMY_PORTAL_NORMAL = register("dummy_portal_normal", EntityType.Builder.<DummyPortalNormalEntity>of(DummyPortalNormalEntity::new, MobCategory.CREATURE)
			.setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(3f, 3f));
	public static final DeferredHolder<EntityType<?>, EntityType<DummyPortalRedEntity>> DUMMY_PORTAL_RED = register("dummy_portal_red", EntityType.Builder.<DummyPortalRedEntity>of(DummyPortalRedEntity::new, MobCategory.CREATURE)
			.setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(3f, 3f));
	public static final DeferredHolder<EntityType<?>, EntityType<DummyPortalPurpleEntity>> DUMMY_PORTAL_PURPLE = register("dummy_portal_purple", EntityType.Builder.<DummyPortalPurpleEntity>of(DummyPortalPurpleEntity::new, MobCategory.CREATURE)
			.setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(3f, 3f));
	public static final DeferredHolder<EntityType<?>, EntityType<PortalBeruEntity>> PORTAL_BERU = register("portal_beru", EntityType.Builder.<PortalBeruEntity>of(PortalBeruEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64)
			.setUpdateInterval(3).fireImmune().sized(3f, 3f));
	public static final DeferredHolder<EntityType<?>, EntityType<PortalSEntity>> PORTAL_S = register("portal_s",
			EntityType.Builder.<PortalSEntity>of(PortalSEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<RedGateEntity>> RED_GATE = register("red_gate",
			EntityType.Builder.<RedGateEntity>of(RedGateEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<PortalLushEntity>> PORTAL_LUSH = register("portal_lush", EntityType.Builder.<PortalLushEntity>of(PortalLushEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64)
			.setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<PortalKargalgansThroneRoomEntity>> PORTAL_KARGALGANS_THRONE_ROOM = register("portal_kargalgans_throne_room",
			EntityType.Builder.<PortalKargalgansThroneRoomEntity>of(PortalKargalgansThroneRoomEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)
					.fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<RandomCaveLargeEntity>> RANDOM_CAVE_LARGE = register("random_cave_large", EntityType.Builder.<RandomCaveLargeEntity>of(RandomCaveLargeEntity::new, MobCategory.CREATURE)
			.setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<PortalAncientGolemEntity>> PORTAL_ANCIENT_GOLEM = register("portal_ancient_golem", EntityType.Builder.<PortalAncientGolemEntity>of(PortalAncientGolemEntity::new, MobCategory.CREATURE)
			.setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<PortalEntity>> PORTAL = register("portal",
			EntityType.Builder.<PortalEntity>of(PortalEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<Portal1Entity>> PORTAL_1 = register("portal_1",
			EntityType.Builder.<Portal1Entity>of(Portal1Entity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<DatapackGateEntity>> DATAPACK_GATE = register("datapack_gate",
			EntityType.Builder.<DatapackGateEntity>of(DatapackGateEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)
					.fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<SpawnerPortalEntity>> SPAWNER_PORTAL = register("spawner_portal", EntityType.Builder.<SpawnerPortalEntity>of(SpawnerPortalEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<Portal12Entity>> PORTAL_12 = register("portal_12",
			EntityType.Builder.<Portal12Entity>of(Portal12Entity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<PortalSewersEntity>> PORTAL_SEWERS = register("portal_sewers", EntityType.Builder.<PortalSewersEntity>of(PortalSewersEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<PortalLabEntity>> PORTAL_LAB = register("portal_lab", EntityType.Builder.<PortalLabEntity>of(PortalLabEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64)
			.setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<PortalJobChangeEntity>> PORTAL_JOB_CHANGE = register("portal_job_change", EntityType.Builder.<PortalJobChangeEntity>of(PortalJobChangeEntity::new, MobCategory.CREATURE)
			.setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<CartenonGateEntity>> CARTENON_GATE = register("cartenon_gate", EntityType.Builder.<CartenonGateEntity>of(CartenonGateEntity::new, MobCategory.CREATURE)
			.setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(2).fireImmune().sized(1.2f, 2.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<PortalCemeteryEntity>> PORTAL_CEMETERY = register("portal_cemetery", EntityType.Builder.<PortalCemeteryEntity>of(PortalCemeteryEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<TrainingBotEntity>> TRAINING_BOT = register("training_bot", EntityType.Builder.<TrainingBotEntity>of(TrainingBotEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(256).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<ShadowSoulEntity>> SHADOW_SOUL = register("shadow_soul", EntityType.Builder.<ShadowSoulEntity>of(ShadowSoulEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64)
			.setUpdateInterval(3).fireImmune().sized(0.6f, 1f));
	public static final DeferredHolder<EntityType<?>, EntityType<FlagOfProtectionEntity>> FLAG_OF_PROTECTION = register("flag_of_protection", EntityType.Builder.<FlagOfProtectionEntity>of(FlagOfProtectionEntity::new, MobCategory.MONSTER)
			.setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.3f, 0.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<BellOfHealingEntity>> BELL_OF_HEALING = register("bell_of_healing", EntityType.Builder.<BellOfHealingEntity>of(BellOfHealingEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<BearTrapEntity>> BEAR_TRAP = register("bear_trap",
			EntityType.Builder.<BearTrapEntity>of(BearTrapEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<IcecleEntity>> ICECLE = register("icecle",
			EntityType.Builder.<IcecleEntity>of(IcecleEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(32).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<AfterImageEntity>> AFTER_IMAGE = register("after_image", EntityType.Builder.<AfterImageEntity>of(AfterImageEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(1)
			.setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<AfterImage1Entity>> AFTER_IMAGE_1 = register("after_image_1", EntityType.Builder.<AfterImage1Entity>of(AfterImage1Entity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(1).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<AfterImage2Entity>> AFTER_IMAGE_2 = register("after_image_2", EntityType.Builder.<AfterImage2Entity>of(AfterImage2Entity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(1).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<SecretaryEntity>> SECRETARY = register("secretary", EntityType.Builder.<SecretaryEntity>of(SecretaryEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64)
			.setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<EsilRadiruEntity>> ESIL_RADIRU = register("esil_radiru", EntityType.Builder.<EsilRadiruEntity>of(EsilRadiruEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<ElderBeastEntity>> ELDER_BEAST = register("elder_beast",
			EntityType.Builder.<ElderBeastEntity>of(ElderBeastEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(2f, 4f));
	public static final DeferredHolder<EntityType<?>, EntityType<DetectEyeInvEntity>> DETECT_EYE_INV = register("detect_eye_inv", EntityType.Builder.<DetectEyeInvEntity>of(DetectEyeInvEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.1f, 0.1f));
	public static final DeferredHolder<EntityType<?>, EntityType<IceBallEntity>> ICE_BALL = register("ice_ball",
			EntityType.Builder.<IceBallEntity>of(IceBallEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.3f, 0.3f));
	public static final DeferredHolder<EntityType<?>, EntityType<IceChunkEntity>> ICE_CHUNK = register("ice_chunk",
			EntityType.Builder.<IceChunkEntity>of(IceChunkEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(3f, 2f));
	public static final DeferredHolder<EntityType<?>, EntityType<DaggerSlashEntity>> DAGGER_SLASH = register("dagger_slash", EntityType.Builder.<DaggerSlashEntity>of(DaggerSlashEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(1f, 1f));
	public static final DeferredHolder<EntityType<?>, EntityType<ThrownDaggerEntity>> THROWN_DAGGER = register("thrown_dagger", EntityType.Builder.<ThrownDaggerEntity>of(ThrownDaggerEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(96).setUpdateInterval(1).sized(0.45f, 0.25f));
	public static final DeferredHolder<EntityType<?>, EntityType<ArrowSplashEntity>> ARROW_SPLASH = register("arrow_splash", EntityType.Builder.<ArrowSplashEntity>of(ArrowSplashEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.2f, 0.2f));
	public static final DeferredHolder<EntityType<?>, EntityType<GoblinClubEntity>> GOBLIN_CLUB = register("goblin_club",
			EntityType.Builder.<GoblinClubEntity>of(GoblinClubEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<GoblinArcherEntity>> GOBLIN_ARCHER = register("goblin_archer",
			EntityType.Builder.<GoblinArcherEntity>of(GoblinArcherEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<GoblinMageEntity>> GOBLIN_MAGE = register("goblin_mage",
			EntityType.Builder.<GoblinMageEntity>of(GoblinMageEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<GoblinClubShadowEntity>> GOBLIN_CLUB_SHADOW = register("goblin_club_shadow",
			EntityType.Builder.<GoblinClubShadowEntity>of(GoblinClubShadowEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<GoblinArcherShadowEntity>> GOBLIN_ARCHER_SHADOW = register("goblin_archer_shadow",
			EntityType.Builder.<GoblinArcherShadowEntity>of(GoblinArcherShadowEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<GoblinMageShadowEntity>> GOBLIN_MAGE_SHADOW = register("goblin_mage_shadow",
			EntityType.Builder.<GoblinMageShadowEntity>of(GoblinMageShadowEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<SlasheffectswordEntity>> SLASHEFFECTSWORD = register("slasheffectsword", EntityType.Builder.<SlasheffectswordEntity>of(SlasheffectswordEntity::new, MobCategory.MONSTER)
			.setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.4f, 0.4f));
	public static final DeferredHolder<EntityType<?>, EntityType<IgrisDeadBodyEntity>> IGRIS_DEAD_BODY = register("igris_dead_body", EntityType.Builder.<IgrisDeadBodyEntity>of(IgrisDeadBodyEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(1.2f, 2.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<BeruDeadBodyEntity>> BERU_DEAD_BODY = register("beru_dead_body", EntityType.Builder.<BeruDeadBodyEntity>of(BeruDeadBodyEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.9f, 0.4f));
	public static final DeferredHolder<EntityType<?>, EntityType<CursedChainsEntity>> CURSED_CHAINS = register("cursed_chains", EntityType.Builder.<CursedChainsEntity>of(CursedChainsEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.3f, 0.3f));
	public static final DeferredHolder<EntityType<?>, EntityType<DragonheadEntity>> DRAGONHEAD = register("dragonhead", EntityType.Builder.<DragonheadEntity>of(DragonheadEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64)
			.setUpdateInterval(3).fireImmune().sized(0.6f, 0.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<CurseMagicEntity>> CURSE_MAGIC = register("curse_magic", EntityType.Builder.<CurseMagicEntity>of(CurseMagicEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64)
			.setUpdateInterval(3).fireImmune().sized(0.1f, 0.1f));
	public static final DeferredHolder<EntityType<?>, EntityType<GreenOrcEntity>> GREEN_ORC = register("green_orc",
			EntityType.Builder.<GreenOrcEntity>of(GreenOrcEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.9f, 4f));
	public static final DeferredHolder<EntityType<?>, EntityType<HighOrcEntity>> HIGH_ORC = register("high_orc",
			EntityType.Builder.<HighOrcEntity>of(HighOrcEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.9f, 4f));
	public static final DeferredHolder<EntityType<?>, EntityType<ShadowGreenOrcEntity>> SHADOW_GREEN_ORC = register("shadow_green_orc",
			EntityType.Builder.<ShadowGreenOrcEntity>of(ShadowGreenOrcEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.9f, 4f));
	public static final DeferredHolder<EntityType<?>, EntityType<ShadowHighOrcEntity>> SHADOW_HIGH_ORC = register("shadow_high_orc",
			EntityType.Builder.<ShadowHighOrcEntity>of(ShadowHighOrcEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.9f, 4f));
	public static final DeferredHolder<EntityType<?>, EntityType<TuskShadowEntity>> TUSK_SHADOW = register("tusk_shadow",
			EntityType.Builder.<TuskShadowEntity>of(TuskShadowEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

					.sized(0.8f, 2.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<ShadowIronEntity>> SHADOW_IRON = register("shadow_iron",
			EntityType.Builder.<ShadowIronEntity>of(ShadowIronEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

					.sized(0.9f, 3.6f));
	public static final DeferredHolder<EntityType<?>, EntityType<SkeletonSummonerEntity>> SKELETON_SUMMONER = register("skeleton_summoner",
			EntityType.Builder.<SkeletonSummonerEntity>of(SkeletonSummonerEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(1.2f, 3.4f));
	public static final DeferredHolder<EntityType<?>, EntityType<MagicalSkullEntity>> MAGICAL_SKULL = register("magical_skull",
			EntityType.Builder.<MagicalSkullEntity>of(MagicalSkullEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.4f, 0.4f));
	public static final DeferredHolder<EntityType<?>, EntityType<ManaArrowEntity>> MANA_ARROW = register("projectile_mana_arrow",
			EntityType.Builder.<ManaArrowEntity>of(ManaArrowEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<HomingFlameArrowEntity>> HOMING_FLAME_ARROW = register("projectile_homing_flame_arrow", EntityType.Builder.<HomingFlameArrowEntity>of(HomingFlameArrowEntity::new, MobCategory.MISC)
			.setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<RulersHandEntity>> RULERS_HAND = register("projectile_rulers_hand",
			EntityType.Builder.<RulersHandEntity>of(RulersHandEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<RulersAuthorityAuraEntity>> RULERS_AUTHORITY_AURA = register("rulers_authority_aura",
			EntityType.Builder.<RulersAuthorityAuraEntity>of(RulersAuthorityAuraEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(1).sized(0.1f, 0.1f));
	public static final DeferredHolder<EntityType<?>, EntityType<DKCTowerAuraEntity>> DKC_TOWER_AURA = register("dkc_tower_aura",
			EntityType.Builder.<DKCTowerAuraEntity>of(DKCTowerAuraEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).setTrackingRange(32).setUpdateInterval(20)
					.fireImmune().sized(1.0f, 1.0f));
	public static final DeferredHolder<EntityType<?>, EntityType<SpiderWebEntity>> SPIDER_WEB = register("projectile_spider_web",
			EntityType.Builder.<SpiderWebEntity>of(SpiderWebEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<ShadowStepEntity>> SHADOW_STEP = register("projectile_shadow_step",
			EntityType.Builder.<ShadowStepEntity>of(ShadowStepEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<LightBallEntity>> LIGHT_BALL = register("projectile_light_ball",
			EntityType.Builder.<LightBallEntity>of(LightBallEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<SlashEntity>> SLASH = register("projectile_slash",
			EntityType.Builder.<SlashEntity>of(SlashEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<Slash2Entity>> SLASH_2 = register("projectile_slash_2",
			EntityType.Builder.<Slash2Entity>of(Slash2Entity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<Slash3Entity>> SLASH_3 = register("projectile_slash_3",
			EntityType.Builder.<Slash3Entity>of(Slash3Entity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<Slash4Entity>> SLASH_4 = register("projectile_slash_4",
			EntityType.Builder.<Slash4Entity>of(Slash4Entity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<Slash5Entity>> SLASH_5 = register("projectile_slash_5",
			EntityType.Builder.<Slash5Entity>of(Slash5Entity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<Slash6Entity>> SLASH_6 = register("projectile_slash_6",
			EntityType.Builder.<Slash6Entity>of(Slash6Entity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<SlashEffectEntity>> SLASH_EFFECT = register("slash_effect",
			EntityType.Builder.<SlashEffectEntity>of(SlashEffectEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(2.8f, 1.6f));
	public static final DeferredHolder<EntityType<?>, EntityType<BasicAttackSlashEntity>> BASIC_ATTACK_SLASH = register("basic_attack_slash",
			EntityType.Builder.<BasicAttackSlashEntity>of(BasicAttackSlashEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(2.6f, 1.3f));
	public static final DeferredHolder<EntityType<?>, EntityType<GlacialPursuitEntity>> GLACIAL_PURSUIT = register("glacial_pursuit",
			EntityType.Builder.<GlacialPursuitEntity>of(GlacialPursuitEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(192).setUpdateInterval(1).sized(1.35f, 0.35f));
	public static final DeferredHolder<EntityType<?>, EntityType<WhiteFlameVfxEntity>> WHITE_FLAME_VFX = register("white_flame_vfx",
			EntityType.Builder.<WhiteFlameVfxEntity>of(WhiteFlameVfxEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(96).setUpdateInterval(1).sized(0.1f, 0.1f));
	public static final DeferredHolder<EntityType<?>, EntityType<FireMageVfxEntity>> FIRE_MAGE_VFX = register("fire_mage_vfx",
			EntityType.Builder.<FireMageVfxEntity>of(FireMageVfxEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(256).setUpdateInterval(1).sized(0.1f, 0.1f));
	public static final DeferredHolder<EntityType<?>, EntityType<HealerVfxEntity>> HEALER_VFX = register("healer_vfx",
			EntityType.Builder.<HealerVfxEntity>of(HealerVfxEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(256).setUpdateInterval(1).sized(0.2f, 0.2f));
	public static final DeferredHolder<EntityType<?>, EntityType<BarrierVfxEntity>> BARRIER_VFX = register("barrier_vfx",
			EntityType.Builder.<BarrierVfxEntity>of(BarrierVfxEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(256).setUpdateInterval(1).sized(0.2f, 0.2f));
	public static final DeferredHolder<EntityType<?>, EntityType<ArcaneVfxEntity>> ARCANE_VFX = register("arcane_vfx",
			EntityType.Builder.<ArcaneVfxEntity>of(ArcaneVfxEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(256).setUpdateInterval(1).sized(0.1f, 0.1f));
	public static final DeferredHolder<EntityType<?>, EntityType<RadiruBloodSpearEntity>> RADIRU_BLOOD_SPEAR = register("radiru_blood_spear",
			EntityType.Builder.<RadiruBloodSpearEntity>of(RadiruBloodSpearEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(96).setUpdateInterval(1).sized(0.35f, 0.35f));
	public static final DeferredHolder<EntityType<?>, EntityType<LiuSwordVfxEntity>> LIU_SWORD_VFX = register("liu_sword_vfx",
			EntityType.Builder.<LiuSwordVfxEntity>of(LiuSwordVfxEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(1).sized(0.1f, 0.1f));
	public static final DeferredHolder<EntityType<?>, EntityType<BeastVfxEntity>> BEAST_VFX = register("beast_vfx",
			EntityType.Builder.<BeastVfxEntity>of(BeastVfxEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(1).sized(0.1f, 0.1f));
	public static final DeferredHolder<EntityType<?>, EntityType<LiuSwordBeamEntity>> LIU_SWORD_BEAM = register("liu_sword_beam",
			EntityType.Builder.<LiuSwordBeamEntity>of(LiuSwordBeamEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(256).setUpdateInterval(1).sized(0.2f, 0.2f));
	public static final DeferredHolder<EntityType<?>, EntityType<DualWieldFlurryEntity>> DUAL_WIELD_FLURRY = register("dual_wield_flurry",
			EntityType.Builder.<DualWieldFlurryEntity>of(DualWieldFlurryEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(6.0f, 3.0f));
	public static final DeferredHolder<EntityType<?>, EntityType<CrossStrikeEntity>> CROSS_STRIKE = register("cross_strike",
			EntityType.Builder.<CrossStrikeEntity>of(CrossStrikeEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(4.8f, 2.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<QuickSlashesEntity>> QUICK_SLASHES = register("quick_slashes",
			EntityType.Builder.<QuickSlashesEntity>of(QuickSlashesEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(11.0f, 4.0f));
	public static final DeferredHolder<EntityType<?>, EntityType<SwordBeamProjectileEntity>> SWORD_BEAM_PROJECTILE = register("projectile_sword_beam",
			EntityType.Builder.<SwordBeamProjectileEntity>of(SwordBeamProjectileEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(1.8f, 1.0f));
	public static final DeferredHolder<EntityType<?>, EntityType<DragonBreatheEntity>> DRAGON_BREATHE = register("projectile_dragon_breathe",
			EntityType.Builder.<DragonBreatheEntity>of(DragonBreatheEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<ManaBulletEntity>> MANA_BULLET = register("projectile_mana_bullet",
			EntityType.Builder.<ManaBulletEntity>of(ManaBulletEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<DivineArrowEntity>> DIVINE_ARROW = register("projectile_divine_arrow",
			EntityType.Builder.<DivineArrowEntity>of(DivineArrowEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<ShamanMagicEntity>> SHAMAN_MAGIC = register("projectile_shaman_magic",
			EntityType.Builder.<ShamanMagicEntity>of(ShamanMagicEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<WhiteFlameEntity>> WHITE_FLAME = register("projectile_white_flame",
			EntityType.Builder.<WhiteFlameEntity>of(WhiteFlameEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<DragonFireballEntity>> DRAGON_FIREBALL = register("projectile_dragon_fireball",
			EntityType.Builder.<DragonFireballEntity>of(DragonFireballEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<RangerProjectileEntity>> RANGER_PROJECTILE = register("projectile_ranger_projectile", EntityType.Builder.<RangerProjectileEntity>of(RangerProjectileEntity::new, MobCategory.MISC)
			.setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<MagicMissileEntity>> MAGIC_MISSILE = register("projectile_magic_missile",
			EntityType.Builder.<MagicMissileEntity>of(MagicMissileEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<NecroBlastEntity>> NECRO_BLAST = register("projectile_necro_blast",
			EntityType.Builder.<NecroBlastEntity>of(NecroBlastEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<DemonEntity>> DEMON = register("demon",
			EntityType.Builder.<DemonEntity>of(DemonEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.6f, 2.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<DemonKnightEntity>> DEMON_KNIGHT = register("demon_knight",
			EntityType.Builder.<DemonKnightEntity>of(DemonKnightEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.7f, 2.6f));
	public static final DeferredHolder<EntityType<?>, EntityType<CerberusEntity>> CERBERUS = register("cerberus",
			EntityType.Builder.<CerberusEntity>of(CerberusEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(3f, 3.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<VulcanEntity>> VULCAN = register("vulcan",
			EntityType.Builder.<VulcanEntity>of(VulcanEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(2f, 5f));
	public static final DeferredHolder<EntityType<?>, EntityType<BaranEntity>> BARAN = register("baran",
			EntityType.Builder.<BaranEntity>of(BaranEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3).fireImmune().sized(1.0f, 3.0f));
	public static final DeferredHolder<EntityType<?>, EntityType<SilladBossEntity>> SILLAD_BOSS = register("sillad_boss",
			EntityType.Builder.<SilladBossEntity>of(SilladBossEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)
					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<KaiselinEntity>> KAISELIN = register("kaiselin",
			EntityType.Builder.<KaiselinEntity>of(KaiselinEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3).fireImmune().sized(3.25f, 2.6f));
	public static final DeferredHolder<EntityType<?>, EntityType<ShadowKaiselinEntity>> SHADOW_KAISELIN = register("shadow_kaiselin",
			EntityType.Builder.<ShadowKaiselinEntity>of(ShadowKaiselinEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3).fireImmune()
					.sized(3.25f, 2.6f));

	private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return REGISTRY.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			IgrisEntity.init();
			ShadowIgrisEntity.init();
			ShadowSold1Entity.init();
			SungJinWooEntity.init();
			OrcEntity.init();
			OrcShadowEntity.init();
			GemGolemEntity.init();
			AttackshardEntity.init();
			BeruBossEntity.init();
			BeruShadowEntity.init();
			CentipedeEntity.init();
			DKnight1Entity.init();
			DKnight2Entity.init();
			DKnight3Entity.init();
			KasakaEntity.init();
			MiniGemGolemEntity.init();
			SteelFangWolfEntity.init();
			SteelFangWolfShadowEntity.init();
			AncientSamuraiEntity.init();
			StoneGolemEntity.init();
			SpiderBossEntity.init();
			FireFlyEntity.init();
			PolarBearEntity.init();
			ShadowPolarBearEntity.init();
			IceElfEntity.init();
			BarukaEntity.init();
			ChoijongEntity.init();
			BaekYoonhoEntity.init();
			MagicEyeEntity.init();
			GoblinKingEntity.init();
			StatueOfGodEntity.init();
			KangTaeshikEntity.init();
			RedAntsEntity.init();
			ThomasAndreEntity.init();
			FangedKasakaEntity.init();
			FxPuddleEntity.init();
			FxspikEntity.init();
			StatueaxeEntity.init();
			StatuehammerEntity.init();
			StatueswordEntity.init();
			FuturisticGolemEntity.init();
			MutatedEntity.init();
			KamishShadowEntity.init();
			BloodRedComIgrisEntity.init();
			IgrisShadowEntity.init();
			AncientGolemEntity.init();
			HunterEntity.init();
			ChaHaeInEntity.init();
			KargalganEntity.init();
			SkeletonWarriorEntity.init();
			SkeletonBruteEntity.init();
			KamishEntity.init();
			SteelFangedLycanEntity.init();
			DummyPortalNormalEntity.init();
			DummyPortalRedEntity.init();
			DummyPortalPurpleEntity.init();
			PortalBeruEntity.init();
			PortalSEntity.init();
			RedGateEntity.init();
			PortalLushEntity.init();
			PortalKargalgansThroneRoomEntity.init();
			RandomCaveLargeEntity.init();
			PortalAncientGolemEntity.init();
			PortalEntity.init();
			Portal1Entity.init();
			SpawnerPortalEntity.init();
			Portal12Entity.init();
			PortalSewersEntity.init();
			PortalLabEntity.init();
			PortalJobChangeEntity.init();
			CartenonGateEntity.init();
			PortalCemeteryEntity.init();
			TrainingBotEntity.init();
			ShadowSoulEntity.init();
			FlagOfProtectionEntity.init();
			BellOfHealingEntity.init();
			BearTrapEntity.init();
			IcecleEntity.init();
			AfterImageEntity.init();
			AfterImage1Entity.init();
			AfterImage2Entity.init();
			SecretaryEntity.init();
			ElderBeastEntity.init();
			DetectEyeInvEntity.init();
			IceBallEntity.init();
			IceChunkEntity.init();
			DaggerSlashEntity.init();
			ArrowSplashEntity.init();
			GoblinClubEntity.init();
			GoblinArcherEntity.init();
			GoblinMageEntity.init();
			GoblinClubShadowEntity.init();
			GoblinArcherShadowEntity.init();
			GoblinMageShadowEntity.init();
			SlasheffectswordEntity.init();
			IgrisDeadBodyEntity.init();
			BeruDeadBodyEntity.init();
			CursedChainsEntity.init();
			DragonheadEntity.init();
			CurseMagicEntity.init();
			GreenOrcEntity.init();
			HighOrcEntity.init();
			ShadowGreenOrcEntity.init();
			ShadowHighOrcEntity.init();
			TuskShadowEntity.init();
			ShadowIronEntity.init();
			SkeletonSummonerEntity.init();
			MagicalSkullEntity.init();
			DemonEntity.init();
			DemonKnightEntity.init();
			CerberusEntity.init();
			VulcanEntity.init();
			BaranEntity.init();
			SilladBossEntity.init();
			KaiselinEntity.init();
			ShadowKaiselinEntity.init();
		});
	}

	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(IGRIS.get(), IgrisEntity.createAttributes().build());
		event.put(SHADOW_IGRIS.get(), ShadowIgrisEntity.createAttributes().build());
		event.put(SHADOW_SOLD_1.get(), ShadowSold1Entity.createAttributes().build());
		event.put(SUNG_JIN_WOO.get(), SungJinWooEntity.createAttributes().build());
		event.put(ORC.get(), OrcEntity.createAttributes().build());
		event.put(ORC_SHADOW.get(), OrcShadowEntity.createAttributes().build());
		event.put(GEM_GOLEM.get(), GemGolemEntity.createAttributes().build());
		event.put(ATTACKSHARD.get(), AttackshardEntity.createAttributes().build());
		event.put(BERU_BOSS.get(), BeruBossEntity.createAttributes().build());
		event.put(BERU_SHADOW.get(), BeruShadowEntity.createAttributes().build());
		event.put(CENTIPEDE.get(), CentipedeEntity.createAttributes().build());
		event.put(D_KNIGHT_1.get(), DKnight1Entity.createAttributes().build());
		event.put(D_KNIGHT_2.get(), DKnight2Entity.createAttributes().build());
		event.put(D_KNIGHT_3.get(), DKnight3Entity.createAttributes().build());
		event.put(KASAKA.get(), KasakaEntity.createAttributes().build());
		event.put(MINI_GEM_GOLEM.get(), MiniGemGolemEntity.createAttributes().build());
		event.put(STEEL_FANG_WOLF.get(), SteelFangWolfEntity.createAttributes().build());
		event.put(STEEL_FANG_WOLF_SHADOW.get(), SteelFangWolfShadowEntity.createAttributes().build());
		event.put(ANCIENT_SAMURAI.get(), AncientSamuraiEntity.createAttributes().build());
		event.put(STONE_GOLEM.get(), StoneGolemEntity.createAttributes().build());
		event.put(SPIDER_BOSS.get(), SpiderBossEntity.createAttributes().build());
		event.put(FIRE_FLY.get(), FireFlyEntity.createAttributes().build());
		event.put(POLAR_BEAR.get(), PolarBearEntity.createAttributes().build());
		event.put(SHADOW_POLAR_BEAR.get(), ShadowPolarBearEntity.createAttributes().build());
		event.put(ICE_ELF.get(), IceElfEntity.createAttributes().build());
		event.put(BARUKA.get(), BarukaEntity.createAttributes().build());
		event.put(CHOIJONG.get(), ChoijongEntity.createAttributes().build());
		event.put(BAEK_YOONHO.get(), BaekYoonhoEntity.createAttributes().build());
		event.put(MAGIC_EYE.get(), MagicEyeEntity.createAttributes().build());
		event.put(GOBLIN_KING.get(), GoblinKingEntity.createAttributes().build());
		event.put(STATUE_OF_GOD.get(), StatueOfGodEntity.createAttributes().build());
		event.put(KANG_TAESHIK.get(), KangTaeshikEntity.createAttributes().build());
		event.put(RED_ANTS.get(), RedAntsEntity.createAttributes().build());
		event.put(THOMAS_ANDRE.get(), ThomasAndreEntity.createAttributes().build());
		event.put(FANGED_KASAKA.get(), FangedKasakaEntity.createAttributes().build());
		event.put(FX_PUDDLE.get(), FxPuddleEntity.createAttributes().build());
		event.put(FXSPIK.get(), FxspikEntity.createAttributes().build());
		event.put(STATUEAXE.get(), StatueaxeEntity.createAttributes().build());
		event.put(STATUEHAMMER.get(), StatuehammerEntity.createAttributes().build());
		event.put(STATUESWORD.get(), StatueswordEntity.createAttributes().build());
		event.put(FUTURISTIC_GOLEM.get(), FuturisticGolemEntity.createAttributes().build());
		event.put(MUTATED.get(), MutatedEntity.createAttributes().build());
		event.put(KAMISH_SHADOW.get(), KamishShadowEntity.createAttributes().build());
		event.put(BLOOD_RED_COM_IGRIS.get(), BloodRedComIgrisEntity.createAttributes().build());
		event.put(IGRIS_SHADOW.get(), IgrisShadowEntity.createAttributes().build());
		event.put(ANCIENT_GOLEM.get(), AncientGolemEntity.createAttributes().build());
		event.put(HUNTER.get(), HunterEntity.createAttributes().build());
		event.put(CHA_HAE_IN.get(), ChaHaeInEntity.createAttributes().build());
		event.put(KARGALGAN.get(), KargalganEntity.createAttributes().build());
		event.put(SKELETON_WARRIOR.get(), SkeletonWarriorEntity.createAttributes().build());
		event.put(SKELETON_BRUTE.get(), SkeletonBruteEntity.createAttributes().build());
		event.put(KAMISH.get(), KamishEntity.createAttributes().build());
		event.put(STEEL_FANGED_LYCAN.get(), SteelFangedLycanEntity.createAttributes().build());
		event.put(DUMMY_PORTAL_NORMAL.get(), DummyPortalNormalEntity.createAttributes().build());
		event.put(DUMMY_PORTAL_RED.get(), DummyPortalRedEntity.createAttributes().build());
		event.put(DUMMY_PORTAL_PURPLE.get(), DummyPortalPurpleEntity.createAttributes().build());
		event.put(PORTAL_BERU.get(), PortalBeruEntity.createAttributes().build());
		event.put(PORTAL_S.get(), PortalSEntity.createAttributes().build());
		event.put(RED_GATE.get(), RedGateEntity.createAttributes().build());
		event.put(PORTAL_LUSH.get(), PortalLushEntity.createAttributes().build());
		event.put(PORTAL_KARGALGANS_THRONE_ROOM.get(), PortalKargalgansThroneRoomEntity.createAttributes().build());
		event.put(RANDOM_CAVE_LARGE.get(), RandomCaveLargeEntity.createAttributes().build());
		event.put(PORTAL_ANCIENT_GOLEM.get(), PortalAncientGolemEntity.createAttributes().build());
		event.put(PORTAL.get(), PortalEntity.createAttributes().build());
		event.put(PORTAL_1.get(), Portal1Entity.createAttributes().build());
		event.put(DATAPACK_GATE.get(), Portal1Entity.createAttributes().build());
		event.put(SPAWNER_PORTAL.get(), SpawnerPortalEntity.createAttributes().build());
		event.put(PORTAL_12.get(), Portal12Entity.createAttributes().build());
		event.put(PORTAL_SEWERS.get(), PortalSewersEntity.createAttributes().build());
		event.put(PORTAL_LAB.get(), PortalLabEntity.createAttributes().build());
		event.put(PORTAL_JOB_CHANGE.get(), PortalJobChangeEntity.createAttributes().build());
		event.put(CARTENON_GATE.get(), CartenonGateEntity.createAttributes().build());
		event.put(PORTAL_CEMETERY.get(), PortalCemeteryEntity.createAttributes().build());
		event.put(TRAINING_BOT.get(), TrainingBotEntity.createAttributes().build());
		event.put(SHADOW_SOUL.get(), ShadowSoulEntity.createAttributes().build());
		event.put(FLAG_OF_PROTECTION.get(), FlagOfProtectionEntity.createAttributes().build());
		event.put(BELL_OF_HEALING.get(), BellOfHealingEntity.createAttributes().build());
		event.put(BEAR_TRAP.get(), BearTrapEntity.createAttributes().build());
		event.put(ICECLE.get(), IcecleEntity.createAttributes().build());
		event.put(AFTER_IMAGE.get(), AfterImageEntity.createAttributes().build());
		event.put(AFTER_IMAGE_1.get(), AfterImage1Entity.createAttributes().build());
		event.put(AFTER_IMAGE_2.get(), AfterImage2Entity.createAttributes().build());
		event.put(SECRETARY.get(), SecretaryEntity.createAttributes().build());
		event.put(ESIL_RADIRU.get(), EsilRadiruEntity.createAttributes().build());
		event.put(ELDER_BEAST.get(), ElderBeastEntity.createAttributes().build());
		event.put(DETECT_EYE_INV.get(), DetectEyeInvEntity.createAttributes().build());
		event.put(ICE_BALL.get(), IceBallEntity.createAttributes().build());
		event.put(ICE_CHUNK.get(), IceChunkEntity.createAttributes().build());
		event.put(DAGGER_SLASH.get(), DaggerSlashEntity.createAttributes().build());
		event.put(ARROW_SPLASH.get(), ArrowSplashEntity.createAttributes().build());
		event.put(GOBLIN_CLUB.get(), GoblinClubEntity.createAttributes().build());
		event.put(GOBLIN_ARCHER.get(), GoblinArcherEntity.createAttributes().build());
		event.put(GOBLIN_MAGE.get(), GoblinMageEntity.createAttributes().build());
		event.put(GOBLIN_CLUB_SHADOW.get(), GoblinClubShadowEntity.createAttributes().build());
		event.put(GOBLIN_ARCHER_SHADOW.get(), GoblinArcherShadowEntity.createAttributes().build());
		event.put(GOBLIN_MAGE_SHADOW.get(), GoblinMageShadowEntity.createAttributes().build());
		event.put(SLASHEFFECTSWORD.get(), SlasheffectswordEntity.createAttributes().build());
		event.put(IGRIS_DEAD_BODY.get(), IgrisDeadBodyEntity.createAttributes().build());
		event.put(BERU_DEAD_BODY.get(), BeruDeadBodyEntity.createAttributes().build());
		event.put(CURSED_CHAINS.get(), CursedChainsEntity.createAttributes().build());
		event.put(DRAGONHEAD.get(), DragonheadEntity.createAttributes().build());
		event.put(CURSE_MAGIC.get(), CurseMagicEntity.createAttributes().build());
		event.put(GREEN_ORC.get(), GreenOrcEntity.createAttributes().build());
		event.put(HIGH_ORC.get(), HighOrcEntity.createAttributes().build());
		event.put(SHADOW_GREEN_ORC.get(), ShadowGreenOrcEntity.createAttributes().build());
		event.put(SHADOW_HIGH_ORC.get(), ShadowHighOrcEntity.createAttributes().build());
		event.put(TUSK_SHADOW.get(), TuskShadowEntity.createAttributes().build());
		event.put(SHADOW_IRON.get(), ShadowIronEntity.createAttributes().build());
		event.put(SKELETON_SUMMONER.get(), SkeletonSummonerEntity.createAttributes().build());
		event.put(MAGICAL_SKULL.get(), MagicalSkullEntity.createAttributes().build());
		event.put(DEMON.get(), DemonEntity.createAttributes().build());
		event.put(DEMON_KNIGHT.get(), DemonKnightEntity.createAttributes().build());
		event.put(CERBERUS.get(), CerberusEntity.createAttributes().build());
		event.put(VULCAN.get(), VulcanEntity.createAttributes().build());
		event.put(BARAN.get(), BaranEntity.createAttributes().build());
		event.put(SILLAD_BOSS.get(), SilladBossEntity.createAttributes().build());
		event.put(KAISELIN.get(), KaiselinEntity.createAttributes().build());
		event.put(SHADOW_KAISELIN.get(), ShadowKaiselinEntity.createAttributes().build());
	}

	@SubscribeEvent
	public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
		registerHostileGroundSpawn(event, MINI_GEM_GOLEM);
		registerHostileGroundSpawn(event, ORC);
		registerHostileGroundSpawn(event, FIRE_FLY);
		registerHostileGroundSpawn(event, RED_ANTS);
		registerHostileGroundSpawn(event, STEEL_FANGED_LYCAN);
		registerHostileGroundSpawn(event, MAGIC_EYE);
		registerHostileGroundSpawn(event, DEMON_KNIGHT);
		registerHostileGroundSpawn(event, DEMON);
		registerHostileGroundSpawn(event, STEEL_FANG_WOLF);
		registerHostileGroundSpawn(event, SKELETON_WARRIOR);
		registerHostileGroundSpawn(event, CENTIPEDE);
		registerHostileGroundSpawn(event, GREEN_ORC);
		registerHostileGroundSpawn(event, MUTATED);
		registerHostileGroundSpawn(event, FUTURISTIC_GOLEM);
		registerHostileGroundSpawn(event, HIGH_ORC);
		registerHostileGroundSpawn(event, SKELETON_BRUTE);
		registerHostileGroundSpawn(event, STONE_GOLEM);

		event.register(CHOIJONG.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) -> world.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)
						&& world.getRawBrightness(pos, 0) > 8,
				RegisterSpawnPlacementsEvent.Operation.REPLACE);
		event.register(SHADOW_POLAR_BEAR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) -> world.getDifficulty() != Difficulty.PEACEFUL
						&& Monster.isDarkEnoughToSpawn(world, pos, random)
						&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random),
				RegisterSpawnPlacementsEvent.Operation.REPLACE);
		event.register(SUNG_JIN_WOO.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) -> reason != MobSpawnType.NATURAL
						&& reason != MobSpawnType.CHUNK_GENERATION
						&& world.getDifficulty() != Difficulty.PEACEFUL
						&& Monster.isDarkEnoughToSpawn(world, pos, random)
						&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random),
				RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	/**
	 * Natural territory mobs share vanilla hostile-mob placement semantics: they
	 * start on a valid floor, do not spawn in peaceful difficulty, and respect the
	 * dimension's configured monster-light limits. Non-natural spawn types which
	 * deliberately ignore light (for example spawners) retain that behavior.
	 */
	private static <T extends Mob> void registerHostileGroundSpawn(RegisterSpawnPlacementsEvent event,
			DeferredHolder<EntityType<?>, EntityType<T>> entityType) {
		event.register(entityType.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				SololevelingModEntities::checkHostileGroundSpawnRules,
				RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	private static <T extends Mob> boolean checkHostileGroundSpawnRules(EntityType<T> entityType,
			ServerLevelAccessor world, MobSpawnType reason, BlockPos pos, RandomSource random) {
		return world.getDifficulty() != Difficulty.PEACEFUL
				&& (MobSpawnType.ignoresLightRequirements(reason) || Monster.isDarkEnoughToSpawn(world, pos, random))
				&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random);
	}
}
