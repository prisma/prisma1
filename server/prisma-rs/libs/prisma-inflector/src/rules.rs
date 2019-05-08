use super::Pluralize;
use regex::Regex;
use unicode_segmentation::UnicodeSegmentation;

#[derive(Debug)]
pub enum Rule {
    Category(CategoryRule),
    Regex(RegexRule),
}

#[derive(Debug)]
pub struct CategoryRule {
    singular: String,
    plural: String,
    words: &'static [&'static str],
}

impl Pluralize for CategoryRule {
    fn pluralize(&self, s: &str) -> Option<String> {
        let normalized = s.to_lowercase().to_owned();

        for suffix in self.words {
            if normalized.ends_with(suffix) {
                if !normalized.ends_with(&self.singular) {
                    panic!("Inflection rule error.");
                }

                let chars = normalized.graphemes(true).collect::<Vec<&str>>();
                let end_index = chars.len() - self.singular.len();

                return Some(format!("{}{}", chars[0..end_index].join(""), self.plural));
            }
        }

        None
    }
}

#[derive(Debug)]
pub struct RegexRule {
    singular: Regex,
    plural: String,
}

impl Pluralize for RegexRule {
    fn pluralize(&self, s: &str) -> Option<String> {
        let candidate = self.singular.replace(s, &self.plural as &str);
        if candidate == s {
            None
        } else {
            Some(candidate.to_string())
        }
    }
}

impl Rule {
    pub fn category(singular: String, plural: String, words: &'static [&'static str]) -> Rule {
        Rule::Category(CategoryRule {
            singular,
            plural,
            words,
        })
    }

    pub fn regex(singular: Regex, plural: String) -> Rule {
        Rule::Regex(RegexRule { singular, plural })
    }
}

impl Pluralize for Rule {
    fn pluralize(&self, s: &str) -> Option<String> {
        self.pluralize(s)
    }
}
