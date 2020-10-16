package io.github.terra121.projection;

public abstract class ProjectionTransform extends GeographicProjection {
    protected final GeographicProjection input;

    public ProjectionTransform(GeographicProjection input) {
        this.input = input;
    }

    public boolean upright() {
        return this.input.upright();
    }

    public double[] bounds() {
        return this.input.bounds();
    }

    public double metersPerUnit() {
        return this.input.metersPerUnit();
    }
}
