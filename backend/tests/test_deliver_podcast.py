"""
Tests for deliver_podcast_and_text().

External dependencies mocked per test:
  - asyncio.run       → prevents executing the real coroutine / file creation
  - edge_tts.Communicate → prevents actual TTS generation (AsyncMock for .save)
  - requests.post     → prevents real Telegram messages
  - builtins.open     → prevents reading a non-existent audio file
"""
import pytest
from unittest.mock import AsyncMock, MagicMock, call

from main import deliver_podcast_and_text, VOICE_MODEL, CHAT_ID


# ---------------------------------------------------------------------------
# Shared fixture: mock everything except the thing being tested
# ---------------------------------------------------------------------------

@pytest.fixture
def base_mocks(mocker):
    """Suppress all I/O side-effects. Returns a namespace of mock objects."""
    class M:
        run = mocker.patch("main.asyncio.run")
        post = mocker.patch("main.requests.post")
        open = mocker.patch("builtins.open", mocker.mock_open())
    return M()


# ---------------------------------------------------------------------------
# Message content — empty list
# ---------------------------------------------------------------------------

def test_empty_list_no_news_message(base_mocks, mocker):
    deliver_podcast_and_text([])
    text_sent = base_mocks.post.call_args_list[0].kwargs["data"]["text"]
    assert "אין חדשות משמעותיות היום" in text_sent


def test_empty_list_audio_text_no_news(mocker):
    """TTS text must mention 'no news' when list is empty."""
    mock_communicate = MagicMock()
    mock_communicate.save = AsyncMock()
    mock_cls = mocker.patch("main.edge_tts.Communicate", return_value=mock_communicate)
    mocker.patch("main.requests.post")
    mocker.patch("builtins.open", mocker.mock_open())

    deliver_podcast_and_text([])

    tts_text = mock_cls.call_args[0][0]
    assert "אין חדשות משמעותיות היום" in tts_text


# ---------------------------------------------------------------------------
# Message content — populated list
# ---------------------------------------------------------------------------

def test_message_contains_all_item_titles(base_mocks, sample_news_items):
    deliver_podcast_and_text(sample_news_items)
    text_sent = base_mocks.post.call_args_list[0].kwargs["data"]["text"]
    for item in sample_news_items:
        assert item["title"] in text_sent


def test_message_bold_titles(base_mocks, sample_news_items):
    deliver_podcast_and_text(sample_news_items)
    text_sent = base_mocks.post.call_args_list[0].kwargs["data"]["text"]
    for item in sample_news_items:
        assert f"*{item['title']}*" in text_sent


def test_message_contains_links(base_mocks, sample_news_items):
    deliver_podcast_and_text(sample_news_items)
    text_sent = base_mocks.post.call_args_list[0].kwargs["data"]["text"]
    for item in sample_news_items:
        assert item["link"] in text_sent


# ---------------------------------------------------------------------------
# Telegram call parameters
# ---------------------------------------------------------------------------

def test_telegram_text_call_uses_markdown(base_mocks, sample_news_items):
    deliver_podcast_and_text(sample_news_items)
    text_call_data = base_mocks.post.call_args_list[0].kwargs["data"]
    assert text_call_data["parse_mode"] == "Markdown"
    assert text_call_data["chat_id"] == CHAT_ID


def test_telegram_text_and_audio_both_called(base_mocks, sample_news_items):
    deliver_podcast_and_text(sample_news_items)
    assert base_mocks.post.call_count == 2


def test_telegram_audio_call_sends_file(base_mocks, sample_news_items):
    deliver_podcast_and_text(sample_news_items)
    audio_call_kwargs = base_mocks.post.call_args_list[1].kwargs
    assert "audio" in audio_call_kwargs["files"]
    assert audio_call_kwargs["data"]["chat_id"] == CHAT_ID


# ---------------------------------------------------------------------------
# TTS / asyncio
# ---------------------------------------------------------------------------

def test_asyncio_run_is_called(base_mocks, sample_news_items):
    deliver_podcast_and_text(sample_news_items)
    base_mocks.run.assert_called_once()


def test_tts_communicate_called_with_voice_model(mocker, sample_news_items):
    """Let asyncio.run execute the real coroutine; mock edge_tts instead."""
    mock_communicate = MagicMock()
    mock_communicate.save = AsyncMock()
    mock_cls = mocker.patch("main.edge_tts.Communicate", return_value=mock_communicate)
    mocker.patch("main.requests.post")
    mocker.patch("builtins.open", mocker.mock_open())

    deliver_podcast_and_text(sample_news_items)

    mock_cls.assert_called_once()
    assert mock_cls.call_args[0][1] == VOICE_MODEL


def test_audio_file_opened_for_telegram(base_mocks, sample_news_items):
    deliver_podcast_and_text(sample_news_items)
    base_mocks.open.assert_called_with("maccabi_daily.mp3", "rb")
