#version 150

uniform float GameTime;
uniform vec2 MousePos;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float hash1(float x) {
    return fract(sin(x * 91.3458) * 47453.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        v += amp * noise(p);
        p *= 2.04;
        amp *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = texCoord;
    float t = GameTime * 24000.0;
    vec2 mouse = MousePos;

    vec3 top = vec3(0.045, 0.028, 0.115);
    vec3 bot = vec3(0.006, 0.007, 0.020);
    vec3 col = mix(bot, top, uv.y);

    vec2 flow = vec2(t * 0.0017, -t * 0.0010);
    float n = fbm(uv * 3.4 + flow);
    n += 0.55 * fbm(uv * 7.0 - flow * 1.8);
    col += vec3(0.18, 0.08, 0.48) * pow(clamp(n * 0.72, 0.0, 1.0), 2.0);
    col += vec3(0.02, 0.22, 0.42) * pow(clamp(n * 0.55, 0.0, 1.0), 2.6);

    float grain = hash(floor(uv * vec2(260.0, 180.0)) + floor(vec2(t * 1.4)));
    col += vec3(0.08, 0.10, 0.16) * (grain - 0.5) * 0.20;

    float scan = sin(uv.y * 720.0 + t * 0.16) * 0.5 + 0.5;
    col += vec3(0.04, 0.14, 0.20) * scan * 0.11;

    float md = distance(uv, mouse);
    float local = exp(-md * 8.0);
    col += vec3(0.42, 0.14, 0.95) * local * 0.36;
    col += vec3(0.08, 0.50, 0.95) * local * 0.20;

    float cursorBand = smoothstep(0.18, 0.0, abs(uv.y - mouse.y));
    float line = floor(uv.y * 82.0);
    float gate = hash1(line + floor(t * 2.3));
    if (gate > 0.72) {
        float slice = smoothstep(0.52, 0.0, abs(fract(uv.y * 82.0) - 0.5));
        float nearMouse = cursorBand * smoothstep(0.36, 0.0, abs(uv.x - mouse.x));
        float jitter = (hash1(line * 4.7 + floor(t * 8.0)) - 0.5) * 0.18 * nearMouse;
        float shard = step(0.42, hash(vec2(floor((uv.x + jitter) * 32.0), line + floor(t * 3.0))));
        col.r += slice * nearMouse * shard * 0.30;
        col.g += slice * nearMouse * shard * 0.10;
        col.b += slice * nearMouse * shard * 0.42;
        col += vec3(0.16, 0.04, 0.25) * slice * nearMouse;
    }

    float cross = smoothstep(0.010, 0.0, abs(uv.x - mouse.x)) + smoothstep(0.010, 0.0, abs(uv.y - mouse.y));
    col += vec3(0.26, 0.70, 1.0) * cross * exp(-md * 4.0) * 0.22;

    float randomLine = step(0.988, hash(vec2(floor(uv.y * 120.0), floor(t * 0.75))));
    float randomShape = smoothstep(0.8, 0.0, abs(fract(uv.y * 120.0) - 0.5));
    col += vec3(0.10, 0.36, 0.62) * randomLine * randomShape * 0.38;

    float pulse = 0.5 + 0.5 * sin(t * 0.045);
    float centre = smoothstep(0.82, 0.15, length((uv - 0.5) * vec2(1.25, 1.0)));
    col += vec3(0.10, 0.03, 0.22) * centre * (0.40 + 0.25 * pulse);

    float vig = smoothstep(1.10, 0.30, length((uv - 0.5) * vec2(1.12, 1.0)));
    col *= vig;

    fragColor = vec4(col, 0.95);
}
