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
    p = fract(p * vec2(173.9, 409.1));
    p += dot(p, p + 31.37);
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

float fbm(vec2 p) {
    float value = 0.0;
    float amp = 0.52;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amp;
        p = p * 2.13 + vec2(7.4, 19.2);
        amp *= 0.5;
    }
    return value;
}

void main() {
    vec2 uv = texCoord0;
    float time = GameTime * 1800.0;
    float along = uv.x;
    float acrossSigned = uv.y * 2.0 - 1.0;
    float mass = pow(max(0.0, sin(along * 3.14159)), 0.85);
    float tipFade = smoothstep(0.0, 0.025, along) * (1.0 - smoothstep(0.965, 1.0, along));
    float voidNoise = fbm(vec2(along * 19.0 - time * 0.15 + localPos.x * 0.11, uv.y * 23.0 + time * 0.05));
    float tex = texture(Sampler0, vec2(fract(along * 2.4 - time * 0.035), fract(uv.y * 2.5 + voidNoise))).a;
    float curve = (voidNoise - 0.5) * 0.045 * mass;
    float dist = abs(acrossSigned - curve);

    float width = (0.08 + 0.33 * mass) * (0.92 + voidNoise * 0.16);
    float blade = 1.0 - smoothstep(width * 0.42, width, dist);
    float core = 1.0 - smoothstep(0.010, width * 0.13, dist);
    float outerSmoke = max(0.0, (1.0 - smoothstep(width * 1.0, width * 2.45, dist)) - blade);
    float corruptedEdge = max(0.0, (1.0 - smoothstep(width * 0.78, width, dist)) - (1.0 - smoothstep(width * 0.45, width * 0.62, dist)));
    float tear = smoothstep(0.58, 1.0, voidNoise + tex * 0.45);
    float pulse = pow(max(0.0, sin(along * 64.0 - time * 0.7 + voidNoise * 8.0)), 10.0) * blade;

    float alpha = (blade * 0.58 + core * 0.78 + corruptedEdge * 0.32 + outerSmoke * 0.20 + pulse * 0.46) * tipFade * vertexColor.a;
    alpha *= 0.82 + tear * 0.22;
    if (alpha < 0.01) {
        discard;
    }

    vec3 blackViolet = vec3(0.08, 0.0, 0.14);
    vec3 deepPurple = vec3(0.34, 0.02, 0.62);
    vec3 curseBlue = vec3(0.12, 0.72, 1.0);
    vec3 paleCore = vec3(0.82, 0.94, 1.0);
    float light = texture(Sampler2, lightMap).r;
    vec3 color = blackViolet * outerSmoke * 1.45 + deepPurple * blade * 1.25 + curseBlue * corruptedEdge * 1.55 + paleCore * core * 2.6 + vec3(0.5, 0.06, 0.95) * pulse;
    color *= 0.94 + light * 0.06;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 1.0));
}
