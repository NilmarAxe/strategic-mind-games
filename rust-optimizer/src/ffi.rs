//! Foreign Function Interface for Java/Python integration
//! Provides both C-style FFI and JNI bindings

use crate::{AlphaBetaSearch, GameState, Player};
use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use serde::{Serialize, Deserialize};

/// Result of a search operation
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SearchResult {
    pub best_move: Option<MoveResult>,
    pub evaluation: f64,
    pub nodes_explored: u64,
    pub depth_reached: u8,
    pub time_ms: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MoveResult {
    pub action: String,
    pub confidence: f64,
}

/// Search for optimal move using alpha-beta pruning (C-style FFI)
/// 
/// # Safety
/// This function is unsafe because it deals with raw pointers from FFI
#[no_mangle]
pub unsafe extern "C" fn search_optimal_move(
    game_state_json: *const c_char,
    max_depth: u8,
    player_id: u8,
) -> *mut c_char {
    // Safety check
    if game_state_json.is_null() {
        eprintln!("[FFI] Error: Null game_state_json pointer");
        return std::ptr::null_mut();
    }

    // Convert C string to Rust string
    let c_str = match CStr::from_ptr(game_state_json).to_str() {
        Ok(s) => s,
        Err(e) => {
            eprintln!("[FFI] Error converting C string: {}", e);
            return std::ptr::null_mut();
        }
    };

    // Parse JSON
    let state: GameState = match serde_json::from_str(c_str) {
        Ok(s) => s,
        Err(e) => {
            eprintln!("[FFI] JSON parse error: {}", e);
            return std::ptr::null_mut();
        }
    };

    let player = if player_id == 1 {
        Player::Player1
    } else {
        Player::Player2
    };

    // Perform search
    let mut search = AlphaBetaSearch::new(max_depth, true);
    let result: SearchResult = search.search(&state, player);

    // Serialize result
    let result_json = match serde_json::to_string(&result) {
        Ok(json) => json,
        Err(e) => {
            eprintln!("[FFI] JSON serialization error: {}", e);
            return std::ptr::null_mut();
        }
    };

    // Convert to C string
    match CString::new(result_json) {
        Ok(c_string) => c_string.into_raw(),
        Err(e) => {
            eprintln!("[FFI] CString creation error: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Free memory allocated by search_optimal_move
/// 
/// # Safety
/// This function is unsafe because it deals with raw pointers
/// The pointer must have been created by search_optimal_move
#[no_mangle]
pub unsafe extern "C" fn free_result_string(s: *mut c_char) {
    if !s.is_null() {
        // Reconstruct the CString and let it drop
        let _ = CString::from_raw(s);
    }
}

/// Evaluate a game state (C-style FFI)
/// 
/// # Safety
/// This function is unsafe because it deals with raw pointers
#[no_mangle]
pub unsafe extern "C" fn evaluate_state(
    game_state_json: *const c_char,
    player_id: u8,
) -> f64 {
    if game_state_json.is_null() {
        eprintln!("[FFI] Error: Null game_state_json pointer");
        return 0.0;
    }

    let c_str = match CStr::from_ptr(game_state_json).to_str() {
        Ok(s) => s,
        Err(e) => {
            eprintln!("[FFI] Error converting C string: {}", e);
            return 0.0;
        }
    };

    let state: GameState = match serde_json::from_str(c_str) {
        Ok(s) => s,
        Err(e) => {
            eprintln!("[FFI] JSON parse error: {}", e);
            return 0.0;
        }
    };

    let player = if player_id == 1 {
        Player::Player1
    } else {
        Player::Player2
    };

    let evaluator = crate::evaluation::Evaluator::new();
    evaluator.evaluate(&state, player)
}

/// Get library version
#[no_mangle]
pub extern "C" fn get_version() -> *const c_char {
    static VERSION: &str = "1.0.0\0";
    VERSION.as_ptr() as *const c_char
}

/// Initialize the library (for any setup needed)
#[no_mangle]
pub extern "C" fn initialize_optimizer() -> i32 {
    // Perform any initialization
    // Return 0 for success, non-zero for error
    0
}

// JNI Bindings (only compiled when 'jni' feature is enabled)
#[cfg(feature = "jni")]
pub mod jni_bindings {
    use jni::JNIEnv;
    use jni::objects::{JClass, JString};
    use jni::sys::{jdouble, jint, jstring};

    /// JNI wrapper for search_optimal_move
    #[no_mangle]
    pub extern "system" fn Java_com_mindgames_integration_RustBridge_searchOptimalMove(
        env: JNIEnv,
        _class: JClass,
        game_state_json: JString,
        max_depth: jint,
        player_id: jint,
    ) -> jstring {
        // Convert JString to Rust String
        let json_str: String = match env.get_string(game_state_json) {
            Ok(s) => s.into(),
            Err(e) => {
                eprintln!("[JNI] Error getting string: {:?}", e);
                return JString::default().into_inner();
            }
        };

        // Call the C FFI function
        let result = unsafe {
            let c_json = match std::ffi::CString::new(json_str) {
                Ok(s) => s,
                Err(e) => {
                    eprintln!("[JNI] CString creation error: {}", e);
                    return JString::default().into_inner();
                }
            };
            
            let result_ptr = super::search_optimal_move(
                c_json.as_ptr(),
                max_depth as u8,
                player_id as u8,
            );
            
            if result_ptr.is_null() {
                eprintln!("[JNI] Search returned null");
                return JString::default().into_inner();
            }
            
            let result_cstr = std::ffi::CStr::from_ptr(result_ptr);
            let result_str = result_cstr.to_string_lossy().into_owned();
            super::free_result_string(result_ptr);
            result_str
        };

        // Convert back to JString
        match env.new_string(result) {
            Ok(s) => s.into_inner(),
            Err(e) => {
                eprintln!("[JNI] Error creating JString: {:?}", e);
                JString::default().into_inner()
            }
        }
    }

    /// JNI wrapper for evaluate_state
    #[no_mangle]
    pub extern "system" fn Java_com_mindgames_integration_RustBridge_evaluateState(
        env: JNIEnv,
        _class: JClass,
        game_state_json: JString,
        player_id: jint,
    ) -> jdouble {
        let json_str: String = match env.get_string(game_state_json) {
            Ok(s) => s.into(),
            Err(e) => {
                eprintln!("[JNI] Error getting string: {:?}", e);
                return 0.0;
            }
        };

        unsafe {
            let c_json = match std::ffi::CString::new(json_str) {
                Ok(s) => s,
                Err(e) => {
                    eprintln!("[JNI] CString creation error: {}", e);
                    return 0.0;
                }
            };
            
            super::evaluate_state(c_json.as_ptr(), player_id as u8)
        }
    }

    /// JNI wrapper for initialization
    #[no_mangle]
    pub extern "system" fn Java_com_mindgames_integration_RustBridge_nativeInitialize(
        _env: JNIEnv,
        _class: JClass,
    ) -> jint {
        super::initialize_optimizer()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_ffi_search() {
        let json = r#"{"round":1,"phase":"Claim","player1_trust":50,"player2_trust":50,"current_claim":null,"move_history":[]}"#;
        let c_json = std::ffi::CString::new(json).unwrap();
        
        unsafe {
            let result = search_optimal_move(c_json.as_ptr(), 3, 1);
            assert!(!result.is_null());
            
            // Verify we can read the result
            let result_str = std::ffi::CStr::from_ptr(result);
            let result_string = result_str.to_string_lossy();
            println!("Result: {}", result_string);
            
            free_result_string(result);
        }
    }

    #[test]
    fn test_ffi_evaluate() {
        let json = r#"{"round":1,"phase":"Claim","player1_trust":50,"player2_trust":50,"current_claim":null,"move_history":[]}"#;
        let c_json = std::ffi::CString::new(json).unwrap();
        
        unsafe {
            let eval = evaluate_state(c_json.as_ptr(), 1);
            println!("Evaluation: {}", eval);
            // Should return a reasonable value
            assert!(eval.abs() < 100.0);
        }
    }

    #[test]
    fn test_version() {
        let version = get_version();
        unsafe {
            let version_str = std::ffi::CStr::from_ptr(version);
            println!("Version: {:?}", version_str);
        }
    }

    #[test]
    fn test_initialize() {
        let result = initialize_optimizer();
        assert_eq!(result, 0);
    }
}
