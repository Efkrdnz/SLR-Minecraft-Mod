#version 150

uniform float GameTime;
uniform vec2 MousePos;
uniform vec2 MouseVelocity;
uniform float ScrollOffset;
uniform float UnlockedRatio;

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265359;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float hash1(float p) {
    return fract(sin(p * 91.3458) * 47453.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += amplitude * noise(p);
        p = p * 2.03 + vec2(13.1, 7.7);
        amplitude *= 0.5;
    }
    return value;
}

float segmentDistance(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 0.0001), 0.0, 1.0);
    return length(pa - ba * h);
}

float lineMask(float distanceToLine, float width) {
    return 1.0 - smoothstep(width, width * 2.6, distanceToLine);
}

float emberLayer(vec2 uv, float t, float scale, float speed) {
    vec2 p = uv * vec2(scale, scale * 1.35);
    p.y += t * speed;
    vec2 cell = floor(p);
    vec2 local = fract(p) - 0.5;
    float seed = hash(cell);
    local.x += (seed - 0.5) * 0.65 + sin(t * 0.7 + seed * 20.0) * 0.12;
    local.y += (hash(cell + 17.3) - 0.5) * 0.7;
    float mote = 1.0 - smoothstep(0.025, 0.16, length(local));
    mote *= step(0.79, seed);
    return mote * (0.55 + 0.45 * sin(t * 4.0 + seed * 45.0));
}

float towerSilhouette(vec2 uv, float scrollPhase) {
    float y = uv.y;
    float bodyRange = smoothstep(0.035, 0.070, y) * (1.0 - smoothstep(0.985, 1.015, y));
    float taper = smoothstep(0.04, 0.98, y);
    float halfWidth = mix(0.055, 0.205, taper);

    // Repeated buttresses make the central void read as an impossibly tall tower.
    float floorCell = fract((1.0 - y + scrollPhase) * 11.0);
    float balcony = 1.0 - smoothstep(0.055, 0.145, abs(floorCell - 0.5));
    halfWidth += balcony * mix(0.022, 0.047, taper);

    float body = (1.0 - smoothstep(halfWidth, halfWidth + 0.009, abs(uv.x - 0.5))) * bodyRange;
    float spireHalf = max(0.0, (y - 0.012) * 0.70);
    float spire = (1.0 - smoothstep(spireHalf, spireHalf + 0.007, abs(uv.x - 0.5)))
            * (1.0 - smoothstep(0.078, 0.092, y));
    return max(body, spire);
}

float towerWindows(vec2 uv, float scrollPhase) {
    float taper = smoothstep(0.04, 0.98, uv.y);
    float halfWidth = mix(0.055, 0.205, taper);
    float floorCoord = (1.0 - uv.y + scrollPhase) * 11.0;
    float floorLine = 1.0 - smoothstep(0.018, 0.055, abs(fract(floorCoord) - 0.5));
    float inside = 1.0 - smoothstep(halfWidth * 0.78, halfWidth * 0.88, abs(uv.x - 0.5));
    float slitPattern = step(0.58, hash(vec2(floor(floorCoord), floor((uv.x + 0.5) * 31.0))));
    return floorLine * inside * slitPattern;
}

float distantLightning(vec2 uv, float t, float side) {
    float epoch = floor(t * 0.72 + side * 7.0);
    float gate = step(0.82, hash(vec2(epoch, side * 11.0)));
    float baseX = mix(0.12, 0.88, side);
    float crooked = (noise(vec2(uv.y * 15.0, epoch + side * 5.0)) - 0.5) * 0.12;
    crooked += sin(uv.y * 34.0 + epoch) * 0.013;
    float bolt = exp(-abs(uv.x - baseX - crooked) * 185.0);
    float fade = (1.0 - smoothstep(0.12, 0.98, uv.y)) * (0.72 + 0.28 * sin(uv.y * 80.0 + epoch));
    return bolt * gate * fade;
}

float demonicSeal(vec2 p, float t) {
    float radius = length(p);
    float angle = atan(p.y, p.x);
    float outer = lineMask(abs(radius - 0.120), 0.0027);
    float inner = lineMask(abs(radius - 0.079), 0.0021);

    // Segmented rune band, rotating against the inner sigil.
    float runeCells = fract((angle / (2.0 * PI) + 0.5 + t * 0.018) * 18.0);
    float runes = step(0.22, runeCells) * step(runeCells, 0.64);
    runes *= lineMask(abs(radius - 0.100), 0.006);

    // Pentagram drawn as five chords rather than a generic cursor ripple.
    vec2 star0 = vec2(cos(-PI * 0.5), sin(-PI * 0.5)) * 0.066;
    vec2 star1 = vec2(cos(-PI * 0.5 + 2.0 * PI / 5.0), sin(-PI * 0.5 + 2.0 * PI / 5.0)) * 0.066;
    vec2 star2 = vec2(cos(-PI * 0.5 + 4.0 * PI / 5.0), sin(-PI * 0.5 + 4.0 * PI / 5.0)) * 0.066;
    vec2 star3 = vec2(cos(-PI * 0.5 + 6.0 * PI / 5.0), sin(-PI * 0.5 + 6.0 * PI / 5.0)) * 0.066;
    vec2 star4 = vec2(cos(-PI * 0.5 + 8.0 * PI / 5.0), sin(-PI * 0.5 + 8.0 * PI / 5.0)) * 0.066;
    float star = lineMask(segmentDistance(p, star0, star2), 0.0017);
    star = max(star, lineMask(segmentDistance(p, star2, star4), 0.0017));
    star = max(star, lineMask(segmentDistance(p, star4, star1), 0.0017));
    star = max(star, lineMask(segmentDistance(p, star1, star3), 0.0017));
    star = max(star, lineMask(segmentDistance(p, star3, star0), 0.0017));
    return max(max(outer, inner), max(runes * 0.72, star * 0.78));
}

void main() {
    float t = GameTime * 1200.0;
    float unlocked = clamp(UnlockedRatio, 0.0, 1.0);
    // Match the Java tower's 42 px floor pitch and upward movement exactly.
    float scrollPhase = -ScrollOffset / 462.0;
    vec2 mouse = clamp(MousePos, vec2(0.0), vec2(1.0));
    vec2 velocity = clamp(MouseVelocity, vec2(-1.0), vec2(1.0));

    vec2 originalUv = texCoord;
    vec2 mouseDelta = originalUv - mouse;
    mouseDelta.x *= 1.25;
    float mouseRadius = length(mouseDelta);

    // The seal twists the smoke and heat field instead of producing System-style data glitches.
    float distortion = exp(-mouseRadius * 10.0) * sin(mouseRadius * 72.0 - t * 2.3) * 0.010;
    vec2 tangent = vec2(-mouseDelta.y, mouseDelta.x) / max(mouseRadius, 0.001);
    vec2 uv = originalUv + tangent * distortion;
    uv -= velocity * exp(-mouseRadius * 16.0) * 0.008;

    vec3 col = mix(vec3(0.006, 0.001, 0.002), vec3(0.095, 0.004, 0.008), 1.0 - uv.y);

    // Heavy black and blood-red smoke rolls around the tower.
    vec2 smokeFlow = vec2(sin(t * 0.055) * 0.16, -t * 0.035);
    float smokeA = fbm(uv * vec2(3.2, 2.6) + smokeFlow);
    float smokeB = fbm(uv * vec2(6.7, 4.8) - smokeFlow * 1.45);
    float bloodCloud = smoothstep(0.43, 0.83, smokeA + smokeB * 0.30);
    col += vec3(0.34, 0.004, 0.010) * bloodCloud * 0.54;
    vec2 sootCoord = uv * 4.1 + vec2(t * 0.014, t * 0.018);
    float sootField = noise(sootCoord) + noise(sootCoord * 2.07 + vec2(4.2, 8.7)) * 0.42;
    float soot = smoothstep(0.55, 1.02, sootField);
    col *= 1.0 - soot * 0.43;

    // Heat filaments crawl upward through the haze.
    float heatNoise = noise(vec2(uv.y * 18.0 - t * 0.65, floor(uv.x * 13.0)));
    float filamentX = fract(uv.x * 6.0 + heatNoise * 0.19);
    float filament = 1.0 - smoothstep(0.012, 0.055, abs(filamentX - 0.5));
    filament *= smoothstep(0.30, 0.88, smokeA);
    col += vec3(0.55, 0.025, 0.006) * filament * 0.21;

    float lightning = distantLightning(uv, t, 0.0) + distantLightning(uv, t, 1.0);
    col += vec3(1.0, 0.075, 0.035) * lightning * 0.68;
    col += vec3(1.0, 0.42, 0.20) * pow(lightning, 3.0) * 0.42;

    float tower = towerSilhouette(uv, scrollPhase);
    float windows = towerWindows(uv, scrollPhase);
    float towerEdgeWidth = mix(0.055, 0.205, smoothstep(0.04, 0.98, uv.y));
    float towerEdge = exp(-abs(abs(uv.x - 0.5) - towerEdgeWidth) * 105.0);
    towerEdge *= smoothstep(0.06, 0.14, uv.y);

    // The black silhouette is punctuated by unlocked energy rising from the base.
    col = mix(col, vec3(0.001, 0.0002, 0.0005), tower * 0.88);
    float unlockedHeight = 1.0 - smoothstep(0.0, 0.030, abs(uv.y - (1.0 - unlocked)));
    float unlockedRegion = smoothstep(1.0 - unlocked - 0.025, 1.0 - unlocked + 0.025, uv.y);
    col += vec3(0.58, 0.012, 0.018) * windows * (0.18 + unlockedRegion * 0.82);
    col += vec3(0.85, 0.028, 0.022) * towerEdge * (0.07 + unlockedRegion * 0.20);
    col += vec3(1.0, 0.10, 0.035) * unlockedHeight * tower * 0.56;

    float embers = emberLayer(uv, t, 17.0, 0.52);
    embers += emberLayer(uv + vec2(0.31, 0.17), t, 27.0, 0.31) * 0.58;
    col += vec3(1.0, 0.11, 0.025) * embers * 0.72;
    col += vec3(1.0, 0.58, 0.21) * pow(embers, 3.0) * 0.75;

    vec2 sealSpace = originalUv - mouse;
    sealSpace.x *= 1.25;
    float seal = demonicSeal(sealSpace, t);
    float sealPulse = 0.72 + 0.28 * sin(t * 3.2);
    col += vec3(0.82, 0.012, 0.020) * seal * sealPulse * 0.78;
    col += vec3(1.0, 0.22, 0.08) * pow(seal, 2.0) * 0.34;

    float velocityLength = min(length(velocity), 1.0);
    vec2 velocityDirection = velocity / max(velocityLength, 0.001);
    float wake = exp(-mouseRadius * 15.0);
    wake *= max(0.0, dot(normalize(mouseDelta + vec2(0.0001)), -velocityDirection));
    col += vec3(0.62, 0.008, 0.012) * wake * velocityLength * 0.30;

    float grain = hash(floor(originalUv * vec2(190.0, 260.0)) + floor(t * 2.0));
    col += vec3(0.16, 0.005, 0.008) * (grain - 0.5) * 0.055;

    float vignette = 1.0 - smoothstep(0.30, 0.91, length((originalUv - 0.5) * vec2(1.12, 0.92)));
    col *= 0.47 + vignette * 0.53;
    fragColor = vec4(clamp(col, 0.0, 1.0), 0.975);
}
