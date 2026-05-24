#version 150

in vec2 pixelCoord;
in vec2 rectSize;
in vec4 cornerRadii;
in float thickness;
in float guiScale;
in float timeSeconds;
in float speed;
in float saturation;
in float value;
in float alphaMul;

out vec4 outColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.yz : r.xw;
    r.x = (p.y > 0.0) ? r.y : r.x;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float perimeterCoord(vec2 uv) {
    // uv in [0..1], returns 0..1 along rectangle perimeter, starting top-left going clockwise
    float x = clamp(uv.x, 0.0, 1.0);
    float y = clamp(uv.y, 0.0, 1.0);

    float top = x;
    float right = 1.0 + y;
    float bottom = 2.0 + (1.0 - x);
    float left = 3.0 + (1.0 - y);

    float mTop = step(y, 0.0005);
    float mBottom = step(0.9995, y);
    float mLeft = step(x, 0.0005);
    float mRight = step(0.9995, x);

    float coord = left;
    coord = mix(coord, top, mTop);
    coord = mix(coord, bottom, mBottom);
    coord = mix(coord, right, mRight);
    coord = mix(coord, left, mLeft);

    return fract(coord / 4.0);
}

void main() {
    vec2 halfSize = rectSize * 0.5;
    vec2 center = pixelCoord - halfSize;

    float maxRadius = min(halfSize.x, halfSize.y);
    vec4 radii = min(cornerRadii, vec4(maxRadius));

    float dist = roundedBoxSDF(center, halfSize, radii);

    float pixelWidth = fwidth(dist);
    float smoothing = max(pixelWidth, 0.5 / guiScale);

    float outerEdge = smoothstep(-smoothing, smoothing, dist);
    float innerEdge = smoothstep(-smoothing, smoothing, dist + thickness);
    float outlineMask = innerEdge * (1.0 - outerEdge);

    if (outlineMask < 0.01) {
        discard;
    }

    vec2 uv = pixelCoord / max(rectSize, vec2(1.0));
    float p = perimeterCoord(uv);

    float t = timeSeconds * speed;
    float hue = fract(p + t);

    // Richer spectral variation.
    float harmonicA = sin((p + t) * 12.0) * 0.04;
    float harmonicB = sin((p * 5.3 - t * 1.9) * 3.14159) * 0.025;
    hue = fract(hue + harmonicA + harmonicB);

    vec3 col = hsv2rgb(vec3(hue, saturation, value));

    float hueB = fract(hue + 0.13 + sin((p + t * 0.7) * 20.0) * 0.018);
    vec3 colB = hsv2rgb(vec3(hueB, saturation * 0.78, value * 1.05));
    col = mix(col, colB, 0.40);

    // Small brightness pulse
    float pulse = 0.80 + 0.20 * sin((t + p) * 6.28318);
    col *= pulse;

    float sparkle = step(0.985, hash12(floor(pixelCoord * 1.5) + vec2(t * 17.0, t * 9.0)));
    col += sparkle * 0.15;
    col = clamp(col, vec3(0.0), vec3(1.0));

    outColor = vec4(col, outlineMask * alphaMul);
}
