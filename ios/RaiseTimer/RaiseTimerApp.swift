import SwiftUI
import UIKit

final class AppDelegate: NSObject, UIApplicationDelegate {
    static var orientationLock: UIInterfaceOrientationMask = .portrait

    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        Self.orientationLock
    }
}

@main
struct RaiseTimerApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @State private var store = TournamentStore()
    @State private var turnTimerEngine = TurnTimerEngine()
    @Environment(\.scenePhase) private var scenePhase

    /// The screen should stay awake while either the tournament clock or the dealer turn
    /// timer is counting down.
    private var keepAwake: Bool {
        store.state.isRunning || turnTimerEngine.isRunning
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(store)
                .environment(turnTimerEngine)
                .onAppear {
                    // Apply initial screen-awake state (e.g. when restoring a running
                    // tournament across a cold launch).
                    UIApplication.shared.isIdleTimerDisabled = keepAwake
                }
                .onChange(of: store.state.isRunning) { _, _ in
                    // Keep the screen awake while the tournament clock OR the dealer turn
                    // timer is running so players can glance at it without having to tap.
                    // Reset automatically when both stop, or on app termination.
                    UIApplication.shared.isIdleTimerDisabled = keepAwake
                }
                .onChange(of: turnTimerEngine.isRunning) { _, _ in
                    UIApplication.shared.isIdleTimerDisabled = keepAwake
                }
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                // Re-sync the running timer against wall-clock time every time the app
                // returns to foreground (handles backgrounding, screen lock, etc.).
                store.catchUpFromBackground()
                // Re-apply keep-awake in case it was reset while backgrounded.
                UIApplication.shared.isIdleTimerDisabled = keepAwake
            } else if newPhase == .background {
                // Let the device sleep normally when the app is not visible.
                UIApplication.shared.isIdleTimerDisabled = false
            }
        }
    }
}
