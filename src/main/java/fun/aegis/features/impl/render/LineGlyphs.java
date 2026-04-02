package fun.aegis.features.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.aegis.events.player.TickEvent;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.ColorSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.display.color.ColorAssist;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class LineGlyphs extends Module {

    public static LineGlyphs getInstance() {
        return Instance.get(LineGlyphs.class);
    }

    final SliderSettings glyphsCount = new SliderSettings("Количество", "Количество глифов").setValue(70).range(10, 200);
    final SliderSettings speed = new SliderSettings("Скорость", "Скорость движения глифов").setValue(1.0f).range(0.1f, 3.0f);
    final SelectSetting colorMode = new SelectSetting("Режим цвета", "Режим окраски глифов")
            .value("Rainbow", "Client", "Picker", "DoublePicker").selected("Client");
    final ColorSetting pickColor1 = new ColorSetting("Цвет 1", "Первый цвет").value(0xFF64FF64)
            .visible(() -> colorMode.getSelected().contains("Picker"));
    final ColorSetting pickColor2 = new ColorSetting("Цвет 2", "Второй цвет").value(0xFF3C3CFF)
            .visible(() -> colorMode.isSelected("DoublePicker"));
    final SliderSettings spawnRadius = new SliderSettings("Радиус спавна", "Радиус появления глифов").setValue(15).range(6, 30);
    final SliderSettings lineWidth = new SliderSettings("Толщина линий", "Толщина линий глифов").setValue(2.0f).range(0.5f, 5.0f);

    final Identifier glowTexture = Identifier.of("textures/new_particles/glow.png");
    final Random rand = new Random();
    final List<GlyphVecGen> glyphVecGens = new ArrayList<>();
    float stateAnim = 0f;

    public LineGlyphs() {
        super("LineGlyphs", "LineGlyphs", ModuleCategory.RENDER);
        setup(glyphsCount, speed, colorMode, pickColor1, pickColor2, spawnRadius, lineWidth);
    }

    @Override
    public void activate() {
        super.activate();
        glyphVecGens.clear();
    }

    @Override
    public void deactivate() {
        super.deactivate();
        glyphVecGens.clear();
    }

    private int getStateColor(int index, float alphaPC) {
        int color;
        switch (colorMode.getSelected()) {
            case "Rainbow" -> color = ColorAssist.rainbow(10, index, 0.7f, 1.0f, 1.0f);
            case "Picker" -> color = pickColor1.getColor();
            case "DoublePicker" -> color = ColorAssist.fade(8, index, pickColor1.getColor(), pickColor2.getColor());
            default -> color = ColorAssist.fade(8, index, ColorAssist.getClientColor(), ColorAssist.getClientColor(0.5f));
        }
        return ColorAssist.replAlpha(color, (int) (255 * alphaPC));
    }


    private Vector3d randGlyphSpawnPos() {
        if (mc.player == null) return new Vector3d(0, 0, 0);

        int radius = spawnRadius.getInt();
        double dst = 6 + rand.nextDouble() * (radius - 6);
        double fov = mc.options.getFov().getValue();
        double radianYaw = Math.toRadians(mc.player.getYaw() - fov * 0.75 + rand.nextDouble() * fov * 1.5);

        double randXOff = -(Math.sin(radianYaw) * dst);
        double randYOff = -2 + rand.nextDouble() * 14;
        double randZOff = Math.cos(radianYaw) * dst;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        return new Vector3d(camPos.x + randXOff, camPos.y + randYOff, camPos.z + randZOff);
    }

    private void addAllGlyphs(int countCap) {
        while (glyphVecGens.size() < countCap) {
            Vector3d pos = randGlyphSpawnPos();
            int steps = 7 + rand.nextInt(6); // 7-12 steps
            glyphVecGens.add(new GlyphVecGen(pos, steps));
        }
    }

    private void glyphsRemoveAuto(float moduleAlphaPC) {
        glyphVecGens.removeIf(gen -> gen.isToRemove(moduleAlphaPC));
    }

    private void glyphsUpdate() {
        for (GlyphVecGen gen : glyphVecGens) {
            gen.update();
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        glyphsUpdate();
        addAllGlyphs(glyphsCount.getInt());
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null) return;

        float targetAnim = state ? 1.0f : 0.0f;
        stateAnim = MathHelper.lerp(0.1f, stateAnim, targetAnim);

        if (!state && stateAnim < 0.03f) {
            glyphVecGens.clear();
            return;
        }

        glyphsRemoveAuto(stateAnim);
        drawAllGlyphs(stateAnim, event.getPartialTicks());
    }

    private void drawAllGlyphs(float alphaPC, float pTicks) {
        if (glyphVecGens.isEmpty()) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth(lineWidth.getValue());
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // Draw all lines
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        int vertexCount = 0;

        int colorIndex = 0;
        for (GlyphVecGen gen : glyphVecGens) {
            float genAlpha = alphaPC * gen.getAlphaPC();
            if (genAlpha * 255 < 1) continue;

            List<Vector3d> positions = gen.getPosVectors(pTicks);
            if (positions.size() < 2) continue;

            for (int i = 0; i < positions.size() - 1; i++) {
                Vector3d p1 = positions.get(i);
                Vector3d p2 = positions.get(i + 1);

                float aPC1 = genAlpha * (0.3f + (float) i / positions.size() * 0.7f);
                float aPC2 = genAlpha * (0.3f + (float) (i + 1) / positions.size() * 0.7f);

                int color1 = getStateColor(colorIndex + i * 50, aPC1);
                int color2 = getStateColor(colorIndex + (i + 1) * 50, aPC2);

                buffer.vertex((float) (p1.x - camPos.x), (float) (p1.y - camPos.y), (float) (p1.z - camPos.z)).color(color1);
                buffer.vertex((float) (p2.x - camPos.x), (float) (p2.y - camPos.y), (float) (p2.z - camPos.z)).color(color2);
                vertexCount += 2;
            }
            colorIndex += 180;
        }

        if (vertexCount > 0) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        // Draw points/glow
        drawPoints(alphaPC, pTicks, camPos, camera);

        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
    }


    private void drawPoints(float alphaPC, float pTicks, Vec3d camPos, Camera camera) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, glowTexture);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int vertexCount = 0;

        int colorIndex = 0;
        for (GlyphVecGen gen : glyphVecGens) {
            float genAlpha = alphaPC * gen.getAlphaPC();
            if (genAlpha * 255 < 1) continue;

            List<Vector3d> positions = gen.getPosVectors(pTicks);

            for (int i = 0; i < positions.size(); i++) {
                Vector3d pos = positions.get(i);
                float aPC = genAlpha * (0.3f + (float) i / positions.size() * 0.7f);
                int color = getStateColor(colorIndex + i * 50, aPC);

                float size = 0.12f;

                MatrixStack matrices = new MatrixStack();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
                matrices.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

                Matrix4f m = matrices.peek().getPositionMatrix();

                buffer.vertex(m, -size, -size, 0).texture(0, 0).color(color);
                buffer.vertex(m, -size, size, 0).texture(0, 1).color(color);
                buffer.vertex(m, size, size, 0).texture(1, 1).color(color);
                buffer.vertex(m, size, -size, 0).texture(1, 0).color(color);
                vertexCount += 4;
            }
            colorIndex += 180;
        }

        if (vertexCount > 0) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    private class GlyphVecGen {
        final List<Vector3d> vecGens = new ArrayList<>();
        int currentStepTicks;
        int lastStepSet = 10;
        int stepsAmount;
        double yaw;
        double pitch;
        float alphaPC = 0.0f;
        float targetAlpha = 1.0f;
        final double stepLength;

        GlyphVecGen(Vector3d spawnPos, int maxStepsAmount) {
            vecGens.add(spawnPos);
            stepsAmount = maxStepsAmount;
            yaw = rand.nextDouble() * 360;
            pitch = (rand.nextDouble() - 0.5) * 180;
            stepLength = 1.5 + rand.nextDouble() * 2.0;
            currentStepTicks = 8 + rand.nextInt(8);
            lastStepSet = currentStepTicks;
        }

        void update() {
            // Smooth alpha animation
            alphaPC = MathHelper.lerp(0.08f, alphaPC, targetAlpha);

            if (stepsAmount <= 0) {
                targetAlpha = 0.0f;
                return;
            }

            if (currentStepTicks > 0) {
                currentStepTicks -= (int) Math.max(1, speed.getValue() * 2);
            } else {
                // Add new segment
                Vector3d lastPos = vecGens.get(vecGens.size() - 1);

                // Change direction by 90 degrees
                if (rand.nextBoolean()) {
                    yaw += rand.nextBoolean() ? 90 : -90;
                } else {
                    pitch += rand.nextBoolean() ? 90 : -90;
                    pitch = MathHelper.clamp(pitch, -90, 90);
                }

                double yawRad = Math.toRadians(yaw);
                double pitchRad = Math.toRadians(pitch);

                double dx = -Math.sin(yawRad) * Math.cos(pitchRad) * stepLength;
                double dy = Math.sin(pitchRad) * stepLength;
                double dz = Math.cos(yawRad) * Math.cos(pitchRad) * stepLength;

                vecGens.add(new Vector3d(lastPos.x + dx, lastPos.y + dy, lastPos.z + dz));

                currentStepTicks = 8 + rand.nextInt(8);
                lastStepSet = currentStepTicks;
                stepsAmount--;
            }
        }

        List<Vector3d> getPosVectors(float pTicks) {
            if (vecGens.size() < 2) return vecGens;

            List<Vector3d> result = new ArrayList<>(vecGens);

            // Interpolate last point for smooth animation
            float progress = 1.0f - (float) currentStepTicks / lastStepSet;
            progress = MathHelper.clamp(progress, 0, 1);

            if (result.size() >= 2) {
                Vector3d prev = result.get(result.size() - 2);
                Vector3d last = result.get(result.size() - 1);
                double x = MathHelper.lerp(progress, prev.x, last.x);
                double y = MathHelper.lerp(progress, prev.y, last.y);
                double z = MathHelper.lerp(progress, prev.z, last.z);
                result.set(result.size() - 1, new Vector3d(x, y, z));
            }

            return result;
        }

        float getAlphaPC() {
            return MathHelper.clamp(alphaPC, 0.0f, 1.0f);
        }

        boolean isToRemove(float moduleAlphaPC) {
            return targetAlpha == 0.0f && alphaPC < 0.02f;
        }
    }
}
