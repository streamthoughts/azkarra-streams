---
layout: default
title: "Topology & Dependency Injection"
parent: "Developer Guide"
nav_order: 4
---

# Topology & Dependency Injection

The dependency injection pattern is a common practice in software 
development to get code that is modular and testable.

Azkarra does not implement any advanced dependency injection mechanism.
We think that you can leverage one of the existing IoC frameworks for achieving this.

However, Azkarra implements a simple mechanism based on the factory pattern 
for internally registering and getting concrete class instances.

You can easily use this mechanism to inject services into your topologies.

## Component

In Azkarra, a `TopologyProvider` or any other class of service is called a component. 
A component can be of any type, it is just a java class.

To be managed by the application, the components must be registered to the `AzkarraContext` 
either programmatically, or by external or annotated configuration.

For registering a component, `AzkarraContext` defines the method `AzkarraContext#addComponent()`.
```java
var context = AzkarraContext.create()
context.addComponent(ServiceA.class);
```
The only requirement, when using this method, is that the registered component class
 must defined a no-arg constructor to be later instantiated.
 
In addition, when a component is added to the context is registered for the given class type and all its super types.

Later, we can retrieve an instance of a registered component through the method `AzkarraContext#getComponentForType`.

```java
ServiceA instance = context.getComponentForType(ServiceA.class);
```

The returned object can be a new instance or an instance shared across the application (i.e a singleton).

You should note that when "component-scan" is enable, classes must be annotated with `@Component` 
to be automatically registered.

The `@Singleton` annotation can also be used for declaring a component shared across the application.

## Versioned

A component can be attached to a version by implementing the `Versioned` interface.
This is the case, for example, for the  the `TopologyProvider` interface.

```java
public interface Versioned {
    String version();
}
```

Currently, Azkarra supports the following version schema: 

`<major version>.<minor version>.<incremental version>-<qualifier>`.

Here are some valid examples : 

- `2`
- `2.0`
- `2.0.1`
- `2.0.1-SNAPSHOT`
- `2.0.1-rc3`

Moreover, Azkarra only supports a pre-defined subset of qualifiers: `alpha`, `beta`, `snapshot`, `rc` and `release`.

You can then get a component instance for the latest version registered in the context.

```java
ServiceA instance = context.getLatestComponentForType(ServiceA.class);
```

## ComponentFactory

For building more complex objects, you can implement the so-called `ComponentFactory` interface.

```java 
public class ServiceAFactory implement ComponentFactory<ServiceA> {

    public T make() {
        return new ServiceA();
    }

    public Class<T> getType() {
        return ServiceA.class
    }

    boolean isSingleton() {
        return true;
    }
}
```
Azkarra provides helpers methods for creating a such factory based on a specified class : 

```java
ComponentFactory<ServiceA> f = ComponentFactory.singletonOf(ServiceA.class);
// or
ComponentFactory<ServiceA> f = ComponentFactory.prototypeOf(ServiceA.class);
```

Then, registering a component factory is almost similar to previous example :

```java
var context = AzkarraContext.create()
context.addComponent(new ServiceAFactory());
```

## ComponentModule

When building component with dependencies you can extend the `ComponentModule` class which under the hood implement 
the following interfaces : `ComponentFactory`, `ComponentRegistryAware` and `Configurable`.

```java
public class CustomTopologyProviderModule extends ComponentModule<CustomTopologyProvider> {
    @Override
    public CustomTopologyProvider make() {
        final ServiceA serviceA = getComponentForType(ServiceA.class);
        final ServiceB serviceB = getComponentForType(ServiceB.class);
        return new CustomTopologyProvider(serviceA, serviceB);
    }
}
```

Both `ComponentFactory` and `ComponentModule` can be annotated with `@Component` in order to be automatically registered.

A complete code example is available on the GitHub project [azkarra-examples](https://github.com/streamthoughts/azkarra-streams/tree/master/azkarra-examples/src/main/java/io/streamthoughts/examples/azkarra/dependency)