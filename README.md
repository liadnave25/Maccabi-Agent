🏀 Maccabi Intelligence Agent (MIA)
📝 Project Overview
MIA is a sophisticated Multi-Agent AI System built using the CrewAI framework and Google Gemini 2.5 Flash. The system acts as a personalized sports journalist and radio broadcaster that bridges the gap between fragmented Hebrew sports media and the user's English-speaking Telegram inbox. It autonomously researches, filters, summarizes, and generates an audio podcast of the latest news regarding Maccabi Tel Aviv Football and Basketball Clubs.

🎯 Primary Objective
To automate the daily consumption of sports news by creating an end-to-end "research-to-action" pipeline. The agent fetches real-time data from primary Israeli sources, formats it into a professional Markdown report, converts the clean text into a high-quality voice podcast, and delivers both directly to a private Telegram Bot.

🏗️ System Architecture
1. The LLM Brain
Model: Gemini 2.5 Flash.

Key Feature: High context window and speed, allowing the agent to process multiple Google Search results simultaneously without losing focus or hallucinating.

2. The Agentic Team
Maccabi Specialist (Researcher): * Role: Expert journalist.

Logic: Uses Hebrew search queries via the Serper API to capture "first-to-report" news from sites like ONE, Sport5, and Walla. It conducts separate, independent searches for Football and Basketball to avoid data overlap.

Constraint: Strictly filters out former players and focuses only on the current 2026 active roster.

Sports Newsletter Editor (Writer):

Role: Content Architect.

Logic: Translates Hebrew findings into professional English, maintaining a strict Markdown structure (Press Conference, Player Spotlight, Next Match).

3. The Action Layer (Audio & Telegram Integration)
Text-to-Speech (TTS): Utilizes edge-tts (Microsoft's Edge Neural Voices) to generate a daily MP3 podcast featuring a professional American news anchor voice ("Christopher").

Delivery Protocol: Uses the Telegram Bot API (requests.post) to push both the raw Markdown text (with clickable source links) and the generated audio file directly to the user's mobile device.

🛠️ Current Technical Solutions (What we solved)
The "Robot Reading URLs" Error (Regex Filtering): Solved the issue of the TTS engine reading aloud complex URLs and markdown symbols by implementing a Python re (Regex) filter. It creates a "clean" text layer specifically for the audio generator, even creatively replacing emojis (like ⚽) with smooth verbal transitions ("Moving to Football").

Cloud Migration & True Automation (AWS EC2): Migrated the script from a local Windows environment to an Amazon Web Services (AWS) EC2 Ubuntu Instance. The system is now 100% autonomous, running via a Linux cron job scheduled for 11:00 AM IST daily, requiring zero local computing power.

Search Reliability: Replaced unstable free search tools with the robust Serper API for targeted, reliable, and consistent Google search results restricted to specific Israeli sports domains.

The 429 Quota Error: Managed by implementing Process.sequential and optimizing prompt structures to stay within the Gemini API Free Tier limits.

🚀 Future Expansion & Roadmap
1. Advanced Telegram Interactions
Two-Way Communication: Upgrade the Telegram bot from a "broadcast-only" channel to an interactive agent where the user can reply and ask follow-up questions about the daily news (e.g., "Tell me more about the injury mentioned").

2. Specialized Integration (Web & Apps)
API-First Approach: Wrap the Crew in a FastAPI or Flask backend to serve as a content engine for a dedicated Maccabi fan site.

Direct Scraping: Move beyond general search tools to direct scraping of the EuroLeague Official API or Israeli Football Association database for live minute-by-minute updates.

3. Real-time Triggers
News Watchdog: Setting up a secondary, lightweight agent that scans headlines every hour and triggers an emergency Telegram push notification only when "Breaking News" (like a major transfer or coach firing) is detected.

🔑 Configuration Checklist for Deployment
Python Environment & Libraries:

Bash
pip install crewai edge-tts requests python-dotenv
Environment Variables (.env file):

GOOGLE_API_KEY: Google AI Studio (Gemini API).

SERPER_API_KEY: Serper.dev API key for Google Searches.

TELEGRAM_BOT_TOKEN: The token provided by BotFather.

TELEGRAM_CHAT_ID: The specific user/group ID to receive the broadcast.