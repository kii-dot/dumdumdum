package runners.helpers

import commons.ErgCommons
import mint.{Client, NFTMinter, TweetProtocol}
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoClient,
  ErgoId,
  ErgoProver,
  NetworkType,
  ReducedTransaction,
  RestApiErgoClient,
  SecretString,
  SignedTransaction
}
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import profile.{CreateDumDumDumsTx, DumDumDumHandler}
import tokens.SigUSD

/**
  * Spender
  * Get boxes from explorer, and then tries to spend it.
  * Explorer -> Retrieve input boxes
  * Output boxes for spending.
  */
object Spender {
  val message: String = "First DumDumDum Tweet :D"

  val walletAddress: String = {
    ""
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
          nftId =
            "5f02aad340ca7dff6447e6331c2ded8463ae8424dc292d1d1e8b7f0e4f0104e3",
          amount = Long.MaxValue
        )

        burnDumDumDumsTx
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
      mintDumDumDums(ergoClient, prover)
    // Tx Ends Here

    val signed =
      txJsons.map(tx => prover.signReduced(tx._2, 0))

    signed.map(signedTx => client.getContext.sendTransaction(signedTx))
    signed.foreach(txJson => System.out.println(txJson.toJson(true)))
  }
}
