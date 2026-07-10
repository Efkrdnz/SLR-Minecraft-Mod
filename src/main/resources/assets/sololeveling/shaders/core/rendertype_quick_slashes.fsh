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
    p = fract(p * vec2(269.3, 383.7));
    p += dot(p, p + 37.43);
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
        p = p * 2.13 + vec2(8.2, 13.4);
        amp *= 0.5;
    }
    return value;
}

void main() {
    vec2 uv = texCoord0;
    float time = GameTime * 1900.0;
    float along = uv.x;
    float acrossSigned = uv.y * 2.0 - 1.0;
    float bladeMass = pow(max(0.0, sin(along * 3.14159)), 1.12);
    float tipFade = smoothstep(0.0, 0.025, along) * (1.0 - smoothstep(0.972, 1.0, along));
    float n = fbm(vec2(along * 21.0 - time * 0.14 + localPos.x * 0.11, uv.y * 35.0 + time * 0.05));
    float tex = texture(Sampler0, vec2(fract(along * 2.7 + time * 0.043), fract(uv.y * 3.2 + n))).a;
    float curve = (n - 0.5) * 0.018;
    float dist = abs(acrossSigned - curve);

    float width = (0.075 + 0.66 * bladeMass) * (0.9 + n * 0.18);
    float blade = 1.0 - smoothstep(width * 0.25, width, dist);
    float core = 1.0 - smoothstep(0.012, width * 0.14, dist);
    float edge = max(0.0, (1.0 - smoothstep(width * 0.82, width, dist)) - (1.0 - smoothstep(width * 0.52, width * 0.68, dist)));
    float afterGlow = max(0.0, (1.0 - smoothstep(width * 1.0, width * 1.78, dist)) - blade);
    float broken = smoothstep(0.48, 0.98, n + tex * 0.32);
    float spark = pow(max(0.0, sin(along * 96.0 - time * 0.82 + n * 7.0)), 16.0) * blade;

    float alpha = (blade * 0.5 + core * 0.92 + edge * 0.34 + afterGlow * 0.16 + spark * 0.62) * tipFade * vertexColor.a;
    alpha *= 0.78 + broken * 0.22;
    if (alpha < 0.01) {
        discard;
    }

    vec3 tint = max(vertexColor.rgb, vec3(0.35, 0.56, 0.9));
    vec3 whiteHot = vec3(0.96, 1.0, 1.0);
    vec3 electricBlue = vec3(0.18, 0.72, 1.0);
    vec3 shadowViolet = vec3(0.48, 0.16, 1.0);
    float light = texture(Sampler2, lightMap).r;
    vec3 color = whiteHot * core * 2.65 + electricBlue * blade * 1.05 + shadowViolet * afterGlow * 0.95 + tint * edge * 1.2 + vec3(0.82, 0.95, 1.0) * spark * 1.45;
    color *= 0.92 + light * 0.08;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 1.0));
}
