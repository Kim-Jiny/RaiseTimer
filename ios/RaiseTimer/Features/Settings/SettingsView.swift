import PhotosUI
import SwiftUI
import UIKit

struct SettingsView: View {
    @Environment(TournamentStore.self) private var store
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var newTournamentName = ""
    @State private var slotsExpanded = false
    @State private var themeExpanded = true
    @State private var logoExpanded = false
    private let themeColumns = Array(repeating: GridItem(.flexible(), spacing: 10), count: 3)

    var body: some View {
        let state = store.state
        let _ = (RTTheme.currentPreset = state.config.themePreset)
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("빠른 설정")
                            .font(.headline.bold())
                            .foregroundStyle(RTTheme.onSurface)
                        Text("현재 사용 중인 핵심 설정만 먼저 보여줍니다.")
                            .font(.subheadline)
                            .foregroundStyle(RTTheme.onSurfaceMuted)
                        SettingSummaryRow(label: "현재 슬롯", value: store.currentTournamentName)
                        SettingSummaryRow(label: "테마", value: state.config.themePreset.title)
                        SettingSummaryRow(
                            label: "전체화면 로고",
                            value: state.config.fullscreenLogoFileName == nil ? "사용 안 함" : "적용됨"
                        )
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(RTTheme.surfaceElevated, in: RoundedRectangle(cornerRadius: 20))

                    SettingsDisclosureCard(
                        title: "테마 컬러",
                        summary: state.config.themePreset.title,
                        isExpanded: $themeExpanded
                    ) {
                        Text("앱 포인트 컬러와 타이머 분위기를 프리셋으로 바꿉니다.")
                            .font(.subheadline)
                            .foregroundStyle(RTTheme.onSurfaceMuted)

                        LazyVGrid(columns: themeColumns, spacing: 10) {
                            ForEach(ThemePreset.allCases, id: \.self) { preset in
                                ThemePresetCard(
                                    preset: preset,
                                    selected: state.config.themePreset == preset
                                ) {
                                    store.updateThemePreset(preset)
                                }
                            }
                        }
                    }

                    SettingsDisclosureCard(
                        title: "전체화면 로고",
                        summary: state.config.fullscreenLogoFileName == nil ? "사용 안 함" : "적용됨",
                        isExpanded: $logoExpanded
                    ) {
                        Text("선택한 이미지는 전체화면 타이머 배경에 은은하게 표시됩니다.")
                            .font(.subheadline)
                            .foregroundStyle(RTTheme.onSurfaceMuted)

                        LogoPreviewCard(fileName: state.config.fullscreenLogoFileName)

                        HStack(spacing: 10) {
                            PhotosPicker(selection: $selectedPhoto, matching: .images) {
                                Text(state.config.fullscreenLogoFileName == nil ? "로고 선택" : "로고 변경")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(RTTheme.chipGold)
                            .foregroundStyle(RTTheme.feltGreenDark)

                            Button("로고 제거") {
                                store.updateFullscreenLogoFileName(nil)
                            }
                            .buttonStyle(.bordered)
                            .disabled(state.config.fullscreenLogoFileName == nil)
                            .frame(maxWidth: .infinity)
                        }
                    }

                    SettingsDisclosureCard(
                        title: "토너먼트 슬롯",
                        summary: store.currentTournamentName,
                        isExpanded: $slotsExpanded
                    ) {
                        Text("현재 슬롯을 바꾸거나 새 슬롯을 만들어 여러 토너먼트를 관리합니다.")
                            .font(.subheadline)
                            .foregroundStyle(RTTheme.onSurfaceMuted)

                        HStack(spacing: 10) {
                            TextField("새 슬롯 이름", text: $newTournamentName)
                                .textFieldStyle(.roundedBorder)

                            Button("생성") {
                                store.createTournamentSlot(name: newTournamentName)
                                newTournamentName = ""
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(RTTheme.chipGold)
                            .foregroundStyle(RTTheme.feltGreenDark)
                            .disabled(newTournamentName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        }

                        ForEach(store.tournamentSlots) { slot in
                            VStack(alignment: .leading, spacing: 8) {
                                Text(slot.isCurrent ? "\(slot.name) (현재)" : slot.name)
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(RTTheme.onSurface)
                                Text("\(slot.activePlayerCount)/\(slot.playerCount)명 · \(slot.updatedAt.formatted(date: .numeric, time: .shortened))")
                                    .font(.caption)
                                    .foregroundStyle(RTTheme.onSurfaceMuted)
                                HStack(spacing: 10) {
                                    Button("불러오기") {
                                        store.switchTournamentSlot(id: slot.id)
                                    }
                                    .buttonStyle(.borderedProminent)
                                    .tint(RTTheme.chipGold)
                                    .foregroundStyle(RTTheme.feltGreenDark)
                                    .disabled(slot.isCurrent)

                                    Button("삭제", role: .destructive) {
                                        store.deleteTournamentSlot(id: slot.id)
                                    }
                                    .buttonStyle(.bordered)
                                    .disabled(store.tournamentSlots.count <= 1)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(12)
                            .background(RTTheme.surfaceHighlight, in: RoundedRectangle(cornerRadius: 16))
                        }
                    }
                }
                .padding()
            }
            .background(RTTheme.feltGreenDark.ignoresSafeArea())
            .navigationTitle("설정")
        }
        .task(id: selectedPhoto) {
            guard let selectedPhoto,
                  let data = try? await selectedPhoto.loadTransferable(type: Data.self)
            else { return }
            let fileName = LogoStore.saveImageData(data)
            store.updateFullscreenLogoFileName(fileName)
        }
    }
}

private struct SettingsDisclosureCard<Content: View>: View {
    let title: String
    let summary: String
    @Binding var isExpanded: Bool
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Button {
                isExpanded.toggle()
            } label: {
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(title)
                            .font(.headline.bold())
                            .foregroundStyle(RTTheme.onSurface)
                        Text(summary)
                            .font(.subheadline)
                            .foregroundStyle(RTTheme.onSurfaceMuted)
                    }
                    Spacer()
                    Text(isExpanded ? "접기" : "열기")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(RTTheme.chipGold)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if isExpanded {
                content
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(RTTheme.surfaceElevated, in: RoundedRectangle(cornerRadius: 20))
    }
}

private struct SettingSummaryRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .foregroundStyle(RTTheme.onSurfaceMuted)
            Spacer()
            Text(value)
                .fontWeight(.semibold)
                .foregroundStyle(RTTheme.onSurface)
        }
    }
}

private struct ThemePresetCard: View {
    let preset: ThemePreset
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 6) {
                    ForEach(previewColors, id: \.self) { color in
                        Circle()
                            .fill(color)
                            .frame(width: 18, height: 18)
                    }
                }
                Text(preset.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(RTTheme.onSurface)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(selected ? RTTheme.surfaceHighlight : RTTheme.surface, in: RoundedRectangle(cornerRadius: 16))
            .overlay {
                RoundedRectangle(cornerRadius: 16)
                    .stroke(selected ? RTTheme.chipGold : .clear, lineWidth: 2)
            }
        }
        .buttonStyle(.plain)
    }

    private var previewColors: [Color] {
        switch preset {
        case .emerald: return [Color(hex: 0x2B8A5A), Color(hex: 0xD4A017), Color(hex: 0x0F5132)]
        case .ocean: return [Color(hex: 0x3BA7D6), Color(hex: 0x67D3F4), Color(hex: 0x123A52)]
        case .ruby: return [Color(hex: 0xC94B73), Color(hex: 0xF07AA2), Color(hex: 0x5D203B)]
        case .sunset: return [Color(hex: 0xFF8A3D), Color(hex: 0xFFB36B), Color(hex: 0x6C3722)]
        case .lavender: return [Color(hex: 0xA884FF), Color(hex: 0xC7B2FF), Color(hex: 0x4B3570)]
        case .slate: return [Color(hex: 0x8FB3C9), Color(hex: 0xBED3E0), Color(hex: 0x2B4252)]
        case .mint: return [Color(hex: 0x4FD1B5), Color(hex: 0x8AE9D4), Color(hex: 0x1C5C50)]
        case .coral: return [Color(hex: 0xFF7A6B), Color(hex: 0xFFA597), Color(hex: 0x7D3B36)]
        case .midnight: return [Color(hex: 0x7CA6FF), Color(hex: 0xB5CBFF), Color(hex: 0x233A74)]
        }
    }
}

private struct LogoPreviewCard: View {
    let fileName: String?
    @State private var logoImage: UIImage?

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [RTTheme.feltGlow.opacity(0.22), RTTheme.feltGreenDark, RTTheme.feltGreen],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            if let image = logoImage {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .padding(24)
                    .opacity(0.16)
            }

            Text("00:45")
                .font(.system(size: 72, weight: .bold, design: .monospaced))
                .foregroundStyle(RTTheme.onSurface)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 180)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .task(id: fileName) {
            logoImage = await LogoStore.image(for: fileName)
        }
    }
}

private extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}
