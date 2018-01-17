import * as nock from 'nock'

export default () => {


  nock('http://localhost:60000', {"encodedQueryParams":true})
    .post('/system', {"query":"\n            {\n              viewer {\n                id\n              }\n            }\n            "})
    .reply(200, {"data":{"viewer":{"id":"static-viewer-id"}}}, [ 'Server',
      'nginx/1.13.3',
      'Date',
      'Tue, 03 Oct 2017 09:42:52 GMT',
      'Content-Type',
      'application/json',
      'Content-Length',
      '45',
      'Connection',
      'close',
      'Request-Id',
      'eu-west-1:system:cj8bf1jgc001d01830nh7tnyf' ]);


  const payload = {"query":"\n      mutation ($token: String!) {\n        authenticateCustomer(input: {\n          auth0IdToken: $token\n        }) {\n          token\n          user {\n            id\n          }\n        }\n      }\n      ","variables":{"token":"MuchTokenSuchMasterWow"}}
  nock('http://localhost:60000', {"encodedQueryParams":true})
    .post('/system', data => data.query === payload.query)
    .reply(200, {"data":{"authenticateCustomer":{"token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MDcwMjM3NzMsImNsaWVudElkIjoiY2o4YTAxZHN1MDAwMDAxMjM1aWF1aTFoYiJ9.WscmbACu0HqPEDSk_U66TNOskGddmt2plJAew6XCyNw","user":{"id":"cj8a01dsu000001235iaui1hb"}}}}, [ 'Server',
      'nginx/1.13.3',
      'Date',
      'Tue, 03 Oct 2017 09:42:53 GMT',
      'Content-Type',
      'application/json',
      'Content-Length',
      '245',
      'Connection',
      'close',
      'Request-Id',
      'eu-west-1:system:cj8bf1l0m001e0183qxzbudax' ]);
}
