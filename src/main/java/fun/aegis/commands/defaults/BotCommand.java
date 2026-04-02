package fun.aegis.commands.defaults;

import fun.aegis.Aegis;
import fun.aegis.features.bot.Bot;
import fun.aegis.features.bot.BotManager;
import fun.aegis.features.bot.BotTask;
import fun.aegis.display.screens.bot.BotGUI;
import fun.aegis.utils.client.managers.api.command.Command;
import fun.aegis.utils.client.managers.api.command.argument.IArgConsumer;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.display.interfaces.QuickImports;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class BotCommand extends Command implements QuickImports {
    private final BotManager botManager;
    private int[] selectedBots;
    private boolean selectAll;

    public BotCommand(Aegis main) {
        super("bot");
        this.botManager = BotManager.getInstance();
        this.selectedBots = new int[0];
        this.selectAll = false;
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            logDirect("Usage: .bot [c/list/u/st/t/k/gui]", Formatting.RED);
            return;
        }

        String subcommand = args.getString().toLowerCase();

        switch (subcommand) {
            case "c" -> createBot(args);
            case "list" -> listBots();
            case "u" -> useBot(args);
            case "st" -> stopTasks();
            case "t" -> taskCommand(args);
            case "k" -> killBot(args);
            case "gui" -> openGui();
            default -> logDirect("Unknown subcommand: " + subcommand, Formatting.RED);
        }
    }

    private void createBot(IArgConsumer args) throws CommandException {
        args.requireMin(2);
        String nick = args.getString();
        String serverIP = args.getString();
        
        Bot bot = botManager.createBot(nick, serverIP);
        BotGUI.getInstance().addBotTab(bot);
        logDirect("Bot created: " + bot.getNick() + " (ID: " + bot.getId() + ")", Formatting.GREEN);
    }

    private void listBots() {
        List<String> bots = botManager.listBots();
        if (bots.isEmpty()) {
            logDirect("No bots created", Formatting.YELLOW);
            return;
        }
        logDirect("Bots:", Formatting.AQUA);
        for (String bot : bots) {
            logDirect("  " + bot, Formatting.GRAY);
        }
    }

    private void useBot(IArgConsumer args) throws CommandException {
        args.requireMin(2);
        String idArg = args.getString().toLowerCase();
        String action = args.getString().toLowerCase();

        if (idArg.equals("sa")) {
            selectAll = true;
            logDirect("Select all enabled", Formatting.GREEN);
            return;
        }

        if (idArg.equals("sm")) {
            String[] ids = action.split(",");
            selectedBots = new int[ids.length];
            for (int i = 0; i < ids.length; i++) {
                selectedBots[i] = Integer.parseInt(ids[i].trim());
            }
            logDirect("Selected " + selectedBots.length + " bots", Formatting.GREEN);
            return;
        }

        int botId = Integer.parseInt(idArg);
        Bot bot = botManager.getBot(botId);
        if (bot == null) {
            logDirect("Bot not found: " + botId, Formatting.RED);
            return;
        }

        switch (action) {
            case "s" -> {
                if (args.hasAny()) {
                    StringBuilder msg = new StringBuilder();
                    while (args.hasAny()) {
                        msg.append(args.getString()).append(" ");
                    }
                    botManager.addTask(botId, "say", msg.toString().trim());
                    logDirect("Task added: say", Formatting.GREEN);
                }
            }
            case "c" -> {
                if (args.hasAny()) {
                    StringBuilder msg = new StringBuilder();
                    while (args.hasAny()) {
                        msg.append(args.getString()).append(" ");
                    }
                    botManager.addTask(botId, "chat", msg.toString().trim());
                    logDirect("Task added: chat", Formatting.GREEN);
                }
            }
            case "cs" -> {
                if (args.hasAny()) {
                    StringBuilder msg = new StringBuilder();
                    while (args.hasAny()) {
                        msg.append(args.getString()).append(" ");
                    }
                    if (selectAll) {
                        for (Bot b : botManager.getAllBots()) {
                            botManager.addTask(b.getId(), "cs", msg.toString().trim());
                        }
                        logDirect("Chatspammer started on all bots", Formatting.GREEN);
                    } else if (selectedBots.length > 0) {
                        for (int id : selectedBots) {
                            botManager.addTask(id, "cs", msg.toString().trim());
                        }
                        logDirect("Chatspammer started on " + selectedBots.length + " bots", Formatting.GREEN);
                    } else {
                        botManager.addTask(botId, "cs", msg.toString().trim());
                        logDirect("Task added: chatspammer", Formatting.GREEN);
                    }
                }
            }
            case "csd" -> {
                if (args.hasAny()) {
                    String delay = args.getString();
                    StringBuilder msg = new StringBuilder();
                    while (args.hasAny()) {
                        msg.append(args.getString()).append(" ");
                    }
                    if (selectAll) {
                        for (Bot b : botManager.getAllBots()) {
                            botManager.addTask(b.getId(), "csd", delay, msg.toString().trim());
                        }
                        logDirect("Chatspammer with delay started on all bots", Formatting.GREEN);
                    } else if (selectedBots.length > 0) {
                        for (int id : selectedBots) {
                            botManager.addTask(id, "csd", delay, msg.toString().trim());
                        }
                        logDirect("Chatspammer with delay started on " + selectedBots.length + " bots", Formatting.GREEN);
                    } else {
                        botManager.addTask(botId, "csd", delay, msg.toString().trim());
                        logDirect("Task added: chatspammer with delay", Formatting.GREEN);
                    }
                }
            }
            case "csm" -> {
                if (args.hasAny()) {
                    StringBuilder remaining = new StringBuilder();
                    while (args.hasAny()) {
                        remaining.append(args.getString()).append(" ");
                    }
                    String[] messages = parseMessages(remaining.toString());
                    if (selectAll) {
                        for (Bot b : botManager.getAllBots()) {
                            BotTask task = new BotTask(b.getId(), "csm");
                            task.setMessages(messages);
                            b.addTask(task);
                        }
                        logDirect("Chatspammer multiple started on all bots", Formatting.GREEN);
                    } else if (selectedBots.length > 0) {
                        for (int id : selectedBots) {
                            Bot b = botManager.getBot(id);
                            if (b != null) {
                                BotTask task = new BotTask(id, "csm");
                                task.setMessages(messages);
                                b.addTask(task);
                            }
                        }
                        logDirect("Chatspammer multiple started on " + selectedBots.length + " bots", Formatting.GREEN);
                    } else {
                        BotTask task = new BotTask(botId, "csm");
                        task.setMessages(messages);
                        bot.addTask(task);
                        logDirect("Task added: chatspammer multiple", Formatting.GREEN);
                    }
                }
            }
            case "csmd" -> {
                if (args.hasAny()) {
                    String delay = args.getString();
                    StringBuilder remaining = new StringBuilder();
                    while (args.hasAny()) {
                        remaining.append(args.getString()).append(" ");
                    }
                    String[] messages = parseMessages(remaining.toString());
                    if (selectAll) {
                        for (Bot b : botManager.getAllBots()) {
                            BotTask task = new BotTask(b.getId(), "csmd", delay);
                            task.setMessages(messages);
                            task.setDelay((long) (Double.parseDouble(delay) * 1000));
                            b.addTask(task);
                        }
                        logDirect("Chatspammer multiple with delay started on all bots", Formatting.GREEN);
                    } else if (selectedBots.length > 0) {
                        for (int id : selectedBots) {
                            Bot b = botManager.getBot(id);
                            if (b != null) {
                                BotTask task = new BotTask(id, "csmd", delay);
                                task.setMessages(messages);
                                task.setDelay((long) (Double.parseDouble(delay) * 1000));
                                b.addTask(task);
                            }
                        }
                        logDirect("Chatspammer multiple with delay started on " + selectedBots.length + " bots", Formatting.GREEN);
                    } else {
                        BotTask task = new BotTask(botId, "csmd", delay);
                        task.setMessages(messages);
                        task.setDelay((long) (Double.parseDouble(delay) * 1000));
                        bot.addTask(task);
                        logDirect("Task added: chatspammer multiple with delay", Formatting.GREEN);
                    }
                }
            }
            case "css" -> {
                if (selectAll) {
                    for (Bot b : botManager.getAllBots()) {
                        botManager.addTask(b.getId(), "css");
                    }
                    selectAll = false;
                    logDirect("Chatspammer stopped on all bots", Formatting.YELLOW);
                } else if (selectedBots.length > 0) {
                    for (int id : selectedBots) {
                        botManager.addTask(id, "css");
                    }
                    selectedBots = new int[0];
                    logDirect("Chatspammer stopped on selected bots", Formatting.YELLOW);
                } else {
                    botManager.addTask(botId, "css");
                    logDirect("Chatspammer stopped", Formatting.YELLOW);
                }
            }
            case "gs" -> {
                String stopstream = args.hasAny() ? args.getString() : "";
                botManager.addTask(botId, "getscreen", stopstream);
                logDirect("Task added: getscreen", Formatting.GREEN);
            }
            default -> logDirect("Unknown action: " + action, Formatting.RED);
        }
    }

    private String[] parseMessages(String input) {
        java.util.List<String> messages = new java.util.ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"([^\"]*)\"");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            messages.add(matcher.group(1));
        }
        return messages.toArray(new String[0]);
    }

    private void stopTasks() {
        logDirect("All tasks stopped", Formatting.YELLOW);
    }

    private void taskCommand(IArgConsumer args) throws CommandException {
        if (!args.hasAny()) return;
        String action = args.getString().toLowerCase();

        switch (action) {
            case "l" -> listAllTasks();
            case "k" -> {
                if (args.hasAny()) {
                    int taskId = Integer.parseInt(args.getString());
                    logDirect("Task killed: " + taskId, Formatting.YELLOW);
                }
            }
            default -> logDirect("Unknown task action: " + action, Formatting.RED);
        }
    }

    private void listAllTasks() {
        logDirect("Active tasks:", Formatting.AQUA);
        for (Bot bot : botManager.getAllBots()) {
            List<String> tasks = botManager.listTasks(bot.getId());
            if (!tasks.isEmpty()) {
                logDirect("  Bot " + bot.getId() + ":", Formatting.GRAY);
                for (String task : tasks) {
                    logDirect("    " + task, Formatting.GRAY);
                }
            }
        }
    }

    private void killBot(IArgConsumer args) throws CommandException {
        if (!args.hasAny()) return;
        int botId = Integer.parseInt(args.getString());
        botManager.removeBot(botId);
        BotGUI.getInstance().removeBotTab(botId);
        logDirect("Bot killed: " + botId, Formatting.YELLOW);
    }

    private void openGui() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            BotGUI gui = BotGUI.getInstance();
            if (gui.isVisible()) {
                gui.hide();
            } else {
                gui.show();
            }
        });
        logDirect("Bot GUI toggled", Formatting.GREEN);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return Stream.of("c", "list", "u", "st", "t", "k", "gui");
    }

    @Override
    public String getShortDesc() {
        return "Bot management system";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
            "Bot management system",
            ".bot c <nick> <serverIP> - Create bot",
            ".bot list - List all bots",
            ".bot u <id> <action> - Use bot",
            ".bot st - Stop all tasks",
            ".bot t <action> - Task management",
            ".bot k <id> - Kill bot",
            ".bot gui - Open bot GUI"
        );
    }
}
