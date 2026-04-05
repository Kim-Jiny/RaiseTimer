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
                        ForEach(state.players) { player in
                            PlayerRow(player: player, rebuyAllowed: rebuyAllowed)
                                .listRowBackground(
                                    player.isEliminated ? RTTheme.surface : RTTheme.feltGreen
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
