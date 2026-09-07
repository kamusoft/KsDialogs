# kmp-facade デルタ (add-loading)

commonMain の公開面は色を含む型を持たない (core/ADR-0023 の非対称 — DialogOptions と同じ)。挙動の全量検証は Native 側で、KMP はパススルーの成立を検証する。

## ADDED Requirements

### Requirement: 共有コードからの Loading 呼び出し

commonMain に契約 `interface KsLoading` + `expect object Loading` を追加する (SHALL — core/ADR-0002)。公開面は show / hide / setMessage / 値を返すスコープ形 `start` (進捗報告は関数型 `(Double) -> Unit` 相当) と placement 引数のみで、スタイル (色・フォントを含む `LoadingStyle`) は commonMain に公開しない — スタイルは各 OS 側 (Android Native / iOS Native) の設定プロパティで行う。カスタム Loading の View 登録は各 OS 側で行い、共有 VM 型をキーに共有コードから表示できる。

#### Scenario: [LD-KM-01] 共有コードの start が両 OS で表示・進捗・終了まで到達する
- **GIVEN** UI 層を参照しない共有コードと、既定ローディングを表示できるホスト
- **WHEN** 共有コードからスコープ形 start を呼び、処理が進捗を報告して値を返す
- **THEN** Native 側で表示・進捗反映・終了 (表示消滅) が観察でき、共有コードが処理の戻り値を受け取る (両 OS で成立)

#### Scenario: [LD-KM-02] commonMain の公開面にスタイル型が存在しない
- **GIVEN** commonMain の公開 API 面 (api-surface-check)
- **WHEN** Loading の公開面を検査する
- **THEN** show / hide / setMessage / start / placement と共有 VM 契約・進捗受け口のみが公開され、色・フォントを含むスタイル型・options 型は commonMain に存在しない

### Requirement: 共有 VM によるカスタム Loading

commonMain にカスタム Loading の共有呼び出し面を持つ (SHALL): VM を受ける表示 (`show(vm, placement?)`) とスコープ形 (`start(vm, placement?, action)`) のオーバーロード、VM 契約 (Dialog の共有 VM 契約と同じ参照型限定)、進捗受け口 interface (`onProgress(Double)` 相当 — 関数のみのため commonMain に定義できる)。Android 側は Native の型との typealias で追加実装なしに Native 登録が働く (Dialog の VM 供給と同型)。iOS 側の利用者向け登録面は Swift パッケージ側の KMP 面 (kmp/ADR-0003・0004 の型付き facade) に Loading 版を追加し、内部 interop 型を利用者に直接使わせない。

#### Scenario: [LD-KM-03] 共有 VM のカスタム Loading が両 OS で通る
- **GIVEN** 進捗受け口を実装した共有 VM 型を、Android は Native 登録・iOS は Swift パッケージ KMP 面の登録で構成したホスト
- **WHEN** 共有コードからスコープ形 start (vm) を呼び、処理が進捗を報告する
- **THEN** カスタム View が表示され、進捗が VM の受け口へ届き、完了で表示が消える (両 OS で成立)

#### Scenario: [LD-KM-04] iOS Swift パッケージ KMP 面の Loading 登録の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** Swift パッケージ KMP 面の型付き登録 (Loading 版) と共有 VM の表示呼び出しを記述する
- **THEN** コンパイルが通る (内部 interop 型を直接参照せずに書ける)
