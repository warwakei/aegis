package fun.aegis.utils.display.font;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import fun.aegis.Aegis;

import java.awt.*;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Fonts {

    @SneakyThrows
    public static FontRenderer create(float size, String name) {
        String path = "assets/minecraft/fonts/" + name + ".ttf";
        
        InputStream inputStream = Aegis.class.getClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            System.err.println("Font file not found: " + path);
            throw new IllegalArgumentException("Font file not found: " + path);
        }

        try (InputStream is = inputStream) {
            byte[] fontData = is.readAllBytes();
            Font font = Font.createFont(Font.TRUETYPE_FONT, new java.io.ByteArrayInputStream(fontData)).deriveFont(size / 2f);
            return new FontRenderer(font, size / 2f);
        }
    }

    private static final Map<FontKey, FontRenderer> fontCache = new HashMap<>();

    public static void init() {
        for (Type type : Type.values()) {
            for (int size = 4; size <= 32; size++) {
                fontCache.put(new FontKey(size, type), create(size, type.getType()));
            }
        }
    }



    public static FontRenderer getSize(int size) {
        return getSize(size, Type.INST);
    }

    public static FontRenderer getSize(int size, Type type) {
        return fontCache.computeIfAbsent(new FontKey(size, type), k -> create(size, type.getType()));
    }

    @Getter
    @RequiredArgsConstructor
    public enum Type {
        DEFAULT("sf_medium"),
        REGULAR("sf_regular"),
        SEMI("sf_semibold"),
        BOLD("sf_bold"),
        BOLDED("bold"),
        MANROPEEXTRABOLD("manropeextrabold"),
        MANROPEBOLD("manropebold"),
        CATACLYSMREGULAR("cataclysm_regular"),
        ICONCATACLYSMREG("iconcataclysmreg"),
        INST("suisseintl"),
        ICONS("icons"),
        ICONSTYPENEW("icon2"),
        GUIICONS("categoryicons"),
        ICONSCATEGORY("categoryicons"),;

        private final String type;
    }

    private record FontKey(int size, Type type) {
    }
}
