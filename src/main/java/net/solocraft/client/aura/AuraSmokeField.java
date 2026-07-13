package net.solocraft.client.aura;

import net.solocraft.SololevelingMod;
import net.solocraft.client.aura.ClientPlayerAuraManager.AuraInstance;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side, world-space smoke and ember simulation for player auras.
 *
 * <p>Each tick the active auras <em>emit</em> puffs at random points around the
 * wearer. Puffs inherit part of the player's velocity, then rise with buoyancy
 * and wander through a cheap turbulence field before fading out. Because they
 * live in world space rather than the player's render pose, fast movement leaves
 * a streaming trail behind and standing still makes the aura billow in place —
 * this is what stops the effect from reading as a solid decal glued to the body.</p>
 */
@Mod.EventBusSubscriber(
		modid = SololevelingMod.MODID,
		bus = Mod.EventBusSubscriber.Bus.FORGE,
		value = Dist.CLIENT
)
public final class AuraSmokeField {
	private static final Map<Integer, List<Puff>> PUFFS = new ConcurrentHashMap<>();
	private static final Map<Integer, Vec3> LEAN = new ConcurrentHashMap<>();
	private static final Random RNG = new Random();

	private static final int MAX_PER_ENTITY = 96;
	private static final double EMIT_RANGE_SQR = 60.0D * 60.0D;

	private AuraSmokeField() {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.isPaused()) {
			return;
		}

		tick(minecraft);
	}

	private static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null) {
			PUFFS.clear();
			LEAN.clear();
			return;
		}

		long time = level.getGameTime();
		Vec3 reference = minecraft.player != null ? minecraft.player.position() : Vec3.ZERO;

		for (Player player : level.players()) {
			int id = player.getId();

			if (player.isSpectator()) {
				PUFFS.remove(id);
				LEAN.remove(id);
				continue;
			}

			List<AuraInstance> auras = PlayerAuraRenderer.activeAuras(player);
			if (auras.isEmpty()) {
				PUFFS.remove(id);
				LEAN.remove(id);
				continue;
			}

			updateLean(player);

			if (player.distanceToSqr(reference.x, reference.y, reference.z) <= EMIT_RANGE_SQR) {
				List<Puff> list = PUFFS.computeIfAbsent(id, ignored -> new ArrayList<>());
				for (AuraInstance instance : auras) {
					emit(list, player, instance, time);
				}
				trim(list);
			}
		}

		Iterator<Map.Entry<Integer, List<Puff>>> entries = PUFFS.entrySet().iterator();
		while (entries.hasNext()) {
			List<Puff> list = entries.next().getValue();
			Iterator<Puff> puffs = list.iterator();
			while (puffs.hasNext()) {
				Puff puff = puffs.next();
				integrate(puff, time);
				if (puff.age >= puff.maxAge) {
					puffs.remove();
				}
			}
			if (list.isEmpty()) {
				entries.remove();
			}
		}
	}

	private static void trim(List<Puff> list) {
		while (list.size() > MAX_PER_ENTITY) {
			list.remove(0);
		}
	}

	private static void updateLean(Player player) {
		Vec3 velocity = player.getDeltaMovement();
		double targetX = Mth.clamp(-velocity.x * 2.4D, -0.5D, 0.5D);
		double targetZ = Mth.clamp(-velocity.z * 2.4D, -0.5D, 0.5D);

		Vec3 previous = LEAN.getOrDefault(player.getId(), Vec3.ZERO);
		double x = previous.x + (targetX - previous.x) * 0.22D;
		double z = previous.z + (targetZ - previous.z) * 0.22D;

		LEAN.put(player.getId(), new Vec3(x, 0.0D, z));
	}

	private static void emit(List<Puff> list, Player player, AuraInstance instance, long time) {
		PlayerAuraDefinition definition = PlayerAuraRegistry.get(instance.auraId());
		if (definition == null) {
			return;
		}

		float intensity = instance.intensity() * instance.envelope(0.0F, time);
		if (intensity <= 0.02F) {
			return;
		}

		boolean shadow = definition.fluid() != null
				&& definition.fluid().style() == PlayerAuraDefinition.FluidStyle.SHADOW_RIFT;
		boolean whiteHair = definition.fluid() != null
				&& definition.fluid().style() == PlayerAuraDefinition.FluidStyle.WHITE_FLAME_HAIR;
		float radius = Math.max(player.getBbWidth() * 0.5F + 0.2F, definition.radius());
		float height = Math.max(1.4F, player.getBbHeight() * definition.heightScale());
		float density = definition.fluid() != null ? definition.fluid().opacity() : 0.7F;

		float rate = (whiteHair ? 4.6F : shadow ? 2.6F : 2.0F) + intensity * (whiteHair ? 3.0F : 2.4F);
		int count = Mth.floor(rate);
		if (RNG.nextFloat() < rate - count) {
			count++;
		}

		Vec3 motion = player.getDeltaMovement();

		for (int i = 0; i < count; i++) {
			double angle = RNG.nextDouble() * Math.PI * 2.0D;
			double distance = Math.sqrt(RNG.nextDouble()) * radius * (whiteHair ? 0.42D : 0.72D);
			double x = player.getX() + Math.cos(angle) * distance;
			double z = player.getZ() + Math.sin(angle) * distance;
			double y = whiteHair
					? player.getY() + player.getBbHeight() * (0.78D + RNG.nextDouble() * 0.42D)
					: player.getY() + 0.08D + RNG.nextDouble() * height * 0.9D;

			boolean bright = RNG.nextFloat() < (shadow ? 0.20F : 0.30F);
			double inherit = bright ? 0.42D : 0.6D;

			Puff puff = new Puff();
			puff.px = puff.ppx = x;
			puff.py = puff.ppy = y;
			puff.pz = puff.ppz = z;
			puff.vx = motion.x * inherit + (RNG.nextDouble() - 0.5D) * 0.02D;
			puff.vz = motion.z * inherit + (RNG.nextDouble() - 0.5D) * 0.02D;
			puff.vy = (bright ? 0.03D : 0.018D) + RNG.nextDouble() * 0.02D + (whiteHair ? 0.018D : 0.0D);
			puff.age = 0.0D;
			puff.maxAge = (bright ? 15 + RNG.nextInt(12) : 32 + RNG.nextInt(24)) * (shadow ? 1.25D : 1.0D);
			puff.size = bright
					? radius * (whiteHair ? 0.14F + RNG.nextFloat() * 0.10F : 0.10F + RNG.nextFloat() * 0.07F)
					: radius * (whiteHair ? 0.22F + RNG.nextFloat() * 0.28F : 0.30F + RNG.nextFloat() * 0.34F);
			if (definition.smokeColor() >= 0) {
				// Explicit smoke palette: gold-ish body, white-hot embers.
				puff.color = bright
						? mixColor(definition.smokeColor(), 0xFFFFFF, 0.45F + RNG.nextFloat() * 0.4F)
						: mixColor(definition.smokeColor(), 0xFFFFFF, 0.05F + RNG.nextFloat() * 0.4F);
			} else {
				puff.color = bright
						? mixColor(definition.primaryColor(), 0xFFFFFF, 0.30F + RNG.nextFloat() * 0.25F)
						: mixColor(definition.secondaryColor(), definition.primaryColor(),
								0.28F + RNG.nextFloat() * 0.5F);
			}
			puff.bright = bright;
			puff.hair = whiteHair;
			puff.baseAlpha = Mth.clamp(intensity * density * (bright ? 1.05F : 0.72F), 0.0F, 1.0F);
			puff.seed = RNG.nextInt(16);
			puff.texture = definition.fallbackTexture();
			list.add(puff);
		}
	}

	private static void integrate(Puff puff, long time) {
		puff.ppx = puff.px;
		puff.ppy = puff.py;
		puff.ppz = puff.pz;

		double t = time * 0.12D + puff.seed * 1.3D;
		double acceleration = puff.bright ? 0.0007D : 0.0013D;
		double buoyancy = (puff.bright ? 0.011D : 0.006D) + (puff.hair ? 0.006D : 0.0D);

		puff.vx += Math.sin(puff.py * 2.1D + t * 1.6D + puff.seed) * acceleration
				+ Math.sin(puff.pz * 1.7D - t) * acceleration * 0.5D;
		puff.vz += Math.cos(puff.py * 2.3D - t * 1.3D + puff.seed) * acceleration
				+ Math.cos(puff.px * 1.9D + t * 0.8D) * acceleration * 0.5D;
		puff.vy += buoyancy;

		double drag = puff.bright ? 0.90D : 0.93D;
		puff.vx *= drag;
		puff.vy *= drag;
		puff.vz *= drag;

		puff.px += puff.vx;
		puff.py += puff.vy;
		puff.pz += puff.vz;
		puff.age += 1.0D;
	}

	private static int mixColor(int a, int b, float t) {
		t = Mth.clamp(t, 0.0F, 1.0F);
		int ar = (a >> 16) & 0xFF;
		int ag = (a >> 8) & 0xFF;
		int ab = a & 0xFF;
		int br = (b >> 16) & 0xFF;
		int bg = (b >> 8) & 0xFF;
		int bb = b & 0xFF;
		int r = Math.round(ar + (br - ar) * t);
		int g = Math.round(ag + (bg - ag) * t);
		int bl = Math.round(ab + (bb - ab) * t);
		return (r << 16) | (g << 8) | bl;
	}

	/** Movement-lag offset applied to the dense aura body so it drags behind the wearer. */
	public static Vec3 lean(int entityId) {
		return LEAN.getOrDefault(entityId, Vec3.ZERO);
	}

	static Map<Integer, List<Puff>> puffs() {
		return PUFFS;
	}

	/** One simulated smoke/ember particle in world space. */
	static final class Puff {
		double px;
		double py;
		double pz;
		double ppx;
		double ppy;
		double ppz;
		double vx;
		double vy;
		double vz;
		double age;
		double maxAge;
		float size;
		float baseAlpha;
		int color;
		boolean bright;
		boolean hair;
		int seed;
		ResourceLocation texture;
	}
}
