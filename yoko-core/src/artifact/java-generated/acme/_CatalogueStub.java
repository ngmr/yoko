package acme;


/**
* acme/_CatalogueStub.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from ../idl/acme.idl
* Thursday, 25 May 2023 13:10:15 o'clock BST
*/

public class _CatalogueStub extends org.omg.CORBA.portable.ObjectImpl implements acme.Catalogue
{

  public void addProduct (acme.Product product) throws acme.PRODUCT_TOO_EXIST
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("addProduct", true);
                acme.ProductHelper.write ($out, product);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:acme/PRODUCT_TOO_EXIST:1.0"))
                    throw acme.PRODUCT_TOO_EXISTHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                addProduct (product        );
            } finally {
                _releaseReply ($in);
            }
  } // addProduct

  public acme.Product[] getProducts ()
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("getProducts", true);
                $in = _invoke ($out);
                acme.Product $result[] = acme.ProductListHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return getProducts (        );
            } finally {
                _releaseReply ($in);
            }
  } // getProducts

  public acme.Product findProduct (String name) throws acme.PRODUCT_NOT_EXIST
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("findProduct", true);
                $out.write_string (name);
                $in = _invoke ($out);
                acme.Product $result = acme.ProductHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:acme/PRODUCT_NOT_EXIST:1.0"))
                    throw acme.PRODUCT_NOT_EXISTHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return findProduct (name        );
            } finally {
                _releaseReply ($in);
            }
  } // findProduct

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:acme/Catalogue:1.0"};

  public String[] _ids ()
  {
    return (String[])__ids.clone ();
  }

  private void readObject (java.io.ObjectInputStream s) throws java.io.IOException
  {
     String str = s.readUTF ();
     String[] args = null;
     java.util.Properties props = null;
     org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init (args, props);
   try {
     org.omg.CORBA.Object obj = orb.string_to_object (str);
     org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl) obj)._get_delegate ();
     _set_delegate (delegate);
   } finally {
     orb.destroy() ;
   }
  }

  private void writeObject (java.io.ObjectOutputStream s) throws java.io.IOException
  {
     String[] args = null;
     java.util.Properties props = null;
     org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init (args, props);
   try {
     String str = orb.object_to_string (this);
     s.writeUTF (str);
   } finally {
     orb.destroy() ;
   }
  }
} // class _CatalogueStub
