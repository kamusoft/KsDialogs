import SwiftUI

/// 出入りの演出を選んでからダイアログを表示する画面。
struct SampleTransitionPanelScreen: View {
    /// 確定した結果をメニュー画面へ渡す。
    let onResult: (String) -> Void
    /// メニュー画面へ戻る。
    let onClose: () -> Void

    @State private var model = SampleTransitionPanelModel()

    /// プリセットのチップを並べる列。
    private let presetColumns = [
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8)
    ]

    var body: some View {
        VStack(spacing: 0) {
            SampleTransitionPanelHeader(onBack: onClose)
            SampleDivider()

            ScrollView {
                VStack(spacing: 0) {
                    SampleSectionCaption(title: SampleText.presetCaption)
                    presetChips
                    SampleSectionCaption(title: SampleText.adjustCaption)
                    adjustBlock

                    if let lastResult = model.lastResult {
                        SampleResultArea(result: lastResult)
                            .background(SampleTheme.surfaceVariant)
                            .padding(.top, 16)
                    }
                }
            }

            SampleDivider()
            showButton
        }
        .background { SampleTheme.surface.ignoresSafeArea() }
    }

    /// 演出を選ぶチップの並び。
    private var presetChips: some View {
        LazyVGrid(columns: presetColumns, spacing: 8) {
            ForEach(SampleTransitionChoice.allCases) { choice in
                SampleChip(
                    label: choice.label,
                    isSelected: choice == model.transitionChoice,
                    minHeight: 44,
                    fontSize: 14
                ) {
                    model.transitionChoice = choice
                }
            }
        }
        .padding(.horizontal, 16)
    }

    /// 時間とイージングの調整部。
    ///
    /// 無演出と自作フックは調整値を使わないため、値は保ったまま操作できなくする。
    private var adjustBlock: some View {
        VStack(spacing: 0) {
            HStack(spacing: 8) {
                Text(SampleText.durationLabel)
                    .font(.system(size: 14))
                    .foregroundStyle(SampleTheme.onSurface)
                Spacer(minLength: 8)
                Text(model.durationText)
                    .font(.system(size: 14, design: .monospaced))
                    .foregroundStyle(SampleTheme.onSurfaceMuted)
            }
            .padding(.bottom, 6)

            Slider(value: $model.durationMilliseconds, in: 100...600, step: 10)
                .tint(SampleTheme.primary)
                .accessibilityLabel(SampleText.durationLabel)
                .accessibilityValue(model.durationText)

            Text(SampleText.easingLabel)
                .font(.system(size: 14))
                .foregroundStyle(SampleTheme.onSurface)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 14)
                .padding(.bottom, 6)

            HStack(spacing: 8) {
                ForEach(SampleEasingChoice.allCases) { choice in
                    SampleChip(
                        label: choice.label,
                        isSelected: choice == model.easingChoice,
                        minHeight: 36,
                        fontSize: 12
                    ) {
                        model.easingChoice = choice
                    }
                }
            }
        }
        .padding(.horizontal, 16)
        .disabled(!model.allowsAdjustments)
        .opacity(model.allowsAdjustments ? 1 : 0.4)
    }

    /// 選んだ演出でダイアログを出す操作。
    private var showButton: some View {
        Button {
            Task { await showTransitionDialog() }
        } label: {
            Text(SampleText.displayAction)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(SampleTheme.onPrimary)
                .frame(maxWidth: .infinity, minHeight: 44)
                .background(SampleTheme.primary, in: .rect(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 16))
    }

    private func showTransitionDialog() async {
        if let result = await model.showTransitionDialog() {
            onResult(result)
        }
    }
}

#Preview {
    SampleTransitionPanelScreen(onResult: { _ in }, onClose: {})
}
