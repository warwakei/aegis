package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import rich.events.api.EventHandler;
import rich.events.impl.GlassHandsRenderEvent;
import rich.events.impl.WorldChangeEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.render.shader.GlassHandsRenderer;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GlassHands extends ModuleStructure {

    private static GlassHands instance;

    SliderSettings blurRadius = new SliderSettings("Сила размытия", "Сила эффекта размытия стекла")
            .setValue(3.0f).range(1.0f, 8.0f);

    SliderSettings blurIterations = new SliderSettings("Качество", "Количество итераций размытия")
            .setValue(4).range(1, 8);

    SliderSettings saturation = new SliderSettings("Насыщенность", "Насыщенность цвета")
            .setValue(0).range(0.0f, 2.0f);

    BooleanSetting enableReflection = new BooleanSetting("Reflection", "Enable reflective sheen")
            .setValue(true);

    BooleanSetting dynamicBlur = new BooleanSetting("Dynamic Blur", "Increase blur while moving")
            .setValue(true);

    SliderSettings motionBlurBoost = new SliderSettings("Motion Boost", "Additional blur strength while moving")
            .setValue(1.4f).range(0.0f, 3.0f)
            .visible(dynamicBlur::isValue);

    BooleanSetting pulse = new BooleanSetting("Pulse", "Animate tint and glow over time")
            .setValue(true);

    SliderSettings pulseSpeed = new SliderSettings("Pulse Speed", "Pulse animation speed")
            .setValue(1.25f).range(0.1f, 3.0f)
            .visible(pulse::isValue);

    SliderSettings pulseStrength = new SliderSettings("Pulse Strength", "Pulse animation amplitude")
            .setValue(0.18f).range(0.0f, 0.6f)
            .visible(pulse::isValue);

    BooleanSetting enableTint = new BooleanSetting("Оттенок", "Включить цветной оттенок стекла")
            .setValue(false);

    SliderSettings tintIntensity = new SliderSettings("Сила оттенка", "Интенсивность оттенка")
            .setValue(0.2f).range(0.0f, 0.7f)
            .visible(enableTint::isValue);

    ColorSetting tintColor = new ColorSetting("Цвет оттенка", "Цвет оттенка стекла")
            .value(0xFF00FFFF)
            .visible(enableTint::isValue);

    BooleanSetting enableEdgeGlow = new BooleanSetting("Свечение краёв", "Свечение по краям стекла")
            .setValue(true);

    SliderSettings edgeGlowIntensity = new SliderSettings("Сила свечения", "Интенсивность свечения краёв")
            .setValue(0.2f).range(0.0f, 1.0f)
            .visible(enableEdgeGlow::isValue);

    public GlassHands() {
        super("GlassHands", "Делает руки и предметы стеклянными", ModuleCategory.RENDER);
        settings(
                blurRadius, blurIterations, saturation,
                enableReflection,
                dynamicBlur, motionBlurBoost,
                pulse, pulseSpeed, pulseStrength,
                enableTint, tintIntensity, tintColor,
                enableEdgeGlow, edgeGlowIntensity
        );
        instance = this;
    }

    public static GlassHands getInstance() {
        return instance;
    }

    @Override
    public void activate() {
        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
        if (renderer != null) {
            renderer.invalidate();
            renderer.setEnabled(true);
            updateRendererSettings();
        }
    }

    @Override
    public void deactivate() {
        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
        if (renderer != null) {
            renderer.setEnabled(false);
        }
    }

    @EventHandler
    public void onWorldChange(WorldChangeEvent event) {
        if (!isState()) return;

        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
        if (renderer != null) {
            renderer.invalidate();
            renderer.setEnabled(true);
            updateRendererSettings();
        }
    }

    @EventHandler
    public void onGlassHandsRender(GlassHandsRenderEvent event) {
        if (!isState()) return;

        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
        if (renderer == null) return;

        updateRendererSettings();

        if (event.getPhase() == GlassHandsRenderEvent.Phase.PRE) {
            renderer.captureSceneBeforeHands();
        } else if (event.getPhase() == GlassHandsRenderEvent.Phase.POST) {
            renderer.captureSceneAfterHands();
            renderer.renderGlassEffect();
        }
    }

    private void updateRendererSettings() {
        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
        if (renderer == null) return;

        float finalBlurRadius = blurRadius.getValue();
        float finalSaturation = saturation.getValue();
        float finalTintIntensity = enableTint.isValue() ? tintIntensity.getValue() : 0.0f;
        float finalEdgeGlow = enableEdgeGlow.isValue() ? edgeGlowIntensity.getValue() : 0.0f;

        if (dynamicBlur.isValue() && mc.player != null) {
            float speed = (float) mc.player.getVelocity().horizontalLength();
            finalBlurRadius += Math.min(1.0f, speed * 3.0f) * motionBlurBoost.getValue();
        }

        if (pulse.isValue()) {
            double time = System.currentTimeMillis() * 0.001 * pulseSpeed.getValue();
            float wave = (float) ((Math.sin(time) * 0.5) + 0.5);
            float amp = pulseStrength.getValue();
            finalSaturation += wave * amp;
            finalTintIntensity += wave * amp * 0.35f;
            finalEdgeGlow += wave * amp * 0.6f;
        }

        renderer.setBlurRadius(Math.max(1.0f, Math.min(8.0f, finalBlurRadius)));
        renderer.setBlurIterations(blurIterations.getInt());
        renderer.setSaturation(Math.max(0.0f, Math.min(2.5f, finalSaturation)));
        renderer.setReflect(enableReflection.isValue());

        if (enableTint.isValue()) {
            renderer.setTintColor(tintColor.getColor());
            renderer.setTintIntensity(Math.max(0.0f, Math.min(0.8f, finalTintIntensity)));
        } else {
            renderer.setTintColor(0x00000000);
            renderer.setTintIntensity(0.0f);
        }

        renderer.setEdgeGlowIntensity(enableEdgeGlow.isValue()
                ? Math.max(0.0f, Math.min(1.0f, finalEdgeGlow))
                : 0.0f);
    }
}
