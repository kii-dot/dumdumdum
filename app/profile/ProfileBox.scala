package profile

import boxes.{Box, BoxWrapper, BoxWrapperHelper}
import commons.ErgCommons
import config.Configs.{dumdumdumsNFT, dumdumdumsProfileToken, serviceFee, serviceOwner}
import contracts.{ProfileBoxContract, ProfileTokenDistributionBoxContract}
import edge.registers.{AddressRegister, CollByteRegister, CollStringRegister, LongRegister, StringRegister}
import explorer.Explorer
import io.circe.Json
import mint.Client
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoContract, ErgoId, ErgoToken, InputBox}
import registers.Register
import special.collection.Coll
import tokens.TokenHelper

import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`

case class ProfileBox(
  addressRegister: AddressRegister,
  profilePictureRegister: CollByteRegister,
  followingRegister: CollStringRegister,
  override val tokens: Seq[ErgoToken] = Seq(
    new ErgoToken(dumdumdumsProfileToken, 1)
  ),
  override val value: Long = ErgCommons.MinBoxFee,
  override val id: ErgoId = null,
  override val box: Option[Box] = Option.empty
) extends BoxWrapper {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    ProfileBoxContract
      .getContract(addressRegister.getAddress)
      .contract
      .ergoContract

  override def R4: Option[Register[_]] = Option(addressRegister)
  override def R5: Option[Register[_]] = Option(profilePictureRegister)
  override def R6: Option[Register[_]] = Option(followingRegister)
}

object ProfileBox {

  def get(address: Address)(implicit client: Client): InputBox = {
    implicit val ctx: BlockchainContext = client.getContext
    val profileInputBox: InputBox = client
      .getCoveringBoxesFor(
        ProfileBoxContract.getContract(address).contract.ergoContract.toAddress,
        ErgCommons.MinMinerFee
      )
      .getBoxes
      .head

    profileInputBox
  }

  def from(inputBox: InputBox): ProfileBox =
    ProfileBox(
      value = inputBox.getValue,
      id = inputBox.getId,
      tokens = inputBox.getTokens.toSeq,
      addressRegister = new AddressRegister(
        inputBox.getRegisters.get(0).getValue.asInstanceOf[Coll[Byte]].toArray
      ),
      profilePictureRegister = new CollByteRegister(
        inputBox.getRegisters.get(1).getValue.asInstanceOf[Coll[Byte]].toArray
      ),
      followingRegister = new CollStringRegister(
        inputBox.getRegisters
          .get(2)
          .getValue
          .asInstanceOf[Coll[Coll[Byte]]]
          .toArray
      ),
      box = Option(Box(inputBox))
    )

  def changeNFT(profileBox: ProfileBox, nftId: String): ProfileBox =
    profileBox.copy(profilePictureRegister =
      new CollByteRegister(ErgoId.create(nftId).getBytes)
    )
}

case class ProfileTokenDistributionBox(
  override val id: ErgoId = null,
  override val tokens: Seq[ErgoToken] = Seq(
    new ErgoToken(dumdumdumsNFT, 1),
    new ErgoToken(dumdumdumsProfileToken, 1000)
  ),
  override val value: Long = ErgCommons.MinBoxFee,
  override val box: Option[Box] = Option.empty
) extends BoxWrapper {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    ProfileTokenDistributionBoxContract.getContract.contract.ergoContract

  override def R4: Option[Register[_]] = Option(new LongRegister(serviceFee))
  override def R5: Option[Register[_]] = Option(new AddressRegister(serviceOwner))
}

object ProfileTokenDistributionBox extends BoxWrapperHelper {

  override def from(inputBox: InputBox): ProfileTokenDistributionBox =
    ProfileTokenDistributionBox(
      id = inputBox.getId,
      tokens = inputBox.getTokens.toSeq,
      value = inputBox.getValue,
      box = Option(Box(inputBox))
    )

  def decrementProfileToken(
    profileTokenDistributionBox: ProfileTokenDistributionBox
  ): ProfileTokenDistributionBox = {
    val newTokenList: Seq[ErgoToken] =
      profileTokenDistributionBox.tokens.map(
        TokenHelper.decrement(_, ErgoId.create(dumdumdumsProfileToken))
      )
    profileTokenDistributionBox.copy(
      tokens = newTokenList
    )
  }
}

object NFTHelper {

  def getNFTInputBox(
    nftId: String
  )(explorer: Explorer, ctx: BlockchainContext): InputBox = {
    val nftJson: Json = explorer.getUnspentTokenBoxes(nftId)
    val nftBox: InputBox = ctx
      .getBoxesById(nftJson.hcursor.downField("boxId").as[String].getOrElse(""))
      .head

    nftBox
  }
}
