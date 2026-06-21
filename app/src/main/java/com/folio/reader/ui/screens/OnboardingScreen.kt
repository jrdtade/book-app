package com.folio.reader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.folio.reader.ui.UserViewModel
import com.folio.reader.ui.folioViewModel
import com.folio.reader.ui.theme.Ink3

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        when (step) {
            0 -> WelcomeStep(onGetStarted = { step = 1 })
            else -> SetupStep(onDone = onDone)
        }
    }
}

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Welcome to Folio", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "Your private, offline EPUB reader. No account, no tracking — just your books.",
            style = MaterialTheme.typography.bodyLarge,
            color = Ink3,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onGetStarted, modifier = Modifier.fillMaxWidth()) { Text("Get Started") }
    }
}

@Composable
private fun SetupStep(onDone: () -> Unit) {
    val vm: UserViewModel = folioViewModel()
    var name by remember { mutableStateOf("") }
    var goalText by remember { mutableStateOf("24") }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("A little about you", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        Text("What should we call you?", style = MaterialTheme.typography.labelLarge, color = Ink3)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Your name") },
            singleLine = true,
        )
        Spacer(Modifier.height(20.dp))

        Text("Annual reading goal", style = MaterialTheme.typography.labelLarge, color = Ink3)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = goalText,
            onValueChange = { v -> if (v.all { it.isDigit() }) goalText = v },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Books this year") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                val goal = goalText.toIntOrNull()?.coerceIn(1, 999) ?: 24
                vm.completeOnboarding(name.trim().ifBlank { "Reader" }, goal)
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Start Reading") }
    }
}
