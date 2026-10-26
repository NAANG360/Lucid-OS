package com.cosmos.setup

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive edge-to-edge layout
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Fail-safe: If somehow launched after setup is complete, kill it immediately and go home
        if (Settings.Secure.getInt(contentResolver, Settings.Secure.USER_SETUP_COMPLETE, 0) == 1) {
            handoverToSystemLauncher()
            return
        }

        setContent {
            MaterialTheme {
                LucidSetupEngine(
                    onSetupFinished = { finalizeProvisioningAndLaunch() }
                )
            }
        }
    }

    private fun finalizeProvisioningAndLaunch() {
        // 1. Mark device as provisioned and setup completed in Android Framework
        try {
            Settings.Global.putInt(contentResolver, Settings.Global.DEVICE_PROVISIONED, 1)
            Settings.Secure.putInt(contentResolver, Settings.Secure.USER_SETUP_COMPLETE, 1)
        } catch (_: Exception) {}

        // 2. Disable this activity so it never runs again
        val component = ComponentName(this, MainActivity::class.java)
        packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        // 3. Handover to the system launcher (CosmosOS)
        handoverToSystemLauncher()
    }

    private fun handoverToSystemLauncher() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

// Setup wizard state machine and navigation.
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LucidSetupEngine(onSetupFinished: () -> Unit) {
    var currentStep by remember { mutableStateOf(0) }

    BackHandler(enabled = currentStep > 0) {
        currentStep -= 1
    }

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally { width -> width } + fadeIn() with
                    slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() with
                    slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "SetupFlowTransition"
    ) { step ->
        when (step) {
            0 -> LucidWelcomeScreen(onNext = { currentStep = 1 })
            1 -> LucidWifiScreen(onNext = { currentStep = 2 })
            2 -> LucidAccountScreen(onComplete = onSetupFinished)
        }
    }
}

@Composable
fun LucidWelcomeScreen(onNext: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Z", fontSize = 110.sp, fontWeight = FontWeight.Black,
                    color = Color(0xFFFF9BB5), modifier = Modifier.offset(y = (-8).dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("LucidOS", fontSize = 44.sp, fontWeight = FontWeight.Bold,
                color = Color.White, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(48.dp))
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape)
                    .background(Color(0xFF0D5EAF)).clickable { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Text("→", color = Color.White, fontSize = 32.sp,
                    fontWeight = FontWeight.Normal, modifier = Modifier.offset(y = (-2).dp))
            }
        }
    }
}

@Composable
fun LucidWifiScreen(onNext: () -> Unit) {
    var selectedNetwork by remember { mutableStateOf("LucidAP.NET") }
    val networks = listOf("LucidAP.NET", "MoAP", "Jane’s Hotspot", "Mcdonalds wifi")

    SetupGradientContainer {
        SetupCardContainer {
            Text("Connect to the internet", fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                color = Color.White, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                networks.forEach { ssid ->
                    val isSelected = selectedNetwork == ssid
                    Row(
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                            .clip(RoundedCornerShape(16.dp)).background(Color(0xFF140827))
                            .border(1.dp, if (isSelected) Color(0xFF7A22FF)
                                else Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .clickable { selectedNetwork = ssid }.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📶", fontSize = 14.sp, modifier = Modifier.padding(end = 16.dp))
                        Text(ssid, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF140827))
                    .clickable { onNext() }.padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text("Connect", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun LucidAccountScreen(onComplete: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    SetupGradientContainer {
        SetupCardContainer {
            Text("Log into your account", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                color = Color.White, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(36.dp))
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                placeholder = { Text("Username", color = Color.White.copy(alpha = 0.6f),
                    fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF140827), unfocusedContainerColor = Color(0xFF140827),
                    focusedBorderColor = Color(0xFF7A22FF), unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                placeholder = { Text("Password", color = Color.White.copy(alpha = 0.6f),
                    fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF140827), unfocusedContainerColor = Color(0xFF140827),
                    focusedBorderColor = Color(0xFF7A22FF), unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF140827))
                    .clickable { onComplete() }.padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun SetupGradientContainer(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(colors = listOf(Color(0xFF3800E5), Color(0xFF5B00FF), Color(0xFF8F00FF)))
        ),
        contentAlignment = Alignment.Center, content = content
    )
}

@Composable
fun SetupCardContainer(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(0.86f).wrapContentHeight()
            .shadow(24.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.4f)),
        color = Color(0xFF260D4D), shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, content = content
        )
    }
}
