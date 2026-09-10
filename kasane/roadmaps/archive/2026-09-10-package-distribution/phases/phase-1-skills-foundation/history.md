# phase-1-skills-foundation 議論履歴

## 2026-09-04: 論点 1 — KMP Skill は 1 本にまとめるか

ksn-scout で KMP Sample (`samples/kmp/`) と `kmp/ksdialogs-kmp/` を調査し、KMP 利用者が書くコードを側ごとに確定した: 共有コード (commonMain) は `jp.kamusoft.ksdialogs.kmp.*` のみ、Android ホストは Android Native API そのもの (Android Native Sample の登録コードと逐語一致)、iOS ホストは iOS Native パッケージの KMP 向けサブ面 (`Dialog.shared.kmp.register` / `notifier(for:)`)。KMP artifact 側には登録 API がない (kmp/ADR-0002・0003 どおり Native へ全委譲)。

選択肢: A. 1 本 (SKILL.md は共有コード + 3 側 Setup + 最小コード、ホスト側は references/ で振り分け) / B. 3 本 (共有 / Android ホスト / iOS ホスト) / C. 1 本 + Android ホスト側は Android Skill 併用。

採用: A。理由: 分割軸「利用者の状況」で KMP アプリ開発は 1 状況 (書く人は同じ 1 プロジェクト)、iOS ホスト側は KMP 専用面で純 iOS Skill と共有できない、Android ホスト側の重複は閉世界性の代償として受け入れ manifest targets で追従する。B は各 Skill に前提説明が入り発火条件も細かくなる、C は閉世界性違反で却下。

付随して phase-7 (依存宣言の公開座標) と phase-2 (`KotlinMultiplatformLinkedPackage` の手書き要否) への申し送りを TODO に積んだ。

## 2026-09-04: 論点 2 — 移行 Skill の対象 (MAUI Skill に含めるか独立させるか)

選択肢: A. 独立した移行 Skill (`ksdialogs-aiforms-migration`、SKILL.md + references/api-mapping.md) / B. MAUI Skill の references/ に移行章として同居。

採用: A。理由: 分割軸「利用者の状況 (platform × 新規 / 移行)」で移行は別状況、同居は新規導入者に旧 API 対応表を読ませて段階開示に反する。互換 shim なし (cross/ADR-0001) のため移行者に必要なのは対応表であり MAUI Skill のレシピ形式と別物。KsSettingsView の `kssettingsview-aiforms-migration` と同型で骨格を流用できる。

調査で判明した KsDialogs 固有の前提: (1) Toast は移植元で Obsolete のため互換なし → 移行 Skill で「対応先なし」と明記。(2) 移行 Skill の源泉 concept が KsDialogs にない (KsSettingsView は `cross/reference/aiforms-spec-summary.md` を源泉にしたが、KsDialogs の concepts は各 core concept が「移植元との差」に触れるのみで、旧 API のメンバー一覧を持つ concept は存在しない。handbook の aiforms-origin-reference.md は参照規約であって API 一覧ではない)。(2) は論点 2b として継続。

## 2026-09-04: 論点 2b — 移行 Skill の源泉 concept をどこに置くか

選択肢: A. 新 API 側の concepts のみを源泉、旧 API 側は移植元 README から phase-2 で一度書き起こし追従しない / B. 移植元 API の要約 concept を新設して源泉に加える (KsSettingsView 型) / C. 旧 → 新の対応表 concept を新設し Skill をその翻訳にする。

採用: A。理由: 旧 API 側は凍結対象で追従の実益がない、要約 concept は外部一次情報の写しで層が重なる (handbook aiforms-origin-reference.md も要約を挟まない運用)、対応表 concept は派生物と正の二重化。B・C は phase-2 の前に concept 追加 change を挟む必要もあった。制約として生成は移植元のローカル clone 前提、3e の未掲載候補は報告のみで維持。

## 2026-09-04: 論点 3 — manifest の excluded に置く concepts の当たり

docs-refresh 3a の対象 concepts を数えると 10 本 (core/api 8・maui/api 1・cross/reference 1)。利用者向けでないのは cross/reference/reference-repositories.md (ローカルパス対応表) のみ。KsSettingsView の excluded 5 本はすべて architecture カテゴリだったが KsDialogs には未だなく、phase-6 以降の蒸留で増える見込み。

選択肢: A. 今は 1 本 + architecture カテゴリは既定で除外候補 (3c 発火時に確定) / B. 今は 1 本のみで将来規則なし / C. excluded 空で始め初回失敗で決める。採用: A。理由: phase-2 の初期 manifest が生成時に完成し、将来の concept 追加時も既定値で即決できる。既定は候補であって自動除外ではない。

## 2026-09-04: 論点 4 — docs-refresh 3d の読む対象を 4 ビルドルートに合わせる

実物確認: AGP / Kotlin / minSdk / compileSdk は `android/gradle/libs.versions.toml` に集約され、KMP ルートは同 catalog を共有 (`kmp/settings.gradle.kts`)。Gradle wrapper は android / kmp の 2 本 (どちらも 9.7.0)。iOS は `ios/Package.swift` (tools 6.3 / iOS 17)。MAUI は csproj に TFM net10.0 系・SupportedOSPlatformVersion 17.0 / 24.0・Microsoft.Maui.Controls 10.0.1。README の表は Minimum OS 列のみで toolchain 版の記載なし。

選択肢: A. 4 行の表 (catalog から読む・wrapper 2 本・MAUI 本体下限を含む) / B. KsSettingsView の表を固有値差し替えで写す。採用: A。理由: B では minSdk / compileSdk が build.gradle.kts から取れず (参照のみ)、KMP wrapper と MAUI 本体下限 (消費者の csproj で効く値) を見ない。

これで論点は出尽くし、次は ksn-propose (フェーズ由来入力) で変更提案を起こす段階。
