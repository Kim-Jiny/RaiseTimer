import AVFoundation
import AudioToolbox
import UIKit

/// Plays the level-change alert on iOS. Looks for a bundled `level_change.wav` (or `.m4a`);
/// if absent, falls back to a system sound + notification haptic so the app still signals
/// transitions without shipping an audio asset.
final class SoundPlayer {
    private var audioPlayer: AVAudioPlayer?
    private let feedback = UINotificationFeedbackGenerator()

    init() {
        configureAudioSession()
        loadLevelChangeAsset()
    }

    private func configureAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            // Silently ignore — audio is best effort.
        }
    }

    private func loadLevelChangeAsset() {
        let exts = ["wav", "m4a", "mp3", "caf"]
        for ext in exts {
            if let url = Bundle.main.url(forResource: "level_change", withExtension: ext) {
                audioPlayer = try? AVAudioPlayer(contentsOf: url)
                audioPlayer?.prepareToPlay()
                return
            }
        }
    }

    func playLevelChange() {
        if let player = audioPlayer {
            player.currentTime = 0
            player.play()
        } else {
            AudioServicesPlaySystemSound(SystemSoundID(1005)) // short new-mail-like beep
        }
        feedback.notificationOccurred(.warning)
    }

    func playCountdownTick() {
        AudioServicesPlaySystemSound(SystemSoundID(1104)) // key click
    }
}
