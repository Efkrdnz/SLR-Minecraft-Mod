#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
in vec2 localPos;
in vec2 lightMap;
in vec3 viewNormal;
in vec3 viewPos;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(91.7, 447.3));
    p += dot(p, p + 26.6);
    return fract(p.x * p.y);
}

void main() {
    vec4 base = texture(Sampler0, texCoord0);
    if (base.a < 0.02) {
        discard;
    }

    // Cell noise gives glass-like shards rather than a soft dissolve, which is
    // what separates an attackable Shadow Feint from the Stealth decoy.
    vec2 cell = floor(texCoord0 * 9.0);
    float shard = hash(cell);
    float time = GameTime * 1200.0;

    // Shards breathe out of phase so the body reads as brittle, not pulsing.
    float phase = fract(shard + time * 0.05);
    float loosen = smoothstep(0.55, 1.0, phase);

    vec3 viewDir = normalize(-viewPos);
    float facing = clamp(abs(dot(normalize(viewNormal), viewDir)), 0.0, 1.0);
    float rim = 1.0 - smoothstep(0.10, 0.80, facing);

    float seam = smoothstep(0.42, 0.5, abs(fract(texCoord0.x * 9.0) - 0.5))
            + smoothstep(0.42, 0.5, abs(fract(texCoord0.y * 9.0) - 0.5));
    seam = clamp(seam, 0.0, 1.0);

    float alpha = base.a * vertexColor.a
            * (0.42 + rim * 0.4 + seam * 0.22) * (1.0 - loosen * 0.35);
    if (alpha < 0.015) {
        discard;
    }

    vec3 ink = vec3(0.06, 0.05, 0.13);
    vec3 violet = vec3(0.52, 0.38, 1.0);
    vec3 silver = vec3(0.88, 0.90, 1.0);
    float light = texture(Sampler2, lightMap).r;
    vec3 color = ink * 0.8
            + violet * (0.55 + rim * 0.7)
            + silver * seam * 0.65
            + silver * pow(rim, 4.0) * 0.5;
    color *= 0.85 + light * 0.15;
    fragColor = vec4(color * ColorModulator.rgb * vertexColor.rgb,
            min(alpha, 0.88));
}
