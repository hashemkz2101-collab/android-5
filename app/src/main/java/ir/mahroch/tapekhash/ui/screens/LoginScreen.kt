package ir.mahroch.tapekhash.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ir.mahroch.tapekhash.data.ApiException
import ir.mahroch.tapekhash.data.ApiClient
import ir.mahroch.tapekhash.data.Session
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var remember by remember { mutableStateOf(true) }
    var showAdvanced by remember { mutableStateOf(false) }
    var baseUrl by remember { mutableStateOf(Session.baseUrl) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // تلاش برای ورود خودکار با remember-token در اولین بار باز شدن صفحه
    LaunchedEffect(Unit) {
        val rtoken = Session.rememberToken
        if (rtoken != null) {
            try {
                val res = ApiClient.call("loginWithRememberToken", JSONObject().put("remember_token", rtoken), auth = false)
                Session.token = res.getString("token")
                Session.saveUser(res.getJSONObject("user"))
                onLoggedIn()
                return@LaunchedEffect
            } catch (e: Exception) {
                Session.rememberToken = null
            }
        }
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    fun doLogin() {
        if (username.isBlank() || password.isBlank()) {
            error = "نام کاربری و رمز عبور را وارد کنید."
            return
        }
        Session.baseUrl = baseUrl.trim()
        loading = true
        error = null
        scope.launch {
            try {
                val body = JSONObject()
                    .put("username", username.trim())
                    .put("password", password)
                    .put("remember", remember)
                val res = ApiClient.call("login", body, auth = false)
                Session.token = res.getString("token")
                if (res.has("remember_token")) Session.rememberToken = res.getString("remember_token")
                Session.saveUser(res.getJSONObject("user"))
                onLoggedIn()
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Exception) {
                error = "خطا در اتصال به سرور. اتصال اینترنت را بررسی کنید."
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("ورود به سامانه", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("نام کاربری") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("رمز عبور") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = remember, onCheckedChange = { remember = it })
            Text("مرا به خاطر بسپار")
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { doLogin() }, modifier = Modifier.fillMaxWidth()) {
            Text("ورود")
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = { showAdvanced = !showAdvanced }) {
            Text(if (showAdvanced) "پنهان کردن تنظیمات آدرس سرور" else "تنظیمات آدرس سرور")
        }
        if (showAdvanced) {
            OutlinedTextField(
                value = baseUrl, onValueChange = { baseUrl = it },
                label = { Text("آدرس API (اگر پوشه روی هاست را عوض کردید)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        }
    }
}
