import SwiftUI

struct TimerView: View {
    @Environment(TournamentStore.self) private var store

    var body: some View {
        let state = store.state
        ZStack {
            RTTheme.feltGreenDark.ignoresSafeArea()
            VStack(spacing: 18) {
                LevelHeader(state: state)

                Text(clockString(state.remainingSeconds))
                    .font(.system(size: 108, weight: .bold, design: .monospaced))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity, minHeight: 180)

                CurrentBlindsCard(level: state.currentLevel)

                if let next = state.nextLevel {
                    NextBlindsCard(level: next)
                }

                Spacer(minLength: 0)

                HStack(spacing: 24) {
                    Button {
                        store.previousLevel()
                    } label: {
                        Image(systemName: "backward.fill")
                            .font(.title2)
                            .frame(width: 56, height: 56)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(RTTheme.feltGreen)

                    Button {
                        store.toggleRunning()
                    } label: {
                        Label(
                            state.isRunning ? "일시정지" : "시작",
                            systemImage: state.isRunning ? "pause.fill" : "play.fill"
                        )
                        .font(.title3.bold())
                        .frame(minWidth: 140, minHeight: 56)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(RTTheme.chipGold)
                    .foregroundStyle(RTTheme.feltGreenDark)

                    Button {
                        store.nextLevel()
                    } label: {
                        Image(systemName: "forward.fill")
                            .font(.title2)
                            .frame(width: 56, height: 56)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(RTTheme.feltGreen)
                }

                Button {
                    store.reset()
                } label: {
                    Label("리셋", systemImage: "arrow.counterclockwise")
                }
                .buttonStyle(.bordered)
                .tint(.white)

                Text("남은 인원 \(state.activePlayers.count) / \(state.players.count)")
                    .font(.callout)
                    .foregroundStyle(RTTheme.onSurfaceMuted)
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
    }

    private func clockString(_ totalSeconds: Int) -> String {
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
}

private struct LevelHeader: View {
    let state: TournamentState
    var body: some View {
        let level = state.currentLevel
        Text(level.isBreak ? "휴식 (Break)" : "레벨 \(level.level)")
            .font(.title2.bold())
            .foregroundStyle(RTTheme.chipGold)
    }
}

private struct CurrentBlindsCard: View {
    let level: BlindLevel
    var body: some View {
        VStack(spacing: 4) {
            Text(level.isBreak
                 ? "-"
                 : "\(chips(level.smallBlind)) / \(chips(level.bigBlind))")
                .font(.system(size: 34, weight: .semibold))
                .foregroundStyle(.white)
            if level.ante > 0 {
                Text("앤티 \(chips(level.ante))")
                    .foregroundStyle(RTTheme.onSurfaceMuted)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(RTTheme.surface, in: RoundedRectangle(cornerRadius: 16))
    }
}

private struct NextBlindsCard: View {
    let level: BlindLevel
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("다음")
                    .font(.caption)
                    .foregroundStyle(RTTheme.onSurfaceMuted)
                Text(nextLabel)
                    .foregroundStyle(.white)
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(RTTheme.feltGreen, in: RoundedRectangle(cornerRadius: 12))
    }

    private var nextLabel: String {
        if level.isBreak { return "휴식 \(level.durationSeconds / 60)분" }
        let blinds = "\(chips(level.smallBlind)) / \(chips(level.bigBlind))"
        if level.ante > 0 { return "\(blinds) · 앤티 \(chips(level.ante))" }
        return blinds
    }
}

private func chips(_ value: Int) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    return formatter.string(from: NSNumber(value: value)) ?? "\(value)"
}
