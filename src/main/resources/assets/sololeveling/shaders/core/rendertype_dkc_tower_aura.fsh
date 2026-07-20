#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);
    float a = hash(cell);
    float b = hash(cell + vec2(1.0, 0.0));
    float c = hash(cell + vec2(0.0, 1.0));
    float d = hash(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.56;
    for (int i = 0; i < 3; i++) {
        value += noise(p) * amplitude;
        p = p * 2.06 + vec2(7.13, 3.71);
        amplitude *= 0.47;
    }
    return value;
}

void main() {
    float time = GameTime * 6000.0;
    float angle = texCoord0.x * 6.2831853;
    float height = texCoord0.y;

    // Periodic cylindrical coordinates keep the wrap seam invisible while the
    // noise streams upward along the tower.
    vec2 flowCoord = vec2(cos(angle) * 2.8 + height * 1.3,
                          sin(angle) * 2.8 + height * 7.5 - time * 0.24);
    float flow = fbm(flowCoord);
    vec2 detailCoord = vec2(cos(angle * 2.0 + 0.7) * 4.6 + height * 2.1 - time * 0.035,
                            sin(angle * 2.0 + 0.7) * 4.6 + height * 15.0 - time * 0.47);
    float detail = fbm(detailCoord + flow * 1.65);

    float broadFlame = smoothstep(0.43, 0.82, flow + detail * 0.27);
    float tongueWave = sin(angle * 7.0 + height * 31.0 - time * 1.05 + flow * 7.0);
    float tongues = pow(max(0.0, tongueWave), 4.0) * smoothstep(0.38, 0.78, detail);
    float filament = smoothstep(0.055, 0.0, abs(detail - 0.55));
    filament *= 0.42 + 0.58 * (sin(height * 72.0 - time * 1.7 + flow * 9.0) * 0.5 + 0.5);

    float emberTexture = texture(Sampler0, vec2(fract(texCoord0.x * 2.0 - time * 0.012),
                                                 fract(height * 3.0 - time * 0.075))).a;
    float verticalFade = smoothstep(0.0, 0.035, height) * (1.0 - smoothstep(0.82, 1.0, height));
    float breakup = 0.70 + emberTexture * 0.30;
    float energy = clamp(broadFlame * 0.68 + tongues * 0.78 + filament * 0.62, 0.0, 1.35);
    float alpha = vertexColor.a * verticalFade * breakup
            * (broadFlame * 0.24 + tongues * 0.42 + filament * 0.36);

    if (alpha < 0.014)
        discard;

    vec3 bloodRed = vec3(0.42, 0.012, 0.003);
    vec3 furnaceRed = vec3(0.95, 0.075, 0.008);
    vec3 moltenOrange = vec3(1.0, 0.31, 0.015);
    vec3 gold = vec3(1.0, 0.76, 0.11);
    vec3 whiteHot = vec3(1.0, 0.95, 0.67);

    vec3 color = mix(bloodRed, furnaceRed, smoothstep(0.16, 0.62, energy));
    color = mix(color, moltenOrange, smoothstep(0.44, 0.88, energy));
    color = mix(color, gold, smoothstep(0.72, 1.05, energy));
    color = mix(color, whiteHot, smoothstep(1.0, 1.28, energy) * filament);
    color *= 1.04 + tongues * 0.34 + filament * 0.26;

    fragColor = vec4(color * ColorModulator.rgb, min(alpha * ColorModulator.a, 0.78));
}
