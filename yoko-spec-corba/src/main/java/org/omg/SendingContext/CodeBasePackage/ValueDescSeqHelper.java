/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omg.SendingContext.CodeBasePackage;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.ValueDefPackage.FullValueDescription;
import org.omg.CORBA.ValueDefPackage.FullValueDescriptionHelper;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

import java.util.stream.IntStream;

public class ValueDescSeqHelper {
    private static final TypeCode TYPE =
            ORB.init().create_alias_tc(id(), "ValueDescSeq", ORB.init().create_sequence_tc(0, FullValueDescriptionHelper.type()));

    public static void insert(Any any, FullValueDescription[] s) {
        any.type(type()); write(any.create_output_stream(), s);
    }

    public static FullValueDescription[] extract(Any any) {
        return read(any.create_input_stream());
    }

    public static TypeCode type() {
        return TYPE;
    }

    public String get_id() {
        return id();
    }

    public TypeCode get_type() {
        return type();
    }

    @SuppressWarnings("unused")
    public void write_Object(OutputStream out, Object obj) {
        throw new RuntimeException(" not implemented");
    }

    @SuppressWarnings("unused")
    public Object read_Object(InputStream in) {
        throw new RuntimeException(" not implemented");
    }

    public static String id() {
        return "IDL:omg.org/SendingContext/CodeBase/ValueDescSeq:1.0";
    }

    public static FullValueDescription[] read(InputStream in) {
        final int length = in.read_long();
        return IntStream.range(0, length).sequential()
                .mapToObj(i -> FullValueDescriptionHelper.read(in))
                .toArray(FullValueDescription[]::new);
    }

    public static void write(OutputStream out, FullValueDescription[] fvds) {

        out.write_long(fvds.length);
        for (FullValueDescription fvd : fvds) FullValueDescriptionHelper.write(out, fvd);

    }
}
