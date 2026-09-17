package com.example.backend.ot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class TextOperation {

    public enum Type {
        RETAIN,
        INSERT,
        DELETE
    }

    @Data
    @NoArgsConstructor
    public static class Component {
        private Type type;
        private int count;
        private String text;

        @JsonCreator
        public Component(
                @JsonProperty("type") Type type,
                @JsonProperty("count") int count,
                @JsonProperty("text") String text
        ) {
            this.type = type;
            this.count = count;
            this.text = text;
        }

        public static Component retain(int count) {
            return new Component(Type.RETAIN, count, null);
        }

        public static Component insert(String text) {
            return new Component(Type.INSERT, text != null ? text.length() : 0, text);
        }

        public static Component delete(int count) {
            return new Component(Type.DELETE, count, null);
        }
    }

    private List<Component> components = new ArrayList<>();

    public TextOperation(List<Component> components) {
        this.components = components != null ? components : new ArrayList<>();
    }

    public TextOperation retain(int count) {
        if (count > 0) {
            components.add(Component.retain(count));
        }
        return this;
    }

    public TextOperation insert(String text) {
        if (text != null && !text.isEmpty()) {
            components.add(Component.insert(text));
        }
        return this;
    }

    public TextOperation delete(int count) {
        if (count > 0) {
            components.add(Component.delete(count));
        }
        return this;
    }
}
