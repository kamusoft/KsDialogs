import SwiftUI

/// 撮影用の場面の背景に置く、架空のノートアプリの一覧画面。
///
/// ダイアログ等が「アプリのどこからでも出せる」ことが伝わるよう、メニュー画面ではなく
/// 実アプリらしい画面を敷く。中身は固定の見本データで、操作には反応しない。
struct ShowcaseNotesScreen: View {
    var body: some View {
        NavigationStack {
            List {
                Section("ピン留め") {
                    ForEach(ShowcaseNote.pinned) { note in
                        ShowcaseNoteRow(note: note)
                    }
                }
                Section("最近") {
                    ForEach(ShowcaseNote.recent) { note in
                        ShowcaseNoteRow(note: note)
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("ノート")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Image(systemName: "square.and.pencil")
                        .foregroundStyle(Color.orange)
                }
            }
        }
    }
}

/// ノート一覧の 1 行。色付きのアイコン・題・抜粋・日付を出す。
private struct ShowcaseNoteRow: View {
    let note: ShowcaseNote

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: note.symbol)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 40, height: 40)
                .background(note.color, in: .rect(cornerRadius: 10))

            VStack(alignment: .leading, spacing: 3) {
                HStack {
                    Text(note.title)
                        .font(.system(size: 16, weight: .semibold))
                        .lineLimit(1)
                    Spacer()
                    Text(note.date)
                        .font(.system(size: 13))
                        .foregroundStyle(.secondary)
                }
                Text(note.excerpt)
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
        }
        .padding(.vertical, 4)
    }
}

/// 見本のノート。
private struct ShowcaseNote: Identifiable {
    let id = UUID()
    let title: String
    let excerpt: String
    let date: String
    let symbol: String
    let color: Color

    static let pinned: [ShowcaseNote] = [
        ShowcaseNote(title: "旅行の持ち物リスト", excerpt: "パスポート、充電器、折りたたみ傘…", date: "9:12", symbol: "suitcase.fill", color: .orange),
        ShowcaseNote(title: "買い物リスト", excerpt: "牛乳、たまご、りんご 3 個、バナナ", date: "昨日", symbol: "cart.fill", color: .green),
    ]

    static let recent: [ShowcaseNote] = [
        ShowcaseNote(title: "週次ミーティング", excerpt: "リリース日程とレビューの担当を確認", date: "月曜日", symbol: "person.2.fill", color: .blue),
        ShowcaseNote(title: "読みたい本", excerpt: "デザインの本を 2 冊、小説を 1 冊", date: "11/2", symbol: "book.fill", color: .purple),
        ShowcaseNote(title: "レシピ: カレー", excerpt: "玉ねぎはあめ色になるまで炒める", date: "10/30", symbol: "fork.knife", color: .red),
        ShowcaseNote(title: "ジムのメニュー", excerpt: "スクワット 3 セット、ランニング 20 分", date: "10/28", symbol: "figure.run", color: .teal),
        ShowcaseNote(title: "アイデアメモ", excerpt: "設定画面にダークモードの切り替えを追加", date: "10/25", symbol: "lightbulb.fill", color: .yellow),
        ShowcaseNote(title: "引っ越しの手続き", excerpt: "住所変更、電気・ガス・水道の連絡", date: "10/20", symbol: "house.fill", color: .indigo),
    ]
}

#Preview {
    ShowcaseNotesScreen()
}
