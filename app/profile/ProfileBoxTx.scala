package profile

import commons.ErgCommons
import config.Configs.{dumdumdumsNFT, dumdumdumsProfileToken, serviceFee}
import contracts.ProfileBoxContract
import edge.registers.{AddressRegister, CollAddressRegister, CollByteRegister, CollStringRegister, StringRegister}
import mint.{Client, TweetExplorer}
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoId, ErgoToken, InputBox, OutBox}
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
class ProfileBoxCreationTx(
  userAddress: Address,
  nftId: String,
  client: Client,
  explorer: TweetExplorer
)(
  implicit val ctx: BlockchainContext
) extends Tx {
  override val changeAddress: P2PKAddress = userAddress.asP2PK()

  val profileTokenDistributionBox: InputBox = ctx
    .getBoxesById(
      explorer
        .getUnspentTokenBoxes(dumdumdumsNFT)
        .hcursor
        .downField("boxId")
        .as[String]
        .getOrElse("")
    )
    .head

  override val inputBoxes: Seq[InputBox] = {
    val txFeeFromUser: Seq[InputBox] =
      client
        .getCoveringBoxesFor(
          userAddress,
          ErgCommons.MinMinerFee * 2 + serviceFee,
          Seq(new ErgoToken(nftId, 1)).asJava
        )
        .toSeq

    Seq(profileTokenDistributionBox) ++ txFeeFromUser
  }

  override def getOutBoxes: Seq[OutBox] = {
    val outProfileTokenDistributionBox: ProfileTokenDistributionBox =
      ProfileTokenDistributionBox.decrementProfileToken(
        ProfileTokenDistributionBox.from(profileTokenDistributionBox)
      )

    val profileBox: ProfileBox = ProfileBox(
      tokens =
        Seq(new ErgoToken(dumdumdumsProfileToken, 1), new ErgoToken(nftId, 1)),
      addressRegister = new AddressRegister(userAddress),
      followingRegister = CollAddressRegister.empty
    )

    Seq(
      outProfileTokenDistributionBox.getOutBox(ctx, ctx.newTxBuilder()),
      profileBox.getOutBox(ctx, ctx.newTxBuilder())
    )
  }
}

/**
  * // ============== Profile Box Change Profile NFT TX ================= //
  * inputs:  1. ProfileBox, 2. TxFeeFromUser
  * outputs: 1. ProfileBox, 2. MiningBox
  * dataInputs: 1. BoxWithNFT (that user owns)
  * @param ctx Blockchain Context
  */
class ProfileBoxChangeProfileNFTTx(
  userAddress: Address,
  nftId: String,
  client: Client,
  explorer: TweetExplorer
)(
  implicit val ctx: BlockchainContext
) extends Tx {
  override val changeAddress: P2PKAddress = userAddress.asP2PK()

  override val inputBoxes: Seq[InputBox] = {
    val profileInputBox: InputBox = ProfileBox.get(userAddress)(client)
    val txFeeFromUser: Seq[InputBox] =
      client
        .getCoveringBoxesFor(
          userAddress,
          ErgCommons.MinMinerFee * 2,
          Seq(new ErgoToken(nftId, 1)).asJava
        )

    Seq(profileInputBox) ++ txFeeFromUser
  }

  override def getOutBoxes: Seq[OutBox] = {
    val profileBox: ProfileBox =
      ProfileBox
        .from(ProfileBox.get(userAddress)(client))
        .copy(
          tokens = Seq(
            new ErgoToken(dumdumdumsProfileToken, 1),
            new ErgoToken(nftId, 1)
          )
        )

    Seq(
      profileBox.getOutBox(ctx, ctx.newTxBuilder())
    )
  }
}

/**
  * // ============== Profile Box Add Following TX ================= //
  * inputs:  1. ProfileBox, 2. TxFeeFromUser
  * outputs: 1. ProfileBox, 2. MiningBox
  * dataInputs: 1. ToFollowsProfileBox (you can only follow someone who has a profile)
  * @param ctx Blockchain Context
  */
class ProfileBoxAddFollowingTx(userAddress: Address)(
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
class ProfileBoxRemoveFollowingTx(userAddress: Address)(
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
class DeleteProfileBoxTx(userAddress: Address, client: Client)(
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
