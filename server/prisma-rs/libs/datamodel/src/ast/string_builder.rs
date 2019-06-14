use super::renderer::LineWriteable;

pub struct StringBuilder {
    buffer: Vec<String>,
}

impl StringBuilder {
    pub fn new() -> StringBuilder {
        StringBuilder { buffer: Vec::new() }
    }
}

impl ToString for StringBuilder {
    fn to_string(&self) -> String {
        self.buffer.join("")
    }
}

impl LineWriteable for StringBuilder {
    fn write(&mut self, text: &str) {
        self.buffer.push(String::from(text));
    }

    fn end_line(&mut self) {
        unimplemented!("Cannot render new line in string builder.")
    }

    fn line_empty(&self) -> bool {
        self.buffer.len() == 0
    }
}
