package com.carlens.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carlens.platform.auth.BiometricHelper
import com.carlens.ui.CarLensAppView
import com.carlens.ui.MainViewModel
import com.carlens.ui.theme.CarLensTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var biometricHelper: BiometricHelper

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Graceful best-effort: notifications work if granted, reminders still visible in-app if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleDeepLinkIntent(intent)

        // Request notification permission on Android 13+ (gracefully best-effort)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val settingsLoaded by viewModel.settingsLoaded.collectAsStateWithLifecycle()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            var isUnlocked by rememberSaveable { mutableStateOf(false) }
            var lastStoppedAt by rememberSaveable { mutableLongStateOf(0L) }
            val lifecycleOwner = LocalLifecycleOwner.current

            // Re-lock after 30s in background (gives grace period for camera / SAF pickers)
            DisposableEffect(lifecycleOwner, settings.biometricLock) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            lastStoppedAt = System.currentTimeMillis()
                        }
                        Lifecycle.Event.ON_START -> {
                            if (settings.biometricLock && lastStoppedAt > 0L &&
                                System.currentTimeMillis() - lastStoppedAt > 30_000L
                            ) {
                                isUnlocked = false
                            }
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Trigger biometric prompt once DataStore settings have loaded
            LaunchedEffect(settingsLoaded, settings.biometricLock, isUnlocked) {
                if (!settingsLoaded) return@LaunchedEffect
                if (settings.biometricLock && !isUnlocked) {
                    authenticateUser { isUnlocked = true }
                } else if (!settings.biometricLock) {
                    isUnlocked = true
                }
            }

            CarLensTheme(themeMode = settings.themeMode) {
                if (!settingsLoaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                } else if (settings.biometricLock && !isUnlocked) {
                    BiometricLockScreen(
                        onUnlockClick = { authenticateUser { isUnlocked = true } }
                    )
                } else {
                    CarLensAppView(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        if (intent == null) return
        val vehicleId = intent.getStringExtra("vehicle_id")
        val targetType = intent.getStringExtra("target_type")
        if (!vehicleId.isNullOrBlank() || !targetType.isNullOrBlank()) {
            viewModel.handleNotificationDeepLink(vehicleId, targetType)
            intent.removeExtra("vehicle_id")
            intent.removeExtra("target_type")
            intent.removeExtra("target_id")
        }
    }

    private fun authenticateUser(onSuccess: () -> Unit) {
        if (biometricHelper.canAuthenticate()) {
            biometricHelper.prompt(
                activity = this,
                title = "Unlock CarLens",
                subtitle = "Verify your fingerprint or PIN",
                onSuccess = onSuccess,
                onError = { /* Allow user to tap Unlock to retry */ }
            )
        } else {
            // Biometric not enrolled or unavailable; fallback to unlocked
            onSuccess()
        }
    }
}

@Composable
fun BiometricLockScreen(onUnlockClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "CarLens Locked",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Biometric authentication is required to access your vehicle records.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onUnlockClick) {
                Text("Unlock")
            }
        }
    }
}
