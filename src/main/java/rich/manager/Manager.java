package rich.manager;

import lombok.Getter;
import rich.client.draggables.HudManager;
import rich.client.splash.LoadingStages;
import rich.client.splash.SplashScreenManager;
import rich.util.render.shader.ShaderCompilationTracker;
import rich.command.CommandManager;
import rich.events.api.EventManager;
import rich.modules.impl.combat.aura.attack.StrikerConstructor;
import rich.modules.module.*;
import rich.screens.clickgui.ClickGui;
import rich.util.config.ConfigSystem;
import rich.util.config.impl.bind.BindConfig;
import rich.util.config.impl.blockesp.BlockESPConfig;

import rich.util.config.impl.drag.DragConfig;
import rich.util.config.impl.friend.FriendConfig;
import rich.util.config.impl.ignore.IgnoreConfig;
import rich.util.config.impl.prefix.PrefixConfig;
import rich.util.config.impl.proxy.ProxyConfig;
import rich.util.config.impl.staff.StaffConfig;
import rich.util.modules.ModuleProvider;
import rich.util.modules.ModuleSwitcher;
import rich.util.render.shader.RenderCore;
import rich.util.render.shader.Scissor;
import rich.util.render.font.FontInitializer;
import rich.util.repository.macro.MacroRepository;
import rich.util.repository.way.WayRepository;
import rich.util.tps.TPSCalculate;

/**
 *  © 2026 Copyright Aegis Neo 062 - Dev Build 2026 14:03 21.04
 *        All Rights Reserved ®
 */

@Getter
public class Manager {
    public StrikerConstructor attackPerpetrator = new StrikerConstructor();
    private EventManager eventManager;
    private RenderCore renderCore;
    private Scissor scissor;
    private ModuleProvider moduleProvider;
    private ModuleRepository moduleRepository;
    private ModuleSwitcher moduleSwitcher;
    private ClickGui clickgui;
    private ConfigSystem configSystem;
    private CommandManager commandManager;
    private TPSCalculate tpsCalculate;
    private HudManager hudManager = new HudManager();

    public void init() {
        LoadingStages.LOADING_CONFIG.update();
        
        LoadingStages.LOADING_REPOSITORIES.update();
        MacroRepository.getInstance().init();
        WayRepository.getInstance().init();
        
        LoadingStages.LOADING_FRIENDS_CONFIG.update();
        BlockESPConfig.getInstance().load();
        FriendConfig.getInstance().load();
        IgnoreConfig.getInstance().load();

        
        LoadingStages.LOADING_STAFF_CONFIG.update();
        PrefixConfig.getInstance().load();
        StaffConfig.getInstance().load();
        ProxyConfig.getInstance().load();
        DragConfig.getInstance().load();
        BindConfig.getInstance();

        FontInitializer.register();

        LoadingStages.LOADING_TEXTURES.update();
        tpsCalculate = new TPSCalculate();

        LoadingStages.LOADING_MODELS.update();
        clickgui = new ClickGui();
        
        LoadingStages.INITIALIZING_EVENT_SYSTEM.update();
        eventManager = new EventManager();
        renderCore = new RenderCore();
        renderCore.init();
        scissor = new Scissor();
        
        LoadingStages.INITIALIZING_HUD_ELEMENTS.update();
        hudManager = new HudManager();
        hudManager.initElements();
        
        LoadingStages.LOADING_MODULES.update();
        moduleRepository = new ModuleRepository();
        moduleRepository.setup();
        moduleProvider = new ModuleProvider(moduleRepository.modules());
        moduleSwitcher = new ModuleSwitcher(moduleRepository.modules(), eventManager);
        
        LoadingStages.INITIALIZING_COMMANDS.update();
        configSystem = new ConfigSystem();
        configSystem.init();
        commandManager = new CommandManager();
        commandManager.init();
        
        LoadingStages.FINALIZING_CONFIGS.update();
        
        LoadingStages.FINALIZING.update();
        LoadingStages.COMPLETE.update();
    }
}