import Foundation

/// 撮影のために起動時へ渡せる設定 (cross/ADR-0010)。
///
/// 値は launch arguments の `--キー 値` の隣接トークンペアで受け取る
/// (`--demo basic-dialog --loading-step-interval-ms 2000`)。
/// キーの直後に値が無い・空文字・検証に通らない場合は、そのキーを取り込まずに既定動作へ倒す。
/// 同じキーが複数回現れたときは最初の 1 組を採る。
struct SampleCaptureOptions {
    /// 自動再生するデモ。指定なし・定義外の ID なら nil。
    let demo: SampleDemoId?

    /// Loading 進捗の刻み間隔 (ミリ秒)。指定なし・受理範囲外・数値でない値なら nil。
    let loadingStepIntervalMilliseconds: Int?

    /// このプロセスの起動引数から読み取った設定。
    static let current = SampleCaptureOptions(arguments: ProcessInfo.processInfo.arguments)

    init(arguments: [String]) {
        demo = Self.value(of: Self.demoKey, in: arguments).flatMap(SampleDemoId.init(rawValue:))
        loadingStepIntervalMilliseconds = Self.loadingStepInterval(in: arguments)
    }

    /// 刻み間隔を読み、受理範囲の整数のときだけ返す。
    private static func loadingStepInterval(in arguments: [String]) -> Int? {
        guard let value = value(of: loadingStepIntervalKey, in: arguments),
              let milliseconds = Int(value),
              acceptedIntervalRange.contains(milliseconds)
        else {
            return nil
        }
        return milliseconds
    }

    /// 自動再生するデモを指定するキー。
    private static let demoKey = "demo"

    /// Loading 進捗の刻み間隔を指定するキー。
    private static let loadingStepIntervalKey = "loading-step-interval-ms"

    /// 受理する刻み間隔の範囲 (ミリ秒)。
    private static let acceptedIntervalRange = 1...600_000

    /// `--<キー>` の直後のトークンを値として読む。空文字は値が渡されていないのと同じに扱う。
    private static func value(of key: String, in arguments: [String]) -> String? {
        guard let keyIndex = arguments.firstIndex(of: "--\(key)") else { return nil }
        let valueIndex = arguments.index(after: keyIndex)
        guard valueIndex < arguments.endIndex else { return nil }
        let value = arguments[valueIndex]
        return value.isEmpty ? nil : value
    }
}
