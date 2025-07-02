PORT="${1:-4000}"

kill -9 $(lsof -ti ":$PORT") 2>/dev/null
git pull
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export FLASK_APP=run.py
rm -rf migrations
flask db upgrade || (
  flask db init && flask db migrate -m "init" && flask db upgrade && echo "Migration is finished."
)
gunicorn -b ":$PORT" run:app
echo "Backend is running on PORT: $PORT"