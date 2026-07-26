package com.avelcam.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avelcam.android.camera.CameraPreview
import com.avelcam.android.encoder.diagnostic.EncoderDiagnosticPanel
import com.avelcam.android.ui.theme.AvelCamTheme
import com.avelcam.android.ui.theme.Dark
import com.avelcam.android.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[CameraViewModel::class.java]

        setContent {
            AvelCamTheme {
                CameraScreen(viewModel)
            }
        }
    }
}

class CameraViewModel : ViewModel() {
    private val _state = MutableStateFlow(CameraState())
    val state = _state.asStateFlow()

    fun setPermissionState(state: PermissionUiState) {
        _state.update { it.copy(permissionState = state) }
    }

    fun setAvailability(hasRearCamera: Boolean, hasFrontCamera: Boolean) {
        _state.update { old ->
            var selected = old.selectedLens
            if (!hasRearCamera && hasFrontCamera) {
                selected = CameraSelector.LENS_FACING_FRONT
            } else if (!hasFrontCamera && hasRearCamera) {
                selected = CameraSelector.LENS_FACING_BACK
            }
            old.copy(
                selectedLens = selected,
                hasRearCamera = hasRearCamera,
                hasFrontCamera = hasFrontCamera
            )
        }
    }

    fun setError(message: String?) {
        _state.update { it.copy(errorMessage = message) }
    }

    fun switchCamera() {
        _state.update {
            it.copy(selectedLens = nextCameraForSwitch(it.selectedLens, it.hasRearCamera, it.hasFrontCamera))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CameraScreen(viewModel: CameraViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var permissionChecked by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            context as ComponentActivity,
            Manifest.permission.CAMERA
        )
        viewModel.setPermissionState(
            resolvePermissionState(
                granted = isGranted,
                shouldShowRationale = shouldShowRationale,
                hasPermissionCapability = true
            )
        )
    }

    LaunchedEffect(Unit) {
        if (!permissionChecked) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            viewModel.setPermissionState(
                if (hasPermission) PermissionUiState.GRANTED else PermissionUiState.NOT_REQUESTED
            )
            permissionChecked = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AvelCam") })
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = Dark
        ) {
            Column {
            when (uiState.permissionState) {
                PermissionUiState.GRANTED -> {
                    CameraPermissionGrantedScreen(
                        uiState = uiState,
                        onSwitch = viewModel::switchCamera,
                        onAvailabilityUpdated = { hasRear, hasFront ->
                            viewModel.setAvailability(hasRear, hasFront)
                        },
                        onError = viewModel::setError
                    )
                }
                PermissionUiState.DENIED -> {
                    PermissionStateScreen(
                        title = "Camera access denied",
                        message = "AvelCam needs camera permission to show the live preview.",
                        actionText = "Try again",
                        onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }
                PermissionUiState.PERMANENTLY_DENIED -> {
                    PermissionStateScreen(
                        title = "Camera permission permanently denied",
                        message = "Please open Settings and grant camera permission for AvelCam.",
                        actionText = "Open app settings",
                        onAction = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }
                    )
                }
                PermissionUiState.UNAVAILABLE -> {
                    PermissionStateScreen(
                        title = "Camera unavailable",
                        message = "Camera capability is unavailable on this device.",
                        actionText = "Retry",
                        onAction = { viewModel.setPermissionState(PermissionUiState.NOT_REQUESTED) }
                    )
                }
                PermissionUiState.NOT_REQUESTED -> {
                    PermissionStateScreen(
                        title = "Camera permission required",
                        message = "Allow camera access to open local live preview.",
                        actionText = "Grant permission",
                        onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }
            }
                if (BuildConfig.DEBUG) {
                    EncoderDiagnosticPanel()
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionGrantedScreen(
    uiState: CameraState,
    onSwitch: () -> Unit,
    onAvailabilityUpdated: (Boolean, Boolean) -> Unit,
    onError: (String?) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            selectedLens = uiState.selectedLens,
            onAvailabilityUpdated = onAvailabilityUpdated,
            onError = onError
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier.padding(10.dp),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Card(modifier = Modifier.align(Alignment.CenterVertically)) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = if (uiState.selectedLens == CameraSelector.LENS_FACING_FRONT) {
                            "Current camera: Front"
                        } else {
                            "Current camera: Rear"
                        }
                    )
                }

                Button(onClick = onSwitch, enabled = uiState.canSwitchCamera) {
                    Text(
                        text = if (uiState.selectedLens == CameraSelector.LENS_FACING_BACK) {
                            "Switch to front"
                        } else {
                            "Switch to rear"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStateScreen(
    title: String,
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAction) {
            Text(actionText)
        }
    }
}
