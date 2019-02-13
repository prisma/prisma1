impl Service<http::Request> for HelloWorld {
    type Response = http::Response;
    type Error = http::Error;
    type Future = Box<Future<Item = Self::Response, Error = Self::Error>>;

    fn poll_ready(&mut self) -> Poll<(), Self::Error> {
        Ok(Async::Ready(()))
    }

    fn call(&mut self, req: http::Request) -> Self::Future {
        // Create the HTTP response
        let resp = http::Response::ok().with_body(b"hello world\n");

        // Return the response as an immediate future
        futures::finished(resp).boxed()
    }
}
