use crate::{GameTree, GameState, Move, Player, SearchResult};
use crate::evaluation::Evaluator;
use std::time::Instant;

/// Minimax search algorithm implementation
pub struct MinimaxSearch {
    evaluator: Evaluator,
    max_depth: u8,
    nodes_explored: u64,
}

impl MinimaxSearch {
    pub fn new(max_depth: u8) -> Self {
        Self {
            evaluator: Evaluator::new(),
            max_depth,
            nodes_explored: 0,
        }
    }

    pub fn search(&mut self, state: &GameState, player: Player) -> SearchResult {
        let start_time = Instant::now();
        self.nodes_explored = 0;

        let tree = GameTree::new(state.clone());
        let (best_move, evaluation) = self.minimax(&tree, state, self.max_depth, player, true);

        let time_ms = start_time.elapsed().as_millis() as u64;

        SearchResult {
            best_move: best_move.unwrap_or_else(|| self.default_move(state, player)),
            evaluation,
            nodes_explored: self.nodes_explored,
            depth_reached: self.max_depth,
            time_ms,
        }
    }

    fn minimax(
        &mut self,
        tree: &GameTree,
        state: &GameState,
        depth: u8,
        player: Player,
        is_maximizing: bool,
    ) -> (Option<Move>, f64) {
        self.nodes_explored += 1;

        // Terminal conditions
        if depth == 0 || tree.is_terminal(state) {
            let eval = self.evaluator.evaluate(state, player);
            return (None, eval);
        }

        let moves = tree.generate_moves(state, player);

        if moves.is_empty() {
            let eval = self.evaluator.evaluate(state, player);
            return (None, eval);
        }

        if is_maximizing {
            let mut max_eval = f64::NEG_INFINITY;
            let mut best_move = None;

            for move_candidate in moves {
                let new_state = tree.apply_move(state, &move_candidate);
                let (_, eval) = self.minimax(
                    tree,
                    &new_state,
                    depth - 1,
                    player.opponent(),
                    false,
                );

                if eval > max_eval {
                    max_eval = eval;
                    best_move = Some(move_candidate);
                }
            }

            (best_move, max_eval)
        } else {
            let mut min_eval = f64::INFINITY;
            let mut best_move = None;

            for move_candidate in moves {
                let new_state = tree.apply_move(state, &move_candidate);
                let (_, eval) = self.minimax(
                    tree,
                    &new_state,
                    depth - 1,
                    player.opponent(),
                    true,
                );

                if eval < min_eval {
                    min_eval = eval;
                    best_move = Some(move_candidate);
                }
            }

            (best_move, min_eval)
        }
    }

    fn default_move(&self, state: &GameState, player: Player) -> Move {
        Move {
            action: crate::Action::Accept,
            player,
            claim: None,
            confidence: 0.5,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::Phase;

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
    fn test_minimax_search() {
        let mut search = MinimaxSearch::new(3);
        let state = create_test_state();
        let result = search.search(&state, Player::Player1);
        
        assert!(result.nodes_explored > 0);
        assert!(result.depth_reached > 0);
    }
}