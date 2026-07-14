#version 150

uniform sampler2D Sampler2;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 localPos;
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

float fbm(vec2 p) {
    float result = 0.0;
    float weight = 0.55;
    for (int i = 0; i < 4; i++) {
        result += noise(p) * weight;
        p = p * 2.03 + vec2(4.7, 8.1);
        weight *= 0.48;
    }
    return result;
}

void main() {
    float kind = floor(texCoord0.x / 2.0);
    vec2 uv = vec2(fract(texCoord0.x), texCoord0.y);
    float time = GameTime * 1600.0;
    float upward = 1.0 - uv.y;
    float broad = fbm(vec2(uv.x * 5.0 + time * 0.04, upward * 4.0 - time * 0.20));
    float detail = fbm(vec2(uv.x * 13.0 - time * 0.11 + broad * 2.0,
                            upward * 10.0 - time * 0.42));
    float edge = smoothstep(0.0, 0.16, uv.x) * smoothstep(0.0, 0.16, 1.0 - uv.x);
    float verticalFade = smoothstep(0.0, 0.08, upward) * smoothstep(0.0, 0.10, 1.0 - upward);
    float alpha;

    if (kind < 0.5) {
        float stream = smoothstep(0.55, 0.91, broad * 0.65 + detail * 0.55);
        alpha = stream * verticalFade * 0.66;
    } else if (kind < 1.5) {
		float flameEdge = 1.0 - smoothstep(0.27, 0.50, abs(uv.x - 0.5));
		float broken = smoothstep(0.30, 0.68, broad + detail * 0.38);
		float body = smoothstep(0.05, 0.28, upward) * (1.0 - smoothstep(0.91, 1.0, upward));
		alpha = flameEdge * mix(0.58, 1.0, broken) * body;
    } else if (kind < 2.5) {
        float diagonal = 1.0 - abs(uv.x - 0.5) * 2.0;
        float sharp = pow(max(0.0, diagonal), 6.0);
        alpha = sharp * (0.58 + detail * 0.42);
	} else if (kind < 3.5) {
        float pulse = 0.66 + 0.34 * sin(time * 0.65 + uv.x * 18.0);
        alpha = edge * pulse;
	} else if (kind < 4.5) {
		vec2 p = uv * 2.0 - 1.0;
		p.y *= 0.88;
		vec2 warp = vec2(broad - 0.5, detail - 0.5) * 0.24;
		float distanceField = length(p + warp);
		float softVolume = 1.0 - smoothstep(0.58 + broad * 0.10, 1.02 + detail * 0.08, distanceField);
		float liquidDensity = smoothstep(0.20, 0.82, broad * 0.56 + detail * 0.44);
		float caustic = pow(max(0.0, sin((upward + detail * 0.18) * 22.0 - time * 0.46)), 5.0);
		alpha = softVolume * (0.07 + liquidDensity * 0.39 + caustic * 0.13);
	} else if (kind < 5.5) {
		float sideFade = 1.0 - smoothstep(0.25, 0.50, abs(uv.x - 0.5));
		float rolling = sin(upward * 13.0 - time * 0.29 + broad * 5.5) * 0.5 + 0.5;
		float openVeil = smoothstep(0.26, 0.76, broad * 0.62 + detail * 0.38);
		alpha = sideFade * verticalFade * (0.05 + openVeil * 0.34 + rolling * 0.11);
	} else {
		float ribbonFade = 1.0 - smoothstep(0.28, 0.50, abs(uv.x - 0.5));
		float endFade = smoothstep(0.0, 0.12, upward) * smoothstep(0.0, 0.14, 1.0 - upward);
		float rolling = sin(upward * 16.0 - time * 0.38 + detail * 6.0) * 0.5 + 0.5;
		alpha = ribbonFade * endFade * (0.08 + broad * 0.27 + rolling * 0.15);
    }

    alpha *= vertexColor.a;
    if (alpha < 0.012)
        discard;

	float flowingCore = pow(max(0.0, sin(upward * 19.0 - time * 0.41 + detail * 5.0)), 7.0);
    float hotCore = smoothstep(0.70, 0.98, detail + (1.0 - abs(uv.x - 0.5) * 2.0) * 0.24)
			+ flowingCore * step(3.5, kind) * 0.45;
    vec3 color = mix(vertexColor.rgb, vec3(1.0), hotCore * 0.48);
    color *= 1.02 + hotCore * 0.42;
    color *= 0.96 + texture(Sampler2, lightMap).r * 0.04;
    fragColor = vec4(color * ColorModulator.rgb, min(alpha, 0.78) * ColorModulator.a);
}
