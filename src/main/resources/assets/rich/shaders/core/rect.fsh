#version 150

in vec2 fragCoord;
in vec2 pixelCoord;

layout(std140) uniform RectData {
    vec4 rect;
    vec4 screen;
    vec4 radii;
    vec4 colors[9];
    vec4 shadowColor;
    vec4 shadowOffsetAndRadius;
};

out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.yz : r.xw;
    r.x = (p.y > 0.0) ? r.y : r.x;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

vec3 toLinear(vec3 srgb) {
    return pow(srgb, vec3(2.2));
}

vec3 toSRGB(vec3 linear) {
    return pow(linear, vec3(1.0 / 2.2));
}

vec4 toLinear(vec4 srgb) {
    return vec4(toLinear(srgb.rgb), srgb.a);
}

vec4 toSRGB(vec4 linear) {
    return vec4(toSRGB(linear.rgb), linear.a);
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float smoothInterpolate(float t) {
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}

vec4 sampleGradientAt(vec2 uv) {
    float x = clamp(uv.x, 0.0, 1.0);
    float y = clamp(uv.y, 0.0, 1.0);

    x = smoothInterpolate(x);
    y = smoothInterpolate(y);

    vec4 c0 = toLinear(colors[0]);
    vec4 c1 = toLinear(colors[1]);
    vec4 c2 = toLinear(colors[2]);
    vec4 c3 = toLinear(colors[3]);
    vec4 c4 = toLinear(colors[4]);
    vec4 c5 = toLinear(colors[5]);
    vec4 c6 = toLinear(colors[6]);
    vec4 c7 = toLinear(colors[7]);
    vec4 c8 = toLinear(colors[8]);

    vec4 top = mix(mix(c0, c1, x), mix(c1, c2, x), x);
    vec4 middle = mix(mix(c3, c4, x), mix(c4, c5, x), x);
    vec4 bottom = mix(mix(c6, c7, x), mix(c7, c8, x), x);

    return mix(mix(top, middle, y), mix(middle, bottom, y), y);
}

vec4 sampleGradient(vec2 uv) {
    if (screen.w <= 0.0) {
        return toSRGB(sampleGradientAt(uv));
    }

    float blur = screen.w * 0.01;
    vec4 sum = vec4(0.0);
    float weightSum = 0.0;

    sum += sampleGradientAt(uv) * 4.0;
    weightSum += 4.0;

    sum += sampleGradientAt(uv + vec2(-blur, 0.0)) * 2.0;
    sum += sampleGradientAt(uv + vec2(blur, 0.0)) * 2.0;
    sum += sampleGradientAt(uv + vec2(0.0, -blur)) * 2.0;
    sum += sampleGradientAt(uv + vec2(0.0, blur)) * 2.0;
    weightSum += 8.0;

    sum += sampleGradientAt(uv + vec2(-blur, -blur));
    sum += sampleGradientAt(uv + vec2(blur, -blur));
    sum += sampleGradientAt(uv + vec2(-blur, blur));
    sum += sampleGradientAt(uv + vec2(blur, blur));
    weightSum += 4.0;

    float blur2 = blur * 2.0;
    sum += sampleGradientAt(uv + vec2(-blur2, 0.0)) * 0.5;
    sum += sampleGradientAt(uv + vec2(blur2, 0.0)) * 0.5;
    sum += sampleGradientAt(uv + vec2(0.0, -blur2)) * 0.5;
    sum += sampleGradientAt(uv + vec2(0.0, blur2)) * 0.5;
    weightSum += 2.0;

    return toSRGB(sum / weightSum);
}

void main() {
    vec2 uv = fragCoord * 0.5 + 0.5;
    vec2 halfRectSize = rect.zw * 0.5;
    vec2 center = pixelCoord - halfRectSize;
    
    float maxRadius = min(halfRectSize.x, halfRectSize.y);
    vec4 currentRadii = min(radii, vec4(maxRadius));
    
    float rectSDF = roundedBoxSDF(center, halfRectSize, currentRadii);
    float pixelWidth = fwidth(rectSDF);
    float smoothing = max(pixelWidth, 0.5 / screen.z);
    float rectAlpha = 1.0 - smoothstep(-smoothing, smoothing, rectSDF);

    vec4 rectColor = sampleGradient(uv);

    // Soft inner light for richer depth.
    vec2 edgeUv = abs(uv * 2.0 - 1.0);
    float edgeFactor = 1.0 - clamp(max(edgeUv.x, edgeUv.y), 0.0, 1.0);
    float innerLight = pow(edgeFactor, 1.6) * 0.22;
    rectColor.rgb += innerLight * vec3(0.80, 0.88, 1.0);

    // Keep panel clean: no visible grain texture on UI background.

    rectColor.a *= rectAlpha;

    vec2 shadowFragCoord = pixelCoord - shadowOffsetAndRadius.xy - halfRectSize;
    float shadowSDF = roundedBoxSDF(shadowFragCoord, halfRectSize, currentRadii);
    float shadowAlpha = 1.0 - smoothstep(-shadowOffsetAndRadius.z, shadowOffsetAndRadius.z, shadowSDF);

    vec4 finalShadowColor = shadowColor;
    finalShadowColor.a *= shadowAlpha;

    // Slightly broadened penumbra for softer premium shadow feel.
    float penumbra = smoothstep(shadowOffsetAndRadius.z * 0.3, shadowOffsetAndRadius.z * 2.0, abs(shadowSDF));
    finalShadowColor.a *= (1.0 - penumbra * 0.50);

    fragColor = rectColor + finalShadowColor * (1.0 - rectColor.a);
}
