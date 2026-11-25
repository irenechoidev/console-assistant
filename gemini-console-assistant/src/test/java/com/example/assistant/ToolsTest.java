package com.example.assistant;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolsTest {

    @Test
    void calculatorHandlesPrecedenceAndParentheses() {
        assertEquals(14, Calculator.evaluate("2 + 3 * 4"));
        assertEquals(20, Calculator.evaluate("(2 + 3) * 4"));
        assertEquals(512, Calculator.evaluate("2 ^ 3 ^ 2")); // right-associative
        assertEquals(-5, Calculator.evaluate("-(2 + 3)"));
        assertEquals(409.5, Calculator.evaluate("2340 * 17.5 / 100"), 1e-9);
    }

    @Test
    void calculatorRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> Calculator.evaluate("2 +"));
        assertThrows(IllegalArgumentException.class, () -> Calculator.evaluate("1 / 0"));
        assertThrows(IllegalArgumentException.class, () -> Calculator.evaluate("System.exit(0)"));
    }

    @Test
    void calculateToolFormatsWholeNumbers() {
        assertEquals("14", Tools.execute("calculate", Map.of("expression", "2 + 3 * 4")));
        assertEquals("2.5", Tools.execute("calculate", Map.of("expression", "5 / 2")));
    }

    @Test
    void timeToolValidatesTimeZone() {
        assertTrue(Tools.execute("get_current_time", Map.of("timezone", "Asia/Seoul")).contains("Asia/Seoul"));
        assertThrows(IllegalArgumentException.class,
                () -> Tools.execute("get_current_time", Map.of("timezone", "Mars/Base")));
    }

    @Test
    void unknownToolIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Tools.execute("delete_files", Map.of()));
    }

    @Test
    void daysBetweenCalculatesCorrectly() {
        assertEquals("31 days", Tools.execute("days_between", Map.of("date1", "2026-11-25", "date2", "2026-12-26")));
        assertEquals("31 days", Tools.execute("days_between", Map.of("date1", "2026-12-26", "date2", "2026-11-25")));
        assertEquals("0 days", Tools.execute("days_between", Map.of("date1", "2026-12-25", "date2", "2026-12-25")));
    }

    @Test
    void daysBetweenRejectsInvalidDates() {
        assertThrows(IllegalArgumentException.class,
                () -> Tools.execute("days_between", Map.of("date1", "not-a-date", "date2", "2026-12-25")));
    }

    @Test
    void convertUnitsHandlesLengthWeightTemperature() {
        assertEquals("1.60934 km", Tools.execute("convert_units", Map.of("value", 1.0, "from", "miles", "to", "km")));
        assertEquals("2.20462 lbs", Tools.execute("convert_units", Map.of("value", 1.0, "from", "kg", "to", "lbs")));
        assertEquals("32 fahrenheit", Tools.execute("convert_units", Map.of("value", 0.0, "from", "celsius", "to", "fahrenheit")));
        assertEquals("100 celsius", Tools.execute("convert_units", Map.of("value", 212.0, "from", "fahrenheit", "to", "celsius")));
    }

    @Test
    void convertUnitsRejectsUnsupportedConversion() {
        assertThrows(IllegalArgumentException.class,
                () -> Tools.execute("convert_units", Map.of("value", 1.0, "from", "km", "to", "kg")));
    }

    @Test
    void convertCurrencyConvertsCorrectly() {
        String result = Tools.execute("convert_currency", Map.of("amount", 1.0, "from", "USD", "to", "USD"));
        assertTrue(result.startsWith("1 USD"));

        String usdToEur = Tools.execute("convert_currency", Map.of("amount", 100.0, "from", "USD", "to", "EUR"));
        assertTrue(usdToEur.contains("EUR"));
    }

    @Test
    void convertCurrencyRejectsUnsupportedCurrency() {
        assertThrows(IllegalArgumentException.class,
                () -> Tools.execute("convert_currency", Map.of("amount", 100.0, "from", "USD", "to", "BTC")));
    }
}
