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

package acme.idl;

import org.apache.yoko.util.Assert;
import org.omg.CORBA.Any;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.IORInterceptor;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ObjectReferenceTemplate;

@SuppressWarnings("unused")
final class MyIORInterceptor_impl extends LocalObject implements IORInterceptor {
    private final Codec cdrCodec_;

    MyIORInterceptor_impl(ORBInitInfo info) {
        CodecFactory factory = info.codec_factory();

        Encoding how = new Encoding(
                (byte) ENCODING_CDR_ENCAPS.value, (byte) 0,
                (byte) 0);

        try {
            cdrCodec_ = factory.create_codec(how);
        } catch (UnknownEncoding ex) {
            throw new RuntimeException();
        }
        Assert.ensure(cdrCodec_ != null);
    }

    //
    // IDL to Java Mapping
    //

    public String name() {
        return "";
    }

    public void destroy() {}

    public void establish_components(IORInfo info) {
        try {
            Policy p = info.get_effective_policy(MY_SERVER_POLICY_ID.value);
            if (p == null) {
                return;
            }
            MyServerPolicy policy = MyServerPolicyHelper.narrow(p);

            MyComponent content = new MyComponent();
            content.val = policy.value();
            Any any = ORB.init().create_any();
            MyComponentHelper.insert(any, content);

            byte[] encoding;
            try {
                encoding = cdrCodec_.encode_value(any);
            } catch (InvalidTypeForEncoding ex) {
                throw new RuntimeException();
            }

            TaggedComponent component = new TaggedComponent();
            component.tag = MY_COMPONENT_ID.value;
            component.component_data = new byte[encoding.length];
            System.arraycopy(encoding, 0, component.component_data, 0,
                    encoding.length);

            info.add_ior_component(component);
        } catch (INV_POLICY ignored) {}
    }

    public void components_established(IORInfo info) {
    }

    public void adapter_state_change(ObjectReferenceTemplate[] templates,
            short state) {
    }

    public void adapter_manager_state_change(int id, short state) {
    }
}
