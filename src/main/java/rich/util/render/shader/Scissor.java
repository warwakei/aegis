package rich.util.render.shader;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;

public class Scissor {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Deque<int[]> scissorStack = new ArrayDeque<>();

    public static void enable(float x, float y, float width, float height, float guiScale) {
        int windowHeight = mc.getWindow().getHeight();

        int scissorX = (int) (x * guiScale);
        int scissorY = (int) (windowHeight - (y + height) * guiScale);
        int scissorWidth = (int) (width * guiScale);
        int scissorHeight = (int) (height * guiScale);

        scissorX = Math.max(0, scissorX);
        scissorY = Math.max(0, scissorY);
        scissorWidth = Math.max(0, scissorWidth);
        scissorHeight = Math.max(0, scissorHeight);

        if (!scissorStack.isEmpty()) {
            int[] parent = scissorStack.peek();
            int parentX = parent[0];
            int parentY = parent[1];
            int parentX2 = parentX + parent[2];
            int parentY2 = parentY + parent[3];

            int newX2 = scissorX + scissorWidth;
            int newY2 = scissorY + scissorHeight;

            scissorX = Math.max(scissorX, parentX);
            scissorY = Math.max(scissorY, parentY);
            newX2 = Math.min(newX2, parentX2);
            newY2 = Math.min(newY2, parentY2);

            scissorWidth = Math.max(0, newX2 - scissorX);
            scissorHeight = Math.max(0, newY2 - scissorY);
        }

        scissorStack.push(new int[]{scissorX, scissorY, scissorWidth, scissorHeight});

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    public static void enable(float x, float y, float width, float height) {
        int currentGuiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        enable(x, y, width, height, currentGuiScale);
    }

    public static void disable() {
        if (!scissorStack.isEmpty()) {
            scissorStack.pop();
        }

        if (scissorStack.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            int[] parent = scissorStack.peek();
            GL11.glScissor(parent[0], parent[1], parent[2], parent[3]);
        }
    }

    public static void reset() {
        scissorStack.clear();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}