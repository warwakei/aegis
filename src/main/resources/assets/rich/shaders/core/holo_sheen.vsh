#version 150

layout(std140) uniform SheenData {
    vec4 rect;
    vec4 screen;
    vec4 radii;
    vec4 tintColor;
    vec4 params0; // time, intensity, speed, angle
    vec4 params1; // grain, blendMode, unused...
};

out vec2 fragCoord;
out vec2 pixelCoord;
out vec2 rectSize;
out vec4 cornerRadii;
out vec4 tint;
out float guiScale;
out float timeSeconds;
out float intensity;
out float speed;
out float angle;
out float grain;

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

    vec2 screenPos = rect.xy + pos * rect.zw;
    vec2 ndcPos = (screenPos / screen.xy) * 2.0 - 1.0;
    ndcPos.y = -ndcPos.y;

    gl_Position = vec4(ndcPos, 0.0, 1.0);

    fragCoord = pos;
    pixelCoord = pos * rect.zw;
    rectSize = rect.zw;
    cornerRadii = radii;

    guiScale = screen.z;
    tint = tintColor;
    timeSeconds = params0.x;
    intensity = params0.y;
    speed = params0.z;
    angle = params0.w;
    grain = params1.x;
}

