import calendar
import json
from datetime import date, timedelta
from flask import Blueprint, request, jsonify, current_app
from flask_login import login_required, current_user
from app.models import WaterLog, WaterStats
from app import db

main_bp = Blueprint("main", __name__, url_prefix="/api")

def get_week_start_end(target_date):
    days_since_monday = target_date.weekday()
    week_start = target_date - timedelta(days=days_since_monday)
    week_end=week_start + timedelta(days=6)
    return week_start, week_end
def get_month_start_end(target_date):
    month_start = target_date.replace(day=1)
    next_month = month_start.replace(month=month_start.month + 1) if month_start.month < 12 else month_start.replace(year=month_start.year + 1, month=1)
    month_end = next_month - timedelta(days=1)
    return month_start, month_end


@main_bp.route("/log", methods=["POST"])
@login_required
def log_water():
    data = request.json or {}
    volume = float(data.get("volume_ml", 0.0))
    if volume < 0.0:
        return jsonify({"error":"Non negative volume required"}), 400

    today = date.today()
    log = WaterLog.query.filter_by(user_id=current_user.id, date=today).first()
    if not log:
        log = WaterLog(user_id=current_user.id, date=today, volume_ml=0)

    log.volume_ml = volume
    private_daily_goal = log.daily_goal_ml
    log.goal_met = log.volume_ml >= private_daily_goal

    db.session.add(log)
    db.session.commit()

    update_user_stats(current_user.id)
    return jsonify({
        "date": str(log.date),
        "volume_ml": log.volume_ml,
        "goal_met": log.goal_met,
        "daily_goal_ml": private_daily_goal
    })

@main_bp.route("/detailed-stats", methods=["GET"])
@login_required
def detailed_stats():
    update_user_stats(current_user.id)

    stats = WaterStats.query.filter_by(
        user_id=current_user.id,
        calculated_date=date.today()
    ).first()

    if not stats:
        return jsonify({"error": "No statistics available"}), 404

    goal_completed_dates = json.loads(stats.month_goal_completed_dates)

    return jsonify({
        "today_percentage": round(stats.today_percentage, 2),
        "week_percentage": round(stats.week_percentage, 2),
        "month_percentage": round(stats.month_percentage, 2),
        "today_volume_ml": stats.today_volume_ml,
        "week_volume_ml": stats.week_volume_ml,
        "month_volume_ml": stats.month_volume_ml,
        "days_in_current_month": stats.days_in_current_month,
        "month_goal_completed_dates": goal_completed_dates,
        "calculated_date": str(stats.calculated_date)
    })
@main_bp.route("daily-goal", methods=["POST", "GET"])
@login_required
def daily_goal():
    today = date.today()
    today_log=WaterLog.query.filter_by(user_id=current_user.id, date=today).first()
    if request.method == "POST":
        data = request.json or {}
        new_goal = data.get("daily_volume_goal")
        try:
            new_goal = float(new_goal)
            if new_goal <=0:
                return jsonify({"error":"Positive volume required."}), 400
        except(TypeError, ValueError):
            return jsonify({"error":"Invalid volume value"}), 400

        if not today_log and today_log.daily_goal_ml is None:
            today_log = WaterLog(user_id=current_user.id, date=today)
        today_log.daily_goal_ml = new_goal
        db.session.add(today_log)
        db.session.commit()

        return jsonify("daily_volume_goal", new_goal), 200
    else:
        if today_log.daily_goal_ml is None:
            today_log.daily_goal_ml = current_app.config.get("DAILY_GOAL_ML", 3000.0)
            private_daily_goal = today_log.daily_goal_ml
        else:
            private_daily_goal = today_log.daily_goal_ml
        db.session.add(today_log)
        db.session.commit()
        return jsonify({"daily_volume_goal": private_daily_goal}), 200

def update_user_stats(user_id):
    today=date.today()

    today_log=WaterLog.query.filter_by(user_id=user_id, date=today).first()
    if not today_log or today_log.daily_goal_ml is None:
        daily_goal_ml = 3000.0
        today_log.daily_goal_ml = daily_goal_ml

    else:
        daily_goal_ml = today_log.daily_goal_ml
        db.session.add(today_log)
        db.session.commit()
    today_volume=float(today_log.volume_ml) if today_log else 0
    today_percentage = (today_volume / daily_goal_ml) * 100

    week_start, week_end = get_week_start_end(today)
    week_logs = WaterLog.query.filter(
        WaterLog.user_id == user_id,
        WaterLog.date >= week_start,
        WaterLog.date <= week_end
    ).all()
    week_volume = float(sum(log.volume_ml for log in week_logs))
    week_goal_ml = daily_goal_ml*7
    week_percentage=(week_volume/week_goal_ml)*100

    month_start, month_end = get_month_start_end(today)
    month_logs = WaterLog.query.filter(
        WaterLog.user_id == user_id,
        WaterLog.date >= month_start,
        WaterLog.date <= month_end
    ).all()
    month_volume = float(sum(log.volume_ml for log in month_logs))

    days_in_month = calendar.monthrange(today.year, today.month)[1]
    month_goal_ml = float(days_in_month * daily_goal_ml)
    month_percentage = (month_volume / month_goal_ml) * 100

    goal_completed_dates=[
        str(log.date) for log in month_logs
        if log.goal_met
    ]

    stats = WaterStats.query.filter_by(
        user_id=user_id,
        calculated_date=today
    ).first()

    if not stats:
        stats= WaterStats(user_id=user_id, calculated_date=today)

    stats.today_percentage = today_percentage
    stats.week_percentage = week_percentage
    stats.month_percentage = month_percentage
    stats.month_goal_completed_dates = json.dumps(goal_completed_dates)
    stats.today_volume_ml = today_volume
    stats.week_volume_ml = week_volume
    stats.month_volume_ml = month_volume
    stats.days_in_current_month = days_in_month

    db.session.add(stats)
    db.session.commit()

    return stats

