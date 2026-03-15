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

import java.util.Arrays;

/**
 * SpEL bridge for runtime script invocation.
 * Provides the {@code #script(...)} function that SpEL expressions can call.
 *
 * <p>Two call forms are supported:
 * <ul>
 *   <li>{@code #script('scriptId', payload)} — invokes {@code run(Map payload)}</li>
 *   <li>{@code #script('scriptId', 'functionName', arg1, arg2, ...)} — invokes named function</li>
 * </ul>
 *
 * <p>This class holds a thread-local reference to the active runtime so that the static
 * {@code invoke} method (required by SpEL's registerFunction) can delegate to instance state.</p>
 */
public class ScriptBridge {

    private static final Logger logger = LoggerFactory.getLogger(ScriptBridge.class);

    private static final ThreadLocal<ScriptBridge> ACTIVE_BRIDGE = new ThreadLocal<>();

    private final RuntimeScriptRegistry registry;
    private final GroovyScriptCompiler compiler;
    private final ScriptExecutor executor;
    private final long executionTimeoutMs;

    public ScriptBridge(RuntimeScriptRegistry registry, GroovyScriptCompiler compiler,
                        ScriptExecutor executor, long executionTimeoutMs) {
        this.registry = registry;
        this.compiler = compiler;
        this.executor = executor;
        this.executionTimeoutMs = executionTimeoutMs;
    }

    /**
     * Activate this bridge on the current thread.
     * Must be called before SpEL evaluation that uses {@code #script(...)}.
     */
    public void activate() {
        ACTIVE_BRIDGE.set(this);
    }

    /**
     * Deactivate the bridge on the current thread.
     * Should be called after SpEL evaluation completes (typically in a finally block).
     */
    public void deactivate() {
        ACTIVE_BRIDGE.remove();
    }

    /**
     * Static entry point called by SpEL via registerFunction.
     * Dispatches to the thread-local active bridge instance.
     *
     * @param args Variable arguments: first is scriptId (String),
     *             remaining are either [payload] or [functionName, arg1, arg2, ...]
     * @return The script function's return value
     */
    public static Object invoke(Object... args) {
        ScriptBridge bridge = ACTIVE_BRIDGE.get();
        if (bridge == null) {
            throw new ScriptExecutionException(
                    "Runtime scripts are not configured — #script(...) cannot be used", null);
        }
        return bridge.doInvoke(args);
    }

    private Object doInvoke(Object[] args) {
        if (args == null || args.length < 2) {
            throw new ScriptExecutionException(
                    "#script() requires at least 2 arguments: scriptId and payload/functionName", null);
        }

        String scriptId = String.valueOf(args[0]);
        ScriptMetadata meta = registry.getScript(scriptId);
        Class<?> compiledClass = compiler.getOrCompile(meta);

        // Determine call form
        if (args.length == 2) {
            // Short form: #script('id', payload) → run(payload)
            logger.debug("Invoking script '{}' default run() function", scriptId);
            return executor.execute(compiledClass, null, new Object[]{args[1]}, executionTimeoutMs);
        } else if (args[1] instanceof String functionName) {
            // Named form: #script('id', 'functionName', arg1, arg2, ...)
            Object[] functionArgs = Arrays.copyOfRange(args, 2, args.length);
            logger.debug("Invoking script '{}'.{}() with {} args", scriptId, functionName, functionArgs.length);
            return executor.execute(compiledClass, functionName, functionArgs, executionTimeoutMs);
        } else {
            // Fallback: treat all remaining args as run() args
            Object[] runArgs = Arrays.copyOfRange(args, 1, args.length);
            logger.debug("Invoking script '{}' run() with {} args", scriptId, runArgs.length);
            return executor.execute(compiledClass, null, runArgs, executionTimeoutMs);
        }
    }

    /**
     * Get the Method reference for SpEL registerFunction.
     */
    public static java.lang.reflect.Method getInvokeMethod() {
        try {
            return ScriptBridge.class.getMethod("invoke", Object[].class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("ScriptBridge.invoke method not found", e);
        }
    }
}
