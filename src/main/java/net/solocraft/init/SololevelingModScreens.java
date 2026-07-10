package net.solocraft.init;

import net.solocraft.client.gui.GuildComputerScreen;
import net.solocraft.client.gui.WTJoinScreen;
import net.solocraft.client.gui.UnlockedSkillsTab7Screen;
import net.solocraft.client.gui.UnlockedSkillsTab6Screen;
import net.solocraft.client.gui.UnlockedSkillsTab5Screen;
import net.solocraft.client.gui.UnlockedSkillsTab4Screen;
import net.solocraft.client.gui.UnlockedSkillsTab3Screen;
import net.solocraft.client.gui.UnlockedSkillsTab2Screen;
import net.solocraft.client.gui.UnlockedSkillsTab1Screen;
import net.solocraft.client.gui.TrainingGUIScreen;
import net.solocraft.client.gui.StorepotionScreen;
import net.solocraft.client.gui.StoreWeaponScreen;
import net.solocraft.client.gui.StoreWeaponReworkScreen;
import net.solocraft.client.gui.StorePotionNewScreen;
import net.solocraft.client.gui.StoreGUIScreen;
import net.solocraft.client.gui.SpecialCraftingGUIScreen;
import net.solocraft.client.gui.ShopScreen;
import net.solocraft.client.gui.ShadowSummonGUIScreen;
import net.solocraft.client.gui.ShadowDismissScreen;
import net.solocraft.client.gui.ShadowCommandScreen;
import net.solocraft.client.gui.ShadowGUIScreen;
import net.solocraft.client.gui.ShadowExchangeSaveScreen;
import net.solocraft.client.gui.ShadowExchangeSETScreen;
import net.solocraft.client.gui.ShadowExchangeMainGUIScreen;
import net.solocraft.client.gui.SelectionBoxGUIScreen;
import net.solocraft.client.gui.RewardPanelScreen;
import net.solocraft.client.gui.ReaderGUIScreen;
import net.solocraft.client.gui.QuestsScreen;
import net.solocraft.client.gui.PocketDimensionGUIScreen;
import net.solocraft.client.gui.PathScreen;
import net.solocraft.client.gui.PanelReworkScreen;
import net.solocraft.client.gui.PanelRework2Screen;
import net.solocraft.client.gui.PanelEarlyScreen;
import net.solocraft.client.gui.MiscItemsScreen;
import net.solocraft.client.gui.HuntersJoinScreen;
import net.solocraft.client.gui.HunterIDGuiScreen;
import net.solocraft.client.gui.FoodGuiScreen;
import net.solocraft.client.gui.FireGriamoreScreen;
import net.solocraft.client.gui.EquippedAbilitiesScreen;
import net.solocraft.client.gui.DailyQuestsScreen;
import net.solocraft.client.gui.ChooseClassScreen;
import net.solocraft.client.gui.AhjinJoinScreen;
import net.solocraft.client.gui.AbilitiesGUIScreen;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.gui.screens.MenuScreens;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SololevelingModScreens {
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(SololevelingModMenus.PANEL_EARLY.get(), PanelEarlyScreen::new);
			MenuScreens.register(SololevelingModMenus.REWARD_PANEL.get(), RewardPanelScreen::new);
			MenuScreens.register(SololevelingModMenus.TRAINING_GUI.get(), TrainingGUIScreen::new);
			MenuScreens.register(SololevelingModMenus.AHJIN_JOIN.get(), AhjinJoinScreen::new);
			MenuScreens.register(SololevelingModMenus.STORE_GUI.get(), StoreGUIScreen::new);
			MenuScreens.register(SololevelingModMenus.STORE_WEAPON.get(), StoreWeaponScreen::new);
			MenuScreens.register(SololevelingModMenus.STOREPOTION.get(), StorepotionScreen::new);
			MenuScreens.register(SololevelingModMenus.ABILITIES_GUI.get(), AbilitiesGUIScreen::new);
			MenuScreens.register(SololevelingModMenus.POCKET_DIMENSION_GUI.get(), PocketDimensionGUIScreen::new);
			MenuScreens.register(SololevelingModMenus.CHOOSE_CLASS.get(), ChooseClassScreen::new);
			MenuScreens.register(SololevelingModMenus.SPECIAL_CRAFTING_GUI.get(), SpecialCraftingGUIScreen::new);
			MenuScreens.register(SololevelingModMenus.FIRE_GRIAMORE.get(), FireGriamoreScreen::new);
			MenuScreens.register(SololevelingModMenus.SHADOW_GUI.get(), ShadowGUIScreen::new);
			MenuScreens.register(SololevelingModMenus.HUNTERS_JOIN.get(), HuntersJoinScreen::new);
			MenuScreens.register(SololevelingModMenus.WT_JOIN.get(), WTJoinScreen::new);
			MenuScreens.register(SololevelingModMenus.DAILY_QUESTS.get(), DailyQuestsScreen::new);
			MenuScreens.register(SololevelingModMenus.STORE_WEAPON_REWORK.get(), StoreWeaponReworkScreen::new);
			MenuScreens.register(SololevelingModMenus.SHOP.get(), ShopScreen::new);
			MenuScreens.register(SololevelingModMenus.FOOD_GUI.get(), FoodGuiScreen::new);
			MenuScreens.register(SololevelingModMenus.SHADOW_SUMMON_GUI.get(), ShadowSummonGUIScreen::new);
			MenuScreens.register(SololevelingModMenus.SHADOW_DISMISS.get(), ShadowDismissScreen::new);
			MenuScreens.register(SololevelingModMenus.SHADOW_COMMAND.get(), ShadowCommandScreen::new);
			MenuScreens.register(SololevelingModMenus.READER_GUI.get(), ReaderGUIScreen::new);
			MenuScreens.register(SololevelingModMenus.HUNTER_ID_GUI.get(), HunterIDGuiScreen::new);
			MenuScreens.register(SololevelingModMenus.MISC_ITEMS.get(), MiscItemsScreen::new);
			MenuScreens.register(SololevelingModMenus.SELECTION_BOX_GUI.get(), SelectionBoxGUIScreen::new);
			MenuScreens.register(SololevelingModMenus.PANEL_REWORK.get(), PanelReworkScreen::new);
			MenuScreens.register(SololevelingModMenus.PANEL_REWORK_2.get(), PanelRework2Screen::new);
			MenuScreens.register(SololevelingModMenus.EQUIPPED_ABILITIES.get(), EquippedAbilitiesScreen::new);
			MenuScreens.register(SololevelingModMenus.UNLOCKED_SKILLS_TAB_1.get(), UnlockedSkillsTab1Screen::new);
			MenuScreens.register(SololevelingModMenus.UNLOCKED_SKILLS_TAB_2.get(), UnlockedSkillsTab2Screen::new);
			MenuScreens.register(SololevelingModMenus.UNLOCKED_SKILLS_TAB_3.get(), UnlockedSkillsTab3Screen::new);
			MenuScreens.register(SololevelingModMenus.UNLOCKED_SKILLS_TAB_4.get(), UnlockedSkillsTab4Screen::new);
			MenuScreens.register(SololevelingModMenus.UNLOCKED_SKILLS_TAB_5.get(), UnlockedSkillsTab5Screen::new);
			MenuScreens.register(SololevelingModMenus.UNLOCKED_SKILLS_TAB_6.get(), UnlockedSkillsTab6Screen::new);
			MenuScreens.register(SololevelingModMenus.SHADOW_EXCHANGE_SET.get(), ShadowExchangeSETScreen::new);
			MenuScreens.register(SololevelingModMenus.SHADOW_EXCHANGE_SAVE.get(), ShadowExchangeSaveScreen::new);
			MenuScreens.register(SololevelingModMenus.SHADOW_EXCHANGE_MAIN_GUI.get(), ShadowExchangeMainGUIScreen::new);
			MenuScreens.register(SololevelingModMenus.STORE_POTION_NEW.get(), StorePotionNewScreen::new);
			MenuScreens.register(SololevelingModMenus.UNLOCKED_SKILLS_TAB_7.get(), UnlockedSkillsTab7Screen::new);
			MenuScreens.register(SololevelingModMenus.PATH.get(), PathScreen::new);
			MenuScreens.register(SololevelingModMenus.QUESTS.get(), QuestsScreen::new);
			// Guild System
			MenuScreens.register(SololevelingModMenus.GUILD_COMPUTER.get(), GuildComputerScreen::new);
		});
	}
}
