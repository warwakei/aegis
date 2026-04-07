package rich.modules.impl.render;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import rich.IMinecraft;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.ColorUtil;
import rich.util.render.Render2D;

import java.awt.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatHUD extends ModuleStructure implements IMinecraft {

    final SelectSetting backgroundMode = new SelectSetting("Фон", "Стиль фона чата")
            .value("Градиент", "Однотонный", "Blur")
            .selected("Градиент");

    final SliderSettings width = new SliderSettings("Ширина", "Ширина чата")
            .range(200f, 500f).setValue(320f);

    final SliderSettings height = new SliderSettings("Высота", "Высота чата")
            .range(100f, 300f).setValue(180f);

    final SliderSettings cornerRadius = new SliderSettings("Скругление", "Радиус скругления углов")
            .range(0f, 15f).setValue(8f);

    final SliderSettings outlineThickness = new SliderSettings("Толщина аутлайна", "Толщина обводки")
            .range(0f, 2f).setValue(0.5f);

    final SliderSettings chatOpacity = new SliderSettings("Прозрачность чата", "Общая прозрачность")
            .range(0.3f, 1.0f).setValue(1.0f);

    final BooleanSetting smoothBackground = new BooleanSetting("Плавный фон", "Сглаживание фона чата")
            .setValue(true);

    // Настройки цветов
    final ColorSetting backgroundColor1 = new ColorSetting("Цвет фона 1", "Верхний цвет градиента")
            .value(0x1A1A24)
            .visible(() -> backgroundMode.isSelected("Градиент"));

    final ColorSetting backgroundColor2 = new ColorSetting("Цвет фона 2", "Нижний цвет градиента")
            .value(0x121218)
            .visible(() -> backgroundMode.isSelected("Градиент"));

    final ColorSetting outlineColor = new ColorSetting("Цвет аутлайна", "Цвет обводки")
            .value(0x3A3A4A);

    final BooleanSetting rainbowOutline = new BooleanSetting("Радужный аутлайн", "Анимированная радужная обводка")
            .setValue(false);

    final SliderSettings rainbowSpeed = new SliderSettings("Скорость радуги", "Скорость анимации радуги")
            .range(1f, 10f).setValue(5f)
            .visible(() -> rainbowOutline.isValue());

    // Дополнительные эффекты
    final BooleanSetting blurEffect = new BooleanSetting("Blur эффект", "Размытие фона под чатом")
            .setValue(false)
            .visible(() -> backgroundMode.isSelected("Blur"));

    final SliderSettings blurRadius = new SliderSettings("Радиус blur", "Сила размытия")
            .range(2f, 10f).setValue(4f)
            .visible(() -> blurEffect.isValue());

    final BooleanSetting shadow = new BooleanSetting("Тень", "Тень под чатом")
            .setValue(true);

    final SliderSettings shadowSize = new SliderSettings("Размер тени", "Радиус тени")
            .range(5f, 20f).setValue(10f)
            .visible(() -> shadow.isValue());

    public ChatHUD() {
        super("ChatHUD", "Улучшенный чат с красивым фоном и аутлайном", ModuleCategory.RENDER);
        settings(
                backgroundMode, width, height, cornerRadius, outlineThickness, chatOpacity, smoothBackground,
                backgroundColor1, backgroundColor2, outlineColor, rainbowOutline, rainbowSpeed,
                blurEffect, blurRadius, shadow, shadowSize
        );
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onRender2D(DrawContext context) {
        if (mc.player == null || mc.world == null) return;
        if (mc.options.hudHidden) return;

        ChatHud chatHud = mc.inGameHud.getChatHud();
        if (chatHud == null) return;

        // Всегда рендерим кастомный фон
        renderCustomChatBackground(context);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void renderCustomChatBackground(DrawContext context) {
        float chatWidth = width.getValue();
        float chatHeight = height.getValue();
        float x = 2;
        float y = mc.getWindow().getScaledHeight() - chatHeight - 4;
        float cornerRad = cornerRadius.getValue();
        float outlineThick = outlineThickness.getValue();
        float alpha = chatOpacity.getValue();

        long currentTime = System.currentTimeMillis();

        // Рендерим тень
        if (shadow.isValue()) {
            float shadowSizeVal = shadowSize.getValue();
            Render2D.blur(x - shadowSizeVal / 2, y - shadowSizeVal / 2,
                    chatWidth + shadowSizeVal, chatHeight + shadowSizeVal,
                    shadowSizeVal, cornerRad + shadowSizeVal / 2,
                    new Color(0, 0, 0, (int) (60 * alpha)).getRGB());
        }

        // Рендерим blur если включён
        if (backgroundMode.isSelected("Blur") && blurEffect.isValue()) {
            Render2D.blur(x, y, chatWidth, chatHeight,
                    blurRadius.getValue(), cornerRad,
                    new Color(20, 20, 30, (int) (100 * alpha)).getRGB());
        }

        // Определяем цвета фона
        int bgTop, bgBottom;
        if (backgroundMode.isSelected("Градиент")) {
            bgTop = applyAlpha(backgroundColor1.getColor(), alpha);
            bgBottom = applyAlpha(backgroundColor2.getColor(), alpha);
        } else {
            int baseColor = backgroundColor1.getColor();
            bgTop = applyAlpha(baseColor, alpha);
            bgBottom = applyAlpha(baseColor, alpha);
        }

        // Рендерим фон с градиентом
        if (backgroundMode.isSelected("Градиент")) {
            Render2D.gradientRect(x, y, chatWidth, chatHeight,
                    new int[]{bgTop, bgTop, bgBottom, bgBottom},
                    cornerRad);
        } else {
            Render2D.rect(x, y, chatWidth, chatHeight, bgTop, cornerRad);
        }

        // Рендерим аутлайн
        if (outlineThick > 0) {
            int outlineCol;
            if (rainbowOutline.isValue()) {
                float speed = rainbowSpeed.getValue() * 100;
                outlineCol = ColorUtil.rainbow((int) speed, 0, 0.6f, 0.8f, alpha);
            } else {
                outlineCol = applyAlpha(outlineColor.getColor(), alpha);
            }
            Render2D.outline(x, y, chatWidth, chatHeight, outlineThick, outlineCol, cornerRad);
        }
    }

    private int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
