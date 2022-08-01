import org.omg.CORBA.ORB;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;

import java.io.Serializable;
import java.util.Objects;

public class  NullFieldsTest {
    interface AbstractInterface extends Serializable {}
    interface AbstractValue { String toString(); }
    static class NotSerializableClass {}

    static class NoddyHolder implements Serializable {
        int placeHolder = -1;
        AbstractInterface a = null;
        AbstractValue b = null;
        NotSerializableClass c = null;
    }

    int returnCode = 0;
    int nextBit = 1;

    public static void main(String[] args) throws Exception {
        new NullFieldsTest().run();
    }

    public void run() throws Exception {
        OutputStream out = (OutputStream) ORB.init().create_any().create_output_stream();
        out.write_value(new NoddyHolder());
        out.close();

        InputStream in = (InputStream) out.create_input_stream();

        // check the value tag is correct
        int valueTag = in.read_long();
        assertEqual(0x7fffff02, valueTag);
        // read the rep id
        String repId = in.read_string();
        System.out.println("### repository ID = " + repId);

        // read the placeHolder int next, leaving the cursor on a 4-byte boundary
        assertEqual(-1, in.read_long());

        // We expect the following in the stream:
        // - boolean false, 3 bytes padding,
        // - 4-byte null
        // - 4-byte null
        // - 4-byte null
        StringBuilder hex = new StringBuilder();
        for (int b = in.read(); -1 != b; b = in.read())
            hex.append(String.format("%s%02x", (hex.length() % 9 == 8) ? " " : "", b));
        assertEqual("00bdbdbd 00000000 00000000 00000000", hex.toString());

        in = (InputStream) out.create_input_stream();
        NoddyHolder actual = (NoddyHolder)in.read_value();

        assertEqual(-1, actual.placeHolder);
        assertEqual(null, actual.a);
        assertEqual(null, actual.b);
        assertEqual(null, actual.c);
        System.exit(returnCode);
    }

    <T> void assertEqual(T expected, T actual) {
        boolean equal = Objects.equals(expected, actual);
        String icon = equal ? "\u2705" : "\u274c";
        System.out.println(icon + " expected: " + expected);
        System.out.println(icon + "   actual: " + actual);
        if (!equal) returnCode |= nextBit;
        nextBit <<= 1;
    }
}
