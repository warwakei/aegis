#version 150

in vec2 texCoord;
in vec2 texelSize;
in float offset;

out vec4 fragColor;

uniform sampler2D Sampler0;

void main() {
    vec2 uv = texCoord;
    vec2 halfpixel = texelSize * 0.5 * offset;

    vec4 sum = texture(Sampler0, uv) * 4.0;
    sum += texture(Sampler0, clamp(uv - halfpixel.xy, vec2(0.005), vec2(0.995)));
    sum += texture(Sampler0, clamp(uv + halfpixel.xy, vec2(0.005), vec2(0.995)));
    sum += texture(Sampler0, clamp(uv + vec2(halfpixel.x, -halfpixel.y), vec2(0.005), vec2(0.995)));
    sum += texture(Sampler0, clamp(uv - vec2(halfpixel.x, -halfpixel.y), vec2(0.005), vec2(0.995)));

    fragColor = sum / 8.0;
}