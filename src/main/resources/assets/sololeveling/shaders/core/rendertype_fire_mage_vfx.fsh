#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 localPos;
in vec2 lightMap;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 34.37);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float weight = 0.56;
    for (int i = 0; i < 5; i++) {
        value += noise(p) * weight;
        p = p * 2.03 + vec2(7.13, 3.71);
        weight *= 0.46;
    }
    return value;
}

float sharpRing(float value, float center, float width) {
    return 1.0 - smoothstep(width * 0.35, width, abs(value - center));
}

void main() {
    float kind = floor(texCoord0.x + 0.0001);
    float stage = clamp(floor(texCoord0.y + 0.0001), 1.0, 5.0);
    vec2 uv = vec2(fract(texCoord0.x), fract(texCoord0.y));
    vec2 p = uv * 2.0 - 1.0;
    float time = GameTime * 1800.0;
    float stageN = (stage - 1.0) * 0.25;
    float broad = fbm(vec2(uv.x * (3.8 + stageN), uv.y * 4.4 - time * 0.34));
    float detail = fbm(vec2(uv.x * (10.0 + stage), uv.y * (9.0 + stage) - time * 0.72 + broad * 2.8));
    float micro = noise(vec2(uv.x * 31.0 - time * 0.61, uv.y * 27.0 + time * 0.19));
    float alpha = 0.0;
    float core = 0.0;

    if (kind < 1.0) {
        float centerLine = (detail - 0.5) * (0.16 - stageN * 0.055);
        float distanceToCore = abs(p.x - centerLine);
        float taper = smoothstep(0.0, 0.09, uv.y) * smoothstep(0.0, 0.2, 1.0 - uv.y);
        float body = 1.0 - smoothstep(0.13 + stageN * 0.06, 0.54 + stageN * 0.10, distanceToCore);
        float filaments = pow(max(0.0, sin((p.x + detail * 0.20) * 23.0 + uv.y * 7.0 - time)), 10.0);
        core = 1.0 - smoothstep(0.018, 0.105 + stageN * 0.025, distanceToCore);
        alpha = taper * (body * (0.32 + broad * 0.62) + filaments * 0.36 + core * 0.78);
    } else if (kind < 2.0) {
        float radius = length(p);
        float warpedRadius = radius + (broad - 0.5) * (0.11 + stageN * 0.05);
        float sphere = 1.0 - smoothstep(0.47, 0.96, warpedRadius);
        float corona = sharpRing(warpedRadius, 0.72, 0.18 + stageN * 0.04);
        float rays = pow(max(0.0, sin(atan(p.y, p.x) * (9.0 + stage * 2.0) - time * 0.78 + detail * 2.0)), 11.0);
        core = 1.0 - smoothstep(0.05, 0.43, radius);
        alpha = sphere * (0.45 + detail * 0.63) + corona * 0.58 + rays * (1.0 - smoothstep(0.28, 1.0, radius)) * 0.42;
    } else if (kind < 3.0) {
        float radius = length(p);
        float angle = atan(p.y, p.x);
        float ring = sharpRing(radius + (detail - 0.5) * 0.045, 0.67, 0.12 - stageN * 0.018);
        float glyph = pow(max(0.0, cos(angle * (6.0 + stage) + time * 0.17)), 18.0);
        float radial = smoothstep(0.22, 0.84, radius) * (1.0 - smoothstep(0.84, 1.02, radius));
        core = ring;
        alpha = ring * (0.62 + broad * 0.48) + glyph * radial * (0.25 + stageN * 0.26);
    } else if (kind < 4.0) {
        float side = 1.0 - smoothstep(0.24 + stageN * 0.06, 0.56, abs(p.x) + (detail - 0.5) * 0.16);
        float rise = smoothstep(-1.0, -0.58, p.y) * smoothstep(1.0, 0.42, p.y);
        float tongues = pow(max(0.0, sin(p.x * (13.0 + stage * 2.0) + detail * 4.0 + time * 0.62)), 8.0);
        core = side * smoothstep(0.48, -0.16, p.y);
        alpha = side * rise * (0.24 + broad * 0.72) + tongues * rise * (0.18 + stageN * 0.20);
    } else if (kind < 5.0) {
        vec2 stretched = vec2(p.x * 1.22, p.y * 0.70);
        float radius = length(stretched);
        float head = 1.0 - smoothstep(0.18, 0.73, radius + (detail - 0.5) * 0.10);
        float wake = (1.0 - smoothstep(0.05, 0.53, abs(p.x + (detail - 0.5) * 0.16))) * smoothstep(0.1, 0.95, uv.y);
        core = 1.0 - smoothstep(0.04, 0.28, radius);
        alpha = head * (0.42 + broad * 0.64) + wake * (0.25 + detail * 0.42);
    } else {
        float radius = length(p);
        float angle = atan(p.y, p.x);
        float shell = sharpRing(radius + (broad - 0.5) * 0.08, 0.48 + stageN * 0.08, 0.16);
        float rays = pow(max(0.0, sin(angle * (11.0 + stage * 2.0) - time * 0.9 + detail * 1.8)), 13.0);
        float interior = 1.0 - smoothstep(0.05, 0.72, radius);
        core = 1.0 - smoothstep(0.02, 0.25, radius);
        alpha = shell * 0.84 + rays * interior * (0.42 + stageN * 0.38) + interior * detail * 0.36;
    }

    alpha *= vertexColor.a * (0.91 + stageN * 0.16);
    alpha *= 0.92 + micro * 0.08;
    if (alpha < 0.012) discard;

    vec3 base = vertexColor.rgb;
	bool blueFire = base.b > base.r * 1.08;
	vec3 lowTint = blueFire ? vec3(0.10, 0.34, 0.92) : vec3(0.64, 0.26, 0.08);
	vec3 hotStart = blueFire ? vec3(0.02, 0.52, 1.0) : vec3(1.0, 0.36, 0.035);
	vec3 hotEnd = blueFire ? vec3(0.72, 0.96, 1.0) : vec3(1.0, 0.93, 0.58);
    vec3 ember = mix(base * lowTint, base, broad * 0.72 + 0.18);
    vec3 hot = mix(hotStart, hotEnd, core);
    vec3 color = mix(ember, hot, clamp(core * 0.82 + detail * 0.24, 0.0, 1.0));
    color *= 1.08 + core * (0.82 + stageN * 0.28);
    float fallbackEnergy = dot(texture(Sampler0, uv).rgb, vec3(0.333333));
    float lightEnergy = texture(Sampler2, lightMap).r;
    color *= 0.97 + fallbackEnergy * 0.03;
    color *= 0.95 + lightEnergy * 0.05;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.94) * ColorModulator.a);
}
