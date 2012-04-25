import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.xerox.amazonws.ec2.Jec2;

public class Main {
  public static int     count = 1;
  private static Logger LOG   = Logger.getLogger( Main.class );
  
  public static void main( String[] args ) throws Exception {
    if ( args.length < 1 ) {
      args = new String[] { "DanTest" };
    }
    for ( int i = 0; i < args.length; i++ ) {
      String name = args[i];
      String className = args[i];
      if ( name.matches( ".*\\.\\d*" ) ) {
        count = Integer.parseInt( className.replaceAll( ".*\\.", "" ) );
        className = name.replaceAll( "\\." + count, "" );
      }
      Class testClass = Class.forName( className );
      Result res = JUnitCore.runClasses( testClass );
      if ( !res.wasSuccessful( ) ) {
        for ( Failure f : res.getFailures( ) ) {
          LOG.error( String.format( "FAIL ================== %30.30s ================== ", f.getTestHeader( ) ) );
          LOG.error( "FAIL " + f.getMessage( ) );
          LOG.error( "FAIL " + f.getDescription( ) );
          LOG.error( "FAIL " + f.getTrace( ) );
        }
        System.exit( 1 );
      }
    }
    System.exit( 0 );
  }
  
}
