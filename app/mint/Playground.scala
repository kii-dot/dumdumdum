package runners.helpers

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
import tokens.SigUSD

/**
  * Spender
  * Get boxes from explorer, and then tries to spend it.
  * Explorer -> Retrieve input boxes
  * Output boxes for spending.
  */
object Spender {
  val name: String = "SLT Test 2"
  val message: String = "test tweet"

  val walletAddress: String = {
    ""
  }

  val tweetId: String =
    "e853d1a8a853a781a5f2b519ea29f5a337b8b629f937864e7e7e00bf2c3a0ada"

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

  def mint(
    ergoClient: ErgoClient,
    prover: ErgoProver
  ): Seq[SignedTransaction] = {
    val txJson: Seq[SignedTransaction] = ergoClient.execute { implicit ctx =>
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

      val signed: Seq[SignedTransaction] =
        mintReducedTx.map(tx => prover.signReduced(tx._2, 0))

      signed.map(signedTx => ctx.sendTransaction(signedTx))
      signed
    }

    txJson
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

    val txJsons: Seq[SignedTransaction] = mint(ergoClient, prover)

    txJsons.foreach(txJson => System.out.println(txJson.toJson(true)))
  }
}
