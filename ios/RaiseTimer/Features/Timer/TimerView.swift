import SwiftUI

struct TimerView: View {
    @Environment(TournamentStore.self) private var store

    var body: some View {
        let state = store.state
        ZStack {
            LinearGradient(
                colors: [RTTheme.feltGlow.opacity(0.22), RTTheme.feltGreenDark, RTTheme.feltGreen],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 14) {
                LevelHeader(state: state)
                TimerHero(state: state)
                QuickStats(state: state)
                CurrentBlindsCard(level: state.currentLevel)

                if let next = state.nextLevel {
                    NextBlindsCard(level: next)
                }

                Spacer(minLength: 0)

                HStack(spacing: 12) {
                    RoundActionButton(systemName: "backward.fill", tint: RTTheme.surfaceHighlight) {
                        store.previousLevel()
                    }

                    Button {
                        store.toggleRunning()
                    } label: {
                        Label(
                            state.isRunning ? "일시정지" : "시작",
                            systemImage: state.isRunning ? "pause.fill" : "play.fill"
                        )
                        .font(.headline.bold())
                        .frame(maxWidth: .infinity, minHeight: 58)
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
                .foregroundStyle(.white)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
    }

    private func clockString(_ totalSeconds: Int) -> String {
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }

    @ViewBuilder
    private func TimerHero(state: TournamentState) -> some View {
        VStack(spacing: 10) {
            Text(state.isRunning ? "진행 중" : "준비됨")
                .font(.caption.weight(.semibold))
                .foregroundStyle(RTTheme.chipGoldSoft)

            Text(clockString(state.remainingSeconds))
                .font(.system(size: 96, weight: .bold, design: .monospaced))
                .foregroundStyle(.white)

            Text(
                state.currentLevel.isBreak
                ? "Break"
                : "Blinds \(chips(state.currentLevel.smallBlind)) / \(chips(state.currentLevel.bigBlind))"
            )
            .font(.subheadline.weight(.medium))
            .foregroundStyle(.white)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(RTTheme.surfaceHighlight, in: Capsule())
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
        .padding(.horizontal, 18)
        .background(RTTheme.surfaceElevated.opacity(0.96), in: RoundedRectangle(cornerRadius: 28))
    }
}

private struct LevelHeader: View {
    let state: TournamentState
    var body: some View {
        HStack(spacing: 10) {
            Circle()
                .fill(RTTheme.chipGoldSoft)
                .frame(width: 10, height: 10)
            let level = state.currentLevel
            Text(level.isBreak ? "휴식 시간" : "레벨 \(level.level)")
                .font(.title2.bold())
                .foregroundStyle(.white)
            Spacer()
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
                .foregroundStyle(.white)
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
                .foregroundStyle(.white)
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
                    .foregroundStyle(.white)
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
        .foregroundStyle(.white)
    }
}

private func chips(_ value: Int) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    return formatter.string(from: NSNumber(value: value)) ?? "\(value)"
}
