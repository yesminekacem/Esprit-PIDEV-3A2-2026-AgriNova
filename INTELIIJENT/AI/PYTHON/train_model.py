# train_model.py
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.preprocessing import LabelEncoder
import pickle

# Load CSV
df = pd.read_csv("inventory.csv")

# Encode categorical columns
le_item_type = LabelEncoder()
df['item_type_encoded'] = le_item_type.fit_transform(df['item_type'])

le_condition = LabelEncoder()
df['condition_encoded'] = le_condition.fit_transform(df['condition_status'])

le_rental_status = LabelEncoder()
df['rental_status_encoded'] = le_rental_status.fit_transform(df['rental_status'])

# Features and target
X = df[['item_type_encoded', 'total_usage_hours', 'condition_encoded', 'is_rentable', 'rental_status_encoded', 'unit_price', 'rental_price_per_day']].fillna(0)
y = df['quantity']

# Train model
model = RandomForestRegressor(n_estimators=100, random_state=42)
model.fit(X, y)

# Save model and encoders
with open("inventory_model.pkl", "wb") as f:
    pickle.dump({
        'model': model,
        'le_item_type': le_item_type,
        'le_condition': le_condition,
        'le_rental_status': le_rental_status
    }, f)

print("Model trained and saved as inventory_model.pkl")