#version 150

layout(std140) uniform IriOutlineData {
    vec4 rect;
    vec4 screen; // screenW, screenH, guiScale, thickness
    vec4 radii;
    vec4 params0; // time, speed, saturation, value
    vec4 params1; // alpha, unused...
};

out vec2 pixelCoord;
out vec2 rectSize;
out vec4 cornerRadii;
out float thickness;
out float guiScale;
out float timeSeconds;
out float speed;
out float saturation;
out float value;
out float alphaMul;

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

    float padding = screen.w + 2.0;

    vec2 expandedPos = rect.xy - padding + pos * (rect.zw + padding * 2.0);
    vec2 ndcPos = (expandedPos / screen.xy) * 2.0 - 1.0;
    ndcPos.y = -ndcPos.y;

    gl_Position = vec4(ndcPos, 0.0, 1.0);

    pixelCoord = pos * (rect.zw + padding * 2.0) - padding;
    rectSize = rect.zw;
    cornerRadii = radii;

    guiScale = screen.z;
    thickness = screen.w;

    timeSeconds = params0.x;
    speed = params0.y;
    saturation = params0.z;
    value = params0.w;
    alphaMul = params1.x;
}

