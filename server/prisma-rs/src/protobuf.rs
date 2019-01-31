use std::mem;

pub mod prisma {
    use rusqlite::{
        types::{Null, ToSql, ToSqlOutput},
        Error as RusqlError,
    };

    include!(concat!(env!("OUT_DIR"), "/prisma.rs"));

    use value_container::PrismaValue;

    impl ToSql for PrismaValue {
        fn to_sql(&self) -> Result<ToSqlOutput, RusqlError> {
            let value = match self {
                PrismaValue::String(value) => ToSqlOutput::from(value.as_ref() as &str),
                PrismaValue::Enum(value) => ToSqlOutput::from(value.as_ref() as &str),
                PrismaValue::Json(value) => ToSqlOutput::from(value.as_ref() as &str),
                PrismaValue::Uuid(value) => ToSqlOutput::from(value.as_ref() as &str),
                PrismaValue::GraphqlId(value) => ToSqlOutput::from(value.as_ref() as &str),
                PrismaValue::Float(value) => ToSqlOutput::from(*value as f64),
                PrismaValue::Int(value) => ToSqlOutput::from(*value),
                PrismaValue::Relation(value) => ToSqlOutput::from(*value as i64),
                PrismaValue::Boolean(value) => ToSqlOutput::from(*value),
                PrismaValue::DateTime(value) => value.to_sql().unwrap(),
                PrismaValue::Null(_) => ToSqlOutput::from(Null),
            };

            Ok(value)
        }
    }
}

#[repr(C)]
#[no_mangle]
pub struct ProtoBufEnvelope {
    pub data: *mut u8,
    pub len: usize,
}

impl ProtoBufEnvelope {
    pub fn into_boxed_ptr(self) -> *mut ProtoBufEnvelope {
        Box::into_raw(Box::new(self))
    }
}

impl Drop for ProtoBufEnvelope {
    fn drop(&mut self) {
        if self.len > 0 {
            unsafe {
                drop(Box::from_raw(self.data));
            };
        }
    }
}

impl From<Vec<u8>> for ProtoBufEnvelope {
    fn from(mut v: Vec<u8>) -> Self {
        let len = v.len();
        let data = v.as_mut_ptr();

        mem::forget(v);
        ProtoBufEnvelope { data, len }
    }
}
