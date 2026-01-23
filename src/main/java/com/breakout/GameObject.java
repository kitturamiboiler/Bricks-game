package com.breakout;

public abstract class GameObject implements Paintable { // interface 상속
    private double x;
    private double y;
    private double width;
    private double height;

    public GameObject(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean instersects(GameObject other) {
        // 컴퓨터 그래픽 좌표계는 ------- 천장에 가까우면 - / 멀면 +
        // | -> x축에 닿는 이 부분도 가까우면 - / 멀면 + 이다.
        return x < other.x + other.width && x + width > other.x && y < other.y + other.height && y + height > other.y;
    }
}
