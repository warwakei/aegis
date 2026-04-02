package fun.aegis.common.repository.way;

import antidaunleak.api.annotation.Native;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.display.interfaces.QuickLogger;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.math.projection.Projection;
import fun.aegis.events.render.DrawEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WayRepository implements QuickImports, QuickLogger {
    public WayRepository(EventManager eventManager) {
        eventManager.register(this);
    }

    public List<Way> wayList = new ArrayList<>();

    public boolean isEmpty() {
        return wayList.isEmpty();
    }

    public void addWay(String name, BlockPos pos, String server) {
        wayList.add(new Way(name, pos, server));
    }

    public boolean hasWay(String text) {
        return wayList.stream().anyMatch(s -> s.name().equalsIgnoreCase(text));
    }

    public void deleteWay(String name) {
        wayList.removeIf(macro -> macro.name().equalsIgnoreCase(name));
    }

    public void clearList() {
        if (!isEmpty()) wayList.clear();
    }

    @EventHandler

    public void onDraw(DrawEvent e) {
        if (isEmpty() || mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null) return;

        MatrixStack matrix = e.getDrawContext().getMatrices();

        wayList.forEach(way -> {
            Vec3d wayVec = way.pos().toCenterPos();
            Vec3d vec = Projection.worldSpaceToScreenSpace(wayVec);

            if (Projection.canSee(wayVec) && way.server().equalsIgnoreCase(mc.getNetworkHandler().getServerInfo().address)) {
                String text = way.name() + " - " + Calculate.round(mc.getEntityRenderDispatcher().camera.getPos().distanceTo(wayVec),0.1F) + "m";
                FontRenderer font = Fonts.getSize(14, Fonts.Type.SEMI);
                float height = font.getStringHeight(text) / 4;
                float width = font.getStringWidth(text);
                float padding = 3;
                double x = vec.getX() - width / 2;
                double y = vec.getY() - height / 2;
                rectangle.render(ShapeProperties.create(matrix, x - padding,y - padding,width + padding * 2,height + padding * 2)
                        .round(2)
                        .thickness(2)
                        .outlineColor(new Color(55, 52, 55, 155).getRGB())
                        .color(
                                new Color(19, 19, 21, 225).getRGB(),
                                new Color(19, 19, 21, 225).getRGB(),
                                new Color(19, 19, 21, 225).getRGB(),
                                new Color(19, 19, 21, 225).getRGB())
                        .build());

                font.drawString(matrix,text,x,y + 0.5f, ColorAssist.getText());
            }
        });
    }
}
