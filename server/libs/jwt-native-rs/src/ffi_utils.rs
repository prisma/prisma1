use std::ffi::{CStr, CString};
use std::os::raw::c_char;

/////////////////
// Rust -> JVM //
/////////////////

/// We need to honor zero termination. Might lead to reallocation.
pub fn string_to_ptr(s: String) -> *mut c_char {
    let cs: CString = CString::new(s).unwrap();
    cs.into_raw()
}

/////////////////
// JVM -> Rust //
/////////////////

/// Expects a NULL terminated UTF-8 string behind the pointer.
/// ONLY use for Rust owned memory and stack parameters, never for JVM owned memory.
pub fn to_string(pointer: *const c_char) -> String {
    unsafe {
        String::from_utf8(CString::from_raw(pointer as *mut c_char).into_bytes()).unwrap()
    }
}

/// Expects a NULL terminated UTF-8 string behind the pointer.
/// Use if the memory is not owned by Rust, for example when working on JVM owned memory.
pub fn to_str<'a>(pointer: *const c_char) -> &'a str {
    unsafe {
        CStr::from_ptr(pointer).to_str().unwrap()
    }
}

pub fn str_vec_from_pointers<'a>(raw: *const *const c_char, num_elements: i64) -> Vec<&'a str> {
    let mut vec: Vec<&str> = Vec::with_capacity(num_elements as usize);
    let mut offset = 0; // Start scanning at 0
    unsafe {
        for i in 0..num_elements {
            let ptr: *const c_char = raw.offset(i as isize).read();
            let s = to_str(ptr);

            vec.push(s);
        }
    }

    vec
}

pub fn str_vec_from_bytes<'a>(raw: *const c_char, num_elements: i64) -> Vec<&'a str> {
    let mut vec: Vec<&str> = Vec::with_capacity(num_elements as usize);
    let mut offset = 0; // Start scanning at 0
    unsafe {
        for i in 0..num_elements {
            let ptr = { raw.offset(offset as isize) };
            let s = to_str(ptr);

            offset += s.len() + 1; // Include NULL termination
            vec.push(s)
        }
    }

    vec
}
