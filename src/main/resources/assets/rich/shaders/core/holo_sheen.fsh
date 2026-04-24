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
    vec2 rectSize = rect.zw;
    vec2 halfSize = rectSize * 0.5;
    vec2 center = pixelCoord - halfSize;

    float maxRadius = min(halfSize.x, halfSize.y);
    vec4 currentRadii = min(radii, vec4(maxRadius));

    float dist = roundedBoxSDF(center, halfSize, currentRadii);

    float pixelWidth = fwidth(dist);
    float smoothing = max(pixelWidth, 0.5 / screen.z);
    float alpha = 1.0 - smoothstep(-smoothing, smoothing, dist);

    if (alpha < 0.01) {
        discard;
    }

    vec2 uv = pixelCoord / max(rect.zw, vec2(1.0)); // rectSize is rect.zw

    float t = fract(params0.x * params0.z); // params0.x = timeSeconds, params0.z = speed
    vec2 dir = vec2(cos(params0.w), sin(params0.w)); // angle

    // Seamless moving bands without hard reset pop at cycle boundary.
    float sweep = dot(uv - 0.5, normalize(dir));
    float travel = fract(t + sweep * 0.32);
    float d = abs(travel - 0.5);
    d = min(d, 1.0 - d);

    float bandWidth = 0.12;
    float primaryBand = exp(-pow(d / bandWidth, 2.0));
    float secondaryBand = exp(-pow(abs(d - 0.11) / (bandWidth * 1.35), 2.0)) * 0.58;
    float tertiaryBand = exp(-pow(abs(d - 0.22) / (bandWidth * 1.75), 2.0)) * 0.34;
    float band = primaryBand + secondaryBand + tertiaryBand;

    // Subtle iridescence inside the band
    float hue = fract(0.58 + sweep * 0.35 + t * 0.25);
    vec3 iri = hsv2rgb(vec3(hue, 0.55, 1.0));

    float hue2 = fract(hue + 0.17 + sin((uv.x + uv.y + t) * 6.0) * 0.04);
    vec3 iri2 = hsv2rgb(vec3(hue2, 0.42, 1.0));

    // Grain to avoid "flat digital"
    float n = texture(NoiseSampler, uv * 10.0 + params0.x * 0.1).r; // params0.x = timeSeconds
    float g = (n - 0.5) * params1.x; // grain is params1.x

    float scan = sin((uv.y + t * 0.8) * 140.0) * 0.006;
    float shimmer = 0.5 + 0.5 * sin((uv.x * 16.0 + uv.y * 23.0 + t * 8.0) * 3.14159);

    float sheen = band * params0.y * (0.88 + shimmer * 0.18); // intensity is params0.y
    vec3 col = mix(vec3(1.0), iri, 0.55) * sheen;
    col += iri2 * secondaryBand * params0.y * 0.25;
    col += vec3(scan);
    col += g * 0.6;

    vec4 src = vec4(col * tintColor.rgb, alpha * tintColor.a * sheen);
    fragColor = src;
}
