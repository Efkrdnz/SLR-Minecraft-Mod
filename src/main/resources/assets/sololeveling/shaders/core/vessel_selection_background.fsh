#version 150

uniform float GameTime;
uniform vec2 MousePos;
uniform vec4 ThemeWeights0; // System, Ruler, Shadow, Frost
uniform vec4 ThemeWeights1; // White Flame, Beast, generic Monarch, reserved

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amplitude;
        p = p * 2.03 + vec2(11.7, 7.9);
        amplitude *= 0.5;
    }
    return value;
}

float segmentDistance(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

vec3 systemTheme(vec2 uv, float t) {
    vec3 col = mix(vec3(0.003, 0.006, 0.018), vec3(0.025, 0.055, 0.13), 1.0 - uv.y);
    float flow = fbm(uv * 4.0 + vec2(t * 0.018, -t * 0.012));
    col += vec3(0.02, 0.19, 0.44) * flow * flow * 0.68;
    float scan = 0.5 + 0.5 * sin(uv.y * 640.0 + t * 2.3);
    col += vec3(0.02, 0.08, 0.14) * scan * 0.13;
    return col;
}

vec3 rulerTheme(vec2 uv, float t) {
    vec3 col = mix(vec3(0.025, 0.012, 0.002), vec3(0.13, 0.065, 0.006), 1.0 - uv.y);
    vec2 p = (uv - vec2(0.5, 0.38)) * vec2(1.0, 0.86);
    float radius = length(p);
    float angle = atan(p.y, p.x);
    float rays = pow(0.5 + 0.5 * sin(angle * 17.0 + fbm(p * 5.0) * 7.0 - t * 0.9), 10.0);
    rays *= (1.0 - smoothstep(0.06, 0.62, radius)) * smoothstep(0.06, 0.22, radius);
    float corona = 1.0 - smoothstep(0.05, 0.54, radius);
    float core = 1.0 - smoothstep(0.025, 0.145, radius);
    float ring = exp(-abs(radius - 0.185) * 42.0);
    col += vec3(1.0, 0.42, 0.035) * corona * 0.48;
    col += vec3(1.0, 0.78, 0.22) * rays * 0.72;
    col += vec3(1.0, 0.58, 0.08) * ring * 0.34;
    col += vec3(1.0, 0.91, 0.58) * core * 0.75;
    float dust = step(0.988, hash(floor(uv * vec2(120.0, 170.0)) + floor(t * 1.8)));
    col += vec3(1.0, 0.75, 0.24) * dust * 0.75;
    return col;
}

vec3 shadowTheme(vec2 uv, float t) {
    vec3 col = mix(vec3(0.004, 0.001, 0.008), vec3(0.045, 0.008, 0.075), 1.0 - uv.y);
    vec2 drift = vec2(sin(t * 0.14) * 0.18, -t * 0.09);
    float smoke = fbm(uv * vec2(4.2, 3.2) + drift);
    float smoke2 = fbm(uv * vec2(8.0, 5.0) - drift * 1.4);
    float veil = smoothstep(0.34, 0.84, smoke + smoke2 * 0.35);
    col += vec3(0.18, 0.015, 0.35) * veil * 0.78;
    float aura = exp(-abs(length((uv - vec2(0.5, 0.54)) * vec2(0.88, 1.0)) - 0.34) * 24.0);
    aura *= 0.62 + 0.38 * sin(uv.y * 38.0 - t * 2.0 + smoke * 8.0);
    col += vec3(0.46, 0.06, 0.95) * aura * 0.62;
    float sparks = step(0.992, hash(floor(uv * vec2(150.0, 210.0)) + floor(t * 2.2)));
    col += vec3(0.72, 0.34, 1.0) * sparks;
    return col;
}

vec3 frostTheme(vec2 uv, float t) {
    vec3 col = mix(vec3(0.005, 0.025, 0.055), vec3(0.025, 0.16, 0.26), 1.0 - uv.y);
    float frost = fbm(uv * 6.0 + vec2(t * 0.012, -t * 0.018));
    col += vec3(0.06, 0.31, 0.48) * frost * 0.52;
    float fieldA = abs(fract(uv.x * 2.9 + uv.y * 4.7 + frost * 0.72) - 0.5);
    float fieldB = abs(fract(-uv.x * 4.1 + uv.y * 3.2 + frost * 0.54) - 0.5);
    float cracks = 1.0 - smoothstep(0.006, 0.040, min(fieldA, fieldB));
    col += vec3(0.72, 0.94, 1.0) * cracks * (0.58 + 0.22 * sin(t * 1.7));
    float fog = fbm(vec2(uv.x * 2.8 + t * 0.028, uv.y * 2.2 - t * 0.012));
    fog *= smoothstep(0.18, 0.85, uv.y);
    col += vec3(0.52, 0.73, 0.78) * smoothstep(0.48, 0.82, fog) * 0.34;
    return col;
}

vec3 whiteFlameTheme(vec2 uv, float t) {
    vec3 col = mix(vec3(0.003, 0.012, 0.035), vec3(0.012, 0.07, 0.17), 1.0 - uv.y);
    vec2 flameUv = vec2(uv.x * 5.4, uv.y * 4.0 + t * 0.11);
    float flameNoise = fbm(flameUv + vec2(sin(uv.y * 10.0 + t) * 0.25, 0.0));
    float tongues = smoothstep(0.48, 0.88, flameNoise + (uv.y - 0.42) * 0.27);
    float edgeMask = 1.0 - smoothstep(0.04, 0.42, min(uv.x, 1.0 - uv.x));
    col += vec3(0.04, 0.34, 0.92) * tongues * (0.45 + edgeMask * 0.65);
    col += vec3(0.72, 0.94, 1.0) * pow(tongues, 3.0) * 0.75;
    float boltX1 = 0.23 + sin(uv.y * 19.0 + floor(t * 4.0)) * 0.028 + noise(vec2(uv.y * 13.0, floor(t * 5.0))) * 0.045;
    float boltX2 = 0.78 + sin(uv.y * 23.0 - floor(t * 3.5)) * 0.032 - noise(vec2(uv.y * 11.0, floor(t * 4.0))) * 0.05;
    float bolt = exp(-abs(uv.x - boltX1) * 145.0) + exp(-abs(uv.x - boltX2) * 145.0);
    bolt *= step(0.42, hash(vec2(floor(t * 7.0), 9.0)));
    col += vec3(0.72, 0.93, 1.0) * bolt * 0.95;
    return col;
}

vec3 beastTheme(vec2 uv, float t) {
    vec3 col = mix(vec3(0.018, 0.002, 0.002), vec3(0.12, 0.012, 0.008), 1.0 - uv.y);
    float smoke = fbm(uv * vec2(4.0, 3.0) + vec2(-t * 0.035, t * 0.018));
    col += vec3(0.36, 0.018, 0.006) * smoothstep(0.38, 0.82, smoke) * 0.62;
    float d1 = segmentDistance(uv, vec2(0.12, 0.82), vec2(0.83, 0.17));
    float d2 = segmentDistance(uv, vec2(0.20, 0.88), vec2(0.91, 0.23));
    float d3 = segmentDistance(uv, vec2(0.04, 0.75), vec2(0.74, 0.10));
    float claw = exp(-min(d1, min(d2, d3)) * 105.0);
    float pulse = 0.72 + 0.28 * sin(t * 2.2);
    col += vec3(0.72, 0.015, 0.004) * claw * 0.68 * pulse;
    col += vec3(1.0, 0.33, 0.12) * pow(claw, 3.0) * 0.9;
    return col;
}

void main() {
    vec2 uv = texCoord;
    float t = GameTime * 1200.0;
    vec4 w0 = max(ThemeWeights0, vec4(0.0));
    vec4 w1 = max(ThemeWeights1, vec4(0.0));
    float total = max(0.0001, dot(w0, vec4(1.0)) + dot(w1, vec4(1.0)));

    vec3 col = vec3(0.0);
    if (w0.x > 0.001) col += systemTheme(uv, t) * w0.x;
    if (w0.y > 0.001) col += rulerTheme(uv, t) * w0.y;
    if (w0.z > 0.001) col += shadowTheme(uv, t) * w0.z;
    if (w0.w > 0.001) col += frostTheme(uv, t) * w0.w;
    if (w1.x > 0.001) col += whiteFlameTheme(uv, t) * w1.x;
    if (w1.y > 0.001) col += beastTheme(uv, t) * w1.y;
    if (w1.z > 0.001) col += shadowTheme(uv, t) * w1.z;
    col /= total;

    vec3 accent = (vec3(0.25, 0.78, 1.0) * w0.x
            + vec3(1.0, 0.72, 0.18) * w0.y
            + vec3(0.65, 0.24, 1.0) * w0.z
            + vec3(0.77, 0.94, 1.0) * w0.w
            + vec3(0.48, 0.82, 1.0) * w1.x
            + vec3(1.0, 0.20, 0.10) * w1.y
            + vec3(0.72, 0.34, 1.0) * w1.z) / total;
    float cursor = exp(-length((uv - MousePos) * vec2(1.0, 1.35)) * 8.5);
    col += accent * cursor * 0.12;

    float grain = hash(floor(uv * vec2(180.0, 250.0)) + floor(t * 2.0));
    col += accent * (grain - 0.5) * 0.025;
    float vignette = 1.0 - smoothstep(0.30, 0.92, length((uv - 0.5) * vec2(1.15, 0.94)));
    col *= 0.58 + vignette * 0.42;
    fragColor = vec4(clamp(col, 0.0, 1.0), 0.97);
}
