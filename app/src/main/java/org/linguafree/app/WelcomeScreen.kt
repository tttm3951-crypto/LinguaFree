package org.linguafree.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(original:ImageBitmap,avatar:Int,name:String,lesson:Int,xp:Int,
                  onContinue:()->Unit,onHome:()->Unit,onAvatar:()->Unit,onHide:()->Unit){
 Column(Modifier.fillMaxWidth().padding(horizontal=10.dp),horizontalAlignment=Alignment.CenterHorizontally,
     verticalArrangement=Arrangement.spacedBy(16.dp)){
  Text("С возвращением, $name!",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
  FoxAvatar(original,avatar,Modifier.size(180.dp),mood="greeting")
  Text("🦊 Коснись лисички — она поздоровается!",style=MaterialTheme.typography.bodyLarge)
  Text("Твой прогресс на месте: $xp XP. Продолжим с урока ${lesson+1}?")
  Button(onClick=onContinue,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text("Продолжить обучение 🚀")}
  OutlinedButton(onClick=onHome,modifier=Modifier.fillMaxWidth()){Text("Выбрать занятие")}
  TextButton(onClick=onAvatar){Text("Выбрать другую лисичку")}
  TextButton(onClick=onHide){Text("Пропускать приветствие при следующем запуске")}
 }
}
