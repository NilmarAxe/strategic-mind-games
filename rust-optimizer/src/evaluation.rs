use crate::{GameState, Player};

/// State evaluation function for game tree search
pub struct Evaluator {
    weights: EvaluationWeights,
}

#[derive(Debug, Clone)]
struct EvaluationWeights {
    trust_differential: f64,
    trust_absolute: f64,
    round_progress: f64,
    momentum: f64,
    position_advantage: f64,
}

impl Default for EvaluationWeights {
    fn default() -> Self {
        Self {
            trust_differential: 1.0,
            trust_absolute: 0.5,
            round_progress: 0.3,
            momentum: 0.7,
            position_advantage: 0.8,
        }
    }
}

impl Evaluator {
    pub fn new() -> Self {
        Self {
            weights: EvaluationWeights::default(),
        }
    }

    pub fn with_weights(weights: EvaluationWeights) -> Self {
        Self { weights }
    }

    /// Evaluate game state from perspective of given player
    /// Returns value between -100 and +100
    pub fn evaluate(&self, state: &GameState, player: Player) -> f64 {
        let mut score = 0.0;

        // Trust differential (most important)
        score += self.evaluate_trust_differential(state, player) * self.weights.trust_differential;

        // Absolute trust position
        score += self.evaluate_trust_absolute(state, player) * self.weights.trust_absolute;

        // Round progress (endgame considerations)
        score += self.evaluate_round_progress(state, player) * self.weights.round_progress;

        // Momentum
        score += self.evaluate_momentum(state, player) * self.weights.momentum;

        // Position advantage
        score += self.evaluate_position_advantage(state, player) * self.weights.position_advantage;

        // Clamp to reasonable range
        score.max(-100.0).min(100.0)
    }

    fn evaluate_trust_differential(&self, state: &GameState, player: Player) -> f64 {
        let (my_trust, opp_trust) = match player {
            Player::Player1 => (state.player1_trust, state.player2_trust),
            Player::Player2 => (state.player2_trust, state.player1_trust),
        };

        let differential = my_trust - opp_trust;

        // Normalize to -50 to +50 range
        (differential as f64 / 3.0).max(-50.0).min(50.0)
    }

    fn evaluate_trust_absolute(&self, state: &GameState, player: Player) -> f64 {
        let my_trust = match player {
            Player::Player1 => state.player1_trust,
            Player::Player2 => state.player2_trust,
        };

        // Bonus for high trust, penalty for low trust
        if my_trust >= 80 {
            20.0
        } else if my_trust <= 0 {
            -20.0
        } else {
            0.0
        }
    }

    fn evaluate_round_progress(&self, state: &GameState, player: Player) -> f64 {
        let progress = state.round as f64 / 20.0;

        let (my_trust, opp_trust) = match player {
            Player::Player1 => (state.player1_trust, state.player2_trust),
            Player::Player2 => (state.player2_trust, state.player1_trust),
        };

        // In endgame, trust lead becomes more valuable
        if progress > 0.75 {
            let lead = my_trust - opp_trust;
            lead as f64 * (progress * 2.0)
        } else {
            0.0
        }
    }

    fn evaluate_momentum(&self, state: &GameState, player: Player) -> f64 {
        if state.move_history.len() < 3 {
            return 0.0;
        }

        let recent_moves = &state.move_history[state.move_history.len().saturating_sub(5)..];

        let my_moves: Vec<_> = recent_moves
            .iter()
            .filter(|m| {
                (player == Player::Player1 && m.player == Player::Player1)
                    || (player == Player::Player2 && m.player == Player::Player2)
            })
            .collect();

        if my_moves.is_empty() {
            return 0.0;
        }

        // Calculate trend (simplified - in real impl would track trust changes)
        let confidence_avg = my_moves.iter().map(|m| m.confidence).sum::<f64>() / my_moves.len() as f64;

        (confidence_avg - 0.5) * 20.0
    }

    fn evaluate_position_advantage(&self, state: &GameState, player: Player) -> f64 {
        let (my_trust, opp_trust) = match player {
            Player::Player1 => (state.player1_trust, state.player2_trust),
            Player::Player2 => (state.player2_trust, state.player1_trust),
        };

        // Evaluate strategic position
        let mut advantage = 0.0;

        // Near victory
        if my_trust >= 90 {
            advantage += 30.0;
        }

        // Opponent near defeat
        if opp_trust <= -40 {
            advantage += 25.0;
        }

        // Danger zone
        if my_trust <= -40 {
            advantage -= 25.0;
        }

        // Opponent near victory
        if opp_trust >= 90 {
            advantage -= 30.0;
        }

        advantage
    }
}

impl Default for Evaluator {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::Phase;

    fn create_test_state(p1_trust: i32, p2_trust: i32) -> GameState {
        GameState {
            round: 10,
            phase: Phase::Claim,
            player1_trust: p1_trust,
            player2_trust: p2_trust,
            current_claim: None,
            move_history: Vec::new(),
        }
    }

    #[test]
    fn test_evaluator_balanced() {
        let evaluator = Evaluator::new();
        let state = create_test_state(50, 50);
        let eval = evaluator.evaluate(&state, Player::Player1);
        
        assert!(eval.abs() < 10.0); // Should be near zero for balanced state
    }

    #[test]
    fn test_evaluator_advantage() {
        let evaluator = Evaluator::new();
        let state = create_test_state(80, 30);
        let eval = evaluator.evaluate(&state, Player::Player1);
        
        assert!(eval > 0.0); // Player1 should have positive evaluation
    }

    #[test]
    fn test_evaluator_disadvantage() {
        let evaluator = Evaluator::new();
        let state = create_test_state(20, 70);
        let eval = evaluator.evaluate(&state, Player::Player1);
        
        assert!(eval < 0.0); // Player1 should have negative evaluation
    }
}