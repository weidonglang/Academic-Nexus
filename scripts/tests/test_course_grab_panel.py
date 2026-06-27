import importlib.util
from pathlib import Path


MODULE_PATH = Path(__file__).resolve().parents[1] / "course_grab_panel.py"
spec = importlib.util.spec_from_file_location("course_grab_panel", MODULE_PATH)
panel = importlib.util.module_from_spec(spec)
spec.loader.exec_module(panel)


def test_extract_records_from_data_records():
    records, path = panel.extract_offering_records({"data": {"records": [{"id": 1}]}})

    assert path == "data.records"
    assert records == [{"id": 1}]


def test_extract_records_from_data_array():
    records, path = panel.extract_offering_records({"data": [{"offeringId": 2}]})

    assert path == "data"
    assert records == [{"offeringId": 2}]


def test_extract_records_from_root_records():
    records, path = panel.extract_offering_records({"records": [{"id": 3}]})

    assert path == "records"
    assert records == [{"id": 3}]


def test_normalize_offering_supports_id_aliases():
    row = panel.normalize_offering_item({
        "offeringId": 9,
        "courseCode": "CS101",
        "courseName": "软件工程",
        "teacherName": "王老师",
        "capacity": 50,
        "selectedCount": 7,
        "scheduleText": "周一 1-2节",
        "classroom": "A101",
    })

    assert row == ["9", "CS101", "软件工程", "王老师", "50", "7", "43", "周一 1-2节", "A101"]


def test_empty_diagnostics_mentions_term_and_token():
    message = panel.build_api_empty_diagnostics(
        200,
        "/api/admin/course-offerings?page=1&size=200&term=2026-2027-1",
        {"data": {"records": []}},
        "2026-2027-1",
        True,
    )

    assert "HTTP 200" in message
    assert "currentTerm=2026-2027-1" in message
    assert "tokenPresent=yes" in message
