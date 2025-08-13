
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.solocraft.init;

import net.solocraft.SololevelingMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SololevelingModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SololevelingMod.MODID);
	public static final RegistryObject<CreativeModeTab> SOLO_LEVELING_WEAPONS = REGISTRY.register("solo_leveling_weapons",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.sololeveling.solo_leveling_weapons")).icon(() -> new ItemStack(SololevelingModItems.KASAKAS_VENOM_FANGS.get())).displayItems((parameters, tabData) -> {
				tabData.accept(SololevelingModItems.HAMMER.get());
				tabData.accept(SololevelingModItems.WAR_AXE.get());
				tabData.accept(SololevelingModItems.ICE_SPEAR.get());
				tabData.accept(SololevelingModItems.DEMON_KINGS_LONG_SWORD.get());
				tabData.accept(SololevelingModItems.FROST_BLADE.get());
				tabData.accept(SololevelingModItems.S_TIER_SWORD.get());
				tabData.accept(SololevelingModItems.A_TIER_SWORD.get());
				tabData.accept(SololevelingModItems.B_TIER_SWORD.get());
				tabData.accept(SololevelingModItems.C_TIER_SWORD.get());
				tabData.accept(SololevelingModItems.D_TIER_SWORD.get());
				tabData.accept(SololevelingModItems.E_TIER_SWORD.get());
				tabData.accept(SololevelingModItems.KATANA_STIER.get());
				tabData.accept(SololevelingModItems.KATANA_S.get());
				tabData.accept(SololevelingModItems.SWORD_ENRICHED_B.get());
				tabData.accept(SololevelingModItems.SWORD_NATURE_B.get());
				tabData.accept(SololevelingModItems.SWORD_TWINWING_C.get());
				tabData.accept(SololevelingModItems.SWORD_WARRIOR_D.get());
				tabData.accept(SololevelingModItems.SWORD_CURVED_D.get());
				tabData.accept(SololevelingModItems.KAMISH_WRATH_2.get());
				tabData.accept(SololevelingModItems.KAMISH_WRATH.get());
				tabData.accept(SololevelingModItems.DEMON_KINGS_DAGGER.get());
				tabData.accept(SololevelingModItems.KASAKAS_AWAKENED_VENOM_FANG.get());
				tabData.accept(SololevelingModItems.BARUKAS_DAGGER.get());
				tabData.accept(SololevelingModItems.KNIGHT_KILLER.get());
				tabData.accept(SololevelingModItems.MYTHIC_DAGGER.get());
				tabData.accept(SololevelingModItems.KASAKAS_VENOM_FANGS.get());
				tabData.accept(SololevelingModItems.GRAVITY_DAGGER.get());
				tabData.accept(SololevelingModItems.EMERALD_DAGGER.get());
				tabData.accept(SololevelingModItems.DAGGER_HEAT_A.get());
				tabData.accept(SololevelingModItems.DAGGER_DUOLITY_A.get());
				tabData.accept(SololevelingModItems.DAGGER_GOLDEN_B.get());
				tabData.accept(SololevelingModItems.DAGGER_CHAIN_C.get());
				tabData.accept(SololevelingModItems.DAGGER_KNIGHT_D.get());
				tabData.accept(SololevelingModItems.DAGGER_KARAMBIT_E.get());
				tabData.accept(SololevelingModItems.MANA_GUN.get());
				tabData.accept(SololevelingModItems.STORM_GRIAMORE.get());
				tabData.accept(SololevelingModItems.SPIRIT_BOW.get());
			})

					.build());
	public static final RegistryObject<CreativeModeTab> SOLO_LEVELING_MOBS = REGISTRY.register("solo_leveling_mobs",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.sololeveling.solo_leveling_mobs")).icon(() -> new ItemStack(SololevelingModItems.BLOOD_RED_COM_IGRIS_SPAWN_EGG.get())).displayItems((parameters, tabData) -> {
				tabData.accept(SololevelingModItems.SUNG_JIN_WOO_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.ORC_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.GEM_GOLEM_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.BERU_BOSS_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.CENTIPEDE_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.D_KNIGHT_1_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.D_KNIGHT_2_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.D_KNIGHT_3_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.MINI_GEM_GOLEM_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.STEEL_FANG_WOLF_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.ANCIENT_SAMURAI_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.STONE_GOLEM_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.SPIDER_BOSS_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.FIRE_FLY_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.POLAR_BEAR_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.ICE_ELF_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.BARUKA_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.CHOIJONG_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.BAEK_YOONHO_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.GOBLIN_KING_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.STATUE_OF_GOD_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.KANG_TAESHIK_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.RED_ANTS_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.THOMAS_ANDRE_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.FANGED_KASAKA_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.STATUEAXE_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.STATUEHAMMER_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.STATUESWORD_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.FUTURISTIC_GOLEM_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.MUTATED_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.BLOOD_RED_COM_IGRIS_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.ANCIENT_GOLEM_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.HUNTER_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.CHA_HAE_IN_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.KARGALGAN_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.SKELETON_WARRIOR_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.SKELETON_BRUTE_SPAWN_EGG.get());
			})

					.build());
	public static final RegistryObject<CreativeModeTab> SOLO_LEVELING_CHEAT_ITEMS = REGISTRY.register("solo_leveling_cheat_items",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.sololeveling.solo_leveling_cheat_items")).icon(() -> new ItemStack(SololevelingModItems.CLASS_CHOOSER.get())).displayItems((parameters, tabData) -> {
				tabData.accept(SololevelingModItems.LEVEL_ITEM.get());
				tabData.accept(SololevelingModItems.COIN_ITEM.get());
				tabData.accept(SololevelingModItems.COIN_ITEM_100.get());
				tabData.accept(SololevelingModItems.BLUEKEY.get());
				tabData.accept(SololevelingModItems.YELLOWKEY.get());
				tabData.accept(SololevelingModItems.REDKEY.get());
				tabData.accept(SololevelingModItems.SHADOW_MONARCH.get());
				tabData.accept(SololevelingModItems.TEST_PARTICLES.get());
				tabData.accept(SololevelingModItems.CLASS_CHOOSER.get());
				tabData.accept(SololevelingModItems.PURIFIED_BLOOD_OF_THE_DEMON_KING.get());
				tabData.accept(SololevelingModItems.WORLD_TREES_FRAGMENT.get());
				tabData.accept(SololevelingModItems.SPRING_WATER_OF_THE_ECHOING_FOREST.get());
				tabData.accept(SololevelingModItems.GIVE_BERU.get());
				tabData.accept(SololevelingModItems.GIVE_IGRIS.get());
				tabData.accept(SololevelingModItems.GRAND_MAGE.get());
				tabData.accept(SololevelingModItems.GG.get());
				tabData.accept(SololevelingModItems.ROTATION_DEVICE.get());
				tabData.accept(SololevelingModItems.MAGIC_READER.get());
				tabData.accept(SololevelingModItems.GIVE_KAMISH.get());
				tabData.accept(SololevelingModItems.JOB_CHANGE_DEBUG.get());
				tabData.accept(SololevelingModItems.KAMISH_TOOTH.get());
				tabData.accept(SololevelingModItems.ASSASIN_STARTERPACK.get());
				tabData.accept(SololevelingModItems.MAGE_STARTERPACK.get());
				tabData.accept(SololevelingModItems.FIGHTER_STARTERPACK.get());
				tabData.accept(SololevelingModItems.TANKER_STARTERPACK.get());
				tabData.accept(SololevelingModItems.HEALER_STARTERPACK.get());
				tabData.accept(SololevelingModItems.RANGER_STARTERPACK.get());
				tabData.accept(SololevelingModItems.RANDOM_SPECIAL_BOX.get());
				tabData.accept(SololevelingModItems.SELECTION_SPECIAL_BOX.get());
				tabData.accept(SololevelingModItems.ASSASSIN_MASTERY_ITEM.get());
				tabData.accept(SololevelingModItems.MAGE_MASTERY_ITEM.get());
				tabData.accept(SololevelingModItems.FIGHTER_MASTERY_ITEM.get());
				tabData.accept(SololevelingModItems.TANKER_MASTERY_ITEM.get());
				tabData.accept(SololevelingModItems.HEALER_MASTERY_ITEM.get());
				tabData.accept(SololevelingModItems.RANGER_MASTERY_ITEM.get());
				tabData.accept(SololevelingModItems.SMALL_HEALTH_POTION.get());
				tabData.accept(SololevelingModItems.MEDIUM_HEALTH_POTION.get());
				tabData.accept(SololevelingModItems.LARGE_HEALTH_POTION.get());
				tabData.accept(SololevelingModItems.SMALL_MANA_POTION.get());
				tabData.accept(SololevelingModItems.MEDIUM_MANA_POTION.get());
				tabData.accept(SololevelingModItems.LARGE_MANA_POTION.get());
				tabData.accept(SololevelingModItems.SMALL_FATIGUE_POTION.get());
				tabData.accept(SololevelingModItems.MEDIUM_FATIGUE_POTION.get());
				tabData.accept(SololevelingModItems.LARGE_FATIGUE_POTION.get());
				tabData.accept(SololevelingModItems.HOLY_WATER_OF_LIFE.get());
				tabData.accept(SololevelingModItems.GIVE_TUSK.get());
				tabData.accept(SololevelingModItems.DKC_LEVEL_ITEM.get());
			})

					.build());
	public static final RegistryObject<CreativeModeTab> DUNGEON_BLOCKS = REGISTRY.register("dungeon_blocks",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.sololeveling.dungeon_blocks")).icon(() -> new ItemStack(SololevelingModBlocks.DEEPSLATE_KEYBLOCK_BLUE.get())).displayItems((parameters, tabData) -> {
				tabData.accept(SololevelingModBlocks.GOBLIN_SPAWNER.get().asItem());
				tabData.accept(SololevelingModBlocks.DISAPPEARING_BLOCK.get().asItem());
				tabData.accept(SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get().asItem());
				tabData.accept(SololevelingModBlocks.DEEPSLATE_KEYBLOCK.get().asItem());
				tabData.accept(SololevelingModBlocks.DEEPSLATE_KEYBLOCK_BLUE.get().asItem());
				tabData.accept(SololevelingModBlocks.DEEPSLATE_KEYBLOCK_RED.get().asItem());
				tabData.accept(SololevelingModBlocks.CRYSTAL_GOLEM_SPAWNER.get().asItem());
				tabData.accept(SololevelingModBlocks.GOLEM_DROP_BLOCK_GEM.get().asItem());
				tabData.accept(SololevelingModBlocks.INSTANCE_DUNGEON_KEY_LOGGER.get().asItem());
				tabData.accept(SololevelingModBlocks.INSTANCE_COVER.get().asItem());
				tabData.accept(SololevelingModBlocks.HUNTER_RANK_EVALUATOR.get().asItem());
				tabData.accept(SololevelingModBlocks.WOODEN_PASSAGE_OPEN.get().asItem());
				tabData.accept(SololevelingModBlocks.CELL_DOOR_CLOSED.get().asItem());
				tabData.accept(SololevelingModBlocks.DUNGEON_FLOOR.get().asItem());
				tabData.accept(SololevelingModBlocks.DUNGEON_BRICKS.get().asItem());
				tabData.accept(SololevelingModBlocks.CELL_DOOR_OPEN.get().asItem());
				tabData.accept(SololevelingModBlocks.DUNGEON_WALL_SMALL.get().asItem());
				tabData.accept(SololevelingModBlocks.DUNGEON_BARREL.get().asItem());
				tabData.accept(SololevelingModBlocks.DUNGEON_TOOLS.get().asItem());
				tabData.accept(SololevelingModBlocks.SKELETON.get().asItem());
				tabData.accept(SololevelingModBlocks.DUNGEON_GRAVE_1.get().asItem());
				tabData.accept(SololevelingModBlocks.DUNGEON_GRAVE_2.get().asItem());
				tabData.accept(SololevelingModBlocks.PASSAGE_WALL.get().asItem());
				tabData.accept(SololevelingModBlocks.DUNGEON_WALL.get().asItem());
				tabData.accept(SololevelingModBlocks.EVALUATOR_TEST.get().asItem());
			})

					.build());
	public static final RegistryObject<CreativeModeTab> DUNGEON_PORTALS = REGISTRY.register("dungeon_portals",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.sololeveling.dungeon_portals")).icon(() -> new ItemStack(SololevelingModItems.CREATIVETABITEM.get())).displayItems((parameters, tabData) -> {
				tabData.accept(SololevelingModItems.JOB_KEY.get());
				tabData.accept(SololevelingModItems.INSTANCE_DUNGEON_KEY.get());
				tabData.accept(SololevelingModItems.PORTAL_BERU_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.RED_GATE_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.PORTAL_LUSH_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.PORTAL_KARGALGANS_THRONE_ROOM_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.RANDOM_CAVE_LARGE_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.PORTAL_ANCIENT_GOLEM_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.PORTAL_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.SPAWNER_PORTAL_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.PORTAL_12_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.PORTAL_SEWERS_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.PORTAL_LAB_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.PORTAL_CEMETERY_SPAWN_EGG.get());
			})

					.build());
	public static final RegistryObject<CreativeModeTab> EXPERIMENTAL = REGISTRY.register("experimental",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.sololeveling.experimental")).icon(() -> new ItemStack(SololevelingModItems.PURIFIED_BLOOD_OF_THE_DEMON_KING.get())).displayItems((parameters, tabData) -> {
				tabData.accept(SololevelingModItems.KAMISH_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.STEEL_FANGED_LYCAN_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.DUMMY_PORTAL_NORMAL_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.DUMMY_PORTAL_RED_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.DUMMY_PORTAL_PURPLE_SPAWN_EGG.get());
				tabData.accept(SololevelingModItems.HUNTER_ID.get());
			})

					.build());
	public static final RegistryObject<CreativeModeTab> RUNESTONES = REGISTRY.register("runestones",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.sololeveling.runestones")).icon(() -> new ItemStack(SololevelingModItems.RUNESTONE_SHADOW_EXCHANGE.get())).displayItems((parameters, tabData) -> {
				tabData.accept(SololevelingModItems.RUNESTONE_SHADOW_EXCHANGE.get());
				tabData.accept(SololevelingModItems.RUNESTONE_SHADOW_SPIRITUAL_BODY_MANIFESTATION.get());
				tabData.accept(SololevelingModItems.MURDERIOUS_INTENT_STONE.get());
				tabData.accept(SololevelingModItems.TELEKINESIS_STONE.get());
				tabData.accept(SololevelingModItems.STEALTH_STONE.get());
				tabData.accept(SololevelingModItems.RUNESTONE_BACKSTAB.get());
				tabData.accept(SololevelingModItems.RUNESTONE_DUALWIELD.get());
				tabData.accept(SololevelingModItems.RUNESTONE_SHADOWSTEP.get());
				tabData.accept(SololevelingModItems.RUNESTONE_QUICKSLASHES.get());
				tabData.accept(SololevelingModItems.RUNESTONE_CURSE_CHAINS.get());
				tabData.accept(SololevelingModItems.RUNESTONE_CURSE_SPHERE.get());
				tabData.accept(SololevelingModItems.RUNESTONE_CURSED_SMOKE.get());
				tabData.accept(SololevelingModItems.RUNESTONE_WATERSLASH.get());
				tabData.accept(SololevelingModItems.RUNESTONE_LIGHTBALL.get());
				tabData.accept(SololevelingModItems.RUNESTONE_FIREBALL.get());
				tabData.accept(SololevelingModItems.RUNESTONE_LIGHT_GOLEM.get());
				tabData.accept(SololevelingModItems.RUNESTONE_FLAME_TORNADO.get());
				tabData.accept(SololevelingModItems.RUNESTONE_HEAVY_FLAME.get());
				tabData.accept(SololevelingModItems.RUNESTONE_FLAME_VORTEX.get());
				tabData.accept(SololevelingModItems.RUNESTONE_FIRE_RAIN.get());
				tabData.accept(SololevelingModItems.RUNESTONE_DETECTION.get());
				tabData.accept(SololevelingModItems.RUNESTONE_MAGIC_MISSILES.get());
				tabData.accept(SololevelingModItems.RUNESTONE_SLASHDASH.get());
				tabData.accept(SololevelingModItems.RUNESTONE_SLASH_FURY.get());
				tabData.accept(SololevelingModItems.RUNESTONE_CRITICALSTRIKE.get());
				tabData.accept(SololevelingModItems.RUNESTONE_SWORDOF_LIGHT.get());
				tabData.accept(SololevelingModItems.RUNESTONE_SLAM.get());
				tabData.accept(SololevelingModItems.RUNESTONE_SWORD_DANCE.get());
				tabData.accept(SololevelingModItems.RUNESTONE_HEAL_BEAM.get());
				tabData.accept(SololevelingModItems.RUNESTONE_BLESSING_MARK.get());
				tabData.accept(SololevelingModItems.RUNESTONE_PURIFICATION.get());
				tabData.accept(SololevelingModItems.RUNESTONE_PHYSICAL.get());
				tabData.accept(SololevelingModItems.RUNESTONE_HASTE.get());
				tabData.accept(SololevelingModItems.RUNESTONE_OVERHEAL.get());
				tabData.accept(SololevelingModItems.RUNESTONE_TANK_LEAP.get());
				tabData.accept(SololevelingModItems.RUNESTONE_PROTECTION_MARK.get());
				tabData.accept(SololevelingModItems.RUNESTONE_REINFORCEMENT.get());
				tabData.accept(SololevelingModItems.RUNESTONE_SHIELD_BASH.get());
				tabData.accept(SololevelingModItems.RUNESTONE_WILLPOWER.get());
				tabData.accept(SololevelingModItems.RUNESTONE_TAUNT.get());
				tabData.accept(SololevelingModItems.RUNESTONE_SHARPSHOOTER.get());
				tabData.accept(SololevelingModItems.RUNESTONE_PROXIMITY_TRAP.get());
				tabData.accept(SololevelingModItems.RUNESTONE_HIGH_VALUE_TARGET.get());
				tabData.accept(SololevelingModItems.RUNESTONE_BACKSTEP.get());
				tabData.accept(SololevelingModItems.RUNESTONE_HAWKEYE.get());
				tabData.accept(SololevelingModItems.RUNESTONE_HYPERFOCUS.get());
			})

					.build());

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {

		if (tabData.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
			tabData.accept(SololevelingModBlocks.DUNGEON_BLOCK.get().asItem());
			tabData.accept(SololevelingModBlocks.DUNGEON_BLOCK_2.get().asItem());
		}

		if (tabData.getTabKey() == CreativeModeTabs.COMBAT) {
			tabData.accept(SololevelingModItems.CHOI_CLOAK_CHESTPLATE.get());
			tabData.accept(SololevelingModItems.SUNG_JIN_WOO_DRIP_CHESTPLATE.get());
			tabData.accept(SololevelingModItems.SUNG_JIN_WOO_DRIP_LEGGINGS.get());
			tabData.accept(SololevelingModItems.SUNG_JIN_WOO_DRIP_2_CHESTPLATE.get());
			tabData.accept(SololevelingModItems.SUNG_JIN_WOO_DRIP_2_LEGGINGS.get());
		}

		if (tabData.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
			tabData.accept(SololevelingModItems.TRAINING_BOT_SPAWN_EGG.get());
			tabData.accept(SololevelingModItems.AFTER_IMAGE_SPAWN_EGG.get());
			tabData.accept(SololevelingModItems.SECRETARY_SPAWN_EGG.get());
			tabData.accept(SololevelingModItems.ELDER_BEAST_SPAWN_EGG.get());
			tabData.accept(SololevelingModItems.GOBLIN_CLUB_SPAWN_EGG.get());
			tabData.accept(SololevelingModItems.GOBLIN_ARCHER_SPAWN_EGG.get());
			tabData.accept(SololevelingModItems.GOBLIN_MAGE_SPAWN_EGG.get());
			tabData.accept(SololevelingModItems.GREEN_ORC_SPAWN_EGG.get());
			tabData.accept(SololevelingModItems.HIGH_ORC_SPAWN_EGG.get());
			tabData.accept(SololevelingModItems.SKELETON_SUMMONER_SPAWN_EGG.get());
		}

		if (tabData.getTabKey() == CreativeModeTabs.INGREDIENTS) {
			tabData.accept(SololevelingModItems.MANA_CRYSTAL_E.get());
			tabData.accept(SololevelingModItems.MANA_CRYSTAL_D.get());
			tabData.accept(SololevelingModItems.MANA_CRYSTAL_C.get());
			tabData.accept(SololevelingModItems.MANA_CRYSTAL_B.get());
			tabData.accept(SololevelingModItems.MANA_CRYSTAL_A.get());
			tabData.accept(SololevelingModItems.MANA_CRYSTAL_S.get());
		}
	}
}
