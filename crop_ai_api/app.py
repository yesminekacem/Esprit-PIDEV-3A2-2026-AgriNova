from flask import Flask, request, jsonify
import requests
import json

app = Flask(__name__)

@app.route("/generate-tasks", methods=["POST"])
def generate_tasks():
    data = request.json

    prompt = f"""
    Generate 5 farming tasks in JSON array format.

    Crop: {data['crop_name']}
    Growth Stage: {data['growth_stage']}
    Temperature: {data['temperature']}°C
    Soil Moisture: {data['soil_moisture']}
    Location: {data['location']}

    Return ONLY this JSON format:

    [
      {{
        "title": "",
        "description": "",
        "priority": "Low/Medium/High"
      }}
    ]
    """

    response = requests.post(
        "http://localhost:11434/api/generate",
        json={
            "model": "llama3",
            "prompt": prompt,
            "stream": False
        }
    )

    result = response.json()["response"]

    if result.startswith("```"):
        result = result.replace("```json", "").replace("```", "").strip()

    start = result.find("[")
    end = result.rfind("]") + 1
    clean_json = result[start:end]

    tasks = json.loads(clean_json)

    return jsonify(tasks)


if __name__ == "__main__":
    app.run(debug=True)