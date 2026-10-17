#version 150

layout(std140) uniform GlowOutlineData {
    vec4 rect;
    vec4 screen;
    vec4 radii;
    vec4 params; // x = progress, y = base alpha
    vec4 color;
};

out vec2 fragCoord;
out vec2 pixelCoord;
out vec2 rectSize;
out vec4 cornerRadii;
out vec4 tintColor;
out float thickness;
out float progress;
out float baseAlpha;
out float guiScale;

void main() {
    vec2 positions[6] = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 1.0)
    );

    vec2 pos = positions[gl_VertexID];

    // expand the quad so the glow can bleed outside the rect
    float padding = screen.w + 2.0;

    vec2 expandedPos = rect.xy - padding + pos * (rect.zw + padding * 2.0);
    vec2 ndcPos = (expandedPos / screen.xy) * 2.0 - 1.0;
    ndcPos.y = -ndcPos.y;

    gl_Position = vec4(ndcPos, 0.0, 1.0);

    fragCoord = pos;
    pixelCoord = pos * (rect.zw + padding * 2.0) - padding;
    rectSize = rect.zw;
    cornerRadii = radii;
    thickness = screen.w;
    guiScale = screen.z;
    progress = params.x;
    baseAlpha = params.y;
    tintColor = color;
}
