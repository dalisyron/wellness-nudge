package com.mimik.wellnessnudge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.mimik.wellnessnudge.R

private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val plusJakartaSans = GoogleFont("Plus Jakarta Sans")
private val inter = GoogleFont("Inter")

val PlusJakartaSansFamily = FontFamily(
    Font(googleFont = plusJakartaSans, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = plusJakartaSans, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = plusJakartaSans, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = plusJakartaSans, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold),
)

val InterFamily = FontFamily(
    Font(googleFont = inter, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = inter, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = inter, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = inter, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold),
)

/**
 * Typography mapped to Stitch design system tokens (DESIGN.md). Plus Jakarta
 * for headlines + the nudge text itself (it's the "voice" of the product),
 * Inter for everything else.
 */
val WellnessTypography = Typography(
    // headline-lg
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp,
    ),
    // headline-md
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
    ),
    // Used for big nudge result text (Stitch "nudge-text")
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = InterFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
    ),
    // body-lg
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
    ),
    // body-md
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
    ),
    // label-lg
    labelLarge = TextStyle(
        fontFamily = InterFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = InterFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    // label-sm — used as the "TODAY'S SIGNALS" uppercase tracked label
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
