# MoodLight — Play Store 스크린샷 프롬프트

Claude에게 붙여넣어 HTML 목업을 생성하고, PNG로 변환해 Play Console에 업로드하는 전체 워크플로우.

---

## 워크플로우 요약

```
1. 아래 프롬프트를 새 Claude 대화에 붙여넣기
2. 생성된 HTML을 .html 파일로 저장
3. HTML → PNG 변환 (아래 방법 중 선택)
4. Play Console 업로드 (최소 1080×1920 또는 320×568)
```

---

## HTML → PNG 변환 방법

### 방법 A — 브라우저 내장 캡처 (가장 간단)

1. HTML 파일을 Chrome에서 열기
2. `F12` → Console 탭에 아래 코드 입력:

```js
// html2canvas CDN 로드 후 캡처
const s = document.createElement('script');
s.src = 'https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js';
document.head.appendChild(s);
s.onload = () => {
  html2canvas(document.querySelector('.slide'), { scale: 1 }).then(c => {
    const a = document.createElement('a');
    a.download = 'screenshot.png';
    a.href = c.toDataURL('image/png');
    a.click();
  });
};
```

> `.slide`는 각 프롬프트에서 1080×1920 wrapper에 붙이는 클래스명. 프롬프트에 `class="slide"` 포함 명시.

### 방법 B — Playwright (고해상도, 권장)

```bash
npm install -g playwright
npx playwright screenshot --full-page screenshot1.html screenshot1.png
```

### 방법 C — Claude에게 직접 요청

HTML 생성 후 같은 대화에서:

```
이 HTML을 Playwright로 1080×1920 PNG로 캡처하는 Node.js 스크립트도 만들어줘.
파일명은 screenshot_01_main.png으로 저장해줘.
```

---

## 스크린샷 1 — 메인 야간등 화면

**컨셉**: "스마트폰이 완벽한 야간등이 됩니다"  
**메인 컬러**: 따뜻한 노랑 `#FFD6A0`

```
Create a single HTML file that renders a smartphone Play Store screenshot mockup.
Add class="slide" to the outermost wrapper div.

## Canvas
- Size: 1080px wide × 1920px tall (use transform: scale(0.4) on the wrapper so it
  fits the browser, but keep the internal dimensions at 1080×1920)
- Background: #0D0A14

## Phone frame
- Draw a rounded rectangle (radius 60px) with a subtle border: 2px solid
  rgba(255,255,255,0.15), no shadow
- Screen area fills the inside of the frame

## App UI to render (light tab)
- Full screen background color: #FFD6A0 (warm yellow) at 85% brightness —
  add a semi-transparent black overlay (rgba(0,0,0,0.15)) on top
- Center: a glowing orb — 260px diameter circle, color #FFD6A0,
  box-shadow: 0 0 80px 40px rgba(255,214,160,0.6), 0 0 160px 80px rgba(255,214,160,0.3)
- Inside orb: emoji "🌙" at 72px font-size, centered
- Top area: a small pill button "⏱ 30분" — background rgba(255,255,255,0.15),
  text #F0E8FF, font-size 14px, border-radius 20px, padding 6px 16px
- Bottom hint: upward chevron icon (↑) + text "밀어서 색상·밝기 조절"
  in #F0E8FF at 50% opacity, font-size 13px, bouncing animation (keyframe:
  translateY 0px → -6px → 0px, 1.5s infinite)
- Status bar (top): time "22:14" left, battery/signal icons right, text #F0E8FF

## Marketing overlay (below the phone frame)
- Large headline text: "스마트폰이\n완벽한 야간등이 됩니다"
  font-size 52px, font-weight 800, color #F0E8FF, line-height 1.25
  centered, with a subtle text-shadow: 0 2px 20px rgba(255,214,160,0.4)
- Sub-text below: "부드러운 조명으로 새벽 수유도 편안하게"
  font-size 24px, color rgba(240,232,255,0.6)

## Overall slide background
- Use a dark radial gradient:
  radial-gradient(ellipse at center top, #1A0E2A 0%, #0D0A14 60%)
- The phone frame sits centered, 80px from top of canvas
- Marketing text sits 40px below the phone frame

## Typography
- Use Google Font "Noto Sans KR" for all Korean text
- Import via @import url in <style>
```

---

## 스크린샷 2 — 수면 사운드 탭

**컨셉**: "자장가와 백색소음 — 화면을 꺼도 계속 재생됩니다"  
**메인 컬러**: 하늘 파랑 `#A8D8FF`

```
Create a single HTML file that renders a smartphone Play Store screenshot mockup.
Add class="slide" to the outermost wrapper div.

## Canvas
- Size: 1080px × 1920px (scale wrapper to 0.4 for preview)
- Slide background: radial-gradient(ellipse at 30% 40%, #0D1A2A 0%, #0D0A14 70%)

## Phone frame
- Rounded rectangle, border: 2px solid rgba(255,255,255,0.12)

## App UI — Sound tab
Bottom navigation bar at bottom of screen:
- 3 tabs: "💡 조명" | "🎵 사운드" (active, #A8D8FF underline) | "⚙️ 설정"
- Bar background: #1A1625, height 64px
- Active tab text: #A8D8FF, others: rgba(240,232,255,0.4)

Main content area (background #0D0A14):

Top sub-tab switcher (pill style):
- Background: #1A1625, border-radius: 30px
- Two pills: "🎵 자장가" (active — background #A8D8FF, text #0D0A14, font-weight 700)
  and "🌊 백색소음" (inactive — transparent, text rgba(240,232,255,0.5))
- Width: 400px total, centered, margin-top 24px

Section label: "자장가" in rgba(240,232,255,0.5), font-size 12px,
  letter-spacing 2px, uppercase, margin: 24px 0 12px 32px

Lullaby track cards (3 visible cards):
Each card: background #1A1625, border-radius 16px, border: 1px solid rgba(255,255,255,0.08)
padding 20px 24px, margin: 0 24px 12px, display flex align-items center
- Left: music note icon ♪ in a 44px circle (background rgba(168,216,255,0.15),
  color #A8D8FF)
- Center: track title (e.g. "Brahms Lullaby", "Twinkle Twinkle", "Baa Baa Black Sheep")
  in #F0E8FF 16px, subtitle "자장가" in rgba(240,232,255,0.4) 13px
- Right: play button ▶ for inactive, animated equalizer bars
  (3 bars, heights animating 8px↔24px, color #A8D8FF) for the first/active track

## Marketing overlay
Headline: "자장가와 백색소음\n수면 유도 사운드"
font-size 50px, weight 800, color #F0E8FF
Sub: "화면을 꺼도 계속 재생됩니다"
font-size 24px, color rgba(240,232,255,0.55)

## Typography: Google Font "Noto Sans KR"
```

---

## 스크린샷 3 — 수면 타이머

**컨셉**: "잠들면 알아서 꺼집니다"  
**메인 컬러**: 민트 그린 `#B8F5C8`

```
Create a single HTML file that renders a smartphone Play Store screenshot mockup.
Add class="slide" to the outermost wrapper div.

## Canvas
- Size: 1080px × 1920px (scale wrapper 0.4)
- Slide background: radial-gradient(ellipse at 60% 30%, #1A0A1A 0%, #0D0A14 65%)

## Phone frame
- Border: 2px solid rgba(255,255,255,0.12)

## App UI — Light tab with timer running
Background: full screen #B8F5C8 (mint green) with rgba(0,0,0,0.20) overlay

Timer display at top-center:
- A circular arc progress ring: 200px diameter SVG circle
  - Track circle: stroke rgba(255,255,255,0.15), stroke-width 8
  - Progress arc: stroke #B8F5C8, stroke-width 8, stroke-dasharray based on
    ~60% progress, stroke-linecap round
  - Inside the ring: "47:32" in #F0E8FF, font-size 28px, font-weight 700
  - Below the time: "남은 시간" in rgba(240,232,255,0.5), font-size 11px

Center: glowing orb 240px, color #B8F5C8,
  box-shadow: 0 0 80px 40px rgba(184,245,200,0.5)
  Inside: "👶" emoji 68px

Timer preset chips row below orb (show as selectable options):
Small horizontal scrollable row of rounded chips:
"15분" | "30분" | "1시간" (active, background #B8F5C8, text #0D0A14) | "2시간" | "3시간"
Inactive chips: background rgba(255,255,255,0.1), text rgba(240,232,255,0.6)
font-size 13px, padding 6px 14px, border-radius 20px

Status bar top: time "23:05", battery, text #F0E8FF

## Marketing overlay
Headline: "잠들면 알아서\n꺼집니다"
font-size 54px, weight 800, color #F0E8FF,
  text-shadow: 0 2px 24px rgba(184,245,200,0.35)
Sub: "타이머 종료 시 사운드 자동 정지 · 화면 자동 잠금"
font-size 22px, color rgba(240,232,255,0.55)

## Typography: Google Font "Noto Sans KR"
```

---

## 스크린샷 4 — 비주얼 패턴 7종 쇼케이스

**컨셉**: "7가지 비주얼 패턴"  
**메인 컬러**: 라벤더 `#D4B8FF`

```
Create a single HTML file that renders a smartphone Play Store screenshot mockup.
Add class="slide" to the outermost wrapper div.

## Canvas
- Size: 1080px × 1920px (scale wrapper 0.4)
- Slide background: #0D0A14

## Phone frame
- Border: 2px solid rgba(255,255,255,0.12)

## App UI — Bottom sheet (pattern selector) open over light screen
Main screen behind sheet: full-screen background #D4B8FF (lavender),
  rgba(0,0,0,0.25) overlay, glowing orb center

Bottom sheet overlay:
- Covers bottom 55% of screen
- Background: #1A1625, top border-radius 28px
- Top drag handle: 40px × 4px, background rgba(255,255,255,0.2), centered, margin-top 12px

Sheet title: "비주얼 패턴" — #F0E8FF, font-size 16px, font-weight 600,
  margin: 16px 0 16px 24px

Pattern grid: 4 columns, 2 rows
Each cell:
- Size ~(phone_width - 48px) / 4 wide, aspect 1:1
- Background: #0D0A14, border-radius 16px
- Border: 1px solid rgba(255,255,255,0.08)
- Selected cell (HEARTBEAT): border 2px solid #FFB8D9,
  background rgba(255,184,217,0.12)
- Icon: emoji or small illustration centered
- Label below icon: Korean name in rgba(240,232,255,0.7) 11px

7 cells content:
1. "없음" — plain circle icon
2. "별빛 ✨" — star dots
3. "촛불 🕯️" — flame shape
4. "파도 🌊" — sine wave lines
5. "눈송이 ❄️" — snowflake
6. "심장박동 ♥" — selected state, pulsing heart (CSS animation: scale 0.9→1.1)
7. "버블 🫧" — circle bubbles

## Marketing overlay
Headline: "7가지 비주얼 패턴"
font-size 56px, weight 800, color #F0E8FF
Sub: "별빛부터 심장박동까지, 오늘 밤 분위기는 직접 골라요"
font-size 22px, color rgba(240,232,255,0.55), max-width 700px

## Typography: Google Font "Noto Sans KR"
```

---

## 스크린샷 5 — 색상 & 아이콘 커스텀

**컨셉**: "나만의 무드로 완전히 커스텀"  
**메인 컬러**: 소프트 핑크 `#FFB8D9`

```
Create a single HTML file that renders a smartphone Play Store screenshot mockup.
Add class="slide" to the outermost wrapper div.

## Canvas
- Size: 1080px × 1920px (scale wrapper 0.4)
- Slide background: radial-gradient(ellipse at 50% 60%, #1A1025 0%, #0D0A14 70%)

## Phone frame
- Border: 2px solid rgba(255,255,255,0.12)

## App UI — Light screen with color control sheet open
Main screen: full background #FFB8D9 (soft pink), rgba(0,0,0,0.2) overlay
Center orb: 240px, color #FFB8D9, strong glow

Bottom control sheet (open, covers ~60% of screen):
Background: #1A1625, top radius 28px
Drag handle centered at top

Color picker row (ColorPickerRow):
- Row label: "색상" rgba(240,232,255,0.5) 12px
- 6 color swatches in a row (44px circles, 2px white border on selected):
  #FFD6A0 (selected, ring), #A8D8FF, #B8F5C8, #FFB8D9, #D4B8FF,
  last one: rainbow gradient circle with "+" icon (custom color button)
- Below: 2 recent custom color chips (small 32px circles) + label "최근 색상"

Brightness slider:
- Row label: "밝기" rgba(240,232,255,0.5) 12px
- Custom slider track: gradient from #1A1625 left to #FFB8D9 right
- Thumb: white circle 24px, positioned at 75%
- Left icon: dim sun, right icon: bright sun, both in rgba(240,232,255,0.4)

Center icon row:
- Row label: "중앙 아이콘" rgba(240,232,255,0.5) 12px
- 5 emoji buttons in a row (52px rounded squares, background rgba(255,255,255,0.06)):
  🌙 👶 🌟 🐑 🦋
- Active (🌙): background rgba(255,184,217,0.2), border 1.5px solid #FFB8D9
- Camera icon button at end: 52px square, dashed border rgba(255,255,255,0.2)

## Marketing overlay
Headline: "나만의 무드로\n완전히 커스텀"
font-size 52px, weight 800, color #F0E8FF,
  text-shadow: 0 2px 24px rgba(255,184,217,0.3)
Sub: "색상, 밝기, 아이콘 — 모든 것을 직접 설정하세요"
font-size 23px, color rgba(240,232,255,0.55)

## Typography: Google Font "Noto Sans KR"
```

---

## PNG 일괄 생성 스크립트 요청 프롬프트

5개 HTML 파일을 만든 뒤, 아래 프롬프트로 한 번에 PNG 변환 스크립트를 만들 수 있음:

```
다음 5개 HTML 파일을 각각 1080×1920 PNG로 캡처하는 Node.js 스크립트를 만들어줘.
Playwright를 사용하고, 각 파일의 .slide 요소를 캡처해줘.

입력 파일:
- screenshot_01_main.html → screenshot_01_main.png
- screenshot_02_sound.html → screenshot_02_sound.png
- screenshot_03_timer.html → screenshot_03_timer.png
- screenshot_04_patterns.html → screenshot_04_patterns.png
- screenshot_05_custom.html → screenshot_05_custom.png

조건:
- viewport: 1080×1920
- deviceScaleFactor: 2 (레티나 품질)
- 배경 흰색 없이 실제 배경색 유지
- 저장 경로: ./screenshots/ 폴더
```

---

## Play Console 업로드 스펙

| 항목 | 규격 |
|------|------|
| 형식 | JPEG 또는 24-bit PNG (투명도 없음) |
| 최소 해상도 | 320px (짧은 변) |
| 최대 해상도 | 3840px |
| 권장 비율 | 9:16 (1080×1920) |
| 최대 파일 크기 | 8MB |
| 최소 장수 | 2장 / 최대 8장 |

---

## 앱 디자인 컬러 레퍼런스

| 용도 | 컬러 | 값 |
|------|------|----|
| 배경 | Background | `#0D0A14` |
| 패널 | Panel | `#1A1625` |
| 텍스트 | TextPrimary | `#F0E8FF` |
| 텍스트 (흐림) | TextDim | `rgba(240,232,255,0.45)` |
| 따뜻한 노랑 | WarmYellow | `#FFD6A0` |
| 하늘 파랑 | SkyBlue | `#A8D8FF` |
| 민트 그린 | MintGreen | `#B8F5C8` |
| 소프트 핑크 | SoftPink | `#FFB8D9` |
| 라벤더 | Lavender | `#D4B8FF` |
