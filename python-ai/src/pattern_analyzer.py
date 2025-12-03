import numpy as np
from typing import Dict, List
from collections import defaultdict, deque
import logging

logger = logging.getLogger(__name__)


class PatternAnalyzer:
    """
    Analyzes player patterns and strategies to predict future behavior.
    Identifies tendencies and exploitable patterns.
    """

    def __init__(self, history_window: int = 20):
        self.history_window = history_window
        self.patterns = defaultdict(lambda: deque(maxlen=history_window))
        self.tendencies = {}

    def analyze_player_pattern(self, move_history: List[Dict]) -> Dict:
        """
        Analyze complete pattern from move history.
        Returns dictionary of identified patterns and tendencies.
        """
        if not move_history:
            return self._default_pattern()

        pattern_data = {
            'bluff_frequency': self._calculate_bluff_frequency(move_history),
            'aggression_level': self._calculate_aggression(move_history),
            'consistency': self._calculate_consistency(move_history),
            'risk_preference': self._calculate_risk_preference(move_history),
            'challenge_tendency': self._calculate_challenge_tendency(move_history),
            'adaptability': self._calculate_adaptability(move_history)
        }

        logger.debug(f"Pattern analysis: {pattern_data}")

        return pattern_data

    def _default_pattern(self) -> Dict:
        """Return neutral pattern for players with no history"""
        return {
            'bluff_frequency': 0.5,
            'aggression_level': 0.5,
            'consistency': 0.5,
            'risk_preference': 0.5,
            'challenge_tendency': 0.5,
            'adaptability': 0.5
        }

    def _calculate_bluff_frequency(self, move_history: List[Dict]) -> float:
        """Calculate how often player bluffs"""
        claims = [m for m in move_history if m.get('action') == 'CLAIM']

        if not claims:
            return 0.5

        bluffs = sum(1 for c in claims if c.get('is_bluff', False))

        return bluffs / len(claims)

    def _calculate_aggression(self, move_history: List[Dict]) -> float:
        """Calculate player's aggression level based on boldness"""
        claims = [m for m in move_history if m.get('action') == 'CLAIM']

        if not claims:
            return 0.5

        avg_boldness = np.mean([c.get('boldness', 0.5) for c in claims])

        return float(avg_boldness)

    def _calculate_consistency(self, move_history: List[Dict]) -> float:
        """Calculate how consistent player's strategy is"""
        if len(move_history) < 5:
            return 0.5

        claims = [m for m in move_history if m.get('action') == 'CLAIM']

        if not claims:
            return 0.5

        boldness_values = [c.get('boldness', 0.5) for c in claims]

        # Lower standard deviation = more consistent
        std_dev = np.std(boldness_values)

        # Convert to 0-1 scale (higher = more consistent)
        consistency = 1.0 - min(1.0, std_dev * 2)

        return float(consistency)

    def _calculate_risk_preference(self, move_history: List[Dict]) -> float:
        """Calculate player's risk-taking tendency"""
        recent_moves = move_history[-10:]

        if not recent_moves:
            return 0.5

        # High risk moves: high boldness claims, aggressive challenges
        risk_scores = []

        for move in recent_moves:
            if move.get('action') == 'CLAIM':
                risk_scores.append(move.get('boldness', 0.5))
            elif move.get('action') == 'CHALLENGE':
                risk_scores.append(0.7)  # Challenging is risky
            else:
                risk_scores.append(0.3)  # Accepting is safe

        return float(np.mean(risk_scores)) if risk_scores else 0.5

    def _calculate_challenge_tendency(self, move_history: List[Dict]) -> float:
        """Calculate how often player challenges vs accepts"""
        decisions = [
            m for m in move_history
            if m.get('action') in ['CHALLENGE', 'ACCEPT']
        ]

        if not decisions:
            return 0.5

        challenges = sum(1 for d in decisions if d.get('action') == 'CHALLENGE')

        return challenges / len(decisions)

    def _calculate_adaptability(self, move_history: List[Dict]) -> float:
        """
        Calculate how well player adapts strategy based on outcomes.
        Higher = changes strategy after losses.
        """
        if len(move_history) < 10:
            return 0.5

        # Look for strategy changes after negative outcomes
        adaptations = 0
        windows = len(move_history) // 5

        for i in range(windows):
            start = i * 5
            end = start + 5
            window = move_history[start:end]

            # Check if strategy changed after bad result
            trust_changes = [m.get('trust_change', 0) for m in window]
            boldness_changes = [
                m.get('boldness', 0.5) for m in window
                if m.get('action') == 'CLAIM'
            ]

            if trust_changes and boldness_changes:
                if trust_changes[0] < 0 and len(boldness_changes) > 1:
                    if abs(boldness_changes[0] - boldness_changes[-1]) > 0.2:
                        adaptations += 1

        return min(1.0, adaptations / max(1, windows))

    def predict_next_move(self, move_history: List[Dict], game_state: Dict) -> Dict:
        """
        Predict opponent's likely next move based on patterns.
        """
        pattern = self.analyze_player_pattern(move_history)

        phase = game_state.get('phase', 'CLAIM')

        if phase == 'CLAIM':
            predicted_boldness = self._predict_boldness(pattern, game_state)
            will_bluff = pattern['bluff_frequency'] > 0.5

            return {
                'likely_action': 'CLAIM',
                'predicted_boldness': predicted_boldness,
                'likely_bluff': will_bluff,
                'confidence': 0.6 + (pattern['consistency'] * 0.3)
            }
        else:
            will_challenge = pattern['challenge_tendency'] > 0.5

            return {
                'likely_action': 'CHALLENGE' if will_challenge else 'ACCEPT',
                'confidence': 0.5 + (pattern['consistency'] * 0.4)
            }

    def _predict_boldness(self, pattern: Dict, game_state: Dict) -> float:
        """Predict boldness of next claim"""
        base_boldness = pattern['aggression_level']

        trust_diff = game_state.get('trust_differential', 0)

        # Adjust for desperation
        if trust_diff < -20:
            base_boldness += 0.2

        return max(0.1, min(0.9, base_boldness))

    def update_patterns(self, outcome: Dict):
        """Update pattern tracking with new outcome"""
        player = outcome.get('player')
        if player:
            self.patterns[player].append(outcome)

        logger.debug(f"Updated patterns for player: {player}")
