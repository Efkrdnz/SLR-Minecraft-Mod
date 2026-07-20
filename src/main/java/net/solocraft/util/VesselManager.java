package net.solocraft.util;

import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber
public final class VesselManager {
	public static final String RULER = "ruler";
	public static final String MONARCH = "monarch";

	private static final List<VesselDefinition> DEFINITIONS = List.of(
			new VesselDefinition(RULER, "ashborn", 1, "Ashborn", "Shadow Monarch", "Command the dead and an endless shadow army."),
			new VesselDefinition(RULER, "christopher_reed", 2, "Christopher Reed", "Flame Incarnation", "Overwhelm enemies with a Ruler's destructive flame."),
			new VesselDefinition(RULER, "thomas_andre", 5, "Thomas Andre", "Goliath", "Crush the battlefield through unmatched physical force."),
			new VesselDefinition(RULER, "liu_zhigang", 6, "Liu Zhigang", "Sword Sovereign", "Release vast sword beams with absolute precision."),
			new VesselDefinition(RULER, "sung_il_hwan", 7, "Sung Il-Hwan", "Silent Authority", "Fight through speed, assassination, and Ruler power."),
			new VesselDefinition(RULER, "go_gunhee", 8, "Go Gunhee", "Brilliant Fragment", "Dominate close combat with reinforced authority."),
			new VesselDefinition(MONARCH, "sillad", 3, "Sillad", "Frost Monarch", "Freeze the battlefield and shatter immobilized enemies."),
			new VesselDefinition(MONARCH, "baran", 4, "Baran", "Monarch of White Flames", "Rule demonic flame, lightning, and infernal armies."),
			new VesselDefinition(MONARCH, "rakan", 9, "Rakan", "Monarch of Fangs", "Hunt with feral speed, claws, and bestial power."));

	private VesselManager() {
	}

	public static int assign(CommandContext<CommandSourceStack> context, String type, String identity) {
		VesselDefinition definition = definition(type, identity);
		if (definition == null)
			return 0;

		try {
			int changed = 0;
			int locked = 0;
			for (Entity target : EntityArgument.getEntities(context, "name")) {
				if (!(target instanceof ServerPlayer player))
					continue;
				AssignmentResult result = assignPlayer(player, definition, true);
				if (result == AssignmentResult.LOCKED) {
					locked++;
					player.sendSystemMessage(Component.literal("That vessel has reached the server limit.").withStyle(ChatFormatting.RED));
					continue;
				}
				if (result == AssignmentResult.SUCCESS) {
					JobChangeQuestManager.finish(player);
					player.sendSystemMessage(Component.literal("Vessel assigned: " + definition.commandDisplay())
							.withStyle(RULER.equals(definition.type()) ? ChatFormatting.AQUA : ChatFormatting.DARK_PURPLE));
					changed++;
				}
			}
			int result = changed;
			int failed = locked;
			context.getSource().sendSuccess(() -> Component.literal("Assigned " + definition.commandDisplay() + " to " + result
					+ " player(s)" + (failed > 0 ? "; " + failed + " blocked by the vessel limit" : "")), true);
			return changed;
		} catch (CommandSyntaxException exception) {
			context.getSource().sendFailure(Component.literal("Unable to resolve vessel targets"));
			return 0;
		}
	}

	public static int reset(CommandContext<CommandSourceStack> context) {
		try {
			int changed = 0;
			for (Entity target : EntityArgument.getEntities(context, "name")) {
				if (!(target instanceof ServerPlayer player))
					continue;
				resetPlayer(player);
				player.sendSystemMessage(Component.literal("Vessel status reset").withStyle(ChatFormatting.GRAY));
				changed++;
			}
			int result = changed;
			context.getSource().sendSuccess(() -> Component.literal("Reset vessel status for " + result + " player(s)"), true);
			return changed;
		} catch (CommandSyntaxException exception) {
			context.getSource().sendFailure(Component.literal("Unable to resolve vessel targets"));
			return 0;
		}
	}

	public static int openSelection(CommandContext<CommandSourceStack> context) {
		try {
			int opened = 0;
			for (Entity target : EntityArgument.getEntities(context, "name")) {
				if (!(target instanceof ServerPlayer player))
					continue;
				JobChangeQuestManager.openSelectionFromCommand(player);
				opened++;
			}
			int result = opened;
			context.getSource().sendSuccess(() -> Component.literal("Opened vessel selection for " + result + " player(s)"), true);
			return opened;
		} catch (CommandSyntaxException exception) {
			context.getSource().sendFailure(Component.literal("Unable to resolve vessel targets"));
			return 0;
		}
	}

	public static AssignmentResult assignPlayer(ServerPlayer player, String type, String identity, boolean enforceLimit) {
		VesselDefinition definition = definition(type, identity);
		return definition == null ? AssignmentResult.INVALID : assignPlayer(player, definition, enforceLimit);
	}

	public static AssignmentResult assignPlayer(ServerPlayer player, VesselDefinition definition, boolean enforceLimit) {
		if (player == null || definition == null)
			return AssignmentResult.INVALID;
		VesselClaimSavedData claims = VesselClaimSavedData.get(player.serverLevel());
		int limit = vesselLimit(player);
		if (enforceLimit && !claims.tryClaim(definition.key(), player.getUUID(), limit))
			return AssignmentResult.LOCKED;
		if (!enforceLimit)
			claims.claimExisting(definition.key(), player.getUUID());
		applyDefinition(player, definition);
		return AssignmentResult.SUCCESS;
	}

	public static void resetPlayer(ServerPlayer player) {
		if (player == null)
			return;
		if (LiuManifestationManager.isActive(player))
			LiuManifestationManager.restore(player);
		VesselClaimSavedData.get(player.serverLevel()).release(player.getUUID());
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			revokeAutomaticAuthority(capability);
			capability.vesselType = "";
			capability.vesselIdentity = "";
			capability.JOB = 0.0D;
			capability.syncPlayerVariables(player);
		});
		JobSkillManager.syncJobSkills(player);
	}

	public static void releaseClaim(ServerPlayer player) {
		if (player != null)
			VesselClaimSavedData.get(player.serverLevel()).release(player.getUUID());
	}

	public static boolean isRulerVessel(Entity entity) {
		return entity != null && entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(capability -> RULER.equals(capability.vesselType)).orElse(false);
	}

	public static String identity(Entity entity) {
		return entity == null ? "" : entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(capability -> capability.vesselIdentity).orElse("");
	}

	public static List<VesselDefinition> definitions() {
		return DEFINITIONS;
	}

	public static VesselDefinition definition(String type, String identity) {
		String normalizedType = type == null ? "" : type.toLowerCase();
		String normalizedIdentity = normalizeIdentity(identity);
		return DEFINITIONS.stream()
				.filter(value -> value.type().equals(normalizedType) && value.identity().equals(normalizedIdentity))
				.findFirst().orElse(null);
	}

	public static VesselDefinition definitionForJob(int jobId) {
		return DEFINITIONS.stream().filter(value -> value.jobId() == jobId).findFirst().orElse(null);
	}

	public static VesselDefinition currentDefinition(Entity entity) {
		if (entity == null)
			return null;
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		VesselDefinition explicit = definition(vars.vesselType, vars.vesselIdentity);
		return explicit != null ? explicit : definitionForJob((int) vars.JOB);
	}

	public static int vesselLimit(ServerPlayer player) {
		return player.level().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_MONARCH_LIMIT);
	}

	public static int claimCount(ServerPlayer player, VesselDefinition definition) {
		return VesselClaimSavedData.get(player.serverLevel()).count(definition.key());
	}

	public static int[] claimCounts(ServerPlayer player) {
		VesselClaimSavedData data = VesselClaimSavedData.get(player.serverLevel());
		int[] counts = new int[DEFINITIONS.size()];
		for (int i = 0; i < DEFINITIONS.size(); i++)
			counts[i] = data.count(DEFINITIONS.get(i).key());
		return counts;
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			reconcileExistingPlayer(player);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			reconcileExistingPlayer(player);
	}

	private static void reconcileExistingPlayer(ServerPlayer player) {
		VesselDefinition definition = currentDefinition(player);
		if (definition == null) {
			releaseClaim(player);
			return;
		}
		// Existing worlds are grandfathered so lowering the gamerule never deletes a job.
		assignPlayer(player, definition, false);
	}

	private static void applyDefinition(ServerPlayer player, VesselDefinition definition) {
		if (LiuManifestationManager.isActive(player))
			LiuManifestationManager.restore(player);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (RULER.equals(definition.type())) {
				if (!hasAbility(capability.abilities, "telekinesis")) {
					capability.abilities = appendAbility(capability.abilities, "telekinesis");
					capability.vesselGrantedAuthority = true;
				}
			} else {
				revokeAutomaticAuthority(capability);
			}
			capability.vesselType = definition.type();
			capability.vesselIdentity = definition.identity();
			capability.JOB = definition.jobId();
			capability.syncPlayerVariables(player);
		});
		JobSkillManager.syncJobSkills(player);
	}

	private static String normalizeIdentity(String identity) {
		if (identity == null)
			return "";
		return "sung_il_whan".equalsIgnoreCase(identity) ? "sung_il_hwan" : identity.toLowerCase();
	}

	private static void revokeAutomaticAuthority(SololevelingModVariables.PlayerVariables capability) {
		if (capability.vesselGrantedAuthority) {
			capability.abilities = removeAbility(capability.abilities, "telekinesis");
			capability.vesselGrantedAuthority = false;
		}
	}

	private static boolean hasAbility(String abilities, String ability) {
		return abilityList(abilities).stream().anyMatch(ability::equalsIgnoreCase);
	}

	private static String appendAbility(String abilities, String ability) {
		List<String> values = abilityList(abilities);
		if (values.stream().noneMatch(ability::equalsIgnoreCase))
			values.add(ability);
		return String.join(" ", values);
	}

	private static String removeAbility(String abilities, String ability) {
		List<String> values = abilityList(abilities);
		values.removeIf(ability::equalsIgnoreCase);
		return values.isEmpty() ? "\"\"" : String.join(" ", values);
	}

	private static List<String> abilityList(String abilities) {
		if (abilities == null || abilities.isBlank())
			return new ArrayList<>();
		return new ArrayList<>(Arrays.stream(abilities.replace('"', ' ').trim().split("\\s+"))
				.filter(value -> !value.isBlank()).toList());
	}

	public enum AssignmentResult {
		SUCCESS,
		LOCKED,
		INVALID
	}

	public record VesselDefinition(String type, String identity, int jobId, String name, String powerName, String description) {
		public String key() {
			return type + ":" + identity;
		}

		public String commandDisplay() {
			return (RULER.equals(type) ? "Ruler" : "Monarch") + " / " + name;
		}
	}
}
