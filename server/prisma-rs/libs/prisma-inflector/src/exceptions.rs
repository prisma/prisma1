lazy_static! {
    pub static ref UNCOUNTABLE: Vec<&'static str> = vec![
        // endings
        "fish", "ois", "sheep", "deer", "pox", "itis",

        // words
        "bison", "flounder", "pliers", "bream",
        "gallows", "proceedings", "breeches", "graffiti", "rabies",
        "britches", "headquarters", "salmon", "carp", "herpes",
        "scissors", "chassis", "high-jinks", "sea-bass", "clippers",
        "homework", "series", "cod", "innings", "shears",
        "contretemps", "jackanapes", "species", "corps", "mackerel",
        "swine", "debris", "measles", "trout", "diabetes", "mews",
        "tuna", "djinn", "mumps", "whiting", "eland", "news",
        "wildebeest", "elk", "pincers", "sugar",
    ];

    pub static ref STANDARD_IRREGULAR: Vec<(&'static str, &'static str)> = vec![
        ("child", "children"),        // classical
        ("ephemeris", "ephemerides"), // classical
        ("mongoose", "mongoose"),     // anglicized
        ("mythos", "mythoi"),         // classical
        ("soliloquy", "soliloquies"), // anglicized
        ("trilby", "trilbys"),        // anglicized
        ("genus", "genera"),          // classical
        ("quiz", "quizzes"),
    ];

    pub static ref IRREGULAR_ANGLICIZED: Vec<(&'static str, &'static str)> = vec![
        ("beef", "beefs"),
        ("brother", "brothers"),
        ("cow", "cows"),
        ("genie", "genies"),
        ("money", "moneys"),
        ("octopus", "octopuses"),
        ("opus", "opuses"),
    ];

    pub static ref IRREGULAR_CLASSICAL: Vec<(&'static str, &'static str)> = vec![
        ("beef", "beeves"),
        ("brother", "brethren"),
        ("cos", "kine"),
        ("genie", "genii"),
        ("money", "monies"),
        ("octopus", "octopodes"),
        ("opus", "opera"),
    ];

    pub static ref IRREGULAR_SUFFIX_INFLECTIONS: Vec<(&'static str, &'static str)> = vec![
        ("man$", "men"),
        ("([lm])ouse$", "${1}ice"),
        ("tooth$", "teeth"),
        ("goose$", "geese"),
        ("foot$", "feet"),
        ("zoon$", "zoa"),
        ("([csx])is$", "${1}es"),
    ];

    pub static ref MODERN_CLASSICAL_INFLECTIONS: Vec<(&'static str, &'static str)> = vec![
        ("trix$", "trices"),
        ("eau$", "eaux"),
        ("ieu$", "ieux"),
        ("(..[iay])nx$", "${1}nges"),
    ];

    pub static ref ADDITIONAL_SUFFIX_INFLECTIONS: Vec<(&'static str, &'static str)> = vec![
        // The suffixes -ch, -sh, and -ss all take -es in the plural (churches, classes, etc)...
        (r"([cs])h$", "${1}hes"),
        ("ss$", "sses"),

        // Certain words ending in -f or -fe take -ves in the plural (lives, wolves, etc)...
        ("([aeo]l)f$", "${1}ves"),
        ("([^d]ea)f$", "${1}ves"),
        ("(ar)f$", "${1}ves"),
        ("([nlw]i)fe$", "${1}ves"),

        // Words ending in -y take -ys
        ("([aeiou])y$", "${1}ys"),
        ("y$", "ies"),
    ];

}
