use log::{SetLoggerError, LevelFilter, Record, Level, Metadata, Log, set_logger, set_max_level, max_level};
use std::env;

static LOGGER: SimpleLogger = SimpleLogger;

pub struct SimpleLogger;

impl Log for SimpleLogger {
    fn enabled(&self, metadata: &Metadata) -> bool {
        metadata.level() <= max_level()
    }

    fn log(&self, record: &Record) {
        if self.enabled(record.metadata()) {
            println!("[{}][JDBC-rs] {}", record.level(), record.args());
        }
    }

    fn flush(&self) {}
}

pub fn init() {
    let level = match env::var("LOG_LEVEL") {
        Ok(x) => x.to_uppercase(),
        Err(e) => String::from("INFO"),
    };

    let level_filter = match level.as_ref() {
        "TRACE" => LevelFilter::Trace,
        "DEBUG" => LevelFilter::Debug,
        "INFO" => LevelFilter::Info,
        "ERROR" => LevelFilter::Error,
        "WARNING" => LevelFilter::Warn,
        _ => LevelFilter::Info,
    };

    set_logger(&LOGGER).map(|()| set_max_level(level_filter)).unwrap();
}