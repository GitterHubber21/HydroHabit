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

    def set_password(self, password: str):
        self.password_hash = generate_password_hash(password)

    def check_password(self, password: str) -> bool:
        return check_password_hash(self.password_hash, password)


class WaterLog(db.Model):
    __tablename__ = "water_logs"

    id= db.Column(db.Integer, primary_key=True)
    date = db.Column(db.Date, default=date.today, nullable=False)
    volume_ml = db.Column(db.Integer, default=0, nullable=False)
    goal_met = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)


@login_manager.user_loader
def load_user(user_id):
    return User.query.get(int(user_id))

