#version 150

uniform sampler2D NoiseSampler;

in vec2 fragCoord;
in vec2 pixelCoord;

layout(std140) uniform SheenData {
    vec4 rect;
    vec4 screen;
    vec4 radii;
    vec4 tintColor;
    vec4 params0; // time, intensity, speed, angle
    vec4 params1; // grain, blendMode, unused...
};

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
    vec2 halfSize = rect.zw * 0.5;
    vec2 center = pixelCoord - halfSize;

    float maxRadius = min(halfSize.x, halfSize.y);
    vec4 currentRadii = min(radii, vec4(maxRadius));

    float dist = roundedBoxSDF(center, halfSize, currentRadii);

    float pixelWidth = fwidth(dist);
    float smoothing = max(pixelWidth, 0.5 / screen.z); // guiScale is screen.z
    float alpha = 1.0 - smoothstep(-smoothing, smoothing, dist);

    if (alpha < 0.01) {
        discard;
    }

    vec2 uv = pixelCoord / max(rect.zw, vec2(1.0)); // rectSize is rect.zw

    float t = fract(params0.x * params0.z); // timeSeconds * speed
    vec2 dir = vec2(cos(params0.w), sin(params0.w)); // angle

    // Sheen band traveling across the panel
    float sweep = dot(uv - 0.5, normalize(dir));
    float bandCenter = mix(-0.85, 0.85, t);
    float bandWidth = 0.12;
    float band = exp(-pow((sweep - bandCenter) / bandWidth, 2.0));

    // Subtle iridescence inside the band
    float hue = fract(0.58 + sweep * 0.35 + t * 0.25);
    vec3 iri = hsv2rgb(vec3(hue, 0.55, 1.0));

    // Grain to avoid "flat digital"
    float n = texture(NoiseSampler, uv * 10.0 + timeSeconds * 0.1).r; // Use NoiseSampler
    float g = (n - 0.5) * params1.x; // grain is params1.x

    float sheen = band * params0.y; // intensity is params0.y
    vec3 col = mix(vec3(1.0), iri, 0.55) * sheen;
    col += g;

    // Pure additive overlay, alpha gated by panel SDF
    // fragColor = vec4(col * tintColor.rgb, alpha * tintColor.a * sheen); // Original blending

    // New blending based on blendMode
    if (params1.y == 1.0) { // Example: Additive blending
        fragColor = vec4(col * tintColor.rgb, alpha * tintColor.a * sheen) + fragColor;
    } else if (params1.y == 2.0) { // Example: Screen blending
        vec4 src = vec4(col * tintColor.rgb, alpha * tintColor.a * sheen);
        fragColor = 1.0 - (1.0 - src) * (1.0 - fragColor);
    } else { // Default: Alpha blending
        fragColor = vec4(col * tintColor.rgb, alpha * tintColor.a * sheen);
    }
}

