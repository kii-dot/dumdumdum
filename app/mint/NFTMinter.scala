package mint

import boxes.{Box, BoxWrapper, CustomBoxData}
import registers.Register
import commons.ErgCommons
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  Eip4Token,
  ErgoContract,
  ErgoId,
  ErgoToken,
  InputBox,
  OutBox,
  ReducedTransaction,
}
import txs.Tx

import java.security.MessageDigest
import javax.inject.Inject
import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`

case class NFTMinter @Inject() (client: Client, tweetProtocol: TweetProtocol) {

  def mintTweetTx(
    message: String,
    address: String
  ): Seq[(Address, ReducedTransaction)] = {
    val parsedAddress = Address.create(address);
    val mintTweet =
      tweetProtocol.mintTweet(parsedAddress, message, TweetAction.post)

    mintTweet
  }

  def reply(
    tweetId: String,
    message: String,
    address: String
  ): Seq[(Address, ReducedTransaction)] = {
    val parsedAddress = Address.create(address);
    val mintTweet = tweetProtocol.mintTweet(
      parsedAddress,
      message,
      TweetAction.reply(tweetId)
    )

    mintTweet
  }

  def retweet(
    tweetId: String,
    message: String,
    address: String
  ): Seq[(Address, ReducedTransaction)] = {
    val parsedAddress = Address.create(address);
    val mintTweet = tweetProtocol.mintTweet(
      parsedAddress,
      message,
      TweetAction.retweet(tweetId)
    )

    mintTweet
  }

}

class TweetProtocol @Inject() (client: Client) {

  def createIssuerBoxTx(address: Address) = {
    val unspentBox = client.getCoveringBoxesFor(address, ErgCommons.MinBoxFee)

    val createIssuerBoxTx =
      new CreateIssuerBoxTx(unspentBox.getBoxes.toSeq, address)(
        client.getContext
      )
    createIssuerBoxTx
  }

  def createIssuanceBoxTx(
    issuerBox: InputBox,
    address: Address,
    message: String,
    tweetAction: (Byte, String)
  ) = {
    val createIssuanceBoxTx =
      new CreateIssuanceBoxTx(Seq(issuerBox), address, message, tweetAction)(
        client.getContext
      )

    createIssuanceBoxTx
  }

  def mintTweet(
    address: Address,
    message: String,
    tweetAction: (Byte, String)
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
      issuerBoxTxOutBoxes.head,
      address,
      message,
      tweetAction = tweetAction
    )
    val reducedIssuanceBoxTxWithAddress: (Address, ReducedTransaction) =
      (address, issuanceBoxTx.reduceTx)

    Seq(reducedIssuerBoxTxWithAddress, reducedIssuanceBoxTxWithAddress)
  }
}

class CreateIssuerBoxTx(
  override val inputBoxes: Seq[InputBox],
  address: Address
)(implicit val ctx: BlockchainContext)
    extends Tx {
  override val changeAddress: P2PKAddress = address.asP2PK()

  override def getOutBoxes: Seq[OutBox] =
    Seq(
      ctx
        .newTxBuilder()
        .outBoxBuilder()
        .value(ErgCommons.MinBoxFee * 2)
        .contract(address.toErgoContract)
        .registers((new Register(10L)).toErgoValue.get)
        .build()
    )

  override def getCustomOutBoxes(customData: Seq[CustomBoxData]): Seq[OutBox] =
    ???
}

class CreateIssuanceBoxTx(
  override val inputBoxes: Seq[InputBox],
  address: Address,
  message: String,
  tweetAction: (Byte, String)
)(implicit val ctx: BlockchainContext)
    extends Tx {
  override val changeAddress: P2PKAddress = address.asP2PK()

  override def getOutBoxes: Seq[OutBox] =
    Seq(
      TweetBox(inputBoxes.head.getId, address = address, message, tweetAction)
        .getOutBox(ctx, ctx.newTxBuilder())
    )

  override def getCustomOutBoxes(customData: Seq[CustomBoxData]): Seq[OutBox] =
    ???
}

case class TweetBox(
  issuerBoxId: ErgoId,
  address: Address,
  message: String,
  tweetAction: (Byte, String),
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null),
  override val value: Long = ErgCommons.MinBoxFee
) extends BoxWrapper {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    address.toErgoContract

  override val tokens: Seq[ErgoToken] = Seq(
    new Eip4Token(
      issuerBoxId.toString,
      1,
      getTweetName,
      message,
      0,
      R7.get.toErgoValue.get,
      R8.get.toErgoValue.get,
      R9.get.toErgoValue.get
    )
  )

  def getTweetName: String =
    s"Tweet::${issuerBoxId.toString}"

  def getSHA256OfTweet: String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(message.getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString

  override def R4: Option[Register[_]] =
    Option(new Register[String](getTweetName))
  override def R5: Option[Register[_]] = Option(new Register[String](message))
  override def R6: Option[Register[_]] = Option(new Register[String]("0"))

  override def R7: Option[Register[_]] =
    Option(new Register[Array[Byte]](NFTType.tweet))

  override def R8: Option[Register[_]] =
    Option(new Register[String](getSHA256OfTweet))

  override def R9: Option[Register[_]] =
    Option(new Register[(Byte, String)](tweetAction))
}

object NFTType {
  val tweet: Array[Byte] = Array[Byte](1, 5);
}

object TweetAction {
  def post: (Byte, String) = (0, "")
  def reply(tweetId: String): (Byte, String) = (1, tweetId)
  def retweet(tweetId: String): (Byte, String) = (2, tweetId)
}
