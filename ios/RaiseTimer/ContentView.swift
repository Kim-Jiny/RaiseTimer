import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            TimerView()
                .tabItem { Label("타이머", systemImage: "timer") }

            PlayersView()
                .tabItem { Label("플레이어", systemImage: "person.3.fill") }

            StructureView()
                .tabItem { Label("블라인드", systemImage: "slider.horizontal.3") }

            PayoutView()
                .tabItem { Label("상금", systemImage: "dollarsign.circle.fill") }
        }
        .tint(RTTheme.chipGold)
    }
}

enum RTTheme {
    static let feltGreen = Color(red: 0.06, green: 0.32, blue: 0.20)
    static let feltGreenDark = Color(red: 0.04, green: 0.23, blue: 0.14)
    static let feltGreenLight = Color(red: 0.11, green: 0.42, blue: 0.27)
    static let chipGold = Color(red: 0.83, green: 0.63, blue: 0.09)
    static let chipRed = Color(red: 0.72, green: 0.11, blue: 0.11)
    static let surface = Color(red: 0.05, green: 0.16, blue: 0.11)
    static let onSurfaceMuted = Color(red: 0.66, green: 0.76, blue: 0.70)
}
