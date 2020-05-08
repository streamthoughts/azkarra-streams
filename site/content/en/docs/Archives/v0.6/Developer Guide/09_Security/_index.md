---
date: 2020-02-12
title: "Security"
linkTitle: "Security"
weight: 9
description: >
  How to secure your streams application?
---


## 8.1 Why Azkarra needs to be secured?

{{% alert title="" color="info" %}}
Security is one of the most important concerns (if not the most important) when it comes to deploying an application in production.
{{% /alert %}}

Azkarra allows you to access information about the running Kafka Streams instances (metrics, configuration, states, etc) via the REST APIs. 
Some non-critical data, accessible through these HTTP endpoints, may provide information about your application considered to be private. 
For example, it could be the Kafka topics that are consumed or used internally by your application.

In addition, the state stores of your streams application can be queried. Depending on your business nature, 
you may want to protect some sensitive data returned by the application.

Finally, the REST APIs enable you to perform some operations like stopping and restarting a Kafka Streams instance.
While some of these operations can be useful during development or for some production scenarios, it can be also necessary to not allow them.


Azkarra is composed of three standard components for security :

* **Encryption on the wire**: This allows data transmitted between an Azkarra application and a client application or between two Azkarra applications (i.e when using interactive queries) to be encrypted over the network.
* **Authentication**: This allows client applications or an Azkarra application, accessing the REST APIs, to authenticate to the Azkarra application.
* **Authorization**: This allows you to allow or deny your client applications to perform some operations or to access specific REST resources.

## Headless Mode

Azkarra has a headless mode that can be used to restrict REST APIs usage.

When the headless mode is enabled, Azkarra returns a 401 Unauthorized HTTP response for POST, PUT and DELETE requests. 

The only exception to this rule is that you can still query the state stores of your application.

| Property                            | Type    | Description                                     |
|-------------------------------------|---------|-------------------------------------------------|
|  `azkarra.server.headless`          | boolean | Enable Server Headless mode                     |

## Disabling Web UI

As of Azkarra 0.4, you can also disable the Web UI by using the server configuration :

| Property                            | Type    | Description                                     |
|-------------------------------------|---------|-------------------------------------------------|
|  `azkarra.server.enable.ui`         | boolean | Enable Web UI                                   |

## Encryption using TLS (or SSL)


```
azkarra.server.ssl.enable=true
azkarra.server.ssl.keystore.location="/path/to/server.ks.pkcs12"
azkarra.server.ssl.keystore.type="PKCS12"
azkarra.server.ssl.keystore="password"

azkarra.server.ssl.key.password="password"
azkarra.server.ssl.truststore.location="/path/to/server.ts.pkcs12"
azkarra.server.ssl.truststore.type="PKCS12"
azkarra.server.ssl.truststore="password"
```

SSL hostname verification can be disable using the configuration : 

```
azkarra.server.ssl.ignore.hostname.verification=true
```

This can be useful if, for example, you are using a self-signed certificate during the development phase.


## Authentication

### HTTP Basic Auth

You can configure HTTP Basic Auth to secure access to the Azkarra REST APIs (and Web UI).
Usually, this is the simplest mechanism to implement.

1 ) Configure your Azkarra application with the following properties : 

```
azkarra.server.rest.authentication.mechanism="BASIC_AUTH"
azkarra.server.rest.authentication.realm="AzkarraServer"
azkarra.server.rest.authentication.roles="admin, alice"
```

2 ) Then add the users. Azkarra supports two approaches for doing that : 

#### JAAS Configuration 

Azkarra supports the Java Authentication and Authorization Service ([JAAS](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jaas/JAASRefGuide.html)) and provides a `PropertiesFileLoginModule` 
for loading users from a properties file.

3 ) Create a JAAS configuration file. For example `/etc/azkarra/azkarra-server-jass.conf`:

```
AzkarraServer {
 io.streamthoughts.azkarra.http.security.jaas.spi.PropertiesFileLoginModule required
   file="/etc/azkarra/azkarra.password"
   reloadInterval="60"
   reload="true"
   debug="true";
};
```

N.B: This is important that `azkarra.server.rest.authentication.realm` matches the section with the JAAS file.

4 ) Create a password files. For example `/etc/azkarra/azkarra.password`:

```
admin:MD5:fa0deb5e70ca0c27c04b717a9c60d657,Administrator
alice:alice-secret,Developer
```

N.B: You can passe a password as a hash MD5 (e.g : `echo -n “admin-secret” | md5sum`).

5 ) Pass the JAAS config file location as JVM parameter. For example:

```
-Djava.security.auth.login.config=/path/to/azkarra-server-jaas.conf
```

#### UserIdentityManager 

The approach is to configure a `UserIdentityManager` using the server configuration:
 
```
azkarra.server.user.identity.manager.class
```

Azkarra only provides a simple implementation named `InMemoryUserIdentityManager`.
This class accepts a single configuration property `rest.authentication.users` that must contain the list of users separated by a comma.

You can implement a custom `UserIdentityManager`, for example, to load users from an external database. 

Here is a complete code example using a programmatic configuration style:

```java
@AzkarraStreamsApplication
public class BasicAuthenticationExample {

  public static void main(final String[] args) {

    final Conf serverConfig = ServerConfBuilder.newBuilder()                   
            .setAuthenticationMethod(
                SecurityMechanism.BASIC_AUTH.name()
            )
            .setAuthenticationRealm("AzkarraServer")
            .setAuthenticationRoles("admin, alice")
            .setUserIdentityManager(
                 InMemoryUserIdentityManager.class
            )
            .setAuthenticationUsers(
                 "admin:admin-secret, alice:alice-secret"
            )
            .build();

        new AzkarraApplication()
            .setConfiguration(AzkarraConf.create("application"))
            .enableHttpServer(true, serverConfig)
            .run(args);
    }
}
```

N.B: Basic Authentication does not protect credentials transmitted over the network, you will typically use it in conjunction with TLS (SSL).

### SSL Client Certificate Authentication

Azkarra also supports authentication using client certification (also called SSL Two-Way Authentication). This allows Azkarra to verify the identity of the client by requesting the client to issue a trusted certificate (i.e: a certificate signed by a certificate authority).

If you want to enable Client Certificate authentication, configure your application with the following property : 

```
azkarra.server.rest.authentication.mechanism="CLIENT_CERT_AUTH"
```

## Authorization

Azkarra implements a very simple and pluggable authorization mechanism via 
the interface `AuthorizationManager`.
 
In Azkarra, a user is ALLOW or DENY to perform an **operation** (`GET`, `POST`, `PUT`, etc) on a **resource** (e.g: /streams/:id).

You can configure the authorization manager using the server property :


| Property                            | Type    | Description                                     |
|-------------------------------------|---------|-------------------------------------------------|
|  `azkarra.server.authorization.manager.class`          | string  | The fully qualified name of the class implementing  `AuthorizationManager` to be used to authorize an authenticated user.               |


By default, Azkarra provides a simple authorization manager implementation so-called `SimpleAuthorizationManager`.
This class can be configured using the following properties : 


| Property                            | Type    | Description                                     |
|-------------------------------------|---------|-------------------------------------------------|
|  `azkarra.server.rest.authentication.roles` | string  | The list of users or roles to authorize separated by a comma (default: `*`).  |
|  `azkarra.server.auth.restricted.roles` | string  | The list of users or roles to restrict access  |

N.B: You can use a wildcard(\*) to authorize all authenticated users.

A restricted user will only be allowed to perform GET HTTP requests.


### AzkarraPrincipalBuilder

The principal used to authorize a user depends on the mechanism used for authentication. 
For example, the principal attached to a user authenticated using SSL Client is built from the X509Certificate subject (e.g: `CN=localhost,OU=Unknown,O=Unknown,L=Unknown,ST=Unknown,C=Unknown`).

For customizing the principal name, you can configure an `AzkarraPrincipalBuilder` using the server configuration : 


| Property                            | Type    | Description                                     |
|-------------------------------------|---------|-------------------------------------------------|
|  `azkarra.server.principal.builder.class`  | string  | The fully qualified name of the class implementing `AzkarraPrincipalBuilder`.  |


