package rich.modules.impl.render.worldparticles;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import rich.util.render.сliemtpipeline.ClientPipelines;

public class ParticleRenderer {

    private static final Identifier GLOW_TEXTURE = Identifier.of("rich", "textures/world/dashbloom.png");
    private static final Identifier GLOW_TEXTURE_SECONDARY = Identifier.of("rich", "textures/world/dashbloomsample.png");

    public static RenderLayer getQuadsLayer() {
        return ClientPipelines.WORLD_PARTICLES_QUADS;
    }

    public static RenderLayer getLinesLayer() {
        return ClientPipelines.WORLD_PARTICLES_LINES;
    }

    public static RenderLayer getGlowLayer() {
        return ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_TEXTURE);
    }

    public static RenderLayer getGlowLayerSecondary() {
        return ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_TEXTURE_SECONDARY);
    }

    public static void drawCube(VertexConsumer b, Matrix4f m, int color, float s) {
        float h = s / 2.0F;
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int bl = color & 0xFF;
        int a = color >> 24 & 0xFF;

        b.vertex(m, -h, h, -h).color(r, g, bl, a);
        b.vertex(m, -h, h, h).color(r, g, bl, a);
        b.vertex(m, h, h, h).color(r, g, bl, a);
        b.vertex(m, h, h, -h).color(r, g, bl, a);

        b.vertex(m, -h, -h, -h).color(r, g, bl, a);
        b.vertex(m, h, -h, -h).color(r, g, bl, a);
        b.vertex(m, h, -h, h).color(r, g, bl, a);
        b.vertex(m, -h, -h, h).color(r, g, bl, a);

        b.vertex(m, -h, h, h).color(r, g, bl, a);
        b.vertex(m, -h, -h, h).color(r, g, bl, a);
        b.vertex(m, h, -h, h).color(r, g, bl, a);
        b.vertex(m, h, h, h).color(r, g, bl, a);

        b.vertex(m, -h, h, -h).color(r, g, bl, a);
        b.vertex(m, h, h, -h).color(r, g, bl, a);
        b.vertex(m, h, -h, -h).color(r, g, bl, a);
        b.vertex(m, -h, -h, -h).color(r, g, bl, a);

        b.vertex(m, -h, h, -h).color(r, g, bl, a);
        b.vertex(m, -h, -h, -h).color(r, g, bl, a);
        b.vertex(m, -h, -h, h).color(r, g, bl, a);
        b.vertex(m, -h, h, h).color(r, g, bl, a);

        b.vertex(m, h, h, -h).color(r, g, bl, a);
        b.vertex(m, h, h, h).color(r, g, bl, a);
        b.vertex(m, h, -h, h).color(r, g, bl, a);
        b.vertex(m, h, -h, -h).color(r, g, bl, a);
    }

    public static void drawLines(VertexConsumer b, Matrix4f m, int c, float s) {
        float h = s / 2.0F;
        int r = c >> 16 & 0xFF;
        int g = c >> 8 & 0xFF;
        int bl = c & 0xFF;
        int a = c >> 24 & 0xFF;

        line(b, m, -h, -h, -h, h, -h, -h, r, g, bl, a);
        line(b, m, h, -h, -h, h, -h, h, r, g, bl, a);
        line(b, m, h, -h, h, -h, -h, h, r, g, bl, a);
        line(b, m, -h, -h, h, -h, -h, -h, r, g, bl, a);
        line(b, m, -h, h, -h, h, h, -h, r, g, bl, a);
        line(b, m, h, h, -h, h, h, h, r, g, bl, a);
        line(b, m, h, h, h, -h, h, h, r, g, bl, a);
        line(b, m, -h, h, h, -h, h, -h, r, g, bl, a);
        line(b, m, -h, -h, -h, -h, h, -h, r, g, bl, a);
        line(b, m, h, -h, -h, h, h, -h, r, g, bl, a);
        line(b, m, h, -h, h, h, h, h, r, g, bl, a);
        line(b, m, -h, -h, h, -h, h, h, r, g, bl, a);
    }

    private static void line(VertexConsumer b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int bl, int a) {
        b.vertex(m, x1, y1, z1).color(r, g, bl, a);
        b.vertex(m, x2, y2, z2).color(r, g, bl, a);
    }

    public static void drawGlow(VertexConsumer buffer, Matrix4f matrix, int color, int alpha, float size) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        float half = size / 2.0f;
        buffer.vertex(matrix, -half, -half, 0).texture(0, 0).color(r, g, b, alpha);
        buffer.vertex(matrix, -half, half, 0).texture(0, 1).color(r, g, b, alpha);
        buffer.vertex(matrix, half, half, 0).texture(1, 1).color(r, g, b, alpha);
        buffer.vertex(matrix, half, -half, 0).texture(1, 0).color(r, g, b, alpha);
    }
}