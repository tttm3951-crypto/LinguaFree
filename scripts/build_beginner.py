"""Original beginner explanations and exercises; no book text/images reproduced."""
import json
from pathlib import Path

# title, level, category, rule, example EN/RU, five (prompt, right, two distractors, spoken answer).
UNITS = [
("Буква и звук", "Pre-A1", "reading", "Буква — знак, звук — то, что слышим. Имя буквы не всегда совпадает со звуком в слове. Начни с узнавания букв и коротких слов; не заменяй английские звуки русскими буквами.", ["A is a letter. — A — буква.", "I can read a word. — Я могу прочитать слово."], [
("Найди строчную букву для A", "a", "e", "o", "A is a letter."),
("Найди заглавную букву для b", "B", "D", "P", "B is a letter."),
("В какой паре одна и та же буква?", "M m", "N h", "R n", "This is the letter M."),
("Сколько букв в слове map?", "3", "2", "4", "This is a map."),
("Что мы произносим, когда читаем слово?", "Звуки", "Только имена букв", "Цвета", "I can read.")]),
("Короткие гласные", "Pre-A1", "reading", "Слушай слово целиком, затем сравнивай гласный звук. В коротких cat, bed, sit, dog, sun гласная не читается как её имя в алфавите. Звуки и написание изучаем вместе, а не по одному правилу без исключений.", ["The cat is small. — Кошка маленькая.", "The sun is up. — Солнце взошло."], [
("В каком слове гласная a?", "cat", "bed", "sit", "The cat is small."),
("Найди слово с короткой e", "bed", "sun", "dog", "This is my bed."),
("Найди слово с короткой i", "sit", "cat", "sun", "Please sit here."),
("В каком слове гласная o?", "dog", "bed", "sit", "The dog can run."),
("В каком слове гласная u?", "sun", "cat", "dog", "The sun is bright.")]),
("Немая e и чтение", "Pre-A1", "reading", "В парах cap–cape, kit–kite конечная e обычно не звучит, но меняет чтение предыдущей гласной. Это полезный образец, а не закон для любого английского слова. Сравни звучание готовых слов.", ["I have a kite. — У меня есть воздушный змей.", "This is a cake. — Это торт."], [
("Выбери слово с конечной немой e", "kite", "kit", "kid", "I have a kite."),
("Какое слово значит «торт»?", "cake", "cat", "cap", "This is a cake."),
("В какой паре добавлена конечная e?", "cap — cape", "cat — dog", "sun — bed", "This is a cape."),
("Какое слово значит «имя»?", "name", "man", "map", "My name is Mia."),
("Конечная e меняет чтение всегда без исключений?", "Нет, есть исключения", "Да, всегда", "В английском нет e", "There are exceptions.")]),
("Буквосочетания", "Pre-A1", "reading", "Две буквы могут обозначать один звук: sh, ch, th. В th кончик языка подходит к зубам; не пытайся оценивать акцент по написанию. Слушай и повторяй пары, сначала медленно.", ["This is a fish. — Это рыба.", "The chair is red. — Стул красный."], [
("Найди слово с sh", "fish", "cat", "pen", "This is a fish."),
("Найди слово с ch", "chair", "ship", "sun", "The chair is red."),
("Найди слово с th", "three", "tree", "tea", "I have three pens."),
("Какие буквы вместе есть в слове shop?", "sh", "ch", "ph", "The shop is open."),
("Сколько букв в сочетании ch?", "Две буквы", "Одна буква", "Три буквы", "This is a chair.")]),
("Я, ты, он и она", "Pre-A1", "words", "Местоимение заменяет имя: I — я, you — ты или вы, he — он, she — она, it — предмет или животное без уточнения пола. I всегда пишем с заглавной буквы.", ["She is my friend. — Она моя подруга.", "We are here. — Мы здесь."], [
("Выбери «Я здесь»", "I am here.", "He is here.", "They are here.", "I am here."),
("Выбери «Она моя подруга»", "She is my friend.", "He is my friend.", "It is my friend.", "She is my friend."),
("Tom — мальчик. Замени имя", "He is Tom.", "She is Tom.", "We are Tom.", "He is Tom."),
("Выбери «Мы готовы»", "We are ready.", "They are ready.", "I am ready.", "We are ready."),
("Как пишется «я» внутри английского предложения?", "Всегда I", "Всегда i", "Только me", "I can help.")]),
("Один и много", "Pre-A1", "words", "У большинства существительных множественное число образуется с -s: bag–bags. После некоторых окончаний добавляется -es: box–boxes. Есть особые формы: child–children; их узнаём в примерах.", ["I have two bags. — У меня две сумки.", "The children are here. — Дети здесь."], [
("Одна книга — book. Две книги?", "two books", "two book", "two bookes", "I have two books."),
("Одна коробка — box. Три коробки?", "three boxes", "three boxs", "three box", "There are three boxes."),
("Много детей", "The children are here.", "The childs are here.", "The child is here.", "The children are here."),
("Выбери единственное число", "one apple", "two apples", "three apples", "I have one apple."),
("Выбери множественное число", "four pens", "one pen", "a pen", "I have four pens.")]),
("Мой, твой, чей?", "Pre-A1", "words", "Перед предметом можно поставить my, your, his, her, our, their. Они отвечают на вопрос «чей?». Для имени владельца используем 's: Mia's bag. Не путай my bag и I am Mia.", ["This is our room. — Это наша комната.", "This is Mia's bag. — Это сумка Мии."], [
("Выбери «моя ручка»", "my pen", "I pen", "me pen", "This is my pen."),
("Выбери «твоя книга»", "your book", "you book", "yours book", "This is your book."),
("Выбери «её сумка»", "her bag", "she bag", "hers bag", "This is her bag."),
("Выбери «наша комната»", "our room", "we room", "us room", "This is our room."),
("Выбери «сумка Мии»", "Mia's bag", "Mia bag's", "Mias bag's", "This is Mia's bag.")]),
("Числа и порядок", "Pre-A1", "words", "Числа one, two, three называют количество. First, second, third называют место по порядку. Не смешивай thirteen (13) и thirty (30): слушай ударение и проверяй число в ситуации.", ["I have three pencils. — У меня три карандаша.", "This is my first lesson. — Это мой первый урок."], [
("У тебя 3 карандаша", "I have three pencils.", "I have thirteen pencils.", "I have thirty pencils.", "I have three pencils."),
("Какое число — thirteen?", "13", "30", "3", "I am thirteen."),
("Какое число — thirty?", "30", "13", "3", "There are thirty pages."),
("Выбери «первый урок»", "my first lesson", "my one lesson", "my third lesson", "This is my first lesson."),
("Выбери «второй день»", "the second day", "the two day", "the third day", "This is the second day.")]),
("Приветствие и знакомство", "Pre-A1", "sentences", "Сначала учимся здороваться, называть себя и задавать короткий вопрос. Good morning — утром, Good evening — вечером, Good night — при прощании перед сном. Вопрос и ответ произносим парой.", ["Hello! My name is Ben. — Привет! Меня зовут Бен.", "How are you? I am fine. — Как дела? У меня всё хорошо."], [
("Поздоровайся утром", "Good morning!", "Good night!", "Goodbye!", "Good morning!"),
("Назови себя: Ben", "My name is Ben.", "Your name is Ben.", "His name is Ben.", "My name is Ben."),
("Спроси «Как дела?»", "How are you?", "Where are you?", "Who is she?", "How are you?"),
("Ответь: всё хорошо", "I am fine, thank you.", "I am a pencil.", "It is under the bed.", "I am fine, thank you."),
("Попрощайся перед сном", "Good night!", "Good morning!", "Good afternoon!", "Good night!")]),
("A, an и the", "A1", "words", "A/an вводит один исчисляемый предмет, который ещё не определён для собеседника. An ставим перед гласным звуком, a — перед согласным: an apple, a book. The помогает назвать уже понятный или конкретный предмет. Ориентируемся на звук, а не только на букву.", ["I have an orange. — У меня есть апельсин.", "The orange is sweet. — Этот апельсин сладкий."], [
("Один ещё не известный собеседнику предмет: ___ apple", "an apple", "a apple", "apple an", "I have an apple."),
("Одна книга: ___ book", "a book", "an book", "book a", "I have a book."),
("Книгу уже назвали. Она синяя", "The book is blue.", "An book is blue.", "Book the is blue.", "The book is blue."),
("В hour начальная h не звучит. Выбери", "an hour", "a hour", "hour an", "I need an hour."),
("Выбираем a/an по чему?", "По первому звуку", "Только по первой букве", "По цвету предмета", "I have a yellow bag.")]),
("Это и те предметы", "A1", "words", "This — один предмет рядом, that — один дальше. These — несколько рядом, those — несколько дальше. Сопоставляй и расстояние, и количество.", ["This is my desk. — Это моя парта.", "Those are birds. — Вон те — птицы."], [
("Одна ручка у тебя в руке", "This is a pen.", "These are pens.", "Those are pens.", "This is a pen."),
("Несколько книг рядом", "These are books.", "This is a book.", "That is a book.", "These are books."),
("Один дом вдалеке", "That is a house.", "These are houses.", "Those are houses.", "That is a house."),
("Несколько птиц вдалеке", "Those are birds.", "That is a bird.", "This is a bird.", "Those are birds."),
("Выбери согласованное предложение", "These are my pencils.", "These is my pencil.", "This are my pencils.", "These are my pencils.")]),
("Сколько: some, many, much", "A1", "words", "Книги можно посчитать: many books. Воду обычно считаем порциями: much water. Some означает некоторое количество и часто используется в утверждениях. Для начала сравниваем предметы и вещества; сложные исключения оставим на потом.", ["There are some apples. — Есть несколько яблок.", "There is some water. — Есть немного воды."], [
("Книги считаются по одной", "How many books?", "How much books?", "How book many?", "How many books are there?"),
("Спроси о количестве воды", "How much water?", "How many water?", "How waters?", "How much water is there?"),
("Выбери «немного молока»", "some milk", "many milk", "a milk books", "There is some milk."),
("Выбери «много ручек»", "many pens", "much pens", "a pens", "There are many pens."),
("Что обычно нельзя посчитать без меры?", "water", "pencil", "apple", "I drink some water.")]),
("Где находится предмет?", "A1", "sentences", "In — внутри, on — на поверхности, under — под, next to — рядом. Строим простую схему: предмет + is + место. Проверяем смысл по ситуации, а не только переводим одно слово.", ["The ball is under the chair. — Мяч под стулом.", "The book is on the desk. — Книга на парте."], [
("Ключи внутри сумки", "The keys are in the bag.", "The keys are under the bag.", "The keys are on the bag.", "The keys are in the bag."),
("Книга на парте", "The book is on the desk.", "The book is in the desk.", "The book is under the desk.", "The book is on the desk."),
("Мяч под стулом", "The ball is under the chair.", "The ball is on the chair.", "The ball is in the chair.", "The ball is under the chair."),
("Школа рядом с парком", "The school is next to the park.", "The school is in the park.", "The school is under the park.", "The school is next to the park."),
("Между двумя коробками", "The toy is between the boxes.", "The toy is above the boxes.", "The toy is on the boxes.", "The toy is between the boxes.")]),
("Am, is, are", "A1", "sentences", "В английском предложении «Я рад» нужен глагол be: I am happy. С I — am, с he/she/it — is, с you/we/they — are. В вопросе be переходит перед тем, о ком спрашиваем; not создаёт отрицание.", ["You are kind. — Ты добрый.", "Is she ready? — Она готова?"], [
("Я рад", "I am happy.", "I is happy.", "I are happy.", "I am happy."),
("Она готова", "She is ready.", "She am ready.", "She are ready.", "She is ready."),
("Мы здесь", "We are here.", "We is here.", "We am here.", "We are here."),
("Спроси: она готова?", "Is she ready?", "She ready is?", "Are she ready?", "Is she ready?"),
("Скажи: он не устал", "He is not tired.", "He not tired.", "He are not tired.", "He is not tired.")]),
("У меня есть: have", "A1", "sentences", "Have означает «иметь»: I have a bag. С he/she/it в настоящем времени — has. В обычном вопросе используем do/does, а после does возвращаем have. Конструкцию have got можно изучить отдельно, не смешивая схемы.", ["She has a bike. — У неё есть велосипед.", "Do you have a pen? — У тебя есть ручка?"], [
("У меня есть сумка", "I have a bag.", "I has a bag.", "I am a bag.", "I have a bag."),
("У неё есть велосипед", "She has a bike.", "She have a bike.", "She is a bike.", "She has a bike."),
("Спроси: у тебя есть ручка?", "Do you have a pen?", "Are you have a pen?", "Does you has a pen?", "Do you have a pen?"),
("Спроси о нём", "Does he have a dog?", "Does he has a dog?", "Do he has a dog?", "Does he have a dog?"),
("У нас нет машины", "We do not have a car.", "We are not have a car.", "We does not have a car.", "We do not have a car.")]),
("Я умею: can", "A1", "sentences", "Can помогает рассказать об умении. После can используем глагол без to и без -s. Вопрос начинается с can; отрицание — cannot или can't. У разных лиц форма can одна и та же.", ["I can draw. — Я умею рисовать.", "Can you swim? — Ты умеешь плавать?"], [
("Я умею рисовать", "I can draw.", "I can to draw.", "I can draws.", "I can draw."),
("Она умеет плавать", "She can swim.", "She cans swim.", "She can swims.", "She can swim."),
("Спроси об умении плавать", "Can you swim?", "You swim can?", "Do you can swim?", "Can you swim?"),
("Я не умею летать", "I cannot fly.", "I not can fly.", "I can not to fly.", "I cannot fly."),
("Короткий положительный ответ", "Yes, I can.", "Yes, I do can.", "Yes, I am can.", "Yes, I can.")]),
("В комнате есть: there is/are", "A1", "sentences", "Чтобы впервые сказать, что где-то что-то есть, начинаем с There is (один предмет) или There are (несколько). Затем называем предмет и место. Вопрос меняет порядок is/are и there.", ["There is a lamp in the room. — В комнате есть лампа.", "There are two chairs. — Есть два стула."], [
("В комнате одна лампа", "There is a lamp in the room.", "There are a lamp in the room.", "There am a lamp in the room.", "There is a lamp in the room."),
("В комнате два стула", "There are two chairs.", "There is two chairs.", "There be two chairs.", "There are two chairs."),
("Спроси: есть ли парк?", "Is there a park?", "There a park is?", "Are there a park?", "Is there a park?"),
("Спроси: есть ли книги?", "Are there any books?", "Is there any books?", "There books are?", "Are there any books?"),
("В коробке нет игрушки", "There is no toy in the box.", "There are no toy in the box.", "There no is toy in the box.", "There is no toy in the box.")]),
("Что, где и кто?", "A1", "sentences", "What спрашивает «что», where — «где», who — «кто», when — «когда». Сначала короткий вопрос с be: Where is the bag? Затем — ответ целой фразой. Не нужно заучивать все типы вопросов за один урок.", ["Where is my bag? — Где моя сумка?", "Who is your teacher? — Кто твой учитель?"], [
("Спроси, где сумка", "Where is the bag?", "Who is the bag?", "When is the bag?", "Where is the bag?"),
("Спроси, кто учитель", "Who is the teacher?", "Where is the teacher?", "When is the teacher?", "Who is the teacher?"),
("Спроси, что это", "What is this?", "Who is this thing?", "Where this is?", "What is this?"),
("Спроси, когда урок", "When is the lesson?", "Who is the lesson?", "What lesson is when?", "When is the lesson?"),
("Ответь на Where is the cat?", "The cat is on the sofa.", "The cat is black.", "The cat is small.", "The cat is on the sofa.")]),
("Обычно: Present Simple", "A1", "time", "Для привычек используем Present Simple. I play, но she plays. Вопрос: Do you play? / Does she play? После does окончание -s у основного глагола исчезает. Сначала отработай утверждение, затем вопрос и отрицание.", ["I walk to school every day. — Я хожу в школу каждый день.", "She reads after lunch. — Она читает после обеда."], [
("Я играю каждый день", "I play every day.", "I plays every day.", "I am play every day.", "I play every day."),
("Она читает после обеда", "She reads after lunch.", "She read after lunch.", "She reading after lunch.", "She reads after lunch."),
("Спроси о его привычке", "Does he walk to school?", "Does he walks to school?", "Is he walk to school?", "Does he walk to school?"),
("Я не играю ночью", "I do not play at night.", "I does not play at night.", "I am not play at night.", "I do not play at night."),
("Какое слово указывает на привычку?", "every day", "right now", "yesterday", "I read every day.")]),
("Сейчас: Present Continuous", "A1", "time", "Для действия в процессе сейчас: am/is/are + глагол с -ing. I am reading. Сравни привычку I read every day и процесс I am reading now. Одного слова now недостаточно: проверяй смысл действия.", ["I am drawing now. — Я сейчас рисую.", "They are playing outside. — Они играют на улице."], [
("Я сейчас читаю", "I am reading now.", "I reading now.", "I am read now.", "I am reading now."),
("Она сейчас рисует", "She is drawing now.", "She are drawing now.", "She drawing now.", "She is drawing now."),
("Они сейчас играют", "They are playing now.", "They is playing now.", "They am playing now.", "They are playing now."),
("Спроси о действии сейчас", "Are you reading?", "Do you reading?", "You are read?", "Are you reading?"),
("Выбери привычку, а не действие сейчас", "I read every evening.", "I am reading now.", "She is reading now.", "I read every evening.")]),
("Вчера и завтра", "A1", "time", "Сначала сравниваем время на понятной линии: вчера — сейчас — завтра. Для начала: was/were — был/были; played — играл; will + глагол — простое сообщение о будущем. Все сложные времена остаются в следующих уровнях, а не в первом маршруте.", ["I was at home yesterday. — Вчера я был дома.", "I will help you tomorrow. — Завтра я тебе помогу."], [
("Вчера я был дома", "I was at home yesterday.", "I am at home tomorrow.", "I were at home yesterday.", "I was at home yesterday."),
("Вчера мы были в парке", "We were in the park yesterday.", "We was in the park yesterday.", "We are in the park tomorrow.", "We were in the park yesterday."),
("Вчера я играл", "I played yesterday.", "I play tomorrow.", "I playing yesterday.", "I played yesterday."),
("Завтра я помогу", "I will help tomorrow.", "I will helps tomorrow.", "I helped tomorrow.", "I will help tomorrow."),
("Какое слово означает «вчера»?", "yesterday", "tomorrow", "today", "I was here yesterday.")]),
]

def build():
    result=[]
    for n,(title,level,group,rule,examples,questions) in enumerate(UNITS):
        qs=[]
        for i,(prompt,right,wrong1,wrong2,speech) in enumerate(questions):
            choices=[right,wrong1,wrong2]; shift=(n+i)%3; choices=choices[shift:]+choices[:shift]
            qs.append(dict(prompt=prompt,choices=choices,answer=choices.index(right),speech=speech,explanation=f'Верный вариант: {right}. '+rule))
        result.append(dict(id=f'start-{n+1:02d}',title=title,level=level,group=group,rule=rule,examples=examples,questions=qs))
    return result

if __name__=='__main__':
    path=Path(__file__).resolve().parents[1]/'app/src/main/assets/beginner.json'
    path.write_text(json.dumps(build(),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(f'{len(UNITS)} original beginner topics / {len(UNITS)*5} exercises')
