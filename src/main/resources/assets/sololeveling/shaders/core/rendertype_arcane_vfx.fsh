#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 viewPosition;
in vec3 viewNormal;
in vec2 lightMap;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
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
    for (int i = 0; i < 4; i++) {
        value += noise(p) * weight;
        p = p * 2.03 + vec2(7.13, 3.91);
        weight *= 0.48;
    }
    return value;
}

float line(float value, float width) {
    return 1.0 - smoothstep(0.0, width, abs(value));
}

void main() {
    float kind = floor(texCoord0.x + 0.0001);
    float stage = clamp(floor(texCoord0.y + 0.0001), 1.0, 5.0);
    vec2 uv = vec2(fract(texCoord0.x), fract(texCoord0.y));
    vec2 p = uv * 2.0 - 1.0;
    float time = GameTime * 1600.0;
    float stageN = (stage - 1.0) * 0.25;
    float broad = fbm(uv * (3.2 + stage * 0.35) + vec2(time * 0.09, -time * 0.12));
    float detail = fbm(uv * (9.0 + stage * 1.4) + vec2(-time * 0.23, time * 0.17) + broad * 1.8);
    float fresnel = pow(clamp(1.0 - abs(dot(normalize(-viewPosition), normalize(viewNormal))), 0.0, 1.0), 1.6);
    float alpha = 0.0;
    float core = 0.0;

    if (kind < 1.0) {
        float bend = (detail - 0.5) * (0.055 - stageN * 0.025);
        float filament = abs(p.x - bend);
        float taper = smoothstep(0.0, 0.16, uv.y) * smoothstep(0.0, 0.22, 1.0 - uv.y);
        core = 1.0 - smoothstep(0.012, 0.095, filament);
        float sparks = pow(max(0.0, sin((p.x + detail * 0.08) * 31.0 - time * 1.9)), 14.0);
        alpha = taper * (core * 0.95 + (1.0 - smoothstep(0.06, 0.62, filament)) * (0.24 + broad * 0.36) + sparks * 0.22);
    } else if (kind < 2.0) {
        float radius = length(p);
        float angle = atan(p.y, p.x);
        float ring = line(radius - (0.64 + (broad - 0.5) * 0.035), 0.07);
        float runes = pow(max(0.0, cos(angle * (6.0 + stage * 2.0) + time * 0.32)), 19.0)
                * smoothstep(0.22, 0.96, radius);
        core = ring + runes;
        alpha = (1.0 - smoothstep(0.88, 1.04, radius)) * (ring * 0.72 + runes * 0.84 + detail * 0.12);
    } else if (kind < 3.0) {
        float radius = length(p);
        float swirl = atan(p.y, p.x) + radius * (7.0 + stage) - time * 1.4;
        float bands = pow(0.5 + 0.5 * sin(swirl + detail * 2.2), 5.0);
        float shell = line(radius - 0.66, 0.16);
        core = shell + bands * 0.52;
        alpha = (1.0 - smoothstep(0.78, 1.05, radius)) * (0.08 + shell * 0.52 + bands * 0.28 + fresnel * 0.30);
    } else if (kind < 4.0) {
        float gridX = line(fract((uv.x + detail * 0.018) * (5.0 + stage)) - 0.5, 0.10);
        float gridY = line(fract((uv.y - time * 0.08) * (5.0 + stage)) - 0.5, 0.10);
        float frame = max(line(abs(p.x) - 0.78, 0.13), line(abs(p.y) - 0.78, 0.13));
        core = max(frame, gridX * gridY);
        alpha = frame * 0.72 + gridX * gridY * 0.34 + fresnel * 0.20;
    } else if (kind < 5.0) {
        float center = 1.0 - smoothstep(0.018, 0.14, abs(p.x + (detail - 0.5) * 0.05));
        float edge = 1.0 - smoothstep(0.66, 1.02, length(p));
        float pulse = 0.55 + 0.45 * sin(uv.y * (24.0 + stage * 3.0) - time * 2.1);
        core = center;
        alpha = edge * (center * 0.78 + pulse * 0.12 + detail * 0.10);
    } else if (kind < 6.0) {
        float zig = sin((uv.y * (8.0 + stage) + floor(uv.y * 7.0) * 1.7) * 3.14159) * 0.045;
        float cut = 1.0 - smoothstep(0.012, 0.095, abs(p.x - zig));
        float edge = smoothstep(0.0, 0.12, uv.y) * smoothstep(0.0, 0.18, 1.0 - uv.y);
        float fracture = pow(clamp(1.0 - abs(detail - 0.5) * 17.0, 0.0, 1.0), 2.2);
        core = cut + fracture * 0.36;
        alpha = edge * (cut * 0.92 + fracture * 0.28 + fresnel * 0.12);
    } else if (kind < 7.0) {
        float radius = length(p);
        float angle = atan(p.y, p.x);
        float rings = line(fract(radius * (4.0 + stage) - time * 0.24) - 0.5, 0.14);
        float spokes = pow(max(0.0, cos(angle * (7.0 + stage * 2.0) - time * 0.18)), 17.0)
                * smoothstep(0.15, 0.96, radius);
        core = rings * 0.56 + spokes;
        alpha = (1.0 - smoothstep(0.90, 1.05, radius)) * (rings * 0.38 + spokes * 0.78 + detail * 0.10);
    } else {
        float radius = length(p);
        float burst = pow(max(0.0, 1.0 - radius), 1.45);
        float shards = pow(max(0.0, sin(atan(p.y, p.x) * (9.0 + stage * 2.0) + time * 0.9)), 15.0) * burst;
        float coreDisk = 1.0 - smoothstep(0.0, 0.34 + stageN * 0.10, radius);
        core = coreDisk + shards;
        alpha = coreDisk * 0.62 + shards * 0.82 + broad * burst * 0.18;
    }

    bool orb = vertexColor.b > vertexColor.g * 1.20 || vertexColor.r > vertexColor.g * 1.35;
    alpha *= vertexColor.a * (0.94 + stageN * 0.10);
    if (alpha < 0.008) discard;

    vec3 base = vertexColor.rgb;
    vec3 inner = orb ? vec3(0.88, 0.94, 1.0) : vec3(0.94, 0.98, 1.0);
    vec3 low = orb ? mix(vec3(0.015, 0.035, 0.34), vec3(0.38, 0.012, 0.10), detail * detail)
                   : mix(vec3(0.105, 0.018, 0.28), vec3(0.015, 0.23, 0.31), broad);
    vec3 color = mix(low, base, 0.46 + broad * 0.38);
    color = mix(color, inner, clamp(core * 0.73 + fresnel * 0.24, 0.0, 1.0));
    color *= 1.05 + core * (0.62 + stageN * 0.22) + fresnel * 0.18;
    float fallbackEnergy = dot(texture(Sampler0, uv).rgb, vec3(0.333333));
    float lightEnergy = texture(Sampler2, lightMap).r;
    color *= 0.97 + fallbackEnergy * 0.03;
    color *= 0.96 + lightEnergy * 0.04;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.92) * ColorModulator.a);
}
