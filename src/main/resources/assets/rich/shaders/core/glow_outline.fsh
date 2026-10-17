#version 150

in vec2 fragCoord;
in vec2 pixelCoord;
in vec2 rectSize;
in vec4 cornerRadii;
in vec4 tintColor;
in float thickness;
in float progress;
in float baseAlpha;
in float guiScale;

out vec4 outColor;

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

    vec2 normPos = pixelCoord / rectSize;
    float diag = (normPos.x + (1.0 - normPos.y)) * 0.5;

    // four-band light sweep travelling across the element
    float rayPos = progress * 1.50 - 0.25;
    float distToRay = abs(diag - rayPos);

    float glowWidth = 0.105;
    float glowSharp = exp(-distToRay * distToRay / (glowWidth * glowWidth));
    float glowSoft = exp(-distToRay * distToRay / ((glowWidth * 2.5) * (glowWidth * 2.5)));
    float glowWide = exp(-distToRay * distToRay / ((glowWidth * 5.0) * (glowWidth * 5.0)));

    // the ray leaves with soft fades at both ends instead of popping in
    float progressFade = smoothstep(0.0, 0.10, progress) * smoothstep(1.0, 0.90, progress);
    glowSharp *= progressFade;
    glowSoft *= progressFade;
    glowWide *= progressFade;

    float glow = glowSharp * 0.60 + glowSoft * 0.30 + glowWide * 0.10;

    // fine shimmer riding on the beam
    float shimmer = sin(distToRay * 40.0 + progress * 20.0) * 0.5 + 0.5;
    float sparkle = hash12(pixelCoord + vec2(progress * 100.0, 0.0));
    float sparkleMask = step(0.97, sparkle) * max(0.0, 1.0 - abs(distToRay) * 3.0);
    float sparkleEffect = sparkleMask * 0.10 * shimmer;

    float finalAlpha = baseAlpha + glow * (1.0 - baseAlpha);
    finalAlpha = min(finalAlpha + sparkleEffect, 1.0);
    finalAlpha *= outlineMask;

    vec3 col = tintColor.rgb;
    float brightness = 1.0 + glow * 0.65 + sparkleEffect * 0.5;
    col = min(col * brightness, vec3(1.0));

    // warm leading edge, cool trailing tail
    float warmShift = glowSharp * 0.08 - glowWide * 0.04;
    col += vec3(warmShift, warmShift * 0.5, -warmShift * 0.3);
    col = clamp(col, vec3(0.0), vec3(1.0));

    outColor = vec4(col, finalAlpha * tintColor.a);
}
