import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.DialogViewModel
import kotlin.coroutines.cancellation.CancellationException

class ConfirmViewModel(val message: String) : DialogViewModel<Boolean>

@Throws(DialogException::class, CancellationException::class)
suspend fun showConfirmation(message: String): DialogResult<Boolean> =
    Dialog.instance.show(ConfirmViewModel(message))
