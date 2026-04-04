package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.Getter;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.string.chat.ChatMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Модуль для автоматического решения математических задач в чате (Jenro Chat Game)
 * Формат: "Решите: X + Y кто первый решит получит: Z$"
 * Отправляет ответ в локальный чат максимально быстро
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JenroChatGame extends ModuleStructure {

    // Паттерн для парсинга: "Решите: 73 + 881" или "Решите: 100 - 50" и т.д.
    private static final Pattern MATH_PATTERN = Pattern.compile(
            "Решите:\\s*(-?\\d+(?:\\.\\d+)?)\\s*([+\\-*/×÷^])\\s*(-?\\d+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE
    );

    // Паттерн для более сложных выражений с несколькими операциями
    private static final Pattern COMPLEX_MATH_PATTERN = Pattern.compile(
            "Решите:\\s*(.+?)\\s+(?:кто|первый)",
            Pattern.CASE_INSENSITIVE
    );

    final SliderSettings priority = new SliderSettings("Приоритет", "Приоритет отправки (0 = мгновенно)")
            .range(0, 50)
            .setValue(0);

    final BooleanSetting globalChat = new BooleanSetting("Глобальный чат", "Отправлять в глобальный чат (с !)")
            .setValue(false);

    final BooleanSetting debugMode = new BooleanSetting("Дебаг", "Показывать отладочные сообщения")
            .setValue(false);

    private long lastSolveTime = 0;
    private String lastExpression = "";
    private String lastAnswer = "";

    public JenroChatGame() {
        super("Jenro ChatGame", "Автоматическое решение математики в чате", ModuleCategory.MISC);
        settings(priority, globalChat, debugMode);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        lastSolveTime = 0;
        lastExpression = "";
        lastAnswer = "";
        ChatMessage.brandmessage("Jenro ChatGame: включён");
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        ChatMessage.brandmessage("Jenro ChatGame: выключен");
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onPacket(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.RECEIVE) return;
        if (!(event.getPacket() instanceof GameMessageS2CPacket packet)) return;

        try {
            String text = packet.content().getString();
            if (text == null) return;

            // Ищем сообщение с математикой
            String cleanText = stripColorCodes(text);
            if (!cleanText.contains("Решите:") && !cleanText.contains("реши:") && !cleanText.contains("реши:")) return;

            // Извлекаем математическое выражение
            String answer = solveExpression(cleanText);
            if (answer == null) return;

            // Проверяем приоритет (задержку)
            long currentTime = System.currentTimeMillis();
            long delay = (long) priority.getValue();
            if (currentTime - lastSolveTime < delay) return;

            // Отправляем ответ
            sendAnswer(answer);

            lastSolveTime = currentTime;
            lastExpression = extractExpression(cleanText);
            lastAnswer = answer;

            if (debugMode.isValue()) {
                ChatMessage.brandmessage("Решено: " + lastExpression + " = " + answer);
            }

        } catch (Exception e) {
            if (debugMode.isValue()) {
                ChatMessage.brandmessage("Ошибка решения: " + e.getMessage());
            }
        }
    }

    /**
     * Извлекает и решает математическое выражение из текста
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private String solveExpression(String text) {
        try {
            // Сначала пробуем простой паттерн с двумя числами
            Matcher simpleMatcher = MATH_PATTERN.matcher(text);
            if (simpleMatcher.find()) {
                double num1 = Double.parseDouble(simpleMatcher.group(1));
                String operator = simpleMatcher.group(2);
                double num2 = Double.parseDouble(simpleMatcher.group(3));

                double result = calculate(num1, operator, num2);
                return formatResult(result);
            }

            // Если не получилось, пробуем сложный паттерн
            Matcher complexMatcher = COMPLEX_MATH_PATTERN.matcher(text);
            if (complexMatcher.find()) {
                String expression = complexMatcher.group(1).trim();
                double result = evaluateComplexExpression(expression);
                return formatResult(result);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Вычисляет простое выражение с двумя числами
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private double calculate(double num1, String operator, double num2) {
        return switch (operator) {
            case "+" -> num1 + num2;
            case "-", "−" -> num1 - num2;
            case "*", "×" -> num1 * num2;
            case "/", "÷" -> num2 != 0 ? num1 / num2 : Double.NaN;
            case "^" -> Math.pow(num1, num2);
            default -> Double.NaN;
        };
    }

    /**
     * Вычисляет сложное выражение (поддержка нескольких операций)
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private double evaluateComplexExpression(String expression) {
        try {
            // Заменяем символы для совместимости
            expression = expression.replace("×", "*")
                    .replace("÷", "/")
                    .replace("−", "-")
                    .replace(",", ".");

            // Простой evaluator для базовых операций
            // Поддерживает: +, -, *, /, ^ с правильным порядком операций
            return evaluateExpression(expression);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * Простой evaluator выражений с поддержкой порядка операций
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private double evaluateExpression(String expr) {
        expr = expr.trim();

        // Убираем скобки рекурсивно
        while (expr.contains("(")) {
            int openParen = expr.lastIndexOf('(');
            int closeParen = expr.indexOf(')', openParen);
            if (closeParen == -1) break;

            String inner = expr.substring(openParen + 1, closeParen);
            double innerResult = evaluateSimple(inner);
            expr = expr.substring(0, openParen) + innerResult + expr.substring(closeParen + 1);
        }

        return evaluateSimple(expr);
    }

    /**
     * Вычисляет выражение без скобок
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private double evaluateSimple(String expr) {
        // Сначала возведение в степень
        while (expr.contains("^")) {
            int idx = expr.indexOf('^');
            double[] nums = extractNumbersAround(expr, idx);
            double result = Math.pow(nums[0], nums[1]);
            expr = replaceExpression(expr, idx, nums, result);
        }

        // Затем умножение и деление
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (c == '*' || c == '/') {
                double[] nums = extractNumbersAround(expr, i);
                double result = c == '*' ? nums[0] * nums[1] : nums[0] / nums[1];
                expr = replaceExpression(expr, i, nums, result);
                i = 0; // Начинаем сначала
            } else {
                i++;
            }
        }

        // Затем сложение и вычитание
        i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (c == '+' || c == '-') {
                // Пропускаем унарный минус в начале
                if (c == '-' && i == 0) {
                    i++;
                    continue;
                }
                double[] nums = extractNumbersAround(expr, i);
                double result = c == '+' ? nums[0] + nums[1] : nums[0] - nums[1];
                expr = replaceExpression(expr, i, nums, result);
                i = 0; // Начинаем сначала
            } else {
                i++;
            }
        }

        return Double.parseDouble(expr.trim());
    }

    /**
     * Извлекает два числа вокруг оператора
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private double[] extractNumbersAround(String expr, int operatorIdx) {
        // Левое число
        int leftStart = operatorIdx - 1;
        while (leftStart >= 0 && (Character.isDigit(expr.charAt(leftStart)) || expr.charAt(leftStart) == '.' || 
               (expr.charAt(leftStart) == '-' && leftStart > 0 && !Character.isDigit(expr.charAt(leftStart - 1))))) {
            leftStart--;
        }
        leftStart++;
        String leftStr = expr.substring(leftStart, operatorIdx).trim();
        double left = Double.parseDouble(leftStr);

        // Правое число
        int rightEnd = operatorIdx + 1;
        while (rightEnd < expr.length() && (Character.isDigit(expr.charAt(rightEnd)) || expr.charAt(rightEnd) == '.')) {
            rightEnd++;
        }
        String rightStr = expr.substring(operatorIdx + 1, rightEnd).trim();
        double right = Double.parseDouble(rightStr);

        return new double[]{left, right};
    }

    /**
     * Заменяет выражение результатом
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private String replaceExpression(String expr, int operatorIdx, double[] nums, double result) {
        int leftStart = operatorIdx - 1;
        while (leftStart >= 0 && (Character.isDigit(expr.charAt(leftStart)) || expr.charAt(leftStart) == '.' ||
               (expr.charAt(leftStart) == '-' && leftStart > 0))) {
            leftStart--;
        }
        leftStart++;

        int rightEnd = operatorIdx + 1;
        while (rightEnd < expr.length() && (Character.isDigit(expr.charAt(rightEnd)) || expr.charAt(rightEnd) == '.')) {
            rightEnd++;
        }

        String resultStr = formatResult(result);
        return expr.substring(0, leftStart) + resultStr + expr.substring(rightEnd);
    }

    /**
     * Форматирует результат (убираем .0 если целое число)
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private String formatResult(double result) {
        if (result == Math.floor(result) && !Double.isInfinite(result)) {
            return String.valueOf((long) result);
        }
        return String.valueOf(result);
    }

    /**
     * Отправляет ответ в чат
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void sendAnswer(String answer) {
        if (globalChat.isValue()) {
            // Глобальный чат с !
            mc.player.networkHandler.sendChatMessage("!" + answer);
        } else {
            // Локальный чат
            mc.player.networkHandler.sendChatMessage(answer);
        }
    }

    /**
     * Извлекает выражение из текста для дебага
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private String extractExpression(String text) {
        Matcher simpleMatcher = MATH_PATTERN.matcher(text);
        if (simpleMatcher.find()) {
            return simpleMatcher.group(1) + " " + simpleMatcher.group(2) + " " + simpleMatcher.group(3);
        }
        return text;
    }

    /**
     * Убирает цветовые коды из текста
     */
    private String stripColorCodes(String text) {
        return text.replaceAll("(?i)[§&][0-9a-fk-or]", "").trim();
    }
}
