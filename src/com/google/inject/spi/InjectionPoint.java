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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.internal.Annotations;
import com.google.inject.internal.Errors;
import com.google.inject.internal.ErrorsException;
import com.google.inject.internal.MoreTypes;
import com.google.inject.internal.Nullability;
import com.google.inject.internal.TypeResolver;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A constructor, field or method that can receive injections. Typically this is a member with the
 * {@literal @}{@link Inject} annotation. For non-private, no argument constructors, the member may
 * omit the annotation. 
 *
 * @author crazybob@google.com (Bob Lee)
 */
public final class InjectionPoint implements Serializable {

  private final boolean optional;
  private final Member member;
  private final ImmutableList<Dependency<?>> dependencies;

  private InjectionPoint(Member member,
      ImmutableList<Dependency<?>> dependencies, boolean optional) {
    this.member = member;
    this.dependencies = dependencies;
    this.optional = optional;
  }

  InjectionPoint(TypeResolver typeResolver, Method method) {
    this.member = method;

    Inject inject = method.getAnnotation(Inject.class);
    this.optional = inject.optional();

    this.dependencies = forMember(method, typeResolver, method.getParameterAnnotations());
  }

  InjectionPoint(TypeResolver typeResolver, Constructor<?> constructor) {
    this.member = constructor;
    this.optional = false;
    this.dependencies = forMember(constructor, typeResolver, constructor.getParameterAnnotations());
  }

  InjectionPoint(TypeResolver typeResolver, Field field) {
    this.member = field;

    Inject inject = field.getAnnotation(Inject.class);
    this.optional = inject.optional();

    Annotation[] annotations = field.getAnnotations();

    Errors errors = new Errors(field);
    Key<?> key = null;
    try {
      key = Annotations.getKey(typeResolver.getFieldType(field), field, annotations, errors);
    } catch (ErrorsException e) {
      errors.merge(e.getErrors());
    }
    errors.throwConfigurationExceptionIfErrorsExist();

    this.dependencies = ImmutableList.<Dependency<?>>of(
        newDependency(key, Nullability.allowsNull(annotations), -1));
  }

  private ImmutableList<Dependency<?>> forMember(Member member, TypeResolver typeResolver,
      Annotation[][] paramterAnnotations) {
    Errors errors = new Errors(member);
    Iterator<Annotation[]> annotationsIterator = Arrays.asList(paramterAnnotations).iterator();

    List<Dependency<?>> dependencies = Lists.newArrayList();
    int index = 0;

    for (Type parameterType : typeResolver.getParameterTypes(member)) {
      try {
        Annotation[] parameterAnnotations = annotationsIterator.next();
        Key<?> key = Annotations.getKey(parameterType, member, parameterAnnotations, errors);
        dependencies.add(newDependency(key, Nullability.allowsNull(parameterAnnotations), index));
        index++;
      } catch (ErrorsException e) {
        errors.merge(e.getErrors());
      }
    }

    errors.throwConfigurationExceptionIfErrorsExist();
    return ImmutableList.copyOf(dependencies);
  }

  // This metohd is necessary to create a Dependency<T> with proper generic type information
  private <T> Dependency<T> newDependency(Key<T> key, boolean allowsNull, int parameterIndex) {
    return new Dependency<T>(this, key, allowsNull, parameterIndex);
  }

  /**
   * Returns the injected constructor, field, or method.
   */
  public Member getMember() {
    return member;
  }

  /**
   * Returns the dependencies for this injection point. If the injection point is for a method or
   * constructor, the dependencies will correspond to that member's parameters. Field injection
   * points always have a single dependency for the field itself.
   *
   * @return a possibly-empty list
   */
  public List<Dependency<?>> getDependencies() {
    return dependencies;
  }

  /**
   * Returns true if this injection point shall be skipped if the injector cannot resolve bindings
   * for all required dependencies. Both explicit bindings (as specified in a module), and implicit
   * bindings ({@literal @}{@link com.google.inject.ImplementedBy ImplementedBy}, default
   * constructors etc.) may be used to satisfy optional injection points.
   */
  public boolean isOptional() {
    return optional;
  }

  @Override public boolean equals(Object o) {
    return o instanceof InjectionPoint
        && member.equals(((InjectionPoint) o).member);
  }

  @Override public int hashCode() {
    return member.hashCode();
  }

  @Override public String toString() {
    return MoreTypes.toString(member);
  }

  private Object writeReplace() throws ObjectStreamException {
    Member serializableMember = member != null ? MoreTypes.serializableCopy(member) : null;
    return new InjectionPoint(serializableMember, dependencies, optional);
  }

  /**
   * Returns a new injection point for the injectable constructor of {@code type}.
   *
   * @param type a concrete type with exactly one constructor annotated {@literal @}{@link Inject},
   *     or a no-arguments constructor that is not private.
   * @throws ConfigurationException if there is no injectable constructor, more than one injectable
   *     constructor, or if parameters of the injectable constructor are malformed, such as a
   *     parameter with multiple binding annotations.
   */
  public static InjectionPoint forConstructorOf(Type type) {
    Errors errors = new Errors(type);
    TypeResolver typeResolver = new TypeResolver(type);
    Class<?> rawType = typeResolver.getRawType();

    Constructor<?> injectableConstructor = null;
    for (Constructor<?> constructor : rawType.getDeclaredConstructors()) {
      Inject inject = constructor.getAnnotation(Inject.class);
      if (inject != null) {
        if (inject.optional()) {
          errors.optionalConstructor(constructor);
        }

        if (injectableConstructor != null) {
          errors.tooManyConstructors(rawType);
        }

        injectableConstructor = constructor;
        checkForMisplacedBindingAnnotations(injectableConstructor, errors);
      }
    }

    errors.throwConfigurationExceptionIfErrorsExist();

    if (injectableConstructor != null) {
      return new InjectionPoint(typeResolver, injectableConstructor);
    }

    // If no annotated constructor is found, look for a no-arg constructor instead.
    try {
      Constructor<?> noArgConstructor = rawType.getDeclaredConstructor();

      // Disallow private constructors on non-private classes (unless they have @Inject)
      if (Modifier.isPrivate(noArgConstructor.getModifiers())
          && !Modifier.isPrivate(rawType.getModifiers())) {
        errors.missingConstructor(rawType);
        throw new ConfigurationException(errors.getMessages());
      }

      checkForMisplacedBindingAnnotations(noArgConstructor, errors);
      return new InjectionPoint(typeResolver, noArgConstructor);
    } catch (NoSuchMethodException e) {
      errors.missingConstructor(rawType);
      throw new ConfigurationException(errors.getMessages());
    }
  }

  /**
   * Adds all static method and field injection points on {@code type} to {@code injectionPoints}.
   * All fields are added first, and then all methods. Within the fields, supertype fields are added
   * before subtype fields. Similarly, supertype methods are added before subtype methods.
   *
   * @throws ConfigurationException if there is a malformed injection point on {@code type}, such as
   *      a field with multiple binding annotations. When such an exception is thrown, the valid
   *      injection points are still added to the collection.
   */
  public static void addForStaticMethodsAndFields(Type type, Collection<InjectionPoint> sink) {
    Errors errors = new Errors();
    addInjectionPoints(type, Factory.FIELDS, true, sink, errors);
    addInjectionPoints(type, Factory.METHODS, true, sink, errors);
    errors.throwConfigurationExceptionIfErrorsExist();
  }

  /**
   * Adds all instance method and field injection points on {@code type} to {@code injectionPoints}.
   * All fields are added first, and then all methods. Within the fields, supertype fields are added
   * before subtype fields. Similarly, supertype methods are added before subtype methods.
   *
   * @throws ConfigurationException if there is a malformed injection point on {@code type}, such as
   *      a field with multiple binding annotations. When such an exception is thrown, the valid
   *      injection points are still added to the collection.
   */
  public static void addForInstanceMethodsAndFields(Type type, Collection<InjectionPoint> sink) {
    // TODO (crazybob): Filter out overridden members.
    Errors errors = new Errors();
    addInjectionPoints(type, Factory.FIELDS, false, sink, errors);
    addInjectionPoints(type, Factory.METHODS, false, sink, errors);
    errors.throwConfigurationExceptionIfErrorsExist();
  }

  private static void checkForMisplacedBindingAnnotations(Member member, Errors errors) {
    Annotation misplacedBindingAnnotation = Annotations.findBindingAnnotation(
        errors, member, ((AnnotatedElement) member).getAnnotations());
    if (misplacedBindingAnnotation != null) {
      errors.misplacedBindingAnnotation(member, misplacedBindingAnnotation);
    }
  }

  private static <M extends Member & AnnotatedElement> void addInjectionPoints(Type type,
      Factory<M> factory, boolean statics, Collection<InjectionPoint> injectionPoints,
      Errors errors) {
    if (type == Object.class) {
      return;
    }

    TypeResolver typeResolver = new TypeResolver(type);

    // Add injectors for superclass first.
    Type superType = typeResolver.getSupertype(MoreTypes.getRawType(type).getSuperclass());
    addInjectionPoints(superType, factory, statics, injectionPoints, errors);

    // Add injectors for all members next
    addInjectorsForMembers(typeResolver, factory, statics, injectionPoints, errors);
  }

  private static <M extends Member & AnnotatedElement> void addInjectorsForMembers(
      TypeResolver typeResolver, Factory<M> factory, boolean statics,
      Collection<InjectionPoint> injectionPoints, Errors errors) {
    for (M member : factory.getMembers(typeResolver.getRawType())) {
      if (isStatic(member) != statics) {
        continue;
      }

      Inject inject = member.getAnnotation(Inject.class);
      if (inject == null) {
        continue;
      }

      try {
        injectionPoints.add(factory.create(typeResolver, member, errors));
      } catch (ConfigurationException ignorable) {
        if (!inject.optional()) {
          errors.merge(ignorable.getErrorMessages());
        }
      }
    }
  }

  private static boolean isStatic(Member member) {
    return Modifier.isStatic(member.getModifiers());
  }

  private interface Factory<M extends Member & AnnotatedElement> {
    Factory<Field> FIELDS = new Factory<Field>() {
      public Field[] getMembers(Class<?> type) {
        return type.getDeclaredFields();
      }
      public InjectionPoint create(TypeResolver typeResolver, Field member, Errors errors) {
        return new InjectionPoint(typeResolver, member);
      }
    };

    Factory<Method> METHODS = new Factory<Method>() {
      public Method[] getMembers(Class<?> type) {
        return type.getDeclaredMethods();
      }
      public InjectionPoint create(TypeResolver typeResolver, Method member, Errors errors) {
        checkForMisplacedBindingAnnotations(member, errors);
        return new InjectionPoint(typeResolver, member);
      }
    };

    M[] getMembers(Class<?> type);
    InjectionPoint create(TypeResolver typeResolver, M member, Errors errors);
  }

  private static final long serialVersionUID = 0;
}
