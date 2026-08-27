#version 150

uniform float GameTime;
uniform vec2 MousePos;
uniform vec2 MouseVelocity;
uniform float ScrollOffset;
uniform float UnlockedRatio;
uniform float FocusRatio;

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265359;

// The Java tower viewport is not centred on the panel, so the shader has to be
// told where the drawn shaft sits. Both values are panel-relative (0..1) and
// track PathScreen's VIEW_X / VIEW_W / shaftHalfWidth.
const float SHAFT_CENTRE_X = 0.303;
const float SHAFT_HALF = 0.233;

// Sin-free hash. This runs tens of times per fragment across the whole panel,
// and at GUI scale 3-4 that panel is close to a megapixel, so a transcendental
// here was the dominant GPU cost. Pure ALU is several times cheaper and also
// avoids the banding that sin-based hashes show on some drivers.
float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
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

// Three octaves rather than four: the fourth contributed 6% of the amplitude
// for 25% of the cost, and the smoke reads the same without it.
float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.54;
    for (int i = 0; i < 3; i++) {
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

float distantLightning(vec2 uv, float t, float side) {
    float epoch = floor(t * 0.72 + side * 7.0);
    float gate = step(0.82, hash(vec2(epoch, side * 11.0)));
    // The gate depends only on time, so this branch is uniform across every
    // fragment in the frame: roughly 82% of frames skip the bolt entirely.
    if (gate <= 0.0)
        return 0.0;
    float baseX = mix(0.12, 0.88, side);
    float crooked = (noise(vec2(uv.y * 15.0, epoch + side * 5.0)) - 0.5) * 0.12;
    crooked += sin(uv.y * 34.0 + epoch) * 0.013;
    float bolt = exp(-abs(uv.x - baseX - crooked) * 185.0);
    float fade = (1.0 - smoothstep(0.12, 0.98, uv.y)) * (0.72 + 0.28 * sin(uv.y * 80.0 + epoch));
    return bolt * gate * fade;
}

// Slanted rain, driven hard by the storm that crowns the upper floors.
float rainLayer(vec2 uv, float t, float scale, float speed, float slant) {
    vec2 p = vec2(uv.x + uv.y * slant, uv.y) * vec2(scale, scale * 0.32);
    p.y += t * speed;
    vec2 cell = floor(p);
    float seed = hash(cell);
    vec2 local = fract(p) - 0.5;
    local.x += (seed - 0.5) * 0.72;
    float streak = (1.0 - smoothstep(0.010, 0.038, abs(local.x)))
            * (1.0 - smoothstep(0.10, 0.46, abs(local.y)));
    return streak * step(0.66, seed);
}

// Large, slow, dark flakes drifting in front of everything else.
float ashLayer(vec2 uv, float t, float scale, float speed) {
    vec2 p = uv * vec2(scale, scale * 1.2);
    p.y += t * speed;
    p.x += sin(t * 0.31 + uv.y * 5.0) * 0.35;
    vec2 cell = floor(p);
    vec2 local = fract(p) - 0.5;
    float seed = hash(cell + 3.7);
    local += (vec2(seed, hash(cell + 9.1)) - 0.5) * 0.6;
    return (1.0 - smoothstep(0.05, 0.13, length(local))) * step(0.86, seed);
}

// Light spilling off the summit, broken into shafts by the smoke.
float godRays(vec2 uv, float t) {
    vec2 delta = uv - vec2(SHAFT_CENTRE_X, 0.045);
    float falloff = exp(-length(delta) * 2.4) * (1.0 - smoothstep(0.0, 0.9, uv.y));
    if (falloff < 0.004)
        return 0.0;
    float angle = atan(delta.x, max(delta.y, 0.0001));
    float shafts = smoothstep(0.38, 0.86, noise(vec2(angle * 8.0, t * 0.21)));
    return shafts * falloff;
}

// Distant spires sit near the panel edges, where the UI leaves the sky visible.
// tipY is the spire's crown; everything below it tapers outward to the base.
float distantSpire(vec2 uv, float centreX, float width, float tipY, float phase) {
    float topness = 1.0 - smoothstep(tipY, 1.05, uv.y);
    float halfWidth = width * mix(1.0, 0.20, topness);
    float body = (1.0 - smoothstep(halfWidth, halfWidth + 0.006, abs(uv.x - centreX)))
            * smoothstep(tipY - 0.012, tipY + 0.012, uv.y);
    float notch = step(0.55, fract((1.0 - uv.y + phase) * 26.0));
    return max(0.0, body - notch * 0.06);
}

float filmic(float channel) {
    return channel / (channel + 0.78) * 1.34;
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
    // One unit per eleven floor steps of PathScreen.FLOOR_STEP (48 px), so the
    // far spires drift with the climb instead of against it.
    float scrollPhase = -ScrollOffset / 528.0;
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

    float focus = clamp(FocusRatio, 0.0, 1.0);

    vec3 col = mix(vec3(0.006, 0.001, 0.002), vec3(0.095, 0.004, 0.008), 1.0 - uv.y);

    // A blood eclipse anchors the composition high on the left, where the panel
    // leaves sky visible beside the drawn shaft.
    vec2 moonDelta = (uv - vec2(0.135, 0.175)) * vec2(1.16, 1.0);
    float moonRadius = length(moonDelta);
    float moonDisc = 1.0 - smoothstep(0.062, 0.068, moonRadius);
    float moonCorona = exp(-max(0.0, moonRadius - 0.064) * 15.0);
    // Crater detail is only visible on the disc itself, which is a tiny and
    // spatially coherent slice of the panel.
    float moonCrater = moonDisc > 0.0 ? noise(moonDelta * 22.0 + 4.0) : 0.0;
    col += vec3(0.52, 0.055, 0.048) * moonDisc * (0.62 + moonCrater * 0.5);
    col += vec3(0.34, 0.030, 0.028) * moonCorona * 0.55;
    col += vec3(0.20, 0.014, 0.016) * exp(-moonRadius * 4.2) * 0.7;

    // Two far spires, one per edge, drifting against the scroll for depth.
    float spirePhase = scrollPhase * 0.42;
    float spireA = distantSpire(uv, 0.085, 0.052, 0.30, spirePhase);
    float spireB = distantSpire(uv, 0.915, 0.060, 0.21, spirePhase + 0.31);
    float spireC = distantSpire(uv, 0.680, 0.038, 0.44, spirePhase + 0.63);
    float spires = max(spireA, max(spireB, spireC));
    col = mix(col, vec3(0.020, 0.005, 0.011), spires * 0.90);
    float spireLights = step(0.80, hash(vec2(floor(uv.x * 90.0),
            floor((1.0 - uv.y + spirePhase) * 26.0))));
    col += vec3(0.62, 0.085, 0.030) * spires * spireLights * 0.34;

    // Heavy black and blood-red smoke rolls around the tower.
    vec2 smokeFlow = vec2(sin(t * 0.055) * 0.16, -t * 0.035);
    float smokeA = fbm(uv * vec2(3.2, 2.6) + smokeFlow);
    float smokeB = fbm(uv * vec2(6.7, 4.8) - smokeFlow * 1.45);
    float bloodCloud = smoothstep(0.43, 0.83, smokeA + smokeB * 0.30);
    col += vec3(0.34, 0.004, 0.010) * bloodCloud * 0.54;
    vec2 sootCoord = uv * 4.1 + vec2(t * 0.014, t * 0.018);
    float soot = smoothstep(0.42, 0.78, noise(sootCoord));
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

    // The shaft itself is drawn in Java on top of this quad, so the shader only
    // supplies the mass and the heat rim that sit immediately outside it.
    float shaftOffset = abs(uv.x - SHAFT_CENTRE_X);
    float tower = 1.0 - smoothstep(SHAFT_HALF, SHAFT_HALF + 0.075, shaftOffset);
    float towerEdge = exp(-abs(shaftOffset - SHAFT_HALF) * 46.0);

    col = mix(col, vec3(0.004, 0.001, 0.002), tower * 0.62);
    float unlockedHeight = 1.0 - smoothstep(0.0, 0.030, abs(uv.y - (1.0 - unlocked)));
    float unlockedRegion = smoothstep(1.0 - unlocked - 0.025, 1.0 - unlocked + 0.025, uv.y);
    col += vec3(0.85, 0.028, 0.022) * towerEdge * (0.10 + unlockedRegion * 0.42);
    col += vec3(1.0, 0.10, 0.035) * unlockedHeight * tower * 0.56;

    // Throne light bleeding down from the summit, gated on how far the run has come.
    float rays = godRays(uv, t);
    col += vec3(1.0, 0.24, 0.10) * rays * (0.14 + unlocked * 0.46);

    // The band the dossier is inspecting glows so selection reads on the backdrop.
    float focusBand = 1.0 - smoothstep(0.0, 0.055, abs(uv.y - (1.0 - focus)));
    col += vec3(0.70, 0.10, 0.06) * focusBand * 0.24;
    col += vec3(1.0, 0.36, 0.14) * focusBand * tower * 0.30;

    // Storm rain: two layers, heavier as the player nears Baran's floors.
    float stormWeight = 0.35 + unlocked * 0.65;
    float rain = rainLayer(uv, t, 52.0, 2.9, 0.16) * 0.62
            + rainLayer(uv + vec2(0.37, 0.11), t, 88.0, 4.4, 0.21) * 0.38;
    col += vec3(0.55, 0.30, 0.36) * rain * stormWeight * 0.30;
    col += vec3(1.0, 0.55, 0.45) * rain * lightning * 2.4;

    float embers = emberLayer(uv, t, 17.0, 0.52);
    embers += emberLayer(uv + vec2(0.31, 0.17), t, 27.0, 0.31) * 0.58;
    col += vec3(1.0, 0.11, 0.025) * embers * 0.72;
    col += vec3(1.0, 0.58, 0.21) * pow(embers, 3.0) * 0.75;

    // Ash falls the other way, which keeps the ember rise from reading as a loop.
    float ash = ashLayer(uv, t, 14.0, 0.085) + ashLayer(uv + vec2(0.53, 0.29), t, 21.0, 0.135);
    col = mix(col, vec3(0.035, 0.014, 0.018), clamp(ash, 0.0, 1.0) * 0.68);

    // The seal never reaches past radius 0.13, and the twenty-odd segment and
    // ring evaluations behind it are the most expensive block in the shader.
    // Skipping them outside that disc is a coherent branch on the cursor.
    vec2 sealSpace = originalUv - mouse;
    sealSpace.x *= 1.25;
    if (length(sealSpace) < 0.155) {
        float seal = demonicSeal(sealSpace, t);
        float sealPulse = 0.72 + 0.28 * sin(t * 3.2);
        col += vec3(0.82, 0.012, 0.020) * seal * sealPulse * 0.78;
        col += vec3(1.0, 0.22, 0.08) * pow(seal, 2.0) * 0.34;
    }

    float velocityLength = min(length(velocity), 1.0);
    vec2 velocityDirection = velocity / max(velocityLength, 0.001);
    float wake = exp(-mouseRadius * 15.0);
    wake *= max(0.0, dot(normalize(mouseDelta + vec2(0.0001)), -velocityDirection));
    col += vec3(0.62, 0.008, 0.012) * wake * velocityLength * 0.30;

    // A bolt lights the whole frame, not just its own pixels.
    float flash = clamp(lightning * 0.35, 0.0, 0.6);
    col += vec3(0.30, 0.10, 0.12) * flash;

    float grain = hash(floor(originalUv * vec2(190.0, 260.0)) + floor(t * 2.0));
    col += vec3(0.16, 0.005, 0.008) * (grain - 0.5) * 0.055;

    // Warm-tinted falloff reads as soot on glass rather than a black overlay.
    float edge = length((originalUv - 0.5) * vec2(1.12, 0.92));
    float vignette = 1.0 - smoothstep(0.30, 0.91, edge);
    col *= 0.47 + vignette * 0.53;
    col = mix(col, col * vec3(1.14, 0.72, 0.66), smoothstep(0.42, 0.95, edge) * 0.55);

    col = vec3(filmic(col.r), filmic(col.g), filmic(col.b));
    fragColor = vec4(clamp(col, 0.0, 1.0), 0.975);
}
