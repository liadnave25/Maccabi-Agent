"""
Tests for the internet_search @tool function.

All tests mock requests.request (no real Serper calls) and time.sleep
(no 15-second delays).  The @tool decorator wraps the function in a
LangChain StructuredTool; call the underlying logic via ._run().
"""
import json
import pytest
from unittest.mock import MagicMock

from main import internet_search, LOCAL_SITES, GLOBAL_BASKETBALL_SITES, GLOBAL_FOOTBALL_SITES


# ---------------------------------------------------------------------------
# Autouse: suppress the 15-second sleep in every test in this module
# ---------------------------------------------------------------------------

@pytest.fixture(autouse=True)
def no_sleep(mocker):
    return mocker.patch("main.time.sleep")


# ---------------------------------------------------------------------------
# Helper
# ---------------------------------------------------------------------------

def _mock_serper(mocker, sample_serper_response):
    mock_resp = MagicMock()
    mock_resp.json.return_value = sample_serper_response
    return mocker.patch("main.requests.request", return_value=mock_resp)


def _sent_query(mock_req):
    """Decode the JSON payload sent to requests.request and return the 'q' field."""
    raw_data = mock_req.call_args.kwargs.get("data") or mock_req.call_args[1]["data"]
    return json.loads(raw_data)["q"]


# ---------------------------------------------------------------------------
# Query-construction tests
# ---------------------------------------------------------------------------

def test_local_basketball_query(mocker, sample_serper_response):
    mock_req = _mock_serper(mocker, sample_serper_response)
    internet_search._run(query_data="Basketball", scope="local")
    assert "מכבי תל אביב כדורסל" in _sent_query(mock_req)


def test_local_football_query(mocker, sample_serper_response):
    mock_req = _mock_serper(mocker, sample_serper_response)
    internet_search._run(query_data="Football", scope="local")
    assert "מכבי תל אביב כדורגל" in _sent_query(mock_req)


def test_local_uses_local_sites(mocker, sample_serper_response):
    mock_req = _mock_serper(mocker, sample_serper_response)
    internet_search._run(query_data="Football", scope="local")
    q = _sent_query(mock_req)
    for site in LOCAL_SITES:
        assert site in q


def test_global_basketball_routing(mocker, sample_serper_response):
    mock_req = _mock_serper(mocker, sample_serper_response)
    internet_search._run(query_data='"Player A" OR "Player B"', scope="global_basketball")
    q = _sent_query(mock_req)
    for site in GLOBAL_BASKETBALL_SITES:
        assert site in q
    for site in LOCAL_SITES:
        assert site not in q


def test_global_football_routing(mocker, sample_serper_response):
    mock_req = _mock_serper(mocker, sample_serper_response)
    internet_search._run(query_data='"Player A"', scope="global_football")
    q = _sent_query(mock_req)
    for site in GLOBAL_FOOTBALL_SITES:
        assert site in q


def test_global_query_wraps_maccabi(mocker, sample_serper_response):
    mock_req = _mock_serper(mocker, sample_serper_response)
    internet_search._run(query_data='"John Doe"', scope="global_basketball")
    assert "Maccabi Tel Aviv" in _sent_query(mock_req)


# ---------------------------------------------------------------------------
# Output-format tests
# ---------------------------------------------------------------------------

def test_output_format(mocker, sample_serper_response):
    _mock_serper(mocker, sample_serper_response)
    result = internet_search._run(query_data="Basketball", scope="local")
    assert "Title:" in result
    assert "Link:" in result
    assert "Snippet:" in result
    assert "---" in result


def test_no_results(mocker):
    mock_resp = MagicMock()
    mock_resp.json.return_value = {"organic": []}
    mocker.patch("main.requests.request", return_value=mock_resp)
    result = internet_search._run(query_data="Basketball", scope="local")
    assert "No results found" in result


def test_request_exception(mocker):
    mocker.patch("main.requests.request", side_effect=ConnectionError("timeout"))
    result = internet_search._run(query_data="Football", scope="local")
    assert "Error executing Google Search" in result


# ---------------------------------------------------------------------------
# Side-effect / header tests
# ---------------------------------------------------------------------------

def test_sleep_is_called(no_sleep, mocker, sample_serper_response):
    # no_sleep is the autouse mock; we just assert it received the right value.
    _mock_serper(mocker, sample_serper_response)
    internet_search._run(query_data="Football", scope="local")
    no_sleep.assert_called_once_with(15)


def test_api_key_sent_in_header(mocker, sample_serper_response):
    mock_req = _mock_serper(mocker, sample_serper_response)
    internet_search._run(query_data="Football", scope="local")
    headers = mock_req.call_args.kwargs.get("headers") or mock_req.call_args[1]["headers"]
    assert headers["X-API-KEY"] == "fake-serper-key"
