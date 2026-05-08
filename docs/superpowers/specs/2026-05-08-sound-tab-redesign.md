# Sound Tab Redesign

**Date:** 2026-05-08  
**Status:** Approved

## Overview

사운드 탭 UX를 두 카테고리(자장가 / 백색소음)로 재구성한다. 볼륨 조절과 PRO 제한을 제거하고, 자장가는 순차 플레이리스트 방식, 백색소음은 단일 선택 방식으로 단순화한다.

---

## UI Structure

### Tab Layout
- 상단: Material3 `TabRow` — `🎵 자장가` / `🌊 백색소음` 두 탭
- 기존 화면 헤더("🔊 사운드" 제목 + 볼륨 버튼) 제거
- 하단: 기존 `AdBannerView` 유지

### 자장가 탭
- `LazyColumn` 형태의 곡 리스트
- 각 행: 곡 제목 + 우측에 재생/정지 상태 표시
- 재생 중인 곡: `WaveformAnimation` + 강조 스타일 표시
- 탭 동작:
  - 곡 탭 → 해당 곡 인덱스부터 순차 재생 시작
  - 재생 중인 곡 재탭 → 정지
  - 다른 곡 탭 → 즉시 그 곡으로 전환

### 백색소음 탭
- 기존 `SoundCard` 컴포넌트 재사용, 2열 그리드 유지
- 단일 선택: 탭 시 이전 소리 자동 정지, 새 소리 재생
- 재탭 시 정지
- PRO 잠금 배지(`ProBadgeOverlay`) 및 페이월 제거

---

## Data Model

### 신규: `LullabyTrack` enum
```kotlin
enum class LullabyTrack(val title: String, val resourceName: String) {
    BRAHMS("브람스 자장가", "lullaby_brahms"),
    TWINKLE("반짝반짝 작은별", "lullaby_twinkle"),
    // MP3 파일 추가 시 여기에 항목 추가
}
```
- `resourceName`은 `res/raw/` 파일명과 1:1 매핑
- 파일 없을 때 `SoundPlayer`는 graceful skip (기존 동작 유지)

### 변경: `SoundType` enum
- `LULLABY` 항목 제거 (LullabyTrack으로 대체)
- 나머지 6개(RAIN, WAVE, FOREST, FIRE, PIANO, WIND) 유지
- `isPro` 플래그 전부 `false`로 변경

---

## SoundPlayer 변경

자장가 플레이리스트 재생을 위한 로직 추가:

- `playLullaby(track: LullabyTrack, allTracks: List<LullabyTrack>)` — 지정 인덱스부터 재생 시작
- `setOnCompletionListener` → 다음 곡 자동 재생, 마지막 곡 완료 시 인덱스 0으로 순환
- `stopLullaby()` — 자장가 MediaPlayer 해제
- 백색소음: 단일 MediaPlayer만 사용하도록 단순화 (기존 다중 지원 → 단일)
- 볼륨 관련 상태(`_volumes`, `setVolume()`) 제거

---

## SoundViewModel 상태 재구성

```kotlin
data class SoundUiState(
    val selectedTab: SoundTab = SoundTab.LULLABY,   // 선택된 탭
    val currentTrackIndex: Int? = null,              // 재생 중인 자장가 인덱스 (null=정지)
    val activeWhiteNoise: SoundType? = null,         // 재생 중인 백색소음 (null=정지)
)

enum class SoundTab { LULLABY, WHITE_NOISE }
```

**재생 상호 배타성:** 자장가와 백색소음은 동시에 재생되지 않는다. 한쪽을 재생하면 다른 쪽은 자동 정지.

주요 메서드:
- `selectTab(tab: SoundTab)` — 탭 전환 (재생은 유지, 탭 상태만 변경)
- `toggleLullaby(index: Int)` — 자장가 재생 시작 시 백색소음 정지 후 재생; 재탭 시 정지
- `toggleWhiteNoise(sound: SoundType)` — 백색소음 재생 시작 시 자장가 정지 후 재생; 재탭/다른 항목 탭 처리

---

## 제거 항목

| 항목 | 이유 |
|------|------|
| 볼륨 버튼 & `VolumeBottomSheet` | UX 단순화 |
| `SoundViewModel.volumes`, `setVolume()` | 볼륨 제거 |
| PRO 체크 로직 (사운드 탭) | 모두 무료 |
| `PaywallBottomSheet` 사운드 탭 연동 | PRO 제거 |
| `SoundType.LULLABY` | `LullabyTrack`으로 대체 |

---

## 파일 변경 범위

| 파일 | 변경 유형 |
|------|----------|
| `data/model/SoundType.kt` | LULLABY 제거, isPro → false |
| `data/model/LullabyTrack.kt` | 신규 생성 |
| `data/SoundPlayer.kt` | 플레이리스트 로직 추가, 볼륨 제거 |
| `ui/screen/sound/SoundViewModel.kt` | 상태 재구성 |
| `ui/screen/sound/SoundScreen.kt` | TabRow + 두 탭 UI |

---

## MP3 파일 추가 방법 (개발자 참고)

1. `app/src/main/res/raw/` 에 MP3 파일 추가
2. `LullabyTrack` enum에 항목 추가 (`resourceName` = 파일명)
3. 빌드 후 자동 반영
