"""
train.py — Advanced ML training pipeline for Alzheimer recommendation system.
Models: Random Forest + Gradient Boosting (ensemble)
Evaluation: 5-fold cross-validation, ROC-AUC, classification report

Run once:
    python model/train.py
"""

import os
import json
import joblib
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.model_selection import StratifiedKFold, cross_val_score
from sklearn.metrics import (
    roc_auc_score, accuracy_score, classification_report,
    confusion_matrix, roc_curve,
)

# ── Paths ──────────────────────────────────────────────────────────────────────
BASE_DIR  = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_PATH = os.path.join(BASE_DIR, "data", "alzheimers_disease_data.csv")
SAVED_DIR = os.path.join(BASE_DIR, "saved_models")
os.makedirs(SAVED_DIR, exist_ok=True)

# ── Features ───────────────────────────────────────────────────────────────────
FEATURE_COLS = [
    "Age", "Gender", "Ethnicity", "EducationLevel",
    "BMI", "Smoking", "AlcoholConsumption", "PhysicalActivity", "DietQuality", "SleepQuality",
    "FamilyHistoryAlzheimers", "CardiovascularDisease", "Diabetes",
    "Depression", "HeadInjury", "Hypertension",
    "SystolicBP", "DiastolicBP", "CholesterolTotal", "CholesterolLDL",
    "CholesterolHDL", "CholesterolTriglycerides",
    "MMSE", "FunctionalAssessment", "MemoryComplaints",
    "BehavioralProblems", "ADL",
    "Confusion", "Disorientation", "PersonalityChanges",
    "DifficultyCompletingTasks", "Forgetfulness",
]

# ── 1. Load data ───────────────────────────────────────────────────────────────
def load_data():
    print(f"Loading: {DATA_PATH}")
    df = pd.read_csv(DATA_PATH)
    print(f"  Shape: {df.shape}  |  Missing: {df.isnull().sum().sum()}")
    print(f"  Diagnosis: {df['Diagnosis'].value_counts().to_dict()}")
    return df

# ── 2. EDA ─────────────────────────────────────────────────────────────────────
def run_eda(df):
    print("\n-- EDA --")
    # Diagnosis distribution
    fig, axes = plt.subplots(1, 3, figsize=(15, 4))

    df['Diagnosis'].value_counts().plot(kind='bar', ax=axes[0],
        color=['#4CAF50','#F44336'], edgecolor='white')
    axes[0].set_title('Diagnosis Distribution', fontweight='bold')
    axes[0].set_xticklabels(['No Alzheimer\'s','Alzheimer\'s'], rotation=0)

    # MMSE by diagnosis
    df[df['Diagnosis']==0]['MMSE'].hist(ax=axes[1], alpha=0.6, color='#4CAF50',
        label="No Alzheimer's", bins=20)
    df[df['Diagnosis']==1]['MMSE'].hist(ax=axes[1], alpha=0.6, color='#F44336',
        label="Alzheimer's", bins=20)
    axes[1].set_title('MMSE Distribution by Diagnosis', fontweight='bold')
    axes[1].legend()

    # Correlation heatmap (top features)
    top_feats = ['MMSE','ADL','FunctionalAssessment','PhysicalActivity',
                 'DietQuality','SleepQuality','SystolicBP','BMI','Diagnosis']
    corr = df[top_feats].corr()
    sns.heatmap(corr, ax=axes[2], cmap='coolwarm', center=0, annot=True,
                fmt='.2f', linewidths=0.5, annot_kws={'size': 7})
    axes[2].set_title('Key Feature Correlations', fontweight='bold')

    plt.tight_layout()
    plt.savefig(os.path.join(SAVED_DIR, 'eda.png'), dpi=100)
    plt.close()
    print("  EDA plot saved.")

# ── 3. Preprocess ──────────────────────────────────────────────────────────────
def preprocess(df):
    X = df[FEATURE_COLS].values
    y = df['Diagnosis'].values
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)

    # Healthy baseline: mean/std of non-diagnosed patients
    healthy_df    = df[df['Diagnosis'] == 0][FEATURE_COLS]
    healthy_mean  = healthy_df.mean().to_dict()
    healthy_std   = healthy_df.std().to_dict()

    print(f"  Healthy baseline computed from {len(healthy_df)} non-diagnosed patients.")
    return X_scaled, y, scaler, healthy_mean, healthy_std

# ── 4. Train & evaluate models ─────────────────────────────────────────────────
def train_models(X_scaled, y):
    print("\n-- Training models --")
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)

    # Random Forest
    rf = RandomForestClassifier(
        n_estimators=300, max_depth=12, min_samples_leaf=10,
        class_weight='balanced', random_state=42, n_jobs=-1
    )
    rf_auc = cross_val_score(rf, X_scaled, y, cv=cv, scoring='roc_auc')
    rf_acc = cross_val_score(rf, X_scaled, y, cv=cv, scoring='accuracy')
    print(f"  Random Forest   AUC: {rf_auc.mean():.4f} +/- {rf_auc.std():.4f}  "
          f"Acc: {rf_acc.mean():.4f}")
    rf.fit(X_scaled, y)

    # Gradient Boosting
    gb = GradientBoostingClassifier(
        n_estimators=200, max_depth=4, learning_rate=0.05,
        subsample=0.8, random_state=42
    )
    gb_auc = cross_val_score(gb, X_scaled, y, cv=cv, scoring='roc_auc')
    gb_acc = cross_val_score(gb, X_scaled, y, cv=cv, scoring='accuracy')
    print(f"  Gradient Boost  AUC: {gb_auc.mean():.4f} +/- {gb_auc.std():.4f}  "
          f"Acc: {gb_acc.mean():.4f}")
    gb.fit(X_scaled, y)

    # Best model
    best = 'rf' if rf_auc.mean() >= gb_auc.mean() else 'gb'
    print(f"  Best model: {best.upper()}")

    # Full train metrics
    y_prob_rf = rf.predict_proba(X_scaled)[:, 1]
    y_prob_gb = gb.predict_proba(X_scaled)[:, 1]
    y_prob_ensemble = (y_prob_rf + y_prob_gb) / 2

    print(f"\n  Train metrics (ensemble):")
    print(f"    AUC:      {roc_auc_score(y, y_prob_ensemble):.4f}")
    print(f"    Accuracy: {accuracy_score(y, (y_prob_ensemble >= 0.5).astype(int)):.4f}")
    print(classification_report(y, (y_prob_ensemble >= 0.5).astype(int),
                                 target_names=['No Alzheimer\'s', 'Alzheimer\'s']))

    metrics = {
        'rf_cv_auc_mean':  round(float(rf_auc.mean()), 4),
        'rf_cv_auc_std':   round(float(rf_auc.std()),  4),
        'rf_cv_acc_mean':  round(float(rf_acc.mean()), 4),
        'gb_cv_auc_mean':  round(float(gb_auc.mean()), 4),
        'gb_cv_auc_std':   round(float(gb_auc.std()),  4),
        'gb_cv_acc_mean':  round(float(gb_acc.mean()), 4),
        'ensemble_train_auc': round(float(roc_auc_score(y, y_prob_ensemble)), 4),
        'best_model': best,
    }
    return rf, gb, metrics

# ── 5. Feature importance (ensemble average) ───────────────────────────────────
def compute_feature_importance(rf, gb):
    print("\n-- Feature importance --")
    rf_imp = pd.Series(rf.feature_importances_, index=FEATURE_COLS)
    gb_imp = pd.Series(gb.feature_importances_, index=FEATURE_COLS)
    avg_imp = ((rf_imp + gb_imp) / 2).sort_values(ascending=False)

    print("  Top 10 features:")
    for feat, imp in avg_imp.head(10).items():
        print(f"    {feat:<28} {imp:.4f}")

    # Feature importance plot
    plt.figure(figsize=(10, 6))
    avg_imp.head(15).sort_values().plot(kind='barh', color='steelblue')
    plt.title('Top 15 Features — Ensemble Importance (RF + GB)', fontweight='bold')
    plt.xlabel('Importance Score')
    plt.tight_layout()
    plt.savefig(os.path.join(SAVED_DIR, 'feature_importance.png'), dpi=100)
    plt.close()
    print("  Feature importance plot saved.")

    return avg_imp.to_dict()

# ── 6. ROC curve ───────────────────────────────────────────────────────────────
def plot_roc(rf, gb, X_scaled, y):
    y_prob_rf  = rf.predict_proba(X_scaled)[:, 1]
    y_prob_gb  = gb.predict_proba(X_scaled)[:, 1]
    y_prob_ens = (y_prob_rf + y_prob_gb) / 2

    fig, ax = plt.subplots(figsize=(7, 6))
    for probs, label, color in [
        (y_prob_rf,  f"Random Forest (AUC={roc_auc_score(y, y_prob_rf):.3f})",  'blue'),
        (y_prob_gb,  f"Gradient Boost (AUC={roc_auc_score(y, y_prob_gb):.3f})", 'orange'),
        (y_prob_ens, f"Ensemble       (AUC={roc_auc_score(y, y_prob_ens):.3f})", 'green'),
    ]:
        fpr, tpr, _ = roc_curve(y, probs)
        ax.plot(fpr, tpr, label=label, linewidth=2)

    ax.plot([0,1],[0,1],'k--', linewidth=1)
    ax.set_xlabel('False Positive Rate'); ax.set_ylabel('True Positive Rate')
    ax.set_title('ROC Curve — Model Comparison', fontweight='bold')
    ax.legend(); ax.grid(alpha=0.3)
    plt.tight_layout()
    plt.savefig(os.path.join(SAVED_DIR, 'roc_curve.png'), dpi=100)
    plt.close()
    print("  ROC curve saved.")

# ── 7. Save all artifacts ──────────────────────────────────────────────────────
def save_artifacts(rf, gb, scaler, feature_importance, healthy_mean, healthy_std, metrics):
    joblib.dump(rf, os.path.join(SAVED_DIR, 'rf_model.pkl'))
    joblib.dump(gb, os.path.join(SAVED_DIR, 'gb_model.pkl'))
    joblib.dump(scaler, os.path.join(SAVED_DIR, 'scaler.pkl'))

    artifacts = {
        'feature_cols':       FEATURE_COLS,
        'feature_importance': feature_importance,
        'healthy_mean':       healthy_mean,
        'healthy_std':        healthy_std,
        'metrics':            metrics,
    }
    with open(os.path.join(SAVED_DIR, 'model_artifacts.json'), 'w') as f:
        json.dump(artifacts, f, indent=2)

    print(f"\n  Saved: rf_model.pkl, gb_model.pkl, scaler.pkl, model_artifacts.json")

# ── Main ───────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    df                                              = load_data()
    run_eda(df)
    X_scaled, y, scaler, healthy_mean, healthy_std  = preprocess(df)
    rf, gb, metrics                                 = train_models(X_scaled, y)
    feature_importance                              = compute_feature_importance(rf, gb)
    plot_roc(rf, gb, X_scaled, y)
    save_artifacts(rf, gb, scaler, feature_importance, healthy_mean, healthy_std, metrics)
    print("\nTraining complete.")
