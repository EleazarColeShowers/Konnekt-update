import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView


@Composable
fun CameraPermissionWrapper(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewUseCase = remember { Preview.Builder().build() }

    LaunchedEffect(isFrontCamera) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        cameraProvider.unbindAll()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(
                if (isFrontCamera) CameraSelector.LENS_FACING_FRONT
                else CameraSelector.LENS_FACING_BACK
            ).build()

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            previewUseCase
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                previewUseCase.setSurfaceProvider(this.surfaceProvider)
            }
        }
    )
}
