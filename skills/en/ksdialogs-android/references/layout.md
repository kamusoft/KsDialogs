# Attach layout

The size of a Dialog, its position, the overlay behind it, and the handling of outside taps are specified by attaching them to the content. This document covers the attributes that can be attached, how their values are resolved, and how to write the attachment in Compose and in Views.

## The two attached values

| Value | What it supplies | Routes it can take |
|---|---|---|
| `DialogOptions` | Size, reference area, overlay, outside-tap handling | Attachment only |
| `DialogPlacement` | Where it sits (alignment and offset) | Attachment and the `show` argument |

Both are `data class` types whose constructor arguments all have defaults. Numbers are `Double` values in dp, not pixels.

### `DialogOptions` properties

| Property | Type | What it supplies | Default |
|---|---|---|---|
| `layoutArea` | `DialogLayoutArea` (`WINDOW` / `VISIBLE_AREA`) | The area that size and position are computed against | `VISIBLE_AREA` |
| `dialogMargin` | `DialogEdgeInsets` | The margin deducted from each edge of the reference area | 24 on every edge |
| `proportionalWidth` / `proportionalHeight` | `Double` | The ratio against the reference area on that axis | `-1.0` (unspecified) |
| `overlayColor` | `Int` (`@ColorInt` ARGB 32-bit) | The color covering the area behind the Dialog | `0x66000000` (black 40%) |
| `isCanceledOnTouchOutside` | `Boolean` | Whether an outside tap cancels | `true` |

`DialogEdgeInsets` takes the four edges in `top`, `left`, `bottom`, `right` order. Write `DialogEdgeInsets(24.0)` for one value on every edge, or `DialogEdgeInsets.ZERO` for no margin.

### `DialogPlacement` properties

| Property | Type | What it supplies | Default |
|---|---|---|---|
| `horizontalAlignment` / `verticalAlignment` | `DialogAlignment` (`START` / `CENTER` / `END` / `FILL`) | The alignment on that axis | `CENTER` |
| `offsetX` / `offsetY` | `Double` | The translation applied after alignment. Positive `offsetX` moves right, positive `offsetY` moves down | `0.0` |

`START` and `END` in `DialogAlignment` are physical directions (left and right on the horizontal axis, top and bottom on the vertical one) and do not follow the writing direction.

## How values are resolved

- **Ratios** — values greater than `0` and at most `1` are used as they are, values above `1` are clamped to `1`. Zero, negative, and non-finite values mean unspecified
- **Margins** — a negative edge is clamped to `0` on that edge alone, and a non-finite edge falls back to the default 24
- **Offsets** — non-finite values become `0`
- **Size precedence** — ratio > `FILL` alignment > the content's own size. On a single axis a ratio beats `FILL`, and `FILL` then acts as center alignment
- **Bounds** — a ratio is taken against the area before the margin is deducted, while the size limit is the area after the deduction. Offsets are not clamped to that limit, so they can push content off screen
- **Outside taps** — when `isCanceledOnTouchOutside` is `true`, a tap outside the content's bounds yields `Cancelled`. When it is `false`, the tap does nothing and does not pass through to the screen behind. This works independently of the `overlayColor` value

## Which values take effect

The effective values are the ones attached when the first native layout pass finished.

- Rewriting the attached values while the Dialog is shown does not affect the Dialog already on screen
- When the window dimensions or the system bar insets change, the Dialog is repositioned with those same effective values
- The soft keyboard appearing or disappearing does not move it
- A `placement` passed to `show` replaces the attached `DialogPlacement` as a whole object. It is not merged field by field
- There is no route that passes `DialogOptions` as a `show` argument

## Attach to Compose content

Declare `KsDialogAttributes` at the top of the content. A declaration written only inside a lazily evaluated scope such as `LazyColumn` does not run on the first composition and has no effect on that presentation. `KsDialogAttributes` comes from the Compose artifact `jp.kamusoft:ksdialogs`.

The following is sheet-style content pinned to the bottom edge at full width, attaching the margin, ratios, overlay, and placement together.

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogEdgeInsets
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.compose.KsDialogAttributes

@Composable
fun SheetConfirmContent(viewModel: ConfirmViewModel, notifier: DialogNotifier<Boolean>) {
    KsDialogAttributes(
        options = DialogOptions(
            dialogMargin = DialogEdgeInsets(16.0),
            proportionalWidth = 1.0,
            proportionalHeight = 0.8,
            isCanceledOnTouchOutside = true,
        ),
        placement = DialogPlacement(
            horizontalAlignment = DialogAlignment.FILL,
            verticalAlignment = DialogAlignment.END,
            offsetY = -12.0,
        ),
    )
    Column {
        Text(viewModel.message)
        Button(onClick = { notifier.complete(true) }) { Text("OK") }
    }
}
```

## Attach to View content

Set the `View` extension properties `ksDialogOptions` and `ksDialogPlacement`. The values are stored as View tags, so setting them while building the View is enough.

The following writes the same sheet-style content as a View, changing the reference area to the whole window and giving each edge a different margin.

```kotlin
import android.content.Context
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogEdgeInsets
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.ksDialogOptions
import jp.kamusoft.ksdialogs.ksDialogPlacement

class SheetConfirmCardView(
    context: Context,
    viewModel: ConfirmViewModel,
    notifier: DialogNotifier<Boolean>,
) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        addView(TextView(context).apply { text = viewModel.message })
        addView(
            Button(context).apply {
                text = "OK"
                setOnClickListener { notifier.complete(true) }
            },
        )
        ksDialogOptions = DialogOptions(
            layoutArea = DialogLayoutArea.WINDOW,
            dialogMargin = DialogEdgeInsets(24.0, 20.0, 32.0, 20.0),
            proportionalWidth = 0.9,
            overlayColor = Color.argb(77, 0, 0, 0),
            isCanceledOnTouchOutside = false,
        )
        ksDialogPlacement = DialogPlacement(verticalAlignment = DialogAlignment.END)
    }
}
```

## Register

Attachment is written on the content side, so registration looks no different from content without attributes.

```kotlin
import android.app.Application
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Dialog.instance.registry.registerCompose(ConfirmViewModel::class) { viewModel, notifier ->
            SheetConfirmContent(viewModel, notifier)
        }
    }
}
```

For View content, call `register` in the same place.

```kotlin
Dialog.instance.registry.register(ConfirmViewModel::class, ::SheetConfirmCardView)
```

## Change the position for a single presentation

Passing `placement` to `show` replaces the attached position for that call alone. The attached `DialogOptions` still applies.

The following shows registered content near the top edge, moved down by 20 dp.

```kotlin
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import kotlinx.coroutines.launch

class ItemActivity : ComponentActivity() {
    fun onDeleteClicked() {
        lifecycleScope.launch {
            Dialog.instance.show(
                ConfirmViewModel("Delete this item?"),
                placement = DialogPlacement(
                    horizontalAlignment = DialogAlignment.CENTER,
                    verticalAlignment = DialogAlignment.START,
                    offsetY = 20.0,
                ),
            )
        }
    }
}
```
