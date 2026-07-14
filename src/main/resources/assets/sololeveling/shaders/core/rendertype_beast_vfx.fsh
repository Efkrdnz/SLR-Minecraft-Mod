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
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0)), f.x), f.y);
}

float lineMask(float distanceToLine, float width) {
    return 1.0 - smoothstep(width * 0.42, width, distanceToLine);
}

float segmentDistance(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

void main() {
    float kind = floor(texCoord0.x);
    vec2 uv = vec2(fract(texCoord0.x), texCoord0.y);
    vec2 p = uv * 2.0 - 1.0;
    float time = GameTime * 2400.0;
    float grain = noise(vec2(uv.x * 17.0 - time * 0.48, uv.y * 13.0 + time * 0.21));
    float fine = noise(vec2(uv.x * 39.0 + time * 0.31, uv.y * 31.0 - time * 0.56));
    float edge = smoothstep(0.0, 0.035, uv.x) * smoothstep(0.0, 0.035, 1.0 - uv.x)
               * smoothstep(0.0, 0.035, uv.y) * smoothstep(0.0, 0.035, 1.0 - uv.y);
    float alpha = 0.0;
    float core = 0.0;

    if (kind < 1.0) {
        float taper = pow(max(0.0, sin(uv.x * 3.14159265)), 0.54);
        float cells = uv.x * 7.0;
        float cell = floor(cells);
        float local = fract(cells);
        float from = hash(vec2(cell, 12.7)) * 2.0 - 1.0;
        float to = hash(vec2(cell + 1.0, 12.7)) * 2.0 - 1.0;
        float fracture = mix(from, to, local) * 0.085 * taper;
        float cut = p.y - p.x * 0.055 - fracture;
        float blade = lineMask(abs(cut), 0.032 * taper + 0.0025);
        float bloom = lineMask(abs(cut), 0.105 * taper + 0.008) * 0.23;
        float torn = 0.72 + 0.28 * step(0.28, hash(vec2(floor(uv.x * 23.0), 4.4)) + fine * 0.12);
        alpha = edge * taper * (blade * torn + bloom) * (0.82 + grain * 0.18);
        core = lineMask(abs(cut), 0.009 * taper + 0.0014);
    } else if (kind < 2.0) {
        float taper = pow(max(0.0, sin(uv.x * 3.14159265)), 0.43);
        float serration = (floor(fract(uv.x * 10.0) * 2.0) - 0.5) * 0.036 * taper;
        float cut = p.y - p.x * 0.08 - serration;
        float wound = lineMask(abs(cut), 0.045 * taper + 0.003);
        float afterImage = lineMask(abs(cut), 0.14 * taper + 0.012) * 0.19;
        alpha = edge * taper * (wound + afterImage) * (0.76 + fine * 0.24);
        core = lineMask(abs(cut), 0.012 * taper + 0.0015);
    } else if (kind < 3.0) {
        float tail = pow(max(0.0, 1.0 - uv.y), 0.62);
        float head = smoothstep(0.0, 0.16, uv.y);
        float width = (0.12 + tail * 0.42) * (0.78 + grain * 0.22);
        float stream = 1.0 - smoothstep(width * 0.35, width, abs(p.x));
        float split = lineMask(abs(abs(p.x) - width * 0.72), 0.055 + tail * 0.03);
        float breaks = 0.5 + 0.5 * smoothstep(0.35, 0.78,
                noise(vec2(uv.y * 19.0 + time * 1.35, floor((p.x + 1.0) * 6.0))));
        alpha = edge * head * tail * (stream * (0.22 + breaks * 0.48) + split * 0.5);
        core = stream * tail * smoothstep(0.35, 0.95, breaks) * 0.55;
    } else if (kind < 4.0) {
        float leftFang = segmentDistance(p, vec2(-0.68, 0.42), vec2(-0.12, -0.72));
        float rightFang = segmentDistance(p, vec2(0.68, 0.42), vec2(0.12, -0.72));
        float brow = segmentDistance(p, vec2(-0.68, 0.42), vec2(0.0, 0.72));
        brow = min(brow, segmentDistance(p, vec2(0.0, 0.72), vec2(0.68, 0.42)));
        float mark = lineMask(min(min(leftFang, rightFang), brow), 0.09);
        float inner = lineMask(min(leftFang, rightFang), 0.032);
        float pulse = 0.76 + 0.24 * sin(time * 2.2 + grain * 2.0);
        alpha = edge * mark * pulse;
        core = inner;
    } else {
        float taper = pow(max(0.0, sin(uv.x * 3.14159265)), 0.5);
        float cut = p.y - p.x * 0.04;
        float blade = lineMask(abs(cut), 0.05 * taper + 0.004);
        float pressure = lineMask(abs(cut), 0.17 * taper + 0.014) * 0.22;
        float flash = 0.78 + 0.22 * sin(time * 3.4 + fine * 3.0);
        alpha = edge * taper * (blade + pressure) * flash;
        core = lineMask(abs(cut), 0.013 * taper + 0.0018);
    }

    alpha *= vertexColor.a;
    if (alpha < 0.01) discard;

    vec3 base = max(vertexColor.rgb, vec3(0.015));
    float luminance = dot(base, vec3(0.299, 0.587, 0.114));
    vec3 saturated = mix(vec3(luminance), base, 1.34);
    vec3 bloodHot = vec3(1.0, 0.32, 0.25);
    vec3 ivory = vec3(1.0, 0.88, 0.72);
    vec3 hot = kind >= 3.0 ? ivory : bloodHot;
    float energy = clamp(core * 1.85 + fine * 0.11, 0.0, 1.0);
    vec3 color = mix(saturated * (0.92 + grain * 0.2), hot * 1.42, energy);
    float fallback = dot(texture(Sampler0, uv).rgb, vec3(0.333333));
    float light = texture(Sampler2, lightMap).r;
    color *= 0.98 + fallback * 0.02;
    color *= 0.96 + light * 0.04;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.95) * ColorModulator.a);
}
