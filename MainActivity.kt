package com.example.naturemath

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.random.Random

val LightGreenBg = Color(0xFFF1F8E9)
val LeafGreen = Color(0xFF4CAF50)
val DarkForestGreen = Color(0xFF2E7D32)
val OrangeFruit = Color(0xFFFF9800)
val AppleRed = Color(0xFFE53935)
val GoldStar = Color(0xFFFFC107)

data class MathLevel(
    val id: Int,
    val title: String,
    val subtitle: String,
    val fruitIcon: String
)

data class Question(
    val visualPrompt: String,
    val textPrompt: String,
    val options: List<Int>,
    val correctAnswer: Int
)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ar")
            }
        }
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    NatureMathApp(speakArabic = { phrase ->
                        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "feedback")
                    })
                }
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

class ProgressManager(context: Context) {
    private val prefs = context.getSharedPreferences("nature_math_prefs", Context.MODE_PRIVATE)

    fun getUnlockedLevel(): Int = prefs.getInt("unlocked_level", 1)
    fun unlockNextLevel(currentLevel: Int) {
        if (currentLevel >= getUnlockedLevel()) {
            prefs.edit().putInt("unlocked_level", currentLevel + 1).apply()
        }
    }

    fun getStars(levelId: Int): Int = prefs.getInt("stars_level_$levelId", 0)
    fun saveStars(levelId: Int, stars: Int) {
        val oldStars = getStars(levelId)
        if (stars > oldStars) {
            prefs.edit().putInt("stars_level_$levelId", stars).apply()
        }
    }

    fun getTotalPoints(): Int = prefs.getInt("total_points", 0)
    fun addPoints(points: Int) {
        prefs.edit().putInt("total_points", getTotalPoints() + points).apply()
    }
}

@Composable
fun NatureMathApp(speakArabic: (String) -> Unit) {
    val context = LocalContext.current
    val progressManager = remember { ProgressManager(context) }

    var currentScreen by remember { mutableStateOf("home") }
    var selectedLevel by remember { mutableStateOf<MathLevel?>(null) }
    var unlockedLevel by remember { mutableIntStateOf(progressManager.getUnlockedLevel()) }
    var totalPoints by remember { mutableIntStateOf(progressManager.getTotalPoints()) }
    var earnedCertificateLevel by remember { mutableStateOf<MathLevel?>(null) }

    val levels = listOf(
        MathLevel(1, "بستان الأعداد", "تعلم العدّ من 1 إلى 10", "🍎"),
        MathLevel(2, "مزرعة الجمع", "جمع الفواكه والأشجار", "🍊"),
        MathLevel(3, "حديقة الطرح", "الطرح البسيط للأعداد", "🍓"),
        MathLevel(4, "واحة الأعداد الكبيرة", "الجمع والطرح حتى 20", "🍌"),
        MathLevel(5, "حقل الضرب", "مقدمة في الضرب والتكرار", "🍇"),
        MathLevel(6, "نهر القسمة", "التوزيع بالتساوي", "🍉")
    )

    Surface(modifier = Modifier.fillMaxSize(), color = LightGreenBg) {
        when (currentScreen) {
            "home" -> HomeScreen(
                levels = levels,
                unlockedLevel = unlockedLevel,
                totalPoints = totalPoints,
                progressManager = progressManager,
                onSelectLevel = { level ->
                    selectedLevel = level
                    currentScreen = "quiz"
                }
            )
            "quiz" -> selectedLevel?.let { level ->
                QuizScreen(
                    level = level,
                    speakArabic = speakArabic,
                    onBack = { currentScreen = "home" },
                    onLevelCompleted = { stars, pointsEarned ->
                        progressManager.addPoints(pointsEarned)
                        progressManager.saveStars(level.id, stars)
                        if (stars >= 2) {
                            progressManager.unlockNextLevel(level.id)
                            earnedCertificateLevel = level
                            currentScreen = "certificate"
                        } else {
                            currentScreen = "home"
                        }
                        unlockedLevel = progressManager.getUnlockedLevel()
                        totalPoints = progressManager.getTotalPoints()
                    }
                )
            }
            "certificate" -> earnedCertificateLevel?.let { level ->
                CertificateScreen(
                    level = level,
                    onContinue = { currentScreen = "home" }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    levels: List<MathLevel>,
    unlockedLevel: Int,
    totalPoints: Int,
    progressManager: ProgressManager,
    onSelectLevel: (MathLevel) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LeafGreen)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🌳 حديقة الحساب",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "⭐ $totalPoints نقطة",
                    color = DarkForestGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(levels) { level ->
                val isUnlocked = level.id <= unlockedLevel
                val stars = progressManager.getStars(level.id)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isUnlocked) { onSelectLevel(level) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) Color.White else Color(0xFFE0E0E0)
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = level.fruitIcon, fontSize = 40.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "المستوى ${level.id}: ${level.title}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) DarkForestGreen else Color.Gray
                            )
                            Text(
                                text = level.subtitle,
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                            if (isUnlocked) {
                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    repeat(3) { index ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (index < stars) GoldStar else Color.LightGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (!isUnlocked) {
                            Icon(Icons.Default.Lock, contentDescription = "مغلق", tint = Color.Gray)
                        } else {
                            Button(
                                onClick = { onSelectLevel(level) },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeFruit),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (stars > 0) "إعادة" else "ابدأ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizScreen(
    level: MathLevel,
    speakArabic: (String) -> Unit,
    onBack: () -> Unit,
    onLevelCompleted: (stars: Int, points: Int) -> Unit
) {
    val context = LocalContext.current
    val totalQuestions = 5
    var currentQuestionIndex by remember { mutableIntStateOf(1) }
    var correctAnswersCount by remember { mutableIntStateOf(0) }
    var question by remember { mutableStateOf(generateQuestionForLevel(level.id)) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isCorrectFeedback by remember { mutableStateOf(false) }

    fun playFeedback(isCorrect: Boolean) {
        val resId = if (isCorrect) R.raw.applause_excellent else R.raw.try_again
        val mp = MediaPlayer.create(context, resId)
        mp.setOnCompletionListener { it.release() }
        mp.start()
        speakArabic(if (isCorrect) "أحسنت، ممتاز!" else "غير صحيح، حاول مرة أخرى")
    }

    LaunchedEffect(feedbackMessage, isCorrectFeedback) {
        if (feedbackMessage != null && isCorrectFeedback) {
            delay(2000)
            feedbackMessage = null
            if (currentQuestionIndex < totalQuestions) {
                currentQuestionIndex++
                question = generateQuestionForLevel(level.id)
            } else {
                val stars = when {
                    correctAnswersCount == totalQuestions -> 3
                    correctAnswersCount >= 4 -> 2
                    else -> 1
                }
                onLevelCompleted(stars, correctAnswersCount * 10)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع", tint = DarkForestGreen)
            }
            Text(
                text = "السؤال $currentQuestionIndex من $totalQuestions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkForestGreen
            )
            Text(text = "⭐ ${correctAnswersCount * 10}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        LinearProgressIndicator(
            progress = { currentQuestionIndex.toFloat() / totalQuestions },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
            color = LeafGreen,
            trackColor = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (question.visualPrompt.isNotEmpty()) {
                    Text(
                        text = question.visualPrompt,
                        fontSize = 36.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 46.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = question.textPrompt,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkForestGreen,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = feedbackMessage != null,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isCorrectFeedback) LeafGreen else AppleRed)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = feedbackMessage ?: "",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            question.options.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowOptions.forEach { option ->
                        Button(
                            onClick = {
                                if (option == question.correctAnswer) {
                                    if (!isCorrectFeedback) {
                                        correctAnswersCount++
                                    }
                                    isCorrectFeedback = true
                                    feedbackMessage = "👏 أحسنت، ممتاز!"
                                    playFeedback(true)
                                } else {
                                    isCorrectFeedback = false
                                    feedbackMessage = "غير صحيح، حاول مرة أخرى"
                                    playFeedback(false)
                                }
                            },
                            modifier = Modifier.weight(1f).height(70.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeFruit)
                        ) {
                            Text(
                                text = option.toString(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CertificateScreen(level: MathLevel, onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(4.dp, GoldStar, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🌻🌳🏆🌳🌻", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "شهادة تفوق وإنجاز",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkForestGreen
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "تهانينا يا بطل! لقد أتقنت بنجاح:\n«${level.title}»",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("⭐⭐⭐", fontSize = 32.sp)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("الانتقال للمستوى التالي 🌳", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun generateQuestionForLevel(levelId: Int): Question {
    val natureIcons = listOf("🍎", "🍊", "🍓", "🍌", "🌳", "🌻", "🍉", "🍇")
    val icon = natureIcons.random()

    return when (levelId) {
        1 -> {
            val count = Random.nextInt(1, 10)
            val visual = List(count) { icon }.joinToString(" ")
            Question(
                visualPrompt = visual,
                textPrompt = "كم عدد العناصر في الصورة؟",
                options = generateOptions(count, 1, 10),
                correctAnswer = count
            )
        }
        2 -> {
            val a = Random.nextInt(1, 6)
            val b = Random.nextInt(1, 6)
            val ans = a + b
            val visual = "${List(a) { icon }.joinToString("")}  +  ${List(b) { icon }.joinToString("")}"
            Question(
                visualPrompt = visual,
                textPrompt = "$a + $b = ؟",
                options = generateOptions(ans, 2, 12),
                correctAnswer = ans
            )
        }
        3 -> {
            val a = Random.nextInt(4, 10)
            val b = Random.nextInt(1, a)
            val ans = a - b
            val visual = List(a) { icon }.joinToString(" ")
            Question(
                visualPrompt = visual,
                textPrompt = "لديك $a ، أخذنا منها $b ، كم بقي؟\n($a - $b = ؟)",
                options = generateOptions(ans, 1, 9),
                correctAnswer = ans
            )
        }
        4 -> {
            val isAdd = Random.nextBoolean()
            val a = Random.nextInt(8, 15)
            val b = Random.nextInt(2, 7)
            val ans = if (isAdd) a + b else a - b
            val op = if (isAdd) "+" else "-"
            Question(
                visualPrompt = "🌳🍎🍊🌳",
                textPrompt = "$a $op $b = ؟",
                options = generateOptions(ans, 2, 22),
                correctAnswer = ans
            )
        }
        5 -> {
            val a = Random.nextInt(2, 6)
            val b = Random.nextInt(2, 6)
            val ans = a * b
            Question(
                visualPrompt = "🧺 $a سلال في كل سلة $b $icon",
                textPrompt = "$a × $b = ؟",
                options = generateOptions(ans, 4, 30),
                correctAnswer = ans
            )
        }
        else -> {
            val divisor = Random.nextInt(2, 5)
            val ans = Random.nextInt(2, 6)
            val total = divisor * ans
            Question(
                visualPrompt = "وزع $total $icon على $divisor أطباق بالتساوي",
                textPrompt = "$total ÷ $divisor = ؟",
                options = generateOptions(ans, 1, 10),
                correctAnswer = ans
            )
        }
    }
}

fun generateOptions(correct: Int, min: Int, max: Int): List<Int> {
    val options = mutableSetOf(correct)
    while (options.size < 4) {
        val wrong = Random.nextInt(min, max + 2)
        if (wrong != correct) options.add(wrong)
    }
    return options.shuffled()
}
