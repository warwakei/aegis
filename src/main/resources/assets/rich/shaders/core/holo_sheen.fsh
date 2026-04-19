#version 150

in vec2 fragCoord;
in vec2 pixelCoord;
in vec2 rectSize;
in vec4 cornerRadii;
in vec4 tint;
in float guiScale;
in float timeSeconds;
in float intensity;
in float speed;
in float angle;
in float grain;

out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.yz : r.xw;
    r.x = (p.y > 0.0) ? r.y : r.x;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float hash12(vec2 p) {
    // Cheap stable noise in 0..1
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec2 halfSize = rectSize * 0.5;
    vec2 center = pixelCoord - halfSize;

    float maxRadius = min(halfSize.x, halfSize.y);
    vec4 radii = min(cornerRadii, vec4(maxRadius));

    float dist = roundedBoxSDF(center, halfSize, radii);

    float pixelWidth = fwidth(dist);
    float smoothing = max(pixelWidth, 0.5 / guiScale);
    float alpha = 1.0 - smoothstep(-smoothing, smoothing, dist);

    if (alpha < 0.01) {
        discard;
    }

    vec2 uv = pixelCoord / max(rectSize, vec2(1.0));

    float t = fract(timeSeconds * speed);
    vec2 dir = vec2(cos(angle), sin(angle));

    // Sheen band traveling across the panel
    float sweep = dot(uv - 0.5, normalize(dir));
    float bandCenter = mix(-0.85, 0.85, t);
    float bandWidth = 0.12;
    float band = exp(-pow((sweep - bandCenter) / bandWidth, 2.0));

    // Subtle iridescence inside the band
    float hue = fract(0.58 + sweep * 0.35 + t * 0.25);
    vec3 iri = hsv2rgb(vec3(hue, 0.55, 1.0));

    // Grain to avoid "flat digital"
    float n = hash12(gl_FragCoord.xy);
    float g = (n - 0.5) * grain;

    float sheen = band * intensity;
    vec3 col = mix(vec3(1.0), iri, 0.55) * sheen;
    col += g;

    // Pure additive overlay, alpha gated by panel SDF
    fragColor = vec4(col * tint.rgb, alpha * tint.a * sheen);
}

