/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.customization.picker.settings.data.repository

import android.app.UiModeManager
import android.content.Context
import android.content.Context.UI_MODE_SERVICE
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ColorContrastSectionRepository(
    private val context: Context,
    private val bgDispatcher: CoroutineDispatcher
) {
    var uiModeManager =
        context.applicationContext.getSystemService(UI_MODE_SERVICE) as UiModeManager?
    var contrast: Flow<Float> = callbackFlow {
        val executor: Executor = bgDispatcher.asExecutor()
        val listener =
            UiModeManager.ContrastChangeListener { contrast ->
                // Emit the new contrast value whenever it changes
                trySend(contrast)
            }

        // Emit the current contrast value immediately
        uiModeManager?.contrast?.let { currentContrast -> trySend(currentContrast) }

        // Register the listener with the UiModeManager
        uiModeManager?.addContrastChangeListener(executor, listener)

        // Await close signals to unregister the listener to prevent memory leaks
        awaitClose {
            // Unregister the listener when the flow collection is cancelled or no longer in use
            uiModeManager?.removeContrastChangeListener(listener)
        }
    }
}
