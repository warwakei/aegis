package fun.aegis.display.hud;

import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.Registries;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.glow.GlowEffect;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.math.time.StopWatch;
import fun.aegis.utils.client.chat.StringHelper;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.events.packet.PacketEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CoolDowns extends AbstractDraggable {
    public static CoolDowns getInstance() {
        return Instance.getDraggable(CoolDowns.class);
    }

    public final List<CoolDown> list = new ArrayList<>();
    private long lastItemChange = 0;
    private int currentItemIndex = 0;
    private static final Item[] EXAMPLE_ITEMS = {
            Items.ENDER_EYE, Items.ENDER_PEARL, Items.SUGAR, Items.MACE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.TRIDENT, Items.CROSSBOW, Items.DRIED_KELP, Items.NETHERITE_SCRAP
    };

    public CoolDowns() {
        super("Кулдауны", 10, 40, 80, 23, true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        list.removeIf(c -> c.anim.isFinished(Direction.BACKWARDS));
        list.stream().filter(c -> !Objects.requireNonNull(mc.player).getItemCooldownManager().isCoolingDown(c.item.getDefaultStack())).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastItemChange >= 1000) {
                currentItemIndex = (currentItemIndex + 1) % EXAMPLE_ITEMS.length;
                lastItemChange = currentTime;
            }
        }
    }

    @Override
    public void packet(PacketEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;
        switch (e.getPacket()) {
            case CooldownUpdateS2CPacket c -> {
                Item item = Registries.ITEM.get(c.cooldownGroup());
                list.stream().filter(coolDown -> coolDown.item.equals(item)).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
                if (c.cooldown() != 0) {
                    list.add(new CoolDown(item, new StopWatch().setMs(-c.cooldown() * 50L), new Decelerate().setMs(150).setValue(1.0F)));
                }
            }
            case PlayerRespawnS2CPacket p -> list.clear();
            default -> {}
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer fontCoolDown = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer icon = Fonts.getSize(21, Fonts.Type.ICONCATACLYSMREG);
        FontRenderer icon2 = Fonts.getSize(14, Fonts.Type.ICONSTYPENEW);
        FontRenderer items = Fonts.getSize(12, Fonts.Type.ICONCATACLYSMREG);

        long activeCooldowns = list.stream().filter(c -> !c.anim.isFinished(Direction.BACKWARDS)).count();
        java.lang.String cooldownCountText = java.lang.String.valueOf(activeCooldowns);
        float textWidth = items.getStringWidth(cooldownCountText);
        float boxWidth = textWidth + 6;

        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(4).softness(5.0F).thickness(0).color(ColorAssist.getRect(0.4F)).build());

        
        // Перемещаем иконку вправо
        float iconX = getX() + getWidth() - 18;
        icon.drawString(matrix, "D", iconX, getY() + 5f, new Color(225, 225, 255, 255).getRGB());
        font.drawString(matrix, getName(), getX() + 4, getY() + 6.5f, ColorAssist.getText());

        float centerX = getX() + getWidth() / 2.0F;
        int offset = 23;
        int maxWidth = 110;

        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            float centerY = getY() + offset;
            Item item = EXAMPLE_ITEMS[currentItemIndex];
            java.lang.String name = "Example CoolDowns";
            java.lang.String duration = "**:**";
            int textColor = ColorAssist.getText();
            int textAlpha = 255;
            int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
            int color = new Color(225, 225, 255, 255).getRGB();
            float durationWidth = fontCoolDown.getStringWidth(duration);
            float durationBoxWidth = durationWidth + 6;
            Calculate.scale(matrix, centerX, centerY, 1, 1, () -> {
                Render2D.drawStack(matrix, item.getDefaultStack(), getX() + 3.5f, centerY - 3, false, 0.5F);
                fontCoolDown.drawString(matrix, name, getX() + 18, centerY + 1, colorWithAlpha);
                
                float timerX = getX() + getWidth() - durationWidth - 8 - 12;
                float timerY = centerY - 2;
                float timerWidth = durationWidth + 18;
                float timerHeight = 10;
                
                icon2.drawString(matrix, "n", timerX + 2, centerY + 1, new Color(225, 225, 255, 255).getRGB());
                fontCoolDown.drawString(matrix, duration, timerX + 12, centerY + 1, color);
            });
            int width = (int) fontCoolDown.getStringWidth(name + duration) + 30;
            maxWidth = Math.max(width, maxWidth);
            offset += 11;
        } else {
            for (CoolDown coolDown : list) {
                float animation = coolDown.anim.getOutput().floatValue();
                float centerY = getY() + offset;
                long elapsedTime = coolDown.time.elapsedTime();
                int time = 0;
                if (elapsedTime >= -2147483648L && elapsedTime <= 2147483647L) {
                    time = (int) (-elapsedTime / 1000);
                } else {
                    time = elapsedTime < 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                }
                java.lang.String name = coolDown.item.getDefaultStack().getName().getString();
                java.lang.String duration = StringHelper.getDuration(time);
                int textColor = ColorAssist.getText();
                int textAlpha = 255;
                int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
                int color = new Color(225, 225, 255, 255).getRGB();
                float durationWidth = fontCoolDown.getStringWidth(duration);
                float durationBoxWidth = durationWidth + 6;
                Calculate.scale(matrix, centerX, centerY, 1, animation, () -> {
                    Render2D.drawStack(matrix, coolDown.item.getDefaultStack(), getX() + 3.5f, centerY - 3, false, 0.5F);
                    fontCoolDown.drawString(matrix, name, getX() + 18, centerY + 1, colorWithAlpha);
                    
                    float timerX = getX() + getWidth() - durationWidth - 8 - 12;
                    float timerY = centerY - 2;
                    float timerWidth = durationWidth + 18;
                    float timerHeight = 10;
                    
                    icon2.drawString(matrix, "n", timerX + 2, centerY + 1, new Color(225, 225, 255, 255).getRGB());
                    fontCoolDown.drawString(matrix, duration, timerX + 12, centerY + 1, color);
                });
                int width = (int) fontCoolDown.getStringWidth(name + duration) + 30;
                maxWidth = Math.max(width, maxWidth);
                offset += (int) (11 * animation);
            }
        }
        setWidth(maxWidth + 10);
        setHeight(offset);
    }

    public record CoolDown(Item item, StopWatch time, Animation anim) {}
}
