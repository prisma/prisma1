use crate::{filter::RecordFinder, mutaction::Path};

pub struct RecordAddress {
    pub path: Path,
    pub record_finder: RecordFinder,
}
