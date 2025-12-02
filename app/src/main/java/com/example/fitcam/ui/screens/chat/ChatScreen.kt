package com.example.fitcam.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitcam.ui.theme.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FitCamCream)
    ) {
        // --- HEADER ---
        Surface(shadowElevation = 4.dp, color = FitCamCream) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(FitCamYellow),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AI", fontWeight = FontWeight.Bold, color = FitCamBlue)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("FitCam Coach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FitCamBlue)
                    Text("Online", style = MaterialTheme.typography.bodySmall, color = Color.Green)
                }
            }
        }

        // --- CHAT LIST ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
            
            if (isLoading) {
                item {
                    Text("Coach is typing...", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }

        // --- INPUT AREA ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask about your progress...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FitCamBlue,
                    unfocusedBorderColor = Color.LightGray
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) FitCamBlue else Color.Gray)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val bubbleColor = if (message.isUser) FitCamBlue else Color.White
    val textColor = if (message.isUser) Color.White else Color.Black
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            MarkdownText(
                text = message.text,
                color = textColor,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun MarkdownText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp
) {
    val styledText = buildAnnotatedString {
        // 1. Split berdasarkan baris untuk menangani List Item (*)
        val lines = text.split("\n")
        
        lines.forEachIndexed { index, line ->
            var currentLine = line.trim()
            
            // Deteksi Bullet Point (* atau - di awal kalimat)
            if (currentLine.startsWith("* ") || currentLine.startsWith("- ")) {
                append("•  ") // Ganti simbol markdown dengan Bullet asli yang rapi
                currentLine = currentLine.substring(2) // Hapus "* "
            } else if (index > 0) {
                append("\n") // Tambah enter jika bukan baris pertama
            }

            // 2. Deteksi BOLD (**teks**)
            // Kita split berdasarkan tanda "**"
            val parts = currentLine.split("**")
            
            parts.forEachIndexed { partIndex, part ->
                // Jika index ganjil (1, 3, 5...), itu adalah teks di dalam **...** (BOLD)
                if (partIndex % 2 == 1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(part)
                    }
                } else {
                    // Teks biasa
                    append(part)
                }
            }
        }
    }

    Text(
        text = styledText,
        color = color,
        modifier = modifier,
        fontSize = fontSize,
        lineHeight = 22.sp // Memberi jarak antar baris agar lebih enak dibaca
    )
}
