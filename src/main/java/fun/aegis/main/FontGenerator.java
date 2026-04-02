package fun.aegis.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FontGenerator {
    public static final String CHARSET = "\"\\\" ¡‰·₴≠¿×ØøАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюяє–—‘’“”„…←↑→↓ʻˌ;⁰¹³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾ⁱ™ʔʕ¤¥©®µ¶¼½¾·‐‚†‡•′″‴‹›‽⁂℗−∞Є♠♣♥♦♭♮♯⚀⚁⚂⚃⚄⚅ʬ❄⏏⏻⏼⏽⭘▲▶▼◀●◦�¦ᴀʙᴄᴅᴇꜰɢʜᴊᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡʏᴢ§ʡʢʘǀǃǂǁ☂♤♧♡♢↔∑□△▷▽◁○☆★₀₁₂₃₄₅₆₇₈₉₊₋₌₍₎∫⌀⌘⚠⓪①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳ⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩ☑☒!#$%&'()*+,-./0123456789:;<=>[\\\\]^_`?@ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz{|}~£ƒªº¬«»≡±≥≤⌠⌡÷≈°∙√ⁿ²■\"";

    public static void main(String[] args) {
        String fontFolderPath = "mtsdf-font/";
        Path outputPath = initFile(fontFolderPath);
        if (outputPath == null) {
        }

        generate(fontFolderPath, outputPath, "iconmain");
    }

    @SuppressWarnings("SameParameterValue")
    private static void generate(String fontFolderPath, Path outputPath, String fontPath) {

        File fontFolder = new File(fontFolderPath + fontPath);
        File[] fontFiles = fontFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".ttf"));
        if (fontFiles != null && fontFiles.length > 0) {
            ExecutorService executor = Executors.newFixedThreadPool(fontFiles.length);
            List<Process> processes = new ArrayList<>();
            for (File fontFile : fontFiles) {
                executor.execute(() -> {
                    try {
                        String fontFileName = fontFile.getName().replaceFirst("[.][^.]+$", "");
                        String command = String.format("%s/atlas-gen.exe -font %s -charset %s/charset.txt -type mtsdf -format png -imageout %s.png -json %s.json -size 64 -square4 -pxrange 12",
                                fontFolderPath, fontFile.getAbsolutePath(), fontFolderPath, outputPath.resolve(fontFileName.toLowerCase().replaceAll("-", "_")), outputPath.resolve(fontFileName.toLowerCase().replaceAll("-", "_")));
                        Process process = Runtime.getRuntime().exec(command);
                        processes.add(process);
                        int exitCode = process.waitFor();
                        if (exitCode == 0) {
                            System.out.println("Атлас для шрифта " + fontFileName + " успешно создан.");
                        } else {
                            System.out.println("Ошибка при создании атласа для шрифта " + fontFileName);
                        }
                    } catch (IOException | InterruptedException e) {
                        System.err.println("Ошибка при выполнении команды для шрифта " + fontFile.getName() + ": " + e.getMessage());
                    }
                });
            }
            executor.shutdown();
            try {
                if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS))
                    System.out.println("Процесс завершён.");
            } catch (InterruptedException e) {
                System.err.println("Ошибка при ожидании завершения потоков: " + e.getMessage());
            }
            for (Process process : processes) {
                process.destroy();
            }
        } else {
            System.out.println("Не найдены файлы шрифтов в указанной папке.");
        }
    }

    private static Path initFile(String fontFolderPath) {
        Path outputPath = Path.of(fontFolderPath + "out");
        if (Files.notExists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                System.err.println("Ошибка при создании папки: " + e.getMessage());
                return null;
            }
        }
        Path charsetPath = Path.of(fontFolderPath + "charset.txt");
        if (Files.notExists(charsetPath)) {
            try {
                Files.createFile(charsetPath);
                Files.write(charsetPath, CHARSET.getBytes());
            } catch (IOException e) {
                System.err.println("Ошибка при создании charset.txt: " + e.getMessage());
                return null;
            }
        }
        return outputPath;
    }
}
