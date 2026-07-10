
package net.solocraft.command;

import org.checkerframework.checker.units.qual.s;

import net.solocraft.procedures.SLRstatsvitalitysetProcedure;
import net.solocraft.procedures.SLRstatsvitalityaddProcedure;
import net.solocraft.procedures.SLRstatsstrengthsetProcedure;
import net.solocraft.procedures.SLRstatsstrengthaddProcedure;
import net.solocraft.procedures.SLRstatsspsetProcedure;
import net.solocraft.procedures.SLRstatsspaddProcedure;
import net.solocraft.procedures.SLRstatssensesetProcedure;
import net.solocraft.procedures.SLRstatsintelligencesetProcedure;
import net.solocraft.procedures.SLRstatsintelligenceaddProcedure;
import net.solocraft.procedures.SLRstatsagilitysetProcedure;
import net.solocraft.procedures.SLRstatsagilityaddProcedure;
import net.solocraft.procedures.SLRshopsetProcedure;
import net.solocraft.procedures.SLRshopgetProcedure;
import net.solocraft.procedures.SLRgoldsetProcedure;
import net.solocraft.procedures.SLRgoldresetProcedure;
import net.solocraft.procedures.SLRgoldgetProcedure;
import net.solocraft.procedures.SLRdimensionProcedure;
import net.solocraft.procedures.SLRcompletedDungeonsProcedure;
import net.solocraft.procedures.SLRcompletedDungeonPlayerProcedure;
import net.solocraft.procedures.SLRWhiteFlameMonarchProcedure;
import net.solocraft.procedures.SLRShadowMonarchProcedure;
import net.solocraft.procedures.SLRSetLevelProcedure;
import net.solocraft.procedures.SLRSRankProcedure;
import net.solocraft.procedures.SLRRewardsSetGoldsProcedure;
import net.solocraft.procedures.SLRRewardSetSkillPointsProcedure;
import net.solocraft.procedures.SLRRewardSetItemboxProcedure;
import net.solocraft.procedures.SLRRewardSetItemProcedure;
import net.solocraft.procedures.SLRRewardSetFullRecoveryProcedure;
import net.solocraft.procedures.SLRRewardCollectProcedure;
import net.solocraft.procedures.SLRResetProcedure;
import net.solocraft.procedures.SLRPlayerProcedure;
import net.solocraft.procedures.SLRPenaltyTriggerProcedure;
import net.solocraft.procedures.SLRJobResetProcedure;
import net.solocraft.procedures.SLRGrandMageProcedure;
import net.solocraft.procedures.SLRFrostMonarchProcedure;
import net.solocraft.procedures.SLRFinishDailyProcedure;
import net.solocraft.procedures.SLRERankProcedure;
import net.solocraft.procedures.SLRDungeonBreakProcedure;
import net.solocraft.procedures.SLRDRankProcedure;
import net.solocraft.procedures.SLRClassProcedure;
import net.solocraft.procedures.SLRCRankProcedure;
import net.solocraft.procedures.SLRCLassRandomProcedure;
import net.solocraft.procedures.SLRBRankProcedure;
import net.solocraft.procedures.SLRARankProcedure;
import net.solocraft.guild.GuildSavedData;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.common.util.FakePlayerFactory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;

import net.solocraft.dungeon.DungeonGenerator;
import net.solocraft.dungeon.DungeonTheme;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mod.EventBusSubscriber
public class SlrCommand {
	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(
				Commands.literal("slr").requires(s -> s.hasPermission(3)).then(Commands.argument("name", EntityArgument.players()).then(Commands.literal("player").then(Commands.argument("player", BoolArgumentType.bool()).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRPlayerProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("level").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0, 500)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRSetLevelProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("class").then(Commands.literal("assassin").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRClassProcedure.execute(arguments, 1);
					return 0;
				})).then(Commands.literal("mage").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRClassProcedure.execute(arguments, 2);
					return 0;
				})).then(Commands.literal("fighter").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRClassProcedure.execute(arguments, 3);
					return 0;
				})).then(Commands.literal("tanker").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRClassProcedure.execute(arguments, 4);
					return 0;
				})).then(Commands.literal("healer").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRClassProcedure.execute(arguments, 5);
					return 0;
				})).then(Commands.literal("ranger").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRClassProcedure.execute(arguments, 6);
					return 0;
				})).then(Commands.literal("random").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRCLassRandomProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("reset").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRResetProcedure.execute(world, arguments, entity);
					return 0;
				})).then(Commands.literal("job").then(Commands.literal("reset").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRJobResetProcedure.execute(arguments);
					return 0;
				})).then(Commands.literal("ShadowMonarch").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRShadowMonarchProcedure.execute(arguments);
					return 0;
				})).then(Commands.literal("GrandMage").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRGrandMageProcedure.execute(arguments);
					return 0;
				})).then(Commands.literal("FrostMonarch").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRFrostMonarchProcedure.execute(arguments);
					return 0;
				})).then(Commands.literal("MonarchOfWhiteFlames").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRWhiteFlameMonarchProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("rank").then(Commands.literal("E").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRERankProcedure.execute(entity);
					return 0;
				})).then(Commands.literal("D").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRDRankProcedure.execute(entity);
					return 0;
				})).then(Commands.literal("C").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRCRankProcedure.execute(entity);
					return 0;
				})).then(Commands.literal("B").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRBRankProcedure.execute(entity);
					return 0;
				})).then(Commands.literal("A").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRARankProcedure.execute(entity);
					return 0;
				})).then(Commands.literal("S").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRSRankProcedure.execute(world, x, y, z, entity);
					return 0;
				}))).then(Commands.literal("OP").then(Commands.literal("DungeonBreak").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRDungeonBreakProcedure.execute(world, x, y, z, entity);
					return 0;
				})).then(Commands.literal("TriggerPenaltyZone").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRPenaltyTriggerProcedure.execute(world, arguments);
					return 0;
				})).then(Commands.literal("FinishDaily").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRFinishDailyProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("shop").then(Commands.literal("set").then(Commands.argument("amount", DoubleArgumentType.doubleArg(1, 6)).then(Commands.argument("item", ItemArgument.item(event.getBuildContext())).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRshopsetProcedure.execute(arguments, entity);
					return 0;
				})))).then(Commands.literal("get").then(Commands.argument("amount", DoubleArgumentType.doubleArg(1, 6)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRshopgetProcedure.execute(arguments, entity);
					return 0;
				})))).then(Commands.literal("gold").then(Commands.literal("Set").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRgoldsetProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("Add").then(Commands.argument("amount", DoubleArgumentType.doubleArg(1)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRgoldgetProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("reset").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRgoldresetProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("stats").then(Commands.literal("skillpoints").then(Commands.literal("Set").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRstatsspsetProcedure.execute(arguments, entity);
					return 0;
				}))).then(Commands.literal("Add").then(Commands.argument("amount", DoubleArgumentType.doubleArg(1)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRstatsspaddProcedure.execute(arguments, entity);
					return 0;
				})))).then(Commands.literal("strength").then(Commands.literal("Set").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRstatsstrengthsetProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("Add").then(Commands.argument("amount", DoubleArgumentType.doubleArg(1)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRstatsstrengthaddProcedure.execute(arguments);
					return 0;
				})))).then(Commands.literal("vitality").then(Commands.literal("Set").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRstatsvitalitysetProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("Add").then(Commands.argument("amount", DoubleArgumentType.doubleArg(1)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRstatsvitalityaddProcedure.execute(arguments);
					return 0;
				})))).then(Commands.literal("agility").then(Commands.literal("Set").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRstatsagilitysetProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("Add").then(Commands.argument("amount", DoubleArgumentType.doubleArg(1)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRstatsagilityaddProcedure.execute(arguments);
					return 0;
				})))).then(Commands.literal("sense").then(Commands.literal("Set").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0, 100)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRstatssensesetProcedure.execute(arguments);
					return 0;
				})))).then(Commands.literal("intelligence").then(Commands.literal("Set").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRstatsintelligencesetProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("Add").then(Commands.argument("amount", DoubleArgumentType.doubleArg(1)).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRstatsintelligenceaddProcedure.execute(arguments);
					return 0;
				}))))).then(Commands.literal("debug").then(Commands.literal("Dimension").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRdimensionProcedure.execute(entity);
					return 0;
				})).then(Commands.literal("ClearedGates").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRcompletedDungeonsProcedure.execute(world, entity);
					return 0;
				})).then(Commands.literal("CurrentGatesStatus").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRcompletedDungeonPlayerProcedure.execute(world, entity);
					return 0;
				}))).then(Commands.literal("rewards").then(Commands.literal("collect").executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRRewardCollectProcedure.execute(arguments);
					return 0;
				})).then(Commands.literal("set").then(Commands.argument("slot", DoubleArgumentType.doubleArg(1, 3)).then(Commands.literal("RandomItem").then(Commands.argument("AutoCollect", BoolArgumentType.bool()).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRRewardSetItemboxProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("FullRecovery").then(Commands.argument("AutoCollect", BoolArgumentType.bool()).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRRewardSetFullRecoveryProcedure.execute(arguments);
					return 0;
				}))).then(Commands.literal("SkillPoints").then(Commands.argument("amount", DoubleArgumentType.doubleArg(1)).then(Commands.argument("AutoCollect", BoolArgumentType.bool()).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRRewardSetSkillPointsProcedure.execute(arguments);
					return 0;
				})))).then(Commands.literal("Golds").then(Commands.argument("amount", DoubleArgumentType.doubleArg(1)).then(Commands.argument("AutoCollect", BoolArgumentType.bool()).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRRewardsSetGoldsProcedure.execute(arguments);
					return 0;
				})))).then(Commands.literal("Item").then(Commands.argument("item", ItemArgument.item(event.getBuildContext())).then(Commands.argument("AutoCollect", BoolArgumentType.bool()).executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					SLRRewardSetItemProcedure.execute(arguments);
					return 0;
				})))))))
				.then(Commands.literal("shadows")
					.then(Commands.literal("add").then(Commands.argument("shadow", StringArgumentType.word()).executes(arguments -> {
						return modifyShadowSoldiers(arguments, 1);
					})))
					.then(Commands.literal("remove").then(Commands.argument("shadow", StringArgumentType.word()).executes(arguments -> {
						return modifyShadowSoldiers(arguments, -1);
					}))))
				// ── Guild commands ────────────────────────────────────────────
				.then(Commands.literal("guild")
					.then(Commands.literal("list").executes(arguments -> {
						var source = arguments.getSource();
						ServerLevel sl = source.getLevel();
						var guilds = GuildSavedData.get(sl).allGuilds();
						if (guilds.isEmpty()) {
							source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("§7No guilds exist yet."), false);
						} else {
							source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("§e══ Guild List ══"), false);
							for (var g : guilds) {
								final var gf = g;
								source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
									"§f" + gf.name + " §7(Lv." + gf.level + ")  §8Owner: §f" + gf.ownerName + "  §8Clears: §e" + gf.totalClears), false);
							}
						}
						return 0;
					}))
					.then(Commands.literal("level").then(Commands.argument("amount", DoubleArgumentType.doubleArg(1, 10)).executes(arguments -> {
						ServerLevel sl = arguments.getSource().getLevel();
						for (var target : EntityArgument.getPlayers(arguments, "name")) {
							var guild = GuildSavedData.get(sl).getGuildForPlayer(target.getUUID());
							if (guild != null) {
								guild.level = (int) DoubleArgumentType.getDouble(arguments, "amount");
								GuildSavedData.get(sl).markDirty();
								final var gf = guild;
								arguments.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
									"§aSet guild §e" + gf.name + " §alevel to §e" + gf.level), false);
							} else {
								final var tf = target;
								arguments.getSource().sendFailure(net.minecraft.network.chat.Component.literal(
									"§c" + tf.getName().getString() + " is not in any guild."));
							}
						}
						return 0;
					})))
					.then(Commands.literal("xp").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(arguments -> {
						ServerLevel sl = arguments.getSource().getLevel();
						for (var target : EntityArgument.getPlayers(arguments, "name")) {
							var guild = GuildSavedData.get(sl).getGuildForPlayer(target.getUUID());
							if (guild != null) {
								guild.xp = (long) DoubleArgumentType.getDouble(arguments, "amount");
								GuildSavedData.get(sl).markDirty();
								final var gf = guild;
								arguments.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
									"§aSet guild §e" + gf.name + " §aXP to §e" + gf.xp), false);
							} else {
								final var tf = target;
								arguments.getSource().sendFailure(net.minecraft.network.chat.Component.literal(
									"§c" + tf.getName().getString() + " is not in any guild."));
							}
						}
						return 0;
					})))
					.then(Commands.literal("give").executes(arguments -> {
						for (var target : EntityArgument.getPlayers(arguments, "name")) {
							target.getInventory().add(new ItemStack(SololevelingModItems.GUILD_COMPUTER.get()));
							final var tf = target;
							arguments.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
								"§aGave §eGuild Computer §ato §f" + tf.getName().getString()), false);
						}
						return 0;
					}))
					.then(Commands.literal("leave").executes(arguments -> {
						var source = arguments.getSource();
						ServerLevel sl = source.getLevel();
						net.minecraft.server.level.ServerPlayer self;
						try { self = source.getPlayerOrException(); }
						catch (Exception e) { source.sendFailure(net.minecraft.network.chat.Component.literal("§cMust be run as a player.")); return 0; }
						GuildSavedData data = GuildSavedData.get(sl);
						var guild = data.getGuildForPlayer(self.getUUID());
						if (guild == null) {
							source.sendFailure(net.minecraft.network.chat.Component.literal("§cYou are not in any guild."));
							return 0;
						}
						if (guild.ownerUUID.equals(self.getUUID())) {
							// Owner leaving = delete the guild entirely
							String guildName = guild.name;
							data.deleteGuild(guild.id);
							source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
								"§c§lGuild §e" + guildName + " §c§lhas been disbanded."), false);
						} else {
							// Regular member — just remove them
							String guildName = guild.name;
							guild.memberPermissions.removeIf(p -> p.playerUUID.equals(self.getUUID()));
							data.markDirty();
							source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
								"§7You have left §e" + guildName + "§7."), false);
						}
						return 1;
					}))
					.then(Commands.literal("remove").then(Commands.argument("target", EntityArgument.player()).executes(arguments -> {
						ServerLevel sl = arguments.getSource().getLevel();
						net.minecraft.server.level.ServerPlayer targetPlayer = EntityArgument.getPlayer(arguments, "target");
						for (var owner : EntityArgument.getPlayers(arguments, "name")) {
							var guild = GuildSavedData.get(sl).getGuildForPlayer(owner.getUUID());
							if (guild == null) {
								final var of = owner;
								arguments.getSource().sendFailure(net.minecraft.network.chat.Component.literal(
									"§c" + of.getName().getString() + " is not in any guild."));
								continue;
							}
							if (targetPlayer.getUUID().equals(guild.ownerUUID)) {
								arguments.getSource().sendFailure(net.minecraft.network.chat.Component.literal(
									"§cCannot remove the guild owner."));
								continue;
							}
							final var tf = targetPlayer;
							boolean removed = guild.memberPermissions.removeIf(p -> p.playerUUID.equals(tf.getUUID()));
							if (removed) {
								GuildSavedData.get(sl).markDirty();
								final var gf = guild;
								arguments.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
									"§aRemoved §f" + tf.getName().getString() + " §afrom §e" + gf.name), false);
							} else {
								arguments.getSource().sendFailure(net.minecraft.network.chat.Component.literal(
									"§c" + tf.getName().getString() + " is not a member of that guild."));
							}
						}
						return 0;
					})))
				)
				.then(Commands.literal("structure")
					.then(Commands.argument("dungeon", StringArgumentType.word())
						.executes(arguments -> {
							String dungeonName = StringArgumentType.getString(arguments, "dungeon");
							List<String> structures = structuresForDungeon(dungeonName);
							if (structures == null || structures.isEmpty()) {
								arguments.getSource().sendFailure(Component.literal("§cUnknown dungeon structure set: §e" + dungeonName
										+ "§c. Try: §7" + String.join(", ", STRUCTURE_SETS.keySet())));
								return 0;
							}
							int totalPlaced = 0;
							for (var target : EntityArgument.getPlayers(arguments, "name")) {
								totalPlaced += placeStructureGallery(target.serverLevel(), target.blockPosition().offset(8, 0, 8), structures);
							}
							final int placed = totalPlaced;
							final String normalizedName = normalizeDungeonName(dungeonName);
							arguments.getSource().sendSuccess(() -> Component.literal("§aPlaced §e" + placed + " §a" + normalizedName + " structure option(s)."), false);
							return placed > 0 ? 1 : 0;
						})
					)
				)
				// ── /slr <target> generate dungeon <complexity> [theme] ──────────────
				.then(Commands.literal("generate")
					.then(Commands.literal("dungeon")
						.then(Commands.argument("complexity", IntegerArgumentType.integer(1, 10))
							.executes(arguments -> {
								net.minecraft.server.level.ServerPlayer target;
								try { target = EntityArgument.getPlayer(arguments, "name"); }
								catch (Exception e) {
									arguments.getSource().sendFailure(net.minecraft.network.chat.Component.literal("§cTarget player not found."));
									return 0;
								}
								DungeonTheme theme = DungeonTheme.random();
								net.minecraft.core.BlockPos origin = target.blockPosition();
								String result = DungeonGenerator.generate(target.serverLevel(), origin,
										IntegerArgumentType.getInteger(arguments, "complexity"), theme);
								arguments.getSource().sendSuccess(
										() -> net.minecraft.network.chat.Component.literal(result), false);
								return 1;
							})
							.then(Commands.argument("theme", StringArgumentType.word())
								.executes(arguments -> {
									net.minecraft.server.level.ServerPlayer target;
									try { target = EntityArgument.getPlayer(arguments, "name"); }
									catch (Exception e) {
										arguments.getSource().sendFailure(net.minecraft.network.chat.Component.literal("§cTarget player not found."));
										return 0;
									}
									DungeonTheme theme = DungeonTheme.fromString(
											StringArgumentType.getString(arguments, "theme"));
									net.minecraft.core.BlockPos origin = target.blockPosition();
									String result = DungeonGenerator.generate(target.serverLevel(), origin,
											IntegerArgumentType.getInteger(arguments, "complexity"), theme);
									arguments.getSource().sendSuccess(
											() -> net.minecraft.network.chat.Component.literal(result), false);
									return 1;
								})
							)
						)
					)
				)
			));
	}

	private static int modifyShadowSoldiers(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> arguments, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		String shadow = StringArgumentType.getString(arguments, "shadow");
		int changed = 0;
		for (var target : EntityArgument.getPlayers(arguments, "name")) {
			if (ShadowMonarchManager.modifyShadowAmount(target, shadow, amount)) {
				changed++;
				final var tf = target;
				final String action = amount > 0 ? "added to" : "removed from";
				arguments.getSource().sendSuccess(() -> Component.literal("§aShadow §e" + shadow + " §a" + action + " §f" + tf.getName().getString() + "§a."), false);
			} else {
				final var tf = target;
				arguments.getSource().sendFailure(Component.literal("§cUnknown shadow type §e" + shadow + " §cfor §f" + tf.getName().getString() + "§c."));
			}
		}
		return changed;
	}

	private static final Map<String, List<String>> STRUCTURE_SETS = Map.ofEntries(
			Map.entry("instance", List.of("instance_first_room", "instancestart", "dunduninstance1", "dunduninstance2", "instanceboss", "instancegoblin", "instancegoblinlycan", "instancegoblinnlycan", "instancelycan")),
			Map.entry("instancedungeon", List.of("instance_first_room", "instancestart", "dunduninstance1", "dunduninstance2", "instanceboss", "instancegoblin", "instancegoblinlycan", "instancegoblinnlycan", "instancelycan")),
			Map.entry("kasaka", List.of("updatedkasakadungeon", "kasakadungeon")),
			Map.entry("erank", List.of("erankstart", "erankroom1", "erankroom2", "erankroom3", "erankroom4", "erankleftrightand", "erankrightleftand", "erankbig1", "erankbig2", "erankboss")),
			Map.entry("e", List.of("erankstart", "erankroom1", "erankroom2", "erankroom3", "erankroom4", "erankleftrightand", "erankrightleftand", "erankbig1", "erankbig2", "erankboss")),
			Map.entry("cemetery", List.of("b_rank_cemetery_enterance", "b_rank_cemetery_corridor1", "b_rank_cemetery_corridor2", "b_rank_cemetery_corridor3", "b_rank_cemetery_corridor4", "b_rank_cemetery_turn_left",
					"b_rank_cemetery_turn_right", "b_rank_cemetery_mid1", "b_rank_cemetery_mid2", "b_rank_cemetery_boss")),
			Map.entry("brank", List.of("b_rank_cemetery_enterance", "b_rank_cemetery_corridor1", "b_rank_cemetery_corridor2", "b_rank_cemetery_corridor3", "b_rank_cemetery_corridor4", "b_rank_cemetery_turn_left",
					"b_rank_cemetery_turn_right", "b_rank_cemetery_mid1", "b_rank_cemetery_mid2", "b_rank_cemetery_boss")),
			Map.entry("lab", List.of("labdunstart", "labduncor1", "labduncor2", "labduncor3", "labdunrturn", "labdunlturn", "labdunboss")),
			Map.entry("large", List.of("updatedlargerandstart", "bigroom1", "bigroom2", "bigroom3", "bigroom4", "bigroom5", "bigroomboss")),
			Map.entry("largecave", List.of("updatedlargerandstart", "bigroom1", "bigroom2", "bigroom3", "bigroom4", "bigroom5", "bigroomboss")),
			Map.entry("kamish", List.of("kamishupdatedstart", "kamishroom1", "kamishroom2", "kamishroom3", "kamishblock", "kamishboss")),
			Map.entry("dkc", List.of("dkc_left", "dkc_middle", "dkc_middle_boss", "dkc_middle_boss_baran", "dkc_middle_boss_cerberus", "dkc_right")),
			Map.entry("demoncastle", List.of("dkc_left", "dkc_middle", "dkc_middle_boss", "dkc_middle_boss_baran", "dkc_middle_boss_cerberus", "dkc_right")),
			Map.entry("kargalgan", List.of("dun_kargalgan_enterance", "dun_kargalgan", "dun_kargalgan_bossroom")),
			Map.entry("igris", List.of("jobchange_dungeon1", "updateddungeonigris", "dungeonigris", "igrisdungeon", "igrisdungeon1", "igrisdungeon2")),
			Map.entry("beru", List.of("updateddungeonberu", "berudungeon", "dungeonberu", "dungeonberu2")),
			Map.entry("ancientgolem", List.of("dungeon_ancientgolem")),
			Map.entry("lush", List.of("lushcave")),
			Map.entry("random", List.of("drankdunnew", "updatedrandomdungeon", "randomdun1", "dungeon1", "testroom1", "testroom2", "testroom3", "testroom11", "testroom21", "testroom31")));

	private static List<String> structuresForDungeon(String dungeonName) {
		return STRUCTURE_SETS.get(normalizeDungeonName(dungeonName));
	}

	private static String normalizeDungeonName(String dungeonName) {
		return dungeonName.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
	}

	private static int placeStructureGallery(ServerLevel level, BlockPos origin, List<String> structureNames) {
		int placed = 0;
		int xOffset = 0;
		for (String structureName : structureNames) {
			StructureTemplate template = level.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", structureName));
			if (template == null || template.getSize().getX() <= 0 || template.getSize().getZ() <= 0) {
				continue;
			}
			BlockPos placeAt = origin.offset(xOffset, 0, 0);
			template.placeInWorld(level, placeAt, placeAt, new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), level.random, 3);
			xOffset += Math.max(8, template.getSize().getX()) + 6;
			placed++;
		}
		return placed;
	}
}
