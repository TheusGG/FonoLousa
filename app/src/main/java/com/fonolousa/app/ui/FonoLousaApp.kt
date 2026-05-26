package com.fonolousa.app.ui

import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fonolousa.app.audio.AudioPlayer
import com.fonolousa.app.BuildConfig
import com.fonolousa.app.data.Categoria
import com.fonolousa.app.data.DataRepository
import com.fonolousa.app.data.ItemFono
import com.fonolousa.app.data.Nivel
import com.fonolousa.app.data.SessionRepository
import com.fonolousa.app.data.local.ClinicalResultEntity
import com.fonolousa.app.data.local.ItemProgressEntity
import com.fonolousa.app.data.local.SessionEventEntity
import com.fonolousa.app.ui.theme.ChalkGreen
import com.fonolousa.app.ui.theme.ChalkGreenAlt
import com.fonolousa.app.ui.theme.ChalkShadow
import com.fonolousa.app.ui.theme.ChalkWhite
import com.fonolousa.app.update.UpdateChecker
import com.fonolousa.app.update.UpdateState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FonoLousaApp(
    repository: DataRepository,
    sessionRepository: SessionRepository,
    audioPlayer: AudioPlayer
) {
    val navController = rememberNavController()
    val progress by sessionRepository.progress.collectAsState(initial = emptyList())
    val favorites by sessionRepository.favorites.collectAsState(initial = emptyList())
    val recentEvents by sessionRepository.recentEvents.collectAsState(initial = emptyList())
    val clinicalResults by sessionRepository.clinicalResults.collectAsState(initial = emptyList())

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                categorias = repository.categorias(),
                onCategoryClick = { navController.navigate("levels/${it.id}") },
                onUpdateClick = { navController.navigate("update") },
                onReportClick = { navController.navigate("report") },
                onClinicalClick = { navController.navigate("clinical") }
            )
        }
        composable("update") {
            UpdateScreen(onBack = { navController.popBackStack() })
        }
        composable("report") {
            SessionReportScreen(
                progress = progress,
                favorites = favorites,
                events = recentEvents,
                clinicalResults = clinicalResults,
                onBack = { navController.popBackStack() }
            )
        }
        composable("clinical") {
            ClinicalAssessmentScreen(
                repository = repository,
                sessionRepository = sessionRepository,
                audioPlayer = audioPlayer,
                categorias = repository.categorias(),
                results = clinicalResults,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "levels/{categoryId}",
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { entry ->
            val category = repository.categoria(entry.arguments?.getString("categoryId").orEmpty())
            LevelScreen(
                categoria = category,
                onBack = { navController.popBackStack() },
                onLevelClick = { navController.navigate("items/${category.id}/${it.nivel}") }
            )
        }
        composable(
            route = "items/{categoryId}/{level}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("level") { type = NavType.IntType }
            )
        ) { entry ->
            val categoryId = entry.arguments?.getString("categoryId").orEmpty()
            val level = entry.arguments?.getInt("level") ?: 1
            val category = repository.categoria(categoryId)
            val nivel = repository.nivel(categoryId, level)
            ItemsGridScreen(
                repository = repository,
                sessionRepository = sessionRepository,
                audioPlayer = audioPlayer,
                categoria = category,
                nivel = nivel,
                progress = progress,
                onBack = { navController.popBackStack() },
                onItemClick = { index -> navController.navigate("viewer/$categoryId/$level/$index") }
            )
        }
        composable(
            route = "viewer/{categoryId}/{level}/{index}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("level") { type = NavType.IntType },
                navArgument("index") { type = NavType.IntType }
            )
        ) { entry ->
            val categoryId = entry.arguments?.getString("categoryId").orEmpty()
            val level = entry.arguments?.getInt("level") ?: 1
            val index = entry.arguments?.getInt("index") ?: 0
            val category = repository.categoria(categoryId)
            val nivel = repository.nivel(categoryId, level)
            ItemViewerScreen(
                repository = repository,
                sessionRepository = sessionRepository,
                audioPlayer = audioPlayer,
                categoria = category,
                nivel = nivel,
                initialIndex = index,
                progress = progress,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    categorias: List<Categoria>,
    onCategoryClick: (Categoria) -> Unit,
    onUpdateClick: () -> Unit,
    onReportClick: () -> Unit,
    onClinicalClick: () -> Unit
) {
    BlackboardScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onClinicalClick,
                    modifier = Modifier
                        .size(58.dp)
                        .border(2.dp, ChalkWhite.copy(alpha = 0.78f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Assessment,
                        contentDescription = "Avaliacao clinica",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                IconButton(
                    onClick = onReportClick,
                    modifier = Modifier
                        .size(58.dp)
                        .border(2.dp, ChalkWhite.copy(alpha = 0.78f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Assessment,
                        contentDescription = "Relatorio da sessao",
                        tint = ChalkWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                IconButton(
                    onClick = onUpdateClick,
                    modifier = Modifier
                        .size(58.dp)
                        .border(2.dp, ChalkWhite.copy(alpha = 0.78f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = "Atualizar app",
                        tint = ChalkWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            ChalkText(
                text = "FonoLousa",
                fontSize = 52,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            ChalkText(
                text = "Versao ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                fontSize = 18,
                color = ChalkWhite.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )
            ChalkText(
                text = "Toque em uma categoria",
                fontSize = 24,
                modifier = Modifier.padding(top = 6.dp, bottom = 22.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categorias, key = { it.id }) { categoria ->
                    CategoryButton(categoria = categoria, onClick = { onCategoryClick(categoria) })
                }
            }
        }
    }
}

@Composable
private fun UpdateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val checker = remember { UpdateChecker() }
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }

    BlackboardScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            TopBar(title = "Atualizar app", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, ChalkWhite.copy(alpha = 0.78f), RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ChalkText(
                    text = "Canal de atualizacao",
                    fontSize = 30,
                    fontWeight = FontWeight.Black
                )
                ChalkText(
                    text = updateMessage(state),
                    fontSize = 22,
                    color = ChalkWhite.copy(alpha = 0.9f)
                )
                Button(
                    onClick = {
                        scope.launch {
                            state = UpdateState.Checking
                            state = checker.check()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Text(
                        text = "Verificar atualizacao",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                val available = state as? UpdateState.Available
                if (available != null) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(available.manifest.apkUrl))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107),
                            contentColor = Color(0xFF1A1A1A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Icon(Icons.Filled.SystemUpdate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Abrir pagina de download",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionReportScreen(
    progress: List<ItemProgressEntity>,
    favorites: List<ItemProgressEntity>,
    events: List<SessionEventEntity>,
    clinicalResults: List<ClinicalResultEntity>,
    onBack: () -> Unit
) {
    val totalViews = progress.sumOf { it.views }
    val totalPlays = progress.sumOf { it.plays }
    val practiced = progress.count { it.views > 0 || it.plays > 0 }
    val formatter = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }

    BlackboardScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            TopBar(title = "Relatorio da sessao", onBack = onBack)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ReportMetric("Itens", practiced.toString(), Modifier.weight(1f))
                ReportMetric("Toques", totalViews.toString(), Modifier.weight(1f))
                ReportMetric("Sons", totalPlays.toString(), Modifier.weight(1f))
                ReportMetric("Favoritos", favorites.size.toString(), Modifier.weight(1f))
            }
            ClinicalChartSection(
                results = clinicalResults,
                modifier = Modifier.padding(top = 22.dp)
            )
            ChalkText(
                text = "Ultimas interacoes",
                fontSize = 28,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (events.isEmpty()) {
                    ChalkText(
                        text = "Nenhuma interacao registrada ainda.",
                        fontSize = 22,
                        color = ChalkWhite.copy(alpha = 0.82f)
                    )
                } else {
                    events.take(40).forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .border(1.dp, ChalkWhite.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                ChalkText(
                                    text = event.word.replaceFirstChar { it.uppercase() },
                                    fontSize = 21,
                                    maxLines = 1
                                )
                                ChalkText(
                                    text = "${event.categoryId} - nivel ${event.level}",
                                    fontSize = 16,
                                    color = ChalkWhite.copy(alpha = 0.74f),
                                    maxLines = 1
                                )
                            }
                            ChalkText(
                                text = "${event.eventType.label()}  ${formatter.format(Date(event.createdAt))}",
                                fontSize = 16,
                                textAlign = TextAlign.End,
                                color = ChalkWhite.copy(alpha = 0.84f),
                                modifier = Modifier.width(138.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, ChalkWhite.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.11f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ChalkText(text = value, fontSize = 28, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        ChalkText(
            text = label,
            fontSize = 16,
            color = ChalkWhite.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ClinicalAssessmentScreen(
    repository: DataRepository,
    sessionRepository: SessionRepository,
    audioPlayer: AudioPlayer,
    categorias: List<Categoria>,
    results: List<ClinicalResultEntity>,
    onBack: () -> Unit
) {
    val activities = listOf("nomeacao", "repeticao", "discriminacao")
    var activity by remember { mutableStateOf(activities.first()) }
    var categoryIndex by remember { mutableStateOf(0) }
    var levelIndex by remember { mutableStateOf(0) }
    var itemIndex by remember { mutableStateOf(0) }
    var showFeedback by remember { mutableStateOf<String?>(null) }
    var pulse by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val categoria = categorias[categoryIndex.coerceIn(0, categorias.lastIndex)]
    val nivel = categoria.niveis[levelIndex.coerceIn(0, categoria.niveis.lastIndex)]
    val clinicalItems = remember(categoria.id, nivel.nivel) {
        nivel.itens.distinctBy { it.palavra.trim().lowercase(Locale("pt", "BR")) }
    }
    val item = clinicalItems[itemIndex.coerceIn(0, clinicalItems.lastIndex)]
    val comparison = clinicalItems.getOrNull((itemIndex + 1) % clinicalItems.size) ?: item
    val imageScale by animateFloatAsState(
        targetValue = if (pulse) 1.03f else 1f,
        animationSpec = tween(160),
        finishedListener = { pulse = false },
        label = "clinicalPulse"
    )

    fun nextItem() {
        itemIndex = if (itemIndex >= clinicalItems.lastIndex) 0 else itemIndex + 1
    }

    LaunchedEffect(activity, item.id) {
        if (activity == "repeticao") {
            audioPlayer.play(item.arquivoSom, item.audioText(nivel.nivel))
            pulse = true
        }
    }

    BlackboardScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp)
        ) {
            TopBar(title = "Avaliacao clinica", onBack = onBack)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                activities.forEach { option ->
                    Button(
                        onClick = {
                            activity = option
                            showFeedback = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activity == option) Color(0xFFFFC107) else Color.White.copy(alpha = 0.16f),
                            contentColor = if (activity == option) Color(0xFF1A1A1A) else ChalkWhite
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                    ) {
                        Text(activity.label(), fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NavButton(
                    text = categoria.nome,
                    enabled = categorias.size > 1,
                    iconLeft = true,
                    onClick = {
                        categoryIndex = if (categoryIndex == 0) categorias.lastIndex else categoryIndex - 1
                        levelIndex = 0
                        itemIndex = 0
                    },
                    modifier = Modifier.weight(1f)
                )
                NavButton(
                    text = "Nivel ${nivel.nivel}",
                    enabled = categoria.niveis.size > 1,
                    iconLeft = false,
                    onClick = {
                        levelIndex = if (levelIndex >= categoria.niveis.lastIndex) 0 else levelIndex + 1
                        itemIndex = 0
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ChalkText(
                    text = activity.prompt(),
                    fontSize = 22,
                    textAlign = TextAlign.Center,
                    color = ChalkWhite.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                )
                AssetImage(
                    repository = repository,
                    path = item.arquivoImagem,
                    contentDescription = item.displayText(nivel.nivel),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(260.dp)
                        .graphicsLayer(scaleX = imageScale, scaleY = imageScale)
                        .clip(RoundedCornerShape(8.dp))
                        .border(4.dp, parseColor(categoria.cor), RoundedCornerShape(8.dp))
                        .background(Color.White)
                )
                ChalkText(
                    text = if (activity == "nomeacao") "Figura ${itemIndex + 1}/${clinicalItems.size}" else item.displayText(nivel.nivel),
                    fontSize = 28,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            audioPlayer.play(item.arquivoSom, item.audioText(nivel.nivel))
                            pulse = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(58.dp)
                    ) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Som", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    if (activity == "discriminacao") {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                audioPlayer.play(item.arquivoSom, item.audioText(nivel.nivel))
                                audioPlayer.play(comparison.arquivoSom, comparison.audioText(nivel.nivel))
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(58.dp)
                        ) {
                            Text("Tocar par", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (showFeedback != null) {
                    ChalkText(
                        text = showFeedback.orEmpty(),
                        fontSize = 22,
                        color = Color(0xFFFFC107),
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            sessionRepository.recordClinicalResult(activity, categoria.id, nivel.nivel, item, false)
                        }
                        showFeedback = "Erro registrado"
                        nextItem()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(62.dp)
                ) {
                    Text("Erro", fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            sessionRepository.recordClinicalResult(activity, categoria.id, nivel.nivel, item, true)
                        }
                        showFeedback = "Acerto registrado"
                        audioPlayer.playVictory()
                        nextItem()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(62.dp)
                ) {
                    Text("Acerto", fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
            ClinicalChartSection(
                results = results,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun ClinicalChartSection(
    results: List<ClinicalResultEntity>,
    modifier: Modifier = Modifier
) {
    val summaries = listOf("nomeacao", "repeticao", "discriminacao").map { activity ->
        val items = results.filter { it.activity == activity }
        val percent = if (items.isEmpty()) 0 else (items.count { it.isCorrect } * 100f / items.size).roundToInt()
        ClinicalBar(activity.label(), percent, items.size)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(154.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, ChalkWhite.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(12.dp)
    ) {
        ChalkText(
            text = "Desempenho clinico",
            fontSize = 20,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            summaries.forEach { summary ->
                ClinicalBarView(summary = summary, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ClinicalBarView(summary: ClinicalBar, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        ChalkText(
            text = "${summary.percent}%",
            fontSize = 18,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(((summary.percent.coerceIn(0, 100) / 100f) * 54).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFC107))
            )
        }
        ChalkText(
            text = "${summary.label} (${summary.total})",
            fontSize = 13,
            color = ChalkWhite.copy(alpha = 0.82f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private data class ClinicalBar(
    val label: String,
    val percent: Int,
    val total: Int
)

@Composable
private fun CategoryButton(categoria: Categoria, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(90),
        label = "categoryScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, ChalkWhite.copy(alpha = 0.78f), RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = if (pressed) 0.18f else 0.10f))
            .clickable(interactionSource = interaction, indication = null) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(parseColor(categoria.cor)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = categoria.nome.take(1),
                color = ChalkWhite,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.height(10.dp))
        ChalkText(
            text = categoria.nome,
            fontSize = 22,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LevelScreen(
    categoria: Categoria,
    onBack: () -> Unit,
    onLevelClick: (Nivel) -> Unit
) {
    BlackboardScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            TopBar(title = categoria.nome, onBack = onBack)
            ChalkText(
                text = "Escolha o n\u00edvel",
                fontSize = 34,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                categoria.niveis.forEach { nivel ->
                    LevelCard(
                        nivel = nivel,
                        color = parseColor(categoria.cor),
                        onClick = { onLevelClick(nivel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCard(nivel: Nivel, color: Color, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val badge = when (nivel.nivel) {
        1 -> "\u2605"
        2 -> "\u2605\u2605"
        3 -> "\u2605\u2605\u2605"
        else -> "\uD83C\uDFC6"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(3.dp, color, RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.11f))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.Center
    ) {
        ChalkText(
            text = "$badge  N\u00edvel ${nivel.nivel}",
            fontSize = 29,
            fontWeight = FontWeight.Black
        )
        ChalkText(
            text = nivel.descricao,
            fontSize = 23,
            color = ChalkWhite.copy(alpha = 0.88f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ItemsGridScreen(
    repository: DataRepository,
    sessionRepository: SessionRepository,
    audioPlayer: AudioPlayer,
    categoria: Categoria,
    nivel: Nivel,
    progress: List<ItemProgressEntity>,
    onBack: () -> Unit,
    onItemClick: (Int) -> Unit
) {
    BlackboardScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp)
        ) {
            TopBar(title = "${categoria.nome} - N\u00edvel ${nivel.nivel}", onBack = onBack)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(top = 18.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(nivel.itens, key = { _, item -> item.id }) { index, item ->
                    ItemCell(
                        repository = repository,
                        sessionRepository = sessionRepository,
                        audioPlayer = audioPlayer,
                        item = item,
                        categoria = categoria,
                        level = nivel.nivel,
                        progress = progress.firstOrNull {
                            it.itemKey == SessionRepository.itemKey(categoria.id, nivel.nivel, item.id)
                        },
                        borderColor = parseColor(categoria.cor),
                        onClick = { onItemClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemCell(
    repository: DataRepository,
    sessionRepository: SessionRepository,
    audioPlayer: AudioPlayer,
    item: ItemFono,
    categoria: Categoria,
    level: Int,
    progress: ItemProgressEntity?,
    borderColor: Color,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(214.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(3.dp, borderColor, RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, borderColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .background(Color.White)
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch { sessionRepository.recordView(categoria.id, level, item) }
                    onClick()
                }
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            AssetImage(
                repository = repository,
                path = item.arquivoImagem,
                contentDescription = item.displayText(level),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = item.displayText(level).uppercase(),
            color = Color(0xFF2A2A2A),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch { sessionRepository.recordView(categoria.id, level, item) }
                    onClick()
                }
                .padding(top = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    audioPlayer.play(item.arquivoSom, item.audioText(level))
                    scope.launch { sessionRepository.recordPlay(categoria.id, level, item) }
                },
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(borderColor)
            ) {
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = "Tocar som",
                    tint = ChalkWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        AnimatedVisibility(visible = progress?.isFavorite == true || (progress?.plays ?: 0) > 0) {
            Text(
                text = "${if (progress?.isFavorite == true) "Favorito" else ""}${if ((progress?.plays ?: 0) > 0) "  ${progress?.plays}x" else ""}".trim(),
                color = borderColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ItemViewerScreen(
    repository: DataRepository,
    sessionRepository: SessionRepository,
    audioPlayer: AudioPlayer,
    categoria: Categoria,
    nivel: Nivel,
    initialIndex: Int,
    progress: List<ItemProgressEntity>,
    onBack: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, nivel.itens.lastIndex)) }
    var pulse by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val item = nivel.itens[currentIndex]
    val currentProgress = progress.firstOrNull {
        it.itemKey == SessionRepository.itemKey(categoria.id, nivel.nivel, item.id)
    }
    val imageScale by animateFloatAsState(
        targetValue = if (pulse) 1.03f else 1f,
        animationSpec = tween(160),
        finishedListener = { pulse = false },
        label = "soundPulse"
    )

    LaunchedEffect(item.id) {
        audioPlayer.play(item.arquivoSom, item.audioText(nivel.nivel))
        sessionRepository.recordView(categoria.id, nivel.nivel, item)
        sessionRepository.recordPlay(categoria.id, nivel.nivel, item)
        pulse = true
        showConfetti = currentIndex == nivel.itens.lastIndex
        if (showConfetti) audioPlayer.playVictory()
    }

    BlackboardScreen {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
            ) {
                TopBar(title = "${categoria.nome} - N\u00edvel ${nivel.nivel}", onBack = onBack)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AssetImage(
                        repository = repository,
                        path = item.arquivoImagem,
                        contentDescription = item.displayText(nivel.nivel),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .aspectRatio(1f)
                            .graphicsLayer(scaleX = imageScale, scaleY = imageScale)
                            .clip(RoundedCornerShape(8.dp))
                            .border(4.dp, ChalkWhite, RoundedCornerShape(8.dp))
                            .background(Color.White)
                    )
                    ChalkText(
                        text = item.displayText(nivel.nivel),
                        fontSize = 46,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 22.dp)
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                audioPlayer.play(item.arquivoSom, item.audioText(nivel.nivel))
                                scope.launch { sessionRepository.recordPlay(categoria.id, nivel.nivel, item) }
                                pulse = true
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .border(2.dp, ChalkWhite, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VolumeUp,
                                contentDescription = "Repetir som",
                                tint = ChalkWhite,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    sessionRepository.setFavorite(
                                        categoria.id,
                                        nivel.nivel,
                                        item,
                                        currentProgress?.isFavorite != true
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .border(2.dp, ChalkWhite, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (currentProgress?.isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (currentProgress?.isFavorite == true) Color(0xFFFFC107) else ChalkWhite,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavButton(
                        text = "Anterior",
                        enabled = currentIndex > 0,
                        iconLeft = true,
                        onClick = { currentIndex = (currentIndex - 1).coerceAtLeast(0) },
                        modifier = Modifier.weight(1f)
                    )
                    ChalkText(
                        text = "${currentIndex + 1}/${nivel.itens.size}",
                        fontSize = 22,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(90.dp)
                    )
                    NavButton(
                        text = "Pr\u00f3ximo",
                        enabled = currentIndex < nivel.itens.lastIndex,
                        iconLeft = false,
                        onClick = { currentIndex = (currentIndex + 1).coerceAtMost(nivel.itens.lastIndex) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (showConfetti) ConfettiBurst()
        }
    }
}

@Composable
private fun NavButton(
    text: String,
    enabled: Boolean,
    iconLeft: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.16f),
            contentColor = ChalkWhite,
            disabledContainerColor = Color.White.copy(alpha = 0.07f),
            disabledContentColor = ChalkWhite.copy(alpha = 0.44f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(64.dp)
    ) {
        if (iconLeft) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = null)
            Spacer(Modifier.width(4.dp))
        }
        Text(text = text, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (!iconLeft) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(58.dp)
                .border(2.dp, ChalkWhite.copy(alpha = 0.78f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = ChalkWhite,
                modifier = Modifier.size(34.dp)
            )
        }
        ChalkText(
            text = title,
            fontSize = 30,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 18.dp)
        )
    }
}

@Composable
private fun ConfettiBurst() {
    val transition = rememberInfiniteTransition(label = "confetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiProgress"
    )
    val colors = listOf(
        Color(0xFFFFC107),
        Color(0xFFFF5C8A),
        Color(0xFF64DD17),
        Color(0xFF40C4FF),
        Color(0xFFFFAB40)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val count = 34
        repeat(count) { index ->
            val lane = (index * 37 % 100) / 100f
            val delay = (index % 7) / 7f
            val t = ((progress + delay) % 1f)
            val x = size.width * lane + kotlin.math.sin((t * 6.28f) + index) * 42f
            val y = size.height * t
            val side = 10f + (index % 4) * 4f
            drawRect(
                color = colors[index % colors.size],
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(side, side * 0.58f),
                alpha = (1f - t * 0.35f).coerceIn(0.35f, 1f)
            )
        }
    }
}

@Composable
private fun BlackboardScreen(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChalkGreen)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = Color.White.copy(alpha = 0.035f)
            val dotColor = ChalkGreenAlt.copy(alpha = 0.12f)
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y + 40f),
                    strokeWidth = 2f
                )
                y += 76f
            }
            for (i in 0..18) {
                val x = ((i * 97) % size.width.toInt()).toFloat()
                val cy = ((i * 151) % size.height.toInt()).toFloat()
                drawCircle(color = dotColor, radius = 38f, center = Offset(x, cy))
            }
        }
        content()
    }
}

@Composable
private fun ChalkText(
    text: String,
    fontSize: Int,
    modifier: Modifier = Modifier,
    color: Color = ChalkWhite,
    fontWeight: FontWeight = FontWeight.Bold,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            shadow = Shadow(
                color = ChalkShadow,
                offset = Offset(2f, 3f),
                blurRadius = 2f
            )
        ),
        modifier = modifier
    )
}

@Composable
private fun AssetImage(
    repository: DataRepository,
    path: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val bitmap: ImageBitmap? = remember(path) {
        try {
            context.assets.open(repository.assetPath(path)).use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contentDescription.take(2).uppercase(),
                color = ChalkGreen,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun parseColor(hex: String): Color {
    return Color(android.graphics.Color.parseColor(hex))
}

private fun updateMessage(state: UpdateState): String {
    return when (state) {
        UpdateState.Idle -> "Toque no botao para verificar se existe uma nova versao publicada."
        UpdateState.Checking -> "Verificando nova versao..."
        is UpdateState.Available -> "Canal oficial disponivel.\n${state.manifest.notes}"
        is UpdateState.UpToDate -> "Este tablet ja esta com a versao ${state.currentVersionName}."
        is UpdateState.NotConfigured -> state.message
        is UpdateState.Error -> "Falha ao verificar: ${state.message}"
    }
}

private fun ItemFono.displayText(level: Int): String {
    return if (level == 4) {
        frase.replaceFirstChar { it.uppercase() }
    } else {
        palavra
    }
}

private fun ItemFono.audioText(level: Int): String {
    return if (level == 4) frase else palavra
}

private fun String.label(): String {
    return when (this) {
        "view" -> "viu"
        "play" -> "som"
        "favorite" -> "favoritou"
        "unfavorite" -> "removeu"
        "clinical_correct" -> "acertou"
        "clinical_error" -> "errou"
        "nomeacao" -> "Nomeacao"
        "repeticao" -> "Repeticao"
        "discriminacao" -> "Discriminacao"
        else -> this
    }
}

private fun String.prompt(): String {
    return when (this) {
        "nomeacao" -> "Mostre a figura e marque a resposta da crianca."
        "repeticao" -> "Toque o som e marque se a crianca repetiu corretamente."
        "discriminacao" -> "Toque os sons e marque se a crianca discriminou corretamente."
        else -> "Marque o desempenho da crianca."
    }
}
