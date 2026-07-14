package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class VesselManager {
    public static final String RULER = "ruler";
    public static final String MONARCH = "monarch";

    private VesselManager() {
    }

    public static int assign(CommandContext<CommandSourceStack> context, String type, String identity) {
        VesselDefinition definition = definition(type, identity);
        if (definition == null)
            return 0;

        try {
            int changed = 0;
            for (Entity target : EntityArgument.getEntities(context, "name")) {
				if (target instanceof net.minecraft.server.level.ServerPlayer player && LiuManifestationManager.isActive(player))
					LiuManifestationManager.restore(player);
                target.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                    if (RULER.equals(definition.type)) {
                        if (!hasAbility(capability.abilities, "telekinesis")) {
                            capability.abilities = appendAbility(capability.abilities, "telekinesis");
                            capability.vesselGrantedAuthority = true;
                        }
                    } else {
                        revokeAutomaticAuthority(capability);
                    }
                    capability.vesselType = definition.type;
                    capability.vesselIdentity = definition.identity;
                    capability.JOB = definition.jobId;
                    capability.syncPlayerVariables(target);
                });
                JobSkillManager.syncJobSkills(target);
                target.sendSystemMessage(Component.literal("Vessel assigned: " + definition.commandDisplay)
                        .withStyle(RULER.equals(definition.type) ? ChatFormatting.AQUA : ChatFormatting.DARK_PURPLE));
                changed++;
            }
            String display = definition.commandDisplay;
            int result = changed;
            context.getSource().sendSuccess(() -> Component.literal("Assigned " + display + " to " + result + " player(s)"), true);
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
				if (target instanceof net.minecraft.server.level.ServerPlayer player && LiuManifestationManager.isActive(player))
					LiuManifestationManager.restore(player);
                target.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                    revokeAutomaticAuthority(capability);
                    capability.vesselType = "";
                    capability.vesselIdentity = "";
                    capability.JOB = 0.0D;
                    capability.syncPlayerVariables(target);
                });
                JobSkillManager.syncJobSkills(target);
                target.sendSystemMessage(Component.literal("Vessel status reset").withStyle(ChatFormatting.GRAY));
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

    public static boolean isRulerVessel(Entity entity) {
        return entity != null && entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .map(capability -> RULER.equals(capability.vesselType)).orElse(false);
    }

    public static String identity(Entity entity) {
        return entity == null ? "" : entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .map(capability -> capability.vesselIdentity).orElse("");
    }

    private static VesselDefinition definition(String type, String identity) {
        if (RULER.equals(type)) {
            return switch (identity) {
                case "ashborn" -> new VesselDefinition(RULER, identity, 1, "Ruler / Ashborn");
                case "christopher_reed" -> new VesselDefinition(RULER, identity, 2, "Ruler / Christopher Reed");
                case "thomas_andre" -> new VesselDefinition(RULER, identity, 5, "Ruler / Thomas Andre");
                case "liu_zhigang" -> new VesselDefinition(RULER, identity, 6, "Ruler / Liu Zhigang");
                case "sung_il_hwan", "sung_il_whan" -> new VesselDefinition(RULER, "sung_il_hwan", 7, "Ruler / Sung Il-Hwan");
                case "go_gunhee" -> new VesselDefinition(RULER, identity, 8, "Ruler / Go Gunhee");
                default -> null;
            };
        }
        if (MONARCH.equals(type)) {
            return switch (identity) {
                case "sillad" -> new VesselDefinition(MONARCH, identity, 3, "Monarch / Sillad");
                case "baran" -> new VesselDefinition(MONARCH, identity, 4, "Monarch / Baran");
                case "rakan" -> new VesselDefinition(MONARCH, identity, 9, "Monarch / Rakan");
                default -> null;
            };
        }
        return null;
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

    private record VesselDefinition(String type, String identity, int jobId, String commandDisplay) {
    }
}
