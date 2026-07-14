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
    p += dot(p, p + 41.37);
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
    float weight = 0.58;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * weight;
        p = p * 2.05 + vec2(4.7, 8.3);
        weight *= 0.46;
    }
    return value;
}

void main() {
    float kind = floor(texCoord0.x);
    vec2 uv = vec2(fract(texCoord0.x), texCoord0.y);
    vec2 p = uv * 2.0 - 1.0;
    float time = GameTime * 1900.0;
    float broad = fbm(vec2(uv.x * 5.0 + time * 0.16, uv.y * 5.8 - time * 0.42));
    float crystal = fbm(vec2(uv.x * 17.0 - time * 0.31, uv.y * 13.0 + broad * 3.2));
    float alpha;
    float core;

    if (kind < 1.0) {
        float diamond = 1.0 - smoothstep(0.48, 1.0, abs(p.x) + abs(p.y));
        float fracture = pow(max(0.0, sin((p.x * 1.7 + p.y) * 23.0 + crystal * 8.0)), 12.0);
        alpha = diamond * (0.48 + broad * 0.46 + fracture * 0.68);
        core = diamond * smoothstep(0.42, 0.86, crystal + 0.32);
    } else if (kind < 2.0) {
        float side = 1.0 - smoothstep(0.12, 0.96, abs(p.x));
        float taper = smoothstep(0.0, 0.12, uv.y) * smoothstep(0.0, 0.24, 1.0 - uv.y);
        float streak = pow(max(0.0, sin((uv.x + broad * 0.18) * 19.0 - uv.y * 8.0 + time)), 9.0);
        alpha = side * taper * (0.16 + broad * 0.62 + streak * 0.52);
        core = side * taper * smoothstep(0.68, 1.12, crystal + streak * 0.45);
    } else {
        float crest = 1.0 - smoothstep(0.58, 1.02, abs(p.x) + abs(p.y) * 0.46);
        float veins = pow(max(0.0, sin((p.x * 1.4 - p.y) * 27.0 + crystal * 9.0)), 11.0);
        alpha = crest * (0.18 + broad * 0.50 + veins * 0.62);
        core = crest * smoothstep(0.65, 1.03, crystal + veins * 0.35);
    }

    alpha *= vertexColor.a;
    if (alpha < 0.012) discard;

    vec3 deepIce = vec3(0.025, 0.24, 0.52);
    vec3 frost = vec3(0.20, 0.78, 1.0);
    vec3 whiteIce = vec3(0.88, 0.99, 1.0);
    vec3 color = mix(deepIce, frost, broad * 0.72 + 0.2);
    color = mix(color, whiteIce, core * 0.92);
    color *= 1.16 + core * 1.35;
    float fallbackEnergy = dot(texture(Sampler0, uv).rgb, vec3(0.333333));
    float lightEnergy = texture(Sampler2, lightMap).r;
    color *= 0.95 + fallbackEnergy * 0.05;
    color *= 0.95 + lightEnergy * 0.05;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.94) * ColorModulator.a);
}
