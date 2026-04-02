package fun.aegis.utils.display.systemrender.renderers.impl;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.aegis.utils.display.atlasfont.providers.ResourceProvider;
import fun.aegis.utils.display.systemrender.renderers.IRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import fun.aegis.utils.display.systemrender.builders.states.QuadColorState;
import fun.aegis.utils.display.systemrender.builders.states.QuadRadiusState;
import fun.aegis.utils.display.systemrender.builders.states.SizeState;

public record BuiltBlur(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness,
        float blurRadius
) implements IRenderer {

    private static final ShaderProgramKey BLUR_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("blur"),
            VertexFormats.POSITION_COLOR, Defines.EMPTY);

    private static final Supplier<SimpleFramebuffer> TEMP_FBO_SUPPLIER = Suppliers
            .memoize(() -> new SimpleFramebuffer(1, 1, true));

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        Framebuffer mainFbo = MinecraftClient.getInstance().getFramebuffer();

        if (mainFbo == null || mainFbo.textureWidth <= 0 || mainFbo.textureHeight <= 0) {
            return;
        }

        SimpleFramebuffer tempFbo = TEMP_FBO_SUPPLIER.get();

        if (tempFbo.textureWidth != mainFbo.textureWidth || tempFbo.textureHeight != mainFbo.textureHeight) {
            tempFbo.resize(mainFbo.textureWidth, mainFbo.textureHeight);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        tempFbo.beginWrite(false);
        mainFbo.draw(mainFbo.textureWidth, mainFbo.textureHeight);

        mainFbo.beginWrite(false);

        RenderSystem.setShaderTexture(0, tempFbo.getColorAttachment());

        float width = this.size.width();
        float height = this.size.height();
        ShaderProgram shader = RenderSystem.setShader(BLUR_SHADER_KEY);
        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(),
                this.radius.radius3(), this.radius.radius4());
        shader.getUniform("Smoothness").set(this.smoothness);
        shader.getUniform("BlurRadius").set(this.blurRadius);

        BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, x, y, z).color(this.color.color1());
        builder.vertex(matrix, x, y + height, z).color(this.color.color2());
        builder.vertex(matrix, x + width, y + height, z).color(this.color.color3());
        builder.vertex(matrix, x + width, y, z).color(this.color.color4());

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.setShaderTexture(0, 0);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
