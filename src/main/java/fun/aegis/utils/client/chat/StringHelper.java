package fun.aegis.utils.client.chat;

import fun.aegis.utils.display.font.FontRenderer;
import
lombok.experimental.UtilityClass;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
@UtilityClass
public class StringHelper {
    public java.lang.String randomString(int length) {
        return IntStream.range(0, length)
                .mapToObj(operand -> java.lang.String.valueOf((char) new Random().nextInt('a', 'z' + 1)))
                .collect(Collectors.joining());
    }

    public java.lang.String getBindName(int key) {
        if (key < 0) return "N/A";
        return PlayerInteractionHelper.getKeyType(key).createFromCode(key).getTranslationKey().replace("key.keyboard.", "")
                .replace("key.mouse.", "mouse ").replace(".", " ").toUpperCase();
    }

    public java.lang.String wrap(java.lang.String input, int width, int size) {
        java.lang.String[] words = input.split(" ");
        StringBuilder output = new StringBuilder();
        float lineWidth = 0;
        for (java.lang.String word : words) {
            float wordWidth = Fonts.getSize(size).getStringWidth(word);
            if (lineWidth + wordWidth > width) {
                output.append("\n");
                lineWidth = 0;
            } else if (lineWidth > 0) {
                output.append(" ");
                lineWidth += Fonts.getSize(size).getStringWidth(" ");
            }
            output.append(word);
            lineWidth += wordWidth;
        }
        return output.toString();
    }

    public java.lang.String getUserRole() {
        return switch ("DEVELOPER") {
            case "Разработчик" -> "Developer";
            case "Администратор" -> "Admin";
            default -> "User";
        };
    }

    public java.lang.String getDuration(int time) {
        int mins = time / 60;
        java.lang.String sec = java.lang.String.format("%02d", time % 60);
        return mins + ":" + sec;
    }

    public java.lang.String trimToWidth(java.lang.String text, float maxWidth, FontRenderer font) {
        StringBuilder builder = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (font.getStringWidth(builder.toString() + c) > maxWidth) break;
            builder.append(c);
        }
        return builder.toString();
    }
}
