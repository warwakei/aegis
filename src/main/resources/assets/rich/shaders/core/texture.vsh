#version 150

struct TextureInstance {
    vec4 rect;
    vec4 uvCoords;
    vec4 radii;
    vec4 rotationData;
    vec4 colors[4];
};

layout(std140) uniform TextureData {
    vec4 screen;
    TextureInstance instances[64];
};

out vec2 fragCoord;
out vec2 pixelCoord;
out vec2 texCoord;
out vec2 rectSize;
out vec4 cornerRadii;
out vec4 fragColors[4];
out float fragSmoothness;
out float guiScale;

void main() {
    int instanceIndex = gl_VertexID / 6;
    int vertexIndex = gl_VertexID % 6;

    TextureInstance inst = instances[instanceIndex];

    vec2 positions[6] = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 1.0)
    );

    vec2 pos = positions[vertexIndex];

    float rotation = inst.rotationData.x;
    vec2 center = inst.rect.xy + inst.rect.zw * 0.5;
    vec2 localPos = (pos - 0.5) * inst.rect.zw;

    float cosR = cos(rotation);
    float sinR = sin(rotation);
    vec2 rotatedPos = vec2(
    localPos.x * cosR - localPos.y * sinR,
    localPos.x * sinR + localPos.y * cosR
    );

    vec2 screenPos = center + rotatedPos;
    vec2 ndcPos = (screenPos / screen.xy) * 2.0 - 1.0;
    ndcPos.y = -ndcPos.y;

    gl_Position = vec4(ndcPos, 0.0, 1.0);

    fragCoord = pos;
    pixelCoord = pos * inst.rect.zw;
    rectSize = inst.rect.zw;
    cornerRadii = inst.radii;
    fragSmoothness = screen.z;
    guiScale = screen.w;

    texCoord = vec2(
    mix(inst.uvCoords.x, inst.uvCoords.z, pos.x),
    mix(inst.uvCoords.y, inst.uvCoords.w, pos.y)
    );

    for (int i = 0; i < 4; i++) {
        fragColors[i] = inst.colors[i];
    }
}