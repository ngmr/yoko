/*
 * Copyright 2021 IBM Corporation and others.
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
package versioned;

import acme.Widget;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("unused")
public class BigWidget extends NonSerializableSuper implements Widget {
    private static final long serialVersionUID = 1L;
    private String a = "a_v2";
    private transient String b = "b_v2";
    private String d = "d_v2";
    private String e = "e_v2";
    private String f;
    private transient String g;
    private String i;
    private String j;

    public BigWidget() {
        f = "f_v2";
        g = "g_v2";
        i = "i_v2";
        j = "j_v2";
    }

    @Override
    public Widget validateAndReplace() {
        // check the v1 was transmitted correctly
        assertThat(a, endsWith("v1"));
        assertThat(b, nullValue());
        assertThat(d, nullValue());
        assertThat(e, endsWith("v1"));
        assertThat(f, endsWith("v1"));
        assertThat(g, nullValue());
        assertThat(i, nullValue());
        assertThat(j, endsWith("v1"));
        return new BigWidget();
    }

    @Override
    public String toString() {
        return "WidgetImpl{" +
                "a='" + a + '\'' +
                ", b='" + b + '\'' +
                ", d='" + d + '\'' +
                ", e='" + e + '\'' +
                ", f='" + f + '\'' +
                ", g='" + g + '\'' +
                ", i='" + i + '\'' +
                ", j='" + j + '\'' +
                '}';
    }
}
