## stub-server

This library is supposed to help you when writing tests for an application. It allows you to write your tests in a black box fashion. The idea is to boot up your full application in a Test and then run your Tests against the HTTP interface. As our applications are usually very small, this should not be too complicated. The problem is that your application will talk to other Services to do its work (see left picture below). 

This is where the `stub-server` lib is going to help you. You can start the StubServer inside your Test and have it serve content you specify. So you can provide stubs for other services for your Test!

<pre>
                                                                           
        without stub-server                       with stub-server
                                                                           
          ┌───────────────┐                       ┌───────────────┐        
          │   Your Test   │                       │   Your Test   │        
          └───────────────┘                       └───────────────┘        
                  │                                       │                
                  ▼                                       ▼                
     ┌─────────────────────────┐             ┌─────────────────────────┐   
     │                         │             │                         │   
     │       Application       │             │       Application       │   
     │                         │             │                         │   
     └─────────────────────────┘             └─────────────────────────┘   
                  │                                       │                
       ┌──────────┴────────┐                              │                
       ▼                   ▼                              ▼                
┌─────────────┐     ┌─────────────┐          ┌────────────────────────┐    
│  Service 1  │     │  Service 2  │          │      stub-server       │    
└─────────────┘     └─────────────┘          └────────────────────────┘    
</pre>

### How to setup

The library is inside our central `libs` directory. Just add add a dependency from your project to the `stub-server` in the root `build.sbt` of the project. You may not need to do this, because the lib is included by `backend-shared`.

```scala
lazy val myProject =
  Project(id = "my-project", base = file("./my-project"))
    .dependsOn(stubServer % "compile")
```

And add the following  import to your test:
```scala
import cool.graph.stub.Import._ // this import is all you need
````

### How to suppress verbose log output
We use the Jetty Server underneath. This server has the super annoying property to configure it's logging via class loading magic. This leads to very verbose logging in Play projects. Here's the fix:

1. Place a file `jetty-logging.properties` in the `src/test/resources` or `src/main/resources` folder of your project.
2. Add the line `org.eclipse.jetty.util.log.class=cool.graph.stub.JustWarningsLogger` to this file.

### How to use

In order to configure the stub server you need to configure the stubs. A Stub declares what to return on a matching request. The following line declares a stub for a `GET` call for the URl path `/path`. All calls to the stub server at the `/path` with the `GET` Http method will be answered by this stub. The stub specifies that the answer should have a status code of 200 and an empty JSON object as answer.

```scala
val myStub = Request("GET", "/path").stub(200, "{}")
```

You probably need to create multiple stubs for your tests. You can then use `withStubServer` to boot up a stub server:

```scala
withStubServer(List(myStub)) { // configure the stub server
  // in this block make sure all your HTTP calls are sent to the stub server
  // if a stub matches the request, its configured response is returned
  // if no stub matches, a 999 is returned
}
```

### Details on Stub Matching

When a request hits the stub server, it has to decide with which stub to respond. There may be multiple matching stubs or none at all. The matching algorithm works like this:

1. A stub has to exactly match the Http method, path and body of the request, otherwise it is removed from the list of candidates.
2. From the list of candidates the stub is selected like this:
  1. If the list of candidates list is empty, respond with a 999 and message that no stub was found for this request.
  2. If there's exactly one candidate left, use this stub.
  3. If there is more than one candidate left, use the query parameters to rank them. A stub may include query parameters. For each stub parameter matching a parameter in the request, the stub is awarded one point rank score. The stub with highest ranking score (== greatest number of matching parameters) is chosen.
  
Consider the following example:

```scala
val myStub = Request("GET", "/path").stub(200, "{}")
val myStubWithParams = Request("GET", "/path", Map("param1" -> "foo").stub(500, "{}")

withStubServer(List(myStub,myStubWithParams)) {
  // in this block a call to /path?param1=foo will result in a 500, as this matches the 2nd stub
  // other calls to this endpoint will result in a 200
}
```

Sometimes you do want to deactivate the matching on request bodies, you can this by calling `ignoreBody`:
```scala
val myStub = Request("POST", "/path").stub(200, "{}").ignoreBody
```

#### Important hint on debugging

It might be necessary to do some debugging of the Stubs. This might be necessary because you forget to stub one call your app is making. Or you are relying on details of the more sophisticated Stub Matching rules mentioned above. So you want to know why the stub server returned a 999. 

**Therefore it is a good idea to print the responses that you are getting from the stub server.** If something fails, the body will include the response from the stub server, which will tell you more details. In the example below if one of the calls to the stub server fails, the response body will contain something like: `{ "message": "Stub not found for request [URL: /some-path?param1=value1] [METHOD: POST]" }`. This will give you quick insight on what stub you might have missed to declare.
