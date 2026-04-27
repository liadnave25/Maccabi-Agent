"""
Tests for upload_to_firebase().

All Firebase SDK calls are mocked so no real cloud state is touched.
Note on imports: main.py uses `from firebase_admin import credentials, firestore, storage`
so those names live in the `main` module's namespace and must be patched there.
`firebase_admin.initialize_app` and `firebase_admin._apps` are patched on the
`firebase_admin` module itself (accessed as `main.firebase_admin.*`).
"""
import datetime
import pytest
from unittest.mock import MagicMock, call
import firebase_admin

from main import upload_to_firebase


DATE_STR = "April 26, 2025"
AUDIO_PATH = "maccabi_daily.mp3"
FAKE_SIGNED_URL = "https://storage.googleapis.com/fake/podcast.mp3?token=abc"


# ---------------------------------------------------------------------------
# Shared fixture: full Firebase mock chain
# ---------------------------------------------------------------------------

@pytest.fixture
def fb(mocker, sample_news_items):
    """
    Returns a namespace with every Firebase mock wired up.
    `fb.db`  — mock Firestore client
    `fb.bucket` — mock Storage bucket
    `fb.blob`   — mock blob returned by bucket.blob()
    `fb.docs`   — list of mock old-news documents
    `fb.init`   — mock for firebase_admin.initialize_app
    `fb.items`  — the sample news items list passed to the function
    """
    mock_blob = MagicMock()
    mock_blob.generate_signed_url.return_value = FAKE_SIGNED_URL

    mock_bucket = MagicMock()
    mock_bucket.blob.return_value = mock_blob

    mock_doc1, mock_doc2 = MagicMock(), MagicMock()
    mock_db = MagicMock()
    mock_db.collection.return_value.stream.return_value = [mock_doc1, mock_doc2]

    class M:
        db = mock_db
        bucket = mock_bucket
        blob = mock_blob
        docs = [mock_doc1, mock_doc2]
        init = mocker.patch("main.firebase_admin.initialize_app")
        items = sample_news_items

    mocker.patch("main.firestore.client", return_value=mock_db)
    mocker.patch("main.storage.bucket", return_value=mock_bucket)
    mocker.patch("main.credentials.Certificate", return_value=MagicMock())

    return M()


# ---------------------------------------------------------------------------
# Initialization branch
# ---------------------------------------------------------------------------

def test_initializes_firebase_when_no_apps(fb, mocker):
    mocker.patch.object(firebase_admin, "_apps", {})
    upload_to_firebase(fb.items, AUDIO_PATH, DATE_STR)
    fb.init.assert_called_once()


def test_skips_init_when_already_initialized(fb, mocker):
    mocker.patch.object(firebase_admin, "_apps", {"[DEFAULT]": MagicMock()})
    upload_to_firebase(fb.items, AUDIO_PATH, DATE_STR)
    fb.init.assert_not_called()


# ---------------------------------------------------------------------------
# Storage upload
# ---------------------------------------------------------------------------

def test_uploads_audio_file(fb, mocker):
    mocker.patch.object(firebase_admin, "_apps", {"[DEFAULT]": MagicMock()})
    upload_to_firebase(fb.items, AUDIO_PATH, DATE_STR)
    fb.blob.upload_from_filename.assert_called_once_with(AUDIO_PATH)


def test_signed_url_uses_365_day_expiration(fb, mocker):
    mocker.patch.object(firebase_admin, "_apps", {"[DEFAULT]": MagicMock()})
    upload_to_firebase(fb.items, AUDIO_PATH, DATE_STR)
    fb.blob.generate_signed_url.assert_called_once_with(
        expiration=datetime.timedelta(days=365)
    )


# ---------------------------------------------------------------------------
# Firestore — System/LatestPodcast document
# ---------------------------------------------------------------------------

def test_system_latest_podcast_set(fb, mocker):
    mocker.patch.object(firebase_admin, "_apps", {"[DEFAULT]": MagicMock()})
    upload_to_firebase(fb.items, AUDIO_PATH, DATE_STR)

    # Verify .set() was called on System/LatestPodcast with the signed URL
    system_doc_set = (
        fb.db.collection("System").document("LatestPodcast").set
    )
    system_doc_set.assert_called_once()
    set_payload = system_doc_set.call_args[0][0]
    assert set_payload["audio_url"] == FAKE_SIGNED_URL
    assert set_payload["date"] == DATE_STR


# ---------------------------------------------------------------------------
# Firestore — old-news cleanup
# ---------------------------------------------------------------------------

def test_old_news_deleted(fb, mocker):
    mocker.patch.object(firebase_admin, "_apps", {"[DEFAULT]": MagicMock()})
    upload_to_firebase(fb.items, AUDIO_PATH, DATE_STR)
    for doc in fb.docs:
        doc.reference.delete.assert_called_once()


def test_empty_list_cleanup_still_runs(fb, mocker):
    mocker.patch.object(firebase_admin, "_apps", {"[DEFAULT]": MagicMock()})
    upload_to_firebase([], AUDIO_PATH, DATE_STR)
    for doc in fb.docs:
        doc.reference.delete.assert_called_once()


# ---------------------------------------------------------------------------
# Firestore — new news upload
# ---------------------------------------------------------------------------

def test_new_items_uploaded_count(fb, mocker):
    mocker.patch.object(firebase_admin, "_apps", {"[DEFAULT]": MagicMock()})
    upload_to_firebase(fb.items, AUDIO_PATH, DATE_STR)
    assert fb.db.collection("DailyNews").add.call_count == len(fb.items)


def test_date_injected_into_items(fb, mocker):
    mocker.patch.object(firebase_admin, "_apps", {"[DEFAULT]": MagicMock()})
    upload_to_firebase(fb.items, AUDIO_PATH, DATE_STR)
    for upload_call in fb.db.collection("DailyNews").add.call_args_list:
        assert upload_call[0][0]["date"] == DATE_STR


def test_empty_list_uploads_nothing(fb, mocker):
    mocker.patch.object(firebase_admin, "_apps", {"[DEFAULT]": MagicMock()})
    upload_to_firebase([], AUDIO_PATH, DATE_STR)
    fb.db.collection("DailyNews").add.assert_not_called()
