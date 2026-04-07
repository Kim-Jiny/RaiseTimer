import SwiftUI

struct StructureView: View {
    @Environment(TournamentStore.self) private var store
    @State private var presetName = ""

    var body: some View {
        let state = store.state
        let totalMinutes = state.config.levels.reduce(0) { $0 + $1.durationSeconds } / 60
        NavigationStack {
            Form {
                Section(String(localized: "structure_tournament_settings")) {
                    NumberField(
                        label: String(localized: "structure_starting_stack"),
                        value: Binding(
                            get: { state.config.startingStack },
                            set: { newValue in store.updateConfig { $0.startingStack = newValue } }
                        )
                    )
                    NumberField(
                        label: String(localized: "structure_buyin"),
                        value: Binding(
                            get: { state.config.buyInAmount },
                            set: { newValue in store.updateConfig { $0.buyInAmount = newValue } }
                        )
                    )
                    NumberField(
                        label: String(localized: "structure_fee"),
                        value: Binding(
                            get: { state.config.feePerEntry },
                            set: { newValue in store.updateConfig { $0.feePerEntry = newValue } }
                        )
                    )
                    Toggle(String(localized: "structure_rebuy_allowed"), isOn: Binding(
                        get: { state.config.rebuyAllowed },
                        set: { newValue in store.updateConfig { $0.rebuyAllowed = newValue } }
                    ))
                }

                Section {
                    LabeledContent(String(localized: "structure_level_count"), value: String(format: String(localized: "structure_level_count_format"), state.config.levels.count))
                    LabeledContent(String(localized: "structure_estimated_duration"), value: String(format: String(localized: "structure_minutes_format"), totalMinutes))
                }

                Section(String(localized: "structure_save_settings")) {
                    TextField(String(localized: "structure_settings_name"), text: $presetName)
                    Button(String(localized: "structure_save_current")) {
                        store.saveCurrentBlindStructure(name: presetName)
                        presetName = ""
                    }
                    .disabled(presetName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                    if state.config.savedBlindStructures.isEmpty {
                        Text(String(localized: "structure_no_saved"))
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(state.config.savedBlindStructures.reversed()) { preset in
                            VStack(alignment: .leading, spacing: 10) {
                                Text(preset.name)
                                    .font(.headline)
                                Text(String(format: String(localized: "structure_preset_detail_format"), preset.levels.count, preset.payoutPercents.count))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                HStack {
                                    Button(String(localized: "structure_load")) {
                                        store.loadBlindStructure(id: preset.id)
                                    }
                                    .buttonStyle(.borderedProminent)

                                    Button(String(localized: "structure_delete"), role: .destructive) {
                                        store.deleteBlindStructure(id: preset.id)
                                    }
                                    .buttonStyle(.bordered)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }

                Section(String(localized: "structure_blind_levels")) {
                    ForEach(Array(state.config.levels.enumerated()), id: \.element.id) { index, level in
                        LevelEditor(index: index, level: level)
                    }
                    .onDelete { indexSet in
                        indexSet.sorted(by: >).forEach { store.removeLevel(at: $0) }
                    }

                    Button {
                        store.addLevel()
                    } label: {
                        Label(String(localized: "structure_add_level"), systemImage: "plus.circle.fill")
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(RTTheme.feltGreenDark)
            .dismissKeyboardOnTap()
            .toolbar {
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button(String(localized: "common_done")) {
                        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    }
                }
            }
            .navigationTitle(String(localized: "structure_nav_title"))
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
                Text(level.isBreak ? String(format: String(localized: "structure_break_index_format"), index + 1) : String(format: String(localized: "timer_level_format"), level.level))
                    .font(.headline)
                Spacer()
                Toggle(String(localized: "structure_break_toggle"), isOn: Binding(
                    get: { level.isBreak },
                    set: { isBreak in
                        var copy = level
                        copy.isBreak = isBreak
                        store.updateLevel(at: index, with: copy)
                    }
                ))
                .labelsHidden()
                Text(String(localized: "structure_break_toggle"))
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
                    NumberField(label: String(localized: "structure_ante"), value: Binding(
                        get: { level.ante },
                        set: { newValue in
                            var copy = level; copy.ante = newValue
                            store.updateLevel(at: index, with: copy)
                        }
                    ))
                }
            }

            NumberField(label: String(localized: "structure_duration_seconds"), value: Binding(
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
