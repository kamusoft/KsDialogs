# Tasks: deliver-swiftui-content-safe-area-ios

## 1. SwiftUI の中身のホストと器のレイアウト (ios/Sources)
- [ ] 1.1 ホストが SwiftUI の中身にシステムの領域だけを標準の安全領域として渡すようにする (キーボードの領域は含めない) (→ Requirement: 窓全体基準で端の軸に付けた SwiftUI の中身は、器と安全領域の重なりを標準の安全領域として受け取る)
- [ ] 1.2 器へ伝える中身の要求サイズを、ホストが受け取っている安全領域に左右されない大きさにする。中身の要求サイズが表示中に変わったら器が追従するようにする (→ Requirement: 安全領域を受け渡しても、器の大きさと位置は変わらない)
- [ ] 1.3 基準領域が窓全体で、レイアウトの解の位置が端 (前端・後端) に固定される軸にだけ、中身に安全領域を渡す。中央に固定される軸と、窓全体以外の基準領域では渡さない。軸ごとの配置は器のレイアウトの解から取り、Dialog・Loading・Toast の 3 つの器で同じく効かせる (→ Requirement: 窓全体基準で端の軸に付けた SwiftUI の中身は…受け取る / 安全領域を受け渡しても、器の大きさと位置は変わらない)

## 2. テスト (ios/Tests)
- [ ] 2.1 端の軸の受け取り: [PB-IS-01]〜[PB-IS-03] (Dialog)、[PB-IS-05] (Loading)、[PB-IS-06] (Toast)、窓の変化 [PB-IS-07]、キーボードを含めない設定 [PB-IS-08]、比率サイズの全面 [PB-IS-09] のテストを足す (→ Requirement: 窓全体基準で端の軸に付けた SwiftUI の中身は…受け取る)
- [ ] 2.2 中央の軸: [PB-IS-04] のテストを足す (→ 同上)
- [ ] 2.3 器の大きさと位置: [PB-IS-10]〜[PB-IS-16] のテストを足す (→ Requirement: 安全領域を受け渡しても、器の大きさと位置は変わらない)
- [ ] 2.4 窓全体以外の基準領域: [PB-IS-17]〜[PB-IS-19] のテストを足す (→ Requirement: 窓全体以外の基準領域では、SwiftUI の中身は安全領域を受け取らない)
- [ ] 2.5 ios/ の全件テスト (Simulator) と kmp/ の `allTests --rerun-tasks`、`python3 scripts/scenario-id-coverage.py` を回し、件数と結果を `evidence/test-run-summary.md` に残す

## 3. Simulator での実測
- [ ] 3.1 テスト用の窓ではなく、ノッチのある機種の Simulator の実際の窓で、窓全体基準で上端・下端に付けた SwiftUI の中身が受け取る安全領域と器の矩形を測り、`evidence/` に残す (委任状の完了条件 5)
