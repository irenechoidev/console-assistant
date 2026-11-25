package com.example.assistant;

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Tool;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public final class Tools {
    private Tools() {}

    // ---------- 1. Definitions sent to the model ----------

    public static final FunctionDeclaration GET_CURRENT_TIME = FunctionDeclaration.builder()
            .name("get_current_time")
            .description("Get the current date and time. Use this whenever the user asks about "
                    + "the current time, today's date, or the day of the week.")
            .parameters(Schema.builder()
                    .type("OBJECT")
                    .properties(Map.of(
                            "timezone", Schema.builder()
                                    .type("STRING")
                                    .description("IANA time zone, e.g. 'Asia/Seoul' or 'America/New_York'. "
                                            + "Omit to use the computer's local time zone.")
                                    .build()))
                    .build())
            .build();

    public static final FunctionDeclaration CALCULATE = FunctionDeclaration.builder()
            .name("calculate")
            .description("Evaluate a math expression exactly. Supports + - * / % ^ and parentheses. "
                    + "Use this for any arithmetic instead of calculating in your head.")
            .parameters(Schema.builder()
                    .type("OBJECT")
                    .properties(Map.of(
                            "expression", Schema.builder()
                                    .type("STRING")
                                    .description("The expression, e.g. '(12.5 + 7) * 3'")
                                    .build()))
                    .required(List.of("expression"))
                    .build())
            .build();

    public static final FunctionDeclaration DAYS_UNTIL = FunctionDeclaration.builder()
            .name("days_until")
            .description("Calculate the number of days from today until a future date. "
                    + "Use this for any question about how many days until an event.")
            .parameters(Schema.builder()
                    .type("OBJECT")
                    .properties(Map.of(
                            "date", Schema.builder()
                                    .type("STRING")
                                    .description("The target date in yyyy-MM-dd format, e.g. '2026-12-25'.")
                                    .build()))
                    .required(List.of("date"))
                    .build())
            .build();

    public static final FunctionDeclaration DAYS_BETWEEN = FunctionDeclaration.builder()
            .name("days_between")
            .description("Calculate the number of days between two dates.")
            .parameters(Schema.builder()
                    .type("OBJECT")
                    .properties(Map.of(
                            "date1", Schema.builder()
                                    .type("STRING")
                                    .description("The start date in yyyy-MM-dd format, e.g. '2026-01-01'.")
                                    .build(),
                            "date2", Schema.builder()
                                    .type("STRING")
                                    .description("The end date in yyyy-MM-dd format, e.g. '2026-12-31'.")
                                    .build()))
                    .required(List.of("date1", "date2"))
                    .build())
            .build();

    public static final FunctionDeclaration CONVERT_UNITS = FunctionDeclaration.builder()
            .name("convert_units")
            .description("Convert a value between units of length, weight, or temperature. "
                    + "Supported units: km, miles, meters, feet, inches, cm (length); "
                    + "kg, lbs, g, oz (weight); celsius, fahrenheit, kelvin (temperature).")
            .parameters(Schema.builder()
                    .type("OBJECT")
                    .properties(Map.of(
                            "value", Schema.builder()
                                    .type("NUMBER")
                                    .description("The numeric value to convert.")
                                    .build(),
                            "from", Schema.builder()
                                    .type("STRING")
                                    .description("The unit to convert from, e.g. 'km', 'celsius', 'lbs'.")
                                    .build(),
                            "to", Schema.builder()
                                    .type("STRING")
                                    .description("The unit to convert to, e.g. 'miles', 'fahrenheit', 'kg'.")
                                    .build()))
                    .required(List.of("value", "from", "to"))
                    .build())
            .build();

    public static final FunctionDeclaration CONVERT_CURRENCY = FunctionDeclaration.builder()
            .name("convert_currency")
            .description("Convert an amount between currencies using fixed exchange rates. "
                    + "Supported currencies: USD, EUR, GBP, JPY, KRW, CAD, AUD, CNY.")
            .parameters(Schema.builder()
                    .type("OBJECT")
                    .properties(Map.of(
                            "amount", Schema.builder()
                                    .type("NUMBER")
                                    .description("The amount to convert.")
                                    .build(),
                            "from", Schema.builder()
                                    .type("STRING")
                                    .description("The currency code to convert from, e.g. 'USD'.")
                                    .build(),
                            "to", Schema.builder()
                                    .type("STRING")
                                    .description("The currency code to convert to, e.g. 'KRW'.")
                                    .build()))
                    .required(List.of("amount", "from", "to"))
                    .build())
            .build();

    public static final Tool ALL = Tool.builder()
            .functionDeclarations(List.of(
                    GET_CURRENT_TIME, CALCULATE, DAYS_UNTIL,
                    DAYS_BETWEEN, CONVERT_UNITS, CONVERT_CURRENCY))
            .build();

    // ---------- 2. Code that runs when the model asks ----------

    public static String execute(String toolName, Map<String, Object> input) {
        return switch (toolName) {
            case "get_current_time"  -> getCurrentTime(input);
            case "calculate"         -> calculate(input);
            case "days_until"        -> daysUntil(input);
            case "days_between"      -> daysBetween(input);
            case "convert_units"     -> convertUnits(input);
            case "convert_currency"  -> convertCurrency(input);
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }

    private static String getCurrentTime(Map<String, Object> input) {
        Object tz = input.get("timezone");
        ZoneId zone;
        if (tz == null || tz.toString().isBlank()) {
            zone = ZoneId.systemDefault();
        } else {
            try {
                zone = ZoneId.of(tz.toString());
            } catch (Exception e) {
                throw new IllegalArgumentException("Unknown time zone: " + tz);
            }
        }
        ZonedDateTime now = ZonedDateTime.now(zone);
        return now.format(DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd HH:mm:ss")) + " (" + zone + ")";
    }

    private static String calculate(Map<String, Object> input) {
        Object expr = input.get("expression");
        if (!(expr instanceof String expression)) {
            throw new IllegalArgumentException("'expression' must be a string");
        }
        double result = Calculator.evaluate(expression);
        return formatDouble(result);
    }

    private static String daysUntil(Map<String, Object> input) {
        Object dateObj = input.get("date");
        if (!(dateObj instanceof String dateStr)) {
            throw new IllegalArgumentException("'date' must be a string");
        }
        LocalDate target = parseDate(dateStr);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        long days = ChronoUnit.DAYS.between(today, target);
        if (days < 0) return Math.abs(days) + " days ago";
        if (days == 0) return "today";
        return days + " days";
    }

    private static String daysBetween(Map<String, Object> input) {
        Object d1 = input.get("date1");
        Object d2 = input.get("date2");
        if (!(d1 instanceof String) || !(d2 instanceof String)) {
            throw new IllegalArgumentException("'date1' and 'date2' must be strings");
        }
        LocalDate date1 = parseDate((String) d1);
        LocalDate date2 = parseDate((String) d2);
        long days = Math.abs(ChronoUnit.DAYS.between(date1, date2));
        return days + " days";
    }

    private static String convertUnits(Map<String, Object> input) {
        double value = toDouble(input.get("value"), "value");
        String from = toString(input.get("from"), "from").toLowerCase();
        String to = toString(input.get("to"), "to").toLowerCase();

        double result = switch (from + "->" + to) {
            // Length
            case "km->miles"    -> value * 0.621371;
            case "miles->km"    -> value * 1.60934;
            case "meters->feet" -> value * 3.28084;
            case "feet->meters" -> value * 0.3048;
            case "cm->inches"   -> value * 0.393701;
            case "inches->cm"   -> value * 2.54;
            case "meters->km"   -> value / 1000;
            case "km->meters"   -> value * 1000;
            // Weight
            case "kg->lbs"      -> value * 2.20462;
            case "lbs->kg"      -> value * 0.453592;
            case "g->oz"        -> value * 0.035274;
            case "oz->g"        -> value * 28.3495;
            case "kg->g"        -> value * 1000;
            case "g->kg"        -> value / 1000;
            // Temperature
            case "celsius->fahrenheit"    -> value * 9 / 5 + 32;
            case "fahrenheit->celsius"    -> (value - 32) * 5 / 9;
            case "celsius->kelvin"        -> value + 273.15;
            case "kelvin->celsius"        -> value - 273.15;
            case "fahrenheit->kelvin"     -> (value - 32) * 5 / 9 + 273.15;
            case "kelvin->fahrenheit"     -> (value - 273.15) * 9 / 5 + 32;
            default -> {
                if (from.equals(to)) yield value;
                throw new IllegalArgumentException("Unsupported conversion: " + from + " to " + to);
            }
        };
        return formatDouble(result) + " " + to;
    }

    private static String convertCurrency(Map<String, Object> input) {
        double amount = toDouble(input.get("amount"), "amount");
        String from = toString(input.get("from"), "from").toUpperCase();
        String to = toString(input.get("to"), "to").toUpperCase();

        // Fixed rates relative to USD (approximate)
        Map<String, Double> toUsd = Map.of(
                "USD", 1.0,
                "EUR", 1.08,
                "GBP", 1.27,
                "JPY", 0.0067,
                "KRW", 0.00072,
                "CAD", 0.74,
                "AUD", 0.65,
                "CNY", 0.14
        );

        Double fromRate = toUsd.get(from);
        Double toRate = toUsd.get(to);
        if (fromRate == null) throw new IllegalArgumentException("Unsupported currency: " + from);
        if (toRate == null)   throw new IllegalArgumentException("Unsupported currency: " + to);

        double result = amount * fromRate / toRate;
        return formatDouble(result) + " " + to + " (fixed rates — not live)";
    }

    // ---------- Helpers ----------

    private static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd, e.g. '2026-12-25'.");
        }
    }

    private static double toDouble(Object value, String fieldName) {
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("'" + fieldName + "' must be a number");
        }
    }

    private static String toString(Object value, String fieldName) {
        if (value == null) throw new IllegalArgumentException("'" + fieldName + "' is required");
        return value.toString();
    }

    private static String formatDouble(double result) {
        return (result == Math.rint(result) && Math.abs(result) < 1e15)
                ? String.valueOf((long) result)
                : String.valueOf(result);
    }
}