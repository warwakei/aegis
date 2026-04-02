package fun.aegis.display.screens.bot;

import fun.aegis.features.bot.Bot;
import fun.aegis.features.bot.BotManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.List;

public class BotGUI extends JFrame implements KeyListener {
    private static BotGUI instance;
    private BotManager botManager;
    private JTextArea outputArea;
    private JTextField inputField;
    private List<String> commandHistory;
    private int historyIndex;
    private JTabbedPane tabbedPane;
    private Map<Integer, JTextArea> botChatAreas;

    private BotGUI() {
        this.botManager = BotManager.getInstance();
        this.commandHistory = new ArrayList<>();
        this.historyIndex = -1;
        this.botChatAreas = new HashMap<>();
        initGUI();
    }

    public static BotGUI getInstance() {
        if (instance == null) {
            instance = new BotGUI();
        }
        return instance;
    }

    private void initGUI() {
        setTitle("Bot Manager");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);

        Color bgColor = new Color(25, 25, 25);
        Color fgColor = new Color(0, 255, 100);
        Color accentColor = new Color(50, 50, 50);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(bgColor);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(accentColor);
        tabbedPane.setForeground(fgColor);
        tabbedPane.setFont(new Font("Monospaced", Font.BOLD, 12));

        tabbedPane.addTab("Console", createConsoleTab());
        tabbedPane.addTab("Help", createHelpTab());
        tabbedPane.addTab("Tasks", createTasksTab());

        inputField = new JTextField();
        inputField.setBackground(accentColor);
        inputField.setForeground(fgColor);
        inputField.setFont(new Font("Monospaced", Font.PLAIN, 13));
        inputField.setCaretColor(fgColor);
        inputField.setBorder(BorderFactory.createLineBorder(fgColor, 1));
        inputField.addKeyListener(this);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBackground(bgColor);
        JLabel promptLabel = new JLabel(">>> ");
        promptLabel.setForeground(fgColor);
        promptLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
        inputPanel.add(promptLabel, BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        add(mainPanel);
        updateBotList();
    }

    public void addBotTab(Bot bot) {
        JPanel botPanel = new JPanel(new BorderLayout(5, 5));
        botPanel.setBackground(new Color(25, 25, 25));
        botPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(20, 20, 20));
        chatArea.setForeground(new Color(0, 255, 100));
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        chatArea.append("[Bot " + bot.getId() + " connected]\n");

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBackground(new Color(20, 20, 20));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 50), 1));

        botChatAreas.put(bot.getId(), chatArea);
        botPanel.add(scrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Bot " + bot.getId(), botPanel);
    }

    public void removeBotTab(int botId) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getTitleAt(i).equals("Bot " + botId)) {
                tabbedPane.removeTabAt(i);
                break;
            }
        }
        botChatAreas.remove(botId);
    }

    public void appendBotChat(int botId, String message) {
        JTextArea area = botChatAreas.get(botId);
        if (area != null) {
            area.append(message + "\n");
            area.setCaretPosition(area.getDocument().getLength());
        }
    }

    private JPanel createConsoleTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(25, 25, 25));

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setBackground(new Color(20, 20, 20));
        outputArea.setForeground(new Color(0, 255, 100));
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBackground(new Color(20, 20, 20));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 50), 1));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHelpTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(25, 25, 25));

        JTextArea helpArea = new JTextArea();
        helpArea.setEditable(false);
        helpArea.setBackground(new Color(20, 20, 20));
        helpArea.setForeground(new Color(0, 255, 100));
        helpArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        StringBuilder help = new StringBuilder();
        help.append("╔════════════════════════════════════════╗\n");
        help.append("║     BOT MANAGER - COMMAND REFERENCE    ║\n");
        help.append("╚════════════════════════════════════════╝\n\n");
        help.append("CREATE BOT:\n");
        help.append("  .bot c <nick> <serverIP:port>\n");
        help.append("  Example: .bot c MyBot 192.168.1.1:25565\n\n");
        help.append("LIST BOTS:\n");
        help.append("  .bot list\n\n");
        help.append("SPAM COMMANDS:\n");
        help.append("  .bot u <id> cs <message>           - spam\n");
        help.append("  .bot u <id> csd <delay> <message>  - spam with delay\n");
        help.append("  .bot u <id> csm \"msg1\", \"msg2\"    - spam multiple\n");
        help.append("  .bot u <id> csmd <delay> \"m1\", \"m2\" - spam multiple delay\n");
        help.append("  .bot u <id> css                     - stop spam\n\n");
        help.append("MULTI-BOT:\n");
        help.append("  .bot u sa                           - select all\n");
        help.append("  .bot u sm 1,2,3 <action>           - select multiple\n\n");
        help.append("OTHER:\n");
        help.append("  .bot u <id> gs                      - view bot screen\n");
        help.append("  .bot u <id> gs ss                   - stop viewing\n");
        help.append("  .bot k <id>                         - kill bot\n");
        help.append("  .bot gui                            - toggle this window\n");

        helpArea.setText(help.toString());
        helpArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(helpArea);
        scrollPane.setBackground(new Color(20, 20, 20));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 50), 1));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTasksTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(25, 25, 25));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JTextArea tasksArea = new JTextArea();
        tasksArea.setEditable(false);
        tasksArea.setBackground(new Color(20, 20, 20));
        tasksArea.setForeground(new Color(0, 255, 100));
        tasksArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        tasksArea.setLineWrap(true);
        tasksArea.setWrapStyleWord(true);
        tasksArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        tasksArea.setText("Active Tasks:\n\n");

        JScrollPane scrollPane = new JScrollPane(tasksArea);
        scrollPane.setBackground(new Color(20, 20, 20));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 50), 1));

        JButton refreshButton = new JButton("REFRESH");
        refreshButton.setBackground(new Color(50, 50, 50));
        refreshButton.setForeground(new Color(0, 255, 100));
        refreshButton.setFont(new Font("Monospaced", Font.BOLD, 11));
        refreshButton.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 100), 1));
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> {
            tasksArea.setText("Active Tasks:\n\n");
            for (Bot bot : botManager.getAllBots()) {
                tasksArea.append("┌─ Bot " + bot.getId() + " (" + bot.getNick() + ")\n");
                List<String> tasks = botManager.listTasks(bot.getId());
                if (tasks.isEmpty()) {
                    tasksArea.append("│  └─ No active tasks\n");
                } else {
                    for (int i = 0; i < tasks.size(); i++) {
                        String prefix = i == tasks.size() - 1 ? "│  └─ " : "│  ├─ ";
                        tasksArea.append(prefix + tasks.get(i) + "\n");
                    }
                }
                tasksArea.append("└\n\n");
            }
            tasksArea.setCaretPosition(tasksArea.getDocument().getLength());
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(25, 25, 25));
        buttonPanel.add(refreshButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    public void show() {
        setVisible(true);
        inputField.requestFocus();
    }

    public void hide() {
        setVisible(false);
    }

    private void updateBotList() {
        if (outputArea == null) return;
        outputArea.setText("");
        outputArea.append("╔════════════════════════════════════════╗\n");
        outputArea.append("║          BOT MANAGER v1.0             ║\n");
        outputArea.append("╚════════════════════════════════════════╝\n\n");
        outputArea.append("Active Bots:\n");
        for (Bot bot : botManager.getAllBots()) {
            outputArea.append("  ├─ " + bot.getInfo() + "\n");
        }
        outputArea.append("\n");
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            String cmd = inputField.getText().trim();
            if (!cmd.isEmpty()) {
                if (outputArea != null) {
                    outputArea.append(">>> " + cmd + "\n");
                    outputArea.setCaretPosition(outputArea.getDocument().getLength());
                }
                commandHistory.add(cmd);
                historyIndex = -1;
                inputField.setText("");
            }
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            if (historyIndex < commandHistory.size() - 1) {
                historyIndex++;
                inputField.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
            }
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            if (historyIndex > 0) {
                historyIndex--;
                inputField.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
            } else if (historyIndex == 0) {
                historyIndex = -1;
                inputField.setText("");
            }
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            hide();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}
}
