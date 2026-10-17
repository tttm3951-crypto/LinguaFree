package org.linguafree.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Measures actual text before deciding whether the device needs an accessibility scroll fallback. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseView(
    title:String,prompt:String,choices:List<String>,answer:Int,selected:Int?,explanation:String,
    audio:Boolean,showAudio:Boolean,typed:Boolean,missing:Boolean,wordBank:Boolean,
    typedAnswer:String,onTyped:(String)->Unit,onAnswer:(Int)->Unit,onSpeak:(Boolean)->Unit,
    onBack:()->Unit,modifier:Modifier=Modifier
){
    val correct=choices[answer]
    val missingIndex=correct.indexOfFirst{it.lowercaseChar() in "aeiou"}.let{if(it>0)it else 1}.coerceAtMost(correct.lastIndex)
    var bank by remember(prompt,choices){mutableStateOf(emptyList<Int>())}
    var hint by remember(prompt){mutableStateOf(false)}
    val tokens=remember(correct){(correct.split(" ")+"the").shuffled()}
    val measurer=rememberTextMeasurer()
    val density=LocalDensity.current
    BoxWithConstraints(modifier){
        val textSize=if(maxHeight<520.dp)20.sp else 23.sp
        val width=with(density){(maxWidth-24.dp).roundToPx().coerceAtLeast(1)}
        fun textHeight(text:String,size:androidx.compose.ui.unit.TextUnit):Float{
            return with(density){measurer.measure(AnnotatedString(text),TextStyle(fontSize=size),constraints=Constraints(maxWidth=width)).size.height.toDp().value}
        }
        val optionHeight=choices.sumOf{maxOf(48f,textHeight(it,18.sp)+24).toDouble()}.toFloat()
        val feedbackHeight=if(selected!=null)textHeight(explanation,16.sp)+16 else 0f
        val inputHeight=if(typed)180f else if(wordBank)240f+textHeight(correct,18.sp) else optionHeight
        val required=textHeight(title,16.sp)+textHeight(if(audio)"Прослушай и выбери перевод" else prompt,textSize)+inputHeight+feedbackHeight+(if(showAudio)52 else 0)+72
        val fallback=required>maxHeight.value
        Column((if(fallback)Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxSize()),verticalArrangement=Arrangement.spacedBy(6.dp)){
            TextButton(onClick=onBack,contentPadding=PaddingValues(0.dp)){Text("‹ $title",fontSize=16.sp)}
            Text(if(audio)"🎧 Прослушай и выбери перевод" else prompt,fontSize=textSize)
            if(showAudio)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedButton(onClick={onSpeak(false)},modifier=Modifier.weight(1f)){Text("🔊 Слушать")}
                OutlinedButton(onClick={onSpeak(true)},modifier=Modifier.weight(1f)){Text("🐢 Медленно")}
            }
            if(typed){
                Text(if(missing)"Впиши букву: "+correct.replaceRange(missingIndex,missingIndex+1,"_") else "Напиши английский ответ",fontSize=16.sp)
                OutlinedTextField(value=typedAnswer,onValueChange=onTyped,enabled=selected==null,singleLine=true,modifier=Modifier.fillMaxWidth())
                if(selected==null){
                    TextButton(onClick={hint=!hint}){Text(if(hint)"Первая буква: ${correct.first()}" else "Нужна подсказка?")}
                    Button(onClick={val right=typedAnswer.trim().equals(if(missing)correct[missingIndex].toString() else correct,true);onAnswer(if(right)answer else (answer+1)%choices.size)},enabled=typedAnswer.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Проверить")}
                }
            }else if(wordBank){
                Text(bank.joinToString(" "){tokens[it]}.ifBlank{"Собери фразу из слов"},fontSize=18.sp)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    tokens.forEachIndexed{index,word->OutlinedButton(onClick={bank=bank+index},enabled=selected==null&&index !in bank){Text(word,fontSize=16.sp)}}
                }
                if(selected==null){
                    TextButton(onClick={bank=emptyList()}){Text("Очистить")}
                    Button(onClick={onAnswer(if(bank.joinToString(" "){tokens[it]}.equals(correct,true))answer else (answer+1)%choices.size)},enabled=bank.isNotEmpty(),modifier=Modifier.fillMaxWidth()){Text("Проверить")}
                }
            }else{
                choices.forEachIndexed{index,choice->
                    val optionModifier=if(!fallback)Modifier.fillMaxWidth().weight(1f).heightIn(min=48.dp) else Modifier.fillMaxWidth().heightIn(min=48.dp)
                    OutlinedButton(onClick={onAnswer(index)},enabled=selected==null,modifier=optionModifier,contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp)){
                        Text((if(selected!=null&&index==answer)"✓ " else "")+choice,fontSize=18.sp)
                    }
                }
            }
            if(selected!=null){
                Text(if(selected==answer)"🦊 Верно!" else "🦊 Верный ответ: $correct",fontSize=18.sp)
                Text(explanation,fontSize=16.sp)
            }
        }
    }
}
