use actix::System;
use actix_web::{http::Method, server, App, HttpRequest, Json, Responder};

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

mod req_handlers;
use req_handlers::{PrismaRequest, GraphQlBody};

fn handle_stuff((json, req): (Json<GraphQlBody>, HttpRequest)) -> impl Responder {
    let preq: PrismaRequest<GraphQlBody> = (json.into_inner(), req).into();

    ""
}

fn main() {
    let sys = System::new("prisma");

    server::new(|| App::new().resource("/", |r| r.method(Method::POST).with(handle_stuff)))
        .bind("127.0.0.1:8000")
        .unwrap()
        .start();

    println!("Started http server: 127.0.0.1:8000");
    let _ = sys.run();
}
