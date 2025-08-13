package net.solocraft.procedures;

import org.checkerframework.checker.units.qual.s;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.gui.components.EditBox;

import java.util.HashMap;

public class InvestNumberProcedure {
	public static void execute(Entity entity, HashMap guistate) {
		if (entity == null || guistate == null)
			return;
		if (new Object() {
			double convert(String s) {
				try {
					return Double.parseDouble(s.trim());
				} catch (Exception e) {
				}
				return 0;
			}
		}.convert(guistate.containsKey("text:investvalue") ? ((EditBox) guistate.get("text:investvalue")).getValue() : "") > 0) {
			{
				double _setval = new Object() {
					double convert(String s) {
						try {
							return Double.parseDouble(s.trim());
						} catch (Exception e) {
						}
						return 0;
					}
				}.convert(guistate.containsKey("text:investvalue") ? ((EditBox) guistate.get("text:investvalue")).getValue() : "");
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.investvalue = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
	}
}
