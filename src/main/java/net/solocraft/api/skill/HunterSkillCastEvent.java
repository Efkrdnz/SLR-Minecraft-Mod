package net.solocraft.api.skill;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.Event;

/**
 * Fired when a hunter casts the skill they have selected.
 *
 * <p>Posted after the mod's own dispatch has run, so a contributed skill has
 * somewhere to be handled. Built-in skills post it too and nothing subscribes to
 * them; a subscriber is expected to check {@link #getSkill()} against its own
 * names and ignore everything else.
 *
 * <p>This carries no permission of its own. Whether the hunter has learned the
 * skill, can afford it, or is off cooldown is the subscriber's business, exactly
 * as it is for the mod's own skills.
 */
public class HunterSkillCastEvent extends Event {
	private final LevelAccessor level;
	private final double x;
	private final double y;
	private final double z;
	private final Entity caster;
	private final String skill;

	public HunterSkillCastEvent(LevelAccessor level, double x, double y, double z,
			Entity caster, String skill) {
		this.level = level;
		this.x = x;
		this.y = y;
		this.z = z;
		this.caster = caster;
		this.skill = skill == null ? "" : skill;
	}

	public LevelAccessor getLevel() {
		return level;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	public Entity getCaster() {
		return caster;
	}

	/** The selected skill's name, as it appears in the hunter's learned list. */
	public String getSkill() {
		return skill;
	}
}
