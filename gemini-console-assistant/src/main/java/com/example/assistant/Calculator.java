package com.example.assistant;

/**
 * Grammar (recursive descent):
 *   expression := term   (('+' | '-') term)*
 *   term       := factor (('*' | '/' | '%') factor)*
 *   factor     := unary  ('^' factor)?          // right-associative
 *   unary      := '-' unary | primary
 *   primary    := number | '(' expression ')'
 */
public final class Calculator {
    private final String input;
    private int pos = 0;

    private Calculator(String input) {
        this.input = input;
    }

    public static double evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Expression is empty");
        }
        Calculator c = new Calculator(expression);
        double result = c.parseExpression();
        c.skipSpaces();
        if (c.pos < c.input.length()) {
            throw new IllegalArgumentException("Unexpected character '" + c.input.charAt(c.pos) + "' at position " + c.pos);
        }
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new IllegalArgumentException("Result is not a finite number (division by zero?)");
        }
        return result;
    }

    private double parseExpression() {
        double value = parseTerm();
        while (true) {
            if (eat('+')) value += parseTerm();
            else if (eat('-')) value -= parseTerm();
            else return value;
        }
    }

    private double parseTerm() {
        double value = parseFactor();
        while (true) {
            if (eat('*')) value *= parseFactor();
            else if (eat('/')) value /= parseFactor();
            else if (eat('%')) value %= parseFactor();
            else return value;
        }
    }

    private double parseFactor() {
        double base = parseUnary();
        if (eat('^')) return Math.pow(base, parseFactor());
        return base;
    }

    private double parseUnary() {
        if (eat('-')) return -parseUnary();
        if (eat('+')) return parseUnary();
        return parsePrimary();
    }

    private double parsePrimary() {
        if (eat('(')) {
            double value = parseExpression();
            if (!eat(')')) throw new IllegalArgumentException("Missing ')'");
            return value;
        }
        skipSpaces();
        int start = pos;
        while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
            pos++;
        }
        if (start == pos) {
            throw new IllegalArgumentException("Expected a number at position " + pos);
        }
        try {
            return Double.parseDouble(input.substring(start, pos));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number '" + input.substring(start, pos) + "'");
        }
    }

    private boolean eat(char expected) {
        skipSpaces();
        if (pos < input.length() && input.charAt(pos) == expected) {
            pos++;
            return true;
        }
        return false;
    }

    private void skipSpaces() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
    }
}
