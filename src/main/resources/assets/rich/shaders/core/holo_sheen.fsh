#version 150

uniform sampler2D NoiseSampler;

in vec2 fragCoord;
in vec2 pixelCoord;

layout(std140) uniform SheenData {
    vec4 rect;
    vec4 screen;
    vec4 radii;
    vec4 tintColor;
    vec4 params0;
    vec4 params1;
};

out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.yz : r.xw;
    r.x = (p.y > 0.0) ? r.y : r.x;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float hash12(vec2 p) {
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

    vec2 uv = pixelCoord / max(rect.zw, vec2(1.0));

    float t = fract(params0.x * params0.z);
    vec2 dir = vec2(cos(params0.w), sin(params0.w));

    float sweep = dot(uv - 0.5, normalize(dir));
    float travel = fract(t + sweep * 0.34);
    float d = abs(travel - 0.5);
    d = min(d, 1.0 - d);

    float bandWidth = 0.115;
    float primaryBand = exp(-pow(d / bandWidth, 2.0));
    float secondaryBand = exp(-pow(abs(d - 0.105) / (bandWidth * 1.30), 2.0)) * 0.65;
    float tertiaryBand = exp(-pow(abs(d - 0.21) / (bandWidth * 1.70), 2.0)) * 0.40;
    float band = primaryBand + secondaryBand + tertiaryBand;

    float hue = fract(0.55 + sweep * 0.42 + t * 0.30);
    vec3 iri = hsv2rgb(vec3(hue, 0.65, 1.0));

    float hue2 = fract(hue + 0.20 + sin((uv.x + uv.y + t) * 6.8) * 0.050);
    vec3 iri2 = hsv2rgb(vec3(hue2, 0.50, 1.0));

    float n = texture(NoiseSampler, uv * 12.0 + params0.x * 0.10).r;
    float g = (n - 0.5) * params1.x;

    float scan = sin((uv.y + t * 0.85) * 160.0) * 0.008;
    float shimmer = 0.5 + 0.5 * sin((uv.x * 19.0 + uv.y * 26.0 + t * 9.0) * 3.14159);

    float sheen = band * params0.y * (0.85 + shimmer * 0.25);
    vec3 col = mix(vec3(1.0), iri, 0.52) * sheen;
    col += iri2 * secondaryBand * params0.y * 0.32;
    col += vec3(scan);
    col += g * 0.70;

    vec4 src = vec4(col * tintColor.rgb, alpha * tintColor.a * sheen);
    fragColor = src;
}