package org.linguafree.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** Original vector character. Body, tail, ears, blinking, gaze and gestures animate independently. */
@Composable
fun AnimatedFox(modifier:Modifier=Modifier.size(120.dp),mood:String="curious",variant:Int=0){
 val palette=listOf(
  listOf(0xFFF28A30,0xFFCE5E19,0xFFFFD19C),
  listOf(0xFFE4EEF7,0xFF8EAABE,0xFFFFFFFF),
  listOf(0xFF9F86E8,0xFF6844A3,0xFFD9C9FF),
  listOf(0xFF73B5A0,0xFF397C69,0xFFD5F5DF),
  listOf(0xFFF1C350,0xFFBD8125,0xFFFFE9A3),
  listOf(0xFFEB98BC,0xFFAB507C,0xFFFFD8E8)
 )[variant.coerceIn(0,5)]
 val fur=Color(palette[0]);val darkFur=Color(palette[1]);val highlight=Color(palette[2])
 val cream=Color(0xFFFFEAD6);val ink=Color(0xFF24313E)
 val transition=rememberInfiniteTransition(label="foxBody")
 val blink by transition.animateFloat(1f,1f,infiniteRepeatable(keyframes{
  durationMillis=4200;1f at 0;1f at 3700;0.08f at 3820;1f at 3940;1f at 4200
 }),label="foxBlink")
 val wag by transition.animateFloat(-15f,15f,infiniteRepeatable(tween(650),RepeatMode.Reverse),label="foxTail")
 val wave by transition.animateFloat(-18f,18f,infiniteRepeatable(tween(350),RepeatMode.Reverse),label="foxPaws")
 val ear by transition.animateFloat(-2f,2f,infiniteRepeatable(tween(1150),RepeatMode.Reverse),label="foxEars")
 val breath by transition.animateFloat(1f,1.025f,infiniteRepeatable(tween(1600),RepeatMode.Reverse),label="foxBreathing")
 val dance by transition.animateFloat(-1f,1f,infiniteRepeatable(tween(360),RepeatMode.Reverse),label="foxDance")
 val gaze by animateFloatAsState(if(mood=="listening")-2f else if(mood=="curious")1.5f else 0f,label="foxGaze")
 val smile by animateFloatAsState(if(mood=="happy"||mood=="celebrate"||mood=="greeting")1f else 0f,label="foxSmile")
 val tilt by animateFloatAsState(if(mood=="support")-6f else if(mood=="curious"||mood=="listening")5f else 0f,label="foxTilt")
 val celebrate=mood=="celebrate"
 Canvas(modifier){
  val u=min(size.width,size.height)/100f
  fun oval(c:Color,x:Float,y:Float,w:Float,h:Float){drawOval(c,Offset(x*u,y*u),Size(w*u,h*u))}
  fun triangle(c:Color,ax:Float,ay:Float,bx:Float,by:Float,cx:Float,cy:Float){
   drawPath(Path().apply{moveTo(ax*u,ay*u);lineTo(bx*u,by*u);lineTo(cx*u,cy*u);close()},c)
  }
  oval(ink.copy(alpha=.13f),26f,94f,50f,4f)
  translate(top=if(celebrate)dance*3*u else 0f){
   // Tail has its own pivot, independent of the head and paws.
   rotate(wag,Offset(63*u,78*u)){
    drawPath(Path().apply{
     moveTo(61*u,85*u);cubicTo(99*u,96*u,103*u,52*u,85*u,48*u)
     cubicTo(90*u,69*u,69*u,63*u,61*u,85*u);close()
    },fur)
    triangle(cream,85f,48f,96f,66f,80f,66f)
   }
   scale(1f,breath,Offset(50*u,93*u)){
    oval(darkFur,29f,57f,43f,38f);oval(cream,38f,61f,26f,29f)
   }
   oval(darkFur,27f,89f,23f,8f);oval(darkFur,53f,89f,23f,8f)
   oval(cream,30f,91f,17f,4f);oval(cream,56f,91f,17f,4f)
   val arm=if(celebrate)-125f else if(mood=="greeting")-90f+wave else if(mood=="support")-35f else wave*.15f
   rotate(arm,Offset(33*u,66*u)){oval(fur,24f,64f,12f,23f);oval(cream,25f,78f,10f,8f)}
   rotate(if(celebrate)125f else -wave*.15f,Offset(68*u,66*u)){oval(fur,66f,64f,12f,23f);oval(cream,67f,78f,10f,8f)}
   rotate(tilt+(if(celebrate)dance*4 else 0f),Offset(50*u,49*u)){
    triangle(darkFur,20f,34f,24f,5f+ear,43f,26f)
    triangle(darkFur,57f,26f,76f,5f-ear,80f,34f)
    triangle(Color(0xFFFFBFA8),25f,26f,27f,13f+ear,37f,27f)
    triangle(Color(0xFFFFBFA8),63f,27f,73f,13f-ear,75f,26f)
    oval(fur,17f,20f,66f,50f);oval(highlight,31f,28f,38f,24f)
    oval(cream,20f,43f,32f,24f);oval(cream,48f,43f,32f,24f)
    val eyeHeight=if(mood=="surprised")13f else if(mood=="support")6f else 10f
    oval(ink,33f+gaze,37f,7f,eyeHeight*blink);oval(ink,60f+gaze,37f,7f,eyeHeight*blink)
    oval(Color.White,35f+gaze,38f,2f,3f*blink);oval(Color.White,62f+gaze,38f,2f,3f*blink)
    triangle(ink,45f,49f,55f,49f,50f,56f)
    oval(Color(0xFFAD5C65),45f,58f,10f,if(mood=="surprised")9f else 2f+smile*5f)
    if(mood=="support"||smile>.5f){oval(Color(0xFFFFB5A4),25f,49f,10f,4f);oval(Color(0xFFFFB5A4),65f,49f,10f,4f)}
   }
  }
  if(celebrate){
   listOf(Color(0xFFFFD95B),Color(0xFFBCA4FF),Color(0xFFFF92B7)).forEachIndexed{i,c->
    oval(c,8f+i*37f,10f+(i%2)*10f+dance*4,4f,4f)
    oval(c,12f+i*34f,32f-(i%2)*20f-dance*3,3f,3f)
   }
  }
 }
}
