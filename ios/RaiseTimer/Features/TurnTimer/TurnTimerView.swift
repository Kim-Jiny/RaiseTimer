import SwiftUI
import UIKit

struct TurnTimerView: View {
    @Environment(TurnTimerEngine.self) private var engine
    @Environment(TournamentStore.self) private var store
    @State private var fullscreenLogo: UIImage?
    var isFullscreen = false
    var onToggleFullscreen: (() -> Void)? = nil

    private let presets = [15, 30, 45, 60, 90, 120]
    @State private var timeChipText: String = ""
    @FocusState private var timeChipFocused: Bool

    private var timeChipSeconds: Int {
        store.state.config.turnTimeChipSeconds
    }

    var body: some View {
        Group {
            if isFullscreen {
                fullscreenContent
            } else {
                normalContent
            }
        }
        .onAppear {
            guard isFullscreen else { return }
            OrientationLock.lockLandscape()
        }
        .onDisappear {
            guard isFullscreen else { return }
            OrientationLock.lockPortrait()
        }
        .task(id: store.state.config.fullscreenLogoFileName) {
            fullscreenLogo = await LogoStore.image(for: store.state.config.fullscreenLogoFileName)
        }
    }

    // MARK: - Normal (portrait) content

    private var normalContent: some View {
        NavigationStack {
            ZStack {
                RTTheme.feltGreenDark.ignoresSafeArea()

                VStack(spacing: 24) {
                    presetButtons

                    Spacer()

                    timerRing(size: 300, fontSize: 72)

                    Spacer()

                    timeChipField

                    controlButtons(compact: false)
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 16)
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        onToggleFullscreen?()
                    } label: {
                        Image(systemName: "arrow.up.left.and.arrow.down.right")
                            .font(.headline.weight(.semibold))
                            .frame(width: 42, height: 42)
                            .background(RTTheme.surfaceHighlight, in: RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(RTTheme.onSurface)
                }
            }
            .navigationTitle(String(localized: "turn_timer_title"))
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }

    // MARK: - Fullscreen (landscape) content

    private var fullscreenContent: some View {
        ZStack {
            LinearGradient(
                colors: [RTTheme.feltGlow.opacity(0.22), RTTheme.feltGreenDark, RTTheme.feltGreen],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            // Logo background
            if let image = fullscreenLogo {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .padding(32)
                    .opacity(0.12)
            }

            HStack(spacing: 28) {
                // Left: timer ring
                timerRing(size: 260, fontSize: 64)

                // Right: controls
                VStack(spacing: 16) {
                    // Preset chips (horizontal wrap)
                    HStack(spacing: 8) {
                        ForEach(presets, id: \.self) { seconds in
                            Button {
                                engine.selectPreset(seconds)
                            } label: {
                                Text(formatPresetLabel(seconds))
                                    .font(.caption.bold())
                                    .foregroundStyle(engine.totalSeconds == seconds ? RTTheme.feltGreenDark : .white)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(
                                        engine.totalSeconds == seconds ? RTTheme.chipGold : RTTheme.surfaceHighlight,
                                        in: Capsule()
                                    )
                            }
                            .disabled(engine.isRunning)
                        }
                    }

                    Spacer()

                    controlButtons(compact: true)
                }
                .frame(maxWidth: .infinity)
            }
            .padding(.horizontal, 28)
            .padding(.vertical, 20)

            // Close button (top-right)
            VStack {
                HStack {
                    Spacer()
                    if let onToggleFullscreen {
                        Button(action: onToggleFullscreen) {
                            Image(systemName: "xmark")
                                .font(.headline.weight(.bold))
                                .frame(width: 42, height: 42)
                                .background(RTTheme.surfaceElevated.opacity(0.92), in: RoundedRectangle(cornerRadius: 14))
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(RTTheme.onSurface)
                    }
                }
                Spacer()
            }
            .padding(.horizontal, 28)
            .padding(.vertical, 20)
        }
    }

    // MARK: - Shared Components

    private var timeChipField: some View {
        HStack(spacing: 12) {
            Text(String(localized: "turn_timer_time_chip"))
                .font(.subheadline.bold())
                .foregroundStyle(.white)

            TextField("", text: $timeChipText)
                .keyboardType(.numberPad)
                .focused($timeChipFocused)
                .multilineTextAlignment(.center)
                .font(.headline.bold())
                .foregroundStyle(RTTheme.feltGreenDark)
                .frame(width: 70, height: 36)
                .background(RTTheme.chipGold, in: RoundedRectangle(cornerRadius: 10))
                .onAppear {
                    if timeChipText.isEmpty { timeChipText = String(timeChipSeconds) }
                }
                .onChange(of: timeChipSeconds) { _, newValue in
                    // Re-sync when the underlying value changes (e.g. switching tournament
                    // slots), unless the user is actively editing. Prevents a stale field
                    // value from being committed back over another slot's setting.
                    if !timeChipFocused { timeChipText = String(newValue) }
                }
                .onChange(of: timeChipFocused) { _, focused in
                    if !focused { commitTimeChip() }
                }
                .onSubmit { commitTimeChip() }

            Text(String(localized: "turn_timer_seconds_unit"))
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.85))

            Spacer()
        }
        .padding(.horizontal, 4)
    }

    private func commitTimeChip() {
        let trimmed = timeChipText.trimmingCharacters(in: .whitespaces)
        let parsed = Int(trimmed) ?? timeChipSeconds
        let clamped = max(1, min(parsed, 600))
        store.updateConfig { $0.turnTimeChipSeconds = clamped }
        timeChipText = String(clamped)
    }

    private var presetButtons: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(presets, id: \.self) { seconds in
                    Button {
                        engine.selectPreset(seconds)
                    } label: {
                        Text(formatPresetLabel(seconds))
                            .font(.subheadline.bold())
                            .foregroundStyle(engine.totalSeconds == seconds ? RTTheme.feltGreenDark : .white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 10)
                            .background(
                                engine.totalSeconds == seconds ? RTTheme.chipGold : RTTheme.surfaceHighlight,
                                in: Capsule()
                            )
                    }
                    .disabled(engine.isRunning)
                }
            }
            .padding(.horizontal, 4)
        }
    }

    private func timerRing(size: CGFloat, fontSize: CGFloat) -> some View {
        ZStack {
            Circle()
                .stroke(RTTheme.surfaceHighlight, lineWidth: 12)

            Circle()
                .trim(from: 0, to: CGFloat(engine.progress))
                .stroke(
                    progressColor,
                    style: StrokeStyle(lineWidth: 12, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .animation(.linear(duration: 0.3), value: engine.remainingSeconds)

            VStack(spacing: 8) {
                Text(formatTime(engine.remainingSeconds))
                    .font(.system(size: fontSize, weight: .bold, design: .monospaced))
                    .foregroundStyle(progressColor)
                    .minimumScaleFactor(0.7)

                if engine.remainingSeconds == 0 && engine.hasAlerted {
                    Text(String(localized: "turn_timer_times_up"))
                        .font(.title3.bold())
                        .foregroundStyle(RTTheme.chipRed)
                }
            }
        }
        .frame(maxWidth: size, maxHeight: size)
    }

    private func controlButtons(compact: Bool) -> some View {
        HStack(spacing: compact ? 12 : 20) {
            Button {
                engine.reset()
            } label: {
                Label(String(localized: "turn_timer_reset"), systemImage: "arrow.counterclockwise")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, compact ? 12 : 16)
                    .background(RTTheme.surfaceHighlight, in: RoundedRectangle(cornerRadius: 16))
            }

            Button {
                engine.addTime(timeChipSeconds)
            } label: {
                Label(
                    String(format: String(localized: "turn_timer_add_time"), timeChipSeconds),
                    systemImage: "plus.circle.fill"
                )
                .font(.headline)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, compact ? 12 : 16)
                .background(RTTheme.feltGreen, in: RoundedRectangle(cornerRadius: 16))
            }

            Button {
                engine.toggle()
            } label: {
                Label(
                    engine.isRunning ? String(localized: "turn_timer_pause") : String(localized: "turn_timer_start"),
                    systemImage: engine.isRunning ? "pause.fill" : "play.fill"
                )
                .font(.headline)
                .foregroundStyle(RTTheme.feltGreenDark)
                .frame(maxWidth: .infinity)
                .padding(.vertical, compact ? 12 : 16)
                .background(RTTheme.chipGold, in: RoundedRectangle(cornerRadius: 16))
            }
            .disabled(engine.remainingSeconds == 0 && !engine.hasAlerted)
        }
    }

    // MARK: - Helpers

    private var progressColor: Color {
        let p = engine.progress
        if engine.remainingSeconds == 0 { return RTTheme.chipRed }
        if p <= 0.2 { return RTTheme.chipRed }
        if p <= 0.4 { return RTTheme.chipGold }
        return RTTheme.chipGoldSoft
    }

    private func formatTime(_ seconds: Int) -> String {
        let m = seconds / 60
        let s = seconds % 60
        return String(format: "%d:%02d", m, s)
    }

    private func formatPresetLabel(_ seconds: Int) -> String {
        if seconds >= 60 {
            let m = seconds / 60
            let s = seconds % 60
            return s > 0 ? "\(m)m \(s)s" : "\(m)m"
        }
        return "\(seconds)s"
    }
}

private enum OrientationLock {
    static func lockLandscape() {
        AppDelegate.orientationLock = [.landscapeLeft, .landscapeRight]
        rotate(to: .landscapeRight)
    }

    static func lockPortrait() {
        AppDelegate.orientationLock = .portrait
        rotate(to: .portrait)
    }

    private static func rotate(to orientation: UIInterfaceOrientation) {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene else {
            UIDevice.current.setValue(orientation.rawValue, forKey: "orientation")
            UIViewController.attemptRotationToDeviceOrientation()
            return
        }

        if #available(iOS 16.0, *) {
            let preferences = UIWindowScene.GeometryPreferences.iOS(
                interfaceOrientations: orientation.isLandscape ? [.landscapeLeft, .landscapeRight] : .portrait
            )
            try? windowScene.requestGeometryUpdate(preferences)
            windowScene.windows.first(where: \.isKeyWindow)?
                .rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
        } else {
            UIDevice.current.setValue(orientation.rawValue, forKey: "orientation")
            UIViewController.attemptRotationToDeviceOrientation()
        }
    }
}
