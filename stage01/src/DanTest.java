import java.util.BitSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.*;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.GroupDescription;
import com.xerox.amazonws.ec2.ImageDescription;
import com.xerox.amazonws.ec2.KeyPairInfo;
import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;

import static org.junit.Assert.*;

public class DanTest extends TestContext {
  private static Logger LOG = Logger.getLogger( DanTest.class );
  private final TestAssets    assets;
  
  public DanTest( ) throws Exception {
    super( );
    this.assets = new TestAssets( );
  }

  @Before public void doSetup( ) throws Exception {
    String keyName = this.unique( Names.keypair );
    KeyPairInfo keypair = client( ).createKeyPair( keyName );
    this.assets.setKeypair( keypair );

    if ( this.isManaged( ) ) {
      String addr = client( ).allocateAddress( );
      header(Names.address, addr );
      this.assets.setAddress( addr );
    }
    header(Names.keypair, keyName, keypair );
    assertNotNull( keypair );
    
    final String groupName = this.unique( Names.group ).substring( 0, 24 );
    header(Names.group, groupName );
    client( ).createSecurityGroup( groupName, this.toString( ) );
    GroupDescription group = Iterables.find( client( ).describeSecurityGroups( EMPTY ), new Predicate<GroupDescription>( ) {
      @Override
      public boolean apply( GroupDescription arg0 ) {
        return groupName.matches( arg0.getName( ) );
      }
    } );
    this.assets.setGroup( group );
    client( ).authorizeSecurityGroupIngress( this.assets.getGroup( ).getName( ), Protocols.tcp.name( ), 22, 22, "0.0.0.0/0" );
    client( ).authorizeSecurityGroupIngress( this.assets.getGroup( ).getName( ), Protocols.icmp.name( ), -1, -1, "0.0.0.0/0" );
    group = Iterables.find( client( ).describeSecurityGroups( EMPTY ), new Predicate<GroupDescription>( ) {
      @Override
      public boolean apply( GroupDescription arg0 ) {
        return groupName.matches( arg0.getName( ) );
      }
    } );
    this.assets.setGroup( group );
    header(Names.group, group );
    assertNotNull( group );
    assertFalse( group.getPermissions( ).isEmpty( ) );
    
    header( Names.image );
    ImageDescription img = Iterables.find( client().describeImages( EMPTY ), new Predicate<ImageDescription>( ) {
      @Override
      public boolean apply( ImageDescription arg0 ) {
        return ImageTypes.machine.name( ).equals(arg0.getImageType( ));
      }
    } );
    this.assets.setImage( img );
    header( Names.image,img );
    assertNotNull( img );
  }
  
  
  @Test public void runTest() throws Exception {
    final String groupName = this.assets.getGroup( ).getName( );
    GroupDescription group = Iterables.find( client( ).describeSecurityGroups( EMPTY ), new Predicate<GroupDescription>( ) {
      @Override
      public boolean apply( GroupDescription arg0 ) {
        return groupName.matches( arg0.getName( ) );
      }
    } );
    assertNotNull( "Failed to lookup group: " + this.unique( Names.group ), this.assets.getGroup( ) );
    
    LaunchConfiguration config = this.assets.getLaunchConfiguration( );
    header(Names.instances,config);
    ReservationDescription rsv = client( ).runInstances( config );
    this.assets.setReservation( rsv );
    header(Names.instances,rsv);
    
    long start = System.currentTimeMillis( );
    final String rsvId = this.assets.getReservation( ).getReservationId( ); 
    while( System.currentTimeMillis( ) - start < TimeUnit.SECONDS.toMillis( 120 ) ) {
      TimeUnit.SECONDS.sleep( 1 );
      
      ReservationDescription mine = Iterables.find( client( ).describeInstances( EMPTY ), new Predicate<ReservationDescription>(){
      @Override
      public boolean apply( ReservationDescription arg0 ) {
        return rsvId.equals( arg0.getReservationId( ) );
      }});
      assertNotNull( mine );
      header("WAITING", mine);
      if( Iterables.all( mine.getInstances( ), new Predicate<Instance>( ) {
        @Override public boolean apply( Instance input ) { return input.isRunning( ); }
      } ) ) {
        break;
      }
    }
    
    sshAndCheckAddress( "SYSTEM_ADDRESS" );

    List<Instance> firstInstance = this.assets.getReservation( ).getInstances( );
    Instance pick = firstInstance.get( 0 );//TODO: make this try them all; need to allocate more addresses...
    client().associateAddress( pick.getInstanceId( ), this.assets.getAddress( ) );
    ReservationDescription assignedAddr = Iterables.find( client( ).describeInstances( EMPTY ), new Predicate<ReservationDescription>(){
      @Override
      public boolean apply( ReservationDescription arg0 ) {
        return rsvId.equals( arg0.getReservationId( ) );
      }});
    assertNotNull( assignedAddr );
    header("ASSIGNED_ADDRESS", assignedAddr);
    sshAndCheckAddress( "ASSIGNED_ADDRESS" );
    client().disassociateAddress( this.assets.getAddress( ) );
    ReservationDescription unassignedAddr = Iterables.find( client( ).describeInstances( EMPTY ), new Predicate<ReservationDescription>(){
      @Override
      public boolean apply( ReservationDescription arg0 ) {
        return rsvId.equals( arg0.getReservationId( ) );
      }});
    assertNotNull( unassignedAddr );
    header("UNASSIGNED_ADDRESS", unassignedAddr);
  }

  private void sshAndCheckAddress( String phaseName ) throws InterruptedException, EC2Exception {
    for ( int i = 0; i < 100; i++ ) {
      TimeUnit.SECONDS.sleep( 2 );
      Iterable<ReservationDescription> reservations = Iterables.filter( client( ).describeInstances( EMPTY ), new Predicate<ReservationDescription>(){
        @Override
        public boolean apply( ReservationDescription input ) {
          return DanTest.this.assets.getReservation( ).getReservationId( ).equals( input.getReservationId( ) );
        }
      });
      List<String> ipInfo = Lists.transform( reservations.iterator( ).next( ).getInstances( ), new Function<Instance, String>( ) {
        @Override
        public String apply( Instance from ) {
          try {
            LOG.info( "Trying to ssh to instance: " + from.toString( ) );
            return SshUtil.exec( unique( "trying-ssh" ), DanTest.this.assets.getKeypair( ).getKeyMaterial( ), from.getDnsName( ), "ip addr show dev eth0" );
          } catch ( Exception e ) {
            return "FAIL";
          }
        }
      } );
      for ( String out : ipInfo ) {
        header( phaseName, out );//TODO: check valid? -- already would get an exception in real failure.
        if( !"FAIL".equals( out ) ) {
          return;
        }
      }
    }
    List<String> consoleOutput = Lists.transform( this.assets.getReservation( ).getInstances( ), new Function<Instance,String>() {
      @Override public String apply( Instance from ) {
        try {
          return client( ).getConsoleOutput( from.getInstanceId( ) ).getOutput( );
        } catch ( Exception e ) {
          return "FAIL";
        }
      }
    });
    for( String out : consoleOutput ) {
      header("CONSOLE_OUTPUT", consoleOutput );//TODO: trim this to a sane length
    }
    throw new RuntimeException( "Failed to ssh to instances." );
  }

  @After public void teardown() {
    try {
      header(Names.keypair, this.assets.getKeypair( ) );
      client( ).deleteKeyPair( this.assets.getKeypair( ).getKeyName( ) );
    } catch ( Throwable e ) {
      LOG.debug( e );
    }
    try {
      header(Names.group, this.assets.getGroup( ) );
      client( ).deleteSecurityGroup( this.assets.getGroup( ).getName( ) );
    } catch ( Throwable e ) {
      LOG.debug( e );
    }
    try {
      header(Names.instances, this.assets.getReservation( ) );
      final String rsvId = this.assets.getReservation( ).getReservationId( );
      final List<String> instanceIds = Lists.transform( this.assets.getReservation( ).getInstances( ), new Function<Instance,String>( ){
        @Override public String apply( Instance from ) { return from.getInstanceId( ); }        
      } );
      client( ).terminateInstances( instanceIds );
      long start = System.currentTimeMillis( );
      while( System.currentTimeMillis( ) - start < TimeUnit.SECONDS.toMillis( 30 ) ) {
        TimeUnit.SECONDS.sleep( 1 );
        
        ReservationDescription mine = Iterables.find( client( ).describeInstances( EMPTY ), new Predicate<ReservationDescription>(){
          @Override public boolean apply( ReservationDescription arg0 ) { return rsvId.equals( arg0.getReservationId( ) ); 
        }});
        assertNotNull( mine );
        header("WAITING", mine);
        if( Iterables.all( mine.getInstances( ), new Predicate<Instance>( ) {
          @Override public boolean apply( Instance input ) { return input.isTerminated( ); }
        } ) ) {
          client( ).terminateInstances( instanceIds );
          break;
        }
      }
    } catch ( Throwable e ) {
      LOG.debug( e, e );
    }
    if( this.isManaged( ) ) try {
      header(Names.address, this.assets.getAddress( ) );
      client( ).releaseAddress( this.assets.getAddress( ) );
    } catch ( Throwable e ) {
      LOG.debug( e );
    }
  }

  
  enum ImageTypes {
    machine, kernel, ramdisk
  }

  enum Protocols {
    tcp, udp, icmp
  }
  
  enum Names {
    group,keypair,image,address,instances;
  }
  
  static class TestAssets {
    private int instanceCount = 1;
    private KeyPairInfo keypair;
    private GroupDescription group;
    private ReservationDescription reservation;
    private ImageDescription image;
    private String address;
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
    public LaunchConfiguration getLaunchConfiguration() {
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
