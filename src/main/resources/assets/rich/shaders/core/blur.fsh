#version 150

in vec2 fragCoord;
in vec2 pixelCoord;
in vec2 texCoord;
in vec2 rectSize;
in vec4 cornerRadii;
in float guiScale;
in float blurRadius;
in vec2 texelSize;
in vec4 tintColor;
in vec2 resolution;

out vec4 fragColor;

uniform sampler2D Sampler0;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.yz : r.xw;
    r.x = (p.y > 0.0) ? r.y : r.x;

    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec4 sampleQualityBlur(vec2 uv, float radius, vec2 jitter) {
    vec4 accum = vec4(0.0);
    float weightSum = 0.0;

    int rings = 7;
    int ringSamples = 16;
    float sigma = max(radius * 0.46, 0.35);
    float invTwoSigma2 = 1.0 / max(2.0 * sigma * sigma, 0.0001);

    vec2 aniso = vec2(1.0 + radius * 0.035, 1.0 - radius * 0.022);

    for (int ring = 0; ring <= rings; ring++) {
        float rNorm = float(ring) / float(rings);
        float ringRadius = rNorm * radius;

        int angular = ring == 0 ? 1 : ringSamples + ring * 2;
        for (int i = 0; i < angular; i++) {
            float fi = float(i);
            float fa = float(angular);
            float a = (fi / fa) * 6.28318530718 + jitter.x * 6.28318530718 + rNorm * 1.7;

            vec2 dir = vec2(cos(a), sin(a)) * aniso;
            vec2 offset = dir * ringRadius * texelSize;
            vec2 suv = clamp(uv + offset + jitter * texelSize * 0.8, vec2(0.001), vec2(0.999));

            float d2 = dot(offset / texelSize, offset / texelSize);
            float w = exp(-d2 * invTwoSigma2);

            vec4 c = texture(Sampler0, suv);
            accum += c * w;
            weightSum += w;
        }
    }

    return accum / max(weightSum, 1e-5);
}

void main() {
    vec2 halfSize = rectSize * 0.5;
    vec2 center = pixelCoord - halfSize;

    float maxRadius = min(halfSize.x, halfSize.y);
    vec4 rRadii = min(cornerRadii, vec4(maxRadius));

    float dist = roundedBoxSDF(center, halfSize, rRadii);

    float pixelWidth = fwidth(dist);
    float smoothing = max(pixelWidth, 0.5 / guiScale);
    float alpha = 1.0 - smoothstep(-smoothing, smoothing, dist);

    if (alpha < 0.01) {
        discard;
    }

    vec2 jitter = vec2(
            hash12(floor(pixelCoord) + vec2(3.17, 9.31)) - 0.5,
            hash12(floor(pixelCoord.yx) + vec2(6.77, 1.29)) - 0.5
    );

    vec4 blurred = sampleQualityBlur(texCoord, max(0.8, blurRadius * 1.25), jitter);

    vec4 centerSample = texture(Sampler0, texCoord);
    vec3 enhanced = mix(blurred.rgb, centerSample.rgb, 0.06);

    float vignette = smoothstep(1.2, 0.15, length((fragCoord - 0.5) * vec2(1.08, 0.92)));
    float localContrast = dot(abs(dFdx(enhanced)) + abs(dFdy(enhanced)), vec3(0.3333));
    enhanced += (0.045 + localContrast * 0.15) * vignette * (0.5 + 0.5 * tintColor.a);

    vec3 finalColor = mix(enhanced, tintColor.rgb, tintColor.a * 0.9);
    finalColor += jitter.x * 0.006;
    finalColor = clamp(finalColor, vec3(0.0), vec3(1.0));

    fragColor = vec4(finalColor, alpha);
}
