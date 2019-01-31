use ProtocolError;
use Result;
use std::os::raw::c_char;
use ffi_utils::to_str;

#[derive(Serialize, Deserialize, Debug)]
pub struct Grant {
    pub target: String,
    pub action: String,
}

impl Grant {
    /// Checks if self fulfills the other grant, meaning that the current grant is greater or equal in access power.
    pub fn fulfills(&self, other: &Grant) -> Result<bool> {
        // Format of target is "<service name>/<stage>"
        let (self_service, self_stage) = self.service_and_stage()?;
        let (other_service, other_stage) = other.service_and_stage()?;

        // "*" always fulfills, else it must be identical
        let valid_service = self_service == "*" || self_service == other_service;
        let valid_stage = self_stage == "*" || self_stage == other_stage;
        let valid_action = self.action == "*" || self.action == other.action;

        Ok(valid_service && valid_stage && valid_action)
    }

    pub fn service_and_stage(&self) -> Result<(&str, &str)> {
        let splitted: Vec<&str> = self.target.split("/").collect();
        if splitted.len() != 2 {
            Err(ProtocolError::GenericError(format!("Invalid grant target: {}, expected format <service name>/<stage>", self.target)))
        } else {
            Ok((splitted[0], splitted[1]))
        }
    }

    pub fn from(target: *const c_char, action: *const c_char) -> Option<Grant> {
        if target.is_null() || action.is_null() { return None; }
        Some(Grant {
            target: to_str(target).to_owned(),
            action: to_str(action).to_owned(),
        })
    }
}

