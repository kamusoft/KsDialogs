# Android binding の警告 (Metadata.xml の改訂前後)

コマンド: cd maui/android/KsDialogs.Binding.Android && dotnet build -c Release --no-incremental
(改訂前は Metadata.xml の companion 節だけを外した状態。生ログは手元保管、ここは抜粋)

## 改訂前 (BG8401 が 4 種)
BG8401: 入れ子にされた型名が重複しているため、'KsDialogs.Bridge.MauiDialogBridge.Companion' をスキップしています。(Java の型: 'jp.kamusoft.ksdialogs.maui.MauiDialogBridge')
BG8401: 入れ子にされた型名が重複しているため、'KsDialogs.Bridge.MauiDialogContent.Companion' をスキップしています。(Java の型: 'jp.kamusoft.ksdialogs.maui.MauiDialogContent')
BG8401: 入れ子にされた型名が重複しているため、'KsDialogs.Bridge.MauiLoadingBridge.Companion' をスキップしています。(Java の型: 'jp.kamusoft.ksdialogs.maui.MauiLoadingBridge')
BG8401: 入れ子にされた型名が重複しているため、'KsDialogs.Bridge.MauiToastBridge.Companion' をスキップしています。(Java の型: 'jp.kamusoft.ksdialogs.maui.MauiToastBridge')

## 改訂後 (BG8401 なし。落としたフィールドの 2 度目の照合が BG8A00 になる)
40 warning BG8605
4 warning BG8606
16 warning BG8A00

BG8A00: Metadata.xml 要素 '<remove-node path="/api/package[@name='jp.kamusoft.ksdialogs.maui']/class[@name='MauiDialogBridge']/field[@name='Companion']" />' と一致するノードがありませんでした。
BG8A00: Metadata.xml 要素 '<remove-node path="/api/package[@name='jp.kamusoft.ksdialogs.maui']/class[@name='MauiDialogContent']/field[@name='Companion']" />' と一致するノードがありませんでした。
BG8A00: Metadata.xml 要素 '<remove-node path="/api/package[@name='jp.kamusoft.ksdialogs.maui']/class[@name='MauiLoadingBridge']/field[@name='Companion']" />' と一致するノードがありませんでした。
BG8A00: Metadata.xml 要素 '<remove-node path="/api/package[@name='jp.kamusoft.ksdialogs.maui']/class[@name='MauiToastBridge']/field[@name='Companion']" />' と一致するノードがありませんでした。

## facade の net10.0-android ビルド (Release)
コマンド: cd maui && dotnet build KsDialogs.Maui/KsDialogs.Maui.csproj -c Release -f net10.0-android
結果: 0 エラー。MauiDialogBridge.Shared / MauiLoadingBridge.Shared / MauiToastBridge.Shared と
MauiDialogContent.ApplyAttributes の参照は outer 型の static として解決している
