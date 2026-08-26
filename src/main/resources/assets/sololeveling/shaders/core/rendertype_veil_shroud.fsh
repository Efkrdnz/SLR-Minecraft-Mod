#version 150

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
    // Rim term: the shell is brightest where the surface turns away from the
    // camera, so the silhouette reads without filling the body in.
    vec3 viewDir = normalize(-viewPos);
    float facing = clamp(abs(dot(normalize(viewNormal), viewDir)), 0.0, 1.0);
    float rim = 1.0 - smoothstep(0.05, 0.72, facing);
    float interior = smoothstep(0.25, 0.95, facing);

    float time = GameTime * 900.0;
    float wisp = noise(vec2(texCoord0.x * 7.0, texCoord0.y * 3.5 - time * 0.05));
    float drift = noise(vec2(texCoord0.x * 3.0 + time * 0.02, texCoord0.y * 2.0));

    // vertexColor.a carries the owner/observer opacity split.
    float alpha = (rim * 0.62 + interior * 0.10 + wisp * rim * 0.28
            + drift * 0.05) * vertexColor.a;
    if (alpha < 0.012) {
        discard;
    }

    vec3 ink = vec3(0.05, 0.04, 0.11);
    vec3 violet = vec3(0.55, 0.42, 1.0);
    vec3 silver = vec3(0.86, 0.88, 1.0);
    float light = texture(Sampler2, lightMap).r;
    vec3 color = ink * interior * 0.9
            + violet * rim * 1.15
            + silver * pow(rim, 3.0) * 0.9
            + violet * wisp * rim * 0.5;
    color *= 0.85 + light * 0.15;
    // Capped below 1.0: a concealment shell must never read as a solid body.
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.92));
}
