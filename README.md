# LAN Bricks

Java 21 · Swing · Maven 기반 벽돌깨기 프로젝트입니다. 싱글 플레이, LAN 대전, 3칸 아이템 인벤토리와 로컬 랭킹을 구현한 개발 버전입니다.
게임 화면은 영어를 사용합니다. 실제 두 PC 간 LAN 접속은 추가 확인이 필요합니다.

## 실행

```sh
cd /home/mjm32/projects/lan-bricks
mvn clean verify
java -jar target/lan-bricks.jar
```

IntelliJ에서 이 폴더의 pom.xml을 Maven 프로젝트로 열고 JDK 21을 선택합니다.
공유 실행 설정 `LAN Bricks` 또는 `game.bricks.bootstrap.Main`을 실행합니다.
WSL 프로젝트 경로는 `\\wsl.localhost\Ubuntu\home\mjm32\projects\lan-bricks`입니다.

## 구조·규칙

- [개발 환경](docs/development.md)
- [환경 설정·생성자 주입](docs/bootstrap.md)
- [DDD 구조와 구현 순서](docs/architecture.md)
- [원본 리팩토링과 현재 규칙](docs/refactoring.md)
- [커밋·브랜치 규칙](CONTRIBUTING.md)
- [기획서](docs/game-plan.pdf)
- 원본 코드: `legacy/upstream` (새 Maven 빌드에서 제외)

원본 저장소: https://github.com/kitturamiboiler/Bricks-game
원본 main과 커밋 이력은 유지하며 개발 작업은 dev에 통합하며, 이후 기능과 리팩토링은 별도 작업 브랜치에서 진행합니다.
