use super::renderer::LineWriteable;
use super::string_builder::StringBuilder;
use std::cmp::max;

const COLUMN_SPACING: usize = 2;

enum Row {
    Regular(Vec<String>),
    Interleaved(String),
}

pub struct TableFormat {
    table: Vec<Row>,
    row: i32,
    line_ending: bool,
}

impl TableFormat {
    pub fn new() -> TableFormat {
        TableFormat {
            table: Vec::new(),
            row: -1,
            line_ending: true,
        }
    }

    pub fn interleave_writer(&mut self) -> TableFormatInterleaveWrapper {
        TableFormatInterleaveWrapper {
            formatter: self,
            string_builder: StringBuilder::new(),
        }
    }

    pub fn interleave(&mut self, text: &str) {
        self.table.push(Row::Interleaved(String::from(text)));
        // We've just ended a line.
        self.line_ending = false;
        self.row = self.row + 1;

        // Prepare next new line.
        self.end_line();
    }

    fn start_new_line(&mut self) {
        self.table.push(Row::Regular(Vec::new()));
        self.row = self.row + 1;
    }

    pub fn render(&self, target: &mut LineWriteable) {
        // First, measure cols
        let mut len = 0;

        for row in &self.table {
            if let Row::Regular(row) = row {
                len = max(len, row.len());
            }
        }

        let mut cols_width = vec![0; len];

        for row in &self.table {
            if let Row::Regular(row) = row {
                for (i, col) in row.iter().enumerate() {
                    cols_width[i] = max(cols_width[i], col.len());
                }
            }
        }

        // Then, render
        for row in &self.table {
            match row {
                Row::Regular(row) => {
                    for (i, col) in row.iter().enumerate() {
                        let spacing = if i == row.len() - 1 {
                            0 // Do not space last column.
                        } else {
                            cols_width[i] - col.len() + COLUMN_SPACING
                        };
                        target.write(&format!("{}{}", col, " ".repeat(spacing)));
                    }
                }
                Row::Interleaved(text) => {
                    target.write(text);
                }
            }

            target.end_line();
        }
    }
}

impl LineWriteable for TableFormat {
    fn write(&mut self, text: &str) {
        if self.line_ending {
            self.start_new_line();
            self.line_ending = false;
        }

        let trimmed = text.trim();

        match &mut self.table[self.row as usize] {
            Row::Regular(row) => row.push(String::from(trimmed)),
            _ => panic!("State error: Not inside a regular table row."),
        }
    }

    fn end_line(&mut self) {
        // Lazy line ending.
        if self.line_ending {
            self.start_new_line();
        }

        self.line_ending = true;
    }
}

pub struct TableFormatInterleaveWrapper<'a> {
    formatter: &'a mut TableFormat,
    string_builder: StringBuilder,
}

impl<'a> LineWriteable for TableFormatInterleaveWrapper<'a> {
    fn write(&mut self, text: &str) {
        self.string_builder.write(text);
    }

    fn end_line(&mut self) {
        self.formatter.interleave(&self.string_builder.to_string());
        self.string_builder = StringBuilder::new();
    }
}
