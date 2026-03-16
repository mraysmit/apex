package dev.mars.apex.core.script;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.concurrent.*;

/**
 * Executes named functions on compiled Groovy script classes with timeout enforcement.
 * Handles positional argument passing and numeric coercion.
 */
public class ScriptExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ScriptExecutor.class);

    private final ExecutorService executor;

    public ScriptExecutor() {
        // Use virtual threads to isolate long-running script calls without creating
        // unbounded platform thread pressure under concurrency.
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Execute a function on a compiled Groovy class.
     *
     * @param compiledClass The compiled Groovy class
     * @param functionName  Function to invoke; null means "run"
     * @param args          Positional arguments
     * @param timeoutMs     Max execution time in milliseconds
     * @return The function's return value
     * @throws ScriptExecutionTimeoutException if execution exceeds timeout
     * @throws ScriptExecutionException if the function throws or cannot be invoked
     */
    public Object execute(Class<?> compiledClass, String functionName, Object[] args, long timeoutMs) {
        String targetFunction = (functionName != null && !functionName.isEmpty()) ? functionName : "run";

        Future<Object> future = executor.submit(() -> invokeFunction(compiledClass, targetFunction, args));

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.info("Script function '{}' timed out after {}ms", targetFunction, timeoutMs);
            throw new ScriptExecutionTimeoutException(
                    "Script function '" + targetFunction + "' exceeded timeout of " + timeoutMs + "ms");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ScriptExecutionException see) {
                logger.info("Script function '{}' failed: {}", targetFunction, see.getMessage());
                throw see;
            }
            logger.info("Script function '{}' failed: {}", targetFunction, cause.getMessage());
            throw new ScriptExecutionException(
                    "Script function '" + targetFunction + "' failed: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptExecutionException(
                    "Script function '" + targetFunction + "' was interrupted", e);
        }
    }

    /**
     * Shutdown the executor service.
     */
    public void shutdown() {
        executor.shutdownNow();
    }

    private Object invokeFunction(Class<?> compiledClass, String functionName, Object[] args)
            throws ScriptExecutionException {
        try {
            Object instance = compiledClass.getDeclaredConstructor().newInstance();
            Method method = findMethod(compiledClass, functionName, args);
            Object[] coercedArgs = coerceArguments(method, args);
            long start = System.nanoTime();
            Object result = method.invoke(instance, coercedArgs);
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            logger.debug("Executed {}.{}() in {}ms", compiledClass.getSimpleName(), functionName, durationMs);
            return result;
        } catch (InvocationTargetException e) {
            throw new ScriptExecutionException(
                    "Script function '" + functionName + "' threw: " + e.getTargetException().getMessage(),
                    e.getTargetException());
        } catch (ScriptExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ScriptExecutionException(
                    "Failed to invoke script function '" + functionName + "': " + e.getMessage(), e);
        }
    }

    private Method findMethod(Class<?> clazz, String methodName, Object[] args) {
        // Try exact match first by argument count
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                return m;
            }
        }

        // If method exists but arity differs, fail explicitly instead of invoking an arbitrary overload.
        StringBuilder availableSignatures = new StringBuilder();
        int matchCount = 0;
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(methodName)) {
                if (matchCount > 0) {
                    availableSignatures.append(", ");
                }
                availableSignatures.append(methodName)
                        .append("(")
                        .append(m.getParameterCount())
                        .append(" args)");
                matchCount++;
            }
        }

        if (matchCount > 0) {
            throw new ScriptExecutionException(
                    "Method '" + methodName + "' on script class " + clazz.getSimpleName()
                            + " does not accept " + args.length + " argument(s). Available: "
                            + availableSignatures,
                    null);
        }

        throw new ScriptExecutionException(
                "No method '" + methodName + "' found in script class " + clazz.getSimpleName(), null);
    }

    /**
     * Coerce arguments to match method parameter types.
     * Primary coercion: numeric types → BigDecimal when target is BigDecimal.
     */
    private Object[] coerceArguments(Method method, Object[] args) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (args.length != paramTypes.length) {
            return args; // Let invocation handle mismatch
        }
        Object[] coerced = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            coerced[i] = coerceArg(args[i], paramTypes[i]);
        }
        return coerced;
    }

    private Object coerceArg(Object arg, Class<?> targetType) {
        if (arg == null) {
            return null;
        }
        if (targetType.isAssignableFrom(arg.getClass())) {
            return arg;
        }
        // Numeric → BigDecimal coercion
        if (targetType == BigDecimal.class && arg instanceof Number num) {
            return new BigDecimal(num.toString());
        }
        return arg;
    }
}
