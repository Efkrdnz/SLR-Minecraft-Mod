#version 150

uniform float GameTime;
uniform float RankLevel;
uniform float Theme;
uniform float Seed;
uniform vec3 PrimaryColor;
uniform vec3 SecondaryColor;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32 + Seed);
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
    float weight = 0.55;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * weight;
        p = p * 2.03 + vec2(4.7, 8.1);
        weight *= 0.48;
    }
    return value;
}

float lineField(float value, float width) {
    return 1.0 - smoothstep(0.0, width, abs(value));
}

void main() {
    vec2 uv = texCoord;
    float time = GameTime * 24000.0;
    float rank = clamp(RankLevel / 7.0, 0.0, 1.0);
    float intensity = 0.18 + rank * 0.82;
    float phase = Seed * 1.731;

    float slowNoise = fbm(uv * vec2(3.8, 3.0) + vec2(time * 0.004, -time * 0.002) + phase);
    float fineNoise = fbm(uv * vec2(11.0, 7.0) - vec2(time * 0.008, time * 0.003) + slowNoise * 2.0 + phase);
    vec3 baseA = PrimaryColor * (0.028 + rank * 0.024);
    vec3 baseB = SecondaryColor * (0.045 + rank * 0.035);
    vec3 color = mix(vec3(0.004, 0.005, 0.008) + baseA,
                     vec3(0.008, 0.006, 0.011) + baseB, slowNoise * 0.62);

    // Rank progression: brushed metal, then motes, flowing energy, and high-rank pulses.
    float brush = sin((uv.x * 42.0 + uv.y * 19.0) + fineNoise * 3.0) * 0.5 + 0.5;
    color += mix(PrimaryColor, SecondaryColor, uv.y) * brush * (0.012 + rank * 0.025);

    float mote = step(0.985 - rank * 0.035,
            hash(floor(uv * vec2(105.0, 70.0)) + floor(time * (0.025 + rank * 0.045))));
    mote *= 0.25 + 0.75 * sin(time * 0.12 + hash(floor(uv * 90.0)) * 20.0);
    color += PrimaryColor * mote * rank * 0.42;

    float flow = lineField(sin(uv.x * 14.0 + uv.y * 9.0 - time * 0.06 + slowNoise * 5.0), 0.18);
    color += SecondaryColor * flow * smoothstep(0.28, 0.72, rank) * 0.08;

    if (Theme < 0.5) {
        // Layered steel grain; intentionally restrained for standard Association gear.
        float grain = lineField(sin((uv.x - uv.y * 0.28) * 78.0 + phase), 0.12);
        color += mix(vec3(0.22), PrimaryColor, rank) * grain * (0.018 + rank * 0.045);
    } else if (Theme < 1.5) {
        // Frost fractures and suspended ice dust.
        float crystalA = lineField(fract((uv.x + uv.y * 0.57 + slowNoise * 0.13) * 7.0) - 0.5, 0.075);
        float crystalB = lineField(fract((uv.x - uv.y * 0.71 - slowNoise * 0.10) * 6.0) - 0.5, 0.065);
        float snow = step(0.967, hash(floor(vec2(uv.x * 88.0, (uv.y + time * 0.007) * 62.0))));
        color += mix(PrimaryColor, vec3(0.84, 0.98, 1.0), 0.55) * (crystalA + crystalB) * intensity * 0.13;
        color += vec3(0.75, 0.96, 1.0) * snow * intensity * 0.45;
    } else if (Theme < 2.5) {
        // Molten veins and upward embers.
        float vein = lineField(sin(uv.x * 22.0 + uv.y * 15.0 + fineNoise * 7.0 - time * 0.10), 0.16);
        vein *= smoothstep(0.42, 0.70, fineNoise);
        float ember = step(0.972, hash(floor(vec2(uv.x * 96.0, (uv.y + time * 0.014) * 70.0))));
        color += mix(SecondaryColor, PrimaryColor, fineNoise) * vein * intensity * 0.34;
        color += mix(PrimaryColor, vec3(1.0, 0.72, 0.18), 0.55) * ember * intensity * 0.58;
    } else if (Theme < 3.5) {
        // Venom cells and viscous rising bubbles.
        vec2 cells = fract(uv * vec2(17.0, 10.0) + vec2(0.0, -time * 0.006)) - 0.5;
        float membrane = lineField(length(cells) - 0.34, 0.055);
        float bubble = 1.0 - smoothstep(0.035, 0.12, length(cells + vec2(sin(phase) * 0.12, 0.08)));
        color += mix(SecondaryColor, PrimaryColor, 0.68) * membrane * intensity * 0.19;
        color += PrimaryColor * bubble * intensity * 0.22;
    } else if (Theme < 4.5) {
        // Spatial compression orbiting an off-center singularity.
        vec2 center = vec2(0.54 + sin(phase) * 0.08, 0.48 + cos(phase) * 0.06);
        vec2 delta = (uv - center) * vec2(1.0, 1.65);
        float radius = length(delta);
        float angle = atan(delta.y, delta.x);
        float orbit = lineField(sin(radius * 61.0 - angle * 4.0 - time * 0.09 + fineNoise * 3.0), 0.17);
        color += mix(PrimaryColor, SecondaryColor, radius) * orbit * exp(-radius * 2.8) * intensity * 0.24;
        color *= 1.0 - exp(-radius * 14.0) * 0.34;
    } else if (Theme < 5.5) {
        // Living mana vines and drifting leaf sparks.
        float vineA = lineField(uv.x - 0.26 - sin(uv.y * 11.0 - time * 0.035 + phase) * 0.10, 0.025);
        float vineB = lineField(uv.x - 0.73 - cos(uv.y * 9.0 + time * 0.028 + phase) * 0.09, 0.022);
        float leaves = step(0.976, hash(floor(vec2((uv.x + time * 0.004) * 90.0, uv.y * 64.0))));
        color += mix(PrimaryColor, vec3(0.65, 1.0, 0.72), 0.35) * (vineA + vineB) * intensity * 0.18;
        color += PrimaryColor * leaves * intensity * 0.38;
    } else if (Theme < 6.5) {
        // Jagged storm channels with a brief electric core.
        float boltPath = 0.52 + (noise(vec2(uv.y * 12.0, floor(time * 0.07) + phase)) - 0.5) * 0.34;
        float bolt = exp(-abs(uv.x - boltPath) * (95.0 + rank * 55.0));
        float branch = lineField(uv.x - boltPath - sin(uv.y * 28.0 + phase) * 0.14, 0.018);
        color += mix(PrimaryColor, vec3(0.93, 0.98, 1.0), 0.72) * bolt * intensity * 0.62;
        color += SecondaryColor * branch * intensity * 0.10;
    } else if (Theme < 7.5) {
        // Dragon scales, molten seams, and a sovereign fang wake.
        vec2 scaleUv = uv * vec2(19.0, 11.0);
        scaleUv.x += mod(floor(scaleUv.y), 2.0) * 0.5;
        vec2 cell = fract(scaleUv) - 0.5;
        float scales = lineField(length(vec2(cell.x, cell.y * 0.78)) - 0.365, 0.055);
        float fangCenter = 0.80 - uv.x * 0.52 + sin(time * 0.021 + phase) * 0.014;
        float fang = exp(-abs(uv.y - fangCenter) * 80.0);
        color += mix(SecondaryColor, PrimaryColor, 0.42) * scales * intensity * 0.20;
        color += mix(PrimaryColor, vec3(1.0, 0.82, 0.30), 0.52) * fang * intensity * 0.46;
    } else if (Theme < 8.5) {
        // Ethereal mana ribbons.
        float ribbonA = exp(-abs(uv.y - 0.34 - sin(uv.x * 8.0 - time * 0.045 + phase) * 0.11) * 28.0);
        float ribbonB = exp(-abs(uv.y - 0.68 - cos(uv.x * 7.0 + time * 0.038 + phase) * 0.09) * 34.0);
        color += mix(PrimaryColor, vec3(0.82, 1.0, 0.96), 0.48) * (ribbonA + ribbonB) * intensity * 0.16;
    } else if (Theme < 9.5) {
        // Crimson sword wakes and drifting sparks of killing intent.
        float slashA = exp(-abs(uv.y - (0.84 - uv.x * 0.55)) * 58.0);
        float slashB = exp(-abs(uv.y - (0.20 + uv.x * 0.42)) * 74.0);
        color += PrimaryColor * (slashA + slashB * rank) * intensity * 0.25;
    } else if (Theme < 10.5) {
        // Royal geometry and ordered mana channels.
        vec2 grid = abs(fract(uv * vec2(14.0, 8.0) + 0.5) - 0.5);
        float diamond = lineField(grid.x + grid.y - 0.38, 0.055);
        color += mix(PrimaryColor, SecondaryColor, uv.y) * diamond * intensity * 0.16;
    } else if (Theme < 11.5) {
        // Moonlight crescents and refracted glints.
        vec2 p = (uv - vec2(0.22 + mod(Seed, 2.0) * 0.24, 0.50)) * vec2(1.0, 1.55);
        float crescent = lineField(length(p) - 0.25, 0.035) * smoothstep(-0.10, 0.22, p.x);
        float prism = exp(-abs(uv.y - uv.x * 0.32 - 0.22) * 55.0);
        color += mix(PrimaryColor, vec3(0.80, 0.88, 1.0), 0.65) * crescent * intensity * 0.34;
        color += vec3(0.55, 0.82, 1.0) * prism * intensity * 0.13;
    } else {
        // Heavy titan fractures with dust settling through the frame.
        float crack = lineField(sin(uv.x * 19.0 - uv.y * 17.0 + fineNoise * 8.0 + phase), 0.13);
        crack *= smoothstep(0.40, 0.68, fineNoise);
        float dust = step(0.978, hash(floor(vec2(uv.x * 86.0, (uv.y - time * 0.004) * 60.0))));
        color += mix(SecondaryColor, PrimaryColor, fineNoise) * crack * intensity * 0.23;
        color += vec3(0.72, 0.61, 0.45) * dust * intensity * 0.30;
    }

    float edgeDistance = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float edge = exp(-edgeDistance * (20.0 + rank * 8.0));
    float edgeCurrent = sin(uv.x * 23.0 + uv.y * 15.0 - time * (0.05 + rank * 0.08)
            + slowNoise * 6.0 + phase) * 0.5 + 0.5;
    color += mix(SecondaryColor, PrimaryColor, edgeCurrent) * edge * (0.08 + intensity * 0.18);

    float pulse = 0.92 + sin(time * 0.045 + phase) * rank * 0.08;
    float vignette = smoothstep(0.96, 0.30, length((uv - 0.5) * vec2(1.06, 0.84)));
    color *= (0.66 + vignette * 0.34) * pulse;
    fragColor = vec4(color, 0.988);
}
