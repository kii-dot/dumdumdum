package contracts

import boxes.FundsToAddressBox
import commons.ErgCommons
import config.Configs.{serviceFee, serviceOwner}
import edge.registers.{AddressRegister, CollByteRegister, CollStringRegister}
import mint.{Client, TweetExplorer}
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoClient, ErgoId, ErgoProver, ErgoToken, InputBox, NetworkType, OutBox, Parameters, RestApiErgoClient, SecretString}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import profile.{ProfileBox, ProfileTokenDistributionBox}
import txs.Tx

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`

class ProfileTxSpec extends AnyWordSpec with Matchers {
  val networkType: NetworkType = NetworkType.MAINNET

  // Set client in test that inherits
  val client: Client = new Client()
  client.setClient()

  val explorer: TweetExplorer = new TweetExplorer()(client)
  val trueAddress: Address = Address.create("4MQyML64GnzMxZgm")

  val exleDevAddress: Address =
    Address.create("9f83nJY4x9QkHmeek6PJMcTrf2xcaHAT3j5HD5sANXibXjMUixn")

  val addressStr: String = "9i5FPuvo3vYKCTaDQkk7vUDrvnfXHW6GpkWxSKrVFWGmBtzkQWg"
  val address: Address = Address.create(addressStr)

  val nftId: String =
    "322216b9606ac80a41601642707dfaa4461bcffe9ccb08f3fd59d967bdabdeb4"

  def dummyProver: ErgoProver =
    client.getClient.execute { ctx =>
      val configFileName = "ergo_config.json"
      val conf: ErgoToolConfig = ErgoToolConfig.load(configFileName)
      val nodeConf: ErgoNodeConfig = conf.getNode
      val explorerUrl: String =
        RestApiErgoClient.getDefaultExplorerUrl(NetworkType.MAINNET)

      val addressIndex: Int = conf.getParameters.get("addressIndex").toInt

      // create ergoClient instance
      val ergoClient: ErgoClient =
        RestApiErgoClient.create(nodeConf, explorerUrl)

      val prover: ErgoProver = ergoClient.execute { ctx =>
        ctx.newProverBuilder
          .withMnemonic(
            SecretString.create(nodeConf.getWallet.getMnemonic),
            SecretString.create("")
          )
          .withEip3Secret(addressIndex)
          .build()
      }

      return prover
    }

  def createProfileBox(
    address: Address = address,
    nftId: String = ""
  ): ProfileBox = {
    val profileBox: ProfileBox = ProfileBox(
      addressRegister = new AddressRegister(address),
      profilePictureRegister =
        new CollByteRegister(ErgoId.create(nftId).getBytes),
      followingRegister = CollStringRegister.empty
    )

    profileBox
  }

  def createProfileTokenDistributionBox(): ProfileTokenDistributionBox = {
    val profileTokenDistributionBox: ProfileTokenDistributionBox =
      ProfileTokenDistributionBox()
    profileTokenDistributionBox
  }

  class CreateProfileBoxTestTx()(implicit val ctx: BlockchainContext)
      extends Tx {
    override val changeAddress: P2PKAddress = address.asP2PK()

    val profileTokenDistributionBox: InputBox = {
      createProfileTokenDistributionBox().getAsInputBox(
        ctx,
        ctx.newTxBuilder(),
        Tx.dummyTxId,
        0
      )
    }

    override val dataInputs: Seq[InputBox] = {
      val txFeeFromUser: Seq[InputBox] = client
        .getCoveringBoxesFor(
          address,
          ErgCommons.MinMinerFee,
          tokensToSpend = Seq(new ErgoToken(nftId, 1)).toList.asJava
        )
        .filter(inputBox =>
          inputBox.getTokens.exists(token => token.getId.toString == nftId)
        )
      txFeeFromUser
    }

    override val inputBoxes: Seq[InputBox] = {

      val txFeeFromUser: Seq[InputBox] = client
        .getCoveringBoxesFor(address, ErgCommons.MinMinerFee * 2 + serviceFee)
        .getBoxes
        .toSeq
      Seq(profileTokenDistributionBox) ++ txFeeFromUser
    }

    override def getOutBoxes: Seq[OutBox] = {
      val outProfileTokenDistributionBox: OutBox = ProfileTokenDistributionBox
        .decrementProfileToken(
          ProfileTokenDistributionBox.from(profileTokenDistributionBox)
        )
        .getOutBox(ctx, ctx.newTxBuilder())
      val profileBox: OutBox =
        createProfileBox(nftId = nftId).getOutBox(ctx, ctx.newTxBuilder())

      val serviceFeeBox: OutBox =
        FundsToAddressBox(Address.create(serviceOwner), serviceFee).getOutBox(ctx, ctx.newTxBuilder())

      Seq(outProfileTokenDistributionBox, profileBox, serviceFeeBox)
    }
  }

//  class ProfileBox_ChangeNFTTestTx()(implicit val ctx: BlockchainContext)
//    extends Tx {
//        override val changeAddress: P2PKAddress = address.asP2PK()
//
//    override val dataInputs: Seq[InputBox] = {}
//
//    override val inputBoxes: Seq[InputBox] = {
//    }
//
//    override def getOutBoxes: Seq[OutBox] = {
//    }
//  }
//
//  class ProfileBox_DeleteTestTx()(implicit val ctx: BlockchainContext)
//    extends Tx {
//    override val changeAddress: P2PKAddress = address.asP2PK()
//
//    override val dataInputs: Seq[InputBox] = {}
//
//    override val inputBoxes: Seq[InputBox] = {
//    }
//
//    override def getOutBoxes: Seq[OutBox] = {
//    }
//  }
//
//  class ProfileBox_AddFollowingTestTx()(implicit val ctx: BlockchainContext)
//    extends Tx {
//    override val changeAddress: P2PKAddress = address.asP2PK()
//
//    override val dataInputs: Seq[InputBox] = {}
//
//    override val inputBoxes: Seq[InputBox] = {
//    }
//
//    override def getOutBoxes: Seq[OutBox] = {
//    }
//  }
//
//  class ProfileBox_RemoveFollowingTestTx()(implicit val ctx: BlockchainContext)
//    extends Tx {
//    override val changeAddress: P2PKAddress = address.asP2PK()
//
//    override val dataInputs: Seq[InputBox] = {}
//
//    override val inputBoxes: Seq[InputBox] = {
//    }
//
//    override def getOutBoxes: Seq[OutBox] = {
//    }
//  }

  "Profile Distribution Box: CREATION" should {
    val ctx = client.getContext

    val testTx = new CreateProfileBoxTestTx()(client.getContext)

    "pass" in {
      dummyProver.sign(testTx.buildTx)
    }
  }

  "Profile Box: Change NFT" should {
    "change successfully with owned NFT" in {
    }

    "change fail if does not own nft" in {
    }

    "nft register isEmpty array if remove profile nft" in {}
  }

  "Profile Box: Delete Profile" should {
    "remove profile box" in {
    }
  }

  "Profile Box: Add Following" should {
    "add a following" in {}
    "fail if following already exists" in {}
  }

  "Profile Box: Remove Following" should {
    "remove following if exists" in {}

    "fail if following does not exists" in {}
  }
}
