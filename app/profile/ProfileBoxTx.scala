package profile

import commons.ErgCommons
import config.Configs.{dumdumdumsNFT, dumdumdumsProfileToken}
import contracts.ProfileBoxContract
import edge.registers.{AddressRegister, CollStringRegister, StringRegister}
import mint.{Client, TweetExplorer}
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoToken, InputBox, OutBox}
import txs.Tx

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`

/**
  * // ============== Profile Box Creation TX ================= //
  * inputs:  1. ProfileDistributionTokenBox, 2. TxFeeFromUser
  * outputs: 1. ProfileDistributionTokenBox, 2. ProfileBox, 3. MiningBox
  * dataInputs: 1. BoxWithNFT (that user owns)
  * @param ctx Blockchain Context
  */
case class ProfileBoxCreationTx(
  userAddress: Address,
  nftId: String,
  client: Client,
  explorer: TweetExplorer
)(
  implicit val ctx: BlockchainContext
) extends Tx {
  override val changeAddress: P2PKAddress = userAddress.asP2PK()
  val profileTokenDistributionBox: InputBox = ctx.getBoxesById(
    explorer.getUnspentTokenBoxes(dumdumdumsNFT).hcursor.downField("boxId").as[String].getOrElse("")
  ).head

  override val inputBoxes: Seq[InputBox] = {
    val txFeeFromUser: Seq[InputBox] =
      client.getCoveringBoxesFor(userAddress, ErgCommons.MinMinerFee * 2).getBoxes.toSeq

    Seq(profileTokenDistributionBox) ++ txFeeFromUser
  }

  override val dataInputs: Seq[InputBox] = {
    val nftBox: InputBox = NFTHelper.getNFTInputBox(nftId)(explorer, ctx)
    Seq(nftBox)
  }

  override def getOutBoxes: Seq[OutBox] =
  {
    val outProfileTokenDistributionBox: ProfileTokenDistributionBox =
      ProfileTokenDistributionBox.decrementProfileToken(
        ProfileTokenDistributionBox.from(profileTokenDistributionBox))

    val profileBox: ProfileBox = ProfileBox(
      addressRegister = new AddressRegister(userAddress),
      profilePictureRegister = new StringRegister(nftId),
      followingRegister = CollStringRegister.empty
    )

    Seq(outProfileTokenDistributionBox.getOutBox(ctx, ctx.newTxBuilder()),
      profileBox.getOutBox(ctx, ctx.newTxBuilder()))
  }
}

/**
  * // ============== Profile Box Change Profile NFT TX ================= //
  * inputs:  1. ProfileBox, 2. TxFeeFromUser
  * outputs: 1. ProfileBox, 2. MiningBox
  * dataInputs: 1. BoxWithNFT (that user owns)
  * @param ctx Blockchain Context
  */
case class ProfileBoxChangeProfileNFTTx(userAddress: Address, nftId: String, client: Client, explorer: TweetExplorer)(
  implicit val ctx: BlockchainContext
) extends Tx {
  override val changeAddress: P2PKAddress = userAddress.asP2PK()
  override val inputBoxes: Seq[InputBox] = {
    val profileInputBox: InputBox = ProfileBox.get(userAddress)(client)
    val txFeeFromUser: Seq[InputBox] =
      client.getCoveringBoxesFor(userAddress, ErgCommons.MinMinerFee * 2).getBoxes.toSeq

    Seq(profileInputBox) ++ txFeeFromUser
  }

  override val dataInputs: Seq[InputBox] = {
    val nftBox: InputBox = NFTHelper.getNFTInputBox(nftId)(explorer, ctx)
    Seq(nftBox)
  }

  override def getOutBoxes: Seq[OutBox] =
  {
    val profileBox: ProfileBox = ProfileBox.from(ProfileBox.get(userAddress)(client))

    Seq(ProfileBox.changeNFT(profileBox, nftId).getOutBox(ctx, ctx.newTxBuilder()))
  }
}

/**
  * // ============== Profile Box Add Following TX ================= //
  * inputs:  1. ProfileBox, 2. TxFeeFromUser
  * outputs: 1. ProfileBox, 2. MiningBox
  * dataInputs: 1. ToFollowsProfileBox (you can only follow someone who has a profile)
  * @param ctx Blockchain Context
  */
case class ProfileBoxAddFollowingTx(userAddress: Address)(
  implicit val ctx: BlockchainContext
) extends Tx {
  override val changeAddress: P2PKAddress = userAddress.asP2PK()
  override val inputBoxes: Seq[InputBox] = Seq.empty

  override def getOutBoxes: Seq[OutBox] =
    Seq()
}

/**
  * // ============== Profile Box Remove Following TX ================= //
  * inputs:  1. ProfileBox, 2. TxFeeFromUser
  * outputs: 1. ProfileBox, 2. MiningBox
  * dataInputs: 1. ToUnFollowsProfileBox (you can only follow someone who has a profile)
  * @param ctx Blockchain Context
  */
case class ProfileBoxRemoveFollowingTx(userAddress: Address)(
  implicit val ctx: BlockchainContext
) extends Tx {
  override val changeAddress: P2PKAddress = userAddress.asP2PK()
  override val inputBoxes: Seq[InputBox] = Seq.empty

  override def getOutBoxes: Seq[OutBox] =
    Seq()
}

/**
  * // ============== Delete Profile Box TX ================= //
  * inputs:  1. ProfileBox
  * outputs: 1. MiningBox
  * @param ctx Blockchain Context
  */
case class DeleteProfileBoxTx(userAddress: Address, client: Client)(
  implicit val ctx: BlockchainContext
) extends Tx {
  override val changeAddress: P2PKAddress = userAddress.asP2PK()

  override val inputBoxes: Seq[InputBox] = {
    val profileBox: InputBox = client
      .getCoveringBoxesFor(
        ProfileBoxContract.getContract(userAddress).contract.address,
        ErgCommons.MinMinerFee,
        tokensToBurn.toList.asJava
      )
      .head

    Seq(profileBox)
  }

  override val tokensToBurn: Seq[ErgoToken] = Seq(
    new ErgoToken(dumdumdumsProfileToken, 1)
  )

  override def getOutBoxes: Seq[OutBox] = Seq.empty
}
