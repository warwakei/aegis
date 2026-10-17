#version 410 core

layout(std140) uniform Uniforms {
    mat4 uProjection;
    vec4 uRect;
    vec4 uParams;
    vec4 uScreenInfo;
    vec4 uColors[9];
};

in vec2 vUV;
in vec2 vPixelCoord;

out vec4 fragColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// ---------------------------------------------------------------------------
// arc
// ---------------------------------------------------------------------------

float sdArc(in vec2 p, in float sca, in float scb, in float ra, in float rb) {
    p.x = abs(p.x);
    float k = (scb * p.x > sca * p.y) ? length(p - vec2(sca, scb) * ra) : abs(length(p) - ra);
    return k - rb;
}

// bilinear bezier over the 3x3 colour patch
vec4 sampleColor(vec2 uv) {
    vec2 coord = clamp(uv, 0.0, 1.0);

    float u = coord.x;
    float v = coord.y;

    float u0 = (1.0 - u) * (1.0 - u);
    float u1 = 2.0 * u * (1.0 - u);
    float u2 = u * u;

    float v0 = (1.0 - v) * (1.0 - v);
    float v1 = 2.0 * v * (1.0 - v);
    float v2 = v * v;

    vec4 result = vec4(0.0);
    result += uColors[0] * u0 * v0;
    result += uColors[1] * u1 * v0;
    result += uColors[2] * u2 * v0;
    result += uColors[3] * u0 * v1;
    result += uColors[4] * u1 * v1;
    result += uColors[5] * u2 * v1;
    result += uColors[6] * u0 * v2;
    result += uColors[7] * u1 * v2;
    result += uColors[8] * u2 * v2;

    return result;
}

// ---------------------------------------------------------------------------

void main() {
    vec2 center = uRect.xy + uRect.zw * 0.5;
    vec2 p = vPixelCoord - center;

    float rotRad = radians(uParams.w + 90.0);
    float c = cos(-rotRad);
    float s = sin(-rotRad);
    p = vec2(c * p.x - s * p.y, s * p.x + c * p.y);

    float degRad = radians(uParams.z) * 0.5;

    float ra = uParams.x * 0.5 - uParams.y * 0.5;
    float rb = uParams.y * 0.5;

    vec2 sc = vec2(sin(degRad), cos(degRad));

    float dist = sdArc(p, sc.x, sc.y, ra, rb);

    // pixel-space antialiasing, scaled by the gui scale factor
    float aaWidth = 1.8 / uScreenInfo.y;
    float alpha = 1.0 - smoothstep(-aaWidth, aaWidth, dist);

    if (alpha <= 0.001) {
        discard;
    }

    vec4 color = sampleColor(vUV);

    // soft focal shading across the band, thicker arcs read less flat
    float across = clamp(-dist / max(uParams.y * 0.5, 0.0001), 0.0, 1.0);
    float focal = mix(1.12, 0.92, across);

    // tiny dither keeps the gradient from banding on wide arcs
    float dither = (hash12(floor(vPixelCoord)) - 0.5) * (0.8 / 255.0);

    fragColor = vec4(color.rgb * focal + dither, color.a * alpha);
}
