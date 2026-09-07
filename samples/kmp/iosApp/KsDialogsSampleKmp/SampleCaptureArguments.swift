import Foundation
import SampleShared

/// 撮影のために起動時へ渡された値を launch arguments から読む受け口。
///
/// 値は `--キー 値` の隣接トークンペアで受け取る
/// (`--demo basic-dialog --loading-step-interval-ms 2000`)。
/// 取り出すところまでがこの受け口の仕事で、検証と設定への畳み込みは共有の設定型が受け持つ。
enum SampleCaptureArguments {
    /// このプロセスの起動引数から読み取った設定。
    @MainActor static let current = read(from: ProcessInfo.processInfo.arguments)

    /// 起動引数のトークン列から設定を読み取る。
    static func read(from arguments: [String]) -> SampleCaptureOptions {
        SampleCaptureOptions.companion.from(
            demo: value(of: SampleCaptureOptions.companion.DEMO_KEY, in: arguments),
            loadingStepIntervalMilliseconds: value(
                of: SampleCaptureOptions.companion.LOADING_STEP_INTERVAL_KEY,
                in: arguments
            )
        )
    }

    /// `--<キー>` の直後のトークンを値として読む。同じキーが複数回現れたときは最初の 1 組を採る。
    private static func value(of key: String, in arguments: [String]) -> String? {
        guard let keyIndex = arguments.firstIndex(of: "--\(key)") else { return nil }
        let valueIndex = arguments.index(after: keyIndex)
        guard valueIndex < arguments.endIndex else { return nil }
        return arguments[valueIndex]
    }
}
