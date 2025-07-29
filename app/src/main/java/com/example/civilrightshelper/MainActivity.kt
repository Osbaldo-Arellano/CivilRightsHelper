package com.example.civilrightshelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.civilrightshelper.ui.theme.CivilRightsHelperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CivilRightsHelperTheme {
                var currentScreen by remember { mutableStateOf("chat") }
                var selectedLanguage by remember { mutableStateOf("English") }

                when (currentScreen) {
                    "chat" -> ChatScreen(
                        selectedLanguage = selectedLanguage,
                        onNavigateToInfo = { currentScreen = "info" }
                    )
                    "info" -> InfoScreen(
                        selectedLanguage = selectedLanguage,
                        onLanguageChange = { selectedLanguage = it },
                        onBack = { currentScreen = "chat" }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    initialMessages: List<Pair<String, String>> = emptyList(),
    selectedLanguage: String,
    onNavigateToInfo: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Civil Rights Helper") },
            actions = {
                IconButton(onClick = onNavigateToInfo) {
                    Icon(Icons.Default.Info, contentDescription = "Info")
                }
            }
        )

        var query by remember { mutableStateOf(TextFieldValue("")) }
        val messages = remember { mutableStateListOf<Pair<String, String>>() }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            if (messages.isEmpty()) messages.addAll(initialMessages)
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(10.dp)
                .padding(top = 60.dp)
                .padding(bottom = 30.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                for ((sender, message) in messages) {
                    val isUser = sender == "user"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            tonalElevation = 2.dp,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(12.dp),
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Type your question") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Button(
                    onClick = {
                        val userMsg = query.text.trim()
                        if (userMsg.isNotEmpty()) {
                            messages.add("user" to userMsg)
                            query = TextFieldValue("")

                            val aiMsg = StringBuilder()
                            messages.add("ai" to "")

                            coroutineScope.launch {
                                fetchLLMStream(userMsg, selectedLanguage) { chunk ->
                                    aiMsg.append(chunk)
                                    if (messages.isNotEmpty() && messages.last().first == "ai") {
                                        messages[messages.lastIndex] = "ai" to aiMsg.toString()
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("App Info") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "This is a personal-use app. Legal info is not verified.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Select App Language:", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            listOf("English", "Spanish").forEach { lang ->
                LanguageOption(
                    label = lang,
                    selected = selectedLanguage == lang,
                    onClick = { onLanguageChange(lang) }
                )
            }
        }
    }
}

@Composable
fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

suspend fun fetchLLMStream(userMessage: String, language: String, onChunk: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("query", userMessage)
                put("language", language)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("http://10.0.2.2:3000/ask")
                .post(body)
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS) // Infinite stream
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    onChunk("Error: HTTP ${response.code}")
                    return@use
                }

                val reader = response.body?.charStream()
                val buffer = CharArray(256)
                var read: Int

                if (reader != null) {
                    while (reader.read(buffer).also { read = it } != -1) {
                        val chunk = String(buffer, 0, read)
                        if (chunk.contains("[[END_OF_STREAM]]")) break
                        if (chunk.isNotBlank()) {
                            onChunk(chunk)
                        }
                    }
                } else {
                    onChunk("Error: No response body.")
                }
            }

        } catch (e: Exception) {
            onChunk("Error: ${e.localizedMessage}")
        }
    }
}
