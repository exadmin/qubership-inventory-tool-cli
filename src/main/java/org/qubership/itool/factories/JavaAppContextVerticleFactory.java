/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.itool.factories;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.context.FlowContext;

import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.impl.JavaVerticleFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.VerticleFactory;

import static org.qubership.itool.utils.ConfigProperties.CONFIG_PATH_POINTER;


public class JavaAppContextVerticleFactory extends JavaVerticleFactory {
    private static final Logger LOG = LoggerFactory.getLogger(JavaAppContextVerticleFactory.class);

    protected FlowContext applicationContext;
    protected Path customTaskPath;
    protected ClassLoader taskClassLoader;

    public JavaAppContextVerticleFactory(FlowContext applicationContext, JsonObject config) {
        this.applicationContext = applicationContext;

        ClassLoader parentCl = Thread.currentThread().getContextClassLoader();
        if (parentCl == null) {
            parentCl = getClass().getClassLoader();
        }

        customTaskPath = Path.of(
                StringUtils.defaultString(ConfigUtils.getConfigValue(CONFIG_PATH_POINTER, config), "."),
                "default", "tasks");
        if (! Files.isDirectory(customTaskPath)) {
            LOG.warn("Path {} does not exist or is not directory", customTaskPath);
            taskClassLoader = parentCl;
            customTaskPath = null;
            return;
        }
        try {
            taskClassLoader = new URLClassLoader(new URL[] { customTaskPath.toUri().toURL() }, parentCl);
        } catch (MalformedURLException e) { // Should not happen
            LOG.error("Failed to convert Path " + customTaskPath + " to URL", e);
            taskClassLoader = parentCl;
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public void createVerticle(String verticleName, ClassLoader parentClassLoader, Promise<Callable<Verticle>> promise) {
      LOG.debug(verticleName);
      verticleName = VerticleFactory.removePrefix(verticleName);
      Class<Verticle> clazz = null;
      try {
          clazz = (Class<Verticle>) taskClassLoader.loadClass(verticleName);
      } catch (ClassNotFoundException ignore) {
          // Fall thru
      }
      if (clazz == null) {
          try {
//              if (verticleName.endsWith(".java")) {
//                  CompilingClassLoader compilingLoader = new CompilingClassLoader(taskClassLoader, verticleName);
//                  String className = compilingLoader.resolveMainClassName();
//                  clazz = (Class<Verticle>) compilingLoader.loadClass(className);
//              } else
              clazz = (Class<Verticle>) parentClassLoader.loadClass(verticleName);
          } catch (ClassNotFoundException e) {
              promise.fail(e);
              return;
          }
      }

      final Class<Verticle> finalClazz = clazz;
      promise.complete(() -> {
          Verticle verticle = finalClazz.getDeclaredConstructor().newInstance();
          this.applicationContext.initialize(verticle);
          return verticle;
      });
    }

    @Override
    public int order() {
        return -10;
    }

    /* Get a default class loader configured for both pre-packaged and custom tasks */
    public ClassLoader getTaskClassLoader() {
        return taskClassLoader;
    }

    /** Get a directory with custom task implementation. May be used for scanning for tasks
     * packaged as .java files.
     *
     * @return Either directory or {@code null}
     */
    public Path getCustomTaskPath() {
        return customTaskPath;
    }

}
