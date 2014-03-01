package sriracha.simulator.model.elements.sources.transient_functions;

import sriracha.math.interfaces.IComplex;
import sriracha.simulator.model.elements.sources.SourceClass;
import sriracha.simulator.parser.CircuitBuilder;

/**
 * Created by yiqing on 27/02/14.
 */
public class PulseTransFun extends TransientFunction {

    /**
     * Valley or initial voltage or current value. (V or A)
     */
    private double v1 = 0;

    /**
    * Plateau value (V or A)
    */
    private double v2 = 0;

    /**
     * Delay time in seconds
     */
    private double td = 0;

    /**
     * Duration of onset ramp in seconds, from v1 to v2.  NOTE: intermediate points within the ramp
     * are determined using linear interpolation.
     */
    private double tr = 0;

    /**
     * Duration of falling ramp in seconds, from v2 to v1.  NOTE: intermediate points within the ramp
     * are determined using linear interpolation.
     */
    private double tf = 0;

    /**
     * Pulse width in seconds (Plateau width)
     */
    private double pw = 0;

    /**
     * Pulse repetition period in seconds. (Minimum determined by tr + tf + 2*pw).
     * NOTE: if the period value specified is less than the minimum, it will be set to the minimum.
     */
    private double per = 0;

    private double minimumPeriod = per;
    private double[]edgePositions;

    public PulseTransFun(String[]params, SourceClass srcClass){
        super(srcClass);
        int index = 0;

        while(index < params.length){
            switch(index){
                case 0: v1 = CircuitBuilder.parseDouble(params[index]);     break;
                case 1: v2 = CircuitBuilder.parseDouble(params[index]);     break;
                case 2: td = CircuitBuilder.parseDouble(params[index]);   break;
                case 3: tr = CircuitBuilder.parseDouble(params[index]);  break;
                case 4: tf = CircuitBuilder.parseDouble(params[index]);    break;
                case 5: pw = CircuitBuilder.parseDouble(params[index]);    break;
                case 6: per = CircuitBuilder.parseDouble(params[index]);    break;
            }
            index++;
        }
        minimumPeriod = tr + tf + 2*pw;

        if(per < minimumPeriod)
            per = minimumPeriod;

        edgePositions = new double[4];
        edgePositions[0] = tr;
        edgePositions[1] =  edgePositions[0] + pw;
        edgePositions[2] =  edgePositions[1] + tf;
        edgePositions[3] =  edgePositions[2] + pw;
    }

    /**
     * Obtain the volt/current value of the sine wave function at the specified time.
     * The equation is vo + va* exp[-(time-td)*theta]*sin[2*PI*(freq*(time-td)+phi/360)]
     * @param time time at which the equation is probed
     * @return
     */
    @Override
    public double probeValue(double time) {
        //if the time is below or equal the delay value or is at very start
        if(time <= td)
            return v1;

        double trimmedTime = 0;
        //cut out the delay time.
        trimmedTime = (time - td);
        //Reposition time to within the first period, but at the same position on the characteristic curve.
        trimmedTime = trimmedTime - Math.floor(trimmedTime/per)*per;

        int positionIndex = 0;
        //Check between which pulse edges is the trimmed time located.
        while(positionIndex < edgePositions.length){
            if(trimmedTime < edgePositions[positionIndex]){
                switch(positionIndex){
                    case 0:     //If time is between time 0 and first plateau edge
                        return v1 + (v2-v1)/tr*trimmedTime;
                    case 1:     //If time is between first plateau edge and second plateau edge
                        return v2;
                    case 2:     //If time is between second plateau edge and first valley edge
                        return v2 - (v2-v1)/tf*(trimmedTime - edgePositions[1]);
                    case 3:     //If time is between first valley edge and second valley edge
                        return v1;
                }
            }
            positionIndex++;
        }
        /*
         In case the period was longer than the minimum period, all pulse values outside
         the characteristic curve will be using the valley value.
         */
        return v1;
    }
}
