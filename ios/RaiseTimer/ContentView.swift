import SwiftUI
import UIKit

struct ContentView: View {
    @Environment(TournamentStore.self) private var store
    @State private var isTimerFullscreen = false

    var body: some View {
        let themePreset = store.state.config.themePreset
        let _ = (RTTheme.currentPreset = themePreset)
        TabView {
            TimerView(
                onStartFullscreen: {
                    store.start()
                    isTimerFullscreen = true
                },
                onToggleFullscreen: {
                    isTimerFullscreen = true
                }
            )
                .tabItem { Label("타이머", systemImage: "timer") }

            PlayersView()
                .tabItem { Label("플레이어", systemImage: "person.3.fill") }

            StructureView()
                .tabItem { Label("블라인드", systemImage: "slider.horizontal.3") }

            PayoutView()
                .tabItem { Label("상금", systemImage: "dollarsign.circle.fill") }

            SettingsView()
                .tabItem { Label("설정", systemImage: "paintpalette.fill") }
        }
        .id("theme-\(themePreset.rawValue)")
        .tint(RTTheme.color(.chipGold, preset: themePreset))
        .onChange(of: themePreset) { preset in
            RTTheme.currentPreset = preset
        }
        .fullScreenCover(isPresented: $isTimerFullscreen) {
            TimerView(
                isFullscreen: true,
                onToggleFullscreen: {
                    isTimerFullscreen = false
                }
            )
            .id("fullscreen-theme-\(themePreset.rawValue)")
            .environment(store)
            .statusBarHidden(true)
        }
    }
}

enum RTTheme {
    static var currentPreset: ThemePreset = .emerald

    enum Token {
        case feltGreen
        case feltGreenDark
        case feltGreenLight
        case feltGlow
        case chipGold
        case chipGoldSoft
        case chipRed
        case surface
        case surfaceElevated
        case surfaceHighlight
        case onSurface
        case onSurfaceMuted
    }

    static var feltGreen: Color { color(.feltGreen, preset: currentPreset) }
    static var feltGreenDark: Color { color(.feltGreenDark, preset: currentPreset) }
    static var feltGreenLight: Color { color(.feltGreenLight, preset: currentPreset) }
    static var feltGlow: Color { color(.feltGlow, preset: currentPreset) }
    static var chipGold: Color { color(.chipGold, preset: currentPreset) }
    static var chipGoldSoft: Color { color(.chipGoldSoft, preset: currentPreset) }
    static var chipRed: Color { adaptiveColor(light: 0xB71C1C, dark: 0xB71C1C) }
    static var surface: Color { color(.surface, preset: currentPreset) }
    static var surfaceElevated: Color { color(.surfaceElevated, preset: currentPreset) }
    static var surfaceHighlight: Color { color(.surfaceHighlight, preset: currentPreset) }
    static var onSurface: Color { color(.onSurface, preset: currentPreset) }
    static var onSurfaceMuted: Color { color(.onSurfaceMuted, preset: currentPreset) }

    static func color(_ token: Token, preset: ThemePreset) -> Color {
        let palette = palette(for: preset)
        switch token {
        case .feltGreen: return adaptiveColor(light: palette.lightBackground, dark: palette.darkEnd)
        case .feltGreenDark: return adaptiveColor(light: palette.lightMid, dark: palette.darkStart)
        case .feltGreenLight: return adaptiveColor(light: palette.primaryDark, dark: palette.primaryDark)
        case .feltGlow: return adaptiveColor(light: palette.lightGlow, dark: palette.darkGlow)
        case .chipGold: return adaptiveColor(light: palette.primaryDark, dark: palette.primary)
        case .chipGoldSoft: return adaptiveColor(light: palette.primary, dark: palette.primarySoft)
        case .chipRed: return adaptiveColor(light: 0xB71C1C, dark: 0xB71C1C)
        case .surface: return adaptiveColor(light: 0xF8FCF9, dark: palette.surface)
        case .surfaceElevated: return adaptiveColor(light: 0xFFFFFF, dark: palette.surfaceElevated)
        case .surfaceHighlight: return adaptiveColor(light: palette.lightCard, dark: palette.surfaceHighlight)
        case .onSurface: return adaptiveColor(light: 0x143224, dark: 0xE7F3EC)
        case .onSurfaceMuted: return adaptiveColor(light: 0x5A7265, dark: 0xA8C2B3)
        }
    }

    private static func palette(for preset: ThemePreset) -> ThemePaletteTokens {
        switch preset {
        case .emerald:
            return ThemePaletteTokens(
                primary: 0xD4A017,
                primarySoft: 0xF4D57E,
                primaryDark: 0x155239,
                darkGlow: 0x2B8A5A,
                darkStart: 0x0A3A23,
                darkEnd: 0x0F5132,
                surface: 0x0E2A1C,
                surfaceElevated: 0x163524,
                surfaceHighlight: 0x214A35,
                lightGlow: 0x52A67B,
                lightBackground: 0xEDF6F0,
                lightMid: 0xD7EBDD,
                lightCard: 0xE5F1E9
            )
        case .ocean:
            return ThemePaletteTokens(
                primary: 0x67D3F4,
                primarySoft: 0x9AE8FF,
                primaryDark: 0x123A52,
                darkGlow: 0x3BA7D6,
                darkStart: 0x0B2030,
                darkEnd: 0x123A52,
                surface: 0x102C3F,
                surfaceElevated: 0x153A50,
                surfaceHighlight: 0x1B4E6C,
                lightGlow: 0x55BBDD,
                lightBackground: 0xEAF8FD,
                lightMid: 0xD9EFF7,
                lightCard: 0xD6EDF8
            )
        case .ruby:
            return ThemePaletteTokens(
                primary: 0xF07AA2,
                primarySoft: 0xFFB7CB,
                primaryDark: 0x5D203B,
                darkGlow: 0xC94B73,
                darkStart: 0x2A1020,
                darkEnd: 0x5D203B,
                surface: 0x351425,
                surfaceElevated: 0x4A1D31,
                surfaceHighlight: 0x73304B,
                lightGlow: 0xD66A8F,
                lightBackground: 0xFCEEF2,
                lightMid: 0xF7DEE7,
                lightCard: 0xF5DCE6
            )
        case .sunset:
            return ThemePaletteTokens(
                primary: 0xFFFF8A3D,
                primarySoft: 0xFFFFB36B,
                primaryDark: 0xFF6C3722,
                darkGlow: 0xFFFF8A3D,
                darkStart: 0xFF2B1A10,
                darkEnd: 0xFF6C3722,
                surface: 0xFF4A2718,
                surfaceElevated: 0xFF5C3120,
                surfaceHighlight: 0xFF8E4B2D,
                lightGlow: 0xFFFFA45A,
                lightBackground: 0xFFFFF1E6,
                lightMid: 0xFFFFE2CC,
                lightCard: 0xFFFFE8D7
            )
        case .lavender:
            return ThemePaletteTokens(
                primary: 0xFFA884FF,
                primarySoft: 0xFFC7B2FF,
                primaryDark: 0xFF4B3570,
                darkGlow: 0xFFA884FF,
                darkStart: 0xFF21182F,
                darkEnd: 0xFF4B3570,
                surface: 0xFF34244F,
                surfaceElevated: 0xFF412D60,
                surfaceHighlight: 0xFF654A92,
                lightGlow: 0xFFB89EFF,
                lightBackground: 0xFFF4F0FF,
                lightMid: 0xFFE7DFFF,
                lightCard: 0xFFEEE7FF
            )
        case .slate:
            return ThemePaletteTokens(
                primary: 0xFF8FB3C9,
                primarySoft: 0xFFBED3E0,
                primaryDark: 0xFF2B4252,
                darkGlow: 0xFF8FB3C9,
                darkStart: 0xFF162028,
                darkEnd: 0xFF2B4252,
                surface: 0xFF22323E,
                surfaceElevated: 0xFF2B3F4D,
                surfaceHighlight: 0xFF3C5A70,
                lightGlow: 0xFFA7C4D6,
                lightBackground: 0xFFF1F6F9,
                lightMid: 0xFFDDE7EE,
                lightCard: 0xFFE8EFF4
            )
        case .mint:
            return ThemePaletteTokens(
                primary: 0xFF4FD1B5,
                primarySoft: 0xFF8AE9D4,
                primaryDark: 0xFF1C5C50,
                darkGlow: 0xFF4FD1B5,
                darkStart: 0xFF102A24,
                darkEnd: 0xFF1C5C50,
                surface: 0xFF17493F,
                surfaceElevated: 0xFF1F5C4F,
                surfaceHighlight: 0xFF28786A,
                lightGlow: 0xFF68DDC3,
                lightBackground: 0xFFECFCF8,
                lightMid: 0xFFD7F4ED,
                lightCard: 0xFFE3F8F1
            )
        case .coral:
            return ThemePaletteTokens(
                primary: 0xFFFF7A6B,
                primarySoft: 0xFFFFA597,
                primaryDark: 0xFF7D3B36,
                darkGlow: 0xFFFF7A6B,
                darkStart: 0xFF311B1A,
                darkEnd: 0xFF7D3B36,
                surface: 0xFF5F2D29,
                surfaceElevated: 0xFF74403A,
                surfaceHighlight: 0xFFA04E47,
                lightGlow: 0xFFFF8B7E,
                lightBackground: 0xFFFFF1EE,
                lightMid: 0xFFFADFD9,
                lightCard: 0xFFFCE8E3
            )
        case .midnight:
            return ThemePaletteTokens(
                primary: 0xFF7CA6FF,
                primarySoft: 0xFFB5CBFF,
                primaryDark: 0xFF233A74,
                darkGlow: 0xFF7CA6FF,
                darkStart: 0xFF0E1630,
                darkEnd: 0xFF233A74,
                surface: 0xFF1A2A57,
                surfaceElevated: 0xFF22356C,
                surfaceHighlight: 0xFF31539F,
                lightGlow: 0xFF96B8FF,
                lightBackground: 0xFFF1F5FF,
                lightMid: 0xFFDCE6FF,
                lightCard: 0xFFE7EEFF
            )
        }
    }

    private static func adaptiveColor(light: UInt32, dark: UInt32) -> Color {
        Color(
            UIColor { traits in
                UIColor(
                    rgb: traits.userInterfaceStyle == .dark ? dark : light
                )
            }
        )
    }

    private struct ThemePaletteTokens {
        let primary: UInt32
        let primarySoft: UInt32
        let primaryDark: UInt32
        let darkGlow: UInt32
        let darkStart: UInt32
        let darkEnd: UInt32
        let surface: UInt32
        let surfaceElevated: UInt32
        let surfaceHighlight: UInt32
        let lightGlow: UInt32
        let lightBackground: UInt32
        let lightMid: UInt32
        let lightCard: UInt32
    }
}

private extension UIColor {
    convenience init(rgb: UInt32) {
        let red = CGFloat((rgb >> 16) & 0xFF) / 255
        let green = CGFloat((rgb >> 8) & 0xFF) / 255
        let blue = CGFloat(rgb & 0xFF) / 255
        self.init(red: red, green: green, blue: blue, alpha: 1)
    }
}

extension View {
    func dismissKeyboardOnTap() -> some View {
        background(KeyboardDismissGestureView())
    }
}

private struct KeyboardDismissGestureView: UIViewRepresentable {
    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        view.backgroundColor = .clear
        view.isUserInteractionEnabled = false
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        let targetView = uiView.window
        if context.coordinator.attachedView !== targetView {
            context.coordinator.detach()
            context.coordinator.attach(to: targetView)
        }
    }

    static func dismantleUIView(_ uiView: UIView, coordinator: Coordinator) {
        coordinator.detach()
    }

    final class Coordinator: NSObject, UIGestureRecognizerDelegate {
        private lazy var recognizer: UITapGestureRecognizer = {
            let recognizer = UITapGestureRecognizer(target: self, action: #selector(handleTap))
            recognizer.cancelsTouchesInView = false
            recognizer.delegate = self
            return recognizer
        }()

        fileprivate weak var attachedView: UIView?

        func attach(to view: UIView?) {
            guard let view else { return }
            view.addGestureRecognizer(recognizer)
            attachedView = view
        }

        func detach() {
            attachedView?.removeGestureRecognizer(recognizer)
            attachedView = nil
        }

        @objc
        private func handleTap() {
            UIApplication.shared.sendAction(
                #selector(UIResponder.resignFirstResponder),
                to: nil,
                from: nil,
                for: nil
            )
        }

        func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
            !touchTargetsInteractiveControl(touch.view)
        }

        private func touchTargetsInteractiveControl(_ view: UIView?) -> Bool {
            var current = view
            while let candidate = current {
                if candidate is UIControl || candidate is UITextField || candidate is UITextView {
                    return true
                }
                current = candidate.superview
            }
            return false
        }
    }
}
