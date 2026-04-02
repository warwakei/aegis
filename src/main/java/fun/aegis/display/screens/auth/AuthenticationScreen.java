package fun.aegis.display.screens.auth;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import fun.aegis.common.auth.AuthenticationManager;
import fun.aegis.display.screens.mainmenu.MainMenu;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.interfaces.QuickImports;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class AuthenticationScreen extends Screen implements QuickImports {
    private final AuthenticationManager authManager;
    private String usernameInput = "";
    private String passwordInput = "";
    private boolean isLoginMode = true;
    private String errorMessage = "";
    private boolean isAuthenticating = false;
    private long authStartTime = 0;
    private boolean focusUsername = true;

    public AuthenticationScreen(AuthenticationManager authManager) {
        super(Text.literal("Authentication"));
        this.authManager = authManager;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        
        context.fill(0, 0, w, h, new Color(20, 20, 20, 255).getRGB());
        
        int centerX = w / 2;
        int centerY = h / 2;
        
        String title = isLoginMode ? "Вход" : "Регистрация";
        Fonts.getSize(32, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), title, centerX, centerY - 100, 0xFFC8C8C8);
        
        drawInputField(context, centerX - 120, centerY - 40, 240, 25, "Имя пользователя", usernameInput, focusUsername);
        drawInputField(context, centerX - 120, centerY + 10, 240, 25, "Пароль", passwordInput, !focusUsername);
        
        if (!errorMessage.isEmpty()) {
            Fonts.getSize(14, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), errorMessage, centerX, centerY + 60, 0xFFFF6464);
        }
        
        drawButton(context, centerX - 110, centerY + 90, 100, 25, isLoginMode ? "Вход" : "Регистрация");
        drawButton(context, centerX + 10, centerY + 90, 100, 25, isLoginMode ? "Регистрация" : "Вход");
        
        if (isAuthenticating) {
            long elapsed = System.currentTimeMillis() - authStartTime;
            String dots = ".".repeat((int) ((elapsed / 500) % 4));
            Fonts.getSize(14, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), "Загрузка" + dots, centerX, centerY + 140, 0xFF64C8FF);
        }
    }

    private void drawInputField(DrawContext context, int x, int y, int w, int h, String placeholder, String value, boolean focused) {
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, w, h).thickness(2).round(4)
                .outlineColor(focused ? 0xFF6496FF : 0xFF646464)
                .color(0xFF1E1E1E).build());
        
        String displayText = value.isEmpty() ? placeholder : value;
        int textColor = value.isEmpty() ? 0xFF646464 : 0xFFC8C8C8;
        Fonts.getSize(14, Fonts.Type.DEFAULT).drawString(context.getMatrices(), displayText, x + 10, y + 6, textColor);
    }

    private void drawButton(DrawContext context, int x, int y, int w, int h, String text) {
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, w, h).thickness(2).round(4)
                .outlineColor(0xFF646464)
                .color(0xFF323232, 0xFF323232, 0xFF505050, 0xFF505050).build());
        
        Fonts.getSize(14, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), text, x + w / 2, y + 5, 0xFFC8C8C8);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int w = this.width;
        int h = this.height;
        int centerX = w / 2;
        int centerY = h / 2;
        
        if (isIn(mouseX, mouseY, centerX - 120, centerY - 40, 240, 25)) {
            focusUsername = true;
            return true;
        }
        if (isIn(mouseX, mouseY, centerX - 120, centerY + 10, 240, 25)) {
            focusUsername = false;
            return true;
        }
        
        if (isIn(mouseX, mouseY, centerX - 110, centerY + 90, 100, 25)) {
            if (isLoginMode) {
                handleLogin();
            } else {
                handleRegister();
            }
            return true;
        }
        
        if (isIn(mouseX, mouseY, centerX + 10, centerY + 90, 100, 25)) {
            isLoginMode = !isLoginMode;
            errorMessage = "";
            usernameInput = "";
            passwordInput = "";
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            focusUsername = !focusUsername;
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (focusUsername && !usernameInput.isEmpty()) {
                usernameInput = usernameInput.substring(0, usernameInput.length() - 1);
            } else if (!focusUsername && !passwordInput.isEmpty()) {
                passwordInput = passwordInput.substring(0, passwordInput.length() - 1);
            }
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (isLoginMode) {
                handleLogin();
            } else {
                handleRegister();
            }
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32 && chr <= 126) {
            if (focusUsername && usernameInput.length() < 20) {
                usernameInput += chr;
            } else if (!focusUsername && passwordInput.length() < 32) {
                passwordInput += chr;
            }
        }
        return true;
    }

    private void handleLogin() {
        if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
            errorMessage = "Заполните все поля";
            return;
        }
        
        if (isAuthenticating) return;
        
        isAuthenticating = true;
        authStartTime = System.currentTimeMillis();
        errorMessage = "";
        
        new Thread(() -> {
            try {
                boolean success = authManager.login(usernameInput, passwordInput);
                if (success && authManager.isAuthenticated()) {
                    mc.execute(() -> mc.setScreen(new MainMenu()));
                } else {
                    errorMessage = "Неверные учетные данные";
                    isAuthenticating = false;
                }
            } catch (Exception e) {
                errorMessage = "Ошибка входа";
                isAuthenticating = false;
            }
        }).start();
    }

    private void handleRegister() {
        if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
            errorMessage = "Заполните все поля";
            return;
        }
        
        if (usernameInput.length() < 3) {
            errorMessage = "Имя минимум 3 символа";
            return;
        }
        
        if (passwordInput.length() < 6) {
            errorMessage = "Пароль минимум 6 символов";
            return;
        }
        
        if (isAuthenticating) return;
        
        isAuthenticating = true;
        authStartTime = System.currentTimeMillis();
        errorMessage = "";
        
        new Thread(() -> {
            try {
                boolean success = authManager.register(usernameInput, passwordInput);
                if (success && authManager.isAuthenticated()) {
                    mc.execute(() -> mc.setScreen(new MainMenu()));
                } else {
                    errorMessage = "Ошибка регистрации";
                    isAuthenticating = false;
                }
            } catch (Exception e) {
                errorMessage = "Пользователь существует";
                isAuthenticating = false;
            }
        }).start();
    }

    private boolean isIn(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
