/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class OmniboxColors(
    val container: Color,
    val content: Color,
    val placeholder: Color,
    val trailingIcon: Color
)