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
    "euroleaguebasketball.net",
    "basketnews.com",
    "eurohoops.net",
    "sportando.basketball"
]

GLOBAL_FOOTBALL_SITES = [
    "marca.com",
    "gazzetta.it",
    "skysports.com",
    "theathletic.com"
]

SOURCE_TRUST_SCORES = {
    # Official/Top Tier Global
    "euroleaguebasketball.net": 100,
    "theathletic.com": 90,
    "skysports.com": 90,
    
    # Solid European Basketball
    "basketnews.com": 85,
    "eurohoops.net": 80,
    
    # Top Israeli Sports Media
    "sport5.co.il": 85,
    "one.co.il": 80,
    
    # General Israeli Media (Sports Sections)
    "ynet.co.il": 75,
    "sports.walla.co.il": 75,
    "sport1.maariv.co.il": 70,
    
    # Rumor-Heavy Global Sites
    "sportando.basketball": 60,
    "marca.com": 55,
    "gazzetta.it": 55
}

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
def internet_search(query_data: str, scope: str = "local") -> str:
    """
    Searches Google via Serper API for the latest news.
    USAGE INSTRUCTIONS FOR AGENTS:
    - For daily local search: query_data must be 'Football' or 'Basketball', and scope must be 'local'.
    - For global validation: query_data must be a batched string of player names like '("John Doe" OR "Jane Smith")', and scope must be 'global_basketball' or 'global_football'.
    """
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
    goal=f"Extract verified news, press conferences, and strictly CURRENT player mentions for {target_date_str}.",
    backstory=(
        f"You are a dedicated Maccabi Tel Aviv correspondent covering events from exactly {target_date_str} (yesterday). "
        "MANDATE: You must use the 'internet_search' tool TWICE. First with 'Football', then with 'Basketball'. "
        "CORE DIRECTIVE: You must bring me EVERY article from the last 24 hours where a Maccabi Tel Aviv player is explicitly mentioned. "
        "ZERO-TRUST CLASSIFICATION RULE (CRITICAL): Do NOT trust the search query category to determine the sport! Israeli sports websites mix categories in their menus. You MUST classify each snippet based on its actual semantic content. If a result from the 'Football' search contains basketball terms (e.g., Euroleague, specific basketball teams, basketball coaches), you MUST route and group it under Basketball. Do NOT discard misplaced items; just move them to the correct sport's group. "
        "STRICT PROOF RULE: You only see short snippets. For a player to be included, the snippet MUST explicitly contain their full name AND a direct contextual link to Maccabi Tel Aviv (e.g., currently playing for or newly signing with the team). "
        "RIVAL TEAM EXCLUSION: Look closely at WHICH team the player is actually signing with or playing for. If the snippet mentions they joined, signed with, or play for 'Hapoel', 'Jerusalem', or ANY team other than Maccabi Tel Aviv, you MUST completely exclude them! "
        "ANTI-GUESSING RULE: NEVER infer identities from statistics or nicknames. Only extract explicitly written names. "
        "Read the snippet context carefully! Completely IGNORE former players, alumni, or players who moved to other clubs. "
        "Collect injuries, match details, and press conference quotes."
    ),
    llm=llm,
    tools=[internet_search],
    verbose=True
)

validator = Agent(
    role='Fact-Checker & Global Investigator',
    goal='Analyze local news, calculate trust scores, and perform targeted global verification only when necessary.',
    backstory=(
        "You are the 'רעיון של שמש' (Sun's Ray) Engine, a highly analytical fact-checker and data router. "
        "Your job is to process the raw news gathered by the Maccabi Specialist and assign a Confidence Score (0-100%).\n\n"
        "SCORING LOGIC:\n"
        "1. Base Score: Assign a base score using these weights: euroleaguebasketball.net=100, one.co.il=80, sport5.co.il=85, ynet.co.il=75, sports.walla.co.il=75.\n"
        "2. Cross-Validation: If multiple Israeli sites report the EXACT same news, add +10% to the score. If they contradict, drop to 40%.\n\n"
        "THE GATEKEEPER RULE (CRITICAL FOR API SAVING):\n"
        "Do NOT use the internet_search tool for local Israeli news, match summaries, or quotes.\n"
        "You MUST ONLY use the internet_search tool if:\n"
        "A. A rumor involves a foreign player/coach OR an international transfer.\n"
        "B. AND it is only reported by ONE local site (requires verification).\n\n"
        "GLOBAL BATCH SEARCHING:\n"
        "If you find multiple foreign rumors needing validation, you MUST combine their names into a SINGLE batched string like: '(\"Player One\" OR \"Player Two\")'. "
        "Call internet_search exactly ONCE per sport with query_data=batched_string and scope='global_basketball' (or 'global_football').\n\n"
        "OUTPUT FORMAT:\n"
        "Compile a structured summary for the Editor. For each news item include:\n"
        "- Entity (Player Name)\n"
        "- Sport (Football/Basketball)\n"
        "- Confidence Score (%)\n"
        "- Tag ([CONFIRMED], [RUMOR], [GLOBAL SCOOP], or [CONFLICTING])\n"
        "- Primary Source (CRITICAL: If multiple sites report the same news, pick exactly ONE site to be the cited source, strictly choosing the one with the highest Trust Score).\n"
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
        "TRUST SCORE INTEGRATION: You will receive validated data from the 'רעיון של שמש' Validator. "
        "FILTERING RULE: If an item has a confidence score strictly lower than 40% AND is tagged as [CONFLICTING], completely exclude it from the final array to prevent fake news. "
        "PLAYER SPOTLIGHT RULE: Strictly feature ONLY active, current players from the 2026 squad. "
        "CRITICAL JSON RULE: You MUST return the final result ONLY as a valid JSON array of objects. "
        "Do NOT add any markdown formatting like ```json. Do NOT write any introduction or conclusion. Just return the raw array. "
        "Each object in the array MUST strictly have these exact keys:\n"
        "[\n"
        "  {\n"
        '    "title": "A short engaging headline for the app in Hebrew",\n'
        '    "content": "A short summary of the article in Hebrew",\n'
        '    "source": "The source of the news (e.g., ערוץ הספורט, ONE)",\n'
        '    "reliability": "Choose exactly one: CONFIRMED, HIGH, MEDIUM, RUMOR, LOW, or FAKE",\n'
        '    "link": "The URL to the original article",\n'
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
    verbose=True
)

# --- Telegram & Podcast Delivery ---
TELEGRAM_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")
CHAT_ID = os.getenv("TELEGRAM_CHAT_ID")

VOICE_MODEL = "en-US-ChristopherNeural" 

def deliver_podcast_and_text(news_items_list):
    print("\n🎙️ Generating content for Telegram and Podcast...")
    audio_file = "maccabi_daily.mp3"
    
    # Building text format for Telegram and Audio from the JSON
    telegram_message = f"🟡🔵 עדכון חדשות מכבי - {target_date_str} 🔵🟡\n\n"
    audio_text = f"Maccabi Tel Aviv daily update for {target_date_str}. "
    
    for item in news_items_list:
        # Build Telegram text
        telegram_message += f"*{item.get('title', 'עדכון')}*\n"
        telegram_message += f"ספורט: {item.get('sport_type', '')} | מקור: {item.get('source', '')} | אמינות: {item.get('reliability', '')}\n"
        telegram_message += f"{item.get('content', '')}\n"
        telegram_message += f"🔗 [לכתבה המלאה]({item.get('link', '')})\n\n"
        
        # Build Audio text (Keeping it simple for the TTS engine)
        audio_text += f"In {item.get('sport_type', 'Sports')}: {item.get('title', '')}. {item.get('content', '')}. "

    async def create_audio():
        communicate = edge_tts.Communicate(audio_text, VOICE_MODEL)
        await communicate.save(audio_file)
    
    asyncio.run(create_audio())
    print("✅ Podcast generated successfully!")

    print("📱 Sending text to Telegram...")
    send_text_url = f"[https://api.telegram.org/bot](https://api.telegram.org/bot){TELEGRAM_TOKEN}/sendMessage"
    requests.post(send_text_url, data={"chat_id": CHAT_ID, "text": telegram_message, "parse_mode": "Markdown"})

    print("🎧 Sending audio file to Telegram...")
    send_audio_url = f"[https://api.telegram.org/bot](https://api.telegram.org/bot){TELEGRAM_TOKEN}/sendAudio"
    with open(audio_file, "rb") as audio:
        requests.post(send_audio_url, data={"chat_id": CHAT_ID}, files={"audio": audio})
    
    print("🚀 All done! Check your Telegram.")

# --- Firebase Integration ---
def upload_to_firebase(news_items_list, audio_file_path, date_str):
    print("\n☁️ Connecting to Firebase...")
    
    # 1. Initialize Firebase
    if not firebase_admin._apps:
        cred = credentials.Certificate("firebase-key.json")
        firebase_admin.initialize_app(cred, {
            'storageBucket': 'maccabi-agent-app.firebasestorage.app' 
        })
    
    db = firestore.client()
    bucket = storage.bucket()

    # 2. Upload Audio to Storage
    print("🎧 Uploading podcast to Firebase Storage...")
    blob = bucket.blob("latest_podcast.mp3") 
    blob.upload_from_filename(audio_file_path)
    blob.make_public() 
    audio_url = blob.public_url
    print(f"🔗 Audio URL generated: {audio_url}")

    # 3. Write Data to Firestore
    print("📝 Saving news to Firestore Database...")
    
    # עוברים על כל כתבה מה-JSON ודוחפים אותה בנפרד למסד הנתונים
    count = 0
    for item in news_items_list:
        # נוסיף לכל כתבה את הלינק לאודיו, התאריך וחותמת הזמן של השרת
        item["audio_url"] = audio_url
        item["date"] = date_str
        item["timestamp"] = firestore.SERVER_TIMESTAMP
        
        db.collection("DailyNews").add(item)
        count += 1
        
    print(f"✅ Successfully uploaded {count} news items to Firebase!")


# --- Main Block ---
if __name__ == "__main__":
    print(f"--- Starting Maccabi Agentic System for Target Date: {target_date_str} ---")
    try:
        result = maccabi_crew.kickoff()
        
        print("\n--- RAW AI OUTPUT ---\n")
        raw_output = str(result.raw)
        print(raw_output)
        
        # Parse the JSON output
        # מנקים תגיות מרקדאון שלפעמים ה-AI מוסיף למרות שביקשנו שלא
        clean_json_string = raw_output.replace("```json", "").replace("```", "").strip()
        news_items = json.loads(clean_json_string)
        
        print(f"\n✅ Successfully parsed {len(news_items)} news items from AI.")
        
        # Execute delivery and upload
        deliver_podcast_and_text(news_items)
        upload_to_firebase(news_items, "maccabi_daily.mp3", target_date_str)
        
    except json.JSONDecodeError as e:
        print(f"\n❌ JSON Parsing Error: The AI did not return a valid JSON array. Error: {e}")
        print("Please run the script again.")
    except Exception as e:
        print(f"\n❌ An error occurred: {e}")