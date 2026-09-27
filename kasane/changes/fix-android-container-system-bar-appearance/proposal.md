# Proposal: fix-android-container-system-bar-appearance

## Why

Android の Loading / Toast の器は、表示中に提示先の画面のシステムバーの指定を変えてしまう。明るい画面で出すと、ステータスバーのアイコンが白に変わって白地に溶ける。Loading はバーを隠した画面で出すと、ステータスバーとナビゲーションバーを再出現させる。Dialog の器は提示先の指定を写しているが、Loading / Toast の器は写していない (`evidence/sample-*.png`・`evidence/sample-hidden-bars-*.png`)。

発端は、API 35 で間欠的に落ちる instrumented テスト (`DialogTransparentOverlayTests`) だった。調べると、落ちる原因はテストの測り方で、Dialog は無関係だった。テスト用の画面が暗いアイコンを指定しておらず、通る回も白地に白アイコン同士を比べていただけで、明暗の引き継ぎは実質的に検証されていない。あわせて、提示先が旧来のフラグだけで明暗を指定している場合と、API 31 で再表示の作法を指定していない場合に、Dialog の引き継ぎも値を読み損ねることが分かった。

決定は探索で確定済み: 「Dialog / Loading / Toast の器は、提示先の画面のシステムバーの指定を変えない」を core の共通の約束にする — [core/ADR-0039](../../decisions/core/0039-containers-keep-host-system-bar-settings.md) (proposed)。経緯と実測は [exploration.md](exploration.md)。

## What Changes

- **契約 (core)**: Dialog / Loading / Toast の器は、表示中も提示先の画面のシステムバーの指定 (アイコンの明暗・表示/非表示・隠れたバーの再表示の作法) を変えない。暗幕がステータスバーの下を暗くしたときに OS が文字色を合わせるのは約束の外。Dialog の既存の約束 (覆いが透明ならシステムバーの見えは変わらない) もこの約束に含める
- **Android の器**: Loading / Toast の器も、Dialog と同じ引き継ぎ (明暗・表示/非表示・再表示の作法を、画面に載った時点で 1 回写す) を行う。画面の作り直しで器を載せ替えるときは、載せ替え先の画面から写す
- **Android の引き継ぎの読み取り**: 3 つの器に共通の引き継ぎ処理 (`DialogWindowSystemBars`) で、次の 2 点を補強する
  - 明暗: OS の返す値に加えて、提示先の旧来のフラグ (`systemUiVisibility` の明るいバーのフラグ) も合わせて読む
  - 再表示の作法: 提示先が指定していない (OS が 0 を返す) ときは OS の既定として扱う
- **iOS**: コードは変えない (器はステータスバーの見えを何も指定しておらず、約束を満たしている)。core の Scenario (Dialog の明暗 `PB-SB-08`・Loading の `LD-SB-*`・Toast の `TS-SB-*`) の同名テストを足し、器がステータスバーの見えを決める画面を置き換えないことを確かめる。確かめ方は Dialog の既存テスト (`PB-IA-03`、表示/非表示) と同じ形
- **テストの組み立て直し**
  - 画面の指定が保たれること: 器の指定の値を、テストが提示先に与えた指定から組み立てた期待値と比べる (提示先から OS が返す値とは比べない — 旧来のフラグや作法の未指定では OS が 0 を返すため)。提示先には暗いアイコンを明示させ、その前提を確かめてから比べる。判定するのはステータスバーの明暗を決める指定で、実際に描かれた文字色ではない
  - 透明な覆いで暗くならないこと: 帯の明るさの比較を残す。起動スプラッシュが画面から外れたことを明るさとは独立に確かめてから「表示前」を測り、確かめられなければ測らずに失敗させる (`DialogTransparentOverlayTests`)
- **Scenario ID**: Dialog は `PB-SB-08` 以降、Loading は `LD-SB-*`、Toast は `TS-SB-*` を新設する。Loading / Toast の SB 領域は両 Native に同名テストを置く領域として網羅検査 (`scripts/scenario-id-coverage.py` の `MIRROR_AREAS`) に足す
- **概念文書の追随** (蒸留時): core/api/layout-semantics.md (Dialog の既存の 1 文をまとめ直す)・loading-semantics.md・toast-semantics.md に約束を足す

影響する能力: dialog-contract・loading-contract・toast-contract (core 契約)・android-native。ios-native は挙動の差分が無く、テストの追加だけなのでデルタを持たない (確かめ方は core の 3 デルタの冒頭に書く)

## Non-Goals

- **表示後に提示先がシステムバーの指定を変えたときの追随** — 変えない。引き継ぎは表示時の 1 回で、追随しない決まりが既にある (`PB-SB-06` / `PB-SB-07`)。Loading / Toast も同じ扱いにする
- **暗幕に合わせた OS の文字色の調整 (iOS のダーク外観など) を抑えること** — 約束の外に置くと決めた (ADR-0039)
- **API 29 以下での確認** — 旧経路も同じ引き継ぎ処理を通るが、実測・テストはしない。29 以下固有の疑いは実測・修正しないオーナー判断 (2026-09-26) による。このため完了報告は「API 31 と API 35 で全件実行、API 29 は対象外」と呼び、API 29 でだけ判定できる既存 Scenario (PB-SB-04) は未実行として明記する (handbook cross/test-execution.md の「全件実行」には当たらないことを隠さない)
- **MAUI / KMP の変更と個別検証** — 公開 API は変わらない。MAUI Android・KMP Android は同じ Android Native の器を通る (`maui/android/native/ksdialogs-maui-bridge` は `ksdialogs-core` に依存) ので、Native の修正がそのまま効く
- **Sample の変更** — 無し。Sample Android は API 35 以上で暗いアイコンを指定済みで、修正後の見えの確認 (撮影) にそのまま使える
- **利用者向け Skill (`skills/`) の追随** — docs-refresh 経由の手順で行う (CLAUDE.md の運用)

## Impact

- **破壊的変更: なし** (公開 API は変わらない)。見えの変化は次のとおりで、どれも ADR-0039 の約束に沿う方向
  - Android で Loading / Toast を出している間も、ステータスバーの明暗と隠れたバーが画面の指定のまま保たれる (以前は白いアイコンになり、Loading はバーを再出現させた)
  - 旧来のフラグで明暗を指定した画面でも、Dialog が明暗を保つ
  - 作法を指定していない画面では、Dialog の作法が「触れたら出す」ではなく既定 (スワイプで出す) になる。ずれを実測したのは API 31 で、API 35 ではずれていなかった。API 32〜34 は未確認
- **リスク**
  - Toast はフォーカスを取らないので、表示/非表示を写してもバーの制御は提示先に残る (実測では効かず、害もなかった)。実装時に「写しても無害」を確かめる
  - 旧来のフラグ由来の明暗が API 35 以降も OS から返らないことは、ソースを読んだだけで実測していない。実装時に確かめる
  - 作法の値 0 は「指定なし」と「触れたら出す (非推奨の定数)」の区別がつかないので、後者を明示した画面は取りこぼす (受け入れる)
- **テストの実行**: 新しい Android の Scenario はすべて instrumented で、JVM では走らない。CI (API 36) でも回る。作法の補強は、ずれが起きる API でしか判定できない。起きる範囲 (API 32〜34) を実装時に確かめ、`assumeTrue` でその範囲に絞って、手元の専用 AVD (`ksn_api31`、探索で作成) で回す

## 級: M

3 機能にまたがる core の約束を Scenario として固めるデルタスペックが要るが、コードは Android の本体モジュールの小さな変更で公開 API も変わらない (オーナー確定 2026-09-27)。

domain: cross
