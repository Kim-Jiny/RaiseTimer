import Foundation
import UIKit

enum LogoStore {
    private static let directoryName = "Logos"
    private static let fileName = "fullscreen_logo.png"

    static func saveImageData(_ data: Data) -> String? {
        guard let image = UIImage(data: data),
              let compressed = image.resizedForLogo(maxDimension: 1200)?.pngData() else {
            return nil
        }

        let url = logoURL()
        do {
            try FileManager.default.createDirectory(
                at: url.deletingLastPathComponent(),
                withIntermediateDirectories: true,
                attributes: nil
            )
            try compressed.write(to: url, options: .atomic)
            return url.lastPathComponent
        } catch {
            return nil
        }
    }

    static func migrateLegacyBase64(_ base64: String?) -> String? {
        guard let base64,
              let data = Data(base64Encoded: base64) else { return nil }
        return saveImageData(data)
    }

    static func delete(fileName: String?) {
        guard let fileName else { return }
        try? FileManager.default.removeItem(at: logoURL(fileName: fileName))
    }

    static func image(for fileName: String?) async -> UIImage? {
        guard let fileName else { return nil }
        let url = logoURL(fileName: fileName)
        return await Task.detached(priority: .utility) {
            guard let data = try? Data(contentsOf: url) else { return nil }
            return UIImage(data: data)
        }.value
    }

    private static func logoURL(fileName: String = fileName) -> URL {
        let baseDirectory = (try? FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )) ?? FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!

        return baseDirectory
            .appendingPathComponent(directoryName, isDirectory: true)
            .appendingPathComponent(fileName)
    }
}

extension UIImage {
    func resizedForLogo(maxDimension: CGFloat) -> UIImage? {
        let currentMax = max(size.width, size.height)
        guard currentMax > maxDimension else { return self }
        let scale = maxDimension / currentMax
        let targetSize = CGSize(width: size.width * scale, height: size.height * scale)
        let renderer = UIGraphicsImageRenderer(size: targetSize)
        return renderer.image { _ in
            draw(in: CGRect(origin: .zero, size: targetSize))
        }
    }
}
