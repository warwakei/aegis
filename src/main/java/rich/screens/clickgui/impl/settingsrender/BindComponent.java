package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.modules.module.setting.implement.BindSetting;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class BindComponent extends AbstractSettingComponent {
    private boolean listening = false;
    private float listeningAnimation = 0f;
    private float hoverAnimation = 0f;
    private float bindHoverAnimation = 0f;
    private float pulseAnimation = 0f;
    private float scaleAnimation = 1f;
    private float glowAnimation = 0f;
    private float textChangeAnimation = 0f;
    private String previousBindText = "";
    private String currentBindText = "";

    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8f;
    private static final float FAST_ANIMATION_SPEED = 12f;
    private static final float BIND_BOX_WIDTH = 32f;
    private static final float BIND_BOX_HEIGHT = 10f;

    public static final int SCROLL_UP_BIND = 1000;
    public static final int SCROLL_DOWN_BIND = 1001;
    public static final int MIDDLE_MOUSE_BIND = 1002;

    public BindComponent(BindSetting setting) {
        super(setting);
        BindSetting bindSetting = (BindSetting) getSetting();
        this.currentBindText = getBindDisplayName(bindSetting.getKey(), bindSetting.getType());
        this.previousBindText = this.currentBindText;
    }

    private float getDeltaTime() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;
        return deltaTime;
    }

    private float lerp(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) {
            return target;
        }
        return current + diff * Math.min(speed, 1f);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float deltaTime = getDeltaTime();

        boolean hovered = isHover(mouseX, mouseY);
        boolean bindHovered = isBindHover(mouseX, mouseY);

        hoverAnimation = lerp(hoverAnimation, hovered ? 1f : 0f, deltaTime * ANIMATION_SPEED);
        bindHoverAnimation = lerp(bindHoverAnimation, bindHovered ? 1f : 0f, deltaTime * ANIMATION_SPEED);
        listeningAnimation = lerp(listeningAnimation, listening ? 1f : 0f, deltaTime * FAST_ANIMATION_SPEED);

        float scaleTarget = listening ? 1.05f : (bindHovered ? 1.02f : 1f);
        scaleAnimation = lerp(scaleAnimation, scaleTarget, deltaTime * ANIMATION_SPEED);

        glowAnimation = lerp(glowAnimation, listening ? 1f : 0f, deltaTime * ANIMATION_SPEED);

        if (listening) {
            pulseAnimation += deltaTime * 4f;
            if (pulseAnimation > Math.PI * 2) {
                pulseAnimation -= (float)(Math.PI * 2);
            }
        } else {
            pulseAnimation = lerp(pulseAnimation, 0f, deltaTime * ANIMATION_SPEED);
        }

        BindSetting bindSetting = (BindSetting) getSetting();
        String newBindText = listening ? "" : getBindDisplayName(bindSetting.getKey(), bindSetting.getType());

        if (!newBindText.equals(currentBindText)) {
            previousBindText = currentBindText;
            currentBindText = newBindText;
            textChangeAnimation = 0f;
        }

        textChangeAnimation = lerp(textChangeAnimation, 1f, deltaTime * FAST_ANIMATION_SPEED);

        int iconAlpha = (int)(200 * alphaMultiplier);
        Fonts.GUI_ICONS.draw("L", x + 1.5f, y + height / 2 - 6f, 6, new Color(210, 210, 210, iconAlpha).getRGB());

        Fonts.BOLD.draw(getSetting().getName(), x + 9.5f, y + height / 2 - 7.5f, 6, applyAlpha(new Color(210, 210, 220, 200)).getRGB());

        String description = getSetting().getDescription();
        if (description != null && !description.isEmpty()) {
            Fonts.BOLD.draw(description, x + 0.5f, y + height / 2 + 0.5f, 5, applyAlpha(new Color(128, 128, 128, 128)).getRGB());
        }

        renderBindBox(mouseX, mouseY, bindSetting);
    }

    private void renderBindBox(int mouseX, int mouseY, BindSetting bindSetting) {
        float bindBoxX = x + width - BIND_BOX_WIDTH - 2;
        float bindBoxY = y + height / 2 - BIND_BOX_HEIGHT / 2;

        float scaledWidth = BIND_BOX_WIDTH * scaleAnimation;
        float scaledHeight = BIND_BOX_HEIGHT * scaleAnimation;
        float scaledX = bindBoxX - (scaledWidth - BIND_BOX_WIDTH) / 2;
        float scaledY = bindBoxY - (scaledHeight - BIND_BOX_HEIGHT) / 2;

        int bgAlpha = (int)(25 + bindHoverAnimation * 15 + listeningAnimation * 20);
        Color bgColor;
        if (listening) {
            float pulse = (float)(Math.sin(pulseAnimation) * 0.15 + 0.85);
            bgColor = new Color(
                    (int)(60 + 40 * pulse),
                    (int)(80 + 40 * pulse),
                    (int)(120 + 35 * pulse),
                    (int)(bgAlpha * alphaMultiplier)
            );
        } else if (bindSetting.getKey() != GLFW.GLFW_KEY_UNKNOWN && bindSetting.getKey() != -1) {
            bgColor = applyAlpha(new Color(40, 60, 50, bgAlpha));
        } else {
            bgColor = applyAlpha(new Color(40, 40, 45, bgAlpha));
        }

        Render2D.rect(scaledX, scaledY, scaledWidth, scaledHeight, bgColor.getRGB(), 3f);

        float outlineAlpha;
        Color outlineColor;

        if (listening) {
            float pulse = (float)(Math.sin(pulseAnimation) * 0.3 + 0.7);
            outlineAlpha = 150 * pulse * listeningAnimation;
            outlineColor = new Color(120, 160, 220, (int)(outlineAlpha * alphaMultiplier));
        } else if (bindSetting.getKey() != GLFW.GLFW_KEY_UNKNOWN && bindSetting.getKey() != -1) {
            outlineAlpha = 80 + bindHoverAnimation * 40;
            outlineColor = new Color(100, 160, 120, (int)(outlineAlpha * alphaMultiplier));
        } else {
            outlineAlpha = 60 + bindHoverAnimation * 40;
            outlineColor = new Color(120, 120, 125, (int)(outlineAlpha * alphaMultiplier));
        }

        Render2D.outline(scaledX, scaledY, scaledWidth, scaledHeight, 0.5f, outlineColor.getRGB(), 3f);

        renderBindText(scaledX, scaledY, scaledWidth, scaledHeight, bindSetting);

        if (listening) {
            renderListeningIndicator(scaledX, scaledY, scaledWidth, scaledHeight);
        }
    }

    private void renderBindText(float boxX, float boxY, float boxWidth, float boxHeight, BindSetting bindSetting) {
        float textY = boxY + boxHeight / 2 - 2.5f;
        float centerX = boxX + boxWidth / 2;

        Color textColor;
        if (listening) {
            float pulse = (float)(Math.sin(pulseAnimation * 2) * 0.2 + 0.8);
            int alpha = (int)(220 * pulse * alphaMultiplier);
            textColor = new Color(180, 200, 240, alpha);
        } else if (bindSetting.getKey() != GLFW.GLFW_KEY_UNKNOWN && bindSetting.getKey() != -1) {
            int alpha = (int)(200 * alphaMultiplier);
            textColor = new Color(140, 200, 150, alpha);
        } else {
            int alpha = (int)(150 * alphaMultiplier);
            textColor = new Color(140, 140, 150, alpha);
        }

        if (textChangeAnimation < 1f && !previousBindText.equals(currentBindText)) {
            float oldAlpha = 1f - textChangeAnimation;
            float newAlpha = textChangeAnimation;

            float oldOffsetY = -3f * textChangeAnimation;
            float newOffsetY = 3f * (1f - textChangeAnimation);

            if (oldAlpha > 0.01f) {
                Color oldColor = new Color(
                        textColor.getRed(),
                        textColor.getGreen(),
                        textColor.getBlue(),
                        (int)(textColor.getAlpha() * oldAlpha)
                );
                Fonts.BOLD.drawCentered(previousBindText, centerX, textY + oldOffsetY, 5, oldColor.getRGB());
            }

            Color newColor = new Color(
                    textColor.getRed(),
                    textColor.getGreen(),
                    textColor.getBlue(),
                    (int)(textColor.getAlpha() * newAlpha)
            );
            Fonts.BOLD.drawCentered(currentBindText, centerX, textY + newOffsetY, 5, newColor.getRGB());
        } else {
            Fonts.BOLD.drawCentered(currentBindText, centerX, textY, 5, textColor.getRGB());
        }
    }

    private void renderListeningIndicator(float boxX, float boxY, float boxWidth, float boxHeight) {
        float dotSpacing = 3f;
        float dotSize = 1.5f;
        float dotsWidth = dotSpacing * 2;
        float startX = boxX + (boxWidth - dotsWidth) / 2 - dotSize / 2;
        float dotY = boxY + boxHeight - 5.5f;

        for (int i = 0; i < 3; i++) {
            float phase = pulseAnimation + i * 0.5f;
            float pulse = (float)(Math.sin(phase * 2) * 0.5 + 0.5);
            float currentDotSize = dotSize * (0.5f + pulse * 0.5f);

            int alpha = (int)(150 * (0.3f + pulse * 0.7f) * listeningAnimation * alphaMultiplier);

            float dotX = startX + i * dotSpacing + (dotSize - currentDotSize) / 2;
            float adjustedDotY = dotY + (dotSize - currentDotSize) / 2;

            Render2D.rect(dotX, adjustedDotY, currentDotSize, currentDotSize,
                    new Color(120, 160, 220, alpha).getRGB(), currentDotSize / 2);
        }
    }

    private String getBindDisplayName(int key, int type) {
        if (key == GLFW.GLFW_KEY_UNKNOWN || key == -1) return "None";

        if (key == SCROLL_UP_BIND) return "ScrollUp";
        if (key == SCROLL_DOWN_BIND) return "ScrollDn";
        if (key == MIDDLE_MOUSE_BIND) return "MMB";

        if (type == 0) {
            return switch (key) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "LMB";
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "RMB";
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MMB";
                case GLFW.GLFW_MOUSE_BUTTON_4 -> "M4";
                case GLFW.GLFW_MOUSE_BUTTON_5 -> "M5";
                case GLFW.GLFW_MOUSE_BUTTON_6 -> "M6";
                case GLFW.GLFW_MOUSE_BUTTON_7 -> "M7";
                case GLFW.GLFW_MOUSE_BUTTON_8 -> "M8";
                default -> "M" + key;
            };
        }

        String keyName = GLFW.glfwGetKeyName(key, 0);
        if (keyName == null) {
            return switch (key) {
                case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
                case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
                case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
                case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
                case GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
                case GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
                case GLFW.GLFW_KEY_SPACE -> "Space";
                case GLFW.GLFW_KEY_TAB -> "Tab";
                case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps";
                case GLFW.GLFW_KEY_ENTER -> "Enter";
                case GLFW.GLFW_KEY_BACKSPACE -> "Back";
                case GLFW.GLFW_KEY_INSERT -> "Ins";
                case GLFW.GLFW_KEY_DELETE -> "Del";
                case GLFW.GLFW_KEY_HOME -> "Home";
                case GLFW.GLFW_KEY_END -> "End";
                case GLFW.GLFW_KEY_PAGE_UP -> "PgUp";
                case GLFW.GLFW_KEY_PAGE_DOWN -> "PgDn";
                case GLFW.GLFW_KEY_UP -> "Up";
                case GLFW.GLFW_KEY_DOWN -> "Down";
                case GLFW.GLFW_KEY_LEFT -> "Left";
                case GLFW.GLFW_KEY_RIGHT -> "Right";
                case GLFW.GLFW_KEY_F1 -> "F1";
                case GLFW.GLFW_KEY_F2 -> "F2";
                case GLFW.GLFW_KEY_F3 -> "F3";
                case GLFW.GLFW_KEY_F4 -> "F4";
                case GLFW.GLFW_KEY_F5 -> "F5";
                case GLFW.GLFW_KEY_F6 -> "F6";
                case GLFW.GLFW_KEY_F7 -> "F7";
                case GLFW.GLFW_KEY_F8 -> "F8";
                case GLFW.GLFW_KEY_F9 -> "F9";
                case GLFW.GLFW_KEY_F10 -> "F10";
                case GLFW.GLFW_KEY_F11 -> "F11";
                case GLFW.GLFW_KEY_F12 -> "F12";
                case GLFW.GLFW_KEY_ESCAPE -> "Esc";
                case GLFW.GLFW_KEY_PRINT_SCREEN -> "Print";
                case GLFW.GLFW_KEY_SCROLL_LOCK -> "Scroll";
                case GLFW.GLFW_KEY_PAUSE -> "Pause";
                case GLFW.GLFW_KEY_NUM_LOCK -> "NumLk";
                case GLFW.GLFW_KEY_KP_0 -> "Num0";
                case GLFW.GLFW_KEY_KP_1 -> "Num1";
                case GLFW.GLFW_KEY_KP_2 -> "Num2";
                case GLFW.GLFW_KEY_KP_3 -> "Num3";
                case GLFW.GLFW_KEY_KP_4 -> "Num4";
                case GLFW.GLFW_KEY_KP_5 -> "Num5";
                case GLFW.GLFW_KEY_KP_6 -> "Num6";
                case GLFW.GLFW_KEY_KP_7 -> "Num7";
                case GLFW.GLFW_KEY_KP_8 -> "Num8";
                case GLFW.GLFW_KEY_KP_9 -> "Num9";
                case GLFW.GLFW_KEY_KP_DECIMAL -> "Num.";
                case GLFW.GLFW_KEY_KP_DIVIDE -> "Num/";
                case GLFW.GLFW_KEY_KP_MULTIPLY -> "Num*";
                case GLFW.GLFW_KEY_KP_SUBTRACT -> "Num-";
                case GLFW.GLFW_KEY_KP_ADD -> "Num+";
                case GLFW.GLFW_KEY_KP_ENTER -> "NumEnt";
                default -> "Key" + key;
            };
        }
        return keyName.toUpperCase();
    }

    private boolean isBindHover(double mouseX, double mouseY) {
        float bindBoxX = x + width - BIND_BOX_WIDTH - 2;
        float bindBoxY = y + height / 2 - BIND_BOX_HEIGHT / 2;
        return mouseX >= bindBoxX && mouseX <= bindBoxX + BIND_BOX_WIDTH &&
                mouseY >= bindBoxY && mouseY <= bindBoxY + BIND_BOX_HEIGHT;
    }

    public void handleScrollBind(double vertical) {
        if (listening) {
            BindSetting bindSetting = (BindSetting) getSetting();
            if (vertical > 0) {
                bindSetting.setKey(SCROLL_UP_BIND);
            } else {
                bindSetting.setKey(SCROLL_DOWN_BIND);
            }
            bindSetting.setType(2);
            listening = false;
        }
    }

    public void handleMiddleMouseBind() {
        if (listening) {
            BindSetting bindSetting = (BindSetting) getSetting();
            bindSetting.setKey(MIDDLE_MOUSE_BIND);
            bindSetting.setType(2);
            listening = false;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isBindHover(mouseX, mouseY)) {
            if (button == 1) {
                ((BindSetting) getSetting()).setKey(GLFW.GLFW_KEY_UNKNOWN);
                ((BindSetting) getSetting()).setType(1);
                listening = false;
                return true;
            } else if (listening) {
                ((BindSetting) getSetting()).setKey(button);
                ((BindSetting) getSetting()).setType(0);
                listening = false;
                return true;
            } else if (button == 0) {
                listening = true;
                return true;
            }
        } else if (listening) {
            listening = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening = false;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                ((BindSetting) getSetting()).setKey(GLFW.GLFW_KEY_UNKNOWN);
                ((BindSetting) getSetting()).setType(1);
                listening = false;
                return true;
            } else if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                ((BindSetting) getSetting()).setKey(keyCode);
                ((BindSetting) getSetting()).setType(1);
                listening = false;
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isListening() {
        return listening;
    }
}