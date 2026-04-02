package fun.aegis.features.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.util.math.RotationAxis;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.ColorSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.utils.display.color.ColorAssist;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChinaHat extends Module {

    ColorSetting color = new ColorSetting("Color", "Hat color").value(0xFFFFFFFF);
    SliderSettings transparency = new SliderSettings("Transparency", "Overall hat transparency").setValue(0.5f).range(0.1f, 1.0f);
    SliderSettings height = new SliderSettings("Height", "Hat height").setValue(0.30f).range(0.1f, 1.0f);

    public ChinaHat() {
        super("ChinaHat", ModuleCategory.RENDER);
        setup(color, transparency, height);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.player == null || mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        MatrixStack stack = event.getStack();
        float partialTicks = event.getPartialTicks();
        
        // Интерполированная позиция игрока
        double interpX = mc.player.prevX + (mc.player.getX() - mc.player.prevX) * partialTicks;
        double interpY = mc.player.prevY + (mc.player.getY() - mc.player.prevY) * partialTicks;
        double interpZ = mc.player.prevZ + (mc.player.getZ() - mc.player.prevZ) * partialTicks;
        
        // Интерполированные углы головы
        float interpYaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw) * partialTicks;
        float interpPitch = mc.player.prevPitch + (mc.player.getPitch() - mc.player.prevPitch) * partialTicks;
        
        // Высота глаз + смещение над головой
        float eyeHeight = mc.player.getEyeHeight(mc.player.getPose());
        float hatOffset = 0.35f + getYOffset(mc.player);

        stack.push();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // Перемещаемся к позиции игрока
        stack.translate(interpX, interpY + eyeHeight, interpZ);
        
        // Поворачиваем по yaw головы
        stack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(interpYaw));
        
        // Поворачиваем по pitch головы (наклон)
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(interpPitch));
        
        // Смещаем вверх над головой (после поворотов, чтобы шляпа следовала за наклоном)
        stack.translate(0, hatOffset, 0);

        BufferBuilder cone = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        float radiusValue = 0.55f;
        float heightValue = height.getValue();
        int rgb = color.getColor();
        int adjustedColor = ColorAssist.multAlpha(rgb, transparency.getValue());

        cone.vertex(stack.peek().getPositionMatrix(), 0, heightValue, 0).color(ColorAssist.multAlpha(ColorAssist.multBright(rgb, 0.86f), transparency.getValue()));

        float steps = 64;
        double angleStep = 2 * Math.PI / steps;
        for (int i = 0; i <= steps; i++) {
            float x = (float) (Math.cos(i * angleStep) * radiusValue);
            float z = (float) (Math.sin(i * angleStep) * radiusValue);
            cone.vertex(stack.peek().getPositionMatrix(), x, 0, z).color(adjustedColor);
        }

        BufferRenderer.drawWithGlobalProgram(cone.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthFunc(GL11.GL_LESS);
        stack.pop();
    }

    private float getYOffset(Entity entity) {
        float offset = 0.0f;
        if (entity instanceof LivingEntity livingEntity) {
            if (livingEntity.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD).getItem() instanceof ArmorItem) {
                offset += 0.05f;
            }
        }
        return offset;
    }
}
