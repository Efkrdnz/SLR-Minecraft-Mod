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
    p += dot(p, p + 34.45);
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
        p = p * 2.04 + vec2(7.1, 3.7);
        weight *= 0.47;
    }
    return value;
}

void main() {
    float kind = floor(texCoord0.x);
    vec2 uv = vec2(fract(texCoord0.x), texCoord0.y);
    vec2 p = uv * 2.0 - 1.0;
    float time = GameTime * 1800.0;
    float broad = fbm(vec2(uv.x * 4.5 + time * 0.05, uv.y * 5.0 - time * 0.31));
    float detail = fbm(vec2(uv.x * 13.0 - time * 0.13 + broad * 2.2, uv.y * 11.0 - time * 0.58));
    float side = 1.0 - smoothstep(0.25, 0.52, abs(uv.x - 0.5));
    float ends = smoothstep(0.0, 0.08, uv.y) * smoothstep(0.0, 0.12, 1.0 - uv.y);
    float alpha = 0.0;

    if (kind < 1.0) {
        float fork = pow(max(0.0, sin((uv.x + detail * 0.16) * 31.0 + uv.y * 9.0 - time * 0.9)), 12.0);
        alpha = ends * (side * (0.18 + broad * 0.72) + fork * 0.9);
    } else if (kind < 2.0) {
        float lightning = pow(max(0.0, sin(uv.y * 34.0 + detail * 8.0 - time)), 10.0);
        alpha = side * ends * (0.25 + broad * 0.55 + lightning * 0.85);
    } else if (kind < 3.0) {
        float radius = length(p);
        float ring = 1.0 - smoothstep(0.03, 0.13, abs(radius - 0.57 - (broad - 0.5) * 0.06));
        float crown = pow(max(0.0, sin(atan(p.y, p.x) * 7.0 + time * 0.25)), 8.0);
        alpha = (ring + crown * (1.0 - smoothstep(0.2, 0.9, radius)) * 0.7) * (1.0 - smoothstep(0.82, 1.0, radius));
    } else if (kind < 4.0) {
        float body = 1.0 - smoothstep(0.30 + broad * 0.08, 0.50, abs(p.x));
        float head = 1.0 - smoothstep(0.18, 0.32, length(vec2(p.x, p.y - 0.53)));
        float flame = smoothstep(0.28, 0.78, broad + detail * 0.35);
        alpha = (body * smoothstep(-0.9, -0.55, p.y) + head) * (0.28 + flame * 0.72) * ends;
    } else if (kind < 5.0) {
        float radius = length(p);
        float portal = 1.0 - smoothstep(0.04, 0.12, abs(radius - 0.62 - (broad - 0.5) * 0.07));
        float inside = (1.0 - smoothstep(0.2, 0.62, radius)) * smoothstep(0.45, 0.82, broad + detail * 0.25);
        alpha = portal + inside * 0.4;
    } else if (kind < 6.0) {
        float radius = length(p);
        float streak = pow(max(0.0, 1.0 - abs(p.y + sin(p.x * 7.0 + time) * 0.12)), 14.0);
        alpha = (1.0 - smoothstep(0.2, 1.0, radius)) * (streak + detail * 0.35);
    } else {
        float radius = length(p);
        float ring = 1.0 - smoothstep(0.04, 0.15, abs(radius - 0.44 - broad * 0.12));
        float rays = pow(max(0.0, sin(atan(p.y, p.x) * 11.0 - time * 0.7)), 12.0) * (1.0 - smoothstep(0.1, 0.86, radius));
        alpha = ring + rays;
    }

    alpha *= vertexColor.a;
    if (alpha < 0.012) discard;

    float core = smoothstep(0.67, 1.08, detail + side * 0.28);
    vec3 smoke = vec3(0.33, 0.36, 0.42);
    vec3 silver = vec3(0.78, 0.91, 1.0);
    vec3 whiteHot = vec3(1.0, 1.0, 0.94);
    vec3 blood = vec3(0.85, 0.035, 0.045);
    vec3 color = mix(smoke, silver, broad * 0.75 + 0.15);
    if (kind >= 6.0) color = mix(blood, whiteHot, core * 0.72);
    else color = mix(color, whiteHot, core * 0.88);
    color *= 1.1 + core * 0.8;
    float fallbackEnergy = dot(texture(Sampler0, uv).rgb, vec3(0.333333));
    float lightEnergy = texture(Sampler2, lightMap).r;
    color *= 0.96 + fallbackEnergy * 0.04;
    color *= 0.94 + lightEnergy * 0.06;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.92) * ColorModulator.a);
}
