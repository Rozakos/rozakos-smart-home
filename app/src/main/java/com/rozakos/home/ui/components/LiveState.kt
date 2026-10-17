package com.rozakos.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rozakos.home.data.protocol.DeviceState
import com.rozakos.home.data.protocol.FollowerAccount
import com.rozakos.home.data.protocol.Quote
import com.rozakos.home.ui.theme.BrandPalette
import java.util.Locale
import kotlin.math.abs

val Gain = BrandPalette.SuccessInk
val Loss = BrandPalette.Crimson

/** One line for the device card, e.g. "AAPL 231.40 ▲1.2% · NVDA 120.10 ▼0.4% · +2". */
fun DeviceState.summary(maxItems: Int = 2): String = when (this) {
    is DeviceState.Quotes -> {
        val shown = quotes.take(maxItems).joinToString(" · ") { q ->
            listOfNotNull(q.symbol, q.price?.let { formatPrice(it) }, q.changePercent?.let { formatChange(it) })
                .joinToString(" ")
        }
        if (quotes.size > maxItems) "$shown · +${quotes.size - maxItems}" else shown
    }
    is DeviceState.Followers -> {
        val shown = accounts.take(maxItems).joinToString(" · ") { a ->
            "${a.displayName} ${a.followers?.let { compactCount(it) } ?: "—"}"
        }
        if (accounts.size > maxItems) "$shown · +${accounts.size - maxItems}" else shown
    }
}

fun formatPrice(value: Double): String = String.format(Locale.US, if (value >= 1000) "%,.0f" else "%,.2f", value)

fun formatChange(pct: Double): String =
    (if (pct >= 0) "▲" else "▼") + String.format(Locale.US, "%.2f%%", abs(pct))

fun compactCount(n: Long): String = when {
    n >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", n / 1e9)
    n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1e6)
    n >= 10_000 -> String.format(Locale.US, "%.0fK", n / 1e3)
    n >= 1_000 -> String.format(Locale.US, "%.1fK", n / 1e3)
    else -> n.toString()
}.replace(".0M", "M").replace(".0B", "B").replace(".0K", "K")

@Composable
fun CardLivePreview(state: DeviceState) {
    val largeText = LocalDensity.current.fontScale > 1.3f
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state) {
            is DeviceState.Quotes -> {
                state.quotes.take(2).forEach { q ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(q.symbol, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            if (q.price == null || !q.fresh) Text(if (q.price == null) "Waiting for quote" else "Stale quote", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (largeText) Column(horizontalAlignment = Alignment.End) {
                            QuotePrice(q)
                            QuoteChange(q)
                        } else {
                            QuotePrice(q)
                            QuoteChange(q)
                        }
                    }
                }
                if (state.quotes.size > 2) Text("+${state.quotes.size - 2} more on your display",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is DeviceState.Followers -> {
                state.accounts.take(2).forEach { FollowerRow(it) }
                if (state.accounts.size > 2) Text("+${state.accounts.size - 2} more accounts",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LiveStateList(state: DeviceState) {
    Column {
        when (state) {
            is DeviceState.Quotes -> if (state.quotes.isEmpty()) Text("No quotes received yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
            else state.quotes.forEachIndexed { i, q ->
                if (i > 0) HorizontalDivider()
                QuoteRow(q)
            }
            is DeviceState.Followers -> if (state.accounts.isEmpty()) Text("No follower data available yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
            else state.accounts.forEachIndexed { i, a ->
                if (i > 0) HorizontalDivider()
                FollowerRow(a)
            }
        }
    }
}

@Composable
private fun QuoteRow(q: Quote) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(q.symbol, fontWeight = FontWeight.SemiBold)
            if (q.price == null || !q.fresh) Text(if (q.price == null) "Waiting for quote" else "Stale quote",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            QuotePrice(q)
            QuoteChange(q)
        }
    }
}

@Composable
private fun QuotePrice(q: Quote) {
    Text(q.price?.let { "$" + formatPrice(it) } ?: "—",
        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
        color = if (q.fresh) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun QuoteChange(q: Quote) {
    q.changePercent?.let { change ->
        val color = if (!q.fresh) MaterialTheme.colorScheme.onSurfaceVariant else if (change >= 0) Gain else Loss
        Text(formatChange(change), style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold, color = color,
            modifier = Modifier.background(color.copy(alpha = .07f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp))
    }
}

@Composable
private fun FollowerRow(a: FollowerAccount) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(a.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${a.platform.replaceFirstChar { it.uppercase() }} · ${a.handle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(a.followers?.let { compactCount(it) } ?: "—", fontWeight = FontWeight.Medium)
            a.delta?.let {
                Text(
                    (if (it >= 0) "▲ " else "▼ ") + compactCount(abs(it)),
                    color = if (it >= 0) Gain else Loss,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
