# 기존 게임 코드 리팩토링 1단계

## 변경 범위

legacy/upstream의 원본은 수정하지 않았습니다.
기존 Ball·Paddle·Bricks와 BreakoutGame의 규칙을 domain.game으로 옮겼습니다.
이번 단계는 화면에서 독립한 싱글 보드 규칙입니다. Swing 게임 화면·키 입력·LAN은 아직 연결하지 않았습니다.

| 기존 위치 | 새 위치 | 변경 내용 |
| --- | --- | --- |
| Ball.draw / Paddle.draw / Bricks.draw | 이후 ui.swing.GamePanel | 도메인에서 JavaFX와 draw 제거 |
| Ball.update | Ball.move / reflectWalls | 초당 이동 속도, 위·양옆 경계 보정, 하단 반사 제거 |
| Ball.colliding | Ball.bounceOff | 원·사각형 거리 판정, 충돌 면에 맞는 축 반사 |
| Ball.reset / multiplySpeed | Ball.reset / increaseSpeed | 위치 초기화 시 양축 속도 유지, 단계별 1.1배 증가 |
| Paddle.moveLeft / moveRight | Paddle.move | 방향키 상태와 경과 시간, 양쪽 동시 입력 상쇄 |
| Bricks | Brick | 벽돌 한 개의 상태, 외부에서 파괴 상태 직접 변경 금지 |
| 초기 벽돌 생성 / resetBricks | BrickField.create | 보드 너비를 사용해 생성, 중복 생성 로직 통합 |
| BreakoutGame.handle | PlayerBoard.update / step | 점수·목숨·상태·단계 전환을 화면에서 분리 |

## 이번에 고친 오류

1. 마지막 목숨을 잃으면 GAME_OVER로 전환하고 이후 물리·점수·패들 갱신 중단.
2. 공 하단이 보드 하단에 닿으면 목숨을 한 번 차감하고, 남아 있으면 보드 중앙에서 재시작.
3. 벽돌 크기 대신 실제 보드 크기로 다음 단계 시작 좌표 계산.
4. 다음 단계에서도 가로·세로 속도 증가를 유지하고, 재배치할 때 위쪽 방향으로 출발.
5. 인접 벽돌 충돌이 반사를 두 번 뒤집지 않도록 물리 단계당 한 접촉만 해결.
6. 옆면은 가로 반사, 패들은 위에서 내려오는 공만 반사.
7. 30Hz·60Hz 갱신에서 같은 결과를 내도록 초당 120회 고정 물리 계산.

## 현재 규칙과 API

- 기본 논리 보드: 800×600. 허용 최소 크기: 320×600. 창 크기와 논리 보드 크기는 별개입니다.
- READY → start() → RUNNING → 마지막 목숨 소진 → GAME_OVER.
- restart()는 점수·목숨·단계·공·벽돌을 초기화하고 READY로 돌아갑니다.
- update(경과 초, 왼쪽 눌림, 오른쪽 눌림). 한 번에 0~1초를 허용합니다.
  화면 연결 시 정지·최소화 후 큰 시간 차이를 그대로 보내지 않고 새 갱신 시점을 잡아야 합니다.
- 기본 공 속도: 각 축 240px/s. 패들 속도: 420px/s.
- 벽돌 100점, 단계 완료 보너스 500점은 새 기획서를 적용했습니다. 원본은 벽돌 10점이었습니다.
- 첫 단계 6행, 이후 7행은 원본 구성을 유지합니다.
- 공 전체 속도는 900px/s를 상한으로 둡니다. 고정 단계의 충돌 검사가 무리한 이동 폭을 갖지 않도록 한 초기 조정값입니다.
- 이번 단계의 GAME_OVER는 상태 판정이며, 종료 문구를 그리는 UI는 다음 단계에서 연결합니다.
- 화면에는 읽기 전용 getter만 노출하고 이동·충돌·파괴는 domain.game 내부에서 수행합니다.

## 검증

원본 클래스를 임시 폴더에서 직접 호출해 좌표 오류·속도 초기화·목숨 음수·다중 반사를 재현했습니다.
새 BallTest·PlayerBoardTest는 해당 상황의 올바른 결과와 시간 분할·재시작·경계 처리를 검증합니다.
DomainBoundaryTest로 도메인에 Swing·JavaFX·외부 계층·I/O 의존성이 들어오지 않는지 검사합니다.

```sh
cd ~/projects/lan-bricks
mvn clean verify
```

다음 단계는 GameSessionService와 Swing GamePanel·InputController를 연결해 실제 싱글 한 판을 실행하는 작업입니다.
