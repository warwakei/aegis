#version 150

in vec2 texCoord;

out vec4 fragColor;

uniform sampler2D BeforeSampler;
uniform sampler2D AfterSampler;
uniform sampler2D DepthBeforeSampler;
uniform sampler2D DepthAfterSampler;

layout(std140) uniform MaskData {
    vec4 params; // xy = target size
};

void main() {
    float depthBefore = texture(DepthBeforeSampler, texCoord).r;
    float depthAfter = texture(DepthAfterSampler, texCoord).r;

    // depth is the cheap path: anything drawn closer than the scene is geometry
    float depthDiff = depthBefore - depthAfter;
    bool isHand = depthDiff > 0.0001;

    // fallback for pipelines that do not write usable depth
    if (!isHand) {
        vec4 before = texture(BeforeSampler, texCoord);
        vec4 after = texture(AfterSampler, texCoord);
        vec3 colorDiff = abs(after.rgb - before.rgb);
        float maxColorDiff = max(max(colorDiff.r, colorDiff.g), colorDiff.b);
        isHand = maxColorDiff > 0.01;
    }

    float result = isHand ? 1.0 : 0.0;
    fragColor = vec4(result, result, result, 1.0);
}
