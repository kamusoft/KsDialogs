using System;

namespace KsDialogs;

// 各入れ子型の Message (診断文言) は英語固定でローカライズしない (cross/ADR-0015)。
/// <summary>
/// show が結果 (completed / cancelled) を返せない構成エラー。
/// </summary>
/// <remarks>
/// 利用者の操作結果ではなくプログラミングエラーであり、cancelled に化けさせずに投げる。
/// MAUI では show が返す <see cref="System.Threading.Tasks.Task"/> の失敗として届く。
/// この例外が投げられた場合、View は生成も表示もされない。
/// </remarks>
public abstract class DialogException : Exception
{
    // 派生をこのファイルの入れ子の型に閉じるため、基底のコンストラクタは入れ子の型からしか呼べない
    private DialogException(string message) : base(message)
    {
    }

    // 元の失敗を保持したまま報告する入れ子の型のための基底コンストラクタ
    private DialogException(string message, Exception innerException) : base(message, innerException)
    {
    }

    /// <summary>ViewModel 型に対する View factory がレジストリに登録されていない。</summary>
    public sealed class ViewFactoryNotRegistered : DialogException
    {
        internal ViewFactoryNotRegistered(string viewModelTypeName)
            : base($"No View factory is registered for ViewModel type {viewModelTypeName}.")
            => ViewModelTypeName = viewModelTypeName;

        /// <summary>解決できなかった ViewModel の型名。</summary>
        public string ViewModelTypeName { get; }
    }

    /// <summary>
    /// ViewModel 型に対する ViewModel factory がレジストリに登録されていない (型指定 show の構成ミス)。
    /// </summary>
    public sealed class ViewModelFactoryNotRegistered : DialogException
    {
        internal ViewModelFactoryNotRegistered(string viewModelTypeName)
            : base($"No ViewModel factory is registered for ViewModel type {viewModelTypeName}.")
            => ViewModelTypeName = viewModelTypeName;

        /// <summary>解決できなかった ViewModel の型名。</summary>
        public string ViewModelTypeName { get; }
    }

    /// <summary>
    /// 1 行登録が結び付けた View を組み立てられなかった。
    /// </summary>
    /// <remarks>
    /// 1 行登録 (<c>RegisterForDialog</c> / <c>RegisterForLoading</c> / <c>RegisterForToast</c>) では
    /// ライブラリ自身が View を生成するため、その生成が失敗したことを「factory 未登録」と区別して
    /// 報告する。元の失敗は <see cref="Exception.InnerException"/> にそのまま残る。
    /// 利用者が書いた factory や fallback resolver が投げた例外はこの型に包まれず、そのまま届く。
    /// </remarks>
    public sealed class ViewCreationFailed : DialogException
    {
        internal ViewCreationFailed(string viewTypeName, string viewModelTypeName, Exception innerException)
            : base(
                $"Could not create the View {viewTypeName} registered for ViewModel type {viewModelTypeName}.",
                innerException)
        {
            ViewTypeName = viewTypeName;
            ViewModelTypeName = viewModelTypeName;
        }

        /// <summary>生成できなかった View の型名。</summary>
        public string ViewTypeName { get; }

        /// <summary>その View に結び付いた ViewModel の型名。</summary>
        public string ViewModelTypeName { get; }
    }

    /// <summary>
    /// 表示中の ViewModel インスタンスを重ねて show しようとした。
    /// </summary>
    /// <remarks>
    /// 結果報告口はインスタンスに 1 つだけ紐付くため、同じインスタンスの並行表示は成立しない
    /// (core/ADR-0018)。先に表示されているダイアログはこの失敗の影響を受けない。
    /// </remarks>
    public sealed class ViewModelAlreadyShowing : DialogException
    {
        internal ViewModelAlreadyShowing(string viewModelTypeName)
            : base($"This ViewModel instance of type {viewModelTypeName} is already being shown.")
            => ViewModelTypeName = viewModelTypeName;

        /// <summary>重ねて show しようとした ViewModel の型名。</summary>
        public string ViewModelTypeName { get; }
    }

    /// <summary>
    /// 値型を ViewModel として表示しようとした。
    /// </summary>
    /// <remarks>
    /// 結果報告口はインスタンスの同一性で紐付くため、boxing のたびに別インスタンスになる値型は
    /// ViewModel にできない (core/ADR-0018)。登録と型指定 show は <c>class</c> 制約でコンパイル時に
    /// 拒否するが、interface で受けるインスタンス渡し show には制約を書けないため、提示の入口で
    /// この失敗として弾く。
    /// </remarks>
    public sealed class ValueTypeViewModel : DialogException
    {
        internal ValueTypeViewModel(string viewModelTypeName)
            : base($"ViewModel type {viewModelTypeName} is a value type and cannot be used as a ViewModel.")
            => ViewModelTypeName = viewModelTypeName;

        /// <summary>拒否された ViewModel の型名。</summary>
        public string ViewModelTypeName { get; }
    }

    /// <summary>
    /// DI コンテナ由来の解決 (1 行登録・fallback resolver) が要る場面で、アプリの
    /// <see cref="IServiceProvider"/> が用意されていない。
    /// </summary>
    /// <remarks>
    /// <c>AddKsDialogs</c> / <c>RegisterForDialog</c> が登録する初期化サービスは、アプリの起動時に
    /// provider をライブラリへ渡す。この失敗はその配線が済む前に show を呼んだことを示す。
    /// </remarks>
    public sealed class ServiceProviderUnavailable : DialogException
    {
        internal ServiceProviderUnavailable()
            : base("The app's IServiceProvider is not available yet.")
        {
        }
    }

    /// <summary>アクティブな提示先の画面が存在しない。キューイングはせず即座に失敗する。</summary>
    public sealed class PresentationHostUnavailable : DialogException
    {
        internal PresentationHostUnavailable() : base("No screen is available to present the Dialog.")
        {
        }
    }
}
