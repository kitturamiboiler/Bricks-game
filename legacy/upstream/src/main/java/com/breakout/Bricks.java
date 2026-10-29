package com.breakout;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Bricks { // 벽돌 만들기
    private double x;
    private double y;
    private double width;
    private double height;
    // 블록이 깨졌나 안 깨졌나 차이 alive = true -> 살아있음 false -> 블록 뒤짐
    private boolean alive = true;

    public Bricks(double x, double y, double width, double height) {
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

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public void draw(GraphicsContext gc) {
        if (!alive) // == if (alive == false) return;
            return;
        gc.setFill(Color.ORANGE); // 사각형 오렌지 색으로 칠하기
        gc.fillRect(x, y, width, height);
        // stroke를 사용해 테두리 칠하기
        gc.setStroke(Color.BLACK);
        gc.strokeRect(x, y, width, height);
    }
}
