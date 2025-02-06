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

package com.android.customization.picker.mode.ui.binder

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.customization.picker.mode.ui.viewmodel.DarkModeViewModel
import com.android.wallpaper.customization.ui.binder.SwitchColorBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

object DarkModeBinder {
    fun bind(
        darkModeToggle: MaterialSwitch,
        viewModel: DarkModeViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isEnabled.collect { darkModeToggle.isEnabled = it } }
                launch {
                    var binding: SwitchColorBinder.Binding? = null
                    viewModel.previewingIsDarkMode.collect {
                        darkModeToggle.isChecked = it
                        binding?.destroy()
                        binding =
                            SwitchColorBinder.bind(
                                switch = darkModeToggle,
                                isChecked = it,
                                colorUpdateViewModel = colorUpdateViewModel,
                                shouldAnimateColor = shouldAnimateColor,
                                lifecycleOwner = lifecycleOwner,
                            )
                    }
                }
                launch {
                    viewModel.toggleDarkMode.collect {
                        darkModeToggle.setOnCheckedChangeListener { _, _ -> it.invoke() }
                    }
                }
            }
        }
    }
}
