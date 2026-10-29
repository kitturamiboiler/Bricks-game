# 구조와 개발 범위

기획서의 DDD 구조를 단일 Maven 프로젝트로 구성합니다.

| 패키지 | 책임 | 구현 예정 클래스 |
| --- | --- | --- |
| domain.game | 경기·보드·점수·승패 | Match, PlayerBoard, Ball, Paddle, BrickField, Score, Difficulty, MatchResult |
| domain.item | 3칸 FIFO 인벤토리·효과 | Inventory, ItemType, ActiveEffect |
| domain.room | ID·준비·방 정원 | Room, PlayerId, ReadyState |
| domain.ranking | 기록과 저장소 계약 | RankingEntry, RankingPolicy, RankingRepository |
| application | 기능 흐름 조정 | GameSessionService, RoomService, UseItemService, RankingService |
| application.port | 외부 통신 계약 | GameConnection |
| application.dto | 입력·화면 데이터 | InputCommand, GameSnapshot |
| infrastructure.network | TCP·패킷 처리 | TcpHost, TcpClient, PacketCodec, NetworkMessage |
| infrastructure.persistence | 로컬 기록 저장 | FileRankingRepository |
| ui.swing | Swing 화면·입력 | MainFrame, MenuPanel, LobbyPanel, GamePanel, InventoryPanel, RankingPanel, InputController |
| bootstrap | 환경·조립·진입점 | Main, AppConfig |

domain은 Swing·파일·Socket과 바깥 계층을 참조하지 않습니다.
UI → application → domain으로 규칙을 호출하며 infrastructure는 인터페이스를 구현합니다.
bootstrap이 구현체를 조립합니다. MainFrame은 시작 시 제목과 크기 값만 전달받습니다.
아직 구현하지 않은 클래스는 빈 껍데기 대신 package-info.java와 이 문서로 위치·역할을 남깁니다.

## 구현 순서

1. PlayerId, Difficulty, Score와 단일 보드 게임 규칙.
2. 싱글 플레이, 방향키 이동과 Swing 렌더링.
3. Room과 TCP 연결, P1/P2 독립 보드·개별 점수.
4. SPACE 사용, 최대 3칸 FIFO 아이템과 효과 시간.
5. 난이도별 싱글 랭킹·LAN 대전 결과 저장.
6. Windows jpackage EXE와 GitHub Releases 배포.

## 원본 코드 확인 사항

원본은 JavaFX GraphicsContext에 모델의 draw 메서드가 의존하고,
BreakoutGame.start에 입력·계산·점수·그리기가 집중되어 있습니다.
하단 경계 반사와 목숨 감소가 같은 프레임에 처리되고, 게임 종료 후에도 타이머가 계속되며,
레벨 리셋에 벽돌 크기 변수가 화면 크기처럼 사용되는 부분이 있어 검증 후 재작성합니다.
원본을 새 구조에 그대로 복사하지 않으며, 공·패들·벽돌의 규칙을 분리해 테스트하며 옮깁니다.

## 도메인 분리 1단계 완료

Ball·Paddle·Brick·BrickField·PlayerBoard·GameState를 구현했습니다.
규칙 이전과 버그 수정 내역은 [리팩토링 문서](refactoring.md)에 기록했습니다.
