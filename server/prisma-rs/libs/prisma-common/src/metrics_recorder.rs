use log::Level;
use metrics::{Recorder, SetRecorderError};
use metrics_core::Key;
use metrics_runtime::{exporters::LogExporter, observers::JsonBuilder, BuilderError, Controller, Receiver};
use std::time::Duration;

pub struct MetricsRecorder {
    exporter: LogExporter<Controller, JsonBuilder>,
}

impl MetricsRecorder {
    pub fn install() -> Result<Self, BuilderError> {
        let receiver = Receiver::builder().build()?;

        let exporter = LogExporter::new(
            receiver.get_controller(),
            JsonBuilder::new(),
            Level::Info,
            Duration::from_secs(5),
        );

        receiver.install();

        Ok(MetricsRecorder { exporter })
    }

    pub fn run_exporter(&mut self) {
        self.exporter.run();
    }
}

pub struct StupidLogRecorder;

impl StupidLogRecorder {
    pub fn install() -> Result<(), SetRecorderError> {
        metrics::set_recorder(&Self)
    }
}

impl Recorder for StupidLogRecorder {
    fn record_counter(&self, key: Key, value: u64) {
        info!("counter '{}' -> {}", key.name(), value);
    }

    fn record_gauge(&self, key: Key, value: i64) {
        info!("gauge '{}' -> {}", key.name(), value);
    }

    fn record_histogram(&self, key: Key, value: u64) {
        let duration = Duration::from_nanos(value);

        info!(
            "histogram '{}' -> (sec: {}, millis: {}, micros: {})",
            key.name(),
            duration.as_secs(),
            duration.as_millis(),
            duration.as_micros()
        );
    }
}
