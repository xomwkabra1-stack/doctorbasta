package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.security.SecureKeyManager
import com.example.ui.theme.TealPrimary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val maskedKey by viewModel.maskedApiKey.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingKey.collectAsStateWithLifecycle()
    val testResultMessage by viewModel.testResultMessage.collectAsStateWithLifecycle()
    val testSuccess by viewModel.testSuccess.collectAsStateWithLifecycle()

    var inputKey by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var showClearKeyDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "ڕێکخستنەکان و کلیلی API ⚙️",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.CHAT) },
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "گەڕانەوە"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            modifier = modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Security Explanation Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "پاراستنی بەهێز لە ڕێگەی Android Keystore",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "کلیلی تایبەتی Gemini API بە کۆدکردنی AES-256 GCM لەسەر مۆبایلەکەت دەپارێزرێت. کلیلەکە هەرگیز لە کۆددا نانووسرێت و بۆ هیچ لایەنێکی تر نانێردرێت.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // API Key Configuration Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "کلیلی Gemini API",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (maskedKey != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "کلیل بە سەرکەوتوویی پارێزراوە:",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                                        )
                                        Text(
                                            text = maskedKey ?: "",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = inputKey,
                            onValueChange = {
                                inputKey = it
                                viewModel.clearTestStatus()
                            },
                            label = { Text("کلیلی نوێ دابنێ (AIza...)") },
                            placeholder = { Text("کلیلی Gemini لێرە بنووسە یان پیست بکە") },
                            singleLine = true,
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isKeyVisible) "شاردنەوەی کلیل" else "پیشاندانی کلیل"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (inputKey.isNotBlank()) {
                                        viewModel.saveApiKey(inputKey)
                                        inputKey = ""
                                    }
                                },
                                enabled = inputKey.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("save_api_key_button")
                            ) {
                                Text("پاشەکەوتکردن")
                            }

                            if (maskedKey != null) {
                                OutlinedButton(
                                    onClick = { showClearKeyDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("clear_api_key_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("سڕینەوە")
                                }
                            }
                        }
                    }
                }

                // Model Selection Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "مۆدێلی پێشنیازکراوی Gemini",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "مۆدێلی زیرەکی دەستکرد دیاری بکە بۆ وەڵامەکانی دکتۆر بەستە:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SecureKeyManager.SUPPORTED_MODELS.forEach { modelName ->
                                val isSelected = modelName == selectedModel
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setSelectedModel(modelName) },
                                    label = {
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(
                                                text = modelName,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 14.sp
                                            )
                                            val desc = when (modelName) {
                                                "gemini-2.5-flash" -> "خێرا، ژیر و هاوسەنگ (پێشنیازکراوە)"
                                                "gemini-2.5-pro" -> "شیکاریی قووڵ و وەڵامی تێروتەسەل"
                                                "gemini-1.5-flash" -> "خێرایی بەرز"
                                                "gemini-2.0-flash" -> "نەوەی نوێی ۲.۰"
                                                else -> ""
                                            }
                                            if (desc.isNotBlank()) {
                                                Text(
                                                    text = desc,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("model_chip_$modelName")
                                )
                            }
                        }
                    }
                }

                // Connection Test Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "تاقیکردنەوەی پەیوەندی و کارکردن",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "بپشکنە ئایا کلیلەکەت بە تەواوی ئامادەیە بۆ گفتوگۆ لەگەڵ دکتۆر بەستە:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.testApiKey(inputKey.ifBlank { null })
                            },
                            enabled = !isTesting && (maskedKey != null || inputKey.isNotBlank()),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("test_connection_button")
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("خەریکی تاقیکردنەوەیە...")
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تاقیکردنەوەی کلیلی API")
                            }
                        }

                        // Test result feedback banner
                        if (testResultMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val isSuccess = testSuccess == true
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (isSuccess) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = testResultMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isSuccess) Color(0xFF166534) else Color(0xFF991B1B),
                                            lineHeight = 18.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Reset Settings Option
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("reset_settings_button")
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("سڕینەوە و گەڕاندنەوەی هەموو ڕێکخستنەکان")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Confirmation Dialogs
        if (showClearKeyDialog) {
            AlertDialog(
                onDismissRequest = { showClearKeyDialog = false },
                title = { Text("سڕینەوەی کلیلی API ⚠️") },
                text = { Text("ئایا دڵنیایت دەتەوێت کلیلی پاشەکەوتکراوی Gemini بسڕیتەوە لەسەر ئەم ئامێرە؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearKeyDialog = false
                            viewModel.clearApiKey()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("سڕینەوە")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearKeyDialog = false }) {
                        Text("پەشیمانبوونەوە")
                    }
                }
            )
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("گەڕاندنەوە بۆ دۆخی سەرەتا 🔄") },
                text = { Text("ئایا دڵنیایت دەتەوێت هەموو ڕێکخستنەکان و کلیلی پاشەکەوتکراو بسڕیتەوە؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetDialog = false
                            viewModel.resetSettings()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("بەڵێ، گەڕاندنەوە")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("پەشیمانبوونەوە")
                    }
                }
            )
        }
    }
}
