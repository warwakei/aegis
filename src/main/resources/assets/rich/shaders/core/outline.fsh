#version 150

in vec2 fragCoord;
in vec2 pixelCoord;

layout(std140) uniform OutlineData {
    vec4 rect;
    vec4 screen;
    vec4 radii;
    vec4 colors[8];
    vec4 thicknesses;
    vec4 thicknesses2;
    vec4 glowColor;
    vec4 glowParams; // x = glowRadius, y = glowIntensity, z, w = padding
};

out vec4 fragColor;

const float PI = 3.14159265359;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.yz : r.xw;
    r.x = (p.y > 0.0) ? r.y : r.x;

    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float getPerimeterPosition(vec2 center, vec2 halfSize) {
    vec2 normCenter = center / max(halfSize, vec2(0.001));
    float angle = atan(normCenter.y, normCenter.x);
    float t = (angle + PI) / (2.0 * PI);
    return fract(t + 0.125);
}

vec4 getPerimeterColor(float t) {
    float segment = t * 8.0;
    int idx = int(floor(segment));
    float frac = fract(segment);

    int idx1 = idx % 8;
    int idx2 = (idx + 1) % 8;

    return mix(colors[idx1], colors[idx2], smoothstep(0.0, 1.0, frac));
}

float getPerimeterThickness(float t) {
    float segment = t * 8.0;
    int idx = int(floor(segment));
    float frac = fract(segment);

    int idx1 = idx % 8;
    int idx2 = (idx + 1) % 8;

    vec4 t1 = (idx1 < 4) ? thicknesses : thicknesses2;
    vec4 t2 = (idx2 < 4) ? thicknesses : thicknesses2;

    float th1 = (idx1 == 0) ? t1.x : (idx1 == 1) ? t1.y : (idx1 == 2) ? t1.z : t1.w;
    float th2 = (idx2 == 0) ? t2.x : (idx2 == 1) ? t2.y : (idx2 == 2) ? t2.z : t2.w;

    return mix(th1, th2, smoothstep(0.0, 1.0, frac));
}

void main() {
    vec2 halfRectSize = rect.zw * 0.5;
    vec2 center = pixelCoord - halfRectSize;

    float maxRadius = min(halfRectSize.x, halfRectSize.y);
    vec4 currentRadii = min(radii, vec4(maxRadius));

    float dist = roundedBoxSDF(center, halfRectSize, currentRadii);

    float perimeterPos = getPerimeterPosition(center, halfRectSize);
    vec4 outlineColor = getPerimeterColor(perimeterPos);
    float outlineThickness = getPerimeterThickness(perimeterPos);

    float pixelWidth = fwidth(dist);
    float smoothing = max(pixelWidth * screen.z, 0.5 / screen.w); // fragSmoothness is screen.z, guiScale is screen.w

    float outerEdge = smoothstep(-smoothing, smoothing, dist);
    float innerEdge = smoothstep(-smoothing, smoothing, dist + outlineThickness);
    float outlineMask = innerEdge * (1.0 - outerEdge);

    // Glow effect
    float glowRadius = glowParams.x;
    float glowIntensity = glowParams.y;

    float glowSDF = roundedBoxSDF(center, halfRectSize, currentRadii);
    float glowAlpha = 1.0 - smoothstep(-glowRadius, glowRadius, glowSDF);
    glowAlpha *= glowIntensity;

    vec4 finalGlowColor = glowColor;
    finalGlowColor.a *= glowAlpha;

    vec4 finalOutlineColor = vec4(outlineColor.rgb, outlineColor.a * outlineMask);

    fragColor = finalOutlineColor + finalGlowColor * (1.0 - finalOutlineColor.a);
}