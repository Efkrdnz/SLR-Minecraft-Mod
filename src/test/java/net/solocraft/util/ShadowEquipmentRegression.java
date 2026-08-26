package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free source regressions for boss-shadow equipment persistence,
 * server authorization, summoned-state synchronization and ally-safe combat.
 */
public final class ShadowEquipmentRegression {
	private static final Path MAIN_SOURCE = Path.of(
			"src", "main", "java", "net", "solocraft");

	private ShadowEquipmentRegression() {
	}

	public static void main(String[] args) throws IOException {
		rosterPersistsTheRealEquipmentStack();
		serverOwnsTheBossEquipmentWhitelist();
		summonedEquipmentMarkersStaySynchronized();
		permanentRemovalReturnsEquipmentExactlyOnce();
		customizationRequestsRequireTheCurrentOwnedMenu();
		igrisLightningIsVisualOnlyAndManuallyFiltered();
		igrisBaseAreaDamageIsAlwaysGuarded();
		tuskOrbAmplificationCannotStackWithTheHeldOrbHandler();
	}

	private static void rosterPersistsTheRealEquipmentStack()
			throws IOException {
		String manager = read("util", "ShadowMonarchManager.java");
		expectTrue(manager.contains(
				"private static final String EQUIPMENT = \"equipment\"")
						&& manager.contains(
								"private static final String SHADOW_EQUIPMENT = \"sl_shadow_equipment\""),
				"Roster equipment and the summoned effect marker must remain distinct");

		String setter = method(manager,
				"public static boolean setEquipmentForDisplay");
		expectTrue(setter.contains(
				"shadow.put(EQUIPMENT, ItemStackData.save(stack, player.registryAccess()))")
						&& setter.contains(
								"player.getPersistentData().put(ROOT, root(player))"),
				"The full ItemStack tag must be saved on the authoritative roster record");

		String reader = method(manager,
				"private static ItemStack equipmentOf");
		expectTrue(reader.contains(
				"shadow.contains(EQUIPMENT, Tag.TAG_COMPOUND)")
						&& reader.contains(
								"ItemStackData.load(shadow.getCompound(EQUIPMENT), registries)"),
				"Roster equipment must deserialize through ItemStack rather than an item-id flag");

		String clone = method(manager,
				"public static void preserveProgressAfterPlayerClone");
		expectTrue(clone.contains(
				"replacementData.put(ROOT, originalData.getCompound(ROOT).copy())"),
				"Owner death cloning must deep-copy equipment with the full roster");

		String transientDrops = method(manager,
				"public static void dropStoredShadowInventory");
		expectFalse(transientDrops.contains("EQUIPMENT"),
				"Boss equipment must not enter the transient mana-stone drop path");
	}

	private static void serverOwnsTheBossEquipmentWhitelist()
			throws IOException {
		String manager = read("util", "ShadowMonarchManager.java");
		String whitelist = method(manager,
				"public static boolean isValidBossEquipment");
		expectTrue(whitelist.contains("stack.getCount() == 1")
						&& whitelist.contains("\"igris\".equals(type)")
						&& whitelist.contains(
								"SololevelingModItems.DEMON_KINGS_LONG_SWORD.get()")
						&& whitelist.contains("\"tusk\".equals(type)")
						&& whitelist.contains(
								"SololevelingModItems.ORB_OF_AVARICE.get()"),
				"The server whitelist must bind Igris and Tusk to their exact one-count artifacts");

		String setter = method(manager,
				"public static boolean setEquipmentForDisplay");
		expectTrue(setter.contains("player.level().isClientSide()")
						&& setter.contains(
								"!isValidBossEquipment(type, stack)"),
				"Roster writes must be server-only and independently revalidate the whitelist");

		String menu = read("world", "inventory",
				"ShadowCustomizationMenu.java");
		expectTrue(menu.contains(
				"ShadowMonarchManager.isValidBossEquipment(")
						&& menu.contains("getMaxStackSize()")
						&& menu.contains("return 1;"),
				"The vanilla menu slot must mirror the server whitelist and one-item limit");
	}

	private static void summonedEquipmentMarkersStaySynchronized()
			throws IOException {
		String manager = read("util", "ShadowMonarchManager.java");
		String sync = method(manager,
				"private static void syncEquipmentTag");
		expectTrue(sync.contains(
				"entity.getPersistentData().remove(SHADOW_EQUIPMENT)")
						&& sync.contains(
								"entity.getPersistentData().putString(SHADOW_EQUIPMENT"),
				"Summoned equipment sync must both install and clear stale markers");

		String summon = method(manager,
				"private static void tagSummonedEntity");
		expectTrue(summon.contains("syncEquipmentTag(spawned, shadow)"),
				"Every newly summoned or dimension-recalled shadow must receive its marker");

		String setter = method(manager,
				"public static boolean setEquipmentForDisplay");
		expectTrue(setter.contains("findSummonedEntity")
						&& setter.contains(
								"syncEquipmentTag(summoned, shadow)"),
				"Live equip and unequip must immediately update a loaded summon");

		String reconciliation = method(manager,
				"private static void synchronizeShadowLevel");
		expectTrue(reconciliation.contains(
				"syncEquipmentTag(shadowEntity, shadow)"),
				"A summon loaded after an offline change must reconcile from the roster");
	}

	private static void permanentRemovalReturnsEquipmentExactlyOnce()
			throws IOException {
		String manager = read("util", "ShadowMonarchManager.java");
		String recovery = method(manager,
				"private static void returnEquipmentToPlayer");
		int clear = recovery.indexOf("shadow.remove(EQUIPMENT)");
		int insert = recovery.indexOf("player.getInventory().add(equipment)");
		int drop = recovery.indexOf("player.drop(equipment, false)");
		expectTrue(clear >= 0 && insert > clear && drop > insert,
				"Recovery must clear roster ownership before inventory insertion and drop fallback");

		String reset = method(manager,
				"public static void resetPlayerProgress");
		expectTrue(reset.contains(
				"returnEquipmentToPlayer(player, roster.getCompound(index))")
						&& reset.indexOf("returnEquipmentToPlayer")
								< reset.indexOf("playerData.remove(ROOT)"),
				"Character reset must return every artifact before deleting the roster root");

		String trim = method(manager,
				"private static void trimOwnedShadows");
		expectTrue(trim.contains(
				"returnEquipmentToPlayer(player, shadow)")
						&& trim.indexOf("returnEquipmentToPlayer")
								< trim.indexOf("shadows.remove(removeIndex)"),
				"Admin trimming must recover an artifact before removing its shadow record");
	}

	private static void customizationRequestsRequireTheCurrentOwnedMenu()
			throws IOException {
		String packet = read("network",
				"ShadowSummonGUIButtonMessage.java");
		String open = method(packet,
				"private static void openCustomization");
		expectTrue(open.contains("entity instanceof ServerPlayer serverPlayer")
						&& open.contains(
								"entity.containerMenu instanceof ShadowSummonGUIMenu")
						&& open.contains("summonMenu.x != x")
						&& open.contains("summonMenu.y != y")
						&& open.contains("summonMenu.z != z"),
				"Customize packets must be bound to the sender's current summon menu");
		expectTrue(open.contains("type.isEmpty()")
						&& open.contains(
								"ShadowMonarchManager.hasShadowForDisplay(serverPlayer, type)")
						&& open.contains("NetworkHooks.openScreen"),
				"The server must re-check a non-empty owned shadow type before opening");

		String menu = read("world", "inventory",
				"ShadowCustomizationMenu.java");
		String stillValid = method(menu,
				"public boolean stillValid");
		expectTrue(stillValid.contains("player == this.entity")
						&& stillValid.contains("!this.shadowType.isEmpty()")
						&& stillValid.contains(
								"ShadowMonarchManager.hasShadowForDisplay(player, this.shadowType)"),
				"The customization menu must close if its shadow type or ownership disappears");
	}

	private static void igrisLightningIsVisualOnlyAndManuallyFiltered()
			throws IOException {
		String combat = read("util",
				"ShadowEquipmentCombatHandler.java");
		String storm = method(combat,
				"public static void tryIgrisImpactStorm");
		String lightning = method(combat,
				"private static void spawnVisualLightning");
		expectTrue(storm.contains(
				"SololevelingModItems.DEMON_KINGS_LONG_SWORD.get()"),
				"Igris equipment lightning must require the equipped sword");
		expectTrue(lightning.contains("LightningBolt")
						&& lightning.contains("setVisualOnly(true)")
						&& lightning.contains("level.addFreshEntity(lightning)"),
				"Igris equipment lightning bolts must remain visual-only");
		expectTrue(storm.contains(
				"ShadowMonarchManager.canShadowDamage(")
						&& storm.contains("DamageTypes.LIGHTNING_BOLT")
						&& storm.contains(".hurt("),
				"Visual lightning must be paired with explicitly filtered manual damage");

		int manualDamage = storm.indexOf(".hurt(");
		int filter = storm.lastIndexOf(
				"ShadowMonarchManager.canShadowDamage(", manualDamage);
		expectTrue(manualDamage >= 0 && filter >= 0,
				"Igris storm damage may only occur after the ally-safe predicate");
	}

	private static void igrisBaseAreaDamageIsAlwaysGuarded()
			throws IOException {
		assertEveryIgrisHurtIsGuarded("ShadowIgrisSpinProcedure.java", 2);
		assertEveryIgrisHurtIsGuarded("ShadowIgrisStabProcedure.java", 2);
		assertEveryIgrisHurtIsGuarded("ShadowIgrisSlamProcedure.java", 1);
	}

	private static void assertEveryIgrisHurtIsGuarded(String file,
			int expectedDamageSites) throws IOException {
		String source = read("procedures", file);
		expectTrue(count(source, ".hurt(") >= expectedDamageSites,
				file + " no longer exposes every expected base impact to the regression");
		for (int damage = source.indexOf(".hurt("); damage >= 0;
				damage = source.indexOf(".hurt(", damage + 1)) {
			int victimLoop = source.lastIndexOf(
					"for (Entity entityiterator", damage);
			int guard = source.lastIndexOf(
					"ShadowMonarchManager.canShadowDamage(entity, living)",
					damage);
			expectTrue(victimLoop >= 0 && guard > victimLoop,
					file + " contains an Igris AoE damage site outside canShadowDamage");
		}
	}

	private static void tuskOrbAmplificationCannotStackWithTheHeldOrbHandler()
			throws IOException {
		String combat = read("util",
				"ShadowEquipmentCombatHandler.java");
		String detector = method(combat,
				"public static boolean isOrbAmplifiedTuskDamage");
		expectTrue(detector.contains("TuskShadowEntity")
						&& detector.contains(
								"ShadowMonarchManager.isEquipmentEquipped(")
						&& detector.contains(
								"SololevelingModItems.ORB_OF_AVARICE.get()"),
				"Only an Orb-equipped Tusk may claim the equipment amplification");

		String amplifier = method(combat,
				"public static void amplifyEquippedTuskDamage");
		expectTrue(combat.contains(
				"private static final float TUSK_ORB_DAMAGE_MULTIPLIER = 2.0F")
						&& amplifier.contains(
				"isOrbAmplifiedTuskDamage(event.getSource())")
						&& amplifier.contains(
								"event.setAmount(event.getAmount() * TUSK_ORB_DAMAGE_MULTIPLIER)")
						&& count(amplifier, "event.setAmount(") == 1,
				"The Tusk equipment handler must apply one and only one multiplier");

		String heldOrb = read("util", "OrbOfAvariceManager.java");
		String heldAmplifier = method(heldOrb,
				"public static void amplifyMagicDamage");
		int equipmentBypass = heldAmplifier.indexOf(
				"ShadowEquipmentCombatHandler.isOrbAmplifiedTuskDamage");
		int heldCheck = heldAmplifier.indexOf("isHeldBy(caster)");
		expectTrue(equipmentBypass >= 0 && (heldCheck < 0
						|| equipmentBypass < heldCheck),
				"The held-Orb handler must bypass an already amplified equipped Tusk hit");
	}

	private static String method(String source, String signature) {
		int start = source.indexOf(signature);
		expectTrue(start >= 0, "Could not find method: " + signature);
		int bodyStart = source.indexOf('{', start + signature.length());
		expectTrue(bodyStart >= 0, "Could not find method body: " + signature);
		int depth = 0;
		for (int index = bodyStart; index < source.length(); index++) {
			char character = source.charAt(index);
			if (character == '{') {
				depth++;
			} else if (character == '}' && --depth == 0) {
				return source.substring(start, index + 1);
			}
		}
		throw new AssertionError("Unclosed method body: " + signature);
	}

	private static String read(String... parts) throws IOException {
		Path path = MAIN_SOURCE;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static int count(String source, String token) {
		int result = 0;
		for (int position = 0;
				(position = source.indexOf(token, position)) >= 0;
				position += token.length())
			result++;
		return result;
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean value, String message) {
		expectTrue(!value, message);
	}
}
