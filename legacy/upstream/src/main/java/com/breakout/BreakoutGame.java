package com.breakout;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.animation.AnimationTimer;
import javafx.scene.input.KeyCode;
import java.util.List;
import java.util.ArrayList;

public class BreakoutGame extends Application {
    private int score = 0; // score -> 0으로 초기화
    private int life = 3; // 생명은 3에서 스타트
    private int level = 1; // 레벨도 1에서 스타트

    @Override
    public void start(Stage stage) throws IOException {
        // 너비 800, 높이 600 설정
        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // 1. 공 생성
        Ball ball = new Ball(400, 300, 10);

        // 2. 레이아웃 설정
        Pane root = new Pane();
        root.getChildren().add(canvas);

        Scene scene = new Scene(root, 800, 600);

        // 3. 패들 생성
        Paddle paddle = new Paddle(350, 570, 100, 20);
        final boolean[] leftPressed = { false }; // 누르면 true 떼면 false
        final boolean[] rightPressed = { false }; // 누르면 true 떼면 false
        // 3-1. 키보드에 명령 -> pushDown
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.LEFT)
                leftPressed[0] = true;
            if (e.getCode() == KeyCode.RIGHT)
                rightPressed[0] = true;
        });
        // 3-1-2. pushUp
        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.LEFT)
                leftPressed[0] = false;
            if (e.getCode() == KeyCode.RIGHT)
                rightPressed[0] = false;
        });

        // 4. 벽돌 쌓기 -> 리스트로 받음
        List<Bricks> bricks = new ArrayList<>();
        bricks.clear();
        int rows = 6; // 줄 => 세로로 6개
        int cols = 10; // 칸 => 가로로 10개
        double width = 75; // 너비 => Rect width = 75;
        double height = 25; // 높이 => Rect height = 25;
        double gap = 5; // 간격 5
        for (int r = 0; r < rows; r++) { // 0 ~ 4까지 총 5줄 만들기
            for (int c = 0; c < cols; c++) { // 0 ~ 9번까지 10칸 만들기
                // 공의 radius = 10 -> 2R = 지름이 20임
                // width가 800/ height가 600
                // 벽돌의 배치를 고려 했을때 [20]==[760]==[20]으로 설치할 경우 양쪽 [20]의 gap에 공이 들어갈 확률이 있음
                // gap을 고로 5로 줄여 공이 그 간격에 못 들어가게 함
                // 2.5[■■■]2.5 이런식이 될듯 싶음
                // cols = 칸이니까 칸 x(너비 + 간격) +2.5를 함으로서 간격을 맞출수 있다고 생각함
                // rows = 세로의 열이니까 열 x (높이 + 간격)+ 높이또한
                // -------- (x,0)
                // | [25]
                // | ■■■■ 이렇게 되어있으니 총 50이될거라고 생각함
                // | [25]
                // -------- (x,600)
                double x = 2.5 + c * (width + gap);
                double y = 50 + r * (height + gap);
                bricks.add(new Bricks(x, y, width, height));
            }
        }

        // 5. 무한 루프 생성 [계산] -> [검사] -> [그리기] 구조
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 키보드 입력
                if (leftPressed[0])
                    paddle.moveLeft();
                if (rightPressed[0])
                    paddle.moveRight(canvas.getWidth());

                // 공을 움직입니다.
                ball.update(canvas.getWidth(), canvas.getHeight());
                // 공 + 패들 충돌
                if (ball.colliding(paddle)) {
                    // 위아래 속도(dy)를 -로 반전
                    // 👇 컴퓨터 그래픽스 좌표는 여기서부터 시작한다고 함
                    // -------------
                    // | ◀️(-)🔼(-)|
                    // |⬜️⬜️⬜️▶️(+)|
                    // | 🔽(+)⬜️⬜️ |
                    // -------------
                    ball.bounceY();
                    ball.setY(paddle.getY() - ball.getRadius());
                }
                // 벽돌 충돌
                for (Bricks b : bricks) {
                    if (b.isAlive() && ball.colliding(b)) {
                        ball.bounceY();
                        b.setAlive(false);
                        // 점수 주기
                        // 벽돌 1개 == 점수 10점
                        score += 10;
                    }
                }
                // 레벨 관련 다 깨지면 +1단계
                boolean allBroken = true;
                for (Bricks b : bricks) {
                    if (b.isAlive()) {
                        allBroken = false;
                        break;
                    }
                }
                // 전부 다 깨지면
                // 레벨 1up;
                // 속도 1.1배
                // 공위치 400,300으로 초기화
                if (allBroken) {
                    level++;
                    ball.multiplySpeed(1.1);
                    resetBricks(bricks);
                    // width = 800, height = 600;
                    // (width / 2) - 50 = 350 / (height / 2) -100 = 200
                    ball.reset((width / 2) - 50, (height / 2) - 100);
                }
                // 공이 600에 도달했을때 생명력 하나 삭감인데..
                // life가 -1되고 0보다 이상이면 ball 위치는
                // widht = 800 /2 = 400 -50 = 350으로 초기화
                // height = 600 / 2 = 300으로 초기화
                // 실행 결과 -> life -> 3 >> 2 >> 1 >> 0 >> -1 >> -2 >> -3 이렇게 가게 됨.
                // game over text 안 나타남.
                // 시작할때 생각보다 빠르게 생명력이 감소해 버림
                if (ball.getY() + ball.getRadius() > 600) {
                    life--;
                    if (life > 0) {
                        ball.reset(400, 300);
                    } else if (life == 0) {
                        gc.setFill(Color.RED);
                        gc.fillText("Game Over", (width / 2) - 50, height / 2);
                    }
                }
                // 화면 그리기
                gc.clearRect(0, 0, 800, 600); // 화면 지우기
                ball.draw(gc); // 공 그리기
                paddle.draw(gc); // 패들 그리기

                // 벽돌 그리기
                for (Bricks b : bricks) {
                    b.draw(gc);
                }
                // 점수판, 생명, 레벨 표시 -> 검정색
                gc.setFill(Color.BLACK);
                gc.fillText("Score: " + score, 20, 30);
                gc.fillText("Life: " + life, 100, 30);
                gc.fillText("Level: " + level, 700, 30);
            }
        };
        timer.start();
        // Title
        stage.setTitle("My First JavaFX Game");
        stage.setScene(scene);
        stage.show();
    }

    // resetBricks -> 레벨이 끝나고 나면 다시 블록을 채움
    private void resetBricks(List<Bricks> bricks) {
        bricks.clear();
        int rows = 7;
        int cols = 10;
        double width = 75;
        double height = 25;
        double gap = 5;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = 2.5 + c * (width + gap);
                double y = 50 + r * (height + gap);
                bricks.add(new Bricks(x, y, width, height));
            }
        }
    }

    // 실행
    public static void main(String[] args) {
        launch();
    }
}
