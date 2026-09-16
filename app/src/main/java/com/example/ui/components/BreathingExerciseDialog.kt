package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.TealContainerLight
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.delay

@Composable
fun BreathingExerciseDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRunning by remember { mutableStateOf(true) }
    var phase by remember { mutableIntStateOf(0) } // 0: Inhale (4s), 1: Hold (7s), 2: Exhale (8s)
    var secondsLeft by remember { mutableIntStateOf(4) }
    var cycleCount by remember { mutableIntStateOf(1) }

    LaunchedEffect(isRunning, phase, secondsLeft) {
        if (!isRunning) return@LaunchedEffect
        delay(1000)
        if (secondsLeft > 1) {
            secondsLeft -= 1
        } else {
            when (phase) {
                0 -> {
                    phase = 1
                    secondsLeft = 7
                }
                1 -> {
                    phase = 2
                    secondsLeft = 8
                }
                2 -> {
                    phase = 0
                    secondsLeft = 4
                    cycleCount += 1
                }
            }
        }
    }

    val phaseText = when (phase) {
        0 -> "هەناسە هەڵمژە لە لووتتەوە..."
        1 -> "هەناسەکەت ڕابگرە لە ناختدا..."
        else -> "هێواش لە دەمتەوە بیدەرەوە..."
    }

    val phaseColor = when (phase) {
        0 -> TealPrimary
        1 -> Color(0xFFD97706)
        else -> Color(0xFF0284C7)
    }

    val targetScale = when (phase) {
        0 -> 1.25f
        1 -> 1.25f
        else -> 0.85f
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("breathing_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "داخستن")
                    }
                    Text(
                        text = "ڕاهێنانی ٤-٧-٨ی هێوربوونەوە 🧘",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Text(
                    text = "ئامۆژگاریی دکتۆر بەستە: ئەم ڕاهێنانە لێدانی دڵ هێمن دەکاتەوە و دڵەڕاوکێ و زۆر بیرکردنەوە کەمدەکاتەوە.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pulsing Breathing Circle
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(targetScale)
                        .background(phaseColor.copy(alpha = 0.15f), CircleShape)
                        .border(3.dp, phaseColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$secondsLeft",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = phaseColor
                        )
                        Text(
                            text = "چرکە",
                            fontSize = 13.sp,
                            color = phaseColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = phaseText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = phaseColor,
                        fontSize = 16.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "سوڕی ژمارە: $cycleCount",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { isRunning = !isRunning },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (isRunning) "ڕاگرتنی کاتی" else "دەستپێکردنەوە")
                    }

                    OutlinedButton(
                        onClick = {
                            phase = 0
                            secondsLeft = 4
                            cycleCount = 1
                            isRunning = true
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "سەرلەنوێ")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "سەرلەنوێ")
                    }
                }
            }
        }
    }
}
