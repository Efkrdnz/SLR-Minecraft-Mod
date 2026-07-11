#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 localPos;
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
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float weight = 0.54;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * weight;
        p = p * 2.02 + vec2(7.13, 3.71);
        weight *= 0.47;
    }
    return value;
}

void main() {
    float kind = floor(texCoord0.x / 2.0);
    vec2 uv = vec2(fract(texCoord0.x), texCoord0.y);
    float time = GameTime * 1600.0;
    float rise = 1.0 - uv.y;

    vec2 flowP = vec2(uv.x * 4.6, rise * 3.6 - time * 0.24);
    vec2 warpA = vec2(fbm(flowP + vec2(time * 0.035, 0.0)),
                      fbm(flowP + vec2(5.7, -time * 0.045)));
    vec2 warpB = vec2(noise(flowP * 1.86 + warpA * 2.4 - vec2(time * 0.08, 0.0)),
                      noise(flowP * 1.72 - warpA * 2.1 + vec2(2.9, time * 0.06)));
    vec2 warped = uv + (warpB - 0.5) * vec2(0.22, 0.10);
    float broad = fbm(vec2(warped.x * 6.0 + time * 0.025,
                           (1.0 - warped.y) * 5.0 - time * 0.27));
    float detail = fbm(vec2(warped.x * 15.0 - time * 0.13 + broad * 2.8,
                            (1.0 - warped.y) * 12.0 - time * 0.55));
    float side = abs(warped.x - 0.5) * 2.0;
    float verticalFade = smoothstep(0.0, 0.07, rise) * smoothstep(0.0, 0.10, 1.0 - rise);
    float alpha = 0.0;
    float energy = 0.0;
    float voidDensity = 0.0;

    if (kind < 10.5) {
        float flameWidth = mix(0.92, 0.12, pow(clamp(rise, 0.0, 1.0), 0.72));
        float silhouette = 1.0 - smoothstep(flameWidth - 0.16, flameWidth + 0.10,
                                             side + (broad - 0.5) * 0.34);
        float tornBody = smoothstep(0.20, 0.72, broad * 0.58 + detail * 0.48);
        float innerFlow = sin(rise * 23.0 - time * 0.62 + broad * 7.0) * 0.5 + 0.5;
        alpha = silhouette * verticalFade * (0.16 + tornBody * 0.62 + innerFlow * 0.16);
        energy = pow(innerFlow, 6.0) * silhouette + smoothstep(0.72, 1.03, detail) * 0.55;
        voidDensity = silhouette * (0.70 + broad * 0.30);
    } else if (kind < 11.5) {
        float softSide = 1.0 - smoothstep(0.36, 0.56, side);
        float curtain = smoothstep(0.24, 0.77, broad + detail * 0.30);
        float tears = smoothstep(0.34, 0.62, abs(sin(warped.x * 19.0 + broad * 8.0)));
        float fold = pow(1.0 - abs(sin(rise * 14.0 - time * 0.34 + broad * 5.0)), 4.0);
        alpha = softSide * verticalFade * (0.04 + curtain * tears * 0.34 + fold * 0.11);
        energy = fold * curtain * 0.75;
        voidDensity = curtain * softSide;
    } else if (kind < 12.5) {
        float ribbon = 1.0 - smoothstep(0.23, 0.52, side + (detail - 0.5) * 0.18);
        float ends = smoothstep(0.0, 0.10, rise) * smoothstep(0.0, 0.13, 1.0 - rise);
        float current = sin(rise * 19.0 - time * 0.53 + broad * 7.5) * 0.5 + 0.5;
        alpha = ribbon * ends * (0.10 + broad * 0.38 + current * 0.21);
        energy = pow(current, 7.0) * ribbon;
        voidDensity = ribbon * (0.65 + broad * 0.35);
    } else if (kind < 13.5) {
        vec2 p = warped * 2.0 - 1.0;
        p.y *= 0.82;
        float ghost = 1.0 - smoothstep(0.48 + broad * 0.08, 1.02, length(p));
        float fracture = smoothstep(0.25, 0.76, broad * 0.55 + detail * 0.45);
        alpha = ghost * (0.05 + fracture * 0.34);
        energy = smoothstep(0.76, 1.02, detail) * ghost;
        voidDensity = ghost;
    } else if (kind < 14.5) {
        float softSide = 1.0 - smoothstep(0.32, 0.53, side);
        float foldA = sin(rise * 17.0 - time * 0.41 + broad * 8.0);
        float foldB = sin(rise * 9.0 + time * 0.23 - detail * 6.0);
        float interference = pow(clamp(1.0 - abs(foldA + foldB * 0.38), 0.0, 1.0), 5.0);
        float displacedEdge = smoothstep(0.52, 0.78, broad + detail * 0.22);
        alpha = softSide * verticalFade * (0.025 + interference * 0.17 + displacedEdge * 0.075);
        energy = interference * 0.85;
        voidDensity = softSide * displacedEdge * 0.45;
    } else if (kind < 15.5) {
        float split = abs(side - (0.12 + broad * 0.30));
        float flameWidth = mix(0.88, 0.055, pow(clamp(rise, 0.0, 1.0), 0.64));
        float silhouette = 1.0 - smoothstep(flameWidth - 0.13, flameWidth + 0.09,
                                             side + (detail - 0.5) * 0.38);
        float licking = smoothstep(0.18, 0.73, broad + detail * 0.34);
        float vein = pow(max(0.0, sin(rise * 25.0 - time * 0.70 + detail * 8.0)), 7.0);
        alpha = silhouette * verticalFade * (0.13 + licking * 0.69 + vein * 0.16);
        energy = vein * silhouette + (1.0 - smoothstep(0.02, 0.20, split)) * 0.32;
        voidDensity = silhouette * (0.76 + broad * 0.24);
    } else if (kind < 16.5) {
        float centeredX = (warped.x - 0.5) * 2.0;
        float asymmetry = (broad - 0.5) * 0.32
                + sin(rise * 8.0 - time * 0.19) * 0.035;
        float tongues = sin(warped.x * 21.0 + detail * 7.0 - time * 0.31) * 0.5 + 0.5;
        float bodyWidth = mix(0.91, 0.10, pow(clamp(rise, 0.0, 1.0), 0.76));
        bodyWidth += (tongues - 0.5) * smoothstep(0.48, 0.94, rise) * 0.22;
        float silhouette = 1.0 - smoothstep(bodyWidth - 0.13, bodyWidth + 0.09,
                                             abs(centeredX + asymmetry));

        float crownBreak = smoothstep(0.24, 0.73, broad * 0.58 + detail * 0.50);
        crownBreak = mix(1.0, crownBreak, smoothstep(0.52, 0.96, rise));
        float liquidBody = smoothstep(0.16, 0.76, broad * 0.54 + detail * 0.49);
        float risingFold = sin(rise * 24.0 - time * 0.61 + broad * 8.0) * 0.5 + 0.5;

        vec2 bendPoint = vec2(centeredX * 0.72, (rise - 0.46) * 1.18);
        float bendRadius = length(bendPoint + (warpA - 0.5) * 0.18);
        float spaceFold = pow(max(0.0, 1.0 - abs(sin(bendRadius * 24.0
                - time * 0.38 + broad * 5.5))), 7.0);
        float voidTear = smoothstep(0.78, 1.03, detail + broad * 0.18);

        alpha = silhouette * verticalFade * crownBreak
				* (0.17 + liquidBody * 0.74 + risingFold * 0.22 + spaceFold * 0.16);
        energy = risingFold * risingFold * liquidBody * 0.58
                + spaceFold * 0.92 + voidTear * 0.30;
        voidDensity = silhouette * (0.62 + liquidBody * 0.28)
                * (1.0 - spaceFold * 0.20);
	} else {
		float centeredX = (warped.x - 0.5) * 2.0;
		vec2 fieldPoint = vec2(centeredX * 0.76, (rise - 0.45) * 1.10);
		float fieldRadius = length(fieldPoint + (warpA - 0.5) * 0.24);
		float outerWidth = mix(0.96, 0.16, pow(clamp(rise, 0.0, 1.0), 0.72));
		float displacedSide = abs(centeredX + (warpB.x - 0.5) * 0.42
				+ sin(rise * 10.0 - time * 0.21) * 0.05);
		float silhouette = 1.0 - smoothstep(outerWidth - 0.18, outerWidth + 0.13, displacedSide);
		float rippleA = pow(max(0.0, 1.0 - abs(sin(fieldRadius * 22.0
				- time * 0.36 + broad * 6.0))), 6.0);
		float rippleB = pow(max(0.0, 1.0 - abs(sin(fieldRadius * 13.0
				+ time * 0.22 - detail * 5.0))), 8.0);
		float warpedMist = smoothstep(0.26, 0.78, broad * 0.61 + detail * 0.35);
		float tornEdge = smoothstep(0.55, 0.91, detail + abs(warpA.x - 0.5) * 0.36);
		alpha = silhouette * verticalFade
				* (0.035 + warpedMist * 0.25 + rippleA * 0.20 + rippleB * 0.13 + tornEdge * 0.07);
		energy = rippleA * 0.82 + rippleB * 0.52 + tornEdge * 0.24;
		voidDensity = silhouette * warpedMist * 0.58;
    }

    alpha *= vertexColor.a;
    if (alpha < 0.010)
        discard;

    vec3 abyss = vec3(0.010, 0.0, 0.026);
    vec3 deepPurple = mix(vec3(0.075, 0.004, 0.14), vertexColor.rgb, 0.58);
    vec3 violet = vec3(0.72, 0.20, 1.0);
    vec3 paleEdge = vec3(0.91, 0.62, 1.0);
    float edgeRefraction = pow(clamp(1.0 - side, 0.0, 1.0), 0.65)
            * pow(max(0.0, sin(rise * 31.0 - time * 0.47 + warpA.x * 10.0)), 9.0);
    vec3 color = mix(deepPurple, abyss, clamp(voidDensity * 0.62, 0.0, 0.82));
    color = mix(color, violet, clamp(energy * 0.78, 0.0, 0.86));
    color = mix(color, paleEdge, clamp(edgeRefraction * 0.48, 0.0, 0.52));
    color *= 1.04 + energy * 0.42;
    color *= 0.97 + texture(Sampler2, lightMap).r * 0.03;

    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.84) * ColorModulator.a);
}
