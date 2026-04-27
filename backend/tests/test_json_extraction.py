"""
Tests for extract_news_items() — pure Python, no mocking required.

Covers: markdown fence stripping, whitespace handling, sport_type filtering,
empty arrays, and JSONDecodeError propagation.
"""
import json
import pytest

from main import extract_news_items


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_json(items: list) -> str:
    return json.dumps(items, ensure_ascii=False)


BASKETBALL_ITEM = {
    "title": "כותרת כדורסל",
    "content": "תוכן",
    "source": "ספורט 5",
    "reliability": "CONFIRMED",
    "link": "https://example.com/1",
    "sport_type": "כדורסל",
}

FOOTBALL_ITEM = {
    "title": "כותרת כדורגל",
    "content": "תוכן",
    "source": "ONE",
    "reliability": "MEDIUM",
    "link": "https://example.com/2",
    "sport_type": "כדורגל",
}

VOLLEYBALL_ITEM = {
    "title": "כותרת כדורעף",
    "content": "תוכן",
    "source": "ספורט 5",
    "reliability": "LOW",
    "link": "https://example.com/3",
    "sport_type": "כדורעף",
}


# ---------------------------------------------------------------------------
# Parsing / stripping tests
# ---------------------------------------------------------------------------

def test_clean_json_parsed():
    raw = _make_json([BASKETBALL_ITEM])
    result = extract_news_items(raw)
    assert len(result) == 1
    assert result[0]["title"] == BASKETBALL_ITEM["title"]


def test_strips_markdown_json_fence():
    raw = f"```json\n{_make_json([BASKETBALL_ITEM])}\n```"
    result = extract_news_items(raw)
    assert len(result) == 1


def test_strips_plain_fence():
    raw = f"```\n{_make_json([BASKETBALL_ITEM])}\n```"
    result = extract_news_items(raw)
    assert len(result) == 1


def test_strips_leading_trailing_whitespace():
    raw = f"   \n{_make_json([BASKETBALL_ITEM])}\n   "
    result = extract_news_items(raw)
    assert len(result) == 1


def test_strips_fence_and_whitespace_combined():
    raw = f"  ```json\n  {_make_json([FOOTBALL_ITEM])}\n  ```  "
    result = extract_news_items(raw)
    assert len(result) == 1


# ---------------------------------------------------------------------------
# Sport-type filter tests
# ---------------------------------------------------------------------------

def test_keeps_basketball_items():
    raw = _make_json([BASKETBALL_ITEM])
    result = extract_news_items(raw)
    assert result[0]["sport_type"] == "כדורסל"


def test_keeps_football_items():
    raw = _make_json([FOOTBALL_ITEM])
    result = extract_news_items(raw)
    assert result[0]["sport_type"] == "כדורגל"


def test_filters_out_other_sports():
    raw = _make_json([BASKETBALL_ITEM, VOLLEYBALL_ITEM])
    result = extract_news_items(raw)
    assert len(result) == 1
    assert result[0]["sport_type"] == "כדורסל"


def test_filters_out_missing_sport_type():
    item_no_sport = {k: v for k, v in BASKETBALL_ITEM.items() if k != "sport_type"}
    raw = _make_json([item_no_sport, FOOTBALL_ITEM])
    result = extract_news_items(raw)
    assert len(result) == 1
    assert result[0]["sport_type"] == "כדורגל"


def test_empty_array_returns_empty_list():
    result = extract_news_items("[]")
    assert result == []


def test_invalid_json_raises_decode_error():
    with pytest.raises(json.JSONDecodeError):
        extract_news_items("this is not json at all {{}}")
