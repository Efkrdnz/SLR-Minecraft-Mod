#version 150

uniform float GameTime;
uniform float Seed;
uniform vec3 PrimaryColor;
uniform vec3 SecondaryColor;

in vec2 texCoord;
out vec4 fragColor;

float randomCell(vec2 cell) {
    vec3 p = fract(vec3(cell.xyx) * vec3(0.1031, 0.11369, 0.13787));
    p += dot(p, p.yzx + 19.19 + Seed);
    return fract((p.x + p.y) * p.z);
}

float narrowLine(float value, float sharpness) {
    return exp(-abs(value) * sharpness);
}

float manaRibbon(vec2 uv, float origin, float bend, float time) {
    float approach = smoothstep(0.02, 0.78, uv.x);
    float destination = 0.50 + sin(time * 0.026 + bend) * 0.012;
    float wandering = origin
            + sin(uv.x * (7.0 + bend) - time * (0.045 + bend * 0.003) + Seed) * 0.075
                    * (1.0 - approach);
    float path = mix(wandering, destination, approach);
    float taper = mix(24.0, 92.0, approach);
    return narrowLine(uv.y - path, taper)
            * smoothstep(0.00, 0.14, uv.x)
            * (1.0 - smoothstep(0.80, 0.93, uv.x));
}

void main() {
    vec2 uv = texCoord;
    float time = GameTime * 24000.0;
    vec2 center = vec2(0.76, 0.50);
    vec2 lens = (uv - center) * vec2(1.0, 1.55);
    float radius = length(lens);
    float angle = atan(lens.y, lens.x);

    vec3 color = mix(vec3(0.003, 0.004, 0.009), vec3(0.024, 0.003, 0.010),
            smoothstep(0.0, 1.0, uv.x));
    color += SecondaryColor * (0.015 + 0.028 * (1.0 - uv.y));

    // Three blue mana currents are visibly pulled into the blood relic.
    float current = manaRibbon(uv, 0.23, 0.7, time)
            + manaRibbon(uv, 0.49, 2.2, time)
            + manaRibbon(uv, 0.77, 4.1, time);
    float currentPulse = 0.68 + 0.32 * sin(time * 0.085 - uv.x * 31.0);
    color += PrimaryColor * current * currentPulse * 0.34;
    color += vec3(0.68, 0.94, 1.0) * current * current * 0.16;

    // A rotating appraisal seal surrounds the orb without resembling the other
    // tooltip theme branches: broken glyph dashes orbit in opposing directions.
    float outerRing = narrowLine(radius - 0.345, 155.0);
    float innerRing = narrowLine(radius - 0.285, 190.0);
    float outerGlyphs = smoothstep(0.30, 0.86,
            sin(angle * 22.0 - time * 0.052 + Seed * 2.0));
    float innerGlyphs = smoothstep(0.42, 0.92,
            cos(angle * 15.0 + time * 0.071 - Seed));
    color += PrimaryColor * outerRing * outerGlyphs * 0.28;
    color += SecondaryColor * innerRing * innerGlyphs * 0.34;

    // The petrified-blood shell has radial mineral seams and a light-swallowing core.
    float shellMask = (1.0 - smoothstep(0.225, 0.255, radius))
            * smoothstep(0.035, 0.075, radius);
    float strata = sin(angle * 9.0 + log(max(radius, 0.018)) * 28.0
            - time * 0.014 + sin(angle * 3.0 - time * 0.019) * 2.4);
    float mineralSeam = narrowLine(strata, 9.5) * shellMask;
    float shellLight = (1.0 - smoothstep(0.07, 0.26, radius))
            * (0.38 + 0.62 * max(0.0, dot(normalize(lens + vec2(0.0001)), normalize(vec2(-0.8, -0.6)))));
    color += SecondaryColor * shellMask * (0.10 + shellLight * 0.22);
    color += mix(SecondaryColor, vec3(0.88, 0.15, 0.24), 0.55) * mineralSeam * 0.30;
    color *= 1.0 - (1.0 - smoothstep(0.0, 0.105, radius)) * 0.72;

    // Sparse sparks accelerate toward the focus, making the effect feel hungry
    // rather than like the embers, lightning, or ribbons used by other weapons.
    vec2 sparkGrid = floor(vec2(uv.x * 86.0 - time * 0.34,
            uv.y * 61.0 + sin(uv.x * 8.0) * 3.0));
    float sparkSeed = randomCell(sparkGrid);
    float spark = step(0.975, sparkSeed);
    float attraction = smoothstep(0.02, 0.72, uv.x)
            * (1.0 - smoothstep(0.74, 0.90, uv.x));
    float sparkBlink = 0.45 + 0.55 * sin(time * 0.19 + sparkSeed * 31.0);
    color += mix(PrimaryColor, vec3(0.82, 0.97, 1.0), 0.62)
            * spark * attraction * sparkBlink * 0.52;

    float borderDistance = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float border = exp(-borderDistance * 30.0);
    float borderFlow = 0.55 + 0.45 * sin(time * 0.048 + uv.x * 25.0 - uv.y * 17.0);
    color += mix(SecondaryColor, PrimaryColor, borderFlow) * border * 0.16;

    float vignette = 1.0 - smoothstep(0.20, 0.86, length((uv - 0.5) * vec2(0.92, 0.80)));
    color *= 0.72 + vignette * 0.28;
    fragColor = vec4(color, 0.989);
}
