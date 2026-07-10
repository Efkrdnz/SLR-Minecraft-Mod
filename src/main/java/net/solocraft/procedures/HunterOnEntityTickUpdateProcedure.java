package net.solocraft.procedures;

import net.solocraft.entity.HunterEntity;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;

public class HunterOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		HunterAIHelper.tickCooldowns(entity);
		String hunterClass = entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_HunterClass) : "";
		String _expectedName = (entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "")
				+ " Rank " + hunterClass;
		if (entity.getCustomName() == null || !entity.getCustomName().getString().equals(_expectedName)) {
			entity.setCustomName(Component.literal(_expectedName));
		}
		boolean legacyHandled = switch (hunterClass) {
			case "Fighter" -> invokeLegacy("net.solocraft.procedures.RandomHunterFighterTickProcedure", new Class<?>[]{double.class, double.class, double.class, Entity.class}, x, y, z, entity);
			case "Assassin" -> invokeLegacy("net.solocraft.procedures.RandomHunterAssassinTickProcedure", new Class<?>[]{Entity.class}, entity);
			case "Mage" -> invokeLegacy("net.solocraft.procedures.RandomHunterMageTickProcedure", new Class<?>[]{LevelAccessor.class, double.class, double.class, double.class, Entity.class}, world, x, y, z, entity);
			case "Ranger" -> invokeLegacy("net.solocraft.procedures.RandomHunterRangerTickProcedure", new Class<?>[]{Entity.class}, entity);
			case "Tanker" -> invokeLegacy("net.solocraft.procedures.RandomHunterTankerTickProcedure", new Class<?>[]{Entity.class}, entity);
			case "Healer" -> invokeLegacy("net.solocraft.procedures.RandomHunterHealerTickProcedure", new Class<?>[]{LevelAccessor.class, double.class, double.class, double.class, Entity.class}, world, x, y, z, entity);
			default -> true;
		};
		if (!legacyHandled) {
			runFallback(hunterClass, entity);
		}
	}

	private static boolean invokeLegacy(String className, Class<?>[] parameterTypes, Object... args) {
		try {
			Class<?> procedureClass = Class.forName(className);
			Method execute = procedureClass.getMethod("execute", parameterTypes);
			execute.invoke(null, args);
			return true;
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return false;
		}
	}

	private static void runFallback(String hunterClass, Entity entity) {
		switch (hunterClass) {
			case "Fighter" -> HunterAIHelper.fighterCombatTick(entity);
			case "Assassin" -> HunterAIHelper.assassinCombatTick(entity);
			case "Mage" -> HunterAIHelper.casterBacklineTick(entity);
			case "Ranger" -> HunterAIHelper.rangerCombatTick(entity);
			case "Tanker" -> HunterAIHelper.tankerCombatTick(entity);
			case "Healer" -> {
				HunterAIHelper.casterBacklineTick(entity);
				HunterAIHelper.healerSupportTick(entity);
			}
			default -> {
			}
		}
	}
}
