import numpy as np
from typing import Dict, List
import logging

logger = logging.getLogger(__name__)


class BluffDetector:
    """
    Analyzes claims and player behavior to detect bluffs.
    Uses pattern recognition and statistical analysis.
    """
    
    def __init__(self):
        self.bluff_indicators = {
            'high_boldness': 0.25,
            'inconsistency': 0.30,
            'timing_suspicious': 0.15,
            'reputation_factor': 0.30
        }
    
    def detect_bluff(self, claim: Dict, move_history: List[Dict]) -> float:
        """
        Analyze a claim and return probability that it's a bluff (0-1).
        
        Args:
            claim: Current claim data
            move_history: Historical moves from the game
            
        Returns:
            Float between 0 and 1 representing bluff probability
        """
        scores = []
        
        # Indicator 1: Boldness analysis
        boldness_score = self._analyze_boldness(claim)
        scores.append(boldness_score * self.bluff_indicators['high_boldness'])
        
        # Indicator 2: Consistency with history
        consistency_score = self._analyze_consistency(claim, move_history)
        scores.append(consistency_score * self.bluff_indicators['inconsistency'])
        
        # Indicator 3: Timing analysis
        timing_score = self._analyze_timing(claim, move_history)
        scores.append(timing_score * self.bluff_indicators['timing_suspicious'])
        
        # Indicator 4: Player reputation
        reputation_score = self._analyze_reputation(move_history)
        scores.append(reputation_score * self.bluff_indicators['reputation_factor'])
        
        total_score = sum(scores)
        
        logger.debug(f"Bluff detection score: {total_score:.3f}")
        
        return max(0.0, min(1.0, total_score))
    
    def _analyze_boldness(self, claim: Dict) -> float:
        """
        High boldness claims are more likely to be bluffs.
        Returns score 0-1.
        """
        boldness = claim.get('boldness', 0.5)
        
        # Exponential relationship: very bold = very suspicious
        suspicion = (boldness ** 2)
        
        return suspicion
    
    def _analyze_consistency(self, claim: Dict, 
                            move_history: List[Dict]) -> float:
        """
        Check if claim is consistent with player's previous behavior.
        Inconsistent claims are more suspicious.
        """
        if not move_history:
            return 0.5  # No history, neutral
        
        recent_claims = [m for m in move_history[-10:] 
                        if m.get('action') == 'CLAIM']
        
        if not recent_claims:
            return 0.5
        
        # Analyze boldness pattern
        current_boldness = claim.get('boldness', 0.5)
        avg_boldness = np.mean([c.get('boldness', 0.5) for c in recent_claims])
        
        # Large deviation = suspicious
        deviation = abs(current_boldness - avg_boldness)
        
        inconsistency_score = min(1.0, deviation * 2)
        
        return inconsistency_score
    
    def _analyze_timing(self, claim: Dict, move_history: List[Dict]) -> float:
        """
        Analyze if timing of claim is suspicious.
        Desperate situations lead to more bluffs.
        """
        if not move_history:
            return 0.5
        
        # Check recent performance
        recent_moves = move_history[-5:]
        recent_trust_changes = [m.get('trust_change', 0) for m in recent_moves]
        
        if not recent_trust_changes:
            return 0.5
        
        # Losing streak = more likely to bluff
        avg_recent = np.mean(recent_trust_changes)
        
        if avg_recent < -10:
            # Bad streak, likely desperate
            return 0.75
        elif avg_recent < 0:
            return 0.6
        else:
            return 0.4
    
    def _analyze_reputation(self, move_history: List[Dict]) -> float:
        """
        Players with history of bluffing are more likely to bluff again.
        """
        if not move_history:
            return 0.5
        
        # Count known bluffs (challenges that succeeded)
        challenges = [m for m in move_history 
                     if m.get('action') == 'CHALLENGE']
        
        if not challenges:
            return 0.5
        
        successful_challenges = sum(1 for c in challenges 
                                   if c.get('success', False))
        
        bluff_rate = successful_challenges / len(challenges)
        
        return bluff_rate
    
    def update_indicators(self, weights: Dict[str, float]):
        """Update indicator weights based on performance"""
        self.bluff_indicators.update(weights)
        logger.info(f"Updated bluff indicators: {self.bluff_indicators}")