package at.websters.bebetter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.websters.bebetter.data.ApiClient
import at.websters.bebetter.ui.theme.BeBetterTokens
import kotlinx.coroutines.launch

// Deep-link targets: friend invite accept/decline, challenge invite, password reset.

@Composable
fun FriendAcceptScreen(token: String, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Friend invite", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            done ?: "Someone invited you to be friends on BeBetter.",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        if (done == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        busy = true; err = null
                        scope.launch {
                            val r = runCatching { ApiClient.get().acceptFriendLink(mapOf("token" to token)) }
                            busy = false
                            if (r.isSuccess) done = "You are now friends!"
                            else err = "This invite is invalid or already used."
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
                ) { Text("Accept") }
                OutlinedButton(
                    onClick = {
                        busy = true; err = null
                        scope.launch {
                            runCatching { ApiClient.get().declineFriendLink(mapOf("token" to token)) }
                            busy = false
                            done = "Invite declined."
                        }
                    },
                    enabled = !busy
                ) { Text("Decline") }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onDone) { Text("Continue") }
        }
    }
}

@Composable
fun ChallengeInviteScreen(token: String, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(token) {
        val ok = runCatching { ApiClient.get().challengeInvite(token) }.isSuccess
        if (ok) info = "You have been challenged!"
        else err = "This invite is invalid or expired."
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Challenge invite", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            done ?: info ?: "Checking invite…",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        if (done == null && err == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        busy = true; err = null
                        scope.launch {
                            val r = runCatching { ApiClient.get().acceptChallengeInvite(token) }
                            busy = false
                            if (r.isSuccess) done = "Challenge accepted — good luck!"
                            else err = "Could not accept this challenge."
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
                ) { Text("Accept") }
                OutlinedButton(onClick = onDone, enabled = !busy) { Text("Later") }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onDone) { Text("Continue") }
        }
    }
}

@Composable
fun ResetPasswordScreen(token: String, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Set new password", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (done) {
            Text("Password updated — you can sign in now.", fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDone) { Text("Sign in") }
        } else {
            OutlinedTextField(
                value = pw, onValueChange = { pw = it }, placeholder = { Text("New password (min 6 chars)") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = pw2, onValueChange = { pw2 = it }, placeholder = { Text("Repeat new password") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
            )
            err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            Button(
                onClick = {
                    if (pw.length < 6) { err = "Password must be at least 6 characters"; return@Button }
                    if (pw != pw2) { err = "Passwords do not match"; return@Button }
                    busy = true; err = null
                    scope.launch {
                        val r = runCatching { ApiClient.get().resetPassword(mapOf("token" to token, "password" to pw)) }
                        busy = false
                        if (r.isSuccess) done = true
                        else err = "This link is invalid or expired."
                    }
                },
                enabled = !busy && pw.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
            ) { Text(if (busy) "Saving…" else "Set password") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDone) { Text("Cancel") }
        }
    }
}
