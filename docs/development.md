# 개발 환경

- 소스·빌드: Windows → WSL2 → Ubuntu, JDK 21, Maven 3.9.x.
- IDE: Windows용 IntelliJ IDEA에서 WSL 폴더를 Maven 프로젝트로 열기.
- 외부 의존성: FlatLaf 3.7.2, SLF4J 2.0.18, Logback 1.5.38, JUnit Jupiter 6.0.3.
- 컴파일: Java release 21 / UTF-8.
- 저장 데이터·로그: 실행한 OS의 사용자 홈 `.lan-bricks` 아래.

## IntelliJ

1. `\\wsl.localhost\Ubuntu\home\mjm32\projects\lan-bricks` 폴더를 엽니다.
2. pom.xml을 Maven으로 가져오고 Project SDK·Maven Runner에 JDK 21을 지정합니다.
3. WSL 실행 대상이면 Ubuntu JDK와 `/usr/share/maven`을 사용합니다.
4. `LAN Bricks` 실행 설정 또는 Main.main을 실행합니다.

IDE 설정은 개인 PC별로 달라 `.idea`·`.iml`을 커밋하지 않습니다.
JDK는 `java -version`, Maven은 `mvn -version`으로 실제 실행 위치를 확인합니다.

## 검증

```sh
mvn clean verify
java -jar target/lan-bricks.jar --smoke-test
```

`--smoke-test`는 실제 Swing 창을 잠시 열고 닫습니다. GUI 없는 CI에서는 먼저 단위 테스트를 실행합니다.
기본 실행은 창을 유지합니다. 아직 게임 시작 버튼·LAN 서버·랭킹 기능은 없습니다.

## 배포 예정

Windows에서 JDK 21의 jpackage로 런타임 포함 실행 폴더·설치형 EXE를 생성할 계획입니다.
현 단계의 산출물은 의존성을 포함한 `target/lan-bricks.jar`입니다.
Ubuntu에서 빌드해도 Windows EXE 패키징은 Windows에서 수행합니다.
원격 dev push·GitHub Actions·릴리스 게시·방화벽 변경은 이번 환경 구성에 포함하지 않습니다.

## Ubuntu에서 IntelliJ 열기

```sh
cd ~/projects/lan-bricks
idea .
```

`~/.local/bin/idea`가 Ubuntu 경로를 Windows UNC 경로로 변환해 Windows IntelliJ를 엽니다.
새 터미널에서 바로 사용할 수 있습니다. 이미 열려 있던 터미널은 `source ~/.bashrc` 후 실행합니다.
개인 프로젝트 SDK 이름은 `homebrew-21 (WSL)`입니다.
