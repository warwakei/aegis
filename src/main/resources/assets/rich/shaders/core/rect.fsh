#version 150

in vec2 fragCoord;

layout(std140) uniform RectData {
    vec4 rect;
    vec4 screen;
    vec4 radii;
    vec4 colors[9];
    vec4 shadowColor;
    vec4 shadowOffsetAndRadius; // xy = offset, z = radius, w = padding
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

    vec4 result = mix(mix(top, middle, y), mix(middle, bottom, y), y);

    return result;
}

vec4 sampleGradient(vec2 uv) {
    if (screen.w <= 0.0) { // innerBlur is screen.w
        return toSRGB(sampleGradientAt(uv));
    }

    float blur = screen.w * 0.01; // innerBlur is screen.w

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

float dither(vec2 coord) {
    return fract(sin(dot(coord, vec2(12.9898, 78.233))) * 43758.5453) / 255.0;
}

void main() {
    // Calculate UV for gradient sampling
    vec2 uv = fragCoord / rectSize + 0.5;

    // Calculate SDF for the main rectangle
    vec2 halfRectSize = rectSize / 2.0;
    float rectSDF = roundedBoxSDF(fragCoord, halfRectSize - cornerRadii.x, cornerRadii);
    float rectAlpha = 1.0 - smoothstep(-0.5, 0.5, rectSDF); // Sharp edge for the rectangle

    // Calculate color for the main rectangle
    vec4 rectColor = sampleGradient(uv);
    rectColor.a *= rectAlpha;

    // Calculate SDF for the shadow
    vec2 shadowFragCoord = fragCoord - shadowOffsetAndRadius.xy;
    float shadowSDF = roundedBoxSDF(shadowFragCoord, halfRectSize - cornerRadii.x, cornerRadii);

    // Apply blur to the shadow
    float shadowAlpha = 1.0 - smoothstep(-shadowOffsetAndRadius.z, shadowOffsetAndRadius.z, shadowSDF);

    // Combine shadow color with its alpha
    vec4 finalShadowColor = shadowColor;
    finalShadowColor.a *= shadowAlpha;

    // Blend shadow and rectangle. Shadow should be behind.
    fragColor = rectColor + finalShadowColor * (1.0 - rectColor.a);
}