# phase-7-kmp-packaging 議論履歴

## 2026-09-08: A1 発行プラグインと composite build の両立

論点の番号付けを固定した (A publication の形 A1〜A6 / B 公開面の固定 B1〜B2 / C 消費者検証の形 C1〜C3)。phase-5 は決定済み・実装未着手で、kmp/ は `localSwiftPackage` 固定・version 直書き・発行配線なしの状態から出発する。

- 案 a: `:ksdialogs-kmp` にも vanniktech 0.37.0 を適用し、`includeBuild("../android")` を維持 — **採用**
- 案 b: PoC どおり素の `maven-publish` — 却下 (署名・Central upload が kmp だけ別実装になり CI 手順が分岐)

根拠: composite build の置換は解決時にだけ効き POM には宣言座標が載る (PoC 項目 1)。vanniktech は既存 publication を修飾するのみ (プラグインの Platform.kt を scout が裏取り)。AGP 9 の KMP ライブラリプラグインは `androidVariantsToPublish` 省略で公式サポート。`swiftpm-metadata.json` が vanniktech 経由でも載るかは公式記述がなく、dry-run の `publishToMavenLocal` で確認することを受け入れ条件にした。POM 共通部は kmp/build.gradle.kts を新設して android 側と同内容を持つ (別ビルドルート、cross/ADR-0004)。ADR 化は蒸留時に phase-5 の発行設定とまとめて判断する。

## 2026-09-08: A2 dev / publish の Swift 参照切替 (A3 を同時解消)

- 案 a: version から導出 (SNAPSHOT → local / それ以外 → remote exact(version)) + dry-run 用の URL 上書きプロパティ 1 つ — **採用**
- 案 b: Gradle プロパティでモードを明示切替 — 却下 (フラグ忘れが残り、別途ガードが要る)
- 案 c: version 導出のみ、URL 上書きなし — 却下 (dry-run のたびに配信リポジトリへ一時 tag を push する運用になる)

理由: 絶対パス伝播の事故 (PoC 判明事実 1) を構造的に防ぎ、exact の値と version が同じ式から出て lockstep を手で揃えない。dry-run は `file://` の bare clone + tag (PoC 実証済み) を URL 注入で使う。開発ループは SNAPSHOT のまま local 参照で変わらず、Sample の linkage package に差分が出ないため A3 は論点として消えた。既知の限界として SNAPSHOT の mavenLocal 発行物には local パスが載る (文書に明記)。

## 2026-09-08: A4 Kotlin サポート範囲と integrateLinkagePackage 手順

裏取り (scout、Web): SwiftPM import は Kotlin 2.4.0 導入・Alpha。公式ページの「2.4.20-RC3」は試用の推奨版で最低要件ではない (2.4.10 で動いている事実と矛盾しない)。消費側 KGP の最低版とメタデータ互換は公式記述なし (不明)。Stable 化の期日なし (KT-53877)。

- 案 a: 同 minor 2.4.x をサポート、確認済み版はカタログ値を転記 — **採用**
- 案 b: 下限 2.4.0 のみ、上限なし — 却下 (未確認の幅を約束する)
- 案 c: 確認済み版と完全一致のみ — 却下 (patch 更新ごとに非サポート)

integrateLinkagePackage の手順は concepts / Skill の記述が PoC と一致しており変更なし。Skill の「Kotlin の下限は未確定」の文を宣言で置き換える (docs-refresh)。ADR は 0008 の既存方針で足りるため起票・改訂なし。

## 2026-09-08: A5 version の注入 (kmp/ 側の配線)

- 案 a: 導出式と形式検査・SNAPSHOT ガードを kmp/build.gradle.kts に同じく書き、dry-run の発行物比較で揃いを検算 — **採用**
- 案 b: android/gradle/ 配下の共有スクリプトを `apply(from=)` — 却下 (phase-5 design の改訂が要る、kts の制約)
- 案 c: build-logic の included build — 却下 (過剰)

理由: 別ビルドルート (cross/ADR-0004) で式は 3 行程度、ずれは発行物で機械検出できる。version 値は kmp の version・ksdialogs-core 依存版・A2 の Swift 参照導出の共通入力。Sample shared の直書き版はカタログ値に揃える。

## 2026-09-08: A6 Skill の依存宣言が公開座標で解決できるか

設計変更なしで閉じた。Central 発行 (A1) + 推移的依存 + SwiftPM metadata (A2) で `mavenCentral()` だけで解決できる (PoC 項目 1・2)。Skill の `implementation` は誤りで `api` が正 (共有 VM が `DialogViewModel` を継承し、Android アプリ側の登録コードに型が要る)。実解決確認は phase-8、phase-7 は `publishToMavenLocal` の発行物まで。Skill の追従は change 完了後の docs-refresh (TODO 化)。同梱案 (change 内で skills/ を直接編集) は CLAUDE.md の規約に反するため採らず。

## 2026-09-08: B `@Throws` 宣言の回帰検査 (B1 要否 / B2 置き場)

生成ヘッダの実物で、suspend 関数は `@Throws` の有無に関わらず completionHandler に `NSError` が付く (`setMessage` と `show` が同形) ことを確認。ヘッダ検査は非 suspend の `KsToast.show` しか区別できない。

- 案 a: `androidHostTest` の反射テスト (4 経路の throws 節の肯定検査 + それ以外の否定検査) — **採用**
- 案 b: `api-surface-check` に組み込む — 却下 (コンパイル検査は `@Throws` に反応しない)
- 案 c: 検査なし (evidence のみ) — 却下 (退行が利用者の Swift 側 abort でしか分からない)

ADR 化はしない (テストの追加で可逆)。

## 2026-09-08: C1 dry-run の Maven / Swift 参照

翻案元の iOS 消費者検証 (Package.swift.template + `swift build`、dry-run は `path:`) を確認。KMP は `integrateLinkagePackage` が Xcode project を要求するため xcodeproj が要り、KMP metadata に焼き込まれた参照は消費者が変えられない。

- 案 a: linkage 側 (A2 の URL 上書き) とアプリ側 (テンプレート生成の VerificationApp package) の両方を同一 `file://` clone + exact に揃える。prepare-feed に commit + tag を足す — **採用** (PoC 項目 4 で同一 URL のデデュープ実測済み)
- 案 b: アプリ側は `path:` — 却下 (種別違いの identity 衝突が未検証)
- 案 c: 一時 tag の push — 却下 (A2 と同じ理由)

## 2026-09-08: C2 検証範囲

- 案 a: 解決 + Release ビルド 3 段 (Android assembleRelease / shared framework の Release リンク / xcodebuild Release シミュレータ向け) — **採用**
- 案 b: Xcode を回さない — 却下 (linkage 経由のリンクという主眼が抜ける)
- 案 c: 起動まで — 却下 (翻案元にない仕組みが要り目的を超える)

## 2026-09-08: C3 release の 4 本目 (phase-9 への入力)

4 点を申し送りとして確定: package 段は kmp/ の publishToMavenLocal 成果物 (https + exact) を artifact 化し Central deployment は 2 件 / dry-run 段は package 段の kmp artifact を使わず URL 上書き付きで発行し直す (同一性検査の対象外) / publish 段の順序は翻案元のまま (tag 未存在の窓は許容) / smoke 段は Central + https で回し反映待ちに SPM tag と ksdialogs-kmp の HEAD を足す。却下: SPM tag を Maven release より前に push (失敗時に tag だけ残る)。

論点はこれで出尽くした。次は ksn-propose でこのフェーズの変更提案を作る。
