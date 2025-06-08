import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

class Config:
    SECRET_KEY = os.getenv("SECRET_KEY", "day_secret_key")
    SQLALCHEMY_DATABASE_URI = os.getenv(
        "DATABASE_URL",
        f"sqlite:///{BASE_DIR / 'watertracker.db'}"
    )
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    DAILY_GOAL_ML = int(os.getenv("DAILY_GOAL_ML", 2000))