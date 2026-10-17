package org.linguafree.core.ui.mascot

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import org.linguafree.core.ui.R

/** Shop outfits. Positions are fractions of the still "waiting" pose. */
enum class Outfit(val key: String, val res: Int, val cx: Float, val cy: Float, val width: Float, val aspect: Float, val rotation: Float, val title: String) {
    Scarf("OutfitScarf", R.drawable.acc_scarf, 0.60f, 0.59f, 0.42f, 160f / 90f, 0f, "Шарф"),
    Cap("OutfitCap", R.drawable.acc_cap, 0.64f, 0.15f, 0.36f, 120f / 80f, -8f, "Кепка"),
    Bow("OutfitBow", R.drawable.acc_bow, 0.82f, 0.25f, 0.15f, 80f / 50f, 20f, "Бантик"),
    Crown("OutfitCrown", R.drawable.acc_crown, 0.64f, 0.12f, 0.24f, 100f / 70f, -6f, "Корона");

    companion object {
        fun of(key: String?): Outfit? = entries.firstOrNull { it.key == key }
    }
}

/** The fox in its still pose wearing the chosen outfit (profile, shop preview). */
@Composable
fun FoxWithOutfit(outfit: Outfit?, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.aspectRatio(1f)) {
        Image(painterResource(R.drawable.fox_waiting_0), contentDescription = "Лисёнок" + (outfit?.let { " в наряде «${it.title}»" } ?: ""), modifier = Modifier.fillMaxSize())
        if (outfit != null) {
            val w = maxWidth * outfit.width
            val h = w / outfit.aspect
            Box(
                Modifier
                    .offset(x = maxWidth * outfit.cx - w / 2, y = maxWidth * outfit.cy - h / 2)
                    .width(w)
                    .aspectRatio(outfit.aspect)
                    .rotate(outfit.rotation)
            ) {
                Image(painterResource(outfit.res), contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

