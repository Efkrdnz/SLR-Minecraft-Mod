#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
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
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float weight = 0.55;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * weight;
        p = p * 2.02 + vec2(5.1, 9.7);
        weight *= 0.5;
    }
    return value;
}

void main() {
    // The integer part of the U coordinate is a per-puff seed so every puff
    // dissolves through a different slice of the noise field.
    float seed = floor(texCoord0.x);
    vec2 uv = vec2(fract(texCoord0.x), texCoord0.y);

    vec2 p = uv * 2.0 - 1.0;
    float r = length(p);
    float time = GameTime * 1600.0;

    vec2 flow = uv * 2.4 + vec2(seed * 4.7, seed * 2.3 - time * 0.45);
    float broad = fbm(flow);
    float detail = fbm(flow * 2.4 - vec2(0.0, time * 0.7));

    // A soft disc whose edge is pushed around by noise so it billows.
    float edge = 0.98 + (broad - 0.5) * 0.55;
    float body = smoothstep(edge, 0.05, r);

    // Erode the interior into drifting wisps rather than a solid blob.
    float erode = smoothstep(0.12, 0.62, broad * 0.6 + detail * 0.5 + body * 0.35);

    float alpha = body * erode * vertexColor.a;

    if (alpha < 0.004) {
        discard;
    }

    float core = smoothstep(0.55, 0.0, r);
    vec3 color = vertexColor.rgb * (0.80 + core * 0.7 + detail * 0.15);
    color *= 0.96 + texture(Sampler2, lightMap).r * 0.04;

    fragColor = vec4(color * ColorModulator.rgb, clamp(alpha, 0.0, 1.0) * ColorModulator.a);
}
