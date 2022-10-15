package mint

import config.Configs.dumdumdumsNFT
import errors.ParseException
import explorer.Explorer
import json.ErgoJson
import node.{BaseClient, DefaultNodeInfo}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{BlockchainContext, InputBox, NetworkType}
import play.api.libs.json.JsResultException

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Client()
    extends BaseClient(nodeInfo = DefaultNodeInfo(NetworkType.MAINNET)) {}

class TweetExplorer @Inject() (implicit client: Client)
    extends Explorer(
      nodeInfo = DefaultNodeInfo(NetworkType.MAINNET)
    ) {

  def getProfileTokenDistributionInputBox: InputBox =
    client.getClient.execute { (ctx: BlockchainContext) =>
      try {
        val serviceBoxJson =
          getUnspentTokenBoxes(dumdumdumsNFT)

        var dumdumdumsTokenDistributionBox: InputBox =
          ctx.getBoxesById(ErgoJson.getBoxId(serviceBoxJson)).head
        val serviceAddress: String =
          (new ErgoAddressEncoder(NetworkType.MAINNET.networkPrefix))
            .fromProposition(dumdumdumsTokenDistributionBox.getErgoTree)
            .get
            .toString

        dumdumdumsTokenDistributionBox =
          findMempoolBox(serviceAddress, dumdumdumsTokenDistributionBox, ctx)

        dumdumdumsTokenDistributionBox
      } catch {
        case e: ParseException => {
          throw ParseException(e.getMessage)
        }
        case e: JsResultException => throw e
        case e: Throwable =>
          throw e
      }
    }
}
