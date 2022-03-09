/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.yoko.orb.OB;

import acme.idl.foo;
import acme.idl.fooHelper;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import testify.jupiter.annotation.iiop.ConfigureOrb;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ConfigureOrb
class CodecTest {
    @Test
    void testCodec(ORB orb) {
        CodecFactory factory = assertDoesNotThrow(() -> CodecFactoryHelper.narrow(orb.resolve_initial_references("CodecFactory")));
        assertNotNull(factory);

        assertThrows(UnknownEncoding.class, () -> factory.create_codec(new Encoding((short) (ENCODING_CDR_ENCAPS.value+1), (byte)0, (byte)0)));

        Codec cdrCodec = assertDoesNotThrow(() -> factory.create_codec(new Encoding(ENCODING_CDR_ENCAPS.value, (byte)0, (byte)0)));
        assertNotNull(cdrCodec);

        Any any = orb.create_any();
        fooHelper.insert(any, new foo(10));
        {
            byte[] encoding = assertDoesNotThrow(() -> cdrCodec.encode(any));
            Any result = assertDoesNotThrow(() -> cdrCodec.decode(encoding));
            assertEquals(10, fooHelper.extract(result).l);
        }

        {
            byte[] encoding = assertDoesNotThrow(() -> cdrCodec.encode_value(any));
            Any result = assertDoesNotThrow(() -> cdrCodec.decode_value(encoding, fooHelper.type()));
            assertEquals(10, fooHelper.extract(result).l);
        }
    }
}
