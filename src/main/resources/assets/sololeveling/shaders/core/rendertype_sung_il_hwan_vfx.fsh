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

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 33.31);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);
    return mix(mix(hash(cell), hash(cell + vec2(1.0, 0.0)), local.x),
               mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0)), local.x),
               local.y);
}

vec3 safeNormalize(vec3 value) {
    return value * inversesqrt(max(dot(value, value), 1.0e-8));
}

void main() {
    float material = floor(texCoord0.x);
    vec2 uv = vec2(fract(texCoord0.x), texCoord0.y);
    vec2 centered = uv * 2.0 - 1.0;
    float time = GameTime * 2400.0;
    float coarse = valueNoise(uv * vec2(9.0, 13.0)
            + vec2(time * 0.021, -time * 0.013));
    float fine = valueNoise(uv * vec2(31.0, 37.0)
            - vec2(time * 0.017, time * 0.009));
    float light = texture(Sampler2, clamp(lightMap, 0.0, 1.0)).r;
    float whiteSample = dot(texture(Sampler0, uv).rgb,
            vec3(0.333333));
    float edgeDistance = min(min(uv.x, 1.0 - uv.x),
            min(uv.y, 1.0 - uv.y));
    float edge = 1.0 - smoothstep(0.018, 0.13, edgeDistance);
    vec3 normal = safeNormalize(viewNormal);
    vec3 viewDirection = safeNormalize(-viewPosition);
    float fresnel = pow(1.0 - abs(dot(normal, viewDirection)), 2.0);
    vec3 base = max(vertexColor.rgb, vec3(0.004));
    vec3 color;
    float alpha = vertexColor.a;

    if (material < 1.0) {
        // Near-black spatial cloth with a restrained ruler-gold grazing edge.
        float fold = 0.58 + coarse * 0.22 + fine * 0.08;
        color = base * fold * (0.26 + light * 0.48);
        color += vec3(0.78, 0.48, 0.08)
                * (edge * 0.075 + fresnel * 0.09);
        alpha *= 0.86 + fine * 0.12;
    } else if (material < 2.0) {
        // Antique ruler-gold targeting geometry.
        float grain = 0.84 + fine * 0.2;
        color = base * grain * (0.76 + light * 0.38);
        color += vec3(1.0, 0.68, 0.16)
                * (edge * 0.18 + fresnel * 0.12);
    } else if (material < 3.0) {
        // White-hot ruler gold drives every active ability silhouette.
        float pulse = 0.91 + 0.09
                * sin(time * 0.42 + coarse * 4.0);
        color = base * pulse;
        color += vec3(1.0, 0.76, 0.22)
                * (edge * 0.24 + fresnel * 0.11);
    } else if (material < 4.0) {
        // A narrow broken core makes spatial fractures read without bloom.
        float crack = 1.0 - smoothstep(0.025, 0.29,
                abs(centered.x + (fine - 0.5) * 0.16));
        color = mix(base * (0.38 + light * 0.3),
                vec3(1.0, 0.84, 0.34), crack * 0.72);
        alpha *= 0.72 + crack * 0.28;
    } else if (material < 5.0) {
        // Soft, code-authored aura blades; no external bitmap mask required.
        float radial = 1.0 - smoothstep(0.12, 1.0,
                dot(centered, centered));
        float verticalFade = smoothstep(0.0, 0.18, uv.y)
                * (1.0 - smoothstep(0.72, 1.0, uv.y));
        color = base * (0.62 + coarse * 0.22);
        alpha *= max(radial * 0.72, verticalFade * 0.58);
    } else {
        // Spatial cuts use tapered geometry plus a procedural razor profile.
        // This remains a narrow luminous edge instead of filling the authored
        // ribbon like a flat card. Geometry provides pointed tips in the
        // vanilla fallback; this mask adds the white-hot cutting filament.
        float spineTaper = pow(max(0.0,
                sin(uv.y * 3.14159265)), 0.58);
        float spineOffset = (fine - 0.5) * 0.045 * spineTaper;
        float transverse = abs(centered.x - spineOffset);
        float blade = 1.0 - smoothstep(0.68, 1.0, transverse);
        float filament = 1.0 - smoothstep(0.025, 0.19, transverse);
        float needle = 1.0 - smoothstep(0.0, 0.055, transverse);
        float tipFade = smoothstep(0.0, 0.035, uv.y)
                * (1.0 - smoothstep(0.965, 1.0, uv.y));
        float shimmer = 0.9 + 0.1
                * sin(time * 0.72 + uv.y * 31.0 + fine * 2.2);
        color = base * (0.72 + blade * 0.28) * shimmer;
        color += vec3(1.0, 0.69, 0.14) * filament * 0.46;
        color += vec3(1.0, 0.965, 0.78) * needle * 0.72;
        alpha *= tipFade * blade
                * (0.76 + filament * 0.18 + needle * 0.16);
    }

    color *= 0.985 + whiteSample * 0.015;
    alpha *= ColorModulator.a;
    if (alpha < 0.008) {
        discard;
    }
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.96));
}
