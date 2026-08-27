#version 150

uniform sampler2D Sampler2;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
in vec2 localPos;
in vec2 lightMap;

out vec4 fragColor;

void main() {
    float along = texCoord0.x;
    // A slight bow keeps this from reading as a laser, which is the wrong
    // genre for healing.
    float bow = 0.08 * sin(along * 3.14159);
    float dist = abs((texCoord0.y * 2.0 - 1.0) - bow);
    float taper = pow(max(0.0, sin(along * 3.14159)), 0.55);
    float width = 0.10 + 0.16 * taper;

    float body = 1.0 - smoothstep(width * 0.35, width, dist);
    float core = 1.0 - smoothstep(0.0, width * 0.22, dist);

    // Motes travel toward the target so the direction of care is unambiguous.
    float time = GameTime * 2200.0;
    float flow = fract(along * 3.0 - time);
    float mote = smoothstep(0.86, 1.0, flow) * body;

    float alpha = (body * 0.42 + core * 0.75 + mote * 0.5) * taper * vertexColor.a;
    if (alpha < 0.012) {
        discard;
    }

    vec3 coreColor = vec3(0.86, 1.0, 0.90);
    vec3 bodyColor = vec3(0.34, 0.88, 0.56);
    float light = texture(Sampler2, lightMap).r;
    vec3 color = bodyColor * body * 0.9
            + coreColor * core * 1.6
            + coreColor * mote * 0.8;
    color *= 0.85 + light * 0.15;
    fragColor = vec4(color * ColorModulator.rgb * vertexColor.rgb, min(alpha, 0.95));
}
