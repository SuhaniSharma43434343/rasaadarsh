package com.example.rasaushadhies.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.example.rasaushadhies.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val TiroDevanagariFont = GoogleFont("Tiro Devanagari Sanskrit")

val TiroDevanagariFontFamily = FontFamily(
    Font(googleFont = TiroDevanagariFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = TiroDevanagariFont, fontProvider = provider, weight = FontWeight.Bold)
)

// Fallback if Google Fonts not available — uses system serif
val ShlokaFontFamily: FontFamily
    get() = TiroDevanagariFontFamily
