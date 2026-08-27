#version 150

uniform sampler2D Sampler2;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 localPos;
in vec2 lightMap;

out vec4 fragColor;

void main() {
    float radial = length(localPos);
    // vertexColor.b carries stage/5. Ring count equals the output stage, so a
    // player can read their power level by counting rather than by brightness.
    float stageCount = clamp(vertexColor.b * 5.0, 1.0, 5.0);

    float rings = 0.0;
    for (int index = 1; index <= 5; index++) {
        // Not "active": that is a reserved word in GLSL. NVIDIA's compiler
        // accepts it as an identifier, AMD's and Intel's reject the shader, and
        // a core shader that fails to compile takes the whole render pipeline
        // with it -- the game loads, plays sound and accepts clicks, and draws
        // nothing at all.
        float ringOn = step(float(index), stageCount + 0.001);
        float target = 0.20 + float(index) * 0.15;
        rings += ringOn
                * smoothstep(target - 0.035, target, radial)
                * (1.0 - smoothstep(target, target + 0.035, radial));
    }
    rings = clamp(rings, 0.0, 1.0);

    // Counter-rotating rune teeth on the outer edge.
    float angle = atan(localPos.y, localPos.x) + GameTime * 900.0;
    float teeth = smoothstep(0.55, 0.95, abs(cos(angle * 6.0)));

    float alpha = clamp(rings + teeth * rings * 0.6, 0.0, 1.0) * vertexColor.a;
    if (alpha < 0.012) {
        discard;
    }

    float light = texture(Sampler2, lightMap).r;
    vec3 base = vertexColor.rgb;
    vec3 highlight = vec3(1.0, 0.98, 0.86);
    vec3 color = base * rings * 1.35 + highlight * teeth * rings * 0.7;
    color *= 0.85 + light * 0.15;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.92));
}
