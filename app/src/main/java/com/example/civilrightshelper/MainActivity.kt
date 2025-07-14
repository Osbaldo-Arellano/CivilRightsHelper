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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.civilrightshelper.ui.theme.CivilRightsHelperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CivilRightsHelperTheme {
                var currentScreen by remember { mutableStateOf("chat") }

                when (currentScreen) {
                    "chat" -> ChatScreen(onNavigateToInfo = { currentScreen = "info" })
                    "info" -> InfoScreen(onBack = { currentScreen = "chat" })
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

        // Populate preview/test messages only once
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
            // Chat history UI
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

            // Input and Send Button
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
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

                            coroutineScope.launch {
                                val aiResponse = fetchLLMResponse(userMsg)
                                messages.add("ai" to aiResponse)
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
    onBack: () -> Unit = {}
) {
    var selectedLanguage by remember { mutableStateOf("English") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("App Info") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {

            Text(
                text = "This is a personal-use app. Legal info is not verified.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Select App Language:",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LanguageOption(
                label = "English",
                selected = selectedLanguage == "English",
                onClick = { selectedLanguage = "English" }
            )

            LanguageOption(
                label = "Spanish",
                selected = selectedLanguage == "Spanish",
                onClick = { selectedLanguage = "Spanish" }
            )

            LanguageOption(
                label = "Russian",
                selected = selectedLanguage == "Russian",
                onClick = { selectedLanguage = "Russian" }
            )
        }
    }
}

@Composable
fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}



@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChatScreenPreview() {
    val sampleMessages = listOf(
        "user" to "Can I record the police?",
        "ai" to "Yes, in Oregon you can record as long as you don't interfere.",
        "user" to "What do I say if I'm being detained?",
        "ai" to "You can ask: 'Am I being detained, or am I free to go?'"
    )
    CivilRightsHelperTheme {
        ChatScreen(initialMessages = sampleMessages)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun InfoScreenPreview() {
    CivilRightsHelperTheme {
        InfoScreen()
    }
}


suspend fun fetchLLMResponse(userMessage: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://10.0.2.2:3000/ask") // emulator = 10.0.2.2

            val jsonBody = JSONObject()
            jsonBody.put("query", userMessage)

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody.toString())
            }

            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val responseJson = JSONObject(response)
            responseJson.optString("answer", "No answer from server.")

        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.localizedMessage}"
        }
    }
}



