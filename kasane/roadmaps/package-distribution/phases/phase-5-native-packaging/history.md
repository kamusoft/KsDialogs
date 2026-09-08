# phase-5-native-packaging 議論履歴

## 2026-09-08: バージョンの単一ソースとリリース version の注入

- 実物確認: `android/` は本体・compose の各 module がカタログ (`ksdialogs = "0.1.0"`) から version を取り、ルート build.gradle.kts はない。`kmp/` は version を `"0.1.0"` と直書きし、本体への依存版だけカタログから取る。カタログ値が SNAPSHOT でなく、踏襲する SNAPSHOT ガードが効かない状態
- 選択肢: A) カタログを `0.1.0-SNAPSHOT` の開発既定値にし、android / kmp の両ビルドが「`-Pversion=` 注入値があればそれ、無ければカタログ」の同じ式で version と依存版を導出 (KsSettingsView 方式 + KMP 拡張) / B) カタログ値を CI が書き換える / C) version 専用ファイルの新設
- 採用: A。注入点が「両ビルドに `-Pversion=` を渡す」の 1 種類で済み、KMP → 本体の厳密同版 (cross/ADR-0009) が同じ式から導出されて機械的に揃う。オーナー判断「KsSettingsView の方式、`0.1.0-beta.1` のような prerelease で始める」
- 併せて観測: maui の iOS binding は `../../../ios` の Local Swift Package 参照で product `KsDialogs` 1 本のみ。スナップショット化の影響なし (agenda の「着手時に確認」項目は解消)

## 2026-09-08: Android toolchain と vanniktech の互換

- 調査 (scout): vanniktech 最新安定版は 0.37.0 (KsSettingsView と同版)。検証済み範囲は AGP 9.2.1 / 9.3.0-rc01 / Kotlin 2.4.0 まで、最低要件 JDK 17 / Gradle 9.0 / AGP 8.13 / Kotlin 2.2。AGP 9 の publishing まわりでプラグイン側の既知問題なし。AGP 9.3.0 正式版だけ一覧に未掲載
- 選択肢: A) toolchain 据え置き + 0.37.0 / B) AGP を 8.13 系へ下げて翻案元と揃える
- 採用: A。B は KMP の AGP 9 前提プラグインが使えなくなり phase-7 に響く。留保は dry-run (publishToMavenLocal + POM 検算) で押さえる (オーナー OK)

## 2026-09-08: 公開 ABI の `api` スコープ仕分け

- 調査 (scout): 公開宣言の全走査で外部の非プラットフォーム型は `ColorInt` と `Composable` の 2 つのみ、共に `api` 済み。上げるべき `implementation` はゼロ。翻案元の漏れ 2 件 (`RecyclerView.ViewHolder` 継承・`PaddingValues` プロパティ型) の経路も該当なし。留保: 発行後 aar の javap 検算は未実施
- 選択肢: A) 現状維持 + 発行時に aar の機械走査で検算 / B) 静的読解のみで確定 / C) Gradle 検査タスクとして常設
- 採用: A (オーナー OK)

## 2026-09-08: Android の配布物の名前 (統合案からリネームへ)

- オーナー発案「分離する意味がないので統合したい」に対し、実物で確認: MAUI binding は本体 aar を Bind=false で取り込み NuGet は StdLib + Coroutines のみ。統合すると本体が Compose 本体に依存し、MAUI / KMP / View 系の全消費者に Compose (数 MB) が届く。翻案元 KsSettingsView は統合後に MAUI binding へ Compose NuGet 5 本を足し dex の二重定義で版固定の注記を残した。Compose 側の aar は 25 KB でサイズは論点でない。図解 (artifact) で説明し、統合は取り下げ
- 真の狙いは「Android では Compose が主役なので素の名前を Compose 側に」。選択肢: 従う案 (現状名) / 改訂案 (本体 `ksdialogs-core`、Compose 側 `ksdialogs`)
- 採用: 改訂案 (オーナー OK)。cross/ADR-0005 は Android の中でどちらに素の名前を渡すかを検討しておらず前提と衝突しない → amends。android/ADR-0001 はモジュール名だけ amends。cross/ADR-0019 を proposed で起票。proposed の cross/ADR-0008・0009 は本文の座標表記を直接書き換え
- 積み残し: ディレクトリ / project 名・Kotlin パッケージ名・namespace の扱い (次の論点)、roadmap ゴール文の表記 (ksn-roadmap)

## 2026-09-08: 座標リネームの細部

- 選択肢: A) ディレクトリ / project 名は改名、Kotlin パッケージ名と namespace は維持 / B) パッケージ名まで全部揃える / C) 座標だけ差し替えてディレクトリはそのまま
- 採用: A (オーナー OK)。C はディレクトリと座標のねじれが残り、B は利用者の import が変わる割に得がない。cross/ADR-0019 の Decision を結論に合わせて書き直し

## 2026-09-08: POM メタデータ

- 選択肢: A) 共通部はルートで一括、name / description は module ごと / B) 各 module に POM 全文を複製
- 採用: A (オーナー OK)。文言は README の要約から英語で作成し、リネーム後の座標 (`ksdialogs` = Compose 側が主役、`ksdialogs-core` = View 系本体) に合わせた

## 2026-09-08: 発行検証と samples の参照切替

- 翻案元の実装 change の tasks を確認: Android は publishToMavenLocal の検算 + Sample の置換追随、iOS は手動 push + 検証用 prerelease tag で https 解決を確認して tag を削除。消費者ビルドは verification/ (phase-7 相当)
- 選択肢: A) samples はソース参照維持、検証は翻案元と同じ範囲 / B) samples を配布物参照に恒久切替 / C) 一時的に切り替えて戻す
- 採用: A (オーナー確認「KsSettingsView と同じ?」→ 同じ、KsDialogs 追加分は座標追随と javap 検算の同居)

## 2026-09-08: SPM スナップショットの初回 push と申し送りの確認

- 実物確認: 配信リポジトリの README は翻案元テンプレートの名前差し替えと同一 / スクリプトの固有値は配信リポジトリ名とコメントのみ / Package.swift は無改変で置ける / maui iOS binding は product `KsDialogs` 1 本の Local Swift Package 参照で影響なし
- 採用: 確認結果をそのまま決定事項に記録して閉じる (オーナー OK)。論点が出尽くしたため提案化 (ksn-propose) へ
