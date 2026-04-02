package fun.aegis.utils.display.shape;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import org.joml.Vector4i;

@Builder
@Getter
@Setter
public class ShapeProperties {
    private MatrixStack matrix;
    private float x, y, width, height;
    private float softness, thickness;
    private float start, end;

    @Builder.Default
    private float quality = 20;

    private Vector4f round;

    @Builder.Default
    private int outlineColor = -1;
    private Vector4i color;

    @Builder(toBuilder = true)
    private ShapeProperties(MatrixStack matrix, float x, float y, float width, float height, float softness, float thickness, float start, float end, float quality, Vector4f round, int outlineColor, Vector4i color) {
        this.matrix = matrix;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.softness = softness;
        this.thickness = thickness;
        this.round = round != null ? round : new Vector4f(0);
        this.outlineColor = outlineColor;
        this.color = color != null ? color : new Vector4i(-1);
        this.start = start;
        this.end = end;
        this.quality = quality;
    }

    public static class ShapePropertiesBuilder {

        public ShapePropertiesBuilder color(int color) {
            this.color = new Vector4i(color);
            return this;
        }

        public ShapePropertiesBuilder color(Vector4i color) {
            this.color = color;
            return this;
        }

        public ShapePropertiesBuilder color(int... color) {
            this.color = new Vector4i(color);
            return this;
        }

        public ShapePropertiesBuilder round(float round) {
            this.round = new Vector4f(round);
            return this;
        }

        public ShapePropertiesBuilder round(Vector4f round) {
            this.round = new Vector4f(round);
            return this;
        }

        public ShapePropertiesBuilder round(float... round) {
            this.round = new Vector4f(round);
            return this;

        }
    }

    public static ShapeProperties.ShapePropertiesBuilder create(MatrixStack matrix, double x, double y, double width, double height) {
        return ShapeProperties.builder().matrix(matrix).x((float) x).y((float) y).width((float) width).height((float) height);
    }
}
