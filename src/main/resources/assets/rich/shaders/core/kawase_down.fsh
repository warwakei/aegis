#version 150

in vec2 texCoord;
in vec2 texelSize;
in float offset;

out vec4 fragColor;

uniform sampler2D Sampler0;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    vec2 taps[8] = vec2[](
    vec2(-1.0, -1.0),
    vec2( 1.0,  1.0),
    vec2( 1.0, -1.0),
    vec2(-1.0,  1.0),
    vec2( 0.0,  1.0),
    vec2( 0.0, -1.0),
    vec2( 1.0,  0.0),
    vec2(-1.0,  0.0)
    );

    vec2 halfpixel = texelSize * 0.5 * offset;
    vec2 clampMin = vec2(0.005);
    vec2 clampMax = vec2(0.995);

    // dual filter: centre keeps four units of weight, ring taps one each
    vec4 sum = texture(Sampler0, texCoord) * 4.0;

    for (int i = 0; i < 8; i++) {
        sum += texture(Sampler0, clamp(texCoord + taps[i] * halfpixel, clampMin, clampMax));
    }

    vec3 color = (sum / 12.0).rgb;

    // dither: blurred gradients band badly on wide dark areas
    color += (hash12(floor(texCoord * 4096.0)) - 0.5) * (1.0 / 255.0);

    fragColor = vec4(clamp(color, vec3(0.0), vec3(1.0)), 1.0);
}
