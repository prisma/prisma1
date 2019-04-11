#[macro_use]
extern crate log;

#[macro_use]
extern crate rust_embed;

#[macro_use]
extern crate debug_stub_derive;

mod context;
mod data_model;
mod error;
mod req_handlers;
mod serializer;
mod utilities;

use actix_web::{http::Method, server, App, HttpRequest, HttpResponse, Json, Responder};
use context::PrismaContext;
use error::PrismaError;
use req_handlers::{GraphQlBody, GraphQlRequestHandler, PrismaRequest, RequestHandler};
use serde_json;
use std::sync::Arc;

pub type PrismaResult<T> = Result<T, PrismaError>;

#[derive(RustEmbed)]
#[folder = "query-engine/prisma/static_files"]
struct StaticFiles;

#[derive(DebugStub)]
struct RequestContext {
    context: PrismaContext,

    #[debug_stub = "#GraphQlRequestHandler#"]
    graphql_request_handler: GraphQlRequestHandler,
}

#[allow(unused_variables)]
fn main() {
    env_logger::init();

    let context = PrismaContext::new().unwrap();
    let port = context.config.port;
    let request_context = Arc::new(RequestContext {
        context: context,
        graphql_request_handler: GraphQlRequestHandler,
    });

    let sys = actix::System::new("prisma");
    let address = ("0.0.0.0", port);

    server::new(move || {
        App::with_state(Arc::clone(&request_context))
            .resource("/", |r| {
                r.method(Method::POST).with(http_handler);
                r.method(Method::GET).with(playground_handler);
            })
            .resource("/datamodel", |r| r.method(Method::GET).with(data_model_handler))
    })
    .bind(address)
    .unwrap()
    .start();

    println!("Started http server: {}:{}", address.0, address.1);
    let _ = sys.run();
}

fn http_handler((json, req): (Json<Option<GraphQlBody>>, HttpRequest<Arc<RequestContext>>)) -> impl Responder {
    let request_context = req.state();
    let req: PrismaRequest<GraphQlBody> = PrismaRequest {
        body: json.clone().unwrap(),
        path: req.path().into(),
        headers: req
            .headers()
            .iter()
            .map(|(k, v)| (format!("{}", k), v.to_str().unwrap().into()))
            .collect(),
    };

    let result = request_context
        .graphql_request_handler
        .handle(req, &request_context.context);
    serde_json::to_string(&result)
}

fn data_model_handler<T>(_: HttpRequest<T>) -> impl Responder {
    data_model::load_string().unwrap()
}

fn playground_handler<T>(_: HttpRequest<T>) -> impl Responder {
    let index_html = StaticFiles::get("playground.html").unwrap();
    HttpResponse::Ok().content_type("text/html").body(index_html)
}
