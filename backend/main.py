from flask import Flask, jsonify, request

app = Flask(__name__)
data_store = {"quantity": "100ml"}

@app.route('/api/quantity', methods=['POST'])
def update_value():
    content = request.get_json()
    key = content.get("key")
    value = content.get("value")

    if key in data_store:
        data_store[key] = value
        return jsonify({"message": "Value updated", "data": data_store}), 200
    else:
        return jsonify({"error": "Key not found"}), 404


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=4000, debug=False)