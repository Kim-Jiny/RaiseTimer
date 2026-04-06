import SwiftUI

struct StructureView: View {
    @Environment(TournamentStore.self) private var store
    @State private var presetName = ""

    var body: some View {
        let state = store.state
        let totalMinutes = state.config.levels.reduce(0) { $0 + $1.durationSeconds } / 60
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

                Section {
                    LabeledContent("레벨 수", value: "\(state.config.levels.count)개")
                    LabeledContent("예상 진행", value: "\(totalMinutes)분")
                }

                Section("설정 저장") {
                    TextField("설정 이름", text: $presetName)
                    Button("현재 설정 저장") {
                        store.saveCurrentBlindStructure(name: presetName)
                        presetName = ""
                    }
                    .disabled(presetName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                    if state.config.savedBlindStructures.isEmpty {
                        Text("저장된 설정이 없습니다.")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(state.config.savedBlindStructures.reversed()) { preset in
                            VStack(alignment: .leading, spacing: 10) {
                                Text(preset.name)
                                    .font(.headline)
                                Text("\(preset.levels.count)개 레벨 · \(preset.payoutPercents.count)명 분배")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                HStack {
                                    Button("불러오기") {
                                        store.loadBlindStructure(id: preset.id)
                                    }
                                    .buttonStyle(.borderedProminent)

                                    Button("삭제", role: .destructive) {
                                        store.deleteBlindStructure(id: preset.id)
                                    }
                                    .buttonStyle(.bordered)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }

                Section("블라인드 레벨") {
                    ForEach(Array(state.config.levels.enumerated()), id: \.element.id) { index, level in
                        LevelEditor(index: index, level: level)
                    }
                    .onDelete { indexSet in
                        indexSet.sorted(by: >).forEach { store.removeLevel(at: $0) }
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
            .dismissKeyboardOnTap()
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
