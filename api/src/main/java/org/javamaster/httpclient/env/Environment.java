package org.javamaster.httpclient.env;

import consulo.httpClient.localize.HttpClientLocalize;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author UNV
 * @since 2026-04-09
 */
public record Environment(LocalizeValue displayName, @Nullable String value) {
    public static final Environment NO_ENVIRONMENT = new Environment(HttpClientLocalize.noEnv(), null);

    public static Environment of(@Nullable String env) {
        if (StringUtil.isEmpty(env)) {
            return NO_ENVIRONMENT;
        }
        return new Environment(LocalizeValue.of(env), env);
    }
}