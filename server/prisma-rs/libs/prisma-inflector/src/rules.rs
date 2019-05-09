use regex::Regex;
use unicode_segmentation::UnicodeSegmentation;

pub trait Pluralize {
    fn pluralize(&self, s: &str) -> Option<String>;
}

#[derive(Debug)]
pub enum Rule {
    Category(CategoryRule),
    Regex(RegexRule),
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
        match self {
            Rule::Category(c) => c.pluralize(s),
            Rule::Regex(r) => r.pluralize(s),
        }
    }
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
                    panic!("Invariant violation: Invalid inflection rule match: {}.", self.singular);
                }

                let chars = s.graphemes(true).collect::<Vec<&str>>();
                let end_index = chars.len() - self.singular.len();
                let result = format!("{}{}", chars[0..end_index].join(""), self.plural);

                return Some(result);
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
