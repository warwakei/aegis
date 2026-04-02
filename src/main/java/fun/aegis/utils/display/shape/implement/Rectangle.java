package fun.aegis.utils.display.shape.implement;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.display.shape.Shape;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;

public class Rectangle implements Shape, QuickImports {
    private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("minecraft", "core/round"), VertexFormats.POSITION, Defines.EMPTY);

    @Override
    public void render(ShapeProperties shape) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        float scale = (float) mc.getWindow().getScaleFactor();
        float alpha = RenderSystem.getShaderColor()[3];

        Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
        Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0, new Vector3f()).mul(scale);
        Vector3f size = matrix4f.getScale(new Vector3f()).mul(scale);
        Vector4f round = shape.getRound().mul(size.y);

        float softness = shape.getSoftness() * alpha;
        float thickness = shape.getThickness();
        float width = shape.getWidth() * size.x;
        float height = shape.getHeight() * size.y;

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        drawEngine.quad(matrix4f, buffer, shape.getX() - softness / 2, shape.getY() - softness / 2, shape.getWidth() + softness, shape.getHeight() + softness);

        ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
        shader.getUniformOrDefault("size").set(width, height);
        shader.getUniformOrDefault("location").set(pos.x, window.getHeight() - height - pos.y);
        shader.getUniformOrDefault("radius").set(round);
        shader.getUniformOrDefault("softness").set(softness);
        shader.getUniformOrDefault("thickness").set(thickness);
        shader.getUniformOrDefault("color1").set(ColorAssist.redf(shape.getColor().x), ColorAssist.greenf(shape.getColor().x), ColorAssist.bluef(shape.getColor().x), ColorAssist.alphaf(ColorAssist.multAlpha(shape.getColor().x, alpha)));
        shader.getUniformOrDefault("color2").set(ColorAssist.redf(shape.getColor().y), ColorAssist.greenf(shape.getColor().y), ColorAssist.bluef(shape.getColor().y), ColorAssist.alphaf(ColorAssist.multAlpha(shape.getColor().y, alpha)));
        shader.getUniformOrDefault("color3").set(ColorAssist.redf(shape.getColor().z), ColorAssist.greenf(shape.getColor().z), ColorAssist.bluef(shape.getColor().z), ColorAssist.alphaf(ColorAssist.multAlpha(shape.getColor().z, alpha)));
        shader.getUniformOrDefault("color4").set(ColorAssist.redf(shape.getColor().w), ColorAssist.greenf(shape.getColor().w), ColorAssist.bluef(shape.getColor().w), ColorAssist.alphaf(ColorAssist.multAlpha(shape.getColor().w, alpha)));
        shader.getUniformOrDefault("outlineColor").set(ColorAssist.redf(shape.getOutlineColor()), ColorAssist.greenf(shape.getOutlineColor()), ColorAssist.bluef(shape.getOutlineColor()), ColorAssist.alphaf(ColorAssist.multAlpha(shape.getOutlineColor(), alpha)));

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }
}
