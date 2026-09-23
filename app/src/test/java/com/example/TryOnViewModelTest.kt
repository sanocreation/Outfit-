package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.model.ClothingCategory
import com.example.model.TryOnUiState
import com.example.ui.TryOnViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TryOnViewModelTest {

    private lateinit var viewModel: TryOnViewModel

    @Before
    fun setup() {
        viewModel = TryOnViewModel(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun testInitialState() {
        assertNull(viewModel.userImage.value)
        assertNull(viewModel.garmentImage.value)
        assertEquals(ClothingCategory.BLAZERS, viewModel.selectedCategory.value)
        assertTrue(viewModel.uiState.value is TryOnUiState.Idle)
        assertTrue(viewModel.config.value.demoMode)
    }

    @Test
    fun testCategorySelection() {
        viewModel.setCategory(ClothingCategory.DRESSES)
        assertEquals(ClothingCategory.DRESSES, viewModel.selectedCategory.value)

        viewModel.setCategory(ClothingCategory.TOPS)
        assertEquals(ClothingCategory.TOPS, viewModel.selectedCategory.value)
    }

    @Test
    fun testLoadSamplePreset() {
        viewModel.loadSamplePreset()
        assertNotNull(viewModel.userImage.value)
        assertNotNull(viewModel.garmentImage.value)
        assertEquals(R.drawable.img_sample_model, viewModel.userImage.value?.drawableResId)
        assertEquals(R.drawable.img_sample_garment, viewModel.garmentImage.value?.drawableResId)
    }

    @Test
    fun testTryAnotherOutfitPreservesPerson() {
        viewModel.loadSamplePreset()
        assertNotNull(viewModel.userImage.value)
        assertNotNull(viewModel.garmentImage.value)

        viewModel.tryAnotherOutfit(keepPerson = true)
        assertNotNull(viewModel.userImage.value)
        assertNull(viewModel.garmentImage.value)
        assertTrue(viewModel.uiState.value is TryOnUiState.Idle)
    }
}
