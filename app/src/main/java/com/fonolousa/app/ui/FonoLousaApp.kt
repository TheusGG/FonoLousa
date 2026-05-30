package com.fonolousa.app.ui

import android.graphics.BitmapFactory
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.unit.Dp
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
import com.fonolousa.app.update.isTrustedUpdateUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val CLINICAL_TRIALS_PER_ACTIVITY = 10

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
                registeredChildren = clinicalResults.registeredChildNames(),
                onCategoryClick = { navController.navigate("items/${it.id}") },
                onUpdateClick = { navController.navigate("update") },
                onReportClick = { navController.navigate("report") },
                onClinicalClick = { childName -> navController.navigate("clinical/${Uri.encode(childName)}") }
            )
        }
        composable("update") {
            UpdateScreen(onBack = { navController.popBackStack() })
        }
        composable("report") {
            SessionReportScreen(
                sessionRepository = sessionRepository,
                progress = progress,
                favorites = favorites,
                events = recentEvents,
                clinicalResults = clinicalResults,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "clinical/{childName}",
            arguments = listOf(navArgument("childName") { type = NavType.StringType })
        ) { entry ->
            val childName = Uri.decode(entry.arguments?.getString("childName").orEmpty())
            ClinicalAssessmentScreen(
                childName = childName.ifBlank { "Criança" },
                repository = repository,
                sessionRepository = sessionRepository,
                audioPlayer = audioPlayer,
                categorias = repository.categorias(),
                onBack = { navController.popBackStack() },
                onReport = { navController.navigate("report") },
                onHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
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
            route = "items/{categoryId}",
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { entry ->
            val categoryId = entry.arguments?.getString("categoryId").orEmpty()
            val category = repository.categoria(categoryId)
            val itens = remember(categoryId) { repository.itensDaCategoria(categoryId) }
            val nivel = remember(categoryId, itens) {
                Nivel(nivel = 0, descricao = "Todos os estímulos", itens = itens)
            }
            ItemsGridScreen(
                repository = repository,
                sessionRepository = sessionRepository,
                audioPlayer = audioPlayer,
                categoria = category,
                nivel = nivel,
                progress = progress,
                onBack = { navController.popBackStack() },
                onItemClick = { index -> navController.navigate("viewer/$categoryId/0/$index") }
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
            val nivel = if (level <= 0) {
                Nivel(
                    nivel = 0,
                    descricao = "Todos os estímulos",
                    itens = repository.itensDaCategoria(categoryId)
                )
            } else {
                repository.nivel(categoryId, level)
            }
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
    registeredChildren: List<String>,
    onCategoryClick: (Categoria) -> Unit,
    onUpdateClick: () -> Unit,
    onReportClick: () -> Unit,
    onClinicalClick: (String) -> Unit
) {
    var childName by remember { mutableStateOf("") }
    val typedChildName = childName.cleanChildName()
    val existingChildrenByKey = remember(registeredChildren) {
        registeredChildren.associateBy { normalizeChildName(it) }
    }
    val duplicateChildName = typedChildName.isNotEmpty() && normalizeChildName(typedChildName) in existingChildrenByKey
    val canStartNewAssessment = typedChildName.isNotEmpty() && !duplicateChildName

    BlackboardScreen {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compactWidth = maxWidth < 720.dp
            val shortScreen = maxHeight < 620.dp
            val pagePadding = if (compactWidth) 18.dp else 32.dp
            val verticalPadding = if (compactWidth || shortScreen) 14.dp else 24.dp
            val topIconSize = if (compactWidth || shortScreen) 48.dp else 56.dp
            val topIconInnerSize = if (compactWidth || shortScreen) 26.dp else 30.dp
            val titleSize = when {
                compactWidth -> 40
                shortScreen -> 44
                else -> 52
            }
            val versionSize = if (compactWidth || shortScreen) 16 else 18
            val headingSize = if (compactWidth) 22 else 24
            val gridColumns = if (compactWidth) 1 else 2
            val gridSpacing = if (compactWidth) 12.dp else 18.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = pagePadding, vertical = verticalPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onReportClick,
                        modifier = Modifier
                            .size(topIconSize)
                            .border(2.dp, ChalkWhite.copy(alpha = 0.78f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Assessment,
                            contentDescription = "Relatório da sessão",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(topIconInnerSize)
                        )
                    }
                    Spacer(Modifier.width(if (compactWidth) 8.dp else 10.dp))
                    IconButton(
                        onClick = onUpdateClick,
                        modifier = Modifier
                            .size(topIconSize)
                            .border(2.dp, ChalkWhite.copy(alpha = 0.78f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = "Atualizar app",
                            tint = ChalkWhite,
                            modifier = Modifier.size(topIconInnerSize)
                        )
                    }
                }
                ChalkText(
                    text = "FonoLousa",
                    fontSize = titleSize,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                ChalkText(
                    text = "Versão ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    fontSize = versionSize,
                    color = ChalkWhite.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                )
                HomeRegistrationPanel(
                    childName = childName,
                    onChildNameChange = { childName = it.replace("\n", " ").take(40) },
                    duplicateChildName = duplicateChildName,
                    canStartNewAssessment = canStartNewAssessment,
                    compact = compactWidth,
                    onStartAssessment = { onClinicalClick(typedChildName) },
                    onReportClick = onReportClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = if (compactWidth || shortScreen) 12.dp else 16.dp,
                            bottom = if (duplicateChildName) 6.dp else if (compactWidth) 12.dp else 16.dp
                        )
                )
                if (duplicateChildName) {
                    ChalkText(
                        text = "Criança já cadastrada. Use outro nome para novo cadastro.",
                        fontSize = if (compactWidth) 14 else 16,
                        color = Color(0xFFFFB3B3),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (compactWidth) 6.dp else 8.dp)
                    )
                }
                ChalkText(
                    text = "Toque em uma categoria",
                    fontSize = headingSize,
                    modifier = Modifier.padding(
                        top = if (compactWidth) 2.dp else 6.dp,
                        bottom = if (compactWidth) 12.dp else 22.dp
                    )
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(
                        start = if (compactWidth) 2.dp else 8.dp,
                        top = 2.dp,
                        end = if (compactWidth) 2.dp else 8.dp,
                        bottom = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(if (compactWidth) 0.dp else 22.dp),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(categorias, key = { it.id }) { categoria ->
                        CategoryButton(categoria = categoria, onClick = { onCategoryClick(categoria) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRegistrationPanel(
    childName: String,
    onChildNameChange: (String) -> Unit,
    duplicateChildName: Boolean,
    canStartNewAssessment: Boolean,
    compact: Boolean,
    onStartAssessment: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelModifier = modifier
        .clip(RoundedCornerShape(8.dp))
        .border(2.dp, ChalkWhite.copy(alpha = 0.42f), RoundedCornerShape(8.dp))
        .background(Color.White.copy(alpha = 0.10f))
        .padding(if (compact) 10.dp else 12.dp)

    if (compact) {
        Column(
            modifier = panelModifier,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeNameTextField(
                childName = childName,
                onChildNameChange = onChildNameChange,
                duplicateChildName = duplicateChildName,
                compact = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            )
            HomePrimaryButton(
                text = "Cadastrar e avaliar",
                enabled = canStartNewAssessment,
                compact = true,
                onClick = onStartAssessment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
            HomeSecondaryButton(
                text = "Ver Relatórios",
                compact = true,
                onClick = onReportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }
    } else {
        Row(
            modifier = panelModifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeNameTextField(
                childName = childName,
                onChildNameChange = onChildNameChange,
                duplicateChildName = duplicateChildName,
                compact = false,
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp)
            )
            HomePrimaryButton(
                text = "Cadastrar e avaliar",
                enabled = canStartNewAssessment,
                compact = false,
                onClick = onStartAssessment,
                modifier = Modifier
                    .height(62.dp)
                    .width(230.dp)
            )
            HomeSecondaryButton(
                text = "Ver Relatórios",
                compact = false,
                onClick = onReportClick,
                modifier = Modifier
                    .height(62.dp)
                    .width(170.dp)
            )
        }
    }
}

@Composable
private fun HomeNameTextField(
    childName: String,
    onChildNameChange: (String) -> Unit,
    duplicateChildName: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    TextField(
        value = childName,
        onValueChange = onChildNameChange,
        placeholder = {
            Text(
                text = "Nome da nova criança",
                fontSize = if (compact) 16.sp else 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        isError = duplicateChildName,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        textStyle = TextStyle(
            fontSize = if (compact) 17.sp else 18.sp,
            fontWeight = FontWeight.Bold
        ),
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color(0xFF113D18),
            unfocusedTextColor = Color(0xFF113D18),
            focusedContainerColor = Color(0xFFFCFBF1),
            unfocusedContainerColor = Color(0xFFFCFBF1),
            errorContainerColor = Color(0xFFFFF6F6),
            cursorColor = ChalkGreen,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color(0xFFE53935)
        ),
        modifier = modifier
    )
}

@Composable
private fun HomePrimaryButton(
    text: String,
    enabled: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFC107),
            contentColor = Color(0xFF1A1A1A)
        ),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = if (compact) 16.sp else 17.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HomeSecondaryButton(
    text: String,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = if (compact) 16.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UpdateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val checker = remember { UpdateChecker() }
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var installMessage by remember { mutableStateOf<String?>(null) }

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
                    text = "Canal de atualização",
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
                            installMessage = null
                            state = checker.check()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Text(
                        text = "Verificar atualização",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                val available = state as? UpdateState.Available
                if (available != null) {
                    Button(
                        onClick = {
                            scope.launch {
                                installMessage = "Baixando atualização..."
                                installMessage = installUpdate(context, available.manifest.apkUrl)
                            }
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
                            text = "Baixar e instalar",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                if (installMessage != null) {
                    ChalkText(
                        text = installMessage.orEmpty(),
                        fontSize = 20,
                        color = ChalkWhite.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionReportScreen(
    sessionRepository: SessionRepository,
    progress: List<ItemProgressEntity>,
    favorites: List<ItemProgressEntity>,
    events: List<SessionEventEntity>,
    clinicalResults: List<ClinicalResultEntity>,
    onBack: () -> Unit
) {
    val totalViews = progress.sumOf { it.views }
    val totalPlays = progress.sumOf { it.plays }
    val practiced = progress.count { it.views > 0 || it.plays > 0 }
    val formatter = remember { SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR")) }
    val scope = rememberCoroutineScope()
    val childNames = remember(clinicalResults) { clinicalResults.registeredChildNames() }
    var selectedChild by remember { mutableStateOf("") }
    var selectedTrendActivity by remember { mutableStateOf("nomeacao") }
    LaunchedEffect(childNames) {
        if (childNames.isNotEmpty() && selectedChild !in childNames) {
            selectedChild = childNames.first()
        }
    }
    val selectedChildKey = remember(selectedChild) { normalizeChildName(selectedChild) }
    val childFilteredResults = remember(clinicalResults, selectedChildKey) {
        if (selectedChildKey.isBlank()) {
            emptyList()
        } else {
            clinicalResults.filter { normalizeChildName(it.childName) == selectedChildKey }
        }
    }
    val evaluationGroups = remember(childFilteredResults) {
        clinicalEvaluationGroups(childFilteredResults)
    }

    BlackboardScreen {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compactReport = maxWidth < 780.dp || maxHeight < 620.dp
            val pageHorizontalPadding = if (compactReport) 10.dp else 18.dp
            val pageVerticalPadding = if (compactReport) 8.dp else 14.dp
            val sectionSpacing = if (compactReport) 10.dp else 12.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = pageHorizontalPadding, vertical = pageVerticalPadding)
            ) {
                TopBar(title = "Relatório da sessão", onBack = onBack, compact = compactReport)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing)
                ) {
                    if (childNames.isNotEmpty()) {
                        if (compactReport) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReportFilterDropdown(
                                    label = "Criança",
                                    value = selectedChild,
                                    options = childNames,
                                    optionLabel = { it },
                                    onSelect = { selectedChild = it },
                                    compact = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                ReportFilterDropdown(
                                    label = "Gráfico",
                                    value = selectedTrendActivity,
                                    options = clinicalActivityKeys(),
                                    optionLabel = { it.label() },
                                    onSelect = { selectedTrendActivity = it },
                                    compact = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ReportFilterDropdown(
                                    label = "Criança",
                                    value = selectedChild,
                                    options = childNames,
                                    optionLabel = { it },
                                    onSelect = { selectedChild = it },
                                    modifier = Modifier.weight(1f)
                                )
                                ReportFilterDropdown(
                                    label = "Gráfico",
                                    value = selectedTrendActivity,
                                    options = clinicalActivityKeys(),
                                    optionLabel = { it.label() },
                                    onSelect = { selectedTrendActivity = it },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    ClinicalPatientSummarySection(
                        results = childFilteredResults,
                        evaluations = evaluationGroups,
                        compact = compactReport
                    )
                    ClinicalCountsSection(results = childFilteredResults, compact = compactReport)
                    ClinicalChartSection(results = childFilteredResults, compact = compactReport)
                    ClinicalTrendSection(results = childFilteredResults, activity = selectedTrendActivity, compact = compactReport)
                    ClinicalEvaluationsAdminSection(
                        evaluations = evaluationGroups,
                        formatter = formatter,
                        compact = compactReport,
                        onSave = { answers ->
                            scope.launch {
                                answers.forEach { (id, isCorrect) ->
                                    sessionRepository.updateClinicalResult(id, isCorrect)
                                }
                            }
                        },
                        onDelete = { evaluation ->
                            scope.launch {
                                sessionRepository.deleteClinicalResults(evaluation.results.map { it.id })
                            }
                        }
                    )
                    ChalkText(
                        text = "Uso geral do app",
                        fontSize = if (compactReport) 20 else 22,
                        fontWeight = FontWeight.Black
                    )
                    AdaptiveMetricGrid(
                        columns = if (compactReport) 2 else 4,
                        itemCount = 4,
                        horizontalSpacing = if (compactReport) 8.dp else 10.dp,
                        verticalSpacing = 8.dp
                    ) { tileModifier, index ->
                        when (index) {
                            0 -> ReportMetric("Itens", practiced.toString(), tileModifier, compact = compactReport)
                            1 -> ReportMetric("Toques", totalViews.toString(), tileModifier, compact = compactReport)
                            2 -> ReportMetric("Sons", totalPlays.toString(), tileModifier, compact = compactReport)
                            3 -> ReportMetric("Favoritos", favorites.size.toString(), tileModifier, compact = compactReport)
                        }
                    }
                    ChalkText(
                        text = "Últimas interações",
                        fontSize = if (compactReport) 22 else 24,
                        fontWeight = FontWeight.Black
                    )
                    if (events.isEmpty()) {
                        ChalkText(
                            text = "Nenhuma interação registrada ainda.",
                            fontSize = if (compactReport) 18 else 20,
                            color = ChalkWhite.copy(alpha = 0.82f)
                        )
                    } else {
                        events.take(30).forEach { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .border(1.dp, ChalkWhite.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(if (compactReport) 10.dp else 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    ChalkText(
                                        text = event.word.replaceFirstChar { it.uppercase() },
                                        fontSize = if (compactReport) 18 else 21,
                                        maxLines = 1
                                    )
                                    ChalkText(
                                        text = "${event.categoryId} - nível ${event.level}",
                                        fontSize = if (compactReport) 14 else 16,
                                        color = ChalkWhite.copy(alpha = 0.74f),
                                        maxLines = 1
                                    )
                                }
                                ChalkText(
                                    text = "${event.eventType.label()}  ${formatter.format(Date(event.createdAt))}",
                                    fontSize = if (compactReport) 14 else 16,
                                    textAlign = TextAlign.End,
                                    color = ChalkWhite.copy(alpha = 0.84f),
                                    modifier = Modifier.width(if (compactReport) 116.dp else 138.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportFilterDropdown(
    label: String,
    value: String,
    options: List<String>,
    optionLabel: (String) -> String,
    onSelect: (String) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Button(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 52.dp else 58.dp)
        ) {
            Text(
                text = "$label: ${optionLabel(value)}",
                fontSize = if (compact) 15.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AdaptiveMetricGrid(
    columns: Int,
    itemCount: Int,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 10.dp,
    verticalSpacing: Dp = 10.dp,
    content: @Composable (Modifier, Int) -> Unit
) {
    val safeColumns = columns.coerceAtLeast(1)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        (0 until itemCount).chunked(safeColumns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
            ) {
                rowItems.forEach { index ->
                    content(Modifier.weight(1f), index)
                }
                repeat(safeColumns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ReportMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(
        modifier = modifier
            .height(if (compact) 78.dp else 92.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, ChalkWhite.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.11f))
            .padding(if (compact) 8.dp else 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ChalkText(
            text = value,
            fontSize = if (compact) 21 else 24,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        ChalkText(
            text = label,
            fontSize = if (compact) 13 else 14,
            color = ChalkWhite.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ClinicalPatientSummarySection(
    results: List<ClinicalResultEntity>,
    evaluations: List<ClinicalEvaluationGroup>,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val correct = results.count { it.isCorrect }
    val errors = results.size - correct
    val metrics = listOf(
        "Avaliações" to evaluations.size.toString(),
        "Acertos" to correct.toString(),
        "Erros" to errors.toString(),
        "Total" to results.size.toString()
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, ChalkWhite.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(if (compact) 10.dp else 12.dp)
    ) {
        ChalkText(
            text = "Resumo clínico da criança",
            fontSize = if (compact) 18 else 20,
            fontWeight = FontWeight.Black
        )
        AdaptiveMetricGrid(
            columns = if (compact) 2 else 4,
            itemCount = metrics.size,
            modifier = Modifier.padding(top = 10.dp),
            horizontalSpacing = if (compact) 8.dp else 10.dp,
            verticalSpacing = 8.dp
        ) { tileModifier, index ->
            val (label, value) = metrics[index]
            ReportMetric(label, value, tileModifier, compact = compact)
        }
    }
}

@Composable
private fun ClinicalEvaluationsAdminSection(
    evaluations: List<ClinicalEvaluationGroup>,
    formatter: SimpleDateFormat,
    compact: Boolean = false,
    onSave: (Map<Long, Boolean>) -> Unit,
    onDelete: (ClinicalEvaluationGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingKey by remember { mutableStateOf<String?>(null) }
    var pendingDeleteKey by remember { mutableStateOf<String?>(null) }
    var draftAnswers by remember { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, ChalkWhite.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(if (compact) 10.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
    ) {
        ChalkText(
            text = "Avaliações da criança",
            fontSize = if (compact) 18 else 20,
            fontWeight = FontWeight.Black
        )
        if (evaluations.isEmpty()) {
            ChalkText(
                text = "Nenhuma avaliação salva para esta criança.",
                fontSize = if (compact) 16 else 18,
                color = ChalkWhite.copy(alpha = 0.82f)
            )
        } else {
            evaluations.forEach { evaluation ->
                val isEditing = editingKey == evaluation.key
                val isPendingDelete = pendingDeleteKey == evaluation.key
                val cardModifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(if (compact) 8.dp else 10.dp)

                if (compact) {
                    Column(
                        modifier = cardModifier,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClinicalEvaluationSummaryText(
                            evaluation = evaluation,
                            formatter = formatter,
                            compact = true
                        )
                        ClinicalEvaluationActionButtons(
                            isPendingDelete = isPendingDelete,
                            isEditing = isEditing,
                            compact = true,
                            onConfirmDelete = {
                                onDelete(evaluation)
                                pendingDeleteKey = null
                                editingKey = null
                                draftAnswers = emptyMap()
                            },
                            onCancelDelete = { pendingDeleteKey = null },
                            onSaveEdit = {
                                onSave(draftAnswers)
                                editingKey = null
                                draftAnswers = emptyMap()
                            },
                            onCancelEdit = {
                                editingKey = null
                                draftAnswers = emptyMap()
                            },
                            onEdit = {
                                editingKey = evaluation.key
                                pendingDeleteKey = null
                                draftAnswers = evaluation.results.associate { it.id to it.isCorrect }
                            },
                            onDelete = { pendingDeleteKey = evaluation.key }
                        )
                    }
                } else {
                    Row(
                        modifier = cardModifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClinicalEvaluationSummaryText(
                            evaluation = evaluation,
                            formatter = formatter,
                            compact = false,
                            modifier = Modifier.weight(1f)
                        )
                        ClinicalEvaluationActionButtons(
                            isPendingDelete = isPendingDelete,
                            isEditing = isEditing,
                            compact = false,
                            onConfirmDelete = {
                                onDelete(evaluation)
                                pendingDeleteKey = null
                                editingKey = null
                                draftAnswers = emptyMap()
                            },
                            onCancelDelete = { pendingDeleteKey = null },
                            onSaveEdit = {
                                onSave(draftAnswers)
                                editingKey = null
                                draftAnswers = emptyMap()
                            },
                            onCancelEdit = {
                                editingKey = null
                                draftAnswers = emptyMap()
                            },
                            onEdit = {
                                editingKey = evaluation.key
                                pendingDeleteKey = null
                                draftAnswers = evaluation.results.associate { it.id to it.isCorrect }
                            },
                            onDelete = { pendingDeleteKey = evaluation.key }
                        )
                    }
                }
                if (isEditing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        evaluation.results.sortedBy { it.createdAt }.forEachIndexed { index, result ->
                            val isCorrect = draftAnswers[result.id] ?: result.isCorrect
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ChalkText(
                                    text = "${index + 1}. ${result.activity.label()} - ${result.word.replaceFirstChar { it.uppercase() }}",
                                    fontSize = 15,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { draftAnswers = draftAnswers + (result.id to true) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCorrect) Color(0xFF43A047) else Color.White.copy(alpha = 0.16f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Acerto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { draftAnswers = draftAnswers + (result.id to false) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isCorrect) Color(0xFFE53935) else Color.White.copy(alpha = 0.16f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Erro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClinicalEvaluationSummaryText(
    evaluation: ClinicalEvaluationGroup,
    formatter: SimpleDateFormat,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ChalkText(
            text = "Avaliação clínica",
            fontSize = if (compact) 16 else 18,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        ChalkText(
            text = "${formatter.format(Date(evaluation.startedAt))}  -  ${evaluation.activityLabels}",
            fontSize = if (compact) 12 else 13,
            color = ChalkWhite.copy(alpha = 0.74f),
            maxLines = 1
        )
        ChalkText(
            text = "Acertos ${evaluation.correct}  Erros ${evaluation.errors}  Total ${evaluation.total}",
            fontSize = if (compact) 12 else 13,
            color = ChalkWhite.copy(alpha = 0.74f),
            maxLines = 1
        )
    }
}

@Composable
private fun ClinicalEvaluationActionButtons(
    isPendingDelete: Boolean,
    isEditing: Boolean,
    compact: Boolean,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonHeight = if (compact) 40.dp else 42.dp
    val fontSize = if (compact) 12.sp else 13.sp

    Row(
        modifier = if (compact) modifier.fillMaxWidth() else modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val buttonModifier = if (compact) Modifier.weight(1f).height(buttonHeight) else Modifier.height(buttonHeight)

        when {
            isPendingDelete -> {
                Button(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = buttonModifier
                ) {
                    Text("Confirmar", fontSize = fontSize, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onCancelDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = buttonModifier
                ) {
                    Text("Cancelar", fontSize = fontSize, fontWeight = FontWeight.Bold)
                }
            }

            isEditing -> {
                Button(
                    onClick = onSaveEdit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF43A047),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = buttonModifier
                ) {
                    Text("Salvar", fontSize = fontSize, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onCancelEdit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = buttonModifier
                ) {
                    Text("Cancelar", fontSize = fontSize, fontWeight = FontWeight.Bold)
                }
            }

            else -> {
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107),
                        contentColor = Color(0xFF102D13)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = buttonModifier
                ) {
                    Text("Editar", fontSize = fontSize, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = buttonModifier
                ) {
                    Text("Excluir avaliação", fontSize = fontSize, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ClinicalAssessmentScreen(
    childName: String,
    repository: DataRepository,
    sessionRepository: SessionRepository,
    audioPlayer: AudioPlayer,
    categorias: List<Categoria>,
    onBack: () -> Unit,
    onReport: () -> Unit,
    onHome: () -> Unit
) {
    val activities = remember { clinicalActivityKeys() }
    val clinicalStimuli = remember(categorias) { buildClinicalStimuli(categorias) }
    val totalTrials = clinicalStimuli.size.coerceAtLeast(1)
    var activity by remember { mutableStateOf(activities.first()) }
    var itemIndex by remember { mutableStateOf(0) }
    var assessmentFinished by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf<String?>(null) }
    var pulse by remember { mutableStateOf(false) }
    var readingAnswered by remember { mutableStateOf(false) }
    var clinicalSessionId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var savingAnswer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val stimulus = clinicalStimuli.getOrNull(itemIndex.coerceIn(0, totalTrials - 1))
    val item = stimulus?.item
    val comparison = if (clinicalStimuli.isEmpty()) null else clinicalStimuli[(itemIndex + 1) % clinicalStimuli.size]
    val imageScale by animateFloatAsState(
        targetValue = if (pulse) 1.03f else 1f,
        animationSpec = tween(160),
        finishedListener = { pulse = false },
        label = "clinicalPulse"
    )
    val currentActivityIndex = activities.indexOf(activity).coerceAtLeast(0)
    val currentTrial = itemIndex + 1

    fun resetAssessmentProgress() {
        activity = activities.first()
        itemIndex = 0
        assessmentFinished = false
        showFeedback = null
        readingAnswered = false
        clinicalSessionId = UUID.randomUUID().toString()
        savingAnswer = false
    }

    fun nextTrial() {
        readingAnswered = false
        if (itemIndex < totalTrials - 1) {
            itemIndex += 1
            showFeedback = null
            return
        }

        val nextActivityIndex = currentActivityIndex + 1
        if (nextActivityIndex < activities.size) {
            activity = activities[nextActivityIndex]
            itemIndex = 0
            assessmentFinished = false
            showFeedback = "${activities[currentActivityIndex].label()} concluída"
        } else {
            assessmentFinished = true
            showFeedback = "Avaliação concluída"
        }
    }

    fun recordAnswer(isCorrect: Boolean) {
        if (savingAnswer) return
        val currentStimulus = stimulus ?: return
        val currentItem = item ?: return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        savingAnswer = true
        scope.launch {
            try {
                sessionRepository.recordClinicalResult(
                    childName,
                    clinicalSessionId,
                    activity,
                    currentStimulus.categoria.id,
                    currentStimulus.level,
                    currentItem,
                    isCorrect
                )
                showFeedback = if (isCorrect) "Acerto registrado" else "Erro registrado"
                if (isCorrect) {
                    audioPlayer.playVictory()
                }
                if (activity == "leitura") {
                    readingAnswered = true
                } else {
                    nextTrial()
                }
            } finally {
                savingAnswer = false
            }
        }
    }

    BlackboardScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            TopBar(title = "Avaliação clínica", onBack = onBack)
            ChalkText(
                text = childName,
                fontSize = 16,
                color = ChalkWhite.copy(alpha = 0.82f),
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (assessmentFinished) {
                ClinicalFinishedState(
                    childName = childName,
                    onReport = onReport,
                    onRestart = ::resetAssessmentProgress,
                    onHome = onHome,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else if (stimulus == null || item == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ChalkText(
                        text = "Nenhum estímulo disponível para avaliação.",
                        fontSize = 24,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val isReading = activity == "leitura"
                val showImage = !isReading || readingAnswered
                val stimulusText = item.displayText(stimulus.level)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ChalkText(
                        text = activity.label(),
                        fontSize = 22,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFC107),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ChalkText(
                        text = activity.prompt(),
                        fontSize = 18,
                        textAlign = TextAlign.Center,
                        color = ChalkWhite.copy(alpha = 0.9f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    ChalkText(
                        text = activity.trialLabel(currentTrial, totalTrials),
                        fontSize = 22,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    if (isReading) {
                        ReadingStimulusText(
                            text = stimulusText,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    if (showImage) {
                        AssetImage(
                            repository = repository,
                            path = item.arquivoImagem,
                            contentDescription = stimulusText,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .size(if (isReading) 210.dp else 240.dp)
                                .graphicsLayer(scaleX = imageScale, scaleY = imageScale)
                                .clip(RoundedCornerShape(8.dp))
                                .border(4.dp, parseColor(stimulus.categoria.cor), RoundedCornerShape(8.dp))
                                .background(Color.White)
                        )
                    }
                    if (showFeedback != null) {
                        ChalkText(
                            text = showFeedback.orEmpty(),
                            fontSize = 18,
                            color = Color(0xFFFFC107),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            if (!assessmentFinished && stimulus != null && item != null) {
                if (activity == "leitura" && readingAnswered) {
                    Button(
                        onClick = { nextTrial() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107),
                            contentColor = ChalkGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        Text("Proximo", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (activity != "leitura") {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                audioPlayer.play(item.arquivoSom, item.audioText(stimulus.level))
                                pulse = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFC107),
                                contentColor = ChalkGreen
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                        ) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (activity == "discriminacao") "Som 1" else "Som",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (activity == "discriminacao" && comparison != null) {
                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch {
                                        audioPlayer.play(item.arquivoSom, item.audioText(stimulus.level))
                                        delay(900)
                                        audioPlayer.play(comparison.item.arquivoSom, comparison.item.audioText(comparison.level))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC107),
                                    contentColor = ChalkGreen
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                Icon(Icons.Filled.VolumeUp, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Tocar par", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (!(activity == "leitura" && readingAnswered)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Button(
                            onClick = { recordAnswer(false) },
                            enabled = !savingAnswer,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                        ) {
                            Text("Erro", fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = { recordAnswer(true) },
                            enabled = !savingAnswer,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                        ) {
                            Text("Acerto", fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingStimulusText(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.62f)
            .clip(RoundedCornerShape(8.dp))
            .border(3.dp, Color(0xFFFFC107), RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .padding(horizontal = 18.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(Locale.forLanguageTag("pt-BR")),
            color = ChalkGreen,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ClinicalFinishedState(
    childName: String,
    onReport: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ChalkText(
            text = "Avaliação concluída",
            fontSize = 30,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFFC107),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        ChalkText(
            text = childName,
            fontSize = 22,
            color = ChalkWhite.copy(alpha = 0.88f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 22.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onReport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = ChalkGreen
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
            ) {
                Text("Ir para relatório", fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.16f),
                    contentColor = ChalkWhite
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
            ) {
                Text("Nova avaliação", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.16f),
                    contentColor = ChalkWhite
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
            ) {
                Text("Início", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ClinicalCountsSection(
    results: List<ClinicalResultEntity>,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val summaries = clinicalActivitySummaries(results)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, ChalkWhite.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(if (compact) 10.dp else 14.dp)
    ) {
        ChalkText(
            text = "Acertos e erros por avaliação",
            fontSize = if (compact) 18 else 20,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        AdaptiveMetricGrid(
            columns = if (compact) 2 else summaries.size,
            itemCount = summaries.size,
            horizontalSpacing = if (compact) 8.dp else 12.dp,
            verticalSpacing = 8.dp
        ) { tileModifier, index ->
            ClinicalActivityCountCard(
                summary = summaries[index],
                compact = compact,
                modifier = tileModifier
            )
        }
    }
}

@Composable
private fun ClinicalActivityCountCard(
    summary: ClinicalActivitySummary,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(if (compact) 9.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
    ) {
        ChalkText(
            text = summary.label,
            fontSize = if (compact) 15 else 17,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
        ChalkText(
            text = "Acertos: ${summary.correct}",
            fontSize = if (compact) 14 else 16,
            color = Color(0xFFB8F5BD),
            textAlign = TextAlign.Center
        )
        ChalkText(
            text = "Erros: ${summary.errors}",
            fontSize = if (compact) 14 else 16,
            color = Color(0xFFFFB3B3),
            textAlign = TextAlign.Center
        )
        ChalkText(
            text = "Total: ${summary.total}",
            fontSize = if (compact) 13 else 15,
            color = ChalkWhite.copy(alpha = 0.82f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ClinicalChartSection(
    results: List<ClinicalResultEntity>,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val summaries = clinicalActivityKeys().map { activity ->
        val items = results.filter { it.activity == activity }
        val percent = if (items.isEmpty()) 0 else (items.count { it.isCorrect } * 100f / items.size).roundToInt()
        ClinicalBar(activity.label(), percent, items.size)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 132.dp else 154.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, ChalkWhite.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(if (compact) 10.dp else 12.dp)
    ) {
        ChalkText(
            text = "Desempenho clínico",
            fontSize = if (compact) 18 else 20,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            summaries.forEach { summary ->
                ClinicalBarView(summary = summary, compact = compact, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ClinicalTrendSection(
    results: List<ClinicalResultEntity>,
    activity: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val points = remember(results, activity) { clinicalTrendPoints(results, activity) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 132.dp else 154.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, ChalkWhite.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(if (compact) 10.dp else 12.dp)
    ) {
        ChalkText(
            text = "Evolução - ${activity.label()}",
            fontSize = if (compact) 18 else 20,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        if (points.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ChalkText(
                    text = "Sem sessões registradas.",
                    fontSize = if (compact) 16 else 18,
                    color = ChalkWhite.copy(alpha = 0.78f)
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartWidth = size.width
                val chartHeight = size.height
                val stepX = if (points.size <= 1) chartWidth else chartWidth / (points.size - 1)
                val offsets = points.mapIndexed { index, value ->
                    Offset(
                        x = if (points.size <= 1) chartWidth / 2f else stepX * index,
                        y = chartHeight - (value.coerceIn(0f, 100f) / 100f * chartHeight)
                    )
                }
                for (index in 0 until offsets.lastIndex) {
                    drawLine(
                        color = Color(0xFFFFC107),
                        start = offsets[index],
                        end = offsets[index + 1],
                        strokeWidth = 6f
                    )
                }
                offsets.forEach { point ->
                    drawCircle(color = ChalkWhite, radius = 8f, center = point)
                    drawCircle(color = Color(0xFFFFC107), radius = 5f, center = point)
                }
            }
        }
    }
}

@Composable
private fun ClinicalBarView(
    summary: ClinicalBar,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val barHeight = if (compact) 40.dp else 54.dp

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        ChalkText(
            text = "${summary.percent}%",
            fontSize = if (compact) 16 else 18,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .padding(horizontal = if (compact) 3.dp else 6.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight * (summary.percent.coerceIn(0, 100) / 100f))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFC107))
            )
        }
        ChalkText(
            text = "${summary.label} (${summary.total})",
            fontSize = if (compact) 11 else 13,
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

private data class ClinicalStimulus(
    val categoria: Categoria,
    val level: Int,
    val item: ItemFono
)

private data class ClinicalEvaluationGroup(
    val key: String,
    val sessionId: String,
    val startedAt: Long,
    val results: List<ClinicalResultEntity>
) {
    val correct: Int = results.count { it.isCorrect }
    val errors: Int = results.size - correct
    val total: Int = results.size
    val activityLabels: String = results
        .map { it.activity }
        .distinct()
        .joinToString(", ") { it.label() }
}

private fun clinicalActivityKeys(): List<String> =
    listOf("nomeacao", "repeticao", "discriminacao", "leitura")

private fun List<ClinicalResultEntity>.registeredChildNames(): List<String> {
    return map { it.childName.cleanChildName() }
        .filter { it.isNotBlank() }
        .distinctBy { normalizeChildName(it) }
        .sortedBy { normalizeChildName(it) }
}

private fun String.cleanChildName(): String =
    trim().split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ")

private fun normalizeChildName(name: String): String =
    name.cleanChildName().lowercase(Locale.forLanguageTag("pt-BR"))

private fun clinicalEvaluationGroups(results: List<ClinicalResultEntity>): List<ClinicalEvaluationGroup> {
    val activityOrder = clinicalActivityKeys()
    val expectedEvaluationSize = CLINICAL_TRIALS_PER_ACTIVITY * activityOrder.size
    val maxSameEvaluationGapMillis = 5 * 60 * 1000L
    fun activityIndex(activity: String): Int = activityOrder.indexOf(activity).takeIf { it >= 0 } ?: Int.MAX_VALUE

    return results
        .groupBy { it.sessionId }
        .flatMap { (sessionId, sessionResults) ->
            val groups = mutableListOf<List<ClinicalResultEntity>>()
            var current = mutableListOf<ClinicalResultEntity>()
            var previous: ClinicalResultEntity? = null

            sessionResults.sortedBy { it.createdAt }.forEach { result ->
                val previousResult = previous
                val shouldStartNewEvaluation = current.isNotEmpty() && previousResult != null && (
                    result.createdAt - previousResult.createdAt > maxSameEvaluationGapMillis ||
                        activityIndex(result.activity) < activityIndex(previousResult.activity) ||
                        current.size >= expectedEvaluationSize
                    )

                if (shouldStartNewEvaluation) {
                    groups += current.toList()
                    current = mutableListOf()
                }

                current += result
                previous = result
            }

            if (current.isNotEmpty()) {
                groups += current.toList()
            }

            groups.mapIndexed { index, chunk ->
                ClinicalEvaluationGroup(
                    key = "$sessionId|$index",
                    sessionId = sessionId,
                    startedAt = chunk.first().createdAt,
                    results = chunk
                )
            }
        }
        .sortedByDescending { it.startedAt }
}

private fun clinicalTrendPoints(results: List<ClinicalResultEntity>, activity: String): List<Float> {
    return clinicalEvaluationGroups(results)
        .sortedBy { it.startedAt }
        .mapNotNull { evaluation ->
            val activityResults = evaluation.results.filter { it.activity == activity }
            if (activityResults.isEmpty()) {
                null
            } else {
                activityResults.count { it.isCorrect } * 100f / activityResults.size
            }
        }
}

private fun buildClinicalStimuli(categorias: List<Categoria>): List<ClinicalStimulus> {
    val locale = Locale.forLanguageTag("pt-BR")
    val buckets = categorias
        .mapNotNull { categoria ->
            val stimuli = categoria.niveis
                .flatMap { nivel ->
                    nivel.itens.map { item ->
                        ClinicalStimulus(
                            categoria = categoria,
                            level = nivel.nivel,
                            item = item
                        )
                    }
                }
                .distinctBy { it.item.palavra.trim().lowercase(locale) }
                .shuffled()
                .toMutableList()

            if (stimuli.isEmpty()) null else stimuli
        }
        .shuffled()
        .toMutableList()

    val result = mutableListOf<ClinicalStimulus>()
    var previousCategoryId: String? = null

    while (result.size < CLINICAL_TRIALS_PER_ACTIVITY && buckets.any { it.isNotEmpty() }) {
        val bucketIndex = buckets.indexOfFirst { bucket ->
            bucket.isNotEmpty() && bucket.first().categoria.id != previousCategoryId
        }.takeIf { it >= 0 } ?: buckets.indexOfFirst { it.isNotEmpty() }

        if (bucketIndex < 0) break

        val stimulus = buckets[bucketIndex].removeAt(0)
        result += stimulus
        previousCategoryId = stimulus.categoria.id

        val bucket = buckets.removeAt(bucketIndex)
        if (bucket.isNotEmpty()) {
            buckets.add(bucket)
        }
    }

    return result
}

private data class ClinicalActivitySummary(
    val label: String,
    val correct: Int,
    val errors: Int
) {
    val total: Int = correct + errors
}

private fun clinicalActivitySummaries(results: List<ClinicalResultEntity>): List<ClinicalActivitySummary> {
    return clinicalActivityKeys().map { activity ->
        val items = results.filter { it.activity == activity }
        ClinicalActivitySummary(
            label = activity.label(),
            correct = items.count { it.isCorrect },
            errors = items.count { !it.isCorrect }
        )
    }
}

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
            TopBar(
                title = if (nivel.nivel <= 0) categoria.nome else "${categoria.nome} - N\u00edvel ${nivel.nivel}",
                onBack = onBack
            )
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
        AnimatedVisibility(visible = progress?.isFavorite == true) {
            Text(
                text = "Favorito",
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
                TopBar(
                    title = if (nivel.nivel <= 0) categoria.nome else "${categoria.nome} - N\u00edvel ${nivel.nivel}",
                    onBack = onBack
                )
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
private fun TopBar(title: String, onBack: () -> Unit, compact: Boolean = false) {
    val barHeight = if (compact) 50.dp else 58.dp
    val backButtonSize = if (compact) 46.dp else 52.dp
    val iconSize = if (compact) 27.dp else 30.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(backButtonSize)
                .border(2.dp, ChalkWhite.copy(alpha = 0.78f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = ChalkWhite,
                modifier = Modifier.size(iconSize)
            )
        }
        ChalkText(
            text = title,
            fontSize = if (compact) 24 else 28,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (compact) 12.dp else 18.dp)
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
        UpdateState.Idle -> "Toque no botão para verificar se existe uma nova versão publicada."
        UpdateState.Checking -> "Verificando nova versão..."
        is UpdateState.Available -> "Canal oficial disponível.\n${state.manifest.notes}"
        is UpdateState.UpToDate -> "Este aparelho já está com a versão ${state.currentVersionName}."
        is UpdateState.RemoteBehind -> "Canal remoto desatualizado: publicado ${state.remoteVersionName}, app instalado ${state.currentVersionName}."
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
        "nomeacao" -> "Nomeação"
        "repeticao" -> "Repetição"
        "discriminacao" -> "Discriminação"
        "leitura" -> "Leitura"
        else -> this
    }
}

private fun String.trialLabel(currentTrial: Int, totalTrials: Int): String {
    return when (this) {
        "nomeacao" -> "Figura $currentTrial/$totalTrials"
        "repeticao" -> "Áudio $currentTrial/$totalTrials"
        "discriminacao" -> "Par $currentTrial/$totalTrials"
        "leitura" -> "Leitura $currentTrial/$totalTrials"
        else -> "Tentativa $currentTrial/$totalTrials"
    }
}

private suspend fun installUpdate(context: Context, apkUrl: String): String {
    return try {
        if (!isTrustedUpdateUrl(apkUrl)) {
            return "Falha ao instalar: link de atualização fora do canal oficial."
        }
        val apkFile = downloadUpdateApk(context, apkUrl)
        validateDownloadedUpdateApk(context, apkFile)
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        "Instalador aberto. Confirme a atualização para concluir."
    } catch (error: Exception) {
        "Falha ao instalar: ${error.message ?: "não foi possível baixar o APK."}"
    }
}

private suspend fun downloadUpdateApk(context: Context, apkUrl: String): File = withContext(Dispatchers.IO) {
    require(isTrustedUpdateUrl(apkUrl)) { "Link de atualização fora do canal oficial." }
    val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
    val apkFile = File(updateDir, "FonoLousa-update.apk")
    val tempFile = File(updateDir, "FonoLousa-update.apk.tmp")
    var connection: HttpURLConnection? = null
    try {
        if (tempFile.exists()) tempFile.delete()
        connection = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("APK indisponível: HTTP ${connection.responseCode}")
        }
        val expectedLength = connection.contentLengthLong
        if (expectedLength > MAX_UPDATE_APK_BYTES) {
            throw IllegalStateException("APK maior que o limite permitido.")
        }
        if (expectedLength in 1 until MIN_UPDATE_APK_BYTES) {
            throw IllegalStateException("Arquivo recebido não parece ser um APK válido.")
        }
        var copiedBytes = 0L
        connection.inputStream.use { input ->
            tempFile.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    copiedBytes += read
                    if (copiedBytes > MAX_UPDATE_APK_BYTES) {
                        throw IllegalStateException("APK maior que o limite permitido.")
                    }
                    output.write(buffer, 0, read)
                }
            }
        }
        if (copiedBytes < MIN_UPDATE_APK_BYTES) {
            throw IllegalStateException("Arquivo recebido não parece ser um APK válido.")
        }
        if (apkFile.exists()) apkFile.delete()
        if (!tempFile.renameTo(apkFile)) {
            tempFile.copyTo(apkFile, overwrite = true)
            tempFile.delete()
        }
    } finally {
        connection?.disconnect()
    }
    apkFile
}

private fun validateDownloadedUpdateApk(context: Context, apkFile: File) {
    if (!apkFile.isFile || apkFile.length() < MIN_UPDATE_APK_BYTES) {
        throw IllegalStateException("Arquivo recebido não parece ser um APK válido.")
    }

    val packageManager = context.packageManager
    val downloadedInfo = packageManager.getPackageArchiveInfo(
        apkFile.absolutePath,
        packageInfoSignatureFlags()
    ) ?: throw IllegalStateException("Não foi possível validar o APK baixado.")

    if (downloadedInfo.packageName != context.packageName) {
        throw IllegalStateException("APK baixado pertence a outro aplicativo.")
    }

    val installedInfo = packageManager.getPackageInfo(
        context.packageName,
        packageInfoSignatureFlags()
    )
    val downloadedSigners = downloadedInfo.signingCertificateSha256Digests()
    val installedSigners = installedInfo.signingCertificateSha256Digests()
    if (downloadedSigners.isEmpty() || installedSigners.isEmpty()) {
        throw IllegalStateException("Assinatura do APK não pode ser validada.")
    }
    if (downloadedSigners.intersect(installedSigners).isEmpty()) {
        throw IllegalStateException("Assinatura do APK não confere com a versão instalada.")
    }
}

@Suppress("DEPRECATION")
private fun packageInfoSignatureFlags(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        PackageManager.GET_SIGNATURES
    }
}

@Suppress("DEPRECATION")
private fun PackageInfo.signingCertificateSha256Digests(): Set<String> {
    val appSigners = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        signingInfo?.apkContentsSigners?.toList().orEmpty()
    } else {
        signatures?.toList().orEmpty()
    }
    return appSigners
        .map { signature -> signature.toByteArray().sha256Hex() }
        .toSet()
}

private fun ByteArray.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(this)
    return digest.joinToString(separator = "") { byte -> "%02X".format(byte) }
}

private const val MIN_UPDATE_APK_BYTES = 1_000_000L
private const val MAX_UPDATE_APK_BYTES = 100_000_000L

private fun String.prompt(): String {
    return when (this) {
        "nomeacao" -> "Mostre a figura e marque a resposta da criança."
        "repeticao" -> "Toque o som e marque se a criança repetiu corretamente."
        "discriminacao" -> "Toque os sons e marque se a criança discriminou corretamente."
        "leitura" -> "Mostre o texto; após a resposta, revele a figura."
        else -> "Marque o desempenho da criança."
    }
}
