# FreeModelRotation — Fabric 클라이언트 멀티버전 모드 (1.20.1 ~ 1.21.5)

## 정체성
아이템/블록 JSON 모델 element의 회전 각도 제한(22.5 배수 / ±45 / 단일축)을 해제하고 다축(`x/y/z`) 회전을 추가하는 클라이언트 전용 Fabric 모드. 모드린스 배포용.

바닐라는 snapshot 25w46a(1.21.6+)부터 이 기능을 정식 지원한다 — 그래서 이 모드의 대상은 1.21.6 미만(1.20.1 ~ 1.21.5)이다.

원본: `../TT_Utils`(The Tower 서버용, 1.20.1)의 모델 Mixin 3개를 The Tower 의존성 없이 분리한 것. TT_Utils는 참조용 — 읽기만, 수정 금지.

## 기능 = Mixin 3개 (package io.github.yuupering.freemodelrotation.mixin)
- `ModelAngleLimitMixin` — `ModelElement$Deserializer.deserializeRotationAngle` HEAD 가로채 각도 검증 없이 `angle` 그대로 반환(22.5/±45 제한 해제). 단일축 자유각도용.
- `ModelMultiAxisRotationMixin` — `deserializeRotation` HEAD 가로채 다축 파싱. `{"x":..,"y":..,"z":..}` 또는 `{"angle":[x,y,z]}` 배열이면 placeholder `ModelRotation`을 만들어 `MultiAxisRotations.TABLE`에 실제 3축 각도 등록. 단일축(`axis` 있음)은 통과(바닐라 + AngleLimit). axis 누락+스칼라는 모델 안 죽이고 회전만 무시.
- `BakedQuadFactoryMixin` — `BakedQuadFactory.rotateVertex` HEAD 가로채, TABLE에 등록된 placeholder면 `Quaternionf.rotationZYX(z,y,x)`(정점 X→Y→Z)로 origin 기준 회전. 단일축은 통과.
- `util/MultiAxisRotations` — placeholder→[x,y,z] 사이드 테이블(WeakHashMap, sentinel 각도로 키 충돌 방지).

진입점 `FreeModelRotationClient`는 로그만(기능은 전부 Mixin). fabric-api 미사용 — `ClientModInitializer`는 loader 제공.

## ★ 멀티버전 핵심: 소스 분기 0
1.20.1 ~ 1.21.5 전 구간에서 mixin 타깃의 intermediary 매핑이 **완전히 동일**하다(실측):
- `deserializeRotation` → `class_785$class_786.method_3410(JsonObject)Lclass_789;`
- `deserializeRotationAngle` → `class_785$class_786.method_3403(JsonObject)F`
- `rotateVertex` → `class_796.method_3463(Vector3f, class_789)V`

그래서 소스 코드는 7버전 공통, 단일 소스. 버전별로 바뀌는 건 딱 둘:
- yarn 매핑 (gradle.properties / -P)
- Java 버전 + mixin compatibilityLevel (1.20.4 이하=17, 1.20.5+=21) → build.gradle이 `minecraft_version` 보고 자동 결정

Stonecutter 불필요(소스 분기가 없으므로).

## 빌드
- loom 1.6, gradle 8.7(wrapper), 시스템 Java 21 필요(1.21.x 컴파일).
- 단일 버전: `rtk ./gradlew build -x test -Pminecraft_version=1.21.5 -Pyarn_mappings=1.21.5+build.1`
  - build.gradle이 `java17 = mc in [1.20.1,1.20.2,1.20.3,1.20.4]`로 targetJavaVersion(17/21)·mixinCompat(JAVA_17/21) 자동 설정. processResources가 `freemodelrotation.mixins.json`의 `${mixin_compat}`, `fabric.mod.json`의 `${java_version}`/`${minecraft_version}` 주입.
  - jar 버전 = `${mod_version}+${minecraft_version}`(예: `freemodelrotation-1.0.0+1.21.5.jar`).
- 전체(7버전) 빌드: 아래 버전↔yarn 표로 `-P` 순회, jar를 `dist/`로 복사.

### 버전 ↔ yarn (fabric meta 기준, 갱신 시 https://meta.fabricmc.net/v2/versions/yarn/<mc> 조회)
| MC | yarn | Java |
|----|------|------|
| 1.20.1 | 1.20.1+build.10 | 17 |
| 1.20.2 | 1.20.2+build.4 | 17 |
| 1.20.3 | 1.20.3+build.1 | 17 |
| 1.20.4 | 1.20.4+build.3 | 17 |
| 1.20.5 | 1.20.5+build.1 | 21 |
| 1.20.6 | 1.20.6+build.3 | 21 |
| 1.21 | 1.21+build.9 | 21 |
| 1.21.1 | 1.21.1+build.3 | 21 |
| 1.21.2 | 1.21.2+build.1 | 21 |
| 1.21.3 | 1.21.3+build.2 | 21 |
| 1.21.4 | 1.21.4+build.8 | 21 |
| 1.21.5 | 1.21.5+build.1 | 21 |

새 버전 추가: yarn 조회 → 위 표 + 빌드 순회에 한 줄 추가. 타깃 매핑이 같으면(대개 같음) 소스 수정 불필요. 다르면 그 버전만 Mixin 분기 필요(현재까지는 없음).

## 배포(모드린스)
- 파일: `dist/freemodelrotation-1.0.0+<mc>.jar` 7개. 각 파일에 해당 MC 버전 태그.
- 로더: Fabric, 환경: client, 라이선스: MIT.
- 참고: refmap이 전 버전 동일 + loom이 intermediary로 remap하므로, Java 17 그룹(1.20.1/1.20.4)과 Java 21 그룹(1.20.6~1.21.5) 안에서는 클래스 바이트코드가 같다(fabric.mod.json 버전 문자열만 차이). 원하면 2개 파일에 여러 게임버전 태그로 줄여 올릴 수도 있음.
- 설명 필수 문구: "이 모드 없는 클라에선 자유각도 모델이 깨져 보인다"(클라 전용 한계).

## 경쟁 모드
`Modern Java Block/Item Model Backport`(bay4lly, Modrinth, 1.20.1만, 무명) — 자체 파서+UnbakedModel+BakedModel 파이프라인(15+ 클래스, 라이브러리). 우리는 vanilla 후킹 Mixin 3개(드롭인 + 멀티버전). 차별점 = 멀티버전 + 경량 + 설치형.

## 작업 시 주의
- 모든 shell 명령은 `rtk` 프리픽스(상위 `../CLAUDE.md` RTK 규칙).
- 변경 후 대표 버전(1.20.1=Java17, 1.21.5=Java21) 둘 다 빌드해 검증.
