import SwiftUI

enum SetupTab: String, CaseIterable {
    case players
    case structure

    var label: String {
        switch self {
        case .players: return String(localized: "tab_players")
        case .structure: return String(localized: "tab_structure")
        }
    }
}

struct TournamentSetupView: View {
    @State private var selectedTab: SetupTab = .players

    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $selectedTab) {
                ForEach(SetupTab.allCases, id: \.self) { tab in
                    Text(tab.label).tag(tab)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal)
            .padding(.top, 8)
            .padding(.bottom, 4)
            .background(RTTheme.feltGreenDark)

            switch selectedTab {
            case .players:
                PlayersView()
            case .structure:
                StructureView()
            }
        }
    }
}
