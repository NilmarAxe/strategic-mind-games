# Strategic Mind Games

<div align="center">


**A psychological warfare board game with advanced AI**

*Strategic deception meets machine learning. Bluff, analyze patterns, manipulate outcomes.*

</div>

---

## Overview

Strategic Mind Games implements a psychological warfare simulation where victory depends on strategic deception, pattern recognition, and rational decision-making under uncertainty. The system leverages three programming languages to achieve optimal performance across different computational domains.

**Core Premise:** Players make claims with varying boldness levels. Opponents must evaluate claim validity through pattern analysis and probabilistic reasoning. Trust points fluctuate based on successful deceptions and accurate challenges.

**Technical Foundation:** Multi-language architecture integrating Java (UI/core logic), Python (ML-based AI), and Rust (game tree optimization) through well-defined interfaces.

---

## Design Philosophy

This project follows several architectural principles:

1. **Separation of Concerns** - Each language handles what it does best
2. **Fallback Mechanisms** - System remains functional even when components fail
3. **Performance Optimization** - Critical paths optimized in Rust, AI in Python, integration in Java
4. **Extensibility** - Modular design allows independent component upgrades
5. **Robustness** - Comprehensive error handling and graceful degradation

---

## Features

### Game Mechanics
- **Three-Phase Turn System** - Claim, Challenge, Resolution
- **Dynamic Trust Management** - Points range from -50 (defeat) to 100 (victory)
- **Four Claim Types** - Information, Prediction, Accusation, Alliance
- **Boldness System** - Higher risk claims yield higher rewards (and penalties)
- **Pattern-Based Gameplay** - Historical moves influence future predictions

### AI Implementation
- **Machine Learning Models** - Random Forest classifier + Gradient Boosting regressor
- **Bluff Detection** - Multi-factor analysis: boldness, consistency, timing, reputation
- **Behavioral Pattern Analysis** - Tracks aggression, risk preference, adaptability
- **Game Tree Search** - Minimax with Alpha-Beta pruning (Rust-optimized)
- **Strategic Adaptation** - AI adjusts tactics based on opponent behavior

### Technical Features
- **Multi-Language Integration** - Java ↔ Python (REST API) + Java ↔ Rust (JNI/FFI)
- **Fallback Systems** - Local heuristics when external components unavailable
- **Automatic Retry Logic** - Network failures handled transparently
- **Health Monitoring** - Continuous component status checking
- **Comprehensive Logging** - Debug output for all major operations

---

## Architecture

### System Design
```
┌─────────────────────────────────────────────────────────────┐
│                    Java Frontend (UI + Core)                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Game Engine │  │   UI Layer   │  │  Integration │      │
│  │   (Swing)    │  │  (Graphics)  │  │   Bridges    │      │
│  └──────┬───────┘  └──────────────┘  └───┬────┬─────┘      │
│         │                                  │    │            │
└─────────┼──────────────────────────────────┼────┼────────────┘
          │                                  │    │
          │ ┌────────────────────────────────┘    └─────────┐
          │ │                                               │
          │ │ HTTP/REST                              JNI/FFI│
          │ │                                               │
┌─────────▼─▼──────────────┐              ┌─────────────────▼──┐
│  Python AI Engine         │              │  Rust Optimizer    │
│  ┌──────────────────────┐ │              │  ┌───────────────┐ │
│  │  Strategy Model      │ │              │  │  Alpha-Beta   │ │
│  │  (scikit-learn)      │ │              │  │  Pruning      │ │
│  ├──────────────────────┤ │              │  ├───────────────┤ │
│  │  Bluff Detector      │ │              │  │  Game Tree    │ │
│  ├──────────────────────┤ │              │  │  Evaluation   │ │
│  │  Pattern Analyzer    │ │              │  └───────────────┘ │
│  └──────────────────────┘ │              │                    │
│  Flask API Server         │              │  Native Library    │
└───────────────────────────┘              └────────────────────┘
```

### Component Responsibilities

| Component | Language | Purpose | Performance Critical |
|-----------|----------|---------|---------------------|
| **Game Engine** | Java | Turn management, rule enforcement | No |
| **UI Layer** | Java Swing | Graphics, user interaction | No |
| **AI Strategy** | Python | ML-based decision making | No |
| **Tree Search** | Rust | Minimax algorithm optimization | **Yes** |
| **Bluff Detection** | Python | Pattern recognition | No |
| **Integration** | Java | Component orchestration | No |

### Data Flow
```
User Input → Java UI → Game Engine → Decision Required
                                          ↓
                    ┌─────────────────────┴─────────────────────┐
                    ↓                                           ↓
            [Python AI Available?]                    [Rust Available?]
                Yes ↓         ↓ No                    Yes ↓         ↓ No
            ML Strategy   Fallback AI             Alpha-Beta   Heuristic
                    ↓                                           ↓
                    └─────────────────────┬─────────────────────┘
                                          ↓
                                   AI Decision
                                          ↓
                              Game Engine Processes
                                          ↓
                                   Update UI
```

---

## Installation

### Prerequisites

Verify these are installed before proceeding:
```bash
# Java 17+
java -version

# Maven 3.8+
mvn -version

# Python 3.9+
python3 --version

# Rust 1.70+ (optional, for optimization)
cargo --version
```

### Quick Start
```bash
# Clone repository
git clone https://github.com/yourusername/strategic-mind-games.git
cd strategic-mind-games

# Build entire project
./build.sh

# Run game
./run.sh
```

### Detailed Build Process

#### 1. Build Rust Optimizer (Optional)
```bash
cd rust-optimizer
cargo build --release
cd ..
```

**Output:** `target/release/libstrategic_mind_optimizer.{so|dylib|dll}`

#### 2. Setup Python AI
```bash
cd python-ai
python3 -m venv venv
source venv/bin/activate  # Linux/macOS
# venv\Scripts\activate.bat  # Windows

pip install -r requirements.txt
python -m src.training_pipeline  # Train models
deactivate
cd ..
```

**Output:** `models/trained_model.pkl`, `models/scaler.pkl`

#### 3. Build Java Frontend
```bash
cd java-frontend
mvn clean package
cd ..
```

**Output:** `target/strategic-mind-games-1.0.0-jar-with-dependencies.jar`

### Build Options
```bash
# Skip specific components
./build.sh --skip-rust      # Java + Python only
./build.sh --skip-python    # Java + Rust only
./build.sh --skip-training  # Skip ML model training

# Include tests
./build.sh --with-tests
```

### Windows Build
```batch
# Use Windows batch scripts
build.bat
run.bat
clean.bat
```

---

## How to Play

### Game Objective

Reach **100 trust points** or force opponent below **-50 points** within 20 rounds.

### Turn Structure

**Phase 1: Claim**
- Active player creates a claim with adjustable boldness (0.0 - 1.0)
- Higher boldness = higher reward potential + higher risk
- Four claim types available: Information, Prediction, Accusation, Alliance

**Phase 2: Challenge**
- Opponent evaluates claim credibility
- Options: Challenge (risky) or Accept (safe)
- Decision should consider: claim boldness, opponent reputation, game state

**Phase 3: Resolution**
- System determines claim truthfulness
- Points awarded/deducted based on outcome
- Statistics updated for pattern analysis

### Scoring System

| Outcome | Trust Change |
|---------|-------------|
| Successful Claim (accepted) | +5 to +40 |
| Failed Challenge | -15 |
| Successful Challenge | +15 |
| Caught Bluffing | -20 to -55 |

### Strategy Tips

1. **Early Game:** Build reputation with moderate claims (boldness ~0.4)
2. **Mid Game:** Analyze opponent patterns, exploit predictability
3. **End Game:** Calculated aggression if behind, conservative if ahead
4. **Bluffing:** High trust score makes bluffs more believable
5. **Challenging:** Challenge when opponent is desperate or claim is suspiciously bold

---

## AI Difficulty Levels

### Easy (Baseline)
- **Bluff Threshold:** 0.3 (rarely bluffs)
- **Challenge Threshold:** 0.7 (rarely challenges)
- **Risk Tolerance:** 0.2 (very conservative)
- **Behavior:** Predictable, reactive

### Medium (Balanced)
- **Bluff Threshold:** 0.5 (moderate bluffing)
- **Challenge Threshold:** 0.6 (balanced challenges)
- **Risk Tolerance:** 0.5 (balanced)
- **Behavior:** Adapts slowly to patterns

### Hard (Aggressive)
- **Bluff Threshold:** 0.7 (frequent bluffs)
- **Challenge Threshold:** 0.5 (aggressive challenges)
- **Risk Tolerance:** 0.7 (risk-seeking)
- **Behavior:** Pattern recognition, counter-strategies

### Ruthless (Maximum)
- **Bluff Threshold:** 0.85 (near-constant deception)
- **Challenge Threshold:** 0.4 (challenges most claims)
- **Risk Tolerance:** 0.9 (extreme risk-taking)
- **Behavior:** Exploitative, adaptive, unpredictable

---

## Testing

### Run All Tests
```bash
# Complete system test
./test-complete.sh

# Individual components
cd java-frontend && mvn test
cd rust-optimizer && cargo test
cd python-ai && python -m pytest  # if pytest installed
```

### Test Coverage

- **Java Core:** Unit tests for game logic, state management
- **Java UI:** Integration tests for user interactions
- **Python AI:** Model validation, prediction accuracy
- **Rust Optimizer:** Algorithm correctness, performance benchmarks
- **Integration:** End-to-end component communication

---

## Performance Metrics

Benchmarks on Intel i7-10700K, 16GB RAM:

| Operation | Java Only | With Rust | Speedup |
|-----------|-----------|-----------|---------|
| Depth-3 Search | ~150ms | ~8ms | **18.75x** |
| Depth-4 Search | ~850ms | ~35ms | **24.3x** |
| Depth-5 Search | ~4200ms | ~140ms | **30x** |

AI decision latency: 50-200ms (Python ML inference)

---

## Development

### Project Structure
```
strategic-mind-games/
├── java-frontend/           # Java application
│   ├── src/main/java/
│   │   └── com/mindgames/
│   │       ├── Main.java
│   │       ├── core/        # Game engine
│   │       ├── ui/          # Swing interface
│   │       ├── integration/ # Python/Rust bridges
│   │       └── config/      # Configuration
│   └── pom.xml
│
├── python-ai/               # AI engine
│   ├── src/
│   │   ├── ai_engine.py
│   │   ├── strategy_model.py
│   │   ├── bluff_detector.py
│   │   ├── pattern_analyzer.py
│   │   └── api_server.py
│   └── requirements.txt
│
├── rust-optimizer/          # Performance layer
│   ├── src/
│   │   ├── lib.rs
│   │   ├── minimax.rs
│   │   ├── alpha_beta.rs
│   │   └── ffi.rs
│   └── Cargo.toml
│
├── build.sh / build.bat     # Build automation
├── run.sh / run.bat         # Execution scripts
└── README.md
```

### Adding New Features

#### New Claim Type

1. Add enum to `Claim.java`:
```java
public enum ClaimType {
    INFORMATION, PREDICTION, ACCUSATION, ALLIANCE, YOUR_NEW_TYPE
}
```

2. Update Python AI logic in `ai_engine.py`
3. Adjust UI in `ControlPanel.java`

#### New AI Difficulty

1. Add configuration in `python-ai/config.yaml`:
```yaml
difficulty_levels:
  your_level:
    bluff_threshold: 0.6
    challenge_threshold: 0.5
    risk_tolerance: 0.7
```

2. Update `AIEngine` class to recognize new level

#### Extending ML Model

1. Modify feature extraction in `strategy_model.py`
2. Retrain model: `python -m src.training_pipeline`
3. Test with: `mvn test -Dtest=AIBridgeTest`

---

## Troubleshooting

### Common Issues

**Problem:** "AI Bridge initialization failed"
```
Solution: Python server not running. Check:
- Python installed and in PATH
- Virtual environment activated
- Dependencies installed (pip install -r requirements.txt)
Game will use fallback AI automatically.
```

**Problem:** "Rust bridge not available"
```
Solution: Native library not compiled. Run:
cd rust-optimizer
cargo build --release
Game will use Java heuristic automatically.
```

**Problem:** "Failed to load native library"
```
Solution: Library path issue. Verify:
- Rust library exists in rust-optimizer/target/release/
- Java can access library (check java.library.path)
- Correct library for your OS (.so/.dylib/.dll)
```

**Problem:** Python server won't start
```
Solution: Port 5000 might be in use. Check:
lsof -i :5000  # Linux/macOS
netstat -ano | findstr :5000  # Windows
Kill conflicting process or change port in config.properties
```

### Debug Mode

Enable detailed logging:
```bash
# Edit java-frontend/src/main/java/com/mindgames/Main.java
private static final boolean DEBUG_MODE = true;
```

Check logs:
```bash
tail -f logs/strategic-mind-games.log
```

---

## Future Enhancements

Potential expansion areas (prioritized by complexity/impact ratio):

1. **Online Multiplayer** - WebSocket-based networking
2. **Persistent Rankings** - Database integration for player stats
3. **Replay System** - Save/load game states
4. **Advanced Analytics** - Detailed post-game analysis
5. **Custom AI Training** - User-specific opponent models
6. **Tournament Mode** - Bracket-based competitions
7. **More Claim Types** - Expanded strategic options
8. **Mobile Client** - Android/iOS interface

---

## Contributing

Contributions should follow these principles:

1. **Maintain architecture** - Don't break language separation
2. **Preserve fallbacks** - System must degrade gracefully
3. **Add tests** - All new features require unit tests
4. **Document changes** - Update relevant documentation
5. **Follow conventions** - Match existing code style

### Contribution Process
```bash
# Fork repository
# Create feature branch
git checkout -b feature/your-feature

# Make changes, add tests
# Verify build succeeds
./build.sh --with-tests

# Commit with clear message
git commit -m "Add feature: concise description"

# Push and create pull request
git push origin feature/your-feature
```

---

## License

MIT License - see LICENSE file for details.

---

## Acknowledgments

**Technical Foundations:**
- scikit-learn for ML algorithms
- Java Swing for UI framework
- Rust's performance ecosystem

**Inspiration:**
- Game theory concepts from von Neumann and Morgenstern
- Psychological warfare simulations
- Classic board game mechanics
