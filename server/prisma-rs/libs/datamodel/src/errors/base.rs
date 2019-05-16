use crate::ast::Span;
use std;
use colored::Colorize;

// We do not implement the Error trait. It is not needed here,
// as parser errors should be handled and parsed differently than
// conventional errors.

pub trait ErrorWithSpan: std::fmt::Display + std::fmt::Debug {
    fn span(&self) -> Span;
}

// TODO: We should put this somewhere proper and without side effects.
pub fn pretty_print_error(file_name: &str, text: &str, span: &Span, error: &str) {
    let line_number = text[..span.start].matches("\n").count();
    let file_lines = text.split("\n").collect::<Vec<&str>>();

    let chars_in_line_before: usize = file_lines[..line_number].iter().map(|l| l.len()).sum();
    // Don't forget to count the all the line breaks.
    let chars_in_line_before = chars_in_line_before + line_number;

    let line = &file_lines[line_number];

    let start_in_line = span.start - chars_in_line_before;
    let end_in_line = std::cmp::min(start_in_line + (span.end - span.start), line.len());

    let prefix = &line[..start_in_line];
    let offending = &line[start_in_line..end_in_line].bright_red().bold();
    let suffix = &line[end_in_line..];

    let arrow = "-->".bright_blue().bold();

    println!("{}: {}", "error".bright_red().bold(), error.bold());
    println!("  {}  {}:{}", arrow, file_name, line_number + 1);
    println!("{}", format_line_number(0));
    println!("{}", format_line_number_with_line(line_number, &file_lines));
    println!("{}{}{}{}", format_line_number(line_number + 1), prefix, offending, suffix);
    if offending.len() == 0 {
        let spacing = std::iter::repeat(" ").take(start_in_line).collect::<String>();
        println!("{}{}{}", format_line_number(0), spacing, "^ Unexpected Token".bold().bright_red());
    }
    println!("{}", format_line_number_with_line(line_number + 2, &file_lines));
    println!("{}", format_line_number(0));
}

fn format_line_number_with_line(line_number: usize, lines: &Vec<&str>) -> colored::ColoredString {
    if line_number > 0 && line_number <= lines.len() {
        colored::ColoredString::from(
            format!("{}{}", format_line_number(line_number), lines[line_number - 1]).as_str()
        )
    } else {
        format_line_number(line_number)
    }
}
fn format_line_number(line_number: usize) -> colored::ColoredString {
    if line_number > 0 {
        format!("{:2} | ", line_number).bold().bright_blue()
    } else {
        "   | ".bold().bright_blue()
    }
}