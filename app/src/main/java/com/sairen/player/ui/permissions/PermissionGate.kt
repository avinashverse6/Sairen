package com.sairen.player.ui.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sairen.player.R
import com.sairen.player.ui.util.findActivity

/**
 * Gate in front of the library: explains *why* audio access is needed before the
 * system dialog appears, handles a second denial, and offers a route to app
 * settings when the system will no longer show the dialog.
 */
@Composable
fun AudioPermissionGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val permission = remember { Permissions.audioPermission() }

    var state by remember {
        mutableStateOf(PermissionDecision.initial(Permissions.hasAudioAccess(context)))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        state = PermissionDecision.afterRequestResult(
            hasPermission = Permissions.hasAudioAccess(context),
            shouldShowRationale = Permissions.shouldShowRationale(activity, permission),
        )
    }

    // Coming back from system settings must re-evaluate the grant.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state = PermissionDecision.afterResume(
                    hasPermission = Permissions.hasAudioAccess(context),
                    current = state,
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (state) {
        AudioAccessState.GRANTED -> content()
        AudioAccessState.RATIONALE, AudioAccessState.PERMANENTLY_DENIED -> PermissionRequestScreen(
            permanentlyDenied = state == AudioAccessState.PERMANENTLY_DENIED,
            onGrantClick = { launcher.launch(permission) },
            onOpenSettingsClick = { Permissions.openAppSettings(context) },
            modifier = modifier,
        )
    }
}

@Composable
private fun PermissionRequestScreen(
    permanentlyDenied: Boolean,
    onGrantClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(22.dp)
                        .size(44.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(
                    if (permanentlyDenied) R.string.permission_settings_title
                    else R.string.permission_title,
                ),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(
                    if (permanentlyDenied) R.string.permission_settings_body
                    else R.string.permission_body,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 460.dp),
            )
            Spacer(Modifier.height(28.dp))
            if (permanentlyDenied) {
                Button(onClick = onOpenSettingsClick) {
                    Text(stringResource(R.string.permission_open_settings))
                }
                TextButton(onClick = onGrantClick) {
                    Text(stringResource(R.string.permission_check_again))
                }
            } else {
                Button(onClick = onGrantClick) {
                    Text(stringResource(R.string.permission_audio_button))
                }
            }
        }
    }
}
