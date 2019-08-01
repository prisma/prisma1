use metrics_runtime::{Controller, Receiver, BuilderError, exporters::LogExporter, observers::JsonBuilder};
use log::Level;
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

        Ok(MetricsRecorder { exporter, })
    }

    pub fn run_exporter(&mut self) {
        self.exporter.run();
    }
}
