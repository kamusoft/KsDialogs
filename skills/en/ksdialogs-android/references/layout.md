# Attach layout

The size of a Dialog, its position, the overlay behind it, and the handling of outside taps are specified by attaching them to the content. This document covers the attributes that can be attached, how their values are resolved, how to write the attachment in Compose and in Views, and placement against the current page (the area inside the screen's bars).

## The two attached values

| Value | What it supplies | Routes it can take |
|---|---|---|
| `DialogOptions` | Size, reference area, overlay, outside-tap handling | Attachment only |
| `DialogPlacement` | Where it sits (alignment and offset) | Attachment and the `show` argument |

Both are `data class` types whose constructor arguments all have defaults. Numbers are `Double` values in dp, not pixels.

### `DialogOptions` properties

| Property | Type | What it supplies | Default |
|---|---|---|---|
| `layoutArea` | `DialogLayoutArea` (`WINDOW` / `VISIBLE_AREA` / `CURRENT_PAGE`) | The area that size and position are computed against | `VISIBLE_AREA` |
| `dialogMargin` | `DialogEdgeInsets` | The margin deducted from each edge of the reference area | 0 on every edge |
| `proportionalWidth` / `proportionalHeight` | `Double` | The ratio against the reference area on that axis | `-1.0` (unspecified) |
| `overlayColor` | `Int` (`@ColorInt` ARGB 32-bit) | The color covering the area behind the Dialog | `0x66000000` (black 40%) |
| `isCanceledOnTouchOutside` | `Boolean` | Whether an outside tap cancels | `true` |

`DialogEdgeInsets` takes the four edges in `top`, `left`, `bottom`, `right` order. Write `DialogEdgeInsets(24.0)` for one value on every edge, or `DialogEdgeInsets.ZERO` for no margin.

Each `DialogLayoutArea` value selects one reference area. Whichever is chosen applies to both the horizontal and vertical axes.

| Value | Reference area |
|---|---|
| `WINDOW` | The whole window the Dialog is placed in |
| `VISIBLE_AREA` | The window minus the space taken by the system bars and similar |
| `CURRENT_PAGE` | The part of the current page's rectangle, as told by the app, that overlaps the visible area (see "Place against the current page") |

### `DialogPlacement` properties

| Property | Type | What it supplies | Default |
|---|---|---|---|
| `horizontalAlignment` / `verticalAlignment` | `DialogAlignment` (`START` / `CENTER` / `END` / `FILL`) | The alignment on that axis | `CENTER` |
| `offsetX` / `offsetY` | `Double` | The translation applied after alignment. Positive `offsetX` moves right, positive `offsetY` moves down | `0.0` |

`START` and `END` in `DialogAlignment` are physical directions (left and right on the horizontal axis, top and bottom on the vertical one) and do not follow the writing direction.

## How values are resolved

- **Ratios** — values greater than `0` and at most `1` are used as they are, values above `1` are clamped to `1`. Zero, negative, and non-finite values mean unspecified
- **Margins** — a negative edge and a non-finite edge are both clamped to `0` on that edge alone
- **Offsets** — non-finite values become `0`
- **Size precedence** — ratio > `FILL` alignment > the content's own size. On a single axis a ratio beats `FILL`, and `FILL` then acts as center alignment
- **Bounds** — a ratio is taken against the area before the margin is deducted, while the size limit is the area after the deduction. Offsets are not clamped to that limit, so they can push content off screen
- **Outside taps** — when `isCanceledOnTouchOutside` is `true`, a tap outside the content's bounds yields `Cancelled`. When it is `false`, the tap does nothing and does not pass through to the screen behind. This works independently of the `overlayColor` value

## Which values take effect

The effective values are the ones attached when the first native layout pass finished.

- Rewriting the attached values while the Dialog is shown does not affect the Dialog already on screen
- When the window dimensions or the system bar insets change, the Dialog is repositioned with those same effective values
- The soft keyboard appearing or disappearing does not move it
- For `CURRENT_PAGE`, the page rectangle is fetched again when a presentation starts and on each such repositioning. The current page changing through navigation alone does not trigger a repositioning
- The container does not change the host screen's system bar settings (icon contrast, bar visibility), and a transparent `overlayColor` does not change that either
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

## Place against the current page

Choosing `DialogLayoutArea.CURRENT_PAGE` computes size and position against the inside of the page, excluding the screen's top and bottom bars. With bottom alignment on a screen that has a tab bar, the Dialog's bottom edge sits `dialogMargin` above the top of the tab bar, and proportional sizes are ratios of the height without the tab bar.

Android has no OS-level notion of a "page", so the app tells the library where the page is. There are two ways; when both are present the upper one wins, and the lower one is used when the upper one has no candidate.

| Order | UI technology | How to tell | Artifact |
|---|---|---|---|
| 1 | Compose | Apply `Modifier.markAsDialogCurrentPage()` to the page frame | `jp.kamusoft:ksdialogs` |
| 2 | Android View | Register a function returning the page View in `DialogCurrentPage.provider` | `jp.kamusoft:ksdialogs-core` |

When neither yields a page, the presentation does not fail: it uses the same result as `VISIBLE_AREA` and logs the reason as a warning with the tag `KsDialogs`. The reference is the rectangle you tell it (intersected with the visible area), so tell it the frame inside the bars, not the whole screen including them.

### Mark a Compose screen

On a screen built with `Scaffold`, apply it once to the content frame (inside `innerPadding`). The marked composable is a candidate only while it is on screen, and stops being one when it leaves the composition, for example through navigation.

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import jp.kamusoft.ksdialogs.compose.markAsDialogCurrentPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Orders") }) },
        bottomBar = { NavigationBar {} },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .markAsDialogCurrentPage(),
        ) {
            Text("Order list")
        }
    }
}
```

When there are several candidates, the inner one is taken if their rectangles are nested, and otherwise the one that came on screen last. With `Crossfade`, `AnimatedContent`, or a Navigation Compose fade transition, the leaving screen's mark stays a candidate during the switch, so a Dialog shown in that window can use the leaving screen as its reference. Screens switched by a plain conditional do not have this issue.

### Register a page function for a View screen

Assign a function returning the page View to `DialogCurrentPage.provider`. Assigning `null` removes the registration. The function is called on the UI thread when each presentation starts, and whenever the window size or system bar insets change while it is shown. A registration replaced during a presentation takes effect from the next one.

```kotlin
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import jp.kamusoft.ksdialogs.DialogCurrentPage

class OrdersActivity : ComponentActivity() {
    private lateinit var pageContainer: FrameLayout
    private val pageProvider: () -> View? = { pageContainer }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageContainer = FrameLayout(this)
        val bottomBar = LinearLayout(this)
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(pageContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
                addView(bottomBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 160))
            },
        )
    }

    override fun onResume() {
        super.onResume()
        DialogCurrentPage.provider = pageProvider
    }

    override fun onDestroy() {
        if (DialogCurrentPage.provider === pageProvider) {
            DialogCurrentPage.provider = null
        }
        super.onDestroy()
    }
}
```

The page counts as unavailable when the function returns `null`, throws, returns a View with an empty rectangle, or returns a View that is not in a window of the Activity presenting the Dialog (windows of modal dialogs shown by the same Activity included).

### Choose the reference area in the content

The reference area is attached to the content like any other `DialogOptions` value. The following is sheet-style content shown at full width along the bottom of the page.

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogEdgeInsets
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.compose.KsDialogAttributes

@Composable
fun PageSheetContent(viewModel: ConfirmViewModel, notifier: DialogNotifier<Boolean>) {
    KsDialogAttributes(
        options = DialogOptions(
            layoutArea = DialogLayoutArea.CURRENT_PAGE,
            dialogMargin = DialogEdgeInsets(16.0),
            proportionalWidth = 1.0,
        ),
        placement = DialogPlacement(verticalAlignment = DialogAlignment.END),
    )
    Column {
        Text(viewModel.message)
        Button(onClick = { notifier.complete(true) }) { Text("OK") }
    }
}
```

`Loading.instance.options` can also select `CURRENT_PAGE`, and the page is resolved by the same rules as for a Dialog, but how Loading and Toast look with this value may still change.
