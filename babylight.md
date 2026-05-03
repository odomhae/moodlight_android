# 아기 야간등 앱 개발 요청 프롬프트 (Claude Code용)

---

## 프로젝트 개요

BabyLight(https://play.google.com/store/apps/details?id=com.aethiumapps.babylight) 를 참고한
**아기 야간등 Android 앱**을 Kotlin + Jetpack Compose로 개발해줘.

앱의 핵심 목적은 스마트폰 화면을 부드러운 야간등으로 사용하는 것이야.
아기를 재울 때 쓰는 앱이라 UI는 어둡고 따뜻하고 조용한 느낌이어야 해.

---

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose (Material3)
- **아키텍처**: MVVM + Clean Architecture
- **DI**: Hilt
- **상태 관리**: ViewModel + StateFlow
- **네비게이션**: Navigation Compose
- **오디오**: MediaPlayer / ExoPlayer (백그라운드 재생)
- **화면 꺼짐 방지**: WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
- **애니메이션**: Compose Animation API (animateFloatAsState, InfiniteTransition)
- **인앱 결제**: Google Play Billing Library (최신 버전)
- **스토리지**: DataStore Preferences (설정), EncryptedSharedPreferences (프리미엄 상태)
- **빌드**: Gradle (Kotlin DSL), minSdk 26, targetSdk 최신

---

## 화면 구성 (총 4개 탭)

NavigationBar + Scaffold 구조로 탭 네비게이션 구성.

### 1. 조명 탭 (메인 화면) `LightScreen`

**레이아웃:**
- 상단 70%: 조명 영역
  - 화면 전체를 선택한 색상으로 채움 (Box + background)
  - 중앙에 큰 원형 발광 orb (Canvas + RadialGradient + BlurMaskFilter)
  - orb 안에 아기 이모지 텍스트 (탭하면 다음 이모지로 전환: 🌙 👶 🌟 🐑 🦋)
  - orb 아래에 현재 시각 표시 (HH:MM, FontWeight.Light)
  - orb 아래에 날짜 표시 (요일, 월 일)
  - 색상 사이클 모드 활성 시 "🌈 색상 사이클 모드 중" 텍스트 표시
- 하단 30%: 컨트롤 패널 (반투명 다크 배경, Surface + alpha)

**컨트롤 패널 구성 (위→아래):**

1. **색상 선택 도트** (Row, 6개)
   - 🟠 따뜻한 노랑 (#FFD6A0)
   - 🔵 하늘 파랑 (#A8D8FF)
   - 🟢 민트 초록 (#B8F5C8)
   - 🩷 연분홍 (#FFB8D9)
   - 🟣 라벤더 (#D4B8FF)
   - 🌈 무지개 도트 → 색상 자동 사이클 모드 (2초 간격으로 색상 전환)
   - 선택된 도트는 흰색 Border + 체크 아이콘 오버레이

2. **밝기 슬라이더**
   - 좌측: 🌑 아이콘 (최소 밝기)
   - 우측: ☀️ 아이콘 (최대 밝기)
   - Compose Slider 컴포넌트
   - 슬라이더 값에 따라 화면 전체 alpha 조절
   - 최솟값은 0.05f (완전 꺼짐 방지)

3. **사운드 버튼** (LazyRow 가로 스크롤)
   - 🌧️ 빗소리 (무료)
   - 🌊 파도 소리 (무료)
   - 🌲 숲 소리 (무료)
   - 🔥 모닥불 (PRO 잠금 🔒)
   - 🎵 자장가 (PRO 잠금 🔒)
   - 🎹 피아노 (PRO 잠금 🔒)
   - 🌬️ 바람 소리 (PRO 잠금 🔒)
   - 활성 버튼은 강조 스타일, 비활성 사운드 탭 시 재생 중지
   - PRO 잠금 버튼 탭 시 → PaywallBottomSheet 표시

4. **액션 버튼 행** (Row, 3개)
   - ⏱️ 타이머: 탭하면 TimerBottomSheet 열림
   - 🕐 시계 모드: 조명 영역에 시계 표시 토글
   - 💤 수면 모드: 터치 인터셉트 + 자동 밝기 감소 (5분에 걸쳐 70%→10%)

---

### 2. 사운드 탭 `SoundScreen`

- LazyVerticalGrid (columns = 2) 사운드 카드
- 각 카드: 큰 이모지 텍스트 + 이름 + 재생/정지 IconButton
- 재생 중인 사운드는 파형 애니메이션 (InfiniteTransition으로 3개 막대 높이 반복)
- PRO 전용 사운드는 잠금 오버레이 (Box + dimmed scrim + 🔒 아이콘)
- 하단 볼륨 슬라이더 (현재 재생 중인 사운드)
- 여러 사운드 동시 재생 지원 (믹서 기능, PRO)

---

### 3. 타이머 탭 `TimerScreen`

**타이머 설정:**
- 프리셋 버튼 Row: 15분 / 30분 / 1시간 / 2시간
- 커스텀 입력: WheelPicker 방식 (LazyColumn + snap scrolling)

**타이머 동작:**
- 남은 시간 크게 표시 (Text, fontSize = 64.sp)
- 원형 프로그레스 바 (Canvas drawArc)
- 타이머 종료 시 동작 선택 (RadioButton 그룹):
  - 앱 종료 (화면 끄기)
  - 밝기 서서히 감소 후 종료
  - 알림음 재생 후 종료

**수면 예약:**
- TimePickerDialog로 특정 시각 설정
- AlarmManager로 예약 (매일 반복 옵션)

---

### 4. 설정 탭 `SettingsScreen`

- **PRO 업그레이드** 배너 Card (무료 사용자 대상)
- 기본 색상 설정 (color dot Row)
- 기본 밝기 설정 (Slider)
- 화면 방향 고정 (DropdownMenu: 세로/가로/자동)
- 앱 시작 시 자동으로 마지막 설정 복원 (Switch)
- 언어 설정 (한국어/영어/일본어)
- 개인정보 처리방침 (외부 링크)
- 앱 버전 표시
- 리뷰 요청 버튼 (In-App Review API)

---

## 핵심 기능 상세 구현

### 화면 꺼짐 방지

```kotlin
// Activity 또는 Composable에서 처리
@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
```

### 색상 사이클 모드

```kotlin
// LaunchedEffect + delay로 2초 간격 색상 전환
// animateColorAsState로 부드러운 색상 전이
val cycleColors = listOf(
    Color(0xFFFFD6A0),
    Color(0xFFA8D8FF),
    Color(0xFFB8F5C8),
    Color(0xFFFFB8D9),
    Color(0xFFD4B8FF),
)

LaunchedEffect(isCycleMode) {
    if (isCycleMode) {
        while (true) {
            cycleIndex = (cycleIndex + 1) % cycleColors.size
            delay(2000)
        }
    }
}

val animatedColor by animateColorAsState(
    targetValue = cycleColors[cycleIndex],
    animationSpec = tween(durationMillis = 1000)
)
```

### Orb 발광 효과

```kotlin
// Canvas + drawIntoCanvas로 RadialGradient + BlurMaskFilter 구현
Canvas(modifier = Modifier.size(200.dp)) {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                shader = RadialGradientShader(
                    center = center,
                    radius = size.minDimension / 2,
                    colors = listOf(lightColor, lightColor.copy(alpha = 0f)),
                )
                maskFilter = BlurMaskFilter(80f, BlurMaskFilter.Blur.NORMAL)
            }
        }
        canvas.drawCircle(center, size.minDimension / 2, paint)
    }
}
```

### Orb 숨쉬기 + 이모지 떠다니기 애니메이션

```kotlin
val infiniteTransition = rememberInfiniteTransition()

// 숨쉬기: scale 1.0 ↔ 1.04, 4초 주기
val breatheScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
        animation = tween(2000, easing = EaseInOutSine),
        repeatMode = RepeatMode.Reverse
    )
)

// 떠다니기: translateY 0 ↔ -8dp, 6초 주기
val floatOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = -8f,
    animationSpec = infiniteRepeatable(
        animation = tween(3000, easing = EaseInOutSine),
        repeatMode = RepeatMode.Reverse
    )
)
```

### 오디오 재생 (백그라운드 지원)

```kotlin
// MediaPlayer를 Service로 래핑하여 백그라운드 재생
// Foreground Service + MediaSession 사용
// res/raw/ 에 사운드 파일 위치

class AudioService : Service() {
    private val mediaPlayers = mutableMapOf<SoundType, MediaPlayer>()

    fun playSound(type: SoundType) {
        val player = MediaPlayer.create(this, type.resId).apply {
            isLooping = true
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
        }
        player.start()
        mediaPlayers[type] = player
    }

    fun fadeOutAndStop(type: SoundType, durationMs: Long) {
        // ValueAnimator로 볼륨 0까지 페이드아웃 후 stop
    }
}
```

### 프리미엄 / 인앱 결제

```kotlin
// Google Play Billing Library
// 상품 ID:
// - "babylight_pro_lifetime" : 일회성 구매 (inapp)
// - "babylight_pro_monthly"  : 월 구독 (subs)

class BillingRepository @Inject constructor(
    private val billingClient: BillingClient,
    private val encryptedPrefs: EncryptedSharedPreferences
) {
    suspend fun queryProducts(): List<ProductDetails>
    suspend fun launchPurchaseFlow(activity: Activity, product: ProductDetails)
    suspend fun restorePurchases()
    fun isPro(): Boolean = encryptedPrefs.getBoolean("is_pro", false)
}
```

### 수면 모드

```kotlin
// Box 위에 입력 차단 오버레이 (pointerInput 소비)
// animateFloatAsState로 5분에 걸쳐 alpha 0.7f → 0.1f

var sleepModeEnabled by remember { mutableStateOf(false) }
val screenAlpha by animateFloatAsState(
    targetValue = if (sleepModeEnabled) 0.1f else brightnessState,
    animationSpec = tween(durationMillis = 5 * 60 * 1000) // 5분
)

// 수면 모드 해제: 볼륨 키 인터셉트 또는 3초 길게 누르기
if (sleepModeEnabled) {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { sleepModeEnabled = false }
                )
            }
    )
}
```

---

## 디자인 시스템

```kotlin
object AppColors {
    val Background = Color(0xFF0D0A14)   // 딥 다크 퍼플
    val Panel = Color(0xFF1A1625)        // 컨트롤 패널
    val Border = Color(0x1AFFFFFF)       // 테두리 (10% white)
    val TextPrimary = Color(0xFFF0E8FF)  // 기본 텍스트
    val TextDim = Color(0x73F0E8FF)      // 보조 텍스트 (45% white)

    val WarmYellow = Color(0xFFFFD6A0)
    val SkyBlue = Color(0xFFA8D8FF)
    val MintGreen = Color(0xFFB8F5C8)
    val SoftPink = Color(0xFFFFB8D9)
    val Lavender = Color(0xFFD4B8FF)
}

// 폰트: Google Fonts - Nunito
// implementation("androidx.compose.ui:ui-text-google-fonts")
val nunitoFamily = FontFamily(
    Font(GoogleFont("Nunito"), weight = FontWeight.Light),
    Font(GoogleFont("Nunito"), weight = FontWeight.Normal),
    Font(GoogleFont("Nunito"), weight = FontWeight.SemiBold),
    Font(GoogleFont("Nunito"), weight = FontWeight.Bold),
)

// 테마
val AppTheme = darkColorScheme(
    background = AppColors.Background,
    surface = AppColors.Panel,
    onBackground = AppColors.TextPrimary,
    onSurface = AppColors.TextPrimary,
)
```

---

## 프로젝트 구조

```
app/
└── src/main/
    ├── java/com/yourapp/babylight/
    │   ├── MainActivity.kt               # 단일 Activity
    │   │
    │   ├── ui/
    │   │   ├── theme/
    │   │   │   ├── AppTheme.kt
    │   │   │   ├── Color.kt
    │   │   │   └── Type.kt
    │   │   │
    │   │   ├── navigation/
    │   │   │   └── AppNavigation.kt      # NavHost + BottomBar
    │   │   │
    │   │   ├── screen/
    │   │   │   ├── light/
    │   │   │   │   ├── LightScreen.kt
    │   │   │   │   └── LightViewModel.kt
    │   │   │   ├── sound/
    │   │   │   │   ├── SoundScreen.kt
    │   │   │   │   └── SoundViewModel.kt
    │   │   │   ├── timer/
    │   │   │   │   ├── TimerScreen.kt
    │   │   │   │   └── TimerViewModel.kt
    │   │   │   └── settings/
    │   │   │       ├── SettingsScreen.kt
    │   │   │       └── SettingsViewModel.kt
    │   │   │
    │   │   └── component/
    │   │       ├── LightOrb.kt           # 발광 orb Canvas 컴포넌트
    │   │       ├── ColorPickerRow.kt     # 색상 선택 도트
    │   │       ├── BrightnessSlider.kt   # 밝기 슬라이더
    │   │       ├── SoundChip.kt          # 사운드 버튼
    │   │       ├── SoundCard.kt          # 사운드 탭 카드
    │   │       ├── WaveformAnimation.kt  # 파형 애니메이션
    │   │       ├── TimerArcProgress.kt   # 원형 타이머 프로그레스
    │   │       ├── WheelPicker.kt        # 드럼롤 시간 피커
    │   │       ├── ProBadgeOverlay.kt    # PRO 잠금 오버레이
    │   │       └── PaywallBottomSheet.kt # 결제 바텀시트
    │   │
    │   ├── service/
    │   │   └── AudioService.kt           # Foreground Service 오디오
    │   │
    │   ├── data/
    │   │   ├── repository/
    │   │   │   ├── SettingsRepository.kt # DataStore
    │   │   │   └── BillingRepository.kt  # Play Billing
    │   │   └── datastore/
    │   │       └── AppPreferences.kt
    │   │
    │   └── di/
    │       ├── AppModule.kt
    │       ├── AudioModule.kt
    │       └── BillingModule.kt
    │
    └── res/
        └── raw/
            ├── rain.mp3
            ├── wave.mp3
            ├── forest.mp3
            ├── fire.mp3        # PRO
            ├── lullaby.mp3     # PRO
            ├── piano.mp3       # PRO
            └── wind.mp3        # PRO
```

---

## build.gradle.kts (주요 의존성)

```kotlin
dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    kapt("com.google.dagger:hilt-compiler:2.51.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Security (EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:7.0.0")

    // In-App Review
    implementation("com.google.android.play:review-ktx:2.0.1")

    // Google Fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
}
```

---

## 개발 순서 (우선순위)

1. **프로젝트 초기화** — Hilt, Compose, Navigation 세팅
2. **디자인 시스템** — AppTheme, Color, Typography
3. **LightScreen** — 색상 + 밝기 + Orb Canvas 애니메이션
4. **화면 꺼짐 방지** — FLAG_KEEP_SCREEN_ON
5. **AudioService** — 루프 재생, 백그라운드 Foreground Service
6. **색상 사이클 모드** — LaunchedEffect + animateColorAsState
7. **수면 모드** — 터치 차단 + 자동 밝기 감소
8. **TimerScreen** — WheelPicker + Canvas Arc 프로그레스
9. **SoundScreen** — 그리드 카드 + 파형 애니메이션
10. **PRO 결제 플로우** — Billing Library + PaywallBottomSheet
11. **SettingsScreen** — DataStore 연동
12. **전체 QA** — 다크 테마, 접근성, 메모리 누수 점검

---

## 참고 레퍼런스

- 앱 스토어 원본: https://play.google.com/store/apps/details?id=com.aethiumapps.babylight
- 컬러 팔레트: 따뜻한 노랑 계열 기반, 배경은 딥 다크 퍼플 (#0D0A14)
- 폰트: Nunito (Google Fonts for Compose)
- 아키텍처 가이드: https://developer.android.com/topic/architecture
