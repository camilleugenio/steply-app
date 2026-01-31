"""
Flask kcal API
- One-time kcal estimate for an activity: POST /kcal/activity
- Intra-workout (continuous updates via repeated REST calls):
    POST /workouts/start
    POST /workouts/<workout_id>/update
    GET  /workouts/<workout_id>
    POST /workouts/<workout_id>/end
"""

from __future__ import annotations

import time
import uuid
from dataclasses import dataclass, asdict
from typing import Dict, Optional, Tuple, Literal

from flask import Flask, request, jsonify

app = Flask(__name__)

ActivityType = Literal["walk", "run", "cycle"]
Sex = Literal["male", "female"]


def met_for_activity(activity: str, speed_kmh: float) -> Tuple[float, str]:
    if activity == "walk":
        if speed_kmh < 3.2:
            return 2.5, "walk_very_easy(<3.2)"
        if speed_kmh < 4.8:
            return 3.3, "walk_moderate(3.2–4.8)"
        if speed_kmh < 6.4:
            return 4.3, "walk_brisk(4.8–6.4)"
        return 5.5, "walk_very_brisk(>=6.4)"

    if activity == "run":
        if speed_kmh < 8.0:
            return 7.0, "run_easy(<8.0)"
        if speed_kmh < 9.7:
            return 9.8, "run_moderate(8.0–9.7)"
        if speed_kmh < 11.3:
            return 11.0, "run_steady(9.7–11.3)"
        if speed_kmh < 12.9:
            return 11.8, "run_fast(11.3–12.9)"
        return 12.8, "run_very_fast(>=12.9)"

    if activity == "cycle":
        if speed_kmh < 16.0:
            return 4.0, "cycle_leisure(<16)"
        if speed_kmh < 19.0:
            return 6.8, "cycle_moderate(16–19)"
        if speed_kmh < 22.5:
            return 8.0, "cycle_vigorous(19–22.5)"
        return 10.0, "cycle_racing(>=22.5)"

    raise ValueError("Unsupported activity. Use: walk, run, cycle")


def kcal_from_met(met: float, weight_kg: float, duration_sec: float) -> float:
    hours = duration_sec / 3600.0
    return met * weight_kg * hours


def safe_float(x, field: str) -> float:
    try:
        return float(x)
    except Exception:
        raise ValueError(f"Field '{field}' must be a number.")


def safe_int(x, field: str) -> int:
    try:
        return int(x)
    except Exception:
        raise ValueError(f"Field '{field}' must be an integer.")


def require_json() -> dict:
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        raise ValueError("Request body must be JSON object.")
    return data


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


@app.route("/kcal/activity", methods=["POST", "GET"])
def kcal_activity():

    if request.method == "GET":
        return jsonify(
            {
                "hint": "POST JSON to this endpoint.",
                "example": {
                    "activity": "run",
                    "duration_sec": 1800,
                    "distance_km": 5,
                    "weight_kg": 70,
                    "age_years": 27,
                    "sex": "male",
                },
            }
        )

    try:
        data = require_json()

        activity = str(data["activity"]).strip().lower()
        if activity not in ("walk", "run", "cycle"):
            raise ValueError("Field 'activity' must be one of: walk, run, cycle")

        if "duration_sec" in data:
            duration_sec = safe_float(data["duration_sec"], "duration_sec")
        elif "duration_min" in data:
            duration_sec = safe_float(data["duration_min"], "duration_min") * 60.0
        else:
            raise ValueError("Provide either 'duration_sec' or 'duration_min'.")

        distance_km = safe_float(data["distance_km"], "distance_km")
        weight_kg = safe_float(data["weight_kg"], "weight_kg")


        age_years = safe_int(data.get("age_years", 0), "age_years") if "age_years" in data else None
        sex = str(data.get("sex")).strip().lower() if "sex" in data else None

        if duration_sec <= 0:
            raise ValueError("Duration must be > 0.")
        if distance_km <= 0:
            raise ValueError("Distance must be > 0.")
        if weight_kg <= 0:
            raise ValueError("Weight must be > 0.")

        speed_kmh = distance_km / (duration_sec / 3600.0)
        if speed_kmh <= 0 or speed_kmh > 100:
            raise ValueError(f"Unrealistic speed computed ({speed_kmh:.2f} km/h). Check inputs.")

        met, bucket = met_for_activity(activity, speed_kmh)
        kcal = kcal_from_met(met, weight_kg, duration_sec)

        return jsonify(
            {
                "kcal": round(kcal, 2),
                "details": {
                    "activity": activity,
                    "duration_sec": round(duration_sec, 2),
                    "distance_km": round(distance_km, 4),
                    "weight_kg": round(weight_kg, 2),
                    "speed_kmh": round(speed_kmh, 2),
                    "met": met,
                    "met_bucket": bucket,
                    "age_years": age_years,
                    "sex": sex,
                    "note": "Age/sex accepted; MET estimate uses intensity (speed) + weight + duration.",
                },
            }
        )

    except KeyError as e:
        return jsonify({"error": f"Missing field: {e}"}), 400
    except ValueError as e:
        return jsonify({"error": str(e)}), 400



@dataclass
class WorkoutSession:
    workout_id: str
    activity: str
    weight_kg: float
    age_years: Optional[int]
    sex: Optional[str]


    last_elapsed_sec: float = 0.0
    last_distance_km: float = 0.0
    last_kcal: float = 0.0
    last_speed_kmh: float = 0.0
    last_met: float = 0.0
    last_met_bucket: str = ""

    created_ts: float = 0.0
    updated_ts: float = 0.0
    ended: bool = False


WORKOUTS: Dict[str, WorkoutSession] = {}


def get_session_or_404(workout_id: str) -> WorkoutSession:
    sess = WORKOUTS.get(workout_id)
    if not sess:
        raise ValueError("Unknown workout_id.")
    return sess


@app.route("/workouts/start", methods=["POST", "GET"])
def workouts_start():
    if request.method == "GET":
        return jsonify(
            {
                "hint": "POST JSON to start a workout session.",
                "example": {"activity": "run", "weight_kg": 70, "age_years": 27, "sex": "male"},
            }
        )

    try:
        data = require_json()
        activity = str(data["activity"]).strip().lower()
        if activity not in ("walk", "run", "cycle"):
            raise ValueError("Field 'activity' must be one of: walk, run, cycle")

        weight_kg = safe_float(data["weight_kg"], "weight_kg")
        if weight_kg <= 0:
            raise ValueError("weight_kg must be > 0.")

        age_years = safe_int(data.get("age_years", 0), "age_years") if "age_years" in data else None
        sex = str(data.get("sex")).strip().lower() if "sex" in data else None

        workout_id = uuid.uuid4().hex
        now = time.time()
        sess = WorkoutSession(
            workout_id=workout_id,
            activity=activity,
            weight_kg=weight_kg,
            age_years=age_years,
            sex=sex,
            created_ts=now,
            updated_ts=now,
        )
        WORKOUTS[workout_id] = sess

        return jsonify(
            {
                "workout_id": workout_id,
                "message": "Workout started. Send cumulative updates to /workouts/<id>/update",
            }
        )

    except KeyError as e:
        return jsonify({"error": f"Missing field: {e}"}), 400
    except ValueError as e:
        return jsonify({"error": str(e)}), 400


@app.route("/workouts/<workout_id>/update", methods=["POST"])
def workouts_update(workout_id: str):

    try:
        sess = get_session_or_404(workout_id)
        if sess.ended:
            return jsonify({"error": "Workout already ended."}), 409

        data = require_json()

        elapsed_sec = safe_float(data["elapsed_sec"], "elapsed_sec")
        distance_km = safe_float(data["distance_km"], "distance_km")

        if elapsed_sec <= 0:
            raise ValueError("elapsed_sec must be > 0.")
        if distance_km < 0:
            raise ValueError("distance_km must be >= 0.")


        if "weight_kg" in data:
            w = safe_float(data["weight_kg"], "weight_kg")
            if w <= 0:
                raise ValueError("weight_kg must be > 0.")
            sess.weight_kg = w


        prev_t = sess.last_elapsed_sec
        prev_d = sess.last_distance_km

        dt = elapsed_sec - prev_t
        dd = distance_km - prev_d


        if dt < 0:

            return jsonify({"error": "elapsed_sec went backwards"}), 409
        if dd < 0:

            return jsonify({"error": "distance_km went backwards"}), 409


        if dt == 0:
            return jsonify(
                {
                    "workout_id": workout_id,
                    "current_kcal": round(sess.last_kcal, 2),
                    "details": {
                        "activity": sess.activity,
                        "elapsed_sec": round(elapsed_sec, 2),
                        "distance_km": round(distance_km, 4),
                        "weight_kg": round(sess.weight_kg, 2),
                        "speed_kmh": round(sess.last_speed_kmh, 2),
                        "met": sess.last_met,
                        "met_bucket": sess.last_met_bucket,
                        "note": "No new time since last update; kcal unchanged.",
                    },
                }
            )


        if dd == 0:

            met = 0.0
            bucket = "stopped"
            speed_kmh = 0.0


        else:
            speed_kmh = dd / (dt / 3600.0)
            if speed_kmh <= 0 or speed_kmh > 100:
                raise ValueError(f"Unrealistic interval speed ({speed_kmh:.2f} km/h). Check inputs.")
            met, bucket = met_for_activity(sess.activity, speed_kmh)

        kcal_delta = kcal_from_met(met, sess.weight_kg, dt)


        sess.last_kcal += kcal_delta
        sess.last_elapsed_sec = elapsed_sec
        sess.last_distance_km = distance_km
        sess.last_speed_kmh = speed_kmh
        sess.last_met = met
        sess.last_met_bucket = bucket
        sess.updated_ts = time.time()

        return jsonify(
            {
                "workout_id": workout_id,
                "current_kcal": round(sess.last_kcal, 2),
                "details": {
                    "activity": sess.activity,
                    "elapsed_sec": round(elapsed_sec, 2),
                    "distance_km": round(distance_km, 4),
                    "weight_kg": round(sess.weight_kg, 2),
                    "interval_sec": round(dt, 2),
                    "interval_distance_km": round(dd, 4),
                    "speed_kmh": round(speed_kmh, 2),
                    "met": met,
                    "met_bucket": bucket,
                    "kcal_added": round(kcal_delta, 3),
                    "note": "Monotonic kcal: adds kcal over last interval (dt, dd) instead of recomputing from totals.",
                },
            }
        )

    except KeyError as e:
        return jsonify({"error": f"Missing field: {e}"}), 400
    except ValueError as e:
        return jsonify({"error": str(e)}), 400



@app.route("/workouts/<workout_id>", methods=["GET"])
def workouts_get(workout_id: str):
    try:
        sess = get_session_or_404(workout_id)
        payload = asdict(sess)
        return jsonify(payload)
    except ValueError as e:
        return jsonify({"error": str(e)}), 404


@app.route("/workouts/<workout_id>/end", methods=["POST"])
def workouts_end(workout_id: str):
    try:
        sess = get_session_or_404(workout_id)
        sess.ended = True
        sess.updated_ts = time.time()
        return jsonify(
            {
                "workout_id": workout_id,
                "final_kcal": round(sess.last_kcal, 2),
                "message": "Workout ended.",
            }
        )
    except ValueError as e:
        return jsonify({"error": str(e)}), 404


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080, debug=True)
