# [iOS/Swift] MuSA — SonarQube 대체 정적분석 HLD & PoC

| 항목 | 내용 |
|------|------|
| Author | @이병우 (서비스플랫폼팀) |
| Review Date | 2026-04-30 |
| Reviewers | TBU |
| Status | Proposed (PoC 진행: 2026-04-27 ~ 2026-05-15) |

[TOC]

---

## 용어

| 용어 | 설명 |
|------|------|
| **MuSA** | **Mu**sinsa **S**wift **A**nalyzer — 본 설계의 가제. "무사히 Quality Gate 통과" |
| **SonarQube CE** | SonarQube Community Edition (LGPL-3.0, 무료) |
| **SonarQube DE** | SonarQube Developer Edition (상용 라이선스, $7,500/년) |
| **Plugin API** | `org.sonarsource.api.plugin:sonar-plugin-api` — SonarQube 플러그인 개발용 공식 API |
| **Sensor** | 분석 결과를 SonarQube에 적재하는 SonarQube Plugin API 컴포넌트 |
| **Quality Profile** | 언어별 활성화된 룰 집합 |
| **Quality Gate** | 프로젝트 통과 조건 (커버리지/이슈 수 등) |
| **PR Decoration** | SonarQube 분석 결과를 GitHub PR에 인라인/요약 코멘트로 노출하는 기능 |
| **xccov** | Xcode 코드 커버리지 결과 출력 도구 |
| **SwiftLint** | Swift 룰베이스 정적분석 도구 (오픈소스) |
| **xcodebuild + xcpretty** | iOS 빌드/테스트 실행 + 결과 포맷터 |

## 문제 정의

### 배경
- 무신사/29CM iOS는 **현재 SonarQube 기반 정적분석이 적용되지 않은 상태**
- **SonarQube Community Edition은 Swift 언어 플러그인을 공식 제공하지 않음** — Swift 분석은 Developer Edition 이상에서만 지원
- SonarQube **Developer Edition 구매 검토** 중이나 **연 $7,500 (약 1천만원)** 비용 부담
- **OSS 대안(`insideapp-oss/sonar-apple` 등)은 유지보수가 사실상 중단된 상태** — 최신 v0.5.1이 SonarQube 9.9 (Plugin API 9.14) 기준이며, 현재 운영 중인 26.1과 메이저 버전 갭 존재
- 양 모바일팀은 자체적으로 SwiftLint + Danger + xccov 기반 분석을 운영 중이나, 결과가 SonarQube/개발자 포털에 적재되지 않음

### 영향받는 이해관계자
- **무신사 모바일개발팀-iOS** (사용자: app-store-ios)
- **29CM 모바일개발팀-iOS** (사용자: ios-29cm)
- **서비스플랫폼팀** (운영/구축 주체)

### 요구사항
- **(R1)** SonarQube Developer Edition 구매 없이 iOS(Swift) 정적분석 환경 구축
- **(R2)** 분석 결과를 기존 SonarQube 대시보드/Quality Gate/개발자 포털로 일원화
- **(R3)** 양 팀이 운영 중인 SwiftLint 룰셋·자체 컨벤션·xccov 커버리지 자산 재활용
- **(R4)** PR 단계에서 인라인 피드백 제공 (기존 Danger 경험 유지)
- **(R5)** SonarQube 호환성 — 기존 다른 언어 프로젝트와 동일한 사용자 경험

## 변경 범위

### 포함
- **MuSA SonarQube 플러그인 (jar)** 신규 개발
  - Swift 언어 등록, SwiftLint 룰 매핑/등록, 무신사 자체 컨벤션 룰 등록
  - SwiftLintSensor (SwiftLint JSON → SonarQube Issue)
  - XccovCoverageSensor (xccov 결과 → SonarQube Coverage)
  - LLM 기반 이슈 코멘트 데코레이터 (개발자 컨텍스트 코멘트 자동 생성)
  - 기본 Quality Profile (양 팀 룰셋 통합본)
- **양 팀 GitHub Actions 워크플로우 샘플** 작성 (sonar-scanner 호출 패턴)
- **무신사 SonarQube 26.1 운영 환경 (Helm Chart)** 에 jar 주입 방식 정의
- **PoC 결과 보고** 및 호환성/판단 기준 비교

### 미포함
- 무신사 SonarQube **본체 코드 수정**
- **Android (Kotlin)** 정적분석 — 이미 SonarQube + ktlint로 운영 중
- **Objective-C** 분석 (Swift 우선, 추후 검토)
- **xcodebuild 빌드 자동화 자체** — 기존 양 팀 워크플로우 재사용
- 본 PoC 미달 시 **SonarQube Developer Edition 구매로 Fallback** (구매 절차는 별도 문서)

## 시스템 아키텍처

### 컴포넌트 구성

```
[iOS Repo (PR/Push)]                    [SonarQube 26.1 Server]
        │                                       │
        ▼                                       │
[GitHub Actions]                                │
   ├─ SwiftLint        ──> swiftlint.json       │
   ├─ xccov            ──> coverage.xml         │
   └─ sonar-scanner    ─────────────────────────▶
                                                │
                                                ▼
                                       [MuSA Plugin (jar)]
                                       ├─ SwiftLanguage
                                       ├─ SwiftLintRulesDefinition
                                       ├─ MusaConventionRules
                                       ├─ SwiftLintSensor
                                       ├─ XccovCoverageSensor
                                       ├─ SwiftQualityProfile
                                       └─ LLMCommentDecorator
                                                │
                                                ▼
                                  [기존 SonarQube 인프라 재사용]
                                       ├─ 대시보드
                                       ├─ Quality Gate
                                       ├─ PR Decoration (GitHub)
                                       └─ 개발자 포털 적재
```

### 외부 의존
| 외부 서비스/도구 | 역할 | 비고 |
|----------------|------|------|
| 무신사 SonarQube 26.1.0 | 분석 결과 적재/시각화/Quality Gate | 기존 인프라 |
| GitHub Actions | iOS Repo CI 트리거 + 분석 실행 | 양 팀 기존 운영 중 |
| SwiftLint | Swift 룰베이스 정적분석 | OSS, 양 팀 기존 사용 |
| xccov / xcodebuild | iOS 테스트 커버리지 | 양 팀 기존 사용 |
| LLM API | 이슈별 컨텍스트 코멘트 생성 | 신규 도입, 모델 PoC 중 결정 |
| 개발자 포털 | 품질 데이터 노출 | 기존 SonarQube 연동 그대로 |

### 프로세스/데이터 흐름

1. 개발자가 iOS Repo에 PR/Push
2. GitHub Actions가 SwiftLint, xccov 실행 → JSON/XML 산출물 생성
3. sonar-scanner가 산출물과 함께 SonarQube로 전송
4. **MuSA Plugin**이 SonarQube 서버 측에서 다음을 수행:
   - SwiftLint JSON → SonarQube Issue 변환 (룰 매핑)
   - xccov 결과 → SonarQube Coverage 메트릭 적재
   - LLM API 호출 → 이슈별 컨텍스트 코멘트 첨부 (선택적)
5. SonarQube가 Quality Gate 평가 → GitHub PR에 PR Decoration 코멘트 게시
6. 개발자 포털이 SonarQube에서 메트릭을 가져와 노출

### 핵심 원칙

- **룰 판정 = 룰베이스 (SwiftLint + 무신사 컨벤션 룰)** — 결정적(deterministic) 분석 보장
- **LLM = 코멘트 생성에만 사용** — 비결정성 영향을 게이트 로직에서 격리
- **공식 SonarQube Plugin API만 사용** — 라이선스/지원 정책 리스크 최소화

### 차용 자산 (하이브리드 전략)

| 영역 | 출처 | 활용 방식 |
|------|------|----------|
| Plugin 구조/패턴 | [insideapp-oss/sonar-apple](https://github.com/insideapp-oss/sonar-apple) | **패턴 학습만**, 코드 직접 복사 X (LGPL 오염 방지) |
| Swift 룰셋 | 무신사 [app-store-ios](https://github.com/musinsa/app-store-ios) `.swiftlint.yml` | 공식 + 커스텀 룰 통합 |
| 자체 컨벤션 | `scripts/verify_conventions.sh` (Reactor/ViewController/MARK 등) | SonarQube 정식 룰로 격상 |
| 테스트/PR 룰 분리 | 29CM [ios-29cm](https://github.com/29CM-Developers/ios-29cm) `.swiftlint-danger.yml` | 프로덕션/테스트 룰셋 분리 적용 |
| 커버리지 | 양 팀 `coverage-config.yml` + xccov + Python 필터 스크립트 | XccovCoverageSensor에서 import |
| LLM 코멘트 | 신규 개발 | 무신사 IP |

### 비기능 요구사항

| 항목 | 목표 | 비고 |
|------|------|------|
| 성능 | 분석 시간 ≤ 기존 양 팀 SwiftLint 단독 분석 시간 + 30% | sonar-scanner 오버헤드 포함 |
| 가용성 | 기존 SonarQube SLA 그대로 (플러그인은 무상태) | LLM 호출 실패 시 코멘트 생략, 분석 자체는 성공 |
| 보안 | LLM 전송 데이터에 민감 정보 포함 금지 (소스 코드 일부만 컨텍스트로 전송) | PoC 중 정책 정의 |
| 확장성 | 향후 Objective-C/iOS Test Result/Periphery 등 추가 통합 가능한 모듈식 구조 | Sensor 단위 분리 |
| 라이선스 | LGPL/상용 라이선스 우회 없음, 무신사 자체 IP | SonarSource 공식 Plugin API만 사용 |

### 라이선스/정책 검토 결과

자체 Swift 플러그인 개발 가능성을 다음 4가지 관점에서 검증했다.

| 검토 항목 | 결과 | 근거 |
|----------|------|------|
| **기술적 가능성** | ✅ 가능 | SonarSource 공식 가이드 ["Adding new language support"](https://docs.sonarsource.com/sonarqube-server/2026.1/extension-guide/developing-a-plugin) 가 새 언어 추가를 표준 절차로 명시. `org.sonar.api.resources.Language` 인터페이스 구현으로 어떤 언어든 등록 가능 |
| **에디션 호환성** | ✅ 가능 | Community Edition / Developer Edition / Enterprise Edition 모두 자체 플러그인 로드를 지원. 무신사 운영 환경(26.1.0.118079)은 CE이며 별도 라이선스 변경 불필요 |
| **라이선스** | ✅ 자유 | Plugin API(`sonar-plugin-api`)는 LGPL-3.0이며 호출(linking)만으로는 LGPL 의무 발생하지 않음. 자체 작성 코드는 무신사 단독 라이선스(Proprietary 포함) 부여 가능 |
| **상용 분석기와 충돌** | ✅ 없음 | Developer Edition의 SonarSource 공식 Swift 분석기는 `swift` 언어 키를 점유하나, 무신사 SonarQube는 CE이므로 동시 존재 가능성 0. 향후 DE 전환 시에도 별도 언어 키(예: `musa-swift`)로 회피 가능 |

**핵심 원칙**
- `insideapp-oss/sonar-apple` 코드 **직접 복사 금지** (LGPL 의무 발생) → 패턴 학습만 활용
- 모든 플러그인 코드는 **자체 작성** (무신사 IP 보유)
- SonarSource 공식 Plugin API만 사용 (라이선스/지원 정책 우회 없음)

## 트레이드 오프, 대안

### 고려한 대안

| 옵션 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **A** | OSS `insideapp-oss/sonar-apple` (9.9용) → 26.1 포팅 | 룰셋/Sensor 코드 자산 | Plugin API 메이저 1버전+ 갭, 포팅 1~2주, LGPL 의무 발생 가능, 유지보수 부담 내재화 |
| **B** | SonarQube와 별개 자체 도구 + 자체 포털 | LLM 통합 자유도 높음 | 포털/대시보드/Quality Gate 추가 구현 필요, 다른 언어 분석과 사용자 경험 분리됨 |
| **C** | SonarQube Developer Edition 구매 | 공식 지원, 즉시 도입 | 연 $7,500 (약 1천만원) 비용, 향후 라이선스 정책 변경 리스크 |
| **D** | **SonarQube 26.1 자체 플러그인 신규 개발** ⭐ | 공식 경로, 기존 인프라 100% 재사용, 라이선스 리스크 0, 무신사 IP | Plugin API 학습 곡선, 자체 유지보수 책임 |

### 선택한 설계와 대안 비교

**Option D 선택 사유**
- SonarSource 공식 문서상 [자체 플러그인 개발은 표준 지원](https://docs.sonarsource.com/sonarqube-server/2026.1/extension-guide/developing-a-plugin/plugin-basics) 방식으로 명시됨
- Community Edition / Developer Edition 모두 플러그인 개발 가능, **언어 제약 없음**
- 기존 SonarQube 인프라(대시보드/Quality Gate/PR Decoration/개발자 포털 적재) 100% 재사용
- A안 포팅 비용 ≈ D안 신규 개발 비용 (둘 다 2~3주), 단 D안은 깨끗한 최신 코드베이스 + 라이선스 리스크 0
- B안 대비 신규 개발 범위 작음 (포털/대시보드 재사용), 사용자 경험 통일
- C안 대비 비용 절감 + 자체 IP 보유 + LLM 통합 자유도

### 비용 비교

| 항목 | C) Developer Edition | **D) MuSA** |
|------|---------------------|-------------|
| 라이선스 | $7,500/년 (약 1천만원) | $0 |
| SonarQube 인프라 | 기존 운영 중 | 기존 그대로 재사용 |
| 추가 인프라 | 없음 | 없음 (jar만 배포) |
| LLM API 비용 | - | PR당 호출 비용 (PoC 중 산정) |
| 초기 개발 공수 | 0 | 약 3주 (PoC 일정) |
| 유지보수 | SonarSource 책임 | 무신사 책임 |
| **합계 (1년 기준)** | **~1천만원** | **개발 공수 + LLM 소액** |

## 주요 위험

| 분류 | 위험 | 영향 | 대응 |
|------|------|------|------|
| **기술** | Plugin API 학습 곡선으로 일정 초과 | PoC 일정 초과 | OSS `sonar-apple` 패턴 학습 + MVP 범위 축소 (SwiftLint Sensor 우선) |
| **기술** | sonar-scanner 분석 시간 증가로 PR 지연 | 개발자 경험 저하 | 캐싱/증분 분석 옵션 활용, 임계 시 비동기 분석으로 전환 |
| **운영** | 무신사 SonarQube에 jar 배포 시 운영 영향 | 다른 언어 프로젝트 장애 가능 | 별도 인스턴스 또는 staging에서 충분한 검증 후 배포, Helm Chart values로 ConfigMap 주입 |
| **호환성** | SonarQube 26.x → 향후 메이저 업그레이드 시 Plugin API 변경 | MuSA 플러그인 재작업 필요 | LTA(Long-Term Active) 버전 선호, API 호환성 기준 자동 테스트 도입 |
| **품질** | LLM 코멘트 비결정성으로 잘못된 가이드 제공 | 개발자 혼란 | 룰 판정과 LLM 코멘트 분리 (LLM 실패해도 분석 자체는 성공), 코멘트에 LLM 생성 표시 |
| **보안** | LLM API에 소스 코드 컨텍스트 전송 | 코드 외부 유출 | 사내 LLM 사용 또는 데이터 비전송 정책 약정된 외부 API만 사용, 민감 패턴 마스킹 |
| **라이선스** | `sonar-apple` 코드를 직접 복사 시 LGPL 의무 발생 | 사내 사용 시 영향 적으나 외부 배포 어려움 | 패턴 학습만 하고 모든 코드는 자체 작성 원칙 |
| **정책** | SonarSource 향후 정책 변경으로 CE에서 Swift 언어 키 차단 가능성 | 플러그인 무력화 | 자체 플러그인은 별도 언어 키 사용 가능, B안(별도 도구)으로 전환 가능한 경계 설계 |
| **비즈니스** | PoC 결과 기대 수준 미달 | iOS 정적분석 도입 지연 | Fallback: SonarQube Developer Edition 구매 진행 |

### 가정 (Assumptions)

- 무신사 SonarQube 26.1.0 운영 환경의 플러그인 주입(Helm Chart 또는 사이드카) 변경이 가능하다
- 양 팀 iOS Repo의 GitHub Actions 워크플로우에 sonar-scanner 단계 추가가 가능하다
- 사용 가능한 LLM API 제공자(사내 또는 외부)가 PoC 기간 내 확보된다
- xccov 출력 포맷이 SonarQube Generic Coverage 포맷으로 변환 가능하다 (또는 자체 파서로 처리 가능하다)

### 일정 (참고)

| 기간 | 마일스톤 | 산출물 |
|------|---------|--------|
| W1 (2026-04-27 ~ 05-01) | Plugin 골격 + 핵심 Sensor 구현 | Plugin/Language/RulesDefinition, SwiftLintSensor, 무신사 컨벤션 룰, xccov import |
| W2 (2026-05-04 ~ 05-08) | 통합 검증 + LLM 코멘트 + SonarQube 설치 검증 + iOS 개발자 검수 | 양 팀 샘플 레포 분석 성공, LLM 데코레이터, PR Decoration 동작 확인, sonarqube(-mobile) 환경에 jar 배포 및 동작 검증, iOS 개발자 검수 피드백 수렴 |
| W3 (2026-05-11 ~ 05-15) | 검증·튜닝·보고 | 성능/안정성 검증, 판단 기준 비교(DE 대비), PoC 결과 보고 (5/15) |

> 공휴일: 5/1(금) 근로자의 날, 5/5(화) 어린이날

### 성공 기준 / Fallback

**성공 기준**
- 양 팀 샘플 레포에서 분석 → 무신사 SonarQube 대시보드에 정상 적재
- SwiftLint 룰 + 무신사 컨벤션 룰 모두 SonarQube Issue로 표출
- xccov 커버리지가 SonarQube Coverage 메트릭으로 적재
- LLM 코멘트가 이슈 단위로 정상 첨부
- 총 운영 비용 < SonarQube Developer Edition 라이선스 비용

**Fallback**
- PoC 결과 기대 수준 미달 또는 구현 난이도 과도 → SonarQube Developer Edition 구매로 전환

---

## 참고

- [SonarQube Plugin 개발 공식 문서 (2026.1)](https://docs.sonarsource.com/sonarqube-server/2026.1/extension-guide/developing-a-plugin/plugin-basics)
- [SonarQube Developer Edition 견적 분석 (for Mobile)](https://wiki.team.musinsa.com/wiki/spaces/PLATFORM/pages/406487549)
- [HLD(High Level Design) 문서 템플릿 (PRODUCTS)](https://wiki.team.musinsa.com/wiki/spaces/PRODUCTS/pages/162529862/HLD+High+Level+Design)
- [HLD/LLD/ADR 문서란 (QApart)](https://wiki.team.musinsa.com/wiki/spaces/QApart/pages/379225568/HLD+LLD+ADR)
- [insideapp-oss/sonar-apple](https://github.com/insideapp-oss/sonar-apple) (참고 교본)
- [무신사 app-store-ios](https://github.com/musinsa/app-store-ios)
- [29CM ios-29cm](https://github.com/29CM-Developers/ios-29cm)
