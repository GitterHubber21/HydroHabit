from flask import Blueprint, request, jsonify
from flask_login import login_user, logout_user, login_required
from app.models import User
from app import db

auth_bp = Blueprint("auth", __name__, url_prefix="/api")

@auth_bp.route("/signup", methods=["POST"])
def signup():
    data = request.json or {}
    username = data.get("username", "").strip()
    password = data.get("password", "").strip()

    if not username or not password:
        return jsonify({"error": "Username and password required."}), 400
    if User.query.filter_by(username=username).first():
        return jsonify({"error":"Username already exists"}), 409
    user = User(username=username)
    user.set_password(password)
    db.session.add(user)
    db.session.commit()
    return jsonify({"message":"User created successfully"}), 201

@auth_bp.route("/login", methods=["POST"])
def login():
    data=request.json or {}
    user = User.query.filter_by(username=data.get("username")).first()

    if user and user.check_password(data.get("password", "")):
        login_user(user)
        return jsonify({"message":"Logged in"})
    return jsonify({"error":"Invalid credentials"}), 401

@auth_bp.route("/logout", methods=["POST"])
@login_required
def logout():
    logout_user()
    return jsonify({"message":"Logged out successfully"})
@auth_bp.route("/current_user", methods=["GET"])
@login_required
def current_user():
    from flask_login import current_user
    return jsonify({"username": current_user.username})


@auth_bp.route("/change_password", methods=["POST"])
@login_required
def change_password():
    from flask_login import current_user

    data = request.json or {}
    old_password = data.get("old_password", "").strip()
    new_password = data.get("new_password", "").strip()

    if not current_user.check_password(old_password):
        return jsonify({"error": "Current password is incorrect."}), 401

    current_user.set_password(new_password)
    db.session.commit()

    return jsonify({"message": "Password changed successfully"}), 200
@auth_bp.route("/delete_account", methods=["POST"])
@login_required
def delete_account():
    from flask_login import current_user

    data = request.json or {}
    password = data.get("password", "").strip()

    if not current_user.check_password(password):
        return jsonify({"error": "Password is incorrect."}), 401

    db.session.delete(current_user)
    db.session.commit()
    logout_user()
    return jsonify({"message": "Account deleted successfully"}), 200