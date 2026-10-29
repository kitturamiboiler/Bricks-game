# 작업 규칙

- `main`: 검증된 운영·릴리스 기준.
- `dev`: 개발 통합 브랜치.
- 기능은 `feat/<name>`, 버그는 `fix/<name>` 작업 브랜치에서 개발 후 dev로 PR.
- dev 검증 후 main에 반영하고 버전 태그·EXE를 Releases에 게시.
- 커밋 형식: `type(scope): 설명`.
- 유형: feat, fix, refactor, chore, docs, test, build, ci.
- Java: 들여쓰기 4칸, 메서드 사이 한 줄, 입력·처리·출력 단계 사이 한 줄.
- 로그: SLF4J 사용. 규칙 객체는 화면이나 네트워크 구현을 직접 참조하지 않음.
- 기능이 없는 상태를 구현된 것처럼 표시하지 않으며 예외를 무시하지 않음.
