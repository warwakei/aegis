package rich.screens.clickgui.profile;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Path;

public final class ProfileAppearanceScreen extends Screen {
    private final Screen parent;
    private String status = "Картинки хранятся на этом ПК";
    private int statusColor = 0xFFAABBCC;

    public ProfileAppearanceScreen(Screen parent) {
        super(Text.literal("Оформление профиля"));
        this.parent = parent;
    }

    @Override protected void init() {
        int x = width / 2 - 104;
        int y = height / 2 - 36;
        addDrawableChild(ButtonWidget.builder(Text.literal("Выбрать аватарку…"), button -> select(ProfileImages.Kind.AVATAR))
                .dimensions(x, y, 136, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить"), button -> reset(ProfileImages.Kind.AVATAR))
                .dimensions(x + 142, y, 66, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Выбрать фон…"), button -> select(ProfileImages.Kind.BACKGROUND))
                .dimensions(x, y + 25, 136, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить"), button -> reset(ProfileImages.Kind.BACKGROUND))
                .dimensions(x + 142, y + 25, 66, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), button -> close())
                .dimensions(x, y + 70, 208, 20).build());
    }

    private void select(ProfileImages.Kind kind) {
        try (var stack = MemoryStack.stackPush()) {
            var filters = stack.pointers(stack.UTF8("*.png"), stack.UTF8("*.jpg"), stack.UTF8("*.jpeg"));
            String selected = TinyFileDialogs.tinyfd_openFileDialog("Выбрать картинку профиля", "", filters, "PNG / JPEG", false);
            if (selected == null) return;
            ProfileImages.select(kind, Path.of(selected));
            status = "Картинка сохранена";
            statusColor = 0xFF91E9AB;
        } catch (IOException | RuntimeException | LinkageError e) {
            status = e instanceof IOException ? e.getMessage() : "Не удалось выбрать картинку";
            statusColor = 0xFFFF9E9E;
        }
    }

    private void reset(ProfileImages.Kind kind) {
        try {
            ProfileImages.reset(kind);
            status = "Стандартное оформление восстановлено";
            statusColor = 0xFF91E9AB;
        } catch (IOException e) {
            status = "Не удалось сбросить картинку";
            statusColor = 0xFFFF9E9E;
        }
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, height / 2 - 58, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, textRenderer.trimToWidth(status, width - 16),
                width / 2, height / 2 + 16, statusColor);
    }

    @Override public void close() {
        if (client != null) client.setScreen(parent);
    }
}
