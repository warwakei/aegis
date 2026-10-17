#version 410 core

layout(std140) uniform Uniforms {
    mat4 uProjection;
    vec4 uRect;
    vec4 uParams;
    vec4 uParams2;
    vec4 uFillColor;
    vec4 uOutlineColor;
};

in vec2 vUV;
in vec2 vSize;

out vec4 fragColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float sdArc(in vec2 p, in float sca, in float scb, in float ra, in float rb) {
    p.x = abs(p.x);
    float k = (scb * p.x > sca * p.y) ? length(p - vec2(sca, scb) * ra) : abs(length(p) - ra);
    return k - rb;
}

void main() {
    // local pixel space around the quad centre
    vec2 p = (vUV - 0.5) * vSize;

    float rotRad = radians(uParams.w);
    float degRad = radians(uParams.z) * 0.5;

    mat2 rotMat = mat2(cos(rotRad), -sin(rotRad), sin(rotRad), cos(rotRad));
    p = rotMat * p;

    float ra = uParams.x * 0.5 - uParams.y * 0.5;
    float rb = uParams.y * 0.5;
    float outlineThickness = max(uParams2.y, 0.0001);

    vec2 sc = vec2(sin(degRad), cos(degRad));

    float dist = sdArc(p, sc.x, sc.y, ra, rb);
    float edge = fwidth(dist);

    float arcAlpha = 1.0 - smoothstep(-edge, edge, dist);

    if (arcAlpha <= 0.0) {
        discard;
    }

    float innerDist = -dist - outlineThickness;
    float innerEdge = fwidth(innerDist);
    float fillAlpha = 1.0 - smoothstep(-innerEdge, innerEdge, innerDist);

    // 0 at the rim, 1 deep inside: lets the rim stay the brightest part
    float across = clamp(-dist / outlineThickness, 0.0, 1.0);
    float rim = mix(1.18, 0.88, across);

    vec4 fill = uFillColor * fillAlpha;
    vec4 outline = uOutlineColor * (1.0 - fillAlpha);

    vec4 finalColor = fill * rim + outline;

    float dither = (hash12(floor(p * 2.0)) - 0.5) * (0.8 / 255.0);

    fragColor = vec4(finalColor.rgb + dither, finalColor.a * arcAlpha);
}
