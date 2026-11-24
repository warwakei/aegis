package rich.modules.impl.player;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;

/** Optional integration: the game classes are remapped by Loom; Litematica's method names are stable. */
final class LitematicaBridge {
    private final Method getWorld;
    private final Method getRange;
    private final Method inRange;
    private final Toggle rendering;
    private final Toggle schematicRendering;

    LitematicaBridge() throws ReflectiveOperationException {
        getWorld = Class.forName("fi.dy.masa.litematica.world.SchematicWorldHandler").getMethod("getSchematicWorld");
        getRange = Class.forName("fi.dy.masa.litematica.data.DataManager").getMethod("getRenderLayerRange");
        inRange = getRange.getReturnType().getMethod("isPositionWithinRange", BlockPos.class);
        Class<?> visuals = Class.forName("fi.dy.masa.litematica.config.Configs$Visuals");
        rendering = toggle(visuals, "ENABLE_RENDERING");
        schematicRendering = toggle(visuals, "ENABLE_SCHEMATIC_RENDERING");
    }

    private static Toggle toggle(Class<?> config, String name) throws ReflectiveOperationException {
        Object option = config.getField(name).get(null);
        return new Toggle(option, option.getClass().getMethod("getBooleanValue"));
    }

    World visibleWorld() throws ReflectiveOperationException {
        if (!rendering.enabled() || !schematicRendering.enabled()) return null;
        return getWorld.invoke(null) instanceof World schematic ? schematic : null;
    }

    Layer currentLayer() throws ReflectiveOperationException { return new Layer(getRange.invoke(null), inRange); }

    record Layer(Object range, Method method) {
        boolean includes(BlockPos pos) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(method.invoke(range, pos));
        }
    }

    private record Toggle(Object option, Method method) {
        boolean enabled() throws ReflectiveOperationException { return Boolean.TRUE.equals(method.invoke(option)); }
    }
}
