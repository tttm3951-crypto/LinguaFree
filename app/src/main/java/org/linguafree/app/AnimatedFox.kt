package org.linguafree.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Original LinguaFree fox: independent animated ears, eyelids, mouth and paws.
 * Transparent background. No third-party characters or image assets.
 */
@Composable
fun AnimatedFox(modifier:Modifier=Modifier.size(120.dp),happy:Boolean=false){
 val animation=rememberInfiniteTransition(label="foxBody")
 val blink by animation.animateFloat(1f,0.10f,infiniteRepeatable(tween(120,delayMillis=2900),RepeatMode.Reverse),label="foxBlink")
 val paws by animation.animateFloat(-5f,5f,infiniteRepeatable(tween(420),RepeatMode.Reverse),label="foxPaws")
 val ears by animation.animateFloat(-3f,3f,infiniteRepeatable(tween(750),RepeatMode.Reverse),label="foxEars")
 val mouth by animateFloatAsState(if(happy)1f else 0f,label="foxSmile")
 Canvas(modifier){
  val u=min(size.width,size.height)/100f
  fun oval(color:Color,x:Float,y:Float,w:Float,h:Float){
   drawOval(color,Offset(x*u,y*u),Size(w*u,h*u))
  }
  fun tri(color:Color,ax:Float,ay:Float,bx:Float,by:Float,cx:Float,cy:Float){
   val path=Path().apply{moveTo(ax*u,ay*u);lineTo(bx*u,by*u);lineTo(cx*u,cy*u);close()}
   drawPath(path,color)
  }
  // Ears animate separately from the head.
  tri(Color(0xFFCE5E19),14f,38f,22f,4f+ears,43f,27f)
  tri(Color(0xFFCE5E19),57f,27f,78f,4f-ears,86f,38f)
  tri(Color(0xFFFFC3B0),22f,29f,25f,15f+ears,36f,28f)
  tri(Color(0xFFFFC3B0),64f,28f,75f,15f-ears,78f,29f)
  oval(Color(0xFFF28A30),13f,20f,74f,68f)
  // White muzzle, separate cheek highlights.
  oval(Color(0xFFFFEAD6),18f,52f,33f,28f)
  oval(Color(0xFFFFEAD6),49f,52f,33f,28f)
  oval(Color(0xFFFFD19C),29f,32f,42f,25f)
  oval(Color(0xFF202F37),32f,44f,7f,12f*blink)
  oval(Color(0xFF202F37),61f,44f,7f,12f*blink)
  oval(Color.White,34f,45f,2f,2f*blink)
  oval(Color.White,63f,45f,2f,2f*blink)
  tri(Color(0xFF32242A),45f,60f,55f,60f,50f,67f)
  oval(Color(0xFFAF5A56),45f,70f,10f,if(happy)7f else 2f+mouth*4f)
  // Paws move independently in opposite directions.
  oval(Color(0xFFD96825),17f,80f+paws,25f,15f)
  oval(Color(0xFFD96825),58f,80f-paws,25f,15f)
  oval(Color(0xFFFFD3A4),21f,85f+paws,17f,7f)
  oval(Color(0xFFFFD3A4),62f,85f-paws,17f,7f)
 }
}
