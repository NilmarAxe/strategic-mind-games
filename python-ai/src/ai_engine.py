import numpy as np
import logging
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass
import yaml

from strategy_model import StrategyModel
from bluff_detector import BluffDetector
from pattern_analyzer import PatternAnalyzer

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class GameState:
    """Represents the current game state"""
    round_number: int
    phase: str
    player1_trust: int
    player2_trust: int
    current_claim: Optional[Dict] = None
    move_history: List[Dict] = None
    
    def __post_init__(self):
        if self.move_history is None:
            self.move_history = []


@dataclass
class AIDecision:
    """Represents an AI decision"""
    action: str
    confidence: float
    reasoning: str
    claim_data: Optional[Dict] = None
    predicted_outcome: float = 0.0


class AIEngine:
    """
    Core AI engine that makes strategic decisions using ML models
    and game theory principles.
    """
    
    def __init__(self, config_path: str = "config.yaml"):
        self.config = self._load_config(config_path)
        
        self.strategy_model = StrategyModel(
            model_path=self.config['ai']['model_path'],
            scaler_path=self.config['ai']['scaler_path']
        )
        
        self.bluff_detector = BluffDetector()
        self.pattern_analyzer = PatternAnalyzer()
        
        self.difficulty_params = self.config['ai']['difficulty_levels']
        self.current_difficulty = 'medium'
        
        logger.info("AI Engine initialized successfully")
    
    def _load_config(self, config_path: str) -> Dict:
        """Load configuration from YAML file"""
        try:
            with open(config_path, 'r') as f:
                return yaml.safe_load(f)
        except FileNotFoundError:
            logger.warning(f"Config file {config_path} not found, using defaults")
            return self._default_config()
    
    def _default_config(self) -> Dict:
        """Return default configuration"""
        return {
            'ai': {
                'model_path': 'models/trained_model.pkl',
                'scaler_path': 'models/scaler.pkl',
                'difficulty_levels': {
                    'medium': {
                        'bluff_threshold': 0.5,
                        'challenge_threshold': 0.6,
                        'risk_tolerance': 0.5
                    }
                }
            }
        }
    
    def set_difficulty(self, difficulty: str):
        """Set AI difficulty level"""
        if difficulty in self.difficulty_params:
            self.current_difficulty = difficulty
            logger.info(f"AI difficulty set to: {difficulty}")
        else:
            logger.warning(f"Unknown difficulty: {difficulty}")
    
    def make_decision(self, game_state: GameState, player_type: str) -> AIDecision:
        """
        Main decision-making function. Analyzes game state and returns
        optimal action based on ML predictions and strategic analysis.
        """
        phase = game_state.phase.upper()
        
        if phase == "CLAIM":
            return self._make_claim_decision(game_state)
        elif phase == "CHALLENGE":
            return self._make_challenge_decision(game_state)
        else:
            return AIDecision(
                action="WAIT",
                confidence=1.0,
                reasoning="Waiting for resolution phase to complete"
            )
    
    def _make_claim_decision(self, game_state: GameState) -> AIDecision:
        """
        Decide what claim to make. Uses ML model to predict success
        probability and strategic analysis for claim crafting.
        """
        difficulty = self.difficulty_params[self.current_difficulty]
        
        # Analyze current situation
        trust_differential = game_state.player2_trust - game_state.player1_trust
        round_progress = game_state.round_number / 20.0
        
        # Determine optimal boldness based on game state
        optimal_boldness = self._calculate_optimal_boldness(
            trust_differential, 
            round_progress,
            difficulty['risk_tolerance']
        )
        
        # Decide whether to bluff
        should_bluff = self._should_bluff(
            game_state,
            difficulty['bluff_threshold']
        )
        
        # Select claim type based on strategy
        claim_type = self._select_claim_type(
            game_state,
            should_bluff
        )
        
        # Generate claim description
        claim_description = self._generate_claim_description(
            claim_type,
            optimal_boldness,
            should_bluff
        )
        
        # Predict success probability
        features = self._extract_features(game_state, optimal_boldness)
        success_probability = self.strategy_model.predict_success(features)
        
        claim_data = {
            'description': claim_description,
            'type': claim_type,
            'boldness': optimal_boldness,
            'is_bluff': should_bluff
        }
        
        reasoning = self._generate_claim_reasoning(
            should_bluff,
            optimal_boldness,
            success_probability,
            trust_differential
        )
        
        return AIDecision(
            action="CLAIM",
            confidence=success_probability,
            reasoning=reasoning,
            claim_data=claim_data,
            predicted_outcome=self._estimate_outcome(success_probability, optimal_boldness)
        )
    
    def _make_challenge_decision(self, game_state: GameState) -> AIDecision:
        """
        Decide whether to challenge opponent's claim or accept it.
        Uses bluff detection and risk assessment.
        """
        if game_state.current_claim is None:
            return AIDecision(
                action="ACCEPT",
                confidence=0.5,
                reasoning="No claim to evaluate"
            )
        
        difficulty = self.difficulty_params[self.current_difficulty]
        
        # Analyze claim for bluff indicators
        bluff_probability = self.bluff_detector.detect_bluff(
            game_state.current_claim,
            game_state.move_history
        )
        
        # Analyze opponent's pattern
        opponent_pattern = self.pattern_analyzer.analyze_player_pattern(
            game_state.move_history
        )
        
        # Calculate expected value of challenging
        challenge_ev = self._calculate_challenge_expected_value(
            bluff_probability,
            game_state.current_claim['boldness']
        )
        
        # Decision threshold adjusted by difficulty
        challenge_threshold = difficulty['challenge_threshold']
        
        should_challenge = bluff_probability > challenge_threshold or challenge_ev > 0
        
        action = "CHALLENGE" if should_challenge else "ACCEPT"
        
        reasoning = self._generate_challenge_reasoning(
            bluff_probability,
            challenge_ev,
            opponent_pattern,
            should_challenge
        )
        
        return AIDecision(
            action=action,
            confidence=bluff_probability if should_challenge else (1.0 - bluff_probability),
            reasoning=reasoning,
            predicted_outcome=challenge_ev
        )
    
    def _calculate_optimal_boldness(self, trust_diff: int, 
                                    round_progress: float,
                                    risk_tolerance: float) -> float:
        """
        Calculate optimal claim boldness based on game situation.
        Uses strategic principles and risk tolerance.
        """
        # Base boldness on risk tolerance
        base_boldness = risk_tolerance
        
        # Adjust based on trust position
        if trust_diff < -20:
            # Behind: need to take more risks
            position_modifier = 0.2
        elif trust_diff > 20:
            # Ahead: play safer
            position_modifier = -0.2
        else:
            position_modifier = 0.0
        
        # Adjust based on round (endgame urgency)
        if round_progress > 0.75:
            urgency_modifier = 0.15 * (round_progress - 0.75) * 4
        else:
            urgency_modifier = 0.0
        
        optimal = base_boldness + position_modifier + urgency_modifier
        
        # Clamp between 0.1 and 0.95
        return max(0.1, min(0.95, optimal))
    
    def _should_bluff(self, game_state: GameState, threshold: float) -> bool:
        """
        Decide whether to attempt a bluff based on game state and difficulty.
        """
        # Calculate bluff propensity
        trust_factor = game_state.player2_trust / 100.0
        
        # Analyze recent success rate
        recent_moves = game_state.move_history[-10:] if game_state.move_history else []
        success_rate = sum(1 for m in recent_moves if m.get('success', False)) / max(len(recent_moves), 1)
        
        # Random element for unpredictability
        randomness = np.random.random() * 0.3
        
        bluff_score = (trust_factor * 0.4) + (success_rate * 0.4) + randomness
        
        return bluff_score > threshold
    
    def _select_claim_type(self, game_state: GameState, is_bluff: bool) -> str:
        """Select appropriate claim type based on strategy"""
        round_num = game_state.round_number
        
        # Strategic claim type selection
        if round_num <= 5:
            # Early game: information claims
            weights = [0.5, 0.3, 0.1, 0.1]
        elif round_num <= 15:
            # Mid game: mix of types
            weights = [0.3, 0.3, 0.2, 0.2]
        else:
            # Late game: aggressive claims
            weights = [0.2, 0.3, 0.4, 0.1]
        
        types = ['INFORMATION', 'PREDICTION', 'ACCUSATION', 'ALLIANCE']
        
        if is_bluff:
            # Bluffs work better with certain types
            weights[0] *= 1.5  # Information bluffs are harder to verify
            weights[2] *= 0.5  # Accusations are easier to disprove
        
        # Normalize weights
        total = sum(weights)
        weights = [w / total for w in weights]
        
        return np.random.choice(types, p=weights)
    
    def _generate_claim_description(self, claim_type: str, 
                                    boldness: float, 
                                    is_bluff: bool) -> str:
        """Generate natural language claim description"""
        
        templates = {
            'INFORMATION': [
                f"I have reliable intelligence about the situation",
                f"My sources confirm a significant development",
                f"I've discovered critical information that changes everything"
            ],
            'PREDICTION': [
                f"I predict the next phase will favor my position",
                f"Based on my analysis, I foresee a major shift",
                f"The patterns indicate an inevitable outcome"
            ],
            'ACCUSATION': [
                f"Your previous claim was clearly fabricated",
                f"I can prove your last statement was false",
                f"The evidence contradicts your position"
            ],
            'ALLIANCE': [
                f"I propose we collaborate on this matter",
                f"Our interests align in this situation",
                f"A strategic partnership would benefit us both"
            ]
        }
        
        base_claim = np.random.choice(templates.get(claim_type, templates['INFORMATION']))
        
        # Add intensity based on boldness
        if boldness > 0.7:
            intensity = " with absolute certainty"
        elif boldness > 0.4:
            intensity = " with strong confidence"
        else:
            intensity = ""
        
        return base_claim + intensity
    
    def _extract_features(self, game_state: GameState, boldness: float) -> np.ndarray:
        """Extract feature vector for ML model"""
        features = [
            game_state.round_number / 20.0,
            game_state.player2_trust / 100.0,
            game_state.player1_trust / 100.0,
            boldness,
            len(game_state.move_history) / 50.0,
            self._calculate_momentum(game_state.move_history),
            self._calculate_volatility(game_state.move_history)
        ]
        
        return np.array(features).reshape(1, -1)
    
    def _calculate_momentum(self, move_history: List[Dict]) -> float:
        """Calculate recent performance momentum"""
        if not move_history:
            return 0.5
        
        recent = move_history[-5:]
        trust_changes = [m.get('trust_change', 0) for m in recent]
        
        if not trust_changes:
            return 0.5
        
        avg_change = np.mean(trust_changes)
        return 0.5 + (avg_change / 50.0)  # Normalize to 0-1
    
    def _calculate_volatility(self, move_history: List[Dict]) -> float:
        """Calculate strategy volatility"""
        if len(move_history) < 3:
            return 0.5
        
        trust_changes = [m.get('trust_change', 0) for m in move_history[-10:]]
        return min(1.0, np.std(trust_changes) / 20.0)
    
    def _calculate_challenge_expected_value(self, bluff_prob: float, 
                                           boldness: float) -> float:
        """Calculate expected value of challenging"""
        # Points gained if challenge succeeds
        success_gain = 15
        
        # Points lost if challenge fails
        failure_loss = -15
        
        # Expected value calculation
        ev = (bluff_prob * success_gain) + ((1 - bluff_prob) * failure_loss)
        
        return ev
    
    def _estimate_outcome(self, success_prob: float, boldness: float) -> float:
        """Estimate expected outcome of a claim"""
        potential_gain = 10 + (boldness * 30)
        potential_loss = -(15 + (boldness * 35))
        
        return (success_prob * potential_gain) + ((1 - success_prob) * potential_loss)
    
    def _generate_claim_reasoning(self, is_bluff: bool, boldness: float,
                                  success_prob: float, trust_diff: int) -> str:
        """Generate human-readable reasoning for claim decision"""
        reasoning_parts = []
        
        if trust_diff < -20:
            reasoning_parts.append("Behind in trust, taking aggressive stance.")
        elif trust_diff > 20:
            reasoning_parts.append("Ahead in trust, maintaining pressure.")
        else:
            reasoning_parts.append("Competitive position, balanced approach.")
        
        if is_bluff:
            reasoning_parts.append(f"Attempting strategic deception (boldness: {boldness:.2f}).")
        else:
            reasoning_parts.append(f"Making truthful claim (boldness: {boldness:.2f}).")
        
        reasoning_parts.append(f"Predicted success: {success_prob:.1%}.")
        
        return " ".join(reasoning_parts)
    
    def _generate_challenge_reasoning(self, bluff_prob: float, ev: float,
                                     pattern: Dict, should_challenge: bool) -> str:
        """Generate reasoning for challenge decision"""
        reasoning_parts = []
        
        reasoning_parts.append(f"Bluff probability: {bluff_prob:.1%}.")
        reasoning_parts.append(f"Expected value: {ev:.1f}.")
        
        if pattern.get('bluff_frequency', 0) > 0.6:
            reasoning_parts.append("Opponent shows high bluff frequency.")
        
        if should_challenge:
            reasoning_parts.append("Challenge recommended based on analysis.")
        else:
            reasoning_parts.append("Accepting claim is optimal strategy.")
        
        return " ".join(reasoning_parts)
    
    def update_from_result(self, game_state: GameState, 
                           decision: AIDecision, 
                           actual_outcome: Dict):
        """
        Update internal models based on actual game outcome.
        Used for online learning and adaptation.
        """
        # Log for training data collection
        self._log_training_data(game_state, decision, actual_outcome)
        
        # Update pattern analyzer
        self.pattern_analyzer.update_patterns(actual_outcome)
        
        logger.debug(f"Updated models with outcome: {actual_outcome}")
    
    def _log_training_data(self, game_state: GameState,
                           decision: AIDecision,
                           outcome: Dict):
        """Log data for future model training"""
        training_entry = {
            'round': game_state.round_number,
            'phase': game_state.phase,
            'action': decision.action,
            'confidence': decision.confidence,
            'boldness': decision.claim_data.get('boldness', 0) if decision.claim_data else 0,
            'success': outcome.get('success', False),
            'trust_change': outcome.get('trust_change', 0)
        }
        
        # In production, write to database or file
        logger.debug(f"Training data logged: {training_entry}")