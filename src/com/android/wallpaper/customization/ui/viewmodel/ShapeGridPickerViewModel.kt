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
import com.android.customization.model.ResourceConstants
import com.android.customization.model.grid.GridOptionModel
import com.android.customization.picker.grid.domain.interactor.ShapeGridInteractor
import com.android.customization.picker.grid.ui.viewmodel.GridIconViewModel
import com.android.customization.picker.grid.ui.viewmodel.ShapeIconViewModel
import com.android.themepicker.R
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

class ShapeGridPickerViewModel
@AssistedInject
constructor(
    @ApplicationContext private val context: Context,
    interactor: ShapeGridInteractor,
    @Assisted private val viewModelScope: CoroutineScope,
) {

    enum class Tab {
        SHAPE,
        GRID,
    }

    // TODO (b/362237825): Get the shape options from the Launcher
    val shapePaths =
        listOf(
            Pair(
                "arch",
                "M100 83.46C100 85.471 100 86.476 99.9 87.321 99.116 93.916 93.916 99.116 87.321 99.9 86.476 100 85.471 100 83.46 100H16.54C14.529 100 13.524 100 12.679 99.9 6.084 99.116.884 93.916.1 87.321 0 86.476 0 85.471 0 83.46L0 50C0 22.386 22.386 0 50 0 77.614 0 100 22.386 100 50V83.46Z",
            ),
            Pair(
                "4-sided-cookie",
                "M63.605 3C84.733-6.176 106.176 15.268 97 36.395L95.483 39.888C92.681 46.338 92.681 53.662 95.483 60.112L97 63.605C106.176 84.732 84.733 106.176 63.605 97L60.112 95.483C53.662 92.681 46.338 92.681 39.888 95.483L36.395 97C15.267 106.176-6.176 84.732 3 63.605L4.517 60.112C7.319 53.662 7.319 46.338 4.517 39.888L3 36.395C-6.176 15.268 15.267-6.176 36.395 3L39.888 4.517C46.338 7.319 53.662 7.319 60.112 4.517L63.605 3Z",
            ),
            Pair(
                "7-sided-cookie",
                "M35.209 4.878C36.326 3.895 36.884 3.404 37.397 3.006 44.82-2.742 55.18-2.742 62.603 3.006 63.116 3.404 63.674 3.895 64.791 4.878 65.164 5.207 65.351 5.371 65.539 5.529 68.167 7.734 71.303 9.248 74.663 9.932 74.902 9.981 75.147 10.025 75.637 10.113 77.1 10.375 77.831 10.506 78.461 10.66 87.573 12.893 94.032 21.011 94.176 30.412 94.186 31.062 94.151 31.805 94.08 33.293 94.057 33.791 94.045 34.04 94.039 34.285 93.958 37.72 94.732 41.121 96.293 44.18 96.404 44.399 96.522 44.618 96.759 45.056 97.467 46.366 97.821 47.021 98.093 47.611 102.032 56.143 99.727 66.266 92.484 72.24 91.983 72.653 91.381 73.089 90.177 73.961 89.774 74.254 89.572 74.4 89.377 74.548 86.647 76.626 84.477 79.353 83.063 82.483 82.962 82.707 82.865 82.936 82.671 83.395 82.091 84.766 81.8 85.451 81.51 86.033 77.31 94.44 67.977 98.945 58.801 96.994 58.166 96.859 57.451 96.659 56.019 96.259 55.54 96.125 55.3 96.058 55.063 95.998 51.74 95.154 48.26 95.154 44.937 95.998 44.699 96.058 44.46 96.125 43.981 96.259 42.549 96.659 41.834 96.859 41.199 96.994 32.023 98.945 22.69 94.44 18.49 86.033 18.2 85.451 17.909 84.766 17.329 83.395 17.135 82.936 17.038 82.707 16.937 82.483 15.523 79.353 13.353 76.626 10.623 74.548 10.428 74.4 10.226 74.254 9.823 73.961 8.619 73.089 8.017 72.653 7.516 72.24.273 66.266-2.032 56.143 1.907 47.611 2.179 47.021 2.533 46.366 3.241 45.056 3.478 44.618 3.596 44.399 3.707 44.18 5.268 41.121 6.042 37.72 5.961 34.285 5.955 34.04 5.943 33.791 5.92 33.293 5.849 31.805 5.814 31.062 5.824 30.412 5.968 21.011 12.427 12.893 21.539 10.66 22.169 10.506 22.9 10.375 24.363 10.113 24.853 10.025 25.098 9.981 25.337 9.932 28.697 9.248 31.833 7.734 34.461 5.529 34.649 5.371 34.836 5.207 35.209 4.878Z",
            ),
            Pair(
                "sunny",
                "M42.846 4.873C46.084-.531 53.916-.531 57.154 4.873L60.796 10.951C62.685 14.103 66.414 15.647 69.978 14.754L76.851 13.032C82.962 11.5 88.5 17.038 86.968 23.149L85.246 30.022C84.353 33.586 85.897 37.315 89.049 39.204L95.127 42.846C100.531 46.084 100.531 53.916 95.127 57.154L89.049 60.796C85.897 62.685 84.353 66.414 85.246 69.978L86.968 76.851C88.5 82.962 82.962 88.5 76.851 86.968L69.978 85.246C66.414 84.353 62.685 85.898 60.796 89.049L57.154 95.127C53.916 100.531 46.084 100.531 42.846 95.127L39.204 89.049C37.315 85.898 33.586 84.353 30.022 85.246L23.149 86.968C17.038 88.5 11.5 82.962 13.032 76.851L14.754 69.978C15.647 66.414 14.103 62.685 10.951 60.796L4.873 57.154C-.531 53.916-.531 46.084 4.873 42.846L10.951 39.204C14.103 37.315 15.647 33.586 14.754 30.022L13.032 23.149C11.5 17.038 17.038 11.5 23.149 13.032L30.022 14.754C33.586 15.647 37.315 14.103 39.204 10.951L42.846 4.873Z",
            ),
            Pair(
                "circle",
                "M99.18 50C99.18 77.162 77.162 99.18 50 99.18 22.838 99.18.82 77.162.82 50 .82 22.839 22.838.82 50 .82 77.162.82 99.18 22.839 99.18 50Z",
            ),
            Pair(
                "square",
                "M99.18 53.689C99.18 67.434 99.18 74.306 97.022 79.758 93.897 87.649 87.649 93.897 79.758 97.022 74.306 99.18 67.434 99.18 53.689 99.18H46.311C32.566 99.18 25.694 99.18 20.242 97.022 12.351 93.897 6.103 87.649 2.978 79.758.82 74.306.82 67.434.82 53.689L.82 46.311C.82 32.566.82 25.694 2.978 20.242 6.103 12.351 12.351 6.103 20.242 2.978 25.694.82 32.566.82 46.311.82L53.689.82C67.434.82 74.306.82 79.758 2.978 87.649 6.103 93.897 12.351 97.022 20.242 99.18 25.694 99.18 32.566 99.18 46.311V53.689Z",
            ),
        )

    // Tabs
    private val _selectedTab = MutableStateFlow(Tab.SHAPE)
    val selectedTab: StateFlow<Tab> = _selectedTab.asStateFlow()
    val tabs: Flow<List<FloatingToolbarTabViewModel>> =
        _selectedTab.map {
            listOf(
                FloatingToolbarTabViewModel(
                    Icon.Resource(
                        res = R.drawable.ic_category_filled_24px,
                        contentDescription = Text.Resource(R.string.preview_name_shape),
                    ),
                    context.getString(R.string.preview_name_shape),
                    it == Tab.SHAPE,
                ) {
                    _selectedTab.value = Tab.SHAPE
                },
                FloatingToolbarTabViewModel(
                    Icon.Resource(
                        res = R.drawable.ic_apps_filled_24px,
                        contentDescription = Text.Resource(R.string.grid_layout),
                    ),
                    context.getString(R.string.grid_layout),
                    it == Tab.GRID,
                ) {
                    _selectedTab.value = Tab.GRID
                },
            )
        }

    // The currently-set system shape option
    val selectedShapeKey = flowOf(shapePaths[0].first)
    private val overridingShapeKey = MutableStateFlow<String?>(null)
    // If the overriding key is null, use the currently-set system shape option
    val previewingShapeKey =
        combine(overridingShapeKey, selectedShapeKey) { overridingShapeOptionKey, selectedShapeKey
            ->
            overridingShapeOptionKey ?: selectedShapeKey
        }

    val shapeOptions: Flow<List<OptionItemViewModel<ShapeIconViewModel>>> =
        MutableStateFlow(shapePaths.map { toShapeOptionItemViewModel(it.first, it.second) })
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)

    // The currently-set system grid option
    val selectedGridOption =
        interactor.selectedGridOption
            .filterNotNull()
            .map { toGridOptionItemViewModel(it) }
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)
    private val overridingGridOptionKey = MutableStateFlow<String?>(null)
    // If the overriding key is null, use the currently-set system grid option
    val previewingGridOptionKey =
        combine(overridingGridOptionKey, selectedGridOption) {
            overridingGridOptionKey,
            selectedGridOption ->
            overridingGridOptionKey ?: selectedGridOption.key.value
        }

    val gridOptions: Flow<List<OptionItemViewModel<GridIconViewModel>>> =
        interactor.gridOptions
            .filterNotNull()
            .map { gridOptions -> gridOptions.map { toGridOptionItemViewModel(it) } }
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)

    val onApply: Flow<(suspend () -> Unit)?> =
        combine(previewingGridOptionKey, selectedGridOption) {
            previewingGridOptionKey,
            selectedGridOption ->
            if (previewingGridOptionKey == selectedGridOption.key.value) {
                null
            } else {
                { interactor.applySelectedOption(previewingGridOptionKey) }
            }
        }

    fun resetPreview() {
        overridingShapeKey.value = null
        overridingGridOptionKey.value = null
        _selectedTab.value = Tab.SHAPE
    }

    private fun toShapeOptionItemViewModel(
        key: String,
        path: String,
    ): OptionItemViewModel<ShapeIconViewModel> {
        val isSelected =
            previewingShapeKey
                .map { it == key }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily,
                    initialValue = false,
                )

        return OptionItemViewModel(
            key = MutableStateFlow(key),
            payload = ShapeIconViewModel(key, path),
            text = Text.Loaded(key),
            isSelected = isSelected,
            onClicked =
                isSelected.map {
                    if (!it) {
                        { overridingShapeKey.value = key }
                    } else {
                        null
                    }
                },
        )
    }

    private fun toGridOptionItemViewModel(
        option: GridOptionModel
    ): OptionItemViewModel<GridIconViewModel> {
        val iconShapePath =
            context.resources.getString(
                Resources.getSystem()
                    .getIdentifier(
                        ResourceConstants.CONFIG_ICON_MASK,
                        "string",
                        ResourceConstants.ANDROID_PACKAGE,
                    )
            )
        val isSelected =
            previewingGridOptionKey
                .map { it == option.key }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily,
                    initialValue = false,
                )

        return OptionItemViewModel(
            key = MutableStateFlow(option.key),
            payload =
                GridIconViewModel(columns = option.cols, rows = option.rows, path = iconShapePath),
            text = Text.Loaded(option.title),
            isSelected = isSelected,
            onClicked =
                isSelected.map {
                    if (!it) {
                        { overridingGridOptionKey.value = option.key }
                    } else {
                        null
                    }
                },
        )
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): ShapeGridPickerViewModel
    }
}
