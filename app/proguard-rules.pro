# Add project specific ProGuard rules here.

# ---------------------------------------------------------------------------
# SoundType enum
# DataStore에 enum 상수 이름을 문자열로 저장 (SoundType.entries.find { it.name == savedName })
# R8이 상수명을 난독화하면 저장된 값과 매칭 불가 → 이름 반드시 보존
# ---------------------------------------------------------------------------
-keepnames class com.odom.moodlight.data.model.SoundType

# ---------------------------------------------------------------------------
# Stack trace 가독성 (크래시 리포트용)
# ---------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
