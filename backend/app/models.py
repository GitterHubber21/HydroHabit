from datetime import date
from sqlalchemy.orm import backref
from werkzeug.security import generate_password_hash, check_password_hash
from flask_login import UserMixin
from app import db, login_manager

class User(UserMixin, db.Model):
    __tablename__ = "users"

    id=db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(128), nullable=False)
    water_logs = db.relationship("WaterLog", backref="user", lazy=True)
    password_hash = db.Column(db.String(128), nullable=False)

    def set_password(self, password: str):
        self.password_hash = generate_password_hash(password)

    def check_password(self, password: str) -> bool:
        return check_password_hash(self.password_hash, password)


class WaterLog(db.Model):
    __tablename__ = "water_logs"

    id= db.Column(db.Integer, primary_key=True)
    date = db.Column(db.Date, default=date.today, nullable=False)
    volume_ml = db.Column(db.Integer, default=0, nullable=False)
    goal_met = db.Column(db.Boolean, default=False, nullable=False)
    user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)


class WaterStats(db.Model):
    __tablename__ = "water_stats"

    id = db.Column(db.Integer, primary_key=True)

    user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)

    calculated_date = db.Column(db.Date, default=date.today, nullable=False)

    today_percentage = db.Column(db.Float, default=0.0, nullable=False)

    week_percentage = db.Column(db.Float, default=0.0, nullable=False)

    month_percentage = db.Column(db.Float, default=0.0, nullable=False)

    month_goal_completed_dates = db.Column(db.Text, default="[]", nullable=False)

    today_volume_ml = db.Column(db.Float, default=0.0, nullable=False)

    week_volume_ml = db.Column(db.Float, default=0.0, nullable=False)

    month_volume_ml = db.Column(db.Float, default=0.0, nullable=False)

    days_in_current_month = db.Column(db.Integer, default=0, nullable=False)

@login_manager.user_loader
def load_user(user_id):
    return User.query.get(int(user_id))

