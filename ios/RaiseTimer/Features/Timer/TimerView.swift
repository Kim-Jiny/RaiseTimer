import SwiftUI
import UIKit

struct TimerView: View {
    @Environment(TournamentStore.self) private var store
    @State private var fullscreenLogo: UIImage?
    var isFullscreen = false
    var onStartFullscreen: (() -> Void)? = nil
    var onToggleFullscreen: (() -> Void)? = nil

    var body: some View {
        let state = store.state
        ZStack {
            LinearGradient(
                colors: [RTTheme.feltGlow.opacity(0.22), RTTheme.feltGreenDark, RTTheme.feltGreen],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            Group {
                if isFullscreen {
                    fullscreenContent(state: state)
                } else {
                    ScrollView {
                        mainContent(state: state)
                    }
                }
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

    private func clockString(_ totalSeconds: Int) -> String {
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }

    @ViewBuilder
    private func mainContent(state: TournamentState) -> some View {
        VStack(spacing: 14) {
            LevelHeader(state: state, isFullscreen: false, onToggleFullscreen: onToggleFullscreen)
            TimerHero(state: state, isFullscreen: false)
            QuickStats(state: state)
            if state.isTournamentComplete {
                TournamentSummaryCard(state: state)
            }
            CurrentBlindsCard(level: state.currentLevel)

            if let next = state.nextLevel {
                NextBlindsCard(level: next)
            }

            Spacer(minLength: 16)
            controlSection(state: state)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
    }

    @ViewBuilder
    private func fullscreenContent(state: TournamentState) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                LevelPill(state: state)
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

            Spacer(minLength: 24)

            HStack(alignment: .center, spacing: 28) {
                VStack(alignment: .leading, spacing: 14) {
                    Text(state.isRunning ? "진행 중" : "준비됨")
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(RTTheme.chipGoldSoft)

                    Text(clockString(state.remainingSeconds))
                        .font(.system(size: 120, weight: .bold, design: .monospaced))
                        .foregroundStyle(RTTheme.onSurface)
                        .minimumScaleFactor(0.7)

                    Text(
                        state.currentLevel.isBreak
                        ? "Break"
                        : "Blinds \(chips(state.currentLevel.smallBlind)) / \(chips(state.currentLevel.bigBlind))"
                    )
                    .font(.title3.weight(.medium))
                    .foregroundStyle(RTTheme.onSurface)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(RTTheme.surfaceElevated.opacity(0.92), in: Capsule())
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if let next = state.nextLevel {
                    VStack(alignment: .trailing, spacing: 8) {
                        Text("다음 레벨")
                            .font(.headline)
                            .foregroundStyle(RTTheme.onSurfaceMuted)
                        Text(nextLabel(next))
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(RTTheme.onSurface)
                            .multilineTextAlignment(.trailing)
                    }
                }
            }

            Spacer()
        }
        .padding(.horizontal, 28)
        .padding(.vertical, 20)
        .background {
            if let image = fullscreenLogo {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .padding(32)
                    .opacity(0.12)
            }
        }
    }

    @ViewBuilder
    private func TimerHero(state: TournamentState, isFullscreen: Bool) -> some View {
        VStack(spacing: 10) {
            Text(state.isRunning ? "진행 중" : "준비됨")
                .font(.caption.weight(.semibold))
                .foregroundStyle(RTTheme.chipGoldSoft)

            Text(clockString(state.remainingSeconds))
                .font(.system(size: isFullscreen ? 112 : 96, weight: .bold, design: .monospaced))
                .foregroundStyle(RTTheme.onSurface)

            Text(
                state.currentLevel.isBreak
                ? "Break"
                : "Blinds \(chips(state.currentLevel.smallBlind)) / \(chips(state.currentLevel.bigBlind))"
            )
            .font(.subheadline.weight(.medium))
            .foregroundStyle(RTTheme.onSurface)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(RTTheme.surfaceHighlight, in: Capsule())
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, isFullscreen ? 34 : 24)
        .padding(.horizontal, 18)
        .background(RTTheme.surfaceElevated.opacity(0.96), in: RoundedRectangle(cornerRadius: 28))
    }

    @ViewBuilder
    private func controlSection(state: TournamentState) -> some View {
        HStack(spacing: 12) {
            RoundActionButton(systemName: "backward.fill", tint: RTTheme.surfaceHighlight) {
                store.previousLevel()
            }

            Button {
                if !state.isRunning && !isFullscreen, let onStartFullscreen {
                    onStartFullscreen()
                } else {
                    store.toggleRunning()
                }
            } label: {
                Label(
                    state.isRunning ? "일시정지" : "시작",
                    systemImage: state.isRunning ? "pause.fill" : "play.fill"
                )
                .font(.headline.bold())
                .frame(maxWidth: .infinity, minHeight: isFullscreen ? 64 : 58)
            }
            .buttonStyle(.borderedProminent)
            .tint(RTTheme.chipGold)
            .foregroundStyle(RTTheme.feltGreenDark)

            RoundActionButton(systemName: "forward.fill", tint: RTTheme.surfaceHighlight) {
                store.nextLevel()
            }
        }

        Button {
            store.reset()
        } label: {
            Label("레벨 처음부터 리셋", systemImage: "arrow.counterclockwise")
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .tint(RTTheme.surfaceElevated)
        .foregroundStyle(RTTheme.onSurface)
    }
}

private struct TournamentSummaryCard: View {
    let state: TournamentState

    var body: some View {
        let payouts = PayoutCalculator.calculate(state)
        VStack(alignment: .leading, spacing: 6) {
            Text("게임 종료 요약")
                .font(.headline)
                .foregroundStyle(.white)
            Text("우승 \(state.winner?.name ?? "미정")")
                .fontWeight(.bold)
                .foregroundStyle(RTTheme.chipGold)
            ForEach(Array(state.finalStandings.prefix(3)), id: \.id) { player in
                let place = player.placement ?? 0
                let amount: Int = {
                    let idx = place - 1
                    guard idx >= 0, payouts.indices.contains(idx) else { return 0 }
                    return payouts[idx].amount
                }()
                Text("\(place)위 \(player.name) · \(formatted(amount))원")
                    .font(.caption)
                    .foregroundStyle(RTTheme.onSurfaceMuted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(RTTheme.surfaceElevated.opacity(0.96), in: RoundedRectangle(cornerRadius: 20))
    }
}

private struct LevelPill: View {
    let state: TournamentState

    var body: some View {
        let level = state.currentLevel
        HStack(spacing: 10) {
            Circle()
                .fill(RTTheme.chipGoldSoft)
                .frame(width: 10, height: 10)
            Text(level.isBreak ? "휴식 시간" : "레벨 \(level.level)")
                .font(.headline.bold())
                .foregroundStyle(RTTheme.onSurface)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(RTTheme.surfaceElevated.opacity(0.92), in: Capsule())
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
            windowScene.keyWindow?.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
        } else {
            UIDevice.current.setValue(orientation.rawValue, forKey: "orientation")
            UIViewController.attemptRotationToDeviceOrientation()
        }
    }
}

private extension UIWindowScene {
    var keyWindow: UIWindow? {
        windows.first(where: \.isKeyWindow)
    }
}

private func nextLabel(_ level: BlindLevel) -> String {
    if level.isBreak { return "휴식 \(level.durationSeconds / 60)분" }
    let blinds = "\(chips(level.smallBlind)) / \(chips(level.bigBlind))"
    if level.ante > 0 { return "\(blinds) · 앤티 \(chips(level.ante))" }
    return blinds
}

private struct LevelHeader: View {
    let state: TournamentState
    let isFullscreen: Bool
    let onToggleFullscreen: (() -> Void)?

    var body: some View {
        HStack(spacing: 10) {
            Circle()
                .fill(RTTheme.chipGoldSoft)
                .frame(width: 10, height: 10)
            let level = state.currentLevel
            Text(level.isBreak ? "휴식 시간" : "레벨 \(level.level)")
                .font(.title2.bold())
                .foregroundStyle(RTTheme.onSurface)
            Spacer()
            if let onToggleFullscreen {
                Button(action: onToggleFullscreen) {
                    Image(systemName: isFullscreen ? "arrow.down.right.and.arrow.up.left" : "arrow.up.left.and.arrow.down.right")
                        .font(.headline.weight(.semibold))
                        .frame(width: 42, height: 42)
                        .background(RTTheme.surfaceHighlight, in: RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
                .foregroundStyle(RTTheme.onSurface)
            }
        }
    }
}

private struct QuickStats: View {
    let state: TournamentState

    var body: some View {
        HStack(spacing: 10) {
            InfoCard(label: "남은 인원", value: "\(state.activePlayers.count)/\(state.players.count)")
            InfoCard(label: "엔트리", value: "\(state.totalBuyInsAndRebuys)")
        }
    }
}

private struct InfoCard: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.caption)
                .foregroundStyle(RTTheme.onSurfaceMuted)
            Text(value)
                .font(.title3.bold())
                .foregroundStyle(RTTheme.onSurface)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RTTheme.surfaceHighlight.opacity(0.95), in: RoundedRectangle(cornerRadius: 20))
    }
}

private struct CurrentBlindsCard: View {
    let level: BlindLevel
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("현재 블라인드")
                .font(.caption)
                .foregroundStyle(RTTheme.onSurfaceMuted)
            Text(level.isBreak
                 ? "휴식"
                 : "\(chips(level.smallBlind)) / \(chips(level.bigBlind))")
                .font(.system(size: 32, weight: .semibold))
                .foregroundStyle(RTTheme.onSurface)
            if level.ante > 0 {
                Text("앤티 \(chips(level.ante))")
                    .foregroundStyle(RTTheme.onSurfaceMuted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(RTTheme.surfaceElevated, in: RoundedRectangle(cornerRadius: 22))
    }
}

private struct NextBlindsCard: View {
    let level: BlindLevel
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("다음 레벨")
                    .font(.caption)
                    .foregroundStyle(RTTheme.onSurfaceMuted)
                Text(nextLabel)
                    .foregroundStyle(RTTheme.onSurface)
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(RTTheme.surfaceHighlight, in: RoundedRectangle(cornerRadius: 20))
    }

    private var nextLabel: String {
        if level.isBreak { return "휴식 \(level.durationSeconds / 60)분" }
        let blinds = "\(chips(level.smallBlind)) / \(chips(level.bigBlind))"
        if level.ante > 0 { return "\(blinds) · 앤티 \(chips(level.ante))" }
        return blinds
    }
}

private struct RoundActionButton: View {
    let systemName: String
    let tint: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.title3.weight(.semibold))
                .frame(width: 56, height: 56)
        }
        .buttonStyle(.borderedProminent)
        .tint(tint)
        .foregroundStyle(RTTheme.onSurface)
    }
}

private func chips(_ value: Int) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    return formatter.string(from: NSNumber(value: value)) ?? "\(value)"
}

private func formatted(_ value: Int) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    return formatter.string(from: NSNumber(value: value)) ?? "\(value)"
}
