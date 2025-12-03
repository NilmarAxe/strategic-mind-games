fn main() {
    println!("cargo:rerun-if-changed=src/");
    
    // Generate JNI headers if needed
    #[cfg(target_os = "windows")]
    println!("cargo:rustc-link-lib=dylib=jvm");
    
    #[cfg(target_os = "linux")]
    println!("cargo:rustc-link-lib=dylib=jvm");
    
    #[cfg(target_os = "macos")]
    println!("cargo:rustc-link-lib=dylib=jvm");
}