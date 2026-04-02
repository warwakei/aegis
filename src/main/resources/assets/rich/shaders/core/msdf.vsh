#version 150

layout(std140) uniform FontData {
    vec4 screenData;
    vec4 outlineColor;
    vec4 atlasData;
    ivec4 charCount;
    vec4 chars[1024];
};

out vec2 texCoord;
out vec4 charColor;
out float outlineWidth;
out vec4 outColor;
out float pxRange;
out vec2 atlasSize;

vec2 rotate(vec2 point, vec2 pivot, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    vec2 p = point - pivot;
    return vec2(p.x * c - p.y * s, p.x * s + p.y * c) + pivot;
}

void main() {
    int charIndex = gl_VertexID / 6;
    int vertexIndex = gl_VertexID % 6;

    vec4 posSize = chars[charIndex * 4];
    vec4 uvCoords = chars[charIndex * 4 + 1];
    vec4 color = chars[charIndex * 4 + 2];
    vec4 rotationData = chars[charIndex * 4 + 3];

    float rotation = rotationData.x;
    vec2 pivot = rotationData.yz;

    vec2 positions[6] = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 1.0)
    );

    vec2 pos = positions[vertexIndex];
    vec2 screenPos = posSize.xy + pos * posSize.zw;

    if (rotation != 0.0) {
        screenPos = rotate(screenPos, pivot, rotation);
    }

    vec2 ndcPos = (screenPos / screenData.xy) * 2.0 - 1.0;
    ndcPos.y = -ndcPos.y;

    gl_Position = vec4(ndcPos, 0.0, 1.0);

    texCoord = vec2(
    mix(uvCoords.x, uvCoords.z, pos.x),
    mix(uvCoords.y, uvCoords.w, pos.y)
    );

    charColor = color;
    outlineWidth = screenData.w;
    outColor = outlineColor;
    atlasSize = atlasData.xy;
    pxRange = atlasData.z;
}