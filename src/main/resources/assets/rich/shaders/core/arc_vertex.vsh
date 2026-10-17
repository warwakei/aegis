#version 410 core

layout(std140) uniform Uniforms {
    mat4 uProjection;
    vec4 uRect;
    vec4 uParams;
    vec4 uScreenInfo;
    vec4 uColors[9];
};

out vec2 vUV;
out vec2 vPixelCoord;

void main() {
    vec2 positions[6] = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 1.0)
    );

    vec2 vertex = positions[gl_VertexID];
    vec2 pos = uRect.xy + vertex * uRect.zw;

    float scaleFactor = uScreenInfo.y;
    vec2 scaledPos = pos / scaleFactor;

    gl_Position = uProjection * vec4(scaledPos, uScreenInfo.x, 1.0);

    vUV = vertex;
    vPixelCoord = pos;
}
