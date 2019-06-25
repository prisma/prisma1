use crate::{filter::RecordFinder, Path};

pub struct RecordAddress {
    pub path: Path,
    pub record_finder: RecordFinder,
}
