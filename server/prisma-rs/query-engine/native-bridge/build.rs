use prost_build;

fn main() {
    prost_build::compile_protos(&["protobuf/protocol.proto"], &["protobuf/"]).expect("proto compilation failed");
}
