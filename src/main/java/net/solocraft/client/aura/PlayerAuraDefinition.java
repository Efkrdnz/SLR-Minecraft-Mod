package net.solocraft.client.aura;

import net.minecraft.resources.ResourceLocation;

/** Immutable visual recipe used by the player aura renderer. */
public record PlayerAuraDefinition(String id, int primaryColor, int secondaryColor,
		ResourceLocation fallbackTexture, Facing facing, float radius, float heightScale,
		float speed, int shellLayers, int wispCount, int spikeCount, FluidProfile fluid,
		boolean groundRing, int smokeColor) {
	/** Sentinel meaning "derive the smoke palette from the primary/secondary colors". */
	public static final int SMOKE_FROM_COLORS = -1;

	/** Convenience overload that derives the aura's smoke color from its main colors. */
	public PlayerAuraDefinition(String id, int primaryColor, int secondaryColor,
			ResourceLocation fallbackTexture, Facing facing, float radius, float heightScale,
			float speed, int shellLayers, int wispCount, int spikeCount, FluidProfile fluid,
			boolean groundRing) {
		this(id, primaryColor, secondaryColor, fallbackTexture, facing, radius, heightScale,
				speed, shellLayers, wispCount, spikeCount, fluid, groundRing, SMOKE_FROM_COLORS);
	}
	/** Parameters for a soft, non-geometric aura made from overlapping fluid volumes. */
	public record FluidProfile(int lobeCount, int veilCount, int backflowCount,
			float radiusScale, float opacity, float turbulence, float speed, FluidStyle style) {
		public FluidProfile(int lobeCount, int veilCount, int backflowCount,
				float radiusScale, float opacity, float turbulence, float speed) {
			this(lobeCount, veilCount, backflowCount, radiusScale, opacity, turbulence, speed,
					FluidStyle.LIQUID_FLAME);
		}

		public FluidProfile {
			lobeCount = Math.max(0, Math.min(32, lobeCount));
			veilCount = Math.max(0, Math.min(16, veilCount));
			backflowCount = Math.max(0, Math.min(12, backflowCount));
			radiusScale = Math.max(0.45F, Math.min(2.0F, radiusScale));
			opacity = Math.max(0.05F, Math.min(1.0F, opacity));
			turbulence = Math.max(0.0F, Math.min(2.0F, turbulence));
			speed = Math.max(0.0F, Math.min(3.0F, speed));
			if (style == null)
				style = FluidStyle.LIQUID_FLAME;
		}
	}

	public enum FluidStyle {
		LIQUID_FLAME,
		SHADOW_RIFT,
		WHITE_FLAME_HAIR
	}

	public enum Facing {
		/** Wisps follow camera yaw and pitch, like normal particles. */
		CAMERA,
		/** Wisps follow camera yaw but remain upright in world space. */
		HORIZONTAL_CAMERA,
		/** Two intersecting world-space planes; useful for dense or distant auras. */
		CROSSED
	}

	public PlayerAuraDefinition {
		if (id == null || id.isBlank())
			throw new IllegalArgumentException("Aura id cannot be blank");
		if (fallbackTexture == null || facing == null)
			throw new IllegalArgumentException("Aura texture and facing are required");
		radius = Math.max(0.1F, radius);
		heightScale = Math.max(0.1F, heightScale);
		speed = Math.max(0.0F, speed);
		shellLayers = Math.max(0, Math.min(4, shellLayers));
		wispCount = Math.max(0, Math.min(24, wispCount));
		spikeCount = Math.max(0, Math.min(24, spikeCount));
	}
}
