package rich.client.splash;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SystemRequirementsChecker {
    
    public static class RequirementResult {
        private final boolean passed;
        private final String message;
        
        public RequirementResult(boolean passed, String message) {
            this.passed = passed;
            this.message = message;
        }
        
        public boolean isPassed() { return passed; }
        public String getMessage() { return message; }
    }
    
    public static List<RequirementResult> checkSystemRequirements() {
        List<RequirementResult> results = new ArrayList<>();
        
        results.add(checkMinecraftMemory());
        results.add(checkCpuArchitecture());
        results.add(checkSystemMemory());
        results.add(checkGraphicsCard());
        results.add(checkHyperV());
        
        return results;
    }
    
    private static RequirementResult checkMinecraftMemory() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            
            if (maxMemory == -1) {
                maxMemory = Runtime.getRuntime().maxMemory();
            }
            
            long maxMemoryMB = maxMemory / (1024 * 1024);
            
            if (maxMemoryMB < 2368) {
                return new RequirementResult(false, 
                    String.format("Недостаточно памяти для Minecraft: %d МБ (требуется 2368+ МБ)", maxMemoryMB));
            }
            
            return new RequirementResult(true, "Память Minecraft: OK");
        } catch (Exception e) {
            return new RequirementResult(false, "Ошибка проверки памяти Minecraft");
        }
    }
    
    private static RequirementResult checkCpuArchitecture() {
        try {
            String arch = System.getProperty("os.arch").toLowerCase();
            
            if (arch.contains("arm") || arch.contains("aarch")) {
                return new RequirementResult(false, 
                    "Неподдерживаемая архитектура процессора: " + arch + " (ARM не поддерживается)");
            }
            
            return new RequirementResult(true, "Архитектура процессора: OK");
        } catch (Exception e) {
            return new RequirementResult(false, "Ошибка проверки архитектуры процессора");
        }
    }
    
    private static RequirementResult checkSystemMemory() {
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            
            long totalMemory = osBean.getTotalPhysicalMemorySize();
            long totalMemoryMB = totalMemory / (1024 * 1024);
            
            if (totalMemoryMB < 7683) {
                return new RequirementResult(false, 
                    String.format("Недостаточно системной памяти: %d МБ (требуется 7683+ МБ)", totalMemoryMB));
            }
            
            return new RequirementResult(true, "Системная память: OK");
        } catch (Exception e) {
            return new RequirementResult(false, "Ошибка проверки системной памяти");
        }
    }
    
    private static RequirementResult checkGraphicsCard() {
        try {
            String[] gpuCheckCommands = {
                "wmic path win32_VideoController get name",
                "lspci | grep -i vga",
                "system_profiler SPDisplaysDataType"
            };
            
            String gpuInfo = "";
            
            for (String command : gpuCheckCommands) {
                try {
                    Process process = Runtime.getRuntime().exec(command);
                    java.util.Scanner scanner = new java.util.Scanner(process.getInputStream());
                    StringBuilder output = new StringBuilder();
                    
                    while (scanner.hasNextLine()) {
                        output.append(scanner.nextLine()).append("\n");
                    }
                    scanner.close();
                    
                    gpuInfo = output.toString().toLowerCase();
                    if (!gpuInfo.trim().isEmpty()) break;
                } catch (Exception ignored) {}
            }
            
            if (gpuInfo.contains("intel hd graphics") && !gpuInfo.contains("uhd")) {
                return new RequirementResult(false, 
                    "Неподдерживаемая видеокарта: Intel HD Graphics (требуется UHD или дискретная)");
            }
            
            if (isOldNvidiaGT(gpuInfo)) {
                return new RequirementResult(false, 
                    "Неподдерживаемая видеокарта: GeForce GT 320 или ниже (требуется GT 330+)");
            }
            
            return new RequirementResult(true, "Видеокарта: OK");
        } catch (Exception e) {
            return new RequirementResult(true, "Видеокарта: Не удалось проверить (пропускаем)");
        }
    }
    
    private static boolean isOldNvidiaGT(String gpuInfo) {
        if (!gpuInfo.contains("geforce") || !gpuInfo.contains("gt")) {
            return false;
        }
        
        String[] oldModels = {
            "gt 210", "gt 220", "gt 240", "gt 320"
        };
        
        for (String model : oldModels) {
            if (gpuInfo.contains(model)) {
                return true;
            }
        }
        
        if (gpuInfo.contains("gt ")) {
            try {
                String[] parts = gpuInfo.split("gt ");
                if (parts.length > 1) {
                    String numberPart = parts[1].trim().split("\\s+")[0];
                    int modelNumber = Integer.parseInt(numberPart.replaceAll("[^0-9]", ""));
                    
                    if (modelNumber <= 320) {
                        return true;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        
        return false;
    }
    
    private static RequirementResult checkHyperV() {
        // Hyper-V больше не требуется
        return new RequirementResult(true, "Hyper-V: Не требуется");
    }
    
    private static boolean isHyperVEnabled() {
        try {
            // Сначала проверяем что гипервизор активен на системном уровне
            if (!isHypervisorPresent()) {
                return false;
            }
            
            Process process = new ProcessBuilder(
                "dism", "/online", "/get-featureinfo", "/featurename:Microsoft-Hyper-V"
            ).start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean enabled = false;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("State") && line.contains("Enabled")) {
                    enabled = true;
                    break;
                }
            }
            
            process.waitFor();
            reader.close();
            
            if (!enabled) {
                return checkHyperVPowerShell();
            }
            
            return enabled;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean isHypervisorPresent() {
        try {
            Process process = new ProcessBuilder(
                "powershell", "-Command", 
                "Get-CimInstance -ClassName Win32_ComputerSystem | Select-Object -ExpandProperty HypervisorPresent"
            ).start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.lines().reduce("", (a, b) -> a + b).trim();
            reader.close();
            process.waitFor();
            
            return "True".equalsIgnoreCase(output);
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean checkHyperVPowerShell() {
        try {
            Process process = new ProcessBuilder(
                "powershell", "-Command", 
                "Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V | Select-Object State"
            ).start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.lines().reduce("", (a, b) -> a + b);
            reader.close();
            process.waitFor();
            
            return output.contains("Enabled");
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean testHyperVCrypto() {
        try {
            String testData = "AegisNeo-HyperV-Test-" + System.currentTimeMillis();
            byte[] originalData = testData.getBytes(StandardCharsets.UTF_8);
            
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            
            Cipher encryptCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedData = encryptCipher.doFinal(originalData);
            
            Cipher decryptCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedData = decryptCipher.doFinal(encryptedData);
            
            boolean cryptoWorking = Arrays.equals(originalData, decryptedData);
            
            if (!cryptoWorking) {
                return false;
            }
            
            return testSecureRandomEntropy();
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean testSecureRandomEntropy() {
        try {
            SecureRandom sr1 = new SecureRandom();
            SecureRandom sr2 = new SecureRandom();
            
            byte[] random1 = new byte[32];
            byte[] random2 = new byte[32];
            
            sr1.nextBytes(random1);
            sr2.nextBytes(random2);
            
            if (Arrays.equals(random1, random2)) {
                return false;
            }
            
            int uniqueBytes1 = countUniqueBytes(random1);
            int uniqueBytes2 = countUniqueBytes(random2);
            
            return uniqueBytes1 >= 20 && uniqueBytes2 >= 20;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private static int countUniqueBytes(byte[] data) {
        boolean[] seen = new boolean[256];
        int unique = 0;
        
        for (byte b : data) {
            int index = b & 0xFF;
            if (!seen[index]) {
                seen[index] = true;
                unique++;
            }
        }
        
        return unique;
    }
}