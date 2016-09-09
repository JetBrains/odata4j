package org.odata4j.producer.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class LoggingUtils {
    public static void log(Logger log, String operation, Object... namedArgs) {
        if (!log.isLoggable(Level.FINE)) return;

        StringBuilder sb = new StringBuilder(operation).append('(');
        if (namedArgs != null && namedArgs.length > 0) {
            for (int i = 0; i < namedArgs.length; i += 2) {
                if (i > 0) sb.append(',');
                sb.append(namedArgs[i]).append('=').append(namedArgs[i + 1]);
            }
        }

        log.fine(sb.append(')').toString());
    }
}
