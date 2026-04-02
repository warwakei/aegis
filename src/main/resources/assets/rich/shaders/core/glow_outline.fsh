#version 150

in vec2 fragCoord;
in vec2 pixelCoord;
in vec2 rectSize;
in vec4 cornerRadii;
in vec4 fragColor;
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

    float rayPos = progress * 1.4 - 0.2;
    float distToRay = abs(diag - rayPos);

    float glowWidth = 0.12;
    float glow = exp(-distToRay * distToRay / (glowWidth * glowWidth));

    glow *= smoothstep(0.0, 0.1, progress) * smoothstep(1.0, 0.9, progress);

    float finalAlpha = baseAlpha + glow * (1.0 - baseAlpha);
    finalAlpha *= outlineMask;

    vec3 col = fragColor.rgb;
    float brightness = 1.0 + glow * 0.5;
    col = min(col * brightness, vec3(1.0));

    outColor = vec4(col, finalAlpha * fragColor.a);
}