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

package com.android.wallpaper.customization.ui.binder

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.color.ui.binder.ColorOptionIconBinder2
import com.android.customization.picker.color.ui.view.ColorOptionIconView2
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.customization.picker.common.ui.view.SingleRowListItemSpacing
import com.android.customization.picker.mode.ui.binder.DarkModeBinder
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.COLORS
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.view.FloatingToolbar
import com.android.wallpaper.picker.customization.ui.view.adapter.FloatingToolbarTabAdapter
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter2
import java.lang.ref.WeakReference
import kotlinx.coroutines.launch

object ColorsFloatingSheetBinder {

    fun bind(
        view: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val viewModel = optionsViewModel.colorPickerViewModel2
        val isFloatingSheetActive = { optionsViewModel.selectedOption.value == COLORS }

        val tabs = view.requireViewById<FloatingToolbar>(R.id.floating_toolbar)
        val tabContainer =
            tabs.findViewById<ViewGroup>(com.android.wallpaper.R.id.floating_toolbar_tab_container)
        ColorUpdateBinder.bind(
            setColor = { color ->
                DrawableCompat.setTint(DrawableCompat.wrap(tabContainer.background), color)
            },
            color = colorUpdateViewModel.floatingToolbarBackground,
            shouldAnimate = isFloatingSheetActive,
            lifecycleOwner = lifecycleOwner,
        )
        val tabAdapter =
            FloatingToolbarTabAdapter(
                    colorUpdateViewModel = WeakReference(colorUpdateViewModel),
                    shouldAnimateColor = isFloatingSheetActive,
                )
                .also { tabs.setAdapter(it) }

        val floatingSheetContainer =
            view.requireViewById<ViewGroup>(R.id.floating_sheet_content_container)
        ColorUpdateBinder.bind(
            setColor = { color ->
                DrawableCompat.setTint(
                    DrawableCompat.wrap(floatingSheetContainer.background),
                    color,
                )
            },
            color = colorUpdateViewModel.colorSurfaceBright,
            shouldAnimate = isFloatingSheetActive,
            lifecycleOwner = lifecycleOwner,
        )

        val subhead = view.requireViewById<TextView>(R.id.color_type_tab_subhead)

        val colorsAdapter =
            createOptionItemAdapter(
                uiMode = view.resources.configuration.uiMode,
                colorUpdateViewModel = colorUpdateViewModel,
                shouldAnimateColor = isFloatingSheetActive,
                lifecycleOwner = lifecycleOwner,
            )
        val colorsList =
            view.requireViewById<RecyclerView>(R.id.colors_horizontal_list).also {
                it.initColorsList(view.context.applicationContext, colorsAdapter)
            }

        DarkModeBinder.bind(
            darkModeToggle = view.findViewById(R.id.dark_mode_toggle),
            viewModel = optionsViewModel.darkModeViewModel,
            lifecycleOwner = lifecycleOwner,
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.colorTypeTabs.collect { tabAdapter.submitList(it) } }

                launch { viewModel.colorTypeTabSubheader.collect { subhead.text = it } }

                launch {
                    viewModel.colorOptions.collect { colorOptions ->
                        colorsAdapter.setItems(colorOptions) {
                            var indexToFocus = colorOptions.indexOfFirst { it.isSelected.value }
                            indexToFocus = if (indexToFocus < 0) 0 else indexToFocus
                            (colorsList.layoutManager as LinearLayoutManager)
                                .scrollToPositionWithOffset(indexToFocus, 0)
                        }
                    }
                }

                launch {
                    viewModel.previewingColorOption.collect { colorOption ->
                        if (colorOption != null) {
                            colorUpdateViewModel.previewColors(
                                colorOption.seedColor,
                                colorOption.style,
                            )
                        } else colorUpdateViewModel.resetPreview()
                    }
                }
            }
        }
    }

    private fun createOptionItemAdapter(
        uiMode: Int,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): OptionItemAdapter2<ColorOptionIconViewModel> =
        OptionItemAdapter2(
            layoutResourceId = R.layout.color_option2,
            lifecycleOwner = lifecycleOwner,
            bindPayload = { itemView: View, colorIcon: ColorOptionIconViewModel ->
                val colorOptionIconView =
                    itemView.requireViewById<ColorOptionIconView2>(
                        com.android.wallpaper.R.id.background
                    )
                val night = uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
                ColorOptionIconBinder2.bind(colorOptionIconView, colorIcon, night)
                // Return null since it does not need the lifecycleOwner to launch any job for later
                // disposal when rebind.
                return@OptionItemAdapter2 null
            },
            colorUpdateViewModel = WeakReference(colorUpdateViewModel),
            shouldAnimateColor = shouldAnimateColor,
        )

    private fun RecyclerView.initColorsList(
        context: Context,
        adapter: OptionItemAdapter2<ColorOptionIconViewModel>,
    ) {
        apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            addItemDecoration(
                SingleRowListItemSpacing(
                    context.resources.getDimensionPixelSize(
                        R.dimen.floating_sheet_content_horizontal_padding
                    ),
                    0,
                )
            )
        }
    }
}
