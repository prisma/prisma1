use std::ffi::CString;
use std::os::raw::c_char;

/////////////////
// Rust -> JVM //
/////////////////

// We need to honor zero termination. Might lead to reallocation.
pub fn string_to_ptr(s: String) -> *mut c_char {
    let cs: CString = CString::new(s).unwrap();
    cs.into_raw()
}

// Todo what about double free? How does GC + Rust's string drops work here, for example.

/////////////////
// JVM -> Rust //
/////////////////

/// Expects a NULL terminated UTF-8 string behind the pointer.
pub fn to_string(pointer: *const c_char) -> String {
    unsafe {
        String::from_utf8_unchecked(CString::from_raw(pointer as *mut c_char).into_bytes())
    }
}

pub fn to_string_vector(raw: *const c_char, num_elements: i64) -> Vec<String> {
    let mut vec: Vec<String> = Vec::with_capacity(num_elements as usize);
    let mut offset = 0; // Start scanning at 0
    unsafe {
        for i in 0..num_elements {
            let ptr = { raw.offset(offset as isize) };
            println!("Str {} begins at: {:?}", i, ptr);
            let s = to_string(ptr);
            println!("s at {:?}", s.as_ptr());
            offset += s.len() + 1; // Include NULL termination
            vec.push(s)
        }
    }

    vec
}