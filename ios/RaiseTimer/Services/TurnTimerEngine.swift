import Foundation
import Observation
import AudioToolbox
import UIKit

@Observable
final class TurnTimerEngine {
    private(set) var totalSeconds: Int = 30
    private(set) var remainingSeconds: Int = 30
    private(set) var isRunning: Bool = false
    private(set) var hasAlerted: Bool = false

    private var timer: Timer?

    var progress: Double {
        guard totalSeconds > 0 else { return 0 }
        return Double(remainingSeconds) / Double(totalSeconds)
    }

    func selectPreset(_ seconds: Int) {
        guard !isRunning else { return }
        totalSeconds = seconds
        remainingSeconds = seconds
        hasAlerted = false
    }

    func start() {
        if remainingSeconds == 0 {
            remainingSeconds = totalSeconds
            hasAlerted = false
        }
        isRunning = true
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self else { return }
            if self.remainingSeconds > 0 {
                self.remainingSeconds -= 1
                if self.remainingSeconds <= 5 && self.remainingSeconds > 0 {
                    AudioServicesPlaySystemSound(SystemSoundID(1104))
                }
                if self.remainingSeconds == 0 {
                    self.expire()
                }
            }
        }
    }

    func pause() {
        isRunning = false
        timer?.invalidate()
        timer = nil
    }

    func reset() {
        pause()
        remainingSeconds = totalSeconds
        hasAlerted = false
    }

    func toggle() {
        if isRunning { pause() } else { start() }
    }

    private func expire() {
        pause()
        hasAlerted = true
        AudioServicesPlaySystemSound(SystemSoundID(1005))
        let feedback = UINotificationFeedbackGenerator()
        feedback.notificationOccurred(.error)
    }
}
