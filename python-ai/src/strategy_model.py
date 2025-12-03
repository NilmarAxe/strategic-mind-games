import numpy as np
import joblib
from sklearn.ensemble import RandomForestClassifier, GradientBoostingRegressor
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
import logging
import os

logger = logging.getLogger(__name__)


class StrategyModel:
    """
    Machine learning model for predicting move success probability
    and optimal strategy selection.
    """
    
    def __init__(self, model_path: str = None, scaler_path: str = None):
        self.classifier = None
        self.regressor = None
        self.scaler = StandardScaler()
        
        if model_path and os.path.exists(model_path):
            self.load_model(model_path, scaler_path)
        else:
            self._initialize_models()
            logger.warning("No pre-trained model found, initialized new models")
    
    def _initialize_models(self):
        """Initialize ML models with default parameters"""
        self.classifier = RandomForestClassifier(
            n_estimators=200,
            max_depth=15,
            min_samples_split=5,
            random_state=42,
            n_jobs=-1
        )
        
        self.regressor = GradientBoostingRegressor(
            n_estimators=150,
            max_depth=10,
            learning_rate=0.1,
            random_state=42
        )
        
        logger.info("Initialized new ML models")
    
    def predict_success(self, features: np.ndarray) -> float:
        """
        Predict probability of move success given current features.
        Returns value between 0 and 1.
        """
        if self.classifier is None:
            # Fallback heuristic if no trained model
            return self._heuristic_prediction(features)
        
        try:
            scaled_features = self.scaler.transform(features)
            proba = self.classifier.predict_proba(scaled_features)[0]
            return float(proba[1]) if len(proba) > 1 else 0.5
        except Exception as e:
            logger.error(f"Prediction error: {e}")
            return self._heuristic_prediction(features)
    
    def predict_outcome_value(self, features: np.ndarray) -> float:
        """
        Predict expected value of a move (trust points gained/lost).
        """
        if self.regressor is None:
            return 0.0
        
        try:
            scaled_features = self.scaler.transform(features)
            return float(self.regressor.predict(scaled_features)[0])
        except Exception as e:
            logger.error(f"Value prediction error: {e}")
            return 0.0
    
    def _heuristic_prediction(self, features: np.ndarray) -> float:
        """
        Fallback heuristic when ML model unavailable.
        Uses simple rule-based logic.
        """
        features = features.flatten()
        
        # features: [round_progress, my_trust, opp_trust, boldness, history_len, momentum, volatility]
        round_progress = features[0]
        my_trust = features[1]
        opponent_trust = features[2]
        boldness = features[3]
        momentum = features[5] if len(features) > 5 else 0.5
        
        # Base success rate
        base_rate = 0.6
        
        # Adjust for trust levels
        trust_modifier = (my_trust - opponent_trust) * 0.2
        
        # Adjust for boldness (higher boldness = higher risk)
        boldness_modifier = -boldness * 0.2
        
        # Adjust for momentum
        momentum_modifier = (momentum - 0.5) * 0.15
        
        success_prob = base_rate + trust_modifier + boldness_modifier + momentum_modifier
        
        return max(0.1, min(0.9, success_prob))
    
    def train(self, X_train: np.ndarray, y_train: np.ndarray,
              values_train: np.ndarray = None):
        """
        Train the models on historical game data.
        
        Args:
            X_train: Feature matrix
            y_train: Binary success labels
            values_train: Outcome values (optional)
        """
        logger.info(f"Training models on {len(X_train)} samples...")
        
        # Fit scaler
        self.scaler.fit(X_train)
        X_scaled = self.scaler.transform(X_train)
        
        # Train classifier
        self.classifier.fit(X_scaled, y_train)
        
        # Train regressor if values provided
        if values_train is not None:
            self.regressor.fit(X_scaled, values_train)
        
        # Calculate training metrics
        train_score = self.classifier.score(X_scaled, y_train)
        logger.info(f"Classifier training accuracy: {train_score:.3f}")
        
        if values_train is not None:
            reg_score = self.regressor.score(X_scaled, values_train)
            logger.info(f"Regressor training R²: {reg_score:.3f}")
    
    def save_model(self, model_path: str, scaler_path: str):
        """Save trained models to disk"""
        os.makedirs(os.path.dirname(model_path), exist_ok=True)
        
        model_data = {
            'classifier': self.classifier,
            'regressor': self.regressor
        }
        
        joblib.dump(model_data, model_path)
        joblib.dump(self.scaler, scaler_path)
        
        logger.info(f"Models saved to {model_path}")
    
    def load_model(self, model_path: str, scaler_path: str):
        """Load pre-trained models from disk"""
        try:
            model_data = joblib.load(model_path)
            self.classifier = model_data['classifier']
            self.regressor = model_data.get('regressor')
            
            if scaler_path and os.path.exists(scaler_path):
                self.scaler = joblib.load(scaler_path)
            
            logger.info(f"Models loaded from {model_path}")
        except Exception as e:
            logger.error(f"Failed to load models: {e}")
            self._initialize_models()
    
    def get_feature_importance(self) -> dict:
        """Get feature importance from trained classifier"""
        if self.classifier is None:
            return {}
        
        feature_names = [
            'round_progress', 'my_trust', 'opponent_trust', 
            'boldness', 'history_length', 'momentum', 'volatility'
        ]
        
        importances = self.classifier.feature_importances_
        
        return dict(zip(feature_names, importances))