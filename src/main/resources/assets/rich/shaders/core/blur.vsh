#version 150

layout(std140) uniform BlurData {
    vec4 rect;
    vec4 screen; // xy = screen size, z = gui scale, w = blur radius
    vec4 framebufferSize;
    vec4 radii;
    vec4 color; // tint
};

out vec2 fragCoord;
out vec2 pixelCoord;
out vec2 texCoord;
out vec2 rectSize;
out vec4 cornerRadii;
out float guiScale;
out float blurRadius;
out vec2 texelSize;
out vec4 tintColor;
out vec2 resolution;

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
    blurRadius = screen.w;
    resolution = framebufferSize.xy;
    texelSize = 1.0 / resolution;
    tintColor = color;

    // the framebuffer is flipped, fix the sample uv here once
    vec2 fbPos = screenPos * guiScale;
    texCoord = vec2(fbPos.x / resolution.x, 1.0 - (fbPos.y / resolution.y));
}
