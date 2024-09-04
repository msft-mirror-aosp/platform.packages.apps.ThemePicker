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

package com.android.wallpaper.customization.ui.viewmodel

import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModelFactory
import com.android.wallpaper.picker.customization.ui.viewmodel.DefaultCustomizationOptionsViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemePickerCustomizationOptionsViewModel
@AssistedInject
constructor(
    defaultCustomizationOptionsViewModelFactory: DefaultCustomizationOptionsViewModel.Factory,
    keyguardQuickAffordancePickerViewModel2Factory: KeyguardQuickAffordancePickerViewModel2.Factory,
    colorPickerViewModel2Factory: ColorPickerViewModel2.Factory,
    clockPickerViewModelFactory: ClockPickerViewModel.Factory,
    shapeAndGridPickerViewModelFactory: ShapeAndGridPickerViewModel.Factory,
    @Assisted private val viewModelScope: CoroutineScope,
) : CustomizationOptionsViewModel {

    private val defaultCustomizationOptionsViewModel =
        defaultCustomizationOptionsViewModelFactory.create(viewModelScope)

    val clockPickerViewModel = clockPickerViewModelFactory.create(viewModelScope = viewModelScope)
    val keyguardQuickAffordancePickerViewModel2 =
        keyguardQuickAffordancePickerViewModel2Factory.create(viewModelScope = viewModelScope)
    val colorPickerViewModel2 = colorPickerViewModel2Factory.create(viewModelScope = viewModelScope)
    val shapeAndGridPickerViewModel =
        shapeAndGridPickerViewModelFactory.create(viewModelScope = viewModelScope)

    override val selectedOption = defaultCustomizationOptionsViewModel.selectedOption

    override fun deselectOption(): Boolean {
        keyguardQuickAffordancePickerViewModel2.resetPreview()
        return defaultCustomizationOptionsViewModel.deselectOption()
    }

    val onCustomizeClockClicked: Flow<(() -> Unit)?> =
        selectedOption.map {
            if (it == null) {
                {
                    defaultCustomizationOptionsViewModel.selectOption(
                        ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.CLOCK
                    )
                }
            } else {
                null
            }
        }

    val onCustomizeShortcutClicked: Flow<(() -> Unit)?> =
        selectedOption.map {
            if (it == null) {
                {
                    defaultCustomizationOptionsViewModel.selectOption(
                        ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
                            .SHORTCUTS
                    )
                }
            } else {
                null
            }
        }

    val onCustomizeColorsClicked: Flow<(() -> Unit)?> =
        selectedOption.map {
            if (it == null) {
                {
                    defaultCustomizationOptionsViewModel.selectOption(
                        ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.COLORS
                    )
                }
            } else {
                null
            }
        }

    val onCustomizeShapeAndGridClicked: Flow<(() -> Unit)?> =
        selectedOption.map {
            if (it == null) {
                {
                    defaultCustomizationOptionsViewModel.selectOption(
                        ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption
                            .APP_GRID
                    )
                }
            } else {
                null
            }
        }

    @ViewModelScoped
    @AssistedFactory
    interface Factory : CustomizationOptionsViewModelFactory {
        override fun create(
            viewModelScope: CoroutineScope
        ): ThemePickerCustomizationOptionsViewModel
    }
}
