package contracts

import boxes.FundsToAddressBox
import commons.ErgCommons
import config.Configs.{dumdumdumsProfileToken, serviceFee, serviceOwner}
import edge.registers.{AddressRegister, CollAddressRegister, CollStringRegister}
import mint.{Client, TweetExplorer}
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoClient, ErgoProver, ErgoToken, InputBox, NetworkType, OutBox, Parameters, RestApiErgoClient, SecretString}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import profile.{ProfileBox, ProfileTokenDistributionBox}
import txs.Tx

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

  val nftId2: String =
    "83615ca01096203a23e246d3172d83fe6307986bdd4594657620c9d285a4d037"

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
    nftId: String = "",
    followerArray: Seq[Address] = Seq(address)
  ): ProfileBox = {
    val profileBox: ProfileBox = ProfileBox(
      addressRegister = new AddressRegister(address),
      tokens = if (nftId.nonEmpty) Seq(new ErgoToken(dumdumdumsProfileToken, 1), new ErgoToken(nftId, 1)) else {
        Seq(new ErgoToken(dumdumdumsProfileToken, 1))
      },
      followingRegister = new CollAddressRegister(followerArray)
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
        FundsToAddressBox(Address.create(serviceOwner), serviceFee)
          .getOutBox(ctx, ctx.newTxBuilder())

      Seq(outProfileTokenDistributionBox, profileBox, serviceFeeBox)
    }
  }

  class ProfileBox_ChangeNFTTestTx()(implicit val ctx: BlockchainContext)
    extends Tx {
        override val changeAddress: P2PKAddress = address.asP2PK()

    override val inputBoxes: Seq[InputBox] = {
      val profileBox: ProfileBox = createProfileBox(address, nftId = nftId2)
      val profileAsInputBox: InputBox = profileBox.getAsInputBox(ctx, ctx.newTxBuilder(), Tx.dummyTxId, 0)
      val userTx: FundsToAddressBox = FundsToAddressBox(
        value = ErgCommons.MinMinerFee * 3,
        address = address,
        tokens = Seq(new ErgoToken(nftId, 1)),
        R4 = Option(new AddressRegister(address))
      )
      val userTxAsInputBox: InputBox = userTx.getAsInputBox(ctx, ctx.newTxBuilder(), Tx.dummyTxId, 0)

      Seq(profileAsInputBox, userTxAsInputBox)
    }

    override def getOutBoxes: Seq[OutBox] = {
      val profileBox: ProfileBox = createProfileBox(address, nftId)
      val profileBoxAsOutBox: OutBox = profileBox.getOutBox(ctx, ctx.newTxBuilder())
      val userTx: OutBox = FundsToAddressBox(
        address = address,
        tokens = Seq(new ErgoToken(nftId2, 1)),
        R4 = Option(new AddressRegister(address))
      ).getOutBox(ctx, ctx.newTxBuilder())
      Seq(profileBoxAsOutBox, userTx)
    }
  }

  class ProfileBox_DeleteTestTx()(implicit val ctx: BlockchainContext)
    extends Tx {
    override val changeAddress: P2PKAddress = address.asP2PK()

    override val inputBoxes: Seq[InputBox] = {
      val profileBox: ProfileBox = createProfileBox(address, nftId)
      val profileAsInputBox: InputBox = profileBox.getAsInputBox(ctx, ctx.newTxBuilder(), Tx.dummyTxId, 0)
      val userTx: FundsToAddressBox = FundsToAddressBox(
        value = ErgCommons.MinMinerFee * 2,
        address = address,
        R4 = Option(new AddressRegister(address))
      )
      val userTxAsInputBox: InputBox = userTx.getAsInputBox(ctx, ctx.newTxBuilder(), Tx.dummyTxId, 0)

      Seq(profileAsInputBox, userTxAsInputBox)
    }

    override def getOutBoxes: Seq[OutBox] = {
      val userTx: FundsToAddressBox = FundsToAddressBox(
        address,
        tokens = Seq(new ErgoToken(nftId, 1)),
        R4 = Option(new AddressRegister(address)),
        R5 = Option(CollStringRegister.empty)
      )
      val userTxAsOutBox: OutBox = userTx.getOutBox(ctx, ctx.newTxBuilder())

      Seq(userTxAsOutBox)
    }
  }

  class ProfileBox_AddFollowingTestTx()(implicit val ctx: BlockchainContext)
    extends Tx {
    override val changeAddress: P2PKAddress = address.asP2PK()

    override val inputBoxes: Seq[InputBox] = {
      val profileBox: ProfileBox = createProfileBox(address, nftId)
      val profileAsInputBox: InputBox = profileBox.getAsInputBox(ctx, ctx.newTxBuilder(), Tx.dummyTxId, 0)
      val userTx: FundsToAddressBox = FundsToAddressBox(address, R4 = Option(new AddressRegister(exleDevAddress)))
      val userTxAsInputBox: InputBox = userTx.getAsInputBox(ctx, ctx.newTxBuilder(), Tx.dummyTxId, 0)

      Seq(profileAsInputBox, userTxAsInputBox)
    }

    override def getOutBoxes: Seq[OutBox] = {
      val profileBox: ProfileBox = createProfileBox(address, nftId, followerArray = Seq(address, exleDevAddress))
      val profileBoxAsOutBox: OutBox = profileBox.getOutBox(ctx, ctx.newTxBuilder())

      Seq(profileBoxAsOutBox)
    }
  }

  class ProfileBox_RemoveFollowingTestTx()(implicit val ctx: BlockchainContext)
    extends Tx {
    override val changeAddress: P2PKAddress = address.asP2PK()

    override val inputBoxes: Seq[InputBox] = {
      val profileBox: ProfileBox = createProfileBox(address, nftId, followerArray = Seq(address, exleDevAddress))
      val profileAsInputBox: InputBox = profileBox.getAsInputBox(ctx, ctx.newTxBuilder(), Tx.dummyTxId, 0)
      val userTx: FundsToAddressBox = FundsToAddressBox(
        address,
        R4 = Option(new AddressRegister(exleDevAddress)))
      val userTxAsInputBox: InputBox = userTx.getAsInputBox(ctx, ctx.newTxBuilder(), Tx.dummyTxId, 0)

      Seq(profileAsInputBox, userTxAsInputBox)
    }

    override def getOutBoxes: Seq[OutBox] = {
      val profileBox: ProfileBox = createProfileBox(address, nftId)
      val profileBoxAsOutBox: OutBox = profileBox.getOutBox(ctx, ctx.newTxBuilder())

      Seq(profileBoxAsOutBox)
    }
  }

  "Profile Distribution Box: CREATION" should {
    val ctx = client.getContext

    val testTx = new CreateProfileBoxTestTx()(client.getContext)

    "pass" in {
      dummyProver.sign(testTx.buildTx)
    }
  }

  "Profile Box: Change NFT" should {
    "change successfully with owned NFT" in {
      val testTx = new ProfileBox_ChangeNFTTestTx()(client.getContext)

      dummyProver.sign(testTx.buildTx)
    }

    "nft register isEmpty array if remove profile nft" in {}
  }

  "Profile Box: Delete Profile" should {
    "remove profile box" in {
      val testTx = new ProfileBox_DeleteTestTx()(client.getContext)

      dummyProver.sign(testTx.buildTx)
    }
  }

  "Profile Box: Add Following" should {
    "add a following" in {
      val testTx = new ProfileBox_AddFollowingTestTx()(client.getContext)
      dummyProver.sign(testTx.buildTx)
    }
    "fail if following already exists" in {}
  }

  "Profile Box: Remove Following" should {
    "remove following if exists" in {
      val testTx = new ProfileBox_RemoveFollowingTestTx()(client.getContext)

      dummyProver.sign(testTx.buildTx)
    }

    "fail if following does not exists" in {}
  }
}
