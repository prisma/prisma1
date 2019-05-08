lazy_static! {
    pub static ref CATEGORY_EX_ICES: Vec<&'static str> = vec![
        "codex", "murex", "silex",
    ];

    pub static ref CATEGORY_IX_ICES: Vec<&'static str> = vec![
        "radix", "helix",
    ];

    pub static ref CATEGORY_UM_A: Vec<&'static str> = vec![
        "bacterium", "agendum", "desideratum", "erratum", "stratum", "datum", "ovum",
        "extremum", "candelabrum",
    ];

    // Always us -> i
    pub static ref CATEGORY_US_I: Vec<&'static str> = vec![
        "alumnus", "alveolus", "bacillus", "bronchus", "locus", "nucleus", "stimulus",
        "meniscus", "thesaurus",
    ];

    pub static ref CATEGORY_ON_A: Vec<&'static str> = vec![
        "criterion", "perihelion", "aphelion", "phenomenon", "prolegomenon", "noumenon",
        "organon", "asyndeton", "hyperbaton",
    ];

    pub static ref CATEGORY_A_AE: Vec<&'static str> = vec!["alumna", "alga", "vertebra", "persona"];

    // Always o -> os
    pub static ref CATEGORY_O_OS: Vec<&'static str> = vec![
        "albino", "archipelago", "armadillo", "commando", "crescendo", "fiasco",
        "ditto", "dynamo", "embryo", "ghetto", "guano", "inferno", "jumbo", "lumbago",
        "magneto", "manifesto", "medico", "octavo", "photo", "pro", "quarto", "canto",
        "lingo", "generalissimo", "stylo", "rhino", "casino", "auto", "macro", "zero",
    ];

    // Classical o -> i  (normally -> os)
    pub static ref CATEGORY_O_I: Vec<&'static str> = vec![
        "solo", "soprano", "basso", "alto", "contralto", "tempo", "piano", "virtuoso",
    ];

    pub static ref CATEGORY_EN_INA: Vec<&'static str> = vec![
        "stamen", "foramen", "lumen",
    ];

    // -a to -as (anglicized) or -ata (classical)
    pub static ref CATEGORY_A_ATA: Vec<&'static str> = vec![
        "anathema", "enema", "oedema", "bema", "enigma", "sarcoma", "carcinoma", "gumma",
        "schema", "charisma", "lemma", "soma", "diploma", "lymphoma", "stigma", "dogma",
        "magma", "stoma", "drama", "melisma", "trauma", "edema", "miasma",
    ];

    pub static ref CATEGORY_IS_IDES: Vec<&'static str> = vec![
        "iris", "clitoris"
    ];

    // -us to -uses (anglicized) or -us (classical)
    pub static ref CATEGORY_US_US: Vec<&'static str> = vec![
        "apparatus", "impetus", "prospectus", "cantus", "nexus", "sinus", "coitus", "plexus",
        "status", "hiatus",
    ];

    pub static ref CATEGORY_NONE_I: Vec<&'static str> = vec![
        "afreet", "afrit", "efreet",
    ];

    pub static ref CATEGORY_NONE_IM: Vec<&'static str> = vec![
        "cherub", "goy", "seraph",
    ];

    pub static ref CATEGORY_EX_EXES: Vec<&'static str> = vec![
        "apex", "latex", "vertex", "cortex", "pontifex", "vortex", "index", "simplex",
    ];

    pub static ref CATEGORY_IX_IXES: Vec<&'static str> = vec![
        "appendix",
    ];

    pub static ref CATEGORY_S_ES: Vec<&'static str> = vec![
        "acropolis", "chaos", "lens", "aegis", "cosmos", "mantis", "alias", "dais", "marquis",
        "asbestos", "digitalis", "metropolis", "atlas", "epidermis", "pathos", "bathos", "ethos",
        "pelvis", "bias", "gas", "polis", "caddis", "glottis", "rhinoceros", "cannabis", "glottis",
        "sassafras", "canvas", "ibis", "trellis",
    ];

    pub static ref CATEGORY_MAN_MANS: Vec<&'static str> = vec![
        "human", "Alabaman", "Bahaman", "Burman", "German", "Hiroshiman", "Liman", "Nakayaman",
        "Oklahoman", "Panaman", "Selman", "Sonaman", "Tacoman", "Yakiman", "Yokohaman", "Yuman",
    ];
}
