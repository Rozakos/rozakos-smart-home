package com.rozakos.home.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rozakos.home.data.market.MarketSymbol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymbolSearchSheet(
    search: SymbolSearchState,
    selected: List<String>,
    maxSymbols: Int,
    saving: Boolean,
    onQuery: (String) -> Unit,
    onSelect: (MarketSymbol) -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.88f).imePadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add to watchlist", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Find a company, fund or crypto by name.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = search.query, onValueChange = onQuery,
                label = { Text("Company name or symbol") },
                placeholder = { Text("Nvidia, Apple, Bitcoin…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.query.isNotEmpty()) IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            if (selected.size >= maxSymbols) Text("Watchlist full. Remove a symbol before adding another.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    search.loading -> item {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Searching…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    search.error != null -> item {
                        Text(search.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 16.dp))
                    }
                    search.query.isBlank() -> item {
                        Text("Try a company name like Nvidia, or a symbol like NVDA.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                    }
                    search.results.isEmpty() -> item {
                        Text("No matches for “${search.query}”. Try another name or enter a symbol manually.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                    }
                    else -> items(search.results, key = { it.symbol }) { result ->
                        val added = result.symbol in selected
                        Surface(
                            onClick = { keyboard?.hide(); onSelect(result) },
                            enabled = !added && selected.size < maxSymbols && !saving,
                            shape = RoundedCornerShape(16.dp),
                            color = if (added) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(result.name, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${result.symbol} · ${result.exchange} · ${result.kind}",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (added) Text("Already added", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                else Icon(Icons.Outlined.Add, contentDescription = "Add ${result.symbol}",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Text("Choose a result, then tap Save to device to apply your watchlist.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { keyboard?.hide(); onManual() }, modifier = Modifier.padding(bottom = 12.dp)) {
                Text("Enter a symbol manually")
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
