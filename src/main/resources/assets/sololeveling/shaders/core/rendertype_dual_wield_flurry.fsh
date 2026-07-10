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
    p = fract(p * vec2(197.3, 421.7));
    p += dot(p, p + 29.17);
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
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amp;
        p = p * 2.11 + vec2(5.7, 9.3);
        amp *= 0.5;
    }
    return value;
}

void main() {
    vec2 uv = texCoord0;
    float time = GameTime * 1800.0;
    float along = uv.x;
    float across = abs(uv.y * 2.0 - 1.0);
    float taper = pow(max(0.0, sin(along * 3.14159)), 1.55);
    float n = fbm(vec2(along * 18.0 - time * 0.12 + localPos.x * 0.17, uv.y * 28.0 + time * 0.04));
    float tex = texture(Sampler0, vec2(fract(along * 2.5 + time * 0.035), fract(uv.y * 3.0 + n))).a;
    float width = 0.05 + 0.72 * taper * (0.86 + n * 0.18);
    float blade = 1.0 - smoothstep(width * 0.18, width, across);
    float core = 1.0 - smoothstep(0.015, width * 0.16, across);
    float rim = max(0.0, (1.0 - smoothstep(width * 0.82, width, across)) - (1.0 - smoothstep(width * 0.6, width * 0.72, across)));
    float broken = smoothstep(0.44, 0.98, n + tex * 0.36);
    float speedStreak = pow(max(0.0, sin(along * 82.0 - time * 0.75 + n * 6.0)), 12.0) * blade;
    float endFade = smoothstep(0.0, 0.06, along) * (1.0 - smoothstep(0.94, 1.0, along));
    float alpha = (blade * 0.52 + core * 0.88 + rim * 0.38 + speedStreak * 0.7) * endFade * vertexColor.a * (0.78 + broken * 0.22);
    if (alpha < 0.01) {
        discard;
    }
    vec3 whiteHot = vec3(1.0, 1.0, 0.96);
    vec3 coolEdge = vec3(0.56, 0.86, 1.0);
    vec3 paleViolet = vec3(0.78, 0.68, 1.0);
    float light = texture(Sampler2, lightMap).r;
    vec3 color = whiteHot * core * 2.75 + coolEdge * blade * 1.15 + paleViolet * rim * 0.8 + vec3(1.0) * speedStreak * 1.4;
    color *= 0.92 + light * 0.08;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 1.0));
}
