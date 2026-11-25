package com.example.assistant;

import com.google.genai.Client;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ConsoleAssistant {

    private static final String MODEL = "gemini-3.5-flash-lite";
    private static final int MAX_TOOL_ROUNDS = 10;

    private static final String SYSTEM_PROMPT = """
            ## Role
            You are a precise and concise console assistant that helps users with time and math questions.

            ## Tools
            You have six tools — always use them instead of guessing:
            - get_current_time: use for anything about the current date, time, or day of the week.
            - calculate: use for any arithmetic or math expression, no matter how simple.
            - days_until: use for how many days from today until a specific date.
            - days_between: use for how many days between two specific dates.
            - convert_units: use for converting length, weight, or temperature.
            - convert_currency: use for converting between currencies (USD, EUR, GBP, JPY, KRW, CAD, AUD, CNY).

            ## Constraints
            - Never calculate math or dates in your head — always use the appropriate tool.
            - Do not answer questions unrelated to time or math. Politely say it is outside your scope.

            ## Output format
            - Keep answers short and direct — one or two sentences at most.
            - State the result first, then a brief explanation if needed.
            """;

    public static void main(String[] args) {
        Client client = new Client.Builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .build();

        List<Content> history = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Console assistant ready. Type 'exit' to quit.");

        while (true) {
            System.out.print("\nYou: ");
            if (!scanner.hasNextLine()) break;
            String userInput = scanner.nextLine().trim();
            if (userInput.equalsIgnoreCase("exit")) break;
            if (userInput.isEmpty()) continue;

            history.add(Content.builder()
                    .role("user")
                    .parts(List.of(Part.builder().text(userInput).build()))
                    .build());

            try {
                String answer = runAgentLoop(client, history);
                System.out.println("\nAssistant: " + answer);
            } catch (Exception e) {
                System.out.println("\n[API error] " + e.getMessage());
                history.remove(history.size() - 1);
            }
        }
    }

    static String runAgentLoop(Client client, List<Content> history) {
        GenerateContentConfig config = GenerateContentConfig.builder()
                .tools(List.of(Tools.ALL))
                .systemInstruction(Content.builder()
                        .parts(List.of(Part.builder().text(SYSTEM_PROMPT).build()))
                        .build())
                .build();

        for (int round = 1; round <= MAX_TOOL_ROUNDS; round++) {

            GenerateContentResponse response = client.models.generateContent(MODEL, history, config);

            Candidate candidate = response.candidates()
                    .filter(list -> !list.isEmpty())
                    .map(list -> list.get(0))
                    .orElseThrow(() -> new RuntimeException("No response from model (possibly blocked by safety filters)."));
            Content modelContent = candidate.content()
                    .orElseThrow(() -> new RuntimeException("Model returned an empty response."));
            history.add(modelContent);

            List<Part> parts = modelContent.parts().orElse(List.of());
            boolean hasFunctionCall = parts.stream().anyMatch(p -> p.functionCall().isPresent());

            if (!hasFunctionCall) {
                return textOf(parts);
            }

            List<Part> toolResults = new ArrayList<>();
            for (Part part : parts) {
                part.functionCall().ifPresent(call -> toolResults.add(runTool(call)));
            }

            history.add(Content.builder()
                    .role("user")
                    .parts(toolResults)
                    .build());
        }
        return "(Stopped: too many tool calls in one turn.)";
    }

    private static Part runTool(FunctionCall call) {
        String name = call.name().orElse("");
        try {
            Map<String, Object> input = call.args().orElse(Map.of());
            String output = Tools.execute(name, input);
            System.out.println("  [tool] " + name + " " + input + " -> " + output);
            return Part.builder()
                    .functionResponse(FunctionResponse.builder()
                            .name(name)
                            .response(Map.of("result", output))
                            .build())
                    .build();
        } catch (RuntimeException e) {
            System.out.println("  [tool] " + name + " failed: " + e.getMessage());
            return Part.builder()
                    .functionResponse(FunctionResponse.builder()
                            .name(name)
                            .response(Map.of("error", e.getMessage()))
                            .build())
                    .build();
        }
    }

    private static String textOf(List<Part> parts) {
        StringBuilder sb = new StringBuilder();
        for (Part part : parts) {
            part.text().ifPresent(sb::append);
        }
        return sb.toString().trim();
    }
}