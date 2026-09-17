package com.example.backend.ot;

import java.util.ArrayList;
import java.util.List;

public class OTEngine {

    public enum Priority {
        LEFT,
        RIGHT
    }

    public static String apply(String document, TextOperation operation) {
        if (document == null) {
            document = "";
        }
        if (operation == null || operation.getComponents().isEmpty()) {
            return document;
        }

        StringBuilder result = new StringBuilder();
        int docIndex = 0;

        for (TextOperation.Component c : operation.getComponents()) {
            switch (c.getType()) {
                case RETAIN -> {
                    int end = Math.min(docIndex + c.getCount(), document.length());
                    if (docIndex < document.length()) {
                        result.append(document, docIndex, end);
                    }
                    docIndex += c.getCount();
                }
                case INSERT -> {
                    if (c.getText() != null) {
                        result.append(c.getText());
                    }
                }
                case DELETE -> {
                    docIndex += c.getCount();
                }
            }
        }

        // Append any remaining trailing characters if retain didn't cover to the end
        if (docIndex < document.length()) {
            result.append(document.substring(docIndex));
        }

        return result.toString();
    }

    public static TextOperation transform(TextOperation op1, TextOperation op2, Priority priority) {
        if (op1 == null || op1.getComponents().isEmpty()) {
            return new TextOperation();
        }
        if (op2 == null || op2.getComponents().isEmpty()) {
            return new TextOperation(new ArrayList<>(op1.getComponents()));
        }

        List<TextOperation.Component> result = new ArrayList<>();
        List<TextOperation.Component> c1List = new ArrayList<>(op1.getComponents());
        List<TextOperation.Component> c2List = new ArrayList<>(op2.getComponents());

        int i1 = 0, i2 = 0;
        TextOperation.Component c1 = i1 < c1List.size() ? cloneComponent(c1List.get(i1++)) : null;
        TextOperation.Component c2 = i2 < c2List.size() ? cloneComponent(c2List.get(i2++)) : null;

        while (c1 != null || c2 != null) {
            if (c1 != null && c1.getType() == TextOperation.Type.INSERT && c2 != null && c2.getType() == TextOperation.Type.INSERT) {
                if (priority == Priority.LEFT) {
                    result.add(TextOperation.Component.insert(c1.getText()));
                    c1 = i1 < c1List.size() ? cloneComponent(c1List.get(i1++)) : null;
                } else {
                    result.add(TextOperation.Component.retain(c2.getText() != null ? c2.getText().length() : c2.getCount()));
                    c2 = i2 < c2List.size() ? cloneComponent(c2List.get(i2++)) : null;
                }
                continue;
            }

            if (c1 != null && c1.getType() == TextOperation.Type.INSERT) {
                result.add(TextOperation.Component.insert(c1.getText()));
                c1 = i1 < c1List.size() ? cloneComponent(c1List.get(i1++)) : null;
                continue;
            }

            if (c2 != null && c2.getType() == TextOperation.Type.INSERT) {
                result.add(TextOperation.Component.retain(c2.getText() != null ? c2.getText().length() : c2.getCount()));
                c2 = i2 < c2List.size() ? cloneComponent(c2List.get(i2++)) : null;
                continue;
            }

            if (c1 == null) {
                break;
            }
            if (c2 == null) {
                result.add(c1);
                c1 = i1 < c1List.size() ? cloneComponent(c1List.get(i1++)) : null;
                continue;
            }

            if (c1.getType() == TextOperation.Type.RETAIN && c2.getType() == TextOperation.Type.RETAIN) {
                int min = Math.min(c1.getCount(), c2.getCount());
                result.add(TextOperation.Component.retain(min));
                c1.setCount(c1.getCount() - min);
                c2.setCount(c2.getCount() - min);

                if (c1.getCount() == 0) c1 = i1 < c1List.size() ? cloneComponent(c1List.get(i1++)) : null;
                if (c2.getCount() == 0) c2 = i2 < c2List.size() ? cloneComponent(c2List.get(i2++)) : null;

            } else if (c1.getType() == TextOperation.Type.DELETE && c2.getType() == TextOperation.Type.DELETE) {
                int min = Math.min(c1.getCount(), c2.getCount());
                c1.setCount(c1.getCount() - min);
                c2.setCount(c2.getCount() - min);

                if (c1.getCount() == 0) c1 = i1 < c1List.size() ? cloneComponent(c1List.get(i1++)) : null;
                if (c2.getCount() == 0) c2 = i2 < c2List.size() ? cloneComponent(c2List.get(i2++)) : null;

            } else if (c1.getType() == TextOperation.Type.DELETE && c2.getType() == TextOperation.Type.RETAIN) {
                int min = Math.min(c1.getCount(), c2.getCount());
                result.add(TextOperation.Component.delete(min));
                c1.setCount(c1.getCount() - min);
                c2.setCount(c2.getCount() - min);

                if (c1.getCount() == 0) c1 = i1 < c1List.size() ? cloneComponent(c1List.get(i1++)) : null;
                if (c2.getCount() == 0) c2 = i2 < c2List.size() ? cloneComponent(c2List.get(i2++)) : null;

            } else if (c1.getType() == TextOperation.Type.RETAIN && c2.getType() == TextOperation.Type.DELETE) {
                int min = Math.min(c1.getCount(), c2.getCount());
                c1.setCount(c1.getCount() - min);
                c2.setCount(c2.getCount() - min);

                if (c1.getCount() == 0) c1 = i1 < c1List.size() ? cloneComponent(c1List.get(i1++)) : null;
                if (c2.getCount() == 0) c2 = i2 < c2List.size() ? cloneComponent(c2List.get(i2++)) : null;
            }
        }

        return new TextOperation(shorten(result));
    }

    private static List<TextOperation.Component> shorten(List<TextOperation.Component> list) {
        List<TextOperation.Component> optimized = new ArrayList<>();
        for (TextOperation.Component c : list) {
            if (c.getType() != TextOperation.Type.INSERT && c.getCount() <= 0) {
                continue;
            }
            if (c.getType() == TextOperation.Type.INSERT && (c.getText() == null || c.getText().isEmpty())) {
                continue;
            }

            if (!optimized.isEmpty()) {
                TextOperation.Component last = optimized.get(optimized.size() - 1);
                if (last.getType() == c.getType()) {
                    if (c.getType() == TextOperation.Type.RETAIN || c.getType() == TextOperation.Type.DELETE) {
                        last.setCount(last.getCount() + c.getCount());
                        continue;
                    } else if (c.getType() == TextOperation.Type.INSERT) {
                        last.setText(last.getText() + c.getText());
                        last.setCount(last.getText().length());
                        continue;
                    }
                }
            }
            optimized.add(c);
        }
        return optimized;
    }

    private static TextOperation.Component cloneComponent(TextOperation.Component c) {
        return new TextOperation.Component(c.getType(), c.getCount(), c.getText());
    }
}
