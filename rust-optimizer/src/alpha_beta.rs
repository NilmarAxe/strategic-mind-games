use crate::{GameTree, GameState, Move, Player, SearchResult};
use crate::evaluation::Evaluator;
use std::time::Instant;
use rayon::prelude::*;

/// Alpha-Beta pruning search with parallel optimization
pub struct AlphaBetaSearch {
    evaluator: Evaluator,
    max_depth: u8,
    nodes_explored: u64,
    enable_parallel: bool,
}

impl AlphaBetaSearch {
    pub fn new(max_depth: u8, enable_parallel: bool) -> Self {
        Self {
            evaluator: Evaluator::new(),
            max_depth,
            nodes_explored: 0,
            enable_parallel,
        }
    }

    pub fn search(&mut self, state: &GameState, player: Player) -> SearchResult {
        let start_time = Instant::now();
        self.nodes_explored = 0;

        let tree = GameTree::new(state.clone());
        
        let (best_move, evaluation) = if self.enable_parallel && self.max_depth > 3 {
            self.parallel_alpha_beta(&tree, state, self.max_depth, player)
        } else {
            self.alpha_beta(
                &tree,
                state,
                self.max_depth,
                f64::NEG_INFINITY,
                f64::INFINITY,
                player,
                true,
            )
        };

        let time_ms = start_time.elapsed().as_millis() as u64;

        SearchResult {
            best_move: best_move.map(|m| crate::MoveResult {
                action: format!("{:?}", m.action),
                confidence: m.confidence,
            }),
            evaluation,
            nodes_explored: self.nodes_explored,
            depth_reached: self.max_depth,
            time_ms,
        }
    }

    fn alpha_beta(
        &mut self,
        tree: &GameTree,
        state: &GameState,
        depth: u8,
        mut alpha: f64,
        mut beta: f64,
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
                let (_, eval) = self.alpha_beta(
                    tree,
                    &new_state,
                    depth - 1,
                    alpha,
                    beta,
                    player.opponent(),
                    false,
                );

                if eval > max_eval {
                    max_eval = eval;
                    best_move = Some(move_candidate);
                }

                alpha = alpha.max(eval);

                // Beta cutoff
                if beta <= alpha {
                    break;
                }
            }

            (best_move, max_eval)
        } else {
            let mut min_eval = f64::INFINITY;
            let mut best_move = None;

            for move_candidate in moves {
                let new_state = tree.apply_move(state, &move_candidate);
                let (_, eval) = self.alpha_beta(
                    tree,
                    &new_state,
                    depth - 1,
                    alpha,
                    beta,
                    player.opponent(),
                    true,
                );

                if eval < min_eval {
                    min_eval = eval;
                    best_move = Some(move_candidate);
                }

                beta = beta.min(eval);

                // Alpha cutoff
                if beta <= alpha {
                    break;
                }
            }

            (best_move, min_eval)
        }
    }

    fn parallel_alpha_beta(
        &mut self,
        tree: &GameTree,
        state: &GameState,
        depth: u8,
        player: Player,
    ) -> (Option<Move>, f64) {
        let moves = tree.generate_moves(state, player);

        if moves.is_empty() {
            let eval = self.evaluator.evaluate(state, player);
            return (None, eval);
        }

        // Evaluate root moves in parallel
        let results: Vec<(Move, f64)> = moves
            .par_iter()
            .map(|move_candidate| {
                let new_state = tree.apply_move(state, move_candidate);
                let mut local_search = AlphaBetaSearch::new(depth - 1, false);
                let (_, eval) = local_search.alpha_beta(
                    tree,
                    &new_state,
                    depth - 1,
                    f64::NEG_INFINITY,
                    f64::INFINITY,
                    player.opponent(),
                    false,
                );
                (move_candidate.clone(), eval)
            })
            .collect();

        // Find best result
        let (best_move, best_eval) = results
            .into_iter()
            .max_by(|(_, eval1), (_, eval2)| {
                eval1.partial_cmp(eval2).unwrap_or(std::cmp::Ordering::Equal)
            })
            .unwrap();

        (Some(best_move), best_eval)
    }

    fn default_move(&self, _state: &GameState, player: Player) -> Move {
        Move {
            action: crate::Action::Accept,
            player,
            claim: None,
            confidence: 0.5,
        }
    }

    pub fn set_parallel(&mut self, enable: bool) {
        self.enable_parallel = enable;
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
    fn test_alpha_beta_search() {
        let mut search = AlphaBetaSearch::new(4, false);
        let state = create_test_state();
        let result = search.search(&state, Player::Player1);
        
        assert!(result.nodes_explored > 0);
    }

    #[test]
    fn test_parallel_search() {
        let mut search = AlphaBetaSearch::new(4, true);
        let state = create_test_state();
        let result = search.search(&state, Player::Player1);
        
        assert!(result.nodes_explored > 0);
    }
}
