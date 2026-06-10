# Free Model Rotation

Remove Minecraft's item/block model rotation limits — **free angles + multi-axis (x/y/z) rotation**. Client-side, drop-in, zero dependencies.

마인크래프트 아이템/블록 모델의 회전 제한 해제 — **자유 각도 + 다축(x/y/z) 회전**. 클라이언트 전용, 드롭인, 의존성 없음.

**[English](#english) · [한국어](#한국어)**

---

## Supported Versions

Fabric, client-side. Minecraft **1.21.6+ supports this natively** (no mod needed there).

| Minecraft | Java | Minecraft | Java |
|-----------|------|-----------|------|
| 1.20.1 | 17 | 1.21 | 21 |
| 1.20.2 | 17 | 1.21.1 | 21 |
| 1.20.3 | 17 | 1.21.2 | 21 |
| 1.20.4 | 17 | 1.21.3 | 21 |
| 1.20.5 | 21 | 1.21.4 | 21 |
| 1.20.6 | 21 | 1.21.5 | 21 |

---

## English

### What it does

Vanilla Minecraft (before snapshot 25w46a / 1.21.6) restricts JSON model **element** rotation to:
- a **single axis** (x, y, or z — one at a time)
- angles snapped to **22.5° increments**, within **−45°…45°**

This mod removes both:
- **Free angles** — any value, decimals, beyond ±45°
- **Multi-axis** — `{"origin": [...], "x": .., "y": .., "z": ..}` on a single element, applied **X→Y→Z** (the same order as vanilla 25w46a and Blockbench)
- The legacy `{"axis": .., "angle": ..}` single-axis form keeps working

So Blockbench models that use free angles / multi-axis rotation render correctly instead of failing to load (no more purple-black missing texture).

### Important — client-side only

This is a **client** mod. A model with free/multi-axis rotation renders correctly **only for players who have this mod installed**. Players without it see the broken vanilla result (cracked / missing).

Good fit for:
- CustomModelData item models on servers that **require** a client mod (hash-locked / modpack servers)
- Modpacks
- Your own resource packs / singleplayer

It is **not** a fix for public servers where you can't enforce a client mod.

### How to use

1. Install **Fabric Loader** + this mod (client side). **Fabric API is not required.**
2. Build your model in **Blockbench** (Java Block/Item format) with free angles / multi-axis rotation.
3. Put the exported model JSON in your resource pack as usual (item or block model, e.g. via CustomModelData).
4. It renders in-game.

### How it works

Three tiny Mixins on the vanilla model pipeline (no custom renderer, no resource scanning):
- `ModelAngleLimitMixin` → bypass the 22.5°/±45° angle check
- `ModelMultiAxisRotationMixin` → parse `{x,y,z}` (and `{"angle":[x,y,z]}`) multi-axis rotation
- `BakedQuadFactoryMixin` → apply the 3-axis rotation at bake time (JOML quaternion, X→Y→Z)

Single-axis models are left untouched (vanilla path), so vanilla models are never affected.

### Limitations

- `rescale` is not applied to multi-axis rotations (vanilla `rescale` is a ±22.5/45 single-axis correction with no meaning for arbitrary 3-axis rotation).
- Very large angles can show the vanilla baker's per-face lighting/AO seams — a BakedQuad limitation shared with vanilla single-axis rotation.

### License

MIT

---

## 한국어

### 기능

바닐라 마인크래프트(스냅샷 25w46a / 1.21.6 이전)는 JSON 모델 **element** 회전을 다음으로 제한합니다:
- **단일 축**(x, y, z 중 하나만)
- **22.5° 단위**, **−45°~45°** 범위로 스냅

이 모드는 둘 다 해제합니다:
- **자유 각도** — 임의 값, 소수점, ±45° 초과
- **다축** — 한 element에 `{"origin": [...], "x": .., "y": .., "z": ..}`, **X→Y→Z** 순서로 적용(바닐라 25w46a 및 블록벤치와 동일한 순서)
- 기존 `{"axis": .., "angle": ..}` 단일축 표기도 그대로 동작

덕분에 자유 각도 / 다축 회전을 쓴 블록벤치 모델이 로드 실패(보라-검정 미싱 텍스처) 없이 정상 렌더됩니다.

### 중요 — 클라이언트 전용

이건 **클라이언트** 모드입니다. 자유/다축 회전 모델은 **이 모드를 설치한 플레이어에게만** 정상으로 보입니다. 미설치 플레이어는 깨진 바닐라 결과(갈라짐/미싱)를 봅니다.

적합한 경우:
- 클라 모드를 **강제**하는 서버(해시 잠금 / 모드팩)의 CustomModelData 아이템 모델
- 모드팩
- 개인 리소스팩 / 싱글플레이

클라 모드를 강제할 수 없는 공개 서버에는 **부적합**합니다.

### 사용법

1. **Fabric Loader** + 이 모드 설치(클라이언트). **Fabric API 불필요.**
2. **블록벤치**(Java Block/Item 포맷)에서 자유 각도 / 다축 회전 모델 제작.
3. 내보낸 모델 JSON을 리소스팩에 넣기(아이템/블록 모델, 예: CustomModelData).
4. 인게임에서 렌더됩니다.

### 작동 방식

바닐라 모델 파이프라인에 Mixin 3개만 얹습니다(자체 렌더러·리소스 스캔 없음):
- `ModelAngleLimitMixin` → 22.5°/±45° 각도 검증 우회
- `ModelMultiAxisRotationMixin` → `{x,y,z}`(및 `{"angle":[x,y,z]}`) 다축 회전 파싱
- `BakedQuadFactoryMixin` → 베이킹 시 3축 회전 적용(JOML 쿼터니언, X→Y→Z)

단일축 모델은 건드리지 않으므로(바닐라 경로 유지) 일반 바닐라 모델에는 전혀 영향이 없습니다.

### 한계

- 다축 회전에는 `rescale`이 적용되지 않습니다(바닐라 `rescale`은 ±22.5/45 단일축 전용 보정이라 임의 3축 회전에는 의미가 없음).
- 매우 큰 각도는 바닐라 베이커의 면 단위 라이팅/AO 이음매가 보일 수 있습니다 — BakedQuad의 한계로, 바닐라 단일축 회전도 동일합니다.

### 라이선스

MIT
