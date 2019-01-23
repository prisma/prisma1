use std::os::raw::c_char;
use ffi_utils;
use std::mem::size_of;
use std::slice::from_raw_parts_mut;
use ProtocolError;

#[repr(C)]
#[no_mangle]
pub struct ProtocolBuffer {
    error: *mut c_char, // Always use a CString here (ffi_utils::string_to_ptr).
    data: *mut u8,      // Always use raw pointers to Box<[u8]> here
    data_len: usize,    // Data length of all bytes in the data buffer (also including termination for strings!)
}

impl ProtocolBuffer {
    pub fn into_boxed_ptr(self) -> *mut ProtocolBuffer {
        Box::into_raw(Box::new(self))
    }
}

impl Drop for ProtocolBuffer {
    fn drop(&mut self) {
        if !self.error.is_null() {
            drop(ffi_utils::to_string(self.error));
        }

        if self.data_len > 0 {
            unsafe {
                drop(Box::from_raw(self.data));
            };
        }
    }
}

impl From<String> for ProtocolBuffer {
    fn from(s: String) -> Self {
        let len = s.len() + 1;
        let ptr = ffi_utils::string_to_ptr(s) as *mut u8;

        ProtocolBuffer { error: ::std::ptr::null_mut(), data: ptr, data_len: len }
    }
}

impl From<ProtocolError> for ProtocolBuffer {
    fn from(e: ProtocolError) -> Self {
        let s = match e {
            ProtocolError::GenericError(s) => s,
        };

        let ptr = ffi_utils::string_to_ptr(s);
        ProtocolBuffer { error: ptr, data: ::std::ptr::null_mut(), data_len: 0 }
    }
}

impl From<bool> for ProtocolBuffer {
    fn from(b: bool) -> Self {
        let x: Vec<bool> = vec!(b);
        let ptr = Box::into_raw(x.into_boxed_slice()) as *mut u8;

        ProtocolBuffer {
            error: ::std::ptr::null_mut(),
            data: ptr,
            data_len: size_of::<bool>()
        }
    }
}
