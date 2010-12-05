# Overview

This fork of Guice allows for the com.google.inject.spi.Dependency metadata
to be injected into any instance.  This is particularly useful for providers
that need access to the binding key or injection point.

# Example

Say your software uses the following custom logging code:

    @ProvidedBy(MyLoggerProvider.class)
    public static class MyLogger {
      public final String name;

      public MyLogger(String name) {
        this.name = name;
      }

      public void log(String message) {
        System.out.println(name + ": " + message);
      }
    }

With access to the Dependency metadata, you can write a provider that names
the logger based on the class the logger is being injected into, as follows:

    public static class MyLoggerProvider implements Provider<MyLogger> {
      private final Dependency<?> dependency;

      @Inject
      public MyLoggerProvider(Dependency<?> dependency) {
        this.dependency = dependency;
      }

      public MyLogger get() {
        InjectionPoint injectionPoint = dependency.getInjectionPoint();
        if (injectionPoint == null) {
          return new MyLogger("anonymous");
        }
        return new MyLogger(injectionPoint.getMember().getDeclaringClass().getSimpleName());
      }
    }

This quick test demonstrates the logger in action:

    public static class MyService {
      @Inject
      MyLogger logger;
    }

    public void testExampleUsage() {
      DependencyInjectionTest.MyService myService = Guice.createInjector().getInstance(MyService.class);
      assertNotNull(myService);
      assertNotNull(myService.logger);
      assertEquals(myService.logger.name, "MyService");
    }


# Issues with @Singleton

Injection of Dependency metadata for Singletons should be used with caution.  Scopes like
@Singleton will cache an instance after creation.  This means the
injection of Dependency metadata will only happen once, and the contained InjectionPoint
will only reflect the first place the instance is to be injected.  On the other
hand, if the provider is only interested in the Dependency key, the key is stable across
all injections of the instance.

# Implementation Details

Injection of the Dependency object is handled by a new InternalFactory to that has been
added to the Guice internals (see InjectorShell).  This factory simply provides the
Depandency from the InternalContext to the injection system, so it can be injected
at any valid Guice injection point.  This is the exact same strategy used by Guice to
inject java.util.Logger instances.

Additionally, the InternalContext has been extended to maintain a full stack
of Dependency objects instead of just the current Dependency.  This allows the
InternalFactory to fetch the parent of the dependency of the Dependency object.