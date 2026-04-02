package fun.aegis.utils.display.systemrender.renderers.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.aegis.utils.display.systemrender.renderers.IRenderer;
import fun.aegis.utils.display.systemrender.builders.states.QuadColorState;
import fun.aegis.utils.display.systemrender.builders.states.QuadRadiusState;
import fun.aegis.utils.display.systemrender.builders.states.SizeState;
import fun.aegis.utils.display.shape.implement.Rectangle;
import fun.aegis.utils.display.shape.ShapeProperties;
import org.joml.Matrix4f;

public record BuiltLiquidGlass(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness,
        float cornerSmoothness,
        float globalAlpha,
        float fresnelPower,
        float fresnelColorR,
        float fresnelColorG,
        float fresnelColorB,
        float fresnelAlpha,
        float baseAlpha,
        boolean fresnelInvert,
        float fresnelMix,
        float distortStrength
) implements IRenderer {
    private static final Rectangle rectangle = new Rectangle();

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        float width = this.size.width();
        float height = this.size.height();
        
        // Use basic rectangle rendering as fallback
        rectangle.render(ShapeProperties.create(
            new net.minecraft.client.util.math.MatrixStack(), 
            x, y, width, height
        ).round(this.radius.radius1()).color(this.color.color1()).build());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
