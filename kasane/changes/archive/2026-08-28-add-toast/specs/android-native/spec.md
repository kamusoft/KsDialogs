# android-native デルタ (add-toast)

dialog-contract の挙動 Scenario (TS-CO/NM/MX/AT/TR) は同名テストで全量検証する (core/ADR-0016)。レイアウト共通ケース表は Toast の器でも回す (design Decision 7)。本書は Kotlin 公開面に固有の差分のみ。

## ADDED Requirements

### Requirement: Kotlin 公開面の Toast

契約 interface `KsToast` + 既定シングルトン `Toast` を追加する (SHALL — core/ADR-0002)。show は同期 (非 suspend)・戻り値なしで、任意スレッドから呼べ内部で UI スレッドへ直列化する。本体 (ksdialogs) は従来 View 系の登録・インライン factory を持ち、Compose 系オーバーロードは ksdialogs-compose 側に置く (android/ADR-0001 — 本体は Compose 非依存を保つ)。`Toast` は設定プロパティ `style` (`ToastStyle`) と `registry` (`ToastViewRegistry`) を持つ。器は非フォーカス・タッチ素通しの全画面透過 Window で、Activity 再生成では器を使い捨てて新しい resumed Activity へ起動順のまま再取り付けする (design Decision 3・4)。

#### Scenario: [TS-AN-01] 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** show(message) の各省略形・duration / placement 引数・従来 View 系の登録・インライン factory 表示・ToastStyle の一括設定 (アプリ既定配置含む) を記述する
- **THEN** すべてコンパイルが通る

#### Scenario: [TS-AN-04] 公開面の負の compile 検査
- **GIVEN** 禁止形状ごとに独立してコンパイルを試みる検査ソース
- **WHEN** hide 相当の呼び出し / show の戻り値の受け取り / show へのスタイル引数 / DialogOptions 相当の設定プロパティ参照 / 本体 (ksdialogs) からの Compose API 参照を記述する
- **THEN** いずれもコンパイルエラーになる

#### Scenario: [TS-AN-02] Compose 登録のカスタム Toast が従来 View 登録と同じに働く
- **GIVEN** ksdialogs-compose の factory で登録した ToastViewModel 型
- **WHEN** 表示し、duration の経過で消えるまで待つ
- **THEN** 表示・配置・消滅の観察可能挙動が従来 View 登録と一致する

#### Scenario: [TS-AN-03] Activity 再生成で多重 Toast が起動順のまま再取り付けされる
- **GIVEN** 起動順に重なった複数の Toast を表示した Activity
- **WHEN** Activity が再生成される (回転等)
- **THEN** すべての Toast が新しい Activity 上に同じ重なり順で表示され続け、各表示の残り duration は維持される
