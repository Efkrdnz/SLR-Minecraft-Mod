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
    p = fract(p * vec2(241.2, 83.7));
    p += dot(p, p + 37.4);
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
    float time = GameTime * 1100.0;
    float along = uv.x;
    float profile = pow(max(0.0, sin(along * 3.14159)), 1.45);
    float tipFade = smoothstep(0.0, 0.035, along) * (1.0 - smoothstep(0.965, 1.0, along));
    float curve = 0.07 * sin(along * 3.14159) + 0.018 * sin(along * 11.0 + time * 0.05) * profile;
    float dist = abs((uv.y * 2.0 - 1.0) - curve);
    float grain = noise(vec2(along * 22.0 - time * 0.06, uv.y * 18.0));
    float tex = texture(Sampler0, vec2(fract(along * 1.25 + time * 0.02), fract(uv.y * 1.7 + grain))).a;
    float width = 0.018 + 0.24 * profile;
    float coreWidth = width * 0.14;
    float blade = 1.0 - smoothstep(width * 0.25, width, dist);
    float core = 1.0 - smoothstep(coreWidth * 0.25, coreWidth, dist);
    float blueRim = max(0.0, (1.0 - smoothstep(width * 0.72, width, dist)) - (1.0 - smoothstep(width * 0.5, width * 0.66, dist)));
    float glint = pow(max(0.0, sin(along * 64.0 - time * 0.45)), 12.0) * (1.0 - smoothstep(width * 0.28, width * 0.55, dist));
    float edgeBreak = smoothstep(0.62, 1.0, grain + tex * 0.35);
    float alpha = (blade * (0.58 + edgeBreak * 0.12) + core * 0.64 + blueRim * 0.36 + glint * 0.7) * tipFade * vertexColor.a;
    if (alpha < 0.01) {
        discard;
    }
    vec3 steel = vec3(0.62, 0.86, 1.0);
    vec3 coreColor = vec3(1.0, 1.0, 0.96);
    vec3 rim = vec3(0.12, 0.58, 1.0);
    vec3 violet = vec3(0.42, 0.16, 0.95);
    float light = texture(Sampler2, lightMap).r;
    vec3 color = steel * blade * 0.75 + rim * blueRim * 1.25 + violet * edgeBreak * 0.16 + coreColor * core * 2.4 + vec3(0.9, 0.98, 1.0) * glint;
    color *= 0.92 + light * 0.08;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 1.0));
}
