import os
import asyncio
import edge_tts
import sys
import datetime
import requests
import json
import re
from crewai import Agent, Task, Crew, Process, LLM
from crewai.tools import tool
from dotenv import load_dotenv

load_dotenv()

SITES_TO_SEARCH = [
    "sport5.co.il",
    "one.co.il",
    "sport1.maariv.co.il",
    "sports.walla.co.il",
    "ynet.co.il",
    "euroleaguebasketball.net",
    "basketnews.com"
]

today_obj = datetime.date.today()
yesterday_obj = today_obj - datetime.timedelta(days=1)
target_date_str = yesterday_obj.strftime("%B %d, %Y") 

REQUIRED_KEYS = ["GOOGLE_API_KEY", "SERPER_API_KEY", "TELEGRAM_BOT_TOKEN", "TELEGRAM_CHAT_ID"]
for key in REQUIRED_KEYS:
    if not os.getenv(key):
        print(f"❌ Error: {key} is missing in .env file!")
        sys.exit(1)

MY_KEY = os.getenv("GOOGLE_API_KEY")
os.environ["GOOGLE_API_KEY"] = MY_KEY
os.environ["GEMINI_API_KEY"] = MY_KEY

llm = LLM(
    model="gemini/gemini-2.5-flash", 
    api_key=MY_KEY,
    temperature=0 
)

# 2. Tools
@tool("internet_search")
def internet_search(category: str) -> str:
    """
    Searches Google via Serper API for the latest news.
    MUST pass either 'Football' or 'Basketball' as the category.
    """
    api_key = os.getenv("SERPER_API_KEY")
    url = "https://google.serper.dev/search"
    
    sites_query = " OR ".join([f"site:{site}" for site in SITES_TO_SEARCH])
    sites_formatted = f"({sites_query})"
    
    if "basket" in category.lower():
        q = f'מכבי תל אביב כדורסל {sites_formatted}'
    else:
        q = f'מכבי תל אביב כדורגל {sites_formatted}'
        
    print(f"\n🔍 Sending query to Google (Serper): {q}\n")

    payload = json.dumps({
      "q": q,
      "gl": "il",        
      "hl": "iw",        
      "tbs": "qdr:d",    
      "num": 15          
    })
    
    headers = {
      'X-API-KEY': api_key,
      'Content-Type': 'application/json'
    }
    
    try:
        response = requests.request("POST", url, headers=headers, data=payload)
        results = response.json().get('organic', [])
        
        all_results = []
        for r in results:
            all_results.append(f"Title: {r.get('title')}\nLink: {r.get('link')}\nSnippet: {r.get('snippet')}\n---")
            
        final_output = "\n".join(all_results) if all_results else f"No {category} news found in the last 24 hours."
        
        # הדפסה לצורך דיבאג - ככה נראה מה הסוכן באמת קורא!
        print(f"\n--- RAW SERPER RESULTS FOR {category.upper()} ---\n{final_output}\n---------------------------------------\n")
        
        return final_output
    except Exception as e:
        return f"Error executing Google Search: {e}"

# 3. Agents
researcher = Agent(
    role='Maccabi Specialist',
    goal=f"Extract verified news, press conferences, and strictly CURRENT player mentions for {target_date_str}.",
    backstory=(
        f"You are a dedicated Maccabi Tel Aviv correspondent covering events from exactly {target_date_str} (yesterday). "
        "MANDATE: You must use the 'internet_search' tool TWICE. First with 'Football', then with 'Basketball'. "
        "CORE DIRECTIVE: You must bring me EVERY article from the last 24 hours where a Maccabi Tel Aviv player is explicitly mentioned. "
        "STRICT PROOF RULE: You only see short snippets. For a player to be included, the snippet MUST explicitly contain their full name AND a direct contextual link to Maccabi Tel Aviv (e.g., currently playing for or newly signing with the team). "
        "RIVAL TEAM EXCLUSION (CRITICAL): Look closely at WHICH team the player is actually signing with or playing for. If the snippet mentions they joined, signed with, or play for 'Hapoel' (הפועל), 'Jerusalem', or ANY team other than Maccabi Tel Aviv, you MUST completely exclude them! 'Maccabi' must be their actual team, not just a passing mention. "
        "ANTI-GUESSING RULE: NEVER infer identities from statistics, nicknames, or title suffixes (like 'The Fourth'). Only extract explicitly written names. "
        "Read the snippet context carefully! Completely IGNORE former players, alumni, or players who moved to other clubs. "
        "Collect injuries, match details, and press conference quotes."
    ),
    llm=llm,
    tools=[internet_search],
    verbose=True
)

editor = Agent(
    role='Sports Newsletter Editor',
    goal='Create a professional English newsletter strictly divided into Football and Basketball sections.',
    backstory=(
        f"You are a meticulous content curator. The official date of this report is {target_date_str}. "
        "CRITICAL RULE: The newsletter MUST have two main sections: '## ⚽ Maccabi Tel Aviv - Football' and '## 🏀 Maccabi Tel Aviv - Basketball'. "
        "Under EACH of these two sections, include three sub-headers: '### Press Conference Highlights', '### Player Spotlight', and '### Next Match'. "
        "PLAYER SPOTLIGHT RULE: Strictly feature ONLY active, current players from the 2026 squad. If the raw data includes former players or anyone from the 2024 or 2025 seasons who left, you MUST filter them out and exclude them from the final report. "
        "If no current player news is found for a specific section, simply state 'No new updates reported.' "
        "NEXT MATCH ANTI-HALLUCINATION RULE: If the search results do not explicitly state the exact date of the next match, you MUST output: 'No upcoming match details reported today.' Do not guess, assume, or generate a date! Always preserve the original URLs."
    ),
    llm=llm,
    verbose=True
)

# 4. Tasks
research_task = Task(
    description=(
        f"1. Fetch Football news using internet_search('Football').\n"
        f"2. Fetch Basketball news using internet_search('Basketball').\n"
        f"3. Parse the results. Extract ANY mention of specific players, next match info, and press conferences."
    ),
    expected_output="Raw collected data grouped roughly into Football and Basketball, containing URLs and key points.",
    agent=researcher
)

summary_task = Task(
    description=(
        "Format the researcher's findings into the final Markdown newsletter. "
        "Enforce the strict structure: Main headers for Football and Basketball, and the 3 sub-headers under each."
    ),
    expected_output="A perfect Markdown newsletter with active links, ready for Telegram delivery.",
    agent=editor,
    context=[research_task]
)

# 5. Crew & Execution
maccabi_crew = Crew(
    agents=[researcher, editor],
    tasks=[research_task, summary_task],
    process=Process.sequential,
    verbose=True
)

# --- Telegram & Podcast Delivery ---
TELEGRAM_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")
CHAT_ID = os.getenv("TELEGRAM_CHAT_ID")

VOICE_MODEL = "en-US-ChristopherNeural" 

def deliver_podcast_and_text(newsletter_text):
    print("\n🎙️ Generating Podcast... Please wait.")
    audio_file = "maccabi_daily.mp3"
    
    clean_audio_text = re.sub(r'\[.*?\]\(.*?\)', '', newsletter_text)
    clean_audio_text = re.sub(r'http\S+', '', clean_audio_text)
    clean_audio_text = clean_audio_text.replace('#', '').replace('*', '')
    clean_audio_text = clean_audio_text.replace('⚽', 'Moving to Football.').replace('🏀', 'Moving to Basketball.')

    async def create_audio():
        communicate = edge_tts.Communicate(clean_audio_text, VOICE_MODEL)
        await communicate.save(audio_file)
    
    asyncio.run(create_audio())
    print("✅ Podcast generated successfully!")

    print("📱 Sending text to Telegram...")
    send_text_url = f"https://api.telegram.org/bot{TELEGRAM_TOKEN}/sendMessage"
    response_text = requests.post(send_text_url, data={"chat_id": CHAT_ID, "text": newsletter_text})

    print("🎧 Sending audio file to Telegram...")
    send_audio_url = f"https://api.telegram.org/bot{TELEGRAM_TOKEN}/sendAudio"
    with open(audio_file, "rb") as audio:
        response_audio = requests.post(send_audio_url, data={"chat_id": CHAT_ID}, files={"audio": audio})
    
    print("🚀 All done! Check your Telegram.")


# --- Main Block ---
if __name__ == "__main__":
    print(f"--- Starting Maccabi Agentic System for Target Date: {target_date_str} ---")
    try:
        result = maccabi_crew.kickoff()
        
        print("\n--- FINAL REPORT ---\n")
        print(result.raw)
        
        deliver_podcast_and_text(result.raw)
        
    except Exception as e:
        print(f"An error occurred: {e}")