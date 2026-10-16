package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.codingwithtashi.dailyprayer.ui.theme.CormorantGaramond
import com.codingwithtashi.dailyprayer.ui.theme.InstrumentSans
import com.codingwithtashi.dailyprayer.ui.theme.Jomolhari
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TibetanAwareTextTest {
    private val sans = TextStyle(fontFamily = InstrumentSans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)

    @Test fun latinOnlyIsUnchanged() {
        val r = tibetanAware("Hello 108", sans)
        assertThat(r.style).isEqualTo(sans)
        assertThat(r.text.text).isEqualTo("Hello 108")
        assertThat(r.text.spanStyles).isEmpty()
    }

    @Test fun pureTibetanBecomesJomolhari() {
        val r = tibetanAware("བཀྲ་ཤིས། · ༡༢ / ༡༠༨ %", sans)
        assertThat(r.style.fontFamily).isSameInstanceAs(Jomolhari)
        assertThat(r.style.fontSize.value).isWithin(0.001f).of(20f)
        assertThat(r.style.letterSpacing).isEqualTo(0.sp)
        assertThat(r.style.fontWeight).isEqualTo(FontWeight.Normal)
        assertThat(r.text.spanStyles).isEmpty()
    }

    @Test fun cormorantKeepsSize() {
        val r = tibetanAware("བཀྲ", sans.copy(fontFamily = CormorantGaramond, fontSize = 30.sp))
        assertThat(r.style.fontFamily).isSameInstanceAs(Jomolhari)
        assertThat(r.style.fontSize.value).isWithin(0.001f).of(30f)
    }

    @Test fun mixedGetsSpansOnTibetanRunsOnly() {
        val text = "Start · ཞོགས་པའི་ཉམས་ལེན། end"
        val r = tibetanAware(text, sans)
        assertThat(r.style.fontFamily).isSameInstanceAs(InstrumentSans)
        assertThat(r.text.spanStyles).hasSize(1)
        val span = r.text.spanStyles[0]
        assertThat(text.substring(span.start, span.end)).isEqualTo("ཞོགས་པའི་ཉམས་ལེན།")
        assertThat(span.item.fontFamily).isSameInstanceAs(Jomolhari)
        assertThat(span.item.fontWeight).isEqualTo(FontWeight.Normal)
        assertThat(span.item.letterSpacing).isEqualTo(0.sp)
        assertThat(span.item.fontSize.value).isWithin(0.001f).of(1.25f)
    }

    @Test fun jomolhariStyleUnchanged() {
        val style = TextStyle(fontFamily = Jomolhari, fontSize = 20.sp)
        assertThat(tibetanAware("Start བཀྲ", style).style).isEqualTo(style)
        assertThat(tibetanAware("བཀྲ", style).style).isEqualTo(style)
    }

    @Test fun lineHeightIsRaisedForTibetan() {
        val pure = tibetanAware("བཀྲ", sans.copy(lineHeight = 20.sp))
        assertThat(pure.style.lineHeight.value).isWithin(0.001f).of(30f)
        val mixed = tibetanAware("a བཀྲ", sans.copy(lineHeight = 20.sp))
        assertThat(mixed.style.lineHeight.value).isWithin(0.001f).of(30f)
        val roomy = tibetanAware("བཀྲ", sans.copy(lineHeight = 60.sp))
        assertThat(roomy.style.lineHeight.value).isEqualTo(60f)
    }

    @Test fun annotatedStringKeepsExistingSpans() {
        val base = AnnotatedString("a བཀྲ", listOf(AnnotatedString.Range(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold), 0, 1)))
        val r = tibetanAware(base, sans)
        assertThat(r.text.spanStyles).hasSize(2)
    }
}
