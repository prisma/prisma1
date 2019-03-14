mod context;
mod req_handlers;
mod schema;

use actix_web::{http::Method, server, App, HttpRequest, Json, Responder};
use context::PrismaContext;
use lazy_static::lazy_static;
use req_handlers::{GraphQlBody, GraphQlRequestHandler, PrismaRequest, RequestHandler};
use std::env;
use std::sync::Arc;

// lazy_static! {
// }

// fn handler((json, req): (Json<Option<GraphQlBody>>, HttpRequest)) -> impl Responder {
//     // let req: PrismaRequest<GraphQlBody> = (json.clone().unwrap(), req).into();
//     // REQ_HANDLER.handle(req, &CONTEXT);

//     // todo return values
//     ""
// }

struct HttpHandler {
    context: PrismaContext,
    graphql_request_handler: GraphQlRequestHandler,
}

fn handler((json, req): (Json<Option<GraphQlBody>>, HttpRequest<Arc<HttpHandler>>)) -> impl Responder {
    let http_handler = req.state();
    let req: PrismaRequest<GraphQlBody> = PrismaRequest {
        body: json.clone().unwrap(),
        path: req.path().into(),
        headers: req
            .headers()
            .iter()
            .map(|(k, v)| (format!("{}", k), v.to_str().unwrap().into()))
            .collect(),
    };
    http_handler.graphql_request_handler.handle(req, &http_handler.context);

    // todo return values
    ""
}

fn main() {
    // FIXME(katharina): Deduplicate from lib.rs -> separate prisma-core (lib pkg) and prisma (bin pkg)
    let SERVER_ROOT: String = env::var("SERVER_ROOT").unwrap_or_else(|_| String::from("."));

    let http_handler = HttpHandler {
        context: PrismaContext::new(),
        graphql_request_handler: GraphQlRequestHandler,
    };
    let http_handler_arc = Arc::new(http_handler);
    // let handler = http_handler.handle;

    env::set_var("RUST_LOG", "actix_web=debug");
    env::set_var("RUST_BACKTRACE", "1");
    env_logger::init();

    let sys = actix::System::new("prisma");

    server::new(move || {
        App::with_state(Arc::clone(&http_handler_arc)).resource("/", |r| r.method(Method::POST).with(handler))
    })
    .bind("127.0.0.1:8000")
    .unwrap()
    .start();

    println!("Started http server: 127.0.0.1:8000");
    let _ = sys.run();
}
