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
    p = fract(p * vec2(311.7, 97.3));
    p += dot(p, p + 41.91);
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
    float amp = 0.55;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amp;
        p = p * 2.07 + vec2(11.1, 3.7);
        amp *= 0.5;
    }
    return value;
}

void main() {
    vec2 uv = texCoord0;
    float time = GameTime * 1600.0;
    float along = uv.x;
    float acrossSigned = uv.y * 2.0 - 1.0;
    float centerMass = pow(max(0.0, sin(along * 3.14159)), 0.75);
    float tipFade = smoothstep(0.0, 0.022, along) * (1.0 - smoothstep(0.978, 1.0, along));
    float jag = fbm(vec2(along * 24.0 - time * 0.10 + localPos.x * 0.08, uv.y * 34.0 + time * 0.035));
    float tex = texture(Sampler0, vec2(fract(along * 2.2 + time * 0.026), fract(uv.y * 2.8 + jag))).a;
    float curve = (jag - 0.5) * 0.012;
    float dist = abs(acrossSigned - curve);

    float width = (0.245 + 0.055 * centerMass) * (0.96 + jag * 0.07);
    float blade = 1.0 - smoothstep(width * 0.54, width, dist);
    float core = 1.0 - smoothstep(0.018, width * 0.18, dist);
    float emberEdge = max(0.0, (1.0 - smoothstep(width * 0.82, width, dist)) - (1.0 - smoothstep(width * 0.55, width * 0.7, dist)));
    float violetWake = max(0.0, (1.0 - smoothstep(width * 1.05, width * 1.75, dist)) - blade);
    float brokenEdge = smoothstep(0.52, 1.0, jag + tex * 0.35);
    float spark = pow(max(0.0, sin(along * 88.0 - time * 0.72 + jag * 5.0)), 18.0) * blade;

    float alpha = (blade * 0.58 + core * 0.78 + emberEdge * 0.20 + violetWake * 0.10 + spark * 0.46) * tipFade * vertexColor.a;
    alpha *= 0.90 + brokenEdge * 0.10;
    if (alpha < 0.01) {
        discard;
    }

    vec3 whiteHot = vec3(1.0, 0.98, 0.86);
    vec3 bloodRed = vec3(1.0, 0.08, 0.02);
    vec3 moltenOrange = vec3(1.0, 0.42, 0.08);
    vec3 violet = vec3(0.52, 0.12, 1.0);
    float light = texture(Sampler2, lightMap).r;
    vec3 color = bloodRed * blade * 1.05 + moltenOrange * emberEdge * 1.45 + violet * violetWake * 0.8 + whiteHot * core * 2.65 + vec3(1.0, 0.78, 0.42) * spark * 1.35;
    color *= 0.94 + light * 0.06;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 1.0));
}
