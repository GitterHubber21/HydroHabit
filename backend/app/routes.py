from datetime import date
from flask import Blueprint, request, jsonify, current_app
from flask_login import login_required, current_user
from app.models import WaterLog
from app import db

main_bp = Blueprint("main", __name__, url_prefix="/api")
@main_bp.route("/log", methods=["POST"])
@login_required
def log_water():
    data = request.json or {}
    volume = int(data.get("volume_ml", 0))
    if volume <= 0:
        return jsonify({"error":"Positive volume required"}), 400

    today = date.today()
    log = WaterLog.query.filter_by(user_id=current_user.id, date=today).first()
    if not log:
        log=WaterLog.query.filter_by(user_id=current_user.id, date=today)

    log.volume_ml += volume
    daily_goal = current_app.config["DAILY_GOAL_ML"]
    log.goal_met = log.volume_ml >= daily_goal

    db.session.add(log)
    db.session.commit()
    return jsonify({
        "date": str(log.date),
        "volume_ml": log.volume_ml,
        "goal_met": log.goal_met,
        "daily_goal_ml": daily_goal
    })

@main_bp.route("/stats", methods=["GET"])
@login_required
def stats():
    logs = WaterLog.query.filter_by(user_id=current_user.id).all()
    total_volume = sum(l.volume_ml for l in logs)
    days_goal_met=sum(1 for l in logs if l.goal_met)
    return jsonify({
        "total_volume_ml":total_volume,
        "days_goal_completed": days_goal_met,
        "log_count": len(logs)
    })