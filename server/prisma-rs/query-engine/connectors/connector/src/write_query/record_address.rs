use crate::{filter::RecordFinder, write_query::Path};

pub struct RecordAddress {
    pub path: Path,
    pub record_finder: RecordFinder,
}
