package at.websters.bebetter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.websters.bebetter.BeBetterApp
import at.websters.bebetter.data.*
import at.websters.bebetter.ui.components.BeBetterLogo
import at.websters.bebetter.ui.theme.BeBetterTokens
import at.websters.bebetter.ui.theme.isBeBetterDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LegalLinksRow() {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 24.dp)) {
        TextButton(onClick = { uri.openUri("https://bebetter.websters.at/terms") }, contentPadding = PaddingValues(4.dp)) { Text("Terms", fontSize = 12.sp) }
        Text("·", fontSize = 12.sp)
        TextButton(onClick = { uri.openUri("https://bebetter.websters.at/privacy") }, contentPadding = PaddingValues(4.dp)) { Text("Privacy", fontSize = 12.sp) }
        Text("·", fontSize = 12.sp)
        TextButton(onClick = { uri.openUri("https://bebetter.websters.at/imprint") }, contentPadding = PaddingValues(4.dp)) { Text("Imprint", fontSize = 12.sp) }
    }
}

@Composable
fun LoginScreen(onLoggedIn: (String, Boolean, Boolean) -> Unit, onRegister: () -> Unit, onForgot: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as BeBetterApp
    val session = app.session
    val scope = rememberCoroutineScope()
    val isDark = isBeBetterDark()
    var id by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var stay by remember { mutableStateOf(true) }
    var baseUrl by remember { mutableStateOf(SessionManager.DEFAULT_BASE_URL) }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var demoBusy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { baseUrl = session.getBaseUrl() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // top-right theme toggle like web absolute top-4 right-4
        IconButton(
            onClick = { scope.launch { val cur = session.getTheme(); session.saveTheme(if (cur == "light") "dark" else "light") } },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp).size(44.dp)
        ) {
            Icon(if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode, "Toggle theme", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(Modifier.widthIn(max = 384.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                // header gap-4 mb-8
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 32.dp)) {
                    BeBetterLogo(size = 56.dp, light = !isDark)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Welcome back", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
                        Text("Sign in to keep your streaks alive", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                // card p-6 sm:p-7 rounded-2xl space-y-4
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        err?.let {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f)), shape = RoundedCornerShape(8.dp)) {
                                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                            }
                        }
                        OutlinedTextField(
                            value = id, onValueChange = { id = it }, placeholder = { Text("Email or username", fontSize = 14.sp) },
                            singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BeBetterTokens.Accent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        OutlinedTextField(
                            value = pw, onValueChange = { pw = it }, placeholder = { Text("Password", fontSize = 14.sp) },
                            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BeBetterTokens.Accent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showPw = !showPw }, modifier = Modifier.size(44.dp)) {
                                    Icon(if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        )
                        var showServer by remember { mutableStateOf(false) }
                        TextButton(onClick = { showServer = !showServer }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (showServer) "Hide server settings" else "Server: " + baseUrl.take(40), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        if (showServer) {
                            OutlinedTextField(
                                value = baseUrl, onValueChange = { baseUrl = it }, placeholder = { Text("https://app.bebetter.websters.at", fontSize = 14.sp) },
                                singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BeBetterTokens.Accent,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = stay, onCheckedChange = { stay = it }, colors = CheckboxDefaults.colors(checkedColor = BeBetterTokens.Accent))
                            Text("Stay logged in", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                        }
                        Button(
                            onClick = {
                                busy = true; err = null
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        session.saveBaseUrl(baseUrl.ifBlank { SessionManager.DEFAULT_BASE_URL })
                                        ApiClient.setBaseUrl(session.getBaseUrl())
                                        val r = ApiClient.get().login(LoginRequest(id.trim(), pw))
                                        session.saveToken(r.token, stay); ApiClient.invalidate()
                                        val me = ApiClient.get().me().user
                                        withContext(Dispatchers.Main) { onLoggedIn(r.token, me.role == "admin", me.isDemo) }
                                    } catch (e: Exception) {
                                        val msg = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string()?.let {
                                            runCatching { org.json.JSONObject(it).optString("error", it) }.getOrDefault(it)
                                        }?.take(300) ?: (e.message ?: "Login failed")
                                        withContext(Dispatchers.Main) { err = msg; busy = false }
                                    }
                                }
                            },
                            enabled = !busy && !demoBusy && id.isNotBlank() && pw.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
                        ) {
                            if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = androidx.compose.ui.graphics.Color.White)
                            else Text("Sign In", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        TextButton(onClick = onForgot, modifier = Modifier.fillMaxWidth()) {
                            Text("Forgot password?", color = BeBetterTokens.Accent, fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                            Text("or", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        OutlinedButton(
                            onClick = {
                                demoBusy = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        session.saveBaseUrl(baseUrl.ifBlank { SessionManager.DEFAULT_BASE_URL })
                                        ApiClient.setBaseUrl(session.getBaseUrl())
                                        val r = ApiClient.get().demo()
                                        session.saveToken(r.token); ApiClient.invalidate()
                                        withContext(Dispatchers.Main) { onLoggedIn(r.token, false, true) }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) { err = e.message; demoBusy = false }
                                    }
                                }
                            },
                            enabled = !busy && !demoBusy,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            if (demoBusy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Try the Demo", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("Don't have an account? ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = onRegister, contentPadding = PaddingValues(0.dp)) {
                                Text("Sign up", color = BeBetterTokens.Accent, fontSize = 14.sp)
                            }
                        }
                    }
                }
                LegalLinksRow()
            }
        }
    }
}

@Composable
fun RegisterScreen(onDone: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val session = (ctx.applicationContext as BeBetterApp).session
    val scope = rememberCoroutineScope()
    val isDark = isBeBetterDark()
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var stay by remember { mutableStateOf(true) }
    var agree by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        IconButton(onClick = { scope.launch { val cur = session.getTheme(); session.saveTheme(if (cur == "light") "dark" else "light") } }, modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp).size(44.dp)) {
            Icon(if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode, "Toggle theme", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Column(Modifier.widthIn(max = 384.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 32.dp)) {
                    BeBetterLogo(size = 56.dp, light = !isDark)
                    Text("Start your journey", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, textAlign = TextAlign.Center)
                }
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        err?.let {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f)), shape = RoundedCornerShape(8.dp)) {
                                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                            }
                        }
                        OutlinedTextField(username, { username = it }, placeholder = { Text("Username", fontSize = 14.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp), shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BeBetterTokens.Accent, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface))
                        OutlinedTextField(email, { email = it }, placeholder = { Text("Email", fontSize = 14.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp), shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BeBetterTokens.Accent, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface))
                        OutlinedTextField(pw, { pw = it }, placeholder = { Text("Password (min 6 chars)", fontSize = 14.sp) }, visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp), shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BeBetterTokens.Accent, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface),
                            trailingIcon = { IconButton(onClick = { showPw = !showPw }, modifier = Modifier.size(44.dp)) { Icon(if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) } })
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = stay, onCheckedChange = { stay = it }, colors = CheckboxDefaults.colors(checkedColor = BeBetterTokens.Accent))
                            Text("Stay logged in", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                        }
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Checkbox(checked = agree, onCheckedChange = { agree = it }, colors = CheckboxDefaults.colors(checkedColor = BeBetterTokens.Accent))
                            Text("I am at least 16 years old (or have my parent's permission), and I agree to the Terms of Service and Privacy Policy.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                        }
                        Button(
                            onClick = {
                                if (!agree) { err = "Please accept the Terms and Privacy Policy to continue"; return@Button }
                                busy = true; err = null
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val r = ApiClient.get().register(RegisterRequest(email.trim(), pw, username.trim(), true))
                                        session.saveToken(r.token); ApiClient.invalidate()
                                        withContext(Dispatchers.Main) { onDone(r.token) }
                                    } catch (e: Exception) {
                                        val msg = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string()?.take(400) ?: e.message ?: "Registration failed"
                                        withContext(Dispatchers.Main) { err = msg; busy = false }
                                    }
                                }
                            },
                            enabled = !busy && agree,
                            modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
                        ) {
                            if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = androidx.compose.ui.graphics.Color.White) else Text("Create Account", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("Already have an account? ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("Sign in", color = BeBetterTokens.Accent, fontSize = 14.sp) }
                        }
                    }
                }
                LegalLinksRow()
            }
        }
    }
}

@Composable
fun ForgotScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val session = (ctx.applicationContext as BeBetterApp).session
    val isDark = isBeBetterDark()
    var email by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var cooldown by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(cooldown) {
        if (cooldown > 0) { kotlinx.coroutines.delay(1000); cooldown-- }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        IconButton(onClick = { scope.launch { val cur = session.getTheme(); session.saveTheme(if (cur == "light") "dark" else "light") } }, modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp).size(44.dp)) {
            Icon(if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode, "Toggle theme", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Column(Modifier.widthIn(max = 384.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 32.dp)) {
                    BeBetterLogo(size = 56.dp, light = !isDark)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Reset Password", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                        Text("Enter your email to receive a reset link", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        err?.let { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f)), shape = RoundedCornerShape(8.dp)) { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.padding(12.dp)) } }
                        msg?.let { Card(colors = CardDefaults.cardColors(containerColor = BeBetterTokens.Accent.copy(alpha = 0.10f)), shape = RoundedCornerShape(8.dp)) { Text(it, color = BeBetterTokens.Accent, fontSize = 14.sp, modifier = Modifier.padding(12.dp)) } }
                        OutlinedTextField(email, { email = it }, placeholder = { Text("Email", fontSize = 14.sp) }, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp), singleLine = true, shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BeBetterTokens.Accent, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface))
                        Button(onClick = {
                            busy = true; err = null; msg = null
                            scope.launch(Dispatchers.IO) {
                                val res = runCatching { ApiClient.get().forgotPassword(mapOf("email" to email.trim())) }.getOrElse { e ->
                                    withContext(Dispatchers.Main) {
                                        if ((e as? retrofit2.HttpException)?.code() == 429) { err = "Too many attempts — please wait a minute and try again"; cooldown = 60 } else err = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string()?.take(300) ?: e.message ?: "Failed"
                                        busy = false
                                    }
                                    return@launch
                                }
                                withContext(Dispatchers.Main) { msg = "If an account exists, a reset link has been sent"; cooldown = 60; busy = false }
                            }
                        }, enabled = !busy && cooldown == 0 && email.isNotBlank(), modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)) {
                            if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = androidx.compose.ui.graphics.Color.White)
                            else Text(if (cooldown > 0) "Wait ${cooldown}s before retrying" else "Send Reset Link", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        if (msg != null) Text("Didn't get mail? Check spam — links are limited to one every 10 minutes (max 5 a day).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to Sign In", color = BeBetterTokens.Accent, fontSize = 14.sp) }
                    }
                }
                LegalLinksRow()
            }
        }
    }
}
