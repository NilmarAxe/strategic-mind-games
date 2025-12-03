//! Strategic Mind Games - Rust Optimization Layer
//! 
//! High-performance game tree search and decision optimization
//! using minimax with alpha-beta pruning.

pub mod game_tree;
pub mod minimax;
pub mod alpha_beta;
pub mod evaluation;
pub mod ffi;

pub use game_tree::{GameNode, GameTree};
pub use minimax::MinimaxSearch;
pub use alpha_beta::AlphaBetaSearch;
pub use evaluation::Evaluator;

use serde::{Deserialize, Serialize};

/// Represents a game state that can be evaluated
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GameState {
    pub round: u8,
    pub phase: Phase,
    pub player1_trust: i32,
    pub player2_trust: i32,
    pub current_claim: Option<Claim>,
    pub move_history: Vec<Move>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum Phase {
    Claim,
    Challenge,
    Resolution,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Claim {
    pub description: String,
    pub claim_type: ClaimType,
    pub boldness: f64,
    pub is_bluff: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum ClaimType {
    Information,
    Prediction,
    Accusation,
    Alliance,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Move {
    pub action: Action,
    pub player: Player,
    pub claim: Option<Claim>,
    pub confidence: f64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum Action {
    MakeClaim,
    Challenge,
    Accept,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum Player {
    Player1,
    Player2,
}

impl Player {
    pub fn opponent(&self) -> Player {
        match self {
            Player::Player1 => Player::Player2,
            Player::Player2 => Player::Player1,
        }
    }
}

/// Result of a search operation
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SearchResult {
    pub best_move: Move,
    pub evaluation: f64,
    pub nodes_explored: u64,
    pub depth_reached: u8,
    pub time_ms: u64,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_player_opponent() {
        assert_eq!(Player::Player1.opponent(), Player::Player2);
        assert_eq!(Player::Player2.opponent(), Player::Player1);
    }
}

// Export FFI module
pub mod ffi;

// Re-export FFI functions for easier access
pub use ffi::{search_optimal_move, free_result_string, evaluate_state, initialize_optimizer};

#[cfg(feature = "jni")]
pub use ffi::jni_bindings;