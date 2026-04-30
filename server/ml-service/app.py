"""
app.py — Flask API for the Alzheimer recommendation system.

Endpoints:
    GET  /health              → service health check
    GET  /clusters            → all cluster profiles
    POST /recommend           → patient data → recommendations
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
from model.recommender import recommend, get_cluster_profiles

app = Flask(__name__)
CORS(app)


# ── Health check ───────────────────────────────────────────────────────────────
@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "service": "alzheimer-recommendation-service"})


# ── Cluster profiles ───────────────────────────────────────────────────────────
@app.route("/clusters", methods=["GET"])
def clusters():
    try:
        profiles = get_cluster_profiles()
        return jsonify({"clusters": profiles})
    except FileNotFoundError:
        return jsonify({"error": "Model not trained yet. Run: python model/train.py"}), 503


# ── Main recommendation endpoint ───────────────────────────────────────────────
@app.route("/recommend", methods=["POST"])
def get_recommendations():
    data = request.get_json(silent=True)
    if not data:
        return jsonify({"error": "Request body must be JSON with patient features"}), 400

    required_fields = [
        "Age", "Gender", "Ethnicity", "EducationLevel",
        "BMI", "Smoking", "AlcoholConsumption", "PhysicalActivity",
        "DietQuality", "SleepQuality",
        "FamilyHistoryAlzheimers", "CardiovascularDisease", "Diabetes",
        "Depression", "HeadInjury", "Hypertension",
        "SystolicBP", "DiastolicBP", "CholesterolTotal", "CholesterolLDL",
        "CholesterolHDL", "CholesterolTriglycerides",
        "MMSE", "FunctionalAssessment", "MemoryComplaints",
        "BehavioralProblems", "ADL",
        "Confusion", "Disorientation", "PersonalityChanges",
        "DifficultyCompletingTasks", "Forgetfulness",
    ]

    missing = [f for f in required_fields if f not in data]
    if missing:
        return jsonify({"error": f"Missing fields: {missing}"}), 400

    try:
        result = recommend(data)
        return jsonify(result)
    except FileNotFoundError:
        return jsonify({"error": "Model not trained yet. Run: python model/train.py"}), 503
    except Exception as e:
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
