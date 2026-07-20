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
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);
    return mix(mix(hash(cell), hash(cell + vec2(1.0, 0.0)), local.x),
               mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0)), local.x), local.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float weight = 0.56;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * weight;
        p = p * 2.07 + vec2(5.17, 9.23);
        weight *= 0.47;
    }
    return value;
}

float lineMask(float value, float width) {
    return 1.0 - smoothstep(0.0, width, abs(value));
}

float hexLines(vec2 p) {
    p.x *= 1.1547;
    vec2 cell = fract(p) - 0.5;
    vec2 cell2 = fract(p + vec2(0.5, 0.5)) - 0.5;
    float first = max(abs(cell.x) * 0.866 + abs(cell.y) * 0.5, abs(cell.y));
    float second = max(abs(cell2.x) * 0.866 + abs(cell2.y) * 0.5, abs(cell2.y));
    float edge = min(abs(first - 0.46), abs(second - 0.46));
    return 1.0 - smoothstep(0.015, 0.052, edge);
}

void main() {
    float kind = floor(texCoord0.x + 0.0001);
    float stage = clamp(floor(texCoord0.y + 0.0001), 1.0, 5.0);
    vec2 uv = vec2(fract(texCoord0.x), fract(texCoord0.y));
    vec2 p = uv * 2.0 - 1.0;
    float time = GameTime * 1600.0;
    float stageN = (stage - 1.0) * 0.25;
    float broad = fbm(uv * (3.2 + stage * 0.28) + vec2(time * 0.08, -time * 0.13));
    float detail = fbm(uv * (10.0 + stage) + vec2(-time * 0.22, time * 0.31) + broad * 1.7);
    float scan = 0.5 + 0.5 * sin((uv.y * (34.0 + stage * 5.0) - time * 2.4) + detail * 2.0);
    float crack = pow(clamp(1.0 - abs(detail - 0.49) * (19.0 - stageN * 3.0), 0.0, 1.0), 2.4);
    float fresnel = pow(clamp(1.0 - abs(dot(normalize(-viewPosition), normalize(viewNormal))), 0.0, 1.0), 1.7);
    float alpha = 0.0;
    float core = 0.0;

    if (kind < 1.0) {
        float center = (detail - 0.5) * (0.12 - stageN * 0.035);
        float distanceToCore = abs(p.x - center);
        float taper = smoothstep(0.0, 0.12, uv.y) * smoothstep(0.0, 0.18, 1.0 - uv.y);
        core = 1.0 - smoothstep(0.012, 0.105, distanceToCore);
        float filaments = pow(max(0.0, sin((p.x + detail * 0.11) * 26.0 - time * 1.5)), 12.0);
        alpha = taper * ((1.0 - smoothstep(0.08, 0.62, distanceToCore)) * (0.34 + broad * 0.42)
                + core * 0.88 + filaments * 0.28);
    } else if (kind < 2.0) {
        float radius = length(p);
        float angle = atan(p.y, p.x);
        float ring = lineMask(radius - (0.62 + (broad - 0.5) * 0.05), 0.085);
        float spokes = pow(max(0.0, cos(angle * (3.0 + stage) + time * 0.35)), 17.0)
                * smoothstep(0.18, 0.72, radius) * (1.0 - smoothstep(0.72, 0.94, radius));
        core = ring + spokes;
        alpha = ring * 0.78 + spokes * 0.72 + crack * (1.0 - smoothstep(0.2, 0.9, radius)) * 0.42;
    } else if (kind < 3.0) {
        float hex = hexLines(uv * (4.5 + stage * 0.7) + vec2(time * 0.025, 0.0));
        float edge = 1.0 - smoothstep(0.60, 0.99, max(abs(p.x), abs(p.y)));
        float fracture = crack * (0.42 + stageN * 0.30);
        core = hex * (0.54 + stageN * 0.24) + fracture;
        alpha = edge * (0.10 + hex * 0.34 + scan * 0.055 + fracture * 0.38 + fresnel * 0.38);
    } else if (kind < 4.0) {
        float edgeX = lineMask(abs(p.x) - 0.78, 0.16);
        float edgeY = lineMask(abs(p.y) - 0.78, 0.16);
        float diagonal = lineMask(abs(p.x + p.y + (detail - 0.5) * 0.08) - 0.45, 0.055);
        core = max(max(edgeX, edgeY), diagonal);
        alpha = core * (0.66 + scan * 0.30) + fresnel * 0.18;
    } else if (kind < 5.0) {
        float facets = hexLines(uv * (3.0 + stage * 0.55));
        float vertical = pow(max(0.0, sin((uv.x + detail * 0.055) * 25.0)), 16.0);
        core = facets * 0.72 + vertical * 0.52 + crack * 0.48;
        alpha = 0.08 + facets * 0.30 + vertical * 0.22 + crack * 0.30 + fresnel * 0.34;
    } else if (kind < 6.0) {
        float sweep = lineMask(p.x - sin(p.y * 4.0 + time * 0.95) * 0.18, 0.075 + stageN * 0.02);
        float mirrorGrain = pow(detail, 3.0);
        core = sweep + crack * 0.55;
        alpha = 0.045 + sweep * 0.34 + mirrorGrain * 0.16 + fresnel * 0.55;
    } else if (kind < 7.0) {
        float radius = length(p);
        float angle = atan(p.y, p.x);
        float rings = lineMask(fract(radius * (4.0 + stage) - time * 0.32) - 0.5, 0.16);
        float runes = pow(max(0.0, cos(angle * (7.0 + stage * 2.0) + time * 0.24)), 19.0)
                * smoothstep(0.2, 0.96, radius);
        core = rings * 0.52 + runes;
        alpha = (1.0 - smoothstep(0.88, 1.05, radius)) * (rings * 0.37 + runes * 0.74 + crack * 0.24);
    } else {
        float centerLine = 1.0 - smoothstep(0.025, 0.16, abs(p.x + (detail - 0.5) * 0.07));
        float burst = pow(max(0.0, 1.0 - length(p)), 1.7);
        float shards = pow(max(0.0, sin(atan(p.y, p.x) * (8.0 + stage * 2.0) + time)), 14.0) * burst;
        core = max(centerLine, shards);
        alpha = centerLine * (0.50 + scan * 0.42) + shards * 0.76 + crack * burst * 0.32;
    }

    bool orb = vertexColor.b > vertexColor.g * 1.18 || vertexColor.r > vertexColor.g * 1.30;
    alpha *= vertexColor.a * (0.94 + stageN * 0.10);
    alpha *= 0.90 + broad * 0.10;
    if (alpha < 0.009) discard;

    vec3 base = vertexColor.rgb;
    vec3 inner = orb ? vec3(0.78, 0.90, 1.0) : vec3(0.88, 1.0, 1.0);
    vec3 low = orb ? mix(vec3(0.025, 0.07, 0.36), vec3(0.40, 0.015, 0.12), detail * detail)
                   : vec3(0.02, 0.28, 0.46);
    vec3 color = mix(low, base, 0.45 + broad * 0.42);
    color = mix(color, inner, clamp(core * 0.72 + fresnel * 0.26, 0.0, 1.0));
    if (orb)
        color += vec3(0.36, 0.015, 0.08) * crack * (0.50 + stageN * 0.35);
    color *= 1.04 + core * (0.62 + stageN * 0.23) + fresnel * 0.20;
    float fallbackEnergy = dot(texture(Sampler0, uv).rgb, vec3(0.333333));
    float lightEnergy = texture(Sampler2, lightMap).r;
    color *= 0.97 + fallbackEnergy * 0.03;
    color *= 0.96 + lightEnergy * 0.04;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.90) * ColorModulator.a);
}
