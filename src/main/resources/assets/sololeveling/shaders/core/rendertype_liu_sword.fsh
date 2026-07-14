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

float lineMask(float distanceToLine, float width) {
    return 1.0 - smoothstep(width * 0.42, width, distanceToLine);
}

float cutTaper(float x) {
    return pow(max(0.0, sin(x * 3.14159265)), 0.72);
}

float angularCutPath(float x) {
    float grid = x * 5.0;
    float cell = floor(grid);
    float local = fract(grid);
    float from = hash(vec2(cell, 19.73)) * 2.0 - 1.0;
    float to = hash(vec2(cell + 1.0, 19.73)) * 2.0 - 1.0;
    float tipGuard = pow(max(0.0, sin(x * 3.14159265)), 0.48);
    return mix(from, to, local) * 0.052 * tipGuard;
}

void main() {
    float kind = floor(texCoord0.x);
    vec2 uv = vec2(fract(texCoord0.x), texCoord0.y);
    vec2 p = uv * 2.0 - 1.0;
    float time = GameTime * 2400.0;
    float n = noise(vec2(uv.x * 7.0 - time * 0.24, uv.y * 8.5 + time * 0.13));
    float fine = noise(vec2(uv.x * 18.0 + n * 2.1, uv.y * 21.0 - time * 0.48));
    float edge = smoothstep(0.0, 0.075, uv.x) * smoothstep(0.0, 0.075, 1.0 - uv.x)
               * smoothstep(0.0, 0.06, uv.y) * smoothstep(0.0, 0.06, 1.0 - uv.y);
    float alpha = 0.0;
    float core = 0.0;

    if (kind < 1.0) {
        float taper = cutTaper(uv.x);
        float cut = p.y - (p.x * 0.022 + angularCutPath(uv.x));
        float distanceToCut = abs(cut);
        float blade = lineMask(distanceToCut, 0.038 * taper + 0.0035);
        float halo = lineMask(distanceToCut, 0.088 * taper + 0.008) * 0.22;
        float facets = 0.84 + 0.16 * step(0.34,
                hash(vec2(floor(uv.x * 18.0), 7.31)) + fine * 0.08);
        alpha = edge * taper * (halo + blade * facets);
        core = lineMask(distanceToCut, 0.0105 * taper + 0.0018);
    } else if (kind < 2.0) {
        float taper = cutTaper(uv.x);
        float cut = p.y - (p.x * 0.022 + angularCutPath(uv.x));
        float distanceToCut = abs(cut);
        float innerBloom = lineMask(distanceToCut, 0.092 * taper + 0.011);
        float outerBloom = lineMask(distanceToCut, 0.17 * taper + 0.018);
        float energyFlow = 0.58 + fine * 0.28
                + 0.14 * step(0.62, noise(vec2(uv.x * 12.0 - time * 0.18, 3.7)));
        alpha = edge * taper * (innerBloom * 0.43 + outerBloom * 0.11) * energyFlow;
        core = lineMask(distanceToCut, 0.022 * taper + 0.003) * 0.22;
    } else if (kind < 3.0) {
        float taper = cutTaper(uv.x);
        float cut = p.y - (p.x * 0.022 + angularCutPath(uv.x));
        float segmentGrid = uv.x * 12.0;
        float segment = floor(segmentGrid);
        float segmentLocal = fract(segmentGrid);
        float segmentAlive = step(0.24, hash(vec2(segment, 41.17)));
        float segmentEdge = smoothstep(0.0, 0.085, segmentLocal)
                * smoothstep(0.0, 0.085, 1.0 - segmentLocal);
        float stream = lineMask(abs(cut), 0.055 * taper + 0.006);
        float flicker = 0.48 + fine * 0.34;
        alpha = edge * taper * stream * segmentAlive * segmentEdge * flicker * 0.62;
        core = stream * segmentAlive * segmentEdge * 0.08;
    } else if (kind < 4.0) {
        float radius = length(vec2(p.x, p.y * 0.88));
        float ring = lineMask(abs(radius - 0.63 - (n - 0.5) * 0.045), 0.075);
        float cuts = pow(max(0.0, sin(atan(p.y, p.x) * 8.0 - time * 0.55 + fine)), 18.0)
                   * (1.0 - smoothstep(0.18, 0.92, radius));
        alpha = edge * (ring + cuts * 0.8);
        core = lineMask(abs(radius - 0.63), 0.024);
    } else if (kind < 5.0) {
        float radius = length(p);
        float angle = atan(p.y, p.x);
        float ring = lineMask(abs(radius - 0.48 - (n - 0.5) * 0.08), 0.12);
        float rays = pow(max(0.0, sin(angle * 12.0 + fine * 2.0 - time * 0.7)), 16.0)
                   * (1.0 - smoothstep(0.08, 0.92, radius));
        float crossCut = lineMask(abs(p.y - p.x * 0.18), 0.05) + lineMask(abs(p.y + p.x * 0.18), 0.05);
        alpha = edge * (ring + rays * 0.9 + crossCut * (1.0 - smoothstep(0.15, 0.95, radius)) * 0.72);
        core = ring * 0.65 + crossCut;
    } else if (kind < 6.0) {
        float radius = length(p);
        float rings = lineMask(abs(fract(radius * 5.0 - time * 0.08) - 0.5), 0.15);
        float spokes = pow(max(0.0, sin(atan(p.y, p.x) * 14.0 + n * 3.0 + time * 0.22)), 18.0);
        alpha = (1.0 - smoothstep(0.18, 1.0, radius)) * (rings * 0.38 + spokes * 0.42 + n * 0.22);
        core = spokes * 0.35;
    } else if (kind < 7.0) {
        float vertical = smoothstep(-0.94, -0.1, p.y) * (1.0 - smoothstep(0.25, 1.0, p.y));
        float plume = 1.0 - smoothstep(0.12 + n * 0.14, 0.62, abs(p.x));
        float lick = smoothstep(0.34, 0.82, n + fine * 0.23);
        alpha = edge * vertical * plume * (0.25 + lick * 0.75);
        core = plume * smoothstep(-0.8, 0.28, p.y) * (1.0 - smoothstep(-0.15, 0.72, p.y));
    } else if (kind < 8.0) {
        float radius = length(p);
        float shield = lineMask(abs(radius - 0.59 - (n - 0.5) * 0.035), 0.09);
        float facets = pow(max(0.0, cos(atan(p.y, p.x) * 6.0 + time * 0.18)), 22.0)
                     * (1.0 - smoothstep(0.12, 0.82, radius));
        alpha = edge * (shield + facets * 0.85);
        core = lineMask(abs(radius - 0.59), 0.025);
    } else if (kind < 9.0) {
        float streak = lineMask(abs(p.y - (n - 0.5) * 0.24), 0.18);
        float tail = pow(max(0.0, 1.0 - uv.x), 1.6);
        alpha = edge * streak * tail * (0.28 + fine * 0.72);
        core = streak * tail * 0.4;
    } else if (kind < 10.0) {
        float fireNoise = noise(p * 3.8 + vec2(time * 0.18, -time * 0.31));
        float fireFine = noise(p * 8.5 + vec2(-time * 0.42, time * 0.21));
        float radius = length(vec2(p.x, p.y * 0.9));
        float angle = atan(p.y, p.x);
        float boundary = 0.7 + (fireNoise - 0.5) * 0.24
                + sin(angle * 7.0 + fireFine * 2.2) * 0.045;
        float fireball = 1.0 - smoothstep(boundary - 0.18, boundary, radius);
        float pressureShell = lineMask(abs(radius - 0.61 - (fireNoise - 0.5) * 0.07), 0.095);
        float burstRays = pow(max(0.0, sin(angle * 9.0 + fireFine * 2.6 - time * 0.24)), 12.0)
                * (1.0 - smoothstep(0.18, 0.94, radius));
        float crown = (1.0 - smoothstep(0.12 + fireNoise * 0.08, 0.58, abs(p.x)))
                * (1.0 - smoothstep(-0.9, 0.25, p.y))
                * smoothstep(-1.04, -0.34, p.y + fireFine * 0.09);
        alpha = edge * (fireball * (0.5 + fireFine * 0.5)
                + pressureShell * 0.34 + burstRays * 0.5 + crown * 0.58);
        core = (1.0 - smoothstep(0.08, 0.4 + fireFine * 0.05, radius)) * fireball
                + pressureShell * 0.12;
    } else {
        float bridgeTaper = pow(max(0.0, sin(uv.y * 3.14159265)), 0.38);
        float bridgeBend = (noise(vec2(uv.y * 7.0 - time * 0.12, 17.31)) - 0.5)
                * 0.24 * bridgeTaper;
        float bridgeDistance = abs(p.x - bridgeBend);
        float bridge = lineMask(bridgeDistance, 0.22 * bridgeTaper + 0.028);
        float hotBridge = lineMask(bridgeDistance, 0.065 * bridgeTaper + 0.008);
        float flow = noise(vec2(uv.y * 18.0 - time * 0.82, p.x * 2.4 + 6.17));
        float embers = smoothstep(0.62, 0.9, flow) * bridge;
        alpha = edge * bridgeTaper * bridge * (0.52 + flow * 0.48) + embers * 0.22;
        core = hotBridge * (0.64 + embers * 0.36);
    }

    alpha *= vertexColor.a;
    if (alpha < 0.012) discard;

    vec3 base = max(vertexColor.rgb, vec3(0.04));
    float luminance = dot(base, vec3(0.299, 0.587, 0.114));
    vec3 saturated = mix(vec3(luminance), base, 1.28);
    vec3 color;
    if (kind >= 9.0 && kind < 10.0) {
        vec3 fireHot = vec3(1.0, 0.88, 0.24);
        float fireEnergy = clamp(core * 1.48 + fine * 0.12, 0.0, 1.0);
        color = mix(saturated * (0.95 + n * 0.32), fireHot * 1.52, fireEnergy);
    } else if (kind >= 10.0) {
        vec3 bridgeHot = vec3(1.0, 0.95, 0.52);
        float bridgeEnergy = clamp(core * 1.7 + fine * 0.1, 0.0, 1.0);
        color = mix(saturated * (1.0 + n * 0.24), bridgeHot * 1.48, bridgeEnergy);
    } else {
        vec3 whiteHot = vec3(1.0, 0.985, 0.89);
        if (base.b > base.r * 1.15) whiteHot = vec3(0.87, 0.98, 1.0);
        if (base.r > base.g * 1.42) whiteHot = vec3(1.0, 0.78, 0.72);
        float energy = clamp(core * 1.72 + fine * 0.16, 0.0, 1.0);
        color = mix(saturated * (1.05 + n * 0.28), whiteHot * 1.6, energy);
    }
    float fallback = dot(texture(Sampler0, uv).rgb, vec3(0.333333));
    float light = texture(Sampler2, lightMap).r;
    color *= 0.97 + fallback * 0.03;
    color *= 0.96 + light * 0.04;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.94) * ColorModulator.a);
}
