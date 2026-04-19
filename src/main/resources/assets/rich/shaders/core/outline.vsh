#version 150

layout(std140) uniform OutlineData {
    vec4 rect;
    vec4 screen;
    vec4 radii;
    vec4 colors[8];
    vec4 thicknesses;
    vec4 thicknesses2;
    vec4 glowColor;
    vec4 glowParams;
};

out vec2 fragCoord;
out vec2 pixelCoord;

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

    float maxT = max(
        max(max(thicknesses.x, thicknesses.y), max(thicknesses.z, thicknesses.w)),
        max(max(thicknesses2.x, thicknesses2.y), max(thicknesses2.z, thicknesses2.w))
    );

    float padding = maxT + 2.0;

    vec2 expandedPos = rect.xy - padding + pos * (rect.zw + padding * 2.0);
    vec2 ndcPos = (expandedPos / screen.xy) * 2.0 - 1.0;
    ndcPos.y = -ndcPos.y;

    gl_Position = vec4(ndcPos, 0.0, 1.0);

    fragCoord = pos;
    pixelCoord = pos * (rect.zw + padding * 2.0) - padding;
}