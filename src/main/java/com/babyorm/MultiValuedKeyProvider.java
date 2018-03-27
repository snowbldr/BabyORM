package com.babyorm;

import java.util.Map;

/**
 * A provider to provide multiple keys at once.
 */
@FunctionalInterface
public interface MultiValuedKeyProvider extends KeyProvider<Map<String, Object>> {}