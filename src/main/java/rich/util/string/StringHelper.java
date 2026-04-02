package rich.util.string;

import lombok.experimental.UtilityClass;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *  © 2025 Copyright Rich Client 2.0
 *        All Rights Reserved ®
 */

@UtilityClass
public class StringHelper {
    public String randomString(int length) {
        return IntStream.range(0, length)
                .mapToObj(operand -> String.valueOf((char) new Random().nextInt('a', 'z' + 1)))
                .collect(Collectors.joining());
    }

    public String getBindName(int key) {
        if (key < 0) return "N/A";
        return PlayerInteractionHelper.getKeyType(key).createFromCode(key).getTranslationKey().replace("key.keyboard.", "")
                .replace("key.mouse.", "mouse ").replace(".", " ").toUpperCase();
    }

    public String getUserRole() {
        return switch ("DEVELOPER") {
            case "Разработчик" -> "Developer";
            case "Администратор" -> "Admin";
            default -> "User";
        };
    }

    public String getDuration(int time) {
        int mins = time / 60;
        String sec = String.format("%02d", time % 60);
        return mins + ":" + sec;
    }

}