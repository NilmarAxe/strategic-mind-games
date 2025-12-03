use crate::{GameState, Move, Player, Action, Phase};
use std::collections::HashMap;

/// Represents a node in the game tree
#[derive(Debug, Clone)]
pub struct GameNode {
    pub state: GameState,
    pub parent: Option<usize>,
    pub children: Vec<usize>,
    pub evaluation: f64,
    pub move_from_parent: Option<Move>,
}

impl GameNode {
    pub fn new(state: GameState) -> Self {
        Self {
            state,
            parent: None,
            children: Vec::new(),
            evaluation: 0.0,
            move_from_parent: None,
        }
    }

    pub fn with_parent(state: GameState, parent: usize, move_made: Move) -> Self {
        Self {
            state,
            parent: Some(parent),
            children: Vec::new(),
            evaluation: 0.0,
            move_from_parent: Some(move_made),
        }
    }
}

/// Game tree for efficient state space exploration
pub struct GameTree {
    nodes: Vec<GameNode>,
    node_map: HashMap<String, usize>,
}

impl GameTree {
    pub fn new(root_state: GameState) -> Self {
        let root = GameNode::new(root_state);
        let mut nodes = Vec::new();
        nodes.push(root);

        Self {
            nodes,
            node_map: HashMap::new(),
        }
    }

    pub fn root(&self) -> &GameNode {
        &self.nodes[0]
    }

    pub fn get_node(&self, index: usize) -> Option<&GameNode> {
        self.nodes.get(index)
    }

    pub fn get_node_mut(&mut self, index: usize) -> Option<&mut GameNode> {
        self.nodes.get_mut(index)
    }

    pub fn add_child(&mut self, parent_index: usize, state: GameState, move_made: Move) -> usize {
        let child_index = self.nodes.len();
        let mut child = GameNode::with_parent(state, parent_index, move_made);
        
        self.nodes.push(child);

        if let Some(parent) = self.nodes.get_mut(parent_index) {
            parent.children.push(child_index);
        }

        child_index
    }

    pub fn generate_moves(&self, state: &GameState, player: Player) -> Vec<Move> {
        match state.phase {
            Phase::Claim => self.generate_claim_moves(state, player),
            Phase::Challenge => self.generate_challenge_moves(state, player),
            Phase::Resolution => Vec::new(),
        }
    }

    fn generate_claim_moves(&self, state: &GameState, player: Player) -> Vec<Move> {
        let mut moves = Vec::new();

        // Generate claims with varying boldness levels
        let boldness_levels = vec![0.2, 0.4, 0.6, 0.8];

        for boldness in boldness_levels {
            for claim_type in [
                crate::ClaimType::Information,
                crate::ClaimType::Prediction,
                crate::ClaimType::Accusation,
                crate::ClaimType::Alliance,
            ] {
                let claim = crate::Claim {
                    description: format!("Generated claim with boldness {}", boldness),
                    claim_type,
                    boldness,
                    is_bluff: boldness > 0.5, // Simple heuristic
                };

                moves.push(Move {
                    action: Action::MakeClaim,
                    player,
                    claim: Some(claim),
                    confidence: 1.0 - (boldness * 0.3),
                });
            }
        }

        moves
    }

    fn generate_challenge_moves(&self, _state: &GameState, player: Player) -> Vec<Move> {
        vec![
            Move {
                action: Action::Challenge,
                player,
                claim: None,
                confidence: 0.7,
            },
            Move {
                action: Action::Accept,
                player,
                claim: None,
                confidence: 0.6,
            },
        ]
    }

    pub fn apply_move(&self, state: &GameState, move_made: &Move) -> GameState {
        let mut new_state = state.clone();

        match move_made.action {
            Action::MakeClaim => {
                new_state.current_claim = move_made.claim.clone();
                new_state.phase = Phase::Challenge;
            }
            Action::Challenge | Action::Accept => {
                new_state.phase = Phase::Resolution;
                // Simulate outcome based on claim boldness
                if let Some(claim) = &new_state.current_claim {
                    let success_prob = 0.6 - (claim.boldness * 0.3);
                    let is_successful = rand::random::<f64>() < success_prob;

                    if move_made.action == Action::Challenge {
                        if !is_successful {
                            // Challenge succeeded (claim was bluff)
                            match move_made.player {
                                Player::Player1 => new_state.player1_trust += 15,
                                Player::Player2 => new_state.player2_trust += 15,
                            }
                        } else {
                            // Challenge failed
                            match move_made.player {
                                Player::Player1 => new_state.player1_trust -= 15,
                                Player::Player2 => new_state.player2_trust -= 15,
                            }
                        }
                    } else {
                        // Accepted
                        match move_made.player.opponent() {
                            Player::Player1 => new_state.player1_trust += 5,
                            Player::Player2 => new_state.player2_trust += 5,
                        }
                    }
                }
            }
        }

        // Add move to history
        new_state.move_history.push(move_made.clone());

        new_state
    }

    pub fn is_terminal(&self, state: &GameState) -> bool {
        state.round >= 20
            || state.player1_trust >= 100
            || state.player2_trust >= 100
            || state.player1_trust <= -50
            || state.player2_trust <= -50
    }

    pub fn node_count(&self) -> usize {
        self.nodes.len()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn create_test_state() -> GameState {
        GameState {
            round: 1,
            phase: Phase::Claim,
            player1_trust: 50,
            player2_trust: 50,
            current_claim: None,
            move_history: Vec::new(),
        }
    }

    #[test]
    fn test_game_tree_creation() {
        let state = create_test_state();
        let tree = GameTree::new(state);
        assert_eq!(tree.node_count(), 1);
    }

    #[test]
    fn test_generate_claim_moves() {
        let state = create_test_state();
        let tree = GameTree::new(state.clone());
        let moves = tree.generate_moves(&state, Player::Player1);
        assert!(!moves.is_empty());
    }
}