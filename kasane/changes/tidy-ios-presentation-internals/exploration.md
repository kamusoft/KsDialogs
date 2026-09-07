# Exploration: tidy-ios-presentation-internals

## 課題 / 動機

iOS の器 (`ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift` とそのテスト) に、出入りの演出と撤去後配送の実装 (core/ADR-0017) を仕上げる過程で残った**契約には影響しない内部整理の候補**が 4 件ある。いずれもレビューで「今のサイクルで入れる必要はない」と判定されたもので、放置しても挙動は変わらないが、片方だけ直す保守ハザードや検出力の限界として認識しておくべきもの。

発見の文脈: add-presentation-behavior の実装レビュー (`kasane/changes/archive/2026-08-22-add-presentation-behavior/review-003.md` S-2 / `review-005.md` Suggestion / `second-opinion-code-004.md` Minor 2 件の降格) と `handoff-distill.md`。

1. **提示前の閉鎖を「演出なしで撤去」する機構が 2 本立て** — `beginLifecycle()` の `scheduleImmediateRemoval()` (入口判定) と、進行 Task 先頭のガード (`!Task.isCancelled && .attached && !isResultSettled`) が同じ契約セル (created / attached 行の報告列) を担っている。後者は前者を包含しており、`beginLifecycle()` の分岐を外して Task 先頭のガードへ一本化できる見込み。PB-TR-19 (factory 内即報告) の経路が変わるため iOS 全テストの再実行が要る
2. **覆いのフェードの開始が `async let` の子タスク依存** — `none` / 同期完了フックでは最初の描画フレームで覆いの開始が 1 ターン遅れ得る (alpha 0 → 約 4% で視覚的には判別不能)。構造的に揃えるなら `UIViewPropertyAnimator` を親 Task 内で同期開始して完了だけを待つ形
3. **「中身の表示と presentation フック開始の間に描画の機会がない」回帰テストが MainActor の FIFO 順に依存** — 5 連続実行で安定し、直前の `alpha == 0` 期待が前提を見張っているが、厳密には実行順の保証が無い。`CADisplayLink` 等で最初の描画フレームを観察する実描画テストへの置き換え候補 (現状は smoke test として保持)
4. **「撤去要求〜完了通知の窓で器が解放される」順序を直接固定するテストが未配置** — `deliveryOutlivesContainerRelease` が器を直接組み立てて同じ機構 (`DialogOutcomeDelivery` への引き継ぎ) を固定しているため任意。`DialogTestPresentationSurface` の要求/完了分離と弱参照観察を組み合わせれば書ける

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

公開契約・覆すコストの高い判断は含まない見込み (core/ADR-0017 の範囲内の内部整理)。

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- 4 件を 1 change でまとめて扱うか、1 (一本化) だけ先に切り出すか
- 2 の「覆いの同期開始」は Android 側 (`CoroutineStart.UNDISPATCHED` で既に同期開始) と揃える意味があるか、iOS 単独で十分か
- 3 の実描画テストは iOS Simulator 上で安定して回るか (`CADisplayLink` の発火をテストから待てるか) の実測が要る
- 着手時期: 演出まわりに別の変更が入るとき (Loading / Toast への適用など) に同乗させるのが自然

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定

暫定: 公開 API 変更なし・契約変更なし・iOS 単一ビルドルートに閉じるため **S 級** (独立レビューは必須)。3 を入れるなら実描画の実測が加わる。
