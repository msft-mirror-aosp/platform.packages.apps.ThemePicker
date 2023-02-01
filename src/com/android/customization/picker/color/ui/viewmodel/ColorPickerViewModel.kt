/*
 * Copyright (C) 2023 The Android Open Source Project
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
 *
 */
package com.android.customization.picker.color.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.customization.model.color.ColorBundle
import com.android.customization.model.color.ColorSeedOption
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.shared.model.ColorOptionModel
import com.android.customization.picker.color.shared.model.ColorType
import com.android.wallpaper.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/** Models UI state for a color picker experience. */
class ColorPickerViewModel
private constructor(
    context: Context,
    private val interactor: ColorPickerInteractor,
) : ViewModel() {

    private val selectedColorTypeId = MutableStateFlow<ColorType?>(null)

    /** View-models for each color type. */
    val colorTypes: Flow<Map<ColorType, ColorTypeViewModel>> =
        combine(
            interactor.colorOptions,
            selectedColorTypeId,
        ) { colorOptions, selectedColorTypeIdOrNull ->
            colorOptions.keys
                .mapIndexed { index, colorType ->
                    val isSelected =
                        (selectedColorTypeIdOrNull == null && index == 0) ||
                            selectedColorTypeIdOrNull == colorType
                    colorType to
                        ColorTypeViewModel(
                            name =
                                when (colorType) {
                                    ColorType.WALLPAPER_COLOR ->
                                        context.resources.getString(R.string.wallpaper_color_tab)
                                    ColorType.BASIC_COLOR ->
                                        context.resources.getString(R.string.preset_color_tab)
                                },
                            isSelected = isSelected,
                            onClick =
                                if (isSelected) {
                                    null
                                } else {
                                    { this.selectedColorTypeId.value = colorType }
                                },
                        )
                }
                .toMap()
        }

    /** The list of all available color options for the selected Color Type. */
    val colorOptions: Flow<List<ColorOptionViewModel>> =
        combine(interactor.colorOptions, selectedColorTypeId) {
            colorOptions,
            selectedColorTypeIdOrNull ->
            val selectedColorType: ColorType =
                selectedColorTypeIdOrNull ?: ColorType.WALLPAPER_COLOR
            val selectedColorOptions: List<ColorOptionModel> = colorOptions[selectedColorType]!!
            selectedColorOptions.map { colorOptionModel ->
                when (selectedColorType) {
                    ColorType.BASIC_COLOR -> {
                        val colorBundle: ColorBundle = colorOptionModel.colorOption as ColorBundle
                        val primaryColor =
                            colorBundle.previewInfo.resolvePrimaryColor(context.resources)
                        val secondaryColor =
                            colorBundle.previewInfo.resolveSecondaryColor(context.resources)
                        ColorOptionViewModel(
                            color0 = primaryColor,
                            color1 = secondaryColor,
                            color2 = primaryColor,
                            color3 = secondaryColor,
                            contentDescription =
                                colorBundle.getContentDescription(context).toString(),
                            isSelected = colorOptionModel.isSelected,
                            onClick =
                                if (colorOptionModel.isSelected) {
                                    null
                                } else {
                                    { interactor.select(colorOptionModel) }
                                },
                        )
                    }
                    ColorType.WALLPAPER_COLOR -> {
                        val colorSeedOption: ColorSeedOption =
                            colorOptionModel.colorOption as ColorSeedOption
                        val colors = colorSeedOption.previewInfo.resolveColors(context.resources)
                        ColorOptionViewModel(
                            color0 = colors[0],
                            color1 = colors[1],
                            color2 = colors[2],
                            color3 = colors[3],
                            contentDescription =
                                colorSeedOption.getContentDescription(context).toString(),
                            isSelected = colorOptionModel.isSelected,
                            onClick =
                                if (colorOptionModel.isSelected) {
                                    null
                                } else {
                                    { interactor.select(colorOptionModel) }
                                },
                        )
                    }
                }
            }
        }

    class Factory(
        private val context: Context,
        private val interactor: ColorPickerInteractor,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ColorPickerViewModel(
                context = context,
                interactor = interactor,
            )
                as T
        }
    }
}
