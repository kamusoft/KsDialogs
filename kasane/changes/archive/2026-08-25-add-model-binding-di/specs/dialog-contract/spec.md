# dialog-contract デルタ (add-model-binding-di)

Scenario ID は `MB-<領域>-<NN>` (design Decision 7)。挙動系は両 Native のテスト名に ID を含めて対応付ける (core/ADR-0016)。

## ADDED Requirements

### Requirement: notifier の VM 供給

show は View 生成前に notifier を VM に紐付け、show 中 (紐付けから終端まで) は VM から notifier を取得できる (SHALL)。紐付けと取得はインスタンス同一性で行い、VM の等価比較 (equals 相当) に依存しない。show の前および終端後の VM からは notifier を取得できない。**紐付けの除去は、紐付け後に show が終わる全経路 — 正常な結果配送・View factory の例外・提示の失敗・呼び出し元キャンセル・OS 発の器消失 — で必ず行い、呼び出し元へ結果または例外が渡る前に除去が観察可能である** (除去漏れにより同じ VM の次回 show が並行 show と誤判定されてはならない)。紐付けは登録形 (factory の notifier 引数の有無)・呼び出し経路 (インスタンス渡し / 型指定 / インライン) によらず常に行われ、VM 経由の notifier と factory 引数の notifier は同じ配送先を指す。VM 契約は参照型 (class) に限定する — 強制手段は形態別 (Swift: AnyObject 制約 / C#: class 制約 / Kotlin: value class の実行時拒否、各形態 spec 参照)。同一 VM インスタンスを並行して show した場合は構成ミスとして失敗し (結果に化けない)、表示中の先行 show には影響しない。

#### Scenario: [MB-NI-01] VM 引数のみの factory で登録した View が VM 経由の notifier で結果を報告できる
- **GIVEN** VM 引数のみの factory (`(vm) → View`) で登録したコンテンツ
- **WHEN** show で表示し、factory 実行中に VM から notifier を取得して View に保持させ、その notifier で completed を報告する
- **THEN** factory 実行中の取得結果は空でなく、show が宣言結果型の completed で完了する (factory 完了後の紐付けでは通らない検証であること)

#### Scenario: [MB-NI-02] show 前の VM からは notifier を取得できない
- **GIVEN** 生成しただけでまだ show していない VM
- **WHEN** VM から notifier を取得する
- **THEN** 取得結果は空 (nil / null) である

#### Scenario: [MB-NI-03] 結果配送後の VM からは notifier を取得できない
- **GIVEN** show して結果が配送し終わった VM
- **WHEN** VM から notifier を取得する
- **THEN** 取得結果は空 (nil / null) である

#### Scenario: [MB-NI-04] 同一 VM インスタンスの並行 show は構成ミスとして失敗する
- **GIVEN** 表示中のダイアログの VM インスタンス
- **WHEN** 同じインスタンスをもう一度 show する
- **THEN** 2度目の show は構成ミスとして失敗し (cancelled 等の結果に化けない)、表示中のダイアログはそのまま操作でき、報告した結果が先行 show に配送される

#### Scenario: [MB-NI-05] 2引数 factory でも VM 経由の notifier は同じ配送先を指す
- **GIVEN** 従来の2引数 factory (`(vm, notifier) → View`) で登録したコンテンツ
- **WHEN** show で表示し、View が factory 引数の notifier ではなく VM から取得した notifier で報告する
- **THEN** show がその報告の結果で完了する

#### Scenario: [MB-NI-06] 等価な別インスタンスの VM は互いに干渉しない
- **GIVEN** 等価比較 (equals 相当) が一致するが別インスタンスの VM 2つ
- **WHEN** 両方を show して片方の VM の notifier で報告する
- **THEN** 報告した側の show だけが完了し、もう片方は表示されたままになる

#### Scenario: [MB-NI-07] 異常終了でも紐付けが除去され、同じ VM を再 show できる
- **GIVEN** 初回の View factory 呼び出しだけ例外を投げる factory で登録した VM
- **WHEN** show が factory 例外で失敗した後、同じ VM インスタンスをもう一度 show する
- **THEN** 失敗直後の VM から notifier は取得できず、2度目の show は並行 show と誤判定されずに表示・完了できる

### Requirement: 型指定呼び出し

レジストリには VM 型キー → VM factory を登録できる (SHALL)。エントリは View factory と VM factory の2スロットを持ち、再登録は**スロット単位の後勝ち** — 該当スロットだけを置換し他方は保持する。show 時の解決は呼び出し時点のエントリのスナップショットによる。型指定 show は VM factory で VM を生成し、configure (省略可・非同期可) を VM に適用してから View 生成・提示に進む。configure の完了は View 生成より前である。VM factory と configure は View factory と同じ UI スレッド保証で実行される。VM factory・configure が例外 (キャンセルを含む) で終わった場合は提示に進まず、その失敗が呼び出し元へ伝播し、結果には化けない。VM factory 未登録の型指定 show は構成ミスとして失敗し、cancelled 等の結果に化けない。notifier の供給・結果型の復元はインスタンス渡し show と同一の規則に従う。

#### Scenario: [MB-TS-01] 型指定 show が生成 → configure → 表示 → 結果の一連で動く
- **GIVEN** VM factory と View factory を登録した VM 型
- **WHEN** 型指定 show を configure (VM の状態を設定する) 付きで呼び、View が VM 経由の notifier で completed を報告する
- **THEN** configure で設定した状態が表示された View から観察でき、show が宣言結果型の completed で完了する

#### Scenario: [MB-TS-02] 非同期 configure の完了まで View 生成が始まらない
- **GIVEN** 完了までの間を制御できる非同期 configure
- **WHEN** 型指定 show を呼ぶ
- **THEN** configure が完了するまで View factory は呼ばれず、完了後に表示される

#### Scenario: [MB-TS-03] VM factory 未登録の型指定 show は構成ミスとして失敗する
- **GIVEN** View factory のみ登録され VM factory が未登録の VM 型
- **WHEN** 型指定 show を呼ぶ
- **THEN** 構成ミスとして失敗する (cancelled 等の結果に化けない)

#### Scenario: [MB-TS-04] configure 省略の型指定 show は VM factory の生成物をそのまま表示する
- **GIVEN** VM factory と View factory を登録した VM 型
- **WHEN** configure を省略して型指定 show を呼ぶ
- **THEN** VM factory が生成した VM の状態のまま表示され、結果の配送はインスタンス渡し show と同じに働く

#### Scenario: [MB-TS-05] configure の例外は提示に進まず伝播する
- **GIVEN** VM factory と View factory を登録した VM 型と、例外を投げる configure
- **WHEN** 型指定 show を呼ぶ
- **THEN** View factory は呼ばれず、configure の失敗が呼び出し元へ伝播し (結果に化けない)、同じ VM 型のその後の型指定 show は正常に動く

#### Scenario: [MB-TS-06] 再登録はスロット単位の後勝ちで他方を保持する
- **GIVEN** VM factory と View factory の両方を登録した VM 型
- **WHEN** View factory だけを別の factory で再登録し、型指定 show を呼ぶ
- **THEN** 新しい View factory が使われ、VM factory は保持されたまま型指定 show が成立する
