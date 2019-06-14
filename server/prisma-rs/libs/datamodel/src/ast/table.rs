use super::renderer::LineWriteable;
use super::string_builder::StringBuilder;
use std::cmp::max;
use std::ops::Add;

const COLUMN_SPACING: usize = 1;

pub enum Row {
    Regular(Vec<String>),
    Interleaved(String),
}

pub struct TableFormat {
    pub table: Vec<Row>,
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

    pub fn column_locked_writer_for(&mut self, index: usize) -> ColumnLockedWriter {
        ColumnLockedWriter {
            formatter: self,
            column: index
        }
    }

    pub fn column_locked_writer(&mut self) -> ColumnLockedWriter {
        let index = match &self.table[self.row as usize] {
            Row::Regular(row) => {
                row.len() - 1
            },
            Row::Interleaved(_) => panic!("Cannot lock col in interleaved mode")
        };

        ColumnLockedWriter {
            formatter: self,
            column: index
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

    // Safely appends to the column with the given index.
    pub fn append_to(&mut self, text: &str, index: usize) {
        if self.line_ending {
            self.start_new_line();
            self.line_ending = false;
        }

        match &mut self.table[self.row as usize] {
            Row::Regular(row) => {
                while row.len() <= index {
                    row.push(String::new());
                }

                if row[index].is_empty() {
                    row[index] = String::from(text);
                } else {
                    row[index] = format!("{}{}", &row[index], text);
                }
            },
            Row::Interleaved(_) => panic!("Cannot append to col in interleaved mode")
        }
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

    fn line_empty(&self) -> bool {
        self.line_ending
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

    fn line_empty(&self) -> bool {
        self.formatter.line_empty()
    }
}

pub struct ColumnLockedWriter<'a> {
    formatter: &'a mut TableFormat,
    column: usize
}

impl<'a> LineWriteable for ColumnLockedWriter<'a> {
    fn write(&mut self, text: &str) {
        self.formatter.append_to(text, self.column);
    }

    fn end_line(&mut self) {
        self.formatter.end_line();
    }

    fn line_empty(&self) -> bool {
        if self.formatter.line_empty() {
            return true 
        } else {
            match &self.formatter.table.last().unwrap() {
                Row::Regular(row) => row.len() <= self.column || row[self.column].is_empty(),
                Row::Interleaved(s) => s.is_empty()
            }
        }
    }
}
