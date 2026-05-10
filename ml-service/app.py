from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import joblib
import numpy as np
import os

app = FastAPI(title="MindCare ML API", version="1.0")

# Request Model
class PredictionRequest(BaseModel):
    age: int
    mmse: int
    isSmoker: int # 0 or 1
    drinksAlcohol: int
    physicalActivity: int
    familyHistory: int
    hypertension: int
    type2Diabetes: int
    hypercholesterolemia: int
    sleepDisorders: int

# Load models at startup
models = {}

@app.on_event("startup")
def load_models():
    try:
        models['scaler'] = joblib.load('models/scaler.joblib')
        models['gb_model'] = joblib.load('models/gb_model.joblib')
        models['pca'] = joblib.load('models/pca_model.joblib')
        models['kmeans'] = joblib.load('models/kmeans_model.joblib')
        models['cluster_stats'] = joblib.load('models/cluster_stats.joblib')
        print("Models loaded successfully.")
    except Exception as e:
        print(f"Warning: Models not found or failed to load: {e}. Please run train.py first.")

@app.post("/predict")
def predict_alzheimer(req: PredictionRequest):
    if 'gb_model' not in models:
        raise HTTPException(status_code=500, detail="Models are not loaded. Run training script.")
        
    try:
        # Prepare input data in the exact same order as training
        input_data = np.array([[
            req.age, 
            req.mmse, 
            req.isSmoker, 
            req.drinksAlcohol, 
            req.physicalActivity,
            req.familyHistory,
            req.hypertension,
            req.type2Diabetes,
            req.hypercholesterolemia,
            req.sleepDisorders
        ]])
        
        # Scale data
        scaled_data = models['scaler'].transform(input_data)
        
        # Classification (Gradient Boosting)
        is_sick_pred = models['gb_model'].predict(scaled_data)[0]
        is_sick = bool(is_sick_pred)
        
        # Clustering (PCA + K-Means)
        pca_data = models['pca'].transform(scaled_data)
        cluster = models['kmeans'].predict(pca_data)[0]
        
        # Get disease percentage based on cluster
        disease_percentage = models['cluster_stats'].get(cluster, 0.0) * 100
        
        # Optional: refine percentage based on probability from classification
        prob = models['gb_model'].predict_proba(scaled_data)[0][1] * 100
        
        # Final blended percentage (you can adjust this logic)
        final_percentage = round((disease_percentage + prob) / 2, 2)
        
        return {
            "isSick": is_sick,
            "diseasePercentage": final_percentage,
            "cluster": int(cluster)
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/health")
def health_check():
    return {"status": "ok"}
