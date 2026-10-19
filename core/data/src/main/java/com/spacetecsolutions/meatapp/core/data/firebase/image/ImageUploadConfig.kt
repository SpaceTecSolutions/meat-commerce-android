package com.spacetecsolutions.meatapp.core.data.firebase.image

/** Per-destination image policy. Dimensions describe the longest edge; images are never enlarged. */
enum class ImageUploadKind(
    val maxDimension: Int,
    val targetBytes: Long,
    val maxBytes: Long,
) {
    PRODUCT(2048, 1_000_000, 5 * 1024 * 1024L),
    CATEGORY(1600, 700 * 1024L, 5 * 1024 * 1024L),
    SUBCATEGORY(1600, 700 * 1024L, 5 * 1024 * 1024L),
    BANNER(2400, 1_500 * 1024L, 5 * 1024 * 1024L),
    PROFILE(1024, 400 * 1024L, 2 * 1024 * 1024L),
}
