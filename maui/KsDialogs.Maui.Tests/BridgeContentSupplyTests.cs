using System;
using System.Diagnostics;
using KsDialogs.Maui.Tests.Support;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// 受理後に呼ばれる中身の供給元が失敗しても、その失敗が呼び出し境界の外へ出ないことを見る。
/// </summary>
/// <remarks>
/// カスタム View の中身は、Native の器が提示先を確保した後に互換面から C# の供給元を呼んで作られる。
/// この呼び出しは managed / native の境界を跨いでおり、そこを例外が越えると未処理の障害になる。
/// 供給元の失敗を値として返し、警告を残し、次の供給に影響させないところまでがこの面の責務で、
/// その先 (Toast はその 1 枚の破棄、Loading / Dialog は呼び出しの失敗) は Native の器が行う。
/// </remarks>
[TestFixture]
public class BridgeContentSupplyTests
{
    /// <summary>供給元が中身を返せば、その中身がそのまま返る。</summary>
    [Test]
    [Description("中身を作れた供給元の結果はそのまま返る")]
    public void TheSuppliedContentIsReturnedAsIs()
    {
        object content = new();

        object? supplied = BridgeContentSupply.CreateOrDiscard(() => content);

        Assert.That(supplied, Is.SameAs(content));
    }

    /// <summary>呼び出しへ失敗を返す面でも、供給元の結果はそのまま返る。</summary>
    [Test]
    [Description("呼び出しへ失敗を返す面でも中身はそのまま返る")]
    public void TheSuppliedContentIsReturnedAsIsOnTheFailingCallFace()
    {
        object content = new();
        BridgeContentFailure failure = new();

        object? supplied = BridgeContentSupply.CreateOrFail(() => content, failure);

        Assert.That(supplied, Is.SameAs(content));
        Assert.That(failure.Cause, Is.Null, "作れたのだから預かる失敗も無い");
    }

    /// <summary>供給元が投げた失敗は境界の外へ出ず、中身なしとして返る。</summary>
    [Test]
    [Description("供給元が投げた失敗は境界の外へ出ず null になる")]
    public void TheFailureOfTheSupplierDoesNotCrossTheBoundary()
    {
        object? supplied = null;

        Assert.DoesNotThrow(() =>
            supplied = BridgeContentSupply.CreateOrDiscard<object>(
                () => throw new InvalidOperationException("中身を作れません")));

        Assert.That(supplied, Is.Null, "中身なしとして返り、器はこの表示を破棄できる");
    }

    /// <summary>呼び出しへ失敗を返す面でも、失敗は境界の外へ出ず中身なしとして返る。</summary>
    [Test]
    [Description("呼び出しへ失敗を返す面でも失敗は境界の外へ出ず null になる")]
    public void TheFailureDoesNotCrossTheBoundaryOnTheFailingCallFace()
    {
        object? supplied = null;
        BridgeContentFailure failure = new();

        Assert.DoesNotThrow(() =>
            supplied = BridgeContentSupply.CreateOrFail<object>(
                () => throw new InvalidOperationException("中身を作れません"),
                failure));

        Assert.That(supplied, Is.Null, "中身なしとして返り、器はこの呼び出しを失敗にできる");
    }

    /// <summary>呼び出しへ失敗を返す面では、元の失敗がそのまま預かり口に残る。</summary>
    /// <remarks>
    /// 呼び出し元はまだ待っているため、互換面が用意した汎用の理由ではなく
    /// 利用者の factory が投げた失敗そのものを渡せる。gateway はこの預かりを見て投げ直す。
    /// </remarks>
    [Test]
    [Description("呼び出しへ失敗を返す面では元の失敗が型もメッセージも保って預かられる")]
    public void TheOriginalFailureIsHeldForTheWaitingCaller()
    {
        InvalidOperationException thrown = new("中身を作れません");
        BridgeContentFailure failure = new();

        BridgeContentSupply.CreateOrFail<object>(() => throw thrown, failure);

        Assert.That(failure.Cause, Is.SameAs(thrown), "元の失敗がそのまま呼び出し元へ渡せる");
        Assert.That(failure.Cause!.StackTrace, Is.Not.Null, "投げられた場所の手掛かりも残る");
    }

    /// <summary>型付きの失敗も、汎用の失敗に潰されずそのまま預かられる。</summary>
    [Test]
    [Description("型付きの失敗も潰されずそのまま預かられる")]
    public void ATypedFailureIsHeldAsItIs()
    {
        BridgeContentFailure failure = new();

        BridgeContentSupply.CreateOrFail<object>(
            () => throw new DialogException.PresentationHostUnavailable(),
            failure);

        Assert.That(failure.Cause, Is.TypeOf<DialogException.PresentationHostUnavailable>());
    }

    /// <summary>破棄される面 (Toast) には預かり口が無く、失敗は警告だけになる。</summary>
    /// <remarks>show が既に戻っており、渡す先の呼び出しが無いため預かる意味がない。</remarks>
    [Test]
    [Description("破棄される面は預かり口を持たない")]
    public void TheDiscardingFaceHoldsNothing()
    {
        BridgeContentFailure failure = new();

        BridgeContentSupply.CreateOrDiscard<object>(
            () => throw new InvalidOperationException("中身を作れません"));

        Assert.That(failure.Cause, Is.Null);
    }

    /// <summary>預かり口は呼び出し 1 回分なので、別の呼び出しの失敗は混ざらない。</summary>
    [Test]
    [Description("別の呼び出しの失敗は預かり口に混ざらない")]
    public void TheHolderOfAnotherCallIsNotAffected()
    {
        BridgeContentFailure failed = new();
        BridgeContentFailure succeeded = new();

        BridgeContentSupply.CreateOrFail<object>(
            () => throw new InvalidOperationException("中身を作れません"),
            failed);
        object? supplied = BridgeContentSupply.CreateOrFail(() => new object(), succeeded);

        Assert.That(failed.Cause, Is.Not.Null);
        Assert.That(succeeded.Cause, Is.Null, "成立した呼び出しは失敗を持ち込まれない");
        Assert.That(supplied, Is.Not.Null);
    }

    /// <summary>破棄される面の失敗は、破棄の扱いとともに警告に残る。</summary>
    [Test]
    [Description("破棄される面の失敗は破棄の扱いとともに警告として残る")]
    public void TheDiscardedFailureIsReportedAsAWarning()
    {
        RecordingTraceListener listener = new();
        Trace.Listeners.Add(listener);
        try
        {
            BridgeContentSupply.CreateOrDiscard<object>(
                () => throw new InvalidOperationException("中身を作れません"));
        }
        finally
        {
            Trace.Listeners.Remove(listener);
        }

        Assert.That(listener.Warnings, Has.Count.EqualTo(1));

        // 警告本文 (書式 + 効果句) の検査。ここが警告として出ている文言そのもの
        Assert.That(
            listener.Warnings[0],
            Does.StartWith(
                "Could not create the presentation content. "
                + "This presentation is discarded. Other presentations are not affected: "),
            "何が起きたかと、この表示だけが破棄されることが警告の書き出しで分かる");

        // 供給元が投げた失敗の説明。文言はこのテストが自分で投げた例外のもの
        Assert.That(
            listener.Warnings[0],
            Does.Contain("中身を作れません"),
            "原因を追える手掛かりとして、投げられた失敗の説明も警告に載る");
    }

    /// <summary>呼び出しへ返す面の失敗は、失敗として返る扱いとともに警告に残る。</summary>
    /// <remarks>元の失敗は呼び出し元へも渡るが、起きた時点の記録はこの警告に残る。</remarks>
    [Test]
    [Description("呼び出しへ返す面の失敗は失敗として返る扱いとともに警告として残る")]
    public void TheReportedFailureIsReportedAsAWarning()
    {
        RecordingTraceListener listener = new();
        Trace.Listeners.Add(listener);
        try
        {
            BridgeContentSupply.CreateOrFail<object>(
                () => throw new InvalidOperationException("中身を作れません"),
                new BridgeContentFailure());
        }
        finally
        {
            Trace.Listeners.Remove(listener);
        }

        Assert.That(listener.Warnings, Has.Count.EqualTo(1));

        // 警告本文 (書式 + 効果句) の検査。ここが警告として出ている文言そのもの
        Assert.That(
            listener.Warnings[0],
            Does.StartWith("Could not create the presentation content. This call fails: "),
            "何が起きたかと、呼び出しが失敗になることが警告の書き出しで分かる");

        // 供給元が投げた失敗の説明。文言はこのテストが自分で投げた例外のもの
        Assert.That(
            listener.Warnings[0],
            Does.Contain("中身を作れません"),
            "原因を追える手掛かりとして、投げられた失敗の説明も警告に載る");
    }

    /// <summary>失敗した供給の後でも、次の供給はそのまま成立する。</summary>
    [Test]
    [Description("失敗した供給は次の供給に影響しない")]
    public void AFailedSupplyDoesNotAffectTheNextSupply()
    {
        object content = new();

        BridgeContentSupply.CreateOrDiscard<object>(
            () => throw new InvalidOperationException("中身を作れません"));
        BridgeContentSupply.CreateOrFail<object>(
            () => throw new InvalidOperationException("中身を作れません"),
            new BridgeContentFailure());
        object? supplied = BridgeContentSupply.CreateOrFail(() => content, new BridgeContentFailure());

        Assert.That(supplied, Is.SameAs(content));
    }
}
