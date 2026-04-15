import os
import asyncio
import edge_tts
import sys
import datetime
import requests
import time
import json
import re
from crewai import Agent, Task, Crew, Process, LLM
from crewai.tools import tool
from dotenv import load_dotenv
import firebase_admin
from firebase_admin import credentials, firestore, storage

load_dotenv()

LOCAL_SITES = [
    "sport5.co.il",
    "one.co.il",
    "sport1.maariv.co.il",
    "sports.walla.co.il",
    "ynet.co.il"
]

GLOBAL_BASKETBALL_SITES = [
    "euroleaguebasketball.net/euroleague",
    "basketnews.com",
    "eurohoops.net",
    "sportando.basketball"
]

GLOBAL_FOOTBALL_SITES = [
    "marca.com",
    "gazzetta.it",
    "skysports.com",
    "theathletic.com",
    "x.com/FabrizioRomano"
]

SOURCE_TRUST_SCORES = {
    "euroleaguebasketball.net": 100,
    "x.com/FabrizioRomano": 80,
    "sport5.co.il": 35,
    "one.co.il": 35,
    "sport1.maariv.co.il": 35,
    "ynet.co.il": 25,
    "sports.walla.co.il": 25,
    "basketnews.com": 15,
    "eurohoops.net": 15,
    "sportando.basketball": 15,
    "marca.com": 15,
    "gazzetta.it": 15,
    "skysports.com": 15,
    "theathletic.com": 15
}

today_obj = datetime.date.today()
yesterday_obj = today_obj - datetime.timedelta(days=1)
target_date_str = yesterday_obj.strftime("%B %d, %Y") 

# חזרנו לדרוש רק את המפתחות המקוריים
REQUIRED_KEYS = ["GOOGLE_API_KEY", "SERPER_API_KEY", "TELEGRAM_BOT_TOKEN", "TELEGRAM_CHAT_ID"]
for key in REQUIRED_KEYS:
    if not os.getenv(key):
        print(f"❌ Error: {key} is missing in .env file!")
        sys.exit(1)

# חיבור ישיר ל-Gemini
MY_KEY = os.getenv("GOOGLE_API_KEY")
os.environ["GEMINI_API_KEY"] = MY_KEY

llm = LLM(
    model="gemini/gemini-2.5-flash-lite", 
    api_key=MY_KEY,
    temperature=0,
    max_retries=3
)

# 2. Tools
@tool("internet_search")
def internet_search(query_data: str, scope: str = "local") -> str:
    """
    Searches Google via Serper API for the latest news.
    USAGE INSTRUCTIONS FOR AGENTS:
    - For daily local search: query_data must be 'Football' or 'Basketball', and scope must be 'local'.
    - For global validation: query_data must be a batched string of player names like '("John Doe" OR "Jane Smith")', and scope must be 'global_basketball' or 'global_football'.
    """
    time.sleep(15)  # To respect rate limits
    api_key = os.getenv("SERPER_API_KEY")
    url = "https://google.serper.dev/search"
    
    # 1. Routing: Choose the correct whitelist based on the requested scope
    if scope == "global_basketball":
        sites_list = GLOBAL_BASKETBALL_SITES
    elif scope == "global_football":
        sites_list = GLOBAL_FOOTBALL_SITES
    else:
        sites_list = LOCAL_SITES
        
    sites_query = " OR ".join([f"site:{site}" for site in sites_list])
    sites_formatted = f"({sites_query})"
    
    # 2. Query Construction: Build the string sent to Google
    if scope == "local":
        # Standard daily Hebrew search
        if "basket" in query_data.lower():
            q = f'מכבי תל אביב כדורסל {sites_formatted}'
        else:
            q = f'מכבי תל אביב כדורגל {sites_formatted}'
    else:
        # Global validation search using the batched English names
        q = f'{query_data} "Maccabi Tel Aviv" {sites_formatted}'
        
    print(f"\n🔍 Sending query to Google (Serper) [Scope: {scope}]: {q}\n")

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
            
        final_output = "\n".join(all_results) if all_results else f"No results found for query: {query_data} in scope: {scope}"
        
        print(f"\n--- RAW SERPER RESULTS FOR SCOPE '{scope.upper()}' ---\n{final_output}\n---------------------------------------\n")
        
        return final_output
    except Exception as e:
        return f"Error executing Google Search: {e}"
    

# 3. Agents
researcher = Agent(
    role='Maccabi Specialist',
    goal=f"Extract verified news, press conferences, player updates, management news, and significant team events for {target_date_str}.",
    backstory=(
        f"You are a dedicated Maccabi Tel Aviv correspondent covering events from exactly {target_date_str} (yesterday). "
        "MANDATE: You must use the 'internet_search' tool TWICE. First with 'Football', then with 'Basketball'. "
        "CORE DIRECTIVE: You must bring me EVERY article from the last 24 hours concerning Maccabi Tel Aviv's current players, coaches, management figures (e.g., Mitch Goldhar, Ben Mansford, Avi Even, Shimon Mizrahi), or major general team news (e.g., league decisions, stadiums). "
        "ZERO-TRUST CLASSIFICATION RULE (CRITICAL): Do NOT trust the search query category to determine the sport! Israeli sports websites mix categories in their menus. You MUST classify each snippet based on its actual semantic content. If a result from the 'Football' search contains basketball terms (e.g., Euroleague, specific basketball teams, basketball coaches), you MUST route and group it under Basketball. Do NOT discard misplaced items; just move them to the correct sport's group. "
        "STRICT PROOF RULE: You only see short snippets. For an item to be included, the snippet MUST explicitly contain a relevant name (player/coach/management) OR discuss significant official team news AND have a direct contextual link to Maccabi Tel Aviv. "
        "RIVAL TEAM EXCLUSION: Look closely at WHICH team the player/figure is actually associated with. If the snippet mentions they joined, signed with, or play for 'Hapoel', 'Jerusalem', or ANY team other than Maccabi Tel Aviv, you MUST completely exclude them! "
        "ANTI-GUESSING RULE: NEVER infer identities from statistics or nicknames. Only extract explicitly written names. "
        "Read the snippet context carefully! Completely IGNORE former players, alumni, or players who moved to other clubs. "
        "Collect injuries, match details, press conference quotes, and management/league updates."
    ),
    llm=llm,
    tools=[internet_search],
    verbose=True
)

validator = Agent(
    role='Fact-Checker & Global Investigator',
    goal='Analyze local news, calculate trust scores using a consensus addition model, and perform targeted global verification only when necessary.',
    backstory=(
        "You are the MIA Fact-Checker Engine, a highly analytical fact-checker and data router. "
        "Your job is to process the raw news gathered by the Maccabi Specialist and assign a Confidence Score (0-100%).\n\n"
        "ANTI-HALLUCINATION RULE (CRITICAL): You must NEVER translate names, guess identities, or merge distinct individuals into one entity! "
        "If a snippet contains multiple different names, treat them as completely separate people. "
        "DO NOT assume one name is a nickname, alias, or English translation of another. "
        "If the Hebrew syntax makes it ambiguous to determine which person performed the action, completely DROP the item or extract only the explicitly clear facts.\n\n"
        "FORMER PLAYER RULE (CRITICAL FILTER): If the news focuses on a FORMER Maccabi player or coach who is no longer active in the club, you MUST drop the item completely. THE ONLY EXCEPTION is if the snippet explicitly indicates an interview with them (e.g., using words like 'ראיון', 'התראיין', 'מדבר').\n\n"
        "NEW CONSENSUS SCORING LOGIC:\n"
        "1. Base Weights per Source:\n"
        "   - euroleaguebasketball.net/euroleague: 100\n"
        "   - x.com/FabrizioRomano: 80\n"
        "   - sport5.co.il, one.co.il, sport1.maariv.co.il: 35 each\n"
        "   - ynet.co.il, sports.walla.co.il: 25 each\n"
        "   - Any other global site (basketnews, marca, etc.): 15 each\n"
        "2. DIRECT QUOTE BONUS: Add +15 points to the score if the snippet contains a direct quote (in quotation marks) from a figure, OR uses official definitive words like 'רשמי', 'חתם', 'הודיעה'.\n"
        "3. Calculation: If an identical story is reported by multiple sites, SUM their weights. Cap the final score at maximum 100. (e.g., sport5 + one = 35+35=70).\n"
        "4. Tags based on SUM:\n"
        "   - 0 to 39: tag as [RUMOR]\n"
        "   - 40 to 69: tag as [MEDIUM]\n"
        "   - 70 to 100: tag as [CONFIRMED]\n"
        "5. CONFLICTING REPORTS (CRITICAL): If sources explicitly contradict each other (e.g., one says 'signed', another says 'deal collapsed'), DO NOT sum them. Instead, cut the highest score by half and tag it as [CONFLICTING].\n\n"
        "THE GATEKEEPER RULE (CRITICAL FOR API SAVING):\n"
        "Do NOT use the internet_search tool for local Israeli news, match summaries, or quotes.\n"
        "You MUST ONLY use the internet_search tool if:\n"
        "A. A rumor involves a foreign player/coach OR an international transfer.\n"
        "B. AND it is only reported by ONE local site (requires verification).\n\n"
        "OUTPUT FORMAT:\n"
        "Compile a structured summary for the Editor. For each news item include:\n"
        "- Entity (Player/Coach/Management/Event)\n"
        "- Sport (Football/Basketball)\n"
        "- Confidence Score (%)\n"
        "- Tag ([CONFIRMED], [MEDIUM], [RUMOR], or [CONFLICTING])\n"
        "- Primary Source (The site with the highest Trust Score)\n"
        "- Exact URL Link (CRITICAL: You MUST copy the exact original 'Link' from the search results! Do not lose it!)\n"
        "- The core verified text."
    ),
    llm=llm,
    tools=[internet_search],
    verbose=True
)

editor = Agent(
    role='Sports Newsletter Editor',
    goal='Format the final validated news into a strict JSON array format.',
    backstory=(
        f"You are a meticulous content curator. The official date of this report is {target_date_str}. "
        "TRUST SCORE INTEGRATION: You will receive validated data from the MIA Fact-Checker Validator. "
        "FILTERING RULE: If an item has a confidence score strictly lower than 40% AND is tagged as [CONFLICTING], completely exclude it from the final array to prevent fake news. "
        "STRICT SPORT RULE: You are strictly forbidden from including news about Volleyball (כדורעף), Handball (כדוריד), or any sport other than Football and Basketball. If an item is not Football or Basketball, you MUST drop it."
        "TEAM SPOTLIGHT RULE: Strictly feature ONLY active 2026 players, coaches, current management figures, and significant official team news. "
        "URL RULE (CRITICAL): You MUST keep the EXACT original 'link' URL provided by the MIA Fact-Checker. DO NOT modify, shorten, or generate fake/sequential URLs (like /item/123456). Use the raw URL exactly as received. "
        "CRITICAL JSON RULE: You MUST return the final result ONLY as a valid JSON array of objects. "
        "Do NOT add any markdown formatting like ```json. Do NOT write any introduction or conclusion. Just return the raw array. "
        "ESCAPING RULE (CRITICAL): NEVER use double quotes (\"\") inside your text values! If you need to quote someone, you MUST use single quotes (''). "
        "Each object in the array MUST strictly have these exact keys:\n"
        "[\n"
        "  {\n"
        '    "title": "A short engaging headline for the app in Hebrew",\n'
        '    "content": "A short summary of the article in Hebrew",\n'
        '    "source": "The source of the news (e.g., ערוץ הספורט, ONE)",\n'
        '    "reliability": "Choose exactly one: CONFIRMED, HIGH, MEDIUM, RUMOR, LOW, or FAKE",\n'
        '    "link": "The exact unmodified URL to the original article",\n'
        '    "sport_type": "Choose exactly one: כדורסל or כדורגל"\n'
        "  }\n"
        "]\n"
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

validation_task = Task(
    description=(
        "1. Read the raw data collected by the researcher.\n"
        "2. Group the data by player/entity to identify cross-reporting or contradictions.\n"
        "3. Apply the scoring logic to calculate a Confidence Score for each item.\n"
        "4. Decide if a global search is needed. If yes, batch the English names and execute ONE search per sport using the internet_search tool.\n"
        "5. Update the final scores based on the global results.\n"
        "6. Output the final graded list of news items."
    ),
    expected_output="A structured list of news items, each with a Confidence Score, a validation Tag, and verified text, ready for the Editor.",
    agent=validator,
    context=[research_task]
)

summary_task = Task(
    description=(
        "Format the validator's findings into the requested JSON array structure. "
        "Ensure every news item has all the required JSON keys."
    ),
    expected_output="A perfect JSON array representing the news items, ready to be parsed by Python.",
    agent=editor,
    context=[validation_task]
)

# 5. Crew & Execution
maccabi_crew = Crew(
    agents=[researcher, validator, editor],
    tasks=[research_task, validation_task, summary_task], 
    process=Process.sequential,
    max_rpm=10, # ההגבלה הקריטית נגד שגיאות 429
    verbose=True
)

# --- Telegram & Podcast Delivery ---
TELEGRAM_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")
CHAT_ID = os.getenv("TELEGRAM_CHAT_ID")

VOICE_MODEL = "he-IL-AvriNeural"

def deliver_podcast_and_text(news_items_list):
    print("\n🎙️ Generating content for Telegram and Podcast...")
    audio_file = "maccabi_daily.mp3"
    
    telegram_message = f"🟡🔵 עדכון חדשות מכבי - {target_date_str} 🔵🟡\n\n"
    audio_text = f"עדכון חדשות מכבי היומי לתאריך {target_date_str}. "
    
    if not news_items_list:
        telegram_message += "אין חדשות משמעותיות היום. נתראה מחר!\n"
        audio_text += "אין חדשות משמעותיות היום. נתראה מחר!"
    else:
        for item in news_items_list:
            telegram_message += f"*{item.get('title', 'עדכון')}*\n"
            telegram_message += f"ספורט: {item.get('sport_type', '')} | מקור: {item.get('source', '')} | אמינות: {item.get('reliability', '')}\n"
            telegram_message += f"{item.get('content', '')}\n"
            telegram_message += f"🔗 [לכתבה המלאה]({item.get('link', '')})\n\n"
            
            audio_text += f"ב{item.get('sport_type', 'ספורט')}: {item.get('title', '')}. {item.get('content', '')}. "

    async def create_audio():
        communicate = edge_tts.Communicate(audio_text, VOICE_MODEL)
        await communicate.save(audio_file)
    
    asyncio.run(create_audio())
    print("✅ Podcast generated successfully!")

    print("📱 Sending text to Telegram...")
    send_text_url = f"https://api.telegram.org/bot{TELEGRAM_TOKEN}/sendMessage"
    requests.post(send_text_url, data={"chat_id": CHAT_ID, "text": telegram_message, "parse_mode": "Markdown"})

    print("🎧 Sending audio file to Telegram...")
    send_audio_url = f"https://api.telegram.org/bot{TELEGRAM_TOKEN}/sendAudio"
    with open(audio_file, "rb") as audio:
        requests.post(send_audio_url, data={"chat_id": CHAT_ID}, files={"audio": audio})
    
    print("🚀 All done! Check your Telegram.")


# --- Firebase Integration ---
def upload_to_firebase(news_items_list, audio_file_path, date_str):
    print("\n☁️ Connecting to Firebase...")
    
    if not firebase_admin._apps:
        cred = credentials.Certificate("firebase-key.json")
        firebase_admin.initialize_app(cred, {
            'storageBucket': 'maccabi-agent-app.firebasestorage.app' 
        })
    
    db = firestore.client()
    bucket = storage.bucket()

    print("🎧 Uploading podcast to Firebase Storage...")
    blob = bucket.blob("latest_podcast.mp3") 
    blob.upload_from_filename(audio_file_path)
    
    audio_url = blob.generate_signed_url(expiration=datetime.timedelta(days=365))
    print(f"🔗 Audio URL generated: {audio_url}")

    print("📝 Saving news and podcast to Firestore...")
    
    db.collection("System").document("LatestPodcast").set({
        "audio_url": audio_url,
        "date": date_str,
        "timestamp": firestore.SERVER_TIMESTAMP
    })
    print("✅ Podcast URL saved to System/LatestPodcast!")

    print("🗑️ Cleaning up old news from DailyNews collection...")
    old_news_docs = db.collection("DailyNews").stream()
    deleted_count = 0
    for doc in old_news_docs:
        doc.reference.delete()
        deleted_count += 1
    print(f"✅ Deleted {deleted_count} old news items.")

    count = 0
    for item in news_items_list:
        item["date"] = date_str
        item["timestamp"] = firestore.SERVER_TIMESTAMP
        db.collection("DailyNews").add(item)
        count += 1
        
    print(f"✅ Successfully uploaded {count} NEW news items to Firebase!")


# --- Main Block ---
if __name__ == "__main__":
    print(f"--- Starting Maccabi Agentic System for Target Date: {target_date_str} ---")
    
    max_attempts = 4  
    attempt = 1
    success = False
    
    while attempt <= max_attempts and not success:
        try:
            print(f"\n🚀 Attempt {attempt} of {max_attempts}...")
            result = maccabi_crew.kickoff()
            
            print("\n--- RAW AI OUTPUT ---\n")
            raw_output = str(result.raw)
            print(raw_output)
            
            clean_json_string = raw_output.replace("```json", "").replace("```", "").strip()
            news_items = json.loads(clean_json_string)

            valid_sports = ["כדורסל", "כדורגל"]
            news_items = [item for item in news_items if item.get("sport_type") in valid_sports]
            
            print(f"\n✅ Successfully parsed {len(news_items)} news items from AI.")
            
            deliver_podcast_and_text(news_items)
            upload_to_firebase(news_items, "maccabi_daily.mp3", target_date_str)
            
            success = True 
            
        except json.JSONDecodeError as e:
            print(f"\n❌ JSON Parsing Error on attempt {attempt}. Error: {e}")
            if attempt < max_attempts:
                print("⏳ Waiting 60 seconds before retrying...")
                time.sleep(60) 
            attempt += 1
            
        except Exception as e:
            print(f"\n❌ An error occurred on attempt {attempt} (e.g., Google 503 Overload): {e}")
            if attempt < max_attempts:
                print("⏳ Google servers might be heavily loaded or rate limited. Waiting 5 minutes (300 seconds) before retrying...")
                time.sleep(300) 
            attempt += 1
            
    if not success:
        print("\n❌ All attempts failed. The system is giving up for today.")