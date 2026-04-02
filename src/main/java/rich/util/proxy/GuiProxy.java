package rich.util.proxy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import rich.util.config.impl.proxy.ProxyConfig;

public class GuiProxy extends Screen {
    private boolean isSocks4 = false;

    private TextFieldWidget ipPort;
    private TextFieldWidget username;
    private TextFieldWidget password;
    private CheckboxWidget enabledCheck;

    private Screen parentScreen;

    private String msg = "";

    private int[] positionY;
    private int positionX;

    private static String text_proxy = Text.translatable("PROXY").getString();

    public GuiProxy(Screen parentScreen) {
        super(Text.literal(text_proxy));
        this.parentScreen = parentScreen;
    }

    private static boolean isValidIpPort(String ipP) {
        if (ipP == null || ipP.isEmpty()) return false;
        String[] split = ipP.split(":");
        if (split.length > 1) {
            if (!StringUtils.isNumeric(split[1])) return false;
            try {
                int port = Integer.parseInt(split[1]);
                if (port < 0 || port > 0xFFFF) return false;
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private boolean checkProxy() {
        if (!isValidIpPort(ipPort.getText())) {
            this.ipPort.setFocused(true);
            return false;
        }
        return true;
    }

    private void centerButtons(int amount, int buttonLength, int gap) {
        positionX = (this.width / 2) - (buttonLength / 2);
        positionY = new int[amount];
        int center = (this.height + amount * gap) / 2;
        int buttonStarts = center - (amount * gap);
        for (int i = 0; i != amount; i++) {
            positionY[i] = buttonStarts + (gap * i);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEscape()) {
            MinecraftClient.getInstance().setScreen(parentScreen);
            return true;
        }
        super.keyPressed(input);
        msg = "";
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.render(context, mouseX, mouseY, partialTicks);

        if (enabledCheck.isChecked() && !isValidIpPort(ipPort.getText())) {
            enabledCheck.onPress(null);
        }

        context.drawTextWithShadow(this.textRenderer, Text.translatable("Введите айпи адрес и порт. Пример ниже").getString(), this.width / 2 - 106, positionY[3] - 15, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("Айпи:Порт ▸").getString(), this.width / 2 - 140, positionY[3] + 15, 0xA0A0A0);

        this.ipPort.render(context, mouseX, mouseY, partialTicks);

        context.drawTextWithShadow(this.textRenderer, Text.translatable("Никнейм ▸").getString(), this.width / 2 - 131, positionY[4] + 15, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("Пароль ▸").getString(), this.width / 2 - 126, positionY[5] + 15, 0xA0A0A0);
        this.username.render(context, mouseX, mouseY, partialTicks);
        this.password.render(context, mouseX, mouseY, partialTicks);

        context.drawCenteredTextWithShadow(this.textRenderer, msg, this.width / 2, positionY[6] + 5, 0xA0A0A0);
    }

    @Override
    public void init() {
        int buttonLength = 160;
        centerButtons(10, buttonLength, 26);

        ProxyConfig config = ProxyConfig.getInstance();
        Proxy currentProxy = config.getDefaultProxy();

        isSocks4 = currentProxy.type == Proxy.ProxyType.SOCKS4;

        this.ipPort = new TextFieldWidget(this.textRenderer, positionX, positionY[3] + 10, buttonLength, 20, Text.literal(""));
        this.ipPort.setText(currentProxy.ipPort);
        this.ipPort.setMaxLength(1024);
        this.ipPort.setFocused(true);
        this.addSelectableChild(this.ipPort);

        this.username = new TextFieldWidget(this.textRenderer, positionX, positionY[4] + 10, buttonLength, 20, Text.literal(""));
        this.username.setMaxLength(255);
        this.username.setText(currentProxy.username);
        this.addSelectableChild(this.username);

        this.password = new TextFieldWidget(this.textRenderer, positionX, positionY[5] + 10, buttonLength, 20, Text.literal(""));
        this.password.setMaxLength(255);
        this.password.setText(currentProxy.password);
        this.addSelectableChild(this.password);

        int posXButtons = (this.width / 2) - (((buttonLength / 2) * 3) / 2);

        ButtonWidget apply = ButtonWidget.builder(Text.translatable("Применить"), button -> {
            ProxyConfig cfg = ProxyConfig.getInstance();

            if (enabledCheck.isChecked()) {
                if (checkProxy()) {
                    Proxy newProxy = new Proxy(isSocks4, ipPort.getText(), username.getText(), password.getText());
                    cfg.setDefaultProxy(newProxy);
                    cfg.setProxyEnabled(true);
                    cfg.save();
                    MinecraftClient.getInstance().setScreen(new MultiplayerScreen(new TitleScreen()));
                }
            } else {
                Proxy newProxy = new Proxy(isSocks4, ipPort.getText(), username.getText(), password.getText());
                cfg.setDefaultProxy(newProxy);
                cfg.setProxyEnabled(false);
                cfg.save();
                MinecraftClient.getInstance().setScreen(new MultiplayerScreen(new TitleScreen()));
            }
        }).dimensions(posXButtons + (buttonLength / 2 - 62) * 2, positionY[7] - 10, buttonLength / 2 + 3, 20).build();
        this.addDrawableChild(apply);

        CheckboxWidget.Builder checkboxBuilder = CheckboxWidget.builder(Text.translatable("Включить прокси"), this.textRenderer);
        checkboxBuilder.pos((this.width / 2 - 34) - (13 + textRenderer.getWidth(Text.translatable("Включить прокси"))) / 2, positionY[7] + 15);
        if (config.isProxyEnabled()) {
            checkboxBuilder.checked(true);
        }
        this.enabledCheck = checkboxBuilder.build();
        this.addDrawableChild(this.enabledCheck);

        ButtonWidget cancel = ButtonWidget.builder(Text.translatable("Отменить"), (button) -> {
            MinecraftClient.getInstance().setScreen(parentScreen);
        }).dimensions(posXButtons + (buttonLength / 2 - 16) * 2, positionY[7] - 10, buttonLength / 2 - 3, 20).build();
        this.addDrawableChild(cancel);
    }

    @Override
    public void close() {
        msg = "";
        MinecraftClient.getInstance().setScreen(parentScreen);
    }
}