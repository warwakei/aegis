#version 150

in vec2 fragCoord;
in vec2 pixelCoord;
in vec2 rectSize;
in vec4 cornerRadii;
in vec4 fragColors[9];
in float guiScale;
in float innerBlur;

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

    vec4 c0 = toLinear(fragColors[0]);
    vec4 c1 = toLinear(fragColors[1]);
    vec4 c2 = toLinear(fragColors[2]);
    vec4 c3 = toLinear(fragColors[3]);
    vec4 c4 = toLinear(fragColors[4]);
    vec4 c5 = toLinear(fragColors[5]);
    vec4 c6 = toLinear(fragColors[6]);
    vec4 c7 = toLinear(fragColors[7]);
    vec4 c8 = toLinear(fragColors[8]);

    vec4 top = mix(mix(c0, c1, x), mix(c1, c2, x), x);
    vec4 middle = mix(mix(c3, c4, x), mix(c4, c5, x), x);
    vec4 bottom = mix(mix(c6, c7, x), mix(c7, c8, x), x);

    vec4 result = mix(mix(top, middle, y), mix(middle, bottom, y), y);

    return result;
}

vec4 sampleGradient(vec2 uv) {
    if (innerBlur <= 0.0) {
        return toSRGB(sampleGradientAt(uv));
    }

    float blur = innerBlur * 0.01;

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
    vec2 halfSize = rectSize * 0.5;
    vec2 center = pixelCoord - halfSize;

    float maxRadius = min(halfSize.x, halfSize.y);
    vec4 radii = min(cornerRadii, vec4(maxRadius));

    vec4 r = vec4(radii.x, radii.y, radii.z, radii.w);

    float dist = roundedBoxSDF(center, halfSize, r);

    float pixelWidth = fwidth(dist);
    float smoothing = max(pixelWidth, 0.5 / guiScale);

    float alpha = 1.0 - smoothstep(-smoothing, smoothing, dist);

    if (alpha < 0.01) {
        discard;
    }

    vec4 color = sampleGradient(fragCoord);

    float noise = dither(gl_FragCoord.xy);
    color.rgb += noise - 0.5 / 255.0;

    fragColor = vec4(color.rgb, color.a * alpha);
}