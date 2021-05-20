package test.fvd;

import java.io.Serializable;

import org.omg.CORBA.ORB;
import test.fvd.Abstract;
import test.fvd.Bounceable;
import test.fvd.Bouncer;
import test.fvd.Value;

public class BouncerImpl implements Bouncer {
    
    final ORB orb;
    
    public BouncerImpl(ORB orb) {
        this.orb = orb;
    }

    @Override
    public Abstract bounceAbstract(Abstract obj) {
        return ((Bounceable)obj).validateAndReplace();
    }

    @Override
    public Object bounceObject(Object obj) {
        return ((Bounceable)obj).validateAndReplace();
    }

    @Override
    public Serializable bounceSerializable(Serializable obj) {
        return ((Bounceable)obj).validateAndReplace();
    }

    @Override
    public Value bounceValue(Value obj){
        return ((Bounceable)obj).validateAndReplace();
    }

    public void shutdown() {
        orb.shutdown(false);
    }
}
