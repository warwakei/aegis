package rich;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.Window;
import rich.util.render.draw.DrawEngine;
import rich.util.render.draw.DrawEngineImpl;

/**
 *  © 2026 Copyright Aegis Neo 062 - Dev Build 2026 14:03 21.04
 *        All Rights Reserved ®
 */

public interface IMinecraft {
    MinecraftClient mc = MinecraftClient.getInstance();
    Window window = MinecraftClient.getInstance().getWindow();
    Tessellator tessellator = Tessellator.getInstance();
    RenderTickCounter tickCounter = mc.getRenderTickCounter();
    DrawEngine drawEngine = new DrawEngineImpl();

}
