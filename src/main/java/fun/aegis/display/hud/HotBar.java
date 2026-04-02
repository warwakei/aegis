package fun.aegis.display.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import fun.aegis.features.impl.render.Hud;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.utils.display.color.ColorAssist;

import java.util.Objects;
import java.util.stream.IntStream;

public class HotBar extends AbstractDraggable {
    private float selectItemX;

    public HotBar() {
        super("Хотбар", 0, 50, 182, 22, true);
    }

    @Override
    public boolean visible() {
        return Hud.getInstance().interfaceSettings.isSelected("Хотбар");
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (mc.player == null) return;
        
        MatrixStack matrix = context.getMatrices();
        PlayerInventory inventory = Objects.requireNonNull(mc.player).getInventory();
        ItemStack offHand = mc.player.getOffHandStack();

        selectItemX = (float) Calculate.interpolate(selectItemX, inventory.selectedSlot * 20, 0.15f);
        setX((mc.getWindow().getScaledWidth() - getWidth()) / 2);
        setY(mc.getWindow().getScaledHeight() - 27);

        // Main hotbar background with blur
        blur.render(ShapeProperties.create(matrix, getX() - 0.5F, getY() - 0.5F, getWidth() + 1, 23F)
                .round(3)
                .thickness(2)
                .softness(1)
                .outlineColor(ColorAssist.multAlpha(ColorAssist.getOutline(), 0.7f))
                .color(ColorAssist.multAlpha(ColorAssist.getRect(0), 0.5f))
                .build());

        // Selected slot highlight with client color outline
        rectangle.render(ShapeProperties.create(matrix, getX() + selectItemX + 1, getY() + 1, 20, 20)
                .round(2.25F)
                .thickness(3)
                .outlineColor(ColorAssist.getClientColor())
                .color(ColorAssist.getRect(0))
                .build());

        // Draw hotbar items
        IntStream.range(0, 9).forEach(i -> drawStack(context, inventory.main.get(i), getX() + i * 20 + 2, getY() + 2, false));
        
        // Offhand item
        if (!offHand.isEmpty()) {
            float offhandX = getX() + (Objects.requireNonNull(mc.player).getMainArm().equals(Arm.RIGHT) ? -28 : 198);
            drawStack(context, offHand, offhandX, getY() + 2, true);
        }
        
        // Experience level
        if (!mc.player.isSpectator() && !mc.player.isCreative()) {
            drawExperienceBar(matrix);
        }
        
        // Overlay info (item name, action bar)
        drawOverlayInfo(matrix);
    }

    public void drawExperienceBar(MatrixStack matrix) {
        if (mc.player == null) return;
        FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        font.drawCenteredString(matrix, mc.player.experienceLevel + "", 
                mc.getWindow().getScaledWidth() / 2F, getY() - 9.5F, ColorAssist.GREEN);
    }


    public void drawOverlayInfo(MatrixStack matrix) {
        float scaledWidth = (float) mc.getWindow().getScaledWidth() / 2;
        float heightStart = mc.getWindow().getScaledHeight() - 75;
        float paddingX = 4;
        float paddingY = 3;
        FontRenderer font = Fonts.getSize(14, Fonts.Type.DEFAULT);

        // Item tooltip
        if (mc.inGameHud.heldItemTooltipFade > 0 && mc.inGameHud.currentStack != null) {
            float alpha = ((float) mc.inGameHud.heldItemTooltipFade * 256.0F / 10.0F) / 255;
            alpha = Math.min(alpha, 1.0f);

            Text text = mc.inGameHud.currentStack.getName();
            float width = font.getStringWidth(text);
            int x = (int) (scaledWidth - width / 2);
            
            Calculate.setAlpha(alpha, () -> {
                blur.render(ShapeProperties.create(matrix, x - paddingX, heightStart - paddingY, 
                        width + paddingX * 2, font.getStringHeight(text) / 2.15F + paddingY * 2)
                        .round(2.5F)
                        .color(ColorAssist.getRect(0.7F))
                        .build());
                font.drawText(matrix, text, x, heightStart + 2.5F);
            });
        }
        
        // Action bar message
        if (mc.inGameHud.overlayRemaining > 0 && mc.inGameHud.overlayMessage != null 
                && !mc.inGameHud.overlayMessage.getString().isEmpty()) {
            float alpha = ((float) mc.inGameHud.overlayRemaining * 256.0F / 10.0F) / 255;
            alpha = Math.min(alpha, 1.0f);

            Text text = mc.inGameHud.overlayMessage;
            float width = font.getStringWidth(text);
            int x = (int) (scaledWidth - width / 2);
            
            Calculate.setAlpha(alpha, () -> {
                blur.render(ShapeProperties.create(matrix, x - paddingX, heightStart - paddingY - 17, 
                        width + paddingX * 2, font.getStringHeight(text) / 2.15F + paddingY * 2)
                        .round(2.5F)
                        .color(ColorAssist.getRect(0.7F))
                        .build());
                font.drawText(matrix, text, x, heightStart - 14.5F);
            });
        }
    }

    public void drawStack(DrawContext context, ItemStack stack, float x, float y, boolean offHand) {
        if (offHand) {
            blur.render(ShapeProperties.create(context.getMatrices(), x - 2.5F, y - 2.5F, 23, 23)
                    .round(3)
                    .thickness(2)
                    .softness(1)
                    .outlineColor(ColorAssist.multAlpha(ColorAssist.getOutline(), 0.7f))
                    .color(ColorAssist.multAlpha(ColorAssist.getRect(0), 0.5f))
                    .build());
        }
        Render2D.defaultDrawStack(context, stack, x, y, false, true, 1);
    }
}
