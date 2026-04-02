package fun.aegis.display.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.*;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import fun.aegis.features.impl.render.Hud;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ScoreBoard extends AbstractDraggable {
    private List<ScoreboardEntry> scoreboardEntries = new ArrayList<>();
    private ScoreboardObjective objective;

    public ScoreBoard() {
        super("Скорборд", 10, 100, 100, 120, true);
    }

    @Override
    public boolean visible() {
        if (!Hud.getInstance().interfaceSettings.isSelected("Скорборд")) return false;
        return !scoreboardEntries.isEmpty();
    }

    @Override
    public void tick() {
        if (mc.world == null) return;
        objective = Objects.requireNonNull(mc.world).getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            scoreboardEntries = new ArrayList<>();
            return;
        }
        scoreboardEntries = mc.world.getScoreboard().getScoreboardEntries(objective).stream()
                .sorted(Comparator.comparing(ScoreboardEntry::value).reversed()
                        .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (mc.world == null || objective == null) return;
        
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        
        MutableText text = Text.empty();
        Text mainText = objective != null ? objective.getDisplayName() : Text.empty();
        
        scoreboardEntries.forEach(entry -> 
            text.append(Team.decorateName(
                    Objects.requireNonNull(mc.world).getScoreboard().getScoreHolderTeam(entry.owner()), 
                    entry.name()
            )).append("\n")
        );

        int padding = 3;
        int offsetText = 14;
        int width = (int) Math.max(font.getStringWidth(text) + padding * 2 + 1, 100);
        int mainBlurColor = ColorAssist.multAlpha(ColorAssist.getClientColor(), 0.07f);

        // Header background
        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), offsetText)
                .round(4, 0, 4, 0)
                .softness(1)
                .thickness(2)
                .quality(40)
                .outlineColor(ColorAssist.getOutline(0.15F))
                .color(ColorAssist.multAlpha(ColorAssist.BLACK, 0.65f))
                .build());
        
        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), offsetText)
                .round(4, 0, 4, 0)
                .softness(1)
                .thickness(2)
                .quality(40)
                .outlineColor(ColorAssist.getOutline(0.8F))
                .color(mainBlurColor)
                .build());

        // Content background
        blur.render(ShapeProperties.create(matrix, getX(), getY() + offsetText - 0.5F, getWidth(), getHeight() - offsetText)
                .quality(40)
                .round(0, 4, 0, 4)
                .softness(1)
                .thickness(2)
                .outlineColor(ColorAssist.getOutline(0.15F))
                .color(ColorAssist.multAlpha(ColorAssist.BLACK, 0.65f))
                .build());
        
        blur.render(ShapeProperties.create(matrix, getX(), getY() + offsetText - 0.5F, getWidth(), getHeight() - offsetText)
                .quality(40)
                .round(0, 4, 0, 4)
                .thickness(2)
                .softness(1)
                .outlineColor(ColorAssist.multAlpha(ColorAssist.multAlpha(ColorAssist.getClientColor(), 0.8f), 0.5f))
                .color(ColorAssist.multAlpha(ColorAssist.getClientColor(), 0.1F))
                .build());

        // Draw title centered
        font.drawText(matrix, mainText, (int) (getX() + (getWidth() - font.getStringWidth(mainText)) / 2), getY() + padding + 1.5F);
        
        // Draw entries
        font.drawText(matrix, text, getX() + padding, getY() + offsetText + padding);

        // Adjust position if on right side of screen
        if (getX() > mc.getWindow().getScaledWidth() / 2) {
            setX(getX() + getWidth() - width);
        }
        
        setWidth(width);
        setHeight((int) (font.getStringHeight(text) / 2.16 + offsetText + padding));
    }
}
