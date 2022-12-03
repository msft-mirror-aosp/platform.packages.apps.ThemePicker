/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.customization.model.picker.quickaffordance.ui.viewmodel

import android.content.Context
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.customization.picker.quickaffordance.data.repository.KeyguardQuickAffordancePickerRepository
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordancePickerViewModel
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordanceSlotViewModel
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordanceSummaryViewModel
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordanceViewModel
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots
import com.android.systemui.shared.quickaffordance.data.content.FakeKeyguardQuickAffordanceProviderClient
import com.android.systemui.shared.quickaffordance.data.content.KeyguardQuickAffordanceProviderClient
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(JUnit4::class)
class KeyguardQuickAffordancePickerViewModelTest {

    private lateinit var underTest: KeyguardQuickAffordancePickerViewModel

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    private lateinit var client: FakeKeyguardQuickAffordanceProviderClient

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val coroutineDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(coroutineDispatcher)
        Dispatchers.setMain(coroutineDispatcher)
        client = FakeKeyguardQuickAffordanceProviderClient()

        underTest =
            KeyguardQuickAffordancePickerViewModel.Factory(
                    context = context,
                    interactor =
                        KeyguardQuickAffordancePickerInteractor(
                            repository =
                                KeyguardQuickAffordancePickerRepository(
                                    client = client,
                                    backgroundDispatcher = coroutineDispatcher,
                                ),
                            client = client,
                        ),
                )
                .create(KeyguardQuickAffordancePickerViewModel::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Select an affordance for each side`() =
        testScope.runTest {
            val slots = mutableListOf<Map<String, KeyguardQuickAffordanceSlotViewModel>>()
            val quickAffordances = mutableListOf<List<KeyguardQuickAffordanceViewModel>>()

            val jobs = buildList {
                add(launch { underTest.slots.toList(slots) })
                add(launch { underTest.quickAffordances.toList(quickAffordances) })
            }

            // Initially, the first slot is selected with the "none" affordance selected.
            assertPickerUiState(
                slots = slots.last(),
                affordances = quickAffordances.last(),
                selectedSlotText = "Left button",
                selectedAffordanceText = "None",
            )
            assertPreviewUiState(
                slots = slots.last(),
                expectedAffordanceNameBySlotId =
                    mapOf(
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START to null,
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END to null,
                    ),
            )

            // Select "affordance 1" for the first slot.
            quickAffordances.last()[1].onClicked?.invoke()
            assertPickerUiState(
                slots = slots.last(),
                affordances = quickAffordances.last(),
                selectedSlotText = "Left button",
                selectedAffordanceText = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_1,
            )
            assertPreviewUiState(
                slots = slots.last(),
                expectedAffordanceNameBySlotId =
                    mapOf(
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START to
                            FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_1,
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END to null,
                    ),
            )

            // Select an affordance for the second slot.
            // First, switch to the second slot:
            slots.last()[KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END]?.onClicked?.invoke()
            // Second, select the "affordance 3" affordance:
            quickAffordances.last()[3].onClicked?.invoke()
            assertPickerUiState(
                slots = slots.last(),
                affordances = quickAffordances.last(),
                selectedSlotText = "Right button",
                selectedAffordanceText = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_3,
            )
            assertPreviewUiState(
                slots = slots.last(),
                expectedAffordanceNameBySlotId =
                    mapOf(
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START to
                            FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_1,
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END to
                            FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_3,
                    ),
            )

            // Select a different affordance for the second slot.
            quickAffordances.last()[2].onClicked?.invoke()
            assertPickerUiState(
                slots = slots.last(),
                affordances = quickAffordances.last(),
                selectedSlotText = "Right button",
                selectedAffordanceText = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_2,
            )
            assertPreviewUiState(
                slots = slots.last(),
                expectedAffordanceNameBySlotId =
                    mapOf(
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START to
                            FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_1,
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END to
                            FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_2,
                    ),
            )

            jobs.forEach { it.cancel() }
        }

    @Test
    fun `Unselect - AKA selecting the none affordance - on one side`() =
        testScope.runTest {
            val slots = mutableListOf<Map<String, KeyguardQuickAffordanceSlotViewModel>>()
            val quickAffordances = mutableListOf<List<KeyguardQuickAffordanceViewModel>>()

            val jobs = buildList {
                add(launch { underTest.slots.toList(slots) })
                add(launch { underTest.quickAffordances.toList(quickAffordances) })
            }

            // Select "affordance 1" for the first slot.
            quickAffordances.last()[1].onClicked?.invoke()
            // Select an affordance for the second slot.
            // First, switch to the second slot:
            slots.last()[KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END]?.onClicked?.invoke()
            // Second, select the "affordance 3" affordance:
            quickAffordances.last()[3].onClicked?.invoke()

            // Switch back to the first slot:
            slots.last()[KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START]?.onClicked?.invoke()
            // Select the "none" affordance, which is always in position 0:
            quickAffordances.last()[0].onClicked?.invoke()

            assertPickerUiState(
                slots = slots.last(),
                affordances = quickAffordances.last(),
                selectedSlotText = "Left button",
                selectedAffordanceText = "None",
            )
            assertPreviewUiState(
                slots = slots.last(),
                expectedAffordanceNameBySlotId =
                    mapOf(
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START to null,
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END to
                            FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_3,
                    ),
            )

            jobs.forEach { it.cancel() }
        }

    @Test
    fun `Show enablement dialog when selecting a disabled affordance`() =
        testScope.runTest {
            val slots = mutableListOf<Map<String, KeyguardQuickAffordanceSlotViewModel>>()
            val quickAffordances = mutableListOf<List<KeyguardQuickAffordanceViewModel>>()
            val dialog = mutableListOf<KeyguardQuickAffordancePickerViewModel.DialogViewModel?>()

            val jobs = buildList {
                add(launch { underTest.slots.toList(slots) })
                add(launch { underTest.quickAffordances.toList(quickAffordances) })
                add(launch { underTest.dialog.toList(dialog) })
            }
            val enablementInstructions = listOf("header", "enablementInstructions")
            val enablementActionText = "enablementActionText"
            val packageName = "packageName"
            val action = "action"
            val enablementActionComponentName = "$packageName/$action"
            // Lets add a disabled affordance to the picker:
            val affordanceIndex =
                client.addAffordance(
                    KeyguardQuickAffordanceProviderClient.Affordance(
                        id = "disabled",
                        name = "disabled",
                        iconResourceId = 0,
                        isEnabled = false,
                        enablementInstructions = enablementInstructions,
                        enablementActionText = enablementActionText,
                        enablementActionComponentName = enablementActionComponentName,
                    )
                )

            // Lets try to select that disabled affordance:
            quickAffordances.last()[affordanceIndex + 1].onClicked?.invoke()

            // We expect there to be a dialog that should be shown:
            assertThat(dialog.last()?.instructionHeader).isEqualTo(enablementInstructions[0])
            assertThat(dialog.last()?.instructions)
                .isEqualTo(enablementInstructions.subList(1, enablementInstructions.size))
            assertThat(dialog.last()?.actionText).isEqualTo(enablementActionText)
            assertThat(dialog.last()?.intent?.`package`).isEqualTo(packageName)
            assertThat(dialog.last()?.intent?.action).isEqualTo(action)

            // Once we report that the dialog has been dismissed by the user, we expect there to be
            // no
            // dialog to be shown:
            underTest.onDialogDismissed()
            assertThat(dialog.last()).isNull()

            jobs.forEach { it.cancel() }
        }

    @Test
    fun `summary - affordance selected in both bottom-start and bottom-end`() =
        testScope.runTest {
            val slots = mutableListOf<Map<String, KeyguardQuickAffordanceSlotViewModel>>()
            val quickAffordances = mutableListOf<List<KeyguardQuickAffordanceViewModel>>()
            val summary = mutableListOf<KeyguardQuickAffordanceSummaryViewModel>()
            val jobs = buildList {
                add(launch { underTest.slots.toList(slots) })
                add(launch { underTest.quickAffordances.toList(quickAffordances) })
                add(launch { underTest.summary.toList(summary) })
            }

            // Select "affordance 1" for the first slot.
            quickAffordances.last()[1].onClicked?.invoke()
            // Select an affordance for the second slot.
            // First, switch to the second slot:
            slots.last()[KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END]?.onClicked?.invoke()
            // Second, select the "affordance 3" affordance:
            quickAffordances.last()[3].onClicked?.invoke()

            assertThat(summary.last())
                .isEqualTo(
                    KeyguardQuickAffordanceSummaryViewModel(
                        description =
                            "${FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_1}," +
                                " ${FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_3}",
                        icon1 = FakeKeyguardQuickAffordanceProviderClient.ICON_1,
                        icon2 = FakeKeyguardQuickAffordanceProviderClient.ICON_3,
                        isIconSpacingVisible = true,
                    )
                )
            jobs.forEach { it.cancel() }
        }

    @Test
    fun `summary - affordance selected only on bottom-start`() =
        testScope.runTest {
            val slots = mutableListOf<Map<String, KeyguardQuickAffordanceSlotViewModel>>()
            val quickAffordances = mutableListOf<List<KeyguardQuickAffordanceViewModel>>()
            val summary = mutableListOf<KeyguardQuickAffordanceSummaryViewModel>()
            val jobs = buildList {
                add(launch { underTest.slots.toList(slots) })
                add(launch { underTest.quickAffordances.toList(quickAffordances) })
                add(launch { underTest.summary.toList(summary) })
            }

            // Select "affordance 1" for the first slot.
            quickAffordances.last()[1].onClicked?.invoke()

            assertThat(summary.last())
                .isEqualTo(
                    KeyguardQuickAffordanceSummaryViewModel(
                        description = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_1,
                        icon1 = FakeKeyguardQuickAffordanceProviderClient.ICON_1,
                        icon2 = null,
                        isIconSpacingVisible = false,
                    )
                )
            jobs.forEach { it.cancel() }
        }

    @Test
    fun `summary - affordance selected only on bottom-end`() =
        testScope.runTest {
            val slots = mutableListOf<Map<String, KeyguardQuickAffordanceSlotViewModel>>()
            val quickAffordances = mutableListOf<List<KeyguardQuickAffordanceViewModel>>()
            val summary = mutableListOf<KeyguardQuickAffordanceSummaryViewModel>()
            val jobs = buildList {
                add(launch { underTest.slots.toList(slots) })
                add(launch { underTest.quickAffordances.toList(quickAffordances) })
                add(launch { underTest.summary.toList(summary) })
            }

            // Select an affordance for the second slot.
            // First, switch to the second slot:
            slots.last()[KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END]?.onClicked?.invoke()
            // Second, select the "affordance 3" affordance:
            quickAffordances.last()[3].onClicked?.invoke()

            assertThat(summary.last())
                .isEqualTo(
                    KeyguardQuickAffordanceSummaryViewModel(
                        description = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_3,
                        icon1 = null,
                        icon2 = FakeKeyguardQuickAffordanceProviderClient.ICON_3,
                        isIconSpacingVisible = false,
                    )
                )
            jobs.forEach { it.cancel() }
        }

    @Test
    fun `summary - no affordances selected`() =
        testScope.runTest {
            val slots = mutableListOf<Map<String, KeyguardQuickAffordanceSlotViewModel>>()
            val quickAffordances = mutableListOf<List<KeyguardQuickAffordanceViewModel>>()
            val summary = mutableListOf<KeyguardQuickAffordanceSummaryViewModel>()
            val jobs = buildList {
                add(launch { underTest.slots.toList(slots) })
                add(launch { underTest.quickAffordances.toList(quickAffordances) })
                add(launch { underTest.summary.toList(summary) })
            }

            assertThat(summary.last().description).isEqualTo("None")
            assertThat(summary.last().icon1).isNotNull()
            assertThat(summary.last().icon2).isNull()
            assertThat(summary.last().isIconSpacingVisible).isFalse()
            jobs.forEach { it.cancel() }
        }

    /**
     * Asserts the entire picker UI state is what is expected. This includes the slot tabs and the
     * affordance list.
     *
     * @param slots The observed slot view-models, keyed by slot ID
     * @param affordances The observed affordances
     * @param selectedSlotText The text of the slot that's expected to be selected
     * @param selectedAffordanceText The text of the affordance that's expected to be selected
     */
    private fun assertPickerUiState(
        slots: Map<String, KeyguardQuickAffordanceSlotViewModel>,
        affordances: List<KeyguardQuickAffordanceViewModel>,
        selectedSlotText: String,
        selectedAffordanceText: String,
    ) {
        assertSlotTabUiState(
            slots = slots,
            slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START,
            isSelected = "Left button" == selectedSlotText,
        )
        assertSlotTabUiState(
            slots = slots,
            slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END,
            isSelected = "Right button" == selectedSlotText,
        )

        var foundSelectedAffordance = false
        affordances.forEach { affordance ->
            val nameMatchesSelectedName = affordance.contentDescription == selectedAffordanceText
            assertWithMessage(
                    "Expected affordance with name \"${affordance.contentDescription}\" to have" +
                        " isSelected=$nameMatchesSelectedName but it was ${affordance.isSelected}"
                )
                .that(affordance.isSelected)
                .isEqualTo(nameMatchesSelectedName)
            foundSelectedAffordance = foundSelectedAffordance || nameMatchesSelectedName
        }
        assertWithMessage("No affordance is selected!").that(foundSelectedAffordance).isTrue()
    }

    /**
     * Asserts that a slot tab has the correct UI state.
     *
     * @param slots The observed slot view-models, keyed by slot ID
     * @param slotId the ID of the slot to assert
     * @param isSelected Whether that slot should be selected
     */
    private fun assertSlotTabUiState(
        slots: Map<String, KeyguardQuickAffordanceSlotViewModel>,
        slotId: String,
        isSelected: Boolean,
    ) {
        val viewModel = slots[slotId] ?: error("No slot with ID \"$slotId\"!")
        assertThat(viewModel.isSelected).isEqualTo(isSelected)
    }

    /**
     * Asserts the UI state of the preview.
     *
     * @param slots The observed slot view-models, keyed by slot ID
     * @param expectedAffordanceNameBySlotId The expected name of the selected affordance for each
     * slot ID or `null` if it's expected for there to be no affordance for that slot in the preview
     */
    private fun assertPreviewUiState(
        slots: Map<String, KeyguardQuickAffordanceSlotViewModel>,
        expectedAffordanceNameBySlotId: Map<String, String?>,
    ) {
        slots.forEach { (slotId, slotViewModel) ->
            val expectedAffordanceName = expectedAffordanceNameBySlotId[slotId]
            val actualAffordanceName =
                slotViewModel.selectedQuickAffordances.firstOrNull()?.contentDescription
            assertWithMessage(
                    "At slotId=\"$slotId\", expected affordance=\"$expectedAffordanceName\" but" +
                        " was \"$actualAffordanceName\"!"
                )
                .that(actualAffordanceName)
                .isEqualTo(expectedAffordanceName)
        }
    }
}
