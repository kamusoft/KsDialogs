# layout-case-fixtures

共通ケース表 (`core/layout-spec/cases.json`) を instrumented test から読み、期待 rect と突き合わせるための
テスト補助コード。Android Native の複数モジュール (`ksdialogs-core` / `ksdialogs`) の androidTest が
同じ物を使うため、どちらのモジュールにも属さないここに置き、双方の androidTest ソースセットへ
ディレクトリごと足している。

ここに置くのは、ライブラリの公開 API だけで書ける補助に限る。モジュール内部 (internal) に触れる補助は
そのモジュールの androidTest 側に置く。
