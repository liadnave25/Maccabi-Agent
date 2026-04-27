import os

# Set fake credentials at module level so that when any test file imports
# main.py the module-level env-var check (sys.exit on missing keys) passes.
os.environ.setdefault("GOOGLE_API_KEY", "fake-google-key")
os.environ.setdefault("SERPER_API_KEY", "fake-serper-key")
os.environ.setdefault("TELEGRAM_BOT_TOKEN", "fake-bot-token")
os.environ.setdefault("TELEGRAM_CHAT_ID", "fake-chat-id")

import pytest


@pytest.fixture
def sample_news_items():
    return [
        {
            "title": "כותרת 1",
            "content": "תוכן 1",
            "source": "ספורט 5",
            "reliability": "CONFIRMED",
            "link": "https://example.com/1",
            "sport_type": "כדורסל",
        },
        {
            "title": "כותרת 2",
            "content": "תוכן 2",
            "source": "ONE",
            "reliability": "MEDIUM",
            "link": "https://example.com/2",
            "sport_type": "כדורגל",
        },
    ]


@pytest.fixture
def sample_serper_response():
    return {
        "organic": [
            {
                "title": "Maccabi Tel Aviv sign new player",
                "link": "https://sport5.co.il/article/123",
                "snippet": "Maccabi Tel Aviv announced today that they have signed a new player.",
            }
        ]
    }
