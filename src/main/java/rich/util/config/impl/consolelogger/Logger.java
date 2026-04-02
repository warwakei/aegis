package rich.util.config.impl.consolelogger;

/**
 *  © 2026 Copyright Rich Client 2.0
 *        All Rights Reserved ®
 */

public class Logger {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN_BG = "\u001B[42m";
    private static final String RED_BG = "\u001B[41m";
    private static final String BLACK = "\u001B[30m";
    private static final String WHITE = "\u001B[97m";
    private static final String BOLD = "\u001B[1m";

    public static void success(String message) {
        System.out.println(GREEN_BG + BLACK + BOLD + " " + message + " " + RESET);
    }

    public static void error(String message) {
        System.out.println(RED_BG + WHITE + BOLD + " " + message + " " + RESET);
    }

    public static void info(String message) {
        System.out.println("\u001B[44m" + WHITE + BOLD + " " + message + " " + RESET);
    }
}