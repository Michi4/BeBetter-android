package at.websters.bebetter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Web parity: centered max-w-sm, logo 56, "Welcome back", card rounded-2xl, .input/.btn styles.
@Composable
fun LoginScreen(onLoggedIn: (String, Boolean, Boolean) -> Unit, onRegister: () -> Unit, onForgot: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as BeBetterApp
    val session = app.session
    val scope = rememberCoroutineScope()
    var id by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var stay by remember { mutableStateOf(true) }
    var baseUrl by remember { mutableStateOf(SessionManager.DEFAULT_BASE_URL) }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var demoBusy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { baseUrl = session.getBaseUrl() }

    fun goThemeToggle() {
        scope.launch {
            val cur = session.getTheme()
            session.saveTheme(if (cur == "light") "dark" else "light")
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        IconButton(onClick = { goThemeToggle() }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Icon(Icons.Filled.BrightnessMedium, "Theme", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(Modifier.widthIn(max = 400.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BeBetterLogo(size = 56.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Welcome back", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Sign in to keep your streaks alive", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        err?.let {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f)), shape = RoundedCornerShape(8.dp)) {
                                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.padding(12.dp))
                            }
                        }
                        OutlinedTextField(
                            id, { id = it }, placeholder = { Text("Email or username") },
                            singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            pw, { pw = it }, placeholder = { Text("Password") },
                            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                IconButton(onClick = { showPw = !showPw }) {
                                    Icon(if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(stay, { stay = it })
                            Text("Stay logged in", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                busy = true; err = null
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        session.saveBaseUrl(baseUrl.ifBlank { SessionManager.DEFAULT_BASE_URL })
                                        ApiClient.setBaseUrl(session.getBaseUrl())
                                        val r = ApiClient.get().login(LoginRequest(id.trim(), pw))
                                        session.saveToken(r.token); ApiClient.invalidate()
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
                            Text(if (busy) "Signing in…" else "Sign In")
                        }
                        TextButton(onClick = onForgot, modifier = Modifier.fillMaxWidth()) {
                            Text("Forgot password?", color = BeBetterTokens.Accent, fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Divider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
                            Text("or", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            Divider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
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
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (demoBusy) "Loading…" else "Try the Demo")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text("Don't have an account? ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = onRegister, contentPadding = PaddingValues(0.dp)) {
                                Text("Sign up", color = BeBetterTokens.Accent, fontSize = 14.sp)
                            }
                        }
                        // Server URL (tidied: collapsible, defaults to prod)
                        OutlinedTextField(
                            baseUrl, { baseUrl = it }, label = { Text("Server", fontSize = 12.sp) },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {}) { Text("Terms", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    TextButton(onClick = {}) { Text("Privacy", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
                    TextButton(onClick = {}) { Text("Imprint", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(onDone: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val session = (ctx.applicationContext as BeBetterApp).session
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 400.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BeBetterLogo(size = 56.dp)
            Text("Create your account", fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Build habits that last. Together.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(email, { email = it }, placeholder = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp), shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(username, { username = it }, placeholder = { Text("Username (3-20, a-z 0-9 _)") }, singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp), shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(pw, { pw = it }, placeholder = { Text("Password (min 6)") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp), shape = RoundedCornerShape(8.dp))
                    Text("By registering you accept the Terms of Service and Privacy Policy.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
                    Button(
                        onClick = {
                            busy = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val r = ApiClient.get().register(RegisterRequest(email.trim(), pw, username.trim(), true))
                                    session.saveToken(r.token); ApiClient.invalidate()
                                    withContext(Dispatchers.Main) { onDone(r.token) }
                                } catch (e: Exception) {
                                    val msg = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string()?.take(400) ?: e.message
                                    withContext(Dispatchers.Main) { err = msg; busy = false }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
                    ) { Text("Sign Up") }
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to login", color = BeBetterTokens.Accent) }
                }
            }
        }
    }
}

@Composable
fun ForgotScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 400.dp).fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BeBetterLogo(size = 56.dp)
            Text("Reset password", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(email, { email = it }, placeholder = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                    Button(onClick = {
                        scope.launch(Dispatchers.IO) {
                            val m = runCatching { ApiClient.get().forgotPassword(mapOf("email" to email.trim())); "If the email exists, a reset link was sent." }.getOrElse { it.message ?: "Failed" }
                            withContext(Dispatchers.Main) { msg = m }
                        }
                    }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(8.dp)) { Text("Send reset link") }
                    msg?.let { Text(it, fontSize = 14.sp) }
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back", color = BeBetterTokens.Accent) }
                }
            }
        }
    }
}
