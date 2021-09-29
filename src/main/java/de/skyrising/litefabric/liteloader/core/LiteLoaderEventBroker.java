package de.skyrising.litefabric.liteloader.core;

public class LiteLoaderEventBroker {
    public static class ReturnValue<T> {
        private T value;
        private boolean set;

        public ReturnValue(T value) {
            this.value = value;
        }

        public ReturnValue() {}

        public boolean isSet() {
            return set;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
            this.set = true;
        }
    }
}
