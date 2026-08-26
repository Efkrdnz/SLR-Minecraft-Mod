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
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 34.45);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
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
    float coarse = noise(uv * vec2(13.0, 17.0) + vec2(time * 0.025, 0.0));
    float fine = noise(uv * vec2(41.0, 37.0) - vec2(0.0, time * 0.018));
    float edgeDistance = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float edge = 1.0 - smoothstep(0.025, 0.15, edgeDistance);
    float light = texture(Sampler2, clamp(lightMap, 0.0, 1.0)).r;
    float whiteSample = dot(texture(Sampler0, uv).rgb, vec3(0.333333));
    vec3 base = max(vertexColor.rgb, vec3(0.008));
    float alpha = vertexColor.a;
    vec3 color;

    if (material < 1.0) {
        // Dark hammered steel. Broad dents are deliberately subtle so the
        // silhouette stays legible under resource packs and low light.
        float hammer = 0.76 + coarse * 0.16 + fine * 0.08;
        float fresnel = pow(1.0 - abs(dot(safeNormalize(viewNormal),
                safeNormalize(-viewPosition))), 2.0);
        color = base * hammer * (0.34 + light * 0.76);
        color += vec3(0.28, 0.31, 0.30) * edge * (0.08 + fresnel * 0.1);
    } else if (material < 2.0) {
        // Pale-gold locking seams and gameplay boundaries.
        float grain = 0.9 + fine * 0.18;
        color = base * grain + vec3(1.0, 0.82, 0.42) * edge * 0.2;
    } else if (material < 3.0) {
        // Ember is reserved for confirmed impacts, Strain, and fractures.
        float flicker = 0.86 + 0.14 * sin(time * 0.85 + coarse * 5.0);
        color = base * flicker + vec3(1.0, 0.32, 0.07) * edge * 0.28;
    } else if (material < 4.0) {
        float hotCore = 1.0 - smoothstep(0.0, 0.38, abs(centered.x));
        color = mix(base * 0.78, vec3(1.0, 0.52, 0.16), hotCore * 0.62);
        alpha *= 0.82 + fine * 0.18;
    } else {
        // Camera-facing dust/spark quads use the only soft mask in the family.
        float radial = 1.0 - smoothstep(0.12, 1.0, dot(centered, centered));
        color = base * (0.82 + fine * 0.24);
        alpha *= radial;
    }

    color *= 0.985 + whiteSample * 0.015;
    alpha *= ColorModulator.a;
    if (alpha < 0.01) {
        discard;
    }
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.96));
}
