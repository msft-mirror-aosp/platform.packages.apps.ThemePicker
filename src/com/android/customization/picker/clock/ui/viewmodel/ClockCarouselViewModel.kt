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
 */
package com.android.customization.picker.clock.ui.viewmodel

import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClockCarouselViewModel(private val interactor: ClockPickerInteractor) {
    val selectedClockId: Flow<String> = interactor.selectedClock.map { it.clockId }

    val allClockIds: Array<String> = interactor.allClocks.map { it.clockId }.toTypedArray()

    fun setSelectedClock(clockId: String) {
        interactor.setSelectedClock(clockId)
    }
}
