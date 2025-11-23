package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;

public class UIEffects {
    // Modern color palette with vibrant colors
    public static final Color PRIMARY = new Color(64, 196, 255); // Vibrant blue
    public static final Color SECONDARY = new Color(255, 82, 82); // Vibrant red
    public static final Color ACCENT = new Color(255, 193, 7); // Vibrant yellow
    public static final Color BACKGROUND = new Color(18, 18, 18); // Dark background
    public static final Color SURFACE = new Color(30, 30, 30); // Dark surface
    public static final Color ON_PRIMARY = new Color(255, 255, 255); // White text
    public static final Color ON_SURFACE = new Color(255, 255, 255); // White text on dark

    // Gradients
    public static final GradientPaint PRIMARY_GRADIENT = new GradientPaint(
            0, 0, new Color(25, 118, 210),
            0, 100, new Color(21, 101, 192));
    public static final GradientPaint SUCCESS_GRADIENT = new GradientPaint(
            0, 0, new Color(76, 175, 80),
            0, 100, new Color(56, 142, 60));
    public static final GradientPaint DANGER_GRADIENT = new GradientPaint(
            0, 0, new Color(211, 47, 47),
            0, 100, new Color(198, 40, 40));

    // Add modern hover effect with smooth scale animation
    public static void addHoverEffect(JButton button) {
        button.addMouseListener(new MouseAdapter() {
            private Timer scaleTimer;
            private float scale = 1.0f;
            private boolean isHovered = false;

            {
                scaleTimer = new Timer(20, e -> {
                    if (isHovered && scale < 1.05f) {
                        scale = Math.min(1.05f, scale + 0.01f);
                        updateButtonScale();
                    } else if (!isHovered && scale > 1.0f) {
                        scale = Math.max(1.0f, scale - 0.01f);
                        updateButtonScale();
                    } else {
                        scaleTimer.stop();
                    }
                });
            }

            private void updateButtonScale() {
                int w = button.getWidth();
                int h = button.getHeight();
                int newW = (int) (w * scale);
                int newH = (int) (h * scale);
                int x = button.getX() - (newW - w) / 2;
                int y = button.getY() - (newH - h) / 2;
                button.setBounds(x, y, newW, newH);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                button.setBackground(button.getBackground().brighter());
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
                scaleTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                button.setBackground(button.getBackground().darker());
                scaleTimer.start();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                scale = 0.95f;
                updateButtonScale();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                scale = isHovered ? 1.05f : 1.0f;
                updateButtonScale();
            }
        });
    }

    // Create modern gradient button with elevation effect
    public static JButton createGradientButton(String text, Color startColor, Color endColor) {
        JButton button = new JButton(text) {
            private boolean isHovered = false;
            private float pulsePhase = 0f;
            private Timer pulseTimer;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        if (pulseTimer == null) {
                            pulseTimer = new Timer(50, evt -> {
                                pulsePhase += 0.1f;
                                if (pulsePhase > 2 * Math.PI)
                                    pulsePhase -= 2 * Math.PI;
                                repaint();
                            });
                        }
                        pulseTimer.start();
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        if (pulseTimer != null)
                            pulseTimer.stop();
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Neon glow effect
                if (isHovered) {
                    float glowIntensity = (float) (Math.sin(pulsePhase) + 1) / 2;
                    Color glowColor = new Color(
                            startColor.getRed(),
                            startColor.getGreen(),
                            startColor.getBlue(),
                            (int) (80 * glowIntensity));

                    for (int i = 0; i < 5; i++) {
                        g2d.setColor(glowColor);
                        g2d.fillRoundRect(-i, -i,
                                getWidth() + (i * 2), getHeight() + (i * 2), 20, 20);
                    }
                }

                // Modern gradient background with shine
                GradientPaint gradient = new GradientPaint(
                        0, 0,
                        isHovered ? startColor.brighter().brighter() : startColor,
                        getWidth(), getHeight(),
                        isHovered ? endColor.brighter().brighter() : endColor);
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

                // Add glass-like highlight
                GradientPaint highlightGradient = new GradientPaint(
                        0, 0,
                        new Color(255, 255, 255, isHovered ? 100 : 50),
                        0, getHeight() / 2,
                        new Color(255, 255, 255, 0));
                g2d.setPaint(highlightGradient);
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() / 2, 15, 15);

                // Draw text with shadow
                g2d.setFont(getFont());
                FontMetrics metrics = g2d.getFontMetrics();
                int x = (getWidth() - metrics.stringWidth(getText())) / 2;
                int y = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();

                // Text shadow
                g2d.setColor(new Color(0, 0, 0, 50));
                g2d.drawString(getText(), x + 1, y + 1);

                // Actual text
                g2d.setColor(Color.WHITE);
                g2d.drawString(getText(), x, y);

                g2d.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(200, 45);
            }
        };

        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    // Create modern panel with glass morphism effect and neon glow
    public static JPanel createGradientPanel(Color startColor, Color endColor, boolean withShadow) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Create neon glow effect
                if (withShadow) {
                    Color glowColor = new Color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), 40);
                    for (int i = 0; i < 5; i++) {
                        g2d.setColor(glowColor);
                        g2d.fillRoundRect(i, i, getWidth() - (i * 2), getHeight() - (i * 2), 25, 25);
                    }
                }

                // Modern glassmorphism background
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(
                                startColor.getRed(),
                                startColor.getGreen(),
                                startColor.getBlue(),
                                200),
                        getWidth(), getHeight(), new Color(
                                endColor.getRed(),
                                endColor.getGreen(),
                                endColor.getBlue(),
                                200));
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);

                // Glass effect - top highlight
                GradientPaint highlightGradient = new GradientPaint(
                        0, 0,
                        new Color(255, 255, 255, 100),
                        0, getHeight() / 2,
                        new Color(255, 255, 255, 0));
                g2d.setPaint(highlightGradient);
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() / 2, 20, 20);

                // Subtle border
                g2d.setColor(new Color(255, 255, 255, 50));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);

                g2d.dispose();
            }
        };
    }

    // Create modern animated border with gradient
    public static Border createAnimatedBorder() {
        return new Border() {
            private Timer timer;
            private float offset = 0f;

            {
                timer = new Timer(50, e -> {
                    offset = (offset + 0.05f) % 1.0f;
                    if (SwingUtilities.getRoot((Component) e.getSource()) != null) {
                        SwingUtilities.getRoot((Component) e.getSource()).repaint();
                    }
                });
                timer.start();
            }

            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Create gradient with moving offset
                float[] fractions = { 0f, 0.5f, 1f };
                Color[] colors = { PRIMARY, PRIMARY.brighter(), PRIMARY };
                LinearGradientPaint gradientPaint = new LinearGradientPaint(
                        x, y, width + x, height + y,
                        fractions,
                        colors,
                        MultipleGradientPaint.CycleMethod.REPEAT);
                g2d.setPaint(gradientPaint);
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(x + 1, y + 1, width - 3, height - 3, 10, 10);

                g2d.dispose();
            }

            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(5, 10, 5, 10);
            }

            @Override
            public boolean isBorderOpaque() {
                return false;
            }
        };
    }

    // Tạo hiệu ứng shine cho button khi win
    public static void addWinningEffect(JButton button) {
        Timer timer = new Timer(50, new ActionListener() {
            float alpha = 0f;
            boolean increasing = true;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (increasing) {
                    alpha += 0.1f;
                    if (alpha >= 1f) {
                        alpha = 1f;
                        increasing = false;
                    }
                } else {
                    alpha -= 0.1f;
                    if (alpha <= 0f) {
                        alpha = 0f;
                        increasing = true;
                    }
                }
                button.setBackground(new Color(1f, 1f, 0f, alpha));
                button.repaint();
            }
        });
        timer.start();
    }

    // Create modern text field with floating effect
    public static JTextField createStyledTextField() {
        JTextField textField = new JTextField() {
            private Color borderColor = new Color(200, 200, 200);
            private float shadowOpacity = 0.0f;
            private Timer fadeTimer;

            {
                setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
                setPreferredSize(new Dimension(250, 40));
                setFont(new Font("Segoe UI", Font.PLAIN, 14));
                setForeground(ON_SURFACE);

                fadeTimer = new Timer(20, e -> {
                    if (hasFocus()) {
                        shadowOpacity = Math.min(1.0f, shadowOpacity + 0.1f);
                    } else {
                        shadowOpacity = Math.max(0.0f, shadowOpacity - 0.1f);
                    }
                    if ((hasFocus() && shadowOpacity == 1.0f) || (!hasFocus() && shadowOpacity == 0.0f)) {
                        ((Timer) e.getSource()).stop();
                    }
                    repaint();
                });

                addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        borderColor = PRIMARY;
                        fadeTimer.start();
                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                        borderColor = new Color(200, 200, 200);
                        fadeTimer.start();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Draw shadow
                if (shadowOpacity > 0) {
                    g2d.setColor(new Color(0, 0, 0, (int) (20 * shadowOpacity)));
                    g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);
                }

                // Draw background
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                // Draw border
                g2d.setColor(borderColor);
                g2d.setStroke(new BasicStroke(hasFocus() ? 2f : 1f));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);

                g2d.dispose();
                super.paintComponent(g);
            }
        };

        return textField;
    }

    // Create modern password field with floating effect
    public static JPasswordField createStyledPasswordField() {
        JPasswordField passwordField = new JPasswordField() {
            private Color borderColor = new Color(200, 200, 200);
            private float shadowOpacity = 0.0f;
            private Timer fadeTimer;

            {
                setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
                setPreferredSize(new Dimension(250, 40));
                setFont(new Font("Segoe UI", Font.PLAIN, 14));
                setForeground(ON_SURFACE);

                fadeTimer = new Timer(20, e -> {
                    if (hasFocus()) {
                        shadowOpacity = Math.min(1.0f, shadowOpacity + 0.1f);
                    } else {
                        shadowOpacity = Math.max(0.0f, shadowOpacity - 0.1f);
                    }
                    if ((hasFocus() && shadowOpacity == 1.0f) || (!hasFocus() && shadowOpacity == 0.0f)) {
                        ((Timer) e.getSource()).stop();
                    }
                    repaint();
                });

                addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        borderColor = PRIMARY;
                        fadeTimer.start();
                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                        borderColor = new Color(200, 200, 200);
                        fadeTimer.start();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Draw shadow
                if (shadowOpacity > 0) {
                    g2d.setColor(new Color(0, 0, 0, (int) (20 * shadowOpacity)));
                    g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);
                }

                // Draw background
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                // Draw border
                g2d.setColor(borderColor);
                g2d.setStroke(new BasicStroke(hasFocus() ? 2f : 1f));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);

                g2d.dispose();
                super.paintComponent(g);
            }
        };

        return passwordField;
    }
}