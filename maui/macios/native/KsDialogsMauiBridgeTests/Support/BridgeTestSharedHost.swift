import Testing

/// 1 プロセスに 1 つしかない提示先を共有するテストの、本体と後片付けの組み立て。
///
/// テストは同じ key window とプロセスの状態を共有して直列に走るため、片付けを飛ばすと
/// 後続のテストが順序依存で落ちる。本体が途中で失敗しても片付けまで済ませ、
/// 本体が通ったときは片付けが完了したことそのものも検証する。
enum BridgeTestSharedHost {
    /// 本体を走らせ、成否によらず後片付けを行う。
    ///
    /// 本体が失敗したときは片付けてから元の失敗を投げ直す (失敗の原因は本体側にあるため)。
    /// - Parameters:
    ///   - body: テストの本体。
    ///   - cleanup: 共有の状態を元に戻し、戻り終えたかを返す処理。
    @MainActor
    static func run(
        _ body: () async throws -> Void,
        cleanup: () async -> Bool,
        sourceLocation: SourceLocation = #_sourceLocation
    ) async throws {
        do {
            try await body()
        } catch {
            _ = await cleanup()
            throw error
        }
        try #require(
            await cleanup(),
            "共有の提示先が片付き、後続のテストの観測点を汚さない",
            sourceLocation: sourceLocation
        )
    }
}
