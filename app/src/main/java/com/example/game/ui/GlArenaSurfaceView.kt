package com.example.game.ui

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.engine.CombatEngine
import com.example.game.model.CombatHudSnapshot
import com.example.game.model.FighterProfile
import com.example.game.render.SnTekkenGlRenderer

@Composable
fun GlArenaSurfaceView(
    combatEngine: CombatEngine?,
    previewFighterProvider: (() -> FighterProfile)? = null,
    onHudUpdated: ((CombatHudSnapshot) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val glView = remember(combatEngine) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            preserveEGLContextOnPause = true
            setRenderer(
                SnTekkenGlRenderer(
                    combatEngine = combatEngine,
                    previewFighterProvider = previewFighterProvider,
                    onHudUpdated = onHudUpdated
                )
            )
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(glView) {
        glView.onResume()
        onDispose {
            glView.onPause()
        }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier
    )
}
