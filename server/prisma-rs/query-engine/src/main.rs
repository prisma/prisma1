use actix_web::{http::Method, server, App, HttpRequest, Json, Responder};
use lazy_static::lazy_static;
use std::env;

#[macro_use]
extern crate prost_derive;

mod ast;
mod context;
mod cursor_condition;
mod data_resolver;
mod database_executor;
mod database_mutaction_executor;
mod node_selector;
mod ordering;
mod protobuf;
mod query_builder;
mod req_handlers;
mod schema;
use prisma_common::{config::PrismaConfig, error::Error};
use protobuf::{ProtoBufEnvelope, ProtoBufInterface};
use std::{fs::File, slice};

lazy_static! {
    pub static ref PBI: ProtoBufInterface = ProtoBufInterface::new(&CONFIG);
    pub static ref SERVER_ROOT: String = env::var("SERVER_ROOT").unwrap_or_else(|_| String::from("."));
    pub static ref CONFIG: PrismaConfig = {
        let path = format!("{}/prisma-rs/config/prisma.yml", *SERVER_ROOT);
        serde_yaml::from_reader(File::open(path).unwrap()).unwrap()
    };
}

macro_rules! data_interface {
    ($($function:ident),*) => (
        pub trait ExternalInterface {
            $(
                fn $function(&self, payload: &mut [u8]) -> Vec<u8>;
            )*
        }

        $(
            #[no_mangle]
            pub unsafe extern "C" fn $function(data: *mut u8, len: usize) -> *mut ProtoBufEnvelope {
                let payload = slice::from_raw_parts_mut(data, len);
                let response_payload = PBI.$function(payload);

                ProtoBufEnvelope::from(response_payload).into_boxed_ptr()
            }
        )*
    )
}

data_interface!(
    get_node_by_where,
    get_nodes,
    get_related_nodes,
    get_scalar_list_values_by_node_ids,
    execute_raw,
    count_by_model,
    count_by_table
);

use context::PrismaContext;
use req_handlers::{GraphQlBody, GraphQlRequestHandler, PrismaRequest, RequestHandler};

lazy_static! {
    pub static ref CONTEXT: PrismaContext = PrismaContext::new();
    pub static ref REQ_HANDLER: GraphQlRequestHandler = GraphQlRequestHandler;

    // FIXME(katharina): Deduplicate from lib.rs -> separate prisma-core (lib pkg) and prisma (bin pkg)
    // pub static ref SERVER_ROOT: String = env::var("SERVER_ROOT").unwrap_or_else(|_| String::from("."));
}

fn handler((json, req): (Json<Option<GraphQlBody>>, HttpRequest)) -> impl Responder {
    let req: PrismaRequest<GraphQlBody> = (json.clone().unwrap(), req).into();
    REQ_HANDLER.handle(req, &CONTEXT);

    // todo return values
    ""
}

fn main() {
    env::set_var("RUST_LOG", "actix_web=debug");
    env::set_var("RUST_BACKTRACE", "1");
    env_logger::init();

    let ast = schema::load_schema();
    println!("{:#?}", ast);

    let sys = actix::System::new("prisma");

    server::new(|| App::new().resource("/", |r| r.method(Method::POST).with(handler)))
        .bind("127.0.0.1:8000")
        .unwrap()
        .start();

    println!("Started http server: 127.0.0.1:8000");
    let _ = sys.run();
}
