import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.xerox.amazonws.ec2.GroupDescription;
import com.xerox.amazonws.ec2.ImageDescription;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.KeyPairInfo;
import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;

public class VmWareSpeedTest {
  private static Logger LOG         = Logger.getLogger( VmWareSpeedTest.class );
  public static int     threadCount = 1;
  
  @Test
  public void runTest( ) throws Exception {
    threadCount = Main.count;
    ExecutorService exec = Executors.newFixedThreadPool( threadCount );
    barrier = new CyclicBarrier( threadCount, new Runnable( ) {
      @Override
      public void run( ) {
        LOG.info( "Starting threads..." );
      }
    } );
    List<Callable<List<Double>>> callables = Lists.newArrayList( );
    for ( int i = 0; i < threadCount; i++ ) {
      callables.add( new DoDescribe( ) );
    }
    long start = System.currentTimeMillis( );
    long total = 0;
    double sum = 0, sumsq = 0, min = 999999999999.9d, max = 0;
    List<Future<List<Double>>> results = exec.invokeAll( callables );
    for ( Future<List<Double>> subResult : results ) {
      List<Double> resultList = subResult.get( );
      int num = resultList.size( );
      total += num;
      for ( Double d : subResult.get( ) ) {
        max = d.doubleValue( ) > max ? d.doubleValue( ) : max;
        min = d.doubleValue( ) < min ? d.doubleValue( ) : min;
        sum += d;
        sumsq += d * d;
      }
    }
    LOG.info( String.format(
                             "file=%-20s count=%-5d min=%-10.5f max=%-10.5f avg=%-10.5f stdev=%-10.5f\n",
                             threadCount, total, min, max, 1000.0 * ( sum / total ),
                             Math.sqrt( sumsq / total - ( sum / total ) * ( sum / total ) ) ) );
    LOG.info( String.format("%10.5f %10.5f %10.5f", 
                            1000.0 * ( sum / total ), 
                            1000.0 * ( sum / total ), 
                            1000.0 * ( sum / total ) +  1000*Math.sqrt( sumsq / total - ( sum / total ) * ( sum / total ) ) ) );
  }
  
  private CyclicBarrier barrier;
  
  class DoDescribe implements Callable<List<Double>> {
    List<Double> times = Lists.newArrayList( );
    
    @Override
    public List<Double> call( ) throws Exception {
      LOG.info( "trying... " );
      ServiceInstance si;
      try {
        si = new ServiceInstance( new URL( "https://192.168.7.175/sdk" ), "Administrator", "dzgz-4l", true );
        LOG.info( "trying... " + si.getAboutInfo( ).getApiType( )+ " " +si.getAboutInfo( ).getApiVersion( ) + " " +si.getAboutInfo( ).getFullName( ) );
      } catch ( Throwable e1 ) {
        LOG.error( e1, e1 );
        barrier.await( );
        return times;
      }
      barrier.await( );

      for ( int i = 0; i < 100; i++ ) {
        long start = System.currentTimeMillis( );
        try {
          for ( ManagedEntity m : new InventoryNavigator( si.getRootFolder( ) ).searchManagedEntities( "VirtualMachine" ) ) {
            LOG.info( m );
          }
          double time = ( System.currentTimeMillis( ) - start ) / 1000.0d;
          times.add( time );
          LOG.info( " " + time );
        } catch ( Throwable e ) {
          LOG.debug( e, e );
        }
      }
      try {
        si.getSessionManager( ).logout( );
      } catch ( Throwable e ) {
        LOG.debug( e, e );
      }
      return times;
    }
    
  }
  
  enum ImageTypes {
    machine, kernel, ramdisk
  }
  
  enum Protocols {
    tcp, udp, icmp
  }
  
  enum Names {
    group, image, address, instances;
  }
  
  static class TestAssets {
    private int                    instanceCount = 1;
    private KeyPairInfo            keypair;
    private GroupDescription       group;
    private ReservationDescription reservation;
    private ImageDescription       image;
    private String                 address;
    
    public KeyPairInfo getKeypair( ) {
      return this.keypair;
    }
    
    public void setKeypair( KeyPairInfo keypair ) {
      this.keypair = keypair;
    }
    
    public ImageDescription getImage( ) {
      return this.image;
    }
    
    public void setImage( ImageDescription image ) {
      this.image = image;
    }
    
    public String getAddress( ) {
      return this.address;
    }
    
    public void setAddress( String address ) {
      this.address = address;
    }
    
    public GroupDescription getGroup( ) {
      return this.group;
    }
    
    public void setGroup( GroupDescription group ) {
      this.group = group;
    }
    
    public int getInstanceCount( ) {
      return this.instanceCount;
    }
    
    public void setInstanceCount( int instanceCount ) {
      this.instanceCount = instanceCount;
    }
    
    public LaunchConfiguration getLaunchConfiguration( ) {
      LaunchConfiguration launchConfiguration = new LaunchConfiguration( this.getImage( ).getImageId( ) );
      launchConfiguration.setKeyName( this.getKeypair( ).getKeyName( ) );
      launchConfiguration.setSecurityGroup( Lists.newArrayList( this.getGroup( ).getName( ) ) );
      launchConfiguration.setMinCount( this.getInstanceCount( ) );
      launchConfiguration.setMaxCount( this.getInstanceCount( ) );
      return launchConfiguration;
    }
    
    public ReservationDescription getReservation( ) {
      return this.reservation;
    }
    
    public void setReservation( ReservationDescription reservation ) {
      this.reservation = reservation;
    }
  }
  
}
