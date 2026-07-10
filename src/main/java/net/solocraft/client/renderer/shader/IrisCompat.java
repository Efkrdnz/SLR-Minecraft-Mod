package net.solocraft.client.renderer.shader;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Soft compatibility bridge to Iris / Oculus.
 *
 * <p>Iris (and its Forge port Oculus) replace the entire rendering pipeline with
 * a shaderpack's gbuffer programs when a pack is enabled, and cannot run a mod's
 * custom core shaders. Any {@code RenderType} built on a custom
 * {@code ShaderStateShard} is therefore dropped/misrendered while a pack is
 * active. Our slash render types query {@link #isShaderPackInUse()} and fall
 * back to a vanilla render type in that case, so the effects stay visible.
 *
 * <p>Oculus is an optional runtime mod and is not on the compile classpath, so
 * the Iris API is accessed purely by reflection. If Oculus is absent (or the API
 * ever changes), this reports {@code false} and the custom shaders are used
 * exactly as before — it can never crash or hard-depend on Iris.
 */
public final class IrisCompat {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static volatile boolean initialized = false;
	private static boolean available = false;
	private static Object irisApi;
	private static Method isShaderPackInUseMethod;

	private IrisCompat() {
	}

	/** @return true only if Oculus/Iris is installed AND a shaderpack is currently active. */
	public static boolean isShaderPackInUse() {
		if (!initialized)
			init();
		if (!available)
			return false;
		try {
			return isShaderPackInUseMethod.invoke(irisApi) instanceof Boolean b && b;
		} catch (Throwable t) {
			return false;
		}
	}

	private static synchronized void init() {
		if (initialized)
			return;
		initialized = true;
		// current Iris/Oculus uses net.irisshaders.*; older Oculus shaded net.coderbot.*
		for (String className : new String[] { "net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.api.v0.IrisApi" }) {
			try {
				Class<?> api = Class.forName(className);
				irisApi = api.getMethod("getInstance").invoke(null);
				isShaderPackInUseMethod = api.getMethod("isShaderPackInUse");
				available = true;
				LOGGER.info("[SoloLeveling] Iris/Oculus detected ({}). Slash effects will fall back to a vanilla render type while a shaderpack is active.", className);
				return;
			} catch (Throwable ignored) {
				// try next candidate
			}
		}
		available = false;
		LOGGER.info("[SoloLeveling] Iris/Oculus not detected; using custom slash shaders normally.");
	}
}
