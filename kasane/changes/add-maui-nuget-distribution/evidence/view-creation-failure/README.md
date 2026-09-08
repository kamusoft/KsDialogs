# View 生成失敗が呼び出し元へ届く型 (修正前後の A/B)

Scenario `MB-MA-14` の実機観測。1 行登録した View のコンストラクタが DI に登録されていない依存
(`System.Net.Http.HttpClient`) を要求する状態を Sample に一時的に作り、`Model Dialog` のデモを
起動引数 (`--demo model-dialog` / `--es demo model-dialog`) で自動再生して、show の失敗として
届いた例外の型・メッセージ・InnerException・公開プロパティを画面に出して撮った。
観測用の一時改変は 3 か所 (View のコンストラクタ引数 1 つ、デモの呼び出しを try / catch で包む、
失敗の公開プロパティを名前で読む補助) で、いずれも修正前後で同じソースを使っている
(公開プロパティはリフレクションで読むため、型が無い修正前でもそのままコンパイルできる)。
観測後に Sample は無改変へ戻した。

| ファイル | 何を撮ったか |
|---|---|
| `01-android-before.png` | 修正前ビルド (Android エミュレータ AVD `Pixel_6`、system image android-31)。`type=System.InvalidOperationException` / `inner=(none)` / `view=(none)` / `vm=(none)`。メッセージは互換面が文字列で運んだ `[System.InvalidOperationException]: Unable to resolve service for type 'System.Net.Http.HttpClient' while attempting to activate 'KsDialogs.Sample.Maui.ModelDialogCardView'.` で、型が失われている |
| `02-android-after.png` | 修正後ビルド。同じ端末・同じ起動引数。`type=KsDialogs.DialogException+ViewCreationFailed` / `inner=System.InvalidOperationException` / `view=KsDialogs.Sample.Maui.ModelDialogCardView` / `vm=KsDialogs.Sample.Maui.ModelDialogViewModel` |
| `03-ios-after.png` | 修正後ビルド (iOS Simulator、iPhone 17)。Android と同じ型・同じ内訳が届く |

修正前の観測は Android だけで行った (iOS は預かり口が既に配線済みで、修正前も型が保たれるため
A/B の対象ではない)。3 枚は別の観測回・別の端末で、md5 はいずれも相異なる。

Android の 2 枚は結果表示の全文を撮るために一覧を末尾までスクロールしてある (メニュー項目の
見えの差はスクロール位置の差)。iOS の 1 枚も同じ理由でスクロールしてある。
