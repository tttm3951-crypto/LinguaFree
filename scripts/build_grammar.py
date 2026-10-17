#!/usr/bin/env python3
"""Original, reviewed seed content; print JSON for reproducible asset generation."""
import json

topics=[]
def topic(id,level,group,title,rule,rows):
    questions=[]
    for i,row in enumerate(rows.strip().splitlines()):
        prompt,correct,wrong1,wrong2=row.split('|')
        choices=[correct,wrong1,wrong2]
        shift=i%3
        choices=choices[shift:]+choices[:shift]
        questions.append(dict(id=f'{id}-{i+1}',prompt=prompt,choices=choices,
            answer=choices.index(correct),speech=correct,explanation=rule))
    topics.append(dict(id=id,level=level,group=group,title=title,rule=rule,
                       examples=[q['speech'] for q in questions[:2]],questions=questions))

topic('a1-subject','A1','pronouns','Кто действует? Личные местоимения',
      'I — я, you — ты/вы, he — он, she — она, it — предмет или животное, we — мы, they — они. Местоимение заменяет имя перед глаголом.', '''
Лиса Мия говорит: «Я твой друг».|I am your friend.|Me am your friend.|My am your friend.
Том — мальчик. Замени имя: «Он мой брат».|He is my brother.|His is my brother.|Him is my brother.
Лена — девочка. Замени имя: «Она читает».|She reads.|Her reads.|Hers reads.
Ты и я вместе: «Мы готовы».|We are ready.|Us are ready.|Our are ready.
Две лисы: «Они играют».|They play.|Them play.|Their play.
''')
topic('a1-be','A1','verbs','Глагол-связка be: am, is, are',
      'Be связывает человека или предмет с описанием: I am, he/she/it is, you/we/they are. В вопросе be ставится перед подлежащим; в отрицании добавь not.', '''
Выбери: «Я счастлив».|I am happy.|I is happy.|I are happy.
Выбери: «Моя сестра дома».|My sister is at home.|My sister are at home.|My sister am at home.
Выбери: «Мы не устали».|We are not tired.|We is not tired.|We am not tired.
Спроси: «Ты готов?»|Are you ready?|Is you ready?|Am you ready?
Спроси: «Он твой учитель?»|Is he your teacher?|Are he your teacher?|Am he your teacher?
''')
topic('a1-main-aux','A1','verbs','Смысловые глаголы и помощник do',
      'Смысловой глагол называет действие. В Present Simple do/does помогают задать вопрос или построить отрицание. После does/does not основной глагол употребляется без -s.', '''
Спроси: «Ты любишь музыку?»|Do you like music?|Does you like music?|Are you like music?
Спроси: «Она играет в теннис?»|Does she play tennis?|Does she plays tennis?|Do she play tennis?
Выбери: «Я не ем мясо».|I do not eat meat.|I does not eat meat.|I am not eat meat.
Выбери: «Он не смотрит телевизор».|He does not watch TV.|He does not watches TV.|He do not watch TV.
Выбери: «Мой брат читает каждый день».|My brother reads every day.|My brother read every day.|My brother reading every day.
''')
topic('a1-present','A1','tenses','Present Simple: привычки и факты',
      'Present Simple — привычки и общие факты. С he/she/it обычно добавляем -s или -es. С I/you/we/they используем начальную форму.', '''
Каждое утро я хожу в школу.|I go to school every morning.|I goes to school every morning.|I going to school every morning.
Она каждый день пьёт воду.|She drinks water every day.|She drink water every day.|She drinking water every day.
Мы часто играем в парке.|We often play in the park.|We often plays in the park.|We often playing in the park.
Мой папа работает в больнице.|My dad works in a hospital.|My dad work in a hospital.|My dad working in a hospital.
Солнце встаёт на востоке.|The sun rises in the east.|The sun rise in the east.|The sun rising in the east.
''')
topic('a1-possessive','A1','pronouns','Мой или твой? Принадлежность',
      'Перед существительным ставим my, your, his, her, its, our, their: my book. Когда существительное уже понятно, можно сказать mine, yours, his, hers, ours, theirs: The book is mine.', '''
Выбери: «Это моя книга».|This is my book.|This is me book.|This is mine book.
Выбери: «Это её сумка».|This is her bag.|This is she bag.|This is hers bag.
Выбери: «Это наш дом».|This is our house.|This is us house.|This is ours house.
Выбери: «Эта ручка моя».|This pen is mine.|This pen is my.|This pen is me.
Спроси: «Эта куртка твоя?»|Is this jacket yours?|Is this jacket your?|Is this jacket you?
''')

topic('a2-object','A2','pronouns','Кому? Объектные местоимения',
      'После глагола и предлога нужны me, you, him, her, it, us, them: help me, with us. Не путай их с I, he, she, we, they перед глаголом.', '''
Выбери: «Пожалуйста, помоги мне».|Please help me.|Please help I.|Please help my.
Выбери: «Я вижу его».|I can see him.|I can see he.|I can see his.
Выбери: «Пойдём с нами».|Come with us.|Come with we.|Come with our.
Выбери: «Она знает их».|She knows them.|She knows they.|She knows their.
Выбери: «Эта открытка для неё».|This card is for her.|This card is for she.|This card is for hers.
''')
topic('a2-regular-irregular','A2','verbs','Правильные и неправильные глаголы',
      'В Past Simple правильные глаголы получают -ed: play → played. Неправильные формы учим в контексте: go → went, eat → ate, see → saw. После did/did not снова нужна начальная форма.', '''
Вчера мы играли в футбол.|We played football yesterday.|We play football yesterday.|We playing football yesterday.
Вчера я ходил в парк.|I went to the park yesterday.|I goed to the park yesterday.|I go to the park yesterday.
Она съела яблоко вчера.|She ate an apple yesterday.|She eated an apple yesterday.|She eat an apple yesterday.
Ты видел лису вчера?|Did you see a fox yesterday?|Did you saw a fox yesterday?|Did you seen a fox yesterday?
Мы не смотрели фильм вчера.|We did not watch a film yesterday.|We did not watched a film yesterday.|We did not watching a film yesterday.
''')
topic('a2-modal','A2','verbs','Модальные глаголы: can, must, should',
      'Can — умение или возможность, must — сильная необходимость, should — совет. После них ставим глагол без to и без -s. Must not означает запрет, do not have to — отсутствие необходимости.', '''
Я умею плавать.|I can swim.|I can to swim.|I can swims.
Тебе следует отдохнуть.|You should rest.|You should to rest.|You should rests.
Он должен надеть шлем.|He must wear a helmet.|He must wears a helmet.|He must to wear a helmet.
Здесь нельзя бегать.|You must not run here.|You must not to run here.|You must not runs here.
Завтра тебе не обязательно приходить.|You do not have to come tomorrow.|You do not has to come tomorrow.|You does not have to come tomorrow.
''')
topic('a2-now-usual','A2','tenses','Present Continuous: сейчас или обычно?',
      'Действие сейчас: am/is/are + глагол-ing. Обычное действие: Present Simple. Подсказки now и every day помогают понять смысл, но сначала прочитай всю ситуацию.', '''
Смотри! Лиса сейчас бежит.|Look! The fox is running.|Look! The fox running.|Look! The fox are running.
Я сейчас читаю книгу.|I am reading a book now.|I reading a book now.|I is reading a book now.
Мы обычно обедаем в час.|We usually have lunch at one.|We usually has lunch at one.|We usually having lunch at one.
Они сейчас не спят.|They are not sleeping now.|They is not sleeping now.|They not sleeping now.
Спроси: «Что она сейчас делает?»|What is she doing now?|What she is doing now?|What are she doing now?
''')
topic('a2-future','A2','tenses','Будущее: планы и решения',
      'Be going to — заранее задуманный план. Will часто выражает решение в момент речи или обещание. Оба способа могут описывать будущее; форму выбираем по ситуации.', '''
Мы уже запланировали поездку: «Мы собираемся посетить музей».|We are going to visit the museum.|We is going to visit the museum.|We going to visit the museum.
Я только что решил помочь: «Я понесу твою сумку».|I will carry your bag.|I will carries your bag.|I will to carry your bag.
Она заранее решила испечь торт.|She is going to bake a cake.|She are going to bake a cake.|She going to bake a cake.
Обещаю: «Я тебе позвоню».|I will call you.|I will calls you.|I will calling you.
Спроси о плане: «Вы собираетесь играть?»|Are you going to play?|Is you going to play?|You are going to play?
''')

topic('b1-reflexive','B1','pronouns','Возвратные и неопределённые местоимения',
      'Myself/yourself/himself/herself/ourselves/themselves указывают, что действие направлено на самого участника. Someone — кто-то, anyone — кто-нибудь, nobody — никто. С nobody не добавляем второе отрицание.', '''
Я сделал этот плакат сам.|I made this poster myself.|I made this poster meself.|I made this poster myselves.
Она посмотрела на себя в зеркало.|She looked at herself in the mirror.|She looked at sheself in the mirror.|She looked at herselfs in the mirror.
Мы сами приготовили обед.|We cooked lunch ourselves.|We cooked lunch ourself.|We cooked lunch usselves.
В комнате никого не было.|Nobody was in the room.|Nobody were in the room.|Nobody weren't in the room.
Кто-нибудь знает ответ?|Does anyone know the answer?|Do anyone know the answer?|Does anyone knows the answer?
''')
topic('b1-stative','B1','verbs','Глаголы состояния и действия',
      'Know, believe, need, understand обычно описывают состояние и не употребляются в Continuous. У некоторых глаголов форма зависит от значения: I think — считаю, I am thinking — обдумываю.', '''
Состояние: «Я знаю ответ».|I know the answer.|I am knowing the answer.|I knows the answer.
Состояние: «Она понимает правило».|She understands the rule.|She is understanding the rule.|She understand the rule.
Сейчас мне нужна помощь.|I need help now.|I am needing help now.|I needs help now.
Мнение: «Я думаю, что это хорошая идея».|I think it is a good idea.|I thinks it is a good idea.|I thinking it is a good idea.
Процесс сейчас: «Я обдумываю задачу».|I am thinking about the problem.|I thinking about the problem.|I am think about the problem.
''')
topic('b1-transitivity','B1','verbs','Переходные глаголы и предлоги',
      'Переходный глагол может иметь прямое дополнение: open the door. Непереходному оно не требуется: arrive. Некоторые сочетания требуют предлога: listen to music, wait for a friend. Один глагол может иметь несколько моделей.', '''
Открой дверь, пожалуйста.|Please open the door.|Please open to the door.|Please open at the door.
Мы слушаем музыку.|We listen to music.|We listen music.|We listen for music.
Я жду своего друга.|I am waiting for my friend.|I am waiting my friend.|I am waiting to my friend.
Она прибыла на станцию.|She arrived at the station.|She arrived the station.|She arrived to the station.
Они обсуждали план.|They discussed the plan.|They discussed about the plan.|They discussed to the plan.
''')
topic('b1-perfect','B1','tenses','Present Perfect и Past Simple',
      'Present Perfect: have/has + третья форма, связь с настоящим или опыт без законченного времени. Past Simple: законченное прошлое, например yesterday. Been и gone различаются: побывал и вернулся / ушёл и ещё не вернулся.', '''
Опыт без указания времени: «Я побывал в Лондоне».|I have been to London.|I have went to London.|I has been to London.
Законченное время: «Я посетил Лондон в прошлом году».|I visited London last year.|I have visited London last year.|I visit London last year.
Она уже закончила домашнее задание.|She has already finished her homework.|She have already finished her homework.|She has already finish her homework.
Мы ещё не видели этот фильм.|We have not seen this film yet.|We have not saw this film yet.|We has not seen this film yet.
Ты когда-нибудь ел суши?|Have you ever eaten sushi?|Have you ever ate sushi?|Has you ever eaten sushi?
''')
topic('b1-past-continuous','B1','tenses','Past Continuous: фон истории',
      'Was/were + глагол-ing — действие в процессе в прошлом. Past Simple — отдельное событие на этом фоне: I was reading when the phone rang.', '''
В восемь вчера я читал.|I was reading at eight yesterday.|I were reading at eight yesterday.|I was read at eight yesterday.
Мы играли, когда начался дождь.|We were playing when it started to rain.|We was playing when it started to rain.|We were play when it started to rain.
Она готовила, когда я пришёл.|She was cooking when I arrived.|She were cooking when I arrived.|She was cook when I arrived.
Они спали в полночь.|They were sleeping at midnight.|They was sleeping at midnight.|They were sleep at midnight.
Что ты делал в шесть вчера?|What were you doing at six yesterday?|What was you doing at six yesterday?|What were you do at six yesterday?
''')

topic('b2-relative','B2','pronouns','Относительные местоимения и уточнения',
      'Who — о людях, which — о вещах, whose — о принадлежности. В ограничительных придаточных часто возможно that; в этих заданиях выбираем из предложенных who/which/whose. После запятой that не используется.', '''
Выбери местоимение: «Девочка, которая выиграла, — моя сестра».|The girl who won is my sister.|The girl which won is my sister.|The girl whose won is my sister.
Книга, которая лежит на столе, моя.|The book which is on the table is mine.|The book who is on the table is mine.|The book whose is on the table is mine.
Это мальчик, чья собака потерялась.|This is the boy whose dog is missing.|This is the boy who dog is missing.|This is the boy which dog is missing.
Моя тётя, которая живёт в Риме, — врач.|My aunt, who lives in Rome, is a doctor.|My aunt, that lives in Rome, is a doctor.|My aunt, whose lives in Rome, is a doctor.
Мой велосипед, который новый, красный.|My bike, which is new, is red.|My bike, that is new, is red.|My bike, who is new, is red.
''')
topic('b2-passive','B2','verbs','Активный и пассивный залог',
      'В пассиве важен объект действия: be в нужном времени + третья форма. The team built the bridge → The bridge was built by the team. Не каждый глагол и не каждый контекст допускают такой переход.', '''
Пассив: «Эту книгу написал молодой автор».|This book was written by a young author.|This book was wrote by a young author.|This book were written by a young author.
Пассив: «Здесь говорят по-английски».|English is spoken here.|English is speak here.|English are spoken here.
Пассив: «Окна моют каждую неделю».|The windows are cleaned every week.|The windows is cleaned every week.|The windows are clean every week.
Пассив: «Результаты объявят завтра».|The results will be announced tomorrow.|The results will announced tomorrow.|The results will be announce tomorrow.
Пассив: «Дверь уже отремонтировали».|The door has already been repaired.|The door has already be repaired.|The door have already been repaired.
''')
topic('b2-past-perfect','B2','tenses','Past Perfect: что случилось раньше',
      'Had + третья форма показывает действие раньше другого момента в прошлом. Сначала автобус уехал, потом мы пришли: The bus had left before we arrived. Контекст важнее механического поиска before/after.', '''
Когда мы пришли, автобус уже уехал.|The bus had already left when we arrived.|The bus had already leave when we arrived.|The bus has already left when we arrived yesterday.
Я закончил работу до того, как она позвонила.|I had finished my work before she called.|I had finish my work before she called.|I have finished my work before she called yesterday.
Он никогда не видел снег до той поездки.|He had never seen snow before that trip.|He had never saw snow before that trip.|He had never see snow before that trip.
Они поели до начала фильма.|They had eaten before the film started.|They had ate before the film started.|They had eat before the film started.
Я понял, что забыл ключи.|I realised that I had forgotten my keys.|I realised that I had forgot my keys.|I realised that I have forget my keys.
''')
topic('b2-future-perfect','B2','tenses','Future Continuous и Future Perfect',
      'Will be + -ing — процесс в определённый момент будущего. Will have + третья форма — результат к будущему сроку. Сравни: tomorrow at six / by tomorrow evening.', '''
Завтра в шесть я буду ехать домой.|I will be travelling home at six tomorrow.|I will travelling home at six tomorrow.|I will be travel home at six tomorrow.
К пятнице мы закончим проект.|We will have finished the project by Friday.|We will have finish the project by Friday.|We will has finished the project by Friday.
В это время завтра она будет лететь в Рим.|She will be flying to Rome this time tomorrow.|She will flying to Rome this time tomorrow.|She will be fly to Rome this time tomorrow.
К полудню они уже уедут.|They will have left by noon.|They will have leave by noon.|They will has left by noon.
В восемь завтра мы будем ужинать.|We will be having dinner at eight tomorrow.|We will having dinner at eight tomorrow.|We will be have dinner at eight tomorrow.
''')
topic('b2-modal-perfect','B2','verbs','Модальные глаголы о прошлом',
      'Must have + третья форма — уверенный вывод о прошлом; might have — возможность; should have — совет или сожаление о прошлом. Это не то же самое, что обязанность в прошлом: had to.', '''
Вывод: «Он, должно быть, забыл встречу».|He must have forgotten the meeting.|He must have forgot the meeting.|He must has forgotten the meeting.
Возможно, она пропустила автобус.|She might have missed the bus.|She might has missed the bus.|She might have miss the bus.
Тебе следовало позвонить раньше.|You should have called earlier.|You should has called earlier.|You should have call earlier.
Она не могла это увидеть: её там не было.|She cannot have seen it; she was not there.|She cannot have saw it; she was not there.|She cannot has seen it; she was not there.
Вчера мне пришлось уйти рано.|I had to leave early yesterday.|I must to leave early yesterday.|I had to left early yesterday.
''')

if __name__=='__main__':
    print(json.dumps(topics,ensure_ascii=False,indent=2))
