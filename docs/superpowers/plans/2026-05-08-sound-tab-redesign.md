# Sound Tab Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 사운드 탭을 자장가(순차 플레이리스트) / 백색소음(단일 선택) TabRow로 재구성하고, 볼륨 및 PRO 제한을 제거한다.

**Architecture:** 신규 `LullabyTrack` enum 추가 → `SoundPlayer`에 플레이리스트 재생 로직 및 `currentLullabyIndex` Flow 추가 → `SoundViewModel` 상태를 `selectedTab / currentTrackIndex / activeWhiteNoise`로 재구성 → `SoundScreen`에 Material3 `TabRow` + 자장가 리스트 + 백색소음 그리드 구현.

**Tech Stack:** Kotlin 2.0, Jetpack Compose, Material3 TabRow, Android MediaPlayer, Hilt

---

## 파일 변경 범위

| 파일 | 유형 | 역할 |
|------|------|------|
| Task | 파일 | 유형 | 역할 |
|------|------|------|------|
| 1 | `data/model/LullabyTrack.kt` | 신규 | 자장가 곡 목록 enum (title + resourceName) |
| 2 | `data/model/SoundType.kt` | 수정 | LULLABY 제거, 모든 isPro → false |
| 3 | `data/SoundPlayer.kt` | 수정 | 플레이리스트 재생 로직 추가, 볼륨 제거 |
| 4 | `ui/screen/sound/SoundViewModel.kt` | 재작성 | selectedTab / currentTrackIndex / activeWhiteNoise 상태 |
| 5 | `ui/screen/sound/SoundScreen.kt` | 재작성 | TabRow + 자장가 LazyColumn + 백색소음 그리드 |
| 6 | `ui/component/SoundCard.kt` | 수정 | isPro 파라미터 및 ProBadgeOverlay 제거 (SoundScreen 재작성 후에 적용) |

> **태스크 순서 이유:** SoundScreen 재작성(Task 5) 완료 후 SoundCard에서 `isPro`를 제거해야 빌드 중단 없이 진행됨.

**빌드 명령 (모든 태스크 공통):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew assembleDebug
```

---

## Task 1: LullabyTrack enum 생성

**Files:**
- Create: `app/src/main/java/com/odom/moodlight/data/model/LullabyTrack.kt`

- [ ] **Step 1: 파일 생성**

```kotlin
package com.odom.moodlight.data.model

enum class LullabyTrack(val title: String, val resourceName: String) {
    BRAHMS("브람스 자장가", "lullaby_brahms"),
    TWINKLE("반짝반짝 작은별", "lullaby_twinkle"),
    MOZART("모차르트 자장가", "lullaby_mozart"),
}
```

> MP3 파일은 `app/src/main/res/raw/lullaby_brahms.mp3` 형식으로 추가. 파일 없을 때는 해당 곡을 건너뜀.

- [ ] **Step 2: 빌드 확인**

```powershell
.\gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/odom/moodlight/data/model/LullabyTrack.kt
git commit -m "feat: add LullabyTrack enum for playlist"
```

---

## Task 2: SoundType — LULLABY 제거, isPro → false

**Files:**
- Modify: `app/src/main/java/com/odom/moodlight/data/model/SoundType.kt`

- [ ] **Step 1: SoundType 수정**

전체 파일을 다음 내용으로 교체:

```kotlin
package com.odom.moodlight.data.model

import androidx.annotation.StringRes
import com.odom.moodlight.R

enum class SoundType(
    val emoji: String,
    @StringRes val labelResId: Int,
    val isPro: Boolean,
    val resourceName: String
) {
    RAIN("🌧️", R.string.sound_rain, false, "rain"),
    WAVE("🌊", R.string.sound_wave, false, "wave"),
    FOREST("🌲", R.string.sound_forest, false, "forest"),
    FIRE("🔥", R.string.sound_fire, false, "fire"),
    PIANO("🎹", R.string.sound_piano, false, "piano"),
    WIND("🌬️", R.string.sound_wind, false, "wind"),
}
```

> `LULLABY` 제거. `isPro`는 `SoundCard`가 아직 참조하므로 필드는 유지하되 모두 `false`.

- [ ] **Step 2: 빌드 확인**

```powershell
.\gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/odom/moodlight/data/model/SoundType.kt
git commit -m "feat: remove LULLABY from SoundType, set all isPro false"
```

---

## Task 3: SoundPlayer — 플레이리스트 로직 추가, 볼륨 제거

**Files:**
- Modify: `app/src/main/java/com/odom/moodlight/data/SoundPlayer.kt`

- [ ] **Step 1: SoundPlayer 수정**

전체 파일을 다음 내용으로 교체:

```kotlin
package com.odom.moodlight.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.odom.moodlight.data.model.LullabyTrack
import com.odom.moodlight.data.model.SoundType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 백색소음 플레이어 (SoundType 키)
    private val players = mutableMapOf<SoundType, MediaPlayer>()
    private val _activeSounds = MutableStateFlow<Set<SoundType>>(emptySet())
    val activeSounds: StateFlow<Set<SoundType>> = _activeSounds.asStateFlow()

    // 자장가 플레이어
    private var lullabyPlayer: MediaPlayer? = null
    private var lullabyPlaylist: List<LullabyTrack> = emptyList()
    private val _currentLullabyIndex = MutableStateFlow<Int?>(null)
    val currentLullabyIndex: StateFlow<Int?> = _currentLullabyIndex.asStateFlow()

    // ── 백색소음 ──────────────────────────────────────────────

    fun play(sound: SoundType) {
        if (players.containsKey(sound)) return
        val resId = context.resources.getIdentifier(sound.resourceName, "raw", context.packageName)
        if (resId == 0) return
        val player = MediaPlayer.create(context, resId) ?: return
        player.apply {
            isLooping = true
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            start()
        }
        players[sound] = player
        _activeSounds.value = _activeSounds.value + sound
    }

    fun stop(sound: SoundType) {
        players[sound]?.apply { stop(); release() }
        players.remove(sound)
        _activeSounds.value = _activeSounds.value - sound
    }

    fun toggle(sound: SoundType) {
        if (_activeSounds.value.contains(sound)) stop(sound) else play(sound)
    }

    fun stopAll() {
        players.values.forEach { it.stop(); it.release() }
        players.clear()
        _activeSounds.value = emptySet()
        stopLullaby()
    }

    fun hasActiveSounds() = players.isNotEmpty() || lullabyPlayer != null

    // ── 자장가 플레이리스트 ──────────────────────────────────

    fun playLullaby(tracks: List<LullabyTrack>, startIndex: Int) {
        stopLullaby()
        lullabyPlaylist = tracks
        playLullabyAtIndex(startIndex, attemptsLeft = tracks.size)
    }

    private fun playLullabyAtIndex(index: Int, attemptsLeft: Int) {
        if (attemptsLeft <= 0) return                          // 모든 곡 파일 없음
        val track = lullabyPlaylist.getOrNull(index) ?: return
        val resId = context.resources.getIdentifier(track.resourceName, "raw", context.packageName)
        if (resId == 0) {
            // 파일 없으면 다음 곡으로 건너뜀
            val next = (index + 1) % lullabyPlaylist.size
            playLullabyAtIndex(next, attemptsLeft - 1)
            return
        }
        lullabyPlayer?.release()
        lullabyPlayer = MediaPlayer.create(context, resId)?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setOnCompletionListener {
                val next = (index + 1) % lullabyPlaylist.size
                playLullabyAtIndex(next, lullabyPlaylist.size)
            }
            start()
        }
        _currentLullabyIndex.value = index
    }

    fun stopLullaby() {
        lullabyPlayer?.apply { stop(); release() }
        lullabyPlayer = null
        lullabyPlaylist = emptyList()
        _currentLullabyIndex.value = null
    }
}
```

- [ ] **Step 2: 빌드 확인**

```powershell
.\gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/odom/moodlight/data/SoundPlayer.kt
git commit -m "feat: add lullaby playlist logic to SoundPlayer, remove volume"
```

---

## Task 6: SoundCard — isPro 파라미터 및 ProBadgeOverlay 제거

**Files:**
- Modify: `app/src/main/java/com/odom/moodlight/ui/component/SoundCard.kt`

- [ ] **Step 1: SoundCard 수정**

전체 파일을 다음 내용으로 교체:

```kotlin
package com.odom.moodlight.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odom.moodlight.R
import com.odom.moodlight.data.model.SoundType
import com.odom.moodlight.ui.theme.AppColors

@Composable
fun SoundCard(
    sound: SoundType,
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) AppColors.TextPrimary.copy(alpha = 0.15f)
                             else AppColors.Panel
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = sound.emoji, fontSize = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(id = sound.labelResId),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            if (isActive) {
                WaveformAnimation(
                    color = AppColors.WarmYellow,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Icon(
                imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isActive) stringResource(R.string.sound_stop)
                                     else stringResource(R.string.sound_play),
                tint = AppColors.TextPrimary
            )
        }
    }
}
```

- [ ] **Step 2: 빌드 확인**

```powershell
.\gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL` (Task 5에서 SoundScreen이 이미 `isPro` 없이 SoundCard를 호출하므로 오류 없음)

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/odom/moodlight/ui/component/SoundCard.kt
git commit -m "feat: remove isPro param and ProBadgeOverlay from SoundCard"
```

---

## Task 4: SoundViewModel 재작성

**Files:**
- Modify: `app/src/main/java/com/odom/moodlight/ui/screen/sound/SoundViewModel.kt`

- [ ] **Step 1: SoundViewModel 재작성**

전체 파일을 다음 내용으로 교체:

```kotlin
package com.odom.moodlight.ui.screen.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.moodlight.data.SoundPlayer
import com.odom.moodlight.data.model.LullabyTrack
import com.odom.moodlight.data.model.SoundType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class SoundTab { LULLABY, WHITE_NOISE }

data class SoundUiState(
    val selectedTab: SoundTab = SoundTab.LULLABY,
    val currentTrackIndex: Int? = null,       // null = 자장가 정지
    val activeWhiteNoise: SoundType? = null,  // null = 백색소음 정지
)

@HiltViewModel
class SoundViewModel @Inject constructor(
    private val soundPlayer: SoundPlayer,
) : ViewModel() {

    val tracks: List<LullabyTrack> = LullabyTrack.entries

    private val _selectedTab = kotlinx.coroutines.flow.MutableStateFlow(SoundTab.LULLABY)

    val state: StateFlow<SoundUiState> = combine(
        _selectedTab,
        soundPlayer.currentLullabyIndex,
        soundPlayer.activeSounds,
    ) { tab, trackIndex, activeSounds ->
        SoundUiState(
            selectedTab = tab,
            currentTrackIndex = trackIndex,
            activeWhiteNoise = activeSounds.firstOrNull(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SoundUiState(),
    )

    fun selectTab(tab: SoundTab) {
        _selectedTab.value = tab
    }

    fun toggleLullaby(index: Int) {
        if (state.value.currentTrackIndex == index) {
            soundPlayer.stopLullaby()
        } else {
            state.value.activeWhiteNoise?.let { soundPlayer.stop(it) }
            soundPlayer.playLullaby(tracks, index)
        }
    }

    fun toggleWhiteNoise(sound: SoundType) {
        if (state.value.activeWhiteNoise == sound) {
            soundPlayer.stop(sound)
        } else {
            soundPlayer.stopLullaby()
            state.value.activeWhiteNoise?.let { soundPlayer.stop(it) }
            soundPlayer.play(sound)
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundPlayer.stopAll()
    }
}
```

- [ ] **Step 2: 빌드 확인**

```powershell
.\gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL` (SoundScreen 컴파일 오류는 Task 6에서 해소)

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/odom/moodlight/ui/screen/sound/SoundViewModel.kt
git commit -m "feat: rewrite SoundViewModel with tab/lullaby/whitenoise state"
```

---

## Task 5: SoundScreen 재작성

**Files:**
- Modify: `app/src/main/java/com/odom/moodlight/ui/screen/sound/SoundScreen.kt`

- [ ] **Step 1: SoundScreen 재작성**

전체 파일을 다음 내용으로 교체:

```kotlin
package com.odom.moodlight.ui.screen.sound

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.moodlight.data.model.LullabyTrack
import com.odom.moodlight.data.model.SoundType
import com.odom.moodlight.ui.component.AdBannerView
import com.odom.moodlight.ui.component.SoundCard
import com.odom.moodlight.ui.component.WaveformAnimation
import com.odom.moodlight.ui.theme.AppColors

@Composable
fun SoundScreen(viewModel: SoundViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = state.selectedTab.ordinal,
            containerColor = AppColors.Panel,
            contentColor = AppColors.WarmYellow,
        ) {
            Tab(
                selected = state.selectedTab == SoundTab.LULLABY,
                onClick = { viewModel.selectTab(SoundTab.LULLABY) },
                text = { Text("🎵 자장가", fontSize = 14.sp) }
            )
            Tab(
                selected = state.selectedTab == SoundTab.WHITE_NOISE,
                onClick = { viewModel.selectTab(SoundTab.WHITE_NOISE) },
                text = { Text("🌊 백색소음", fontSize = 14.sp) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (state.selectedTab) {
                SoundTab.LULLABY -> LullabyTab(
                    tracks = viewModel.tracks,
                    currentTrackIndex = state.currentTrackIndex,
                    onToggle = viewModel::toggleLullaby,
                )
                SoundTab.WHITE_NOISE -> WhiteNoiseTab(
                    activeSound = state.activeWhiteNoise,
                    onToggle = viewModel::toggleWhiteNoise,
                )
            }
        }

        AdBannerView(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun LullabyTab(
    tracks: List<LullabyTrack>,
    currentTrackIndex: Int?,
    onToggle: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(tracks) { index, track ->
            LullabyTrackRow(
                track = track,
                isActive = currentTrackIndex == index,
                onClick = { onToggle(index) },
            )
        }
    }
}

@Composable
private fun LullabyTrackRow(
    track: LullabyTrack,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) AppColors.TextPrimary.copy(alpha = 0.1f)
                             else AppColors.Panel,
        ),
        border = if (isActive) BorderStroke(1.dp, AppColors.WarmYellow.copy(alpha = 0.6f))
                 else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = track.title,
                fontSize = 16.sp,
                color = if (isActive) AppColors.TextPrimary else AppColors.TextDim,
                modifier = Modifier.weight(1f),
            )
            if (isActive) {
                WaveformAnimation(color = AppColors.WarmYellow)
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = AppColors.TextDim,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun WhiteNoiseTab(
    activeSound: SoundType?,
    onToggle: (SoundType) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(SoundType.entries) { sound ->
            SoundCard(
                sound = sound,
                isActive = activeSound == sound,
                onToggle = { onToggle(sound) },
            )
        }
    }
}
```

- [ ] **Step 2: 최종 빌드 확인**

```powershell
.\gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/java/com/odom/moodlight/ui/screen/sound/SoundScreen.kt
git commit -m "feat: rewrite SoundScreen with TabRow, lullaby list, white noise grid"
```

---

## 동작 검증 체크리스트

실기기 또는 에뮬레이터에서 확인:

- [ ] 사운드 탭 진입 시 TabRow에 "🎵 자장가" / "🌊 백색소음" 탭이 표시됨
- [ ] 자장가 탭: 곡 목록이 LazyColumn으로 표시됨
- [ ] 자장가 곡 탭 → 재생 시작, 해당 행에 WaveformAnimation 표시
- [ ] 재생 중인 자장가 곡 재탭 → 정지
- [ ] 다른 자장가 곡 탭 → 이전 곡 정지, 새 곡 재생
- [ ] 백색소음 탭 전환 후 백색소음 선택 → 자장가 자동 정지
- [ ] 백색소음 탭: 2열 그리드, PRO 잠금 배지 없음
- [ ] 백색소음 항목 탭 → 재생, 다른 항목 탭 → 이전 것 정지 후 새 것 재생
- [ ] 앱 타이머 종료 시 모든 소리 정지 (`stopAll()` → `stopLullaby()` 연쇄 호출)

---

## MP3 파일 추가 방법 (나중에)

`LullabyTrack.resourceName`과 동일한 파일명으로 `app/src/main/res/raw/`에 추가:

```
res/raw/lullaby_brahms.mp3
res/raw/lullaby_twinkle.mp3
res/raw/lullaby_mozart.mp3
```

파일 추가 후 별도 코드 변경 없이 자동 반영됨.
