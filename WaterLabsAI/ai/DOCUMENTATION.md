# WaterLabs Property AI — Technical Documentation

## Table of Contents
1. [Overview](#1-overview)
2. [Technology Stack](#2-technology-stack)
3. [Project Structure](#3-project-structure)
4. [Architecture](#4-architecture)
5. [Class Diagram (UML)](#5-class-diagram-uml)
6. [Component Relations](#6-component-relations)
7. [Flow Diagrams](#7-flow-diagrams)
8. [API Reference](#8-api-reference)
9. [Configuration](#9-configuration)
10. [Data Transfer Objects](#10-data-transfer-objects)
11. [Frontend Design](#11-frontend-design)
12. [Exception Handling](#12-exception-handling)
13. [Key Design Decisions](#13-key-design-decisions)

---

## 1. Overview

Property AI is a Spring Boot web application that automates property discovery on MagicBricks using
browser automation (Playwright), then lets users query the results through a conversational AI
assistant (Spring AI + OpenRouter). The entire interaction happens on a single-page UI built with
Thymeleaf, Bootstrap 5, and vanilla JavaScript.

**Core capability:**
- User sets filters in a modal (city, property type, BHK, budget, floor, age)
- Backend launches a real Chromium browser, navigates MagicBricks, applies the filters, scrapes
  listings and their detail pages
- Scraped data is automatically sent to the AI as context
- User then chats with the AI about the results (comparisons, recommendations, area insights)

---

## 2. Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Runtime | Java | 25 |
| Framework | Spring Boot | 4.1.0 |
| AI Integration | Spring AI | 2.0.0 |
| AI Provider | OpenRouter (OpenAI-compatible) | nemotron-3-ultra-550b |
| Browser Automation | Microsoft Playwright | 1.60.0 |
| Template Engine | Thymeleaf | 4.1.0 |
| Build Tool | Maven | — |
| Frontend | Bootstrap 5.3.3 + Bootstrap Icons + Marked.js | — |
| Server Port | Embedded Tomcat | 8085 |

---

## 3. Project Structure

```
com.waterlabs.ai/
├── AiApplication.java                        # Entry point
├── configurations/
│   ├── ChatClientConfig.java                 # ChatClient bean + system prompt
│   ├── ChatMemoryConfiguraiton.java          # Memory repo + window bean
│   └── PlaywrightConfiguration.java         # Playwright singleton bean
├── controller/
│   ├── HomeController.java                   # GET / → renders index.html
│   ├── ChatController.java                   # GET /chat
│   └── PlaywrightController.java            # GET /scrape
├── service/
│   ├── ChatService.java                      # AI chat with memory
│   └── PlayWrightService.java               # MagicBricks browser automation
├── dto/
│   ├── PropertyDTO.java                      # Scraped property record
│   └── ScrapeFiltersDTO.java                # Filter parameters record
└── exceptions/
    └── GlobalExceptionhandler.java           # @ControllerAdvice — handles all exceptions globally

resources/
├── application.properties                    # App config + AI credentials
└── templates/
    └── index.html                            # Single-page UI (Thymeleaf)
```

---

## 4. Architecture

The application follows a classic **MVC layered architecture** with a clear separation between
the web layer (controllers), business logic (services), and data shapes (DTOs).

```
┌─────────────────────────────────────────────────────────────────┐
│                        BROWSER (User)                           │
│                     index.html (SPA)                            │
│  Filter Modal ──► JS fetch('/scrape?...') ──► Property Cards    │
│  Chat Input   ──► JS fetch('/chat?...')  ──► Chat Bubbles       │
└────────────────────┬────────────────────────────────────────────┘
                     │ HTTP (port 8085)
┌────────────────────▼────────────────────────────────────────────┐
│                   SPRING MVC LAYER                              │
│  HomeController     ChatController     PlaywrightController     │
│  GET /              GET /chat          GET /scrape              │
└──────┬──────────────────┬──────────────────────┬───────────────┘
       │                  │                       │
       │          ┌───────▼──────┐    ┌──────────▼──────────┐
       │          │  ChatService │    │  PlayWrightService  │
       │          │              │    │                     │
       │          │ ChatClient   │    │ Playwright Browser  │
       │          │ + Memory     │    │ → MagicBricks.com   │
       │          │ Advisor      │    │ → Scrape listings   │
       │          └──────┬───────┘    └──────────┬──────────┘
       │                 │                        │
       │          ┌──────▼───────┐    ┌──────────▼──────────┐
       │          │  OpenRouter  │    │   PropertyDTO[]     │
       │          │  AI API      │    │   (JSON response)   │
       │          └──────────────┘    └─────────────────────┘
       │
  index.html (Thymeleaf render)
```

---

## 5. Class Diagram (UML)

```
┌─────────────────────────────────────────────────────────────────────────┐
│  <<SpringBootApplication>>                                              │
│  AiApplication                                                          │
│  + main(args: String[]) : void                                          │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<Configuration>>                       │
│  PlaywrightConfiguration                 │
│  ─────────────────────────────────────── │
│  + playWright() : Playwright             │  ← @Bean(destroyMethod="close")
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<Configuration>>                       │
│  ChatClientConfig                        │
│  ─────────────────────────────────────── │
│  - SYSTEM_PROMPT : String                │
│  + defaultChatClient(                    │
│      builder: ChatClient.Builder         │
│    ) : ChatClient                        │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<Configuration>>                       │
│  ChatMemoryConfiguraiton                 │
│  ─────────────────────────────────────── │
│  + chatMemoryRepository()                │
│      : ChatMemoryRepository              │  ← InMemoryChatMemoryRepository
│  + messageWindowChatMemory(              │
│      repo: ChatMemoryRepository          │
│    ) : ChatMemory                        │  ← MessageWindowChatMemory(10)
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<Controller>>                          │
│  HomeController                          │
│  ─────────────────────────────────────── │
│  + home() : String                       │  ← GET /  → "index"
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<Controller>>                          │
│  ChatController                          │
│  ─────────────────────────────────────── │
│  - log : Logger                          │
│  - chatService : ChatService             │
│  ─────────────────────────────────────── │
│  + ChatController(ChatService)           │
│  + getChatResponse(                      │
│      query: String,                      │
│      additionalPrompt: String?           │
│    ) : ResponseEntity<ChatResponse>      │  ← GET /chat
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<Controller>>                          │
│  PlaywrightController                    │
│  ─────────────────────────────────────── │
│  - log : Logger                          │
│  - playWrightService : PlayWrightService │
│  ─────────────────────────────────────── │
│  + PlaywrightController(PlayWrightService│
│  + scrape(                               │
│      city, propertyType, bhk,            │
│      minBudget, maxBudget,               │
│      minFloor, maxAge                    │
│    ) : ResponseEntity<List<PropertyDTO>> │  ← GET /scrape
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<Service>>                             │
│  ChatService                             │
│  ─────────────────────────────────────── │
│  - log : Logger                          │
│  - CONVERSATION_ID : String = "default"  │
│  - chatClient : ChatClient               │
│  - memoryAdvisor : MessageChatMemory     │
│                    Advisor               │
│  ─────────────────────────────────────── │
│  + ChatService(ChatClient, ChatMemory)   │
│  + chat(                                 │
│      query: String,                      │
│      additionalPrompt: String?           │
│    ) : ChatResponse                      │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<Service>>                             │
│  PlayWrightService                       │
│  ─────────────────────────────────────── │
│  - log : Logger                          │
│  - NUM, RANGE, AGE_* : Pattern           │
│  - playwright : Playwright               │
│  ─────────────────────────────────────── │
│  + PlayWrightService(Playwright)         │
│  + getPropertyDetails(                   │
│      filters: ScrapeFiltersDTO           │
│    ) : List<PropertyDTO>                 │
│  - scrapeListings(Page,                  │
│      ScrapeFiltersDTO)                   │
│      : List<PropertyDTO>                 │
│  - getListingUrl(Page, Locator) : String │
│  - scrapeAgeFromDetailPage(              │
│      Page, String) : int                 │
│  - parsePrice(String) : int              │
│  - parseFloor(String) : int              │
│  - parseAgeValue(String) : int           │
│  - safeText(Locator, String) : String    │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<record>>                              │
│  PropertyDTO                             │
│  ─────────────────────────────────────── │
│  + propertyId  : String                  │
│  + title       : String                  │
│  + society     : String                  │
│  + rentInINR   : int                     │
│  + floor       : int                     │
│  + ageInYears  : int  (-1 = unknown)     │
│  + area        : String                  │
│  + url         : String                  │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<record>>                              │
│  ScrapeFiltersDTO                        │
│  ─────────────────────────────────────── │
│  + city         : String                 │
│  + propertyType : String                 │
│  + bhk          : List<String>           │
│  + minBudget    : int                    │
│  + maxBudget    : int                    │
│  + minFloor     : int                    │
│  + maxAge       : int                    │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<RuntimeException>>                    │
│  ScrapperFailedException                 │
│  ─────────────────────────────────────── │
│  + ScrapperFailedException(msg)          │
│  + ScrapperFailedException(msg, cause)   │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<RuntimeException>>                    │
│  AiCallFailedException                   │
│  ─────────────────────────────────────── │
│  + AiCallFailedException(msg)            │
│  + AiCallFailedException(msg, cause)     │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<ControllerAdvice>>                    │
│  GlobalExceptionhandler                  │
│  ─────────────────────────────────────── │
│  - log : Logger                          │
│  ─────────────────────────────────────── │
│  + handleScrapperFailedException(        │
│      ex, request) : ResponseEntity       │  ← 500
│  + handleAiCallFailedException(          │
│      ex, request) : ResponseEntity       │  ← 502
│  + handleMissingParam(                   │
│      ex, request) : ResponseEntity       │  ← 400
│  + handleGenericException(               │
│      ex, request) : ResponseEntity       │  ← 500
│  - buildResponse(status, msg, request)   │
│      : ResponseEntity<ErrorResponseDTO>  │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  <<record>>                              │
│  ErrorResponseDTO                        │
│  ─────────────────────────────────────── │
│  + timestamp : String                    │
│  + status    : String  (e.g. "500")      │
│  + error     : String  (reason phrase)   │
│  + message   : String  (detail)          │
│  + path      : String  (request URI)     │
└──────────────────────────────────────────┘
```

---

## 6. Component Relations

```
AiApplication
    │
    ├── @Bean Playwright ◄────────── PlaywrightConfiguration
    │       └── injected into ──────► PlayWrightService
    │
    ├── @Bean ChatMemoryRepository ◄─ ChatMemoryConfiguraiton
    │       └── injected into ──────► ChatMemory (MessageWindowChatMemory)
    │               └── injected into ──► ChatService (via MessageChatMemoryAdvisor)
    │
    ├── @Bean ChatClient ◄──────────── ChatClientConfig (sets system prompt)
    │       └── injected into ──────► ChatService
    │
    ├── ChatController
    │       └── depends on ────────► ChatService
    │
    ├── PlaywrightController
    │       └── depends on ────────► PlayWrightService
    │               └── produces ──► List<PropertyDTO>
    │               └── consumes ──► ScrapeFiltersDTO
    │
    └── HomeController (no dependencies)

Dependency Injection Summary:
  PlayWrightService   ← Playwright
  ChatService         ← ChatClient, ChatMemory
  ChatController      ← ChatService
  PlaywrightController← PlayWrightService
  ChatClientConfig    ← ChatClient.Builder (auto-provided by Spring AI)
  ChatMemoryConfiguraiton ← (no deps)
  PlaywrightConfiguration ← (no deps)
```

---

## 7. Flow Diagrams

### 7.1 Property Search Flow

```
User                  Browser (index.html)         Spring Backend         MagicBricks
 │                           │                           │                     │
 │── clicks "Find Properties"│                           │                     │
 │                           │── shows filter modal      │                     │
 │── fills filters & clicks  │                           │                     │
 │   "Search Properties"     │                           │                     │
 │                           │── hides modal             │                     │
 │                           │── shows page overlay      │                     │
 │                           │── GET /scrape?city=...    │                     │
 │                           │   &propertyType=...       │                     │
 │                           │   &bhk=3&minBudget=50000  │                     │
 │                           │   &maxBudget=65000        │                     │
 │                           │   &minFloor=10&maxAge=5   │                     │
 │                           │                           │                     │
 │                           │              PlaywrightController               │
 │                           │              builds ScrapeFiltersDTO            │
 │                           │              calls PlayWrightService            │
 │                           │                           │                     │
 │                           │                           │── launch Chromium   │
 │                           │                           │── navigate ─────────►
 │                           │                           │── click Rent tab    │
 │                           │                           │── type city ────────►
 │                           │                           │── select city from  │
 │                           │                           │   autocomplete      │
 │                           │                           │── set property type │
 │                           │                           │── set BHK           │
 │                           │                           │── set budget        │
 │                           │                           │── submit search ────►
 │                           │                           │◄── results page ────│
 │                           │                           │                     │
 │                           │                     scrapeListings():           │
 │                           │                     for each card:              │
 │                           │                       - parse floor/price       │
 │                           │                       - check filters           │
 │                           │                       - open detail page ───────►
 │                           │                       - scrape age ◄────────────│
 │                           │                       - close detail page       │
 │                           │                           │                     │
 │                           │◄── List<PropertyDTO> JSON │                     │
 │                           │── hides overlay           │                     │
 │                           │── renders property cards  │                     │
 │                           │── sends summary to AI     │                     │
 │◄─────── UI updated ───────│                           │                     │
```

### 7.2 Chat Flow

```
User              index.html            ChatController        ChatService
 │                    │                       │                    │
 │── types query      │                       │                    │
 │   + Enter/Send     │                       │                    │
 │                    │── appends user bubble │                    │
 │                    │── shows typing dots   │                    │
 │                    │── GET /chat           │                    │
 │                    │   ?query=...          │                    │
 │                    │   &additionalPrompt=  │                    │
 │                    │                       │── chat(q, prompt)  │
 │                    │                       │                    │── build userMessage
 │                    │                       │                    │   (append additionalPrompt
 │                    │                       │                    │    if present)
 │                    │                       │                    │
 │                    │                       │                    │── chatClient.prompt()
 │                    │                       │                    │   .user(userMessage)
 │                    │                       │                    │   .advisors(memoryAdvisor)
 │                    │                       │                    │   .advisors(conversationId)
 │                    │                       │                    │   .call()
 │                    │                       │                    │
 │                    │                       │                    │◄── ChatResponse (OpenRouter)
 │                    │                       │                    │    (memory saved automatically)
 │                    │                       │◄── ChatResponse    │
 │                    │◄── JSON response      │                    │
 │                    │── remove typing dots  │                    │
 │                    │── render markdown     │                    │
 │                    │── append AI bubble    │                    │
 │◄── UI updated ─────│                       │                    │
```

### 7.3 Memory Flow

```
Conversation turn N:
  MessageChatMemoryAdvisor
    │
    ├── BEFORE call: reads last 10 messages from InMemoryChatMemoryRepository
    │   for conversationId="default-session"
    │   → injects as conversation history into the prompt
    │
    ├── AI call → OpenRouter
    │
    └── AFTER call: saves [UserMessage + AssistantMessage] to repository

Conversation turn N+1:
  Same — previous turn is now in history, sent as context to AI
```

---

## 8. API Reference

### GET `/`
Renders the Thymeleaf `index.html` template. No parameters.

---

### GET `/scrape`
Launches Chromium, navigates MagicBricks with the provided filters, scrapes listings, and
returns a JSON array.

**Query Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `city` | String | `Bangalore` | City to search in |
| `propertyType` | String | `Apartment` | `Apartment` maps to Flat checkbox; anything else maps to House |
| `bhk` | String (multi) | `3` | BHK sizes to include — can be repeated: `bhk=2&bhk=3` |
| `minBudget` | int | `50000` | Minimum rent in INR |
| `maxBudget` | int | `65000` | Maximum rent in INR |
| `minFloor` | int | `10` | Minimum floor number |
| `maxAge` | int | `5` | Maximum property age in years (unknown age = included) |

**Response:** `200 OK` — `application/json`
```json
[
  {
    "propertyId": "MB84277599",
    "title": "3 BHK Flat for Rent",
    "society": "Prestige Lakeside Habitat",
    "rentInINR": 58000,
    "floor": 12,
    "ageInYears": 3,
    "area": "1550 sq.ft",
    "url": "https://www.magicbricks.com/propertyDetails/..."
  }
]
```
`ageInYears = -1` means age was not published on the listing (shown as N/A in UI).

**Error:** `500` with message `"Failed to scrape MagicBricks."` if browser automation fails.

---

### GET `/chat`
Sends a query to the AI with optional additional instructions. Conversation history is
maintained automatically via `MessageChatMemoryAdvisor`.

**Query Parameters:**

| Parameter | Required | Description |
|---|---|---|
| `query` | Yes | The user's message or property context to send to AI |
| `additionalPrompt` | No | Extra instructions appended to the query (e.g. "prefer gated communities") |

**Response:** `200 OK` — Spring AI `ChatResponse` JSON

The UI reads `response.result.output.text` or `.content` for the AI reply text.

---

## 9. Configuration

### application.properties

| Property | Value | Purpose |
|---|---|---|
| `server.port` | `8085` | Application port |
| `spring.ai.openai.base-url` | `https://openrouter.ai/api/v1` | Routes to OpenRouter instead of OpenAI |
| `spring.ai.openai.chat.model` | `nvidia/nemotron-3-ultra-550b-a55b:free` | Free model via OpenRouter |
| `spring.main.allow-bean-definition-overriding` | `true` | Required for Spring AI auto-config compatibility |
| `logging.level.org.springframework.ai` | `DEBUG` | Full AI request/response logging |

### ChatClientConfig
Sets the real estate agent system prompt on every chat call via `builder.defaultSystem(...)`.
The system prompt instructs the AI to:
- Act as an Indian rental property expert
- Use ₹ INR formatting
- Use bullet points and tables for clarity
- Be honest when data is insufficient

### ChatMemoryConfiguraiton
- `InMemoryChatMemoryRepository` — stores conversation messages in a `ConcurrentHashMap` (process-scoped, resets on restart)
- `MessageWindowChatMemory` — sliding window of last **10 messages** per conversation ID
- Current conversation ID is hardcoded as `"default-session"` in `ChatService` (single-user)

### PlaywrightConfiguration
Registers a singleton `Playwright` bean with `destroyMethod = "close"` so the Playwright process
is cleanly shut down when the Spring context closes.

---

## 10. Data Transfer Objects

### PropertyDTO
Java record. Immutable. Serialized as JSON for the `/scrape` response.

| Field | Type | Notes |
|---|---|---|
| `propertyId` | String | Extracted from MagicBricks card ID attribute (`cardidXXXX` → `XXXX`) |
| `title` | String | Property listing title from search result card |
| `society` | String | Society/project name |
| `rentInINR` | int | Monthly rent in INR (digits only, no currency symbol) |
| `floor` | int | Floor number; `0` = ground floor; `-1` = unparseable |
| `ageInYears` | int | Construction age; `-1` = not listed on MagicBricks |
| `area` | String | Carpet or super area with unit (e.g. `"1200 sq.ft"`) |
| `url` | String | Full URL to the MagicBricks detail page |

### ScrapeFiltersDTO
Java record. Immutable. Constructed in `PlaywrightController` from `@RequestParam` values.

| Field | Type | Notes |
|---|---|---|
| `city` | String | Typed into MagicBricks search box |
| `propertyType` | String | `"Apartment"` → checkbox `residential_0`; other → `residential_1` |
| `bhk` | List\<String\> | Values `"1"–"4"`, `"5+"` map to `bhkFlatHouse_0` through `bhkFlatHouse_4` |
| `minBudget` | int | Applied both in Playwright UI and post-scrape filter |
| `maxBudget` | int | Applied both in Playwright UI and post-scrape filter |
| `minFloor` | int | Post-scrape filter only (`floor >= minFloor`) |
| `maxAge` | int | Post-scrape filter only — unknown age (`-1`) passes through |

---

## 11. Frontend Design

### Layout
Two-column CSS Grid layout (full viewport height minus topbar):
- **Left column** — AI chat panel (flex column: header, scrollable messages, input bar)
- **Right column** — scrollable property cards sidebar

### UI Components

| Component | Description |
|---|---|
| Topbar | App branding + "Find Properties" button |
| Filter Modal | Bootstrap modal with all 7 filter inputs + AI instructions textarea |
| Page Overlay | Fixed full-screen blur overlay with spinner — blocks UI during scraping |
| Property Card | Shows title, society, price chip, floor/age/area chips, link to listing |
| Chat Bubble (User) | Right-aligned, accent-colored |
| Chat Bubble (AI) | Left-aligned, light accent background, full Markdown rendered via Marked.js |
| Typing Indicator | Three animated dots shown while waiting for AI response |

### JavaScript Functions

| Function | Purpose |
|---|---|
| `searchProperties()` | Reads all filters, builds URL params, calls `/scrape`, renders results, seeds AI |
| `renderProperties(data)` | Builds property card HTML from `PropertyDTO[]`, updates count badge |
| `sendChat()` | Reads chat input, appends user bubble, calls `sendToAI()` |
| `sendToAI(query, uiNote, additionalPrompt)` | Calls `/chat`, handles typing indicator, renders markdown response |
| `appendUserMessage(text)` | XSS-safe user bubble |
| `appendAIMessage(rawText)` | Parses markdown via `marked.parse()` then appends AI bubble |
| `appendTypingIndicator()` | Adds animated dots, returns unique element ID for later removal |
| `removeTypingIndicator(id)` | Removes the typing indicator by ID |
| `buildPropertySummary(data)` | Formats `PropertyDTO[]` as a numbered text list for AI context |
| `esc(str)` | XSS-safe HTML entity encoding for dynamic content |

### Markdown Rendering
AI responses are piped through `marked.parse()` with:
- `breaks: true` — single newlines become `<br>`
- `gfm: true` — GitHub Flavoured Markdown (tables, strikethrough, fenced code)

Custom CSS inside `.msg-ai` styles: headers, lists, inline code (purple), code blocks (dark theme),
blockquotes, tables, bold, links, and horizontal rules.

---

## 12. Exception Handling

The application uses a centralised `@ControllerAdvice` so every exception — whether from the
scraper, the AI service, or Spring MVC itself — is caught in one place and returned as a
consistent `ErrorResponseDTO` JSON body. The frontend always receives structured JSON rather
than Spring's default whitelabel HTML error page.

### Custom Exceptions

| Class | Extends | Thrown By | Meaning |
|---|---|---|---|
| `ScrapperFailedException` | `RuntimeException` | `PlayWrightService` | Playwright/browser automation failure |
| `AiCallFailedException` | `RuntimeException` | `ChatService` | OpenRouter unreachable, auth failure, or timeout |

Both have a two-arg constructor `(String message, Throwable cause)` to preserve the original
stack trace through the exception chain.

### Handler Map

| Handler Method | Exception Caught | HTTP Status | When It Fires |
|---|---|---|---|
| `handleScrapperFailedException` | `ScrapperFailedException` | `500 Internal Server Error` | Playwright browser fails, site layout changes, timeout on MagicBricks |
| `handleAiCallFailedException` | `AiCallFailedException` | `502 Bad Gateway` | OpenRouter is down, rate-limited, or API key rejected |
| `handleMissingParam` | `MissingServletRequestParameterException` | `400 Bad Request` | `?query=` param absent on `/chat` |
| `handleGenericException` | `Exception` (catch-all) | `500 Internal Server Error` | Any unexpected exception not covered above |

### Error Response Shape

All handlers return the same `ErrorResponseDTO` record:

```json
{
  "timestamp": "2026-07-16T12:34:56.789",
  "status":    "500",
  "error":     "Internal Server Error",
  "message":   "MagicBricks scraping failed: Timeout 15000ms exceeded.",
  "path":      "/scrape"
}
```

### Exception Flow Diagram

```
PlayWrightService
  └── catch (Exception ex)
        └── throw ScrapperFailedException(msg, ex)
              └── GlobalExceptionhandler.handleScrapperFailedException()
                    └── 500 + ErrorResponseDTO JSON

ChatService
  └── catch (Exception ex)
        └── throw AiCallFailedException(msg, ex)
              └── GlobalExceptionhandler.handleAiCallFailedException()
                    └── 502 + ErrorResponseDTO JSON

Spring MVC
  └── missing @RequestParam
        └── MissingServletRequestParameterException
              └── GlobalExceptionhandler.handleMissingParam()
                    └── 400 + ErrorResponseDTO JSON

Anything else
  └── GlobalExceptionhandler.handleGenericException()
        └── 500 + ErrorResponseDTO JSON (generic message, full stack in logs)
```

### Why 502 for AI failures?
`502 Bad Gateway` is the semantically correct status when *our* server cannot reach an
*upstream* service (OpenRouter). It distinguishes AI provider outages from our own bugs,
which makes monitoring and alerting easier — a spike in 502s on `/chat` means OpenRouter is
having issues, while 500s on `/chat` would indicate a bug in our code.

---

## 13. Key Design Decisions

### Why Playwright (not an API)?
MagicBricks has no public search API. The only way to get structured rental data is to drive a
real browser — Playwright automates the UI interactions exactly as a human would (typing, clicking
checkboxes, reading suggestion dropdowns).

### Why `dispatchEvent("click")` for city autocomplete?
MagicBricks renders the `#serachSuggest` dropdown in the DOM immediately but hides it via CSS
(`display:none` / `opacity`). Playwright's `VISIBLE` state check polls the computed style and
never fires because the element remains technically hidden. `dispatchEvent("click")` bypasses
Playwright's actionability checks and fires the `onclick` handler directly.

### Why `ATTACHED` for dropdown items?
Items inside the suggestion dropdown are injected by JS as the user types — they don't exist at
all until the AJAX call returns. `ATTACHED` waits for DOM insertion without caring about visibility,
which is exactly the right signal here.

### Why post-scrape filtering in addition to browser-side filtering?
MagicBricks applies budget and BHK filters server-side but imprecisely (can include slightly
out-of-range results). Floor and age have no server-side filter on MagicBricks at all. The
post-scrape `scrapeListings()` filter is the source of truth.

### Why `ageInYears = -1` is included (not filtered out)?
Not all landlords publish the age of construction. Excluding unknowns would silently drop valid
listings. `-1` is passed to the frontend as `"N/A"` and the AI is instructed to note when data
is missing rather than fabricate it.

### Why a sliding window of 10 messages?
The nemotron model has a large context window but is free-tier — keeping the last 10 messages
(5 user + 5 assistant) gives enough conversational context while avoiding token bloat. The window
is configurable in `ChatMemoryConfiguraiton`.

### Why `CONVERSATION_ID = "default-session"` hardcoded?
The current build is single-user. When multi-user support is added, this constant will be
replaced by a session ID or user ID passed from the controller.

### Why `spring.main.allow-bean-definition-overriding = true`?
Spring AI auto-configures a `ChatClient` bean. `ChatClientConfig` also defines one with
`defaultSystem(...)`. The override flag allows the custom bean to win without a startup conflict.

---

*Documentation generated for WaterLabs Property AI v0.0.1-SNAPSHOT*,
*CI Pipeline added with Github Actions*

