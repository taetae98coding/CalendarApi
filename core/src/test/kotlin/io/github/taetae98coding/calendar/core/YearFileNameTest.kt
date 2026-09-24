package io.github.taetae98coding.calendar.core

import io.github.taetae98coding.calendar.core.file.YearFileName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YearFileNameTest {
    @Test
    fun `연도 항목 이름은 파일과 폴더 두 가지다`() {
        assertEquals(2026, YearFileName.yearOf("2026"))
        assertEquals(2026, YearFileName.yearOf("2026.json"))
        assertNull(YearFileName.yearOf("meta.json"))
        assertNull(YearFileName.yearOf("20260.json"))
        assertEquals("2026.json", YearFileName.file(2026))
        assertEquals("2026", YearFileName.directory(2026))
    }
}
