mod context;
mod req_handlers;
mod schema;

use actix_web::{http::Method, server, App, HttpRequest, Json, Responder};
use context::PrismaContext;
use lazy_static::lazy_static;
use req_handlers::{GraphQlBody, GraphQlRequestHandler, PrismaRequest, RequestHandler};
use std::env;

lazy_static! {
    pub static ref CONTEXT: PrismaContext = PrismaContext::new();
    pub static ref REQ_HANDLER: GraphQlRequestHandler = GraphQlRequestHandler;

    // FIXME(katharina): Deduplicate from lib.rs -> separate prisma-core (lib pkg) and prisma (bin pkg)
    pub static ref SERVER_ROOT: String = env::var("SERVER_ROOT").unwrap_or_else(|_| String::from("."));
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

    let sys = actix::System::new("prisma");

    server::new(|| App::new().resource("/", |r| r.method(Method::POST).with(handler)))
        .bind("127.0.0.1:8000")
        .unwrap()
        .start();

    println!("Started http server: 127.0.0.1:8000");
    let _ = sys.run();
}
