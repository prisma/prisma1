pub struct RawQuery(pub String);

impl RawQuery {
    pub fn is_select(&self) -> bool {
        let splitted: Vec<&str> = self.0.split(" ").collect();
        splitted
            .first()
            .map(|t| t.to_uppercase().trim() == "SELECT")
            .unwrap_or(false)
    }
}

impl<T> From<T> for RawQuery
where
    T: Into<String>,
{
    fn from(s: T) -> Self {
        RawQuery(s.into())
    }
}
