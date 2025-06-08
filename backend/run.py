from flask import Flask, jsonify, request
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

data_store = {
    "API_KEY_123": {"quantity": "100ml"},
    "API_KEY_456": {"quantity": "200ml"},
}

@app.route('/api/quantity', methods=['GET'])
def get_value():
    api_key = request.headers.get("x-api-key")  # API key in headers
    if not api_key or api_key not in data_store:
        return jsonify({"error": "Invalid or missing API key"}), 403
    return jsonify(data_store[api_key])

@app.route('/api/quantity', methods=['POST'])
def update_value():
    api_key = request.headers.get("x-api-key")
    if not api_key or api_key not in data_store:
        return jsonify({"error": "Invalid or missing API key"}), 403

    content = request.get_json()
    key = content.get("key")
    value = content.get("value")

    if key in data_store[api_key]:
        data_store[api_key][key] = value
        return jsonify({"message": "Value updated", "data": data_store[api_key]}), 200
    else:
        return jsonify({"error": "Key not found"}), 404

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=4000)
