# Aegis Neo — AGENTS.md

## Build & commands

- Build: `./gradlew build` (Java 21, Fabric Loom 1.15, Minecraft 1.21.11)
- `compileJava` depends on `generateVersion` (generates `build/generated-src/rich/util/Version.java` from `src/main/resources/rich/util/Version.java.template`)
- Source set includes `build/generated-src` — do not edit `Version.java`
- Gradle config cache disabled (`org.gradle.configuration-cache=false`)
- Custom Gradle home: `D:/gradle_home` (machine-specific, set in `gradle.properties`)
- Launcher sub-project at `launcher/` — builds separately, jar name is `AegisUpdater03.jar` (no version in archive name: `archiveVersion = ""`, `archiveClassifier = ""`)
- Mod main jar: `AegisNeo-<version>.jar` (archivesBaseName = `AegisNeo`)
- No CI, no pre-commit hooks, no code formatter or linter config found

## Project structure

- **Main mod ID:** `copyright`, client-side only (`"environment": "client"`)
- **Updater mod ID:** `aegis_updater`, runs before main mod, auto-downloads from GitHub (`warwakei/aegis`)
- **PreLaunch entrypoints (main mod):** `rich.util.mods.config.wave.ResourceManager`, `rich.util.mods.config.wave.WaveCapesConfigOverride` (runs before splash)
- **PreLaunch entrypoint (updater):** `aegis.updater.UpdaterEntrypoint`
- **Client entrypoint:** `rich.Initialization` (implements `ClientModInitializer`)
- **Init flow:** `Initialization.onInitializeClient()` → splash screen setup → `SplashScreenManager` calls `Initialization.init()` → `Manager.init()` bootstraps everything in order: macro/way repos → config loads (BlockESP, Friend, Ignore, Prefix, Staff, Proxy, Drag, Bind) → fonts → TPS tracker → ClickGui → EventManager → RenderCore → Scissor → HUD elements → ModuleRepository → ModuleProvider → ModuleSwitcher → ConfigSystem → CommandManager → HeartbeatManager (license check)
- **Main mod requires updater:** throws `RuntimeException` if `aegis_updater` not loaded (`Initialization.java:26`)
- **`AegisNeo/` dir:** empty, reserved for runtime config output
- **`run/` dir:** Minecraft dev runtime (gitignored), contains real Fabric profile

## Architecture

- **Modules:** registered in `ModuleRepository.setup()` via `builder().add(new X())`. Categories: `COMBAT, MOVEMENT, RENDER, PLAYER, MISC, AUTOBUY`. Modules extend `ModuleStructure` → `SettingRepository`. Hidden modules (e.g., `AutoParser`) registered via `.hidden()` and auto-enabled.
- **Module access:** `Instance.get(Class)` → `Initialization.getInstance().getManager().getModuleProvider().get(instance)` — singleton lookup from `ConcurrentHashMap`.
- **Events:** custom annotation-based system (`@EventHandler` with `Priority` byte constants), reflection-driven via `EventManager`. Listeners registered via `EventManager.register(this)`. Module `toggleSilent` auto-registers/unregisters on enable/disable. Has 47+ event types in `rich.events.impl`.
- **Settings:** defined as fields in module constructors, names are in Russian (e.g. `"Режим наводки"`, `"Дистанция удара"`). Types: `SelectSetting`, `SliderSettings`, `BooleanSetting`, `BindSetting`, `ColorSetting`, `GroupSetting`, `MultiSelectSetting`, `TextSetting`, `ButtonSetting`.
- **Config:** custom serialization (`ConfigSystem` → `ConfigSerializer`), auto-saves on shutdown via JVM hook. Configs stored per-system under `ConfigPath.getConfigFile()` (`autoconfig.aegisconfig`). Auto-saver runs on background thread.
- **Commands:** prefix-based, defined in `CommandManager.init()`, Russian chat strings (`"Неизвестная команда"`). 12 commands registered (Help, Config, AutoBuy, Friend, Ignore, Macro, Bind, Prefix, Way, Staff, BlockESP, Title).
- **Mixins:** 58 mixins registered in `mixins.json` (51 client + 7 mixin-only), package `rich.mixin`.
- **Access widener:** 59 lines at `src/main/resources/accesswidener` — exposes Vec3d x/y/z, entity velocity, cooldowns, render pipeline internals, etc.

## Render system

- Custom shader-based rendering via `RenderCore` with `ShaderCompilationTracker` for async shader compilation tracking
- Render pipelines: `KawaseBlurPipeline`, `TexturePipeline`, `RectPipeline`, `OutlinePipeline`, `BlurPipeline`, `GlowOutlinePipeline`, `GlassCompositePipeline`, `HoloSheenPipeline`, `IridescentOutlinePipeline`, `MaskDiffPipeline`, `Arc2D`, `ArcOutline2D`
- Font system: `FontInitializer.register()` → loads custom atlases via `FontAtlas` + `FontRenderer` + `FontPipeline`
- **Cyrillic path quirk:** `src/main/java/rich/util/render/сliemtpipeline/ClientPipelines.java` uses Cyrillic 'с' instead of Latin 'c' — will fail to compile on non-Windows systems; rename directory to `clientpipeline` (Latin) if porting
- Glass hands renderer: `GlassHandsRenderer` with dedicated `GlassHandsRenderEvent`
- Item render: custom `ItemRender` + `FakePlayerRenderer`

## Lombok conventions

Extensively used: `@Getter`, `@Setter`, `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)`, `@UtilityClass`, `@RequiredArgsConstructor`, `@NonFinal`, `lombok.experimental.FieldDefaults`

## Anti-piracy / license

- `antidaunleak.api` — native (VMProtect) anti-leak integration, `@Native` annotation on critical methods
- `HeartbeatManager` — license validation on startup, sends Discord webhook notification
- `ResourceManager.g1()` provides HWID-based auth string
- `UserProfile` from antidaunleak provides `username`, `uid`, `hwid` profile data
- `@Native(type = Native.Type.VMProtectBeginUltra)` on `Initialization.p1()`
- `DiscordWebhookNotifier.sendLaunchNotification()` fires on game start

## NetPanel

- `src/main/resources/netpanel/` contains an HTML/JS/CSS overlay served via the `NetPanel` module
- `app.js`, `index.html`, `style.css` — web-based UI rendered in-game

## Code quirks

- All settings names, command messages, loading stage text are in **Russian**
- `Instance.get(Class)` provides singleton access to modules via `ModuleProvider`
- Module names in `ModuleRepository.setup()` are in English, but setting names (`"Режим наводки"`) are Russian
- Launcher updater uses `fabric-loader:before: ["*"]` to run before all other mods
- Updater downloads via GitHub API, uses temp `.jar.tmp` + atomic move pattern, creates `.aegis_needs_restart` flag file
- Version template expanded via Gradle `processResources` AND `generateVersion` Copy task (two expansion paths — `fabric.mod.json` gets `${version}`, template gets `project.mod_version`)
