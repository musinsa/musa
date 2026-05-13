# MuSA PoC 결과 보고

| 항목 | 내용 |
|------|------|
| 기간 | 2026-04-27 ~ 2026-05-15 |
| 담당 | @이병우 (서비스플랫폼팀) |
| 플러그인 버전 | v0.0.2 |
| SonarQube | sonarqube-mobile.mng.musinsa.io (26.1.0) |

---

## 성공 기준 달성 현황

| 성공 기준 | 결과 | 비고 |
|-----------|------|------|
| 양 팀 레포 → SonarQube 대시보드 적재 | ✅ 달성 | ios-29cm, app-store-ios 모두 정상 |
| SwiftLint 룰 Issue 표출 | ✅ 달성 | ios-29cm: 16,898개 / app-store-ios: 5,388개 |
| 무신사 컨벤션 룰 Issue 표출 | ✅ 달성 | ios-29cm: 9,303개 / app-store-ios: 5,382개 |
| xccov 커버리지 적재 | ⚠️ 부분 달성 | 센서 코드 구현 완료, runner 환경(Linux)에서 xcodebuild 실행 불가 |
| LLM 코멘트 첨부 | ➖ 범위 제외 | PoC 핵심 기능 검증 후 추후 구현 |

---

## 구현 내용

### 플러그인 컴포넌트

| 컴포넌트 | 상태 | 설명 |
|----------|------|------|
| `SwiftLanguage` | ✅ | Swift 언어 등록 (언어 키: `swift`) |
| `SwiftRulesDefinition` | ✅ | SwiftLint 공식 룰 31개 등록 |
| `MusaConventionRules` | ✅ | 무신사 컨벤션 룰 5개 등록 |
| `SwiftLintSensor` | ✅ | SwiftLint JSON → SonarQube Issue 변환 |
| `XccovCoverageSensor` | ✅ | xccov XML → SonarQube Coverage 변환 (코드 완성) |
| `SwiftQualityProfile` | ✅ | Musinsa Swift 프로파일 (전체 룰 활성화) |

### 무신사 컨벤션 룰 (musa-convention 레포지토리)

| 룰 ID | 설명 |
|-------|------|
| `view_controller_suffix` | UIViewController 서브클래스 이름은 ViewController로 끝나야 함 |
| `mark_section` | MARK 주석은 `// MARK: -` 형식 사용 |
| `no_force_unwrap_iboutlet` | @IBOutlet에 강제 언래핑(!) 금지 |
| `reactor_import_order` | ReactorKit은 RxSwift보다 먼저 import |
| `reactor_action_naming` | Reactor Action 명명 규칙 |

### 주요 해결 이슈

| 문제 | 원인 | 해결 |
|------|------|------|
| SwiftLint 이슈 0개 import | Docker 컨테이너 `/work/` 경로 vs runner 절대경로 불일치 | `SwiftLintSensor`에 경로 정규화 추가 |
| Unmapped rule 경고 | SwiftRulesDefinition에 미등록 룰 | 10개 룰 추가 등록 |
| 커스텀 룰 이슈 0개 | SwiftLint `.swiftlint.yml`에 custom_rules 미정의 | 양 팀 레포에 custom_rules 추가 |

---

## SonarQube 대시보드

- **ios-29cm**: https://sonarqube-mobile.mng.musinsa.io/project/issues?id=ios-29cm&branch=feature%2FMCMP-3845-musa-plugin-poc&issueStatuses=OPEN%2CCONFIRMED
- **app-store-ios**: https://sonarqube-mobile.mng.musinsa.io/project/issues?id=app-store-ios&branch=feature%2FMCMP-3845-musa-plugin-poc&issueStatuses=OPEN%2CCONFIRMED

---

## 미완료 및 후속 과제

| 과제 | 우선순위 | 비고 |
|------|---------|------|
| xccov 실제 커버리지 연동 | 중 | macOS runner 확보 필요 |
| LLM 코멘트 데코레이터 | 낮 | 사내 LLM API 확보 후 구현 |
| PR Decoration 검증 | 높 | PR 생성 후 인라인 코멘트 동작 확인 필요 |
| Helm Chart 배포 자동화 | 중 | 현재 kubectl cp 수동 배포 → values.yaml 주입 방식으로 전환 |
| ncloc 메트릭 | 낮 | Swift 파서 구현 필요 (SonarQube 기본 미지원) |

---

## 결론

SonarQube Developer Edition($7,500/년) 없이 커스텀 플러그인으로 Swift 정적분석 환경 구축 가능함을 검증했습니다.

- SwiftLint 룰 + 무신사 자체 컨벤션 룰이 SonarQube Issue로 정상 표출
- 기존 SonarQube 인프라(대시보드/Quality Gate/개발자 포털) 100% 재사용
- 커버리지 센서 코드 완성, macOS runner 확보 시 즉시 활성화 가능

**권고: PoC 성공 → 프로덕션 적용 진행**
