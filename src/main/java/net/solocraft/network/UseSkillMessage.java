
package net.solocraft.network;

import net.solocraft.procedures.SkillSlotHelper;
import net.solocraft.procedures.UseSkillOnKeyReleasedProcedure;
import net.solocraft.procedures.UseSkillOnKeyPressedProcedure;
import net.solocraft.util.MageQTEHelper;
import net.solocraft.SololevelingMod;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class UseSkillMessage {
	int type, pressedms;

	public UseSkillMessage(int type, int pressedms) {
		this.type = type;
		this.pressedms = pressedms;
	}

	public UseSkillMessage(FriendlyByteBuf buffer) {
		this.type = buffer.readInt();
		this.pressedms = buffer.readInt();
	}

	public static void buffer(UseSkillMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.type);
		buffer.writeInt(message.pressedms);
	}

	public static void handler(UseSkillMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			pressAction(context.getSender(), message.type, message.pressedms);
		});
		context.setPacketHandled(true);
	}

	public static void pressAction(Player entity, int type, int pressedms) {
		if (entity == null)
			return;
		Level world = entity.level();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(entity.blockPosition()))
			return;
		if (type == 0) {
			toggleSkillPage(entity);
		}
		if (type >= 10 && type <= 17) {
			pressHotbarSlot(entity, type - 9);
		}
		if (type >= 20 && type <= 27) {
			releaseHotbarSlot(entity, pressedms);
		}
	}

	private static void toggleSkillPage(Player entity) {
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.PskillPage = capability.PskillPage >= 2 ? 1 : 2;
			capability.syncPlayerVariables(entity);
		});
	}

	private static void pressHotbarSlot(Player entity, int hotbarSlot) {
		Level world = entity.level();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (!capability.combatmode)
				return;
			int slot = SkillSlotHelper.activeSlot(entity, hotbarSlot);
			capability.PselectedPower = SkillSlotHelper.getSlot(capability, slot);
			capability.Skillcycle = hotbarSlot;
			capability.syncPlayerVariables(entity);
		});
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (!vars.combatmode || vars.PselectedPower.isEmpty())
			return;
		if (world.isClientSide() && !MageQTEHelper.MAGE_SKILLS.contains(vars.PselectedPower))
			return;
		UseSkillOnKeyPressedProcedure.execute(world, x, y, z, entity);
	}

	private static void releaseHotbarSlot(Player entity, int pressedms) {
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (!vars.combatmode || vars.PselectedPower.isEmpty())
			return;
		if (entity.level().isClientSide() && !MageQTEHelper.MAGE_SKILLS.contains(vars.PselectedPower))
			return;
		UseSkillOnKeyReleasedProcedure.execute(entity.level(), entity, pressedms);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(UseSkillMessage.class, UseSkillMessage::buffer, UseSkillMessage::new, UseSkillMessage::handler);
	}
}
