# Restyle Guide — Existing Jetpack Compose App, Duolingo-Referenced Design

This file is a **design brief for restyling an app that already exists**. It borrows Duolingo's *visual structure and interaction feel* (chunky 3D "slab" controls, flat canvas, thick borders, curved learning path, gamified HUD, playful feedback). It does **not** borrow Duolingo's brand colors or typefaces: the app keeps its own palette and fonts.

---

## 0. Hard Constraints (read first, never violate)

### 0.1 Do not change the build or its dependencies

- **Do not edit**: `build.gradle` / `build.gradle.kts` (project and module), `settings.gradle*`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `proguard-*.pro`, version catalogs, BOMs.
- **Do not** add, remove, or upgrade any library (no Coil, `material-icons-extended`, Accompanist, font libraries, etc.) (Except Lottie if u need it).
- **Do not** change `compileSdk`, `minSdk`, `targetSdk`, Kotlin, Compose compiler, or Compose BOM versions.
- **Do not** add `AndroidManifest.xml` permissions. (Haptics here use Compose's `LocalHapticFeedback`, which needs none.)
- If a snippet in this guide does not compile with the app's current versions, **adapt the snippet** (see §9). Never bump a version to make it compile.
- Final check: `git diff --stat` must show **no changes** to any file listed above.

### 0.2 Change presentation only

- **Preserve**: ViewModels, repositories, use cases, DI setup, navigation graph and routes, state/data models, string resource keys, analytics calls, and tests.
- **Preserve public composable signatures** wherever possible. Restyle the *internals* of existing components. If a new component is needed, add it beside the old one and swap call sites.
- **Brand stays**: use the app's existing `MaterialTheme.colorScheme` and typography. No Duolingo hex values, no Duolingo fonts, no Duolingo illustrations or sounds.
- New code goes in **new files** under `ui/designsystem/`. Edit existing theme files only if a step below says so (and then minimally).

---

## 1. Borrowed vs. Not Borrowed

| Borrowed (apply) | Not borrowed (keep the app's own) |
|---|---|
| 3D slab button with a collapsing ledge | Brand colors |
| Cards and tiles: 2dp border plus a 4dp bottom ledge, no shadow | Typefaces |
| Flat canvas, generous rounding, playful feedback | Mascot, logo, badges, wordmark |
| Curved wave-shaped learning path | Sound effects, copywriting |
| Streak / gems / hearts HUD pattern | Existing navigation structure and routes |
| Lesson layout: close + progress bar on top, feedback sheet at the bottom | Business logic |
| Correct/wrong: shake, pop, haptic, confetti | |
| Bottom bar with a soft tinted pill on the active tab | |

---

## 2. Working Method (follow in order)

1. **Audit the existing theme.** Read the project's `Color.kt`, `Type.kt`, `Theme.kt`. Note which `colorScheme` roles carry the brand, and which font families are used. Do not edit them.
2. **Add the design-system files** from §4 and §5 under `ui/designsystem/` (`DsTokens.kt`, `DsComponents.kt`). Rename the package to match the project.
3. **Inventory the screens.** List every screen and reusable component, and map each to a pattern in §3 and §5.
4. **Restyle in this order**: buttons → cards/tiles → inputs → HUD / top bar → bottom bar → lists/path → feedback/celebration → dialogs/sheets.
5. **Swap call sites** to `Ds*` components, or restyle the old component's body, keeping parameters unchanged.
6. **Verify**:
   - the project builds with its *current* Gradle configuration,
   - existing unit/UI tests still pass,
   - `git diff --stat -- '*.gradle*' '*.toml' 'gradle/**' '*.pro' '**/AndroidManifest.xml'` is empty,
   - light and dark mode both look right.

---

## 3. Visual Rules (apply to every screen, including elements without a Ds component)

| Aspect | Rule |
|---|---|
| Canvas | `colorScheme.background` / `surface`. Flat. **No elevation or drop shadows anywhere.** Depth comes from the 4dp ledge. |
| Ledge | 4dp, a darker tone of the element's own face: `face.ledge()` (§4). Press moves the face down 4dp. |
| Border | 2dp. Neutral elements use `Ds.colors.neutralBorder`. |
| Radius | Button 16dp, Card 16dp, Tile/Input 12dp, Pill 50%, Node circle, Dialog / Bottom sheet top corners 24dp. |
| Spacing | 4dp grid. Screen horizontal padding 16dp. 24dp between sections. 8–12dp inside groups. |
| Buttons | Label uppercase, heaviest UI weight, 0.4sp letter-spacing, 56dp face height. |
| Text color | `onSurface` for primary, `onSurfaceVariant` for secondary. Never hard-coded black. |
| Cards (generic) | `DsCard`: surface fill, 2dp neutral border, 4dp neutral ledge, 16dp radius, 16dp padding. |
| Text fields | 12dp radius, 2dp border (`neutralBorder`), focused border `Ds.colors.selectedBorder`, error border `colorScheme.error`. No filled/underline style. |
| Top bar | Flat, no elevation. In lessons: close (X) on the left, progress bar in the middle, hearts on the right. In the main tabs: HUD chips on the trailing edge. |
| Progress | 16dp-tall pill, track `neutralFill`, fill `primary` with a thin lighter highlight strip (`DsProgressBar`). |
| Lists | Prefer `DsCard` rows with 12dp gaps over dividers. |
| Dialogs / sheets | 24dp top radius, no elevation shadow, primary action as `DsButton`. |
| Feedback sheet (lesson) | Slides up from the bottom, filled with `successContainer` (correct) or `errorContainer` (wrong), with a glyph, a title in `success` / `error`, and a `DsButton` (Primary or Destructive). |
| Gamification colors | Streak → `streak`, gems/XP → `reward`, links/info → `info`, premium → `premium`, hearts → `error`. |
| Empty / locked | `neutralFill` with `onSurfaceVariant` content. |

---

## 4. Design Tokens (new file: `DsTokens.kt`)

Everything here is **derived from the app's existing `MaterialTheme`**, so no new palette and no new font is introduced. Only roles that do not exist in Material (success, streak, reward, info, premium) need decisions, and those fall back to the app's own scheme colors until you override them.

Only color roles available since Material 3 1.0 are used (`primary`, `onPrimary`, `primaryContainer`, `secondary`, `tertiary`, `surface`, `onSurface`, `onSurfaceVariant`, `error`, `errorContainer`, `onError`, `inverseSurface`, `inverseOnSurface`, `background`).

```kotlin
package your.app.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Darker tone for the 3D ledge. Works with any brand color. */
fun Color.ledge(fraction: Float = 0.2f): Color = lerp(this, Color.Black, fraction)

// ───────────────────────── Colors ─────────────────────────

@Immutable
class DsColors(
    val success: Color,            // correct answer border/text
    val successContainer: Color,   // correct tile / feedback sheet fill
    val selectedContainer: Color,  // selected tile fill
    val selectedBorder: Color,     // selected tile / focused input border
    val progressFill: Color,       // lighter tone for progress highlight, pulse ring
    val streak: Color,
    val reward: Color,             // gems, XP, gold node
    val info: Color,
    val premium: Color,
    val neutralFill: Color,        // locked node, progress track, disabled face
    val neutralBorder: Color,      // tile / card border
    val neutralLedge: Color,       // ledge under neutral elements
)

/**
 * Optional brand overrides for the five roles Material does not have.
 * Leave null to derive from the app's existing scheme.
 * Use values that ALREADY exist in the app's palette; do not import Duolingo's colors.
 */
class DsOverride(
    val success: Color? = null,
    val streak: Color? = null,
    val reward: Color? = null,
    val info: Color? = null,
    val premium: Color? = null,
)

val DsLightOverride = DsOverride() // fill from the app's light palette if it has dedicated hues
val DsDarkOverride = DsOverride()  // fill from the app's dark palette if it has dedicated hues

@Composable
fun dsColors(): DsColors {
    val cs = MaterialTheme.colorScheme
    return remember(cs) {
        val isLight = cs.background.luminance() > 0.5f
        val o = if (isLight) DsLightOverride else DsDarkOverride

        val success = o.success ?: cs.primary
        val info = o.info ?: cs.secondary
        val tintLow = if (isLight) 0.16f else 0.28f

        DsColors(
            success = success,
            successContainer = lerp(cs.surface, success, tintLow),
            selectedContainer = lerp(cs.surface, info, tintLow),
            selectedBorder = lerp(cs.surface, info, 0.65f),
            progressFill = lerp(cs.primary, Color.White, 0.25f),
            streak = o.streak ?: cs.tertiary,
            reward = o.reward ?: cs.secondary,
            info = info,
            premium = o.premium ?: cs.tertiary,
            neutralFill = lerp(cs.surface, cs.onSurface, 0.08f),
            neutralBorder = lerp(cs.surface, cs.onSurface, 0.15f),
            neutralLedge = lerp(cs.surface, cs.onSurface, 0.25f),
        )
    }
}

// ───────────────────────── Typography ─────────────────────────
// Hierarchy only (size / weight / spacing). Font families come from the app's own Typography.

@Immutable
class DsTextRoles(
    val display: TextStyle,
    val heroNumber: TextStyle,
    val screenTitle: TextStyle,
    val question: TextStyle,
    val hudNumber: TextStyle,
    val sectionHeader: TextStyle,
    val skillLabel: TextStyle,
    val button: TextStyle,
    val tile: TextStyle,
    val body: TextStyle,
    val helper: TextStyle,
    val meta: TextStyle,
    val tab: TextStyle,
    val labelUpper: TextStyle,
)

fun dsTextRoles(displayFamily: FontFamily?, uiFamily: FontFamily?) = DsTextRoles(
    display       = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Bold,      fontSize = 34.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    heroNumber    = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Bold,      fontSize = 56.sp, lineHeight = 56.sp, letterSpacing = (-1.0).sp),
    screenTitle   = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Bold,      fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
    question      = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Bold,      fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    hudNumber     = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Bold,      fontSize = 18.sp, lineHeight = 18.sp, letterSpacing = (-0.1).sp),
    sectionHeader = TextStyle(fontFamily = uiFamily,      fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    skillLabel    = TextStyle(fontFamily = uiFamily,      fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 19.sp),
    button        = TextStyle(fontFamily = uiFamily,      fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 17.sp, letterSpacing = 0.4.sp),
    tile          = TextStyle(fontFamily = uiFamily,      fontWeight = FontWeight.Bold,      fontSize = 17.sp, lineHeight = 20.sp),
    body          = TextStyle(fontFamily = uiFamily,      fontWeight = FontWeight.Normal,    fontSize = 16.sp, lineHeight = 22.sp),
    helper        = TextStyle(fontFamily = uiFamily,      fontWeight = FontWeight.Medium,    fontSize = 14.sp, lineHeight = 18.sp),
    meta          = TextStyle(fontFamily = uiFamily,      fontWeight = FontWeight.Medium,    fontSize = 13.sp, lineHeight = 16.sp),
    tab           = TextStyle(fontFamily = uiFamily,      fontWeight = FontWeight.Bold,      fontSize = 11.sp, lineHeight = 11.sp, letterSpacing = 0.2.sp),
    labelUpper    = TextStyle(fontFamily = uiFamily,      fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 1.0.sp),
)

@Composable
fun dsText(): DsTextRoles {
    val t = MaterialTheme.typography
    return remember(t) { dsTextRoles(t.headlineMedium.fontFamily, t.bodyLarge.fontFamily) }
}

// ───────────────────────── Accessor ─────────────────────────
// Needs no provider and no edits to the existing theme.
object Ds {
    val colors: DsColors @Composable get() = dsColors()
    val text: DsTextRoles @Composable get() = dsText()
}
```

If the app has a distinct display font, pass it directly: `dsTextRoles(MyDisplayFont, MyUiFont)` in `dsText()`. Weights are a hierarchy. If a family lacks `ExtraBold`, use the heaviest weight it has.

---

## 5. Components (new file: `DsComponents.kt`)

All components use **only** `Ds.colors`, `Ds.text`, and `MaterialTheme.colorScheme`. They take `Painter` for icons, so the project's existing icons or drawables work and no icon library is needed (see §7). Split the file as you like.

```kotlin
package your.app.ui.designsystem

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random
```

### 5.1 `DsButton`, the 3D slab

Press moves the face down 4dp onto the ledge. Use for **every** primary/secondary/destructive action.

```kotlin
enum class DsButtonVariant { Primary, Neutral, Destructive }

@Composable
fun DsButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DsButtonVariant = DsButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val c = Ds.colors
    val (top, ledge, content) = when {
        !enabled -> Triple(c.neutralFill, c.neutralLedge, cs.onSurfaceVariant)
        variant == DsButtonVariant.Primary -> Triple(cs.primary, cs.primary.ledge(), cs.onPrimary)
        variant == DsButtonVariant.Neutral -> Triple(cs.surface, c.neutralLedge, cs.onSurface)
        else -> Triple(cs.error, cs.error.ledge(), cs.onError)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val faceOffset by animateDpAsState(
        targetValue = if (pressed && enabled) 4.dp else 0.dp,
        animationSpec = if (pressed) tween(80) else spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "slab",
    )
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(16.dp)

    Box(modifier.height(60.dp)) { // 56dp face + 4dp ledge reveal
        Box(Modifier.fillMaxWidth().height(56.dp).align(Alignment.BottomCenter).clip(shape).background(ledge))
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset(y = faceOffset)
                .clip(shape)
                .background(top)
                .then(if (variant == DsButtonVariant.Neutral && enabled) Modifier.border(2.dp, c.neutralBorder, shape) else Modifier)
                .clickable(enabled = enabled, interactionSource = interaction, indication = null) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(title.uppercase(), style = Ds.text.button, color = content)
        }
    }
}
```

### 5.2 `DsCard`, the generic container

Use it to restyle any existing card, list row, or panel.

```kotlin
@Composable
fun DsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ds.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .padding(bottom = 4.dp) // reserve the ledge
            .drawBehind {
                drawRoundRect(
                    color = c.neutralLedge,
                    topLeft = Offset(0f, 4.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                )
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, c.neutralBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
}
```

### 5.3 `DsProgressBar`

```kotlin
@Composable
fun DsProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val c = Ds.colors
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(400), label = "progress")
    val pill = RoundedCornerShape(50)
    Box(modifier.fillMaxWidth().height(16.dp).clip(pill).background(c.neutralFill)) {
        Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(pill).background(MaterialTheme.colorScheme.primary)) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp, start = 8.dp, end = 8.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(pill)
                    .background(c.progressFill.copy(alpha = 0.6f)),
            )
        }
    }
}
```

### 5.4 `DsAnswerTile`

```kotlin
enum class DsTileState { Idle, Selected, Correct, Wrong }

@Composable
fun DsAnswerTile(text: String, state: DsTileState, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val c = Ds.colors

    val fill = when (state) {
        DsTileState.Idle -> cs.surface
        DsTileState.Selected -> c.selectedContainer
        DsTileState.Correct -> c.successContainer
        DsTileState.Wrong -> cs.errorContainer
    }
    val border = when (state) {
        DsTileState.Idle -> c.neutralBorder
        DsTileState.Selected -> c.selectedBorder
        DsTileState.Correct -> c.success
        DsTileState.Wrong -> cs.error
    }
    val ledge = if (state == DsTileState.Idle) c.neutralLedge else border
    val textColor = when (state) {
        DsTileState.Idle, DsTileState.Selected -> cs.onSurface
        DsTileState.Correct -> c.success
        DsTileState.Wrong -> cs.error
    }

    val shake = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state) {
        when (state) {
            DsTileState.Wrong -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                repeat(4) { shake.animateTo(if (it % 2 == 0) 6f else -6f, tween(50)) }
                shake.animateTo(0f, tween(50))
            }
            DsTileState.Correct -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            else -> Unit
        }
    }

    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .absoluteOffset(x = shake.value.dp)
            .height(52.dp)
            .semantics { contentDescription = "Answer: $text, ${state.name.lowercase()}" },
    ) {
        Box(Modifier.fillMaxWidth().height(48.dp).align(Alignment.BottomCenter).clip(shape).background(ledge))
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(fill)
                .border(2.dp, border, shape)
                .clickable(onClick = onTap)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = Ds.text.tile, color = textColor)
        }
    }
}
```

> Never signal correct/wrong by color alone. Add a check or X glyph in the feedback sheet, plus the shake / pop animation.

### 5.5 `DsSkillNode` and `DsLearningPath`

```kotlin
enum class DsSkillState { Locked, Available, Current, Completed, Gold, Legendary }

@Composable
fun DsSkillNode(
    icon: Painter,
    state: DsSkillState,
    crownCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    crownIcon: Painter? = null, // optional: project's own badge icon
) {
    val cs = MaterialTheme.colorScheme
    val c = Ds.colors
    val size = if (state == DsSkillState.Current) 96.dp else 80.dp

    val top = when (state) {
        DsSkillState.Locked -> c.neutralFill
        DsSkillState.Gold -> c.reward
        DsSkillState.Legendary -> c.premium
        else -> cs.primary
    }
    val ledge = if (state == DsSkillState.Locked) c.neutralLedge else top.ledge()
    val glyph = when (state) {
        DsSkillState.Locked -> cs.onSurfaceVariant
        DsSkillState.Gold, DsSkillState.Legendary -> cs.surface // verify contrast on your hues
        else -> cs.onPrimary
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.7f), label = "node")
    val haptics = LocalHapticFeedback.current
    val lockPainter = rememberVectorPainter(Icons.Filled.Lock)

    val ring = if (state == DsSkillState.Current) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            1f, 1.12f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ring",
        ).value
    } else 1f

    Box(modifier.size(size + 4.dp), contentAlignment = Alignment.Center) {
        if (state == DsSkillState.Current) {
            Box(Modifier.size(size).scale(ring).clip(CircleShape).border(4.dp, c.progressFill, CircleShape))
        }
        Box(Modifier.size(size).offset(y = 4.dp).clip(CircleShape).background(ledge))
        Box(
            Modifier
                .size(size)
                .scale(pressScale)
                .clip(CircleShape)
                .background(top)
                .clickable(interactionSource = interaction, indication = null) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = if (state == DsSkillState.Locked) lockPainter else icon,
                contentDescription = null,
                tint = glyph,
                modifier = Modifier.size(size * 0.4f),
            )
        }
        if (crownCount > 0 && crownIcon != null && state != DsSkillState.Locked) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .clip(RoundedCornerShape(50))
                    .background(cs.inverseSurface)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(crownIcon, null, tint = c.reward, modifier = Modifier.size(12.dp))
                Text("$crownCount", style = Ds.text.meta, color = cs.inverseOnSurface)
            }
        }
    }
}

data class DsPathNode(
    val id: String,
    val icon: Painter,
    val title: String,
    val state: DsSkillState,
    val crowns: Int = 0,
)

/** Curved vertical path: nodes follow a ±60dp wave, never a straight list. */
@Composable
fun DsLearningPath(
    nodes: List<DsPathNode>,
    onNodeTap: (DsPathNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = 32.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(nodes, key = { _, n -> n.id }) { index, node ->
            val waveOffset = (sin(index * Math.PI / 3) * 60).dp
            Column(
                Modifier.offset(x = waveOffset),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DsSkillNode(node.icon, node.state, node.crowns, { onNodeTap(node) })
                Text(node.title, style = Ds.text.skillLabel, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
```

### 5.6 `DsHudChip` (streak / gems / hearts)

Pass the tint explicitly: `Ds.colors.streak`, `Ds.colors.info`, `MaterialTheme.colorScheme.error`.

```kotlin
@Composable
fun DsHudChip(
    icon: Painter,
    tint: Color,
    value: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier.defaultMinSize(minHeight = 48.dp).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
        Text("$value", style = Ds.text.hudNumber, color = valueColor)
    }
}

/** Streak with an idle pulse. Disable the pulse when animations are turned off (§8). */
@Composable
fun DsStreak(count: Int, icon: Painter, modifier: Modifier = Modifier, animate: Boolean = true) {
    val tint = Ds.colors.streak
    val scale = if (animate) {
        rememberInfiniteTransition(label = "flame").animateFloat(
            1f, 1.05f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "idle",
        ).value
    } else 1f
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, "Streak", tint = tint, modifier = Modifier.size(20.dp).scale(scale))
        Text("$count", style = Ds.text.hudNumber, color = tint)
    }
}
```

### 5.7 `DsBottomBar`

Restyles the bottom bar only. **Navigation logic and routes stay in the app**; pass the existing selected index and callback.

```kotlin
data class DsTab(val label: String, val icon: Painter)

@Composable
fun DsBottomBar(tabs: List<DsTab>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column {
        HorizontalDivider(color = Ds.colors.neutralBorder, thickness = 2.dp)
        NavigationBar(containerColor = cs.surface, tonalElevation = 0.dp) {
            tabs.forEachIndexed { i, tab ->
                NavigationBarItem(
                    selected = selectedIndex == i,
                    onClick = { onSelect(i) },
                    icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(28.dp)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = cs.primary,
                        unselectedIconColor = cs.onSurfaceVariant,
                        indicatorColor = cs.primaryContainer,
                    ),
                )
            }
        }
    }
}
```

> `HorizontalDivider` exists in newer Material 3. On older versions use `Divider(color = …, thickness = 2.dp)`. Do not upgrade to get it (§9).

### 5.8 `DsCelebration` and `DsConfetti`

```kotlin
@Composable
fun DsCelebration(
    title: String,
    xpEarned: Int,
    continueLabel: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    mascot: @Composable () -> Unit = {}, // the app's own illustration, ~280dp
    animate: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val c = Ds.colors
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }

    Box(modifier.fillMaxSize().background(cs.primary)) {
        Column(
            Modifier.fillMaxSize().padding(top = 80.dp, bottom = 16.dp).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(Modifier.size(280.dp)) { mascot() }
            Text(title.uppercase(), style = Ds.text.screenTitle, color = cs.onPrimary)
            Text(
                "+$xpEarned XP",
                style = Ds.text.question,
                color = cs.onPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(c.reward.copy(alpha = 0.25f))
                    .border(2.dp, c.reward, RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            DsButton(continueLabel, onContinue, variant = DsButtonVariant.Neutral)
        }
        if (animate) DsConfetti()
    }
}

@Composable
fun DsConfetti(modifier: Modifier = Modifier) {
    data class Piece(val x: Float, val color: Color, val delay: Int, val spin: Float)

    val cs = MaterialTheme.colorScheme
    val c = Ds.colors
    val palette = listOf(cs.primary, c.streak, c.reward, c.info, cs.error, c.premium)
    val pieces = remember(palette) {
        List(12) { Piece(Random.nextFloat(), palette.random(), Random.nextInt(0, 300), Random.nextFloat() * 720f - 360f) }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1200)) }

    Canvas(modifier.fillMaxSize()) {
        pieces.forEach { p ->
            val t = ((progress.value * 1200f - p.delay) / 1200f).coerceIn(0f, 1f)
            val y = t * (size.height + 40f) - 20f
            rotate(p.spin * t, pivot = Offset(p.x * size.width, y)) {
                drawRoundRect(
                    color = p.color,
                    topLeft = Offset(p.x * size.width - 4f, y),
                    size = Size(8.dp.toPx(), 14.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
        }
    }
}
```

> On confetti: the palette uses theme colors, so it always matches the brand. On the celebration screen, `cs.primary` confetti is invisible on the `cs.primary` background; that is fine, the other five roles carry it.

---

## 6. Motion & Haptics

| Moment | Recipe |
|---|---|
| Button press | Face `animateDpAsState` 0 → 4dp, 80ms `tween`; release `spring(dampingRatio = 0.7f)`; soft haptic |
| Correct answer | Tile → success colors + check glyph `scale` 0 → 1.2 → 1.0 spring (300ms); success haptic |
| Wrong answer | Tile → error colors + horizontal shake ±6dp ≈ 400ms; warning haptic |
| Node tap | `scale` 0.96 → 1.0 spring (≈180ms); light haptic |
| Lesson complete | Crossfade to primary canvas 300ms, mascot slides up 500ms spring, confetti 1.2s |
| Streak | Idle `scale` 1.0 ↔ 1.05 over 2s; on save, burst 1.3 → 1.0 + confetti |
| Progress | Fill animates 400ms; XP rings sweep over 800ms ease-out |
| Feedback sheet | Slides up from the bottom, 250ms |

```kotlin
// Correct / Celebration / Node unlock → HapticFeedbackType.LongPress
// Wrong answer                        → HapticFeedbackType.LongPress (+ shake)
// Button / node press                 → HapticFeedbackType.TextHandleMove
```

Add sound only through the app's **existing** audio mechanism and assets. Do not add an audio library or Duolingo sounds.

---

## 7. Icons (no new dependency)

`material-icons-extended` is a separate dependency. **Do not add it.**

- **Prefer** icons the app already uses (`painterResource(R.drawable.…)` or whatever it currently has).
- **Core icons** (always available with `material-icons-core`): `Home`, `Person`, `Favorite`, `Star`, `Check`, `Close`, `Lock`, `ShoppingCart`, `ArrowForward`, `Settings`, `Info`, `Search`, `Notifications`. Wrap with `rememberVectorPainter(Icons.Filled.X)`.
- **Missing from core** (flame, gem, trophy, crown, scroll/quests): add a small **XML vector drawable** in `res/drawable/` (an asset, not a dependency), or reuse the app's existing icon. Do not use Duolingo's artwork.

| Role | Source |
|---|---|
| Streak | project icon or `res/drawable/ic_streak.xml` |
| Gems / XP | project icon or `res/drawable/ic_gem.xml` |
| Hearts | `Icons.Filled.Favorite` |
| Locked | `Icons.Filled.Lock` (used inside `DsSkillNode`) |
| Completed | `Icons.Filled.Check` / `Icons.Filled.Star` |
| Close lesson | `Icons.Filled.Close` |
| Continue | `Icons.Filled.ArrowForward` |
| Tabs | the app's existing tab icons |
| Mascot, badges | the app's own assets |

---

## 8. Accessibility

- **Touch targets** ≥ 48dp: the button (56dp), node (80dp), and tile (48dp) pass. Icon-only buttons need a 48dp hit area. `DsHudChip` uses `minHeight = 48.dp`.
- **Font scaling**: `sp` follows the user's font scale. Pin layout-sensitive text (uppercase button labels, HUD numbers, tab labels) so it cannot overflow its fixed-height container, and let node labels scale up to XL only.
- **TalkBack**: `DsAnswerTile` already announces text and state. Announce the result on correct/wrong ("Correct! +10 XP"). Skill nodes should announce title and state ("Basics 1, completed, 3 crowns").
- **Contrast** (the palette is the app's, so verify it): `onPrimary` on `primary` ≥ 4.5:1 (≥ 3:1 for bold 14sp+). `success`, `streak`, `reward`, `info` used as **text** need ≥ 4.5:1 against `surface`, otherwise pair them with an icon or darken them via the override. `onSurfaceVariant` at 13sp must reach 4.5:1. Glyphs on `reward` / `premium` nodes need ≥ 3:1.
- **Never color-only**: correct/wrong always has a glyph and an animation.
- **Reduce motion**: when `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`, call `DsCelebration(animate = false)`, `DsStreak(animate = false)`, and skip the node pulse. Keep the button ledge collapse; it conveys state.
- **Dynamic color (Material You)**: do not change whatever the app already does. If dynamic color is on, `Ds.colors` still derives correctly, but check that `success`, `streak`, `reward`, `info`, `premium` stay distinguishable.

---

## 9. Compatibility Notes (adapt the code, never the versions)

| If the project… | Then |
|---|---|
| Uses **Material 2** (`androidx.compose.material`) | Replace `MaterialTheme.colorScheme.*` with `MaterialTheme.colors.*` (`primary`, `onPrimary`, `surface`, `onSurface`, `background`, `error`, `onError`). Map `primaryContainer` → `primary.copy(alpha = 0.15f)`, `errorContainer` → `error.copy(alpha = 0.15f)`, `inverseSurface` → `onSurface`, `inverseOnSurface` → `surface`, `onSurfaceVariant` → `onSurface.copy(alpha = 0.6f)`, `secondary` / `tertiary` → `secondary`. Use `BottomNavigation` instead of `NavigationBar`. |
| Has an older Compose where `animate*AsState(label = …)` or `rememberInfiniteTransition(label = …)` is missing | Remove the `label` argument. |
| Lacks `HorizontalDivider` | Use `Divider`, or draw a 2dp `Box` with `neutralBorder`. |
| Lacks `ExtraBold` in its font family | Use the heaviest weight available. |
| Has `Modifier.clickable` without `interactionSource` / `indication` named args | Use the positional overload `clickable(interaction, null) { … }`. |
| Is not on Material 3 for the bottom bar | Wrap the project's existing bottom bar with the same colors and 2dp top divider. |

If something still does not compile, simplify the visual (for example, drop the highlight strip) rather than touching dependencies.

---

## 10. Definition of Done

- [ ] No changes to Gradle files, version catalogs, wrapper, ProGuard, or the manifest (`git diff --stat` is empty for them).
- [ ] No new libraries, fonts, or permissions.
- [ ] ViewModels, navigation routes, data models, string keys, and tests are untouched, and tests pass.
- [ ] All new code lives under `ui/designsystem/` plus restyled UI files.
- [ ] No hex color literals and no `FontFamily` inside screens or components. Everything goes through `MaterialTheme` and `Ds`.
- [ ] No elevation or drop shadows remain; depth is the 4dp ledge.
- [ ] Every button is a `DsButton` (or restyled to the same spec); every card/tile follows §3.
- [ ] Light and dark mode both look right; contrast pairs in §8 verified.
- [ ] Correct/wrong feedback uses glyph + animation + haptic, never color alone.
