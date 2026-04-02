package fun.aegis.utils.client.managers.event.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;


@Getter
@AllArgsConstructor
public class EventRender extends EventLayer {
    @Getter
    @AllArgsConstructor
    public static class AfterHand extends EventRender {
        MatrixStack stack;
        RenderTickCounter tickCounter;
    }

    @Getter
    @AllArgsConstructor
    public static class BeforeHud extends EventRender {
        DrawContext context;
        RenderTickCounter tickCounter;
    }

    @Getter
    @AllArgsConstructor
    public static class AfterHud extends EventRender {
        DrawContext context;
        RenderTickCounter tickCounter;
    }

    @Getter
    @AllArgsConstructor
    public static class RenderLabelsEvent<T extends Entity, S extends EntityRenderState> extends EventRender {
        S state;
    }
}
