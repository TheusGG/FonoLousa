package com.fonolousa.app.ui

import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.fonolousa.app.ui.theme.ChalkGreen
import com.fonolousa.app.ui.theme.ChalkGreenAlt
import com.fonolousa.app.ui.theme.ChalkShadow
import com.fonolousa.app.ui.theme.ChalkWhite
import com.fonolousa.app.update.UpdateChecker
import com.fonolousa.app.update.UpdateState
import kotlinx.coroutines.launch

@Composable
fun FonoLousaApp(
    repository: DataRepository,
    audioPlayer: AudioPlayer
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                categorias = repository.categorias(),
                onCategoryClick = { navController.navigate("levels/${it.id}") },
                onUpdateClick = { navController.navigate("update") }
            )
        }
        composable("update") {
            UpdateScreen(onBack = { navController.popBackStack() })
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
                audioPlayer = audioPlayer,
                categoria = category,
                nivel = nivel,
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
                audioPlayer = audioPlayer,
                categoria = category,
                nivel = nivel,
                initialIndex = index,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    categorias: List<Categoria>,
    onCategoryClick: (Categoria) -> Unit,
    onUpdateClick: () -> Unit
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
                            text = "Baixar nova versao",
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
    audioPlayer: AudioPlayer,
    categoria: Categoria,
    nivel: Nivel,
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
                        audioPlayer = audioPlayer,
                        item = item,
                        level = nivel.nivel,
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
    audioPlayer: AudioPlayer,
    item: ItemFono,
    level: Int,
    borderColor: Color,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
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
    }
}

@Composable
private fun ItemViewerScreen(
    repository: DataRepository,
    audioPlayer: AudioPlayer,
    categoria: Categoria,
    nivel: Nivel,
    initialIndex: Int,
    onBack: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, nivel.itens.lastIndex)) }
    var pulse by remember { mutableStateOf(false) }
    val item = nivel.itens[currentIndex]
    val imageScale by animateFloatAsState(
        targetValue = if (pulse) 1.03f else 1f,
        animationSpec = tween(160),
        finishedListener = { pulse = false },
        label = "soundPulse"
    )

    LaunchedEffect(item.id) {
        audioPlayer.play(item.arquivoSom, item.audioText(nivel.nivel))
        pulse = true
    }

    BlackboardScreen {
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
                IconButton(
                    onClick = {
                        audioPlayer.play(item.arquivoSom, item.audioText(nivel.nivel))
                        pulse = true
                    },
                    modifier = Modifier
                        .padding(top = 8.dp)
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
        is UpdateState.Available -> {
            "Nova versao disponivel: ${state.manifest.versionName}\n${state.manifest.notes}"
        }
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
