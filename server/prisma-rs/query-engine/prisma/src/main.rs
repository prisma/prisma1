#[macro_use]
extern crate log;

#[macro_use]
extern crate rust_embed;

#[macro_use]
extern crate debug_stub_derive;

mod context;
mod data_model_loader;
mod dmmf; // Temporary
mod error;
mod exec_loader;
mod req_handlers;
mod serializer;
mod utilities;

use actix_web::{
    http::{Method, StatusCode},
    server, App, HttpRequest, HttpResponse, Json, Responder,
};
use clap::{App as ClapApp, Arg};
use context::PrismaContext;
use error::*;
use req_handlers::{GraphQlBody, GraphQlRequestHandler, PrismaRequest, RequestHandler};
use serde_json;
use std::{env, process, sync::Arc, time::Instant};

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

fn main() {
    let matches = ClapApp::new("Prisma Query Engine")
        .version(env!("CARGO_PKG_VERSION"))
        .arg(
            Arg::with_name("port")
                .short("p")
                .long("port")
                .value_name("port")
                .help("The port the query engine should bind to.")
                .takes_value(true),
        )
        .get_matches();

    let port = matches
        .value_of("port")
        .map(|p| p.to_owned())
        .or_else(|| env::var("PORT").ok())
        .and_then(|p| p.parse::<u16>().ok())
        .unwrap_or_else(|| 4466);

    let now = Instant::now();
    env_logger::init();

    let context = match PrismaContext::new() {
        Ok(ctx) => ctx,
        Err(err) => {
            info!("Encountered error during initialization:");
            err.pretty_print();
            process::exit(1);
        }
    };

    let request_context = Arc::new(RequestContext {
        context,
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
            .resource("/dmmf", |r| r.method(Method::GET).with(dmmf_handler))
            .resource("/status", |r| r.method(Method::GET).with(status_handler))
    })
    .bind(address)
    .unwrap()
    .start();

    trace!("Initialized in {}ms", now.elapsed().as_millis());
    info!("Started http server on {}:{}", address.0, address.1);

    let _ = sys.run();
}

/// Main handler for query engine requests.
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

/// Temporary route to serve a raw v1 SDL string to the playground.
/// Only callable if Prisma was initialized using a v1 data model.
fn data_model_handler(req: HttpRequest<Arc<RequestContext>>) -> impl Responder {
    let request_context = req.state();

    match request_context.context.sdl {
        Some(ref sdl) => HttpResponse::Ok().content_type("application/text").body(sdl),
        None => HttpResponse::with_body(
            StatusCode::UNPROCESSABLE_ENTITY,
            "This endpoint is only callable if Prisma was initialized with a SDL (v1) data model.",
        ),
    }
}

/// Renders the Data Model Meta Format.
/// Only callable if prisma was initialized using a v2 data model.
fn dmmf_handler(req: HttpRequest<Arc<RequestContext>>) -> impl Responder {
    let request_context = req.state();
    match request_context.context.dm {
        Some(ref dm) => {
            let dmmf = dmmf::render_dmmf(dm, &request_context.context.query_schema);
            let serialized = serde_json::to_string(&dmmf).unwrap();

            HttpResponse::Ok().content_type("application/json").body(serialized)
        }
        None => HttpResponse::with_body(
            StatusCode::UNPROCESSABLE_ENTITY,
            "This endpoint is only callable if Prisma was initialized with a v2 data model.",
        ),
    }
}

/// Serves playground html.
fn playground_handler<T>(_: HttpRequest<T>) -> impl Responder {
    let index_html = StaticFiles::get("playground.html").unwrap();
    HttpResponse::Ok().content_type("text/html").body(index_html)
}

/// Simple status endpoint
fn status_handler<T>(_: HttpRequest<T>) -> impl Responder {
    HttpResponse::Ok()
        .content_type("application/json")
        .body("{\"status\": \"ok\"}")
}
