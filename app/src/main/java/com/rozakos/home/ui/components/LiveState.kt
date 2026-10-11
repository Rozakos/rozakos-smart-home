package com.rozakos.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rozakos.home.data.protocol.DeviceState
import com.rozakos.home.data.protocol.FollowerAccount
import com.rozakos.home.data.protocol.Quote
import java.util.Locale
import kotlin.math.abs

val Gain = Color(0xFF15803D)
val Loss = Color(0xFFB91C1C)

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
fun LiveStateList(state: DeviceState) {
    Column {
        when (state) {
            is DeviceState.Quotes -> state.quotes.forEachIndexed { i, q ->
                if (i > 0) HorizontalDivider()
                QuoteRow(q)
            }
            is DeviceState.Followers -> state.accounts.forEachIndexed { i, a ->
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
        Text(q.symbol, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(
            q.price?.let { "$" + formatPrice(it) } ?: "—",
            fontWeight = FontWeight.Medium,
            color = if (q.fresh) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            q.changePercent?.let { formatChange(it) } ?: "",
            color = when {
                q.changePercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                q.changePercent >= 0 -> Gain
                else -> Loss
            },
            style = MaterialTheme.typography.bodyMedium,
        )
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
