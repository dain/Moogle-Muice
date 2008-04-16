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


package com.google.inject;

import com.google.inject.internal.*;
import static com.google.inject.internal.Objects.nonNull;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Reflection that writes reflected data to a generated class.
 *
 * @author jessewilson@google.com (Jesse Wilson)
 */
class CodeGenReflectionFactory implements Reflection.Factory {
  private static final String generatedCodePackage = "com.google.inject";
  private final String name;
  private Map<Class<?>, ConstructionProxy<?>> constructionProxies
      = new HashMap<Class<?>, ConstructionProxy<?>>();

  /**
   * @param name uniquely identifies this reflection instance. This name needs
   *     to be used both at code generation time and then later at runtime.
   */
  public CodeGenReflectionFactory(String name) {
    this.name = name;
  }

  public Reflection create(ErrorHandler errorHandler,
      ConstructionProxyFactory constructionProxyFactory) {
    Reflection delegate = new RuntimeReflectionFactory()
        .create(errorHandler, constructionProxyFactory);
    return new CodeGenReflection(delegate);
  }


  public Reflection.Factory getRuntimeReflectionFactory() {
    final Reflection reflection;
    try {
      reflection = (Reflection) Class.forName(getGeneratedClassName()).newInstance();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to build reflection class for \""
          + name + "\", has code been generated yet?");
    }

    return new Reflection.Factory() {
      public Reflection create(ErrorHandler errorHandler,
          ConstructionProxyFactory constructionProxyFactory) {
        return reflection;
      }
    };
  }

  private String getGeneratedClassName() {
    return generatedCodePackage + "." + generatedClassSimpleName();
  }

  private class CodeGenReflection implements Reflection {
    private final Reflection delegate;

    private CodeGenReflection(Reflection delegate) {
      this.delegate = nonNull(delegate, "delegate");
    }

    public <T> ConstructionProxy<T> getConstructionProxy(Class<T> implementation) {
      ConstructionProxy<T> result = delegate.getConstructionProxy(implementation);
      constructionProxies.put(implementation, result);
      return result;
    }
  }

  /**
   * Writes generated .java files to {@code generatedSourceDirectory}.
   */
  void writeToFile(File generatedSourceDirectory) throws IOException {
    File directory = generatedSourceDirectory;
    for (String packagePart : new String[] { "com", "google", "inject" }) {
      directory = new File(directory, packagePart);
    }
    directory.mkdirs();
    File sourceFile = new File(directory, generatedClassSimpleName() + ".java");
    Writer writer = new OutputStreamWriter(new FileOutputStream(sourceFile), "ISO-8859-1");
    new ClassWriter(writer).writeClass();
    writer.close();
  }

  private String generatedClassSimpleName() {
    return "Generated_" + name;
  }

  private class ClassWriter {
    final Writer writer;

    ClassWriter(Writer writer) {
      this.writer = writer;
    }

    void writeClass() throws IOException {
      writeLine("// Generated by Guice. Do not edit!");
      writeLine("package %s;", generatedCodePackage);
      writeLine();
      writeLine("import %s;", typeName(Reflection.class));
      writeLine("import %s;", typeName(ConstructionProxy.class));
      writeLine("import %s;", typeName(InvocationTargetException.class));
      writeLine("import %s;", typeName(Parameter.class));
      writeLine("import %s;", typeName(List.class));
      writeLine("import %s;", typeName(Member.class));
      writeLine("import %s;", typeName(Parameter.class));
      writeLine("import %s;", typeName(Nullability.class));
      writeLine("import %s;", typeName(Arrays.class));
      writeLine("import %s;", typeName(Key.class));
      writeLine();
      writeLine("public class %s implements Reflection {", generatedClassSimpleName());
      writeLine();
      writeGetConstructionProxy();
      writeLine();
      writeLine("}");
    }

    String keyLiteral(Key<?> key) {
      if (!(key.getTypeLiteral().getType() instanceof Class)) {
        throw new UnsupportedOperationException("TODO");
      }
      if (key.getAnnotationType() != null) {
        throw new UnsupportedOperationException("TODO");
      }
      return String.format("Key.get(%s.class)", typeName(key.getTypeLiteral().getType()));
    }

    String typeName(Type type) {
      if (type instanceof Class<?>) {
        Class<?> clas = (Class<?>) type;
        StringBuilder result = new StringBuilder();
        result.append(clas.getPackage().getName());
        for (Class<?> enclosing = clas.getEnclosingClass(); enclosing != null;
            enclosing = enclosing.getEnclosingClass()) {
          result.append(".").append(enclosing.getSimpleName());
        }
        result.append(".").append(clas.getSimpleName());
        return result.toString();

      } else {
        throw new UnsupportedOperationException();
      }
    }

    void writeGetConstructionProxy() throws IOException {
      writeLine("  public <T> ConstructionProxy<T> getConstructionProxy(Class<T> implementation) {");

      for (Map.Entry<Class<?>, ConstructionProxy<?>> entry : constructionProxies.entrySet()) {
        String implementation = typeName(entry.getKey());
        writeLine("    if (implementation == %s.class) {", implementation);
        writeLine("      return (ConstructionProxy) new ConstructionProxy<%s>() {", implementation);
        writeLine("        public %s newInstance(final Object... arguments) throws InvocationTargetException {", implementation);
        writeLine("          return new %s(", implementation);
        int argument = 0;
        for (Iterator<Parameter<?>> i = entry.getValue().getParameters().iterator(); i.hasNext(); ) {
          Parameter<?> parameter = i.next();
          String separator = i.hasNext() ? "," : "";
          writeLine("              (%s) arguments[%d]%s", typeName(parameter.getKey().getTypeLiteral().getType()), argument, separator);
          argument++;
        }
        writeLine("          );");
        writeLine("        }");
        writeLine("        public List<Parameter<?>> getParameters() {");
        writeLine("          return Arrays.<Parameter<?>>asList(");
        for (Iterator<Parameter<?>> i = entry.getValue().getParameters().iterator(); i.hasNext(); ) {
          Parameter<?> parameter = i.next();
          String separator = i.hasNext() ? "," : "";
          writeLine("              Parameter.create(%s, %s, Nullability.%s)%s", argument, keyLiteral(parameter.getKey()), parameter.getNullability(), separator);
          argument++;
        }
        writeLine("          );");
        writeLine("        }");
        writeLine("        public Member getMember() {");
        writeLine("          return null;");
        writeLine("        }");
        writeLine("      };");
        writeLine("    }");
      }
      writeLine();
      writeLine("    throw new IllegalArgumentException();");
      writeLine("  }");
    }

    void writeLine(String format, Object... args) throws IOException {
      writer.append(String.format(format, args));
      writeLine();
    }

    void writeLine() throws IOException {
      writer.append("\n");
    }
  }
}
