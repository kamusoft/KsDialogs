import KsDialogs
import SwiftUI

// 撮影用の場面 (ShowcaseScene.swift) で出す中身。
// スライドの端末は縮小して載るため、メニュー画面のデモより一回り大きな字・余白で描く。
// 中身はその場で渡すため、どの ViewModel 型もレジストリに登録しない (core/ADR-0013)。

/// 撮影用の配色。
private enum ShowcasePalette {
    /// 破壊的な操作の色。
    static let destructive = Color(red: 0xE5 / 255, green: 0x48 / 255, blue: 0x4D / 255)
    /// 破壊的な操作の色を薄くした地。
    static let destructiveSoft = Color(red: 0xFD / 255, green: 0xEC / 255, blue: 0xEC / 255)
    /// 進捗リングとアクセントの色。
    static let accent = Color(red: 0xF3 / 255, green: 0x9A / 255, blue: 0x2E / 255)
    /// 完了トーストの地 (濃い灰)。
    static let toastBackground = Color(red: 0x1F / 255, green: 0x29 / 255, blue: 0x37 / 255)
    /// 完了の印の色。
    static let success = Color(red: 0x34 / 255, green: 0xC7 / 255, blue: 0x59 / 255)
}

// MARK: - 削除確認ダイアログ

/// 削除確認ダイアログの ViewModel。結果は既定の真偽値。
final class ShowcaseDeleteDialogViewModel: DialogViewModel {}

/// 削除確認ダイアログの中身。アイコン・見出し・本文・2 つのボタンを持つ。
struct ShowcaseDeleteDialogCard: View {
    let onCancel: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Image(systemName: "trash.fill")
                .font(.system(size: 26, weight: .semibold))
                .foregroundStyle(ShowcasePalette.destructive)
                .frame(width: 64, height: 64)
                .background(ShowcasePalette.destructiveSoft, in: .circle)
                .padding(.bottom, 16)

            Text("ノートを削除しますか？")
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(SampleTheme.onSurface)
                .padding(.bottom, 8)

            Text("「旅行の持ち物リスト」を削除します。\nこの操作は取り消せません。")
                .font(.system(size: 15))
                .foregroundStyle(SampleTheme.onSurfaceMuted)
                .multilineTextAlignment(.center)
                .lineSpacing(3)
                .padding(.bottom, 24)

            HStack(spacing: 12) {
                Button(action: onCancel) {
                    Text("キャンセル")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(SampleTheme.onSurface)
                        .frame(maxWidth: .infinity, minHeight: 50)
                }
                .background(SampleTheme.surfaceVariant, in: .rect(cornerRadius: 14))

                Button(action: onDelete) {
                    Text("削除")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity, minHeight: 50)
                }
                .background(ShowcasePalette.destructive, in: .rect(cornerRadius: 14))
            }
        }
        .padding(EdgeInsets(top: 28, leading: 22, bottom: 22, trailing: 22))
        .frame(width: 310)
        .background(SampleTheme.surface, in: .rect(cornerRadius: 26))
        .shadow(color: .black.opacity(0.18), radius: 24, y: 8)
    }
}

// MARK: - アップロード中のローディング

/// アップロード中のローディングの ViewModel。進捗は撮影用に固定値で描くため持たない。
final class ShowcaseUploadLoadingViewModel: LoadingViewModel {}

/// アップロード中のローディングの中身。進捗リングと件数を出す。
struct ShowcaseUploadLoadingCard: View {
    /// 撮影用に固定した進捗。
    private let progress: Double = 0.6

    var body: some View {
        VStack(spacing: 14) {
            ZStack {
                Circle()
                    .stroke(SampleTheme.surfaceVariant, lineWidth: 10)
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(ShowcasePalette.accent, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                Text("\(Int(progress * 100))%")
                    .font(.system(size: 24, weight: .bold, design: .rounded))
                    .foregroundStyle(SampleTheme.onSurface)
            }
            .frame(width: 104, height: 104)

            VStack(spacing: 4) {
                Text("写真をアップロード中")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(SampleTheme.onSurface)
                Text("3 / 5 枚")
                    .font(.system(size: 14))
                    .foregroundStyle(SampleTheme.onSurfaceMuted)
            }
        }
        .padding(EdgeInsets(top: 28, leading: 24, bottom: 24, trailing: 24))
        .frame(width: 240)
        .background(SampleTheme.surface, in: .rect(cornerRadius: 24))
        .shadow(color: .black.opacity(0.18), radius: 24, y: 8)
    }
}

// MARK: - トースト

/// 下部に出す完了トーストの ViewModel。
final class ShowcaseSavedToastViewModel: ToastViewModel {}

/// 下部に出す完了トーストの中身。
struct ShowcaseSavedToastCard: View {
    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 24))
                .foregroundStyle(ShowcasePalette.success)
            Text("ノートを保存しました")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(.white)
        }
        .padding(EdgeInsets(top: 16, leading: 22, bottom: 16, trailing: 26))
        .background(ShowcasePalette.toastBackground, in: .capsule)
        .shadow(color: .black.opacity(0.2), radius: 12, y: 4)
    }
}

#Preview("Dialog") {
    ShowcaseDeleteDialogCard(onCancel: {}, onDelete: {})
}

#Preview("Loading") {
    ShowcaseUploadLoadingCard()
}

#Preview("Toast") {
    ShowcaseSavedToastCard()
}
