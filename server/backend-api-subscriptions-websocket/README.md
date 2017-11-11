# Architecture overview

The implementation of subscriptions is split into 2 projects:

The project *backend-api-subscriptions-websocket* is responsible for just maintaining the Websocket connections. It has exactly 2 responsibilities:
* Receive incoming messages from the connected clients and put them onto the Queue `subscriptions-requests`.
* Listen on the queue `subscriptions-responses` and send the contents to the connected clients.

The project *backend-api-simple-subscriptions* is the actual backend. It has the following responsibilties:
* The `SubscriptionSession` actors are responsible for implementing the [Apollo Subscriptions Protocol](https://github.com/apollographql/subscriptions-transport-ws). It makes sure the protocol is followed correctly. If this is the case subscription start and end messages are forwarded to the *SubscriptionManagers*.
* The *SubscriptionsManager* actors are responsible for managing active susbcriptions. They receive a subscription start message and analyze the query and setup the right channels to listen for changes. The `backend-shared` project is responsible for publishing to those channels whenever changes are written to the Database. When a change is received, they execute the query to build the response payload for connected clients and then publish it to the Queue `subscriptions-responses`. Subscriptions are terminated when a Subscription End message is received.


<pre>                                                                                                           
                                                                                                           
                                                                                                           
     ┌────────────────────────────────────────────────────────────────────────────────────────────────┐    
     │backend-api-subscriptions-websocket                                                             │    
     │                                                                                                │    
     │                                 ┌──────────────────────────┐                                   │    
     │                                 │ WebsocketSessionManager  │                                   │    
     │                                 └──────────────────────────┘                                   │    
     │                                               ┼                                                │    
     │                                               │                                                │    
     │                                              ╱│╲                                               │    
     │                                 ┌──────────────────────────┐                                   │    
     │                    ┌────────────│     WebsocketSession     │◀────┐                             │    
     │                    │            └──────────────────────────┘     │                             │    
     └────────────────────┼─────────────────────────────────────────────┼─────────────────────────────┘    
                          ▼                                             │                                  
            .───────────────────────────.                 .───────────────────────────.                    
           (  Q: subscriptions-requests  )               ( Q: subscriptions-responses  )◀──────────┐       
            `───────────────────────────'                 `───────────────────────────'            │       
                          │                                                                        │       
     ┌────────────────────┼────────────────────────────────────────────────────────────────────────┼──┐    
     │                    │                                                                        │  │    
     │                    │                                                                        │  │    
     │                    │                                                                        │  │    
     │                    ▼                                                                        │  │    
     │    ┌──────────────────────────────┐                 ┌──────────────────────────────┐        │  │    
     │    │  SubscriptionSessionManager  │       ┌────────▶│     SubscriptionsManager     │        │  │    
     │    └──────────────────────────────┘       │         └──────────────────────────────┘        │  │    
     │                    ┼                      │                         ┼                       │  │    
     │                    │                      │                         │                       │  │    
     │                    │                      │                         │                       │  │    
     │                   ╱│╲                     │                        ╱│╲                      │  │    
     │      ┌──────────────────────────┐         │       ┌───────────────────────────────────┐     │  │    
     │      │   SubscriptionSession    │─────────┘       │  SubscriptionsManagerForProject   │     │  │    
     │      └──────────────────────────┘                 └───────────────────────────────────┘     │  │    
     │                                                                     ┼                       │  │    
     │                                                                     │                       │  │    
     │                                                                     │                       │  │    
     │                                                                    ╱│╲                      │  │    
     │                                                   ┌───────────────────────────────────┐     │  │
     │                                                   │   SubscriptionsManagerForModel    │─────┘  │
     │                                                   └───────────────────────────────────┘        │
     │backend-api-simple-subscriptions                                     ▲                          │    
     └─────────────────────────────────────────────────────────────────────┼──────────────────────────┘    
                                                                           │                               
                                                                           │                               
                                                             .───────────────────────────.                 
                                                            (      MutationChannels       )                
                                                             `───────────────────────────'                 
                                                                           ▲                               
     ┌─────────────────────────────────────────────────────────────────────┼──────────────────────────────┐
     │                                                                     │                              │
     │                                                                     │                              │
     │                                                                                                    │
     │                                                                                                    │
     │                                                                                                    │
     │client-shared  PublishSubscriptionEvent mutaction                                                   │
     └────────────────────────────────────────────────────────────────────────────────────────────────────┘</pre>


## Current Problems

* If a Websocket server crashes the corresponding subscriptions are not stopped in the backend.
* The subscriptions backend is currently not horizontally scalable.