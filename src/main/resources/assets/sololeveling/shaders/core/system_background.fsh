#version 150

uniform float GameTime;
uniform vec2 MousePos; // panel-local, 0..1, y measured from the top
uniform float MouseGlitch; // 1 = compact System GUI data-tear, 0 = soft cursor wave

in vec2 texCoord;
out vec4 fragColor;

// ---- cheap value noise -----------------------------------------------------
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
        p *= 2.02;
        amp *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = texCoord;
    // GameTime is a 0..1 fraction over 24000 ticks; scale to raw ticks.
    float t = GameTime * 24000.0;

    // mouse in the same space as uv (uv.y is 0 at top here)
    vec2 mouse = vec2(MousePos.x, MousePos.y);

    // base vertical gradient: deep indigo void -> near black
    vec3 top = vec3(0.025, 0.045, 0.105);
    vec3 bot = vec3(0.002, 0.004, 0.014);
    vec3 col = mix(bot, top, uv.y);

    // slow drifting energy nebula (Solo Leveling azure)
    vec2 flow = vec2(t * 0.0020, t * 0.0011);
    float n = fbm(uv * 3.0 + flow);
    n += 0.5 * fbm(uv * 6.0 - flow * 1.7);
    col += vec3(0.035, 0.25, 0.55) * pow(clamp(n * 0.65, 0.0, 1.0), 2.2) * 0.65;

    // faint mana grid scanlines
    float scan = sin((uv.y * 620.0) + t * 0.12) * 0.5 + 0.5;
    col += vec3(0.015, 0.07, 0.13) * scan * 0.12;

    // ---- subtle descending static -----------------------------------------
    // fine animated grain everywhere (low intensity so text stays readable)
    float grain = hash(floor(uv * vec2(180.0, 320.0)) + floor(vec2(t * 0.9)));
    col += vec3(0.035, 0.075, 0.10) * (grain - 0.5) * 0.14;

    // a soft static band scrolling top -> bottom
    float bandCenter = fract(t * 0.010);
    float bandDist = abs(uv.y - bandCenter);
    float band = smoothstep(0.06, 0.0, bandDist);
    float bandNoise = hash(vec2(floor(uv.x * 220.0), floor(t * 3.0)));
    col += vec3(0.07, 0.16, 0.24) * band * bandNoise * 0.28;
    // tiny horizontal RGB split inside the band
    float split = band * 0.004;
    col.r += band * 0.06 * hash(vec2(floor((uv.x + split) * 200.0), floor(t)));
    col.b += band * 0.06 * hash(vec2(floor((uv.x - split) * 200.0), floor(t)));

    // occasional thin glitch line
    float lineId = floor(uv.y * 90.0);
    float lineGate = hash1(lineId + floor(t * 0.5));
    if (lineGate > 0.985) {
        float jitter = (hash1(lineId + floor(t)) - 0.5) * 0.05;
        float lit = smoothstep(0.5, 0.0, abs(fract(uv.y * 90.0) - 0.5));
        col += vec3(0.14, 0.32, 0.45) * lit * 0.42;
        col.r += lit * 0.12 * step(0.0, jitter);
        col.b += lit * 0.12 * step(jitter, 0.0);
    }

    vec2 delta = uv - mouse;
    float md = distance(uv, mouse);
    if (MouseGlitch > 0.5) {
        // ---- slim cursor glitch, tuned closer to the Shadow UI ------------
        float local = exp(-length(delta * vec2(3.0, 6.2)) * 5.4);
        float xGate = smoothstep(0.145, 0.0, abs(delta.x));
        float yGate = smoothstep(0.075, 0.0, abs(delta.y));

        float line = floor((uv.y - mouse.y) * 92.0);
        float lineNoise = hash1(line + floor(t * 3.2));
        float slice = smoothstep(0.50, 0.0, abs(fract((uv.y - mouse.y) * 92.0) - 0.5));
        float shard = step(0.62, hash(vec2(floor((uv.x + (lineNoise - 0.5) * 0.055) * 38.0), line + floor(t * 4.0))));
        float slices = slice * shard * xGate * yGate;

        vec2 blockCell = floor((delta + vec2(0.08)) * vec2(72.0, 118.0));
        float blocks = step(0.78, hash(blockCell + floor(t * 9.0))) * local;

        float cross = smoothstep(0.006, 0.0, abs(delta.x)) + smoothstep(0.006, 0.0, abs(delta.y));
        cross *= exp(-md * 7.0);

        col += vec3(0.16, 0.05, 0.30) * slices * 0.18;
        col.r += slices * 0.16;
        col.g += slices * 0.08;
        col.b += slices * 0.30;
        col += vec3(0.10, 0.46, 0.92) * blocks * 0.13;
        col += vec3(0.22, 0.72, 1.0) * cross * 0.12;
    } else {
        // ---- soft cursor glow + ripple ------------------------------------
        float glow = exp(-md * 7.0);
        col += vec3(0.18, 0.44, 0.80) * glow * 0.42;
        float ripple = sin(md * 55.0 - t * 0.6) * 0.5 + 0.5;
        float rippleFall = exp(-md * 4.0);
        col += vec3(0.10, 0.30, 0.52) * ripple * rippleFall * 0.14;
    }

    // pulsing radial glow from centre
    float pulse = 0.5 + 0.5 * sin(t * 0.05);
    float centre = smoothstep(0.85, 0.15, length(uv - 0.5));
    col += vec3(0.04, 0.15, 0.30) * centre * (0.24 + 0.24 * pulse);

    // vignette
    float vig = smoothstep(1.15, 0.35, length((uv - 0.5) * vec2(1.15, 1.0)));
    col *= vig;

    fragColor = vec4(col, 0.94);
}
