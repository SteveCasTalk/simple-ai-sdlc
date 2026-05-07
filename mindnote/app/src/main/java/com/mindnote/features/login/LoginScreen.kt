package com.mindnote.features.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Placeholder login screen — wired up so the nav graph has a destination for signed-out
 * users. The real login form (with the auth API client and field validation) lands in a
 * separate slice; this file exists only so #18 can land independently.
 */
@Composable
fun LoginScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Login")
        Text("(placeholder — full login form arrives in a follow-up slice)")
    }
}
