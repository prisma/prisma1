use super::{categories, exceptions, rules::Rule, Pluralize};
use regex::Regex;

#[derive(Debug, PartialEq)]
pub enum Mode {
    Anglicized,
    Classical,
}

#[derive(Debug)]
pub struct Inflector {
    pub mode: Mode,
    rules: Vec<Rule>,
    _inhibit: (),
}

impl Pluralize for Inflector {
    fn pluralize(&self, s: &str) -> Option<String> {
        for rule in &self.rules {
            if let Some(s) = match rule {
                Rule::Category(c) => c.pluralize(s),
                Rule::Regex(r) => r.pluralize(s),
            } {
                return Some(s);
            }
        }

        None
    }
}

impl Inflector {
    pub fn new(mode: Mode) -> Inflector {
        let mut rules = vec![];

        // Rules for words that do not inflect in the plural (such as fish, travois, chassis, nationality endings
        rules.push(Self::category_rule("", "", &exceptions::UNCOUNTABLE));

        // Handle standard irregular plurals (mongooses, oxen, etc.)
        exceptions::STANDARD_IRREGULAR.iter().for_each(|irr| {
            Self::irregular(irr.0, irr.1).into_iter().for_each(|r| rules.push(r));
        });

        // Handle additional standard irregular plurals
        // I don't know why Rust is throwing a type error here without .to_vec (lazy static issues?)
        let additional_irregulars = match mode {
            Mode::Anglicized => exceptions::IRREGULAR_ANGLICIZED.to_vec(),
            Mode::Classical => exceptions::IRREGULAR_CLASSICAL.to_vec(),
        };

        additional_irregulars.iter().for_each(|irr| {
            Self::irregular(irr.0, irr.1).into_iter().for_each(|r| rules.push(r));
        });

        rules.push(Self::category_rule("", "s", &categories::CATEGORY_MAN_MANS));

        // Handle irregular inflections for common suffixes
        exceptions::IRREGULAR_SUFFIX_INFLECTIONS
            .iter()
            .for_each(|(singular, plural)| {
                rules.push(Self::regex_rule(singular, plural));
            });

        // Handle fully assimilated classical inflections
        rules.push(Self::category_rule("ex", "ices", &categories::CATEGORY_EX_ICES));
        rules.push(Self::category_rule("ix", "ices", &categories::CATEGORY_IX_ICES));
        rules.push(Self::category_rule("um", "a", &categories::CATEGORY_UM_A));
        rules.push(Self::category_rule("on", "a", &categories::CATEGORY_ON_A));
        rules.push(Self::category_rule("a", "ae", &categories::CATEGORY_A_AE));

        // Handle classical variants of modern inflections
        if mode == Mode::Classical {
            exceptions::MODERN_CLASSICAL_INFLECTIONS
                .iter()
                .for_each(|(singular, plural)| {
                    rules.push(Self::regex_rule(singular, plural));
                });

            rules.push(Self::category_rule("en", "ina", &categories::CATEGORY_EN_INA));
            rules.push(Self::category_rule("a", "ata", &categories::CATEGORY_A_ATA));
            rules.push(Self::category_rule("is", "ides", &categories::CATEGORY_IS_IDES));
            rules.push(Self::category_rule("", "", &categories::CATEGORY_US_US));
            rules.push(Self::category_rule("o", "i", &categories::CATEGORY_O_I));
            rules.push(Self::category_rule("", "i", &categories::CATEGORY_NONE_I));
            rules.push(Self::category_rule("", "im", &categories::CATEGORY_NONE_IM));
            rules.push(Self::category_rule("ex", "ices", &categories::CATEGORY_EX_EXES));
            rules.push(Self::category_rule("ix", "ices", &categories::CATEGORY_IX_IXES));
        };

        rules.push(Self::category_rule("us", "i", &categories::CATEGORY_US_I));
        rules.push(Self::regex_rule("([cs]h|[zx])$", "$1es"));
        rules.push(Self::category_rule("", "es", &categories::CATEGORY_S_ES));
        rules.push(Self::category_rule("", "es", &categories::CATEGORY_IS_IDES));
        rules.push(Self::category_rule("", "es", &categories::CATEGORY_US_US));
        rules.push(Self::regex_rule("(us)$", "$1es"));
        rules.push(Self::category_rule("", "s", &categories::CATEGORY_A_ATA));

        exceptions::ADDITIONAL_SUFFIX_INFLECTIONS
            .iter()
            .for_each(|(singular, plural)| {
                rules.push(Self::regex_rule(singular, plural));
            });

        // Some words ending in -o take -os (including does preceded by a vowel)
        rules.push(Self::category_rule("o", "os", &categories::CATEGORY_O_I));
        rules.push(Self::category_rule("o", "os", &categories::CATEGORY_O_OS));
        rules.push(Self::regex_rule("([aeiou])o$", "$1os"));

        // The rest take -oes
        rules.push(Self::regex_rule("o$", "oes"));
        rules.push(Self::regex_rule("ulum", "ula"));
        rules.push(Self::category_rule("", "es", &categories::CATEGORY_A_ATA));
        rules.push(Self::regex_rule("s$", "ses"));

        // Global fallback, just assume that the plural adds -s
        rules.push(Self::regex_rule("$", "s"));

        Inflector {
            mode,
            rules,
            _inhibit: (),
        }
    }

    fn irregular(singular: &'static str, plural: &'static str) -> Vec<Rule> {
        let first_singular = singular.chars().next().unwrap();
        let first_plural = plural.chars().next().unwrap();

        // Rules are all 1-byte characters, so we can use slices.
        if first_singular == first_plural {
            vec![Rule::regex(
                Regex::new(&format!(
                    "(?i)({}){}$",
                    first_singular.to_owned(),
                    singular[1..].to_owned()
                ))
                .unwrap(),
                format!("$1{}", plural[1..].to_owned()),
            )]
        } else {
            vec![
                Rule::regex(
                    Regex::new(&format!(
                        "{}(?i){}$",
                        first_singular.to_uppercase(),
                        singular[1..].to_owned()
                    ))
                    .unwrap(),
                    format!("{}{}", first_plural.to_uppercase(), plural[1..].to_owned()),
                ),
                Rule::regex(
                    Regex::new(&format!(
                        "{}(?i){}$",
                        first_singular.to_lowercase(),
                        singular[1..].to_owned()
                    ))
                    .unwrap(),
                    format!("{}{}", first_plural.to_lowercase(), plural[1..].to_owned()),
                ),
            ]
        }
    }

    fn regex_rule(singular: &'static str, plural: &'static str) -> Rule {
        Rule::regex(Regex::new(&format!("(?i){}", singular)).unwrap(), plural.into())
    }

    fn category_rule(singular: &'static str, plural: &'static str, words: &'static [&'static str]) -> Rule {
        Rule::category(singular.into(), plural.into(), words)
    }
}
