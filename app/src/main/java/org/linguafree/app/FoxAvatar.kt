package org.linguafree.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay

val foxAvatarNames=listOf("Моя первая лисичка","Рыжик","Снежинка","Космос","Лесная","Солнышко","Розочка")

@Composable
fun FoxAvatar(original:ImageBitmap,avatar:Int,modifier:Modifier=Modifier,mood:String="curious"){
 var touch by remember{mutableIntStateOf(0)}
 var greeting by remember{mutableStateOf(false)}
 val context=LocalContext.current
 val haptics=LocalHapticFeedback.current
 LaunchedEffect(touch){if(touch>0){greeting=true;delay(1400);greeting=false}}
 val scale by animateFloatAsState(if(greeting)1.08f else 1f,animationSpec=spring(),label="avatarTouch")
 val interactive=modifier.graphicsLayer{scaleX=scale;scaleY=scale}.clickable(role=Role.Button,onClickLabel="Поздороваться с лисичкой"){
  touch++
  val prefs=context.getSharedPreferences("linguafree_progress",android.content.Context.MODE_PRIVATE)
  if(prefs.getBoolean("hapticsEnabled",prefs.getBoolean("effectsEnabled",true)))haptics.performHapticFeedback(HapticFeedbackType.LongPress)
 }
 if(avatar==0)Image(bitmap=original,contentDescription="Лисичка: ${foxAvatarNames[0]}",modifier=interactive)
 else AnimatedFox(modifier=interactive,mood=if(greeting)"greeting" else mood,variant=(avatar-1).coerceIn(0,5))
}

@Composable
fun FoxAvatarPicker(original:ImageBitmap,selected:Int,onSelect:(Int)->Unit,onBack:()->Unit){
 Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)){
  TextButton(onClick=onBack){Text("‹ К урокам")}
  Text("🦊 Выбери свою лисичку",style=MaterialTheme.typography.headlineSmall)
  Text("Выбор сохранится и появится на экранах обучения.")
  foxAvatarNames.forEachIndexed { index,name ->
   OutlinedButton(onClick={onSelect(index)},modifier=Modifier.fillMaxWidth()){
    FoxAvatar(original,index,Modifier.size(64.dp),mood=if(index==selected)"happy" else "curious")
    Spacer(Modifier.width(12.dp))
    Text((if(index==selected)"✓ " else "")+name,modifier=Modifier.weight(1f))
   }
  }
 }
}
