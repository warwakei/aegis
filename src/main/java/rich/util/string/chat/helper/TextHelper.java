package rich.util.string.chat.helper;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import rich.util.color.ColorAssist;

public class TextHelper {

    public enum GradientStyle {
        HALF_SPLIT,
        FULL_GRADIENT,
        ASTOLFO,
        TWO_COLOR_FADE
    }

    public static Text applyGradient(String text, GradientStyle style, int color1, int color2, boolean bold) {
        switch (style) {
            case HALF_SPLIT:
                return halfSplitGradient(text, color1, color2, bold);
            case FULL_GRADIENT:
                return fullGradient(text, color1, color2, bold);
            case ASTOLFO:
                return astolfoGradient(text, bold);
            case TWO_COLOR_FADE:
                return twoColorFade(text, color1, color2, bold);
            default:
                return Text.literal(text).styled(s -> s.withColor(color1).withBold(bold));
        }
    }

    private static Text halfSplitGradient(String text, int color1, int color2, boolean bold) {
        MutableText result = Text.literal("");
        int midPoint = text.length() / 2;
        for (int i = 0; i < text.length(); i++) {
            int color = i < midPoint ? color1 : color2;
            result.append(Text.literal(String.valueOf(text.charAt(i))).styled(s -> s.withColor(color).withBold(bold)));
        }
        return result;
    }

    private static Text fullGradient(String text, int color1, int color2, boolean bold) {
        MutableText result = Text.literal("");
        for (int i = 0; i < text.length(); i++) {
            float ratio = (float) i / (text.length() - 1);
            int color = ColorAssist.interpolate(color1, color2, ratio);
            result.append(Text.literal(String.valueOf(text.charAt(i))).styled(s -> s.withColor(color).withBold(bold)));
        }
        return result;
    }

    private static Text astolfoGradient(String text, boolean bold) {
        MutableText result = Text.literal("");
        for (int i = 0; i < text.length(); i++) {
            int color = ColorAssist.astolfo(10, i, 0.7f, 0.7f, 1.0f);
            result.append(Text.literal(String.valueOf(text.charAt(i))).styled(s -> s.withColor(color).withBold(bold)));
        }
        return result;
    }

    private static Text twoColorFade(String text, int color1, int color2, boolean bold) {
        MutableText result = Text.literal("");
        for (int i = 0; i < text.length(); i++) {
            float ratio = (float) i / (text.length() - 1);
            int color = ColorAssist.interpolateColor(color1, color2, ratio);
            result.append(Text.literal(String.valueOf(text.charAt(i))).styled(s -> s.withColor(color).withBold(bold)));
        }
        return result;
    }

    public static Text applyPredefinedGradient(String text, String gradientName, boolean bold) {
        switch (gradientName.toLowerCase()) {
            case "red_blue":
                return applyGradient(text, GradientStyle.HALF_SPLIT, ColorAssist.red, ColorAssist.toColor("#0000FF"), bold);
            case "green_purple":
                return applyGradient(text, GradientStyle.HALF_SPLIT, ColorAssist.green, ColorAssist.toColor("#800080"), bold);
            case "yellow_cyan":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorAssist.yellow, ColorAssist.toColor("#00FFFF"), bold);
            case "orange_magenta":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorAssist.orange, ColorAssist.toColor("#FF00FF"), bold);
            case "astolfo":
                return applyGradient(text, GradientStyle.ASTOLFO, 0, 0, bold);
            case "blue_green_fade":
                return applyGradient(text, GradientStyle.TWO_COLOR_FADE, ColorAssist.toColor("#0000FF"), ColorAssist.green, bold);
            case "purple_red_fade":
                return applyGradient(text, GradientStyle.TWO_COLOR_FADE, ColorAssist.toColor("#800080"), ColorAssist.red, bold);
            case "cyan_orange_fade":
                return applyGradient(text, GradientStyle.TWO_COLOR_FADE, ColorAssist.toColor("#00FFFF"), ColorAssist.orange, bold);
            case "white_black":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorAssist.colorForTextWhite$(), ColorAssist.colorForRectsBlack$(), bold);
            case "custom_purple":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorAssist.colorForTextCustom$(), ColorAssist.colorForRectsCustom$(), bold);
            case "black_light_purple":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorAssist.colorForRectsBlack$(), ColorAssist.toColor("#DA70D6"), bold);
            case "dark_red_bright_red":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorAssist.toColor("#8B0000"), ColorAssist.red, bold);
            case "dark_red":
                return applyGradient(text, GradientStyle.HALF_SPLIT, ColorAssist.toColor("#8B0000"), ColorAssist.toColor("#8B0000"), bold);
            case "red_white":
                return applyGradient(text, GradientStyle.HALF_SPLIT, ColorAssist.red, ColorAssist.colorForTextWhite$(), bold);
            case "purple_bright_pink":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorAssist.toColor("#800080"), ColorAssist.toColor("#FF69B4"), bold);
            case "pink_dark_pink":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorAssist.toColor("#FFC1CC"), ColorAssist.toColor("#C71585"), bold);
            case "bright_red":
                return applyGradient(text, GradientStyle.HALF_SPLIT, ColorAssist.red, ColorAssist.red, bold);
            case "dark_green_bright_green":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorAssist.toColor("#006400"), ColorAssist.green, bold);
            case "red_orange":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorAssist.red, ColorAssist.orange, bold);
            case "turquoise_blue":
                return applyGradient(text, GradientStyle.FULL_GRADIENT, ColorAssist.toColor("#40E0D0"), ColorAssist.toColor("#0000FF"), bold);
            default:
                return Text.literal(text).styled(s -> s.withColor(ColorAssist.colorForTextWhite$()).withBold(bold));
        }
    }
}