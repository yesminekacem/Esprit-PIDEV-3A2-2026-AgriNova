from flask import Flask, request, jsonify
import pickle
import pandas as pd

with open("inventory_model.pkl", "rb") as f:
    data = pickle.load(f)

model = data['model']
le_item_type = data['le_item_type']
le_condition = data['le_condition']
le_rental_status = data['le_rental_status']

app = Flask(__name__)

@app.route("/predict", methods=["POST"])
def predict():
    req = request.json
    df = pd.DataFrame([req])

    df['item_type_encoded'] = le_item_type.transform(df['item_type'])
    df['condition_encoded'] = le_condition.transform(df['condition_status'])
    df['rental_status_encoded'] = le_rental_status.transform(df['rental_status'])

    X = df[['item_type_encoded',
            'total_usage_hours',
            'condition_encoded',
            'is_rentable',
            'rental_status_encoded',
            'unit_price',
            'rental_price_per_day']].fillna(0)

    pred = model.predict(X)

    usage = float(req.get('total_usage_hours', 0))
    if usage > 400:
        maintenance_advice = "High usage - schedule maintenance soon."
    elif usage > 200:
        maintenance_advice = "Moderate usage - check in 30 days."
    else:
        maintenance_advice = "Low usage - equipment in good standing."

    rate = float(req.get('rental_price_per_day', 0))
    if rate > 40:
        rental_advice = "Premium item - ensure high visibility in listings."
    elif rate > 15:
        rental_advice = "Good rental candidate - competitive daily rate."
    else:
        rental_advice = "Low rental value - consider bundling with other items."

    return jsonify({
        'predicted_quantity': int(pred[0]),
        'maintenance_advice': maintenance_advice,
        'rental_advice': rental_advice,
        'confidence': 'high' if usage < 500 else 'medium'
    })

@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})

if __name__ == "__main__":
    app.run(debug=True)