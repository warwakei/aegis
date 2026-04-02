package fun.aegis.utils.display.shape.implement;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.aegis.utils.display.atlasfont.providers.ResourceProvider;
import fun.aegis.utils.display.shape.Shape;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.interfaces.QuickImports;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

public class LiquidGlass implements Shape, QuickImports {
    
    private static final ShaderProgramKey LIQUID_GLASS_SHADER_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("liquidglass"),
            VertexFormats.POSITION_COLOR,
            Defines.EMPTY
    );

    // Default shader parameters
    private float smoothness = 1.0f;
    private float cornerSmoothness = 2.0f;
    private float globalAlpha = 1.0f;
    private float fresnelPower = 2.0f;
    private float fresnelColorR = 1.0f;
    private float fresnelColorG = 1.0f;
    private float fresnelColorB = 1.0f;
    private float fresnelAlpha = 0.8f;
    private float baseAlpha = 0.2f;
    private boolean fresnelInvert = false;
    private float fresnelMix = 1.0f;
    private float distortStrength = 0.03f;

    @Override
    public void render(ShapeProperties shape) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
        float width = shape.getWidth();
        float height = shape.getHeight();
        float x = shape.getX();
        float y = shape.getY();
        float z = 0;

        ShaderProgram shader = RenderSystem.setShader(LIQUID_GLASS_SHADER_KEY);
        
        // Set shader uniforms
        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(
            shape.getRound().x, shape.getRound().y, 
            shape.getRound().z, shape.getRound().w
        );
        shader.getUniform("Smoothness").set(this.smoothness);
        shader.getUniform("CornerSmoothness").set(this.cornerSmoothness);
        shader.getUniform("GlobalAlpha").set(this.globalAlpha);
        shader.getUniform("FresnelPower").set(this.fresnelPower);
        shader.getUniform("FresnelColor").set(this.fresnelColorR, this.fresnelColorG, this.fresnelColorB);
        shader.getUniform("FresnelAlpha").set(this.fresnelAlpha);
        shader.getUniform("BaseAlpha").set(this.baseAlpha);
        shader.getUniform("FresnelInvert").set(this.fresnelInvert ? 1 : 0);
        shader.getUniform("FresnelMix").set(this.fresnelMix);
        shader.getUniform("DistortStrength").set(this.distortStrength);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        // Draw quad with vertex colors
        buffer.vertex(matrix4f, x, y, z).color(shape.getColor().x);
        buffer.vertex(matrix4f, x, y + height, z).color(shape.getColor().y);
        buffer.vertex(matrix4f, x + width, y + height, z).color(shape.getColor().z);
        buffer.vertex(matrix4f, x + width, y, z).color(shape.getColor().w);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // Setter methods for shader parameters
    public LiquidGlass setSmoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    public LiquidGlass setCornerSmoothness(float cornerSmoothness) {
        this.cornerSmoothness = cornerSmoothness;
        return this;
    }

    public LiquidGlass setGlobalAlpha(float globalAlpha) {
        this.globalAlpha = globalAlpha;
        return this;
    }

    public LiquidGlass setFresnelPower(float fresnelPower) {
        this.fresnelPower = fresnelPower;
        return this;
    }

    public LiquidGlass setFresnelColor(float r, float g, float b) {
        this.fresnelColorR = r;
        this.fresnelColorG = g;
        this.fresnelColorB = b;
        return this;
    }

    public LiquidGlass setFresnelAlpha(float fresnelAlpha) {
        this.fresnelAlpha = fresnelAlpha;
        return this;
    }

    public LiquidGlass setBaseAlpha(float baseAlpha) {
        this.baseAlpha = baseAlpha;
        return this;
    }

    public LiquidGlass setFresnelInvert(boolean fresnelInvert) {
        this.fresnelInvert = fresnelInvert;
        return this;
    }

    public LiquidGlass setFresnelMix(float fresnelMix) {
        this.fresnelMix = fresnelMix;
        return this;
    }

    public LiquidGlass setDistortStrength(float distortStrength) {
        this.distortStrength = distortStrength;
        return this;
    }
}
