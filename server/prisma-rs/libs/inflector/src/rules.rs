use super::Pluralize;
use regex::Regex;

#[derive(Debug)]
pub enum Rule {
    Category(CategoryRule),
    Regex(RegexRule),
}

#[derive(Debug)]
struct CategoryRule {
    singular: String,
    plural: String,
    words: &'static [&'static str],
}

#[derive(Debug)]
struct RegexRule {
    singular: Regex,
    plural: String,
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
        Rule::Regex(RegexRule {
            singular,
            plural,
        })
    }
}

impl Pluralize for Rule {
    fn pluralize(s: String) -> String {
        //StringBuffer buffer = new StringBuffer();
        //			Matcher matcher = singular.matcher(word);
        //			if (matcher.find()) {
        //				matcher.appendReplacement(buffer, plural);
        //				matcher.appendTail(buffer);
        //				return buffer.toString();
        //			}
        //			return null;

        unimplemented!()
    }

    //        String lowerWord = word.toLowerCase();
    //			for (String suffix : list) {
    //				if (lowerWord.endsWith(suffix)) {
    //					if (!lowerWord.endsWith(singular)) {
    //						throw new RuntimeException("Internal error");
    //					}
    //					return word.substring(0, word.length() - singular.length()) + plural;
    //				}
    //			}
    //			return null;
}
