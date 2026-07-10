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
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
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
        p = p * 2.07 + vec2(7.1, 3.4);
        amp *= 0.5;
    }
    return value;
}

void main() {
    vec2 uv = texCoord0;
    float t = GameTime * 1200.0;
    float along = uv.x;
    float tipProfile = pow(max(0.0, sin(along * 3.14159)), 0.85);
    float lengthFade = smoothstep(0.0, 0.035, along) * (1.0 - smoothstep(0.965, 1.0, along));
    float cutCurve = 0.08 * sin(along * 3.14159) + 0.022 * sin(along * 14.0 + t * 0.08) * tipProfile;
    float warpedAcross = abs((uv.y * 2.0 - 1.0) - cutCurve);
    float edgeNoise = fbm(vec2(along * 9.0 - t * 0.06, uv.y * 18.0 + t * 0.02 + localPos.x * 0.13));
    float sparkNoise = fbm(vec2(along * 34.0 + t * 0.18, uv.y * 7.0));
    float texBreakup = texture(Sampler0, vec2(fract(along * 1.4 + t * 0.03), fract(uv.y * 2.0 + edgeNoise))).a;
    float bladeWidth = 0.012 + 0.2 * tipProfile;
    float chippedWidth = bladeWidth * (0.84 + edgeNoise * 0.12);
    float coreWidth = bladeWidth * (0.1 + 0.04 * sparkNoise);
    float blade = 1.0 - smoothstep(chippedWidth * 0.28, chippedWidth, warpedAcross);
    float core = 1.0 - smoothstep(coreWidth * 0.3, coreWidth, warpedAcross);
    float outerBand = 1.0 - smoothstep(chippedWidth * 0.91, chippedWidth * 1.0, warpedAcross);
    float innerBand = 1.0 - smoothstep(chippedWidth * 0.82, chippedWidth * 0.9, warpedAcross);
    float outline = max(0.0, outerBand - innerBand);
    float glow = (1.0 - smoothstep(bladeWidth * 0.38, bladeWidth * 0.82 + 0.006, warpedAcross)) * tipProfile * lengthFade;
    float streaks = pow(max(0.0, sin(along * 56.0 - t * 0.45)), 8.0) * (1.0 - smoothstep(coreWidth, bladeWidth * 0.45, warpedAcross));
    float brokenEdge = smoothstep(0.36, 0.95, edgeNoise + sparkNoise * 0.35 + texBreakup * 0.2);
    float alpha = (blade * (0.78 + brokenEdge * 0.14) + outline * 0.18 + glow * 0.055 + streaks * 0.68) * lengthFade * vertexColor.a;
    if (alpha < 0.01) {
        discard;
    }
    vec3 base = max(vertexColor.rgb, vec3(0.08));
    vec3 coreColor = vec3(1.0, 0.96, 0.76);
    vec3 edgeColor = mix(vec3(1.0, 0.05, 0.02), vec3(0.85, 0.05, 1.0), smoothstep(0.15, 0.9, along));
    vec3 outerGlow = mix(vec3(0.9, 0.0, 0.05), vec3(0.08, 0.55, 1.0), edgeNoise);
    float lightBoost = texture(Sampler2, lightMap).r;
    vec3 color = base * 0.38 + edgeColor * outline * 0.55 + outerGlow * glow * 0.22 + coreColor * core * 2.85 + vec3(1.0, 0.38, 0.1) * streaks;
    color *= 0.9 + lightBoost * 0.1;
    color *= ColorModulator.rgb;
    fragColor = vec4(color, min(alpha, 1.0));
}
