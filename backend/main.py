from flask import Flask, jsonify

app = Flask(__name__)

@app.route('/api/hello')
def hello():
    return jsonify(message="Hello from Flask!")

@app.route('/api/color')
def color():
    return jsonify(color="Yellow!")

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=4000, debug=False)