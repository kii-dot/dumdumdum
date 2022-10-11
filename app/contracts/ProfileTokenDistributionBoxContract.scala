package contracts

import config.Configs.{dumdumdumsNFT, dumdumdumsProfileToken, serviceOwner}
import org.ergoplatform.appkit.{Address, BlockchainContext}

case class ProfileTokenDistributionBoxContract(
  contract: Contract,
  ownerPk: Address
)

object ProfileTokenDistributionBoxContract {

  def build(
    ownerPk: Address
  )(implicit ctx: BlockchainContext): ProfileTokenDistributionBoxContract = {
    val constants: List[(String, Any)] = List(
      (ProfileTokenDistributionBoxContractConstants.ownerPk, ownerPk),
      (
        ProfileTokenDistributionBoxContractConstants.dumdumdumNFT,
        dumdumdumsNFT
      ),
      (
        ProfileTokenDistributionBoxContractConstants.dumdumdumProfileToken,
        dumdumdumsProfileToken
      )
    )

    ProfileTokenDistributionBoxContract(
      contract = Contract.build(
        DumDumDumContracts.ProfileTokenDistributionBox.contractScript,
        constants = constants: _*
      ),
      ownerPk = ownerPk
    )
  }

  def getContract(
    implicit ctx: BlockchainContext
  ): ProfileTokenDistributionBoxContract =
    this.build(ownerPk = Address.create(serviceOwner))
}

object ProfileTokenDistributionBoxContractConstants {
  val ownerPk: String = "_OwnerPK"
  val dumdumdumNFT: String = "_DumDumDumNFT"
  val dumdumdumProfileToken: String = "_DumDumDumProfileToken"
}
