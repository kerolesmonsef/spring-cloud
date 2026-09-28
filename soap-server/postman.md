# Postman — createUserRequest / updateUserRequest

Server port: `8094` (from `application.properties`). Endpoint: `/ws`. Method: always `POST`. Header: `Content-Type: text/xml`.

## curl (paste into Postman via Import → Raw text)

```bash
curl -X POST http://localhost:8094/ws \
  -H "Content-Type: text/xml" \
  -d '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:use="http://keroles.com/soapserver/users">
   <soapenv:Header/>
   <soapenv:Body>
      <use:createUserRequest>
         <use:name>John Doe</use:name>
         <use:email>john@example.com</use:email>
      </use:createUserRequest>
   </soapenv:Body>
</soapenv:Envelope>'
```

## Raw XML body (Postman: Body → raw → XML)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:use="http://keroles.com/soapserver/users">
   <soapenv:Header/>
   <soapenv:Body>
      <use:createUserRequest>
         <use:name>John Doe</use:name>
         <use:email>john@example.com</use:email>
      </use:createUserRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

Namespace `use` must match `users.xsd` targetNamespace (`http://keroles.com/soapserver/users`), not the bean name.

---

# Postman — updateUserRequest

## curl

```bash
curl -X POST http://localhost:8094/ws \
  -H "Content-Type: text/xml" \
  -d '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:use="http://keroles.com/soapserver/users">
   <soapenv:Header/>
   <soapenv:Body>
      <use:updateUserRequest>
         <use:id>1</use:id>
         <use:name>John Doe Updated</use:name>
         <use:email>john.updated@example.com</use:email>
      </use:updateUserRequest>
   </soapenv:Body>
</soapenv:Envelope>'
```

## Raw XML body (Postman: Body → raw → XML)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:use="http://keroles.com/soapserver/users">
   <soapenv:Header/>
   <soapenv:Body>
      <use:updateUserRequest>
         <use:id>1</use:id>
         <use:name>John Doe Updated</use:name>
         <use:email>john.updated@example.com</use:email>
      </use:updateUserRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

Response is just `success: boolean` (no user object) — see `updateUserResponse` in `users.xsd:64-70`. `id` must exist already (created via `createUserRequest` first).
