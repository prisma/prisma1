use slog::Drain;
use slog_async::Async;
use slog_json::Json;
use slog_scope::{logger, GlobalLoggerGuard};
use slog_term::{FullFormat, TermDecorator};

pub struct Logger {
    _scope_guard: GlobalLoggerGuard,
    _log_guard: (),
}

impl Logger {
    /// Builds a new logger. Depending on `LOG_FORMAT` environment variable,
    /// either produces colorful text or JSON.
    pub fn build(application: &'static str) -> Logger {
        match std::env::var("RUST_LOG_FORMAT").as_ref().map(|s| s.as_str()) {
            Ok("devel") => {
                let decorator = TermDecorator::new().build();
                let drain = FullFormat::new(decorator).build().fuse();
                let drain = slog_envlogger::new(drain);
                let drain = Async::new(drain).build().fuse();

                let log = slog::Logger::root(drain, o!("application" => application));

                Self {
                    _scope_guard: slog_scope::set_global_logger(log),
                    _log_guard: slog_stdlog::init().unwrap(),
                }
            }
            _ => {
                let drain = Json::new(std::io::stdout()).add_default_keys().build().fuse();
                let drain = slog_envlogger::new(drain);
                let drain = Async::new(drain)
                    .chan_size(16384)
                    .overflow_strategy(slog_async::OverflowStrategy::Block)
                    .build()
                    .fuse();

                let log = slog::Logger::root(drain, o!("application" => application));

                let this = Self {
                    _scope_guard: slog_scope::set_global_logger(log),
                    _log_guard: slog_stdlog::init().unwrap(),
                };

                std::panic::set_hook(Box::new(|info| {
                    let payload = info
                        .payload()
                        .downcast_ref::<String>()
                        .map(Clone::clone)
                        .unwrap_or_else(|| info.payload().downcast_ref::<&str>().unwrap().to_string());

                    match info.location() {
                        Some(location) => {
                            slog_error!(
                                logger(),
                                "PANIC";
                                "reason" => payload,
                                "file" => location.file(),
                                "line" => location.line(),
                                "column" => location.column(),
                            );
                        }
                        None => {
                            slog_error!(
                                logger(),
                                "PANIC";
                                "reason" => payload,
                            );
                        }
                    }
                }));

                this
            }
        }
    }
}
