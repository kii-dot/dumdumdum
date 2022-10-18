package profile

import boxes.FundsToAddressBox
import commons.ErgCommons
import config.Configs.{dumdumdumsProfileToken, serviceFee, serviceOwner}
import contracts.ProfileBoxContract
import edge.registers.{AddressRegister, CollAddressRegister}
import mint.{Client, TweetExplorer}
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoToken,
  InputBox,
  OutBox
}
import txs.Tx

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`

abstract class UserTx(userAddress: Address) extends Tx {
  override val changeAddress: P2PKAddress = userAddress.asP2PK()
}

abstract class UserProfileTx(userAddress: Address, client: Client)
    extends UserTx(userAddress) {

  val profileBox: ProfileBox = ProfileBox.from(
    client
      .getAllUnspentBox(
        ProfileBoxContract.getContract(userAddress).contract.address
      )
      .head
  )
}

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
) extends UserTx(userAddress) {
  val txFee: Long = ErgCommons.MinMinerFee * 2 + serviceFee

  val profileTokenDistributionBox: ProfileTokenDistributionBox =
    ProfileTokenDistributionBox.from(
      explorer.getProfileTokenDistributionInputBox
    )

  override val inputBoxes: Seq[InputBox] = {
    val txFeeBox: Seq[InputBox] =
      client
        .getCoveringBoxesFor(
          userAddress,
          txFee + ErgCommons.MinMinerFee,
          Seq(new ErgoToken(nftId, 1)).asJava
        )
        .toSeq

    Seq(profileTokenDistributionBox.box.get.input) ++ txFeeBox
  }

  override def getOutBoxes: Seq[OutBox] = {
    val profileBox: ProfileBox = new ProfileBox(
      addressRegister = new AddressRegister(userAddress),
      followingRegister = new CollAddressRegister(Seq(userAddress)),
      tokens =
        Seq(new ErgoToken(dumdumdumsProfileToken, 1), new ErgoToken(nftId, 1))
    )

    Seq(
      ProfileTokenDistributionBox
        .decrementProfileToken(profileTokenDistributionBox)
        .getOutBox(ctx, ctx.newTxBuilder()),
      profileBox.getOutBox(ctx, ctx.newTxBuilder()),
      FundsToAddressBox(
        address = Address.create(serviceOwner),
        value = serviceFee
      ).getOutBox(ctx, ctx.newTxBuilder())
    )
  }
}

class ProfileBoxIssuerBoxTx(
  userAddress: Address,
  client: Client,
  explorer: TweetExplorer,
  addressToFollow: Option[Address] = Option.empty,
  nftId: String = "",
  txFee: Long = ErgCommons.MinBoxFee * 2
)(implicit val ctx: BlockchainContext)
    extends UserTx(userAddress) {

  val nftToken: Option[ErgoToken] =
    if (nftId.nonEmpty)
      Option(new ErgoToken(nftId, 1))
    else Option.empty

  override val inputBoxes: Seq[InputBox] = {
    if (nftToken.nonEmpty) {
      client
        .getCoveringBoxesFor(
          address = userAddress,
          amount = txFee + ErgCommons.MinMinerFee,
          tokensToSpend = Seq(nftToken.get).asJava
        )
        .toSeq
    } else {
      client
        .getCoveringBoxesFor(
          address = userAddress,
          amount = txFee + ErgCommons.MinMinerFee
        )
        .getBoxes
        .toSeq
    }
  }

  // OutBox with token and user address
  override def getOutBoxes: Seq[OutBox] =
    if (nftToken.nonEmpty) {
      Seq(
        ctx
          .newTxBuilder()
          .outBoxBuilder()
          .value(txFee)
          .tokens(nftToken.get)
          .contract(userAddress.toErgoContract)
          .registers(new AddressRegister(userAddress).toErgoValue.get)
          .build()
      )
    } else if (addressToFollow.nonEmpty) {
      Seq(
        ctx
          .newTxBuilder()
          .outBoxBuilder()
          .value(txFee)
          .contract(userAddress.toErgoContract)
          .registers(new AddressRegister(addressToFollow.get).toErgoValue.get)
          .build()
      )
    } else {
      Seq(
        ctx
          .newTxBuilder()
          .outBoxBuilder()
          .value(txFee)
          .contract(userAddress.toErgoContract)
          .registers(new AddressRegister(userAddress).toErgoValue.get)
          .build()
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
  explorer: TweetExplorer,
  txFeeBox: InputBox
)(
  implicit val ctx: BlockchainContext
) extends UserProfileTx(userAddress, client) {
  override val changeAddress: P2PKAddress = userAddress.asP2PK()

  override val inputBoxes: Seq[InputBox] = {
    val profileInputBox: InputBox = profileBox.box.get.input

    Seq(profileInputBox) ++ Seq(txFeeBox)
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
class ProfileBoxAddFollowingTx(
  userAddress: Address,
  addressToFollow: Address,
  txFeeBox: InputBox,
  client: Client
)(
  implicit val ctx: BlockchainContext
) extends UserProfileTx(userAddress, client) {
  override val changeAddress: P2PKAddress = userAddress.asP2PK()

  override val inputBoxes: Seq[InputBox] = {
    Seq(profileBox.box.get.input, txFeeBox)
  }

  override def getOutBoxes: Seq[OutBox] = {
    val followingRegister: CollAddressRegister = new CollAddressRegister(
      profileBox.followingRegister.collAddress ++ Seq(addressToFollow)
    )
    Seq(
      profileBox
        .copy(
          followingRegister = followingRegister
        )
        .getOutBox(ctx, ctx.newTxBuilder())
    )
  }
}

/**
  * // ============== Profile Box Remove Following TX ================= //
  * inputs:  1. ProfileBox, 2. TxFeeFromUser
  * outputs: 1. ProfileBox, 2. MiningBox
  * dataInputs: 1. ToUnFollowsProfileBox (you can only follow someone who has a profile)
  * @param ctx Blockchain Context
  */
class ProfileBoxRemoveFollowingTx(
  userAddress: Address,
  addressToUnfollow: Address,
  txFeeBox: InputBox,
  client: Client
)(
  implicit val ctx: BlockchainContext
) extends UserProfileTx(userAddress, client) {
  override val changeAddress: P2PKAddress = userAddress.asP2PK()

  override val inputBoxes: Seq[InputBox] = {
    Seq(profileBox.box.get.input, txFeeBox)
  }

  override def getOutBoxes: Seq[OutBox] = {
    val collAddressRegister: CollAddressRegister = new CollAddressRegister(
      profileBox.followingRegister.collAddress.filter(address =>
        !address.equals(addressToUnfollow)
      )
    )
    Seq(
      profileBox
        .copy(
          followingRegister = collAddressRegister
        )
        .getOutBox(ctx, ctx.newTxBuilder())
    )
  }
}

/**
  * // ============== Delete Profile Box TX ================= //
  * inputs:  1. ProfileBox
  * outputs: 1. MiningBox
  * @param ctx Blockchain Context
  */
class DeleteProfileBoxTx(
  userAddress: Address,
  client: Client,
  txFeeBox: InputBox
)(
  implicit val ctx: BlockchainContext
) extends UserProfileTx(userAddress, client) {

  override val inputBoxes: Seq[InputBox] = {
    Seq(profileBox.box.get.input) ++ Seq(txFeeBox)
  }

  override val tokensToBurn: Seq[ErgoToken] = Seq(
    new ErgoToken(dumdumdumsProfileToken, 1)
  )

  override def getOutBoxes: Seq[OutBox] =
    Seq(
      FundsToAddressBox(
        address = userAddress,
        value = ErgCommons.MinBoxFee,
        tokens = Seq(profileBox.tokens(1)),
        R4 = profileBox.R4
      ).getOutBox(ctx, ctx.newTxBuilder())
    )
}
