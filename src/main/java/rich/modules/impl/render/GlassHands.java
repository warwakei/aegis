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
            .setValue(2.5f).range(1.0f, 5.0f);

    SliderSettings blurIterations = new SliderSettings("Качество", "Количество итераций размытия")
            .setValue(3).range(1, 5);

    SliderSettings saturation = new SliderSettings("Насыщенность", "Насыщенность цвета")
            .setValue(0).range(0.0f, 2.0f);

    BooleanSetting enableTint = new BooleanSetting("Оттенок", "Включить цветной оттенок стекла")
            .setValue(false);

    SliderSettings tintIntensity = new SliderSettings("Сила оттенка", "Интенсивность оттенка")
            .setValue(0.2f).range(0.0f, 0.5f)
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
        settings(blurRadius, blurIterations, saturation, enableTint, tintIntensity, tintColor, enableEdgeGlow, edgeGlowIntensity);
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

        renderer.setBlurRadius(blurRadius.getValue());
        renderer.setBlurIterations(blurIterations.getInt());
        renderer.setSaturation(saturation.getValue());
        renderer.setReflect(true);

        if (enableTint.isValue()) {
            renderer.setTintColor(tintColor.getColor());
            renderer.setTintIntensity(tintIntensity.getValue());
        } else {
            renderer.setTintColor(0x00000000);
            renderer.setTintIntensity(0.0f);
        }

        if (enableEdgeGlow.isValue()) {
            renderer.setEdgeGlowIntensity(edgeGlowIntensity.getValue());
        } else {
            renderer.setEdgeGlowIntensity(0.0f);
        }
    }
}