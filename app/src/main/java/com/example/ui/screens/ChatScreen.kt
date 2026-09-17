package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import com.example.viewmodel.AppScreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AboutDoctorDialog
import com.example.ui.components.BreathingExerciseDialog
import com.example.ui.components.ChatBubble
import com.example.ui.components.EmergencyDialog
import com.example.ui.components.MoodSelectorBar
import com.example.ui.theme.CounselorBubbleBorderLight
import com.example.ui.theme.CounselorBubbleLight
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.TealContainerDark
import com.example.ui.theme.TealContainerLight
import com.example.ui.theme.TealPrimary
import com.example.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val showBreathing by viewModel.showBreathingDialog.collectAsStateWithLifecycle()
    val showEmergency by viewModel.showEmergencyDialog.collectAsStateWithLifecycle()
    val showAbout by viewModel.showAboutDialog.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive or when loading starts
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Kurdish is an RTL language - provide RTL Layout Direction
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { viewModel.openAboutDialog() }
                                .padding(vertical = 4.dp)
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_dr_basta),
                                    contentDescription = "دکتۆر بەستە",
                                    modifier = Modifier.size(42.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "دکتۆر بەستە",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "🧠", fontSize = 14.sp)
                                }
                                Text(
                                    text = if (isLoading) "دکتۆر بیردەکاتەوە..." else "ڕاوێژکاری دەروونی • ئامادەیە",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isLoading) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    },
                    actions = {
                        // New Conversation button
                        IconButton(
                            onClick = { showNewChatDialog = true },
                            modifier = Modifier.testTag("new_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = "گفتوگۆی نوێ",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Quick 4-7-8 Breathing Exercise button
                        IconButton(
                            onClick = { viewModel.openBreathingDialog() },
                            modifier = Modifier.testTag("breathing_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = "هێمنبوونەوە",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Emergency crisis info button
                        IconButton(
                            onClick = { viewModel.openEmergencyDialog() },
                            modifier = Modifier.testTag("emergency_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "فریاگوزاری",
                                tint = EmergencyRed
                            )
                        }

                        // Settings button
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                            modifier = Modifier.testTag("settings_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "ڕێکخستنەکان",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Overflow Menu
                        IconButton(
                            onClick = { showMenu = !showMenu },
                            modifier = Modifier.testTag("menu_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "زیاتر"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("ڕێکخستنەکان و کلیلی API ⚙️") },
                                onClick = {
                                    showMenu = false
                                    viewModel.navigateTo(AppScreen.SETTINGS)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("گفتوگۆی نوێ ➕") },
                                onClick = {
                                    showMenu = false
                                    showNewChatDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("ڕاهێنانی هەناسەدان 🧘") },
                                onClick = {
                                    showMenu = false
                                    viewModel.openBreathingDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("دەربارەی دکتۆر بەستە ℹ️") },
                                onClick = {
                                    showMenu = false
                                    viewModel.openAboutDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("سڕینەوەی چات 🗑️") },
                                onClick = {
                                    showMenu = false
                                    viewModel.clearChat()
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("top_bar")
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    // Quick moods/topics bar
                    MoodSelectorBar(
                        moods = viewModel.moodOptions,
                        onMoodSelected = { viewModel.selectMood(it) }
                    )

                    // Message input field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { viewModel.onInputTextChanged(it) },
                            placeholder = {
                                Text(
                                    text = "بە کوردی پرسیار یان کێشەکەت بنووسە...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input_field"),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank() && !isLoading) {
                                        viewModel.sendMessage()
                                    }
                                }
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Send Button
                        Surface(
                            shape = CircleShape,
                            color = if (inputText.isNotBlank() && !isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = if (inputText.isNotBlank()) 2.dp else 0.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(enabled = inputText.isNotBlank() && !isLoading) {
                                    viewModel.sendMessage()
                                }
                                .testTag("send_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "ناردن",
                                        tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 8.dp
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chat_messages_list")
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        onRegenerate = { viewModel.regenerateResponse(it) }
                    )
                }

                if (isLoading) {
                    item {
                        TypingIndicatorBubble()
                    }
                }
            }
        }

        // Dialogs
        if (showNewChatDialog) {
            AlertDialog(
                onDismissRequest = { showNewChatDialog = false },
                title = {
                    Text(
                        text = "دەستپێکردنی گفتوگۆی نوێ 🧠",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = "ئایا دڵنیایت دەتەوێت گفتوگۆیەکی نوێ دەست پێبکەیت؟ چاتەکە پاکدەکرێتەوە و لە سەرەتاوە لەگەڵ دکتۆر بەستە دەستپێدەکەیتەوە.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNewChatDialog = false
                            viewModel.clearChat()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("بەڵێ، دەستپێکردنەوە")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewChatDialog = false }) {
                        Text("پەشیمانبوونەوە")
                    }
                }
            )
        }

        if (showBreathing) {
            BreathingExerciseDialog(onDismiss = { viewModel.closeBreathingDialog() })
        }

        if (showEmergency) {
            EmergencyDialog(onDismiss = { viewModel.closeEmergencyDialog() })
        }

        if (showAbout) {
            AboutDoctorDialog(onDismiss = { viewModel.closeAboutDialog() })
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    val transition = rememberInfiniteTransition(label = "dots")
    val alpha1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("typing_indicator"),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_dr_basta),
                contentDescription = "دکتۆر بەستە",
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دکتۆر بەستە بیردەکاتەوە",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(6.dp).alpha(alpha1).background(MaterialTheme.colorScheme.primary, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(6.dp).alpha(alpha2).background(MaterialTheme.colorScheme.primary, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(6.dp).alpha(alpha3).background(MaterialTheme.colorScheme.primary, CircleShape))
            }
        }
    }
}
