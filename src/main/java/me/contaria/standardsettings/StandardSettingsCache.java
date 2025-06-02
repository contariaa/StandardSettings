package me.contaria.standardsettings;

import me.contaria.standardsettings.options.StandardSetting;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class StandardSettingsCache {
    private final String id;
    protected final Set<Entry<?>> cache;

    public StandardSettingsCache(String id) {
        this.id = id;
        this.cache = new HashSet<>();
        for (StandardSetting<?> setting : StandardSettings.config.standardSettings) {
            this.cache.add(new Entry<>(setting));
        }
    }

    public String getId() {
        return this.id;
    }

    public void load() {
        for (Entry<?> cacheEntry : this.cache) {
            cacheEntry.load();
        }
        StandardSettings.updateSettings();
    }

    protected static class Entry<T> {
        public final StandardSetting<T> setting;
        public final T value;

        private Entry(StandardSetting<T> setting) {
            this.setting = setting;
            this.value = setting.getVanilla();
        }

        public void load() {
            this.setting.setVanilla(this.value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Entry<?> entry = (Entry<?>) o;
            return Objects.equals(this.setting, entry.setting) && Objects.equals(this.value, entry.value);
        }
    }
}
