# Exploration: fix-ios-swiftui-edge-content-placement

## 課題 / 動機
旧 change-id: `ios-end-anchored-content-safe-area-opt-out` (ColorAnalyzer の記録はこの ID で指す。探索で受け取らない指定 (opt-out) を採らなかったため付け直した)。同梱: 簡易起票 `fix-swiftui-window-area-first-layout-width` (2026-10-20、ロードマップ `ios-safe-area-and-kmp-int-result` の phase-1 の実装中に実装ワーカーの実測で見つかったもの。内容は「調査: 同梱した幅の件」に取り込み、ディレクトリは片付けた)

起票元: ../ColorAnalyzer/kasane/changes/archive/2026-10-21-kmp-core-1-0-6-adoption/deviation.md

ColorAnalyzer の KMP 版を 0.1.0-beta.5 に上げたら、iOS の窓の下端に付けた SwiftUI の中身が、beta.4 より約 17pt 上に出るようになった。中身の側で `.ignoresSafeArea` を付けても戻らない。ColorAnalyzer のオーナーは、この見え方を許容できないと判断した (2026-10-21)。

### 起きていること

- 中身: ColorAnalyzer の「下の確認」。窓の下端に横いっぱいの帯を出し、本文と「いいえ」「はい」のボタンを並べる。高さは中身で決まる
- 置き方: 基準領域は窓全体、`DialogPlacement(horizontalAlignment: .fill, verticalAlignment: .end)`
- beta.4 まで: 帯が窓の下端にぴったり付き、原本 (MAUI 版) の画面と一致していた
- beta.5: 帯とボタンが約 17pt 上がる。17pt は、ホームインジケータの領域 (34pt) のちょうど半分
- アプリ側で試したこと: `.ignoresSafeArea` の 4 通り (付ける位置と領域の指定を変えた) と、受け取った安全領域の分を戻す補正。どれも位置を戻せなかった

### 何が問題か

- ios/ADR-0002 の Consequences は「安全領域を避ける・無視するといった標準の書き方がそのまま効く」「無視する指定の中身の見た目は変わらない」としている。下端に付けた、高さが中身で決まる中身では、無視する指定が効かず、この約束が守られていない
- `kasane/concepts/ios/api/layout-surface.md` の「固定サイズの中身の注意」の表にも、「一番外側で安全領域を無視する」中身が下端で重なりの半分だけ上へ動くと実測で載っている。ただし「KsDialogs が保証しない並び方」として書かれていて、利用者が避ける手段が無い
- 安全領域を中身へ届けるなら、届けない (無視する) 選択肢も要る。beta.4 までの見え方に頼っていたアプリが、版を上げただけで見た目を崩され、戻す手段が無い

### このアプリの起票の経緯

beta.5 の対応の元になった簡易起票 `kmp-ios-container-safe-area-and-int-result` は、ColorAnalyzer の移植の間に起票したもの。そこには「器の中に安全領域を届ける」ことだけを書き、既存の中身が安全領域を受け取らずに済む選択肢 (互換) には触れていなかった。この起票で補う。

### 調査 (ksn-explore 2026-10-21、起点 fbaa2f6)

- 中身の実物: `../ColorAnalyzer/kmp/iosApp/iosApp/ColorPicking/BottomConfirmDialogContent.swift`。一番外側は `VStack(spacing: 16)` (本文の `Text` + ボタンの `HStack`)、`.padding(.bottom, 32)` 等の余白、`.background(色)`。`ignoresSafeArea` は付いていない (試した 4 通りはコミットされていない)。配置と基準領域は中身の添付 (`ksDialogOptions(layoutArea: .window, …)` / `ksDialogPlacement(.fill, .end)`) で渡す。beta.4 の頃から書き換えていない
- 仕組み (数値の一致からの推定。実測で確かめる): 器の高さは中身の理想の高さ H のまま (ios/ADR-0002 で要求サイズに重なりを入れない)。ホストは下 34 を安全領域として受け取るので、中身に使える高さは H − 34。文言とボタンの `VStack` は縮めないため H のまま置かれ、SwiftUI はそれを安全領域の内側の中央に置く。はみ出しが上下に 17 ずつ割れ、帯の下端が窓の下端から 17 上がる。地色は中身の大きさに付くので帯ごと上がる
- archive の実測 (`kasane/changes/archive/2026-10-20-deliver-swiftui-content-safe-area-ios/evidence/safe-area-sizing-probe.md`) で「大きさが可変」とした中身は縮められる中身で、文言とボタンを並べた実際の中身は「固定 100」の行と同じ振る舞いになる。`kasane/concepts/ios/api/layout-surface.md` の「大きさが可変の中身ではどちらも起きない」は、縮められない中身には当てはまらない
- 同じ実測で、下端の固定サイズの中身は一番外側で安全領域を無視しても y 735 (無視しないときと同じ) で、無視する指定が効いていない。上端では効く (y 0)
- 影響範囲: 端に付けた (窓全体基準) 縮められない SwiftUI の中身すべて。Dialog・Loading・Toast は同じホスト (`ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIHost.swift`) を通る。上端は 29.5 (59 の半分) 下がる。ColorAnalyzer ではほかに FAB のメニュー (`Library/FabMenuContent.swift`、窓全体のとき下端・右端) が同じ形だが、見た目の記録は無い

### 調査: 同梱した幅の件 (ksn-explore 2026-10-21、起点 fbaa2f6、コードの読みによる。実測は未)

- 3 症状 (窓全体の端に付けた折り返す本文が 734 で止まる・理想 600 の中身が縦長 393 のまま横長で広がらない・中央の長い本文が 389 のまま) は根が同じ一方向の記憶。`ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift` の `layoutSubviews` は、器が要求より狭く置いたときだけ置かれた幅を覚え (`proposedWidth`)、以後はその幅で折り返した大きさを要求する。上限が広がっても置かれた幅と要求が一致したままなので書き換えが起きない。覚えた幅を捨てるのは中身の理想の大きさが変わったとき (`contentPreferredSizeDidChange`) だけで、添付値の到達・制約の張り直し・窓の回転では捨てない
- 器 (`ios/Sources/KsDialogs/Presentation/` の Dialog・Loading・Toast の各 ContainerViewController) から中身へ上限の変化を知らせる経路は無い。`Layout/DialogLayoutApplier.swift` は内容サイズに委ねた軸に幅の制約を張らず、上限 (`width <= maxSize`) だけを張る。全面 (`.fill`) は幅の制約で置かれた幅が要求と食い違うので追従する
- UIView の中身は添付値の到達を待たず、幅も覚えないので症状 1 は構造上起きない。症状 2・3 が起きないことはテストが無く未確認
- 関連テスト: `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift` の [PB-IS-14]・[PB-IS-15]、`DialogSwiftUIContentWidthSettleTests.swift`・`DialogSwiftUIContentSizeFollowTests.swift`。症状 1 を直すと [PB-IS-15] の器の幅は 852 になり、右端も安全領域に重なる。期待値「右 0」は ios/ADR-0002 (端の軸では重なりをそのまま届ける) から「右 59」へ見直しになる見込み

## 検討した選択肢 (却下案と理由を含む)
- **A (採用): 安全領域の内側に収まらない中身は、器の内側の辺 (下端に付けたなら上の辺) にそろえて器いっぱいに置く** — 公開 API は増えない。縮まない中身は beta.4 と同じく器いっぱいに置かれ (ホームインジケータに重なる)、縮められる中身は今までどおり安全領域を避ける。無視する指定の有無で位置が変わらない。SwiftUI がそう並べるかは試作で確かめる
- B (却下): 中身ごとに、安全領域を受け取らない指定を足す — 指定を付けない中身は半分ずれたまま残る。公開 API が 1 つ増え、core の約束にするなら Android 側の対応も要る
- C (却下): 既定を beta.4 (届けない) に戻し、受け取りたい中身だけ指定する — ios/ADR-0002 の決定を実質取り消し、公開 API も 1 つ増える

## 決定事項
- 直し方は A (2026-10-21 オーナー決定)。試作で前提が成り立たなければ B に倒し、ios/ADR-0003 を書き直す
- 簡易起票 `fix-swiftui-window-area-first-layout-width` (窓全体基準の SwiftUI の中身の幅が窓いっぱいまで広がらない) を同梱する (2026-10-21 オーナー決定。推奨は別起票のままだったが、同じ `DialogSwiftUIContentView` の大きさの決め方なので 1 回のテスト・レビューで扱う)
- 幅の件の直し方は X: 器が中身へ有効領域の上限を知らせる内部の経路を足し、上限が広がったら覚えた幅 (`proposedWidth`) を捨てて理想の幅から求め直す (2026-10-21 オーナー決定)。3 症状と Loading・Toast を覆い、上限が変わらない配置の値は変えない。内部実装の局所的な判断のため ADR にしない
- 却下 (幅の件): Y 添付値の到達時だけ求め直す — 回転の 2 症状が残る。Z 内容サイズに委ねた軸では幅を覚えず毎回「理想の幅と今の上限の小さいほう」で求める — 既定の可視領域で頭打ちの本文の幅が 733.33 → 734.0 に動く
- [PB-IS-15] の期待値は、器の幅 852・安全領域は左 59 / 右 59 へ見直す (ios/ADR-0002 の「端の軸では重なりをそのまま届ける」から導ける)
- UIView の中身でも、内容サイズに委ねた軸が回転・上限の拡大で広がることをテストで確かめる (core/ADR-0011)
- 蒸留時に反映: ios/ADR-0002 — footer に `関連: ios/ADR-0003` の行を足す (Consequences「無視する指定の中身の見た目は変わらない」が縮まない中身で成り立っていなかった分を ios/ADR-0003 が補う)
- 蒸留時に反映: concepts — `kasane/concepts/ios/api/layout-surface.md` の「固定サイズの中身の注意」(保証しない並び方) を、収まらない中身は内側の辺にそろうという保証に書き直し、「器の大きさは安全領域で変わらない」節の幅の制約の段落 (起票 `fix-swiftui-window-area-first-layout-width` への言及) を外す
- 器 (有効領域で頭打ち) より大きい縮まない中身も、内側の辺にそろえ、付いた側 (窓の端の外) へはみ出させる (Q、2026-10-21 オーナー決定。相方の提案レビュー spec-001 の指摘 4 から)。却下: P 器より大きい中身は対象外で今の並び方 (前後に半分ずつはみ出す) のまま — 例外が増え、並び方を約束できない / R 器より大きいときだけ付いた辺にそろえる — 決まりが 2 段になる
- 範囲は ios/ADR-0002 が安全領域を届ける範囲と同じ: SwiftUI の Dialog・Loading・Toast の中身 (KMP の Swift 面からの登録を含む)、窓全体基準で片方の端に付けた軸 (配置 start / end。比率サイズの有無を問わない)。上端・下端・左右の端を含む。全面・中央の軸、窓全体以外の基準領域、UIView の中身は変えない

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)
- 作成済み: ios/ADR-0003 (proposed) — 端に付けた SwiftUI の中身が安全領域の内側に収まらないときは、器の内側の辺にそろえて器いっぱいに置く。ios/ADR-0002 を置き換えずに補う (`関連:` 行)

## 未決の論点
- 論点4: 変更級

## UI 素材 (ui/references/ の一覧と注釈)
- `original-maui-bottom-confirm.png`: 原本 (MAUI 版、iPhone 11、@2x)。帯が窓の下端に付く。beta.4 の KMP 版も同じ位置だった
- `beta5-bottom-confirm.png`: ColorAnalyzer の KMP 版 + beta.5 (iPhone 17 のシミュレータ、@3x)。帯とボタンが約 17pt 上がる
- `compare-bottom-edge.png`: 上の 2 枚をポイントの大きさにそろえ、下端の 180pt を並べたもの (左が原本、右が beta.5)

## 変更級の推奨: M (2026-10-21 オーナー確定)

1 つの能力 (iOS の SwiftUI の中身のレイアウト) の中で、観察できる約束 (Scenario) が変わる。PB-IS-12・PB-IS-15 の見直しと、端の縮まない中身の位置・回転で幅が広がること (SwiftUI と UIView の両方) の Scenario の追加をデルタスペックで書く。公開 API の変更なし。UI のモックは作らない (見た目の基準は `ui/references/original-maui-bottom-confirm.png`、beta.4 と同じ位置)。A の前提は試作で確かめ、成り立たなければ提案の段階で B に倒す
