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
    p = fract(p * vec2(91.7, 447.3));
    p += dot(p, p + 26.6);
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
    float time = GameTime * 1500.0;
    float along = uv.x;
    float taper = pow(max(0.0, sin(along * 3.14159)), 1.9);
    float curve = 0.035 * sin(along * 3.14159) + 0.016 * sin(along * 19.0 - time * 0.08);
    float dist = abs((uv.y * 2.0 - 1.0) - curve);
    float cutNoise = noise(vec2(along * 46.0 - time * 0.18, uv.y * 9.0));
    float tex = texture(Sampler0, vec2(fract(along * 2.2 + time * 0.03), fract(uv.y * 1.3 + cutNoise))).a;
    float width = 0.008 + 0.16 * taper * (0.92 + cutNoise * 0.16);
    float core = 1.0 - smoothstep(width * 0.08, width * 0.28, dist);
    float blade = 1.0 - smoothstep(width * 0.2, width, dist);
    float serration = smoothstep(0.55, 0.98, cutNoise + tex * 0.45) * (1.0 - smoothstep(width * 0.48, width, dist));
    float edge = max(0.0, (1.0 - smoothstep(width * 0.76, width, dist)) - (1.0 - smoothstep(width * 0.52, width * 0.68, dist)));
    float endFade = smoothstep(0.0, 0.055, along) * (1.0 - smoothstep(0.94, 1.0, along));
    float alpha = (blade * 0.7 + core * 0.72 + edge * 0.32 + serration * 0.38) * endFade * vertexColor.a;
    if (alpha < 0.01) {
        discard;
    }
    vec3 coreColor = vec3(1.0, 0.96, 0.98);
    vec3 violet = vec3(0.72, 0.12, 1.0);
    vec3 red = vec3(1.0, 0.04, 0.08);
    vec3 ink = vec3(0.08, 0.01, 0.12);
    float light = texture(Sampler2, lightMap).r;
    vec3 color = ink * blade * 0.35 + violet * blade * 0.9 + red * edge * 1.15 + coreColor * core * 2.7 + vec3(1.0, 0.18, 0.35) * serration;
    color *= 0.9 + light * 0.1;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 1.0));
}
