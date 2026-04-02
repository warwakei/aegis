#version 150

in vec2 fragCoord;
in vec2 pixelCoord;
in vec2 texCoord;
in vec2 rectSize;
in vec4 cornerRadii;
in vec4 fragColors[4];
in float fragSmoothness;
in float guiScale;

out vec4 fragColor;

uniform sampler2D Sampler0;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.yz : r.xw;
    r.x = (p.y > 0.0) ? r.y : r.x;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

vec4 sampleGradient(vec2 uv) {
    vec4 top = mix(fragColors[0], fragColors[3], uv.x);
    vec4 bottom = mix(fragColors[1], fragColors[2], uv.x);
    return mix(top, bottom, uv.y);
}

void main() {
    vec2 halfSize = rectSize * 0.5;
    vec2 center = pixelCoord - halfSize;

    vec4 radii = min(cornerRadii, vec4(min(halfSize.x, halfSize.y)));
    float dist = roundedBoxSDF(center, halfSize, radii);

    float edgeSmoothing = fwidth(dist);
    float alpha = clamp(0.5 - dist / max(edgeSmoothing, 0.0001), 0.0, 1.0);

    if (alpha <= 0.0) {
        discard;
    }
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 sharpTexCoord = (floor(texCoord * texSize) + 0.5) / texSize;
    vec4 texColor = mix(texture(Sampler0, sharpTexCoord), texture(Sampler0, texCoord), clamp(fragSmoothness, 0.0, 1.0));

    vec4 tintColor = sampleGradient(fragCoord);

    vec3 finalColor = texColor.rgb * tintColor.rgb;
    float finalAlpha = texColor.a * tintColor.a * alpha;

    fragColor = vec4(finalColor, finalAlpha);
}
