package com.spacetecsolutions.meatapp.core.data.firebase.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageUploadKindTest {
    @Test
    fun `upload policies match destination limits`() {
        assertEquals(2048, ImageUploadKind.PRODUCT.maxDimension)
        assertEquals(1_000_000L, ImageUploadKind.PRODUCT.targetBytes)
        assertEquals(1600, ImageUploadKind.CATEGORY.maxDimension)
        assertEquals(700 * 1024L, ImageUploadKind.CATEGORY.targetBytes)
        assertEquals(ImageUploadKind.CATEGORY.maxDimension, ImageUploadKind.SUBCATEGORY.maxDimension)
        assertEquals(2400, ImageUploadKind.BANNER.maxDimension)
        assertEquals(1_500 * 1024L, ImageUploadKind.BANNER.targetBytes)
        assertEquals(1024, ImageUploadKind.PROFILE.maxDimension)
        assertEquals(400 * 1024L, ImageUploadKind.PROFILE.targetBytes)
        assertEquals(2 * 1024 * 1024L, ImageUploadKind.PROFILE.maxBytes)
    }

    @Test
    fun `target size never exceeds hard upload cap`() {
        ImageUploadKind.values().forEach { kind ->
            assertTrue(kind.targetBytes <= kind.maxBytes)
            assertTrue(kind.maxBytes > 0)
        }
    }
}
