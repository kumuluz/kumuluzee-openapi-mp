/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.openapi.mp.config;

import com.kumuluz.ee.configuration.ConfigurationSource;
import com.kumuluz.ee.configuration.utils.ConfigurationDispatcher;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import java.util.List;
import java.util.Optional;

/**
 * Configuration source that maps mp.openapi configuration properties to kumuluzee.openapi.
 *
 * @author Urban Malc
 * @since 1.0.0
 */
public class KumuluzConfigMapper implements ConfigurationSource {

    private static final String MP_PREFIX = "mp.openapi.";
    private static final String KUMULUZ_PREFIX = "kumuluzee.openapi-mp.";

    private ConfigurationUtil configurationUtil;

    @Override
    public void init(ConfigurationDispatcher configurationDispatcher) {
        configurationUtil = ConfigurationUtil.getInstance();
    }

    @Override
    public Optional<String> get(String key) {

        if (key.startsWith(MP_PREFIX)) {
            return configurationUtil.get(KUMULUZ_PREFIX + key.substring(MP_PREFIX.length()));
        }

        return Optional.empty();
    }

    /**
     * Low priority so mp.openapi still takes precedence.
     *
     * @return configuration source ordinal
     */
    @Override
    public Integer getOrdinal() {
        return 10;
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getInteger(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> getLong(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<Double> getDouble(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<Float> getFloat(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getListSize(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> getMapKeys(String key) {
        return Optional.empty();
    }

    @Override
    public void watch(String key) {

    }

    @Override
    public void set(String key, String value) {

    }

    @Override
    public void set(String key, Boolean value) {

    }

    @Override
    public void set(String key, Integer value) {

    }

    @Override
    public void set(String key, Double value) {

    }

    @Override
    public void set(String key, Float value) {

    }
}
