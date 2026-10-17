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
 * решает матешу в чате
 * шлет ответ побыстрее
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JenroChatGame extends ModuleStructure {

    // патерн решите 73 + 881
    private static final Pattern MATH_PATTERN = Pattern.compile(
            "Решите:\\s*(-?\\d+(?:\\.\\d+)?)\\s*([+\\-*/×÷^])\\s*(-?\\d+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE
    );

    // патерн для сложных примеров
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

            // ищем матешу в чате
            String cleanText = stripColorCodes(text);
            if (!cleanText.contains("Решите:") && !cleanText.contains("реши:") && !cleanText.contains("реши:")) return;

            // тащим пример оттуда
            String answer = solveExpression(cleanText);
            if (answer == null) return;

            // чек дилэя
            long currentTime = System.currentTimeMillis();
            long delay = (long) priority.getValue();
            if (currentTime - lastSolveTime < delay) return;

            // шлем ответ
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
     * достаем пример и решаем
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private String solveExpression(String text) {
        try {
            // снача простой патерн
            Matcher simpleMatcher = MATH_PATTERN.matcher(text);
            if (simpleMatcher.find()) {
                double num1 = Double.parseDouble(simpleMatcher.group(1));
                String operator = simpleMatcher.group(2);
                double num2 = Double.parseDouble(simpleMatcher.group(3));

                double result = calculate(num1, operator, num2);
                return formatResult(result);
            }

            // не вышло пробуем сложный
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
     * считаем простое с 2 числами
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
     * считаем сложное с кучей знаков
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private double evaluateComplexExpression(String expression) {
        try {
            // меняем знаки под себя
            expression = expression.replace("×", "*")
                    .replace("÷", "/")
                    .replace("−", "-")
                    .replace(",", ".");

            // считатель базовых знаков
            return evaluateExpression(expression);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * считалка с порядком знаков
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private double evaluateExpression(String expr) {
        expr = expr.trim();

        // скобки рекурсивно хаваем
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
     * считаем без скобок
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private double evaluateSimple(String expr) {
        // снача степень
        while (expr.contains("^")) {
            int idx = expr.indexOf('^');
            double[] nums = extractNumbersAround(expr, idx);
            double result = Math.pow(nums[0], nums[1]);
            expr = replaceExpression(expr, idx, nums, result);
        }

        // потом умнож деление
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (c == '*' || c == '/') {
                double[] nums = extractNumbersAround(expr, i);
                double result = c == '*' ? nums[0] * nums[1] : nums[0] / nums[1];
                expr = replaceExpression(expr, i, nums, result);
                i = 0; // с ноля погнали
            } else {
                i++;
            }
        }

        // потом плюс минус
        i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (c == '+' || c == '-') {
                // минус в начале скипаем
                if (c == '-' && i == 0) {
                    i++;
                    continue;
                }
                double[] nums = extractNumbersAround(expr, i);
                double result = c == '+' ? nums[0] + nums[1] : nums[0] - nums[1];
                expr = replaceExpression(expr, i, nums, result);
                i = 0; // с ноля погнали
            } else {
                i++;
            }
        }

        return Double.parseDouble(expr.trim());
    }

    /**
     * тащим 2 числа у знака
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private double[] extractNumbersAround(String expr, int operatorIdx) {
        // число слева
        int leftStart = operatorIdx - 1;
        while (leftStart >= 0 && (Character.isDigit(expr.charAt(leftStart)) || expr.charAt(leftStart) == '.' || 
               (expr.charAt(leftStart) == '-' && leftStart > 0 && !Character.isDigit(expr.charAt(leftStart - 1))))) {
            leftStart--;
        }
        leftStart++;
        String leftStr = expr.substring(leftStart, operatorIdx).trim();
        double left = Double.parseDouble(leftStr);

        // число справа
        int rightEnd = operatorIdx + 1;
        while (rightEnd < expr.length() && (Character.isDigit(expr.charAt(rightEnd)) || expr.charAt(rightEnd) == '.')) {
            rightEnd++;
        }
        String rightStr = expr.substring(operatorIdx + 1, rightEnd).trim();
        double right = Double.parseDouble(rightStr);

        return new double[]{left, right};
    }

    /**
     * меняем пример на ответ
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
     * красим ответ без .0
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private String formatResult(double result) {
        if (result == Math.floor(result) && !Double.isInfinite(result)) {
            return String.valueOf((long) result);
        }
        return String.valueOf(result);
    }

    /**
     * шлем ответ в чат
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void sendAnswer(String answer) {
        if (globalChat.isValue()) {
            // глобал с воскл знаком
            mc.player.networkHandler.sendChatMessage("!" + answer);
        } else {
            // локалка обычная
            mc.player.networkHandler.sendChatMessage(answer);
        }
    }

    /**
     * пример для дебага тащим
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
     * режем цвета с текста
     */
    private String stripColorCodes(String text) {
        return text.replaceAll("(?i)[§&][0-9a-fk-or]", "").trim();
    }
}
