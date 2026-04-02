package fun.aegis.features.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.aegis.features.impl.combat.TriggerBot;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.managers.event.types.EventType;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.events.player.RotationUpdateEvent;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.Aegis;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.calc.CalcVector;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.math.time.StopWatch;

import fun.aegis.utils.display.geometry.Render3D;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.ColorSetting;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import net.minecraft.util.Identifier;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetESP extends Module {

    public static TargetESP getInstance() {
        return Instance.get(TargetESP.class);
    }

    Animation esp_anim = new Decelerate().setMs(400).setValue(1);
    SelectSetting targetEspType = new SelectSetting("Отображения таргета", "Выбирает тип цели esp")
            .value("Cube", "Circle", "Ghosts", "Crystals", "Rhombus", "Helix", "Pulse", "Stars", "Spiral Crystals", "Crystals V2")
            .selected("Circle");
    SelectSetting cubeType = new SelectSetting("Картинка для куба", "Выбирает тип куба")
            .value("1", "2", "3", "4", "5")
            .visible(() -> targetEspType.isSelected("Cube"));
    public ColorSetting colorSetting = new ColorSetting("Цвет TargetESP", "Выберите цвет для TargetESP");

    public TargetESP() {
        super("TargetEsp", "Target Esp", ModuleCategory.RENDER);
        setup(targetEspType, cubeType, colorSetting);
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() == EventType.POST) {
            Render3D.updateTargetEsp();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        StrikeManager attackHandler = Aegis.getInstance().getAttackPerpetrator().getAttackHandler();
        StopWatch attackTimer = attackHandler.getAttackTimer();

        LivingEntity currentTarget = null;
        LivingEntity lastTarget = null;

        if (Aura.getInstance().isState()) {
            currentTarget = Aura.getInstance().getTarget();
            lastTarget = Aura.getInstance().getLastTarget();
        } else if (TriggerBot.getInstance().isState()) {
            currentTarget = TriggerBot.getInstance().target;
            lastTarget = TriggerBot.getInstance().target;
        }

        esp_anim.setDirection(currentTarget != null ? Direction.FORWARDS : Direction.BACKWARDS);
        float anim = esp_anim.getOutput().floatValue();

        if (lastTarget != null && !esp_anim.isFinished(Direction.BACKWARDS)) {
            float red = MathHelper.clamp((lastTarget.hurtTime - tickCounter.getTickDelta(false)) / 20, 0, 1);
            switch (targetEspType.getSelected()) {
                case "Cube" -> Render3D.drawCube(lastTarget, anim, red, cubeType.getSelected());
                case "Circle" -> Render3D.drawCircle(e.getStack(), lastTarget, anim, red);
                case "Ghosts" -> Render3D.drawGhosts(lastTarget, anim, red, 0.62F);
                case "Crystals" -> {
                    if (crystalList.isEmpty() || lastTarget != lastRenderedTarget) {
                        createCrystals(lastTarget);
                        lastRenderedTarget = lastTarget;
                    }
                    renderCrystals(e.getStack(), lastTarget, anim, red);
                }
                case "Rhombus" -> renderRhombus(e.getStack(), lastTarget, anim, red);
                case "Helix" -> renderHelix(e.getStack(), lastTarget, anim, red);
                case "Pulse" -> renderPulse(e.getStack(), lastTarget, anim, red);
                case "Stars" -> renderStars(e.getStack(), lastTarget, anim, red);
                case "Spiral Crystals" -> renderSpiralCrystals(e.getStack(), lastTarget, anim, red);
                case "Crystals V2" -> renderCrystalsV2(e.getStack(), lastTarget, anim, red);
            }
        }
    }
    private Entity lastRenderedTarget = null;
    private final List<AnimatedCrystal> crystalList = new ArrayList<>();
    private float rotationAngle = 0;
    private long crystalSpawnTime = 0;

    private static class AnimatedCrystal {
        private final Vec3d targetPosition;
        private final Vec3d spawnDirection;
        private final float spawnDistance;
        private final float size;
        private final float spawnDelay;

        public AnimatedCrystal(Vec3d targetPosition, float spawnDelay) {
            this.targetPosition = targetPosition;
            this.size = 0.065f;
            this.spawnDelay = spawnDelay;
            
            double angle = Math.random() * Math.PI * 2;
            double vertAngle = (Math.random() - 0.5) * Math.PI;
            this.spawnDirection = new Vec3d(
                Math.cos(angle) * Math.cos(vertAngle),
                Math.sin(vertAngle),
                Math.sin(angle) * Math.cos(vertAngle)
            );
            this.spawnDistance = 2.0f + (float)(Math.random() * 1.5f);
        }

        public void render(MatrixStack ms, float anim, float red, float hitPull, Camera camera, float globalYawDegrees, long timeSinceSpawn) {
            float spawnProgress = Math.min(1.0f, Math.max(0f, (timeSinceSpawn - spawnDelay * 150) / 400f));
            float easeProgress = 1.0f - (1.0f - spawnProgress) * (1.0f - spawnProgress);
            
            if (spawnProgress <= 0) return;
            
            float disappearProgress = 1.0f - anim;
            float disappearEase = disappearProgress * disappearProgress;
            
            float finalProgress = easeProgress * (1.0f - disappearEase);
            
            if (finalProgress <= 0.01f) return;
            
            Vec3d currentPos = new Vec3d(
                targetPosition.x + spawnDirection.x * spawnDistance * (1.0f - finalProgress),
                targetPosition.y + spawnDirection.y * spawnDistance * (1.0f - finalProgress),
                targetPosition.z + spawnDirection.z * spawnDistance * (1.0f - finalProgress)
            );
            
            ms.push();
            float pullK = 1.0f - (hitPull * 0.25f);
            ms.translate(currentPos.x * pullK, currentPos.y, currentPos.z * pullK);

            double dx = -currentPos.x;
            double dz = -currentPos.z;
            float yawToCenter = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;

            float centerY = 1.2f;
            float pitch;
            if (currentPos.y < centerY - 0.2f) {
                pitch = 60.0f;
            } else if (currentPos.y > centerY + 0.2f) {
                pitch = 120.0f;
            } else {
                pitch = 90.0f;
            }

            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yawToCenter));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
            
            float scaleAnim = finalProgress;
            ms.scale(scaleAnim, scaleAnim, scaleAnim);
            
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            int baseColor = ColorAssist.interpolateColor(ColorAssist.getClientColor(), new Color(255, 0, 0).getRGB(), red);

            float crystalAnim = finalProgress;
            
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            drawCrystal(ms, baseColor, 0.2f, true, crystalAnim);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            drawCrystal(ms, baseColor, 0.3f, true, crystalAnim);
            drawCrystal(ms, baseColor, 0.8f, false, crystalAnim);

            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            ms.push();
            ms.scale(1.2f, 1.2f, 1.2f);
            drawCrystal(ms, baseColor, 0.3f, true, crystalAnim);
            ms.pop();
            drawBloomSphere(ms, baseColor, crystalAnim, camera);
            RenderSystem.depthMask(true);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            ms.pop();
        }

        private void drawBloomSphere(MatrixStack ms, int baseColor, float anim, Camera camera) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, Identifier.of("textures/features/particles/bloom.png"));
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.depthMask(false);
            int bloomColor = ColorAssist.setAlpha(baseColor, (int) (0.4f * 25 * anim));
            float bloomSize = size * 13.0f;
            float pitch = camera.getPitch();
            float yaw = camera.getYaw();
            int segments = 6;
            for (int i = 0; i < segments; i++) {
                ms.push();
                float angle = (360.0f / segments) * i;
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                Matrix4f matrix = ms.peek().getPositionMatrix();
                BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bufferBuilder.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                ms.pop();
            }
            for (int i = 0; i < segments; i++) {
                ms.push();
                float angle = (360.0f / segments) * i;
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                Matrix4f matrix = ms.peek().getPositionMatrix();
                BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bufferBuilder.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                ms.pop();
            }
            RenderSystem.depthMask(true);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        }

        private void drawCrystal(MatrixStack ms, int baseColor, float alpha, boolean filled, float anim) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

            float s = size;
            float h = size * 2.2f;
            int finalColor = ColorAssist.setAlpha(baseColor, (int) (alpha * 255 * anim));

            Vec3d vTop = new Vec3d(0, h, 0);
            Vec3d vBottom = new Vec3d(0, -h, 0);

            Vec3d[] mid = new Vec3d[] {
                    new Vec3d(s, 0, 0),
                    new Vec3d(0, 0, s),
                    new Vec3d(-s, 0, 0),
                    new Vec3d(0, 0, -s)
            };

            for (int i = 0; i < 4; i++) {
                Vec3d a = mid[i];
                Vec3d b = mid[(i + 1) % 4];
                drawTriangle(ms, bufferBuilder, vTop, a, b, finalColor, true);
            }

            for (int i = 0; i < 4; i++) {
                Vec3d a = mid[(i + 1) % 4];
                Vec3d b = mid[i];
                drawTriangle(ms, bufferBuilder, vBottom, a, b, finalColor, true);
            }

            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        }

        private void drawTriangle(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, int color, boolean filled) {
            if (filled) {
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
            }
        }
    }

    private void createCrystals(Entity target) {
        crystalList.clear();
        int index = 0;

        crystalList.add(new AnimatedCrystal(new Vec3d(0.60, 1.20, 0.00), index++));
        crystalList.add(new AnimatedCrystal(new Vec3d(-0.60, 1.20, 0.00), index++));
        crystalList.add(new AnimatedCrystal(new Vec3d(0.00, 1.20, 0.60), index++));
        crystalList.add(new AnimatedCrystal(new Vec3d(0.00, 1.20, -0.60), index++));

        crystalList.add(new AnimatedCrystal(new Vec3d(0.68, 1.75, 0.00), index++));
        crystalList.add(new AnimatedCrystal(new Vec3d(-0.68, 1.75, 0.00), index++));
        crystalList.add(new AnimatedCrystal(new Vec3d(0.20, 1.75, 0.68), index++));
        crystalList.add(new AnimatedCrystal(new Vec3d(-0.20, 1.75, 0.60), index++));
        crystalList.add(new AnimatedCrystal(new Vec3d(0.00, 1.75, -0.60), index++));

        crystalList.add(new AnimatedCrystal(new Vec3d(0.68, 0.65, 0.00), index++));
        crystalList.add(new AnimatedCrystal(new Vec3d(-0.68, 0.65, -0.00), index++));
        crystalList.add(new AnimatedCrystal(new Vec3d(0.20, 0.65, -0.60), index++));
        crystalList.add(new AnimatedCrystal(new Vec3d(-0.20, 0.65, -0.60), index++));
        crystalList.add(new AnimatedCrystal(new Vec3d(0.00, 0.65, 0.72), index++));
        
        crystalSpawnTime = System.currentTimeMillis();
    }

    private void renderCrystals(MatrixStack ms, Entity target, float anim, float red) {
        if (target == null || crystalList.isEmpty()) {
            return;
        }
        RenderSystem.enableDepthTest();
        Vec3d targetPos = CalcVector.lerpPosition(target);
        rotationAngle = (rotationAngle + 0.5f) % 360;

        float hitPull = MathHelper.clamp(red, 0.0f, 1.0f);
        hitPull = hitPull * hitPull;
        
        long timeSinceSpawn = System.currentTimeMillis() - crystalSpawnTime;

        ms.push();
        ms.translate(targetPos.x, targetPos.y, targetPos.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));
        Camera camera = mc.gameRenderer.getCamera();
        for (AnimatedCrystal crystal : crystalList) {
            crystal.render(ms, anim, red, hitPull, camera, rotationAngle, timeSinceSpawn);
        }
        ms.pop();
        RenderSystem.enableDepthTest();
    }

    private float rhombusRotation = 0;
    
    private void renderRhombus(MatrixStack ms, LivingEntity target, float anim, float red) {
        Vec3d targetPos = CalcVector.lerpPosition(target);
        rhombusRotation = (rhombusRotation + 2.0f) % 360;
        
        float hitEffect = (float) Math.sin(red * Math.PI) * 0.3f;
        float scale = anim * (1.0f + hitEffect);
        
        int baseColor = ColorAssist.interpolateColor(colorSetting.getColor(), new Color(255, 50, 50).getRGB(), red);
        
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        
        ms.push();
        ms.translate(targetPos.x, targetPos.y + target.getHeight() / 2, targetPos.z);
        
        for (int i = 0; i < 4; i++) {
            ms.push();
            float angle = rhombusRotation + (i * 90);
            float radius = 0.8f * scale;
            float yOffset = (float) Math.sin(Math.toRadians(rhombusRotation * 2 + i * 45)) * 0.3f;
            
            double x = Math.cos(Math.toRadians(angle)) * radius;
            double z = Math.sin(Math.toRadians(angle)) * radius;
            
            ms.translate(x, yOffset, z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rhombusRotation));
            ms.scale(scale * 0.15f, scale * 0.15f, scale * 0.15f);
            
            drawRhombusShape(ms, baseColor, anim);
            ms.pop();
        }
        
        ms.pop();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
    
    private void drawRhombusShape(MatrixStack ms, int color, float anim) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = ms.peek().getPositionMatrix();
        int c = ColorAssist.setAlpha(color, (int)(200 * anim));
        
        buffer.vertex(matrix, 0, 1, 0).color(c);
        buffer.vertex(matrix, 0.5f, 0, 0.5f).color(c);
        buffer.vertex(matrix, 0.5f, 0, -0.5f).color(c);
        
        buffer.vertex(matrix, 0, 1, 0).color(c);
        buffer.vertex(matrix, 0.5f, 0, -0.5f).color(c);
        buffer.vertex(matrix, -0.5f, 0, -0.5f).color(c);
        
        buffer.vertex(matrix, 0, 1, 0).color(c);
        buffer.vertex(matrix, -0.5f, 0, -0.5f).color(c);
        buffer.vertex(matrix, -0.5f, 0, 0.5f).color(c);
        
        buffer.vertex(matrix, 0, 1, 0).color(c);
        buffer.vertex(matrix, -0.5f, 0, 0.5f).color(c);
        buffer.vertex(matrix, 0.5f, 0, 0.5f).color(c);
        
        buffer.vertex(matrix, 0, -1, 0).color(c);
        buffer.vertex(matrix, 0.5f, 0, -0.5f).color(c);
        buffer.vertex(matrix, 0.5f, 0, 0.5f).color(c);
        
        buffer.vertex(matrix, 0, -1, 0).color(c);
        buffer.vertex(matrix, -0.5f, 0, -0.5f).color(c);
        buffer.vertex(matrix, 0.5f, 0, -0.5f).color(c);
        
        buffer.vertex(matrix, 0, -1, 0).color(c);
        buffer.vertex(matrix, -0.5f, 0, 0.5f).color(c);
        buffer.vertex(matrix, -0.5f, 0, -0.5f).color(c);
        
        buffer.vertex(matrix, 0, -1, 0).color(c);
        buffer.vertex(matrix, 0.5f, 0, 0.5f).color(c);
        buffer.vertex(matrix, -0.5f, 0, 0.5f).color(c);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private float helixPhase = 0;
    
    private void renderHelix(MatrixStack ms, LivingEntity target, float anim, float red) {
        Vec3d targetPos = CalcVector.lerpPosition(target);
        helixPhase = (helixPhase + 3.0f) % 360;
        
        int baseColor = ColorAssist.interpolateColor(colorSetting.getColor(), new Color(255, 80, 80).getRGB(), red);
        float hitExpand = 1.0f + (float) Math.sin(red * Math.PI) * 0.4f;
        
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(2.5f);
        
        ms.push();
        ms.translate(targetPos.x, targetPos.y, targetPos.z);
        
        for (int helix = 0; helix < 2; helix++) {
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
            
            float phaseOffset = helix * 180;
            int segments = 40;
            float height = target.getHeight();
            float radius = 0.5f * anim * hitExpand;
            
            for (int i = 0; i < segments; i++) {
                float t1 = (float) i / segments;
                float t2 = (float) (i + 1) / segments;
                
                float angle1 = helixPhase + phaseOffset + t1 * 720;
                float angle2 = helixPhase + phaseOffset + t2 * 720;
                
                float x1 = (float) Math.cos(Math.toRadians(angle1)) * radius;
                float z1 = (float) Math.sin(Math.toRadians(angle1)) * radius;
                float y1 = t1 * height;
                
                float x2 = (float) Math.cos(Math.toRadians(angle2)) * radius;
                float z2 = (float) Math.sin(Math.toRadians(angle2)) * radius;
                float y2 = t2 * height;
                
                int alpha = (int) (255 * anim * (0.5f + 0.5f * Math.sin(t1 * Math.PI)));
                int c = ColorAssist.setAlpha(baseColor, alpha);
                
                Render3D.vertexLine(ms, buffer, new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z2), c, c);
            }
            
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
        
        ms.pop();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private float pulseTime = 0;
    private final List<Float> pulseRings = new ArrayList<>();
    
    private void renderPulse(MatrixStack ms, LivingEntity target, float anim, float red) {
        Vec3d targetPos = CalcVector.lerpPosition(target);
        pulseTime += 0.05f;
        
        if (pulseTime >= 1.0f) {
            pulseRings.add(0f);
            pulseTime = 0;
        }
        
        int baseColor = ColorAssist.interpolateColor(colorSetting.getColor(), new Color(255, 100, 100).getRGB(), red);
        
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(3.0f);
        
        ms.push();
        ms.translate(targetPos.x, targetPos.y + target.getHeight() / 2, targetPos.z);
        
        List<Float> toRemove = new ArrayList<>();
        
        for (int r = 0; r < pulseRings.size(); r++) {
            float progress = pulseRings.get(r);
            progress += 0.02f;
            pulseRings.set(r, progress);
            
            if (progress >= 1.0f) {
                toRemove.add(progress);
                continue;
            }
            
            float radius = progress * 1.5f * anim;
            float alpha = (1.0f - progress) * anim;
            int c = ColorAssist.setAlpha(baseColor, (int)(alpha * 255));
            
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
            
            int segments = 32;
            for (int i = 0; i < segments; i++) {
                float angle1 = (float) (i * 2 * Math.PI / segments);
                float angle2 = (float) ((i + 1) * 2 * Math.PI / segments);
                
                float x1 = (float) Math.cos(angle1) * radius;
                float z1 = (float) Math.sin(angle1) * radius;
                float x2 = (float) Math.cos(angle2) * radius;
                float z2 = (float) Math.sin(angle2) * radius;
                
                Render3D.vertexLine(ms, buffer, new Vec3d(x1, 0, z1), new Vec3d(x2, 0, z2), c, c);
            }
            
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
        
        pulseRings.removeIf(p -> p >= 1.0f);
        
        ms.pop();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private float starsRotation = 0;
    
    private void renderStars(MatrixStack ms, LivingEntity target, float anim, float red) {
        Vec3d targetPos = CalcVector.lerpPosition(target);
        starsRotation = (starsRotation + 1.5f) % 360;
        
        float hitEffect = (float) Math.sin(red * Math.PI) * 0.25f;
        int baseColor = ColorAssist.interpolateColor(colorSetting.getColor(), new Color(255, 200, 50).getRGB(), red);
        
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        
        ms.push();
        ms.translate(targetPos.x, targetPos.y + target.getHeight() / 2, targetPos.z);
        
        for (int i = 0; i < 6; i++) {
            ms.push();
            
            float angle = starsRotation + (i * 60);
            float radius = (0.7f + hitEffect) * anim;
            float yOffset = (float) Math.sin(Math.toRadians(starsRotation * 2 + i * 60)) * 0.4f;
            
            double x = Math.cos(Math.toRadians(angle)) * radius;
            double z = Math.sin(Math.toRadians(angle)) * radius;
            
            ms.translate(x, yOffset, z);
            
            Camera camera = mc.gameRenderer.getCamera();
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(starsRotation * 2));
            
            float starScale = 0.12f * anim;
            ms.scale(starScale, starScale, starScale);
            
            drawStar(ms, baseColor, anim);
            
            ms.pop();
        }
        
        ms.pop();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
    
    private void drawStar(MatrixStack ms, int color, float anim) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = ms.peek().getPositionMatrix();
        int c = ColorAssist.setAlpha(color, (int)(220 * anim));
        
        int points = 5;
        float outerRadius = 1.0f;
        float innerRadius = 0.4f;
        
        for (int i = 0; i < points; i++) {
            float angle1 = (float) (i * 2 * Math.PI / points - Math.PI / 2);
            float angle2 = (float) ((i + 0.5) * 2 * Math.PI / points - Math.PI / 2);
            float angle3 = (float) ((i + 1) * 2 * Math.PI / points - Math.PI / 2);
            
            float x1 = (float) Math.cos(angle1) * outerRadius;
            float y1 = (float) Math.sin(angle1) * outerRadius;
            float x2 = (float) Math.cos(angle2) * innerRadius;
            float y2 = (float) Math.sin(angle2) * innerRadius;
            float x3 = (float) Math.cos(angle3) * outerRadius;
            float y3 = (float) Math.sin(angle3) * outerRadius;
            
            buffer.vertex(matrix, 0, 0, 0).color(c);
            buffer.vertex(matrix, x1, y1, 0).color(c);
            buffer.vertex(matrix, x2, y2, 0).color(c);
            
            buffer.vertex(matrix, 0, 0, 0).color(c);
            buffer.vertex(matrix, x2, y2, 0).color(c);
            buffer.vertex(matrix, x3, y3, 0).color(c);
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private final List<AnimatedCrystal> spiralCrystalList = new ArrayList<>();
    private float spiralRotation = 0;
    private Entity lastSpiralTarget = null;
    private long spiralSpawnTime = 0;
    
    private void createSpiralCrystals() {
        spiralCrystalList.clear();
        for (int i = 0; i < 12; i++) {
            float angle = i * 30;
            float height = 0.3f + (i * 0.15f);
            float radius = 0.45f + (float) Math.sin(i * 0.5) * 0.15f;
            float x = (float) Math.cos(Math.toRadians(angle)) * radius;
            float z = (float) Math.sin(Math.toRadians(angle)) * radius;
            spiralCrystalList.add(new AnimatedCrystal(new Vec3d(x, height, z), i));
        }
        spiralSpawnTime = System.currentTimeMillis();
    }
    
    private void renderSpiralCrystals(MatrixStack ms, LivingEntity target, float anim, float red) {
        if (spiralCrystalList.isEmpty() || lastSpiralTarget != target) {
            createSpiralCrystals();
            lastSpiralTarget = target;
        }
        
        Vec3d targetPos = CalcVector.lerpPosition(target);
        spiralRotation = (spiralRotation + 1.8f) % 360;
        
        float hitPull = MathHelper.clamp(red, 0f, 1f);
        hitPull = hitPull * hitPull;
        
        long timeSinceSpawn = System.currentTimeMillis() - spiralSpawnTime;
        
        RenderSystem.enableDepthTest();
        
        ms.push();
        ms.translate(targetPos.x, targetPos.y, targetPos.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spiralRotation));
        
        Camera camera = mc.gameRenderer.getCamera();
        for (int i = 0; i < spiralCrystalList.size(); i++) {
            AnimatedCrystal crystal = spiralCrystalList.get(i);
            float wave = (float) Math.sin(System.currentTimeMillis() / 400.0 + i * 0.4) * 0.08f;
            
            ms.push();
            ms.translate(0, wave, 0);
            crystal.render(ms, anim, red, hitPull, camera, spiralRotation, timeSinceSpawn);
            ms.pop();
        }
        ms.pop();
        RenderSystem.enableDepthTest();
    }

    private final List<AnimatedCrystal> crystalsV2List = new ArrayList<>();
    private float v2Rotation = 0;
    private Entity lastV2Target = null;
    private long v2SpawnTime = 0;
    
    private void createCrystalsV2() {
        crystalsV2List.clear();
        java.util.Random rand = new java.util.Random();
        
        int count = 20;
        for (int i = 0; i < count; i++) {
            float angle = rand.nextFloat() * 360f;
            float height = 0.4f + rand.nextFloat() * 1.6f;
            float radius = 0.4f + rand.nextFloat() * 0.35f;
            
            float x = (float) Math.cos(Math.toRadians(angle)) * radius;
            float z = (float) Math.sin(Math.toRadians(angle)) * radius;
            
            x += (rand.nextFloat() - 0.5f) * 0.15f;
            z += (rand.nextFloat() - 0.5f) * 0.15f;
            
            crystalsV2List.add(new AnimatedCrystal(new Vec3d(x, height, z), i));
        }
        v2SpawnTime = System.currentTimeMillis();
    }
    
    private void renderCrystalsV2(MatrixStack ms, LivingEntity target, float anim, float red) {
        if (crystalsV2List.isEmpty() || lastV2Target != target) {
            createCrystalsV2();
            lastV2Target = target;
        }
        
        Vec3d targetPos = CalcVector.lerpPosition(target);
        v2Rotation = (v2Rotation + 0.7f) % 360;
        
        float hitPull = MathHelper.clamp(red, 0f, 1f);
        hitPull = hitPull * hitPull;
        
        long timeSinceSpawn = System.currentTimeMillis() - v2SpawnTime;
        
        RenderSystem.enableDepthTest();
        
        ms.push();
        ms.translate(targetPos.x, targetPos.y, targetPos.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(v2Rotation));
        
        Camera camera = mc.gameRenderer.getCamera();
        for (int i = 0; i < crystalsV2List.size(); i++) {
            AnimatedCrystal crystal = crystalsV2List.get(i);
            float wave = (float) Math.sin(System.currentTimeMillis() / 350.0 + i * 0.7) * 0.06f;
            float sway = (float) Math.cos(System.currentTimeMillis() / 500.0 + i * 0.5) * 0.03f;
            
            ms.push();
            ms.translate(sway, wave, sway * 0.5f);
            crystal.render(ms, anim, red, hitPull, camera, v2Rotation, timeSinceSpawn);
            ms.pop();
        }
        ms.pop();
        RenderSystem.enableDepthTest();
    }
}
