#version 150

layout(std140) uniform GlassData {
    vec4 resolution; // xy = framebuffer, z = saturation, w = reflect toggle
    vec4 tintColor;
    vec4 settings; // x = tint, y = edge glow
};

out vec2 texCoord;
out vec2 texelSize;

void main() {
    vec2 positions[6] = vec2[](
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2( 1.0,  1.0),
    vec2(-1.0, -1.0),
    vec2( 1.0,  1.0),
    vec2(-1.0,  1.0)
    );

    vec2 uvs[6] = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 1.0)
    );

    gl_Position = vec4(positions[gl_VertexID], 0.0, 1.0);
    texCoord = uvs[gl_VertexID];
    texelSize = 1.0 / resolution.xy;
}
