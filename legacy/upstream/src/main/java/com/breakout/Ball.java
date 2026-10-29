package com.breakout;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

//클래스 볼 생성
public class Ball {
    private double x;
    private double y;
    private double radius;
    // 기본 초기 공 속도 설정
    private double dx = 4;
    private double dy = 4;

    public Ball(double x, double y, double radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getRadius() {
        return radius;
    }

    // 1. 위아래 방향 반사 -방향이면 *-1 -> [+] // +면 *-1 -> [-]
    public void bounceY() {
        this.dy *= -1;
    }

    // 2. 패들 위에 공
    public void setY(double newY) {
        this.y = newY;
    }

    public void update(double width, double height) {
        x += dx;
        y += dy;
        // 벽에 튕기는 로직
        // x-radius < 0 왼쪽 벽 x +radius > width 오른쪽 벽
        // delta x의 값에 -1 곱 => 속도 방향 반대
        if (x - radius < 0 || x + radius > width)
            dx *= -1;
        if (y - radius < 0 || y + radius > height)
            dy *= -1;
    }

    public void draw(GraphicsContext gc) {
        gc.setFill(Color.RED);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    // 충돌 체크
    public boolean colliding(Paddle other) {
        return x - radius < other.getX() + other.getWidth() &&
                x + radius > other.getX() &&
                y - radius < other.getY() + other.getHeight() &&
                y + radius > other.getY();
    }

    public boolean colliding(Bricks other) {
        return x - radius < other.getX() + other.getWidth() &&
                x + radius > other.getX() &&
                y - radius < other.getY() + other.getHeight() &&
                y + radius > other.getY();
    }

    // 공이 죽고 부활함
    public void reset(double x, double y) {
        this.x = x;
        this.y = y;
        // 처음 공 기본 속도가 4였기에 -4로 지정
        this.dy = -4;
    }

    // 레벨당 속도 증가
    public void multiplySpeed(double factor) {
        this.dx *= factor;
        this.dy *= factor;
    }

}
