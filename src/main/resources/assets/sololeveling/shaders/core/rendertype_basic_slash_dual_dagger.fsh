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
    p = fract(p * vec2(313.4, 151.9));
    p += dot(p, p + 41.2);
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
    float time = GameTime * 1700.0;
    float along = uv.x;
    float y = uv.y * 2.0 - 1.0;
    float taper = pow(max(0.0, sin(along * 3.14159)), 1.65);
    float jitter = noise(vec2(along * 52.0 - time * 0.22, uv.y * 14.0));
    float width = 0.006 + 0.145 * taper * (0.9 + jitter * 0.2);
    float laneA = abs(y - 0.15 * sin(along * 3.14159));
    float laneB = abs(y + 0.15 * sin(along * 3.14159));
    float dist = min(laneA, laneB);
    float core = 1.0 - smoothstep(width * 0.08, width * 0.27, dist);
    float blade = 1.0 - smoothstep(width * 0.22, width, dist);
    float edge = max(0.0, (1.0 - smoothstep(width * 0.76, width, dist)) - (1.0 - smoothstep(width * 0.48, width * 0.66, dist)));
    float tex = texture(Sampler0, vec2(fract(along * 2.4 + time * 0.04), fract(uv.y * 2.0 + jitter))).a;
    float pulse = pow(max(0.0, sin(along * 74.0 - time * 0.55 + jitter * 5.0)), 10.0);
    float endFade = smoothstep(0.0, 0.05, along) * (1.0 - smoothstep(0.95, 1.0, along));
    float alpha = (blade * 0.62 + core * 0.82 + edge * 0.36 + pulse * 0.45 + tex * blade * 0.08) * endFade * vertexColor.a;
    if (alpha < 0.01) {
        discard;
    }
    float side = smoothstep(0.0, 1.0, uv.y);
    vec3 cyan = vec3(0.08, 0.94, 1.0);
    vec3 magenta = vec3(1.0, 0.05, 0.72);
    vec3 coreColor = vec3(1.0, 0.98, 1.0);
    vec3 edgeColor = mix(cyan, magenta, side);
    vec3 opposite = mix(magenta, cyan, side);
    float light = texture(Sampler2, lightMap).r;
    vec3 color = edgeColor * blade * 1.0 + opposite * edge * 1.25 + coreColor * core * 2.6 + vec3(0.65, 0.85, 1.0) * pulse;
    color *= 0.9 + light * 0.1;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 1.0));
}
