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

#define PI 3.14159265359

float sdArc(in vec2 p, in float sca, in float scb, in float ra, in float rb) {
    p.x = abs(p.x);
    return ((scb*p.x > sca*p.y) ? length(p-vec2(sca,scb)*ra) : 
                                  abs(length(p)-ra)) - rb;
}

void main() {
    vec2 p = (vUV - 0.5) * vSize;
    
    float rotRad = radians(uParams.w);
    float degRad = radians(uParams.z) * 0.5;
    
    mat2 rotMat = mat2(cos(rotRad), -sin(rotRad), sin(rotRad), cos(rotRad));
    p = rotMat * p;
    
    float ra = uParams.x * 0.5 - uParams.y * 0.5;
    float rb = uParams.y * 0.5;
    float outlineThickness = uParams2.y;
    
    vec2 sc = vec2(sin(degRad), cos(degRad));
    
    float dist = sdArc(p, sc.x, sc.y, ra, rb);
    float edge = fwidth(dist);
    
    float arcAlpha = 1.0 - smoothstep(-edge, edge, dist);
    
    if (arcAlpha <= 0.0) discard;
    
    float innerDist = -dist - outlineThickness;
    float innerEdge = fwidth(innerDist);
    float fillAlpha = 1.0 - smoothstep(-innerEdge, innerEdge, innerDist);
    
    vec4 fill = uFillColor * fillAlpha;
    vec4 outline = uOutlineColor * (1.0 - fillAlpha);
    
    vec4 finalColor = fill + outline;
    fragColor = vec4(finalColor.rgb, finalColor.a * arcAlpha);
}
