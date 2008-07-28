/**
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject.spi;

import com.google.common.base.Nullable;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.inject.AbstractModule;
import static com.google.inject.Asserts.assertContains;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.name.Names;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.logging.Logger;
import junit.framework.TestCase;

/**
 * @author jessewilson@google.com (Jesse Wilson)
 */
public class SpiBindingsTest extends TestCase {

  public void testBindConstant() {
    checkInjector(
        new AbstractModule() {
          protected void configure() {
            bindConstant().annotatedWith(Names.named("one")).to(1);
          }
        },

        new FailingElementVisitor() {
          @Override public <T> Void visitBinding(Binding<T> binding) {
            assertEquals(Key.get(Integer.class, Names.named("one")), binding.getKey());
            return null;
          }
        }
    );
  }

  public void testToInstanceBinding() {
    checkInjector(
        new AbstractModule() {
          protected void configure() {
            bind(String.class).toInstance("A");
          }
        },

        new FailingElementVisitor() {
          @Override public <T> Void visitBinding(Binding<T> command) {
            assertContains(command.getSource().toString(), "SpiBindingsTest.java");
            assertEquals(Key.get(String.class), command.getKey());
            command.acceptTargetVisitor(new FailingTargetVisitor<T>() {
              @Override public Void visitToInstance(T instance) {
                assertEquals("A", instance);
                return null;
              }
            });
            return null;
          }
        }
    );
  }

  public void testToProviderBinding() {
    final Provider<String> stringProvider = new StringProvider();

    checkInjector(
        new AbstractModule() {
          protected void configure() {
            bind(String.class).toProvider(stringProvider);
          }
        },

        new FailingElementVisitor() {
          @Override public <T> Void visitBinding(Binding<T> command) {
            assertContains(command.getSource().toString(), "SpiBindingsTest.java");
            assertEquals(Key.get(String.class), command.getKey());
            command.acceptTargetVisitor(new FailingTargetVisitor<T>() {
              @Override public Void visitToProvider(Provider<? extends T> provider) {
                assertSame(stringProvider, provider);
                return null;
              }
            });
            return null;
          }
        }
    );
  }

  public void testToProviderKeyBinding() {
    checkInjector(
        new AbstractModule() {
          protected void configure() {
            bind(String.class).toProvider(StringProvider.class);
          }
        },

        new FailingElementVisitor() {
          @Override public <T> Void visitBinding(Binding<T> command) {
            assertContains(command.getSource().toString(), "SpiBindingsTest.java");
            assertEquals(Key.get(String.class), command.getKey());
            command.acceptTargetVisitor(new FailingTargetVisitor<T>() {
              @Override public Void visitToProviderKey(Key<? extends Provider<? extends T>> key) {
                assertEquals(Key.get(StringProvider.class), key);
                return null;
              }
            });
            return null;
          }
        }
    );
  }

  public void testToKeyBinding() {
    final Key<String> aKey = Key.get(String.class, Names.named("a"));
    final Key<String> bKey = Key.get(String.class, Names.named("b"));

    checkInjector(
        new AbstractModule() {
          protected void configure() {
            bind(aKey).to(bKey);
            bind(bKey).toInstance("B");
          }
        },

        new FailingElementVisitor() {
          @Override public <T> Void visitBinding(Binding<T> command) {
            assertContains(command.getSource().toString(), "SpiBindingsTest.java");
            assertEquals(aKey, command.getKey());
            command.acceptTargetVisitor(new FailingTargetVisitor<T>() {
              @Override public Void visitToKey(Key<? extends T> key) {
                assertEquals(bKey, key);
                return null;
              }
            });
            return null;
          }
        },

        new FailingElementVisitor() {
          @Override public <T> Void visitBinding(Binding<T> command) {
            assertEquals(bKey, command.getKey());
            return null;
          }
        }
    );
  }

  public void testToConstructorBinding() {
    checkInjector(
        new AbstractModule() {
          protected void configure() {
            bind(D.class);
          }
        },

        new FailingElementVisitor() {
          @Override public <T> Void visitBinding(Binding<T> command) {
            assertContains(command.getSource().toString(), "SpiBindingsTest.java");
            assertEquals(Key.get(D.class), command.getKey());
            command.acceptTargetVisitor(new FailingTargetVisitor<T>() {
              @Override public Void visitConstructor(Constructor<? extends T> constructor) {
                Constructor<?> expected = D.class.getDeclaredConstructors()[0];
                assertEquals(expected, constructor);
                return null;
              }
            });
            return null;
          }
        }
    );
  }

  public void testConstantBinding() {
    checkInjector(
        new AbstractModule() {
          protected void configure() {
            bindConstant().annotatedWith(Names.named("one")).to(1);
          }
        },

        new FailingElementVisitor() {
          @Override public <T> Void visitBinding(Binding<T> command) {
            assertContains(command.getSource().toString(), "SpiBindingsTest.java");
            assertEquals(Key.get(Integer.class, Names.named("one")), command.getKey());
            command.acceptTargetVisitor(new FailingTargetVisitor<T>() {
              @Override public Void visitConstant(T value) {
                assertEquals((Integer) 1, value);
                return null;
              }
            });
            return null;
          }
        }
    );
  }

  public void testConvertedConstantBinding() {
    Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bindConstant().annotatedWith(Names.named("one")).to("1");
      }
    });

    Binding<Integer> binding = injector.getBinding(Key.get(Integer.class, Names.named("one")));
    assertEquals(Key.get(Integer.class, Names.named("one")), binding.getKey());
    assertContains(binding.getSource().toString(), "SpiBindingsTest.java");
    binding.acceptTargetVisitor(new FailingTargetVisitor<Integer>() {
      @Override public Void visitConvertedConstant(Integer value) {
        assertEquals((Integer) 1, value);
        return null;
      }
    });
  }

  public void testProviderBinding() {
    Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bind(String.class).toInstance("A");
      }
    });

    Key<Provider<String>> providerOfStringKey = new Key<Provider<String>>() {};
    Binding<Provider<String>> binding = injector.getBinding(providerOfStringKey);
    assertEquals(providerOfStringKey, binding.getKey());
    assertContains(binding.getSource().toString(), "SpiBindingsTest.java");
    binding.acceptTargetVisitor(new FailingTargetVisitor<Provider<String>>() {
      @Override public Void visitProviderBinding(Key<?> provided) {
        assertEquals(Key.get(String.class), provided);
        return null;
      }
    });
  }

  public void testScopes() {
    checkInjector(
        new AbstractModule() {
          protected void configure() {
            bind(String.class).annotatedWith(Names.named("a"))
                .toProvider(StringProvider.class).in(Singleton.class);
            bind(String.class).annotatedWith(Names.named("b"))
                .toProvider(StringProvider.class).in(Scopes.SINGLETON);
            bind(String.class).annotatedWith(Names.named("c"))
                .toProvider(StringProvider.class).asEagerSingleton();
            bind(String.class).annotatedWith(Names.named("d"))
                .toProvider(StringProvider.class);
          }
        },

        new FailingElementVisitor() {
          @Override public <T> Void visitBinding(Binding<T> command) {
            assertEquals(Key.get(String.class, Names.named("a")), command.getKey());
            command.acceptScopingVisitor(new FailingBindScopingVisitor() {
              @Override public Void visitScope(Scope scope) {
                // even though we bound with an annotation, the injector always uses instances
                assertSame(Scopes.SINGLETON, scope);
                return null;
              }
            });
            return null;
          }
        },

        new FailingElementVisitor() {
          @Override public <T> Void visitBinding(Binding<T> command) {
            assertEquals(Key.get(String.class, Names.named("b")), command.getKey());
            command.acceptScopingVisitor(new FailingBindScopingVisitor() {
              @Override public Void visitScope(Scope scope) {
                assertSame(Scopes.SINGLETON, scope);
                return null;
              }
            });
            return null;
          }
        },

        new FailingElementVisitor() {
          @Override public <T> Void visitBinding(Binding<T> command) {
            assertEquals(Key.get(String.class, Names.named("c")), command.getKey());
            command.acceptScopingVisitor(new FailingBindScopingVisitor() {
              @Override public Void visitEagerSingleton() {
                return null;
              }
            });
            return null;
          }
        },

        new FailingElementVisitor() {
          @Override public <T> Void visitBinding(Binding<T> command) {
            assertEquals(Key.get(String.class, Names.named("d")), command.getKey());
            command.acceptScopingVisitor(new FailingBindScopingVisitor() {
              @Override public Void visitNoScoping() {
                return null;
              }
            });
            return null;
          }
        }
    );
  }

  public void checkInjector(Module module, Element.Visitor<?>... visitors) {
    Injector injector = Guice.createInjector(module);

    List<Binding<?>> bindings = Lists.newArrayList(
        Iterables.filter(injector.getBindings().values(), isUserBinding));
    orderByKey.sort(bindings);

    assertEquals(bindings.size(), visitors.length);

    for (int i = 0; i < visitors.length; i++) {
      Element.Visitor<?> visitor = visitors[i];
      Binding<?> binding = bindings.get(i);
      binding.acceptVisitor(visitor);
    }
  }

  private final Predicate<Binding<?>> isUserBinding = new Predicate<Binding<?>>() {
    private final ImmutableSet<Key<?>> BUILT_IN_BINDINGS = ImmutableSet.of(
        Key.get(Injector.class), Key.get(Stage.class), Key.get(Logger.class));
    public boolean apply(@Nullable Binding<?> binding) {
      return !BUILT_IN_BINDINGS.contains(binding.getKey());
    }
  };

  private final Ordering<Binding<?>> orderByKey = new Ordering<Binding<?>>() {
    public int compare(Binding<?> a, Binding<?> b) {
      return a.getKey().toString().compareTo(b.getKey().toString());
    }
  };

  private static class StringProvider implements Provider<String> {
    public String get() {
      return "A";
    }
  }

  private static class C { }

  private static class D extends C {
    @Inject public D(Injector unused) { }
  }
}
