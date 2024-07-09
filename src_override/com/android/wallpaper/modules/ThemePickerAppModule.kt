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
package com.android.wallpaper.modules

import android.content.Context
import com.android.customization.model.color.ColorCustomizationManager
import com.android.customization.model.theme.OverlayManagerCompat
import com.android.customization.module.CustomizationInjector
import com.android.customization.module.DefaultCustomizationPreferences
import com.android.customization.module.ThemePickerInjector
import com.android.customization.module.logging.ThemesUserEventLogger
import com.android.customization.module.logging.ThemesUserEventLoggerImpl
import com.android.customization.picker.color.data.repository.ColorPickerRepository
import com.android.customization.picker.color.data.repository.ColorPickerRepositoryImpl
import com.android.systemui.shared.customization.data.content.CustomizationProviderClient
import com.android.systemui.shared.customization.data.content.CustomizationProviderClientImpl
import com.android.wallpaper.customization.ui.binder.ThemePickerCustomizationOptionsBinder
import com.android.wallpaper.module.DefaultPartnerProvider
import com.android.wallpaper.module.PartnerProvider
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.picker.preview.ui.util.DefaultImageEffectDialogUtil
import com.android.wallpaper.picker.preview.ui.util.ImageEffectDialogUtil
import com.android.wallpaper.util.converter.DefaultWallpaperModelFactory
import com.android.wallpaper.util.converter.WallpaperModelFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class ThemePickerAppModule {
    @Binds @Singleton abstract fun bindInjector(impl: ThemePickerInjector): CustomizationInjector

    @Binds
    @Singleton
    abstract fun bindUserEventLogger(impl: ThemesUserEventLoggerImpl): UserEventLogger

    @Binds
    @Singleton
    abstract fun bindThemesUserEventLogger(impl: ThemesUserEventLoggerImpl): ThemesUserEventLogger

    @Binds
    @Singleton
    abstract fun bindWallpaperModelFactory(
        impl: DefaultWallpaperModelFactory
    ): WallpaperModelFactory

    @Binds
    @Singleton
    abstract fun bindPartnerProvider(impl: DefaultPartnerProvider): PartnerProvider

    @Binds
    @Singleton
    abstract fun bindEffectsWallpaperDialogUtil(
        impl: DefaultImageEffectDialogUtil
    ): ImageEffectDialogUtil

    @Binds
    @Singleton
    abstract fun bindCustomizationOptionsBinder(
        impl: ThemePickerCustomizationOptionsBinder
    ): CustomizationOptionsBinder

    @Binds
    @Singleton
    abstract fun bindColorPickerRepository(impl: ColorPickerRepositoryImpl): ColorPickerRepository

    @Binds
    @Singleton
    abstract fun bindWallpaperPreferences(
        impl: DefaultCustomizationPreferences
    ): WallpaperPreferences

    companion object {

        @Provides
        @Singleton
        fun provideColorCustomizationManager(
            @ApplicationContext context: Context
        ): ColorCustomizationManager {
            return ColorCustomizationManager.getInstance(context, OverlayManagerCompat(context))
        }

        @Provides
        @Singleton
        fun provideCustomizationProviderClient(
            @ApplicationContext context: Context,
            @BackgroundDispatcher bgDispatcher: CoroutineDispatcher,
        ): CustomizationProviderClient {
            return CustomizationProviderClientImpl(context, bgDispatcher)
        }
    }
}
