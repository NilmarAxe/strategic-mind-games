from flask import Flask, request, jsonify
import logging
import yaml
from typing import Dict, Any

from ai_engine import AIEngine, GameState, AIDecision

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize AI Engine
ai_engine = AIEngine()

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({'status': 'ok', 'service': 'Strategic Mind AI Engine'}), 200

@app.route('/ai/decide', methods=['POST'])
def ai_decide():
    """
    Main AI decision endpoint.
    Receives game state and returns optimal move.
    """
    try:
        data = request.get_json()
        
        if not data or 'game_state' not in data:
            return jsonify({'error': 'Missing game_state'}), 400
        
        # Parse game state
        game_state_dict = data['game_state']
        game_state = parse_game_state(game_state_dict)
        
        # Get player type for difficulty
        player_type = data.get('player_type', 'AI_MEDIUM')
        difficulty = extract_difficulty(player_type)
        ai_engine.set_difficulty(difficulty)
        
        # Make decision
        decision = ai_engine.make_decision(game_state, player_type)
        
        # Convert to response format
        response = serialize_decision(decision)
        
        logger.info(f"AI Decision: {decision.action} (confidence: {decision.confidence:.2f})")
        
        return jsonify(response), 200
        
    except Exception as e:
        logger.error(f"Error in ai_decide: {str(e)}", exc_info=True)
        return jsonify({'error': str(e)}), 500

@app.route('/ai/set_difficulty', methods=['POST'])
def set_difficulty():
    """Set AI difficulty level"""
    try:
        data = request.get_json()
        difficulty = data.get('difficulty', 'medium')
        
        ai_engine.set_difficulty(difficulty)
        
        return jsonify({'status': 'ok', 'difficulty': difficulty}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/ai/analyze', methods=['POST'])
def analyze_state():
    """Analyze game state and return strategic insights"""
    try:
        data = request.get_json()
        game_state_dict = data['game_state']
        game_state = parse_game_state(game_state_dict)
        
        analysis = {
            'trust_differential': game_state.player2_trust - game_state.player1_trust,
            'round_progress': game_state.round_number / 20.0,
            'phase': game_state.phase,
            'recommendation': 'aggressive' if game_state.player2_trust < game_state.player1_trust else 'defensive'
        }
        
        return jsonify(analysis), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

def parse_game_state(state_dict: Dict[str, Any]) -> GameState:
    """Parse game state from dictionary"""
    return GameState(
        round_number=state_dict.get('round', 1),
        phase=state_dict.get('phase', 'CLAIM'),
        player1_trust=state_dict.get('player1_trust', 50),
        player2_trust=state_dict.get('player2_trust', 50),
        current_claim=state_dict.get('current_claim'),
        move_history=state_dict.get('move_history', [])
    )

def extract_difficulty(player_type: str) -> str:
    """Extract difficulty from player type string"""
    player_type_upper = player_type.upper()
    
    if 'EASY' in player_type_upper:
        return 'easy'
    elif 'HARD' in player_type_upper:
        return 'hard'
    elif 'RUTHLESS' in player_type_upper:
        return 'ruthless'
    else:
        return 'medium'

def serialize_decision(decision: AIDecision) -> Dict[str, Any]:
    """Convert AIDecision to JSON-serializable dictionary"""
    response = {
        'action': decision.action,
        'confidence': decision.confidence,
        'reasoning': decision.reasoning,
        'predicted_outcome': decision.predicted_outcome
    }
    
    if decision.claim_data:
        response['claim_data'] = decision.claim_data
    
    return response

if __name__ == '__main__':
    port = 5000
    logger.info(f"Starting AI Engine API Server on port {port}")
    app.run(host='0.0.0.0', port=port, debug=False)