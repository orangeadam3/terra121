package io.github.terra121.dataset;

import java.util.ArrayList;

public class RandomAccessRunlength<E> {

    private ArrayList<Run> data;
    int size;

    public RandomAccessRunlength() {
        data = new ArrayList<Run>();
        size = 0;
    }

    public int size() {
        return size;
    }

    public void add(E point) {
        if(data.size()==0 || !point.equals(data.get(data.size()-1).value)) {
            data.add(new Run(point, size));
        }
        size++;
    }

    public void addRun(E point, int num) {
        if(data.size()==0 || !point.equals(data.get(data.size()-1).value)) {
            data.add(new Run(point, size));
        }
        size += num;
    }

    private int getIdx(int idx) {
        if(idx >= size) {
            throw new IndexOutOfBoundsException(idx + " >= " + size);
        }

        int low = 0;
        int high = data.size();

        while(low < high-1) {
            int mid = low + (high - low) / 2;
            int val = data.get(mid).index;
            if(idx < val)
                high = mid;
            else if(val < idx)
                low = mid;
            else return mid;
        }

        return low;
    }

    public E get(int idx) {
        return data.get(getIdx(idx)).value;
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
        public E value;
        public int index;
        public Run(E val, int idx) {
            value = val;
            index = idx;
        }
    }
}