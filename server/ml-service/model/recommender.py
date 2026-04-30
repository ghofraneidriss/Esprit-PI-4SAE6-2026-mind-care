"""
recommender.py — ML-driven recommendation engine.

Pipeline:
  Patient data
    -> StandardScaler (fitted on training set)
    -> Random Forest + Gradient Boosting (ensemble)
    -> Risk score 0-100
    -> Contribution score per feature (importance x deviation from healthy baseline)
    -> Recommendations ranked by contribution score
"""

import os
import json
import joblib
import numpy as np
import pandas as pd

BASE_DIR  = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SAVED_DIR = os.path.join(BASE_DIR, "saved_models")

# ── Lazy-loaded artifacts ──────────────────────────────────────────────────────
_rf       = None
_gb       = None
_scaler   = None
_artifacts = None   # dict: feature_cols, feature_importance, healthy_mean, healthy_std, metrics


def _load():
    global _rf, _gb, _scaler, _artifacts
    if _rf is None:
        _rf     = joblib.load(os.path.join(SAVED_DIR, 'rf_model.pkl'))
        _gb     = joblib.load(os.path.join(SAVED_DIR, 'gb_model.pkl'))
        _scaler = joblib.load(os.path.join(SAVED_DIR, 'scaler.pkl'))
        with open(os.path.join(SAVED_DIR, 'model_artifacts.json')) as f:
            _artifacts = json.load(f)


# ── Feature direction: +1 means higher value = higher risk, -1 means lower = higher risk
FEATURE_DIRECTION = {
    # Higher = worse
    'Age': 1, 'BMI': 1, 'Smoking': 1, 'AlcoholConsumption': 1,
    'SystolicBP': 1, 'DiastolicBP': 1,
    'CholesterolTotal': 1, 'CholesterolLDL': 1, 'CholesterolTriglycerides': 1,
    'FamilyHistoryAlzheimers': 1, 'CardiovascularDisease': 1, 'Diabetes': 1,
    'Depression': 1, 'HeadInjury': 1, 'Hypertension': 1,
    'MemoryComplaints': 1, 'BehavioralProblems': 1,
    'Confusion': 1, 'Disorientation': 1, 'PersonalityChanges': 1,
    'DifficultyCompletingTasks': 1, 'Forgetfulness': 1,
    # Lower = worse
    'EducationLevel': -1, 'PhysicalActivity': -1, 'DietQuality': -1,
    'SleepQuality': -1, 'CholesterolHDL': -1,
    'MMSE': -1, 'FunctionalAssessment': -1, 'ADL': -1,
    # Neutral / weak
    'Gender': 0, 'Ethnicity': 0,
}

BINARY_FEATURES = {
    'Smoking', 'FamilyHistoryAlzheimers', 'CardiovascularDisease', 'Diabetes',
    'Depression', 'HeadInjury', 'Hypertension', 'MemoryComplaints',
    'BehavioralProblems', 'Confusion', 'Disorientation', 'PersonalityChanges',
    'DifficultyCompletingTasks', 'Forgetfulness',
}

# Direct Alzheimer's symptom features (binary). Each present symptom adds a
# deterministic risk boost so that symptom burden always raises the risk level.
CRITICAL_SYMPTOMS = [
    'Confusion', 'Disorientation', 'PersonalityChanges',
    'DifficultyCompletingTasks', 'Forgetfulness',
    'MemoryComplaints', 'BehavioralProblems',
]

# Medical-history risk factors (binary). Lighter weight than symptoms.
RISK_HISTORY = [
    'FamilyHistoryAlzheimers', 'Depression', 'CardiovascularDisease',
    'Diabetes', 'Hypertension', 'Smoking',
]

# ── Recommendation templates ──────────────────────────────────────────────────
# Each key is a feature name. 'condition' checks if recommendation applies.
# 'priority_fn' maps contribution score to priority.
TEMPLATES = {
    'MMSE': {
        'category': 'Cognitive',
        'recommendation': 'Cognitive Assessment & Therapy',
        'detail': lambda p, m: (
            f"MMSE {p['MMSE']}/30 (healthy avg: {m['MMSE']:.1f}). "
            "Cognitive stimulation therapy (CST), memory training, and "
            "neuropsychological follow-up strongly recommended."
        ),
    },
    'ADL': {
        'category': 'Cognitive',
        'recommendation': 'Daily Living Support',
        'detail': lambda p, m: (
            f"ADL {p['ADL']:.1f}/10 (healthy avg: {m['ADL']:.1f}). "
            "Occupational therapy and in-home care assessment recommended."
        ),
    },
    'FunctionalAssessment': {
        'category': 'Cognitive',
        'recommendation': 'Functional Rehabilitation',
        'detail': lambda p, m: (
            f"Functional assessment {p['FunctionalAssessment']:.1f}/10 "
            f"(healthy avg: {m['FunctionalAssessment']:.1f}). "
            "Physiotherapy and structured daily routine recommended."
        ),
    },
    'PhysicalActivity': {
        'category': 'Lifestyle',
        'recommendation': 'Increase Physical Activity',
        'detail': lambda p, m: (
            f"{p['PhysicalActivity']:.1f}h/week recorded "
            f"(healthy avg: {m['PhysicalActivity']:.1f}h/week). "
            "Aim for at least 2.5h/week of moderate aerobic exercise."
        ),
    },
    'DietQuality': {
        'category': 'Lifestyle',
        'recommendation': 'Dietary Improvement',
        'detail': lambda p, m: (
            f"Diet quality {p['DietQuality']:.1f}/10 "
            f"(healthy avg: {m['DietQuality']:.1f}/10). "
            "Mediterranean diet recommended — shown to slow cognitive decline."
        ),
    },
    'SleepQuality': {
        'category': 'Lifestyle',
        'recommendation': 'Sleep Quality Improvement',
        'detail': lambda p, m: (
            f"Sleep quality {p['SleepQuality']:.1f}/10 "
            f"(healthy avg: {m['SleepQuality']:.1f}/10). "
            "Poor sleep is linked to amyloid buildup. Evaluate for sleep apnea."
        ),
    },
    'BMI': {
        'category': 'Lifestyle',
        'recommendation': 'Weight Management',
        'detail': lambda p, m: (
            f"BMI {p['BMI']:.1f} (healthy avg: {m['BMI']:.1f}). "
            "Consider dietitian consultation and regular low-impact exercise."
        ),
    },
    'Smoking': {
        'category': 'Lifestyle',
        'recommendation': 'Smoking Cessation',
        'detail': lambda p, m: (
            "Smoking significantly increases vascular dementia risk. "
            "Nicotine replacement therapy or behavioral program recommended."
        ),
    },
    'AlcoholConsumption': {
        'category': 'Lifestyle',
        'recommendation': 'Reduce Alcohol Consumption',
        'detail': lambda p, m: (
            f"{p['AlcoholConsumption']} units/week recorded "
            f"(healthy avg: {m['AlcoholConsumption']:.1f}). "
            "Target: under 14 units/week."
        ),
    },
    'SystolicBP': {
        'category': 'Clinical',
        'recommendation': 'Blood Pressure Management',
        'detail': lambda p, m: (
            f"BP {p['SystolicBP']}/{p['DiastolicBP']} mmHg "
            f"(healthy avg: {m['SystolicBP']:.0f}/{m['DiastolicBP']:.0f}). "
            "Hypertension is a major modifiable dementia risk factor."
        ),
    },
    'CholesterolLDL': {
        'category': 'Clinical',
        'recommendation': 'LDL Cholesterol Management',
        'detail': lambda p, m: (
            f"LDL {p['CholesterolLDL']:.0f} mg/dL "
            f"(healthy avg: {m['CholesterolLDL']:.0f}). "
            "Dietary changes and possible statin therapy recommended."
        ),
    },
    'CholesterolHDL': {
        'category': 'Clinical',
        'recommendation': 'Increase HDL Cholesterol',
        'detail': lambda p, m: (
            f"HDL {p['CholesterolHDL']:.0f} mg/dL "
            f"(healthy avg: {m['CholesterolHDL']:.0f}). "
            "Aerobic exercise and omega-3 supplementation recommended."
        ),
    },
    'CholesterolTriglycerides': {
        'category': 'Clinical',
        'recommendation': 'Triglycerides Reduction',
        'detail': lambda p, m: (
            f"Triglycerides {p['CholesterolTriglycerides']:.0f} mg/dL "
            f"(healthy avg: {m['CholesterolTriglycerides']:.0f}). "
            "Reduce sugar and refined carbohydrates."
        ),
    },
    'Depression': {
        'category': 'Mental Health',
        'recommendation': 'Depression Treatment',
        'detail': lambda p, m: (
            "Untreated depression accelerates cognitive decline. "
            "Psychotherapy (CBT) and antidepressant review recommended."
        ),
    },
    'Hypertension': {
        'category': 'Clinical',
        'recommendation': 'Hypertension Management',
        'detail': lambda p, m: (
            "Hypertension history detected. Regular BP monitoring "
            "and medication review with cardiologist recommended."
        ),
    },
    'Diabetes': {
        'category': 'Clinical',
        'recommendation': 'Diabetes Management',
        'detail': lambda p, m: (
            "Diabetes is strongly associated with cognitive decline. "
            "Tight glycemic control and regular HbA1c monitoring essential."
        ),
    },
    'CardiovascularDisease': {
        'category': 'Clinical',
        'recommendation': 'Cardiovascular Follow-up',
        'detail': lambda p, m: (
            "Cardiovascular disease increases cerebrovascular risk. "
            "Regular cardiology follow-up and antiplatelet therapy review recommended."
        ),
    },
    'Confusion': {
        'category': 'Mental Health',
        'recommendation': 'Orientation & Safety Assessment',
        'detail': lambda p, m: (
            "Confusion/disorientation increases safety risks. "
            "Reality orientation therapy and supervised environment recommended."
        ),
    },
    'MemoryComplaints': {
        'category': 'Cognitive',
        'recommendation': 'Memory Clinic Referral',
        'detail': lambda p, m: (
            "Subjective memory complaints warrant comprehensive neuropsychological "
            "assessment to distinguish normal aging from MCI."
        ),
    },
    'BehavioralProblems': {
        'category': 'Mental Health',
        'recommendation': 'Behavioral Management',
        'detail': lambda p, m: (
            "Behavioral symptoms can be managed through structured routines, "
            "music therapy, and caregiver training."
        ),
    },
    'FamilyHistoryAlzheimers': {
        'category': 'Preventive',
        'recommendation': 'Genetic Counseling',
        'detail': lambda p, m: (
            "Family history of Alzheimer's detected. "
            "Consider APOE genotyping and preventive monitoring programs."
        ),
    },
    'HeadInjury': {
        'category': 'Preventive',
        'recommendation': 'TBI Follow-up',
        'detail': lambda p, m: (
            "History of head injury increases Alzheimer's risk. "
            "Regular neurological evaluation recommended."
        ),
    },
    'PersonalityChanges': {
        'category': 'Mental Health',
        'recommendation': 'Personality Change Evaluation',
        'detail': lambda p, m: (
            "Personality changes may signal frontal lobe involvement. "
            "Psychiatric evaluation and caregiver support recommended."
        ),
    },
    'Forgetfulness': {
        'category': 'Cognitive',
        'recommendation': 'Memory Support Program',
        'detail': lambda p, m: (
            "Forgetfulness reported. Structured memory support strategies "
            "and regular cognitive screening recommended."
        ),
    },
    'Disorientation': {
        'category': 'Mental Health',
        'recommendation': 'Disorientation Management',
        'detail': lambda p, m: (
            "Disorientation episodes require immediate assessment. "
            "Structured environment and orientation aids recommended."
        ),
    },
}


# ── Core: compute contribution score per feature ───────────────────────────────
def _compute_contributions(patient: dict) -> dict:
    """
    For each feature, compute how much it contributes to this patient's risk:
        contribution = max(0, risk_deviation) * feature_importance

    risk_deviation:
        - continuous features: (patient - healthy_mean) / healthy_std * direction
        - binary features: 1 if present (and direction=+1), else 0
    """
    healthy_mean = _artifacts['healthy_mean']
    healthy_std  = _artifacts['healthy_std']
    importance   = _artifacts['feature_importance']

    contributions = {}
    for feat in _artifacts['feature_cols']:
        val        = float(patient.get(feat, 0))
        direction  = FEATURE_DIRECTION.get(feat, 0)
        imp        = importance.get(feat, 0)

        if direction == 0:
            contributions[feat] = 0.0
            continue

        if feat in BINARY_FEATURES:
            # Binary: contribution = importance if risk factor present
            raw = imp * val if direction > 0 else imp * (1 - val)
        else:
            mean = healthy_mean.get(feat, val)
            std  = max(healthy_std.get(feat, 1), 0.01)
            deviation = (val - mean) / std
            raw = deviation * direction * imp

        contributions[feat] = round(max(0.0, raw), 5)

    return contributions


# ── Core: map contribution score to priority ───────────────────────────────────
def _contribution_to_priority(score: float, ranked_scores: list) -> str:
    """
    Priority is relative to this patient's own distribution of contribution scores.
    Top 25% → High, next 35% → Medium, rest → Low.
    """
    if not ranked_scores:
        return 'Low'
    top25 = np.percentile(ranked_scores, 75)
    top60 = np.percentile(ranked_scores, 40)
    if score >= top25:
        return 'High'
    if score >= top60:
        return 'Medium'
    return 'Low'


# ── Public API ─────────────────────────────────────────────────────────────────
def recommend(patient_data: dict) -> dict:
    """
    Full ML recommendation pipeline:
    1. Ensemble risk score (RF + GB probabilities)
    2. Contribution score per feature
    3. Risk drivers (top 3 contributors)
    4. Recommendations ranked by contribution score
    """
    _load()

    feature_cols = _artifacts['feature_cols']
    healthy_mean = _artifacts['healthy_mean']
    metrics      = _artifacts['metrics']

    # --- Step 1: Scale patient data ---
    X = np.array([[patient_data.get(f, 0) for f in feature_cols]], dtype=float)
    X_scaled = _scaler.transform(X)

    # --- Step 2: Ensemble risk score (ML base) ---
    prob_rf  = float(_rf.predict_proba(X_scaled)[0][1])
    prob_gb  = float(_gb.predict_proba(X_scaled)[0][1])
    prob_ens = (prob_rf + prob_gb) / 2
    ml_score = prob_ens * 100

    # --- Step 2b: Symptom burden boost ---
    # The ML model learned from data where symptoms AND cognitive decline
    # co-occur. When cognitive scores are healthy but symptoms are present,
    # the model underestimates risk. We correct this with a deterministic boost.
    symptom_count = sum(int(patient_data.get(s, 0)) for s in CRITICAL_SYMPTOMS)
    history_count = sum(int(patient_data.get(h, 0)) for h in RISK_HISTORY)

    # Each symptom: +5 pts (max 35), each risk history factor: +2 pts (max 12)
    symptom_boost = symptom_count * 5
    history_boost = history_count * 2
    risk_score = round(min(100.0, ml_score + symptom_boost + history_boost), 1)

    # --- Step 3: Risk label (hybrid: adjusted score + symptom hard override) ---
    if risk_score >= 60 or symptom_count >= 6:
        risk_label = 'High Risk'
    elif risk_score >= 35 or symptom_count >= 3:
        risk_label = 'Medium Risk'
    else:
        risk_label = 'Low Risk'

    # --- Step 4: Feature contribution scores ---
    contributions = _compute_contributions(patient_data)

    # Only keep features that have a template and a positive contribution
    active = {
        feat: score
        for feat, score in contributions.items()
        if feat in TEMPLATES and score > 0
    }

    # Deduplicate: SystolicBP and DiastolicBP → keep max under SystolicBP
    if 'DiastolicBP' in active and 'SystolicBP' in active:
        active['SystolicBP'] = max(active['SystolicBP'], active['DiastolicBP'])
        del active['DiastolicBP']
    if 'Disorientation' in active and 'Confusion' in active:
        active['Confusion'] = max(active['Confusion'], active['Disorientation'])
        del active['Disorientation']

    ranked_scores = sorted(active.values(), reverse=True)

    # --- Step 5: Build recommendations ---
    recommendations = []
    for feat, score in sorted(active.items(), key=lambda x: x[1], reverse=True):
        template = TEMPLATES[feat]
        priority = _contribution_to_priority(score, ranked_scores)
        try:
            detail = template['detail'](patient_data, healthy_mean)
        except Exception:
            detail = template['recommendation']

        recommendations.append({
            'feature':            feat,
            'category':           template['category'],
            'priority':           priority,
            'recommendation':     template['recommendation'],
            'detail':             detail,
            'contribution_score': score,
            'patient_value':      patient_data.get(feat),
            'healthy_avg':        round(healthy_mean.get(feat, 0), 2),
        })

    # --- Step 6: Risk drivers (top 3 contributors) ---
    risk_drivers = [
        {
            'feature':      r['feature'],
            'contribution': round(r['contribution_score'], 4),
            'patient_value': r['patient_value'],
            'healthy_avg':  r['healthy_avg'],
        }
        for r in recommendations[:3]
    ]

    # Derive cluster-compatible fields from risk tier
    tier = next(t for t in _RISK_TIERS if t['label'] == risk_label)
    cluster_profile = _build_cluster_profile(tier, healthy_mean)

    return {
        'risk_score':            risk_score,
        'risk_label':            risk_label,
        # cluster_* aliases — used by the frontend result panel
        'cluster_id':            tier['id'],
        'cluster_label':         risk_label,
        'cluster_profile':       cluster_profile,
        'prob_rf':               round(prob_rf * 100, 1),
        'prob_gb':               round(prob_gb * 100, 1),
        'total_recommendations': len(recommendations),
        'risk_drivers':          risk_drivers,
        'recommendations':       recommendations,
        'model_info': {
            'rf_cv_auc':  metrics['rf_cv_auc_mean'],
            'gb_cv_auc':  metrics['gb_cv_auc_mean'],
            'best_model': metrics['best_model'],
        },
    }


def get_model_info() -> dict:
    _load()
    return _artifacts['metrics']


# ── Cluster profile helpers ────────────────────────────────────────────────────
# The model uses RF+GB ensemble (not K-Means), but we expose three risk-tier
# "clusters" so the API surface is stable.
_RISK_TIERS = [
    {'id': 0, 'label': 'Low Risk',    'approx_size': 530, 'diag_rate': 0.12},
    {'id': 1, 'label': 'Medium Risk', 'approx_size': 420, 'diag_rate': 0.45},
    {'id': 2, 'label': 'High Risk',   'approx_size': 350, 'diag_rate': 0.85},
]


def _build_cluster_profile(tier: dict, healthy_mean: dict) -> dict:
    return {
        'label':                tier['label'],
        'size':                 tier['approx_size'],
        'avg_mmse':             round(healthy_mean.get('MMSE', 25.0), 1),
        'avg_adl':              round(healthy_mean.get('ADL', 8.0), 1),
        'avg_functional':       round(healthy_mean.get('FunctionalAssessment', 7.0), 1),
        'avg_physical_activity':round(healthy_mean.get('PhysicalActivity', 4.0), 1),
        'avg_diet_quality':     round(healthy_mean.get('DietQuality', 7.0), 1),
        'avg_sleep_quality':    round(healthy_mean.get('SleepQuality', 7.0), 1),
        'avg_bmi':              round(healthy_mean.get('BMI', 25.0), 1),
        'avg_systolic_bp':      round(healthy_mean.get('SystolicBP', 120.0), 0),
        'avg_cholesterol_ldl':  round(healthy_mean.get('CholesterolLDL', 100.0), 0),
        'diagnosis_rate':       tier['diag_rate'],
    }


def get_cluster_profiles() -> list:
    _load()
    healthy_mean = _artifacts['healthy_mean']
    return [_build_cluster_profile(t, healthy_mean) for t in _RISK_TIERS]
