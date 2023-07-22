package it.unimore.iot.project.smartobjects.models.actuators;

public class LightActivatedActuator extends GenericActuator<Double, Boolean> {

    public static final double DEFAULT_ACTIVATION_SUPERIOR_BOUND = 10.8;
    public static final double DEFAULT_ACTIVATION_INFERIOR_BOUND = -1;

    private double inferiorBound;
    private double superiorBound;

    public LightActivatedActuator() {
        this(DEFAULT_ACTIVATION_INFERIOR_BOUND, DEFAULT_ACTIVATION_SUPERIOR_BOUND);
    }

    /**
     *
     * @param inferiorBound Minimum lux value to activate the actuator. Value <= 0 for no bound.
     * @param superiorBound Maximum lux value to activate the actuator. Value <= 0 for no bound.
     */
    public LightActivatedActuator(double inferiorBound, double superiorBound) {
        super();
        this.inferiorBound = inferiorBound;
        this.superiorBound = superiorBound;
    }

    public double getInferiorBound() {
        return inferiorBound;
    }

    public void setInferiorBound(double inferiorBound) {
        this.inferiorBound = inferiorBound;
    }

    public double getSuperiorBound() {
        return superiorBound;
    }

    public void setSuperiorBound(double superiorBound) {
        this.superiorBound = superiorBound;
    }

    @Override
    public void onDataChanged(Double value) {
        if (value == null || value < 0.0) {
            value = 0.0;
        }
        setSavedValue(value);

        if (this.isActive() ) {
            if (superiorBound > 0 && inferiorBound > 0) {
                setStatus(value <= superiorBound && value >= inferiorBound);
            } else if (superiorBound > 0) {
                setStatus(value <= superiorBound);
            } else if (inferiorBound > 0) {
                setStatus(value >= inferiorBound);
            } else {
                setStatus(true);
            }
        } else {
            setStatus(false);
        }
    }
}
