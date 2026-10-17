#version 150

in vec2 texCoord;
in vec2 texelSize;

out vec4 fragColor;

uniform sampler2D SceneSampler;
uniform sampler2D BlurSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform GlassData {
    vec4 resolution; // xy = framebuffer, z = saturation, w = reflect toggle
    vec4 tintColor;
    vec4 settings; // x = tint, y = edge glow
};

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// ---------------------------------------------------------------------------
// glass
// ---------------------------------------------------------------------------

vec3 adjustSaturation(vec3 color, float saturation) {
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    return mix(vec3(gray), color, saturation);
}

// combined sobel over the mask, gives a stable rim and a normal-ish gradient
float getEdge(vec2 uv) {
    float center = texture(MaskSampler, uv).r;
    float edge = 0.0;

    edge += abs(center - texture(MaskSampler, uv + vec2(texelSize.x, 0.0)).r);
    edge += abs(center - texture(MaskSampler, uv - vec2(texelSize.x, 0.0)).r);
    edge += abs(center - texture(MaskSampler, uv + vec2(0.0, texelSize.y)).r);
    edge += abs(center - texture(MaskSampler, uv - vec2(0.0, texelSize.y)).r);

    edge += 0.5 * abs(center - texture(MaskSampler, uv + texelSize).r);
    edge += 0.5 * abs(center - texture(MaskSampler, uv - texelSize).r);
    edge += 0.5 * abs(center - texture(MaskSampler, uv + vec2(texelSize.x, -texelSize.y)).r);
    edge += 0.5 * abs(center - texture(MaskSampler, uv + vec2(-texelSize.x, texelSize.y)).r);

    return clamp(edge * 1.6, 0.0, 1.0);
}

vec2 getRefraction(vec2 uv) {
    vec2 grad = vec2(
    texture(MaskSampler, uv + vec2(texelSize.x, 0.0)).r - texture(MaskSampler, uv - vec2(texelSize.x, 0.0)).r,
    texture(MaskSampler, uv + vec2(0.0, texelSize.y)).r - texture(MaskSampler, uv - vec2(0.0, texelSize.y)).r
    );
    return grad * 0.018;
}

// ---------------------------------------------------------------------------

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    float maskValue = texture(MaskSampler, texCoord).r;

    if (maskValue < 0.5) {
        fragColor = scene;
        return;
    }

    float saturation = resolution.z;
    float doReflect = resolution.w;
    float tintIntensity = settings.x;
    float edgeGlowIntensity = settings.y;

    // optional mirror flip, gives the slab a sense of thickness
    vec2 blurUV = texCoord;
    if (doReflect > 0.5) {
        vec2 offset = texCoord - vec2(0.5);
        blurUV = vec2(0.5) + offset * 0.7;
    }

    vec2 refraction = getRefraction(texCoord);

    // chromatic split, red sits closest to the surface, blue deepest
    vec3 blurChromatic = vec3(
    texture(BlurSampler, clamp(blurUV + refraction * 1.40, vec2(0.001), vec2(0.999))).r,
    texture(BlurSampler, clamp(blurUV + refraction * 1.00, vec2(0.001), vec2(0.999))).g,
    texture(BlurSampler, clamp(blurUV + refraction * 0.60, vec2(0.001), vec2(0.999))).b
    );
    vec3 blurBase = texture(BlurSampler, clamp(blurUV, vec2(0.001), vec2(0.999))).rgb;
    vec3 blurMix = mix(blurBase, blurChromatic, 0.50);

    vec3 glassColor = adjustSaturation(blurMix, saturation);

    if (tintIntensity > 0.001) {
        glassColor = mix(glassColor, tintColor.rgb, tintIntensity);
    }

    float edge = getEdge(texCoord);

    if (edgeGlowIntensity > 0.001) {
        vec3 glowColor = (tintColor.a < 0.01) ? vec3(1.0) : tintColor.rgb;
        glassColor += edge * glowColor * edgeGlowIntensity * 1.25;
    }

    // fresnel rim plus a faint internal sheen, both edge driven
    float fresnel = pow(edge, 1.8) * 0.35;
    glassColor += fresnel * 0.12;
    glassColor += pow(edge, 4.0) * 0.05 * vec3(0.85, 0.92, 1.0);

    // a couple of glints only along the rim, never inside the slab
    float glint = step(0.9965, hash12(floor(texCoord * resolution.xy * 0.85)));
    glassColor += glint * 0.06 * (0.3 + edge * 0.7);

    glassColor = clamp(glassColor, vec3(0.0), vec3(1.0));
    glassColor += (hash12(floor(texCoord * resolution.xy)) - 0.5) * (1.0 / 255.0);

    fragColor = vec4(clamp(glassColor, vec3(0.0), vec3(1.0)), 1.0);
}
