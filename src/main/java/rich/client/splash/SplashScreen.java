package rich.client.splash;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class SplashScreen extends JFrame {
    private static final String CARD_PROGRESS = "progress";
    private static final String CARD_REQUIREMENTS = "requirements";
    private static final String CARD_READY = "ready";
    private static final String CARD_FINISHING = "finishing";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private final JProgressBar progressBar;
    private final JLabel percentLabel;
    private final JLabel statusLabel;

    private final JTextArea requirementsArea;
    private final JLabel requirementsTitle;
    private final JLabel requirementsSubtitle;

    private volatile CountDownLatch requirementsLatch;
    private volatile AtomicBoolean requirementsDecision;
    private volatile CountDownLatch launchLatch;

    private volatile int currentProgress = 0;
    private volatile String currentStatus = "Инициализация...";

    private Timer animationTimer;
    private volatile float animationTime = 0f;
    private volatile float readyAnim = 0f;
    private volatile boolean readyVisible = false;

    private JLabel readyTitleLabel;
    private JLabel injectedLabel;
    private RoundedActionButton launchButton;

    private JLabel finishingLabel;
    private Timer finishingDotsTimer;
    private volatile int finishingDots = 0;

    public SplashScreen() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);
        setResizable(false);
        setSize(660, 340);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));

        // Таймер для плавной анимации
        animationTimer = new Timer(16, e -> {
            animationTime += 0.016f;
            if (readyVisible && readyAnim < 1f) {
                readyAnim = Math.min(1f, readyAnim + 0.03f);
                updateReadyVisuals();
            }
            repaint();
        });
        animationTimer.start();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyWindowShape();
            }
        });

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int w = getWidth();
                int h = getHeight();
                
                long time = System.currentTimeMillis();
                float t = time / 1000.0f;

                // Анимированный градиентный фон
                float bgShift = (float)(Math.sin(t * 0.3) * 0.2);
                GradientPaint bg = new GradientPaint(
                        0, 0, new Color(7 + (int)(bgShift * 10), 8 + (int)(bgShift * 8), 13 + (int)(bgShift * 15)),
                        0, h, new Color(19 + (int)(bgShift * 8), 15 + (int)(bgShift * 12), 30 + (int)(bgShift * 10))
                );
                g2.setPaint(bg);
                g2.fillRoundRect(0, 0, w, h, 28, 28);

                // Плавающие неоновые орбы с улучшенной анимацией
                float pulse1 = (float) ((Math.sin(t * 1.2f) + 1.0) * 0.5);
                float pulse2 = (float) ((Math.cos(t * 0.8f) + 1.0) * 0.5);

                int orbAX = (int) (w * 0.16f + Math.sin(t * 0.6f) * 35f);
                int orbAY = (int) (h * 0.23f + Math.cos(t * 0.4f) * 20f);
                int orbBX = (int) (w * 0.80f + Math.cos(t * 0.5f) * 30f);
                int orbBY = (int) (h * 0.74f + Math.sin(t * 0.35f) * 25f);
                int orbCX = (int) (w * 0.5f + Math.sin(t * 0.7f) * 40f);
                int orbCY = (int) (h * 0.1f + Math.cos(t * 0.6f) * 15f);

                // Первый орб - фиолетовый
                RadialGradientPaint orbA = new RadialGradientPaint(
                        new Point(orbAX, orbAY),
                        250f,
                        new float[]{0f, 0.7f, 1f},
                        new Color[]{
                            new Color(146, 93, 255, (int)(60 + 40 * pulse1)), 
                            new Color(146, 93, 255, (int)(20 + 15 * pulse1)), 
                            new Color(146, 93, 255, 0)
                        }
                );
                g2.setPaint(orbA);
                g2.fillOval(orbAX - 250, orbAY - 250, 500, 500);

                // Второй орб - синий
                RadialGradientPaint orbB = new RadialGradientPaint(
                        new Point(orbBX, orbBY),
                        280f,
                        new float[]{0f, 0.6f, 1f},
                        new Color[]{
                            new Color(70, 148, 255, (int)(45 + 35 * pulse2)), 
                            new Color(70, 148, 255, (int)(15 + 20 * pulse2)), 
                            new Color(70, 148, 255, 0)
                        }
                );
                g2.setPaint(orbB);
                g2.fillOval(orbBX - 280, orbBY - 280, 560, 560);

                // Третий орб - розовый
                RadialGradientPaint orbC = new RadialGradientPaint(
                        new Point(orbCX, orbCY),
                        200f,
                        new float[]{0f, 0.8f, 1f},
                        new Color[]{
                            new Color(255, 120, 180, (int)(35 + 25 * pulse1)), 
                            new Color(255, 120, 180, (int)(10 + 15 * pulse1)), 
                            new Color(255, 120, 180, 0)
                        }
                );
                g2.setPaint(orbC);
                g2.fillOval(orbCX - 200, orbCY - 200, 400, 400);

                // Улучшенная рамка контейнера с неоновым свечением
                int cx = 26;
                int cy = 24;
                int cw = w - 52;
                int ch = h - 48;
                
                for (int i = 0; i < 15; i++) {
                    int a = Math.max(0, (int)(35 - i * 2.5 + pulse1 * 10));
                    Color frameGlow = new Color(125 + (int)(pulse1 * 30), 103 + (int)(pulse2 * 25), 170 + (int)(pulse1 * 40), a);
                    g2.setColor(frameGlow);
                    g2.fillRoundRect(cx - i, cy - i, cw + i * 2, ch + i * 2, 26 + i, 26 + i);
                }

                // Анимированные световые акценты по углам
                g2.setColor(new Color(255, 255, 255, (int)(30 + pulse1 * 20)));
                g2.fillOval(cx + 5, cy + 5, 6, 6);
                g2.fillOval(cx + cw - 11, cy + 5, 6, 6);
                g2.fillOval(cx + 5, cy + ch - 11, 6, 6);
                g2.fillOval(cx + cw - 11, cy + ch - 11, 6, 6);
                g2.fillRoundRect(12, 12, w - 24, h - 24, 24, 24);

                g2.setColor(new Color(255, 255, 255, 22));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 28, 28);
                g2.dispose();
            }
        };
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        root.setOpaque(false);

        progressBar = new JProgressBar(0, 100);
        progressBar.setOpaque(false);
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.setStringPainted(false);
        progressBar.setUI(new BasicProgressBarUI() {
            @Override
            protected Dimension getPreferredInnerHorizontal() {
                return new Dimension(530, 16);
            }

            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Insets b = progressBar.getInsets();
                int barRectWidth = progressBar.getWidth() - (b.right + b.left);
                int barRectHeight = progressBar.getHeight() - (b.top + b.bottom);
                if (barRectWidth <= 0 || barRectHeight <= 0) {
                    g2.dispose();
                    return;
                }

                int amountFull = getAmountFull(b, barRectWidth, barRectHeight);

                g2.setColor(new Color(255, 255, 255, 22));
                g2.fillRoundRect(b.left, b.top, barRectWidth, barRectHeight, 14, 14);

                if (amountFull > 0) {
                    GradientPaint fill = new GradientPaint(
                            0, 0, new Color(111, 74, 240),
                            amountFull, 0, new Color(74, 152, 236)
                    );
                    g2.setPaint(fill);
                    g2.fillRoundRect(b.left, b.top, amountFull, barRectHeight, 14, 14);

                    int stripeOffset = (int) ((animationTime * 95f) % 24f);
                    g2.setClip(b.left, b.top, amountFull, barRectHeight);
                    for (int x = -24 + stripeOffset; x < amountFull + 24; x += 24) {
                        g2.setColor(new Color(255, 255, 255, 24));
                        g2.fillRoundRect(b.left + x, b.top + 1, 9, barRectHeight - 2, 8, 8);
                    }
                    g2.setClip(null);

                    GradientPaint gloss = new GradientPaint(
                            0, b.top, new Color(255, 255, 255, 90),
                            0, b.top + barRectHeight, new Color(255, 255, 255, 0)
                    );
                    g2.setPaint(gloss);
                    g2.fillRoundRect(b.left + 1, b.top + 1, Math.max(0, amountFull - 2), Math.max(2, barRectHeight / 2), 12, 12);
                }
                g2.dispose();
            }
        });

        JLabel brandLabel = new JLabel("AEGIS NEO", SwingConstants.CENTER);
        brandLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
        brandLabel.setForeground(new Color(168, 183, 218));

        percentLabel = new JLabel("0%", SwingConstants.CENTER);
        percentLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 50));
        percentLabel.setForeground(Color.WHITE);

        statusLabel = new JLabel(currentStatus, SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(208, 201, 225));

        JPanel progressContent = new FrostedPanel(18);
        progressContent.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        progressContent.add(brandLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(8, 0, 0, 0);
        progressContent.add(percentLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(12, 16, 0, 16);
        progressContent.add(progressBar, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(10, 0, 0, 0);
        progressContent.add(statusLabel, gbc);

        JPanel progressCard = new JPanel(new BorderLayout());
        progressCard.setOpaque(false);
        progressCard.add(progressContent, BorderLayout.CENTER);

        requirementsTitle = new JLabel("Системные требования не выполнены", SwingConstants.CENTER);
        requirementsTitle.setFont(new Font("Segoe UI Semibold", Font.BOLD, 20));
        requirementsTitle.setForeground(new Color(255, 190, 190));

        requirementsSubtitle = new JLabel("Некоторые параметры могут ухудшить стабильность и графику", SwingConstants.CENTER);
        requirementsSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        requirementsSubtitle.setForeground(new Color(206, 194, 216));

        requirementsArea = new JTextArea();
        requirementsArea.setEditable(false);
        requirementsArea.setLineWrap(true);
        requirementsArea.setWrapStyleWord(true);
        requirementsArea.setOpaque(false);
        requirementsArea.setForeground(new Color(236, 230, 247));
        requirementsArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JScrollPane reqScroll = new JScrollPane(requirementsArea);
        reqScroll.setOpaque(false);
        reqScroll.getViewport().setOpaque(false);
        reqScroll.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JButton continueButton = new RoundedActionButton("Продолжить", new Color(35, 28, 55, 235), new Color(95, 78, 165, 200));
        continueButton.addActionListener(e -> finishRequirements(true));

        JButton exitButton = new RoundedActionButton("Закрыть игру", new Color(48, 30, 42, 235), new Color(155, 85, 115, 200));
        exitButton.addActionListener(e -> finishRequirements(false));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(continueButton);
        actions.add(exitButton);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setOpaque(false);
        titlePanel.add(requirementsTitle);
        titlePanel.add(requirementsSubtitle);

        JPanel requirementsContent = new FrostedPanel(18);
        requirementsContent.setLayout(new BorderLayout(10, 10));
        requirementsContent.add(titlePanel, BorderLayout.NORTH);
        requirementsContent.add(reqScroll, BorderLayout.CENTER);
        requirementsContent.add(actions, BorderLayout.SOUTH);

        JPanel requirementsCard = new JPanel(new BorderLayout());
        requirementsCard.setOpaque(false);
        requirementsCard.add(requirementsContent, BorderLayout.CENTER);

        readyTitleLabel = new JLabel("Successfully loaded.", SwingConstants.CENTER);
        readyTitleLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 28));
        readyTitleLabel.setForeground(new Color(232, 240, 255, 0));

        launchButton = new RoundedActionButton("launch", new Color(38, 45, 70, 220), new Color(124, 162, 230, 180));
        launchButton.setPreferredSize(new Dimension(132, 40));
        launchButton.setVisualAlpha(0f);
        launchButton.addActionListener(e -> {
            CountDownLatch latch = launchLatch;
            if (latch != null) {
                latch.countDown();
            }
        });

        JPanel launchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        launchPanel.setOpaque(false);
        launchPanel.add(launchButton);

        JPanel centerGap = new JPanel(new BorderLayout());
        centerGap.setOpaque(false);
        centerGap.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        JPanel readyContent = new FrostedPanel(18);
        readyContent.setLayout(new BorderLayout(0, 5));
        readyContent.add(readyTitleLabel, BorderLayout.NORTH);
        readyContent.add(centerGap, BorderLayout.CENTER);
        readyContent.add(launchPanel, BorderLayout.SOUTH);

        JPanel readyCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                float eased = readyAnim * readyAnim * (3f - 2f * readyAnim);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, eased))));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        readyCard.setOpaque(false);
        readyCard.add(readyContent, BorderLayout.CENTER);

        finishingLabel = new JLabel("Finishing", SwingConstants.CENTER);
        finishingLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 26));
        finishingLabel.setForeground(new Color(232, 240, 255));

        JPanel finishingContent = new FrostedPanel(18);
        finishingContent.setLayout(new BorderLayout());
        finishingContent.add(finishingLabel, BorderLayout.CENTER);

        JPanel finishingCard = new JPanel(new BorderLayout());
        finishingCard.setOpaque(false);
        finishingCard.add(finishingContent, BorderLayout.CENTER);

        contentPanel.setOpaque(false);
        contentPanel.add(progressCard, CARD_PROGRESS);
        contentPanel.add(requirementsCard, CARD_REQUIREMENTS);
        contentPanel.add(readyCard, CARD_READY);
        contentPanel.add(finishingCard, CARD_FINISHING);

        root.add(contentPanel, BorderLayout.CENTER);
        add(root);

        applyWindowShape();
        switchToProgressCard();
        setVisible(true);
        toFront();
        requestFocus();
    }

    private void applyWindowShape() {
        setShape(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 28, 28));
    }

    private void switchToProgressCard() {
        SwingUtilities.invokeLater(() -> {
            readyVisible = false;
            cardLayout.show(contentPanel, CARD_PROGRESS);
            toFront();
            requestFocus();
        });
    }

    private void switchToRequirementsCard(String message) {
        SwingUtilities.invokeLater(() -> {
            readyVisible = false;
            requirementsArea.setText(message);
            requirementsArea.setCaretPosition(0);
            cardLayout.show(contentPanel, CARD_REQUIREMENTS);
            toFront();
            requestFocus();
        });
    }

    private void switchToReadyCard() {
        SwingUtilities.invokeLater(() -> {
            readyAnim = 0f;
            readyVisible = true;
            cardLayout.show(contentPanel, CARD_READY);
            toFront();
            requestFocus();
        });
    }

    private void switchToFinishingCard() {
        SwingUtilities.invokeLater(() -> {
            readyVisible = false;
            cardLayout.show(contentPanel, CARD_FINISHING);
            toFront();
            requestFocus();
        });
    }

    private float easeOutCubic(float x) {
        float inv = 1f - x;
        return 1f - inv * inv * inv;
    }

    private void updateReadyVisuals() {
        float eased = easeOutCubic(Math.max(0f, Math.min(1f, readyAnim)));

        if (readyTitleLabel != null) {
            int a = (int) (255f * eased);
            int yPad = (int) (18f * (1f - eased));
            readyTitleLabel.setForeground(new Color(232, 240, 255, Math.max(0, Math.min(255, a))));
            readyTitleLabel.setBorder(BorderFactory.createEmptyBorder(Math.max(0, yPad), 0, 0, 0));
        }

        if (launchButton != null) {
            float btnAlpha = Math.max(0f, Math.min(1f, eased));
            int yPad = (int) (16f * (1f - eased));
            launchButton.setVisualAlpha(btnAlpha);
            launchButton.setBorder(BorderFactory.createEmptyBorder(9 + Math.max(0, yPad), 18, 9, 18));
            launchButton.setForeground(new Color(245, 240, 252, (int) (255f * btnAlpha)));
        }
    }

    private void finishRequirements(boolean proceed) {
        AtomicBoolean decision = requirementsDecision;
        CountDownLatch latch = requirementsLatch;
        if (decision != null && latch != null) {
            decision.set(proceed);
            latch.countDown();
        }
    }

    public boolean showRequirements(List<SystemRequirementsChecker.RequirementResult> failedRequirements) {
        StringBuilder sb = new StringBuilder();
        sb.append("Обнаружены следующие проблемы:\n\n");
        for (SystemRequirementsChecker.RequirementResult result : failedRequirements) {
            sb.append("• ").append(result.getMessage()).append('\n');
        }
        sb.append("\nВыберите действие для продолжения.");

        requirementsDecision = new AtomicBoolean(false);
        requirementsLatch = new CountDownLatch(1);
        switchToRequirementsCard(sb.toString());

        try {
            requirementsLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            switchToProgressCard();
        }
        return requirementsDecision.get();
    }

    public void updateProgress(int progress, String status) {
        SwingUtilities.invokeLater(() -> {
            currentProgress = Math.min(100, Math.max(0, progress));
            currentStatus = status;
            progressBar.setValue(currentProgress);
            percentLabel.setText(currentProgress + "%");
            statusLabel.setText(status);
            cardLayout.show(contentPanel, CARD_PROGRESS);
            repaint();
        });
    }

    public void showReadyAndWaitForLaunch() {
        launchLatch = new CountDownLatch(1);
        switchToReadyCard();
        try {
            launchLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shows a minimal finishing screen and animates dots ("Finishing...") until {@link #close()} is called.
     * Must be safe to call from any thread.
     */
    public void showFinishing() {
        switchToFinishingCard();
        SwingUtilities.invokeLater(() -> {
            if (finishingDotsTimer != null && finishingDotsTimer.isRunning()) {
                return;
            }

            finishingDots = 0;
            finishingDotsTimer = new Timer(350, e -> {
                finishingDots = (finishingDots + 1) % 4;
                StringBuilder sb = new StringBuilder("Finishing");
                for (int i = 0; i < finishingDots; i++) sb.append('.');
                if (finishingLabel != null) {
                    finishingLabel.setText(sb.toString());
                }
            });
            finishingDotsTimer.start();
        });
    }

    public void setProgress(int progress) {
        updateProgress(progress, currentStatus);
    }

    public void setStatus(String status) {
        updateProgress(currentProgress, status);
    }

    public int getProgress() {
        return currentProgress;
    }

    public void close() {
        SwingUtilities.invokeLater(() -> {
            if (finishingDotsTimer != null && finishingDotsTimer.isRunning()) {
                finishingDotsTimer.stop();
            }
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }
            dispose();
        });
    }

    private static class FrostedPanel extends JPanel {
        private final int radius;

        private FrostedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int w = getWidth();
            int h = getHeight();

            // Анимированный неоновый блюр
            long time = System.currentTimeMillis();
            float pulse = (float)(0.5 + 0.5 * Math.sin(time / 800.0));
            
            // Многослойный блюр с неоновым свечением
            for (int i = 0; i < 20; i++) {
                int a = Math.max(0, Math.min(255, (int)(45 - i * 2 + pulse * 15)));
                Color glowColor = new Color(
                    Math.min(255, 120 + (int)(pulse * 30)), 
                    Math.min(255, 105 + (int)(pulse * 20)), 
                    Math.min(255, 180 + (int)(pulse * 40)), 
                    a
                );
                g2.setColor(glowColor);
                g2.fillRoundRect(i, i, w - i * 2, h - i * 2, radius + i * 2, radius + i * 2);
            }

            // Дополнительный внешний неоновый ореол
            for (int i = 20; i < 32; i++) {
                int a = Math.max(0, Math.min(255, (int)(18 - (i - 20) + pulse * 8)));
                Color outerGlow = new Color(
                    Math.min(255, 90 + (int)(pulse * 40)), 
                    Math.min(255, 80 + (int)(pulse * 30)), 
                    Math.min(255, 160 + (int)(pulse * 50)), 
                    a
                );
                g2.setColor(outerGlow);
                g2.fillRoundRect(i, i, w - i * 2, h - i * 2, radius + i * 3, radius + i * 3);
            }

            // Основная карточка с динамическим градиентом
            float gradientShift = (float)(Math.sin(time / 1200.0) * 0.1);
            GradientPaint card = new GradientPaint(
                    0, 0, new Color(
                        Math.min(255, 22 + (int)(gradientShift * 10)), 
                        Math.min(255, 24 + (int)(gradientShift * 8)), 
                        Math.min(255, 35 + (int)(gradientShift * 12)), 
                        240
                    ),
                    w, h, new Color(
                        Math.min(255, 28 + (int)(gradientShift * 8)), 
                        Math.min(255, 26 + (int)(gradientShift * 10)), 
                        Math.min(255, 42 + (int)(gradientShift * 15)), 
                        220
                    )
            );
            g2.setPaint(card);
            g2.fillRoundRect(0, 0, w, h, radius, radius);

            // Анимированный внутренний свет
            float innerGlow = (float)(0.3 + 0.4 * Math.sin(time / 600.0));
            g2.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, (int)(45 * innerGlow)))));
            g2.drawRoundRect(1, 1, w - 3, h - 3, radius - 1, radius - 1);
            
            // Тонкая внешняя граница с пульсацией
            g2.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, (int)(25 + pulse * 15)))));
            g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
            
            // Добавляем тонкие световые акценты по углам
            g2.setColor(new Color(150, 120, 255, Math.max(0, Math.min(255, (int)(30 + pulse * 20)))));
            g2.fillOval(5, 5, 8, 8);
            g2.fillOval(w - 13, 5, 8, 8);
            g2.fillOval(5, h - 13, 8, 8);
            g2.fillOval(w - 13, h - 13, 8, 8);
            
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedActionButton extends JButton {
        private final Color baseColor;
        private final Color strokeColor;
        private float visualAlpha = 1f;

        private RoundedActionButton(String text, Color baseColor, Color strokeColor) {
            super(text);
            this.baseColor = baseColor;
            this.strokeColor = strokeColor;

            setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
            setForeground(new Color(245, 240, 252));
            setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
            setFocusPainted(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public void setVisualAlpha(float alpha) {
            this.visualAlpha = Math.max(0f, Math.min(1f, alpha));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int w = getWidth();
            int h = getHeight();
            
            long time = System.currentTimeMillis();
            float pulse = (float)(0.5 + 0.5 * Math.sin(time / 1000.0));

            // Неоновый блюр для кнопки с анимацией
            for (int i = 0; i < 12; i++) {
                int a = Math.max(0, (int)(35 - i * 3 + pulse * 10));
                Color glowColor = getModel().isRollover() ? 
                    new Color(baseColor.getRed() + 30, baseColor.getGreen() + 20, baseColor.getBlue() + 40, a) :
                    new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), a);
                g2.setColor(glowColor);
                g2.fillRoundRect(i, i, w - i * 2, h - i * 2, 15 + i, 15 + i);
            }

            float brighten = getModel().isRollover() ? 0.2f : 0f;
            float pressed = getModel().isPressed() ? -0.15f : 0f;
            float animation = (float)(0.1 * Math.sin(time / 800.0));
            
            int r = Math.max(0, Math.min(255, (int) (baseColor.getRed() * (1f + brighten + pressed + animation))));
            int gr = Math.max(0, Math.min(255, (int) (baseColor.getGreen() * (1f + brighten + pressed + animation))));
            int b = Math.max(0, Math.min(255, (int) (baseColor.getBlue() * (1f + brighten + pressed + animation))));

            // Многослойный градиент кнопки с идеальными границами
            GradientPaint buttonGradient = new GradientPaint(
                    0, 0, new Color(
                        Math.min(255, r + 10), 
                        Math.min(255, gr + 8), 
                        Math.min(255, b + 15), 
                        (int)(baseColor.getAlpha() * visualAlpha)
                    ),
                    0, h, new Color(
                        Math.max(0, r - 20), 
                        Math.max(0, gr - 18), 
                        Math.max(0, b - 25), 
                        (int)(baseColor.getAlpha() * visualAlpha)
                    )
            );
            g2.setPaint(buttonGradient);
            g2.fillRoundRect(0, 0, w, h, 15, 15);

            // Внутренний неоновый свет при hover
            if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, (int)(35 + pulse * 15)))));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 13, 13);
            }

            // Идеально ровная граница кнопки
            int sr = Math.min(255, (int) (strokeColor.getRed() * visualAlpha + pulse * 20));
            int sg = Math.min(255, (int) (strokeColor.getGreen() * visualAlpha + pulse * 15));
            int sb = Math.min(255, (int) (strokeColor.getBlue() * visualAlpha + pulse * 25));
            
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(sr, sg, sb, (int)(strokeColor.getAlpha() * visualAlpha)));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 15, 15);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 15, 15);

            // Добавляем световые точки по углам при hover
            if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, (int)(50 + pulse * 30)));
                g2.fillOval(3, 3, 4, 4);
                g2.fillOval(w - 7, 3, 4, 4);
                g2.fillOval(3, h - 7, 4, 4);
                g2.fillOval(w - 7, h - 7, 4, 4);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
