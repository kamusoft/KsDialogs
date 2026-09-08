using System;
using System.Threading.Tasks;

namespace KsDialogs.ApiSurfaceCheck;

/// <summary>
/// 構成ミスの失敗型の公開面の正の検証。
/// </summary>
/// <remarks>
/// このファイルがコンパイルできることが検証結果であり、失敗型とその内訳が
/// 利用者から見えなくなればビルドが失敗する。
/// </remarks>
public static class DialogExceptionApiSurfaceChecks
{
    /// <summary>View の生成失敗は型で捕まえられ、内訳と原因を読める。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>読み取った View 型名・ViewModel 型名・原因の型名。失敗しなければ空。</returns>
    public static async Task<string> AcceptsCatchingViewCreationFailed(IKsDialog dialogs)
    {
        try
        {
            _ = await dialogs.ShowAsync(new ConsumerDialogViewModel());
        }
        catch (DialogException.ViewCreationFailed failure)
        {
            string viewTypeName = failure.ViewTypeName;
            string viewModelTypeName = failure.ViewModelTypeName;
            Exception? cause = failure.InnerException;
            return $"{viewTypeName}/{viewModelTypeName}/{cause?.GetType().FullName}";
        }

        return string.Empty;
    }

    /// <summary>生成失敗は構成ミスの失敗としてまとめても捕まえられる。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>捕まえた失敗の説明。失敗しなければ空。</returns>
    public static async Task<string> AcceptsCatchingAsDialogException(IKsDialog dialogs)
    {
        try
        {
            _ = await dialogs.ShowAsync(new ConsumerDialogViewModel());
        }
        catch (DialogException failure)
        {
            return failure.Message;
        }

        return string.Empty;
    }
}
