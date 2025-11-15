package com.lfs.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoadingSpinner extends JComponent {

    private static final int DOTS = 12;
    private static final int RADIUS = 15;
    private static final int DOT_RADIUS = 3;

    private int currentDot = 0;
    private Timer timer;

    public LoadingSpinner() {
        timer = new Timer(80, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentDot = (currentDot + 1) % DOTS;
                repaint();
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        timer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;

            for (int i = 0; i < DOTS; i++) {
                double angle = 2 * Math.PI * i / DOTS;
                int x = centerX + (int) (RADIUS * Math.cos(angle));
                int y = centerY + (int) (RADIUS * Math.sin(angle));

                // Calculate opacity
                int distance = (currentDot - i + DOTS) % DOTS;
                int alpha = 255 - (distance * (255 / DOTS));
                g2d.setColor(new Color(200, 200, 200, alpha));

                g2d.fillOval(x - DOT_RADIUS, y - DOT_RADIUS, 2 * DOT_RADIUS, 2 * DOT_RADIUS);
            }
        } finally {
            g2d.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(2 * (RADIUS + DOT_RADIUS), 2 * (RADIUS + DOT_RADIUS));
    }
}
