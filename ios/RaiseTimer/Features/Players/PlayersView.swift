import SwiftUI

struct PlayersView: View {
    @Environment(TournamentStore.self) private var store
    @State private var newName: String = ""

    var body: some View {
        let state = store.state
        let rebuyAllowed = state.config.rebuyAllowed
        NavigationStack {
            ZStack {
                RTTheme.feltGreenDark.ignoresSafeArea()
                VStack(spacing: 12) {
                    HStack {
                        StatCapsule(label: "총 엔트리", value: "\(state.totalBuyInsAndRebuys)")
                        StatCapsule(label: "상금", value: "\(formattedAmount(state.totalPrizePool))원")
                    }
                    .padding(.horizontal)

                    HStack {
                        TextField("이름 입력", text: $newName)
                            .textFieldStyle(.roundedBorder)
                        Button {
                            store.addPlayer(name: newName)
                            newName = ""
                        } label: {
                            Image(systemName: "plus.circle.fill")
                                .font(.title2)
                        }
                        .tint(RTTheme.chipGold)
                    }
                    .padding(.horizontal)

                    List {
                        if state.players.isEmpty {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("아직 참가자가 없습니다")
                                    .foregroundStyle(.white)
                                Text("플레이어를 추가하면 탈락 관리와 상금 계산이 바로 연결됩니다.")
                                    .font(.caption)
                                    .foregroundStyle(RTTheme.onSurfaceMuted)
                            }
                            .padding(.vertical, 24)
                            .listRowBackground(RTTheme.surfaceHighlight)
                        }
                        ForEach(state.players) { player in
                            PlayerRow(player: player, rebuyAllowed: rebuyAllowed)
                                .listRowBackground(
                                    player.isEliminated ? RTTheme.surfaceElevated : RTTheme.surfaceHighlight
                                )
                        }
                        .onDelete { indexSet in
                            indexSet.forEach { idx in
                                store.removePlayer(id: state.players[idx].id)
                            }
                        }
                    }
                    .scrollContentBackground(.hidden)
                }
            }
            .navigationTitle("참가자 \(state.activePlayers.count)/\(state.players.count)")
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}

private struct StatCapsule: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(RTTheme.onSurfaceMuted)
            Text(value)
                .font(.headline.bold())
                .foregroundStyle(.white)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RTTheme.surfaceElevated, in: RoundedRectangle(cornerRadius: 18))
    }
}

private struct PlayerRow: View {
    @Environment(TournamentStore.self) private var store
    let player: Player
    let rebuyAllowed: Bool

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(player.name)
                    .font(.headline)
                    .foregroundStyle(.white)
                    .strikethrough(player.isEliminated)
                let meta: String = {
                    var parts: [String] = []
                    if player.rebuyCount > 0 { parts.append("리바이 \(player.rebuyCount)") }
                    if let place = player.placement { parts.append("\(place)위") }
                    return parts.joined(separator: " · ")
                }()
                if !meta.isEmpty {
                    Text(meta)
                        .font(.caption)
                        .foregroundStyle(RTTheme.onSurfaceMuted)
                }
            }

            Spacer()

            if rebuyAllowed {
                HStack(spacing: 4) {
                    Button {
                        store.adjustRebuy(id: player.id, delta: -1)
                    } label: { Image(systemName: "minus.circle") }
                        .buttonStyle(.borderless)
                    Text("\(player.rebuyCount)")
                        .foregroundStyle(.white)
                        .frame(minWidth: 16)
                    Button {
                        store.adjustRebuy(id: player.id, delta: +1)
                    } label: { Image(systemName: "plus.circle") }
                        .buttonStyle(.borderless)
                }
            }

            if player.isEliminated {
                Button {
                    store.revivePlayer(id: player.id)
                } label: {
                    Image(systemName: "arrow.uturn.backward")
                }
                .buttonStyle(.borderless)
                .tint(RTTheme.chipGold)
            } else {
                Button {
                    store.eliminatePlayer(id: player.id)
                } label: {
                    Image(systemName: "xmark.circle.fill")
                }
                .buttonStyle(.borderless)
                .tint(RTTheme.chipRed)
            }
        }
    }
}

private func formattedAmount(_ value: Int) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    return formatter.string(from: NSNumber(value: value)) ?? "\(value)"
}
