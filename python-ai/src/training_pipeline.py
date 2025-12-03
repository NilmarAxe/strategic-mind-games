import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report
import logging
import os

from strategy_model import StrategyModel

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class TrainingPipeline:
    """
    Complete training pipeline for AI models.
    Generates synthetic training data and trains models.
    """
    
    def __init__(self, output_dir: str = "models/"):
        self.output_dir = output_dir
        os.makedirs(output_dir, exist_ok=True)
        
    def generate_synthetic_data(self, n_samples: int = 10000) -> tuple:
        """
        Generate synthetic training data based on game mechanics.
        """
        logger.info(f"Generating {n_samples} synthetic training samples...")
        
        features = []
        labels = []
        values = []
        
        for _ in range(n_samples):
            # Generate random game state features
            round_progress = np.random.random()
            my_trust = np.random.uniform(-50, 100)
            opponent_trust = np.random.uniform(-50, 100)
            boldness = np.random.random()
            history_len = np.random.randint(0, 50)
            momentum = np.random.random()
            volatility = np.random.random()
            
            # Calculate success based on heuristics
            trust_advantage = (my_trust - opponent_trust) / 150.0
            risk_factor = boldness * 0.5
            
            success_prob = 0.6 + trust_advantage * 0.3 - risk_factor + (momentum - 0.5) * 0.1
            success_prob = max(0.0, min(1.0, success_prob))
            
            success = 1 if np.random.random() < success_prob else 0
            
            # Calculate value
            if success:
                value = 10 + boldness * 30
            else:
                value = -(15 + boldness * 35)
            
            features.append([
                round_progress, my_trust / 100.0, opponent_trust / 100.0,
                boldness, history_len / 50.0, momentum, volatility
            ])
            labels.append(success)
            values.append(value)
        
        X = np.array(features)
        y = np.array(labels)
        v = np.array(values)
        
        logger.info(f"Generated {len(X)} samples with {np.mean(y):.2%} success rate")
        
        return X, y, v
    
    def train_models(self, X: np.ndarray, y: np.ndarray, v: np.ndarray):
        """Train classification and regression models"""
        logger.info("Training models...")
        
        # Split data
        X_train, X_test, y_train, y_test, v_train, v_test = train_test_split(
            X, y, v, test_size=0.2, random_state=42
        )
        
        # Initialize and train model
        model = StrategyModel()
        model.train(X_train, y_train, v_train)
        
        # Evaluate
        y_pred_proba = [model.predict_success(x.reshape(1, -1)) for x in X_test]
        y_pred = [1 if p > 0.5 else 0 for p in y_pred_proba]
        
        accuracy = accuracy_score(y_test, y_pred)
        logger.info(f"Test Accuracy: {accuracy:.3f}")
        
        print("\nClassification Report:")
        print(classification_report(y_test, y_pred))
        
        # Save models
        model_path = os.path.join(self.output_dir, "trained_model.pkl")
        scaler_path = os.path.join(self.output_dir, "scaler.pkl")
        model.save_model(model_path, scaler_path)
        
        logger.info(f"Models saved to {self.output_dir}")
        
        return model
    
    def run_full_pipeline(self, n_samples: int = 10000):
        """Execute complete training pipeline"""
        logger.info("Starting full training pipeline...")
        
        # Generate data
        X, y, v = self.generate_synthetic_data(n_samples)
        
        # Train models
        model = self.train_models(X, y, v)
        
        # Display feature importance
        importance = model.get_feature_importance()
        logger.info("\nFeature Importance:")
        for feature, imp in sorted(importance.items(), key=lambda x: x[1], reverse=True):
            logger.info(f"  {feature}: {imp:.4f}")
        
        logger.info("Training pipeline completed successfully!")
        
        return model


if __name__ == "__main__":
    pipeline = TrainingPipeline()
    pipeline.run_full_pipeline(n_samples=15000)