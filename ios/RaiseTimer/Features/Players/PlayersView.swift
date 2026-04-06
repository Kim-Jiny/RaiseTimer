import SwiftUI

struct PlayersView: View {
    @Environment(TournamentStore.self) private var store
    @State private var newName: String = ""
    @FocusState private var isNameFieldFocused: Bool

    var body: some View {
        let state = store.state
        let rebuyAllowed = state.config.rebuyAllowed
        let canAddPlayer = !newName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        let payouts = PayoutCalculator.calculate(state)
        NavigationStack {
            ZStack {
                RTTheme.feltGreenDark.ignoresSafeArea()
                VStack(spacing: 12) {
                    HStack {
                        StatCapsule(label: "총 엔트리", value: "\(state.totalBuyInsAndRebuys)")
                        StatCapsule(label: "상금", value: "\(formattedAmount(state.totalPrizePool))원")
                    }
                    .padding(.horizontal)

                    if state.isTournamentComplete {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("게임 종료 요약")
                                .font(.headline)
                                .foregroundStyle(.white)
                            Text("우승 \(state.winner?.name ?? "미정")")
                                .fontWeight(.bold)
                                .foregroundStyle(RTTheme.chipGold)
                            ForEach(state.finalStandings.prefix(3), id: \.id) { player in
                                let amount = player.placement.flatMap { place in
                                    payouts.indices.contains(place - 1) ? payouts[place - 1].amount : 0
                                } ?? 0
                                Text("\(player.placement ?? 0)위 \(player.name) · \(formattedAmount(amount))원")
                                    .font(.caption)
                                    .foregroundStyle(RTTheme.onSurfaceMuted)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                        .background(RTTheme.surfaceHighlight, in: RoundedRectangle(cornerRadius: 18))
                        .padding(.horizontal)
                    }

                    HStack {
                        TextField("이름 입력", text: $newName)
                            .textFieldStyle(.roundedBorder)
                            .submitLabel(.done)
                            .focused($isNameFieldFocused)
                            .onSubmit {
                                guard canAddPlayer else { return }
                                store.addPlayer(name: newName)
                                newName = ""
                                isNameFieldFocused = false
                            }
                        Button {
                            store.addPlayer(name: newName)
                            newName = ""
                            isNameFieldFocused = false
                        } label: {
                            Image(systemName: "plus.circle.fill")
                                .font(.title2)
                        }
                        .tint(RTTheme.chipGold)
                        .disabled(!canAddPlayer)
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
                            let idsToRemove = indexSet.map { state.players[$0].id }
                            idsToRemove.forEach { store.removePlayer(id: $0) }
                        }
                    }
                    .scrollContentBackground(.hidden)
                }
            }
            .dismissKeyboardOnTap()
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
                        .disabled(player.rebuyCount == 0)
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
