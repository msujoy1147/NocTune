package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.cos
import kotlin.math.sin

// Ten standard audio bands requested
val EQUALIZER_BANDS = listOf(
    "32Hz", "64Hz", "125Hz", "250Hz", "500Hz", "1KHz", "2KHz", "4KHz", "8KHz", "16KHz"
)

// Map of band index to standard preset gains (-12dB to +12dB)
val EQUALIZER_PRESETS = mapOf(
    "Normal" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    "Pop" to listOf(-1f, 1.5f, 2.5f, 3f, 1.5f, -0.5f, -1f, -1f, -0.5f, 0f),
    "Rock" to listOf(4f, 3f, -2f, -4f, -1f, 1.5f, 3f, 4f, 4.5f, 4.5f),
    "Jazz" to listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f),
    "Classical" to listOf(3.5f, 2.5f, 1.5f, 1f, -1f, -1f, 0f, 1.5f, 2.5f, 3.5f),
    "Bass Boost" to listOf(8f, 6.5f, 5f, 3f, 1f, 0f, 0f, 0f, 0f, 0f),
    "Vocal" to listOf(-2f, -1.5f, -1f, 1f, 4f, 4f, 3f, 1.5f, 0f, -1f),
    "Dance" to listOf(4.5f, 6f, 4f, 0f, 1.5f, 3f, 4.5f, 4f, 1f, 0f),
    "Electronic" to listOf(4f, 3.5f, 1f, 0f, -1.5f, 2f, 1f, 1.5f, 4f, 4.5f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerPanel(
    isPlaying: Boolean,
    isNightMode: Boolean,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    // Equalizer state holding the 10 band gains
    var activePreset by remember { mutableStateOf("Normal") }
    var bandGains by remember { 
        mutableStateOf(EQUALIZER_PRESETS["Normal"] ?: List(10) { 0f }) 
    }

    // Toggle states for luxurious effects
    var bassBoostEnabled by remember { mutableStateOf(true) }
    var virtualizerEnabled by remember { mutableStateOf(false) }
    var reverbEnabled by remember { mutableStateOf(false) }
    var surroundEnabled by remember { mutableStateOf(true) }
    var loudnessEnabled by remember { mutableStateOf(false) }

    // Helper to load presets
    fun loadPreset(presetName: String) {
        activePreset = presetName
        EQUALIZER_PRESETS[presetName]?.let {
            bandGains = it
        }
        Toast.makeText(context, "Preset loaded: $presetName", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        com.example.ui.theme.NocTuneTheme(darkTheme = isNightMode) {
            // Core high-fidelity theme colors matching current color scheme
            val appColors = com.example.ui.theme.LocalAppColors.current
            val deepEspresso = appColors.deepEspresso
            val darkMocha = appColors.darkMocha
            val coffeeBrown = appColors.coffeeBrown
            val softLatte = appColors.softLatte
            val warmCream = appColors.warmCream
            val secondaryText = appColors.secondaryText
            val isNight = appColors.isNight
            val neonTeal = if (isNight) Color(0xFF00F5D4) else Color(0xFF00B4D8)

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(deepEspresso),
                color = Color.Transparent
            ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                coffeeBrown.copy(alpha = 0.25f),
                                deepEspresso
                            ),
                            center = Offset(400f, 300f)
                        )
                    )
            ) {
                // Main Dialog Backing & Scrollable Panel
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                ) {
                    // Header Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(coffeeBrown.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = coffeeBrown,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Studio EQ Engine",
                                    color = warmCream,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Hi-Fi Studio Mastering Suite",
                                    color = secondaryText,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Close button with ripple accessibility
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(warmCream.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Equalizer panel",
                                tint = warmCream
                            )
                        }
                    }

                    // Content scrollable to support any device screens smoothly
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Section 1: Circular Visualizer Dashboard
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(darkMocha.copy(alpha = 0.6f))
                                .border(1.dp, warmCream.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier.size(160.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularAudioVisualizer(
                                        isPlaying = isPlaying,
                                        primaryColor = coffeeBrown,
                                        accentColor = neonTeal,
                                        activePreset = activePreset
                                    )
                                    
                                    // Visualizer Center Dashboard Note
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = softLatte,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = activePreset.uppercase(),
                                            color = warmCream,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = if (isPlaying) "32-BIT ENCODER" else "IDLE STANDARD",
                                            color = if (isPlaying) neonTeal else secondaryText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Section 2: Preset Scroller Chips
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "PRESET SOUND PROFILES",
                                color = softLatte,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                            ) {
                                val presets = listOf("Normal", "Pop", "Rock", "Jazz", "Classical", "Bass Boost", "Vocal", "Dance", "Electronic", "Custom")
                                items(presets) { preset ->
                                    val isSelected = activePreset == preset
                                    val chipBg = if (isSelected) {
                                        Brush.horizontalGradient(listOf(coffeeBrown, softLatte))
                                    } else {
                                        Brush.horizontalGradient(listOf(darkMocha, darkMocha))
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(chipBg)
                                            .border(
                                                1.dp,
                                                if (isSelected) neonTeal.copy(alpha = 0.5f) else warmCream.copy(alpha = 0.12f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                if (preset == "Custom") {
                                                    activePreset = "Custom"
                                                    Toast.makeText(context, "Adjust sliders directly", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    loadPreset(preset)
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = preset,
                                            color = if (isSelected) Color.White else warmCream,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Section 3: Premium Frequency DJ Faders Sliders
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "10-BAND GRAPHIC CONSOLE",
                                    color = softLatte,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Range: -12dB to +12dB",
                                    color = secondaryText,
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(darkMocha.copy(alpha = 0.6f))
                                    .border(1.dp, warmCream.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                                    .padding(vertical = 16.dp, horizontal = 12.dp)
                            ) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    items(10) { index ->
                                        val bandName = EQUALIZER_BANDS[index]
                                        val gain = bandGains[index]

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(48.dp)
                                        ) {
                                            // dB Value Display Indicator
                                            Text(
                                                text = "${if (gain > 0) "+" else ""}${String.format("%.1f", gain)}",
                                                color = if (gain != 0f) neonTeal else secondaryText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )

                                            // Custom Vertical Slider
                                            PremiumVerticalSlider(
                                                value = gain,
                                                rangeMin = -12f,
                                                rangeMax = 12f,
                                                activeColor = coffeeBrown,
                                                indicatorColor = neonTeal,
                                                onValueChange = { newValue ->
                                                    activePreset = "Custom"
                                                    val updated = bandGains.toMutableList()
                                                    updated[index] = newValue
                                                    bandGains = updated
                                                }
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Band frequency label
                                            Text(
                                                text = bandName,
                                                color = warmCream,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Section 4: Sound FX Toggles Panel
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "STUDIO RACK FX MODULES",
                                color = softLatte,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // 5 toggles listed in vertical layout or paired in elegant grid columns
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                val switches = listOf(
                                    ToggleFXData("Bass Boost", "Supercharge low-end sub frequencies", bassBoostEnabled) { bassBoostEnabled = it },
                                    ToggleFXData("Hi-Fi Virtualizer", "Simulate wide spatial acoustic fields", virtualizerEnabled) { virtualizerEnabled = it },
                                    ToggleFXData("Studio Reverb", "Warm room ambience and classic decays", reverbEnabled) { reverbEnabled = it },
                                    ToggleFXData("3D Surround Sound", "Enrich spatial immersive channels", surroundEnabled) { surroundEnabled = it },
                                    ToggleFXData("Master Loudness", "Dynamic loudness enhancement curves", loudnessEnabled) { loudnessEnabled = it }
                                )

                                switches.forEach { switch ->
                                    FxModuleCard(
                                        title = switch.title,
                                        description = switch.description,
                                        checked = switch.checked,
                                        onCheckedChange = switch.onCheckedChange,
                                        primaryColor = coffeeBrown,
                                        indicatorColor = neonTeal,
                                        secondaryText = secondaryText,
                                        tag = switch.title.lowercase().replace(" ", "_")
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

// Data class to wrap our switcher inputs cleanly
data class ToggleFXData(
    val title: String,
    val description: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

@Composable
fun FxModuleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    primaryColor: Color,
    indicatorColor: Color,
    secondaryText: Color,
    tag: String
) {
    val appColors = com.example.ui.theme.LocalAppColors.current
    val darkMocha = appColors.darkMocha
    val warmCream = appColors.warmCream

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (checked) indicatorColor.copy(alpha = 0.3f) else warmCream.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            ),
        color = darkMocha.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    color = warmCream,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = secondaryText,
                    fontSize = 11.sp
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = primaryColor,
                    uncheckedThumbColor = secondaryText,
                    uncheckedTrackColor = warmCream.copy(alpha = 0.12f)
                ),
                modifier = Modifier.testTag("eq_fx_switch_$tag")
            )
        }
    }
}

@Composable
fun CircularAudioVisualizer(
    isPlaying: Boolean,
    primaryColor: Color,
    accentColor: Color,
    activePreset: String,
    modifier: Modifier = Modifier
) {
    val appColors = com.example.ui.theme.LocalAppColors.current
    val deepEspresso = appColors.deepEspresso
    val infiniteTransition = rememberInfiniteTransition(label = "audio_visualizer")
    
    // Wave animation scaling factor synced with playing status
    val animFactor by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "frequency_scale"
    )

    // Pulse effects for ambient circles
    val innerPulseBg by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )

    Canvas(
        modifier = modifier
            .size(150.dp)
            .padding(10.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.width / 2.1f
        val innerRadius = size.width / 3.4f

        // Draw ambient glowing concentric rings
        drawCircle(
            color = primaryColor.copy(alpha = if (isPlaying) innerPulseBg else 0.03f),
            radius = outerRadius,
            center = center
        )
        drawCircle(
            color = accentColor.copy(alpha = if (isPlaying) 0.05f else 0.02f),
            radius = innerRadius * 1.2f,
            center = center
        )

        // Draw center solid steel console ring representation
        drawCircle(
            color = deepEspresso,
            radius = innerRadius,
            center = center
        )

        // Draw 36 radial bars around the console
        val barCount = 36
        for (i in 0 until barCount) {
            val angleDeg = (360f / barCount) * i
            val angleRad = Math.toRadians(angleDeg.toDouble())

            // Define bar heights that dynamically move and scale based on index & active features
            val baseHeight = 12f + (i % 6) * 5f
            val waveHeight = if (isPlaying) {
                // Generate simulated audio wave peaks using trigonometric curves
                val waveOffset = (sin(i.toDouble() * 0.5 + animFactor * 5.0) + 1.1) * baseHeight
                waveOffset.toFloat() * animFactor
            } else {
                2f + (sin(i.toDouble()) * 2f).toFloat() // static idle
            }

            // High-power preset modifications to visually match standard modes
            val presetAmplifier = when (activePreset) {
                "Bass Boost" -> if (i < 10 || i > 26) 1.5f else 0.8f
                "Vocal" -> if (i in 12..24) 1.4f else 0.7f
                "Rock" -> 1.3f
                else -> 1.0f
            }

            val finalBarHeight = waveHeight * presetAmplifier

            // Outer point and Inner point vectors
            val startX = center.x + (innerRadius + 4f) * cos(angleRad).toFloat()
            val startY = center.y + (innerRadius + 4f) * sin(angleRad).toFloat()
            val endX = center.x + (innerRadius + 4f + finalBarHeight) * cos(angleRad).toFloat()
            val endY = center.y + (innerRadius + 4f + finalBarHeight) * sin(angleRad).toFloat()

            // Alternating radial color gradient for DJ visual layout look
            val barColor = if (i % 2 == 0) primaryColor else accentColor

            drawLine(
                color = barColor.copy(alpha = if (isPlaying) 0.9f else 0.6f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun PremiumVerticalSlider(
    value: Float, // current dB scale
    rangeMin: Float,
    rangeMax: Float,
    activeColor: Color,
    indicatorColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalHeightDp = 180.dp
    val appColors = com.example.ui.theme.LocalAppColors.current
    val deepEspresso = appColors.deepEspresso
    val warmCream = appColors.warmCream

    Box(
        modifier = modifier
            .height(totalHeightDp)
            .width(40.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // DragAmount.y is positive downwards.
                    // Map height delta into value range
                    val totalHeightPx = size.height.toFloat()
                    if (totalHeightPx > 0f) {
                        val valueDelta = -(dragAmount.y / totalHeightPx) * (rangeMax - rangeMin)
                        val targetValue = (value + valueDelta).coerceIn(rangeMin, rangeMax)
                        onValueChange(targetValue)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(24.dp)
                .testTag("eq_vertical_fader_track")
        ) {
            val width = size.width
            val height = size.height
            val trackWidth = 4.dp.toPx()
            
            // Draw background track center line
            drawRoundRect(
                color = warmCream.copy(alpha = 0.15f),
                topLeft = Offset((width - trackWidth) / 2f, 0f),
                size = androidx.compose.ui.geometry.Size(trackWidth, height),
                cornerRadius = CornerRadius(trackWidth / 2f)
            )

            // Convert current dB value (-12 to +12) to fraction (0 to 1), where 0 is at bottom (fraction = 1 representing height-to-0)
            val fraction = (value - rangeMin) / (rangeMax - rangeMin)
            val activeY = height * (1f - fraction)

            // Draw active calibrated fader track with modern neon gradient
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(indicatorColor, activeColor),
                    startY = activeY,
                    endY = height
                ),
                topLeft = Offset((width - trackWidth) / 2f, activeY),
                size = androidx.compose.ui.geometry.Size(trackWidth, height - activeY),
                cornerRadius = CornerRadius(trackWidth / 2f)
            )

            // Draw DJ grid scale marks along sides of the track
            val scaleMarks = 7
            for (i in 0 until scaleMarks) {
                val markY = (height / (scaleMarks - 1)) * i
                val markWidth = if (i == scaleMarks / 2) 12.dp.toPx() else 6.dp.toPx()
                val alpha = if (i == scaleMarks / 2) 0.3f else 0.15f
                
                // Zero dB center gets a brighter anchor mark
                val markColor = if (i == scaleMarks / 2) indicatorColor.copy(alpha = 0.5f) else warmCream.copy(alpha = alpha)

                drawLine(
                    color = markColor,
                    start = Offset(0f, markY),
                    end = Offset(6.dp.toPx(), markY),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = markColor,
                    start = Offset(width - 6.dp.toPx(), markY),
                    end = Offset(width, markY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw the magnificent metallic circular DJ slider thumb knob
            val thumbRadius = 10.dp.toPx()
            
            // Draw outer neon glow ring around thumb
            drawCircle(
                color = indicatorColor.copy(alpha = 0.4e-1f + (fraction * 0.2f)),
                radius = thumbRadius * 1.5f,
                center = Offset(width / 2f, activeY)
            )

            // Outer capsule
            drawCircle(
                color = deepEspresso,
                radius = thumbRadius,
                center = Offset(width / 2f, activeY)
            )

            // Inner active center core
            drawCircle(
                color = indicatorColor,
                radius = thumbRadius * 0.4f,
                center = Offset(width / 2f, activeY)
            )
        }
    }
}
