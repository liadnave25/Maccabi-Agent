# 🏀 Maccabi Intelligence Agent (MIA) 

## 📝 Project Overview 
MIA is a sophisticated, data-driven Multi-Agent AI System built using the CrewAI framework and Google Gemini 2.5 Flash. The system acts as a personalized sports journalist, fact-checker, and radio broadcaster that bridges the gap between fragmented sports media and the user's English-speaking Telegram inbox. 

It autonomously researches, cross-validates, filters, summarizes, and generates an audio podcast of the latest news regarding Maccabi Tel Aviv Football and Basketball Clubs, ensuring high data integrity and zero fake news.

## 🎯 Primary Objective 
To automate the daily consumption of sports news by creating an end-to-end "research-to-action" pipeline. The agent fetches real-time data from primary sources, runs it through a heuristic validation engine to weed out rumors, formats it into a professional Markdown report with natural source citations, converts the clean text into a high-quality voice podcast, and delivers both directly to a private Telegram Bot.

## 🏗️ System Architecture

### 🧠 The LLM Brain
* **Model:** Gemini 2.5 Flash.
* **Key Feature:** High context window and speed, allowing the system to process multiple Google Search results and cross-reference articles simultaneously without losing focus or hallucinating.

### 🤖 The Agentic Team (Middleware & Pipeline)

**1. Maccabi Specialist (The Researcher)**
* **Role:** Field Data Gatherer.
* **Logic:** Uses Hebrew search queries via the Serper API to capture "first-to-report" news from local whitelisted sites. Conducts separate, independent searches for Football and Basketball to avoid data overlap.
* **Constraint:** Strictly filters out former players and focuses only on the current active roster, performing semantic classification to overcome poorly categorized Israeli sports menus.

**2. The "רעיון של שמש" Engine (The Validator / Gatekeeper)**
* **Role:** Fact-Checker & Global Investigator.
* **Logic:** Acts as the analytical middleware. It groups news by entity (Player/Coach) and calculates a **Trust Score (0-100%)** based on a predefined heuristic dictionary of source credibility. 
* **Cross-Validation:** Detects consensus (+10% score boost) or contradictions (drops score to 40% and tags as `[CONFLICTING]`).
* **Resource Routing:** Intelligently decides when to trigger international API searches (e.g., for foreign player rumors) to conserve API quotas.

**3. Sports Newsletter Editor (The Writer)**
* **Role:** Content Architect.
* **Logic:** Translates validated findings into professional English, maintaining a strict Markdown structure. 
* **Data Integration:** Weaves the primary source naturally into the narrative (e.g., *"According to Sport5..."*) and appends the calculated Trust Score and Tag to each item. Automatically filters out items falling below the minimum confidence threshold.

### 🎬 The Action Layer (Audio & Telegram Integration)
* **Text-to-Speech (TTS):** Utilizes `edge-tts` (Microsoft's Edge Neural Voices) to generate a daily MP3 podcast featuring a professional American news anchor voice ("Christopher").
* **Delivery Protocol:** Uses the Telegram Bot API (`requests.post`) to push both the raw Markdown text (with clickable source links) and the generated audio file directly to the user's mobile device.

## 🛠️ Technical Solutions & Engineering Highlights

* **API Optimization & Query Batching:** Reduced global search API calls from $O(N)$ (per rumor) to $O(1)$ by programming the Validator agent to aggregate entities into a single boolean `OR` string for the Serper API (e.g., `("Player A" OR "Player B") AND "Maccabi Tel Aviv"`).
* **Hallucination Prevention (Dynamic Whitelisting):** Searches are strictly routed to dynamic whitelists (Local Israeli, Global Basketball, Global Football) rather than the open web, ensuring the LLM only processes high-signal journalistic text.
* **The "Robot Reading URLs" Error (Regex Filtering):** Solved the issue of the TTS engine reading aloud complex URLs and markdown symbols by implementing a Python `re` filter. It creates a "clean" text layer specifically for the audio generator, replacing emojis with smooth verbal transitions.
* **Cloud Migration & True Automation (AWS EC2):** Migrated from a local Windows environment to an AWS EC2 Ubuntu Instance. The system is 100% autonomous, running via a Linux `cron` job scheduled daily, requiring zero local computing power.
* **The 429 Quota Error:** Managed by implementing `Process.sequential` and optimizing prompt structures to stay within the Gemini API Free Tier limits.

## 🚀 Future Expansion & Roadmap

* **Advanced Telegram Interactions:** Upgrade the Telegram bot from a "broadcast-only" channel to an interactive agent where the user can reply and ask follow-up questions about the daily news using a Vector Database (RAG) for long-term memory.
* **Specialized Integration (Web & Apps):** Wrap the Crew in a FastAPI or Flask backend to serve as a content engine for a dedicated Maccabi fan site.
* **Direct Scraping:** Move beyond general search tools to direct scraping of the EuroLeague Official API or Israeli Football Association database for live minute-by-minute updates.
* **Real-time Triggers:** Set up a secondary, lightweight agent that scans headlines every hour and triggers an emergency Telegram push notification only when "Breaking News" (like a major transfer or coach firing) is detected.

## 🔑 Configuration Checklist for Deployment

**Python Environment & Libraries:**
```bash
pip install crewai edge-tts requests python-dotenv

Environment Variables (.env file):

GOOGLE_API_KEY: Google AI Studio (Gemini API).

SERPER_API_KEY: Serper.dev API key for Google Searches.

TELEGRAM_BOT_TOKEN: The token provided by BotFather.

TELEGRAM_CHAT_ID: The specific user/group ID to receive the broadcast.