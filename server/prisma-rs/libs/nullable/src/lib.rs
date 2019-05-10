use serde::{Deserialize, Deserializer, Serialize, Serializer};

#[derive(Debug, Eq, PartialEq, Hash, Clone)]
pub enum Nullable<T> {
    /// Explicit null value provided.
    Null,
    /// Some value `T`
    NotNull(T),
}

pub fn optional_nullable_deserialize<'de, T, D>(deserializer: D) -> Result<Option<Nullable<T>>, D::Error>
where
    T: Deserialize<'de>,
    D: Deserializer<'de>,
{
    return Nullable::<T>::deserialize(deserializer).map(Some);
}

impl<T> Serialize for Nullable<T>
where
    T: Serialize,
{
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        match self {
            Nullable::Null => serializer.serialize_none(),
            Nullable::NotNull(x) => x.serialize(serializer),
        }
    }
}

impl<'de, T> Deserialize<'de> for Nullable<T>
where
    T: Deserialize<'de>,
{
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        Option::<T>::deserialize(deserializer).map(|x| match x {
            None => Nullable::Null,
            Some(x) => Nullable::NotNull(x),
        })
    }
}
