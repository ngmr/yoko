/*
 * Copyright 2023 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.ORB;

public class OrbInitTest {
    @BeforeClass
    public static void logWhereTheOrbClassComesFromAtRuntime() {
        System.out.println("ORB API class = " + ORB.class);
        System.out.println("ORB API class loader = " + ORB.class.getClassLoader());
    }

    /** Create a non-singleton orb without specifying any properties */
    public static ORB createOrb(String...params){
        return createOrb(null, params);
    }

    /** Create a non-singleton orb */
    public static ORB createOrb(Properties props, String...params){
        final ORB orb = ORB.init(params, props);
        System.out.println("ORB impl class = " + orb.getClass());
        System.out.println("ORB impl class loader = " + orb.getClass().getClassLoader());
        return orb;
    }

    public static Properties props(String...props) {
        Properties result = new Properties();
        String key = null;
        for (String s: props) {
            if (key == null) {
                key = s;
            } else {
                result.setProperty(key, s);
                key = null;
            }
        }
        return result;
    }

    @Test
    public void testORBSingletonIsTheSameInstance() {
        ORB orb1 = ORB.init();
        assertThat(orb1, is(notNullValue()));
        ORB orb2 = ORB.init();
        assertThat(orb2, is(orb1));
    }

    @Test
    public void testORBNoProps() {
        final ORB orb = createOrb();
        assertThat(orb, is(notNullValue()));
    }

    @Test(expected = NO_IMPLEMENT.class)
    public void testORBSingletonDestroy() {
        ORB.init().destroy();
    }

    @Test
    public void testORBExplicitClass() {
        final ORB orb = createOrb(props("org.omg.CORBA.ORBClass","org.apache.yoko.orb.CORBA.ORB"));
        assertThat(orb, is(notNullValue()));
    }
}
