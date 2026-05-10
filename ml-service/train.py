import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.cluster import KMeans
from sklearn.decomposition import PCA
from sklearn.preprocessing import StandardScaler
import joblib
import os

def load_and_preprocess_data():
    print("Loading actual dataset...")
    # Load the real dataset provided by the user
    file_path = r'C:\Users\khaou\OneDrive\Bureau\ESPRIT Cours du Jour\alzheimers_disease_data.csv.xlsx'
    df = pd.read_excel(file_path)
    
    # Feature Engineering to map to PatientProfile booleans
    # 1. Age and MMSE are kept continuous
    # 2. Smoking, FamilyHistoryAlzheimers, Diabetes, Hypertension are already 0/1
    # 3. AlcoholConsumption: binarize (e.g. > median or > 10)
    df['DrinksAlcohol'] = (df['AlcoholConsumption'] > df['AlcoholConsumption'].median()).astype(int)
    # 4. PhysicalActivity: binarize (e.g. > median)
    df['PhysicalActivity_Bool'] = (df['PhysicalActivity'] > df['PhysicalActivity'].median()).astype(int)
    # 5. SleepQuality: binarize (lower quality = sleep disorder)
    df['SleepDisorders'] = (df['SleepQuality'] < df['SleepQuality'].median()).astype(int)
    # 6. CholesterolTotal: binarize (>= 240 is considered high)
    df['Hypercholesterolemia'] = (df['CholesterolTotal'] >= 240).astype(int)
    
    # Rename some columns to match our expected features
    df = df.rename(columns={
        'Smoking': 'IsSmoker',
        'PhysicalActivity_Bool': 'PhysicalActivity',
        'FamilyHistoryAlzheimers': 'FamilyHistory',
        'Diabetes': 'Type2Diabetes',
        'Diagnosis': 'IsSick'
    })
    
    # Select only the features we need
    feature_cols = [
        'Age', 'MMSE', 'IsSmoker', 'DrinksAlcohol', 'PhysicalActivity', 
        'FamilyHistory', 'Hypertension', 'Type2Diabetes', 
        'Hypercholesterolemia', 'SleepDisorders'
    ]
    
    # Drop rows with NaNs in our target columns just in case
    df = df.dropna(subset=feature_cols + ['IsSick'])
    
    return df, feature_cols

def train_models():
    print("Preparing training data based on real dataset fields...")
    df, feature_cols = load_and_preprocess_data()
    
    # Features
    X = df[feature_cols]
    y_class = df['IsSick']
    
    print("Training Gradient Boosting model (Classification)...")
    # Scaling
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    
    gb_model = GradientBoostingClassifier(n_estimators=100, random_state=42)
    gb_model.fit(X_scaled, y_class)
    
    print("Applying PCA before K-Means clustering...")
    pca = PCA(n_components=3, random_state=42) # reduced to 3 components
    X_pca = pca.fit_transform(X_scaled)
    
    print("Training K-Means models on PCA data (Clustering/Staging)...")
    kmeans = KMeans(n_clusters=4, random_state=42, n_init=10)
    df['Cluster'] = kmeans.fit_predict(X_pca)
    
    # Calculate disease percentage per cluster based on sick rate
    cluster_stats = df.groupby('Cluster')['IsSick'].mean().to_dict()
    print("Cluster Disease Percentages:", cluster_stats)
    
    # Save models
    os.makedirs('models', exist_ok=True)
    joblib.dump(scaler, 'models/scaler.joblib')
    joblib.dump(gb_model, 'models/gb_model.joblib')
    joblib.dump(pca, 'models/pca_model.joblib')
    joblib.dump(kmeans, 'models/kmeans_model.joblib')
    joblib.dump(cluster_stats, 'models/cluster_stats.joblib')
    
    print("Models saved successfully in 'models/' directory.")

if __name__ == "__main__":
    train_models()
