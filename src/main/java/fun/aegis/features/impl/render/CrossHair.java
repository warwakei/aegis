package fun.aegis.features.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.hit.EntityHitResult;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.display.geometry.Render2D;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CrossHair extends Module {
    public static CrossHair getInstance() {
        return Instance.get(CrossHair.class);
    }
    private float red = 0;

    private final SliderSettings attackSetting = new SliderSettings("Отступ атаки", "Отступ для кулдауна предмета").setValue(10).range(0, 20);
    private final SliderSettings indentSetting = new SliderSettings("Отступ", "Отступ от центра экрана").setValue(0).range(0, 5);
    private final SliderSettings size1Setting = new SliderSettings("Ширина", "Ширина прицела").setValue(4).range(2, 10);
    private final SliderSettings size2Setting = new SliderSettings("Высота", "Высота прицела").setValue(1).range(1, 4);

    public CrossHair() {
        super("CrossHair", "Cross Hair", ModuleCategory.RENDER);
        setup(attackSetting, indentSetting, size1Setting, size2Setting);
    }

    public void onRenderCrossHair() {
        red = Calculate.interpolateSmooth(2, red, mc.crosshairTarget instanceof EntityHitResult ? 5 : 1);
        int firstColor = ColorAssist.multRed(ColorAssist.WHITE, red), secondColor = ColorAssist.BLACK;
        float x = window.getScaledWidth() / 2F, y = window.getScaledHeight() / 2F;
        float cooldown = attackSetting.getInt() - (attackSetting.getInt() * mc.player.getAttackCooldownProgress(tickCounter.getTickDelta(false)));
        float size = size1Setting.getValue(), size2 = size2Setting.getValue(), offset = size2 / 2, indent = indentSetting.getInt() + cooldown;

        renderMain(x, y, size, size2, 1, indent, offset, secondColor);
        renderMain(x, y, size, size2, 0, indent, offset, firstColor);
    }

    private void renderMain(float x, float y, float size, float size2, float padding, float indent, float offset, int color) {
        Render2D.drawQuad(x - offset - padding / 2, y - size - indent - padding / 2, size2 + padding, size + padding, color);
        Render2D.drawQuad(x - offset - padding / 2, y + indent - padding / 2, size2 + padding, size + padding, color);
        Render2D.drawQuad(x - size - indent - padding / 2, y - offset - padding / 2, size + padding, size2 + padding, color);
        Render2D.drawQuad(x + indent - padding / 2, y - offset - padding / 2, size + padding, size2 + padding, color);
    }
}
