package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ChatMessage
import com.example.ui.theme.CounselorBubbleBorderDark
import com.example.ui.theme.CounselorBubbleBorderLight
import com.example.ui.theme.CounselorBubbleDark
import com.example.ui.theme.CounselorBubbleLight
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.UserBubbleDark
import com.example.ui.theme.UserBubbleLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(
    message: ChatMessage,
    onRegenerate: ((ChatMessage) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUser = message.isUser
    val timeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(if (isUser) "user_message_row" else "counselor_message_row"),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            // Dr. Basta Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_dr_basta),
                    contentDescription = "دکتۆر بەستە",
                    modifier = Modifier.size(38.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message Bubble Container
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            if (!isUser) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                ) {
                    Text(
                        text = "دکتۆر بەستە",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🧠",
                        fontSize = 12.sp
                    )
                    if (message.isCrisisWarning) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmergencyRed
                        ) {
                            Text(
                                text = "فریاگوزاری و سەلامەتی",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            val bubbleShape = if (isUser) {
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
            }

            val isDark = MaterialTheme.colorScheme.background.red < 0.2f
            val bubbleBg = when {
                message.isCrisisWarning -> if (isDark) Color(0xFF450A0A) else EmergencyRedContainer
                isUser -> if (isDark) UserBubbleDark else UserBubbleLight
                else -> if (isDark) CounselorBubbleDark else CounselorBubbleLight
            }

            val bubbleBorder = when {
                message.isCrisisWarning -> EmergencyRed
                !isUser -> if (isDark) CounselorBubbleBorderDark else CounselorBubbleBorderLight
                else -> Color.Transparent
            }

            val textColor = when {
                message.isCrisisWarning -> if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                isUser -> Color.White
                else -> MaterialTheme.colorScheme.onSurface
            }

            Surface(
                shape = bubbleShape,
                color = bubbleBg,
                shadowElevation = if (isUser) 1.dp else 0.5.dp,
                modifier = Modifier
                    .border(1.dp, bubbleBorder, bubbleShape)
                    .testTag(if (isUser) "user_bubble" else "counselor_bubble")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (message.isCrisisWarning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = EmergencyRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تکایە سەلامەتیت بپارێزە",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmergencyRed
                                )
                            )
                        }
                    }

                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = textColor,
                            lineHeight = 22.sp,
                            fontSize = 15.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = if (isUser) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        if (!isUser) {
                            Spacer(modifier = Modifier.width(6.dp))

                            if (onRegenerate != null && !message.isCrisisWarning) {
                                IconButton(
                                    onClick = { onRegenerate(message) },
                                    modifier = Modifier.size(24.dp).testTag("regenerate_button_${message.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "نوێکردنەوەی وەڵام",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Dr. Basta Advice", message.text)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "ئامۆژگارییەکە کۆپیکرا", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp).testTag("copy_button_${message.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "کۆپیکردن",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
