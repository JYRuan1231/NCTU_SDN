from mininet.topo import Topo

class topo_0856566( Topo ):
    def __init__( self ):
        Topo.__init__( self )

        # Add hosts
        h1 = self.addHost( 'h1' )
        h2 = self.addHost( 'h2' )

        # Add switches
        s1 = self.addSwitch( 's1' )
        s2 = self.addSwitch( 's2' )
        s3 = self.addSwitch( 's3' )

        # Add links
        self.addLink( h1, s1 )
        self.addLink( h2, s2 )
        self.addLink( s3, s2 )
        self.addLink( s3, s1 )
        self.addLink( s1, s2 )


        
        


topos = { 'topo_0856566': topo_0856566 }