import SwiftUI

struct StructureView: View {
    @Environment(TournamentStore.self) private var store

    var body: some View {
        let state = store.state
        NavigationStack {
            Form {
                Section("토너먼트 설정") {
                    NumberField(
                        label: "시작 스택",
                        value: Binding(
                            get: { state.config.startingStack },
                            set: { newValue in store.updateConfig { $0.startingStack = newValue } }
                        )
                    )
                    NumberField(
                        label: "바이인 (원)",
                        value: Binding(
                            get: { state.config.buyInAmount },
                            set: { newValue in store.updateConfig { $0.buyInAmount = newValue } }
                        )
                    )
                    NumberField(
                        label: "수수료 / 참가당 (원)",
                        value: Binding(
                            get: { state.config.feePerEntry },
                            set: { newValue in store.updateConfig { $0.feePerEntry = newValue } }
                        )
                    )
                    Toggle("리바이 허용", isOn: Binding(
                        get: { state.config.rebuyAllowed },
                        set: { newValue in store.updateConfig { $0.rebuyAllowed = newValue } }
                    ))
                }

                Section("블라인드 레벨") {
                    ForEach(Array(state.config.levels.enumerated()), id: \.element.id) { index, level in
                        LevelEditor(index: index, level: level)
                    }
                    .onDelete { indexSet in
                        indexSet.forEach { store.removeLevel(at: $0) }
                    }

                    Button {
                        store.addLevel()
                    } label: {
                        Label("레벨 추가", systemImage: "plus.circle.fill")
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(RTTheme.feltGreenDark)
            .navigationTitle("블라인드 구조")
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}

private struct LevelEditor: View {
    @Environment(TournamentStore.self) private var store
    let index: Int
    let level: BlindLevel

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(level.isBreak ? "휴식 \(index + 1)" : "레벨 \(level.level)")
                    .font(.headline)
                Spacer()
                Toggle("휴식", isOn: Binding(
                    get: { level.isBreak },
                    set: { isBreak in
                        var copy = level
                        copy.isBreak = isBreak
                        store.updateLevel(at: index, with: copy)
                    }
                ))
                .labelsHidden()
                Text("휴식")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            if !level.isBreak {
                HStack(spacing: 8) {
                    NumberField(label: "SB", value: Binding(
                        get: { level.smallBlind },
                        set: { newValue in
                            var copy = level; copy.smallBlind = newValue
                            store.updateLevel(at: index, with: copy)
                        }
                    ))
                    NumberField(label: "BB", value: Binding(
                        get: { level.bigBlind },
                        set: { newValue in
                            var copy = level; copy.bigBlind = newValue
                            store.updateLevel(at: index, with: copy)
                        }
                    ))
                    NumberField(label: "앤티", value: Binding(
                        get: { level.ante },
                        set: { newValue in
                            var copy = level; copy.ante = newValue
                            store.updateLevel(at: index, with: copy)
                        }
                    ))
                }
            }

            NumberField(label: "지속(초)", value: Binding(
                get: { level.durationSeconds },
                set: { newValue in
                    var copy = level; copy.durationSeconds = max(1, newValue)
                    store.updateLevel(at: index, with: copy)
                }
            ))
        }
        .padding(.vertical, 4)
    }
}

private struct NumberField: View {
    let label: String
    @Binding var value: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
            TextField(label, value: $value, format: .number)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
        }
    }
}
