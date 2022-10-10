package mint

import explorer.Explorer
import node.{BaseClient, DefaultNodeInfo}
import org.ergoplatform.appkit.NetworkType

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Client()
    extends BaseClient(nodeInfo = DefaultNodeInfo(NetworkType.MAINNET)) {}

class TweetExplorer @Inject() (implicit client: Client)
    extends Explorer(
      nodeInfo = DefaultNodeInfo(NetworkType.MAINNET)
    ) {}
