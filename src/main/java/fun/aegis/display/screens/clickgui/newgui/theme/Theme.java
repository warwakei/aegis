package fun.aegis.display.screens.clickgui.newgui.theme;

import lombok.Getter;
import lombok.Setter;
import java.awt.Color;

@Getter
@Setter
public class Theme {
    public static final Theme DARK = new Theme("Dark",
            new Color(138, 99, 255),    // color (primary)
            new Color(99, 138, 255),    // secondColor
            new Color(18, 18, 24),      // backgroundColor
            new Color(28, 28, 36),      // foregroundColor
            new Color(38, 38, 48),      // foregroundLight
            new Color(22, 22, 28),      // foregroundDark
            new Color(48, 48, 58),      // foregroundGray
            new Color(255, 255, 255),   // white
            new Color(180, 180, 190),   // whiteGray
            new Color(100, 100, 110),   // gray
            new Color(140, 140, 150),   // grayLight
            new Color(58, 58, 68),      // foregroundLightStroke
            new Color(38, 38, 48)       // foregroundStroke
    );

    public static final Theme LIGHT = new Theme("Light",
            new Color(138, 99, 255),
            new Color(99, 138, 255),
            new Color(240, 240, 245),
            new Color(255, 255, 255),
            new Color(245, 245, 250),
            new Color(235, 235, 240),
            new Color(220, 220, 225),
            new Color(30, 30, 40),
            new Color(80, 80, 90),
            new Color(160, 160, 170),
            new Color(120, 120, 130),
            new Color(200, 200, 210),
            new Color(220, 220, 230)
    );

    public static final Theme PURPLE = new Theme("Purple",
            new Color(200, 100, 255),
            new Color(150, 100, 255),
            new Color(25, 15, 35),
            new Color(35, 25, 45),
            new Color(45, 35, 55),
            new Color(30, 20, 40),
            new Color(55, 45, 65),
            new Color(255, 255, 255),
            new Color(200, 190, 210),
            new Color(120, 110, 130),
            new Color(160, 150, 170),
            new Color(65, 55, 75),
            new Color(45, 35, 55)
    );

    public static final Theme BLUE = new Theme("Blue",
            new Color(100, 150, 255),
            new Color(100, 200, 255),
            new Color(15, 25, 40),
            new Color(25, 35, 50),
            new Color(35, 45, 60),
            new Color(20, 30, 45),
            new Color(45, 55, 70),
            new Color(255, 255, 255),
            new Color(190, 200, 210),
            new Color(110, 120, 130),
            new Color(150, 160, 170),
            new Color(55, 65, 80),
            new Color(35, 45, 60)
    );

    public static final Theme CYAN = new Theme("Cyan",
            new Color(100, 200, 200),
            new Color(100, 220, 220),
            new Color(15, 30, 35),
            new Color(25, 40, 45),
            new Color(35, 50, 55),
            new Color(20, 35, 40),
            new Color(45, 60, 65),
            new Color(255, 255, 255),
            new Color(190, 210, 210),
            new Color(110, 130, 130),
            new Color(150, 170, 170),
            new Color(55, 75, 80),
            new Color(35, 55, 60)
    );

    public static final Theme GREEN = new Theme("Green",
            new Color(100, 200, 100),
            new Color(150, 220, 100),
            new Color(15, 30, 15),
            new Color(25, 40, 25),
            new Color(35, 50, 35),
            new Color(20, 35, 20),
            new Color(45, 60, 45),
            new Color(255, 255, 255),
            new Color(190, 210, 190),
            new Color(110, 130, 110),
            new Color(150, 170, 150),
            new Color(55, 75, 55),
            new Color(35, 55, 35)
    );

    public static final Theme RED = new Theme("Red",
            new Color(255, 100, 100),
            new Color(255, 150, 100),
            new Color(40, 15, 15),
            new Color(50, 25, 25),
            new Color(60, 35, 35),
            new Color(45, 20, 20),
            new Color(70, 45, 45),
            new Color(255, 255, 255),
            new Color(210, 190, 190),
            new Color(130, 110, 110),
            new Color(170, 150, 150),
            new Color(80, 55, 55),
            new Color(60, 35, 35)
    );

    public static final Theme ORANGE = new Theme("Orange",
            new Color(255, 150, 100),
            new Color(255, 180, 100),
            new Color(40, 25, 15),
            new Color(50, 35, 25),
            new Color(60, 45, 35),
            new Color(45, 30, 20),
            new Color(70, 55, 45),
            new Color(255, 255, 255),
            new Color(210, 200, 190),
            new Color(130, 120, 110),
            new Color(170, 160, 150),
            new Color(80, 65, 55),
            new Color(60, 45, 35)
    );

    public static final Theme NEON = new Theme("Neon",
            new Color(0, 255, 255),
            new Color(255, 0, 255),
            new Color(10, 10, 20),
            new Color(20, 20, 30),
            new Color(30, 30, 40),
            new Color(15, 15, 25),
            new Color(40, 40, 50),
            new Color(255, 255, 255),
            new Color(200, 200, 210),
            new Color(100, 100, 120),
            new Color(150, 150, 170),
            new Color(50, 50, 70),
            new Color(30, 30, 50)
    );

    public static final Theme SUNSET = new Theme("Sunset",
            new Color(255, 100, 50),
            new Color(255, 150, 80),
            new Color(30, 15, 10),
            new Color(40, 20, 15),
            new Color(50, 30, 20),
            new Color(35, 18, 12),
            new Color(60, 40, 30),
            new Color(255, 255, 255),
            new Color(220, 200, 180),
            new Color(140, 120, 100),
            new Color(180, 160, 140),
            new Color(70, 50, 40),
            new Color(50, 30, 20)
    );

    public static final Theme OCEAN = new Theme("Ocean",
            new Color(50, 150, 200),
            new Color(100, 180, 220),
            new Color(10, 20, 35),
            new Color(20, 30, 50),
            new Color(30, 40, 60),
            new Color(15, 25, 40),
            new Color(40, 50, 70),
            new Color(255, 255, 255),
            new Color(190, 210, 230),
            new Color(110, 130, 150),
            new Color(150, 170, 190),
            new Color(50, 70, 90),
            new Color(30, 50, 70)
    );

    public static final Theme FOREST = new Theme("Forest",
            new Color(80, 180, 100),
            new Color(120, 200, 140),
            new Color(15, 25, 15),
            new Color(25, 35, 25),
            new Color(35, 45, 35),
            new Color(20, 30, 20),
            new Color(45, 55, 45),
            new Color(255, 255, 255),
            new Color(200, 220, 200),
            new Color(120, 140, 120),
            new Color(160, 180, 160),
            new Color(60, 80, 60),
            new Color(40, 60, 40)
    );

    public static final Theme LAVENDER = new Theme("Lavender",
            new Color(180, 120, 200),
            new Color(200, 150, 220),
            new Color(25, 15, 35),
            new Color(35, 25, 45),
            new Color(45, 35, 55),
            new Color(30, 20, 40),
            new Color(55, 45, 65),
            new Color(255, 255, 255),
            new Color(210, 200, 220),
            new Color(130, 120, 140),
            new Color(170, 160, 180),
            new Color(75, 65, 85),
            new Color(55, 45, 65)
    );

    public static final Theme CORAL = new Theme("Coral",
            new Color(255, 120, 100),
            new Color(255, 160, 140),
            new Color(35, 15, 15),
            new Color(45, 25, 25),
            new Color(55, 35, 35),
            new Color(40, 20, 20),
            new Color(65, 45, 45),
            new Color(255, 255, 255),
            new Color(220, 190, 190),
            new Color(140, 110, 110),
            new Color(180, 150, 150),
            new Color(85, 55, 55),
            new Color(65, 35, 35)
    );

    public static final Theme MINT = new Theme("Mint",
            new Color(100, 220, 180),
            new Color(150, 240, 200),
            new Color(15, 30, 25),
            new Color(25, 40, 35),
            new Color(35, 50, 45),
            new Color(20, 35, 30),
            new Color(45, 60, 55),
            new Color(255, 255, 255),
            new Color(200, 230, 220),
            new Color(120, 150, 140),
            new Color(160, 190, 180),
            new Color(60, 90, 80),
            new Color(40, 70, 60)
    );

    public static final Theme PEACH = new Theme("Peach",
            new Color(255, 180, 120),
            new Color(255, 200, 150),
            new Color(40, 25, 15),
            new Color(50, 35, 25),
            new Color(60, 45, 35),
            new Color(45, 30, 20),
            new Color(70, 55, 45),
            new Color(255, 255, 255),
            new Color(220, 210, 190),
            new Color(140, 130, 110),
            new Color(180, 170, 150),
            new Color(85, 70, 55),
            new Color(65, 50, 35)
    );

    public static final Theme MIDNIGHT = new Theme("Midnight",
            new Color(100, 150, 255),
            new Color(150, 180, 255),
            new Color(10, 10, 25),
            new Color(20, 20, 40),
            new Color(30, 30, 50),
            new Color(15, 15, 35),
            new Color(40, 40, 60),
            new Color(255, 255, 255),
            new Color(190, 200, 220),
            new Color(110, 120, 140),
            new Color(150, 160, 180),
            new Color(50, 60, 80),
            new Color(30, 40, 60)
    );

    public static final Theme AURORA = new Theme("Aurora",
            new Color(100, 255, 150),
            new Color(150, 200, 255),
            new Color(15, 25, 20),
            new Color(25, 35, 30),
            new Color(35, 45, 40),
            new Color(20, 30, 25),
            new Color(45, 55, 50),
            new Color(255, 255, 255),
            new Color(200, 220, 210),
            new Color(120, 140, 130),
            new Color(160, 180, 170),
            new Color(60, 80, 70),
            new Color(40, 60, 50)
    );

    public static final Theme CYBERPUNK = new Theme("Cyberpunk",
            new Color(255, 0, 200),
            new Color(0, 255, 200),
            new Color(15, 10, 20),
            new Color(25, 15, 35),
            new Color(35, 25, 45),
            new Color(20, 12, 30),
            new Color(45, 35, 55),
            new Color(255, 255, 255),
            new Color(200, 180, 220),
            new Color(120, 100, 140),
            new Color(160, 140, 180),
            new Color(65, 45, 75),
            new Color(45, 25, 55)
    );

    public static final Theme SAKURA = new Theme("Sakura",
            new Color(255, 150, 180),
            new Color(255, 180, 200),
            new Color(35, 20, 25),
            new Color(45, 30, 35),
            new Color(55, 40, 45),
            new Color(40, 25, 30),
            new Color(65, 50, 55),
            new Color(255, 255, 255),
            new Color(220, 200, 210),
            new Color(140, 120, 130),
            new Color(180, 160, 170),
            new Color(85, 65, 75),
            new Color(65, 45, 55)
    );

    private final String name;
    private Color color;
    private Color secondColor;
    private Color backgroundColor;
    private Color foregroundColor;
    private Color foregroundLight;
    private Color foregroundDark;
    private Color foregroundGray;
    private Color white;
    private Color whiteGray;
    private Color gray;
    private Color grayLight;
    private Color foregroundLightStroke;
    private Color foregroundStroke;

    public Theme(String name, Color color, Color secondColor, Color backgroundColor,
                 Color foregroundColor, Color foregroundLight, Color foregroundDark,
                 Color foregroundGray, Color white, Color whiteGray, Color gray,
                 Color grayLight, Color foregroundLightStroke, Color foregroundStroke) {
        this.name = name;
        this.color = color;
        this.secondColor = secondColor;
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        this.foregroundLight = foregroundLight;
        this.foregroundDark = foregroundDark;
        this.foregroundGray = foregroundGray;
        this.white = white;
        this.whiteGray = whiteGray;
        this.gray = gray;
        this.grayLight = grayLight;
        this.foregroundLightStroke = foregroundLightStroke;
        this.foregroundStroke = foregroundStroke;
    }

    public int getColorInt() {
        return color.getRGB();
    }

    public int getSecondColorInt() {
        return secondColor.getRGB();
    }

    public int getBackgroundColorInt() {
        return backgroundColor.getRGB();
    }

    public int getForegroundColorInt() {
        return foregroundColor.getRGB();
    }

    public int getForegroundLightInt() {
        return foregroundLight.getRGB();
    }

    public int getForegroundDarkInt() {
        return foregroundDark.getRGB();
    }

    public int getWhiteInt() {
        return white.getRGB();
    }

    public int getWhiteGrayInt() {
        return whiteGray.getRGB();
    }

    public int getGrayInt() {
        return gray.getRGB();
    }

    public int getGrayLightInt() {
        return grayLight.getRGB();
    }

    public int getForegroundStrokeInt() {
        return foregroundStroke.getRGB();
    }

    public int getForegroundLightStrokeInt() {
        return foregroundLightStroke.getRGB();
    }

    public int getForegroundGrayInt() {
        return foregroundGray.getRGB();
    }

    public static int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static int mixColors(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
