package org.linguafree.feature.shop

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.linguafree.core.domain.game.HeartsRules
import org.linguafree.core.domain.game.ShopItem
import org.linguafree.core.domain.game.StreakRules
import org.linguafree.core.domain.model.UserSettings
import org.linguafree.core.domain.model.Wallet
import org.linguafree.core.domain.repository.GameRepository
import org.linguafree.core.domain.repository.SettingsRepository
import org.linguafree.core.domain.usecase.NewWallet
import org.linguafree.core.domain.usecase.ShopUseCase
import org.linguafree.core.domain.usecase.TimeProvider
import org.linguafree.core.ui.R
import org.linguafree.core.ui.components.LfButton
import org.linguafree.core.ui.components.LfButtonStyle
import org.linguafree.core.ui.components.LfIcon
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.mascot.FoxWithOutfit
import org.linguafree.core.ui.mascot.Outfit
import org.linguafree.core.ui.theme.LfTheme
import org.linguafree.core.ui.theme.ThemeAccent
import javax.inject.Inject

data class ShopUiState(
    val wallet: Wallet = NewWallet,
    val owned: Set<String> = emptySet(),
    val settings: UserSettings = UserSettings(),
    val now: Long = 0,
)

@HiltViewModel
class ShopViewModel @Inject constructor(
    game: GameRepository,
    private val settings: SettingsRepository,
    private val shop: ShopUseCase,
    private val time: TimeProvider,
) : ViewModel() {
    val message = MutableStateFlow<String?>(null)

    val state: StateFlow<ShopUiState> = combine(game.wallet(), game.inventory(), settings.settings) { w, inv, s ->
        ShopUiState(HeartsRules.regenerate(w, time.now()), inv, s, time.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopUiState())

    fun buy(item: ShopItem) {
        viewModelScope.launch {
            message.value = when (val r = shop.buy(item)) {
                ShopUseCase.Result.Ok -> "Готово! «${item.title}» твоё."
                is ShopUseCase.Result.NotEnoughGems -> "Не хватает ${r.missing} 💎. Кристаллы дают за уроки, задания, сундуки и лиги."
                is ShopUseCase.Result.NotAvailable -> r.reason
            }
        }
    }

    fun wear(outfit: Outfit?) = viewModelScope.launch { settings.update { it.copy(outfit = outfit?.key.orEmpty()) } }
    fun applyTheme(accent: ThemeAccent) = viewModelScope.launch { settings.update { it.copy(accent = accent.name) } }
    fun dismiss() { message.value = null }
}

/** Gem shop. Only learning-earned gems are accepted; no real-money prices anywhere. */
@Composable
fun ShopScreen(viewModel: ShopViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val c = LfTheme.colors
    val w = state.wallet

    LazyColumn(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().testTag("shop"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FoxMascot(FoxEmotion.Correct, Modifier.size(84.dp))
                Column(Modifier.weight(1f)) {
                    Text("Магазин", style = MaterialTheme.typography.headlineMedium, color = c.text)
                    Text("Только за кристаллы, заработанные учёбой", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LfIcon(R.drawable.ic_gem, size = 28.dp)
                    Text(" ${w.gems}", style = MaterialTheme.typography.titleLarge, color = c.gems, modifier = Modifier.testTag("shop-gems"))
                }
            }
        }
        item { SectionTitle("Полезное") }
        item {
            ShopRow(
                ShopItem.StreakFreeze, R.drawable.ic_snowflake, state,
                status = "У тебя ${w.freezes} из ${StreakRules.MAX_FREEZES}",
                available = w.freezes < StreakRules.MAX_FREEZES, onBuy = viewModel::buy,
            )
        }
        item {
            ShopRow(
                ShopItem.HeartsRefill, R.drawable.ic_heart, state,
                status = if (state.settings.heartsDisabled) "Включён режим без сердец" else "Сердец: ${w.hearts} из ${HeartsRules.MAX}",
                available = w.hearts < HeartsRules.MAX && !state.settings.heartsDisabled, onBuy = viewModel::buy,
            )
        }
        item {
            val active = w.doubleXpUntil > state.now
            ShopRow(
                ShopItem.DoubleXp, R.drawable.ic_double_xp, state,
                status = if (active) "Активен ещё ${((w.doubleXpUntil - state.now) / 60000) + 1} мин" else null,
                available = true, onBuy = viewModel::buy,
            )
        }
        item { SectionTitle("Наряды для лисёнка") }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FoxWithOutfit(Outfit.of(state.settings.outfit), Modifier.size(180.dp))
            }
        }
        items(Outfit.entries, key = { it.key }) { outfit ->
            val item = ShopItem.valueOf(outfit.key)
            val owned = outfit.key in state.owned
            val worn = state.settings.outfit == outfit.key
            CosmeticRow(
                title = item.title, description = item.description, price = item.price, owned = owned, active = worn,
                canAfford = w.gems >= item.price,
                onBuy = { viewModel.buy(item) },
                onUse = { viewModel.wear(if (worn) null else outfit) },
                useLabel = if (worn) "Снять" else "Надеть",
                preview = { FoxWithOutfit(outfit, Modifier.size(56.dp)) },
            )
        }
        item { SectionTitle("Темы оформления") }
        item {
            CosmeticRow(
                title = "Тема «Лес»", description = "Зелёная тема LinguaFree", price = 0, owned = true,
                active = state.settings.accent == ThemeAccent.Forest.name, canAfford = true,
                onBuy = {}, onUse = { viewModel.applyTheme(ThemeAccent.Forest) }, useLabel = "Включить",
                preview = { Swatch(ThemeAccent.Forest) },
            )
        }
        items(listOf(ShopItem.ThemeOcean to ThemeAccent.Ocean, ShopItem.ThemeSunset to ThemeAccent.Sunset, ShopItem.ThemeLilac to ThemeAccent.Lilac)) { (item, accent) ->
            CosmeticRow(
                title = item.title, description = item.description, price = item.price, owned = item.name in state.owned,
                active = state.settings.accent == accent.name, canAfford = w.gems >= item.price,
                onBuy = { viewModel.buy(item) }, onUse = { viewModel.applyTheme(accent) }, useLabel = "Включить",
                preview = { Swatch(accent) },
            )
        }
        item {
            Text(
                "В LinguaFree нет покупок за деньги, подписок и рекламы. Кристаллы дают за уроки, задания дня, сундуки, достижения и места в лиге.",
                style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    message?.let { msg ->
        AlertDialog(onDismissRequest = viewModel::dismiss, text = { Text(msg) }, confirmButton = { TextButton(onClick = viewModel::dismiss) { Text("OK") } })
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = LfTheme.colors.text, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Swatch(accent: ThemeAccent) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(accent.primary).border(4.dp, accent.primaryLip, CircleShape))
}

@Composable
private fun ShopRow(item: ShopItem, @DrawableRes icon: Int, state: ShopUiState, status: String?, available: Boolean, onBuy: (ShopItem) -> Unit) {
    val c = LfTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, c.border, RoundedCornerShape(16.dp)).padding(14.dp).testTag("shop-${item.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LfIcon(icon, size = 44.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(item.description, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            status?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = c.gems) }
        }
        LfButton(
            "${item.price} 💎", onClick = { onBuy(item) }, style = LfButtonStyle.Gems,
            enabled = available && state.wallet.gems >= item.price,
        )
    }
}

@Composable
private fun CosmeticRow(
    title: String, description: String, price: Int, owned: Boolean, active: Boolean, canAfford: Boolean,
    onBuy: () -> Unit, onUse: () -> Unit, useLabel: String, preview: @Composable () -> Unit,
) {
    val c = LfTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, if (active) c.primary else c.border, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        preview()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(if (active) "Используется" else description, style = MaterialTheme.typography.bodySmall, color = if (active) c.primary else c.textMuted)
        }
        when {
            owned -> LfButton(useLabel, onClick = onUse, style = LfButtonStyle.Secondary, enabled = !(active && useLabel == "Включить"))
            else -> LfButton("$price 💎", onClick = onBuy, style = LfButtonStyle.Gems, enabled = canAfford)
        }
    }
}
