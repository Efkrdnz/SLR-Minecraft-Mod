#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
in vec2 localPos;
in vec2 lightMap;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
    vec2 uv = texCoord0;
    float time = GameTime * 900.0;
    vec2 p = uv * 2.0 - 1.0;
    p.x *= 1.08;
    float radius = length(p);
    float angle = atan(p.y, p.x);
    float grain = noise(vec2(cos(angle) * 5.0 + time * 0.05, sin(angle) * 5.0 - time * 0.03));
    float fineGrain = noise(p * 18.0 + vec2(time * 0.08, -time * 0.04));
    float tex = texture(Sampler0, vec2(fract(angle / 6.28318 + time * 0.025), fract(radius * 2.4 + fineGrain))).a;
    float broken = smoothstep(0.26, 0.95, grain + tex * 0.25);
    float core = 1.0 - smoothstep(0.02, 0.24, radius);
    float innerFlash = 1.0 - smoothstep(0.0, 0.09, radius);
    float ringRadius = 0.48 + grain * 0.045;
    float ring = (1.0 - smoothstep(0.025, 0.1, abs(radius - ringRadius))) * (0.62 + broken * 0.38);
    float outerDust = (1.0 - smoothstep(0.45, 0.95, radius)) * smoothstep(0.18, 0.42, radius) * (0.35 + fineGrain * 0.45);
    float spokeMask = pow(max(0.0, sin(angle * 9.0 + fineGrain * 4.0 - time * 0.18)), 10.0);
    float sparks = spokeMask * (1.0 - smoothstep(0.18, 0.86, radius)) * smoothstep(0.08, 0.25, radius);
    float vignette = 1.0 - smoothstep(0.86, 1.0, radius);
    float alpha = (core * 0.55 + innerFlash * 0.55 + ring * 0.64 + outerDust * 0.28 + sparks * 0.62) * vignette * vertexColor.a;
    if (alpha < 0.01) {
        discard;
    }
    vec3 inner = vec3(1.0, 0.96, 0.72);
    vec3 hot = vec3(1.0, 0.42, 0.12);
    vec3 amber = vec3(0.95, 0.16, 0.03);
    vec3 smoke = vec3(0.28, 0.05, 0.08);
    float light = texture(Sampler2, lightMap).r;
    vec3 color = smoke * outerDust * 0.55 + amber * ring * 1.05 + hot * sparks * 1.3 + inner * (core + innerFlash) * 1.75;
    color *= 0.9 + light * 0.1;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 1.0));
}
