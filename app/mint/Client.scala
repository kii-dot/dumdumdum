package mint

import config.Configs.dumdumdumsNFT
import contracts.ProfileBoxContract
import errors.ParseException
import explorer.Explorer
import io.circe.Json
import json.ErgoJson
import node.{BaseClient, DefaultNodeInfo}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoId, InputBox, NetworkType}
import play.api.libs.json.JsResultException
import profile.ProfileBox

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Client()
    extends BaseClient(nodeInfo = DefaultNodeInfo(NetworkType.MAINNET)) {}

class TweetExplorer @Inject() (implicit client: Client)
    extends Explorer(
      nodeInfo = DefaultNodeInfo(NetworkType.MAINNET)
    ) {

  def getNFTBox(tokenId: ErgoId): NFT = {
    try {
      val boxJson: Json = getBoxById(tokenId.toString)
      val txId: String = boxJson.hcursor.downField("spentTransactionId").as[String].getOrElse("")
      val tx: Json = getConfirmedTx(txId)
      val outputs: Array[Json] = tx.hcursor.downField("outputs").as[Array[Json]].getOrElse(null)
      val nftJson: Json = outputs.head
      val nftBox: NFT = NFT.from(nftJson)
      nftBox
    } catch {
      case e: ParseException => {
        throw ParseException(e.getMessage)
      }
      case e: JsResultException => throw e
      case e: Throwable =>
        throw e
    }
  }

  def getProfileBox(address: Address): ProfileBox = {
    try {
      val profileBoxInput = client.getAllUnspentBox(ProfileBoxContract.getContract(address)(client.getContext).contract.address).head
      ProfileBox.from(profileBoxInput)
    } catch {
      case e: ParseException => {
        throw ParseException(e.getMessage)
      }
      case e: JsResultException => throw e
      case e: Throwable =>
        throw e
    }
  }

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
