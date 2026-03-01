from flask import Flask, request, jsonify
from ultralytics import YOLO
import tempfile
import os

app = Flask(__name__)

# Load trained model
model = YOLO("runs/detect/train2/weights/best.pt")


@app.route("/")
def home():
    return "Plant Disease API is running!"


@app.route("/detect", methods=["POST"])
def detect():

    if "file" not in request.files:
        return jsonify({"error": "No file uploaded"}), 400

    file = request.files["file"]

    # Save uploaded image temporarily
    with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as temp:
        file.save(temp.name)
        temp_path = temp.name

    # Run detection
    results = model(temp_path)

    # Remove temp file
    os.remove(temp_path)

    # If no detection
    if len(results[0].boxes) == 0:
        return jsonify({
            "disease": "No disease detected",
            "confidence": 0.0
        })

    # Get top detection
    boxes = results[0].boxes
    conf = float(boxes.conf[0])
    cls_id = int(boxes.cls[0])

    # Convert class index to real name
    disease_name = model.names[cls_id]

    return jsonify({
        "disease": disease_name,
        "confidence": conf
    })


if __name__ == "__main__":
    app.run(port=5000, debug=True)