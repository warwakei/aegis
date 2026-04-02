package fun.aegis.utils.display.systemrender.renderers.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.aegis.utils.display.atlasfont.msdf.MsdfFont;
import fun.aegis.utils.display.atlasfont.providers.ColorProvider;
import fun.aegis.utils.display.atlasfont.providers.ResourceProvider;
import fun.aegis.utils.display.systemrender.renderers.IRenderer;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;



public record BuiltText(
        MsdfFont font,
        String text,
    	float size,
        float thickness,
        int color,
		float smoothness,
        float spacing,
		int outlineColor,
		float outlineThickness
    ) implements IRenderer {

	private static final ShaderProgramKey MSDF_FONT_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("msdf_font"),
		VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
	
	@Override
    public void render(Matrix4f matrix, float x, float y, float z) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();

		RenderSystem.setShaderTexture(0, this.font.getTextureId());
		
		boolean outlineEnabled = (this.outlineThickness > 0.0f);
		ShaderProgram shader = RenderSystem.setShader(MSDF_FONT_SHADER_KEY);
		shader.getUniform("Range").set(this.font.getAtlas().range());
		shader.getUniform("Thickness").set(this.thickness);
		shader.getUniform("Smoothness").set(this.smoothness);
		shader.getUniform("Outline").set(outlineEnabled ? 1 : 0);

		if (outlineEnabled) {
			shader.getUniform("OutlineThickness").set(this.outlineThickness);
			float[] outlineComponents = ColorProvider.normalize(this.outlineColor);
			shader.getUniform("OutlineColor").set(outlineComponents[0], outlineComponents[1], 
				outlineComponents[2], outlineComponents[3]);
		}
		
		BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
		this.font.applyGlyphs(matrix, builder, this.text, this.size,
			(this.thickness + this.outlineThickness * 0.5f) * 0.5f * this.size, this.spacing,
				x, y + this.font.getMetrics().baselineHeight() * this.size, z, this.color);
		
		BufferRenderer.drawWithGlobalProgram(builder.end());

		RenderSystem.setShaderTexture(0, 0);

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

}
