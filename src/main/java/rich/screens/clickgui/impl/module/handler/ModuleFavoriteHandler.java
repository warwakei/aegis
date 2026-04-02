package rich.screens.clickgui.impl.module.handler;

import rich.modules.module.ModuleStructure;

import java.util.List;

public class ModuleFavoriteHandler {

    public void toggleFavorite(ModuleStructure module, List<ModuleStructure> displayModules,
                               ModuleAnimationHandler animationHandler) {
        if (module == null) return;

        module.switchFavorite();

        int oldIndex = displayModules.indexOf(module);

        for (ModuleStructure mod : displayModules) {
            float posAnim = animationHandler.getPositionAnimations().getOrDefault(mod, 1f);
            if (posAnim >= 0.99f) {
                animationHandler.getPositionAnimations().put(mod, 0f);
            }
            if (!animationHandler.getModuleAlphaAnimations().containsKey(mod)) {
                animationHandler.getModuleAlphaAnimations().put(mod, 1f);
            }
        }
        animationHandler.getModuleAlphaAnimations().put(module, 0f);
    }
}