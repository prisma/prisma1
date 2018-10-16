use std::os::raw::c_char;
use ffi_utils;
use std::mem::size_of;
use std::slice::from_raw_parts_mut;

#[repr(C)]
#[no_mangle]
pub struct ProtocolBuffer {
    error: *const c_char,
    data: *const c_char,
    data_len: usize,
}

pub enum ProtocolError {
    GenericError(String)
}

impl ProtocolBuffer {
    pub fn into_boxed_ptr(self) -> *mut ProtocolBuffer {
        Box::into_raw(Box::new(self))
    }
}

impl Drop for ProtocolBuffer {
    fn drop(&mut self) {
        println!("[Rust] Dropping ProtocolBuffer");

        if !self.error.is_null() {
            println!("[Rust] Dropping contained error");
            drop(ffi_utils::to_string(self.error));
        }

        if self.data_len > 0 {
            println!("[Rust] Dropping contained data");
            // Note: This is not really a good solution, I'd like to avoid casting to mut, if possible.
            let raw_buffer = unsafe { from_raw_parts_mut(self.data as *mut c_char, self.data_len) }.as_mut_ptr();
            unsafe {
                drop(Box::from_raw(raw_buffer));
            };
        }
    }
}

impl From<String> for ProtocolBuffer {
    fn from(s: String) -> Self {
        let len = s.len() + 1; // todo do we need the +1?
        let ptr = ffi_utils::string_to_ptr(s);

        ProtocolBuffer { error: ::std::ptr::null(), data: ptr, data_len: len }
    }
}

impl From<ProtocolError> for ProtocolBuffer {
    fn from(e: ProtocolError) -> Self {
        let s = match e {
            ProtocolError::GenericError(s) => s,
        };

        let ptr = ffi_utils::string_to_ptr(s);
        ProtocolBuffer { error: ptr, data: ::std::ptr::null(), data_len: 0 }
    }
}


impl From<bool> for ProtocolBuffer {
    fn from(b: bool) -> Self {
        let ptr = Box::into_raw(Box::new(b));
        ProtocolBuffer {
            error: ::std::ptr::null(),
            data: ptr as *const c_char,
            data_len: size_of::<bool>()
        }
    }
}

//fn write_string_buffer(success: bool, data: String) -> Box<ProtocolBuffer> {
//    let len = data.len();
//    let ptr = string_to_ptr(data);
//
//    Box::new(ProtocolBuffer { success: success as u8, len: len, data: ptr })
//}
