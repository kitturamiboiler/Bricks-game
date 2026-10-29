# 환경 설정과 의존성 주입

## 지금 수정할 파일

| 경로 | 역할 |
| --- | --- |
| src/main/resources/game.properties | 기본 제목·창 크기·LAN 포트 |
| src/main/java/game/bricks/bootstrap/AppConfig.java | UTF-8 로딩·실행 옵션 병합·설정 검증 |
| src/main/java/game/bricks/bootstrap/Main.java | 객체를 생성하고 연결하는 조립 지점 |
| src/main/java/game/bricks/bootstrap/DesktopApplication.java | 설정·화면을 생성자로 주입받아 실행 |
| src/main/java/game/bricks/ui/swing/MainFrame.java | 전달받은 값으로 Swing 화면 구성 |
| src/test/java/game/bricks/bootstrap/AppConfigTest.java | 기본값·옵션 우선순위·잘못된 값 검증 |

## 실행 순서

Main → AppConfig.load() → 설정 검증 → FlatLaf 초기화 → Swing EDT →
MainFrame 생성 → DesktopApplication에 생성자 주입 → start().

의존성 주입은 필요한 객체를 클래스 내부에서 몰래 생성하지 않고 생성자로 받는 방식입니다.
Main이 조립을 담당하고 DesktopApplication이 주입받은 화면을 사용합니다.
화면은 bootstrap 설정 클래스를 참조하지 않고 제목·크기를 생성자로 전달받습니다.
현재는 Spring이나 DI 컨테이너 없이 명시적인 new와 생성자로 연결합니다.
서비스·저장소가 구현되면 같은 Main 조립 지점에서 구현체를 서비스 생성자에 전달합니다.
아직 없는 서비스·저장소 인터페이스의 빈 구현은 만들지 않습니다.

## 환경별 값

기본값은 game.properties에 두고, 개발 PC나 접속 시험 값은 JVM 옵션으로 변경합니다.
우선순위는 JVM의 -D 옵션 > game.properties > 데이터 경로의 사용자 홈 기본값입니다.

```sh
cd ~/projects/lan-bricks
mvn clean verify
java -Dnetwork.port=5001 -Dgame.title=벽돌깨기 -jar target/lan-bricks.jar
```

IntelliJ에서는 LAN Bricks 실행 설정의 VM options에 `-Dnetwork.port=5001`을 넣습니다.
프로그램 인수에는 `--smoke-test`를 넣으면 실제 창을 열고 자동으로 닫습니다.
포트는 설정만 읽으며 아직 서버를 열지 않습니다.
설정이 누락되거나 범위를 벗어나면 오류 로그와 종료 코드 1로 시작을 중단합니다.

`-Dapp.data-directory=/원하는/경로`는 향후 게임 기록 저장 위치입니다.
로그는 기존 logback.xml의 `${user.home}/.lan-bricks/logs`에 저장합니다.
데이터 경로 변경이 로그 경로까지 바꾸지는 않습니다.

## 이후 작업

다음은 domain.room.PlayerId, domain.game.Difficulty·Score의 값 검증과 테스트입니다.
그 다음 게임 규칙 → application 서비스 → 저장소·통신 구현 → 화면 연결 순으로 진행합니다.
legacy/upstream은 참고용이며 새 src에 복사하거나 직접 수정하지 않습니다.
