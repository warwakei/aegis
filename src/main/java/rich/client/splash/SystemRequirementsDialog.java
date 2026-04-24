package rich.client.splash;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SystemRequirementsDialog extends JDialog {

    private volatile boolean proceed;

    public SystemRequirementsDialog(List<SystemRequirementsChecker.RequirementResult> failedRequirements) {
        setTitle("Системные требования не выполнены");
        setModal(true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setSize(560, 360);
        setLocationRelativeTo(null);
        setResizable(false);
        setAlwaysOnTop(true);
        setAutoRequestFocus(true);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBackground(new Color(20, 22, 28));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel titleLabel = new JLabel("Aegis не может запуститься");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(255, 120, 120));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JTextArea messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setBackground(new Color(20, 22, 28));
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageArea.setForeground(new Color(220, 226, 236));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        StringBuilder message = new StringBuilder();
        message.append("Обнаружены проблемы с окружением:\n\n");

        for (SystemRequirementsChecker.RequirementResult result : failedRequirements) {
            message.append("• ").append(result.getMessage()).append("\n");
        }

        message.append("\nВы можете продолжить на свой риск или закрыть игру.");
        messageArea.setText(message.toString());

        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(44, 48, 60), 1));
        scrollPane.getViewport().setBackground(new Color(20, 22, 28));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(new Color(20, 22, 28));

        JButton continueButton = new JButton("Продолжить") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                // Блюр тень
                for (int i = 0; i < 6; i++) {
                    int a = Math.max(0, 20 - i * 3);
                    g2.setColor(new Color(0, 0, 0, a));
                    g2.fillRoundRect(i, i, w - i * 2, h - i * 2, 8 + i, 8 + i);
                }
                
                // Градиент кнопки (темнее)
                Color baseColor = getModel().isRollover() ? new Color(40, 115, 210) : new Color(35, 100, 185);
                GradientPaint gradient = new GradientPaint(0, 0, baseColor, 0, h, baseColor.darker());
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                
                // Граница
                g2.setColor(new Color(60, 130, 220, 150));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                g2.dispose();
                
                super.paintComponent(g);
            }
        };
        continueButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        continueButton.setForeground(Color.WHITE);
        continueButton.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        continueButton.setFocusPainted(false);
        continueButton.setOpaque(false);
        continueButton.setContentAreaFilled(false);

        JButton exitButton = new JButton("Закрыть игру") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                // Блюр тень
                for (int i = 0; i < 6; i++) {
                    int a = Math.max(0, 20 - i * 3);
                    g2.setColor(new Color(0, 0, 0, a));
                    g2.fillRoundRect(i, i, w - i * 2, h - i * 2, 8 + i, 8 + i);
                }
                
                // Градиент кнопки (темнее)
                Color baseColor = getModel().isRollover() ? new Color(155, 50, 50) : new Color(135, 40, 40);
                GradientPaint gradient = new GradientPaint(0, 0, baseColor, 0, h, baseColor.darker());
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                
                // Граница
                g2.setColor(new Color(180, 70, 70, 150));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                g2.dispose();
                
                super.paintComponent(g);
            }
        };
        exitButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        exitButton.setForeground(Color.WHITE);
        exitButton.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        exitButton.setFocusPainted(false);
        exitButton.setOpaque(false);
        exitButton.setContentAreaFilled(false);

        continueButton.addActionListener(e -> {
            proceed = true;
            setVisible(false);
            dispose();
        });

        exitButton.addActionListener(e -> {
            proceed = false;
            setVisible(false);
            dispose();
        });

        buttonPanel.add(continueButton);
        buttonPanel.add(exitButton);

        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private boolean showBlocking() {
        toFront();
        requestFocus();
        setVisible(true);
        return proceed;
    }

    public static boolean showRequirementsDialog(List<SystemRequirementsChecker.RequirementResult> results) {
        List<SystemRequirementsChecker.RequirementResult> failedRequirements = results.stream()
                .filter(r -> !r.isPassed())
                .toList();

        if (failedRequirements.isEmpty()) {
            return true;
        }

        AtomicBoolean accepted = new AtomicBoolean(false);
        Runnable showTask = () -> {
            SystemRequirementsDialog dialog = new SystemRequirementsDialog(failedRequirements);
            accepted.set(dialog.showBlocking());
        };

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                showTask.run();
            } else {
                SwingUtilities.invokeAndWait(showTask);
            }
        } catch (Exception e) {
            return false;
        }

        return accepted.get();
    }
}
