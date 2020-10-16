package io.github.terra121.dataset;

import java.util.ArrayList;

public class RandomAccessRunlength<E> {

    private final ArrayList<Run> data;
    int size;

    public RandomAccessRunlength() {
        this.data = new ArrayList<>();
        this.size = 0;
    }

    public int size() {
        return this.size;
    }

    public void add(E point) {
        if (this.data.isEmpty() || !point.equals(this.data.get(this.data.size() - 1).value)) {
            this.data.add(new Run(point, this.size));
        }
        this.size++;
    }

    public void addRun(E point, int num) {
        if (this.data.isEmpty() || !point.equals(this.data.get(this.data.size() - 1).value)) {
            this.data.add(new Run(point, this.size));
        }
        this.size += num;
    }

    private int getIdx(int idx) {
        if (idx >= this.size) {
            throw new IndexOutOfBoundsException(idx + " >= " + this.size);
        }

        int low = 0;
        int high = this.data.size();

        while (low < high - 1) {
            int mid = low + (high - low) / 2;
            int val = this.data.get(mid).index;
            if (idx < val) {
                high = mid;
            } else if (val < idx) {
                low = mid;
            } else {
                return mid;
            }
        }

        return low;
    }

    public E get(int idx) {
        return this.data.get(this.getIdx(idx)).value;
    }

    /*public Iterator<E> iterator(int start) {
        return new RARIt(getIdx());
    }

    public Iterator<E> iterator() {
        return new RARIt(0);
    }

    public class RARIt extends Iterator {

        int data;
        int index;

        private RARIt(int start) {
            data = getIdx(start-1);
            index = start-1;
        }

        public int next() {
            index++;

        }

        public void remove() {

        }
    }*/

    private class Run {
        public final E value;
        public final int index;

        public Run(E val, int idx) {
            this.value = val;
            this.index = idx;
        }
    }
}