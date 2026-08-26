package net.solocraft.util;

import net.solocraft.entity.ai.ShadowIronCombatPolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Balance, AI, presentation, roster, and resource contracts for Shadow Iron. */
public final class ShadowIronAiRegression {
	private static final Path MAIN = Path.of(
			"src", "main", "java", "net", "solocraft");
	private static final Path RESOURCES = Path.of(
			"src", "main", "resources");

	private ShadowIronAiRegression() {
	}

	public static void main(String[] args) throws IOException {
		combatNumbersStayTankFocused();
		tauntAndRescueStayBounded();
		groundedAiHasLayeredRecovery();
		actionsUseOnlyGameplayAnimations();
		domainTextureStateIsSynchronized();
		tauntVisualsAndSoundsStayReadable();
		rosterAndCommandsExposeOneIron();
		developerPreviewGateIsServerAuthoritative();
		ironProgressesThroughMarshalButNotGrandMarshal();
		summonGuiUsesServerOwnedState();
		allSuppliedAssetsArePackaged();
	}

	private static void combatNumbersStayTankFocused() {
		expectEquals(8.0F, ShadowIronCombatPolicy.primaryDamage(8.0D, false),
				"A normal axe swing should use one attack-damage coefficient");
		expectEquals(9.2F, ShadowIronCombatPolicy.primaryDamage(8.0D, true),
				"A successful shield counter should receive a modest reward");
		expectEquals(2.8F, ShadowIronCombatPolicy.secondaryDamage(8.0D),
				"Cleave targets should take controlled splash damage");
		expectEquals(0.60F, ShadowIronCombatPolicy.blockReduction(false, false),
				"A faced melee hit should be meaningfully blocked");
		expectEquals(0.70F, ShadowIronCombatPolicy.blockReduction(true, false),
				"Iron should excel at shielding projectiles");
		expectEquals(0.35F, ShadowIronCombatPolicy.blockReduction(false, true),
				"Boss attacks must retain pressure through the shield");
		expectEquals(0.24F, ShadowIronCombatPolicy.fortificationReduction(8),
				"Roar fortification must have a hard mitigation cap");
	}

	private static void tauntAndRescueStayBounded() {
		expectEquals(100, ShadowIronCombatPolicy.tauntDuration(false, false),
				"Normal mobs should remain taunted for five seconds");
		expectEquals(60, ShadowIronCombatPolicy.tauntDuration(false, true),
				"Elite taunts should be shorter");
		expectEquals(30, ShadowIronCombatPolicy.tauntDuration(true, false),
				"Boss taunts should be soft and brief");
		expectTrue(ShadowIronCombatPolicy.shouldEmergencyIntercept(false,
				1.0D, 9.0D, true),
				"A threatening projectile should trigger a guardian intercept");
		expectFalse(ShadowIronCombatPolicy.shouldEmergencyIntercept(false,
				1.0D, 6.0D, false),
				"Healthy owners should not cause constant melee teleporting");
		expectEquals(0.15F, ShadowIronCombatPolicy.ownerDamageFraction(false),
				"A normal intercepted hit should mostly transfer away from the owner");
		expectEquals(0.35F, ShadowIronCombatPolicy.ownerDamageFraction(true),
				"Boss attacks must still hurt through an intercept");
	}

	private static void groundedAiHasLayeredRecovery() throws IOException {
		String entity = readMain("entity", "ShadowIronEntity.java");
		String goal = readMain("entity", "ai", "ShadowIronCombatGoal.java");
		expectTrue(entity.contains("new GroundPathNavigation(this, level)")
				&& entity.contains("new ShadowIronCombatGoal(this)")
				&& entity.indexOf("new ShadowIronCombatGoal(this)")
						< entity.indexOf("new ShadowFollowOwnerGoal(this)"),
				"Iron should be a grounded fighter with combat movement above following");
		expectTrue(goal.contains("DefaultRandomPos.getPosAway")
				&& goal.contains("tryRecoverStuckShadowNearOwner")
				&& goal.contains("isInWaterOrBubble()"),
				"Iron needs repath, terrain escape, safe recall, and water recovery");
		expectSame(ShadowIronCombatPolicy.RecoveryStage.REPATH,
				ShadowIronCombatPolicy.recoveryStage(20),
				"Twenty stalled ticks should trigger a repath");
		expectSame(ShadowIronCombatPolicy.RecoveryStage.ESCAPE,
				ShadowIronCombatPolicy.recoveryStage(40),
				"Forty stalled ticks should trigger an escape route");
		expectSame(ShadowIronCombatPolicy.RecoveryStage.RECALL,
				ShadowIronCombatPolicy.recoveryStage(70),
				"Seventy stalled ticks should allow safe owner recall");
	}

	private static void actionsUseOnlyGameplayAnimations() throws IOException {
		String entity = readMain("entity", "ShadowIronEntity.java");
		String model = readMain("entity", "model", "ShadowIronModel.java");
		String emissive = readMain("entity", "layer",
				"ShadowIronEmissiveLayer.java");
		String renderers = readMain("init",
				"SololevelingModEntityRenderers.java");
		expectTrue(entity.contains("thenPlay(\"attack\")")
				&& entity.contains("thenPlay(\"block\")")
				&& entity.contains("thenPlay(\"roar\")"),
				"Attack, shield, and Taunt must use their supplied animations");
		expectFalse(entity.contains("thenPlay(\"display\")")
				|| entity.contains("thenLoop(\"display\")"),
				"The screenshot-only display pose must never enter gameplay");
		expectTrue(model.contains("iron_shadow_domain.png")
				&& model.contains("isDomainBoosted()")
				&& emissive.contains("iron_shadow_em.png")
				&& emissive.contains("iron_shadow_em_domain.png")
				&& emissive.contains("isDomainBoosted()")
				&& renderers.contains("SHADOW_IRON.get(), ShadowIronRenderer::new"),
				"Normal/domain skins, emissives, and Iron's renderer must all be wired");
	}

	private static void domainTextureStateIsSynchronized() throws IOException {
		String entity = readMain("entity", "ShadowIronEntity.java");
		String started = readMain("procedures",
				"DomainBoostEffectStartedappliedProcedure.java");
		String expired = readMain("procedures",
				"DomainBoostEffectExpiresProcedure.java");
		expectTrue(entity.contains("EntityDataSerializers.BOOLEAN")
				&& entity.contains("public boolean isDomainBoosted()")
				&& entity.contains("public void setDomainBoosted(boolean boosted)")
				&& entity.contains("hasEffect(")
				&& entity.contains("SololevelingModMobEffects.DOMAIN_BOOST"),
				"Domain texture state must be authoritative and synchronized to render clients");
		expectTrue(started.contains("instanceof ShadowIronEntity iron")
				&& started.contains("iron.setDomainBoosted(true)")
				&& expired.contains("instanceof ShadowIronEntity iron")
				&& expired.contains("iron.setDomainBoosted(false)"),
				"Domain start and expiry callbacks must switch Iron in both directions");
	}

	private static void tauntVisualsAndSoundsStayReadable() throws IOException {
		String manager = readMain("util", "ShadowIronCombatManager.java");
		expectTrue(manager.contains("glowColor(owner, \"iron\")")
				&& manager.contains("TAUNT_HIGHLIGHT_PRIORITY = 325")
				&& manager.contains("PartyService.onlineMembers(owner)")
				&& manager.contains("TankerSkillManager.hasActiveTauntClaim(target)"),
				"Taunted enemies must use Iron's chosen colour without stealing player Taunt");
		expectTrue(manager.contains("SoundEvents.IRON_GOLEM_ATTACK")
				&& manager.contains("SoundEvents.SHIELD_BLOCK")
				&& manager.contains("SoundEvents.RAVAGER_ROAR")
				&& manager.contains("SoundEvents.ARMOR_EQUIP_IRON")
				&& manager.contains("SoundEvents.ENDERMAN_TELEPORT"),
				"Every major action needs a distinct, fitting audio cue");
		expectTrue(manager.contains("LivingChangeTargetEvent")
				&& manager.contains("LivingIncomingDamageEvent")
				&& manager.contains("safeGuardPosition")
				&& manager.contains("ownerDamageFraction")
				&& manager.contains("redirectedDamageFraction"),
				"Taunt locking and guardian interception must remain server authoritative");
		String entity = readMain("entity", "ShadowIronEntity.java");
		expectTrue(entity.contains("if (rescue && !isActionIdle())")
				&& entity.contains("clearAction();")
				&& manager.contains("ShadowIronEntity.Action.BLOCK"),
				"An emergency guard must interrupt attacks instead of waiting for them to finish");
	}

	private static void rosterAndCommandsExposeOneIron() throws IOException {
		String manager = readMain("util", "ShadowMonarchManager.java");
		String registry = readMain("init", "SololevelingModEntities.java");
		String menu = readMain("world", "inventory", "ShadowSummonGUIMenu.java");
		String screen = readMain("client", "gui", "ShadowSummonGUIScreen.java");
		expectTrue(manager.contains("case 13 -> \"iron\"")
				&& manager.contains("case \"iron\", \"shadow_iron\" -> \"iron\"")
				&& manager.contains("Math.max(0, Math.min(1, amount))")
				&& manager.contains("case \"iron\" -> SololevelingModEntities.SHADOW_IRON.get()"),
				"The command/roster route should recognize one unique Iron");
		expectTrue(registry.contains("SHADOW_IRON = register(\"shadow_iron\"")
				&& registry.contains("ShadowIronEntity.createAttributes()"),
				"Iron must have an entity type and registered attributes");
		expectTrue(menu.contains("SHADOW_TYPE_COUNT = 14")
				&& screen.contains("new SummonEntry(13, \"Iron\", \"iron\")")
				&& screen.contains("isGrandMarshalType(entry.type)"),
				"Iron should appear in the UI without receiving boss-only controls");
	}

	private static void summonGuiUsesServerOwnedState() throws IOException {
		String menu = readMain("world", "inventory",
				"ShadowSummonGUIMenu.java");
		String screen = readMain("client", "gui",
				"ShadowSummonGUIScreen.java");
		String packet = readMain("network",
				"ShadowSummonGUIButtonMessage.java");
		String manager = readMain("util", "ShadowMonarchManager.java");
		expectTrue(menu.contains("FIELD_OWNED = 9")
				&& menu.contains("FIELD_SUMMONED = 10")
				&& menu.contains("ownedCountForDisplay(")
				&& menu.contains("summonedCountForDisplay(")
				&& menu.contains("public boolean hasShadow(int buttonId)")
				&& menu.contains("public String shadowCountText(int buttonId)"),
				"Ownership and summon counts must travel through synchronized menu data");
		expectTrue(screen.contains("menu.hasShadow(entry.id)")
				&& screen.contains("menu.shadowCountText(entry.id)")
				&& !screen.contains(
						"ShadowMonarchManager.hasShadowForDisplay(entity, entry.type)"),
				"The client screen must never gate Iron using unsynchronized persistent data");
		expectTrue(packet.contains(
				"ShadowMonarchManager.typeForSummonButton(buttonID)")
				&& !packet.contains("case 13 -> \"iron\""),
				"The summon packet must share the authoritative button/type mapping");
		expectTrue(manager.contains("player.getAbilities().instabuild")
				&& manager.contains("case 13 -> \"iron\""),
				"Creative testing should bypass summon mana while button 13 maps to Iron");
	}

	private static void developerPreviewGateIsServerAuthoritative()
			throws IOException {
		String manager = readMain("util", "ShadowMonarchManager.java");
		String command = readMain("command", "SlrCommand.java");
		expectTrue(manager.contains(
						"public static boolean isShadowAvailableFor(Player player")
				&& manager.contains("\"iron\".equals(type)")
				&& manager.contains("DeveloperModeManager.isEnabled(player)")
				&& manager.contains(
						"type.isEmpty() || !isShadowAvailableFor(player, type)")
				&& manager.contains(
						"amount > 0 && !isShadowAvailableFor(player, type)")
				&& manager.contains(
						"summoned.discard()")
				&& manager.contains(
						"dismissLockedPreviewShadows(ServerPlayer owner)")
				&& manager.contains(
						"for (ServerLevel level : owner.server.getAllLevels())"),
				"Iron must be hidden, ungrantable, and unsummonable without the persisted developer flag");
		expectTrue(command.contains(
						"!ShadowMonarchManager.isShadowAvailableFor(")
				&& command.contains(
						"developer preview is enabled"),
				"The admin grant command must explain Iron's developer lock instead of bypassing it");
	}

	private static void ironProgressesThroughMarshalButNotGrandMarshal()
			throws IOException {
		String manager = readMain("util", "ShadowMonarchManager.java");
		expectTrue(manager.contains("case \"iron\" -> RANK_ELITE")
				&& manager.contains(
						"return \"iron\".equals(type) ? RANK_MARSHAL")
				&& manager.contains(
						"return isBoss(type) || \"iron\".equals(type)")
				&& manager.contains("if (isBoss(type))")
				&& manager.contains("return RANK_GRAND_MARSHAL")
				&& manager.contains("RANK_SCHEMA_VERSION = 3")
				&& manager.contains(
						"previousSchema < 3 && \"iron\".equals(type)")
				&& manager.contains("isMarshalProgressionType(type)"),
				"Iron must progress Elite through Marshal, remain outside Grand Marshal, and migrate old saves");
	}

	private static void allSuppliedAssetsArePackaged() throws IOException {
		Path assets = RESOURCES.resolve(Path.of("assets", "sololeveling"));
		for (Path relative : new Path[]{
				Path.of("geo", "shadow_iron.geo.json"),
				Path.of("animations", "shadow_iron.animation.json"),
				Path.of("textures", "entities", "iron_shadow.png"),
				Path.of("textures", "entities", "iron_shadow_domain.png"),
				Path.of("textures", "entities", "iron_shadow_em.png"),
				Path.of("textures", "entities", "iron_shadow_em_domain.png")}) {
			Path asset = assets.resolve(relative);
			expectTrue(Files.isRegularFile(asset) && Files.size(asset) > 0L,
					"Missing packaged Iron asset: " + relative);
		}
		Path normal = assets.resolve(Path.of("textures", "entities",
				"iron_shadow.png"));
		Path domain = assets.resolve(Path.of("textures", "entities",
				"iron_shadow_domain.png"));
		Path normalEmissive = assets.resolve(Path.of("textures", "entities",
				"iron_shadow_em.png"));
		Path domainEmissive = assets.resolve(Path.of("textures", "entities",
				"iron_shadow_em_domain.png"));
		expectTrue(Files.mismatch(normal, domain) >= 0L
				&& Files.mismatch(normalEmissive, domainEmissive) >= 0L,
				"Domain base and emissive textures must be real visual variants");
		String shadows = Files.readString(RESOURCES.resolve(Path.of("data",
				"minecraft", "tags", "entity_type", "shadows.json")));
		expectTrue(shadows.contains("sololeveling:shadow_iron"),
				"Iron must be recognized by the shared shadow entity tag");
	}

	private static String readMain(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static void expectEquals(float expected, float actual,
			String message) {
		if (Math.abs(expected - actual) > 0.0001F)
			throw new AssertionError(message + ": expected " + expected
					+ ", got " + actual);
	}

	private static void expectEquals(int expected, int actual,
			String message) {
		if (expected != actual)
			throw new AssertionError(message + ": expected " + expected
					+ ", got " + actual);
	}

	private static void expectSame(Object expected, Object actual,
			String message) {
		if (expected != actual)
			throw new AssertionError(message + ": expected " + expected
					+ ", got " + actual);
	}

	private static void expectTrue(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean condition, String message) {
		expectTrue(!condition, message);
	}
}
