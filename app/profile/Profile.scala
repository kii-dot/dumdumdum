package profile

import boxes.{Box, BoxWrapper}
import commons.ErgCommons
import contracts.ProfileBoxContract
import edge.registers.CollAddressRegister
import io.circe.Json
import mint.{Client, CreateIssuerBoxTx, NFT, TweetExplorer}
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.{Address, BlockchainContext, Eip4Token, ErgoContract, ErgoId, ErgoToken, InputBox, OutBox, ReducedTransaction}
import registers.Register
import txs.Tx

import java.security.MessageDigest
import javax.inject.Inject
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.convert.ImplicitConversions.`list asScalaBuffer`

class Profile @Inject() (client: Client, explorer: TweetExplorer) {

  def create(
    address: Address,
    nftId: String
  ): Seq[(Address, ReducedTransaction)] = {
    val createProfileTx: ProfileBoxCreationTx = new ProfileBoxCreationTx(
      userAddress = address,
      nftId = nftId,
      client = client,
      explorer = explorer
    )(client.getContext)

    Seq((address, createProfileTx.reduceTx))
  }

  def changeProfileNFT(
    address: Address,
    nftId: String
  ): Seq[(Address, ReducedTransaction)] = {
    val issuerBoxTx: ProfileBoxIssuerBoxTx = new ProfileBoxIssuerBoxTx(
      userAddress = address,
      nftId = nftId,
      client = client,
      explorer = explorer
    )(client.getContext)
    val reducedIssuerBoxTx: ReducedTransaction = issuerBoxTx.reduceTx

    val issuerBox: InputBox =
      issuerBoxTx.getOutBoxesAsInputBoxes(reducedIssuerBoxTx.getId).head

    val changeProfileNFTTx: ProfileBoxChangeProfileNFTTx =
      new ProfileBoxChangeProfileNFTTx(
        userAddress = address,
        nftId = nftId,
        client = client,
        explorer = explorer,
        txFeeBox = issuerBox
      )(client.getContext)

    Seq((address, reducedIssuerBoxTx), (address, changeProfileNFTTx.reduceTx))
  }

  def delete(address: Address): Seq[(Address, ReducedTransaction)] = {
    val issuerBoxTx: ProfileBoxIssuerBoxTx = new ProfileBoxIssuerBoxTx(
      userAddress = address,
      client = client,
      explorer = explorer
    )(client.getContext)
    val reducedIssuerBoxTx: ReducedTransaction = issuerBoxTx.reduceTx
    val issuerBox: InputBox =
      issuerBoxTx.getOutBoxesAsInputBoxes(reducedIssuerBoxTx.getId).head

    val deleteProfileBoxTx: DeleteProfileBoxTx = new DeleteProfileBoxTx(
      userAddress = address,
      client = client,
      txFeeBox = issuerBox
    )(client.getContext)

    Seq((address, reducedIssuerBoxTx), (address, deleteProfileBoxTx.reduceTx))
  }

  def follow(
    walletAddress: Address,
    addressToFollow: Address
  ): Seq[(Address, ReducedTransaction)] = {
    val issuerBoxTx: ProfileBoxIssuerBoxTx = new ProfileBoxIssuerBoxTx(
      userAddress = walletAddress,
      client = client,
      explorer = explorer,
      addressToFollow = Option(addressToFollow)
    )(client.getContext)
    val reducedIssuerBoxTx: ReducedTransaction = issuerBoxTx.reduceTx
    val issuerBox: InputBox =
      issuerBoxTx.getOutBoxesAsInputBoxes(reducedIssuerBoxTx.getId).head

    val followingTx: ProfileBoxAddFollowingTx = new ProfileBoxAddFollowingTx(
      userAddress = walletAddress,
      txFeeBox = issuerBox,
      client = client,
      addressToFollow = addressToFollow
    )(client.getContext)

    Seq(
      (walletAddress, reducedIssuerBoxTx),
      (walletAddress, followingTx.reduceTx)
    )
  }

  def unfollow(
    walletAddress: Address,
    addressToUnfollow: Address
  ): Seq[(Address, ReducedTransaction)] = {
    val issuerBoxTx: ProfileBoxIssuerBoxTx = new ProfileBoxIssuerBoxTx(
      userAddress = walletAddress,
      client = client,
      explorer = explorer,
      addressToFollow = Option(addressToUnfollow)
    )(client.getContext)
    val reducedIssuerBoxTx: ReducedTransaction = issuerBoxTx.reduceTx
    val issuerBox: InputBox =
      issuerBoxTx.getOutBoxesAsInputBoxes(reducedIssuerBoxTx.getId).head

    val unfollowTx: ProfileBoxRemoveFollowingTx =
      new ProfileBoxRemoveFollowingTx(
        userAddress = walletAddress,
        client = client,
        addressToUnfollow = addressToUnfollow,
        txFeeBox = issuerBox
      )(client.getContext)

    Seq(
      (walletAddress, reducedIssuerBoxTx),
      (walletAddress, unfollowTx.reduceTx)
    )
  }

  def getFollowing(address: Address): CollAddressRegister = {
    val profileInputBox: InputBox = client.getAllUnspentBox(ProfileBoxContract.getContract(address)(client.getContext).contract.address)
      .head

    val profileBox: ProfileBox = ProfileBox.from(profileInputBox)

    profileBox.followingRegister
  }

  def getProfileNFTDetails(address: Address): NFT = {
    val profileBox: ProfileBox = explorer.getProfileBox(address)
    val nft: NFT = explorer.getNFTBox(profileBox.tokens(1).getId)
    nft
  }
}

class DumDumDumHandler @Inject() (client: Client) {

  def createIssuerBoxTx(address: Address): CreateIssuerBoxTx = {
    val unspentBox =
      client.getCoveringBoxesFor(address, ErgCommons.MinBoxFee * 2)

    val createIssuerBoxTx =
      new CreateIssuerBoxTx(unspentBox.getBoxes.toSeq, address)(
        client.getContext
      )
    createIssuerBoxTx
  }

  def createIssuanceBoxTx(
    issuerBox: InputBox,
    address: Address,
    name: String,
    description: String,
    amount: Long,
    optionalLink: String = ""
  ): CreateTokenIssuanceBoxTx = {
    val createIssuanceBoxTx =
      new CreateTokenIssuanceBoxTx(
        Seq(issuerBox),
        address = address,
        name = name,
        description = description,
        amount = amount,
        optionalLink = optionalLink
      )(
        client.getContext
      )

    createIssuanceBoxTx
  }

  def mint(
    address: Address,
    name: String,
    description: String,
    amount: Long,
    optionalLink: String = ""
  ): Seq[(Address, ReducedTransaction)] = {
    val issuerBoxTx = createIssuerBoxTx(address)
    val issuerBoxUnsignedTx = issuerBoxTx.buildTx
    val reducedIssuerBoxTxWithAddress: (Address, ReducedTransaction) = (
      address,
      client.getContext
        .newProverBuilder()
        .build()
        .reduce(issuerBoxUnsignedTx, 0)
    )
    val issuerBoxTxOutBoxes =
      issuerBoxTx.getOutBoxesAsInputBoxes(issuerBoxUnsignedTx.getId)

    val issuanceBoxTx = createIssuanceBoxTx(
      issuerBox = issuerBoxTxOutBoxes.head,
      address = address,
      name = name,
      description = description,
      amount = amount,
      optionalLink = optionalLink
    )
    val reducedIssuanceBoxTxWithAddress: (Address, ReducedTransaction) =
      (address, issuanceBoxTx.reduceTx)

    Seq(reducedIssuerBoxTxWithAddress, reducedIssuanceBoxTxWithAddress)
  }

  def burn(
    address: Address,
    nftId: String,
    amount: Long
  ): Seq[(Address, ReducedTransaction)] = {
    val tokenList = Seq(new ErgoToken(nftId, amount))
    val unspentBox = client.getCoveringBoxesFor(
      address,
      ErgCommons.MinBoxFee * 2,
      tokensToSpend = tokenList.asJava
    )

    val burnTx =
      new BurnTokenTx(
        Seq(unspentBox: _*),
        address = address,
        tokensToBurn = tokenList
      )(client.getContext)

    Seq((address, burnTx.reduceTx))
  }
}

class BurnTokenTx(
  override val inputBoxes: Seq[InputBox],
  address: Address,
  override val tokensToBurn: Seq[ErgoToken]
)(implicit val ctx: BlockchainContext)
    extends Tx {
  override val changeAddress: P2PKAddress = address.asP2PK()

  override def getOutBoxes: Seq[OutBox] = Seq(
    ctx
      .newTxBuilder()
      .outBoxBuilder()
      .value(ErgCommons.MinBoxFee)
      .contract(address.toErgoContract)
      .build()
  )
}

class CreateTokenIssuanceBoxTx(
  override val inputBoxes: Seq[InputBox],
  address: Address,
  name: String,
  description: String,
  amount: Long = 1,
  optionalLink: String = ""
)(implicit val ctx: BlockchainContext)
    extends Tx {
  override val changeAddress: P2PKAddress = address.asP2PK()

  override def getOutBoxes: Seq[OutBox] =
    Seq(
      NFTBox(
        inputBoxes.head.getId,
        address = address,
        name = name,
        description = description,
        amount = amount,
        optionalLink = optionalLink
      ).getOutBox(ctx, ctx.newTxBuilder())
    )
}

case class NFTBox(
  issuerBoxId: ErgoId,
  address: Address,
  name: String,
  description: String,
  amount: Long,
  optionalLink: String = "",
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null),
  override val value: Long = ErgCommons.MinBoxFee
) extends BoxWrapper {

  override val tokens: Seq[ErgoToken] = Seq(
    new Eip4Token(
      issuerBoxId.toString,
      amount,
      name,
      description,
      0,
      R7.get.toErgoValue.get,
      R8.get.toErgoValue.get,
      R9.get.toErgoValue.get
    )
  )

  def getSHA256OfDescription: String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(description.getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    address.toErgoContract

  override def R4: Option[Register[_]] =
    Option(new Register[String](name))

  override def R5: Option[Register[_]] =
    Option(new Register[String](description))
  override def R6: Option[Register[_]] = Option(new Register[String]("0"))

  override def R7: Option[Register[_]] =
    Option(
      new Register[Array[Byte]](
        Eip4Token.AssetType.MEMBERSHIP_THRESHOLD_SIG.getR7ByteArrayForType
      )
    )

  override def R8: Option[Register[_]] =
    Option(new Register[String](getSHA256OfDescription))

  override def R9: Option[Register[_]] =
    Option(new Register[String](optionalLink))
}

case class DumDumDumBox(
  address: Address,
  override val value: Long = ErgCommons.MinBoxFee,
  override val tokens: Seq[ErgoToken],
  override val id: ErgoId = null,
  override val box: Option[Box] = Option.empty
) extends BoxWrapper {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    address.toErgoContract
}

case class CreateDumDumDumsTx(address: Address, inputBoxes: Seq[InputBox])(
  implicit val ctx: BlockchainContext
) extends Tx {
  override val changeAddress: P2PKAddress = address.asP2PK()

  override def getOutBoxes: Seq[OutBox] = {
    val dumdumdumToken: ErgoToken = new Eip4Token(
      inputBoxes.head.getId.toString,
      Long.MaxValue,
      "DumDumDums Profile Token",
      "",
      0
    )
    val dumDumDumBox: DumDumDumBox =
      DumDumDumBox(address, tokens = Seq(dumdumdumToken))
    Seq(dumDumDumBox.getOutBox(ctx, ctx.newTxBuilder()))
  }
}

case class CreateDumDumDumsNFTTx(address: Address, inputBoxes: Seq[InputBox])(
  implicit val ctx: BlockchainContext
) extends Tx {
  override val changeAddress: P2PKAddress = address.asP2PK()

  override def getOutBoxes: Seq[OutBox] = {
    val dumdumdumsNFTToken: ErgoToken = new Eip4Token(
      inputBoxes.head.getId.toString,
      1,
      "DumDumDums ProfileBox NFT",
      "This is the token to identify the DumDumDums Profile Box",
      0
    )
    val dumDumDumsNFTBox: DumDumDumBox =
      DumDumDumBox(address, tokens = Seq(dumdumdumsNFTToken))
    Seq(dumDumDumsNFTBox.getOutBox(ctx, ctx.newTxBuilder()))
  }
}
