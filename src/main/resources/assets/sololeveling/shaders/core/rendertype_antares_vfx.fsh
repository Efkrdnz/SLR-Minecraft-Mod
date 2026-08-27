#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
in vec2 lightMap;
in vec3 viewPosition;
in vec3 viewNormal;

out vec4 fragColor;

// No custom uniforms are declared on purpose. Iris only guarantees the
// vanilla uniform set for a core shader it does not replace, so everything here
// is derived from GameTime, ColorModulator and the two standard samplers.

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 31.19);
    return fract(p.x * p.y);
}

float noiseValue(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);
    return mix(mix(hash(cell), hash(cell + vec2(1.0, 0.0)), local.x),
               mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0)), local.x),
               local.y);
}

// Three octaves is the point where the flame stops reading as scrolling static.
float fbm3(vec2 p) {
    float value = noiseValue(p) * 0.55;
    value += noiseValue(p * 2.07 + vec2(11.3, 5.7)) * 0.30;
    value += noiseValue(p * 4.11 - vec2(3.1, 9.4)) * 0.15;
    return value;
}

vec3 safeNormalize(vec3 value) {
    return value * inversesqrt(max(dot(value, value), 1.0e-8));
}

// Shared heat ramp for every burning material: void red, through crimson and
// ember, into a bone-white core. Driving colour from one temperature curve is
// what makes the destruction flame read as fire rather than as a red tint laid
// over noise.
vec3 destructionRamp(float heat) {
    heat = clamp(heat, 0.0, 1.0);
    vec3 color = mix(vec3(0.055, 0.004, 0.012), vec3(0.52, 0.020, 0.034),
            smoothstep(0.0, 0.34, heat));
    color = mix(color, vec3(0.94, 0.135, 0.048), smoothstep(0.30, 0.66, heat));
    color = mix(color, vec3(1.0, 0.44, 0.155), smoothstep(0.60, 0.86, heat));
    color = mix(color, vec3(1.0, 0.86, 0.66), smoothstep(0.84, 1.0, heat));
    return color;
}

void main() {
    float material = floor(texCoord0.x);
    vec2 uv = vec2(fract(texCoord0.x), texCoord0.y);
    vec2 centered = uv * 2.0 - 1.0;
    float time = GameTime * 2400.0;
    float coarse = noiseValue(uv * vec2(8.0, 15.0)
            + vec2(time * 0.025, -time * 0.041));
    float fine = noiseValue(uv * vec2(27.0, 39.0)
            - vec2(time * 0.052, time * 0.019));
    float light = texture(Sampler2, clamp(lightMap, 0.0, 1.0)).r;
    float whiteSample = dot(texture(Sampler0, uv).rgb, vec3(0.333333));
    vec3 normal = safeNormalize(viewNormal);
    vec3 viewDirection = safeNormalize(-viewPosition);
    float fresnel = pow(1.0 - abs(dot(normal, viewDirection)), 2.2);
    float edgeDistance = min(min(uv.x, 1.0 - uv.x),
            min(uv.y, 1.0 - uv.y));
    float edge = 1.0 - smoothstep(0.015, 0.14, edgeDistance);
    vec3 base = max(vertexColor.rgb, vec3(0.003));
    vec3 color = base;
    float alpha = vertexColor.a;

    if (material < 1.0) {
        // Telegraph: restrained obsidian with a readable crimson perimeter and a
        // sweep travelling the marker, so it reads as a warning not decoration.
        float sweep = fract(uv.y - time * 0.11);
        float band = 1.0 - smoothstep(0.0, 0.22, sweep);
        color = base * (0.25 + light * 0.34 + coarse * 0.12);
        color += vec3(0.63, 0.018, 0.025) * (edge * 0.48 + fresnel * 0.18);
        color += destructionRamp(0.52) * band * 0.30;
        alpha *= 0.68 + edge * 0.24 + band * 0.10;
    } else if (material < 2.0) {
        // Void body: nearly black, with slow internal turbulence so the volume
        // never collapses into a flat silhouette.
        float churn = fbm3(uv * vec2(3.2, 3.8) + vec2(time * 0.012, time * 0.018));
        color = base * (0.13 + light * 0.22 + churn * 0.14);
        color += vec3(0.28, 0.005, 0.012) * (fresnel * 0.34 + edge * 0.12);
        color += destructionRamp(0.22) * smoothstep(0.62, 0.95, churn) * 0.20;
    } else if (material < 3.0) {
        // Destruction flame. Noise is advected along the card and cooled toward
        // the trailing edge, so the sheet burns instead of scrolling.
        vec2 flowUv = vec2(uv.x * 2.6, uv.y * 3.4 - time * 0.090);
        float body = fbm3(flowUv);
        float tongues = fbm3(flowUv * 2.25 + vec2(1.7, -time * 0.055));
        float heat = body * 0.66 + tongues * 0.34;
        heat *= 1.0 - smoothstep(0.32, 1.0, uv.y) * 0.55;
        heat = smoothstep(0.15, 0.85, heat);
        color = mix(base * (0.45 + heat * 0.75), destructionRamp(heat), 0.68);
        color += vec3(1.0, 0.42, 0.16) * fresnel * 0.14;
        alpha *= 0.28 + heat * 0.72;
    } else if (material < 4.0) {
        // White-hot core: a tight filament inside a wider thermal bloom.
        float filament = 1.0 - smoothstep(0.02, 0.30, abs(centered.x));
        float bloom = 1.0 - smoothstep(0.0, 0.85,
                length(centered * vec2(1.0, 0.55)));
        float pulse = 0.92 + 0.08 * sin(time * 0.61 + fine * 5.0);
        float heat = clamp(filament * 0.62 + bloom * 0.52 + fine * 0.10, 0.0, 1.0);
        color = mix(base * pulse, destructionRamp(0.52 + heat * 0.48), 0.72);
        color += vec3(1.0, 0.88, 0.70) * pow(filament, 3.0) * 0.52;
        alpha *= 0.55 + heat * 0.45;
    } else if (material < 5.0) {
        // Ground fractures: a warped fault with a secondary branch, breathing
        // heat from underneath rather than glowing at a constant brightness.
        float warp = (fbm3(uv * vec2(6.0, 2.2)) - 0.5) * 0.30;
        float fault = 1.0 - smoothstep(0.012, 0.17, abs(centered.x + warp));
        float branch = 1.0 - smoothstep(0.008, 0.11,
                abs(centered.x + warp * 2.2 + sin(uv.y * 9.0) * 0.17));
        float crack = max(fault, branch * 0.62);
        float breath = 0.62 + 0.38 * sin(time * 0.42 + uv.y * 6.0);
        color = mix(base * (0.20 + light * 0.22),
                destructionRamp(0.50 + crack * 0.50), crack * 0.90);
        color += vec3(1.0, 0.72, 0.40) * pow(crack, 3.0) * 0.42 * breath;
        alpha *= 0.22 + crack * 0.78;
    } else if (material < 6.0) {
        // Smoke cards: billowing density with embers still alive inside, kept
        // subtle so a shader pack's bloom cannot blow it out.
        float radial = 1.0 - smoothstep(0.05, 1.0, dot(centered, centered));
        float billow = fbm3(uv * vec2(3.4, 3.0)
                + vec2(time * 0.012, -time * 0.030));
        float density = smoothstep(0.26, 0.80, billow);
        color = mix(base * 0.14, base * 0.88, density);
        color += destructionRamp(0.44) * pow(density, 4.0) * 0.24;
        alpha *= radial * (0.16 + density * 0.46);
    } else if (material < 7.0) {
        // Dragon wing membrane: a vein network over a translucent sheet, with a
        // hot rim where light passes through the trailing edge.
        float veinA = 1.0 - smoothstep(0.018, 0.075,
                abs(fract(uv.y * 4.0 + uv.x * 1.3 + fine * 0.08) - 0.5));
        float veinB = 1.0 - smoothstep(0.010, 0.048,
                abs(fract(uv.y * 9.0 - uv.x * 2.1 + coarse * 0.12) - 0.5));
        float veins = max(veinA, veinB * 0.55);
        float translucency = pow(1.0 - abs(dot(normal, viewDirection)), 1.4);
        color = base * (0.24 + light * 0.24 + coarse * 0.12);
        color += vec3(0.62, 0.012, 0.025)
                * (veins * 0.34 + edge * 0.46 + fresnel * 0.20);
        color += destructionRamp(0.38) * translucency * 0.22;
        alpha *= 0.52 + veins * 0.20 + edge * 0.18;
    } else {
        // Sigils, eyes and sovereign marks: a slowly rotating rune ward.
        float angle = atan(centered.y, centered.x);
        float radius = length(centered);
        float diamond = 1.0 - smoothstep(0.52, 0.78,
                abs(centered.x) + abs(centered.y));
        float core = 1.0 - smoothstep(0.05, 0.30, radius);
        float ring = 1.0 - smoothstep(0.02, 0.07, abs(radius - 0.62));
        float runes = step(0.55, fract((angle / 6.2831853 + 0.5
                + time * 0.020) * 12.0));
        color = base + vec3(1.0, 0.21, 0.075)
                * (diamond * 0.28 + core * 0.66 + edge * 0.18);
        color += destructionRamp(0.72) * ring * runes * 0.50;
        alpha *= max(max(diamond, core), ring * runes * 0.85) * 0.82
                + edge * 0.18;
    }

    color *= 0.985 + whiteSample * 0.015;
    alpha *= ColorModulator.a;
    if (alpha < 0.008) {
        discard;
    }
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.96));
}
