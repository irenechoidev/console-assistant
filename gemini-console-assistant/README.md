# Gemini Console Assistant

A console agent powered by the Gemini API. It has six tools and decides by itself when and how to use them.

```
you type -> model thinks -> asks for a tool -> our code runs it
         -> result goes back -> model continues -> ... -> final answer
```

## Tools

| Tool | What it does | Example |
|---|---|---|
| `get_current_time` | Current date and time in any timezone | "What time is it in Seoul?" |
| `calculate` | Evaluate any math expression | "What is 17.5% of 2340?" |
| `days_until` | Days from today until a date | "How many days until Christmas?" |
| `days_between` | Days between two dates | "How many days between Christmas and New Year?" |
| `convert_units` | Length, weight, temperature | "Convert 180cm to feet" |
| `convert_currency` | Between 8 currencies (fixed rates) | "Convert 100 USD to KRW" |

## Things to try

| Ask | What to watch |
|---|---|
| `Hello!` | No tool used: the model answers directly |
| `What time is it in Seoul?` | One tool call |
| `What is 17.5% of 2340?` | Uses `calculate` instead of guessing |
| `How many days until Christmas?` | Uses `days_until` for exact answer |
| `How many days between Christmas and New Year?` | Uses `days_between` |
| `Convert 100 USD to KRW` | Uses `convert_currency` |
| `Convert 180cm to feet` | Uses `convert_units` |
| `What time is it on Mars?` | Tool fails — model reads the error and explains |
| `And in New York?` (after a time question) | Model uses conversation history |


## How it works (read in this order)

1. **`Tools.java`**: each tool has a *definition* (name, description, JSON schema)
   that the model sees, and *Java code* that runs. The model never runs code; it only asks.
2. **`ConsoleAssistant.runAgentLoop`**: the loop itself:
   1. send the full history and tools to the model
   2. save its reply in the history (unchanged)
   3. if no function call parts in the response → return text answer
   4. otherwise run every requested tool
   5. send all results back as a user message → repeat
3. **`Calculator.java`**: a safe recursive descent parser. We don't use a script engine
   because tool input is untrusted and must never run as arbitrary code.

Safety features: `MAX_TOOL_ROUNDS` limit, input validation, errors returned to the
model instead of crashing, unknown tools rejected.


