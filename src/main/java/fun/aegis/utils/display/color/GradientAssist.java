package fun.aegis.utils.display.color;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class GradientAssist {
    public static int[] getGradientColors(String itemName) {
        switch (itemName) {
            case "Дезориентация":
                return new int[]{ColorAssist.toColor("#00FF6A"), ColorAssist.toColor("#2761F5"), ColorAssist.toColor("#B80081")};
            case "Божья аура":
                return new int[]{ColorAssist.toColor("#FBFF00"), ColorAssist.toColor("#FF9873"), ColorAssist.toColor("#FF9873")};
            case "Пласт":
                return new int[]{ColorAssist.toColor("#3B005E"), ColorAssist.toColor("#8900DB"), ColorAssist.toColor("#8900DB")};
            case "Трапка":
                return new int[]{ColorAssist.toColor("#8F0000"), ColorAssist.toColor("#DE0000"), ColorAssist.toColor("#FF0000")};
            case "Огненный смерч":
                return new int[]{ColorAssist.toColor("#ED0000"), ColorAssist.toColor("#FF682B"), ColorAssist.toColor("#FF682B")};
            case "Снежок заморозка":
                return new int[]{ColorAssist.toColor("#2BFFCE"), ColorAssist.toColor("#00E6B0"), ColorAssist.toColor("#00E6B0")};
            case "Явная пыль":
                return new int[]{ColorAssist.toColor("#00FFFA"), ColorAssist.toColor("#00FF95"), ColorAssist.toColor("#00FF95")};
            case "Зелье мочи Флеша":
                return new int[]{ColorAssist.toColor("#00799E"), ColorAssist.toColor("#00FFFF")};
            case "Зелье медика":
                return new int[]{ColorAssist.toColor("#8A007D"), ColorAssist.toColor("#FF00E8")};
            case "Зелье агента":
                return new int[]{ColorAssist.toColor("#D99A00"), ColorAssist.toColor("#FFEE00")};
            case "Зелье победителя":
                return new int[]{ColorAssist.toColor("#00821F"), ColorAssist.toColor("#00FF3D")};
            case "Зелье киллера":
                return new int[]{ColorAssist.toColor("#9C0000"), ColorAssist.toColor("#FF0F0F")};
            case "Зелье отрыжки":
                return new int[]{ColorAssist.toColor("#CC4E00"), ColorAssist.toColor("#FFA100")};
            case "Зелье серной кислоты":
                return new int[]{ColorAssist.toColor("#319C00"), ColorAssist.toColor("#4AEB00")};
            case "Зелье вспышки":
                return new int[]{ColorAssist.toColor("#D48600"), ColorAssist.toColor("#FFE600")};
            default:
                return new int[]{ColorAssist.getText()};
        }
    }

    public static MutableText applyGradientToText(String text, int[] colors, boolean addStarPrefix) {
        MutableText result = Text.empty();
        String fullText = addStarPrefix ? "[★] " + text : text;

        if (colors.length == 2 && addStarPrefix) {
            result.append(Text.literal("[★] ").formatted(Formatting.RESET).styled(style -> style.withColor(colors[0])));
            result.append(Text.literal(text).formatted(Formatting.RESET).styled(style -> style.withColor(colors[1])));
        } else if (colors.length == 0 || colors.length == 1 && colors[0] == ColorAssist.getText()) {
            result.append(Text.literal(fullText).formatted(Formatting.RESET));
        } else {
            int length = fullText.length();
            int colorCount = colors.length;
            for (int i = 0; i < length; i++) {
                float progress = (float) i / (length - 1);
                int colorIndex = (int) (progress * (colorCount - 1));
                int color1 = colors[colorIndex];
                int color2 = colors[Math.min(colorIndex + 1, colorCount - 1)];
                float segmentProgress = (progress * (colorCount - 1)) - colorIndex;
                int interpolatedColor = ColorAssist.interpolateColor(color1, color2, segmentProgress);
                result.append(Text.literal(String.valueOf(fullText.charAt(i))).formatted(Formatting.RESET).styled(style -> style.withColor(interpolatedColor)));
            }
        }

        return result;
    }
}
