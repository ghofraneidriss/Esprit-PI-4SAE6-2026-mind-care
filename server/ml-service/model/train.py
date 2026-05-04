"""
train.py — Advanced ML training pipeline for Alzheimer recommendation system.

Models    : Random Forest (baseline) + Random Forest (Optuna-tuned) + Gradient Boosting
Selection : Winner by 5-fold CV ROC-AUC
SHAP      : TreeExplainer on the winning model → global feature importance
Artifacts : rf_model.pkl | gb_model.pkl | scaler.pkl | shap_explainer.pkl
            model_artifacts.json | eda.png | feature_importance.png
            roc_curve.png | optuna_history.png | shap_importance.png | shap_summary.png

Run once:
    python model/train.py
"""

import os
import json
import warnings
import joblib
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.model_selection import StratifiedKFold, cross_val_score
from sklearn.metrics import (
    roc_auc_score, accuracy_score, classification_report,
    confusion_matrix, roc_curve,
)

warnings.filterwarnings('ignore')

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
    print(f"  Diagnosis distribution: {df['Diagnosis'].value_counts().to_dict()}")
    return df

# ── 2. EDA ─────────────────────────────────────────────────────────────────────
def run_eda(df):
    print("\n-- EDA --")
    fig, axes = plt.subplots(1, 3, figsize=(15, 4))

    df['Diagnosis'].value_counts().plot(
        kind='bar', ax=axes[0],
        color=['#4CAF50', '#F44336'], edgecolor='white'
    )
    axes[0].set_title('Diagnosis Distribution', fontweight='bold')
    axes[0].set_xticklabels(["No Alzheimer's", "Alzheimer's"], rotation=0)

    df[df['Diagnosis'] == 0]['MMSE'].hist(
        ax=axes[1], alpha=0.6, color='#4CAF50', label="No Alzheimer's", bins=20
    )
    df[df['Diagnosis'] == 1]['MMSE'].hist(
        ax=axes[1], alpha=0.6, color='#F44336', label="Alzheimer's", bins=20
    )
    axes[1].set_title('MMSE Distribution by Diagnosis', fontweight='bold')
    axes[1].legend()

    top_feats = [
        'MMSE', 'ADL', 'FunctionalAssessment', 'PhysicalActivity',
        'DietQuality', 'SleepQuality', 'SystolicBP', 'BMI', 'Diagnosis',
    ]
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

    healthy_df   = df[df['Diagnosis'] == 0][FEATURE_COLS]
    healthy_mean = healthy_df.mean().to_dict()
    healthy_std  = healthy_df.std().to_dict()

    print(f"  Healthy baseline computed from {len(healthy_df)} non-diagnosed patients.")
    return X_scaled, y, scaler, healthy_mean, healthy_std

# ── 4. Baseline models ─────────────────────────────────────────────────────────
def train_baseline_models(X_scaled, y):
    print("\n-- Baseline models --")
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)

    rf_base = RandomForestClassifier(
        n_estimators=300, max_depth=12, min_samples_leaf=10,
        class_weight='balanced', random_state=42, n_jobs=-1
    )
    rf_base_auc = cross_val_score(rf_base, X_scaled, y, cv=cv, scoring='roc_auc')
    rf_base_acc = cross_val_score(rf_base, X_scaled, y, cv=cv, scoring='accuracy')
    print(f"  RF Baseline    AUC: {rf_base_auc.mean():.4f} ± {rf_base_auc.std():.4f}  "
          f"Acc: {rf_base_acc.mean():.4f}")
    rf_base.fit(X_scaled, y)

    gb = GradientBoostingClassifier(
        n_estimators=200, max_depth=4, learning_rate=0.05,
        subsample=0.8, random_state=42
    )
    gb_auc = cross_val_score(gb, X_scaled, y, cv=cv, scoring='roc_auc')
    gb_acc = cross_val_score(gb, X_scaled, y, cv=cv, scoring='accuracy')
    print(f"  Gradient Boost AUC: {gb_auc.mean():.4f} ± {gb_auc.std():.4f}  "
          f"Acc: {gb_acc.mean():.4f}")
    gb.fit(X_scaled, y)

    return rf_base, gb, rf_base_auc, rf_base_acc, gb_auc, gb_acc

# ── 5. Optuna hyperparameter optimisation ──────────────────────────────────────
def optimize_with_optuna(X_scaled, y, n_trials=50):
    """
    TPE sampler, 50 trials, 5-fold CV ROC-AUC objective.
    Search space: n_estimators, max_depth, min_samples_leaf, min_samples_split,
                  max_features.
    Returns the fitted optimised RF, best params, CV AUC array, CV Acc array.
    """
    import optuna
    optuna.logging.set_verbosity(optuna.logging.WARNING)

    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)

    def objective(trial):
        params = {
            'n_estimators':      trial.suggest_int('n_estimators', 100, 500),
            'max_depth':         trial.suggest_int('max_depth', 4, 20),
            'min_samples_leaf':  trial.suggest_int('min_samples_leaf', 1, 20),
            'min_samples_split': trial.suggest_int('min_samples_split', 2, 20),
            'max_features':      trial.suggest_categorical('max_features', ['sqrt', 'log2']),
            'class_weight':      'balanced',
            'random_state':      42,
            'n_jobs':            -1,
        }
        model = RandomForestClassifier(**params)
        scores = cross_val_score(model, X_scaled, y, cv=cv, scoring='roc_auc')
        return scores.mean()

    print(f"\n-- Optuna tuning (n_trials={n_trials}) --")
    sampler = optuna.samplers.TPESampler(seed=42)
    study = optuna.create_study(direction='maximize', sampler=sampler)
    study.optimize(objective, n_trials=n_trials, show_progress_bar=False)

    best_params = study.best_params
    best_params.update({'class_weight': 'balanced', 'random_state': 42, 'n_jobs': -1})
    print(f"  Best params: {best_params}")

    rf_opt = RandomForestClassifier(**best_params)
    opt_auc = cross_val_score(rf_opt, X_scaled, y, cv=cv, scoring='roc_auc')
    opt_acc = cross_val_score(rf_opt, X_scaled, y, cv=cv, scoring='accuracy')
    print(f"  RF Optimised   AUC: {opt_auc.mean():.4f} ± {opt_auc.std():.4f}  "
          f"Acc: {opt_acc.mean():.4f}")
    rf_opt.fit(X_scaled, y)

    # Optuna history plot
    trial_values = [t.value for t in study.trials]
    best_so_far  = np.maximum.accumulate(trial_values)
    fig, ax = plt.subplots(figsize=(9, 5))
    ax.scatter(range(len(trial_values)), trial_values, alpha=0.5, s=20,
               color='steelblue', label='Trial AUC')
    ax.plot(range(len(best_so_far)), best_so_far, color='#e74c3c',
            linewidth=2, label='Best so far')
    ax.set_xlabel('Trial')
    ax.set_ylabel('CV ROC-AUC')
    ax.set_title('Optuna Optimisation History — Random Forest', fontweight='bold')
    ax.legend()
    ax.grid(alpha=0.3)
    plt.tight_layout()
    plt.savefig(os.path.join(SAVED_DIR, 'optuna_history.png'), dpi=100)
    plt.close()
    print("  Optuna history plot saved.")

    return rf_opt, best_params, opt_auc, opt_acc

# ── 6. Select winning model ────────────────────────────────────────────────────
def select_winner(rf_base, gb, rf_opt,
                  rf_base_auc, gb_auc, rf_opt_auc):
    candidates = {
        'rf_base': (rf_base, rf_base_auc.mean()),
        'gb':      (gb,      gb_auc.mean()),
        'rf':      (rf_opt,  rf_opt_auc.mean()),
    }
    best_key = max(candidates, key=lambda k: candidates[k][1])
    best_model, best_auc = candidates[best_key]
    print(f"\n  Winner: {best_key.upper()}  (CV AUC = {best_auc:.4f})")
    return best_model, best_key

# ── 7. SHAP explainability ─────────────────────────────────────────────────────
def compute_shap(model, X_scaled):
    """
    TreeExplainer on the winning model.
    Uses up to 500 samples for efficiency.
    Saves: shap_explainer.pkl, shap_importance.png, shap_summary.png.
    Returns feature importance dict (mean |SHAP| per feature).
    """
    import shap

    print("\n-- SHAP explainability --")
    n_samples = min(500, X_scaled.shape[0])
    rng = np.random.default_rng(42)
    idx = rng.choice(X_scaled.shape[0], size=n_samples, replace=False)
    X_sample = X_scaled[idx]

    explainer   = shap.TreeExplainer(model)
    shap_values = explainer.shap_values(X_sample)

    # shap_values shape can be:
    #   list of 2 arrays (n_samples, n_features)  — old shap
    #   ndarray (n_samples, n_features, 2)         — new shap ≥0.46
    #   ndarray (n_samples, n_features)            — regression / single output
    if isinstance(shap_values, list):
        sv = shap_values[1]
    elif isinstance(shap_values, np.ndarray) and shap_values.ndim == 3:
        sv = shap_values[:, :, 1]
    else:
        sv = shap_values

    mean_abs_shap = np.abs(sv).mean(axis=0)
    shap_imp = pd.Series(mean_abs_shap, index=FEATURE_COLS).sort_values(ascending=False)

    print("  Top 10 SHAP features:")
    for feat, val in shap_imp.head(10).items():
        print(f"    {feat:<30} {val:.5f}")

    # Bar chart — mean |SHAP|
    plt.figure(figsize=(10, 6))
    shap_imp.head(15).sort_values().plot(kind='barh', color='#e74c3c')
    plt.title('SHAP Feature Importance — Mean |SHAP Value| (Top 15)', fontweight='bold')
    plt.xlabel('Mean |SHAP Value|')
    plt.tight_layout()
    plt.savefig(os.path.join(SAVED_DIR, 'shap_importance.png'), dpi=100)
    plt.close()

    # Beeswarm / summary plot
    plt.figure(figsize=(10, 7))
    shap.summary_plot(sv, X_sample, feature_names=FEATURE_COLS,
                      max_display=15, show=False)
    plt.title('SHAP Summary Plot', fontweight='bold')
    plt.tight_layout()
    plt.savefig(os.path.join(SAVED_DIR, 'shap_summary.png'), dpi=100)
    plt.close()

    joblib.dump(explainer, os.path.join(SAVED_DIR, 'shap_explainer.pkl'))
    print("  SHAP plots and explainer saved.")

    return shap_imp.to_dict()

# ── 8. Feature importance plot (winner model) ──────────────────────────────────
def compute_feature_importance(model):
    print("\n-- Feature importance (winner model) --")
    imp = pd.Series(model.feature_importances_, index=FEATURE_COLS).sort_values(ascending=False)

    print("  Top 10 features:")
    for feat, val in imp.head(10).items():
        print(f"    {feat:<28} {val:.4f}")

    plt.figure(figsize=(10, 6))
    imp.head(15).sort_values().plot(kind='barh', color='steelblue')
    plt.title('Top 15 Features — Winner Model Importance', fontweight='bold')
    plt.xlabel('Importance Score')
    plt.tight_layout()
    plt.savefig(os.path.join(SAVED_DIR, 'feature_importance.png'), dpi=100)
    plt.close()
    print("  Feature importance plot saved.")

    return imp.to_dict()

# ── 9. ROC curve comparison ────────────────────────────────────────────────────
def plot_roc(rf_base, gb, rf_opt, X_scaled, y):
    curves = [
        (rf_base, 'RF Baseline',    'royalblue'),
        (gb,      'Gradient Boost', 'darkorange'),
        (rf_opt,  'RF Optimised',   'seagreen'),
    ]
    fig, ax = plt.subplots(figsize=(7, 6))
    for model, label, color in curves:
        probs = model.predict_proba(X_scaled)[:, 1]
        fpr, tpr, _ = roc_curve(y, probs)
        auc = roc_auc_score(y, probs)
        ax.plot(fpr, tpr, label=f"{label}  (AUC={auc:.3f})",
                linewidth=2, color=color)
    ax.plot([0, 1], [0, 1], 'k--', linewidth=1)
    ax.set_xlabel('False Positive Rate')
    ax.set_ylabel('True Positive Rate')
    ax.set_title('ROC Curve — Model Comparison', fontweight='bold')
    ax.legend()
    ax.grid(alpha=0.3)
    plt.tight_layout()
    plt.savefig(os.path.join(SAVED_DIR, 'roc_curve.png'), dpi=100)
    plt.close()
    print("  ROC curve saved.")

# ── 10. Save all artifacts ─────────────────────────────────────────────────────
def save_artifacts(rf_opt, gb, scaler,
                   feature_importance, shap_importance,
                   healthy_mean, healthy_std, metrics, best_params):
    joblib.dump(rf_opt, os.path.join(SAVED_DIR, 'rf_model.pkl'))
    joblib.dump(gb,     os.path.join(SAVED_DIR, 'gb_model.pkl'))
    joblib.dump(scaler, os.path.join(SAVED_DIR, 'scaler.pkl'))

    artifacts = {
        'feature_cols':       FEATURE_COLS,
        'feature_importance': feature_importance,
        'shap_importance':    shap_importance,
        'healthy_mean':       healthy_mean,
        'healthy_std':        healthy_std,
        'best_params':        best_params,
        'metrics':            metrics,
    }
    with open(os.path.join(SAVED_DIR, 'model_artifacts.json'), 'w') as f:
        json.dump(artifacts, f, indent=2)

    print(f"\n  Saved: rf_model.pkl, gb_model.pkl, scaler.pkl, "
          f"shap_explainer.pkl, model_artifacts.json")

# ── Main ───────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    df = load_data()
    run_eda(df)
    X_scaled, y, scaler, healthy_mean, healthy_std = preprocess(df)

    # Baseline models
    rf_base, gb, rf_base_auc, rf_base_acc, gb_auc, gb_acc = train_baseline_models(X_scaled, y)

    # Optuna-optimised RF
    rf_opt, best_params, rf_opt_auc, rf_opt_acc = optimize_with_optuna(X_scaled, y, n_trials=50)

    # Winner selection
    winner, winner_key = select_winner(rf_base, gb, rf_opt, rf_base_auc, gb_auc, rf_opt_auc)

    # Use actual winner key for model file ('rf' or 'gb')
    best_model_key = 'gb' if winner_key == 'gb' else 'rf'

    # SHAP on winner
    shap_importance = compute_shap(winner, X_scaled)

    # Classic feature importance
    feature_importance = compute_feature_importance(winner)

    # ROC comparison
    plot_roc(rf_base, gb, rf_opt, X_scaled, y)

    # Full-set evaluation of winner
    y_prob = winner.predict_proba(X_scaled)[:, 1]
    y_pred = (y_prob >= 0.5).astype(int)
    print(f"\n  Full-train evaluation ({winner_key.upper()}):")
    print(f"    AUC:      {roc_auc_score(y, y_prob):.4f}")
    print(f"    Accuracy: {accuracy_score(y, y_pred):.4f}")
    print(classification_report(y, y_pred,
                                 target_names=["No Alzheimer's", "Alzheimer's"]))

    metrics = {
        'best_model':             best_model_key,
        'rf_baseline_auc_mean':   round(float(rf_base_auc.mean()), 4),
        'rf_baseline_auc_std':    round(float(rf_base_auc.std()),  4),
        'rf_baseline_acc_mean':   round(float(rf_base_acc.mean()), 4),
        'rf_optimised_auc_mean':  round(float(rf_opt_auc.mean()),  4),
        'rf_optimised_auc_std':   round(float(rf_opt_auc.std()),   4),
        'rf_optimised_acc_mean':  round(float(rf_opt_acc.mean()),  4),
        'gb_cv_auc_mean':         round(float(gb_auc.mean()),      4),
        'gb_cv_auc_std':          round(float(gb_auc.std()),       4),
        'gb_cv_acc_mean':         round(float(gb_acc.mean()),      4),
        # Legacy aliases kept for recommender.py compatibility
        'rf_cv_auc_mean':         round(float(rf_opt_auc.mean()),  4),
        'rf_cv_auc_std':          round(float(rf_opt_auc.std()),   4),
        'rf_cv_acc_mean':         round(float(rf_opt_acc.mean()),  4),
    }

    save_artifacts(rf_opt, gb, scaler,
                   feature_importance, shap_importance,
                   healthy_mean, healthy_std, metrics, best_params)

    print("\nTraining complete. Run 'python app.py' to start the service.")
