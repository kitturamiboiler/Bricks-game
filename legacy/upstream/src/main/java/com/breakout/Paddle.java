package com.breakout;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Paddle {
    private double x, y, width, height;

    public Paddle(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // getter
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    // 키보드 조작 로직 좌(left)
    public void moveLeft() {
        x -= 7;
        if (x < 0)
            x = 0;
    }

    // 우(right)
    public void moveRight(double canvasWidth) {
        x += 7;
        if (x + width > canvasWidth)
            x = canvasWidth - width;
    }

    // 사각형 패들 색칠
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        gc.fillRect(x, y, width, height);
    }
}