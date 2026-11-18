package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class ClipThemePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val isDark: Boolean,
    val isHighContrastBorder: Boolean = false,
    val bgColor: Color,
    val surfaceColor: Color,
    val surfaceVariantColor: Color,
    val primaryColor: Color,
    val onPrimaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val borderSubtle: Color
) {
    OLED_MIDNIGHT(
        id = "OLED_MIDNIGHT",
        title = "OLED Midnight",
        subtitle = "True #000000 black with low-glare emerald & sky telemetry",
        isDark = true,
        bgColor = Color(0xFF000000),
        surfaceColor = Color(0xFF0B1016),
        surfaceVariantColor = Color(0xFF141D26),
        primaryColor = Color(0xFF10B981),
        onPrimaryColor = Color(0xFF022C1E),
        secondaryColor = Color(0xFF38BDF8),
        tertiaryColor = Color(0xFFA78BFA),
        textPrimary = Color(0xFFE6EDF3),
        textSecondary = Color(0xFF8B949E),
        borderSubtle = Color(0xFF212B36)
    ),
    TERMINAL_GREEN(
        id = "TERMINAL_GREEN",
        title = "Terminal Phosphor Green",
        subtitle = "Retro CRT phosphor matrix engineered to eliminate white glare",
        isDark = true,
        bgColor = Color(0xFF040B06),
        surfaceColor = Color(0xFF09160C),
        surfaceVariantColor = Color(0xFF102615),
        primaryColor = Color(0xFF33FF66),
        onPrimaryColor = Color(0xFF021F09),
        secondaryColor = Color(0xFF22CC52),
        tertiaryColor = Color(0xFF80FF9E),
        textPrimary = Color(0xFF66FF8C),
        textSecondary = Color(0xFF36994E),
        borderSubtle = Color(0xFF1B4324)
    ),
    WARM_AMBER_CRT(
        id = "WARM_AMBER_CRT",
        title = "Warm Amber CRT",
        subtitle = "Zero-blue-light gold & copper telemetry for late-night sessions",
        isDark = true,
        bgColor = Color(0xFF100A03),
        surfaceColor = Color(0xFF1B1106),
        surfaceVariantColor = Color(0xFF281A0A),
        primaryColor = Color(0xFFFFB000),
        onPrimaryColor = Color(0xFF261700),
        secondaryColor = Color(0xFFFF7B00),
        tertiaryColor = Color(0xFFFFD166),
        textPrimary = Color(0xFFFFD580),
        textSecondary = Color(0xFFB8862D),
        borderSubtle = Color(0xFF422C12)
    ),
    CYBER_NAVY_CYAN(
        id = "CYBER_NAVY_CYAN",
        title = "Cybernetic Deep Navy",
        subtitle = "Abyssal navy command deck with bioluminescent cyan accents",
        isDark = true,
        bgColor = Color(0xFF060C17),
        surfaceColor = Color(0xFF0D1829),
        surfaceVariantColor = Color(0xFF15263E),
        primaryColor = Color(0xFF00E5FF),
        onPrimaryColor = Color(0xFF00262B),
        secondaryColor = Color(0xFF7C4DFF),
        tertiaryColor = Color(0xFF18FFFF),
        textPrimary = Color(0xFFE2F1FF),
        textSecondary = Color(0xFF7FA1C3),
        borderSubtle = Color(0xFF203859)
    ),
    SEPIA_PAPER(
        id = "SEPIA_PAPER",
        title = "Sepia Paper",
        subtitle = "Warm e-reader parchment and espresso ink for daytime comfort",
        isDark = false,
        bgColor = Color(0xFFF4ECD8),
        surfaceColor = Color(0xFFEAE0C8),
        surfaceVariantColor = Color(0xFFDFD2B5),
        primaryColor = Color(0xFF8C4A27),
        onPrimaryColor = Color(0xFFFFF8F0),
        secondaryColor = Color(0xFF2E6B5E),
        tertiaryColor = Color(0xFF6B4C35),
        textPrimary = Color(0xFF2C221E),
        textSecondary = Color(0xFF655348),
        borderSubtle = Color(0xFFC8B89A)
    ),
    E_INK_MONOCHROME(
        id = "E_INK_MONOCHROME",
        title = "High-Contrast E-Ink",
        subtitle = "Crisp matte monochrome paper & pure ink with bold borders",
        isDark = false,
        isHighContrastBorder = true,
        bgColor = Color(0xFFF6F6F2),
        surfaceColor = Color(0xFFFFFFFF),
        surfaceVariantColor = Color(0xFFE8E8E3),
        primaryColor = Color(0xFF121212),
        onPrimaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFF333333),
        tertiaryColor = Color(0xFF000000),
        textPrimary = Color(0xFF0D0D0D),
        textSecondary = Color(0xFF424242),
        borderSubtle = Color(0xFF1A1A1A)
    ),
    STANDARD_DARK(
        id = "STANDARD_DARK",
        title = "Standard Slate Dark",
        subtitle = "Balanced charcoal slate with soft mint and indigo indicators",
        isDark = true,
        bgColor = Color(0xFF0F172A),
        surfaceColor = Color(0xFF1E293B),
        surfaceVariantColor = Color(0xFF334155),
        primaryColor = Color(0xFF34D399),
        onPrimaryColor = Color(0xFF062E22),
        secondaryColor = Color(0xFF60A5FA),
        tertiaryColor = Color(0xFFC084FC),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        borderSubtle = Color(0xFF334155)
    ),
    STANDARD_LIGHT(
        id = "STANDARD_LIGHT",
        title = "Standard Daylight",
        subtitle = "Clean laboratory white with crisp emerald & slate contrast",
        isDark = false,
        bgColor = Color(0xFFF1F5F9),
        surfaceColor = Color(0xFFFFFFFF),
        surfaceVariantColor = Color(0xFFE2E8F0),
        primaryColor = Color(0xFF059669),
        onPrimaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFF0284C7),
        tertiaryColor = Color(0xFF7C3AED),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        borderSubtle = Color(0xFFCBD5E1)
    );

    companion object {
        fun fromId(id: String): ClipThemePreset {
            return entries.firstOrNull { it.id == id } ?: OLED_MIDNIGHT
        }
    }
}
