"""
Tests for run_pipeline() — the main retry loop.

Mocks used in every test:
  - main.time.sleep         → no actual waiting
  - main.deliver_podcast_and_text → no Telegram / TTS calls
  - main.upload_to_firebase       → no Firebase calls

Per-test mocks:
  - main.maccabi_crew.kickoff → controls what the LLM "returns"
  - main.extract_news_items   → (some tests) isolates JSON-parse failure
"""
import json
import pytest
from unittest.mock import MagicMock, call

from main import run_pipeline


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

VALID_RAW = json.dumps([
    {
        "title": "כותרת",
        "content": "תוכן",
        "source": "ספורט 5",
        "reliability": "CONFIRMED",
        "link": "https://example.com/1",
        "sport_type": "כדורסל",
    }
])

INVALID_RAW = "this { is not } valid json"
VOLLEYBALL_RAW = json.dumps([
    {
        "title": "כותרת",
        "content": "תוכן",
        "source": "ספורט 5",
        "reliability": "LOW",
        "link": "https://example.com/1",
        "sport_type": "כדורעף",  # filtered out
    }
])


def _valid_result():
    r = MagicMock()
    r.raw = VALID_RAW
    return r


def _invalid_result():
    r = MagicMock()
    r.raw = INVALID_RAW
    return r


# ---------------------------------------------------------------------------
# Autouse: suppress sleep and delivery side-effects in every test
# ---------------------------------------------------------------------------

@pytest.fixture(autouse=True)
def no_sleep(mocker):
    return mocker.patch("main.time.sleep")


@pytest.fixture(autouse=True)
def mock_delivery(mocker):
    class D:
        deliver = mocker.patch("main.deliver_podcast_and_text")
        upload = mocker.patch("main.upload_to_firebase")
    return D()


# ---------------------------------------------------------------------------
# Happy-path tests
# ---------------------------------------------------------------------------

def test_success_on_first_attempt(mocker, mock_delivery):
    mocker.patch("main.maccabi_crew.kickoff", return_value=_valid_result())
    run_pipeline()
    mock_delivery.deliver.assert_called_once()
    mock_delivery.upload.assert_called_once()


def test_kickoff_called_once_on_success(mocker):
    mock_kickoff = mocker.patch("main.maccabi_crew.kickoff", return_value=_valid_result())
    run_pipeline()
    mock_kickoff.assert_called_once()


def test_success_flag_prevents_further_attempts(mocker):
    mock_kickoff = mocker.patch("main.maccabi_crew.kickoff", return_value=_valid_result())
    run_pipeline()
    assert mock_kickoff.call_count == 1


# ---------------------------------------------------------------------------
# JSON error retry tests
# ---------------------------------------------------------------------------

def test_json_error_then_success(mocker, mock_delivery):
    mock_kickoff = mocker.patch(
        "main.maccabi_crew.kickoff",
        side_effect=[_invalid_result(), _valid_result()],
    )
    run_pipeline()
    assert mock_kickoff.call_count == 2
    mock_delivery.deliver.assert_called_once()


def test_json_error_sleeps_60s(no_sleep, mocker):
    mocker.patch(
        "main.maccabi_crew.kickoff",
        side_effect=[_invalid_result(), _valid_result()],
    )
    run_pipeline()
    no_sleep.assert_any_call(60)


def test_json_error_does_not_sleep_on_last_attempt(no_sleep, mocker):
    mocker.patch("main.maccabi_crew.kickoff", return_value=_invalid_result())
    run_pipeline()
    # sleep(60) should only be called 3 times (between attempts 1-2, 2-3, 3-4)
    sleep_60_calls = [c for c in no_sleep.call_args_list if c == call(60)]
    assert len(sleep_60_calls) == 3


# ---------------------------------------------------------------------------
# General exception retry tests
# ---------------------------------------------------------------------------

def test_general_error_then_success(mocker, mock_delivery):
    mock_kickoff = mocker.patch(
        "main.maccabi_crew.kickoff",
        side_effect=[RuntimeError("503"), _valid_result()],
    )
    run_pipeline()
    assert mock_kickoff.call_count == 2
    mock_delivery.deliver.assert_called_once()


def test_general_error_sleeps_300s(no_sleep, mocker):
    mocker.patch(
        "main.maccabi_crew.kickoff",
        side_effect=[RuntimeError("overload"), _valid_result()],
    )
    run_pipeline()
    no_sleep.assert_any_call(300)


# ---------------------------------------------------------------------------
# All-attempts-fail tests
# ---------------------------------------------------------------------------

def test_all_4_attempts_fail_no_delivery(mocker, mock_delivery):
    mocker.patch("main.maccabi_crew.kickoff", return_value=_invalid_result())
    run_pipeline()
    mock_delivery.deliver.assert_not_called()
    mock_delivery.upload.assert_not_called()


def test_max_attempts_is_4(mocker):
    mock_kickoff = mocker.patch("main.maccabi_crew.kickoff", return_value=_invalid_result())
    run_pipeline()
    assert mock_kickoff.call_count == 4


# ---------------------------------------------------------------------------
# Sport-type filter applied before delivery
# ---------------------------------------------------------------------------

def test_sport_filter_applied_before_delivery(mocker, mock_delivery):
    """Items with invalid sport_type must be stripped before deliver is called."""
    r = MagicMock()
    r.raw = VOLLEYBALL_RAW
    # First attempt returns only volleyball (filtered to empty), second returns valid
    mocker.patch(
        "main.maccabi_crew.kickoff",
        side_effect=[r, _valid_result()],
    )
    run_pipeline()
    # Delivery on the second (valid) attempt — the first volleyball result was
    # stripped to [] but still counted as a successful parse, so deliver is
    # called with an empty list on attempt 1 OR with items on attempt 2.
    # Either way, deliver must have been called.
    mock_delivery.deliver.assert_called_once()
    delivered_items = mock_delivery.deliver.call_args[0][0]
    for item in delivered_items:
        assert item["sport_type"] in ("כדורסל", "כדורגל")
