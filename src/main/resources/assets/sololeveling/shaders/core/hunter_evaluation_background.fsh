#version 150

uniform float GameTime;
uniform vec2 MousePos;
uniform vec3 ClassColor;
uniform float RankIntensity;
uniform float WaveStrength;
uniform float Reveal;
uniform float HoldCharge;
uniform float ScanSweep;

in vec2 texCoord;
out vec4 fragColor;

// Panel-relative centre of the appraisal gem, matching HunterEvaluationScreen's
// GEM_CY / PANEL_H so the backdrop's rings sit concentric with the drawn dial.
const float GEM_CY = 0.422;

// Sin-free hash: this runs several times per fragment over the whole panel, and
// at GUI scale 3-4 that panel is most of a megapixel. Pure ALU is much cheaper
// than a transcendental and avoids driver-dependent banding.
float hash(vec2 point) {
    vec3 p3 = fract(vec3(point.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    vec2 curve = local * local * (3.0 - 2.0 * local);
    float a = hash(cell);
    float b = hash(cell + vec2(1.0, 0.0));
    float c = hash(cell + vec2(0.0, 1.0));
    float d = hash(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, curve.x), mix(c, d, curve.x), curve.y);
}

// Distance to the nearest edge of a hex lattice cell. The chamber floor reads as
// an inlaid mana circuit rather than a flat gradient.
float hexEdge(vec2 p) {
    vec2 skewed = vec2(p.x * 1.1547, p.y + p.x * 0.5774);
    vec2 cell = floor(skewed);
    vec2 local = fract(skewed);
    float edge = min(min(local.x, local.y), min(1.0 - local.x, 1.0 - local.y));
    edge = min(edge, abs(local.x - local.y) * 0.7071);
    return edge + hash(cell) * 0.06;
}

void main() {
    vec2 uv = texCoord;
    vec2 centered = (uv - vec2(0.5, GEM_CY)) * vec2(1.18, 1.0);
    float time = GameTime * 1200.0;
    float radius = length(centered);
    float angle = atan(centered.y, centered.x);
    float charge = clamp(HoldCharge, 0.0, 1.0);

    vec3 accent = max(ClassColor, vec3(0.02));
    vec3 color = mix(vec3(0.002, 0.004, 0.012),
            accent * (0.055 + RankIntensity * 0.10), 1.0 - uv.y);

    // Inlaid circuit lattice, energised by the hold and by how far the ceremony
    // has progressed. Brightest near the gem, fading out toward the frame.
    float lattice = 1.0 - smoothstep(0.012, 0.05, hexEdge(uv * vec2(13.0, 11.0)));
    float latticeReach = exp(-radius * 3.1);
    float latticePulse = 0.35 + 0.65 * sin(time * 1.1 - radius * 18.0);
    color += accent * lattice * latticeReach
            * (0.05 + charge * 0.30 + Reveal * 0.10)
            * (0.55 + 0.45 * latticePulse);

    float flowingNoise = noise(uv * vec2(6.0, 4.0)
            + vec2(time * 0.026, -time * 0.018));
    float wave = 0.5 + 0.5 * sin(radius * 92.0
            - time * (2.0 + WaveStrength)
            + flowingNoise * 8.0);
    float ringMask = 1.0 - smoothstep(0.12, 0.62, radius);
    color += accent * wave * ringMask * (0.08 + WaveStrength * 0.10);

    float spiral = 0.5 + 0.5 * sin(angle * 9.0 - radius * 41.0
            + time * (1.45 + charge * 2.2));
    spiral = pow(spiral, 7.0) * (1.0 - smoothstep(0.08, 0.55, radius));
    color += accent * spiral * WaveStrength * (0.18 + charge * 0.22);

    // Mana pulled inward: rings that contract toward the gem while charging.
    if (charge > 0.01) {
        float inflow = fract(radius * 6.0 + time * 1.5);
        float pull = pow(1.0 - inflow, 5.0)
                * (1.0 - smoothstep(0.10, 0.58, radius));
        color += accent * pull * charge * 0.55;
    }

    float core = 1.0 - smoothstep(0.015, 0.24, radius);
    float halo = exp(-abs(radius - (0.20 + Reveal * 0.035)) * 29.0);
    color += accent * core * (0.22 + RankIntensity * 0.52 + charge * 0.30);
    color += accent * halo * (0.11 + Reveal * 0.22 + charge * 0.26);

    // A single bright line sweeps top to bottom while the signature is read.
    if (ScanSweep > 0.001) {
        float sweepY = mix(-0.06, 1.06, ScanSweep);
        float band = exp(-abs(uv.y - sweepY) * 150.0);
        float trail = exp(-max(0.0, uv.y - sweepY) * 16.0) * 0.22;
        color += (accent * 0.6 + vec3(0.35)) * (band * 0.85 + trail * band + trail * 0.25);
    }

    if (RankIntensity > 0.96) {
        float whiteCore = 1.0 - smoothstep(0.01, 0.115, radius);
        color += vec3(1.0, 0.98, 0.92) * whiteCore * 0.82;
    }

    float cursor = exp(-length((uv - MousePos) * vec2(1.0, 1.35)) * 11.0);
    color += accent * cursor * (0.08 + charge * 0.10);
    float grain = hash(floor(uv * vec2(190.0, 250.0)) + floor(time * 2.0));
    color += accent * (grain - 0.5) * 0.026;

    float vignette = 1.0 - smoothstep(0.34, 0.91,
            length((uv - 0.5) * vec2(1.10, 0.94)));
    color *= 0.58 + vignette * 0.42;
    // Filmic roll-off keeps the charged core from clipping to a flat white disc.
    color = color / (color + 0.82) * 1.38;
    fragColor = vec4(clamp(color, 0.0, 1.0), 0.975);
}
