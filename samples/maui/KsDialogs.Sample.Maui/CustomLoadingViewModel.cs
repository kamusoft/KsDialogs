using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace KsDialogs.Sample.Maui;

/// <summary>Custom Loading の ViewModel。</summary>
/// <remarks>
/// 進捗の受け口 (<see cref="ILoadingProgressReceiver"/>) を実装しているため、スコープ形の処理が
/// 報告した進捗が表示中のあいだ転送される。届いた値は変更通知つきの状態として保持し、
/// 中身の View が binding で読む。
/// </remarks>
public sealed class CustomLoadingViewModel : ILoadingViewModel, ILoadingProgressReceiver, INotifyPropertyChanged
{
    private double progress;

    /// <summary>状態の変化を binding へ知らせる。</summary>
    public event PropertyChangedEventHandler? PropertyChanged;

    /// <summary>0〜1 に丸めた後の進捗。まだ報告が無い開始直後は 0。</summary>
    public double Progress
    {
        get => this.progress;
        private set
        {
            if (this.progress.Equals(value))
            {
                return;
            }

            this.progress = value;
            this.OnPropertyChanged();
            this.OnPropertyChanged(nameof(this.ProgressText));
        }
    }

    /// <summary>進捗の百分率 (小数なし)。</summary>
    public string ProgressText => SampleText.ProgressPercentage(this.Progress);

    /// <summary>進捗の報告を受け取る。呼び出しは UI スレッド上で行われる。</summary>
    /// <param name="progress">0〜1 に丸めた後の進捗値。</param>
    public void OnProgress(double progress) => this.Progress = progress;

    private void OnPropertyChanged([CallerMemberName] string? propertyName = null) =>
        this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
}
