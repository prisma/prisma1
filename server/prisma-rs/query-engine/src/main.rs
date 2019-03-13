use actix::System;
use actix_web::{http::Method, server, App, HttpRequest, HttpResponse, Responder};

mod req_handlers;

struct GraphQl {}

fn handle_stuff(req: &HttpRequest) -> impl Responder {
    ""
}

fn main() {
    let sys = System::new("prisma");

    server::new(|| App::new().resource("/", |r| r.method(Method::GET).f(handle_stuff)))
        .bind("127.0.0.1:8000")
        .unwrap()
        .start();

    println!("Started http server: 127.0.0.1:8000");
    let _ = sys.run();
}
