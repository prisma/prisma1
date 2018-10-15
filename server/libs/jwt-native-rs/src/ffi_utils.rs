use std::mem;
use std::ffi::{CStr, CString};
use std::os::raw::c_char;

/////////////////
// Rust -> JVM //
/////////////////

// We need to honor zero termination. Might lead to reallocation.
pub fn string_to_ptr(s: String) -> *const c_char {
    let cs: CString = CString::new(s).unwrap();
    println!("FFI {}", cs.as_bytes_with_nul().len());
    let ptr = cs.as_ptr();
    mem::forget(cs);

    ptr
//    Box::into_raw(s.into_boxed_bytes()) as *const c_char
}

/////////////////
// JVM -> Rust //
/////////////////
// Todo verify that this does 0 copy (JVM sends \0, so it shouldn't)

pub fn to_string(pointer: *const c_char) -> String {
    unsafe {
        String::from(::std::str::from_utf8_unchecked(CStr::from_ptr(pointer).to_bytes()))
    }
}

pub fn to_string_vector(raw: *const c_char, num_elements: i64) -> Vec<String> {
    let mut vec = Vec::new();
    unsafe {
        for offset in 0..num_elements {
            let ptr = { raw.offset(offset as isize) };
            vec.push(to_string(ptr))
        }
    }

    vec
}

