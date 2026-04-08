package rich.netpanel;

import rich.netpanel.loggers.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and manager for NetPanel modules.
 * Architecture inspired by Docker — independent, configurable, toggleable components.
 *
 * Module naming convention follows community plugin patterns:
 *   - Core modules: shipped with AegisNeo
 *   - Community modules: contributed by users (marked with author)
 */
public class ModuleManager {

    private final Map<String, NetPanelModule> modules = new ConcurrentHashMap<>();

    public void init() {
        // ==================== CORE MODULES ====================

        registerModule(NetPanelModule.builder("console", "Console")
                .description("Minecraft console output capture with log4j integration")
                .icon("\u2318")
                .author("AegisNeo")
                .version("1.2")
                .setting("maxLines", 1000)
                .setting("stripColors", false)
                .setting("autoScroll", true)
                .build());

        registerModule(NetPanelModule.builder("chat", "Chat")
                .description("In-game chat capture with nickname autocomplete")
                .icon("\u25c6")
                .author("AegisNeo")
                .version("1.1")
                .setting("maxMessages", 10000)
                .setting("stripNickHead", true)
                .setting("globalChatPrefix", "!")
                .build());

        registerModule(NetPanelModule.builder("system", "System")
                .description("FPS, memory, CPU, disk, uptime monitoring")
                .icon("\u2699")
                .author("AegisNeo")
                .version("1.3")
                .setting("refreshMs", 1000)
                .setting("showDisk", true)
                .build());

        // ==================== DEBUG & DEV MODULES ====================

        registerModule(NetPanelModule.builder("hitreg", "Hitreg Logger")
                .description("Attack hit/miss logging with distance and damage tracking")
                .icon("\u2694")
                .author("xKavv")
                .version("0.9")
                .setting("maxEntries", 500)
                .setting("logAura", true)
                .setting("logMace", true)
                .build());

        registerModule(NetPanelModule.builder("world", "World Info")
                .description("Player coordinates, dimension, biome, weather, seed")
                .icon("\u229e")
                .author("d3v_n1ght")
                .version("0.7")
                .setting("refreshMs", 2000)
                .setting("showSeed", true)
                .build());

        registerModule(NetPanelModule.builder("potions", "Potion Effects")
                .description("Active status effects with duration and amplifier display")
                .icon("\u2295")
                .author("potionMaster42")
                .version("0.5")
                .setting("refreshMs", 3000)
                .setting("showAmbient", false)
                .build());

        registerModule(NetPanelModule.builder("server", "Server Info")
                .description("TPS, ping, player count, server brand detection")
                .icon("\u2298")
                .author("netRunner")
                .version("0.8")
                .setting("refreshMs", 1000)
                .setting("showBrand", true)
                .build());

        // ==================== MODERATION MODULES ====================

        // Jenro Moderating — hidden until configured
        NetPanelModule modModule = NetPanelModule.builder("moderation", "Jenro Moderating")
                .description("Module to help you moderate jenro servers if you moderator or helper. *For example: funnymc.su (funnygame)")
                .icon("\u2630")
                .author("AegisNeo")
                .version("1.0")
                .setting("maxEntries", 2000)
                .setting("showActivity", true)
                .setting("autoTrackJoins", true)
                .build();
        modModule.setEnabled(false);
        registerModule(modModule);

        registerModule(NetPanelModule.builder("anticheat", "Anticheat Monitor")
                .description("Anticheat violation tracking with flag counts and check details")
                .icon("\u26a1")
                .author("guardDev")
                .version("0.6")
                .setting("maxEntries", 1000)
                .setting("autoDetectAnticheat", true)
                .setting("highlightHighVL", true)
                .build());

        // ==================== PERFORMANCE MODULES ====================

        registerModule(NetPanelModule.builder("performance", "Performance Profiler")
                .description("Tick time, GC pauses, entity count, chunk loading stats")
                .icon("\u23f1")
                .author("profiler_X")
                .version("0.4")
                .setting("refreshMs", 2000)
                .setting("trackGC", true)
                .setting("entityThreshold", 200)
                .build());

        registerModule(NetPanelModule.builder("network", "Network Monitor")
                .description("Bandwidth usage, packet stats, latency spikes monitoring")
                .icon("\u27c1")
                .author("packetSniff")
                .version("0.3")
                .setting("refreshMs", 1500)
                .setting("trackPackets", true)
                .setting("showBandwidth", true)
                .build());

        // ==================== SESSION MODULES ====================

        registerModule(NetPanelModule.builder("sessions", "Session Tracker")
                .description("Player join/leave tracking, session duration, IP logging")
                .icon("\u25c9")
                .author("sessionKeeper")
                .version("0.5")
                .setting("maxEntries", 500)
                .setting("trackIP", true)
                .setting("showDuration", true)
                .build());

        // ==================== PLUGIN DEBUG MODULES ====================

        registerModule(NetPanelModule.builder("pluginDebug", "Plugin Debugger")
                .description("Plugin event logging, error tracking, stack trace capture")
                .icon("\u2318")
                .author("debugLord")
                .version("0.3")
                .setting("maxEntries", 800)
                .setting("trackErrors", true)
                .setting("trackEvents", false)
                .build());

        registerModule(NetPanelModule.builder("logFilter", "Log Filter")
                .description("Regex-based log filtering, pattern matching, custom log views")
                .icon("\u23af")
                .author("regexKing")
                .version("0.4")
                .setting("maxEntries", 500)
                .setting("caseSensitive", false)
                .setting("highlightMatches", true)
                .build());
    }

    public void registerModule(NetPanelModule module) {
        modules.put(module.getId(), module);
    }

    public NetPanelModule getModule(String id) {
        return modules.get(id);
    }

    public Collection<NetPanelModule> getAllModules() {
        return modules.values();
    }

    public List<Map<String, Object>> getModulesJson() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (NetPanelModule m : modules.values()) {
            result.add(m.toJson());
        }
        return result;
    }

    public boolean toggleModule(String id, boolean enabled) {
        NetPanelModule module = modules.get(id);
        if (module != null) {
            module.setEnabled(enabled);
            return true;
        }
        return false;
    }

    public boolean updateModuleSetting(String id, String key, Object value) {
        NetPanelModule module = modules.get(id);
        if (module != null) {
            module.setSetting(key, value);
            return true;
        }
        return false;
    }

    public void refreshModule(String id) {
        NetPanelModule module = modules.get(id);
        if (module != null && module.isEnabled()) {
            module.refresh();
        }
    }

    public void refreshAll() {
        for (NetPanelModule m : modules.values()) {
            if (m.isEnabled() && m.isAutoRefresh()) {
                m.refresh();
            }
        }
    }

    public boolean isModuleEnabled(String id) {
        NetPanelModule m = modules.get(id);
        return m != null && m.isEnabled();
    }
}
