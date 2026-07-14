#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Phase;
uniform float Intensity;
uniform vec3 AccentA;
uniform vec3 AccentB;
uniform float SequenceTime;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec3 center = texture(DiffuseSampler, texCoord).rgb;
    vec3 north = texture(DiffuseSampler, texCoord + vec2(0.0, oneTexel.y)).rgb;
    vec3 south = texture(DiffuseSampler, texCoord - vec2(0.0, oneTexel.y)).rgb;
    vec3 east = texture(DiffuseSampler, texCoord + vec2(oneTexel.x, 0.0)).rgb;
    vec3 west = texture(DiffuseSampler, texCoord - vec2(oneTexel.x, 0.0)).rgb;
    vec3 neighborAverage = (north + south + east + west) * 0.25;
    vec3 detail = center - neighborAverage;
    float edge = clamp(length(detail) * 3.4, 0.0, 1.0);
    float overdrive = max(0.0, Intensity - 1.0);
    float sharpenAmount = Phase < 1.5 ? 1.82 : (Phase < 2.5 ? 1.56 : 1.72);
    vec3 sharp = clamp(center + detail * sharpenAmount, 0.0, 1.0);
    float gray = smoothstep(0.075, 0.925, luminance(sharp));
    float ink = step(0.515, gray);
    float inkMix = 0.17 + min(Intensity, 1.25) * 0.055;

    vec2 centered = texCoord - vec2(0.5);
    centered.x *= InSize.x / max(InSize.y, 1.0);
    float radius = length(centered);
    float angle = atan(centered.y, centered.x);
    float vignette = 1.0 - smoothstep(0.28, 1.02, radius);

    vec3 monochrome = vec3(mix(gray, ink, inkMix));
    monochrome = clamp(monochrome + vec3(edge * (0.55 + overdrive * 0.32)), 0.0, 1.0);
    monochrome *= 0.73 + vignette * 0.31;

    float time = clamp(SequenceTime, 0.0, 1.0);
    float load = smoothstep(0.25, 0.98, time);
    float finalLoad = smoothstep(0.77, 0.985, time);
    vec3 accent = mix(AccentA, AccentB, smoothstep(0.42, 0.96, time));

    // Broken concentric rings collapse toward the crosshair as the delayed hit approaches.
    float ringRadius = mix(0.82, 0.055, pow(load, 1.08));
    float ringWidth = mix(0.022, 0.006, load);
    float ring = 1.0 - smoothstep(ringWidth, ringWidth * 2.6, abs(radius - ringRadius));
    float ringFragments = 0.34 + 0.66 * smoothstep(0.16, 0.94,
            abs(sin(angle * 7.0 + sin(angle * 3.0) * 0.72)));
    ring *= ringFragments;
    float echoRadius = ringRadius + mix(0.105, 0.028, load);
    float echoRing = (1.0 - smoothstep(ringWidth * 0.85, ringWidth * 2.2,
            abs(radius - echoRadius))) * (0.35 + ringFragments * 0.4);

    // Thin, stationary rays gain length toward the center instead of spinning around it.
    float rayPattern = pow(abs(sin(angle * 23.0 + sin(angle * 5.0) * 1.15)), 24.0);
    float rayField = smoothstep(mix(0.33, 0.075, load), mix(0.45, 0.14, load), radius)
            * (1.0 - smoothstep(0.72, 1.04, radius));
    float rays = rayPattern * rayField * smoothstep(0.3, 0.78, load);

    // Draw a single tapered sword arc across the frame. The moving head makes the cut read as a
    // continuous strike instead of a line that simply fades into existence everywhere at once.
    float slashProgress = smoothstep(0.785, 0.965, time);
    float slashAlong = centered.x / 0.86;
    float slashAlong01 = slashAlong * 0.5 + 0.5;
    float slashCurve = centered.y + centered.x * 0.105
            + 0.045 * (centered.x * centered.x - 0.28);
    float slashBody = clamp(1.0 - abs(slashAlong), 0.0, 1.0);
    float slashTaper = pow(slashBody, 0.42);
    float slashExtent = 1.0 - smoothstep(0.94, 1.0, abs(slashAlong));

    float sweepHead = mix(-0.1, 1.1, slashProgress);
    float slashReveal = 1.0 - smoothstep(sweepHead, sweepHead + 0.035, slashAlong01);
    float pixelWidth = max(oneTexel.y, 0.00048);
    float coreWidth = mix(pixelWidth * 0.28, pixelWidth * 1.5, slashTaper);
    float slashCore = (1.0 - smoothstep(coreWidth, coreWidth + pixelWidth * 0.82,
            abs(slashCurve))) * slashExtent * slashReveal;
    float haloWidth = mix(pixelWidth * 1.7, 0.018, slashTaper);
    float slashHalo = (1.0 - smoothstep(coreWidth * 1.4, haloWidth,
            abs(slashCurve))) * slashExtent * slashReveal;

    float tipPosition = abs(slashAlong01 - sweepHead);
    float tipAlong = (1.0 - smoothstep(0.0, 0.052, tipPosition))
            * step(0.0, sweepHead) * step(sweepHead, 1.0);
    float slashTip = tipAlong * (1.0 - smoothstep(pixelWidth, 0.03,
            abs(slashCurve))) * slashExtent;
    float centerCore = (1.0 - smoothstep(0.0, mix(0.12, 0.035, finalLoad), radius)) * finalLoad;

    float coloredEdges = edge * smoothstep(0.19, 0.72, load) * (0.1 + finalLoad * 0.18);
    float energyMask = ring * (0.42 + load * 0.52) + echoRing * 0.42 + rays * (0.3 + finalLoad * 0.72)
            + slashHalo * 0.48 + slashTip * 0.62 + coloredEdges;
    vec3 energy = accent * energyMask;
    energy += AccentB * (slashCore * (0.78 + finalLoad * 0.82) + centerCore * 0.8);

    vec3 result;

    if (Phase < 0.5) {
        result = monochrome + energy * 0.16;
    } else if (Phase < 1.5) {
        vec3 negative = 1.0 - monochrome;
        negative = clamp((negative - 0.5) * (1.3 + overdrive * 0.16) + 0.5, 0.0, 1.0);
        negative += vec3(edge * (0.46 + overdrive * 0.82));
        result = negative + energy * (0.34 + overdrive * 0.18);
    } else if (Phase < 2.5) {
        result = monochrome * (0.94 - load * 0.08) + energy * 0.72;
    } else {
        float finalContrast = 1.08 + finalLoad * 0.24;
        vec3 chargedMono = clamp((monochrome - 0.5) * finalContrast + 0.5, 0.0, 1.0);
        chargedMono *= 1.0 - finalLoad * 0.14;
        result = chargedMono + energy * (0.9 + finalLoad * 0.56);
        result += vec3(slashCore * finalLoad * 0.78 + slashTip * finalLoad * 0.34
                + centerCore * finalLoad * 0.52);
    }

    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
