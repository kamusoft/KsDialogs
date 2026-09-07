# Exploration: map-multi-display-scenario-ids

## 課題 / 動機

重ね出し (複数ダイアログ) の core 契約 `kasane/concepts/core/api/multi-display-semantics.md:29` で、「上から順に閉じる」「重ね出し中に外側をタップ」の **2 ケースに対して Scenario ID `PB-MD-01`・`PB-MD-02`・`PB-MD-03` の 3 つが括弧書き**されていて、どの ID がどのケース (と、どの観察結果) に対応するのかが読めない。`PB-MD-04` (下を先に閉じたときの見え方) と `PB-MD-05` (器が外れたときの cancelled) は個別に節・文で対応が付いている。Scenario ID は両 Native 実装の同名テストと契約を結ぶ鍵 (core/ADR-0016) なので、対応が読めないと契約とテストの照合ができない。baseline (split-concepts-platform-surface 着手時点) から存在。

発見の文脈: split-concepts-platform-surface の tasks 2.1 (`multi-display-semantics.md` の core 書き直し) で Scenario ID 接頭辞のバッククォートを外したとき。同 change は契約の記述内容を変えない方針のため見送り。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

なし (契約の内容は変えず、ID とケースの対応を明示するだけ)。

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- iOS / Android のテスト (`PB-MD-01`〜`03` を名前に持つもの) を読んで、3 ID が実際に何を固定しているかを確認する (2 ケース × 観察点で 3 本なのか、3 ケース目が文書から落ちているのか)
- 対応表を `:29` の段落に足すか、`PB-MD-04` と同じく小節に割るか
- ksn-drift のディープ検証 1 件として片付けるのが自然 (change を立てるほどではない可能性)。その場合はこの change を drift の記録へ吸収して閉じる

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定

暫定: 契約文書に対応表を足すだけの **S 級** (テストの読み取りが要るが実装は変えない)。
