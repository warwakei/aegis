package fun.aegis.utils.display.scissor;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import fun.aegis.utils.display.interfaces.QuickImports;

import java.util.Stack;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScissorAssist implements QuickImports {
    Pool<Scissor> scissorPool = new Pool<>(Scissor::new);
    Stack<Scissor> scissorStack = new Stack<>();

    public void push(Matrix4f matrix4f, float x, float y, float width, float height) {
        Scissor currentScissor = scissorPool.get();

        Vector3f pos = matrix4f.transformPosition(x, y, 0, new Vector3f());
        Vector3f size = matrix4f.getScale(new Vector3f()).mul(width, height, 0);

        currentScissor.set(pos.x, pos.y, size.x, size.y);

        if (!scissorStack.isEmpty()) {
            Scissor parent = scissorStack.peek();
            currentScissor.intersect(parent);
        }

        scissorStack.push(currentScissor);
        setScissor(currentScissor);
    }

    public void pop() {
        if (!scissorStack.isEmpty()) {
            scissorPool.free(scissorStack.pop());
            if (scissorStack.isEmpty()) {
                RenderSystem.disableScissor();
            } else {
                setScissor(scissorStack.peek());
            }
        }
    }

    private void setScissor(Scissor scissor) {
        int scaleFactor = (int) window.getScaleFactor();
        int x = scissor.x * scaleFactor;
        int y = window.getHeight() - (scissor.y * scaleFactor + scissor.height * scaleFactor);
        int width = scissor.width * scaleFactor;
        int height = scissor.height * scaleFactor;

        RenderSystem.enableScissor(x, y, width, height);
    }

    private static class Scissor {
        public int x, y;
        public int width, height;

        public void set(double x, double y, double width, double height) {
            this.x = Math.max(0, (int) Math.round(x));
            this.y = Math.max(0, (int) Math.round(y));
            this.width = Math.max(0, (int) Math.round(width));
            this.height = Math.max(0, (int) Math.round(height));
        }

        public void intersect(Scissor parent) {
            int x1 = Math.max(this.x, parent.x);
            int y1 = Math.max(this.y, parent.y);
            int x2 = Math.min(this.x + this.width, parent.x + parent.width);
            int y2 = Math.min(this.y + this.height, parent.y + parent.height);

            this.x = x1;
            this.y = y1;
            this.width = Math.max(0, x2 - x1);
            this.height = Math.max(0, y2 - y1);
        }

        Scissor copy() {
            Scissor newScissor = new Scissor();
            newScissor.set(this.x, this.y, this.width, this.height);
            return newScissor;
        }
    }
}
