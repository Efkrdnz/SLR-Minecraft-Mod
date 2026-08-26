#version 150

uniform sampler2D Sampler2;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 localPos;
in vec2 lightMap;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(91.7, 447.3));
    p += dot(p, p + 26.6);
    return fract(p.x * p.y);
}

void main() {
    float radial = length(localPos);
    // vertexColor.b carries 0..1 expansion, so one shader serves both a static
    // Sanctuary and an expanding Purifying Wave.
    float expansion = clamp(vertexColor.b, 0.05, 1.0);

    float edge = smoothstep(expansion - 0.06, expansion, radial)
            * (1.0 - smoothstep(expansion, expansion + 0.03, radial));
    // A thin gold lip just inside the mint edge is what separates a Healer
    // field from a Barrier Mage construct at a glance.
    float lip = smoothstep(expansion - 0.10, expansion - 0.06, radial)
            * (1.0 - smoothstep(expansion - 0.06, expansion - 0.02, radial));

    float time = GameTime * 500.0;
    float petal = step(0.981, hash(floor(vec2(localPos.x * 22.0,
            localPos.y * 22.0 - time))));
    float interior = petal * (1.0 - smoothstep(0.0, expansion, radial));

    float alpha = (edge * 0.85 + lip * 0.5 + interior * 0.55) * vertexColor.a;
    // Interior stays readable: a filled dome would hide the fight inside it.
    alpha = min(alpha, edge > 0.02 ? 0.95 : 0.12);
    if (alpha < 0.012) {
        discard;
    }

    vec3 mint = vertexColor.rgb;
    vec3 gold = vec3(1.0, 0.92, 0.62);
    float light = texture(Sampler2, lightMap).r;
    vec3 color = mint * edge * 1.3 + gold * lip * 1.1 + mint * interior * 0.9;
    color *= 0.85 + light * 0.15;
    fragColor = vec4(color * ColorModulator.rgb, alpha);
}
