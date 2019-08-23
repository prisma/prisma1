use super::Path;
use crate::filter::RecordFinder;

pub struct RecordAddress {
    pub path: Path,
    pub record_finder: RecordFinder,
}
