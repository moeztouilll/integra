package edu.connexion3a8.utils;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConfettiEngine {

    private static final Random random = new Random();
    private static final Color[] COLORS = {
            Color.web("#e6e6fa"), // Lavender Mist
            Color.web("#2a506b"), // Baltic Blue
            Color.web("#1d3649"), // Deep Baltic
            Color.web("#c07c4b"), // Faded Copper
            Color.web("#8b3a3a"), // Brown Red
            Color.web("#111111") // Black/Dark Gray
    };

    private static class Particle {
        double x, y;
        double w, h;
        Color color;
        double tilt;
        double tiltAngle;
        double tiltAngleInc;
        double vx, vy;

        Particle(double x, double y) {
            this.x = x;
            this.y = y;
            this.w = random.nextDouble() * 10 + 5;
            this.h = random.nextDouble() * 10 + 5;
            this.color = COLORS[random.nextInt(COLORS.length)];
            this.tilt = random.nextInt(10) - 10;
            this.tiltAngleInc = (random.nextDouble() * 0.07) + 0.05;
            this.tiltAngle = 0;

            // Initial burst velocity
            this.vx = (random.nextDouble() - 0.5) * 20; // horizontal scatter
            this.vy = -(random.nextDouble() * 10 + 10); // upwards burst
        }

        void update() {
            this.tiltAngle += this.tiltAngleInc;
            this.y += (Math.cos(this.tiltAngle) + this.vy + this.w / 2) / 2;
            this.x += Math.sin(this.tiltAngle) * 2 + this.vx;

            // Gravity effect
            if (this.vy < 15) {
                this.vy += 0.5;
            }

            // Air resistance on horizontal
            this.vx *= 0.95;

            this.tilt = Math.sin(this.tiltAngle) * 15;
        }

        void draw(GraphicsContext gc) {
            gc.save();
            gc.setFill(color);
            gc.translate(x + w / 2, y + h / 2);
            gc.rotate(tilt);
            gc.fillRect(-w / 2, -h / 2, w, h);
            gc.restore();
        }
    }

    /**
     * Spawns a confetti explosion centered at the given coordinates within the
     * provided Pane.
     */
    public static void fireConfetti(Pane rootPane, double centerX, double centerY) {
        double width = rootPane.getWidth();
        double height = rootPane.getHeight();
        if (width <= 0 || height <= 0)
            return;

        Canvas canvas = new Canvas(width, height);
        canvas.setMouseTransparent(true); // Let clicks pass through
        rootPane.getChildren().add(canvas);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        List<Particle> particles = new ArrayList<>();
        // Spawn 150 particles
        for (int i = 0; i < 150; i++) {
            particles.add(new Particle(centerX, centerY));
        }

        AnimationTimer timer = new AnimationTimer() {
            long startTime = System.nanoTime();

            @Override
            public void handle(long now) {
                // Clear canvas
                gc.clearRect(0, 0, width, height);

                boolean allOut = true;
                for (Particle p : particles) {
                    p.update();
                    p.draw(gc);
                    if (p.y < height + 20) {
                        allOut = false;
                    }
                }

                // Remove canvas after 4 seconds or when all particles fall off
                if (allOut || (now - startTime) > 4_000_000_000L) {
                    rootPane.getChildren().remove(canvas);
                    this.stop();
                }
            }
        };
        timer.start();
    }
}
