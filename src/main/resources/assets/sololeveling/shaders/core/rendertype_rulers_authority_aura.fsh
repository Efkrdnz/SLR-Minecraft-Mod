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
    p += dot(p, p + 34.53);
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
    float amplitude = 0.54;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amplitude;
        p = p * 2.07 + vec2(7.3, 11.9);
        amplitude *= 0.48;
    }
    return value;
}

void main() {
    vec2 uv = texCoord0;
    float time = GameTime * 1750.0;
    float vertical = 1.0 - uv.y;
    float flowNoise = fbm(vec2(uv.x * 7.0 + time * 0.035, vertical * 5.6 - time * 0.19));
    float detailNoise = fbm(vec2(uv.x * 15.0 - time * 0.08 + flowNoise * 1.8,
                                 vertical * 12.0 - time * 0.34));
    float curled = sin(uv.x * 34.0 + vertical * 19.0 - time * 0.7 + flowNoise * 8.0) * 0.5 + 0.5;
    float fine = pow(max(0.0, sin(uv.x * 73.0 - vertical * 31.0 + time * 0.95)), 9.0);
    float textureCloud = texture(Sampler0, vec2(fract(uv.x * 2.0 + time * 0.025),
                                                 fract(vertical * 2.7 - time * 0.09))).a;

    float openCloud = smoothstep(0.48, 0.78, flowNoise + detailNoise * 0.28);
    float stream = smoothstep(0.60, 0.92, curled * 0.58 + detailNoise * 0.62);
    float verticalFade = smoothstep(0.0, 0.09, vertical) * (1.0 - smoothstep(0.88, 1.0, vertical));
    float brokenEdges = 0.62 + textureCloud * 0.38;
    float alpha = (openCloud * 0.46 + stream * 0.35 + fine * 0.22) * verticalFade * brokenEdges;
    alpha *= vertexColor.a;

    if (alpha < 0.012) {
        discard;
    }

    vec3 deepBlue = vec3(0.025, 0.20, 0.72);
    vec3 electricBlue = vec3(0.05, 0.64, 1.0);
    vec3 cyan = vec3(0.36, 0.91, 1.0);
    vec3 whiteCore = vec3(0.86, 0.98, 1.0);
    float glow = smoothstep(0.62, 0.96, detailNoise + fine * 0.35);
    vec3 color = mix(deepBlue, electricBlue, flowNoise);
    color = mix(color, cyan, stream * 0.72);
    color = mix(color, whiteCore, glow * 0.62);
    color *= 1.12 + fine * 0.48;
    color *= max(vertexColor.rgb, vec3(0.36, 0.70, 1.0));
    color *= 0.96 + texture(Sampler2, lightMap).r * 0.04;

    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.72));
}
