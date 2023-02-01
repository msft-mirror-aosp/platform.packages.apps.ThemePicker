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
package com.android.customization.picker.color.data.repository

import android.app.WallpaperColors
import android.content.Context
import android.util.Log
import com.android.customization.model.CustomizationManager
import com.android.customization.model.color.ColorBundle
import com.android.customization.model.color.ColorCustomizationManager
import com.android.customization.model.color.ColorOption
import com.android.customization.model.color.ColorSeedOption
import com.android.customization.model.theme.OverlayManagerCompat
import com.android.customization.picker.color.shared.model.ColorOptionModel
import com.android.customization.picker.color.shared.model.ColorType
import com.android.wallpaper.model.WallpaperColorsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

// TODO (b/262924623): refactor to remove dependency on ColorCustomizationManager & ColorOption
class ColorPickerRepositoryImpl(
    context: Context,
    wallpaperColorsViewModel: WallpaperColorsViewModel,
) : ColorPickerRepository {

    private val homeWallpaperColors: StateFlow<WallpaperColors?> =
        wallpaperColorsViewModel.homeWallpaperColors
    private val lockWallpaperColors: StateFlow<WallpaperColors?> =
        wallpaperColorsViewModel.lockWallpaperColors
    private val colorManager: ColorCustomizationManager =
        ColorCustomizationManager.getInstance(context, OverlayManagerCompat(context))

    /** List of wallpaper and preset color options on the device, categorized by Color Type */
    override val colorOptions: Flow<Map<ColorType, List<ColorOptionModel>>> =
        combine(homeWallpaperColors, lockWallpaperColors) { homeColors, lockColors ->
            colorManager.setWallpaperColors(homeColors, lockColors)
            val wallpaperColorOptions: MutableList<ColorOptionModel> = mutableListOf()
            val presetColorOptions: MutableList<ColorOptionModel> = mutableListOf()
            colorManager.fetchOptions(
                object : CustomizationManager.OptionsFetchedListener<ColorOption?> {
                    override fun onOptionsLoaded(options: MutableList<ColorOption?>?) {
                        options?.forEach { option ->
                            when (option) {
                                is ColorSeedOption -> wallpaperColorOptions.add(option.toModel())
                                is ColorBundle -> presetColorOptions.add(option.toModel())
                            }
                        }
                    }

                    override fun onError(throwable: Throwable?) {
                        Log.e("ColorPickerRepository", "Error loading theme bundles", throwable)
                    }
                },
                /* reload= */ false
            )
            mapOf(
                ColorType.WALLPAPER_COLOR to wallpaperColorOptions,
                ColorType.BASIC_COLOR to presetColorOptions
            )
        }

    override fun select(colorOptionModel: ColorOptionModel) {
        val colorOption: ColorOption = colorOptionModel.colorOption
        colorManager.apply(
            colorOption,
            object : CustomizationManager.Callback {
                override fun onSuccess() = Unit

                override fun onError(throwable: Throwable?) {
                    Log.w("ColorPickerRepository", "Apply theme with error", throwable)
                }
            }
        )
    }

    private fun ColorOption.toModel(): ColorOptionModel {
        return ColorOptionModel(
            colorOption = this,
            isSelected = isActive(colorManager),
        )
    }
}
