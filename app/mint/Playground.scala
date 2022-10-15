package runners.helpers

import commons.ErgCommons
import config.Configs.{dumdumdumsNFT, dumdumdumsProfileToken, serviceOwner}
import contracts.ProfileTokenDistributionBoxContract
import mint.{Client, NFTMinter, TweetProtocol}
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  BoxOperations,
  ErgoClient,
  ErgoId,
  ErgoProver,
  ErgoToken,
  InputBox,
  NetworkType,
  OutBox,
  ReducedTransaction,
  RestApiErgoClient,
  SecretString,
  SignedTransaction,
  UnsignedTransaction
}
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import profile.{CreateDumDumDumsTx, DumDumDumHandler}
import tokens.SigUSD

import scala.collection.JavaConverters.{
  collectionAsScalaIterableConverter,
  seqAsJavaListConverter
}

/**
  * Spender
  * Get boxes from explorer, and then tries to spend it.
  * Explorer -> Retrieve input boxes
  * Output boxes for spending.
  */
object Spender {
  val message: String = "First DumDumDum Tweet :D"

  val walletAddress: String = {
    serviceOwner
  }

  val tweetId: String =
    "093bdedda43c2d24e10403e170f0796ae80099f43f396e6577c4f65199b6d1c4"

  /**
    * 1. tweet
    * 2. reply
    * 3. retweet
    * 4. delete
    */
  val txType: String = "delete"

  val client = new Client
  client.setClient()

  val NFTMinter =
    new NFTMinter(client = client, tweetProtocol = new TweetProtocol(client))

  val DumDumDumHandler =
    new DumDumDumHandler(client = client)

  def mint(
    ergoClient: ErgoClient,
    prover: ErgoProver
  ): Seq[(Address, ReducedTransaction)] = {
    val reducedTxs: Seq[(Address, ReducedTransaction)] = ergoClient.execute {
      implicit ctx =>
        val mintReducedTx: Seq[(Address, ReducedTransaction)] = {
          txType match {
            case "tweet" =>
              NFTMinter.mintTweetTx(
                message,
                walletAddress
              )
            case "reply"   => NFTMinter.reply(tweetId, message, walletAddress)
            case "retweet" => NFTMinter.retweet(tweetId, message, walletAddress)
            case "delete"  => NFTMinter.delete(tweetId, walletAddress)
          }
        }

        mintReducedTx
    }

    reducedTxs
  }

  def mintDumDumDums(
    ergoClient: ErgoClient,
    prover: ErgoProver
  ): Seq[(Address, ReducedTransaction)] = {
    val reducedTxs: Seq[(Address, ReducedTransaction)] = ergoClient.execute {
      implicit ctx =>
        val createDumDumDumsTx = DumDumDumHandler.mint(
          address = Address.create(walletAddress),
          name = "DumDumDums ProfileBox NFT",
          description =
            "This is the token to identify the DumDumDums Profile Box. The token, I, am the decider of fate of these puny Profile Tokens, Buahahhaha.",
          amount = 1,
          optionalLink =
            "DumDumDum, I am the master token of the Profile Box Buahahaha"
        )

        createDumDumDumsTx
    }

    reducedTxs
  }

  def burnDumDumDums(
    ergoClient: ErgoClient,
    prover: ErgoProver
  ): Seq[(Address, ReducedTransaction)] = {
    val reducedTxs: Seq[(Address, ReducedTransaction)] = ergoClient.execute {
      implicit ctx =>
        val burnDumDumDumsTx = DumDumDumHandler.burn(
          address = Address.create(walletAddress),
          nftId = dumdumdumsProfileToken,
          amount = 1000000000
        )

        burnDumDumDumsTx
    }

    reducedTxs
  }

  def mergeDumDumDums(
    ergoClient: ErgoClient,
    prover: ErgoProver
  ): Seq[(Address, ReducedTransaction)] = {
    val reducedTxs: Seq[(Address, ReducedTransaction)] = ergoClient.execute {
      implicit ctx =>
        val boxOperations =
          BoxOperations.createForSender(Address.create(walletAddress), ctx)
        val inputBoxes: java.util.List[InputBox] = boxOperations
          .withAmountToSpend(ErgCommons.MinBoxFee * 2)
          .withTokensToSpend(
            Seq(
              new ErgoToken(dumdumdumsNFT, 1),
              new ErgoToken(dumdumdumsProfileToken, 1)
            ).toList.asJava
          )
          .loadTop()
        val dumdumdumsNFTTokenBox: InputBox = inputBoxes.asScala.toSeq
          .filter(box =>
            box.getTokens.asScala.toSeq
              .exists(token => token.getId == ErgoId.create(dumdumdumsNFT))
          )
          .head

        val dumdumdumsProfileTokenBox: InputBox = inputBoxes.asScala.toSeq
          .filter(box =>
            box.getTokens.asScala.toSeq.exists(token =>
              token.getId == ErgoId.create(dumdumdumsProfileToken)
            )
          )
          .head

        val dumdumdumsNFTToken: ErgoToken =
          dumdumdumsNFTTokenBox.getTokens.asScala.toSeq
            .filter(token => token.getId == ErgoId.create(dumdumdumsNFT))
            .head

        val dumdumdumsProfileTokenToken: ErgoToken =
          dumdumdumsNFTTokenBox.getTokens.asScala.toSeq
            .filter(token =>
              token.getId == ErgoId.create(dumdumdumsProfileToken)
            )
            .head

        val inputBoxToSpend: Seq[InputBox] =
          if (dumdumdumsNFTTokenBox.getId.equals(
                dumdumdumsProfileTokenBox.getId
              )) {
            Seq(dumdumdumsNFTTokenBox)
          } else Seq(dumdumdumsProfileTokenBox, dumdumdumsNFTTokenBox)

        val outBox: OutBox = ctx
          .newTxBuilder()
          .outBoxBuilder()
          .value(ErgCommons.MinBoxFee)
          .tokens(
            dumdumdumsNFTToken,
            dumdumdumsProfileTokenToken
          )
          .contract(
            ProfileTokenDistributionBoxContract
              .getContract(ctx)
              .contract
              .ergoContract
          )
          .build()

        val tx: UnsignedTransaction = ctx
          .newTxBuilder()
          .boxesToSpend(inputBoxes)
          .outputs(outBox)
          .fee(ErgCommons.MinBoxFee)
          .sendChangeTo(Address.create(walletAddress).getErgoAddress)
          .build()

        val reducedTx: ReducedTransaction = prover.reduce(tx, 0)
        Seq((Address.create(walletAddress), reducedTx))
    }

    reducedTxs
  }

  def main(args: Array[String]): Unit = {
    println("Spender Start Spending")
    val configFileName = "ergo_config.json"
    val conf: ErgoToolConfig = ErgoToolConfig.load(configFileName)

    // Node Configuration values
    val nodeConf: ErgoNodeConfig = conf.getNode
    val explorerUrl: String =
      RestApiErgoClient.getDefaultExplorerUrl(NetworkType.MAINNET)

    val addressIndex: Int = conf.getParameters.get("addressIndex").toInt

    // create ergoClient instance
    val ergoClient: ErgoClient = RestApiErgoClient.create(nodeConf, explorerUrl)

    val prover: ErgoProver = ergoClient.execute { ctx =>
      ctx.newProverBuilder
        .withMnemonic(
          SecretString.create(nodeConf.getWallet.getMnemonic),
          SecretString.create("")
        )
        .withEip3Secret(addressIndex)
        .build()
    }

    // Tx happens Here
    val txJsons: Seq[(Address, ReducedTransaction)] =
      mergeDumDumDums(ergoClient, prover)
    // Tx Ends Here

    val signed =
      txJsons.map(tx => prover.signReduced(tx._2, 0))

    signed.map(signedTx => client.getContext.sendTransaction(signedTx))
    signed.foreach(txJson => System.out.println(txJson.toJson(true)))
  }
}
