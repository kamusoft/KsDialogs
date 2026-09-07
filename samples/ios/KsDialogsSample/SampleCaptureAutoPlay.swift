/// 起動引数で指定されたデモの自動再生を、プロセスの起動につき 1 回だけ取り出す受け口。
///
/// 一度取り出したあとは nil を返すため、画面が作り直されても再生は繰り返されない。
@MainActor
enum SampleCaptureAutoPlay {
    private static var isConsumed = false

    /// 自動再生するデモを取り出す。指定が無い場合と 2 回目以降は nil。
    static func consumeDemo() -> SampleDemoId? {
        guard !isConsumed else { return nil }
        isConsumed = true
        return SampleCaptureOptions.current.demo
    }
}
