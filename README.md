# MuSA — Musinsa Swift Analyzer

**Swift 코드 분석을 위한 SonarQube 플러그인** | SwiftLint + 코드 커버리지 + LLM 기반 코멘트 통합

## 개요

MuSA는 SonarQube Community Edition에 Swift 언어 지원을 추가하는 커스텀 SonarQube 플러그인입니다. SonarQube CE 티어에서 Swift를 지원하지 않는 문제(Developer Edition은 연간 $7,500)를 해결하기 위해 다음을 통합합니다:

- **SwiftLint** 정적 분석 규칙
- **무신사 커스텀 컨벤션** (추가 규칙 5개)
- **xccov 코드 커버리지** 메트릭
- **LLM 기반 이슈 코멘트** (개발자를 위한 맥락 피드백)

이 플러그인은 SonarQube Plugin API 표준을 따르며, 기존 SonarQube 인프라(대시보드, Quality Gates, PR 데코레이션)와 원활하게 통합됩니다.

## 문제 배경 및 동기

- **SonarQube Community Edition**은 Swift를 지원하지 않음
- **Developer Edition** (연간 $7,500)은 비용이 크고 과도함
- **오픈소스 대안** (`sonar-apple`)은 유지보수가 중단됨 (마지막 릴리즈: SonarQube 9.9용 v0.5.1)
- **기존 도구** (SwiftLint + Danger + xccov)는 SonarQube/개발자 포털에 통합되지 않음
- **목표:** iOS 팀을 위한 통합 정적 분석 대시보드 + Quality Gates + PR 데코레이션

## 아키텍처

### 핵심 구성 요소

```
[iOS 저장소]
    ↓
[GitHub Actions]
  ├─ SwiftLint (→ swiftlint.json)
  ├─ xccov (→ coverage.xml)
  └─ sonar-scanner (→ SonarQube)
    ↓
[SonarQube 26.1]
  ├─ MuSA 플러그인 (jar)
  │  ├─ SwiftLanguage — "Swift" 언어 등록
  │  ├─ SwiftRulesDefinition — SwiftLint 규칙 12개
  │  ├─ MusaConventionRules — 무신사 커스텀 규칙 5개
  │  ├─ SwiftQualityProfile — 기본 규칙 세트 (총 17개)
  │  ├─ SwiftLintSensor — swiftlint.json 파싱 → 이슈
  │  ├─ XccovCoverageSensor — coverage.xml 파싱 → 라인 커버리지
  │  └─ LLMCommentDecorator — AI 기반 이슈 코멘트
  ├─ 대시보드
  ├─ Quality Gates
  └─ PR 데코레이션 (GitHub)
```

### 기술 스택

| 구성 요소 | 버전 | 용도 |
|-----------|------|------|
| Java | 11+ | 플러그인 언어 |
| Maven | 3.6+ | 빌드 시스템 |
| SonarQube Plugin API | 9.14.0.375 | SonarQube 핵심 통합 |
| Jackson | 2.16.0 | JSON 파싱 (SwiftLint 리포트) |
| JUnit 5 | 5.10.0 | 단위 테스트 |
| SLF4J | 1.7.36 | 로깅 |

## 설치

### 1. 플러그인 빌드

```bash
cd /Users/musinsa/www/musa
mvn clean package -DskipTests
# 출력: target/sonar-musa-plugin-1.0.0.jar
```

### 2. SonarQube에 배포

```bash
# SonarQube extensions 디렉토리에 복사
cp target/sonar-musa-plugin-1.0.0.jar $SONARQUBE_HOME/extensions/plugins/

# SonarQube 재시작
docker restart sonarqube  # 또는 사용 중인 배포 방식
```

### 3. 설치 확인

1. SonarQube 관리자 패널에 로그인
2. **Administration** → **Languages** 이동
3. 언어 목록에 **"Swift"** 가 표시되는지 확인
4. **Quality Profiles** 이동
5. **"Musinsa Swift"** 프로파일이 17개 규칙과 함께 존재하는지 확인

## 사용법

### GitHub Actions 워크플로우 (샘플)

```yaml
name: SonarQube Analysis
on: [push, pull_request]

jobs:
  analyze:
    runs-on: macos-latest
    steps:
      # 1. 코드 체크아웃
      - uses: actions/checkout@v3

      # 2. SwiftLint 실행
      - name: SwiftLint
        run: |
          brew install swiftlint
          swiftlint lint --reporter json > swiftlint.json || true

      # 3. 테스트 + 커버리지 실행
      - name: Build & Test
        run: |
          xcodebuild test \
            -scheme YourApp \
            -configuration Debug \
            -derivedDataPath build \
            -enableCodeCoverage YES
          
          # xccov 커버리지 추출
          xcrun xccov view build/Logs/Test/*.xcresult > coverage.xml

      # 4. SonarQube 업로드
      - name: SonarQube Scan
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: |
          brew install sonar-scanner
          sonar-scanner \
            -Dsonar.projectKey=ios-musinsa \
            -Dsonar.sources=. \
            -Dsonar.host.url=https://sonarqube.musinsa.io \
            -Dsonar.login=$SONAR_TOKEN \
            -Dsonar.swift.swiftlint.reportPath=swiftlint.json \
            -Dsonar.swift.coverage.reportPath=coverage.xml
```

### 설정 프로퍼티

| 프로퍼티 | 필수 | 예시 | 설명 |
|----------|------|------|------|
| `sonar.sources` | ✓ | `.` | 소스 코드 디렉토리 |
| `sonar.swift.swiftlint.reportPath` | ✗ | `swiftlint.json` | SwiftLint JSON 리포트 |
| `sonar.swift.coverage.reportPath` | ✗ | `coverage.xml` | xccov 커버리지 XML |
| `sonar.swift.llm.enabled` | ✗ | `true` | LLM 이슈 코멘트 활성화 |
| `sonar.swift.llm.endpoint` | ✗ | `https://api.openai.com` | LLM API 엔드포인트 |
| `sonar.swift.llm.apiKey` | ✗ | `sk-...` | LLM API 키 |

## 프로젝트 구조

```
musa/
├── pom.xml                          # Maven 빌드 설정
├── README.md                        # 이 파일
├── docs/
│   └── musa-hld.md                 # 고수준 설계 문서
├── src/
│   ├── main/
│   │   ├── java/com/musinsa/sonar/musa/
│   │   │   ├── MuSAPlugin.java              # 플러그인 진입점
│   │   │   ├── languages/
│   │   │   │   └── SwiftLanguage.java       # Swift 언어 등록
│   │   │   ├── rules/
│   │   │   │   ├── SwiftRulesDefinition.java    # SwiftLint 규칙 12개
│   │   │   │   ├── MusaConventionRules.java     # 커스텀 규칙 5개
│   │   │   │   └── SwiftQualityProfile.java     # 기본 프로파일
│   │   │   └── sensors/
│   │   │       ├── SwiftLintSensor.java         # SwiftLint JSON 파싱
│   │   │       └── XccovCoverageSensor.java     # 커버리지 XML 파싱
│   │   └── resources/
│   │       └── com/musinsa/sonar/musa/rules/
│   │           ├── force_try.json
│   │           ├── weak_delegate.json
│   │           ├── trailing_comma.json
│   │           └── ... (나머지 11개 JSON 파일)
│   └── test/
│       └── java/com/musinsa/sonar/musa/
│           ├── SwiftLanguageTest.java
│           ├── SwiftRulesDefinitionTest.java
│           └── SwiftLintSensorTest.java
└── target/
    └── sonar-musa-plugin-1.0.0.jar      # 컴파일된 플러그인
```

## 규칙 및 프로파일

### SwiftLint 규칙 (총 12개)

표준 SwiftLint에서 심각도 매핑과 함께 통합:

- `force_try` (BUG) — force try `try!` 사용 금지
- `weak_delegate` (BUG) — 약한 참조 델리게이트 사용
- `trailing_comma` (CODE_SMELL) — 일관된 후행 쉼표 사용
- ... 외 9개 (`SwiftRulesDefinition.java` 참고)

### 무신사 컨벤션 규칙 (총 5개)

무신사 iOS 팀이 적용하는 커스텀 규칙:

- `musa_naming_convention` (CODE_SMELL)
- `musa_error_handling` (BUG)
- `musa_testing_coverage_minimum` (CODE_SMELL)
- `musa_documentation_required` (CODE_SMELL)
- `musa_dependency_security` (SECURITY_HOTSPOT)

### 기본 Quality 프로파일

**"Musinsa Swift"** — 전체 17개 규칙 활성화 (SwiftLint 12개 + 커스텀 5개)

## 테스트

### 단위 테스트 실행

```bash
mvn test
```

테스트 범위:
- `SwiftLanguageTest` — 언어 등록, 파일 확장자
- `SwiftRulesDefinitionTest` — 규칙 수, 심각도, 유형
- `SwiftLintSensorTest` — JSON 파싱, 이슈 생성
- `XccovCoverageSensorTest` — XML 파싱, 커버리지 매핑

## LLM 코멘트 데코레이터 (예정)

플러그인은 개발자에게 맥락 피드백을 제공하는 LLM 기반 이슈 코멘트를 선택적으로 지원합니다:

```
이슈: 42번 줄의 force_try
자동 코멘트 (Claude): "여기서 force try가 발견되었습니다. 프로덕션 코드의 안전한 에러 처리를 위해
  do-catch 또는 guard let 사용을 고려하세요. 참고: https://docs.swift.org/..."
```

**현황:** Phase 2 예정. 다음이 필요합니다:
- `sonar.swift.llm.enabled=true`
- `sonar.swift.llm.endpoint=<your-llm-service>`
- `sonar.swift.llm.apiKey=<credentials>`

## 배포 (SonarQube Kubernetes)

### Helm 차트 통합

SonarQube Helm values에 플러그인 jar 추가:

```yaml
sonarqube:
  plugins:
    install:
      - sonar-musa-plugin-1.0.0.jar
```

또는 init 컨테이너를 통한 마운트:

```yaml
sonarqube:
  initContainers:
    - name: plugin-loader
      image: alpine
      command:
        - sh
        - -c
        - |
          wget https://github.com/musinsa/musa/releases/download/v1.0.0/sonar-musa-plugin-1.0.0.jar
          cp sonar-musa-plugin-1.0.0.jar /opt/sonarqube/extensions/plugins/
      volumeMounts:
        - name: sonarqube-plugins
          mountPath: /opt/sonarqube/extensions/plugins
```

## 문제 해결

### SonarQube에서 Swift 언어가 표시되지 않는 경우

1. 플러그인 jar가 `$SONARQUBE_HOME/extensions/plugins/` 에 있는지 확인
2. SonarQube 로그 확인: `tail -f $SONARQUBE_HOME/logs/sonar.log`
3. `SwiftLanguage` 등록 메시지 검색
4. SonarQube 재시작: `docker restart sonarqube`

### 규칙이 로드되지 않는 경우

1. `src/main/resources/com/musinsa/sonar/musa/rules/` 에 JSON 파일이 존재하는지 확인
2. `SwiftRulesDefinition.java` 에서 `setHtmlDescription(loadResource(...))` 호출 여부 확인
3. 규칙 저장소 이름이 코드의 `"swiftlint"` 와 일치하는지 확인

### 커버리지가 가져와지지 않는 경우

1. `sonar.swift.coverage.reportPath` 가 유효한 `coverage.xml` 을 가리키는지 확인
2. 로컬 머신에서 실행: `xccov view build/Logs/Test/*.xcresult > coverage.xml`
3. XML 형식이 `XccovCoverageSensor` 파서 기대값과 일치하는지 확인

## 기여

### 로컬 빌드 및 테스트

```bash
mvn clean package -DskipTests
# 수동 테스트를 위해 로컬 SonarQube에 배포
```

### 새 규칙 추가

1. `src/main/resources/com/musinsa/sonar/musa/rules/<rule_key>.json` 에 JSON 규칙 정의 생성
2. `SwiftRulesDefinition.java` 또는 `MusaConventionRules.java` 에 등록
3. `src/test/` 에 단위 테스트 추가
4. 이 README 업데이트

## 성능 및 제한

- **최대 파일 크기:** Swift 파일당 1MB (표준)
- **실행당 최대 이슈 수:** SonarQube 서버 메모리에 따라 제한
- **분석 시간:** 일반적인 iOS 프로젝트 + SwiftLint + 커버리지 약 5~10분
- **커버리지 오버헤드:** 전체 테스트 스위트 필요로 약 2배 느림

## 일정

- **W1 (2026-04-27 ~ 05-03):** 핵심 플러그인 개발 ✅
- **W2 (2026-05-04 ~ 05-10):** LLM 통합 + CI/CD 샘플
- **W3 (2026-05-11 ~ 05-15):** 검증 + PoC 보고서

## 라이선스

무신사 내부 사용 전용 (독점)

## 연락처 및 지원

- **작성자:** @이병우 (byungwoo.lee@musinsa.com)
- **팀:** 서비스플랫폼팀
- **저장소:** https://github.com/musinsa/musa

이슈, 개선 제안, 문의사항은 이 저장소에 이슈를 생성하거나 팀에 연락하세요.
