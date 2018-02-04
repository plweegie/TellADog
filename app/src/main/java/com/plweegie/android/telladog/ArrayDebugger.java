package com.plweegie.android.telladog;


import android.util.Log;

import java.lang.reflect.Array;

public class ArrayDebugger {

    private Object array;

    public ArrayDebugger() {
        this.array = new float[1][4];
    }

    public ArrayDebugger(Object o) {
        this.array = o;
    }

    int[] shapeOf() {
        int size = numDimensions(this.array);
        int[] dimensions = new int[size];
        fillShape(this.array, 0, dimensions);
        return dimensions;
    }

    static int numDimensions(Object o) {
        if (o == null || !o.getClass().isArray()) {
            return 0;
        }
        if (Array.getLength(o) == 0) {
            throw new IllegalArgumentException("array lengths cannot be 0.");
        }
        return 1 + numDimensions(Array.get(o, 0));
    }

    static void fillShape(Object o, int dim, int[] shape) {
        Log.d("ArrayDebugger", "called dim is " + dim);
        if (shape == null || dim == shape.length) {
            Log.d("ArrayDebugger", "returned");
            return;
        }
        final int len = Array.getLength(o);
        Log.d("ArrayDebugger", "len: " + len + " shape[dim]: " + shape[dim]);
        if (shape[dim] == 0) {
            shape[dim] = len;
        } else if (shape[dim] != len) {
            throw new IllegalArgumentException(
                    String.format("mismatched lengths (%d and %d) in dimension %d", shape[dim], len, dim));
        }
        for (int i = 0; i < len; ++i) {
            fillShape(Array.get(o, i), dim + 1, shape);
        }
    }
}
