/**
*
* Licensed to the Apache Software Foundation (ASF) under one or more
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

package org.apache.yoko.rmi.impl;

import org.apache.yoko.rmi.util.ClientUtil;
import org.apache.yoko.rmi.util.stub.MethodRef;
import org.apache.yoko.rmi.util.stub.StubClass;
import org.apache.yoko.rmi.util.stub.StubInitializer;
import org.apache.yoko.util.PrivilegedActions;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.portable.Delegate;
import org.omg.CORBA.portable.IDLEntity;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import javax.rmi.CORBA.PortableRemoteObjectDelegate;
import javax.rmi.CORBA.Stub;
import javax.rmi.CORBA.Tie;
import javax.rmi.CORBA.Util;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.security.AccessController.doPrivileged;
import static javax.rmi.CORBA.Util.getTie;
import static javax.rmi.CORBA.Util.registerTarget;
import static org.apache.yoko.logging.VerboseLogging.wrapped;
import static org.apache.yoko.rmispec.util.UtilLoader.loadServiceClass;
import static org.apache.yoko.util.Exceptions.as;
import static org.apache.yoko.util.PrivilegedActions.GET_CONTEXT_CLASS_LOADER;
import static org.apache.yoko.util.PrivilegedActions.action;
import static org.apache.yoko.util.PrivilegedActions.getDeclaredMethod;
import static org.apache.yoko.util.PrivilegedActions.getMethod;
import static org.apache.yoko.util.PrivilegedActions.getSysProp;


public class PortableRemoteObjectImpl implements PortableRemoteObjectDelegate {
    static final Logger LOGGER = Logger.getLogger(PortableRemoteObjectImpl.class.getName());

    static {
        // Initialize the stub handler factory when first loaded to ensure we have
        // class loading visibility to the factory.
        getRMIStubInitializer();
    }

    public void connect(Remote target, Remote source) throws RemoteException {
        source = toStub(source);

        ObjectImpl obj;
        if (target instanceof ObjectImpl) {
            obj = (ObjectImpl) target;
        } else {
            try {
                exportObject(target);
            } catch (RemoteException ignored) {}
            try {
                obj = (ObjectImpl) toStub(target);
            } catch (NoSuchObjectException ex) {
                throw as(RemoteException::new, ex, "cannot convert to stub!");
            }
        }

        try {
            ((Stub) source).connect(obj._orb());
        } catch (BAD_OPERATION bad_operation) {
            throw as(RemoteException::new, bad_operation, bad_operation.getMessage());
        }
    }

    private Object narrowRMI(ObjectImpl narrowFrom, Class<?> narrowTo) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(String.format("RMI narrowing %s => %s", narrowFrom.getClass().getName(), narrowTo.getName()));
        ObjectImpl object = narrowFrom;

        RMIState state = RMIState.current();

        Stub stub;
        try {
            stub = createStub(state, null, narrowTo);
        } catch (ClassNotFoundException ex) {
            throw as(ClassCastException::new, ex, narrowTo.getName());
        }

        Delegate delegate;
        try {
            // let the stub adopt narrowFrom's identity
            delegate = object._get_delegate();

        } catch (BAD_OPERATION ex) {
            // ignore
            delegate = null;
        }

        stub._set_delegate(delegate);

        return stub;
    }

    private Object narrowIDL(ObjectImpl narrowFrom, Class<?> narrowTo) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(String.format("IDL narrowing %s => %s", narrowFrom.getClass().getName(), narrowTo.getName()));
        final ClassLoader idlClassLoader = doPrivileged(action(narrowTo::getClassLoader));
        final String helperClassName = narrowTo.getName() + "Helper";

        try {
            final Class<?> helperClass = Util.loadClass(helperClassName, null, idlClassLoader);
            final Method helperNarrow = doPrivileged(getMethod(helperClass, "narrow", org.omg.CORBA.Object.class));
            return helperNarrow.invoke(null, narrowFrom);
        } catch (Exception e) {
            throw as(ClassCastException::new, e, narrowTo.getName());
        }
    }

    public Object narrow(Object narrowFrom, @SuppressWarnings("rawtypes") Class narrowTo)
            throws ClassCastException {
        if (narrowFrom == null)
            return null;

        if (narrowTo.isInstance(narrowFrom))
            return narrowFrom;

        final String fromClassName = narrowFrom.getClass().getName();
        final String toClassName = narrowTo.getName();
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.finer(String.format("narrow %s => %s", fromClassName, toClassName));

        if (!(narrowFrom instanceof ObjectImpl))
            throw new ClassCastException(String.format(
                    "object to narrow (runtime type %s) is not an instance of %s",
                    fromClassName, ObjectImpl.class.getName()));
        if (!narrowTo.isInterface())
            throw new ClassCastException(String.format("%s is not an interface", toClassName));

        final boolean isRemote = Remote.class.isAssignableFrom(narrowTo);
        final boolean isIDLEntity = IDLEntity.class.isAssignableFrom(narrowTo);

        if (isRemote && isIDLEntity)
            throw new ClassCastException(String.format(
                    "%s invalidly extends both %s and %s",
                    toClassName, Remote.class.getName(), IDLEntity.class.getName()));
        if (isRemote)
            return narrowRMI((ObjectImpl) narrowFrom, narrowTo);
        if (isIDLEntity)
            return narrowIDL((ObjectImpl) narrowFrom, narrowTo);

        throw new ClassCastException(String.format(
                    "%s extends neither %s nor %s",
                    toClassName, Remote.class.getName(), IDLEntity.class.getName()));
    }

    static Remote narrow1(RMIState state, ObjectImpl object, Class<?> narrowTo) throws ClassCastException {
        Stub stub;

        try {
            stub = createStub(state, null, narrowTo);
        } catch (ClassNotFoundException ex) {
            throw as(ClassCastException::new, ex, narrowTo.getName());
        }

        Delegate delegate;
        try {
            // let the stub adopt narrowFrom's identity
            delegate = object._get_delegate();

        } catch (BAD_OPERATION ex) {
            // ignore
            delegate = null;
        }

        stub._set_delegate(delegate);

        return (Remote) stub;
    }

    static private Stub createStub(RMIState state, String codebase, Class<?> type) throws ClassNotFoundException {
        if (Remote.class == type) {
            return new RMIRemoteStub();
        }

        if (ClientUtil.isRunningAsClientContainer()) {
            Stub stub = state.getStaticStub(codebase, type);
            if (stub != null) {
                return stub;
            }
        }

        return createRMIStub(state, type);
    }

    static Stub createRMIStub(RMIState state, Class<?> type) throws ClassNotFoundException {
        if (!type.isInterface()) {
            throw new RuntimeException("non-interfaces not supported");
        }

        LOGGER.fine("Creating RMI stub for class " + type.getName());

        Constructor<? extends Stub> cons = getRMIStubClassConstructor(state, type);

        try {
            Stub result = cons.newInstance();
            return result;
        } catch (InstantiationException|IllegalAccessException|InvocationTargetException ex) {
            throw new RuntimeException("internal problem: cannot instantiate stub", ex);
        }
    }

    private static final Method stub_write_replace;

    static {
        try {
            stub_write_replace = doPrivileged(getDeclaredMethod(RMIStub.class, "writeReplace"));
        } catch (Throwable ex) {
            throw wrapped(LOGGER, ex, "cannot initialize: \n" + ex.getMessage(), Error::new);
        }
    }

    static Constructor<? extends Stub> getRMIStubClassConstructor(RMIState state, Class<?> type) {
        LOGGER.fine("Requesting stub constructor of class " + type.getName());
        @SuppressWarnings("unchecked")
        Constructor<? extends Stub> cons = state.stub_map.get(type);

        if (cons != null) {
            LOGGER.fine("Returning cached constructor of class " + cons.getDeclaringClass().getName());
            return cons;
        }

        TypeRepository repository = state.repo;
        RemoteDescriptor desc = repository.getRemoteInterface(type);

        MethodDescriptor[] mdesc = desc.getMethods();
        MethodDescriptor[] descriptors = new MethodDescriptor[mdesc.length + 1];
        for (int i = 0; i < mdesc.length; i++) {
            descriptors[i] = mdesc[i];
        }

        LOGGER.finer("TYPE ----> " + type);
        LOGGER.finer("LOADER --> " + doPrivileged(action(type::getClassLoader)));
        LOGGER.finer("CONTEXT -> " + doPrivileged(GET_CONTEXT_CLASS_LOADER));

        MethodRef[] methods = new MethodRef[descriptors.length];

        for (int i = 0; i < mdesc.length; i++) {
            Method m = descriptors[i].getReflectedMethod();
            LOGGER.finer("Method ----> " + m);
            methods[i] = new MethodRef(m);
        }
        methods[mdesc.length] = new MethodRef(stub_write_replace);


        Class<? extends Stub> stubClass = null;

        try {
            /* Construct class! */
            stubClass = StubClass.make(
                    /* the class loader to use */
                    doPrivileged(action(type::getClassLoader)),

                    /* the bean developer's bean class */
                    RMIStub.class,

                    /* interfaces */
                    new Class[] { type },

                    /* the methods */
                    methods,

                    /* contains only ejbCreate */
                    null,

                    /* our data objects */
                    descriptors,

                    /* the handler method */
                    getPOAStubInvokeMethod(),

                    /* package name (use superclass') */
                    getPackageName(type),

                    /* provider of handlers */
                    getRMIStubInitializer()
            );
        } catch (NoClassDefFoundError ex) {
            /* Construct class! */
            stubClass = StubClass.make(
                    /* the class loader to use */
                    doPrivileged(GET_CONTEXT_CLASS_LOADER),

                    /* the bean developer's bean class */
                    RMIStub.class,

                    /* interfaces */
                    new Class[] { type },

                    /* the methods */
                    methods,

                    /* contains only ejbCreate */
                    null,

                    /* our data objects */
                    descriptors,

                    /* the handler method */
                    getPOAStubInvokeMethod(),

                    /* package name (use superclass') */
                    getPackageName(type),

                    /* provider of handlers */
                    getRMIStubInitializer()
            );
        }

        if (stubClass != null) {
            try {
                cons = stubClass.getConstructor();
                state.stub_map.put(type, cons);
            } catch (NoSuchMethodException e) {
                LOGGER.log(Level.FINER, "constructed stub has no default constructor", e);
            }
        }

        return cons;
    }

    static String getPackageName(Class clazz) {
        String class_name = clazz.getName();
        int idx = class_name.lastIndexOf('.');
        if (idx == -1) {
            return null;
        } else {
            return class_name.substring(0, idx);
        }
    }

    private static Method poa_stub_invoke_method;

    static Method getPOAStubInvokeMethod() {
        if (poa_stub_invoke_method == null) {
            // get the interface method used to invoke the stub handler
            poa_stub_invoke_method = doPrivileged(getDeclaredMethod(StubHandler.class, "invoke",
                    RMIStub.class, MethodDescriptor.class, Object[].class));
        }

        return poa_stub_invoke_method;
    }

    public Remote toStub(Remote value)
            throws NoSuchObjectException {
        if (value instanceof Stub)
            return value;

        Tie tie = getTie(value);
        if (tie == null) {
            throw new NoSuchObjectException("object not exported");
        }

        RMIServant servant = (RMIServant) tie;

        try {
            POA poa = servant.getRMIState().getPOA();
            org.omg.CORBA.Object ref = poa.servant_to_reference(servant);
            return (Remote) narrow(ref, servant.getJavaClass());
        } catch (ServantNotActive|WrongPolicy ex) {
            throw new RuntimeException("internal error: " + ex.getMessage(), ex);
        }
    }

    public void exportObject(Remote obj) throws RemoteException {
        RMIState state = RMIState.current();

        try {
            state.checkShutDown();
        } catch (BAD_INV_ORDER ex) {
            throw new RemoteException("RMIState is deactivated", ex);
        }

        Tie tie = getTie(obj);

        if (tie != null)
            throw new RemoteException("object already exported");

        RMIServant servant = new RMIServant(state);
        registerTarget(servant, obj);

        LOGGER.finer("exporting instance of " + obj.getClass().getName()
                + " in " + state.getName());

        try {
            servant._id = state.getPOA().activate_object(servant);
        } catch (ServantAlreadyActive|WrongPolicy ex) {
            throw new RemoteException("internal error: " + ex.getMessage(), ex);
        }
    }

    public void unexportObject(Remote obj) throws NoSuchObjectException {
        javax.rmi.CORBA.Util.unexportObject(obj);
    }

    // the factory object used for creating stub initializers
    static private StubInitializer initializer = null;
    // the default stub handler, which is ours without overrides.
    private static final String defaultInitializer = "org.apache.yoko.rmi.impl.RMIStubInitializer";

    /**
     * Get the RMI stub handler initializer to use for RMI invocation
     * stubs.  The Class in question must implement the StubInitializer method.
     *
     * @return The class used to create StubHandler instances.
     */
    private static StubInitializer getRMIStubInitializer() {
        if (initializer == null) {
            String factory = doPrivileged(getSysProp("org.apache.yoko.rmi.RMIStubInitializerClass", defaultInitializer));
            try {
                final Class<?> type = loadServiceClass(factory, "org.apache.yoko.rmi.RMIStubInitializerClass");
                initializer = (StubInitializer)(doPrivileged(PrivilegedActions.getNoArgConstructor(type)).newInstance());
            } catch (Exception e) {
                throw as(INITIALIZE::new, e,"Can not create RMIStubInitializer: " + factory);
            }
        }
        return initializer;
    }
}
