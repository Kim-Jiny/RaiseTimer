# RaiseTimer

홀덤 토너먼트용 블라인드 타이머 앱. Android (Kotlin + Jetpack Compose) 와 iOS (SwiftUI) 네이티브 앱으로 제공됩니다.

- **번들 ID**: `com.jiny.raisetimer` (양 플랫폼 공통)
- **기능**: 블라인드 레벨 타이머 · 플레이어 관리 · 칩 스택/프라이즈 계산 · 레벨 전환 사운드/햅틱

## 디렉토리

```
android/   — Android Studio 프로젝트 (Kotlin, Compose)
ios/       — SwiftUI 소스 + xcodegen 프로젝트 설정
```

## Android 빌드

요구사항: JDK 17, Android SDK (API 34), Gradle 8.7+ 또는 Android Studio Iguana+

```bash
cd android
./gradlew assembleDebug
./gradlew installDebug        # 연결된 기기/에뮬레이터에 설치
```

`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`는 저장소에 포함되어 있어 별도 설치 없이 바로 빌드할 수 있습니다 (Gradle 8.9 기준).

Android Studio에서 `android/` 폴더를 열어도 됩니다.

## iOS 빌드

요구사항: Xcode 15+, macOS, Homebrew

```bash
cd ios
brew install xcodegen         # 최초 1회
xcodegen generate             # project.yml → RaiseTimer.xcodeproj 생성
open RaiseTimer.xcodeproj
```

Xcode에서 iPhone 시뮬레이터로 Run.

## iOS 에디터 오류에 대한 참고

`ios/RaiseTimer/*.swift` 파일을 Xcode 없이 열면 `Cannot find 'TournamentStore' in scope` 등 **SourceKit 오류**가 보일 수 있습니다. 이는 아직 `.xcodeproj`가 생성되지 않아 파일들이 같은 모듈에 묶이지 않았기 때문이며, `xcodegen generate` 후 Xcode에서 빌드하면 모두 해결됩니다.

## 사운드 리소스

레벨 전환 알림음은 플레이스홀더로 비워 두었습니다. 실제 사용 시 다음 위치에 오디오 파일을 추가하세요:

- Android: `android/app/src/main/res/raw/level_change.ogg`
- iOS: `ios/RaiseTimer/Resources/level_change.wav` (또는 `.m4a`)

파일이 없어도 앱은 동작합니다 — 이 경우 시스템 beep/햅틱만 울립니다.
