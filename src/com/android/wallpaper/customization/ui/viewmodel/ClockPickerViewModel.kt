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

import android.content.Context
import android.content.res.Resources
import androidx.core.graphics.ColorUtils
import com.android.customization.model.color.ColorOptionImpl
import com.android.customization.module.logging.ThemesUserEventLogger
import com.android.customization.module.logging.ThemesUserEventLogger.Companion.NULL_SEED_COLOR
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.customization.picker.clock.shared.toClockSizeForLogging
import com.android.customization.picker.clock.ui.viewmodel.ClockColorViewModel
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.shared.model.ColorOptionModel
import com.android.customization.picker.color.shared.model.ColorType
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.themepicker.R
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** View model for the clock customization screen. */
class ClockPickerViewModel
@AssistedInject
constructor(
    @ApplicationContext context: Context,
    resources: Resources,
    private val clockPickerInteractor: ClockPickerInteractor,
    colorPickerInteractor: ColorPickerInteractor,
    private val logger: ThemesUserEventLogger,
    @BackgroundDispatcher private val backgroundDispatcher: CoroutineDispatcher,
    @Assisted private val viewModelScope: CoroutineScope,
) {

    enum class Tab {
        STYLE,
        COLOR,
        SIZE,
    }

    private val colorMap = ClockColorViewModel.getPresetColorMap(context.resources)

    private val _selectedTab = MutableStateFlow(Tab.STYLE)
    val selectedTab: StateFlow<Tab> = _selectedTab.asStateFlow()

    fun setTab(tab: Tab) {
        _selectedTab.value = tab
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allClocks: StateFlow<List<ClockOptionItemViewModel>> =
        clockPickerInteractor.allClocks
            .mapLatest { allClocks ->
                // Delay to avoid the case that the full list of clocks is not initiated.
                delay(CLOCKS_EVENT_UPDATE_DELAY_MILLIS)
                allClocks.map {
                    val contentDescription =
                        resources.getString(
                            R.string.select_clock_action_description,
                            // TODO (b/350718184): Get ClockConfig.description from ClockRegistry
                            "description"
                        )
                    ClockOptionItemViewModel(
                        clockId = it.clockId,
                        isSelected = it.isSelected,
                        contentDescription = contentDescription,
                    )
                }
            }
            // makes sure that the operations above this statement are executed on I/O dispatcher
            // while parallelism limits the number of threads this can run on which makes sure that
            // the flows run sequentially
            .flowOn(backgroundDispatcher.limitedParallelism(1))
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedClockId: StateFlow<String?> =
        clockPickerInteractor.selectedClockId
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var setSelectedClockJob: Job? = null

    fun setSelectedClock(clockId: String) {
        setSelectedClockJob?.cancel()
        setSelectedClockJob =
            viewModelScope.launch(backgroundDispatcher) {
                clockPickerInteractor.setSelectedClock(clockId)
                logger.logClockApplied(clockId)
            }
    }

    private val selectedColorId: StateFlow<String?> =
        clockPickerInteractor.selectedColorId.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val sliderColorToneProgress =
        MutableStateFlow(ClockMetadataModel.DEFAULT_COLOR_TONE_PROGRESS)
    val isSliderEnabled: Flow<Boolean> =
        combine(selectedClockId, clockPickerInteractor.selectedColorId) { clockId, colorId ->
                if (colorId == null || clockId == null) {
                    false
                } else {
                    // TODO (b/350718184): Get ClockConfig.isReactiveToTone from ClockRegistry
                    false
                }
            }
            .distinctUntilChanged()
    val sliderProgress: Flow<Int> =
        merge(clockPickerInteractor.colorToneProgress, sliderColorToneProgress)

    private val _seedColor: MutableStateFlow<Int?> = MutableStateFlow(null)
    val seedColor: Flow<Int?> = merge(clockPickerInteractor.seedColor, _seedColor)

    /**
     * The slider color tone updates are quick. Do not set color tone and the blended color to the
     * settings until [onSliderProgressStop] is called. Update to a locally cached temporary
     * [sliderColorToneProgress] and [_seedColor] instead.
     */
    fun onSliderProgressChanged(progress: Int) {
        sliderColorToneProgress.value = progress
        val selectedColorId = selectedColorId.value ?: return
        val clockColorViewModel = colorMap[selectedColorId] ?: return
        _seedColor.value =
            blendColorWithTone(
                color = clockColorViewModel.color,
                colorTone = clockColorViewModel.getColorTone(progress),
            )
    }

    suspend fun onSliderProgressStop(progress: Int) {
        val selectedColorId = selectedColorId.value ?: return
        val clockColorViewModel = colorMap[selectedColorId] ?: return
        val seedColor =
            blendColorWithTone(
                color = clockColorViewModel.color,
                colorTone = clockColorViewModel.getColorTone(progress),
            )
        clockPickerInteractor.setClockColor(
            selectedColorId = selectedColorId,
            colorToneProgress = progress,
            seedColor = seedColor,
        )
        logger.logClockColorApplied(seedColor)
    }

    val clockColorOptions: Flow<List<OptionItemViewModel<ColorOptionIconViewModel>>> =
        colorPickerInteractor.colorOptions.map { colorOptions ->
            // Use mapLatest and delay(100) here to prevent too many selectedClockColor update
            // events from ClockRegistry upstream, caused by sliding the saturation level bar.
            delay(COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
            buildList {
                val defaultThemeColorOptionViewModel =
                    (colorOptions[ColorType.WALLPAPER_COLOR]?.find { it.isSelected })
                        ?.toOptionItemViewModel(context)
                        ?: (colorOptions[ColorType.PRESET_COLOR]?.find { it.isSelected })
                            ?.toOptionItemViewModel(context)
                if (defaultThemeColorOptionViewModel != null) {
                    add(defaultThemeColorOptionViewModel)
                }

                colorMap.values.forEachIndexed { index, colorModel ->
                    val isSelectedFlow =
                        selectedColorId
                            .map { colorMap.keys.indexOf(it) == index }
                            .stateIn(viewModelScope)
                    val colorToneProgress = ClockMetadataModel.DEFAULT_COLOR_TONE_PROGRESS
                    add(
                        OptionItemViewModel<ColorOptionIconViewModel>(
                            key = MutableStateFlow(colorModel.colorId) as StateFlow<String>,
                            payload =
                                ColorOptionIconViewModel(
                                    lightThemeColor0 = colorModel.color,
                                    lightThemeColor1 = colorModel.color,
                                    lightThemeColor2 = colorModel.color,
                                    lightThemeColor3 = colorModel.color,
                                    darkThemeColor0 = colorModel.color,
                                    darkThemeColor1 = colorModel.color,
                                    darkThemeColor2 = colorModel.color,
                                    darkThemeColor3 = colorModel.color,
                                ),
                            text =
                                Text.Loaded(
                                    context.getString(
                                        R.string.content_description_color_option,
                                        index,
                                    )
                                ),
                            isTextUserVisible = false,
                            isSelected = isSelectedFlow,
                            onClicked =
                                isSelectedFlow.map { isSelected ->
                                    if (isSelected) {
                                        null
                                    } else {
                                        {
                                            viewModelScope.launch {
                                                val seedColor =
                                                    blendColorWithTone(
                                                        color = colorModel.color,
                                                        colorTone =
                                                            colorModel.getColorTone(
                                                                colorToneProgress,
                                                            ),
                                                    )
                                                clockPickerInteractor.setClockColor(
                                                    selectedColorId = colorModel.colorId,
                                                    colorToneProgress = colorToneProgress,
                                                    seedColor = seedColor,
                                                )
                                                logger.logClockColorApplied(seedColor)
                                            }
                                        }
                                    }
                                },
                        )
                    )
                }
            }
        }

    private suspend fun ColorOptionModel.toOptionItemViewModel(
        context: Context
    ): OptionItemViewModel<ColorOptionIconViewModel> {
        val lightThemeColors =
            (colorOption as ColorOptionImpl)
                .previewInfo
                .resolveColors(
                    /** darkTheme= */
                    false
                )
        val darkThemeColors =
            colorOption.previewInfo.resolveColors(
                /** darkTheme= */
                true
            )
        val isSelectedFlow = selectedColorId.map { it == null }.stateIn(viewModelScope)
        return OptionItemViewModel<ColorOptionIconViewModel>(
            key = MutableStateFlow(key) as StateFlow<String>,
            payload =
                ColorOptionIconViewModel(
                    lightThemeColor0 = lightThemeColors[0],
                    lightThemeColor1 = lightThemeColors[1],
                    lightThemeColor2 = lightThemeColors[2],
                    lightThemeColor3 = lightThemeColors[3],
                    darkThemeColor0 = darkThemeColors[0],
                    darkThemeColor1 = darkThemeColors[1],
                    darkThemeColor2 = darkThemeColors[2],
                    darkThemeColor3 = darkThemeColors[3],
                ),
            text = Text.Loaded(context.getString(R.string.default_theme_title)),
            isTextUserVisible = true,
            isSelected = isSelectedFlow,
            onClicked =
                isSelectedFlow.map { isSelected ->
                    if (isSelected) {
                        null
                    } else {
                        {
                            viewModelScope.launch {
                                clockPickerInteractor.setClockColor(
                                    selectedColorId = null,
                                    colorToneProgress =
                                        ClockMetadataModel.DEFAULT_COLOR_TONE_PROGRESS,
                                    seedColor = null,
                                )
                                logger.logClockColorApplied(NULL_SEED_COLOR)
                            }
                        }
                    }
                },
        )
    }

    val selectedClockSize: Flow<ClockSize> = clockPickerInteractor.selectedClockSize

    fun setClockSize(size: ClockSize) {
        viewModelScope.launch {
            clockPickerInteractor.setClockSize(size)
            logger.logClockSizeApplied(size.toClockSizeForLogging())
        }
    }

    companion object {
        private val helperColorLab: DoubleArray by lazy { DoubleArray(3) }

        fun blendColorWithTone(color: Int, colorTone: Double): Int {
            ColorUtils.colorToLAB(color, helperColorLab)
            return ColorUtils.LABToColor(
                colorTone,
                helperColorLab[1],
                helperColorLab[2],
            )
        }

        const val COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS: Long = 100
        const val CLOCKS_EVENT_UPDATE_DELAY_MILLIS: Long = 100
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): ClockPickerViewModel
    }
}