package fun.aegis.display.screens.clickgui.components.implement.autobuy.settings;

import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.originalitems.ItemRegistry;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.math.calc.Calculate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public abstract class AutoBuySettingsComponent extends AbstractComponent {
    protected final AutoBuyItemSettings settings;

    public AutoBuySettingsComponent(AutoBuyItemSettings settings) {
        this.settings = settings;
        this.height = 15;
    }

    public static class BuyBelowComponent extends AutoBuySettingsComponent {
        private boolean editing = false;
        private String inputText = "";

        public BuyBelowComponent(AutoBuyItemSettings settings) {
            super(settings);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            MatrixStack matrix = context.getMatrices();

            Fonts.getSize(13, Fonts.Type.SEMI).drawString(matrix, "Покупать ниже:", x + 10, y + 10, ColorAssist.getText(0.8f));

            String displayText = editing ? inputText + "_" : settings.getBuyBelow() + "$";
            float textWidth = Fonts.getSize(13, Fonts.Type.DEFAULT).getStringWidth(displayText);

            rectangle.render(ShapeProperties.create(matrix, x + width - textWidth - 16, y + 5, textWidth + 9, 12)
                    .round(3).thickness(2.5f)
                    .outlineColor(editing ? new Color(41, 42, 40, 40).getRGB() : new Color(41, 42, 40, 140).getRGB())
                    .color(new Color(41, 42, 40, 40).getRGB())
                    .build());

            Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(matrix, displayText, x + width - textWidth - 12, y + 10, ColorAssist.getText());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            String displayText = editing ? inputText + "_" : settings.getBuyBelow() + "$";
            float textWidth = Fonts.getSize(13, Fonts.Type.DEFAULT).getStringWidth(displayText);

            if (Calculate.isHovered(mouseX, mouseY, x + width - textWidth - 20, y + 5, textWidth + 15, 15) && button == 0) {
                editing = true;
                inputText = String.valueOf(settings.getBuyBelow());
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (editing) {
                if (keyCode == GLFW.GLFW_KEY_ENTER) {
                    try {
                        int value = Integer.parseInt(inputText);
                        settings.setBuyBelow(Math.max(1, value));
                        AutoBuySettingsManager.getInstance().saveSettings(settings.getItemName(), settings);
                        ItemRegistry.reloadSettings();
                    } catch (NumberFormatException ignored) {}
                    editing = false;
                    return true;
                } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    editing = false;
                    return true;
                } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !inputText.isEmpty()) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                    return true;
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            if (editing && Character.isDigit(chr) && inputText.length() < 9) {
                inputText += chr;
                return true;
            }
            return super.charTyped(chr, modifiers);
        }

        @Override
        public boolean isHover(double mouseX, double mouseY) {
            return Calculate.isHovered(mouseX, mouseY, x, y, width, height);
        }
    }

//    public static class SellAboveComponent extends AutoBuySettingsComponent {
//        private boolean editing = false;
//        private String inputText = "";
//
//        public SellAboveComponent(AutoBuyItemSettings settings) {
//            super(settings);
//        }
//
//        @Override
//        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
//            MatrixStack matrix = context.getMatrices();
//
//            Fonts.getSize(13, Fonts.Type.SEMI).drawString(matrix, "Продавать выше:", x + 10, y + 10, ColorAssist.getText(0.8f));
//
//            String displayText = editing ? inputText + "_" : settings.getSellAbove() + "$";
//            float textWidth = Fonts.getSize(13, Fonts.Type.DEFAULT).getStringWidth(displayText);
//
//            rectangle.render(ShapeProperties.create(matrix, x + width - textWidth - 16, y + 5, textWidth + 9, 12)
//                    .round(3).thickness(2.5f)
//                    .outlineColor(editing ? new Color(41, 42, 40, 40).getRGB() : new Color(41, 42, 40, 140).getRGB())
//                    .color(new Color(41, 42, 40, 40).getRGB())
//                    .build());
//
//            Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(matrix, displayText, x + width - textWidth - 12, y + 10, ColorAssist.getText());
//        }
//
//        @Override
//        public boolean mouseClicked(double mouseX, double mouseY, int button) {
//            String displayText = editing ? inputText + "_" : settings.getSellAbove() + "$";
//            float textWidth = Fonts.getSize(13, Fonts.Type.DEFAULT).getStringWidth(displayText);
//
//            if (Calculate.isHovered(mouseX, mouseY, x + width - textWidth - 20, y + 5, textWidth + 15, 15) && button == 0) {
//                editing = true;
//                inputText = String.valueOf(settings.getSellAbove());
//                return true;
//            }
//            return super.mouseClicked(mouseX, mouseY, button);
//        }
//
//        @Override
//        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//            if (editing) {
//                if (keyCode == GLFW.GLFW_KEY_ENTER) {
//                    try {
//                        int value = Integer.parseInt(inputText);
//                        settings.setSellAbove(Math.max(1, value));
//                        AutoBuySettingsManager.getInstance().saveSettings(settings.getItemName(), settings);
//                        ItemRegistry.reloadSettings();
//                    } catch (NumberFormatException ignored) {}
//                    editing = false;
//                    return true;
//                } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
//                    editing = false;
//                    return true;
//                } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !inputText.isEmpty()) {
//                    inputText = inputText.substring(0, inputText.length() - 1);
//                    return true;
//                }
//            }
//            return super.keyPressed(keyCode, scanCode, modifiers);
//        }
//
//        @Override
//        public boolean charTyped(char chr, int modifiers) {
//            if (editing && Character.isDigit(chr) && inputText.length() < 9) {
//                inputText += chr;
//                return true;
//            }
//            return super.charTyped(chr, modifiers);
//        }
//
//        @Override
//        public boolean isHover(double mouseX, double mouseY) {
//            return Calculate.isHovered(mouseX, mouseY, x, y, width, height);
//        }
//    }

    public static class MinQuantityComponent extends AutoBuySettingsComponent {
        private boolean editing = false;
        private String inputText = "";

        public MinQuantityComponent(AutoBuyItemSettings settings) {
            super(settings);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            MatrixStack matrix = context.getMatrices();

            Fonts.getSize(13, Fonts.Type.SEMI).drawString(matrix, "Количество от:", x + 10, y + 8, ColorAssist.getText(0.8f));

            String displayText = editing ? inputText + "_" : String.valueOf(settings.getMinQuantity());
            float textWidth = Fonts.getSize(13, Fonts.Type.DEFAULT).getStringWidth(displayText);

            rectangle.render(ShapeProperties.create(matrix, x + width - textWidth - 16, y + 5, textWidth + 9, 12)
                    .round(3).thickness(2.5f)
                    .outlineColor(editing ? new Color(41, 42, 40, 40).getRGB() : new Color(41, 42, 40, 140).getRGB())
                    .color(new Color(41, 42, 40, 40).getRGB())
                    .build());

            Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(matrix, displayText, x + width - textWidth - 11.5f, y + 10, ColorAssist.getText());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            String displayText = editing ? inputText + "_" : String.valueOf(settings.getMinQuantity());
            float textWidth = Fonts.getSize(13, Fonts.Type.DEFAULT).getStringWidth(displayText);

            if (Calculate.isHovered(mouseX, mouseY, x + width - textWidth - 20, y + 5, textWidth + 15, 15) && button == 0) {
                editing = true;
                inputText = String.valueOf(settings.getMinQuantity());
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (editing) {
                if (keyCode == GLFW.GLFW_KEY_ENTER) {
                    try {
                        int value = Integer.parseInt(inputText);
                        settings.setMinQuantity(Math.max(1, Math.min(64, value)));
                        AutoBuySettingsManager.getInstance().saveSettings(settings.getItemName(), settings);
                        ItemRegistry.reloadSettings();
                    } catch (NumberFormatException ignored) {}
                    editing = false;
                    return true;
                } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    editing = false;
                    return true;
                } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !inputText.isEmpty()) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                    return true;
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            if (editing && Character.isDigit(chr) && inputText.length() < 2) {
                inputText += chr;
                return true;
            }
            return super.charTyped(chr, modifiers);
        }

        @Override
        public boolean isHover(double mouseX, double mouseY) {
            return Calculate.isHovered(mouseX, mouseY, x, y, width, height);
        }
    }
}
